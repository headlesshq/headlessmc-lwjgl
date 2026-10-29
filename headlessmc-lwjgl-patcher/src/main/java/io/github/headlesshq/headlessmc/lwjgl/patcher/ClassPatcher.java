package io.github.headlesshq.headlessmc.lwjgl.patcher;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;

import java.util.ArrayList;
import java.util.List;

import static org.objectweb.asm.Opcodes.*;

/**
 * Patches a single class: replaces method bodies according to the {@link Catalog}, guards the static initializer and
 * adds the {@link Synthetics}.
 */
public final class ClassPatcher {
    public static final String ORIGINAL_STATIC_INIT = "hmc$clinit";
    private static final String STATIC_INIT_FAILED_DESC = "(Ljava/lang/Throwable;Ljava/lang/String;)V";

    private final ClassPool pool;
    private final Catalog catalog;
    private final Synthetics synthetics;

    public ClassPatcher(ClassPool pool, Catalog catalog, Synthetics synthetics) {
        this.pool = pool;
        this.catalog = catalog;
        this.synthetics = synthetics;
    }

    /**
     * @param module the module of the class.
     * @param node the class, modified in place.
     * @return the bytes of the patched class.
     */
    public byte[] patch(String module, ClassNode node) {
        boolean isInterface = (node.access & ACC_INTERFACE) != 0;
        List<MethodNode> methods = new ArrayList<>(node.methods);
        for (MethodNode method : methods) {
            MethodInfo info = new MethodInfo(node, method);
            if ("<clinit>".equals(method.name)) {
                if (!isInterface) {
                    guardStaticInit(node, method);
                }

                catalog.record(module, info, isInterface ? "keep-interface-static-init" : Catalog.STATIC_INIT_RULE, null);
                continue;
            }

            Behaviour behaviour = catalog.resolve(module, info);
            if (!(behaviour instanceof Behaviour.Keep)) {
                replace(method, behaviour.emit(info));
            }
        }

        for (FieldNode field : node.fields) {
            boolean isStatic = (field.access & ACC_STATIC) != 0;
            if (!isInterface && (!isStatic || field.value == null)) {
                field.access &= ~ACC_FINAL;
            }
        }

        synthetics.apply(node);
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS) {
            @Override
            protected String getCommonSuperClass(String type1, String type2) {
                return pool.commonSuperClass(type1, type2);
            }
        };

        node.accept(writer);
        return writer.toByteArray();
    }

    /**
     * Replaces the body of a method. The local variable entries of {@code this} and the parameters are kept, spanning
     * the new body, so that parameter names stay available for debugging and tools.
     */
    private static void replace(MethodNode method, InsnList instructions) {
        List<LocalVariableNode> parameters = new ArrayList<>();
        if (method.localVariables != null) {
            int parameterSlots = Type.getArgumentsAndReturnSizes(method.desc) >> 2;
            if ((method.access & ACC_STATIC) != 0) {
                parameterSlots--;
            }

            LabelNode start = new LabelNode();
            LabelNode end = new LabelNode();
            for (LocalVariableNode local : method.localVariables) {
                if (local.index < parameterSlots && parameters.stream().noneMatch(existing -> existing.index == local.index)) {
                    parameters.add(new LocalVariableNode(local.name, local.desc, local.signature, start, end, local.index));
                }
            }

            if (!parameters.isEmpty()) {
                instructions.insert(start);
                instructions.add(end);
            }
        }

        method.instructions = instructions;
        method.tryCatchBlocks = new ArrayList<>();
        method.localVariables = parameters.isEmpty() ? null : parameters;
        method.visibleLocalVariableAnnotations = null;
        method.invisibleLocalVariableAnnotations = null;
        method.access &= ~(ACC_NATIVE | ACC_ABSTRACT);
        method.maxLocals = 0;
        method.maxStack = 0;
    }

    /**
     * Renames the static initializer and calls it from a new one that catches every failure, so that a class whose
     * initializer depends on a patched method still initializes, with the failure reported by the runtime.
     */
    private static void guardStaticInit(ClassNode node, MethodNode clinit) {
        clinit.name = ORIGINAL_STATIC_INIT;
        clinit.access = ACC_PRIVATE | ACC_STATIC | ACC_SYNTHETIC;

        MethodNode guard = new MethodNode(ACC_STATIC, "<clinit>", "()V", null, null);
        LabelNode start = new LabelNode();
        LabelNode end = new LabelNode();
        LabelNode handler = new LabelNode();
        InsnList il = guard.instructions;
        il.add(start);
        il.add(new MethodInsnNode(INVOKESTATIC, node.name, ORIGINAL_STATIC_INIT, "()V", false));
        il.add(end);
        il.add(new InsnNode(RETURN));
        il.add(handler);
        if ((node.version & 0xFFFF) >= V1_6) {
            il.add(new FrameNode(F_SAME1, 0, null, 1, new Object[]{"java/lang/Throwable"}));
        }

        il.add(new LdcInsnNode(Type.getObjectType(node.name).getClassName()));
        il.add(new MethodInsnNode(INVOKESTATIC, Behaviour.RUNTIME + "StaticInit", "failed", STATIC_INIT_FAILED_DESC, false));
        il.add(new InsnNode(RETURN));
        guard.tryCatchBlocks.add(new TryCatchBlockNode(start, end, handler, "java/lang/Throwable"));
        node.methods.add(guard);
    }

}
