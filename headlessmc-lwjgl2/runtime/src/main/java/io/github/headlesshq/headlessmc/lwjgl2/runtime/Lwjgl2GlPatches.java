package io.github.headlesshq.headlessmc.lwjgl2.runtime;

import io.github.headlesshq.headlessmc.lwjgl.runtime.Patch;
import io.github.headlesshq.headlessmc.lwjgl.runtime.opengl.GlInfo;
import io.github.headlesshq.headlessmc.lwjgl.runtime.opengl.GlObjects;
import io.github.headlesshq.headlessmc.lwjgl.runtime.opengl.GlState;
import io.github.headlesshq.headlessmc.lwjgl.runtime.opengl.GlValues;

import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_ACTIVE_TEXTURE;
import static org.lwjgl.opengl.GL14.*;
import static org.lwjgl.opengl.GL20.GL_BLEND_EQUATION_ALPHA;
import static org.lwjgl.opengl.GL20.GL_BLEND_EQUATION_RGB;
import static org.lwjgl.opengl.GL20.GL_CURRENT_PROGRAM;
import static org.lwjgl.opengl.GL30.GL_RENDERBUFFER_BINDING;
import static org.lwjgl.opengl.GL30.GL_VERTEX_ARRAY_BINDING;

/**
 * Patches for the OpenGL functions of LWJGL 2: the {@code glGet*} queries answered by {@link GlValues} and
 * {@link GlState}, the state setters, shader, program and texture level queries answered by {@link GlObjects}, and
 * the strings of {@link GlInfo}.
 */
public final class Lwjgl2GlPatches {
    private static final String GL11 = "org/lwjgl/opengl/GL11";
    private static final String GL13 = "org/lwjgl/opengl/GL13";
    private static final String GL14 = "org/lwjgl/opengl/GL14";
    private static final String GL15 = "org/lwjgl/opengl/GL15";
    private static final String GL20 = "org/lwjgl/opengl/GL20";
    private static final String ARB_SHADER_OBJECTS = "org/lwjgl/opengl/ARBShaderObjects";
    private static final String GL30 = "org/lwjgl/opengl/GL30";
    private static final String GL32 = "org/lwjgl/opengl/GL32";
    private static final String GL33 = "org/lwjgl/opengl/GL33";
    private static final String EXT_FRAMEBUFFER_OBJECT = "org/lwjgl/opengl/EXTFramebufferObject";

    private Lwjgl2GlPatches() {
        throw new AssertionError();
    }

    @Patch(owner = GL11)
    public static int glGetInteger(int pname) {
        return GlValues.getInteger(pname);
    }

    @Patch(owner = GL11)
    public static void glGetInteger(int pname, IntBuffer params) {
        int[] values = GlValues.get(pname);
        for (int i = 0; i < values.length && params.position() + i < params.limit(); i++) {
            params.put(params.position() + i, values[i]);
        }
    }

    @Patch(owner = GL11)
    public static float glGetFloat(int pname) {
        return (float) GlValues.getDoubles(pname)[0];
    }

    @Patch(owner = GL11)
    public static void glGetFloat(int pname, FloatBuffer params) {
        double[] values = GlValues.getDoubles(pname);
        for (int i = 0; i < values.length && params.position() + i < params.limit(); i++) {
            params.put(params.position() + i, (float) values[i]);
        }
    }

    @Patch(owner = GL11)
    public static double glGetDouble(int pname) {
        return GlValues.getDoubles(pname)[0];
    }

    @Patch(owner = GL11)
    public static void glGetDouble(int pname, DoubleBuffer params) {
        double[] values = GlValues.getDoubles(pname);
        for (int i = 0; i < values.length && params.position() + i < params.limit(); i++) {
            params.put(params.position() + i, values[i]);
        }
    }

    @Patch(owner = GL11)
    public static boolean glGetBoolean(int pname) {
        return GlValues.getDoubles(pname)[0] != 0.0;
    }

    @Patch(owner = GL11)
    public static void glGetBoolean(int pname, ByteBuffer params) {
        double[] values = GlValues.getDoubles(pname);
        for (int i = 0; i < values.length && params.position() + i < params.limit(); i++) {
            params.put(params.position() + i, (byte) (values[i] != 0.0 ? 1 : 0));
        }
    }

    @Patch(owner = GL32)
    public static long glGetInteger64(int pname) {
        return GlValues.getLong(pname);
    }

    @Patch(owner = GL32)
    public static void glGetInteger64(int pname, LongBuffer params) {
        long[] values = GlValues.getLongs(pname);
        for (int i = 0; i < values.length && params.position() + i < params.limit(); i++) {
            params.put(params.position() + i, values[i]);
        }
    }

