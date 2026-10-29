package io.github.headlesshq.headlessmc.lwjgl3;

import org.junit.jupiter.api.Test;
import org.lwjgl.stb.STBIEOFCallback;
import org.lwjgl.stb.STBIIOCallbacks;
import org.lwjgl.stb.STBIReadCallback;
import org.lwjgl.stb.STBISkipCallback;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.Callback;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Callbacks are Java instances behind fake addresses, which native functions implemented in Java invoke.
 */
class CallbackTest {
    private static final byte[] PNG = {
        (byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n', 0, 0, 0, 13, 'I', 'H', 'D', 'R',
        0, 0, 0, 64, 0, 0, 0, 32, 8, 6, 0, 0, 0
    };

    @Test
    void callbacksDelegateToTheirInstance() {
        STBIEOFCallback eof = STBIEOFCallback.create(user -> 1);
        assertNotEquals(0L, eof.address());
        assertEquals(1, eof.invoke(0L));
        STBIEOFCallback same = STBIEOFCallback.create(eof.address());
        assertEquals(1, same.invoke(0L));
        assertNotNull(Callback.get(eof.address()));
        eof.free();
    }

    @Test
    void imageInfoIsReadWithCallbacks() {
        int[] offset = {0};
        try (STBIReadCallback read = STBIReadCallback.create((user, data, size) -> {
                ByteBuffer target = STBIReadCallback.getData(data, size);
                int count = Math.min(size, PNG.length - offset[0]);
                target.put(PNG, offset[0], count);
                offset[0] += count;
                return count;
            });
             STBISkipCallback skip = STBISkipCallback.create((user, n) -> offset[0] += n);
             STBIEOFCallback eof = STBIEOFCallback.create(user -> offset[0] >= PNG.length ? 1 : 0);
             MemoryStack stack = MemoryStack.stackPush()) {
            STBIIOCallbacks callbacks = STBIIOCallbacks.malloc(stack).read(read).skip(skip).eof(eof);
            IntBuffer x = stack.mallocInt(1);
            IntBuffer y = stack.mallocInt(1);
            IntBuffer channels = stack.mallocInt(1);
            assertTrue(STBImage.stbi_info_from_callbacks(callbacks, 0L, x, y, channels));
            assertEquals(64, x.get(0));
            assertEquals(32, y.get(0));
            assertEquals(4, channels.get(0));
        }
    }

}
