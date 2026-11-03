package io.github.headlesshq.headlessmc.lwjgl.agent;

import java.io.File;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.jar.JarFile;

/**
 * A Java agent that makes the LWJGL of an application headless without HeadlessMc, e.g.
 * {@code java -javaagent:headlessmc-lwjgl-agent.jar -cp ... net.minecraft.client.main.Main}.
 * <p>It does what HeadlessMc does when it launches a game: it replaces the LWJGL jars on the classpath with the patched
 * jars and adds the API jar. The JVM has read the classpath when the agent starts, so the agent</p>
 * <ul>
 *     <li>appends the patched jars and the API jar to the system class loader and swaps the bytes of every LWJGL class
 *     loaded from the original jars with the patched class, see {@link PatchedClassTransformer},</li>
 *     <li>rewrites the classpath properties that launchers read to build their own class loaders or module layers,
 *     {@code java.class.path}, and {@code legacyClassPath} and {@code legacyClassPath.file} of the BootstrapLauncher of
 *     Forge and NeoForge,</li>
 *     <li>sets {@code joml.nounsafe}, because JOML would write with Unsafe at the address of the heap buffers the
 *     patched LWJGL hands out.</li>
 * </ul>
 * <p>The flavour, {@code lwjgl3} or {@code lwjgl2}, is detected from the LWJGL jars on the classpath, or given as agent
 * argument, {@code -javaagent:headlessmc-lwjgl-agent.jar=lwjgl3}, or as {@code -Dhmc.lwjgl.agent.flavor=lwjgl3}.
 * The jars are extracted to {@code hmc.lwjgl.agent.directory}, by default {@code headlessmc-lwjgl-agent} in the
 * temporary directory. {@code -Dhmc.lwjgl.agent.debug=true} logs every replaced jar.</p>
 */
public final class Agent {
    static final String PREFIX = "hmc.lwjgl.agent.";
    static final String FLAVOR = PREFIX + "flavor";
    static final String DIRECTORY = PREFIX + "directory";
    static final String DEBUG = PREFIX + "debug";
    static final String JOML_NO_UNSAFE = "joml.nounsafe";
    static final String CLASS_PATH = "java.class.path";
    static final String LEGACY_CLASS_PATH = "legacyClassPath";
    static final String LEGACY_CLASS_PATH_FILE = "legacyClassPath.file";
    private static final String LOG_PREFIX = "[HeadlessMc-LWJGL-Agent] ";
    private static final Charset UTF_8 = Charset.forName("UTF-8");

    private Agent() {
        throw new AssertionError();
    }

    public static void premain(String arguments, Instrumentation instrumentation) {
        try {
            install(arguments, instrumentation);
        } catch (Throwable t) {
            // a failed agent would let the application crash on the original LWJGL later, fail early and clearly
            throw new IllegalStateException(LOG_PREFIX + "Failed to install the patched LWJGL", t);
        }
    }

    static void install(String arguments, Instrumentation instrumentation) throws IOException {
        List<String> classPath = ClassPath.split(System.getProperty(CLASS_PATH));
        Flavor flavor = flavor(arguments, classPath);
        if (flavor == null) {
            warn("No LWJGL jars found on the classpath, pass the flavour as -javaagent:<agent jar>=lwjgl3 or =lwjgl2");
            return;
        }

        File agentJar = agentJar();
        String version = Agent.class.getPackage().getImplementationVersion();
        PatchedJars jars = PatchedJars.extract(agentJar, directory(), version == null ? "dev" : version, flavor);
        if (System.getProperty(JOML_NO_UNSAFE) == null) {
            System.setProperty(JOML_NO_UNSAFE, "true");
        }

        List<Path> patched = jars.of(flavor);
        instrumentation.addTransformer(new PatchedClassTransformer(patched, jars.directory()));
        for (Path jar : patched) {
            instrumentation.appendToSystemClassLoaderSearch(new JarFile(jar.toFile()));
        }

        instrumentation.appendToSystemClassLoaderSearch(new JarFile(jars.api().toFile()));
        System.setProperty(CLASS_PATH, ClassPath.join(ClassPath.patch(classPath, jars, flavor)));
        String legacyClassPath = System.getProperty(LEGACY_CLASS_PATH);
        if (legacyClassPath != null) {
            System.setProperty(LEGACY_CLASS_PATH, ClassPath.join(ClassPath.patch(ClassPath.split(legacyClassPath), jars, flavor)));
        }

        String legacyClassPathFile = System.getProperty(LEGACY_CLASS_PATH_FILE);
        if (legacyClassPathFile != null && Files.isRegularFile(Paths.get(legacyClassPathFile))) {
            List<String> entries = ClassPath.patch(Files.readAllLines(Paths.get(legacyClassPathFile), UTF_8), jars, flavor);
            Path file = Files.createTempFile(jars.directory(), "legacyClassPath", ".txt");
            file.toFile().deleteOnExit();
            Files.write(file, entries, UTF_8);
            System.setProperty(LEGACY_CLASS_PATH_FILE, file.toString());
        }

        log("Installed the patched " + flavor.directory() + " from " + jars.directory());
    }

    private static Flavor flavor(String arguments, List<String> classPath) {
        String explicit = arguments != null && !arguments.trim().isEmpty() ? arguments : System.getProperty(FLAVOR);
        if (explicit != null) {
            return Flavor.of(explicit);
        }

        Map<Flavor, Integer> flavors = ClassPath.flavors(classPath);
        if (flavors.size() > 1) {
            warn("Found LWJGL 2 and LWJGL 3 jars on the classpath, patching LWJGL 3, pass =lwjgl2 to patch LWJGL 2 instead");
            return Flavor.LWJGL3;
        }

        return flavors.isEmpty() ? null : flavors.keySet().iterator().next();
    }

    private static Path directory() {
        String directory = System.getProperty(DIRECTORY);
        if (directory != null) {
            return Paths.get(directory);
        }

        return Paths.get(System.getProperty("java.io.tmpdir"), "headlessmc-lwjgl-agent");
    }

    private static File agentJar() throws IOException {
        try {
            return new File(Agent.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        } catch (Exception e) {
            throw new IOException("Failed to find the agent jar", e);
        }
    }

    static void log(String message) {
        System.err.println(LOG_PREFIX + message);
    }

    static void warn(String message) {
        System.err.println(LOG_PREFIX + message);
    }

    static void debug(String message) {
        if (Boolean.getBoolean(DEBUG)) {
            System.err.println(LOG_PREFIX + message);
        }
    }

}
