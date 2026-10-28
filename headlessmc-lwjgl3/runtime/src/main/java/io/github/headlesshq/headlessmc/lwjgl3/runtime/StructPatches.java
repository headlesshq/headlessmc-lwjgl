package io.github.headlesshq.headlessmc.lwjgl3.runtime;

import org.lwjgl.system.CustomBuffer;
import org.lwjgl.system.Struct;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Patches for the {@code wrap} methods that LWJGL versions before 3.4 had on {@code Struct} and {@code CustomBuffer},
 * which structs re-added from those versions call to create instances at an address. They call the synthetic
 * {@code hmc$wrap} factories the patcher adds to every struct and buffer class.
 */
public final class StructPatches {
    private static final String STRUCT = "org/lwjgl/system/Struct";
    private static final String POINTER = "org/lwjgl/system/Pointer$Default";
    private static final String WRAPPER = "hmc$wrap";
    private static final Map<Class<?>, Method> WRAPPERS = new ConcurrentHashMap<>();

    private StructPatches() {
        throw new AssertionError();
    }

    @Patch(owner = STRUCT)
    public static Struct<?> wrap(Class<?> type, long address) {
        return wrap(type, address, (ByteBuffer) null);
    }

    @Patch(owner = STRUCT)
    public static Struct<?> wrap(Class<?> type, long address, ByteBuffer container) {
        return (Struct<?>) invoke(type, new Class<?>[]{long.class, ByteBuffer.class}, address, container);
    }

    @Patch(owner = STRUCT)
    public static Struct<?> wrap(Class<?> type, Struct<?> struct) {
        return wrap(type, struct.address(), (ByteBuffer) null);
    }

    @Patch(owner = POINTER)
    public static CustomBuffer<?> wrap(Class<?> type, long address, int capacity) {
        return wrap(type, address, capacity, null);
    }

    @Patch(owner = POINTER)
    public static CustomBuffer<?> wrap(Class<?> type, long address, int capacity, ByteBuffer container) {
        return (CustomBuffer<?>) invoke(type, new Class<?>[]{long.class, int.class, ByteBuffer.class}, address, capacity, container);
    }

    @Patch(owner = POINTER)
    public static CustomBuffer<?> wrap(Class<?> type, CustomBuffer<?> buffer) {
        return wrap(type, buffer.address0(), buffer.capacity(), null);
    }

    private static Object invoke(Class<?> type, Class<?>[] parameters, Object... arguments) {
        try {
            Method wrapper = WRAPPERS.get(type);
            if (wrapper == null) {
                wrapper = type.getMethod(WRAPPER, parameters);
                WRAPPERS.put(type, wrapper);
            }

            return wrapper.invoke(null, arguments);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new IllegalStateException(type.getName() + " has not been patched by HeadlessMc", e);
        } catch (InvocationTargetException e) {
            throw new IllegalStateException("Failed to wrap an address as " + type.getName(), e.getCause());
        }
    }

}
