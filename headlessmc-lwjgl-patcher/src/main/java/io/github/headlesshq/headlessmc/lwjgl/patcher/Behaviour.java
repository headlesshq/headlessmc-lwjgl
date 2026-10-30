package io.github.headlesshq.headlessmc.lwjgl.patcher;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

import static org.objectweb.asm.Opcodes.*;

/**
 * What a patched method does. Every behaviour except {@link Keep} replaces the method body with straight-line code.
 */
public sealed interface Behaviour {
    String RUNTIME = "io/github/headlesshq/headlessmc/lwjgl/runtime/";
    String DUMMY_BUFFERS = RUNTIME + "DummyBuffers";
    String DUMMY_FACTORY = "hmc$dummy";
    long FAKE_ADDRESS = 0x10000L;

    /**
     * @return a short, stable description for the lock file.
     */
    String describe();

    /**
     * @param method the patched method.
     * @return the new method body.
     */
    InsnList emit(MethodInfo method);

    /**
     * Keeps the original method body.
     */
    record Keep() implements Behaviour {
        @Override
        public String describe() {
            return "keep";
        }

        @Override
        public InsnList emit(MethodInfo method) {
            throw new UnsupportedOperationException("Keep does not emit code");
        }
    }

    /**
     * Returns {@code 0}, {@code false}, {@code ""}, an empty array or {@code null}, or does nothing for {@code void}.
     */
    record TypeDefault() implements Behaviour {
        @Override
        public String describe() {
            return "default";
        }

        @Override
        public InsnList emit(MethodInfo method) {
            InsnList il = new InsnList();
            Type type = method.returnType();
            pushDefault(il, type);
            il.add(new InsnNode(type.getOpcode(IRETURN)));
            return il;
        }
    }

    /**
     * Returns a constant.
     *
     * @param value an {@link Integer}, {@link Long}, {@link Float}, {@link Double}, {@link Boolean} or {@link String}.
     */
    record Constant(Object value) implements Behaviour {
        @Override
        public String describe() {
            return value instanceof String s ? "constant \"" + s + "\"" : "constant " + value;
        }

        @Override
        public InsnList emit(MethodInfo method) {
            InsnList il = new InsnList();
            Type type = method.returnType();
            Object constant = value instanceof Boolean b ? (Integer) (b ? 1 : 0) : value;
            switch (type.getSort()) {
                case Type.BOOLEAN, Type.BYTE, Type.CHAR, Type.SHORT, Type.INT ->
                    il.add(new LdcInsnNode(((Number) constant).intValue()));
                case Type.LONG -> il.add(new LdcInsnNode(((Number) constant).longValue()));
                case Type.FLOAT -> il.add(new LdcInsnNode(((Number) constant).floatValue()));
                case Type.DOUBLE -> il.add(new LdcInsnNode(((Number) constant).doubleValue()));
                default -> il.add(new LdcInsnNode(constant));
            }

            il.add(new InsnNode(type.getOpcode(IRETURN)));
            return il;
        }
    }

    /**
     * Returns whether a parameter is not 0, e.g. {@code glIsTexture(name)}: every name an application passes was
     * created by it.
     *
     * @param parameter the index of the parameter, not counting {@code this}.
     */
    record NonZeroParameter(int parameter) implements Behaviour {
        @Override
        public String describe() {
            return "arg" + parameter + " != 0";
        }

        /**
         * Branch free, {@code (x | -x) >>> (bits - 1)} is 1 for every x but 0, so that no stack map frame is needed,
         * which class files older than Java 6 must not have.
         */
        @Override
        public InsnList emit(MethodInfo method) {
            InsnList il = new InsnList();
            Type type = method.argumentTypes()[parameter];
            il.add(new VarInsnNode(type.getOpcode(ILOAD), method.slot(parameter)));
            if (type.getSort() == Type.LONG) {
                il.add(new InsnNode(DUP2));
                il.add(new InsnNode(LNEG));
                il.add(new InsnNode(LOR));
                il.add(new IntInsnNode(BIPUSH, 63));
                il.add(new InsnNode(LUSHR));
                il.add(new InsnNode(L2I));
            } else {
                il.add(new InsnNode(DUP));
                il.add(new InsnNode(INEG));
                il.add(new InsnNode(IOR));
                il.add(new IntInsnNode(BIPUSH, 31));
                il.add(new InsnNode(IUSHR));
            }

            il.add(new InsnNode(IRETURN));
            return il;
        }
    }

