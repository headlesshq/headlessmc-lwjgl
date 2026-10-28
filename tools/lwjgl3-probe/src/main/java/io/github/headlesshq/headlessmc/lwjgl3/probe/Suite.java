package io.github.headlesshq.headlessmc.lwjgl3.probe;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * A group of LWJGL bindings that are probed in one worker process, together with the native state they need.
 */
abstract class Suite {
    /**
     * @return the name of the suite, also the name of the recording file.
     */
    abstract String name();

    /**
     * Creates the native state the probed functions need, e.g. a window with a current context.
     */
    abstract void setUp();

    /**
     * @return the binding classes whose public static methods are probed.
     */
    abstract List<Class<?>> classes();

    /**
     * @return the classes whose {@code int} constants are used as candidates for enum parameters, most important first.
     */
    abstract List<Class<?>> enumClasses();

    /**
     * @return the names of constants used for enum parameters that are not swept completely, e.g. the target of
     * {@code glGetTexParameteri(target, pname)}.
     */
    abstract List<String> contextEnums();

    /**
     * @return native handles of the fixture, keyed by the {@code @NativeType} of the parameters they are passed to.
     */
    abstract Map<String, Long> handles();

    /**
     * @return object names of the fixture, keyed by the name of the parameters they are passed to.
     */
    abstract Map<String, Integer> objects();

    /**
     * @return the candidates for string parameters.
     */
    abstract List<String> strings();

    /**
     * Fetches and clears the error state after a call.
     *
     * @param method the method that was called.
     * @return the name of the error, or {@code null} if the call caused no error.
     */
    abstract String error(Method method);

    /**
     * @return information about the native implementation, written into the header of the recording.
     */
    abstract Map<String, Object> info();

    /**
     * @param internalName the internal name of a capabilities class, e.g. {@code org/lwjgl/opengl/GLCapabilities}.
     * @return the current instance of the capabilities class, or {@code null} if the suite does not use it.
     */
    abstract Object capabilities(String internalName);

    /**
     * @return the capabilities whose fields hold the function pointers of the JNI natives of the suite, or
     * {@code null} if they are not checked.
     */
    Object nativeCapabilities() {
        return null;
    }

    /**
     * @param target a target.
     * @return the first function pointer of the target that is not available, or {@code null} if all are.
     */
    String unavailableFunction(Target target) {
        for (String function : target.functions) {
            if (function.startsWith(Target.NATIVE_FUNCTION)) {
                Object capabilities = nativeCapabilities();
                String name = function.substring(Target.NATIVE_FUNCTION.length());
                try {
                    if (capabilities != null && capabilities.getClass().getField(name).getLong(capabilities) == 0L) {
                        return function;
                    }
                } catch (ReflectiveOperationException ignored) {
                }

                continue;
            }

            int dot = function.lastIndexOf('.');
            Object capabilities = capabilities(function.substring(0, dot));
            try {
                if (capabilities == null || capabilities.getClass().getField(function.substring(dot + 1)).getLong(capabilities) == 0L) {
                    return function;
                }
            } catch (ReflectiveOperationException e) {
                return function;
            }
        }

        return null;
    }

    /**
     * @return names of methods that must not be called, because they destroy the fixture, block, or leak user data.
     */
    abstract Set<String> denied();

    static List<Suite> all() {
        return List.of(new GlSuite(), new AlSuite());
    }

    static Suite byName(String name) {
        for (Suite suite : all()) {
            if (suite.name().equals(name)) {
                return suite;
            }
        }

        throw new IllegalArgumentException("Unknown suite " + name);
    }

    /**
     * @param anchor a class of the jar.
     * @param packageName the package to list, e.g. {@code org.lwjgl.opengl}.
     * @return the public top level classes of the package in the jar of the anchor class, sorted by name.
     */
    static List<Class<?>> classesOf(Class<?> anchor, String packageName) {
        List<Class<?>> result = new ArrayList<>();
        String prefix = packageName.replace('.', '/') + "/";
        try (JarFile jar = new JarFile(Path.of(anchor.getProtectionDomain().getCodeSource().getLocation().toURI()).toFile())) {
            for (JarEntry entry : jar.stream().toList()) {
                String name = entry.getName();
                if (name.startsWith(prefix) && name.endsWith(".class") && name.indexOf('/', prefix.length()) < 0
                    && name.indexOf('$') < 0 && !name.startsWith("META-INF/")) {
                    Class<?> type = Class.forName(name.substring(0, name.length() - 6).replace('/', '.'), false, anchor.getClassLoader());
                    if (Modifier.isPublic(type.getModifiers())) {
                        result.add(type);
                    }
                }
            }
        } catch (IOException | URISyntaxException | ClassNotFoundException e) {
            throw new IllegalStateException("Failed to list " + packageName, e);
        }

        result.sort(Comparator.comparing(Class::getName));
        return result;
    }

    /**
     * @return the {@code public static final int} constants of the given classes, the first name wins for every value.
     */
    static Map<Integer, String> intConstants(List<Class<?>> classes) {
        Map<Integer, String> result = new LinkedHashMap<>();
        for (Class<?> type : classes) {
            for (Field field : type.getFields()) {
                int modifiers = field.getModifiers();
                if (field.getType() == int.class && Modifier.isStatic(modifiers) && Modifier.isFinal(modifiers)) {
                    try {
                        result.putIfAbsent(field.getInt(null), field.getName());
                    } catch (IllegalAccessException e) {
                        throw new IllegalStateException(e);
                    }
                }
            }
        }

        return result;
    }

}
