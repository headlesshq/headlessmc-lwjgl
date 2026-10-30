package io.github.headlesshq.headlessmc.lwjgl3.runtime.opengl;

import io.github.headlesshq.headlessmc.lwjgl.runtime.DummyBuffers;
import io.github.headlesshq.headlessmc.lwjgl.runtime.Patch;
import io.github.headlesshq.headlessmc.lwjgl.runtime.StructMemory;
import io.github.headlesshq.headlessmc.lwjgl.runtime.opengl.GlObjects;
import io.github.headlesshq.headlessmc.lwjgl.runtime.opengl.GlState;

import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Patches that record the size of buffer data in {@link GlObjects}, so that mapping a buffer returns a dummy buffer
 * of the size of its data store and {@code glGetBufferParameteri} answers {@code GL_BUFFER_SIZE}.
 * <p>The native mapping functions, like {@code nglMapBufferRange}, which Minecraft 26.x calls directly, return the
 * address of {@link StructMemory#allocateRaw raw memory}, real up to the raw threshold, which is freed when the buffer
 * is unmapped, instead of a {@code NULL} mapping.</p>
 */
public final class GlBufferPatches {
    private static final String GL15 = "org/lwjgl/opengl/GL15";
    private static final String GL15C = "org/lwjgl/opengl/GL15C";
    private static final String GL30 = "org/lwjgl/opengl/GL30";
    private static final String GL30C = "org/lwjgl/opengl/GL30C";
    private static final String GL45 = "org/lwjgl/opengl/GL45";
    private static final String GL45C = "org/lwjgl/opengl/GL45C";
    private static final String ARB_VBO = "org/lwjgl/opengl/ARBVertexBufferObject";
    private static final String ARB_MAP_BUFFER_RANGE = "org/lwjgl/opengl/ARBMapBufferRange";
    private static final String ARB_DSA = "org/lwjgl/opengl/ARBDirectStateAccess";
    private static final String EXT_DSA = "org/lwjgl/opengl/EXTDirectStateAccess";
    /**
     * The addresses of the mapped buffers, by buffer name.
     */
    private static final Map<Integer, Long> MAPPINGS = new ConcurrentHashMap<>();

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

    @Patch(owner = {GL15C, GL15})
    public static long nglMapBuffer(int target, int access) {
        return map(GlState.boundBuffer(target), GlObjects.bufferSize(target));
    }

    @Patch(owner = ARB_VBO)
    public static long nglMapBufferARB(int target, int access) {
        return nglMapBuffer(target, access);
    }

    @Patch(owner = {GL30C, GL30, ARB_MAP_BUFFER_RANGE})
    public static long nglMapBufferRange(int target, long offset, long length, int access) {
        return map(GlState.boundBuffer(target), length);
    }

    @Patch(owner = {GL45C, GL45, ARB_DSA})
    public static long nglMapNamedBuffer(int buffer, int access) {
        return map(buffer, GlObjects.namedBufferSize(buffer));
    }

    @Patch(owner = EXT_DSA)
    public static long nglMapNamedBufferEXT(int buffer, int access) {
        return nglMapNamedBuffer(buffer, access);
    }

    @Patch(owner = {GL45C, GL45, ARB_DSA})
    public static long nglMapNamedBufferRange(int buffer, long offset, long length, int access) {
        return map(buffer, length);
    }

    @Patch(owner = EXT_DSA)
    public static long nglMapNamedBufferRangeEXT(int buffer, long offset, long length, int access) {
        return map(buffer, length);
    }

    @Patch(owner = {GL15C, GL15})
    public static boolean glUnmapBuffer(int target) {
        return unmap(GlState.boundBuffer(target));
    }

    @Patch(owner = ARB_VBO)
    public static boolean glUnmapBufferARB(int target) {
        return glUnmapBuffer(target);
    }

    @Patch(owner = {GL45C, GL45, ARB_DSA})
    public static boolean glUnmapNamedBuffer(int buffer) {
        return unmap(buffer);
    }

    @Patch(owner = EXT_DSA)
    public static boolean glUnmapNamedBufferEXT(int buffer) {
        return unmap(buffer);
    }

    private static long map(int buffer, long size) {
        long address = StructMemory.allocateRaw(size);
        Long previous = MAPPINGS.put(buffer, address);
        if (previous != null) {
            StructMemory.free(previous);
        }

        return address;
    }

    private static boolean unmap(int buffer) {
        Long address = MAPPINGS.remove(buffer);
        if (address != null) {
            StructMemory.free(address);
        }

        return true;
    }

}
