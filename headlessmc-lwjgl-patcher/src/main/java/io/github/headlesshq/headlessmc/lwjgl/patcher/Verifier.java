package io.github.headlesshq.headlessmc.lwjgl.patcher;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Checks patched classes for anything that could load or call native code.
 */
public final class Verifier {
    private static final Map<String, Set<String>> FORBIDDEN = Map.of(
        "java/lang/System", Set.of("load", "loadLibrary"),
        "java/lang/Runtime", Set.of("load", "loadLibrary"),
        "java/lang/foreign/SymbolLookup", Set.of("libraryLookup", "loaderLookup"),
        "java/lang/foreign/Linker", Set.of("nativeLinker")
    );

    private final List<String> errors = new ArrayList<>();

    /**
     * @param path the path of the class in its jar, for error messages.
     * @param bytes the patched class.
     */
    public void verify(String path, byte[] bytes) {
        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, ClassReader.SKIP_FRAMES);
        for (MethodNode method : node.methods) {
            String location = path + " " + method.name + method.desc;
            if ((method.access & Opcodes.ACC_NATIVE) != 0) {
                errors.add(location + " is still native");
            }

            for (AbstractInsnNode insn : method.instructions) {
                if (insn instanceof MethodInsnNode call) {
                    Set<String> names = FORBIDDEN.get(call.owner);
                    if (names != null && names.contains(call.name)) {
                        errors.add(location + " calls " + call.owner + "." + call.name);
                    }
                }
            }
        }
    }

    public List<String> errors() {
        return errors;
    }

}
