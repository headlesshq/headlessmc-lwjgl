package io.github.headlesshq.headlessmc.lwjgl3.patcher;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InnerClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

import static org.objectweb.asm.Opcodes.*;

/**
 * Adds synthetic members that let patched code create instances of LWJGL classes without running their original
 * constructors:
 * <ul>
 *     <li>a public constructor {@code <init>(Ljava/lang/Void;)V} that only calls the matching super constructor,</li>
 *     <li>a static factory {@code hmc$dummy()} that creates an instance,</li>
 *     <li>a static factory {@code hmc$dummy(int capacity)} for LWJGL buffers.</li>
 * </ul>
 * Abstract classes become concrete, so that they can be instantiated, their abstract methods are patched like any other
 * method.
 */
public final class Synthetics {
    public static final String MARKER_DESC = "(Ljava/lang/Void;)V";
    private static final String STRUCT = "org/lwjgl/system/Struct";
    private static final String CUSTOM_BUFFER = "org/lwjgl/system/CustomBuffer";
    private static final String STRUCT_CONSTRUCTOR = "(JLjava/nio/ByteBuffer;)V";
    private static final String BUFFER_CONSTRUCTOR = "(JLjava/nio/ByteBuffer;IIII)V";

    private final ClassPool pool;
    private final Map<String, Boolean> instantiable = new HashMap<>();

    public Synthetics(ClassPool pool) {
        this.pool = pool;
    }

    /**
     * @param name the internal name of a class.
     * @return {@code true} if the class gets a {@code hmc$dummy()} factory.
     */
    public boolean hasFactory(String name) {
        Boolean cached = instantiable.get(name);
        if (cached != null) {
            return cached;
        }

        boolean result = computeInstantiable(name);
        instantiable.put(name, result);
        return result;
    }

    /**
     * @param name the internal name of a class.
     * @return {@code true} if the class gets a {@code hmc$dummy(int)} factory.
     */
    public boolean hasBufferFactory(String name) {
        return hasFactory(name) && pool.isAssignable(name, CUSTOM_BUFFER) && pool.findMethod(name, "<init>", BUFFER_CONSTRUCTOR) != null;
    }

    /**
     * Adds the synthetic members to the given class, if it is instantiable.
     *
     * @param node the class to modify.
     */
    public void apply(ClassNode node) {
        if (!hasFactory(node.name)) {
            return;
        }

        node.access &= ~ACC_ABSTRACT;
        if (node.innerClasses != null) {
            for (InnerClassNode inner : node.innerClasses) {
                if (inner.name.equals(node.name)) {
                    inner.access &= ~ACC_ABSTRACT;
                }
            }
        }

        boolean useStructConstructor = pool.isAssignable(node.name, STRUCT) && pool.findMethod(node.name, "<init>", STRUCT_CONSTRUCTOR) != null;
        boolean useBufferConstructor = hasBufferFactory(node.name);
        node.methods.add(markerConstructor(node));
        if (useBufferConstructor) {
            node.methods.add(bufferFactory(node));
        }

        node.methods.add(factory(node, useStructConstructor));
    }

    private boolean computeInstantiable(String name) {
        ClassNode node = pool.get(name);
        if (node == null || (node.access & (ACC_INTERFACE | ACC_ENUM | ACC_ANNOTATION | ACC_MODULE)) != 0) {
            return false;
        }

        String superName = node.superName;
        if (superName == null || "java/lang/Object".equals(superName)) {
            return true;
        }

        if (pool.contains(superName)) {
            return hasFactory(superName);
        }

        Class<?> superClass = ClassPool.jdkClass(superName);
        if (superClass == null) {
            return false;
        }

        try {
            Constructor<?> constructor = superClass.getDeclaredConstructor();
            return Modifier.isPublic(constructor.getModifiers()) || Modifier.isProtected(constructor.getModifiers());
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    private MethodNode markerConstructor(ClassNode node) {
        MethodNode method = new MethodNode(ACC_PUBLIC | ACC_SYNTHETIC, "<init>", MARKER_DESC, null, null);
        method.instructions.add(new VarInsnNode(ALOAD, 0));
        if (node.superName != null && pool.contains(node.superName)) {
            method.instructions.add(new InsnNode(ACONST_NULL));
            method.instructions.add(new MethodInsnNode(INVOKESPECIAL, node.superName, "<init>", MARKER_DESC, false));
        } else {
            String superName = node.superName == null ? "java/lang/Object" : node.superName;
            method.instructions.add(new MethodInsnNode(INVOKESPECIAL, superName, "<init>", "()V", false));
        }

        method.instructions.add(new InsnNode(RETURN));
        return method;
    }

    private MethodNode factory(ClassNode node, boolean useStructConstructor) {
        MethodNode method = new MethodNode(ACC_PUBLIC | ACC_STATIC | ACC_SYNTHETIC, Behaviour.DUMMY_FACTORY, "()L" + node.name + ";", null, null);
        method.instructions.add(new TypeInsnNode(NEW, node.name));
        method.instructions.add(new InsnNode(DUP));
        if (useStructConstructor) {
            method.instructions.add(new LdcInsnNode(Behaviour.FAKE_ADDRESS));
            method.instructions.add(new InsnNode(ACONST_NULL));
            method.instructions.add(new MethodInsnNode(INVOKESPECIAL, node.name, "<init>", STRUCT_CONSTRUCTOR, false));
        } else {
            method.instructions.add(new InsnNode(ACONST_NULL));
            method.instructions.add(new MethodInsnNode(INVOKESPECIAL, node.name, "<init>", MARKER_DESC, false));
        }

        method.instructions.add(new InsnNode(ARETURN));
        return method;
    }

    private MethodNode bufferFactory(ClassNode node) {
        MethodNode method = new MethodNode(ACC_PUBLIC | ACC_STATIC | ACC_SYNTHETIC, Behaviour.DUMMY_FACTORY, "(I)L" + node.name + ";", null, null);
        method.instructions.add(new TypeInsnNode(NEW, node.name));
        method.instructions.add(new InsnNode(DUP));
        method.instructions.add(new LdcInsnNode(Behaviour.FAKE_ADDRESS));
        method.instructions.add(new InsnNode(ACONST_NULL));
        method.instructions.add(new InsnNode(ICONST_M1));
        method.instructions.add(new InsnNode(ICONST_0));
        method.instructions.add(new VarInsnNode(ILOAD, 0));
        method.instructions.add(new VarInsnNode(ILOAD, 0));
        method.instructions.add(new MethodInsnNode(INVOKESPECIAL, node.name, "<init>", BUFFER_CONSTRUCTOR, false));
        method.instructions.add(new InsnNode(ARETURN));
        return method;
    }

    static boolean isSynthetic(MethodNode method) {
        return (method.access & Opcodes.ACC_SYNTHETIC) != 0
            && (Behaviour.DUMMY_FACTORY.equals(method.name) || MARKER_DESC.equals(method.desc) && "<init>".equals(method.name));
    }

}
