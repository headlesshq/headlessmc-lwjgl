package io.github.headlesshq.headlessmc.lwjgl3.patcher;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.ModuleExportNode;
import org.objectweb.asm.tree.ModuleNode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Adapts a module descriptor to the patched jar: adds and exports the packages of the embedded runtime and removes
 * every package that no longer exists, e.g. packages that only existed in dropped multi-release classes.
 */
public final class ModuleInfoPatcher {
    private static final Pattern VERSIONED = Pattern.compile("META-INF/versions/(\\d+)/module-info\\.class");

    private ModuleInfoPatcher() {
        throw new AssertionError();
    }

    /**
     * Since all multi-release class overrides are dropped, only the descriptor matching the base classes is kept:
     * the one in the jar root or, if there is none, the one of the lowest version.
     *
     * @param moduleInfos the module descriptors of a jar, keyed by path.
     * @return the path of the descriptor to keep, or {@code null} if the jar is not modular.
     */
    public static String select(Map<String, byte[]> moduleInfos) {
        String selected = null;
        int selectedVersion = Integer.MAX_VALUE;
        for (String path : moduleInfos.keySet()) {
            if ("module-info.class".equals(path)) {
                return path;
            }

            Matcher matcher = VERSIONED.matcher(path);
            if (matcher.matches() && Integer.parseInt(matcher.group(1)) < selectedVersion) {
                selectedVersion = Integer.parseInt(matcher.group(1));
                selected = path;
            }
        }

        return selected;
    }

    /**
     * @param bytes the original {@code module-info.class}.
     * @param runtimePackages the internal names of the runtime packages embedded into the module.
     * @param existingPackages the internal names of all packages in the patched jar.
     * @return the patched {@code module-info.class}.
     */
    public static byte[] patch(byte[] bytes, Collection<String> runtimePackages, Set<String> existingPackages) {
        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, 0);
        ModuleNode module = node.module;
        if (module.exports == null) {
            module.exports = new ArrayList<>();
        }

        module.exports.removeIf(export -> !existingPackages.contains(export.packaze));
        if (module.opens != null) {
            module.opens.removeIf(open -> !existingPackages.contains(open.packaze));
        }

        if (module.packages != null) {
            module.packages.removeIf(pkg -> !existingPackages.contains(pkg));
        }

        for (String pkg : runtimePackages) {
            if (module.packages != null && !module.packages.contains(pkg)) {
                module.packages.add(pkg);
            }

            if (module.exports.stream().noneMatch(export -> export.packaze.equals(pkg))) {
                module.exports.add(new ModuleExportNode(pkg, 0, null));
            }
        }

        ClassWriter writer = new ClassWriter(0);
        node.accept(writer);
        return writer.toByteArray();
    }

}
