package io.github.headlesshq.headlessmc.lwjgl3.runtime.opengl;

import io.github.headlesshq.headlessmc.lwjgl3.runtime.Config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The OpenGL implementation the patched LWJGL pretends to be.
 * <ul>
 *     <li>{@code hmc.lwjgl3.gl.version}: the OpenGL version, default {@code 4.6}.</li>
 *     <li>{@code hmc.lwjgl3.gl.extensions}: comma separated extensions to report, {@code *} for all, default none.</li>
 *     <li>{@code hmc.lwjgl3.gl.vendor}, {@code hmc.lwjgl3.gl.renderer}: the vendor and renderer strings.</li>
 * </ul>
 */
public final class GlInfo {
    private static final Pattern VERSION = Pattern.compile("(\\d+)\\.(\\d+)");
    private static final int MAJOR;
    private static final int MINOR;
    private static final List<String> EXTENSIONS;
    private static final boolean ALL_EXTENSIONS;

    static {
        Matcher matcher = VERSION.matcher(Config.getString("gl.version", "4.6"));
        if (matcher.find()) {
            MAJOR = Integer.parseInt(matcher.group(1));
            MINOR = Integer.parseInt(matcher.group(2));
        } else {
            MAJOR = 4;
            MINOR = 6;
        }

        String extensions = Config.getString("gl.extensions", "").trim();
        ALL_EXTENSIONS = "*".equals(extensions);
        List<String> list = new ArrayList<>();
        if (!ALL_EXTENSIONS) {
            for (String extension : extensions.split(",")) {
                if (!extension.trim().isEmpty()) {
                    list.add(extension.trim());
                }
            }
        }

        EXTENSIONS = Collections.unmodifiableList(list);
    }

    private GlInfo() {
        throw new AssertionError();
    }

    public static int majorVersion() {
        return MAJOR;
    }

    public static int minorVersion() {
        return MINOR;
    }

    public static String version() {
        return MAJOR + "." + MINOR + ".0 HeadlessMc";
    }

    public static String shadingLanguageVersion() {
        return Config.getString("gl.shadingLanguageVersion", MAJOR >= 4 ? MAJOR + "." + MINOR + "0" : "3.30");
    }

    public static String vendor() {
        return Config.getString("gl.vendor", "HeadlessMc");
    }

    public static String renderer() {
        return Config.getString("gl.renderer", "HeadlessMc LWJGL3");
    }

    public static List<String> extensions() {
        return EXTENSIONS;
    }

    /**
     * Decides whether a {@code boolean} field of {@code GLCapabilities} is supported.
     *
     * @param field the name of the field, e.g. {@code OpenGL33} or {@code GL_ARB_debug_output}.
     * @return {@code true} if the field should be set.
     */
    public static boolean isSupported(String field) {
        if (field.startsWith("OpenGL") && field.length() == 8) {
            int major = field.charAt(6) - '0';
            int minor = field.charAt(7) - '0';
            return major < MAJOR || major == MAJOR && minor <= MINOR;
        }

        if (field.startsWith("forwardCompatible")) {
            return false;
        }

        return ALL_EXTENSIONS || EXTENSIONS.contains(field);
    }

}
