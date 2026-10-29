package io.github.headlesshq.headlessmc.lwjgl.patcher;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * All classes of the LWJGL jars that are being patched, used to answer type hierarchy questions.
 * Classes outside the pool are resolved through the JDK running the patcher.
 */
public final class ClassPool {
    private final Map<String, ClassNode> classes = new LinkedHashMap<>();
    private final Map<String, String> modules = new LinkedHashMap<>();

    public void add(String module, ClassNode node) {
        classes.put(node.name, node);
        modules.put(node.name, module);
    }

    public ClassNode get(String name) {
        return classes.get(name);
    }

    public boolean contains(String name) {
        return classes.containsKey(name);
    }

    public String module(String name) {
        return modules.get(name);
    }

    public Collection<ClassNode> classes() {
        return classes.values();
    }

    public String superName(String name) {
        ClassNode node = classes.get(name);
        if (node != null) {
            return node.superName;
        }

        Class<?> type = jdkClass(name);
        if (type == null || type.getSuperclass() == null) {
            return null;
        }

        return type.getSuperclass().getName().replace('.', '/');
    }

    /**
     * @param type the internal name of a class or interface.
     * @param supertype the internal name of a possible supertype.
     * @return {@code true} if {@code type} is {@code supertype} or a subtype of it.
     */
    public boolean isAssignable(String type, String supertype) {
        if (type.equals(supertype) || "java/lang/Object".equals(supertype)) {
            return true;
        }

        ClassNode node = classes.get(type);
        if (node == null) {
            Class<?> jdkType = jdkClass(type);
            Class<?> jdkSupertype = jdkClass(supertype);
            return jdkType != null && jdkSupertype != null && jdkSupertype.isAssignableFrom(jdkType);
        }

        if (node.superName != null && isAssignable(node.superName, supertype)) {
            return true;
        }

        for (String itf : node.interfaces) {
            if (isAssignable(itf, supertype)) {
                return true;
            }
        }

        return false;
    }

    public boolean isInterface(String name) {
        ClassNode node = classes.get(name);
        if (node != null) {
            return (node.access & Opcodes.ACC_INTERFACE) != 0;
        }

        Class<?> type = jdkClass(name);
        return type != null && type.isInterface();
    }

    public MethodNode findMethod(String owner, String name, String desc) {
        ClassNode node = classes.get(owner);
        if (node == null) {
            return null;
        }

        for (MethodNode method : node.methods) {
            if (method.name.equals(name) && method.desc.equals(desc)) {
                return method;
            }
        }

        return null;
    }

    /**
     * Finds the common superclass of two classes, for {@link org.objectweb.asm.ClassWriter#getCommonSuperClass}.
     */
    public String commonSuperClass(String first, String second) {
        if (isAssignable(second, first)) {
            return first;
        }

        if (isAssignable(first, second)) {
            return second;
        }

        if (isInterface(first) || isInterface(second)) {
            return "java/lang/Object";
        }

        String current = first;
        while (current != null && !isAssignable(second, current)) {
            current = superName(current);
        }

        return current == null ? "java/lang/Object" : current;
    }

    static Class<?> jdkClass(String internalName) {
        try {
            return Class.forName(internalName.replace('/', '.'), false, ClassLoader.getPlatformClassLoader());
        } catch (ClassNotFoundException | LinkageError e) {
            return null;
        }
    }

}
