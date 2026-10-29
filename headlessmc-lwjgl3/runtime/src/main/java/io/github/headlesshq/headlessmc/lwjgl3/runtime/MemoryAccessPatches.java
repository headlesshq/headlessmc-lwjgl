package io.github.headlesshq.headlessmc.lwjgl3.runtime;

import io.github.headlesshq.headlessmc.lwjgl.runtime.DummyBuffers;
import io.github.headlesshq.headlessmc.lwjgl.runtime.Patch;
import io.github.headlesshq.headlessmc.lwjgl.runtime.StructMemory;

import org.lwjgl.system.Struct;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import java.nio.charset.StandardCharsets;

/**
 * Patches for the address based accessors of {@code org.lwjgl.system.MemoryUtil}, which only touch the struct memory
 * of {@link StructMemory}: other addresses read as 0 and ignore writes, strings at them are empty, and buffers that are
 * not struct memory have a fake address. {@code C long} and pointers are 64 bit. {@code memRealloc} keeps the contents
 * and the position of the buffer, like the real one.
 */
public final class MemoryAccessPatches {
    private static final String MEMORY_UTIL = "org/lwjgl/system/MemoryUtil";

    private MemoryAccessPatches() {
        throw new AssertionError();
    }

    @Patch(owner = MEMORY_UTIL)
    public static boolean memGetBoolean(long address) {
        return StructMemory.getByte(address) != 0;
    }

    @Patch(owner = MEMORY_UTIL)
    public static void memPutBoolean(long address, boolean value) {
        StructMemory.putByte(address, (byte) (value ? 1 : 0));
    }

    @Patch(owner = MEMORY_UTIL)
    public static byte memGetByte(long address) {
        return StructMemory.getByte(address);
    }

    @Patch(owner = MEMORY_UTIL)
    public static void memPutByte(long address, byte value) {
        StructMemory.putByte(address, value);
    }

    @Patch(owner = MEMORY_UTIL)
    public static char memGetChar(long address) {
        return (char) StructMemory.getShort(address);
    }

    @Patch(owner = MEMORY_UTIL)
    public static void memPutChar(long address, char value) {
        StructMemory.putShort(address, (short) value);
    }

    @Patch(owner = MEMORY_UTIL)
    public static char memGetCharUnaligned(long address) {
        return (char) StructMemory.getShort(address);
    }

    @Patch(owner = MEMORY_UTIL)
    public static void memPutCharUnaligned(long address, char value) {
        StructMemory.putShort(address, (short) value);
    }

    @Patch(owner = MEMORY_UTIL)
    public static short memGetShort(long address) {
        return StructMemory.getShort(address);
    }

    @Patch(owner = MEMORY_UTIL)
    public static void memPutShort(long address, short value) {
        StructMemory.putShort(address, value);
    }

    @Patch(owner = MEMORY_UTIL)
    public static short memGetShortUnaligned(long address) {
        return StructMemory.getShort(address);
    }

    @Patch(owner = MEMORY_UTIL)
    public static void memPutShortUnaligned(long address, short value) {
        StructMemory.putShort(address, value);
    }

    @Patch(owner = MEMORY_UTIL)
    public static int memGetInt(long address) {
        return StructMemory.getInt(address);
    }

    @Patch(owner = MEMORY_UTIL)
    public static void memPutInt(long address, int value) {
        StructMemory.putInt(address, value);
    }

    @Patch(owner = MEMORY_UTIL)
    public static int memGetIntUnaligned(long address) {
        return StructMemory.getInt(address);
    }

    @Patch(owner = MEMORY_UTIL)
    public static void memPutIntUnaligned(long address, int value) {
        StructMemory.putInt(address, value);
    }

    @Patch(owner = MEMORY_UTIL)
    public static long memGetLong(long address) {
        return StructMemory.getLong(address);
    }

    @Patch(owner = MEMORY_UTIL)
    public static void memPutLong(long address, long value) {
        StructMemory.putLong(address, value);
    }

