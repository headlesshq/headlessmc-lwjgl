package io.github.headlesshq.headlessmc.lwjgl3.probe;

import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.NativeType;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

/**
 * A probed method with the candidate values of each of its parameters.
 * Every combination of candidates is one call, combinations are numbered deterministically, so that a worker can
 * continue after the call that crashed the previous one.
 */
final class Target {
    static final int MAX_COMBINATIONS = 20_000;
    static final int OUT_CAPACITY = 1024;
    static final int OUT_LIMIT = 16;
    private static final Pattern RAW_METHOD = Pattern.compile("n[a-z]+[A-Z0-9].*");
    private static final Set<String> ENUM_PARAMETER_NAMES = Set.of(
        "attrib", "attribute", "hint", "pname", "param", "parameter", "target", "cap", "mode", "type", "format",
        "key", "button", "query", "token", "name");
    private static final int INT_SENTINEL = 0x7EADBEEF;
    private static final long LONG_SENTINEL = 0x7EADBEEF7EADBEEFL;
    private static final byte BYTE_SENTINEL = (byte) 0xA5;

    final Method method;
    final String key;
    final List<List<Candidate>> candidates;
    final long combinations;
    /**
     * The capabilities fields ({@code owner.name}) holding the function pointers the method calls.
     */
    final Set<String> functions;

    private Target(Method method, List<List<Candidate>> candidates, Set<String> functions) {
        this.method = method;
        this.functions = functions;
        this.key = Type.getInternalName(method.getDeclaringClass()) + "." + method.getName() + Type.getMethodDescriptor(method);
        this.candidates = candidates;
        long product = 1;
        for (List<Candidate> list : candidates) {
            product = Math.multiplyExact(product, list.size());
        }

        this.combinations = product;
    }

    /**
     * @return the number of calls made for this target.
     */
    long calls() {
        return Math.min(combinations, MAX_COMBINATIONS);
    }

    /**
     * @param call the number of the call, smaller than {@link #calls()}.
     * @return the candidates of the call, evenly sampled if there are more combinations than calls.
     */
    Candidate[] combination(long call) {
        long index = combinations <= MAX_COMBINATIONS ? call : call * (combinations / MAX_COMBINATIONS);
        Candidate[] result = new Candidate[candidates.size()];
        for (int i = candidates.size() - 1; i >= 0; i--) {
            List<Candidate> list = candidates.get(i);
            result[i] = list.get((int) (index % list.size()));
            index /= list.size();
        }

        return result;
    }

    /**
     * Collects the probed methods of a suite: public static methods that return something or write into an output
     * parameter, whose parameters all have candidates, without the raw {@code n*} variants, the denied ones and the
     * ones of compatibility classes that only delegate to the core class, like {@code GL11} to {@code GL11C}.
     */
    static List<Target> collect(Suite suite) {
        Map<Integer, String> enums = Suite.intConstants(suite.enumClasses());
        Map<String, Integer> byName = new HashMap<>();
        enums.forEach((value, name) -> byName.put(name, value));
        List<Candidate> allEnums = new ArrayList<>();
        enums.forEach((value, name) -> allEnums.add(Candidate.value(value, name)));
        List<Candidate> contextEnums = new ArrayList<>();
        for (String name : suite.contextEnums()) {
            Integer value = byName.get(name);
            if (value != null) {
                contextEnums.add(Candidate.value(value, name));
            }
        }

        List<Target> result = new ArrayList<>();
        for (Class<?> type : suite.classes()) {
            ClassNode node = read(type);
            Map<String, List<String>> parameterNames = parameterNames(node);
            Map<String, Set<String>> functions = functions(node);
            Class<?> core = coreClass(type);
            List<Method> methods = new ArrayList<>(Arrays.asList(type.getDeclaredMethods()));
            methods.sort(Comparator.comparing((Method m) -> m.getName()).thenComparing(m -> Type.getMethodDescriptor(m)));
            for (Method method : methods) {
                int modifiers = method.getModifiers();
                if (!Modifier.isPublic(modifiers) || !Modifier.isStatic(modifiers) || method.isSynthetic()
                    || RAW_METHOD.matcher(method.getName()).matches() || suite.denied().contains(method.getName())
                    || core != null && declares(core, method)) {
                    continue;
                }

                List<String> names = parameterNames.get(method.getName() + Type.getMethodDescriptor(method));
                Target target = create(suite, method, names, allEnums, contextEnums,
                                       functions.getOrDefault(method.getName() + Type.getMethodDescriptor(method), Set.of()));
                if (target != null) {
                    result.add(target);
                }
            }
        }

        return result;
    }

