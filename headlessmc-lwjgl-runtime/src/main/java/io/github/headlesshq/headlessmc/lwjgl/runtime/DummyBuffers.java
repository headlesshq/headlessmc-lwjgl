package io.github.headlesshq.headlessmc.lwjgl.runtime;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;

/**
 * Central management of the buffers the patched LWJGL hands out.
 * <p>Every buffer is a heap buffer in native byte order with exactly the requested capacity.
 * Small buffers (up to {@code hmc.lwjgl<2|3>.buffers.smallThreshold} bytes) wrap a fresh array, so values written to them
 * can be read back, e.g. out parameters like {@code glGetIntegerv(pname, buffer)}.
 * Larger buffers are views of one shared array whose contents are garbage by definition, so that the application
 * never actually holds large amounts of memory, e.g. for vertex data or mapped GPU buffers.
 * The shared array grows on demand and logs a warning when it exceeds {@code hmc.lwjgl<2|3>.buffers.warnThreshold} bytes.</p>
 */
public final class DummyBuffers {
    /**
     * The address returned for memory that does not exist. Nonzero and aligned, so that checks like
     * {@code address != NULL} and alignment checks pass.
     */
    public static final long FAKE_ADDRESS = 0x10000L;
    public static final long FAKE_ADDRESS_ALIGNMENT = 64L;

    private static final int SMALL_THRESHOLD = Config.getInt("buffers.smallThreshold", 16 * 1024 * 1024);
    private static final long WARN_THRESHOLD = Config.getLong("buffers.warnThreshold", 64L * 1024L * 1024L);
    private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;

    private static volatile byte[] shared = new byte[0];

    private DummyBuffers() {
        throw new AssertionError();
    }

    public static ByteBuffer bytes(long capacity) {
        int size = checkedSize(capacity, 1);
        if (size <= SMALL_THRESHOLD) {
            return ByteBuffer.wrap(new byte[size]).order(ByteOrder.nativeOrder());
        }

        return sharedView(size);
    }

    public static ShortBuffer shorts(long capacity) {
        int size = checkedSize(capacity, Short.BYTES);
        return size <= SMALL_THRESHOLD ? ShortBuffer.wrap(new short[size / Short.BYTES]) : sharedView(size).asShortBuffer();
    }

    public static CharBuffer chars(long capacity) {
        int size = checkedSize(capacity, Character.BYTES);
        return size <= SMALL_THRESHOLD ? CharBuffer.wrap(new char[size / Character.BYTES]) : sharedView(size).asCharBuffer();
    }

    public static IntBuffer ints(long capacity) {
        int size = checkedSize(capacity, Integer.BYTES);
        return size <= SMALL_THRESHOLD ? IntBuffer.wrap(new int[size / Integer.BYTES]) : sharedView(size).asIntBuffer();
    }

    public static LongBuffer longs(long capacity) {
        int size = checkedSize(capacity, Long.BYTES);
        return size <= SMALL_THRESHOLD ? LongBuffer.wrap(new long[size / Long.BYTES]) : sharedView(size).asLongBuffer();
    }

    public static FloatBuffer floats(long capacity) {
        int size = checkedSize(capacity, Float.BYTES);
        return size <= SMALL_THRESHOLD ? FloatBuffer.wrap(new float[size / Float.BYTES]) : sharedView(size).asFloatBuffer();
    }

    public static DoubleBuffer doubles(long capacity) {
        int size = checkedSize(capacity, Double.BYTES);
        return size <= SMALL_THRESHOLD ? DoubleBuffer.wrap(new double[size / Double.BYTES]) : sharedView(size).asDoubleBuffer();
    }

    public static ByteBuffer wrap(byte[] values) {
        return ByteBuffer.wrap(values.clone()).order(ByteOrder.nativeOrder());
    }

    public static ShortBuffer wrap(short[] values) {
        return ShortBuffer.wrap(values.clone());
    }

    public static IntBuffer wrap(int[] values) {
        return IntBuffer.wrap(values.clone());
    }

    public static LongBuffer wrap(long[] values) {
        return LongBuffer.wrap(values.clone());
    }

    public static FloatBuffer wrap(float[] values) {
        return FloatBuffer.wrap(values.clone());
    }

    public static DoubleBuffer wrap(double[] values) {
        return DoubleBuffer.wrap(values.clone());
    }

    /**
     * @return the size of the shared array, for tests and diagnostics.
     */
    public static int sharedSize() {
        return shared.length;
    }

    private static ByteBuffer sharedView(int size) {
        byte[] array = shared;
        if (array.length < size) {
            array = grow(size);
        }

        ByteBuffer view = ByteBuffer.wrap(array, 0, size).slice();
        return view.order(ByteOrder.nativeOrder());
    }

    private static synchronized byte[] grow(int size) {
        byte[] array = shared;
        if (array.length >= size) {
            return array;
        }

        int newSize = (int) Math.min(MAX_ARRAY_SIZE, Math.max(size, (long) array.length * 2L));
        if (newSize > WARN_THRESHOLD && array.length <= WARN_THRESHOLD) {
            Log.warn("Shared dummy buffer grows to " + newSize + " bytes, which is above "
                         + Config.PREFIX + "buffers.warnThreshold=" + WARN_THRESHOLD);
        }

        shared = array = new byte[newSize];
        return array;
    }

    private static int checkedSize(long capacity, int elementSize) {
        if (capacity < 0) {
            throw new IllegalArgumentException("Negative buffer capacity: " + capacity);
        }

        long bytes = capacity * elementSize;
        if (bytes > MAX_ARRAY_SIZE || bytes < 0) {
            Log.warnOnce("buffers:max", "Requested a buffer of " + capacity + " elements, which exceeds the maximum array size");
            return (MAX_ARRAY_SIZE / elementSize) * elementSize;
        }

        return (int) bytes;
    }

}
