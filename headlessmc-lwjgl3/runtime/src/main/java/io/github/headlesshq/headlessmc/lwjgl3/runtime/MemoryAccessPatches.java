package io.github.headlesshq.headlessmc.lwjgl3.runtime;

import org.lwjgl.system.Struct;

/**
 * Patches for the address based accessors of {@code org.lwjgl.system.MemoryUtil}, which only touch the struct memory
 * of {@link StructMemory}: other addresses read as 0 and ignore writes. {@code C long} and pointers are 64 bit.
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