    @Patch(owner = GL11)
    public static String glGetString(int name) {
        return GlInfo.string(name);
    }

    @Patch(owner = GL30)
    public static String glGetStringi(int name, int index) {
        return GlInfo.string(name, index);
    }

    @Patch(owner = GL11)
    public static boolean glIsEnabled(int cap) {
        return GlState.isEnabled(cap);
    }

    @Patch(owner = GL11)
    public static void glEnable(int cap) {
        GlState.setEnabled(cap, true);
    }

    @Patch(owner = GL11)
    public static void glDisable(int cap) {
        GlState.setEnabled(cap, false);
    }

    @Patch(owner = GL11)
    public static void glViewport(int x, int y, int width, int height) {
        GlState.set(GL_VIEWPORT, x, y, width, height);
    }

    @Patch(owner = GL11)
    public static void glScissor(int x, int y, int width, int height) {
        GlState.set(GL_SCISSOR_BOX, x, y, width, height);
    }

    @Patch(owner = GL11)
    public static void glBindTexture(int target, int texture) {
        GlState.bindTexture(target, texture);
    }

    @Patch(owner = GL13)
    public static void glActiveTexture(int texture) {
        GlState.set(GL_ACTIVE_TEXTURE, texture);
    }

    @Patch(owner = GL33)
    public static void glBindSampler(int unit, int sampler) {
        GlState.bindSampler(unit, sampler);
    }

    @Patch(owner = GL15)
    public static void glBindBuffer(int target, int buffer) {
        GlState.bindBuffer(target, buffer);
    }

    @Patch(owner = GL30)
    public static void glBindFramebuffer(int target, int framebuffer) {
        GlState.bindFramebuffer(target, framebuffer);
    }

    @Patch(owner = EXT_FRAMEBUFFER_OBJECT)
    public static void glBindFramebufferEXT(int target, int framebuffer) {
        GlState.bindFramebuffer(target, framebuffer);
    }

    @Patch(owner = GL30)
    public static void glBindRenderbuffer(int target, int renderbuffer) {
        GlState.set(GL_RENDERBUFFER_BINDING, renderbuffer);
    }

    @Patch(owner = EXT_FRAMEBUFFER_OBJECT)
    public static void glBindRenderbufferEXT(int target, int renderbuffer) {
        GlState.set(GL_RENDERBUFFER_BINDING, renderbuffer);
    }

    @Patch(owner = GL30)
    public static void glBindVertexArray(int array) {
        GlState.set(GL_VERTEX_ARRAY_BINDING, array);
    }

    @Patch(owner = GL20)
    public static void glUseProgram(int program) {
        GlState.set(GL_CURRENT_PROGRAM, program);
    }

    @Patch(owner = GL11)
    public static void glBlendFunc(int sfactor, int dfactor) {
        glBlendFuncSeparate(sfactor, dfactor, sfactor, dfactor);
    }

    @Patch(owner = GL14)
    public static void glBlendFuncSeparate(int sfactorRGB, int dfactorRGB, int sfactorAlpha, int dfactorAlpha) {
        GlState.set(GL_BLEND_SRC_RGB, sfactorRGB);
        GlState.set(GL_BLEND_DST_RGB, dfactorRGB);
        GlState.set(GL_BLEND_SRC_ALPHA, sfactorAlpha);
        GlState.set(GL_BLEND_DST_ALPHA, dfactorAlpha);
    }

    @Patch(owner = GL14)
    public static void glBlendEquation(int mode) {
        GlState.set(GL_BLEND_EQUATION_RGB, mode);
        GlState.set(GL_BLEND_EQUATION_ALPHA, mode);
    }

    @Patch(owner = GL11)
    public static void glDepthFunc(int func) {
        GlState.set(GL_DEPTH_FUNC, func);
    }

    @Patch(owner = GL11)
    public static void glDepthMask(boolean flag) {
        GlState.set(GL_DEPTH_WRITEMASK, flag ? 1 : 0);
    }

    @Patch(owner = GL11)
    public static void glColorMask(boolean red, boolean green, boolean blue, boolean alpha) {
        GlState.set(GL_COLOR_WRITEMASK, red ? 1 : 0, green ? 1 : 0, blue ? 1 : 0, alpha ? 1 : 0);
    }

    @Patch(owner = GL11)
    public static void glClearColor(float red, float green, float blue, float alpha) {
        GlState.set(GL_COLOR_CLEAR_VALUE, red, green, blue, alpha);
    }

    @Patch(owner = GL11)
    public static void glClearDepth(double depth) {
        GlState.set(GL_DEPTH_CLEAR_VALUE, depth);
    }

    @Patch(owner = GL11)
    public static void glStencilMask(int mask) {
        GlState.set(GL_STENCIL_WRITEMASK, mask & 0xFF);
    }

