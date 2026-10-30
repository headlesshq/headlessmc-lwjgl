package io.github.headlesshq.headlessmc.lwjgl.runtime;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * Real memory for LWJGL structs, so that struct fields can be written by handlers and read back by the application.
 * <p>Every struct gets its own direct {@link ByteBuffer}, which the struct object keeps as its container, so the
 * garbage collector frees the memory once the struct is unreachable, whether or not the application frees it.</p>
 * <p>The patched {@code MemoryUtil} accessors only touch memory registered here. Every other address, like the fake
 * addresses of {@link DummyBuffers}, reads as 0 and ignores writes, so a wrong address can never crash the JVM.</p>
 * <p>Raw allocations like {@code nmemAlloc} up to {@code hmc.lwjgl<2|3>.memory.rawThreshold} bytes get real memory as
 * well, which is retained until it is freed, larger ones get the fake address.</p>
 */
public final class StructMemory {
    private static final Object UNSAFE = unsafe();
    private static final MethodHandle GET_BYTE = unsafeMethod("getByte", byte.class, long.class);
    private static final MethodHandle GET_SHORT = unsafeMethod("getShort", short.class, long.class);
    private static final MethodHandle GET_INT = unsafeMethod("getInt", int.class, long.class);
    private static final MethodHandle GET_LONG = unsafeMethod("getLong", long.class, long.class);
    private static final MethodHandle PUT_BYTE = unsafeMethod("putByte", void.class, long.class, byte.class);
    private static final MethodHandle PUT_SHORT = unsafeMethod("putShort", void.class, long.class, short.class);
    private static final MethodHandle PUT_INT = unsafeMethod("putInt", void.class, long.class, int.class);
    private static final MethodHandle PUT_LONG = unsafeMethod("putLong", void.class, long.class, long.class);
    private static final MethodHandle COPY = unsafeMethod("copyMemory", void.class, long.class, long.class, long.class);
    private static final MethodHandle SET = unsafeMethod("setMemory", void.class, long.class, long.class, byte.class);
    private static final MethodHandle OBJECT_GET_LONG = unsafeMethod("getLong", long.class, Object.class, long.class);
    private static final long BUFFER_ADDRESS = bufferAddressOffset();
    private static final ConcurrentSkipListMap<Long, Region> REGIONS = new ConcurrentSkipListMap<>();
    private static final ReferenceQueue<ByteBuffer> COLLECTED = new ReferenceQueue<>();
    private static final long RAW_THRESHOLD = Config.getLong("memory.rawThreshold", 64L * 1024L);
    private static final Map<Long, ByteBuffer> RETAINED = new ConcurrentHashMap<>();

    private StructMemory() {
        throw new AssertionError();
    }

    /**
     * @param size the size in bytes.
     * @return new zeroed memory of the given size, registered for the {@code MemoryUtil} accessors.
     */
    public static ByteBuffer allocate(int size) {
        purge();
        ByteBuffer buffer = ByteBuffer.allocateDirect(Math.max(size, 1));
        long address = address(buffer);
        REGIONS.put(address, new Region(address, buffer.capacity(), buffer));
        return buffer;
    }

    /**
     * @param size the size in bytes.
     * @return the address of new zeroed memory that is retained until {@link #free(long)}, or
     * {@link DummyBuffers#FAKE_ADDRESS} if the size exceeds the raw threshold.
     */
    public static long allocateRaw(long size) {
        if (size < 0 || size > RAW_THRESHOLD) {
            return DummyBuffers.FAKE_ADDRESS;
        }

        ByteBuffer buffer = allocate((int) size);
        long address = address(buffer);
        RETAINED.put(address, buffer);
        return address;
    }

    /**
     * Like {@code realloc}: the contents are kept up to the smaller of the two sizes.
     *
     * @param address an address returned by {@link #allocateRaw(long)}, or 0.
     * @param size the new size in bytes.
     * @return the address of the new memory.
     */
    public static long reallocateRaw(long address, long size) {
        long result = allocateRaw(size);
        ByteBuffer old = RETAINED.get(address);
        if (old != null) {
            long bytes = Math.min(old.capacity(), size);
            if (result != DummyBuffers.FAKE_ADDRESS) {
                copy(address, result, bytes);
            }

            free(address);
        }

        return result;
    }

    /**
     * Releases memory returned by {@link #allocateRaw(long)}, other addresses are ignored.
     */
    public static void free(long address) {
        RETAINED.remove(address);
    }

