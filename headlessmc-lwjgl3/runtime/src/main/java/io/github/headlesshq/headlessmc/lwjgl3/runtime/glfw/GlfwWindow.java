package io.github.headlesshq.headlessmc.lwjgl3.runtime.glfw;

import org.lwjgl.glfw.GLFWCharCallbackI;
import org.lwjgl.glfw.GLFWCharModsCallbackI;
import org.lwjgl.glfw.GLFWCursorEnterCallbackI;
import org.lwjgl.glfw.GLFWCursorPosCallbackI;
import org.lwjgl.glfw.GLFWFramebufferSizeCallbackI;
import org.lwjgl.glfw.GLFWKeyCallbackI;
import org.lwjgl.glfw.GLFWMouseButtonCallbackI;
import org.lwjgl.glfw.GLFWScrollCallbackI;
import org.lwjgl.glfw.GLFWWindowCloseCallbackI;
import org.lwjgl.glfw.GLFWWindowFocusCallbackI;
import org.lwjgl.glfw.GLFWWindowSizeCallbackI;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The state of a GLFW window: its callbacks, input modes, attributes, title and close flag.
 */
public final class GlfwWindow {
    final Map<Integer, Integer> inputModes = new ConcurrentHashMap<>();
    final Map<Integer, Integer> attributes = new ConcurrentHashMap<>();
    volatile GLFWKeyCallbackI key;
    volatile GLFWCharCallbackI character;
    volatile GLFWCharModsCallbackI charMods;
    volatile GLFWMouseButtonCallbackI mouseButton;
    volatile GLFWCursorPosCallbackI cursorPos;
    volatile GLFWCursorEnterCallbackI cursorEnter;
    volatile GLFWScrollCallbackI scroll;
    volatile GLFWWindowSizeCallbackI windowSize;
    volatile GLFWFramebufferSizeCallbackI framebufferSize;
    volatile GLFWWindowFocusCallbackI focus;
    volatile GLFWWindowCloseCallbackI close;
    volatile String title = "";
    volatile boolean shouldClose;
    volatile boolean cursorEntered;

}
