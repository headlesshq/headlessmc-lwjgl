package io.github.headlesshq.headlessmc.lwjgl3.runtime.opengl;

import io.github.headlesshq.headlessmc.lwjgl3.runtime.Patch;

import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.lwjgl.opengl.GL46C.*;

/**
 * Patches that track the size and internal format of texture images, so that {@code glGetTexLevelParameter*}
 * returns what the application uploaded.
 */
public final class GlTexturePatches {
    private static final String GL11 = "org/lwjgl/opengl/GL11";
    private static final String GL11C = "org/lwjgl/opengl/GL11C";
    private static final String GL42 = "org/lwjgl/opengl/GL42";
    private static final String GL42C = "org/lwjgl/opengl/GL42C";
    private static final Map<Long, int[]> LEVELS = new ConcurrentHashMap<>();

    private GlTexturePatches() {
        throw new AssertionError();
    }

    @Patch(owner = {GL11C, GL11})
    public static void glTexImage2D(int target, int level, int internalformat, int width, int height, int border,
                                    int format, int type, ByteBuffer pixels) {
        image(target, level, internalformat, width, height);
    }

    @Patch(owner = {GL11C, GL11})
    public static void glTexImage1D(int target, int level, int internalformat, int width, int border, int format,
                                    int type, ByteBuffer pixels) {
        image(target, level, internalformat, width, 1);
    }

    @Patch(owner = {GL11C, GL11})
    public static void glTexImage2D(int target, int level, int internalformat, int width, int height, int border,
                                    int format, int type, long pixels) {
        image(target, level, internalformat, width, height);
    }

    @Patch(owner = {GL11C, GL11})
    public static void glTexImage1D(int target, int level, int internalformat, int width, int border, int format,
                                    int type, long pixels) {
        image(target, level, internalformat, width, 1);
    }

    @Patch(owner = {GL11C, GL11})
    public static void glTexImage2D(int target, int level, int internalformat, int width, int height, int border,
                                    int format, int type, ShortBuffer pixels) {
        image(target, level, internalformat, width, height);
    }

    @Patch(owner = {GL11C, GL11})
    public static void glTexImage1D(int target, int level, int internalformat, int width, int border, int format,
                                    int type, ShortBuffer pixels) {
        image(target, level, internalformat, width, 1);
    }

    @Patch(owner = {GL11C, GL11})
    public static void glTexImage2D(int target, int level, int internalformat, int width, int height, int border,
                                    int format, int type, IntBuffer pixels) {
        image(target, level, internalformat, width, height);
    }

    @Patch(owner = {GL11C, GL11})
    public static void glTexImage1D(int target, int level, int internalformat, int width, int border, int format,
                                    int type, IntBuffer pixels) {
        image(target, level, internalformat, width, 1);
    }

    @Patch(owner = {GL11C, GL11})
    public static void glTexImage2D(int target, int level, int internalformat, int width, int height, int border,
                                    int format, int type, FloatBuffer pixels) {
        image(target, level, internalformat, width, height);
    }

    @Patch(owner = {GL11C, GL11})
    public static void glTexImage1D(int target, int level, int internalformat, int width, int border, int format,
                                    int type, FloatBuffer pixels) {
        image(target, level, internalformat, width, 1);
    }

    @Patch(owner = {GL11C, GL11})
    public static void glTexImage2D(int target, int level, int internalformat, int width, int height, int border,
                                    int format, int type, DoubleBuffer pixels) {
        image(target, level, internalformat, width, height);
    }

    @Patch(owner = {GL11C, GL11})
    public static void glTexImage1D(int target, int level, int internalformat, int width, int border, int format,
                                    int type, DoubleBuffer pixels) {
        image(target, level, internalformat, width, 1);
    }

    @Patch(owner = {GL11C, GL11})
    public static void glTexImage2D(int target, int level, int internalformat, int width, int height, int border,
                                    int format, int type, short[] pixels) {
        image(target, level, internalformat, width, height);
    }

