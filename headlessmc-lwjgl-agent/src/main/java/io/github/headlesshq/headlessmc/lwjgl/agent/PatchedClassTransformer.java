package io.github.headlesshq.headlessmc.lwjgl.agent;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.ClassFileTransformer;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Replaces every LWJGL class the application loads from the original LWJGL jars with the class of the patched jars.
 * <p>The system class loader reads its classpath before the agent runs, so the original jars stay on it. The agent
 * appends the patched jars to it, which supplies the classes the original jars do not have, like the runtime of the
 * patched LWJGL or modules of newer LWJGL versions, and this transformer swaps the bytes of the classes the original
 * jars do have. Every class of LWJGL is then the patched one, defined by the class loader of the original jars.</p>
 * <p>The patched jars are multi-release jars, the class loader of this transformer picks the classes of the running
 * Java version, like the class loader of the application does.</p>
 */
final class PatchedClassTransformer implements ClassFileTransformer {
    private static final String[] PACKAGES = {
        "org/lwjgl/",
        "io/github/headlesshq/headlessmc/lwjgl/runtime/",
        "io/github/headlesshq/headlessmc/lwjgl2/runtime/",
        "io/github/headlesshq/headlessmc/lwjgl3/runtime/",
    };

    private final Map<String, Boolean> patchedLocations = new ConcurrentHashMap<String, Boolean>();
    private final URLClassLoader patched;
    private final String directory;

    /**
     * @param jars the patched jars.
     * @param directory the directory of the patched jars, classes loaded from there are patched already.
     */
    PatchedClassTransformer(List<Path> jars, Path directory) throws MalformedURLException {
        URL[] urls = new URL[jars.size()];
        for (int i = 0; i < urls.length; i++) {
            urls[i] = jars.get(i).toUri().toURL();
        }

        // no parent, only the patched jars
        this.patched = new URLClassLoader(urls, null);
        this.directory = canonical(directory.toFile()) + File.separator;
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain, byte[] classfileBuffer) {
        if (className == null || classBeingRedefined != null || !isPatched(className) || isFromPatchedJar(protectionDomain)) {
            return null;
        }

        try {
            byte[] bytes = read(className + ".class");
            // proxies of package-private LWJGL interfaces are generated, they have no code source
            URL location = location(protectionDomain);
            if (bytes == null && location != null && className.startsWith("org/lwjgl/")) {
                Agent.warn("No patched class for " + className + " from " + location
                    + ", it might load native libraries");
            }

            return bytes;
        } catch (Throwable t) {
            Agent.warn("Failed to read the patched class " + className + ": " + t);
            return null;
        }
    }

    static boolean isPatched(String className) {
        for (String prefix : PACKAGES) {
            if (className.startsWith(prefix)) {
                return true;
            }
        }

        return false;
    }

    private boolean isFromPatchedJar(ProtectionDomain protectionDomain) {
        URL location = location(protectionDomain);
        if (location == null || !"file".equals(location.getProtocol())) {
            return false;
        }

        String key = location.toString();
        Boolean result = patchedLocations.get(key);
        if (result == null) {
            try {
                result = canonical(new File(location.toURI())).startsWith(directory);
            } catch (Exception e) {
                result = false;
            }

            patchedLocations.put(key, result);
        }

        return result;
    }

    private static String canonical(File file) {
        try {
            return file.getCanonicalPath();
        } catch (IOException e) {
            return file.getAbsolutePath();
        }
    }

    private static URL location(ProtectionDomain protectionDomain) {
        CodeSource codeSource = protectionDomain == null ? null : protectionDomain.getCodeSource();
        return codeSource == null ? null : codeSource.getLocation();
    }

    /**
     * @param resource the name of a resource of the patched jars.
     * @return its content, or {@code null} if the patched jars do not have it.
     */
    byte[] read(String resource) throws IOException {
        InputStream in = patched.getResourceAsStream(resource);
        if (in == null) {
            return null;
        }

        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream(Math.max(in.available(), 4096));
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }

            return out.toByteArray();
        } finally {
            in.close();
        }
    }

}