    /**
     * Returns a parameter, e.g. the output pointer of {@code stbir_resize_uint8_linear}.
     *
     * @param parameter the index of the parameter, not counting {@code this}.
     */
    record Parameter(int parameter) implements Behaviour {
        @Override
        public String describe() {
            return "arg" + parameter;
        }

        @Override
        public InsnList emit(MethodInfo method) {
            InsnList il = new InsnList();
            Type type = method.argumentTypes()[parameter];
            il.add(new VarInsnNode(type.getOpcode(ILOAD), method.slot(parameter)));
            il.add(new InsnNode(method.returnType().getOpcode(IRETURN)));
            return il;
        }
    }

    /**
     * Returns {@code this}, for fluent instance methods.
     */
    record ReturnThis() implements Behaviour {
        @Override
        public String describe() {
            return "this";
        }

        @Override
        public InsnList emit(MethodInfo method) {
            InsnList il = new InsnList();
            il.add(new VarInsnNode(ALOAD, 0));
            il.add(new InsnNode(ARETURN));
            return il;
        }
    }

    /**
     * Returns a new unique, nonzero name or handle.
     */
    record NextId() implements Behaviour {
        @Override
        public String describe() {
            return "next-id";
        }

        @Override
        public InsnList emit(MethodInfo method) {
            InsnList il = new InsnList();
            Type type = method.returnType();
            if (type.getSort() == Type.LONG) {
                il.add(new MethodInsnNode(INVOKESTATIC, RUNTIME + "Ids", "nextLong", "()J", false));
            } else {
                il.add(new MethodInsnNode(INVOKESTATIC, RUNTIME + "Ids", "nextInt", "()I", false));
            }

            il.add(new InsnNode(type.getOpcode(IRETURN)));
            return il;
        }
    }

    /**
     * Stores a function pointer in {@code CallbackSlots} and returns the previous one, for native callback setters like
     * {@code nglfwSetKeyCallback(window, cbfun)}: the first parameter of a setter with two is the scope, e.g. the
     * window, the last one the new function pointer.
     */
    record CallbackSlot() implements Behaviour {
        @Override
        public String describe() {
            return "callback-slot";
        }

        @Override
        public InsnList emit(MethodInfo method) {
            InsnList il = new InsnList();
            int function = method.argumentTypes().length - 1;
            il.add(new LdcInsnNode(method.name()));
            if (function == 0) {
                il.add(new InsnNode(LCONST_0));
            } else {
                il.add(new VarInsnNode(LLOAD, method.slot(0)));
            }

            il.add(new VarInsnNode(LLOAD, method.slot(function)));
            il.add(new MethodInsnNode(INVOKESTATIC, RUNTIME + "CallbackSlots", "swap", "(Ljava/lang/String;JJ)J", false));
            il.add(new InsnNode(LRETURN));
            return il;
        }
    }

    /**
     * Delegates to a static runtime method annotated with {@code @Patch}.
     */
    record Handler(String owner, String name, String desc) implements Behaviour {
        @Override
        public String describe() {
            return "handler " + owner.substring(owner.lastIndexOf('/') + 1) + "." + name;
        }

        @Override
        public InsnList emit(MethodInfo method) {
            InsnList il = new InsnList();
            if (!method.isStatic()) {
                il.add(new VarInsnNode(ALOAD, 0));
            }

            loadArguments(il, method);
            il.add(new MethodInsnNode(INVOKESTATIC, owner, name, desc, false));
            Type returnType = method.returnType();
            Type handlerReturnType = Type.getReturnType(desc);
            if (returnType.getSort() >= Type.ARRAY && !returnType.equals(handlerReturnType)) {
                il.add(new TypeInsnNode(CHECKCAST, returnType.getInternalName()));
            }

            il.add(new InsnNode(returnType.getOpcode(IRETURN)));
            return il;
        }
    }

    /**
     * Returns a {@code java.nio} buffer from {@code DummyBuffers}.
     *
     * @param factory the factory method in {@code DummyBuffers}, e.g. {@code ints}.
     * @param sizeParameter the index of the parameter holding the capacity in elements, -1 for an empty buffer.
     */
    record NioBuffer(String factory, int sizeParameter) implements Behaviour {
        @Override
        public String describe() {
            return "buffer " + factory + (sizeParameter < 0 ? "(0)" : "(arg" + sizeParameter + ")");
        }

