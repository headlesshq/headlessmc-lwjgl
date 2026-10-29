package io.github.headlesshq.headlessmc.lwjgl.patcher;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InnerClassNode;
import org.objectweb.asm.tree.InsnList;
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
 * constructors. Structs and struct buffers get real memory from {@code StructMemory}, other classes a fake address:
 * <ul>
 *     <li>a public constructor {@code <init>(Ljava/lang/Void;)V} that only calls the matching super constructor,</li>
 *     <li>a static factory {@code hmc$dummy()} that creates an instance,</li>
 *     <li>a static factory {@code hmc$dummy(int capacity)} for LWJGL buffers,</li>
 *     <li>static factories {@code hmc$wrap(long address, ...)} that wrap an address, for structs and LWJGL buffers.</li>
 * </ul>
 * Abstract classes become concrete, so that they can be instantiated, their abstract methods are patched like any other
 * method.
 */
public final class Synthetics {
    public static final String MARKER_DESC = "(Ljava/lang/Void;)V";
    private static final String STRUCT = "org/lwjgl/system/Struct";
    private static final String CUSTOM_BUFFER = "org/lwjgl/system/CustomBuffer";
    private static final String STRUCT_BUFFER = "org/lwjgl/system/StructBuffer";
    private static final String POINTER = "org/lwjgl/system/Pointer$Default";
    private static final String STRUCT_MEMORY = Behaviour.RUNTIME + "StructMemory";
    public static final String WRAPPER = "hmc$wrap";
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
        return hasFactory(name) && pool.isAssignable(name, CUSTOM_BUFFER);
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

