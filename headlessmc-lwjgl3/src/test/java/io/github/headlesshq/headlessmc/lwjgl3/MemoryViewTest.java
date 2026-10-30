package io.github.headlesshq.headlessmc.lwjgl3;

import org.junit.jupiter.api.Test;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL15C;
import org.lwjgl.opengl.GL30C;
import org.lwjgl.stb.STBImageResize;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The {@code MemoryUtil} methods that view memory in another form, and other calls whose result Minecraft uses.
 */
class MemoryViewTest {
    /**
     * Minecraft's {@code UnihexProvider} fills the pixels of a glyph into {@code memAllocInt} and uploads
     * {@code memByteBuffer(pixels)}, which failed as an empty buffer when a splash text needed a Unifont glyph.
     */
    @Test
    void byteViewOfAnIntBufferHasItsContents() {
        IntBuffer pixels = MemoryUtil.memAllocInt(16 * 16);
        for (int i = 0; i < pixels.capacity(); i++) {
            pixels.put(i, i);
        }

        ByteBuffer bytes = MemoryUtil.memByteBuffer(pixels);
        assertEquals(16 * 16 * Integer.BYTES, bytes.remaining());
        assertEquals(ByteOrder.nativeOrder(), bytes.order());
        assertEquals(255, bytes.getInt(255 * Integer.BYTES));
        MemoryUtil.memFree(pixels);
    }

    /**
     * Raw allocations are registered memory, a byte view of a buffer over them shares their memory.
     */
    @Test
    void byteViewOfRegisteredMemorySharesIt() {
        long address = MemoryUtil.nmemAlloc(16);
        IntBuffer ints = MemoryUtil.memIntBuffer(address, 4);
        ints.position(1);
        ByteBuffer bytes = MemoryUtil.memByteBuffer(ints);
        assertEquals(3 * Integer.BYTES, bytes.remaining());
        bytes.putInt(0, 7);
        assertEquals(7, ints.get(1));
        assertEquals(7, MemoryUtil.memGetInt(address + Integer.BYTES));
        MemoryUtil.nmemFree(address);
    }

    @Test
    void slicesAndDuplicatesKeepTheirContents() {
        ByteBuffer buffer = MemoryUtil.memAlloc(8).order(ByteOrder.LITTLE_ENDIAN);
        buffer.put(0, (byte) 1).put(4, (byte) 5).position(4);
        ByteBuffer slice = MemoryUtil.memSlice(buffer);
        assertEquals(4, slice.remaining());
        assertEquals(5, slice.get(0));
        assertEquals(ByteOrder.LITTLE_ENDIAN, slice.order());
        ByteBuffer duplicate = MemoryUtil.memDuplicate(buffer);
        assertEquals(4, duplicate.position());
        assertEquals(1, duplicate.get(0));
        MemoryUtil.memFree(buffer);
    }

    /**
     * Minecraft 26.2 reads the names of audio devices with {@code memASCII(address, length)}.
     */
    @Test
    void stringsOfAGivenLengthAreDecoded() {
        long address = MemoryUtil.nmemAlloc(16);
        ByteBuffer memory = MemoryUtil.memByteBuffer(address, 16);
        memory.put("HeadlessMc".getBytes(java.nio.charset.StandardCharsets.US_ASCII));
        assertEquals("Headless", MemoryUtil.memASCII(address, 8));
        assertEquals("HeadlessMc", MemoryUtil.memUTF8(address, 10));
        assertNull(MemoryUtil.memASCIISafe(0L, 4));

        memory.clear();
        memory.putChar(0, 'M').putChar(2, 'c').putChar(4, (char) 0);
        assertEquals("Mc", MemoryUtil.memUTF16(address));
        assertEquals("M", MemoryUtil.memUTF16(address, 1));
        MemoryUtil.nmemFree(address);
    }

    /**
     * Forge's early display and Minecraft encode names on the stack, e.g. {@code stack.UTF8(title)}.
     */
    @Test
    void stackEncodesStringsAndValues() {
        try (org.lwjgl.system.MemoryStack stack = org.lwjgl.system.MemoryStack.stackPush()) {
            ByteBuffer ascii = stack.ASCII("Mc");
            assertEquals(3, ascii.remaining());
            assertEquals('M', ascii.get(0));
            assertEquals(0, ascii.get(2));
            assertEquals(2, stack.UTF8("Mc", false).remaining());
            assertEquals("Mc", MemoryUtil.memUTF16(org.lwjgl.system.MemoryStack.stackUTF16("Mc", false)));
            assertNull(stack.UTF8Safe(null));

            IntBuffer ints = org.lwjgl.system.MemoryStack.stackInts(4, 5);
            assertEquals(2, ints.remaining());
            assertEquals(5, ints.get(1));
            assertEquals(7, org.lwjgl.system.MemoryStack.stackBytes((byte) 7).get(0));
            org.lwjgl.PointerBuffer pointers = stack.pointers(1L, 2L, 3L);
            assertEquals(3, pointers.remaining());
            assertEquals(3L, pointers.get(2));
        }
    }

    /**
     * Minecraft 26.x maps its transient buffers with {@code nglMapBufferRange} and copies from the mapping.
     */
    @Test
    void mappedBuffersHaveMemory() {
        assertTrue(GLFW.glfwInit());
        long window = GLFW.glfwCreateWindow(854, 480, "Map", 0L, 0L);
        try {
            GLFW.glfwMakeContextCurrent(window);
            GL.createCapabilities();
            int buffer = GL15C.glGenBuffers();
            GL15C.glBindBuffer(GL15C.GL_ARRAY_BUFFER, buffer);
            GL15C.glBufferData(GL15C.GL_ARRAY_BUFFER, 64L, GL15C.GL_DYNAMIC_DRAW);
            long address = GL30C.nglMapBufferRange(GL15C.GL_ARRAY_BUFFER, 0L, 64L, GL30C.GL_MAP_WRITE_BIT);
            assertNotEquals(0L, address);
            MemoryUtil.memPutInt(address + 60, 123);
            assertEquals(123, MemoryUtil.memGetInt(address + 60));
            assertTrue(GL15C.glUnmapBuffer(GL15C.GL_ARRAY_BUFFER));
            GL15C.glDeleteBuffers(buffer);
        } finally {
            GLFW.glfwDestroyWindow(window);
        }
    }

    /**
     * Minecraft resizes images with {@code nstbir_resize_uint8} before LWJGL 3.4 and
     * {@code nstbir_resize_uint8_linear} since, which return 1 and the output on success.
     */
    @Test
    void imageResizingSucceeds() {
        ByteBuffer input = MemoryUtil.memAlloc(4 * 4 * 4);
        ByteBuffer output = MemoryUtil.memAlloc(2 * 2 * 4);
        long result = STBImageResize.nstbir_resize_uint8_linear(MemoryUtil.memAddress(input), 4, 4, 0,
                                                                MemoryUtil.memAddress(output), 2, 2, 0,
                                                                STBImageResize.STBIR_RGBA);
        assertEquals(MemoryUtil.memAddress(output), result);
        assertSame(output, STBImageResize.stbir_resize_uint8_linear(input, 4, 4, 0, output, 2, 2, 0, STBImageResize.STBIR_RGBA));
        MemoryUtil.memFree(input);
        MemoryUtil.memFree(output);
    }

}