    /**
     * @param buffer a direct buffer.
     * @return the address of the first byte of the buffer's memory.
     */
    public static long address(Buffer buffer) {
        return invoke(() -> (long) OBJECT_GET_LONG.invokeExact((Object) buffer, BUFFER_ADDRESS));
    }

    /**
     * @param address an address.
     * @param size the number of bytes to access.
     * @return the memory that contains the whole range, or {@code null} if the range is not registered memory.
     */
    private static ByteBuffer find(long address, long size) {
        Map.Entry<Long, Region> entry = REGIONS.floorEntry(address);
        if (entry == null) {
            return null;
        }

        Region region = entry.getValue();
        ByteBuffer buffer = region.get();
        if (buffer == null || size < 0 || address + size > region.address + region.size) {
            return null;
        }

        return buffer;
    }

    private static void purge() {
        Region region;
        while ((region = (Region) COLLECTED.poll()) != null) {
            REGIONS.remove(region.address, region);
        }
    }

    /**
     * @param buffer a buffer.
     * @return the address of the current position of the buffer if it is registered memory, otherwise
     * {@link DummyBuffers#FAKE_ADDRESS}.
     */
    public static long addressOf(Buffer buffer) {
        if (buffer == null || !buffer.isDirect()) {
            return DummyBuffers.FAKE_ADDRESS;
        }

        long address = address(buffer);
        return find(address, buffer.capacity()) == null ? DummyBuffers.FAKE_ADDRESS : address + buffer.position();
    }

    /**
     * @param address an address.
     * @param bytes the number of bytes.
     * @return a buffer over the registered memory at the address, in native byte order, or {@code null} if the range is
     * not registered memory.
     */
    public static ByteBuffer view(long address, int bytes) {
        Map.Entry<Long, Region> entry = REGIONS.floorEntry(address);
        ByteBuffer memory = find(address, bytes);
        if (memory == null) {
            return null;
        }

        ByteBuffer view = memory.duplicate();
        int start = (int) (address - entry.getKey());
        view.limit(start + bytes).position(start);
        return view.slice().order(ByteOrder.nativeOrder());
    }

    /**
     * @param text a string.
     * @return new registered memory holding the null-terminated UTF-8 encoding of the string.
     */
    public static ByteBuffer utf8(CharSequence text) {
        byte[] bytes = text.toString().getBytes(StandardCharsets.UTF_8);
        ByteBuffer buffer = allocate(bytes.length + 1);
        buffer.put(bytes).put((byte) 0).flip();
        return buffer;
    }

    /**
     * @param address the address of a null-terminated string.
     * @param charset the encoding of the string.
     * @return the string, empty if the address is not registered memory.
     */
    public static String string(long address, Charset charset) {
        Map.Entry<Long, Region> entry = REGIONS.floorEntry(address);
        ByteBuffer memory = entry == null ? null : entry.getValue().get();
        if (memory == null || address >= entry.getKey() + entry.getValue().size) {
            return "";
        }

        int start = (int) (address - entry.getKey());
        int end = start;
        while (end < memory.capacity() && memory.get(end) != 0) {
            end++;
        }

        byte[] bytes = new byte[end - start];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = memory.get(start + i);
        }