    @Patch(owner = MEMORY_UTIL)
    public static long memGetLongUnaligned(long address) {
        return StructMemory.getLong(address);
    }

    @Patch(owner = MEMORY_UTIL)
    public static void memPutLongUnaligned(long address, long value) {
        StructMemory.putLong(address, value);
    }

    @Patch(owner = MEMORY_UTIL)
    public static float memGetFloat(long address) {
        return StructMemory.getFloat(address);
    }

    @Patch(owner = MEMORY_UTIL)
    public static void memPutFloat(long address, float value) {
        StructMemory.putFloat(address, value);
    }

    @Patch(owner = MEMORY_UTIL)
    public static float memGetFloatUnaligned(long address) {
        return StructMemory.getFloat(address);
    }

    @Patch(owner = MEMORY_UTIL)
    public static void memPutFloatUnaligned(long address, float value) {
        StructMemory.putFloat(address, value);
    }

    @Patch(owner = MEMORY_UTIL)
    public static double memGetDouble(long address) {
        return StructMemory.getDouble(address);
    }

    @Patch(owner = MEMORY_UTIL)
    public static void memPutDouble(long address, double value) {
        StructMemory.putDouble(address, value);
    }

    @Patch(owner = MEMORY_UTIL)
    public static double memGetDoubleUnaligned(long address) {
        return StructMemory.getDouble(address);
    }

    @Patch(owner = MEMORY_UTIL)
    public static void memPutDoubleUnaligned(long address, double value) {
        StructMemory.putDouble(address, value);
    }

    @Patch(owner = MEMORY_UTIL)
    public static long memGetCLong(long address) {
        return StructMemory.getLong(address);
    }

    @Patch(owner = MEMORY_UTIL)
    public static void memPutCLong(long address, long value) {
        StructMemory.putLong(address, value);
    }

    @Patch(owner = MEMORY_UTIL)
    public static long memGetCLongUnaligned(long address) {
        return StructMemory.getLong(address);
    }

    @Patch(owner = MEMORY_UTIL)
    public static void memPutCLongUnaligned(long address, long value) {
        StructMemory.putLong(address, value);
    }

    @Patch(owner = MEMORY_UTIL)
    public static long memGetAddress(long address) {
        return StructMemory.getLong(address);
    }

    @Patch(owner = MEMORY_UTIL)
    public static void memPutAddress(long address, long value) {
        StructMemory.putLong(address, value);
    }

    @Patch(owner = MEMORY_UTIL)
    public static long memGetAddressUnaligned(long address) {
        return StructMemory.getLong(address);
    }

    @Patch(owner = MEMORY_UTIL)
    public static void memPutAddressUnaligned(long address, long value) {
        StructMemory.putLong(address, value);
    }

    @Patch(owner = MEMORY_UTIL)
    public static long memAddress(ByteBuffer buffer) {
        return StructMemory.addressOf(buffer);
    }

    @Patch(owner = MEMORY_UTIL)
    public static long memAddressSafe(ByteBuffer buffer) {
        return buffer == null ? 0L : StructMemory.addressOf(buffer);
    }

    @Patch(owner = MEMORY_UTIL)
    public static String memUTF8(long address) {
        return StructMemory.string(address, StandardCharsets.UTF_8);
    }

    @Patch(owner = MEMORY_UTIL)
    public static String memUTF8Safe(long address) {
        return address == 0L ? null : StructMemory.string(address, StandardCharsets.UTF_8);
    }

    @Patch(owner = MEMORY_UTIL)
    public static String memASCII(long address) {
        return StructMemory.string(address, StandardCharsets.US_ASCII);
    }

    @Patch(owner = MEMORY_UTIL)
    public static String memASCIISafe(long address) {
        return address == 0L ? null : StructMemory.string(address, StandardCharsets.US_ASCII);
    }

