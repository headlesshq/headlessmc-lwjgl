package io.github.headlesshq.headlessmc.lwjgl.patcher;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.ModuleExportNode;
import org.objectweb.asm.tree.ModuleNode;
import org.objectweb.asm.tree.ModuleRequireNode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Adapts a module descriptor to the patched jar: adds and exports the packages of the embedded runtime and of re-added
 * classes, and removes
 * every package that no longer exists, e.g. packages that only existed in dropped multi-release classes.
 * The module is made {@code open}, so that launchers, mods and HeadlessMc can reflect into it like into a jar on the
 * classpath.
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
     * @param bytes a {@code module-info.class}.
     * @return the name of the module.
     */
    public static String name(byte[] bytes) {
        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, 0);
        return node.module.name;
    }

    /**
     * @param bytes the original {@code module-info.class}.
     * @param exportedPackages the internal names of the packages that have to be exported, e.g. the runtime packages
     *                         embedded into the module.
     * @param existingPackages the internal names of all packages in the patched jar.
     * @param staticRequires the modules to add as {@code requires static}, e.g. the input API, unless already required.
     * @return the patched, open {@code module-info.class}.
     */
    public static byte[] patch(byte[] bytes, Collection<String> exportedPackages, Set<String> existingPackages,
                               Collection<String> staticRequires) {
        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, 0);
        ModuleNode module = node.module;
        if (module.exports == null) {
            module.exports = new ArrayList<>();
        }

        module.exports.removeIf(export -> !existingPackages.contains(export.packaze));
        module.access |= Opcodes.ACC_OPEN;
        module.opens = null;

        if (module.packages != null) {
            module.packages.removeIf(pkg -> !existingPackages.contains(pkg));
        }

        for (String pkg : exportedPackages) {
            if (module.packages != null && !module.packages.contains(pkg)) {
                module.packages.add(pkg);
            }

            if (module.exports.stream().noneMatch(export -> export.packaze.equals(pkg))) {
                module.exports.add(new ModuleExportNode(pkg, 0, null));
            }
        }

        if (module.requires == null) {
            module.requires = new ArrayList<>();
        }

        for (String required : staticRequires) {
            if (module.requires.stream().noneMatch(existing -> existing.module.equals(required))) {
                module.requires.add(new ModuleRequireNode(required, Opcodes.ACC_STATIC_PHASE, null));
            }
        }

        ClassWriter writer = new ClassWriter(0);
        node.accept(writer);
        return writer.toByteArray();
    }

}
