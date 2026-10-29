package io.github.headlesshq.headlessmc.lwjgl3.runtime.opengl;

import io.github.headlesshq.headlessmc.lwjgl.runtime.Patch;
import io.github.headlesshq.headlessmc.lwjgl.runtime.opengl.GlState;
import io.github.headlesshq.headlessmc.lwjgl.runtime.opengl.GlValues;

import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;

/**
 * Patches for indexed buffer bindings and the indexed {@code glGet*i_v} queries, answered by
 * {@link GlValues#getIndexedDoubles(int, int)}.
 */
public final class GlIndexedPatches {
    private static final String GL30 = "org/lwjgl/opengl/GL30";
    private static final String GL30C = "org/lwjgl/opengl/GL30C";
    private static final String GL32 = "org/lwjgl/opengl/GL32";
    private static final String GL32C = "org/lwjgl/opengl/GL32C";
    private static final String GL41 = "org/lwjgl/opengl/GL41";
    private static final String GL41C = "org/lwjgl/opengl/GL41C";

    private GlIndexedPatches() {
        throw new AssertionError();
    }

    @Patch(owner = {GL30C, GL30})
    public static void glBindBufferBase(int target, int index, int buffer) {
        GlState.bindBufferRange(target, index, buffer, 0L, 0L);
    }

    @Patch(owner = {GL30C, GL30})
    public static void glBindBufferRange(int target, int index, int buffer, long offset, long size) {
        GlState.bindBufferRange(target, index, buffer, offset, size);
    }

    @Patch(owner = {GL30C, GL30})
    public static int glGetIntegeri(int target, int index) {
        return GlValues.getIndexed(target, index)[0];
    }

    @Patch(owner = {GL30C, GL30})
    public static void glGetIntegeri_v(int target, int index, IntBuffer data) {
        int[] values = GlValues.getIndexed(target, index);
        for (int i = 0; i < values.length && i < data.remaining(); i++) {
            data.put(data.position() + i, values[i]);
        }
    }

    @Patch(owner = {GL30C, GL30})
    public static void glGetIntegeri_v(int target, int index, int[] data) {
        int[] values = GlValues.getIndexed(target, index);
        System.arraycopy(values, 0, data, 0, Math.min(values.length, data.length));
    }

    @Patch(owner = {GL30C, GL30})
    public static boolean glGetBooleani(int target, int index) {
        return GlValues.getIndexedDoubles(target, index)[0] != 0.0;
    }

    @Patch(owner = {GL30C, GL30})
    public static void glGetBooleani_v(int target, int index, ByteBuffer data) {
        double[] values = GlValues.getIndexedDoubles(target, index);
        for (int i = 0; i < values.length && i < data.remaining(); i++) {
            data.put(data.position() + i, (byte) (values[i] != 0.0 ? 1 : 0));
        }
    }

    @Patch(owner = {GL32C, GL32})
    public static long glGetInteger64i(int target, int index) {
        return GlValues.getIndexedLongs(target, index)[0];
    }

    @Patch(owner = {GL32C, GL32})
    public static void glGetInteger64i_v(int target, int index, LongBuffer data) {
        long[] values = GlValues.getIndexedLongs(target, index);
        for (int i = 0; i < values.length && i < data.remaining(); i++) {
            data.put(data.position() + i, values[i]);
        }
    }

    @Patch(owner = {GL32C, GL32})
    public static void glGetInteger64i_v(int target, int index, long[] data) {
        long[] values = GlValues.getIndexedLongs(target, index);
        System.arraycopy(values, 0, data, 0, Math.min(values.length, data.length));
    }

    @Patch(owner = {GL41C, GL41})
    public static float glGetFloati(int target, int index) {
        return (float) GlValues.getIndexedDoubles(target, index)[0];
    }

    @Patch(owner = {GL41C, GL41})
    public static void glGetFloati_v(int target, int index, FloatBuffer data) {
        double[] values = GlValues.getIndexedDoubles(target, index);
        for (int i = 0; i < values.length && i < data.remaining(); i++) {
            data.put(data.position() + i, (float) values[i]);
        }
    }

    @Patch(owner = {GL41C, GL41})
    public static void glGetFloati_v(int target, int index, float[] data) {
        double[] values = GlValues.getIndexedDoubles(target, index);
        for (int i = 0; i < values.length && i < data.length; i++) {
            data[i] = (float) values[i];
        }
    }

    @Patch(owner = {GL41C, GL41})
    public static double glGetDoublei(int target, int index) {
        return GlValues.getIndexedDoubles(target, index)[0];
    }

    @Patch(owner = {GL41C, GL41})
    public static void glGetDoublei_v(int target, int index, DoubleBuffer data) {
        double[] values = GlValues.getIndexedDoubles(target, index);
        for (int i = 0; i < values.length && i < data.remaining(); i++) {
            data.put(data.position() + i, values[i]);
        }
    }

    @Patch(owner = {GL41C, GL41})
    public static void glGetDoublei_v(int target, int index, double[] data) {
        double[] values = GlValues.getIndexedDoubles(target, index);
        System.arraycopy(values, 0, data, 0, Math.min(values.length, data.length));
    }

}