    @Patch(owner = GL11)
    public static void glCullFace(int mode) {
        GlState.set(GL_CULL_FACE_MODE, mode);
    }

    @Patch(owner = GL11)
    public static void glFrontFace(int mode) {
        GlState.set(GL_FRONT_FACE, mode);
    }

    @Patch(owner = GL11)
    public static void glPolygonMode(int face, int mode) {
        GlState.set(GL_POLYGON_MODE, mode, mode);
    }

    @Patch(owner = GL11)
    public static void glPolygonOffset(float factor, float units) {
        GlState.set(GL_POLYGON_OFFSET_FACTOR, factor);
        GlState.set(GL_POLYGON_OFFSET_UNITS, units);
    }

    @Patch(owner = GL11)
    public static void glLineWidth(float width) {
        GlState.set(GL_LINE_WIDTH, width);
    }

    @Patch(owner = GL11)
    public static void glPixelStorei(int pname, int param) {
        GlState.set(pname, param);
    }

    @Patch(owner = GL11)
    public static void glDrawBuffer(int mode) {
        GlState.set(GL_DRAW_BUFFER, mode);
    }

    @Patch(owner = GL11)
    public static void glReadBuffer(int mode) {
        GlState.set(GL_READ_BUFFER, mode);
    }

    @Patch(owner = GL20)
    public static int glCreateShader(int type) {
        return GlObjects.createShader(type);
    }

    @Patch(owner = GL20)
    public static void glDeleteShader(int shader) {
        GlObjects.deleteShader(shader);
    }

    @Patch(owner = GL20)
    public static void glAttachShader(int program, int shader) {
        GlObjects.attachShader(program);
    }

    @Patch(owner = GL20)
    public static void glDetachShader(int program, int shader) {
        GlObjects.detachShader(program);
    }

    @Patch(owner = GL20)
    public static void glDeleteProgram(int program) {
        GlObjects.deleteProgram(program);
    }

    @Patch(owner = GL20)
    public static int glGetShaderi(int shader, int pname) {
        return GlObjects.shaderValue(shader, pname);
    }

    @Patch(owner = GL20)
    public static int glGetShader(int shader, int pname) {
        return GlObjects.shaderValue(shader, pname);
    }

    @Patch(owner = GL20)
    public static void glGetShader(int shader, int pname, IntBuffer params) {
        params.put(params.position(), GlObjects.shaderValue(shader, pname));
    }

    /**
     * Shader objects and program objects share their names, the status tokens of {@code ARB_shader_objects} have the
     * values of the GL 2.0 ones, e.g. {@code GL_OBJECT_COMPILE_STATUS_ARB} is {@code GL_COMPILE_STATUS}.
     */
    @Patch(owner = ARB_SHADER_OBJECTS)
    public static int glGetObjectParameteriARB(int obj, int pname) {
        int shaderValue = GlObjects.shaderValue(obj, pname);
        return shaderValue != 0 ? shaderValue : GlObjects.programValue(obj, pname);
    }

    @Patch(owner = ARB_SHADER_OBJECTS)
    public static void glGetObjectParameterARB(int obj, int pname, IntBuffer params) {
        params.put(params.position(), glGetObjectParameteriARB(obj, pname));
    }

    @Patch(owner = GL20)
    public static int glGetProgrami(int program, int pname) {
        return GlObjects.programValue(program, pname);
    }

    @Patch(owner = GL20)
    public static int glGetProgram(int program, int pname) {
        return GlObjects.programValue(program, pname);
    }

    @Patch(owner = GL20)
    public static void glGetProgram(int program, int pname, IntBuffer params) {
        params.put(params.position(), GlObjects.programValue(program, pname));
    }

    @Patch(owner = GL11)
    public static int glGetTexLevelParameteri(int target, int level, int pname) {
        return GlObjects.textureLevelValue(target, level, pname);
    }

    @Patch(owner = GL11)
    public static float glGetTexLevelParameterf(int target, int level, int pname) {
        return GlObjects.textureLevelValue(target, level, pname);
    }

    @Patch(owner = GL11)
    public static void glGetTexLevelParameter(int target, int level, int pname, IntBuffer params) {
        params.put(params.position(), GlObjects.textureLevelValue(target, level, pname));
    }

    @Patch(owner = GL11)
    public static void glGetTexLevelParameter(int target, int level, int pname, FloatBuffer params) {
        params.put(params.position(), GlObjects.textureLevelValue(target, level, pname));
    }

    @Patch(owner = GL11)
    public static void glTexImage2D(int target, int level, int internalformat, int width, int height, int border,
                                    int format, int type, ByteBuffer pixels) {
        GlObjects.textureImage(target, level, internalformat, width, height);
    }

