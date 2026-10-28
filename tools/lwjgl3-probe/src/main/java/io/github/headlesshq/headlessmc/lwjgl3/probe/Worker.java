package io.github.headlesshq.headlessmc.lwjgl3.probe;

import org.lwjgl.Version;
import org.lwjgl.system.CustomBuffer;
import org.lwjgl.system.NativeType;
import org.lwjgl.system.Struct;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.Buffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Child process of {@link Probe}: sets up a {@link Suite} and calls its targets, starting at the given method and
 * call. Reports to standard output, one message per line:
 * <ul>
 *     <li>{@code H <json>} the header,</li>
 *     <li>{@code M <index> <key>} before the calls of a method,</li>
 *     <li>{@code P <index> <call>} before every call,</li>
 *     <li>{@code R <json>} a call without error,</li>
 *     <li>{@code S <json>} the summary of a method,</li>
 *     <li>{@code D} when all targets are done.</li>
 * </ul>
 * Other lines, e.g. from native code, are ignored by the supervisor.
 */
public final class Worker {
    private static final int MAX_EQUAL_EXCEPTIONS = 16;

    private Worker() {
        throw new AssertionError();
    }

    public static void main(String[] args) {
        String suiteName = null;
        int startMethod = 0;
        long startCall = 0;
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--suite" -> suiteName = args[++i];
                case "--method" -> startMethod = Integer.parseInt(args[++i]);
                case "--call" -> startCall = Long.parseLong(args[++i]);
                default -> throw new IllegalArgumentException("Unknown argument " + args[i]);
            }
        }

        PrintStream out = new PrintStream(new FileOutputStream(FileDescriptor.out), true, StandardCharsets.UTF_8);
        Suite suite = Suite.byName(suiteName);
        suite.setUp();
        List<Target> targets = Target.collect(suite);

        Map<String, Object> header = new LinkedHashMap<>();
        header.put("type", "header");
        header.put("suite", suite.name());
        header.put("lwjgl", Version.getVersion());
        header.put("java", System.getProperty("java.version"));
        header.put("os", System.getProperty("os.name") + " " + System.getProperty("os.version") + " " + System.getProperty("os.arch"));
        header.put("targets", targets.size());
        header.putAll(suite.info());
        out.println("H " + Json.write(header));

        for (int index = startMethod; index < targets.size(); index++) {
            Target target = targets.get(index);
            out.println("M " + index + " " + target.key);
            run(out, suite, target, index, index == startMethod ? startCall : 0);
        }

        out.println("D");
    }

    private static void run(PrintStream out, Suite suite, Target target, int index, long startCall) {
        String unavailable = suite.unavailableFunction(target);
        if (unavailable != null) {
            Map<String, Object> summary = new LinkedHashMap<>();
            summary.put("type", "summary");
            summary.put("m", target.key);
            summary.put("unavailable", unavailable);
            out.println("S " + Json.write(summary));
            return;
        }

        Map<String, Integer> errors = new TreeMap<>();
        Map<String, Integer> exceptions = new TreeMap<>();
        int ok = 0;
        long calls = 0;
        String lastException = null;
        int equalExceptions = 0;
        String abandoned = null;
        for (long call = startCall; call < target.calls(); call++) {
            out.println("P " + index + " " + call);
            calls++;
            Target.Candidate[] candidates = target.combination(call);
            Object[] arguments = new Object[candidates.length];
            for (int i = 0; i < candidates.length; i++) {
                arguments[i] = candidates[i].create();
            }

            Object result;
            try {
                result = target.method.invoke(null, arguments);
            } catch (InvocationTargetException e) {
                Throwable cause = e.getCause();
                String exception = cause.getClass().getName() + ": " + cause.getMessage();
                exceptions.merge(cause.getClass().getName(), 1, Integer::sum);
                suite.error(target.method);
                equalExceptions = exception.equals(lastException) ? equalExceptions + 1 : 1;
                lastException = exception;
                if (ok == 0 && equalExceptions >= MAX_EQUAL_EXCEPTIONS) {
                    abandoned = exception;
                    break;
                }

                continue;
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(e);
            }

            equalExceptions = 0;
            String error = suite.error(target.method);
            if (error != null) {
                errors.merge(error, 1, Integer::sum);
                continue;
            }

            ok++;
            Map<String, Object> record = new LinkedHashMap<>();
            record.put("m", target.key);
            List<Object> labels = new ArrayList<>();
            Map<String, Object> outputs = new LinkedHashMap<>();
            for (int i = 0; i < candidates.length; i++) {
                labels.add(candidates[i].label());
                if (candidates[i].output()) {
                    outputs.put(Integer.toString(i), Target.Candidate.written(arguments[i]));
                }
            }

            record.put("a", labels);
            if (target.method.getReturnType() != void.class) {
                record.put("r", describe(target.method, result));
            }

            if (!outputs.isEmpty()) {
                record.put("out", outputs);
            }

            out.println("R " + Json.write(record));
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("type", "summary");
        summary.put("m", target.key);
        summary.put("from", startCall);
        summary.put("calls", calls);
        summary.put("combinations", target.combinations);
        summary.put("ok", ok);
        if (!errors.isEmpty()) {
            summary.put("errors", errors);
        }

        if (!exceptions.isEmpty()) {
            summary.put("exceptions", exceptions);
        }

        if (abandoned != null) {
            summary.put("abandoned", abandoned);
        }

        out.println("S " + Json.write(summary));
    }

    private static Object describe(Method method, Object result) {
        if (result == null) {
            return null;
        } else if (result instanceof Long value) {
            NativeType nativeType = method.getAnnotation(NativeType.class);
            if (nativeType != null && (nativeType.value().contains("*") || nativeType.value().equals("GLsync"))) {
                return value == 0L ? "0x0" : "nonnull";
            }

            return value;
        } else if (result instanceof Number || result instanceof Boolean || result instanceof String) {
            return result;
        } else if (result instanceof Struct<?> struct) {
            Map<String, Object> fields = new TreeMap<>();
            for (Method getter : struct.getClass().getDeclaredMethods()) {
                if (Modifier.isPublic(getter.getModifiers()) && !Modifier.isStatic(getter.getModifiers())
                    && getter.getParameterCount() == 0 && getter.getReturnType().isPrimitive()
                    && getter.getReturnType() != void.class && !"sizeof".equals(getter.getName())
                    && !"address".equals(getter.getName())) {
                    try {
                        fields.put(getter.getName(), getter.invoke(struct));
                    } catch (ReflectiveOperationException e) {
                        fields.put(getter.getName(), "exception");
                    }
                }
            }

            Map<String, Object> description = new LinkedHashMap<>();
            description.put("struct", struct.getClass().getSimpleName());
            description.put("fields", fields);
            return description;
        } else if (result instanceof CustomBuffer<?> buffer) {
            return Map.of("type", buffer.getClass().getSimpleName(), "capacity", buffer.capacity());
        } else if (result instanceof Buffer buffer) {
            return Map.of("type", buffer.getClass().getSimpleName(), "capacity", buffer.capacity());
        }

        return Map.of("type", result.getClass().getName());
    }

}
