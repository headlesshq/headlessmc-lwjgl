package io.github.headlesshq.headlessmc.lwjgl3.runtime.glfw;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The state of a GLFW window: its input modes, attributes, title and close flag. Its callbacks are in
 * {@code CallbackSlots}, see {@link GlfwWindows}.
 */
public final class GlfwWindow {
    final Map<Integer, Integer> inputModes = new ConcurrentHashMap<>();
    final Map<Integer, Integer> attributes = new ConcurrentHashMap<>();
    volatile String title = "";
    volatile boolean shouldClose;
    volatile boolean cursorEntered;

}
