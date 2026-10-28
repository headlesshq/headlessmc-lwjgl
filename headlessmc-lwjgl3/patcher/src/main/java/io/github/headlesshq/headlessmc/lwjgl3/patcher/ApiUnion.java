package io.github.headlesshq.headlessmc.lwjgl3.patcher;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InnerClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.objectweb.asm.Opcodes.*;

/**
 * Re-adds the public API that older LWJGL versions had and the latest version removed, so that code compiled
 * against an older version links against the patched jars.
 * <ul>
 *     <li>Missing public classes are copied from the newest old version that has them ({@link #addClasses}).</li>
 *     <li>Missing public and protected methods and fields are copied into the existing classes, members that became
 *     less accessible are made accessible again, removed superinterfaces are added again and a removed superclass
 *     is put back between the class and its current superclass ({@link #addMembers}).</li>
 * </ul>
 * Copied methods are marked {@code native}, so that the {@link Catalog} has to replace their bodies, which may refer
 * to code that no longer exists. Copied constructors delegate to the {@link Synthetics#MARKER_DESC marker constructor}.
 * All classes of all old versions have to be added before the members, both newest version first.
 */
public final class ApiUnion {
    private static final int VISIBILITY = ACC_PUBLIC | ACC_PROTECTED | ACC_PRIVATE;

    private final ClassPool pool;
    private final Synthetics synthetics;
    private final Map<String, String> packageModules = new HashMap<>();
    private final Map<String, String> classOrigins = new TreeMap<>();
    private final TreeMap<String, String> memberOrigins = new TreeMap<>();
    private final List<String> errors = new ArrayList<>();

    public ApiUnion(ClassPool pool, Synthetics synthetics) {
        this.pool = pool;
        this.synthetics = synthetics;
        for (ClassNode node : pool.classes()) {
            packageModules.put(packageName(node.name), pool.module(node.name));
        }
    }

    /**
     * Adds the public classes of a module jar of an older version that are missing.
     *
     * @param version the LWJGL version of the jar.
     * @param module the module the jar belongs to.
     * @param classes the base classes of the jar.
     * @return the added classes, keyed by the module they have to be added to.
     */
    public Map<String, List<ClassNode>> addClasses(String version, String module, List<ClassNode> classes) {
        Map<String, List<ClassNode>> added = new TreeMap<>();
        for (ClassNode old : classes) {
            if (pool.contains(old.name) || (old.access & ACC_PUBLIC) == 0) {
                continue;
            }

            String target = packageModules.getOrDefault(packageName(old.name), module);
            pool.add(target, old);
            packageModules.putIfAbsent(packageName(old.name), target);
            classOrigins.put(old.name, version);
            added.computeIfAbsent(target, k -> new ArrayList<>()).add(old);
        }

        for (List<ClassNode> nodes : added.values()) {
            nodes.forEach(this::registerNestedClass);
        }

        return added;
    }

    /**
     * Adds the {@code InnerClasses} entry of a re-added nested class to its outer class, so that compilers and
     * reflection find it as a member of the outer class.
     */
    private void registerNestedClass(ClassNode node) {
        if (node.innerClasses == null) {
            return;
        }

        for (InnerClassNode inner : node.innerClasses) {
            ClassNode outer = inner.outerName == null ? null : pool.get(inner.outerName);
            if (inner.name.equals(node.name) && outer != null
                && outer.innerClasses.stream().noneMatch(existing -> existing.name.equals(node.name))) {
                outer.innerClasses.add(new InnerClassNode(inner.name, inner.outerName, inner.innerName, inner.access));
            }
        }
    }

    /**
     * Adds the API members of a module jar of an older version that are missing.
     *
     * @param version the LWJGL version of the jar.
     * @param classes the base classes of the jar.
     */
    public void addMembers(String version, List<ClassNode> classes) {
        for (ClassNode old : classes) {
            ClassNode current = pool.get(old.name);
            if (current != null && current != old && isApi(old.access)) {
                widen(current, old.access);
                addSuperclass(version, current, old);
                addFields(version, current, old);
                addMethods(version, current, old);
                addInterfaces(current, old);
            }
        }
    }