    @Patch(owner = GL11)
    public static void glTexImage1D(int target, int level, int internalformat, int width, int border, int format,
                                    int type, ByteBuffer pixels) {
        GlObjects.textureImage(target, level, internalformat, width, 1);
    }

    @Patch(owner = GL11)
    public static void glTexImage2D(int target, int level, int internalformat, int width, int height, int border,
                                    int format, int type, DoubleBuffer pixels) {
        GlObjects.textureImage(target, level, internalformat, width, height);
    }

    @Patch(owner = GL11)
    public static void glTexImage1D(int target, int level, int internalformat, int width, int border, int format,
                                    int type, DoubleBuffer pixels) {
        GlObjects.textureImage(target, level, internalformat, width, 1);
    }

    @Patch(owner = GL11)
    public static void glTexImage2D(int target, int level, int internalformat, int width, int height, int border,
                                    int format, int type, FloatBuffer pixels) {
        GlObjects.textureImage(target, level, internalformat, width, height);
    }

    @Patch(owner = GL11)
    public static void glTexImage1D(int target, int level, int internalformat, int width, int border, int format,
                                    int type, FloatBuffer pixels) {
        GlObjects.textureImage(target, level, internalformat, width, 1);
    }

    @Patch(owner = GL11)
    public static void glTexImage2D(int target, int level, int internalformat, int width, int height, int border,
                                    int format, int type, IntBuffer pixels) {
        GlObjects.textureImage(target, level, internalformat, width, height);
    }

    @Patch(owner = GL11)
    public static void glTexImage1D(int target, int level, int internalformat, int width, int border, int format,
                                    int type, IntBuffer pixels) {
        GlObjects.textureImage(target, level, internalformat, width, 1);
    }

    @Patch(owner = GL11)
    public static void glTexImage2D(int target, int level, int internalformat, int width, int height, int border,
                                    int format, int type, ShortBuffer pixels) {
        GlObjects.textureImage(target, level, internalformat, width, height);
    }

    @Patch(owner = GL11)
    public static void glTexImage1D(int target, int level, int internalformat, int width, int border, int format,
                                    int type, ShortBuffer pixels) {
        GlObjects.textureImage(target, level, internalformat, width, 1);
    }

    @Patch(owner = GL11)
    public static void glTexImage2D(int target, int level, int internalformat, int width, int height, int border,
                                    int format, int type, long pixels) {
        GlObjects.textureImage(target, level, internalformat, width, height);
    }

    @Patch(owner = GL11)
    public static void glTexImage1D(int target, int level, int internalformat, int width, int border, int format,
                                    int type, long pixels) {
        GlObjects.textureImage(target, level, internalformat, width, 1);
    }

    /**
     * Texture parameters are recorded and read back like with LWJGL 3, Minecraft 1.7 to 1.12 read their filters.
     */
    @Patch(owner = GL11)
    public static void glTexParameteri(int target, int pname, int param) {
        GlObjects.textureParameter(target, pname, param);
    }

    @Patch(owner = GL11)
    public static void glTexParameterf(int target, int pname, float param) {
        GlObjects.textureParameter(target, pname, param);
    }

    @Patch(owner = GL11)
    public static void glTexParameter(int target, int pname, IntBuffer params) {
        double[] values = new double[params.remaining()];
        for (int i = 0; i < values.length; i++) {
            values[i] = params.get(params.position() + i);
        }

        GlObjects.textureParameter(target, pname, values);
    }

    @Patch(owner = GL11)
    public static void glTexParameter(int target, int pname, FloatBuffer params) {
        double[] values = new double[params.remaining()];
        for (int i = 0; i < values.length; i++) {
            values[i] = params.get(params.position() + i);
        }

        GlObjects.textureParameter(target, pname, values);
    }

    @Patch(owner = GL11)
    public static int glGetTexParameteri(int target, int pname) {
        return (int) Math.round(GlObjects.textureParameter(target, pname)[0]);
    }

    @Patch(owner = GL11)
    public static float glGetTexParameterf(int target, int pname) {
        return (float) GlObjects.textureParameter(target, pname)[0];
    }

    @Patch(owner = GL11)
    public static void glGetTexParameter(int target, int pname, IntBuffer params) {
        double[] values = GlObjects.textureParameter(target, pname);
        for (int i = 0; i < values.length && i < params.remaining(); i++) {
            params.put(params.position() + i, (int) Math.round(values[i]));
        }
    }

    @Patch(owner = GL11)
    public static void glGetTexParameter(int target, int pname, FloatBuffer params) {
        double[] values = GlObjects.textureParameter(target, pname);
        for (int i = 0; i < values.length && i < params.remaining(); i++) {
            params.put(params.position() + i, (float) values[i]);
        }
    }

}