        @Override
        public InsnList emit(MethodInfo method) {
            InsnList il = new InsnList();
            loadSize(il, method, sizeParameter, true);
            Type returnType = method.returnType();
            il.add(new MethodInsnNode(INVOKESTATIC, DUMMY_BUFFERS, factory, "(J)" + nioType(factory).getDescriptor(), false));
            if (!returnType.equals(nioType(factory))) {
                il.add(new TypeInsnNode(CHECKCAST, returnType.getInternalName()));
            }

            il.add(new InsnNode(ARETURN));
            return il;
        }
    }

    /**
     * Wraps the arguments of the method into a {@code java.nio} buffer, e.g. for {@code MemoryStack.ints(int...)}.
     *
     * @param elementType the primitive element type.
     */
    record ValuesBuffer(Type elementType) implements Behaviour {
        @Override
        public String describe() {
            return "values " + elementType.getClassName();
        }

        @Override
        public InsnList emit(MethodInfo method) {
            InsnList il = new InsnList();
            Type[] arguments = method.argumentTypes();
            Type arrayType = Type.getType("[" + elementType.getDescriptor());
            if (arguments.length == 1 && arguments[0].equals(arrayType)) {
                il.add(new VarInsnNode(ALOAD, method.slot(0)));
            } else {
                il.add(new LdcInsnNode(arguments.length));
                il.add(new IntInsnNode(NEWARRAY, newArrayOperand(elementType)));
                for (int i = 0; i < arguments.length; i++) {
                    il.add(new InsnNode(DUP));
                    il.add(new LdcInsnNode(i));
                    il.add(new VarInsnNode(arguments[i].getOpcode(ILOAD), method.slot(i)));
                    il.add(new InsnNode(elementType.getOpcode(IASTORE)));
                }
            }

            Type bufferType = Type.getReturnType(method.desc());
            il.add(new MethodInsnNode(INVOKESTATIC, DUMMY_BUFFERS, "wrap",
                                      "(" + arrayType.getDescriptor() + ")" + bufferType.getDescriptor(), false));
            il.add(new InsnNode(ARETURN));
            return il;
        }
    }

    /**
     * Returns a new LWJGL buffer ({@code PointerBuffer}, {@code StructBuffer}, ...) via its synthetic factory.
     *
     * @param type the internal name of the buffer class.
     * @param sizeParameter the index of the parameter holding the capacity, -1 for an empty buffer.
     */
    record LwjglBuffer(String type, int sizeParameter) implements Behaviour {
        @Override
        public String describe() {
            return "buffer " + type.substring(type.lastIndexOf('/') + 1) + (sizeParameter < 0 ? "(0)" : "(arg" + sizeParameter + ")");
        }

        @Override
        public InsnList emit(MethodInfo method) {
            InsnList il = new InsnList();
            loadSize(il, method, sizeParameter, false);
            il.add(new MethodInsnNode(INVOKESTATIC, type, DUMMY_FACTORY, "(I)L" + type + ";", false));
            if (!method.returnType().getInternalName().equals(type)) {
                il.add(new TypeInsnNode(CHECKCAST, method.returnType().getInternalName()));
            }

            il.add(new InsnNode(ARETURN));
            return il;
        }
    }

    /**
     * Returns a new dummy instance of an LWJGL class via its synthetic factory.
     *
     * @param type the internal name of the class to instantiate.
     */
    record NewObject(String type) implements Behaviour {
        @Override
        public String describe() {
            return "new " + type.substring(type.lastIndexOf('/') + 1);
        }

        @Override
        public InsnList emit(MethodInfo method) {
            InsnList il = new InsnList();
            il.add(new MethodInsnNode(INVOKESTATIC, type, DUMMY_FACTORY, "()L" + type + ";", false));
            if (!method.returnType().getInternalName().equals(type)) {
                il.add(new TypeInsnNode(CHECKCAST, method.returnType().getInternalName()));
            }

            il.add(new InsnNode(ARETURN));
            return il;
        }
    }

    /**
     * Returns a shared proxy of an LWJGL interface whose methods return default values.
     *
     * @param type the internal name of the interface.
     */
    record ProxyObject(String type) implements Behaviour {
        @Override
        public String describe() {
            return "proxy " + type.substring(type.lastIndexOf('/') + 1);
        }

        @Override
        public InsnList emit(MethodInfo method) {
            InsnList il = new InsnList();
            il.add(new LdcInsnNode(Type.getObjectType(type)));
            il.add(new MethodInsnNode(INVOKESTATIC, RUNTIME + "Dummies", "proxy", "(Ljava/lang/Class;)Ljava/lang/Object;", false));
            il.add(new TypeInsnNode(CHECKCAST, method.returnType().getInternalName()));
            il.add(new InsnNode(ARETURN));
            return il;
        }
    }

