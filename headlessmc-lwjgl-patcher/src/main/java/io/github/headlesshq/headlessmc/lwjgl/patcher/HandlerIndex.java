package io.github.headlesshq.headlessmc.lwjgl.patcher;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Finds the runtime methods annotated with {@code @Patch} and matches them to the LWJGL methods they replace.
 */
public final class HandlerIndex {
    public static final String PATCH_ANNOTATION = "L" + Behaviour.RUNTIME + "Patch;";

    private final Map<String, Behaviour.Handler> handlers = new HashMap<>();
    private final Set<String> used = new HashSet<>();
    private final List<String> errors = new ArrayList<>();

    /**
     * @param runtimeClasses the classes of the runtime.
     * @param pool the classes that are being patched.
     */
    public HandlerIndex(List<ClassNode> runtimeClasses, ClassPool pool) {
        for (ClassNode runtimeClass : runtimeClasses) {
            for (MethodNode method : runtimeClass.methods) {
                AnnotationNode annotation = annotation(method);
                if (annotation != null) {
                    register(runtimeClass, method, annotation, pool);
                }
            }
        }
    }

    public Behaviour.Handler get(MethodInfo method) {
        Behaviour.Handler handler = handlers.get(method.key());
        if (handler != null) {
            used.add(method.key());
        }

        return handler;
    }

    /**
     * @return descriptions of handlers that do not match any LWJGL method.
     */
    public List<String> errors() {
        return errors;
    }

    private void register(ClassNode runtimeClass, MethodNode method, AnnotationNode annotation, ClassPool pool) {
        String location = runtimeClass.name + "." + method.name + method.desc;
        if ((method.access & Opcodes.ACC_STATIC) == 0 || (method.access & Opcodes.ACC_PUBLIC) == 0) {
            errors.add(location + " must be public static");
            return;
        }

        List<String> owners = new ArrayList<>();
        String name = method.name;
        for (int i = 0; i < annotation.values.size(); i += 2) {
            String key = (String) annotation.values.get(i);
            Object value = annotation.values.get(i + 1);
            if ("owner".equals(key)) {
                for (Object owner : (List<?>) value) {
                    owners.add((String) owner);
                }
            } else if ("name".equals(key) && !((String) value).isEmpty()) {
                name = (String) value;
            }
        }

        Behaviour.Handler handler = new Behaviour.Handler(runtimeClass.name, method.name, method.desc);
        for (String owner : owners) {
            String key = target(pool, owner, name, method.desc);
            if (key == null) {
                errors.add(location + " does not match any method " + owner + "." + name);
            } else if (handlers.putIfAbsent(key, handler) != null) {
                errors.add(location + " patches " + key + ", which is already patched by " + handlers.get(key).describe());
            }
        }
    }

    private static String target(ClassPool pool, String owner, String name, String handlerDesc) {
        MethodNode staticTarget = pool.findMethod(owner, name, handlerDesc);
        if (staticTarget != null && (staticTarget.access & Opcodes.ACC_STATIC) != 0) {
            return owner + "." + name + handlerDesc;
        }

        Type[] arguments = Type.getArgumentTypes(handlerDesc);
        if (arguments.length > 0 && arguments[0].getSort() == Type.OBJECT && pool.isAssignable(owner, arguments[0].getInternalName())) {
            Type[] rest = Arrays.copyOfRange(arguments, 1, arguments.length);
            String instanceDesc = Type.getMethodDescriptor(Type.getReturnType(handlerDesc), rest);
            MethodNode instanceTarget = pool.findMethod(owner, name, instanceDesc);
            if (instanceTarget != null && (instanceTarget.access & Opcodes.ACC_STATIC) == 0) {
                return owner + "." + name + instanceDesc;
            }
        }

        return null;
    }

    private static AnnotationNode annotation(MethodNode method) {
        for (List<AnnotationNode> annotations : Arrays.asList(method.invisibleAnnotations, method.visibleAnnotations)) {
            if (annotations != null) {
                for (AnnotationNode annotation : annotations) {
                    if (PATCH_ANNOTATION.equals(annotation.desc)) {
                        return annotation;
                    }
                }
            }
        }

        return null;
    }

}