    private static Target create(Suite suite, Method method, List<String> names, List<Candidate> allEnums,
                                 List<Candidate> contextEnums, Set<String> functions) {
        Parameter[] parameters = method.getParameters();
        int lastEnum = -1;
        for (int i = 0; i < parameters.length; i++) {
            if (isEnum(parameters[i], name(names, i))) {
                lastEnum = i;
            }
        }

        boolean output = false;
        List<List<Candidate>> candidates = new ArrayList<>();
        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            String name = name(names, i);
            List<Candidate> list;
            if (isEnum(parameter, name)) {
                list = i == lastEnum ? allEnums : contextEnums;
            } else {
                list = candidates(suite, parameter, name);
            }

            if (list == null || list.isEmpty()) {
                return null;
            }

            output |= list.get(0).output();
            candidates.add(list);
        }

        if (method.getReturnType() == void.class && !output) {
            return null;
        }

        return new Target(method, candidates, functions);
    }

    private static List<Candidate> candidates(Suite suite, Parameter parameter, String name) {
        Class<?> type = parameter.getType();
        String nativeType = nativeType(parameter);
        boolean isConst = nativeType != null && nativeType.contains("const");
        if (type == boolean.class) {
            return List.of(Candidate.value(false, "false"), Candidate.value(true, "true"));
        } else if (type == float.class) {
            return List.of(Candidate.value(0.0f, "0"), Candidate.value(1.0f, "1"));
        } else if (type == double.class) {
            return List.of(Candidate.value(0.0, "0"), Candidate.value(1.0, "1"));
        } else if (type == byte.class || type == short.class || type == char.class) {
            return null;
        } else if (type == int.class) {
            Integer object = name == null ? null : suite.objects().get(name);
            if (object != null) {
                return List.of(Candidate.value(object, name), Candidate.value(0, "0"));
            }

            return List.of(Candidate.value(0, "0"), Candidate.value(1, "1"));
        } else if (type == long.class) {
            Long handle = nativeType == null ? null : suite.handles().get(nativeType);
            if (handle != null) {
                return List.of(Candidate.value(handle, nativeType));
            } else if (nativeType == null || nativeType.contains("*") || nativeType.contains("GLsync")) {
                return null;
            }

            return List.of(Candidate.value(0L, "0"), Candidate.value(1L, "1"));
        } else if (type == CharSequence.class || type == String.class) {
            List<Candidate> result = new ArrayList<>();
            for (String string : suite.strings()) {
                result.add(Candidate.value(string, Json.write(string)));
            }

            return result;
        } else if (type == PointerBuffer.class || !isConst && (Buffer.class.isAssignableFrom(type) || type.isArray())) {
            return type.isArray() && !type.getComponentType().isPrimitive() ? null : List.of(Candidate.output(type));
        }

        return null;
    }

    private static boolean isEnum(Parameter parameter, String name) {
        if (parameter.getType() != int.class) {
            return false;
        }

        String nativeType = nativeType(parameter);
        if (nativeType != null) {
            return nativeType.toLowerCase().contains("enum");
        }

        return name != null && ENUM_PARAMETER_NAMES.contains(name);
    }

    private static String nativeType(Parameter parameter) {
        NativeType annotation = parameter.getAnnotation(NativeType.class);
        return annotation == null ? null : annotation.value();
    }

    private static String name(List<String> names, int index) {
        return names == null || index >= names.size() ? null : names.get(index);
    }

    private static Class<?> coreClass(Class<?> type) {
        try {
            return Class.forName(type.getName() + "C", false, type.getClassLoader());
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    private static boolean declares(Class<?> type, Method method) {
        try {
            type.getDeclaredMethod(method.getName(), method.getParameterTypes());
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    private static ClassNode read(Class<?> type) {
        ClassNode node = new ClassNode();
        try (InputStream in = type.getResourceAsStream("/" + Type.getInternalName(type) + ".class")) {
            if (in != null) {
                new ClassReader(in).accept(node, ClassReader.SKIP_FRAMES);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read " + type.getName(), e);
        }

        return node;
    }

    /**
     * Reads the parameter names from the local variable tables, since LWJGL is not compiled with {@code -parameters}.
     */
    private static Map<String, List<String>> parameterNames(ClassNode node) {
        Map<String, List<String>> result = new LinkedHashMap<>();
        for (MethodNode method : node.methods) {
            if (method.localVariables == null) {
                continue;
            }

            List<String> names = new ArrayList<>();
            int slot = (method.access & Modifier.STATIC) != 0 ? 0 : 1;
            for (Type argument : Type.getArgumentTypes(method.desc)) {
                String name = null;
                for (LocalVariableNode local : method.localVariables) {
                    if (local.index == slot) {
                        name = local.name;
                        break;
                    }
                }

                names.add(name);
                slot += argument.getSize();
            }

            result.put(method.name + method.desc, names);
        }

        return result;
    }

    /**
     * Finds the function pointers every method loads from a capabilities object, like
     * {@code GL.getICD().glGetIntegerv}, directly or through the raw {@code n*} method of the same class.
     */
    private static Map<String, Set<String>> functions(ClassNode node) {
        Map<String, Set<String>> direct = new HashMap<>();
        Map<String, Set<String>> calls = new HashMap<>();
        for (MethodNode method : node.methods) {
            Set<String> fields = new TreeSet<>();
            Set<String> called = new TreeSet<>();
            for (AbstractInsnNode insn : method.instructions) {
                if (insn instanceof FieldInsnNode field && field.getOpcode() == Opcodes.GETFIELD && "J".equals(field.desc)
                    && field.owner.endsWith("Capabilities")) {
                    fields.add(field.owner + "." + field.name);
                } else if (insn instanceof MethodInsnNode call && call.owner.equals(node.name)) {
                    called.add(call.name + call.desc);
                }
            }

            direct.put(method.name + method.desc, fields);
            calls.put(method.name + method.desc, called);
        }

        Map<String, Set<String>> result = new HashMap<>();
        direct.forEach((method, fields) -> {
            Set<String> all = new TreeSet<>(fields);
            for (String called : calls.get(method)) {
                all.addAll(direct.getOrDefault(called, Set.of()));
            }

            result.put(method, all);
        });
        return result;
    }

    /**
     * A candidate value of a parameter.
     *
     * @param value the value, or the type of the output buffer to create.
     * @param label how the value is written into the recording.
     * @param output {@code true} if a new output buffer of type {@code value} is created for every call.
     */
    record Candidate(Object value, String label, boolean output) {
        static Candidate value(Object value, String label) {
            return new Candidate(value, label, false);
        }

        static Candidate output(Class<?> type) {
            return new Candidate(type, "out:" + type.getSimpleName(), true);
        }

        /**
         * Output buffers have a capacity of {@value #OUT_CAPACITY} elements, so that functions that write more than they
         * are told to do not corrupt memory, but a limit of {@value #OUT_LIMIT} (byte buffers: {@value #OUT_CAPACITY}), since LWJGL uses the remaining
         * elements as the count for functions like {@code glGenTextures}. Arrays have no limit and keep their size.
         *
         * @return the argument to pass, a new output buffer filled with a sentinel for outputs.
         */
        Object create() {
            if (!output) {
                return value;
            }

            Class<?> type = (Class<?>) value;
            if (type == ByteBuffer.class) {
                ByteBuffer buffer = BufferUtils.createByteBuffer(OUT_CAPACITY * 8);
                for (int i = 0; i < buffer.capacity(); i++) {
                    buffer.put(i, BYTE_SENTINEL);
                }

                return buffer.limit(OUT_CAPACITY);
            } else if (type == ShortBuffer.class) {
                ShortBuffer buffer = BufferUtils.createShortBuffer(OUT_CAPACITY);
                for (int i = 0; i < buffer.capacity(); i++) {
                    buffer.put(i, (short) INT_SENTINEL);
                }

                return buffer.limit(OUT_LIMIT);
            } else if (type == IntBuffer.class) {
                IntBuffer buffer = BufferUtils.createIntBuffer(OUT_CAPACITY);
                for (int i = 0; i < buffer.capacity(); i++) {
                    buffer.put(i, INT_SENTINEL);
                }

                return buffer.limit(OUT_LIMIT);
            } else if (type == FloatBuffer.class) {
                FloatBuffer buffer = BufferUtils.createFloatBuffer(OUT_CAPACITY);
                for (int i = 0; i < buffer.capacity(); i++) {
                    buffer.put(i, Float.intBitsToFloat(INT_SENTINEL));
                }

                return buffer.limit(OUT_LIMIT);
            } else if (type == LongBuffer.class) {
                LongBuffer buffer = BufferUtils.createLongBuffer(OUT_CAPACITY);
                for (int i = 0; i < buffer.capacity(); i++) {
                    buffer.put(i, LONG_SENTINEL);
                }

                return buffer.limit(OUT_LIMIT);
            } else if (type == DoubleBuffer.class) {
                DoubleBuffer buffer = BufferUtils.createDoubleBuffer(OUT_CAPACITY);
                for (int i = 0; i < buffer.capacity(); i++) {
                    buffer.put(i, Double.longBitsToDouble(LONG_SENTINEL));
                }

                return buffer.limit(OUT_LIMIT);
            } else if (type == PointerBuffer.class) {
                PointerBuffer buffer = BufferUtils.createPointerBuffer(OUT_CAPACITY);
                for (int i = 0; i < buffer.capacity(); i++) {
                    buffer.put(i, LONG_SENTINEL);
                }

                return buffer.limit(OUT_LIMIT);
            } else if (type == int[].class) {
                int[] array = new int[OUT_CAPACITY];
                Arrays.fill(array, INT_SENTINEL);
                return array;
            } else if (type == float[].class) {
                float[] array = new float[OUT_CAPACITY];
                Arrays.fill(array, Float.intBitsToFloat(INT_SENTINEL));
                return array;
            } else if (type == long[].class) {
                long[] array = new long[OUT_CAPACITY];
                Arrays.fill(array, LONG_SENTINEL);
                return array;
            } else if (type == double[].class) {
                double[] array = new double[OUT_CAPACITY];
                Arrays.fill(array, Double.longBitsToDouble(LONG_SENTINEL));
                return array;
            } else if (type == short[].class) {
                short[] array = new short[OUT_CAPACITY];
                Arrays.fill(array, (short) INT_SENTINEL);
                return array;
            }

            throw new IllegalStateException("Unsupported output type " + type);
        }

        /**
         * @param argument the output buffer after the call.
         * @return the values the call wrote, up to the last one that differs from the sentinel.
         */
        static Object written(Object argument) {
            List<Object> values = new ArrayList<>();
            if (argument instanceof ByteBuffer buffer) {
                int count = 0;
                for (int i = 0; i < buffer.capacity(); i++) {
                    if (buffer.get(i) != BYTE_SENTINEL) {
                        count = i + 1;
                    }
                }

                StringBuilder text = new StringBuilder();
                for (int i = 0; i < count && buffer.get(i) != 0; i++) {
                    text.append((char) (buffer.get(i) & 0xFF));
                }

                Map<String, Object> result = new LinkedHashMap<>();
                result.put("bytes", count);
                result.put("text", text.toString());
                return result;
            } else if (argument instanceof ShortBuffer buffer) {
                for (int i = 0; i < buffer.capacity(); i++) {
                    values.add(buffer.get(i) == (short) INT_SENTINEL ? null : buffer.get(i));
                }
            } else if (argument instanceof IntBuffer buffer) {
                for (int i = 0; i < buffer.capacity(); i++) {
                    values.add(buffer.get(i) == INT_SENTINEL ? null : buffer.get(i));
                }
            } else if (argument instanceof FloatBuffer buffer) {
                for (int i = 0; i < buffer.capacity(); i++) {
                    values.add(Float.floatToRawIntBits(buffer.get(i)) == INT_SENTINEL ? null : buffer.get(i));
                }
            } else if (argument instanceof LongBuffer buffer) {
                for (int i = 0; i < buffer.capacity(); i++) {
                    values.add(buffer.get(i) == LONG_SENTINEL ? null : buffer.get(i));
                }
            } else if (argument instanceof DoubleBuffer buffer) {
                for (int i = 0; i < buffer.capacity(); i++) {
                    values.add(Double.doubleToRawLongBits(buffer.get(i)) == LONG_SENTINEL ? null : buffer.get(i));
                }
            } else if (argument instanceof PointerBuffer buffer) {
                for (int i = 0; i < buffer.capacity(); i++) {
                    long value = buffer.get(i);
                    values.add(value == LONG_SENTINEL ? null : value == 0L ? "0x0" : "nonnull");
                }
            } else if (argument instanceof int[] array) {
                for (int value : array) {
                    values.add(value == INT_SENTINEL ? null : value);
                }
            } else if (argument instanceof float[] array) {
                for (float value : array) {
                    values.add(Float.floatToRawIntBits(value) == INT_SENTINEL ? null : value);
                }
            } else if (argument instanceof long[] array) {
                for (long value : array) {
                    values.add(value == LONG_SENTINEL ? null : value);
                }
            } else if (argument instanceof double[] array) {
                for (double value : array) {
                    values.add(Double.doubleToRawLongBits(value) == LONG_SENTINEL ? null : value);
                }
            } else if (argument instanceof short[] array) {
                for (short value : array) {
                    values.add(value == (short) INT_SENTINEL ? null : value);
                }
            }

            int count = 0;
            for (int i = 0; i < values.size(); i++) {
                if (values.get(i) != null) {
                    count = i + 1;
                }
            }

            return new ArrayList<>(values.subList(0, Math.min(count, 64)));
        }
    }

}
