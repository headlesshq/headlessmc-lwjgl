package io.github.headlesshq.headlessmc.lwjgl3.runtime.opengl;

import io.github.headlesshq.headlessmc.lwjgl.runtime.Patch;
import io.github.headlesshq.headlessmc.lwjgl.runtime.opengl.GlObjects;

import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

/**
 * Patches that record texture images in {@link GlObjects}, so that {@code glGetTexLevelParameter*} returns what the
 * application uploaded.
 */
public final class GlTexturePatches {
    private static final String GL11 = "org/lwjgl/opengl/GL11";
    private static final String GL11C = "org/lwjgl/opengl/GL11C";
    private static final String GL42 = "org/lwjgl/opengl/GL42";
    private static final String GL42C = "org/lwjgl/opengl/GL42C";

    private GlTexturePatches() {
        throw new AssertionError();
    }

    @Patch(owner = {GL11C, GL11})
    public static void glTexImage2D(int target, int level, int internalformat, int width, int height, int border,
                                    int format, int type, ByteBuffer pixels) {
        GlObjects.textureImage(target, level, internalformat, width, height);
    }

    @Patch(owner = {GL11C, GL11})
    public static void glTexImage1D(int target, int level, int internalformat, int width, int border, int format,
                                    int type, ByteBuffer pixels) {
        GlObjects.textureImage(target, level, internalformat, width, 1);
    }

    @Patch(owner = {GL11C, GL11})
    public static void glTexImage2D(int target, int level, int internalformat, int width, int height, int border,
                                    int format, int type, long pixels) {
        GlObjects.textureImage(target, level, internalformat, width, height);
    }

    @Patch(owner = {GL11C, GL11})
    public static void glTexImage1D(int target, int level, int internalformat, int width, int border, int format,
                                    int type, long pixels) {
        GlObjects.textureImage(target, level, internalformat, width, 1);
    }

    @Patch(owner = {GL11C, GL11})
    public static void glTexImage2D(int target, int level, int internalformat, int width, int height, int border,
                                    int format, int type, ShortBuffer pixels) {
        GlObjects.textureImage(target, level, internalformat, width, height);
    }

    @Patch(owner = {GL11C, GL11})
    public static void glTexImage1D(int target, int level, int internalformat, int width, int border, int format,
                                    int type, ShortBuffer pixels) {
        GlObjects.textureImage(target, level, internalformat, width, 1);
    }

    @Patch(owner = {GL11C, GL11})
    public static void glTexImage2D(int target, int level, int internalformat, int width, int height, int border,
                                    int format, int type, IntBuffer pixels) {
        GlObjects.textureImage(target, level, internalformat, width, height);
    }

    @Patch(owner = {GL11C, GL11})
    public static void glTexImage1D(int target, int level, int internalformat, int width, int border, int format,
                                    int type, IntBuffer pixels) {
        GlObjects.textureImage(target, level, internalformat, width, 1);
    }

    @Patch(owner = {GL11C, GL11})
    public static void glTexImage2D(int target, int level, int internalformat, int width, int height, int border,
                                    int format, int type, FloatBuffer pixels) {
        GlObjects.textureImage(target, level, internalformat, width, height);
    }

    @Patch(owner = {GL11C, GL11})
    public static void glTexImage1D(int target, int level, int internalformat, int width, int border, int format,
                                    int type, FloatBuffer pixels) {
        GlObjects.textureImage(target, level, internalformat, width, 1);
    }

    @Patch(owner = {GL11C, GL11})
    public static void glTexImage2D(int target, int level, int internalformat, int width, int height, int border,
                                    int format, int type, DoubleBuffer pixels) {
        GlObjects.textureImage(target, level, internalformat, width, height);
    }

    @Patch(owner = {GL11C, GL11})
    public static void glTexImage1D(int target, int level, int internalformat, int width, int border, int format,
                                    int type, DoubleBuffer pixels) {
        GlObjects.textureImage(target, level, internalformat, width, 1);
    }

    @Patch(owner = {GL11C, GL11})
    public static void glTexImage2D(int target, int level, int internalformat, int width, int height, int border,
                                    int format, int type, short[] pixels) {
        GlObjects.textureImage(target, level, internalformat, width, height);
    }

    @Patch(owner = {GL11C, GL11})
    public static void glTexImage1D(int target, int level, int internalformat, int width, int border, int format,
                                    int type, short[] pixels) {
        GlObjects.textureImage(target, level, internalformat, width, 1);
    }

    @Patch(owner = {GL11C, GL11})
    public static void glTexImage2D(int target, int level, int internalformat, int width, int height, int border,
                                    int format, int type, int[] pixels) {
        GlObjects.textureImage(target, level, internalformat, width, height);
    }

    @Patch(owner = {GL11C, GL11})
    public static void glTexImage1D(int target, int level, int internalformat, int width, int border, int format,
                                    int type, int[] pixels) {
        GlObjects.textureImage(target, level, internalformat, width, 1);
    }

    @Patch(owner = {GL11C, GL11})
    public static void glTexImage2D(int target, int level, int internalformat, int width, int height, int border,
                                    int format, int type, float[] pixels) {
        GlObjects.textureImage(target, level, internalformat, width, height);
    }

    @Patch(owner = {GL11C, GL11})
    public static void glTexImage1D(int target, int level, int internalformat, int width, int border, int format,
                                    int type, float[] pixels) {
        GlObjects.textureImage(target, level, internalformat, width, 1);
    }

    @Patch(owner = {GL11C, GL11})
    public static void glTexImage2D(int target, int level, int internalformat, int width, int height, int border,
                                    int format, int type, double[] pixels) {
        GlObjects.textureImage(target, level, internalformat, width, height);
    }

    @Patch(owner = {GL11C, GL11})
    public static void glTexImage1D(int target, int level, int internalformat, int width, int border, int format,
                                    int type, double[] pixels) {
        GlObjects.textureImage(target, level, internalformat, width, 1);
    }

    @Patch(owner = {GL42C, GL42})
    public static void glTexStorage2D(int target, int levels, int internalformat, int width, int height) {
        for (int level = 0; level < levels; level++) {
            GlObjects.textureImage(target, level, internalformat, Math.max(1, width >> level), Math.max(1, height >> level));
        }
    }

    @Patch(owner = {GL11C, GL11})
    public static int glGetTexLevelParameteri(int target, int level, int pname) {
        return GlObjects.textureLevelValue(target, level, pname);
    }

    @Patch(owner = {GL11C, GL11})
    public static float glGetTexLevelParameterf(int target, int level, int pname) {
        return GlObjects.textureLevelValue(target, level, pname);
    }

    @Patch(owner = {GL11C, GL11})
    public static void glGetTexLevelParameteriv(int target, int level, int pname, IntBuffer params) {
        params.put(params.position(), GlObjects.textureLevelValue(target, level, pname));
    }

    @Patch(owner = {GL11C, GL11})
    public static void glGetTexLevelParameteriv(int target, int level, int pname, int[] params) {
        params[0] = GlObjects.textureLevelValue(target, level, pname);
    }

}
