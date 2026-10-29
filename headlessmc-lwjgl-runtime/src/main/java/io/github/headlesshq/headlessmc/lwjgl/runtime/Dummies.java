package io.github.headlesshq.headlessmc.lwjgl.runtime;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * Creates dummy instances of patched LWJGL classes.
 * The patcher adds a static factory method named {@value #FACTORY} to every patched class it can instantiate,
 * which creates an instance without running any of the original constructors.
 */
public final class Dummies {
    public static final String FACTORY = "hmc$dummy";

    private static final Map<Class<?>, Object> PROXIES = new ConcurrentHashMap<>();

    private Dummies() {
        throw new AssertionError();
    }

    /**
     * @param type a patched LWJGL class.
     * @param <T> the type of the class.
     * @return a new dummy instance of the class.
     */
    public static <T> T create(Class<T> type) {
        try {
            Method factory = type.getDeclaredMethod(FACTORY);
            return type.cast(factory.invoke(null));
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(type.getName() + " has not been patched by HeadlessMc", e);
        }
    }

    /**
     * Returns a proxy for an LWJGL interface, whose methods return {@code 0}, {@code false}, {@code ""} or
     * {@code null} and whose {@code equals}/{@code hashCode} are identity based.
     * One proxy is created per interface and reused.
     *
     * @param type the interface.
     * @return the proxy.
     */
    public static Object proxy(Class<?> type) {
        return PROXIES.computeIfAbsent(type, Dummies::createProxy);
    }

    private static Object createProxy(Class<?> type) {
        return Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (proxy, method, args) -> {
            switch (method.getName()) {
                case "equals":
                    return args != null && args.length == 1 && proxy == args[0];
                case "hashCode":
                    return System.identityHashCode(proxy);
                case "toString":
                    return "HeadlessMc dummy " + type.getName();
                default:
                    return defaultValue(method.getReturnType());
            }
        });
    }

    private static Object defaultValue(Class<?> type) {
        if (type == boolean.class) {
            return false;
        } else if (type == byte.class) {
            return (byte) 0;
        } else if (type == short.class) {
            return (short) 0;
        } else if (type == char.class) {
            return (char) 0;
        } else if (type == int.class) {
            return 0;
        } else if (type == long.class) {
            return 0L;
        } else if (type == float.class) {
            return 0.0f;
        } else if (type == double.class) {
            return 0.0;
        } else if (type == String.class || type == CharSequence.class) {
            return "";
        }

        return null;
    }

    /**
     * Fills the fields of a capabilities instance, like {@code GLCapabilities} or {@code ALCCapabilities}.
     * Every public {@code boolean} field is set according to the given predicate, every public {@code long} field,
     * which holds a function address, is set to a fake nonzero address, so that the function counts as available.
     * The patcher removes {@code final} from these fields.
     *
     * @param capabilities the capabilities instance.
     * @param enabled decides for the name of a {@code boolean} field whether it is supported.
     * @param <T> the type of the capabilities instance.
     * @return the given capabilities instance.
     */
    public static <T> T fillCapabilities(T capabilities, Predicate<String> enabled) {
        for (Class<?> type = capabilities.getClass(); type != null && type != Object.class; type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                int modifiers = field.getModifiers();
                if (Modifier.isStatic(modifiers) || !Modifier.isPublic(modifiers)
                    || field.getType() != boolean.class && field.getType() != long.class) {
                    continue;
                }

                try {
                    if (field.getType() == boolean.class) {
                        field.setBoolean(capabilities, enabled.test(field.getName()));
                    } else if (field.getType() == long.class) {
                        field.setLong(capabilities, DummyBuffers.FAKE_ADDRESS);
                    }
                } catch (ReflectiveOperationException | RuntimeException e) {
                    Log.warnOnce("capabilities:" + type.getName() + "." + field.getName(),
                                 "Failed to set capability " + type.getName() + "." + field.getName() + ": " + e);
                }
            }
        }

        return capabilities;
    }

}
