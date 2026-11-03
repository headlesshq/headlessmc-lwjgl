package io.github.headlesshq.headlessmc.lwjgl.agent;

import java.io.File;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An LWJGL classes jar on the classpath of the application, e.g.
 * {@code libraries/org/lwjgl/lwjgl-glfw/3.3.3/lwjgl-glfw-3.3.3.jar}, recognized by its path, like HeadlessMc does.
 * Jars of native libraries, like {@code lwjgl-glfw-3.3.3-natives-linux.jar}, are not LWJGL classes jars, they are
 * never loaded by the patched LWJGL and stay on the classpath.
 */
final class LwjglJar {
    /**
     * Fallback for jars outside of a maven layout: the artifact, the version and an optional classifier.
     */
    private static final Pattern FILE_NAME = Pattern.compile(
        "(lwjgl(?:-[a-z0-9]+)?|lwjgl_util)-([23]\\.\\d+(?:\\.\\d+)*(?:-nightly-\\d+)?)(-[a-z0-9-]+)?\\.jar");

    private final Flavor flavor;
    private final String artifact;

    private LwjglJar(Flavor flavor, String artifact) {
        this.flavor = flavor;
        this.artifact = artifact;
    }

    /**
     * @return the flavour of the patched LWJGL that replaces this jar.
     */
    Flavor flavor() {
        return flavor;
    }

    /**
     * @return the LWJGL artifact, e.g. {@code lwjgl-glfw} or {@code lwjgl_util}.
     */
    String artifact() {
        return artifact;
    }

    /**
     * @return the path of the patched jar replacing this one inside the agent jar, relative to
     * {@link PatchedJars#RESOURCES}, e.g. {@code lwjgl3/lwjgl-glfw.jar}.
     */
    String patchedJar() {
        return flavor.directory() + "/" + artifact + ".jar";
    }

    /**
     * @param entry an entry of the classpath.
     * @return the LWJGL jar, or {@code null} if the entry is not an LWJGL classes jar.
     */
    static LwjglJar of(String entry) {
        File file = new File(entry);
        String fileName = file.getName();
        if (!fileName.endsWith(".jar")) {
            return null;
        }

        File versionDirectory = file.getParentFile();
        File artifactDirectory = versionDirectory == null ? null : versionDirectory.getParentFile();
        File groupDirectory = artifactDirectory == null ? null : artifactDirectory.getParentFile();
        if (groupDirectory != null && "lwjgl".equals(groupDirectory.getName())) {
            String version = versionDirectory.getName();
            String artifact = artifactDirectory.getName().toLowerCase(Locale.ENGLISH);
            String prefix = artifact + "-" + version;
            if (fileName.startsWith(prefix)) {
                return of(artifact, version, fileName.substring(prefix.length(), fileName.length() - ".jar".length()));
            }
        }

        Matcher matcher = FILE_NAME.matcher(fileName.toLowerCase(Locale.ENGLISH));
        if (matcher.matches()) {
            String classifier = matcher.group(3);
            return of(matcher.group(1), matcher.group(2), classifier == null ? "" : classifier);
        }

        return null;
    }

    private static LwjglJar of(String artifact, String version, String classifier) {
        // lwjgl-3.4.1-unsafe.jar replaces the core jar in Minecraft 26.2, lwjgl-3.3.3-natives-linux.jar is no classes jar
        if (!classifier.isEmpty() && (!classifier.startsWith("-") || classifier.length() == 1 || classifier.startsWith("-natives"))) {
            return null;
        }

        if (version.startsWith("3.") && (artifact.equals("lwjgl") || artifact.startsWith("lwjgl-"))) {
            return new LwjglJar(Flavor.LWJGL3, artifact);
        } else if (version.startsWith("2.") && (artifact.equals("lwjgl") || artifact.equals("lwjgl_util"))) {
            return new LwjglJar(Flavor.LWJGL2, artifact);
        }

        return null;
    }

    @Override
    public String toString() {
        return flavor.directory() + ":" + artifact;
    }

}