    @Patch(owner = MEMORY_UTIL)
    public static ByteBuffer memRealloc(ByteBuffer ptr, int size) {
        ByteBuffer buffer = DummyBuffers.bytes(size);
        if (ptr != null) {
            ByteBuffer old = ptr.duplicate();
            old.clear();
            old.limit(Math.min(old.capacity(), size));
            buffer.put(old);
            buffer.clear();
            buffer.position(Math.min(ptr.position(), size));
        }

        return buffer;
    }

    @Patch(owner = MEMORY_UTIL)
    public static ShortBuffer memRealloc(ShortBuffer ptr, int size) {
        ShortBuffer buffer = DummyBuffers.shorts((long) size << 1);
        if (ptr != null) {
            ShortBuffer old = ptr.duplicate();
            old.clear();
            old.limit(Math.min(old.capacity(), size));
            buffer.put(old);
            buffer.clear();
            buffer.position(Math.min(ptr.position(), size));
        }

        return buffer;
    }

    @Patch(owner = MEMORY_UTIL)
    public static CharBuffer memRealloc(CharBuffer ptr, int size) {
        CharBuffer buffer = DummyBuffers.chars((long) size << 1);
        if (ptr != null) {
            CharBuffer old = ptr.duplicate();
            old.clear();
            old.limit(Math.min(old.capacity(), size));
            buffer.put(old);
            buffer.clear();
            buffer.position(Math.min(ptr.position(), size));
        }

        return buffer;
    }

    @Patch(owner = MEMORY_UTIL)
    public static IntBuffer memRealloc(IntBuffer ptr, int size) {
        IntBuffer buffer = DummyBuffers.ints((long) size << 2);
        if (ptr != null) {
            IntBuffer old = ptr.duplicate();
            old.clear();
            old.limit(Math.min(old.capacity(), size));
            buffer.put(old);
            buffer.clear();
            buffer.position(Math.min(ptr.position(), size));
        }

        return buffer;
    }

    @Patch(owner = MEMORY_UTIL)
    public static LongBuffer memRealloc(LongBuffer ptr, int size) {
        LongBuffer buffer = DummyBuffers.longs((long) size << 3);
        if (ptr != null) {
            LongBuffer old = ptr.duplicate();
            old.clear();
            old.limit(Math.min(old.capacity(), size));
            buffer.put(old);
            buffer.clear();
            buffer.position(Math.min(ptr.position(), size));
        }

        return buffer;
    }

    @Patch(owner = MEMORY_UTIL)
    public static FloatBuffer memRealloc(FloatBuffer ptr, int size) {
        FloatBuffer buffer = DummyBuffers.floats((long) size << 2);
        if (ptr != null) {
            FloatBuffer old = ptr.duplicate();
            old.clear();
            old.limit(Math.min(old.capacity(), size));
            buffer.put(old);
            buffer.clear();
            buffer.position(Math.min(ptr.position(), size));
        }

        return buffer;
    }

    @Patch(owner = MEMORY_UTIL)
    public static DoubleBuffer memRealloc(DoubleBuffer ptr, int size) {
        DoubleBuffer buffer = DummyBuffers.doubles((long) size << 3);
        if (ptr != null) {
            DoubleBuffer old = ptr.duplicate();
            old.clear();
            old.limit(Math.min(old.capacity(), size));
            buffer.put(old);
            buffer.clear();
            buffer.position(Math.min(ptr.position(), size));
        }

        return buffer;
    }

    @Patch(owner = MEMORY_UTIL)
    public static void memCopy(long src, long dst, long bytes) {
        StructMemory.copy(src, dst, bytes);
    }

    @Patch(owner = MEMORY_UTIL)
    public static void memSet(long ptr, int value, long bytes) {
        StructMemory.set(ptr, value, bytes);
    }

    @Patch(owner = MEMORY_UTIL)
    public static void memCopy(Struct<?> src, Struct<?> dst) {
        StructMemory.copy(src.address(), dst.address(), src.sizeof());
    }

    @Patch(owner = MEMORY_UTIL)
    public static void memSet(Struct<?> ptr, int value) {
        StructMemory.set(ptr.address(), value, ptr.sizeof());
    }

}
