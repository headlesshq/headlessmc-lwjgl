package io.github.headlesshq.headlessmc.lwjgl.runtime;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Generates unique, nonzero names and handles, e.g. for {@code glGenTextures} or {@code glfwCreateWindow}.
 */
public final class Ids {
    private static final AtomicLong NEXT = new AtomicLong(1);

    private Ids() {
        throw new AssertionError();
    }

    public static int nextInt() {
        long id = NEXT.getAndIncrement();
        return (int) (id % Integer.MAX_VALUE) + 1;
    }

    public static long nextLong() {
        return NEXT.getAndIncrement() * DummyBuffers.FAKE_ADDRESS_ALIGNMENT + DummyBuffers.FAKE_ADDRESS;
    }

}
