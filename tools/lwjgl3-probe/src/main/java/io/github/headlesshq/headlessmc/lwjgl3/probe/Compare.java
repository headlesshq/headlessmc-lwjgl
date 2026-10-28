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
import java.util.Set;
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
            for (Path file : files.filter(f -> f.toString().endsWith(".jsonl.gz")).sorted().toList()) {
                Path patchedFile = patched.resolve(file.getFileName());
                if (!Files.exists(patchedFile)) {
                    System.err.println("No patched recording for " + file.getFileName());
                    continue;
                }

                String suite = file.getFileName().toString().replace(".jsonl.gz", "");
                Map<String, Map<String, Object>> realRecords = read(file, null);
                compare(suite, realRecords, read(patchedFile, realRecords.keySet()), report.resolve(suite + ".txt"));
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
    /**
     * @param keys the keys to keep, or {@code null} for all; the patched recordings contain every call and are only
     *             needed for the calls that succeeded on the real implementation.
     */
    private static Map<String, Map<String, Object>> read(Path file, Set<String> keys) throws IOException {
        Map<String, Map<String, Object>> result = new LinkedHashMap<>();
        try (BufferedReader reader = Json.open(file)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("{\"type\"")) {
                    continue;
                }

                int arguments = line.indexOf(",\"r\":");
                if (arguments < 0) {
                    arguments = line.indexOf(",\"out\":");
                }

                if (keys != null && arguments >= 0 && !keys.contains(key((Map<String, Object>) Json.read(line.substring(0, arguments) + "}")))) {
                    continue;
                }

                Map<String, Object> record = (Map<String, Object>) Json.read(line);
                String key = key(record);
                if (keys == null || keys.contains(key)) {
                    result.put(key, record);
                }
            }
        }

        return result;
    }

    private static String key(Map<String, Object> record) {
        return record.get("m") + " " + Json.write(record.get("a"));
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