    private void addSuperclass(String version, ClassNode current, ClassNode old) {
        if (old.superName == null || old.superName.equals(current.superName) || pool.isAssignable(current.name, old.superName)) {
            return;
        }

        if (!pool.contains(old.superName) || current.superName == null || !pool.isAssignable(old.superName, current.superName)) {
            errors.add("Cannot restore superclass " + old.superName + " of " + current.name + " from " + version);
            return;
        }

        for (MethodNode method : current.methods) {
            if (!"<init>".equals(method.name)) {
                continue;
            }

            for (AbstractInsnNode insn : method.instructions) {
                if (insn instanceof MethodInsnNode call && call.getOpcode() == INVOKESPECIAL && "<init>".equals(call.name)
                    && call.owner.equals(current.superName)) {
                    if (pool.findMethod(old.superName, "<init>", call.desc) == null) {
                        errors.add("Cannot restore superclass " + old.superName + " of " + current.name + " from " + version
                                       + ", it has no constructor " + call.desc);
                        return;
                    }

                    call.owner = old.superName;
                }
            }
        }

        current.superName = old.superName;
        memberOrigins.put(current.name + ".<superclass>:" + old.superName, version);
    }

    private void addFields(String version, ClassNode current, ClassNode old) {
        for (FieldNode field : old.fields) {
            if (!isApi(field.access)) {
                continue;
            }

            FieldNode existing = findField(current, field.name, field.desc);
            if (existing != null) {
                existing.access = widen(existing.access, field.access);
            } else if (!isInherited(current, field)) {
                current.fields.add(new FieldNode(field.access, field.name, field.desc, field.signature, field.value));
                memberOrigins.put(current.name + "." + field.name + ":" + field.desc, version);
            }
        }
    }

    private void addMethods(String version, ClassNode current, ClassNode old) {
        for (MethodNode method : old.methods) {
            if (!isApi(method.access)) {
                continue;
            }

            MethodNode existing = pool.findMethod(current.name, method.name, method.desc);
            if (existing != null) {
                existing.access = widen(existing.access, method.access);
                continue;
            } else if (!"<init>".equals(method.name) && isInherited(current, method)) {
                continue;
            }

            MethodNode copy;
            if ("<init>".equals(method.name)) {
                if (!synthetics.hasFactory(current.name)) {
                    errors.add("Cannot re-add constructor " + current.name + "." + method.name + method.desc
                                   + " from " + version + ", the class has no marker constructor");
                    continue;
                }

                copy = markerDelegate(current, method);
            } else {
                copy = new MethodNode(method.access, method.name, method.desc, method.signature,
                                      method.exceptions.toArray(String[]::new));
                method.accept(copy);
                copy.access = copy.access & ~ACC_ABSTRACT | ACC_NATIVE;
            }

            current.methods.add(copy);
            memberOrigins.put(current.name + "." + method.name + method.desc, version);
        }
    }

    /**
     * Adds the superinterfaces the old class had, if the class implements all of their abstract methods, so that
     * lambdas of the latest version stay valid.
     */
    private void addInterfaces(ClassNode current, ClassNode old) {
        for (String itf : old.interfaces) {
            if (pool.isAssignable(current.name, itf) || !pool.contains(itf)) {
                continue;
            }

            boolean implemented = true;
            for (MethodNode method : pool.get(itf).methods) {
                if ((method.access & ACC_ABSTRACT) != 0 && !isImplemented(current, method)) {
                    implemented = false;
                    break;
                }
            }

            if (implemented) {
                current.interfaces.add(itf);
            } else {
                errors.add("Cannot restore interface " + itf + " of " + current.name + ", it has unimplemented abstract methods");
            }
        }
    }

    private boolean isImplemented(ClassNode node, MethodNode abstractMethod) {
        for (String type = node.name; type != null && pool.contains(type); type = pool.superName(type)) {
            MethodNode method = pool.findMethod(type, abstractMethod.name, abstractMethod.desc);
            if (method != null && ((method.access & ACC_ABSTRACT) == 0 || (method.access & ACC_NATIVE) != 0)) {
                return true;
            }
        }

        return false;
    }

    /**
     * @return {@code true} if a field with the same name, type and static-ness resolves through the supertypes.
     */
    private boolean isInherited(ClassNode node, FieldNode field) {
        for (String supertype : supertypes(node)) {
            ClassNode superNode = pool.get(supertype);
            FieldNode inherited = superNode == null ? null : findField(superNode, field.name, field.desc);
            if (inherited != null) {
                return isApi(inherited.access) && isStatic(inherited.access) == isStatic(field.access);
            }
        }

        return false;
    }

