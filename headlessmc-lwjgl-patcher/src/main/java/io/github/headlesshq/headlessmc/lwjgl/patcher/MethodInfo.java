package io.github.headlesshq.headlessmc.lwjgl.patcher;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * A method of a class that is being patched.
 *
 * @param owner the declaring class.
 * @param method the method.
 */
public record MethodInfo(ClassNode owner, MethodNode method) {
    public String key() {
        return owner.name + "." + method.name + method.desc;
    }

    public String name() {
        return method.name;
    }

    public String desc() {
        return method.desc;
    }

    public boolean isStatic() {
        return (method.access & Opcodes.ACC_STATIC) != 0;
    }

    public boolean isAbstract() {
        return (method.access & Opcodes.ACC_ABSTRACT) != 0;
    }

    public boolean isBridge() {
        return (method.access & Opcodes.ACC_BRIDGE) != 0;
    }

    public boolean isNative() {
        return (method.access & Opcodes.ACC_NATIVE) != 0;
    }

    public boolean isOwnerInterface() {
        return (owner.access & Opcodes.ACC_INTERFACE) != 0;
    }

    public boolean isOwnerEnum() {
        return (owner.access & Opcodes.ACC_ENUM) != 0;
    }

    public Type returnType() {
        return Type.getReturnType(method.desc);
    }

    public Type[] argumentTypes() {
        return Type.getArgumentTypes(method.desc);
    }

    /**
     * @param index the index of the parameter, not counting {@code this}.
     * @return the local variable slot of the parameter.
     */
    public int slot(int index) {
        int slot = isStatic() ? 0 : 1;
        Type[] arguments = argumentTypes();
        for (int i = 0; i < index; i++) {
            slot += arguments[i].getSize();
        }

        return slot;
    }

    /**
     * @param index the index of the parameter, not counting {@code this}.
     * @return the name of the parameter from the local variable table, or {@code null} if it is not available.
     */
    public String parameterName(int index) {
        if (method.localVariables == null) {
            return null;
        }

        int slot = slot(index);
        for (LocalVariableNode local : method.localVariables) {
            if (local.index == slot) {
                return local.name;
            }
        }

        return null;
    }

}
