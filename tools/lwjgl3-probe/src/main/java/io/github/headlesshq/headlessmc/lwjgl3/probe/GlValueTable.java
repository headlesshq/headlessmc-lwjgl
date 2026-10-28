package io.github.headlesshq.headlessmc.lwjgl3.probe;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Generates the default values of the patched {@code glGet*} queries from a {@code glfw-opengl} recording.
 * <pre>
 * --recording /path/to/recordings/name/glfw-opengl.jsonl
 * --constants org.lwjgl.opengl (package whose constants name the parameters)
 * --output /path/to/gl-values.properties
 * </pre>
 * For every parameter, the most precise successful query wins: {@code glGetDoublev}, or {@code glGetInteger64v}
 * for values beyond the precision of a double, then {@code glGetFloatv}, {@code glGetIntegerv}. Mesa answers
 * {@code glGetInteger64v} for some legacy matrices with garbage, so doubles come first.
 * Driver-specific memory statistics are left out as well. Parameters that describe the state of the probe's own objects, like
 * bindings, are left out.
 */
public final class GlValueTable {
    private static final List<String> QUERIES = List.of(
        "org/lwjgl/opengl/GL11C.glGetDoublev(ILjava/nio/DoubleBuffer;)V",
        "org/lwjgl/opengl/GL32C.glGetInteger64v(ILjava/nio/LongBuffer;)V",
        "org/lwjgl/opengl/GL11C.glGetFloatv(ILjava/nio/FloatBuffer;)V",
        "org/lwjgl/opengl/GL11C.glGetIntegerv(ILjava/nio/IntBuffer;)V");
    private static final Pattern STATE = Pattern.compile(
        ".*(_BINDING|_BINDING_.*|_BOUND|CURRENT_PROGRAM|CURRENT_QUERY|TIMESTAMP|GPU_DISJOINT|ACTIVE_TEXTURE"
            + "|_ARRAY_POINTER|_VIEWPORT|_SCISSOR_BOX|_DRAW_BUFFER.*|_READ_BUFFER|DEBUG_.*_COUNT"
            + "|DEBUG_NEXT_LOGGED_MESSAGE_LENGTH|_FREE_MEMORY_ATI|GPU_MEMORY_INFO_.*)");

    private GlValueTable() {
        throw new AssertionError();
    }

    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws IOException {
        Path recording = null;
        Path output = null;
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--recording" -> recording = Path.of(args[++i]);
                case "--output" -> output = Path.of(args[++i]);
                default -> throw new IllegalArgumentException("Unknown argument " + args[i]);
            }
        }

        Map<String, String> header = Map.of();
        Map<String, Map<String, List<Object>>> byQuery = new TreeMap<>();
        try (BufferedReader reader = Files.newBufferedReader(recording, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                Map<String, Object> record = (Map<String, Object>) Json.read(line);
                if ("header".equals(record.get("type"))) {
                    header = new TreeMap<>();
                    for (Map.Entry<String, Object> entry : record.entrySet()) {
                        header.put(entry.getKey(), String.valueOf(entry.getValue()));
                    }
                } else if (!record.containsKey("type") && QUERIES.contains((String) record.get("m"))) {
                    List<Object> labels = (List<Object>) record.get("a");
                    List<Object> written = (List<Object>) ((Map<String, Object>) record.get("out")).get("1");
                    if (written != null && !written.isEmpty() && !written.contains(null)) {
                        byQuery.computeIfAbsent((String) record.get("m"), k -> new TreeMap<>()).put((String) labels.get(0), written);
                    }
                }
            }
        }

        Map<Integer, String> lines = new TreeMap<>();
        Map<Integer, String> names = new TreeMap<>();
        Map<String, Integer> values = Suite.intConstants(new GlSuite().enumClasses()).entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
        for (String query : QUERIES) {
            for (Map.Entry<String, List<Object>> entry : byQuery.getOrDefault(query, Map.of()).entrySet()) {
                String name = entry.getKey();
                Integer pname = values.get(name);
                if (pname == null || STATE.matcher(name).matches() || lines.containsKey(pname)
                    || query.equals(QUERIES.get(0)) && exceedsDouble(entry.getValue())) {
                    continue;
                }

                List<String> components = new ArrayList<>();
                for (Object value : entry.getValue()) {
                    components.add(format(value));
                }

                lines.put(pname, String.join(",", components));
                names.put(pname, name);
            }
        }

        Files.createDirectories(output.getParent());
        try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(output, StandardCharsets.UTF_8))) {
            writer.println("# glGet values recorded by tools/lwjgl3-probe, regenerate with ./gradlew :lwjgl3-probe:glValueTable");
            writer.println("# " + header.getOrDefault("GL_RENDERER", "?") + ", " + header.getOrDefault("GL_VERSION", "?")
                               + ", LWJGL " + header.getOrDefault("lwjgl", "?"));
            for (Map.Entry<Integer, String> entry : lines.entrySet()) {
                writer.println("# " + names.get(entry.getKey()));
                writer.println(String.format("0x%04X", entry.getKey()) + "=" + entry.getValue());
            }
        }

        System.out.println("Wrote " + lines.size() + " GL values to " + output);
    }

    private static boolean exceedsDouble(List<Object> values) {
        for (Object value : values) {
            if (value instanceof Double decimal && Math.abs(decimal) >= 0x1p53) {
                return true;
            }
        }

        return false;
    }

    private static String format(Object value) {
        if (value instanceof Double decimal && decimal == Math.rint(decimal) && Math.abs(decimal) < 1e15) {
            return Long.toString(decimal.longValue());
        }

        return String.valueOf(value);
    }

}