    @Patch(owner = {GL11C, GL11})
    public static void glTexImage1D(int target, int level, int internalformat, int width, int border, int format,
                                    int type, short[] pixels) {
        image(target, level, internalformat, width, 1);
    }

    @Patch(owner = {GL11C, GL11})
    public static void glTexImage2D(int target, int level, int internalformat, int width, int height, int border,
                                    int format, int type, int[] pixels) {
        image(target, level, internalformat, width, height);
    }

    @Patch(owner = {GL11C, GL11})
    public static void glTexImage1D(int target, int level, int internalformat, int width, int border, int format,
                                    int type, int[] pixels) {
        image(target, level, internalformat, width, 1);
    }

    @Patch(owner = {GL11C, GL11})
    public static void glTexImage2D(int target, int level, int internalformat, int width, int height, int border,
                                    int format, int type, float[] pixels) {
        image(target, level, internalformat, width, height);
    }

    @Patch(owner = {GL11C, GL11})
    public static void glTexImage1D(int target, int level, int internalformat, int width, int border, int format,
                                    int type, float[] pixels) {
        image(target, level, internalformat, width, 1);
    }

    @Patch(owner = {GL11C, GL11})
    public static void glTexImage2D(int target, int level, int internalformat, int width, int height, int border,
                                    int format, int type, double[] pixels) {
        image(target, level, internalformat, width, height);
    }

    @Patch(owner = {GL11C, GL11})
    public static void glTexImage1D(int target, int level, int internalformat, int width, int border, int format,
                                    int type, double[] pixels) {
        image(target, level, internalformat, width, 1);
    }

    @Patch(owner = {GL42C, GL42})
    public static void glTexStorage2D(int target, int levels, int internalformat, int width, int height) {
        for (int level = 0; level < levels; level++) {
            image(target, level, internalformat, Math.max(1, width >> level), Math.max(1, height >> level));
        }
    }

    @Patch(owner = {GL11C, GL11})
    public static int glGetTexLevelParameteri(int target, int level, int pname) {
        return levelValue(target, level, pname);
    }

    @Patch(owner = {GL11C, GL11})
    public static float glGetTexLevelParameterf(int target, int level, int pname) {
        return levelValue(target, level, pname);
    }

    @Patch(owner = {GL11C, GL11})
    public static void glGetTexLevelParameteriv(int target, int level, int pname, IntBuffer params) {
        params.put(params.position(), levelValue(target, level, pname));
    }

    @Patch(owner = {GL11C, GL11})
    public static void glGetTexLevelParameteriv(int target, int level, int pname, int[] params) {
        params[0] = levelValue(target, level, pname);
    }

    private static void image(int target, int level, int internalformat, int width, int height) {
        LEVELS.put(key(target, level), new int[]{width, height, internalformat});
    }

    private static int levelValue(int target, int level, int pname) {
        int[] image = LEVELS.get(key(target, level));
        switch (pname) {
            case GL_TEXTURE_WIDTH:
                return image == null ? 0 : image[0];
            case GL_TEXTURE_HEIGHT:
                return image == null ? 0 : image[1];
            case GL_TEXTURE_DEPTH:
                return image == null ? 0 : 1;
            case GL_TEXTURE_INTERNAL_FORMAT:
                return image == null ? GL_RGBA : image[2];
            default:
                return 0;
        }
    }

    private static long key(int target, int level) {
        boolean cubeFace = target >= GL_TEXTURE_CUBE_MAP_POSITIVE_X && target <= GL_TEXTURE_CUBE_MAP_NEGATIVE_Z;
        int face = cubeFace ? target - GL_TEXTURE_CUBE_MAP_POSITIVE_X : 0;
        return ((long) GlState.boundTexture(target) << 32) | ((long) face << 16) | (level & 0xFFFF);
    }

}
