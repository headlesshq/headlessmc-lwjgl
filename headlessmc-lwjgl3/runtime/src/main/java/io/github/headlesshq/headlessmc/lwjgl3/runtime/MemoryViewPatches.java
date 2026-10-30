package io.github.headlesshq.headlessmc.lwjgl3.runtime;

import io.github.headlesshq.headlessmc.lwjgl.runtime.Patch;
import io.github.headlesshq.headlessmc.lwjgl.runtime.StructMemory;
import org.lwjgl.system.CustomBuffer;
import org.lwjgl.system.Struct;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Patches for the {@code MemoryUtil} methods that view memory in another form: {@code memByteBuffer} of a buffer,
 * struct or custom buffer, {@code memSlice}, {@code memDuplicate} and the strings of a given length.
 * <p>Like LWJGL, a byte view of a buffer covers its remaining elements and shares their memory if the buffer is
 * registered memory. Other buffers, like the heap buffers of {@code memAllocInt}, cannot be viewed as bytes, so the
 * view is a copy of their contents in registered memory: Minecraft fills an {@code IntBuffer} with the pixels of a glyph
 * and uploads {@code memByteBuffer(pixels)}, which must not be empty.</p>
 */
public final class MemoryViewPatches {
    private static final String MEMORY_UTIL = "org/lwjgl/system/MemoryUtil";
    private static final Charset UTF_16 = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN
        ? StandardCharsets.UTF_16LE
        : StandardCharsets.UTF_16BE;

    private MemoryViewPatches() {
        throw new AssertionError();
    }

    @Patch(owner = MEMORY_UTIL)
    public static ByteBuffer memByteBuffer(ShortBuffer buffer) {
        ByteBuffer view = view(buffer, Short.BYTES);
        if (view == null) {
            view = copy(buffer, Short.BYTES);
            view.asShortBuffer().put(buffer.duplicate());
        }

        return view;
    }

    @Patch(owner = MEMORY_UTIL)
    public static ByteBuffer memByteBuffer(CharBuffer buffer) {
        ByteBuffer view = view(buffer, Character.BYTES);
        if (view == null) {
            view = copy(buffer, Character.BYTES);
            view.asCharBuffer().put(buffer.duplicate());
        }

        return view;
    }

    @Patch(owner = MEMORY_UTIL)
    public static ByteBuffer memByteBuffer(IntBuffer buffer) {
        ByteBuffer view = view(buffer, Integer.BYTES);
        if (view == null) {
            view = copy(buffer, Integer.BYTES);
            view.asIntBuffer().put(buffer.duplicate());
        }

        return view;
    }

    @Patch(owner = MEMORY_UTIL)
    public static ByteBuffer memByteBuffer(LongBuffer buffer) {
        ByteBuffer view = view(buffer, Long.BYTES);
        if (view == null) {
            view = copy(buffer, Long.BYTES);
            view.asLongBuffer().put(buffer.duplicate());
        }

        return view;
    }

    @Patch(owner = MEMORY_UTIL)
    public static ByteBuffer memByteBuffer(FloatBuffer buffer) {
        ByteBuffer view = view(buffer, Float.BYTES);
        if (view == null) {
            view = copy(buffer, Float.BYTES);
            view.asFloatBuffer().put(buffer.duplicate());
        }

        return view;
    }

    @Patch(owner = MEMORY_UTIL)
    public static ByteBuffer memByteBuffer(DoubleBuffer buffer) {
        ByteBuffer view = view(buffer, Double.BYTES);
        if (view == null) {
            view = copy(buffer, Double.BYTES);
            view.asDoubleBuffer().put(buffer.duplicate());
        }

        return view;
    }

    /**
     * The remaining elements of the custom buffer, which has registered memory unless it wraps foreign memory.
     */
    @Patch(owner = MEMORY_UTIL)
    public static ByteBuffer memByteBuffer(CustomBuffer<?> buffer) {
        return MemoryAccessPatches.memByteBuffer(buffer.address(), buffer.remaining() * buffer.sizeof());
    }

    @Patch(owner = MEMORY_UTIL)
    public static ByteBuffer memByteBuffer(Struct<?> value) {
        return MemoryAccessPatches.memByteBuffer(value.address(), value.sizeof());
    }

    @Patch(owner = MEMORY_UTIL)
    public static ByteBuffer memSlice(ByteBuffer buffer) {
        return buffer.slice().order(buffer.order());
    }

    @Patch(owner = MEMORY_UTIL)
    public static ShortBuffer memSlice(ShortBuffer buffer) {
        return buffer.slice();
    }

