package io.github.headlesshq.headlessmc.lwjgl3.runtime.opengl;

import io.github.headlesshq.headlessmc.lwjgl.runtime.Patch;
import io.github.headlesshq.headlessmc.lwjgl.runtime.opengl.GlObjects;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL46C.*;

/**
 * Patches that record texture parameters, framebuffer attachments and renderbuffer storage in {@link GlObjects}, and
 * answer the queries for them and for internal formats.
 */
public final class GlObjectPatches {
    private static final String GL11 = "org/lwjgl/opengl/GL11";
    private static final String GL11C = "org/lwjgl/opengl/GL11C";
    private static final String GL30 = "org/lwjgl/opengl/GL30";
    private static final String GL30C = "org/lwjgl/opengl/GL30C";
    private static final String GL32 = "org/lwjgl/opengl/GL32";
    private static final String GL32C = "org/lwjgl/opengl/GL32C";
    private static final String GL42 = "org/lwjgl/opengl/GL42";
    private static final String GL42C = "org/lwjgl/opengl/GL42C";
    private static final String ARB_FRAMEBUFFER_OBJECT = "org/lwjgl/opengl/ARBFramebufferObject";

    private GlObjectPatches() {
        throw new AssertionError();
    }

    @Patch(owner = {GL11C, GL11})
    public static void glTexParameteri(int target, int pname, int param) {
        GlObjects.textureParameter(target, pname, param);
    }

    @Patch(owner = {GL11C, GL11})
    public static void glTexParameterf(int target, int pname, float param) {
        GlObjects.textureParameter(target, pname, param);
    }

    @Patch(owner = {GL11C, GL11})
    public static void glTexParameteriv(int target, int pname, IntBuffer params) {
        double[] values = new double[params.remaining()];
        for (int i = 0; i < values.length; i++) {
            values[i] = params.get(params.position() + i);
        }

        GlObjects.textureParameter(target, pname, values);
    }

    @Patch(owner = {GL11C, GL11})
    public static void glTexParameteriv(int target, int pname, int[] params) {
        double[] values = new double[params.length];
        for (int i = 0; i < values.length; i++) {
            values[i] = params[i];
        }

        GlObjects.textureParameter(target, pname, values);
    }

    @Patch(owner = {GL11C, GL11})
    public static void glTexParameterfv(int target, int pname, FloatBuffer params) {
        double[] values = new double[params.remaining()];
        for (int i = 0; i < values.length; i++) {
            values[i] = params.get(params.position() + i);
        }

        GlObjects.textureParameter(target, pname, values);
    }

    @Patch(owner = {GL11C, GL11})
    public static void glTexParameterfv(int target, int pname, float[] params) {
        double[] values = new double[params.length];
        for (int i = 0; i < values.length; i++) {
            values[i] = params[i];
        }

        GlObjects.textureParameter(target, pname, values);
    }

    @Patch(owner = {GL11C, GL11})
    public static int glGetTexParameteri(int target, int pname) {
        return (int) Math.round(GlObjects.textureParameter(target, pname)[0]);
    }

    @Patch(owner = {GL11C, GL11})
    public static float glGetTexParameterf(int target, int pname) {
        return (float) GlObjects.textureParameter(target, pname)[0];
    }

    @Patch(owner = {GL11C, GL11})
    public static void glGetTexParameteriv(int target, int pname, IntBuffer params) {
        double[] values = GlObjects.textureParameter(target, pname);
        for (int i = 0; i < values.length && i < params.remaining(); i++) {
            params.put(params.position() + i, (int) Math.round(values[i]));
        }
    }

    @Patch(owner = {GL11C, GL11})
    public static void glGetTexParameteriv(int target, int pname, int[] params) {
        double[] values = GlObjects.textureParameter(target, pname);
        for (int i = 0; i < values.length && i < params.length; i++) {
            params[i] = (int) Math.round(values[i]);
        }
    }

    @Patch(owner = {GL11C, GL11})
    public static void glGetTexParameterfv(int target, int pname, FloatBuffer params) {
        double[] values = GlObjects.textureParameter(target, pname);
        for (int i = 0; i < values.length && i < params.remaining(); i++) {
            params.put(params.position() + i, (float) values[i]);
        }
    }