        return new String(bytes, charset);
    }

    /**
     * @param address the address of a UTF-16 string in native byte order, terminated by a 0 char.
     * @return the string, empty if the address is not registered memory.
     */
    public static String utf16(long address) {
        Map.Entry<Long, Region> entry = REGIONS.floorEntry(address);
        ByteBuffer memory = entry == null ? null : entry.getValue().get();
        if (memory == null || address >= entry.getKey() + entry.getValue().size) {
            return "";
        }

        int start = (int) (address - entry.getKey());
        int end = start;
        ByteBuffer chars = memory.duplicate().order(ByteOrder.nativeOrder());
        while (end + 1 < memory.capacity() && chars.getChar(end) != 0) {
            end += 2;
        }

        StringBuilder string = new StringBuilder((end - start) / 2);
        for (int i = start; i < end; i += 2) {
            string.append(chars.getChar(i));
        }

        return string.toString();
    }

    public static byte getByte(long address) {
        ByteBuffer memory = find(address, 1);
        byte value = memory == null ? 0 : invoke(() -> (byte) GET_BYTE.invokeExact(address));
        reachable(memory);
        return value;
    }

    public static short getShort(long address) {
        ByteBuffer memory = find(address, 2);
        short value = memory == null ? 0 : invoke(() -> (short) GET_SHORT.invokeExact(address));
        reachable(memory);
        return value;
    }

    public static int getInt(long address) {
        ByteBuffer memory = find(address, 4);
        int value = memory == null ? 0 : invoke(() -> (int) GET_INT.invokeExact(address));
        reachable(memory);
        return value;
    }

    public static long getLong(long address) {
        ByteBuffer memory = find(address, 8);
        long value = memory == null ? 0L : invoke(() -> (long) GET_LONG.invokeExact(address));
        reachable(memory);
        return value;
    }

    public static float getFloat(long address) {
        return Float.intBitsToFloat(getInt(address));
    }

    public static double getDouble(long address) {
        return Double.longBitsToDouble(getLong(address));
    }

    public static void putByte(long address, byte value) {
        ByteBuffer memory = find(address, 1);
        if (memory != null) {
            run(() -> {
                PUT_BYTE.invokeExact(address, value);
            });
        }

        reachable(memory);
    }

    public static void putShort(long address, short value) {
        ByteBuffer memory = find(address, 2);
        if (memory != null) {
            run(() -> {
                PUT_SHORT.invokeExact(address, value);
            });
        }

        reachable(memory);
    }

    public static void putInt(long address, int value) {
        ByteBuffer memory = find(address, 4);
        if (memory != null) {
            run(() -> {
                PUT_INT.invokeExact(address, value);
            });
        }

        reachable(memory);
    }

    public static void putLong(long address, long value) {
        ByteBuffer memory = find(address, 8);
        if (memory != null) {
            run(() -> {
                PUT_LONG.invokeExact(address, value);
            });
        }

        reachable(memory);
    }

    public static void putFloat(long address, float value) {
        putInt(address, Float.floatToRawIntBits(value));
    }

    public static void putDouble(long address, double value) {
        putLong(address, Double.doubleToRawLongBits(value));
    }

    /**
     * Copies bytes between registered memory, does nothing if one of the ranges is not registered.
     */
    public static void copy(long source, long destination, long bytes) {
        ByteBuffer from = find(source, bytes);
        ByteBuffer to = find(destination, bytes);
        if (from != null && to != null) {
            run(() -> {
                COPY.invokeExact(source, destination, bytes);
            });
        }

        reachable(from);
        reachable(to);
    }

    /**
     * Fills registered memory, does nothing if the range is not registered.
     */
    public static void set(long address, int value, long bytes) {
        ByteBuffer memory = find(address, bytes);
        if (memory != null) {
            run(() -> {
                SET.invokeExact(address, bytes, (byte) value);
            });
        }

        reachable(memory);
    }

    /**
     * Keeps the memory reachable until the access is done, so that the garbage collector cannot free it in between.
     */
    private static void reachable(ByteBuffer memory) {
        if (memory != null && memory.capacity() < 0) {
            throw new AssertionError();
        }
    }

    private static Object unsafe() {
        try {
            Class<?> type = Class.forName("sun.misc.Unsafe");
            Field field = type.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            return field.get(null);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("sun.misc.Unsafe is not available", e);
        }
    }

    private static MethodHandle unsafeMethod(String name, Class<?> returnType, Class<?>... parameters) {
        try {
            return MethodHandles.lookup().findVirtual(UNSAFE.getClass(), name, MethodType.methodType(returnType, parameters)).bindTo(UNSAFE);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("sun.misc.Unsafe has no method " + name, e);
        }
    }

    private static long bufferAddressOffset() {
        try {
            MethodHandle offset = unsafeMethod("objectFieldOffset", long.class, Field.class);
            Field address = Buffer.class.getDeclaredField("address");
            return invoke(() -> (long) offset.invokeExact(address));
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException("java.nio.Buffer has no address field", e);
        }
    }

    private static <T> T invoke(Invocation<T> invocation) {
        try {
            return invocation.invoke();
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable t) {
            throw new IllegalStateException(t);
        }
    }

    private static void run(VoidInvocation invocation) {
        try {
            invocation.invoke();
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable t) {
            throw new IllegalStateException(t);
        }
    }

    private interface Invocation<T> {
        T invoke() throws Throwable;
    }

    private interface VoidInvocation {
        void invoke() throws Throwable;
    }

    private static final class Region extends WeakReference<ByteBuffer> {
        private final long address;
        private final long size;

        private Region(long address, long size, ByteBuffer buffer) {
            super(buffer, COLLECTED);
            this.address = address;
            this.size = size;
        }
    }

}