    @Patch(owner = MEMORY_UTIL)
    public static CharBuffer memSlice(CharBuffer buffer) {
        return buffer.slice();
    }

    @Patch(owner = MEMORY_UTIL)
    public static IntBuffer memSlice(IntBuffer buffer) {
        return buffer.slice();
    }

    @Patch(owner = MEMORY_UTIL)
    public static LongBuffer memSlice(LongBuffer buffer) {
        return buffer.slice();
    }

    @Patch(owner = MEMORY_UTIL)
    public static FloatBuffer memSlice(FloatBuffer buffer) {
        return buffer.slice();
    }

    @Patch(owner = MEMORY_UTIL)
    public static DoubleBuffer memSlice(DoubleBuffer buffer) {
        return buffer.slice();
    }

    @Patch(owner = MEMORY_UTIL)
    public static ByteBuffer memDuplicate(ByteBuffer buffer) {
        return buffer.duplicate().order(buffer.order());
    }

    @Patch(owner = MEMORY_UTIL)
    public static ShortBuffer memDuplicate(ShortBuffer buffer) {
        return buffer.duplicate();
    }

    @Patch(owner = MEMORY_UTIL)
    public static CharBuffer memDuplicate(CharBuffer buffer) {
        return buffer.duplicate();
    }

    @Patch(owner = MEMORY_UTIL)
    public static IntBuffer memDuplicate(IntBuffer buffer) {
        return buffer.duplicate();
    }

    @Patch(owner = MEMORY_UTIL)
    public static LongBuffer memDuplicate(LongBuffer buffer) {
        return buffer.duplicate();
    }

    @Patch(owner = MEMORY_UTIL)
    public static FloatBuffer memDuplicate(FloatBuffer buffer) {
        return buffer.duplicate();
    }

    @Patch(owner = MEMORY_UTIL)
    public static DoubleBuffer memDuplicate(DoubleBuffer buffer) {
        return buffer.duplicate();
    }

    /**
     * @param length the number of bytes, without a terminator.
     */
    @Patch(owner = MEMORY_UTIL)
    public static String memASCII(long address, int length) {
        return string(address, length, StandardCharsets.US_ASCII);
    }

    @Patch(owner = MEMORY_UTIL)
    public static String memASCIISafe(long address, int length) {
        return address == 0L ? null : memASCII(address, length);
    }

    /**
     * @param length the number of bytes, without a terminator.
     */
    @Patch(owner = MEMORY_UTIL)
    public static String memUTF8(long address, int length) {
        return string(address, length, StandardCharsets.UTF_8);
    }

    @Patch(owner = MEMORY_UTIL)
    public static String memUTF8Safe(long address, int length) {
        return address == 0L ? null : memUTF8(address, length);
    }

    @Patch(owner = MEMORY_UTIL)
    public static String memUTF16(long address) {
        return StructMemory.utf16(address);
    }

    @Patch(owner = MEMORY_UTIL)
    public static String memUTF16Safe(long address) {
        return address == 0L ? null : memUTF16(address);
    }

    /**
     * @param length the number of chars, without a terminator.
     */
    @Patch(owner = MEMORY_UTIL)
    public static String memUTF16(long address, int length) {
        return string(address, length * Character.BYTES, UTF_16);
    }

    @Patch(owner = MEMORY_UTIL)
    public static String memUTF16Safe(long address, int length) {
        return address == 0L ? null : memUTF16(address, length);
    }

    /**
     * @return the remaining elements of a direct buffer over registered memory as bytes, or {@code null}.
     */
    private static ByteBuffer view(Buffer buffer, int elementSize) {
        if (!buffer.isDirect()) {
            return null;
        }

        return StructMemory.view(StructMemory.address(buffer) + (long) buffer.position() * elementSize,
                                 buffer.remaining() * elementSize);
    }

    /**
     * @return new registered memory in native byte order for the remaining elements of a buffer.
     */
    private static ByteBuffer copy(Buffer buffer, int elementSize) {
        int bytes = buffer.remaining() * elementSize;
        ByteBuffer copy = StructMemory.allocate(bytes).order(ByteOrder.nativeOrder());
        copy.limit(bytes);
        return copy;
    }

    private static String string(long address, int bytes, Charset charset) {
        ByteBuffer view = StructMemory.view(address, bytes);
        if (view == null) {
            return "";
        }

        byte[] data = new byte[bytes];
        view.get(data);
        return new String(data, charset);
    }

}
