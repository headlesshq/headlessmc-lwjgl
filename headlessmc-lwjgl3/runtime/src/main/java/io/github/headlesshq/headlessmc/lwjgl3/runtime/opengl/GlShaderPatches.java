package io.github.headlesshq.headlessmc.lwjgl3.runtime.opengl;

import io.github.headlesshq.headlessmc.lwjgl.runtime.Ids;
import io.github.headlesshq.headlessmc.lwjgl.runtime.Patch;

import java.nio.IntBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.lwjgl.opengl.GL46C.*;

/**
 * Patches for shader and program queries: every shader compiles, every program links and validates, without info
 * logs or active resources. The type of a shader and the number of shaders attached to a program are tracked.
 */
public final class GlShaderPatches {
    private static final String GL20 = "org/lwjgl/opengl/GL20";
    private static final String GL20C = "org/lwjgl/opengl/GL20C";
    private static final Map<Integer, Integer> SHADER_TYPES = new ConcurrentHashMap<>();
    private static final Map<Integer, Integer> ATTACHED_SHADERS = new ConcurrentHashMap<>();

    private GlShaderPatches() {
        throw new AssertionError();
    }

    @Patch(owner = {GL20C, GL20})
    public static int glCreateShader(int type) {
        int shader = Ids.nextInt();
        SHADER_TYPES.put(shader, type);
        return shader;
    }

    @Patch(owner = {GL20C, GL20})
    public static void glDeleteShader(int shader) {
        SHADER_TYPES.remove(shader);
    }

    @Patch(owner = {GL20C, GL20})
    public static void glAttachShader(int program, int shader) {
        ATTACHED_SHADERS.merge(program, 1, Integer::sum);
    }

    @Patch(owner = {GL20C, GL20})
    public static void glDetachShader(int program, int shader) {
        ATTACHED_SHADERS.computeIfPresent(program, (key, count) -> count > 1 ? count - 1 : null);
    }

    @Patch(owner = {GL20C, GL20})
    public static void glDeleteProgram(int program) {
        ATTACHED_SHADERS.remove(program);
    }

    @Patch(owner = {GL20C, GL20})
    public static int glGetShaderi(int shader, int pname) {
        return shaderValue(shader, pname);
    }

    @Patch(owner = {GL20C, GL20})
    public static void glGetShaderiv(int shader, int pname, IntBuffer params) {
        params.put(params.position(), shaderValue(shader, pname));
    }

    @Patch(owner = {GL20C, GL20})
    public static void glGetShaderiv(int shader, int pname, int[] params) {
        params[0] = shaderValue(shader, pname);
    }

    @Patch(owner = {GL20C, GL20})
    public static int glGetProgrami(int program, int pname) {
        return programValue(program, pname);
    }

    @Patch(owner = {GL20C, GL20})
    public static void glGetProgramiv(int program, int pname, IntBuffer params) {
        params.put(params.position(), programValue(program, pname));
    }

    @Patch(owner = {GL20C, GL20})
    public static void glGetProgramiv(int program, int pname, int[] params) {
        params[0] = programValue(program, pname);
    }

    private static int shaderValue(int shader, int pname) {
        switch (pname) {
            case GL_SHADER_TYPE:
                return SHADER_TYPES.getOrDefault(shader, GL_VERTEX_SHADER);
            case GL_COMPILE_STATUS:
                return GL_TRUE;
            default:
                return 0;
        }
    }

    private static int programValue(int program, int pname) {
        switch (pname) {
            case GL_LINK_STATUS:
            case GL_VALIDATE_STATUS:
                return GL_TRUE;
            case GL_ATTACHED_SHADERS:
                return ATTACHED_SHADERS.getOrDefault(program, 0);
            case GL_TRANSFORM_FEEDBACK_BUFFER_MODE:
                return GL_INTERLEAVED_ATTRIBS;
            default:
                return 0;
        }
    }

}
