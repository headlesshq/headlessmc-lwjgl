package io.github.headlesshq.headlessmc.lwjgl.api;

/**
 * Entry point to control a patched, headless LWJGL from the outside.
 * <p>Input injected through this API is delivered to the application the same way real input would be:
 * the polling state (e.g. {@code glfwGetKey}) changes immediately and events are dispatched to the registered
 * callbacks the next time the application pumps its event queue (e.g. {@code glfwPollEvents}).</p>
 */
public interface HeadlessLwjgl {
    /**
     * @return the keyboard of the headless LWJGL.
     */
    Keyboard keyboard();

    /**
     * @return the mouse of the headless LWJGL.
     */
    Mouse mouse();

    /**
     * @return the window of the headless LWJGL.
     */
    Window window();

    /**
     * Returns the {@link HeadlessLwjgl} implementation of the patched LWJGL.
     *
     * @return the implementation.
     * @throws IllegalStateException if no patched LWJGL is present.
     */
    static HeadlessLwjgl get() {
        return HeadlessLwjglLookup.get();
    }

    /**
     * Registers the implementation explicitly.
     * Called by the patched LWJGL, for environments where {@link java.util.ServiceLoader} cannot find it,
     * e.g. because the API and LWJGL are loaded by different class loaders.
     *
     * @param implementation the implementation to return from {@link #get()}.
     */
    static void register(HeadlessLwjgl implementation) {
        HeadlessLwjglLookup.register(implementation);
    }

}
