package io.github.headlesshq.headlessmc.lwjgl.runtime.opengl;

import io.github.headlesshq.headlessmc.lwjgl.runtime.Ids;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.lwjgl.opengl.GL46C.*;

/**
 * The properties of GL objects that applications query back: shader types, attached shaders, and the size and
 * internal format of texture images. Every shader compiles, every program links and validates, without info logs or
 * active resources.
 */
public final class GlObjects {
    private static final Map<Integer, Integer> SHADER_TYPES = new ConcurrentHashMap<>();
    private static final Map<Integer, Integer> ATTACHED_SHADERS = new ConcurrentHashMap<>();
    private static final Map<Long, int[]> TEXTURE_LEVELS = new ConcurrentHashMap<>();

    private GlObjects() {
        throw new AssertionError();
    }

    public static int createShader(int type) {
        int shader = Ids.nextInt();
        SHADER_TYPES.put(shader, type);
        return shader;
    }

    public static void deleteShader(int shader) {
        SHADER_TYPES.remove(shader);
    }

    public static void attachShader(int program) {
        ATTACHED_SHADERS.merge(program, 1, Integer::sum);
    }

    public static void detachShader(int program) {
        ATTACHED_SHADERS.computeIfPresent(program, (key, count) -> count > 1 ? count - 1 : null);
    }

    public static void deleteProgram(int program) {
        ATTACHED_SHADERS.remove(program);
    }

    public static int shaderValue(int shader, int pname) {
        switch (pname) {
            case GL_SHADER_TYPE:
                return SHADER_TYPES.getOrDefault(shader, GL_VERTEX_SHADER);
            case GL_COMPILE_STATUS:
                return GL_TRUE;
            default:
                return 0;
        }
    }

    public static int programValue(int program, int pname) {
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

    /**
     * Records a texture image of the texture bound to the target.
     */
    public static void textureImage(int target, int level, int internalformat, int width, int height) {
        TEXTURE_LEVELS.put(levelKey(target, level), new int[]{width, height, internalformat});
    }

    public static int textureLevelValue(int target, int level, int pname) {
        int[] image = TEXTURE_LEVELS.get(levelKey(target, level));
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

    private static long levelKey(int target, int level) {
        boolean cubeFace = target >= GL_TEXTURE_CUBE_MAP_POSITIVE_X && target <= GL_TEXTURE_CUBE_MAP_NEGATIVE_Z;
        int face = cubeFace ? target - GL_TEXTURE_CUBE_MAP_POSITIVE_X : 0;
        return ((long) GlState.boundTexture(target) << 32) | ((long) face << 16) | (level & 0xFFFF);
    }

}
