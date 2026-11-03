package io.github.headlesshq.headlessmc.lwjgl.agent;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.CRC32;

/**
 * The patched jars bundled in the agent jar, extracted into a directory, because class loaders and module layers can
 * only load jars from files. The directory name contains a checksum of the bundled jars, so that agents of different
 * builds never share a directory and an existing directory never needs to be rewritten, which would fail on Windows
 * while another JVM uses the jars.
 */
final class PatchedJars {
    static final String RESOURCES = "headlessmc-lwjgl/";
    static final String API = "headlessmc-lwjgl-api.jar";

    private final Path directory;
    private final Map<String, Path> jars;

    private PatchedJars(Path directory, Map<String, Path> jars) {
        this.directory = directory;
        this.jars = jars;
    }

    /**
     * @return the directory the jars were extracted to.
     */
    Path directory() {
        return directory;
    }

    /**
     * @param path the path of a jar relative to {@link #RESOURCES}, e.g. {@code lwjgl3/lwjgl-glfw.jar}.
     * @return the extracted jar, or {@code null} if the agent does not bundle it.
     */
    Path get(String path) {
        return jars.get(path);
    }

    /**
     * @return the extracted API jar.
     */
    Path api() {
        return jars.get(API);
    }

    /**
     * @param flavor the flavour.
     * @return every extracted patched LWJGL jar of the flavour.
     */
    List<Path> of(Flavor flavor) {
        List<Path> result = new ArrayList<Path>();
        String prefix = flavor.directory() + "/";
        for (Map.Entry<String, Path> entry : jars.entrySet()) {
            if (entry.getKey().startsWith(prefix)) {
                result.add(entry.getValue());
            }
        }

        return result;
    }

    /**
     * Extracts the jars of the flavour and the API jar.
     *
     * @param agentJar the agent jar.
     * @param baseDirectory the directory to create the extraction directory in.
     * @param version the version of the agent, part of the directory name.
     * @param flavor the flavour whose jars to extract.
     * @return the extracted jars.
     * @throws IOException if the jars cannot be extracted.
     */
    static PatchedJars extract(File agentJar, Path baseDirectory, String version, Flavor flavor) throws IOException {
        JarFile jar = new JarFile(agentJar, false);
        try {
            List<JarEntry> entries = new ArrayList<JarEntry>();
            CRC32 checksum = new CRC32();
            Enumeration<JarEntry> enumeration = jar.entries();
            while (enumeration.hasMoreElements()) {
                JarEntry entry = enumeration.nextElement();
                String name = entry.getName();
                if (entry.isDirectory() || !name.startsWith(RESOURCES) || !name.endsWith(".jar")) {
                    continue;
                }

                String path = name.substring(RESOURCES.length());
                if (path.equals(API) || path.startsWith(flavor.directory() + "/")) {
                    entries.add(entry);
                    checksum.update(name.getBytes("UTF-8"));
                    checksum.update(longToBytes(entry.getCrc()));
                    checksum.update(longToBytes(entry.getSize()));
                }
            }

            if (entries.isEmpty()) {
                throw new IOException(agentJar + " does not contain the patched " + flavor.directory() + " jars");
            }

            Path directory = baseDirectory.resolve(version + "-" + flavor.directory() + "-" + Long.toHexString(checksum.getValue()));
            Map<String, Path> jars = new LinkedHashMap<String, Path>();
            for (JarEntry entry : entries) {
                String path = entry.getName().substring(RESOURCES.length());
                Path target = directory.resolve(path);
                if (!Files.isRegularFile(target) || Files.size(target) != entry.getSize()) {
                    copy(jar, entry, target);
                }

                jars.put(path, target);
            }

            return new PatchedJars(directory, jars);
        } finally {
            jar.close();
        }
    }

    private static void copy(JarFile jar, JarEntry entry, Path target) throws IOException {
        Files.createDirectories(target.getParent());
        Path temporary = Files.createTempFile(target.getParent(), target.getFileName().toString(), ".tmp");
        try {
            InputStream in = jar.getInputStream(entry);
            try {
                Files.copy(in, temporary, StandardCopyOption.REPLACE_EXISTING);
            } finally {
                in.close();
            }

            try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporary, target);
            }
        } catch (FileAlreadyExistsException e) {
            // another JVM extracted the same jar concurrently
        } catch (IOException e) {
            // e.g. a partial jar of a crashed JVM, which another JVM is using already on Windows
            if (!Files.isRegularFile(target) || Files.size(target) != entry.getSize()) {
                throw e;
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static byte[] longToBytes(long value) {
        byte[] bytes = new byte[8];
        for (int i = 0; i < 8; i++) {
            bytes[i] = (byte) (value >>> (i * 8));
        }

        return bytes;
    }

}
