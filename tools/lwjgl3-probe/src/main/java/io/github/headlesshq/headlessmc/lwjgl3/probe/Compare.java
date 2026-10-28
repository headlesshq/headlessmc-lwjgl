package io.github.headlesshq.headlessmc.lwjgl3.probe;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Stream;

/**
 * Compares a recording of the real LWJGL with a recording of the patched jars, call by call.
 * <pre>
 * --real /path/to/recordings/name
 * --patched /path/to/patched/recordings
 * --report /path/to/report/directory
 * </pre>
 * Writes {@code <report>/<suite>.txt}: per method the calls that succeeded on the real implementation and how many of
 * them returned the same result with the patched jars.
 */
public final class Compare {
    private static final int EXAMPLES = 3;

    private Compare() {
        throw new AssertionError();
    }

    public static void main(String[] args) throws IOException {
        Path real = null;
        Path patched = null;
        Path report = null;
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--real" -> real = Path.of(args[++i]);
                case "--patched" -> patched = Path.of(args[++i]);
                case "--report" -> report = Path.of(args[++i]);
                default -> throw new IllegalArgumentException("Unknown argument " + args[i]);
            }
        }

        Files.createDirectories(report);
        try (Stream<Path> files = Files.list(real)) {
            for (Path file : files.filter(f -> f.toString().endsWith(".jsonl")).sorted().toList()) {
                Path patchedFile = patched.resolve(file.getFileName());
                if (!Files.exists(patchedFile)) {
                    System.err.println("No patched recording for " + file.getFileName());
                    continue;
                }

                String suite = file.getFileName().toString().replace(".jsonl", "");
                compare(suite, read(file), read(patchedFile), report.resolve(suite + ".txt"));
            }
        }
    }

    private static void compare(String suite, Map<String, Map<String, Object>> real, Map<String, Map<String, Object>> patched,
                                Path report) throws IOException {
        Map<String, MethodResult> methods = new TreeMap<>();
        for (Map.Entry<String, Map<String, Object>> entry : real.entrySet()) {
            Map<String, Object> record = entry.getValue();
            MethodResult result = methods.computeIfAbsent((String) record.get("m"), k -> new MethodResult());
            result.calls++;
            Map<String, Object> other = patched.get(entry.getKey());
            if (other == null) {
                result.missing++;
                result.example("missing " + record.get("a"));
            } else if (!same(record, other)) {
                result.different++;
                result.example(record.get("a") + " real " + outcome(record) + " patched " + outcome(other));
            }
        }

        int calls = 0;
        int equal = 0;
        int methodsEqual = 0;
        try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(report, StandardCharsets.UTF_8))) {
            for (Map.Entry<String, MethodResult> entry : methods.entrySet()) {
                MethodResult result = entry.getValue();
                calls += result.calls;
                equal += result.calls - result.different - result.missing;
                if (result.different == 0 && result.missing == 0) {
                    methodsEqual++;
                    continue;
                }

                writer.println(entry.getKey() + " calls=" + result.calls + " different=" + result.different + " missing=" + result.missing);
                for (String example : result.examples) {
                    writer.println("    " + example);
                }
            }
        }

        System.out.println(suite + ": " + methodsEqual + "/" + methods.size() + " methods and " + equal + "/" + calls
                               + " successful real calls return the same with the patched jars, see " + report);
    }

    private static boolean same(Map<String, Object> real, Map<String, Object> patched) {
        return Objects.equals(real.get("r"), patched.get("r")) && Objects.equals(real.get("out"), patched.get("out"));
    }

    private static String outcome(Map<String, Object> record) {
        Map<String, Object> outcome = new LinkedHashMap<>();
        if (record.containsKey("r")) {
            outcome.put("r", record.get("r"));
        }

        if (record.containsKey("out")) {
            outcome.put("out", record.get("out"));
        }

        return Json.write(outcome);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Map<String, Object>> read(Path file) throws IOException {
        Map<String, Map<String, Object>> result = new LinkedHashMap<>();
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                Map<String, Object> record = (Map<String, Object>) Json.read(line);
                if (!record.containsKey("type")) {
                    result.put(record.get("m") + " " + Json.write(record.get("a")), record);
                }
            }
        }

        return result;
    }

    private static final class MethodResult {
        private final List<String> examples = new ArrayList<>();
        private int calls;
        private int different;
        private int missing;

        private void example(String example) {
            if (examples.size() < EXAMPLES) {
                examples.add(example);
            }
        }
    }

}
