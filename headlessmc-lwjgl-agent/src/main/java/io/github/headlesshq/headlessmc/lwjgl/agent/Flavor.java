package io.github.headlesshq.headlessmc.lwjgl.agent;

import java.util.Locale;

/**
 * The patched LWJGL flavours bundled in the agent jar.
 */
enum Flavor {
    LWJGL3("lwjgl3"),
    LWJGL2("lwjgl2");

    private final String directory;

    Flavor(String directory) {
        this.directory = directory;
    }

    /**
     * @return the directory of the patched jars of this flavour in the agent jar, also its name in agent arguments.
     */
    String directory() {
        return directory;
    }

    /**
     * @param name {@code lwjgl3} or {@code lwjgl2}, case-insensitive.
     * @return the flavour.
     * @throws IllegalArgumentException if there is no such flavour.
     */
    static Flavor of(String name) {
        for (Flavor flavor : values()) {
            if (flavor.directory.equals(name.trim().toLowerCase(Locale.ENGLISH))) {
                return flavor;
            }
        }

        throw new IllegalArgumentException("Unknown LWJGL flavour '" + name + "', expected lwjgl3 or lwjgl2");
    }

}