    @Patch(owner = {GL11C, GL11})
    public static void glGetTexParameterfv(int target, int pname, float[] params) {
        double[] values = GlObjects.textureParameter(target, pname);
        for (int i = 0; i < values.length && i < params.length; i++) {
            params[i] = (float) values[i];
        }
    }

    @Patch(owner = {GL30C, GL30, ARB_FRAMEBUFFER_OBJECT})
    public static void glFramebufferTexture2D(int target, int attachment, int textarget, int texture, int level) {
        GlObjects.attach(target, attachment, GL_TEXTURE, texture, level, 0);
    }

    @Patch(owner = {GL30C, GL30, ARB_FRAMEBUFFER_OBJECT})
    public static void glFramebufferTextureLayer(int target, int attachment, int texture, int level, int layer) {
        GlObjects.attach(target, attachment, GL_TEXTURE, texture, level, layer);
    }

    @Patch(owner = {GL32C, GL32})
    public static void glFramebufferTexture(int target, int attachment, int texture, int level) {
        GlObjects.attach(target, attachment, GL_TEXTURE, texture, level, 0);
    }

    @Patch(owner = {GL30C, GL30, ARB_FRAMEBUFFER_OBJECT})
    public static void glFramebufferRenderbuffer(int target, int attachment, int renderbuffertarget, int renderbuffer) {
        GlObjects.attach(target, attachment, GL_RENDERBUFFER, renderbuffer, 0, 0);
    }

    @Patch(owner = {GL30C, GL30, ARB_FRAMEBUFFER_OBJECT})
    public static int glGetFramebufferAttachmentParameteri(int target, int attachment, int pname) {
        return GlObjects.attachmentValue(target, attachment, pname);
    }

    @Patch(owner = {GL30C, GL30, ARB_FRAMEBUFFER_OBJECT})
    public static void glGetFramebufferAttachmentParameteriv(int target, int attachment, int pname, IntBuffer params) {
        params.put(params.position(), GlObjects.attachmentValue(target, attachment, pname));
    }

    @Patch(owner = {GL30C, GL30, ARB_FRAMEBUFFER_OBJECT})
    public static void glGetFramebufferAttachmentParameteriv(int target, int attachment, int pname, int[] params) {
        params[0] = GlObjects.attachmentValue(target, attachment, pname);
    }

    @Patch(owner = {GL30C, GL30, ARB_FRAMEBUFFER_OBJECT})
    public static void glRenderbufferStorage(int target, int internalformat, int width, int height) {
        GlObjects.renderbufferStorage(internalformat, 0, width, height);
    }

    @Patch(owner = {GL30C, GL30, ARB_FRAMEBUFFER_OBJECT})
    public static void glRenderbufferStorageMultisample(int target, int samples, int internalformat, int width, int height) {
        GlObjects.renderbufferStorage(internalformat, samples, width, height);
    }

    @Patch(owner = {GL30C, GL30, ARB_FRAMEBUFFER_OBJECT})
    public static int glGetRenderbufferParameteri(int target, int pname) {
        return GlObjects.renderbufferValue(pname);
    }

    @Patch(owner = {GL30C, GL30, ARB_FRAMEBUFFER_OBJECT})
    public static void glGetRenderbufferParameteriv(int target, int pname, IntBuffer params) {
        params.put(params.position(), GlObjects.renderbufferValue(pname));
    }

    @Patch(owner = {GL30C, GL30, ARB_FRAMEBUFFER_OBJECT})
    public static void glGetRenderbufferParameteriv(int target, int pname, int[] params) {
        params[0] = GlObjects.renderbufferValue(pname);
    }

    @Patch(owner = {GL42C, GL42})
    public static int glGetInternalformati(int target, int internalformat, int pname) {
        return GlObjects.internalformatValues(target, internalformat, pname)[0];
    }

    @Patch(owner = {GL42C, GL42})
    public static void glGetInternalformativ(int target, int internalformat, int pname, IntBuffer params) {
        int[] values = GlObjects.internalformatValues(target, internalformat, pname);
        for (int i = 0; i < values.length && i < params.remaining(); i++) {
            params.put(params.position() + i, values[i]);
        }
    }

    @Patch(owner = {GL42C, GL42})
    public static void glGetInternalformativ(int target, int internalformat, int pname, int[] params) {
        int[] values = GlObjects.internalformatValues(target, internalformat, pname);
        System.arraycopy(values, 0, params, 0, Math.min(values.length, params.length));
    }

}
