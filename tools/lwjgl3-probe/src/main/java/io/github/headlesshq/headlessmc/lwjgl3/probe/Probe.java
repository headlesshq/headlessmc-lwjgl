package io.github.headlesshq.headlessmc.lwjgl3.probe;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Records what the real LWJGL returns: runs every {@link Suite} in {@link Worker} processes and writes
 * {@code <output>/<suite>.jsonl}. A call that crashes the JVM or hangs is recorded as {@code crash} or
 * {@code timeout}, and a new worker continues with the next call. After {@value #MAX_FAILURES_PER_METHOD} failures in
 * a row the rest of the method is skipped and recorded as {@code abandoned}.
 * <pre>
 * --output /path/to/recordings/name
 * --logs /path/to/log/directory
 * [--suites glfw-opengl,openal]
 * [--xvfb] run the workers in xvfb-run
 * [--software] force Mesa's software renderers
 * [--timeout seconds] per call, default 15
 * </pre>
 */
public final class Probe {
    private static final String END = "\u0000end";
    private static final int MAX_RESTARTS = 1000;
    private static final int MAX_FAILURES_PER_METHOD = 3;

    private final Path output;
    private final Path logs;
    private final boolean xvfb;
    private final boolean software;
    private final long timeoutSeconds;

    private Probe(Path output, Path logs, boolean xvfb, boolean software, long timeoutSeconds) {
        this.output = output;
        this.logs = logs;
        this.xvfb = xvfb;
        this.software = software;
        this.timeoutSeconds = timeoutSeconds;
    }

    public static void main(String[] args) throws Exception {
        Path output = null;
        Path logs = null;
        List<String> suites = new ArrayList<>();
        boolean xvfb = false;
        boolean software = false;
        long timeout = 15;
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--output" -> output = Path.of(args[++i]);
                case "--logs" -> logs = Path.of(args[++i]);
                case "--suites" -> suites.addAll(List.of(args[++i].split(",")));
                case "--xvfb" -> xvfb = true;
                case "--software" -> software = true;
                case "--timeout" -> timeout = Long.parseLong(args[++i]);
                default -> throw new IllegalArgumentException("Unknown argument " + args[i]);
            }
        }

        if (output == null || logs == null) {
            throw new IllegalArgumentException("--output and --logs are required");
        }

        if (suites.isEmpty()) {
            Suite.all().forEach(suite -> suites.add(suite.name()));
        }

        Files.createDirectories(output);
        Files.createDirectories(logs);
        Probe probe = new Probe(output, logs, xvfb, software, timeout);
        boolean failed = false;
        for (String suite : suites) {
            failed |= !probe.run(suite);
        }

        if (failed) {
            System.exit(1);
        }
    }

    private boolean run(String suite) throws IOException, InterruptedException {
        Path file = output.resolve(suite + ".jsonl");
        int method = 0;
        long call = 0;
        int restarts = 0;
        String failedKey = null;
        int failures = 0;
        boolean headerWritten = false;
        long start = System.nanoTime();
        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            while (true) {
                Process process = start(suite, method, call, restarts);
                BlockingQueue<String> lines = new LinkedBlockingQueue<>();
                Thread reader = reader(process, lines);
                String key = null;
                int currentMethod = method;
                long currentCall = -1;
                boolean done = false;
                boolean timedOut = false;
                while (true) {
                    String line = lines.poll(timeoutSeconds, TimeUnit.SECONDS);
                    if (line == null) {
                        timedOut = true;
                        break;
                    } else if (line == END) {
                        break;
                    } else if (line.equals("D")) {
                        done = true;
                    } else if (line.startsWith("H ")) {
                        if (!headerWritten) {
                            writer.write(line.substring(2));
                            writer.newLine();
                            headerWritten = true;
                        }
                    } else if (line.startsWith("M ")) {
                        String[] parts = line.split(" ", 3);
                        currentMethod = Integer.parseInt(parts[1]);
                        key = parts[2];
                        currentCall = -1;
                    } else if (line.startsWith("P ")) {
                        currentCall = Long.parseLong(line.substring(line.lastIndexOf(' ') + 1));
                    } else if (line.startsWith("R ") || line.startsWith("S ")) {
                        writer.write(line.substring(2));
                        writer.newLine();
                    }
                }

                process.descendants().forEach(ProcessHandle::destroyForcibly);
                process.destroyForcibly();
                process.waitFor(10, TimeUnit.SECONDS);
                reader.join(TimeUnit.SECONDS.toMillis(10));
                if (done) {
                    break;
                }

                if (key == null || currentCall < 0) {
                    System.err.println(suite + ": worker failed before the first call (exit " + exit(process)
                                           + "), see " + log(suite, restarts));
                    return false;
                }

                Map<String, Object> failure = new LinkedHashMap<>();
                failure.put("type", timedOut ? "timeout" : "crash");
                failure.put("m", key);
                failure.put("call", currentCall);
                failure.put("exit", exit(process));
                writer.write(Json.write(failure));
                writer.newLine();
                writer.flush();
                System.err.println(suite + ": " + failure.get("type") + " in " + key + " call " + currentCall);
                failures = key.equals(failedKey) ? failures + 1 : 1;
                failedKey = key;
                method = currentMethod;
                call = currentCall + 1;
                if (failures >= MAX_FAILURES_PER_METHOD) {
                    Map<String, Object> abandoned = new LinkedHashMap<>();
                    abandoned.put("type", "abandoned");
                    abandoned.put("m", key);
                    abandoned.put("next", call);
                    writer.write(Json.write(abandoned));
                    writer.newLine();
                    method = currentMethod + 1;
                    call = 0;
                }

                if (++restarts > MAX_RESTARTS) {
                    System.err.println(suite + ": giving up after " + MAX_RESTARTS + " restarts");
                    return false;
                }
            }
        }

        System.out.println(suite + ": done in " + TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - start)
                               + "s with " + restarts + " restart(s), wrote " + file);
        return true;
    }

    private Process start(String suite, int method, long call, int restart) throws IOException {
        List<String> command = new ArrayList<>();
        if (xvfb) {
            command.addAll(List.of("xvfb-run", "-a", "-s", "-screen 0 1920x1080x24"));
        }

        command.add(Path.of(System.getProperty("java.home"), "bin", "java").toString());
        command.add("--enable-native-access=ALL-UNNAMED");
        command.add("-cp");
        command.add(System.getProperty("java.class.path"));
        command.add(Worker.class.getName());
        command.addAll(List.of("--suite", suite, "--method", Integer.toString(method), "--call", Long.toString(call)));
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.redirectError(log(suite, restart).toFile());
        builder.environment().put("ALSOFT_DRIVERS", "null");
        if (software) {
            builder.environment().put("LIBGL_ALWAYS_SOFTWARE", "1");
            builder.environment().put("GALLIUM_DRIVER", "llvmpipe");
            builder.environment().put("VK_DRIVER_FILES", "/usr/share/vulkan/icd.d/lvp_icd.json");
        }

        return builder.start();
    }

    private Path log(String suite, int restart) {
        return logs.resolve(suite + "-" + restart + ".log");
    }

    private static Thread reader(Process process, BlockingQueue<String> lines) {
        Thread thread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    lines.add(line);
                }
            } catch (IOException ignored) {
            } finally {
                lines.add(END);
            }
        }, "probe-reader");
        thread.setDaemon(true);
        thread.start();
        return thread;
    }

    private static Object exit(Process process) {
        return process.isAlive() ? "killed" : process.exitValue();
    }

}
