package io.github.headlesshq.headlessmc.lwjgl3.runtime.opengl;

import io.github.headlesshq.headlessmc.lwjgl3.runtime.Patch;

import static org.lwjgl.opengl.GL46C.*;

/**
 * Patches for the OpenGL functions that set state the application can query back, see {@link GlState}.
 */
public final class GlStatePatches {
    private static final String GL11 = "org/lwjgl/opengl/GL11";
    private static final String GL11C = "org/lwjgl/opengl/GL11C";
    private static final String GL13 = "org/lwjgl/opengl/GL13";
    private static final String GL13C = "org/lwjgl/opengl/GL13C";
    private static final String GL14 = "org/lwjgl/opengl/GL14";
    private static final String GL14C = "org/lwjgl/opengl/GL14C";
    private static final String GL15 = "org/lwjgl/opengl/GL15";
    private static final String GL15C = "org/lwjgl/opengl/GL15C";
    private static final String GL20 = "org/lwjgl/opengl/GL20";
    private static final String GL20C = "org/lwjgl/opengl/GL20C";
    private static final String GL30 = "org/lwjgl/opengl/GL30";
    private static final String GL30C = "org/lwjgl/opengl/GL30C";
    private static final String GL33 = "org/lwjgl/opengl/GL33";
    private static final String GL33C = "org/lwjgl/opengl/GL33C";
    private static final String ARB_FRAMEBUFFER_OBJECT = "org/lwjgl/opengl/ARBFramebufferObject";

    private GlStatePatches() {
        throw new AssertionError();
    }

    @Patch(owner = {GL11C, GL11})
    public static void glViewport(int x, int y, int width, int height) {
        GlState.set(GL_VIEWPORT, x, y, width, height);
    }

    @Patch(owner = {GL11C, GL11})
    public static void glScissor(int x, int y, int width, int height) {
        GlState.set(GL_SCISSOR_BOX, x, y, width, height);
    }

    @Patch(owner = {GL11C, GL11})
    public static void glEnable(int target) {
        GlState.setEnabled(target, true);
    }

    @Patch(owner = {GL11C, GL11})
    public static void glDisable(int target) {
        GlState.setEnabled(target, false);
    }

    @Patch(owner = {GL11C, GL11})
    public static boolean glIsEnabled(int cap) {
        return GlState.isEnabled(cap);
    }

    @Patch(owner = {GL11C, GL11})
    public static void glBindTexture(int target, int texture) {
        GlState.bindTexture(target, texture);
    }

    @Patch(owner = {GL13C, GL13})
    public static void glActiveTexture(int texture) {
        GlState.set(GL_ACTIVE_TEXTURE, texture);
    }

    @Patch(owner = {GL33C, GL33})
    public static void glBindSampler(int unit, int sampler) {
        GlState.bindSampler(unit, sampler);
    }

    @Patch(owner = {GL15C, GL15})
    public static void glBindBuffer(int target, int buffer) {
        GlState.bindBuffer(target, buffer);
    }

    @Patch(owner = {GL30C, GL30, ARB_FRAMEBUFFER_OBJECT})
    public static void glBindFramebuffer(int target, int framebuffer) {
        GlState.bindFramebuffer(target, framebuffer);
    }

    @Patch(owner = {GL30C, GL30, ARB_FRAMEBUFFER_OBJECT})
    public static void glBindRenderbuffer(int target, int renderbuffer) {
        GlState.set(GL_RENDERBUFFER_BINDING, renderbuffer);
    }

    @Patch(owner = {GL30C, GL30})
    public static void glBindVertexArray(int array) {
        GlState.set(GL_VERTEX_ARRAY_BINDING, array);
    }

    @Patch(owner = {GL20C, GL20})
    public static void glUseProgram(int program) {
        GlState.set(GL_CURRENT_PROGRAM, program);
    }

    @Patch(owner = {GL11C, GL11})
    public static void glBlendFunc(int sfactor, int dfactor) {
        glBlendFuncSeparate(sfactor, dfactor, sfactor, dfactor);
    }

    @Patch(owner = {GL14C, GL14})
    public static void glBlendFuncSeparate(int sfactorRGB, int dfactorRGB, int sfactorAlpha, int dfactorAlpha) {
        GlState.set(GL_BLEND_SRC_RGB, sfactorRGB);
        GlState.set(GL_BLEND_DST_RGB, dfactorRGB);
        GlState.set(GL_BLEND_SRC_ALPHA, sfactorAlpha);
        GlState.set(GL_BLEND_DST_ALPHA, dfactorAlpha);
    }

    @Patch(owner = {GL14C, GL14})
    public static void glBlendEquation(int mode) {
        GlState.set(GL_BLEND_EQUATION_RGB, mode);
        GlState.set(GL_BLEND_EQUATION_ALPHA, mode);
    }

    @Patch(owner = {GL11C, GL11})
    public static void glDepthFunc(int func) {
        GlState.set(GL_DEPTH_FUNC, func);
    }

    @Patch(owner = {GL11C, GL11})
    public static void glDepthMask(boolean flag) {
        GlState.set(GL_DEPTH_WRITEMASK, flag ? 1 : 0);
    }

    @Patch(owner = {GL11C, GL11})
    public static void glColorMask(boolean red, boolean green, boolean blue, boolean alpha) {
        GlState.set(GL_COLOR_WRITEMASK, red ? 1 : 0, green ? 1 : 0, blue ? 1 : 0, alpha ? 1 : 0);
    }

    @Patch(owner = {GL11C, GL11})
    public static void glClearColor(float red, float green, float blue, float alpha) {
        GlState.set(GL_COLOR_CLEAR_VALUE, red, green, blue, alpha);
    }

    @Patch(owner = {GL11C, GL11})
    public static void glClearDepth(double depth) {
        GlState.set(GL_DEPTH_CLEAR_VALUE, depth);
    }

    @Patch(owner = {GL11C, GL11})
    public static void glStencilMask(int mask) {
        GlState.set(GL_STENCIL_WRITEMASK, mask & 0xFF);
        GlState.set(GL_STENCIL_BACK_WRITEMASK, mask & 0xFF);
    }

    @Patch(owner = {GL11C, GL11})
    public static void glDrawBuffer(int buf) {
        GlState.set(GL_DRAW_BUFFER, buf);
        GlState.set(GL_DRAW_BUFFER0, buf);
    }

    @Patch(owner = {GL11C, GL11})
    public static void glReadBuffer(int src) {
        GlState.set(GL_READ_BUFFER, src);
    }

    @Patch(owner = {GL11C, GL11})
    public static void glCullFace(int mode) {
        GlState.set(GL_CULL_FACE_MODE, mode);
    }

    @Patch(owner = {GL11C, GL11})
    public static void glFrontFace(int dir) {
        GlState.set(GL_FRONT_FACE, dir);
    }

    @Patch(owner = {GL11C, GL11})
    public static void glPolygonMode(int face, int mode) {
        GlState.set(GL_POLYGON_MODE, mode, mode);
    }

    @Patch(owner = {GL11C, GL11})
    public static void glPolygonOffset(float factor, float units) {
        GlState.set(GL_POLYGON_OFFSET_FACTOR, factor);
        GlState.set(GL_POLYGON_OFFSET_UNITS, units);
    }

    @Patch(owner = {GL11C, GL11})
    public static void glLineWidth(float width) {
        GlState.set(GL_LINE_WIDTH, width);
    }

    @Patch(owner = {GL11C, GL11})
    public static void glPixelStorei(int pname, int param) {
        GlState.set(pname, param);
    }

}
