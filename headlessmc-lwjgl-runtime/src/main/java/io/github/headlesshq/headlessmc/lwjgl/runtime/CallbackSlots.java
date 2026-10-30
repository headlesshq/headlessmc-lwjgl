package io.github.headlesshq.headlessmc.lwjgl.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The function pointers registered by native callback setters like {@code nglfwSetKeyCallback(window, cbfun)}, which
 * the patcher implements with {@link #swap}: like the native function, a setter stores the new pointer and returns the
 * previous one, so that the LWJGL wrappers, which are not patched, return the previous callback object itself.
 * Pointers are kept per scope, the first parameter of the setter, e.g. the window, or {@code 0} for global setters
 * like {@code nglfwSetErrorCallback(cbfun)}.
 */
public final class CallbackSlots {
    private static final Map<Long, Map<String, Long>> SCOPES = new ConcurrentHashMap<Long, Map<String, Long>>();

    private CallbackSlots() {
        throw new AssertionError();
    }

    /**
     * @param slot the name of the setter, e.g. {@code nglfwSetKeyCallback}.
     * @param scope the scope of the callback, e.g. the window, or {@code 0}.
     * @param function the new function pointer, {@code 0} to remove the callback.
     * @return the previous function pointer, or {@code 0}.
     */
    public static long swap(String slot, long scope, long function) {
        Map<String, Long> slots = slots(scope);
        Long previous = function == 0L ? slots.remove(slot) : slots.put(slot, function);
        return previous == null ? 0L : previous;
    }

    /**
     * @param slot the name of the setter, e.g. {@code nglfwSetKeyCallback}.
     * @param scope the scope of the callback, e.g. the window, or {@code 0}.
     * @return the current function pointer, or {@code 0}.
     */
    public static long get(String slot, long scope) {
        Map<String, Long> slots = SCOPES.get(scope);
        Long function = slots == null ? null : slots.get(slot);
        return function == null ? 0L : function;
    }

    /**
     * Removes all callbacks of a scope, e.g. of a destroyed window.
     *
     * @param scope the scope of the callbacks.
     * @return the removed function pointers.
     */
    public static List<Long> clear(long scope) {
        Map<String, Long> slots = SCOPES.remove(scope);
        return slots == null ? new ArrayList<Long>() : new ArrayList<Long>(slots.values());
    }

    private static Map<String, Long> slots(long scope) {
        Map<String, Long> slots = SCOPES.get(scope);
        if (slots == null) {
            Map<String, Long> created = new ConcurrentHashMap<String, Long>();
            slots = SCOPES.putIfAbsent(scope, created);
            if (slots == null) {
                slots = created;
            }
        }

        return slots;
    }

}
