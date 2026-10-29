package io.github.headlesshq.headlessmc.lwjgl.api;

/**
 * Marker for HeadlessMc's headless mode: the redirection API of the transformer based headlessmc-lwjgl had this name,
 * and tools like hmc-specifics detect headless mode by its presence, e.g. to not take screenshots of a game that does
 * not render. New code should check for {@link HeadlessLwjgl} instead.
 */
public final class RedirectionApi {
    private RedirectionApi() {
        throw new AssertionError();
    }

}
