package io.github.headlesshq.headlessmc.lwjgl.patcher;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Command line entry point of the patcher.
 * <pre>
 * --input lwjgl-opengl=/path/to/lwjgl-opengl.jar (repeatable)
 * --runtime /path/to/runtime.jar (repeatable)
 * --output /path/to/output/directory
 * --lock /path/to/lock/directory
 * --lwjgl-version 3.4.3
 * --version 0.1.0
 * --compat 3.3.3:lwjgl-opengl=/path/to/lwjgl-opengl-3.3.3.jar (repeatable)
 * [--update-lock]
 * </pre>
 */
public final class Main {
    private Main() {
        throw new AssertionError();
    }

    public static void main(String[] args) throws Exception {
        Map<String, Path> inputs = new LinkedHashMap<>();
        List<Path> runtimes = new ArrayList<>();
        Path output = null;
        Path lock = null;
        String lwjglVersion = "unknown";
        String version = "unknown";
        boolean updateLock = false;
        List<Patcher.CompatInput> compat = new ArrayList<>();
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--input" -> {
                    String[] input = args[++i].split("=", 2);
                    inputs.put(input[0], Path.of(input[1]));
                }
                case "--runtime" -> runtimes.add(Path.of(args[++i]));
                case "--output" -> output = Path.of(args[++i]);
                case "--lock" -> lock = Path.of(args[++i]);
                case "--lwjgl-version" -> lwjglVersion = args[++i];
                case "--version" -> version = args[++i];
                case "--compat" -> {
                    String[] input = args[++i].split("=", 2);
                    String[] versionAndModule = input[0].split(":", 2);
                    compat.add(new Patcher.CompatInput(versionAndModule[0], versionAndModule[1], Path.of(input[1])));
                }
                case "--update-lock" -> updateLock = true;
                default -> throw new IllegalArgumentException("Unknown argument " + args[i]);
            }
        }

        if (inputs.isEmpty() || runtimes.isEmpty() || output == null || lock == null) {
            throw new IllegalArgumentException("--input, --runtime, --output and --lock are required");
        }

        List<String> errors = new Patcher(new Patcher.Options(inputs, runtimes, output, lock, updateLock, lwjglVersion, version, compat)).run();
        if (!errors.isEmpty()) {
            errors.forEach(System.err::println);
            System.err.println(errors.size() + " error(s)");
            System.exit(1);
        }
    }

}