        node.methods.add(markerConstructor(node));
        if (pool.isAssignable(node.name, STRUCT)) {
            node.methods.add(structWrapper(node));
            node.methods.add(structFactory(node));
        } else if (pool.isAssignable(node.name, CUSTOM_BUFFER)) {
            node.methods.add(bufferWrapper(node));
            node.methods.add(bufferFactory(node));
            node.methods.add(factory(node));
        } else {
            node.methods.add(factory(node));
        }
    }

    /**
     * @param name a class.
     * @return the static {@code SIZEOF} field of the struct the class is or holds elements of, or {@code null}.
     */
    private String structSize(String name) {
        String struct = name;
        if (pool.isAssignable(name, STRUCT_BUFFER) && name.endsWith("$Buffer")) {
            struct = name.substring(0, name.length() - "$Buffer".length());
        } else if (!pool.isAssignable(name, STRUCT)) {
            return null;
        }

        ClassNode node = pool.get(struct);
        if (node == null) {
            return null;
        }

        for (FieldNode field : node.fields) {
            if ("SIZEOF".equals(field.name) && "I".equals(field.desc) && (field.access & ACC_STATIC) != 0) {
                return struct;
            }
        }

        return null;
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

    private static MethodNode factory(ClassNode node) {
        MethodNode method = new MethodNode(ACC_PUBLIC | ACC_STATIC | ACC_SYNTHETIC, Behaviour.DUMMY_FACTORY, "()L" + node.name + ";", null, null);
        method.instructions.add(new TypeInsnNode(NEW, node.name));
        method.instructions.add(new InsnNode(DUP));
        method.instructions.add(new InsnNode(ACONST_NULL));
        method.instructions.add(new MethodInsnNode(INVOKESPECIAL, node.name, "<init>", MARKER_DESC, false));
        method.instructions.add(new InsnNode(ARETURN));
        return method;
    }

    /**
     * {@code hmc$dummy()} for structs: a struct with new memory from {@code StructMemory}, or with a fake address if
     * the size of the struct is unknown.
     */
    private MethodNode structFactory(ClassNode node) {
        MethodNode method = new MethodNode(ACC_PUBLIC | ACC_STATIC | ACC_SYNTHETIC, Behaviour.DUMMY_FACTORY, "()L" + node.name + ";", null, null);
        String struct = structSize(node.name);
        if (struct != null) {
            method.instructions.add(new FieldInsnNode(GETSTATIC, struct, "SIZEOF", "I"));
            allocate(method);
        } else {
            method.instructions.add(new LdcInsnNode(Behaviour.FAKE_ADDRESS));
            method.instructions.add(new InsnNode(ACONST_NULL));
        }

        method.instructions.add(new MethodInsnNode(INVOKESTATIC, node.name, WRAPPER, "(JLjava/nio/ByteBuffer;)L" + node.name + ";", false));
        method.instructions.add(new InsnNode(ARETURN));
        return method;
    }

    /**
     * {@code hmc$dummy(int capacity)} for buffers: struct buffers get new memory from {@code StructMemory}, other
     * buffers a fake address.
     */
    private MethodNode bufferFactory(ClassNode node) {
        MethodNode method = new MethodNode(ACC_PUBLIC | ACC_STATIC | ACC_SYNTHETIC, Behaviour.DUMMY_FACTORY, "(I)L" + node.name + ";", null, null);
        String struct = structSize(node.name);
        if (struct != null) {
            method.instructions.add(new VarInsnNode(ILOAD, 0));
            method.instructions.add(new FieldInsnNode(GETSTATIC, struct, "SIZEOF", "I"));
            method.instructions.add(new InsnNode(IMUL));
            allocate(method);
            method.instructions.add(new VarInsnNode(ILOAD, 0));
            method.instructions.add(new InsnNode(SWAP));
        } else {
            method.instructions.add(new LdcInsnNode(Behaviour.FAKE_ADDRESS));
            method.instructions.add(new VarInsnNode(ILOAD, 0));
            method.instructions.add(new InsnNode(ACONST_NULL));
        }

        method.instructions.add(new MethodInsnNode(INVOKESTATIC, node.name, WRAPPER, "(JILjava/nio/ByteBuffer;)L" + node.name + ";", false));
        method.instructions.add(new InsnNode(ARETURN));
        return method;
    }

    /**
     * {@code hmc$wrap(long address, ByteBuffer container)}: a struct at the given address, created with the struct
     * constructor, or for structs of old LWJGL versions without one with the marker constructor and the fields set.
     */
    private MethodNode structWrapper(ClassNode node) {
        MethodNode method = new MethodNode(ACC_PUBLIC | ACC_STATIC | ACC_SYNTHETIC, WRAPPER, "(JLjava/nio/ByteBuffer;)L" + node.name + ";", null, null);
        InsnList il = method.instructions;
        il.add(new TypeInsnNode(NEW, node.name));
        il.add(new InsnNode(DUP));
        if (pool.findMethod(node.name, "<init>", STRUCT_CONSTRUCTOR) != null) {
            il.add(new VarInsnNode(LLOAD, 0));
            il.add(new VarInsnNode(ALOAD, 2));
            il.add(new MethodInsnNode(INVOKESPECIAL, node.name, "<init>", STRUCT_CONSTRUCTOR, false));
        } else {
            il.add(new InsnNode(ACONST_NULL));
            il.add(new MethodInsnNode(INVOKESPECIAL, node.name, "<init>", MARKER_DESC, false));
            il.add(new InsnNode(DUP));
            il.add(new VarInsnNode(LLOAD, 0));
            il.add(new FieldInsnNode(PUTFIELD, POINTER, "address", "J"));
            il.add(new InsnNode(DUP));
            il.add(new VarInsnNode(ALOAD, 2));
            il.add(new FieldInsnNode(PUTFIELD, STRUCT, "container", "Ljava/nio/ByteBuffer;"));
        }

        il.add(new InsnNode(ARETURN));
        return method;
    }

    /**
     * {@code hmc$wrap(long address, int capacity, ByteBuffer container)}: a buffer at the given address, created
     * with the buffer constructor, or with the marker constructor and the fields set.
     */
    private MethodNode bufferWrapper(ClassNode node) {
        MethodNode method = new MethodNode(ACC_PUBLIC | ACC_STATIC | ACC_SYNTHETIC, WRAPPER, "(JILjava/nio/ByteBuffer;)L" + node.name + ";", null, null);
        InsnList il = method.instructions;
        il.add(new TypeInsnNode(NEW, node.name));
        il.add(new InsnNode(DUP));
        if (pool.findMethod(node.name, "<init>", BUFFER_CONSTRUCTOR) != null) {
            il.add(new VarInsnNode(LLOAD, 0));
            il.add(new VarInsnNode(ALOAD, 3));
            il.add(new InsnNode(ICONST_M1));
            il.add(new InsnNode(ICONST_0));
            il.add(new VarInsnNode(ILOAD, 2));
            il.add(new VarInsnNode(ILOAD, 2));
            il.add(new MethodInsnNode(INVOKESPECIAL, node.name, "<init>", BUFFER_CONSTRUCTOR, false));
        } else {
            il.add(new InsnNode(ACONST_NULL));
            il.add(new MethodInsnNode(INVOKESPECIAL, node.name, "<init>", MARKER_DESC, false));
            il.add(new InsnNode(DUP));
            il.add(new VarInsnNode(LLOAD, 0));
            il.add(new FieldInsnNode(PUTFIELD, POINTER, "address", "J"));
            il.add(new InsnNode(DUP));
            il.add(new VarInsnNode(ALOAD, 3));
            il.add(new FieldInsnNode(PUTFIELD, CUSTOM_BUFFER, "container", "Ljava/nio/ByteBuffer;"));
            il.add(new InsnNode(DUP));
            il.add(new InsnNode(ICONST_M1));
            il.add(new FieldInsnNode(PUTFIELD, CUSTOM_BUFFER, "mark", "I"));
            for (String field : new String[]{"limit", "capacity"}) {
                il.add(new InsnNode(DUP));
                il.add(new VarInsnNode(ILOAD, 2));
                il.add(new FieldInsnNode(PUTFIELD, CUSTOM_BUFFER, field, "I"));
            }
        }

        il.add(new InsnNode(ARETURN));
        return method;
    }

    /**
     * Allocates struct memory of the size on the stack, leaving its address and the buffer on the stack.
     */
    private static void allocate(MethodNode method) {
        method.instructions.add(new MethodInsnNode(INVOKESTATIC, STRUCT_MEMORY, "allocate", "(I)Ljava/nio/ByteBuffer;", false));
        method.instructions.add(new InsnNode(DUP));
        method.instructions.add(new MethodInsnNode(INVOKESTATIC, STRUCT_MEMORY, "address", "(Ljava/nio/Buffer;)J", false));
        method.instructions.add(new InsnNode(DUP2_X1));
        method.instructions.add(new InsnNode(POP2));
    }

    static boolean isSynthetic(MethodNode method) {
        return (method.access & Opcodes.ACC_SYNTHETIC) != 0
            && (Behaviour.DUMMY_FACTORY.equals(method.name) || WRAPPER.equals(method.name)
                || MARKER_DESC.equals(method.desc) && "<init>".equals(method.name));
    }

}
