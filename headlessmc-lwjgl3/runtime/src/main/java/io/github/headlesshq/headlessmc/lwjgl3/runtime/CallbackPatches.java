package io.github.headlesshq.headlessmc.lwjgl3.runtime;

import io.github.headlesshq.headlessmc.lwjgl.runtime.Patch;
import org.lwjgl.system.Callback;
import org.lwjgl.system.CallbackI;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Patches for LWJGL's upcall registry: instead of native function pointers, callbacks get unique fake addresses
 * that map back to the Java instance, so that {@code Callback.get}, the {@code create} factories of callback classes
 * and native functions implemented in Java can invoke them. Like native upcalls, a callback stays registered until it
 * is freed.
 */
public final class CallbackPatches {
    private static final String UPCALLS = "org/lwjgl/system/Upcalls";
    private static final long BASE = 0x7E00_0000_0000L;
    private static final long ALIGNMENT = 16L;
    private static final AtomicLong NEXT = new AtomicLong();
    private static final Map<Long, Object> CALLBACKS = new ConcurrentHashMap<>();

    private CallbackPatches() {
        throw new AssertionError();
    }

    @Patch(owner = UPCALLS)
    public static long upcallCreate(Callback.Descriptor descriptor, Object instance) {
        long address = BASE + NEXT.getAndIncrement() * ALIGNMENT;
        CALLBACKS.put(address, instance);
        return address;
    }

    @Patch(owner = UPCALLS)
    @SuppressWarnings("unchecked")
    public static <T extends CallbackI> T upcallGet(long functionPointer) {
        return (T) CALLBACKS.get(functionPointer);
    }

    @Patch(owner = UPCALLS)
    public static void upcallFree(long functionPointer) {
        CALLBACKS.remove(functionPointer);
    }

}
