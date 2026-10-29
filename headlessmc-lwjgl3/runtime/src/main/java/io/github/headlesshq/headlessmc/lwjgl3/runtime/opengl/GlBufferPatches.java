package io.github.headlesshq.headlessmc.lwjgl3.runtime.opengl;

import io.github.headlesshq.headlessmc.lwjgl.runtime.DummyBuffers;
import io.github.headlesshq.headlessmc.lwjgl.runtime.Patch;
import io.github.headlesshq.headlessmc.lwjgl.runtime.opengl.GlObjects;

import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;

/**
 * Patches that record the size of buffer data in {@link GlObjects}, so that mapping a buffer returns a dummy buffer
 * of the size of its data store and {@code glGetBufferParameteri} answers {@code GL_BUFFER_SIZE}.
 */
public final class GlBufferPatches {
    private static final String GL15 = "org/lwjgl/opengl/GL15";
    private static final String GL15C = "org/lwjgl/opengl/GL15C";

    private GlBufferPatches() {
        throw new AssertionError();
    }

    @Patch(owner = {GL15C, GL15})
    public static void glBufferData(int target, long size, int usage) {
        GlObjects.bufferData(target, size);
    }

    @Patch(owner = {GL15C, GL15})
    public static void glBufferData(int target, ByteBuffer data, int usage) {
        GlObjects.bufferData(target, data.remaining());
    }

    @Patch(owner = {GL15C, GL15})
    public static void glBufferData(int target, ShortBuffer data, int usage) {
        GlObjects.bufferData(target, (long) data.remaining() << 1);
    }

    @Patch(owner = {GL15C, GL15})
    public static void glBufferData(int target, IntBuffer data, int usage) {
        GlObjects.bufferData(target, (long) data.remaining() << 2);
    }

    @Patch(owner = {GL15C, GL15})
    public static void glBufferData(int target, LongBuffer data, int usage) {
        GlObjects.bufferData(target, (long) data.remaining() << 3);
    }

    @Patch(owner = {GL15C, GL15})
    public static void glBufferData(int target, FloatBuffer data, int usage) {
        GlObjects.bufferData(target, (long) data.remaining() << 2);
    }

    @Patch(owner = {GL15C, GL15})
    public static void glBufferData(int target, DoubleBuffer data, int usage) {
        GlObjects.bufferData(target, (long) data.remaining() << 3);
    }

    @Patch(owner = {GL15C, GL15})
    public static void glBufferData(int target, short[] data, int usage) {
        GlObjects.bufferData(target, (long) data.length << 1);
    }

    @Patch(owner = {GL15C, GL15})
    public static void glBufferData(int target, int[] data, int usage) {
        GlObjects.bufferData(target, (long) data.length << 2);
    }

    @Patch(owner = {GL15C, GL15})
    public static void glBufferData(int target, long[] data, int usage) {
        GlObjects.bufferData(target, (long) data.length << 3);
    }

    @Patch(owner = {GL15C, GL15})
    public static void glBufferData(int target, float[] data, int usage) {
        GlObjects.bufferData(target, (long) data.length << 2);
    }

    @Patch(owner = {GL15C, GL15})
    public static void glBufferData(int target, double[] data, int usage) {
        GlObjects.bufferData(target, (long) data.length << 3);
    }

    @Patch(owner = {GL15C, GL15})
    public static ByteBuffer glMapBuffer(int target, int access) {
        return DummyBuffers.bytes(GlObjects.bufferSize(target));
    }

    @Patch(owner = {GL15C, GL15})
    public static ByteBuffer glMapBuffer(int target, int access, ByteBuffer old_buffer) {
        long size = GlObjects.bufferSize(target);
        return old_buffer != null && old_buffer.capacity() == size ? old_buffer : DummyBuffers.bytes(size);
    }

    @Patch(owner = {GL15C, GL15})
    public static int glGetBufferParameteri(int target, int pname) {
        return GlObjects.bufferValue(target, pname);
    }

    @Patch(owner = {GL15C, GL15})
    public static void glGetBufferParameteriv(int target, int pname, IntBuffer params) {
        params.put(params.position(), GlObjects.bufferValue(target, pname));
    }

    @Patch(owner = {GL15C, GL15})
    public static void glGetBufferParameteriv(int target, int pname, int[] params) {
        params[0] = GlObjects.bufferValue(target, pname);
    }

}
