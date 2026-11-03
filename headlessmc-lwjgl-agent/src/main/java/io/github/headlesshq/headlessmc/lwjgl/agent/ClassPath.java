package io.github.headlesshq.headlessmc.lwjgl.agent;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A classpath, as a list of entries.
 */
final class ClassPath {
    private ClassPath() {
        throw new AssertionError();
    }

    static List<String> split(String classPath) {
        List<String> result = new ArrayList<String>();
        if (classPath == null) {
            return result;
        }

        for (String entry : classPath.split(File.pathSeparator)) {
            if (!entry.isEmpty()) {
                result.add(entry);
            }
        }

        return result;
    }

    static String join(List<String> entries) {
        StringBuilder builder = new StringBuilder();
        for (String entry : entries) {
            if (builder.length() > 0) {
                builder.append(File.pathSeparatorChar);
            }

            builder.append(entry);
        }

        return builder.toString();
    }

    /**
     * @param entries classpath entries.
     * @return the flavours of the LWJGL jars among the entries, with the number of jars.
     */
    static Map<Flavor, Integer> flavors(List<String> entries) {
        Map<Flavor, Integer> result = new EnumMap<Flavor, Integer>(Flavor.class);
        for (String entry : entries) {
            LwjglJar jar = LwjglJar.of(entry);
            if (jar != null) {
                Integer count = result.get(jar.flavor());
                result.put(jar.flavor(), count == null ? 1 : count + 1);
            }
        }

        return result;
    }

    /**
     * Replaces the LWJGL jars of the flavour with the patched jars and adds the API jar, like HeadlessMc does.
     * Jars of the flavour the agent has no patched jar for stay, with a warning.
     *
     * @param entries the entries of the classpath.
     * @param jars the patched jars.
     * @param flavor the flavour.
     * @return the entries of the patched classpath.
     */
    static List<String> patch(List<String> entries, PatchedJars jars, Flavor flavor) {
        Set<String> result = new LinkedHashSet<String>();
        for (String entry : entries) {
            LwjglJar lwjgl = LwjglJar.of(entry);
            if (lwjgl == null || lwjgl.flavor() != flavor) {
                result.add(entry);
                continue;
            }

            Path patched = jars.get(lwjgl.patchedJar());
            if (patched == null) {
                Agent.warn("No patched LWJGL for " + entry + ", it might load native libraries");
                result.add(entry);
            } else {
                Agent.debug("Replacing " + entry + " with " + patched);
                result.add(patched.toString());
            }
        }

        result.add(jars.api().toString());
        return Collections.unmodifiableList(new ArrayList<String>(result));
    }

}