    /**
     * @return {@code true} if a method with the same descriptor and static-ness resolves through the supertypes and,
     * unless the old method was abstract, is implemented there.
     */
    private boolean isInherited(ClassNode node, MethodNode method) {
        for (String supertype : supertypes(node)) {
            MethodNode inherited = pool.findMethod(supertype, method.name, method.desc);
            if (inherited != null) {
                return isApi(inherited.access) && isStatic(inherited.access) == isStatic(method.access)
                    && ((inherited.access & ACC_ABSTRACT) == 0 || (method.access & ACC_ABSTRACT) != 0);
            }
        }

        return false;
    }

    private List<String> supertypes(ClassNode node) {
        List<String> result = new ArrayList<>();
        collectSupertypes(node, result);
        return result;
    }

    private void collectSupertypes(ClassNode node, List<String> result) {
        List<String> direct = new ArrayList<>(node.interfaces);
        if (node.superName != null) {
            direct.add(0, node.superName);
        }

        for (String supertype : direct) {
            if (!result.contains(supertype)) {
                result.add(supertype);
                ClassNode superNode = pool.get(supertype);
                if (superNode != null) {
                    collectSupertypes(superNode, result);
                }
            }
        }
    }

    private static void widen(ClassNode current, int oldAccess) {
        current.access = widen(current.access, oldAccess);
        if (current.innerClasses != null) {
            for (InnerClassNode inner : current.innerClasses) {
                if (inner.name.equals(current.name)) {
                    inner.access = widen(inner.access, oldAccess);
                }
            }
        }
    }

    private static int widen(int access, int oldAccess) {
        return rank(oldAccess) > rank(access) ? access & ~VISIBILITY | oldAccess & VISIBILITY : access;
    }

    private static int rank(int access) {
        if ((access & ACC_PUBLIC) != 0) {
            return 3;
        } else if ((access & ACC_PROTECTED) != 0) {
            return 2;
        } else if ((access & ACC_PRIVATE) != 0) {
            return 0;
        }

        return 1;
    }

    private static MethodNode markerDelegate(ClassNode owner, MethodNode constructor) {
        MethodNode copy = new MethodNode(constructor.access, constructor.name, constructor.desc, constructor.signature,
                                         constructor.exceptions.toArray(String[]::new));
        copy.instructions.add(new VarInsnNode(ALOAD, 0));
        copy.instructions.add(new InsnNode(ACONST_NULL));
        copy.instructions.add(new MethodInsnNode(INVOKESPECIAL, owner.name, "<init>", Synthetics.MARKER_DESC, false));
        copy.instructions.add(new InsnNode(RETURN));
        return copy;
    }

    private static FieldNode findField(ClassNode node, String name, String desc) {
        for (FieldNode field : node.fields) {
            if (field.name.equals(name) && field.desc.equals(desc)) {
                return field;
            }
        }

        return null;
    }

    private static boolean isApi(int access) {
        return (access & (ACC_PUBLIC | ACC_PROTECTED)) != 0;
    }

    private static boolean isStatic(int access) {
        return (access & ACC_STATIC) != 0;
    }

    static String packageName(String internalName) {
        int index = internalName.lastIndexOf('/');
        return index < 0 ? "" : internalName.substring(0, index);
    }

    /**
     * @param internalName a class.
     * @return the old version the class was copied from, or {@code null} if it exists in the latest version.
     */
    public String classOrigin(String internalName) {
        return classOrigins.get(internalName);
    }

    /**
     * @param key a member key, {@code owner.name(desc)} for methods, {@code owner.name:desc} for fields.
     * @return the old version the member was copied from, or {@code null} if it exists in the latest version.
     */
    public String memberOrigin(String key) {
        return memberOrigins.get(key);
    }

    /**
     * @param owner a class.
     * @return the re-added fields and superclass of the class as {@code name:desc}, with the version they were copied
     * from.
     */
    public Map<String, String> fieldOrigins(String owner) {
        Map<String, String> result = new TreeMap<>();
        String prefix = owner + ".";
        for (Map.Entry<String, String> entry : memberOrigins.subMap(prefix, prefix + Character.MAX_VALUE).entrySet()) {
            String member = entry.getKey().substring(prefix.length());
            if (member.indexOf('(') < 0) {
                result.put(member, entry.getValue());
            }
        }

        return result;
    }

    public List<String> errors() {
        return errors;
    }

}
