package io.github.headlesshq.headlessmc.lwjgl3.runtime.opengl;

import io.github.headlesshq.headlessmc.lwjgl.runtime.Patch;
import io.github.headlesshq.headlessmc.lwjgl.runtime.opengl.GlObjects;

import java.nio.IntBuffer;

/**
 * Patches for shader and program queries, answered by {@link GlObjects}.
 */
public final class GlShaderPatches {
    private static final String GL20 = "org/lwjgl/opengl/GL20";
    private static final String GL20C = "org/lwjgl/opengl/GL20C";

    private GlShaderPatches() {
        throw new AssertionError();
    }

    @Patch(owner = {GL20C, GL20})
    public static int glCreateShader(int type) {
        return GlObjects.createShader(type);
    }

    @Patch(owner = {GL20C, GL20})
    public static void glDeleteShader(int shader) {
        GlObjects.deleteShader(shader);
    }

    @Patch(owner = {GL20C, GL20})
    public static void glAttachShader(int program, int shader) {
        GlObjects.attachShader(program);
    }

    @Patch(owner = {GL20C, GL20})
    public static void glDetachShader(int program, int shader) {
        GlObjects.detachShader(program);
    }

    @Patch(owner = {GL20C, GL20})
    public static void glDeleteProgram(int program) {
        GlObjects.deleteProgram(program);
    }

    @Patch(owner = {GL20C, GL20})
    public static int glGetShaderi(int shader, int pname) {
        return GlObjects.shaderValue(shader, pname);
    }

    @Patch(owner = {GL20C, GL20})
    public static void glGetShaderiv(int shader, int pname, IntBuffer params) {
        params.put(params.position(), GlObjects.shaderValue(shader, pname));
    }

    @Patch(owner = {GL20C, GL20})
    public static void glGetShaderiv(int shader, int pname, int[] params) {
        params[0] = GlObjects.shaderValue(shader, pname);
    }

    @Patch(owner = {GL20C, GL20})
    public static int glGetProgrami(int program, int pname) {
        return GlObjects.programValue(program, pname);
    }

    @Patch(owner = {GL20C, GL20})
    public static void glGetProgramiv(int program, int pname, IntBuffer params) {
        params.put(params.position(), GlObjects.programValue(program, pname));
    }

    @Patch(owner = {GL20C, GL20})
    public static void glGetProgramiv(int program, int pname, int[] params) {
        params[0] = GlObjects.programValue(program, pname);
    }

}