    /**
     * Identity based {@code equals} or {@code hashCode}.
     *
     * @param method {@code equals} or {@code hashCode}.
     */
    record Identity(String method) implements Behaviour {
        @Override
        public String describe() {
            return "identity " + method;
        }

        @Override
        public InsnList emit(MethodInfo info) {
            InsnList il = new InsnList();
            il.add(new VarInsnNode(ALOAD, 0));
            if ("equals".equals(method)) {
                il.add(new VarInsnNode(ALOAD, 1));
                il.add(new MethodInsnNode(INVOKESTATIC, RUNTIME + "Identity", "equals", "(Ljava/lang/Object;Ljava/lang/Object;)Z", false));
            } else {
                il.add(new MethodInsnNode(INVOKESTATIC, RUNTIME + "Identity", "hashCode", "(Ljava/lang/Object;)I", false));
            }

            il.add(new InsnNode(IRETURN));
            return il;
        }
    }

    static void pushDefault(InsnList il, Type type) {
        switch (type.getSort()) {
            case Type.VOID -> { }
            case Type.BOOLEAN, Type.BYTE, Type.CHAR, Type.SHORT, Type.INT -> il.add(new InsnNode(ICONST_0));
            case Type.LONG -> il.add(new InsnNode(LCONST_0));
            case Type.FLOAT -> il.add(new InsnNode(FCONST_0));
            case Type.DOUBLE -> il.add(new InsnNode(DCONST_0));
            case Type.ARRAY -> {
                il.add(new InsnNode(ICONST_0));
                Type element = Type.getType(type.getDescriptor().substring(1));
                if (element.getSort() == Type.OBJECT || element.getSort() == Type.ARRAY) {
                    il.add(new TypeInsnNode(ANEWARRAY, element.getInternalName()));
                } else {
                    il.add(new IntInsnNode(NEWARRAY, newArrayOperand(element)));
                }
            }
            default -> {
                String name = type.getInternalName();
                if ("java/lang/String".equals(name) || "java/lang/CharSequence".equals(name)) {
                    il.add(new LdcInsnNode(""));
                } else {
                    il.add(new InsnNode(ACONST_NULL));
                }
            }
        }
    }

    static void loadArguments(InsnList il, MethodInfo method) {
        Type[] arguments = method.argumentTypes();
        for (int i = 0; i < arguments.length; i++) {
            il.add(new VarInsnNode(arguments[i].getOpcode(ILOAD), method.slot(i)));
        }
    }

    private static void loadSize(InsnList il, MethodInfo method, int parameter, boolean asLong) {
        if (parameter < 0) {
            il.add(new InsnNode(asLong ? LCONST_0 : ICONST_0));
            return;
        }

        Type type = method.argumentTypes()[parameter];
        il.add(new VarInsnNode(type.getOpcode(ILOAD), method.slot(parameter)));
        if (type.getSort() == Type.LONG && !asLong) {
            il.add(new InsnNode(L2I));
        } else if (type.getSort() != Type.LONG && asLong) {
            il.add(new InsnNode(I2L));
        }
    }

    static Type nioType(String factory) {
        return switch (factory) {
            case "bytes" -> Type.getObjectType("java/nio/ByteBuffer");
            case "shorts" -> Type.getObjectType("java/nio/ShortBuffer");
            case "chars" -> Type.getObjectType("java/nio/CharBuffer");
            case "ints" -> Type.getObjectType("java/nio/IntBuffer");
            case "longs" -> Type.getObjectType("java/nio/LongBuffer");
            case "floats" -> Type.getObjectType("java/nio/FloatBuffer");
            case "doubles" -> Type.getObjectType("java/nio/DoubleBuffer");
            default -> throw new IllegalArgumentException(factory);
        };
    }

    private static int newArrayOperand(Type element) {
        return switch (element.getSort()) {
            case Type.BOOLEAN -> Opcodes.T_BOOLEAN;
            case Type.CHAR -> Opcodes.T_CHAR;
            case Type.BYTE -> Opcodes.T_BYTE;
            case Type.SHORT -> Opcodes.T_SHORT;
            case Type.INT -> Opcodes.T_INT;
            case Type.LONG -> Opcodes.T_LONG;
            case Type.FLOAT -> Opcodes.T_FLOAT;
            case Type.DOUBLE -> Opcodes.T_DOUBLE;
            default -> throw new IllegalArgumentException(element.toString());
        };
    }

}
