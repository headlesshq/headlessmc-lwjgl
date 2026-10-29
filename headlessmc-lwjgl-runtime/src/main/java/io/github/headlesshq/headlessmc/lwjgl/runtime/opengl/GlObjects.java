package io.github.headlesshq.headlessmc.lwjgl.runtime.opengl;

import io.github.headlesshq.headlessmc.lwjgl.runtime.Ids;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.lwjgl.opengl.GL46C.*;

/**
 * The properties of GL objects that applications query back: shader types, attached shaders, the size and
 * internal format of texture images, texture parameters, framebuffer attachments, renderbuffer storage, and the size
 * of buffer data, which also decides the size of mapped buffers.
 * Every shader compiles, every program links and validates, without info logs or active resources. Every framebuffer
 * is complete and every internal format is supported.
 */
public final class GlObjects {
    private static final Map<Integer, Integer> SHADER_TYPES = new ConcurrentHashMap<>();
    private static final Map<Integer, Integer> ATTACHED_SHADERS = new ConcurrentHashMap<>();
    private static final Map<Long, int[]> TEXTURE_LEVELS = new ConcurrentHashMap<>();
    private static final Map<Integer, Long> BUFFER_SIZES = new ConcurrentHashMap<>();
    private static final Map<Long, double[]> TEXTURE_PARAMETERS = new ConcurrentHashMap<>();
    private static final Map<Long, int[]> ATTACHMENTS = new ConcurrentHashMap<>();
    private static final Map<Integer, int[]> RENDERBUFFERS = new ConcurrentHashMap<>();
    private static final int COLOR_BITS = 8;
    private static final int DEPTH_BITS = 24;
    private static final int STENCIL_BITS = 8;
    private static final int SAMPLES = 4;

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

    /**
     * Records the size of the data store of the buffer bound to the target, see {@code glBufferData}.
     */
    public static void bufferData(int target, long size) {
        BUFFER_SIZES.put(GlState.boundBuffer(target), size);
    }

    /**
     * @param target a buffer target.
     * @return the size of the data store of the buffer bound to the target.
     */
    public static long bufferSize(int target) {
        return BUFFER_SIZES.getOrDefault(GlState.boundBuffer(target), 0L);
    }

    public static int bufferValue(int target, int pname) {
        switch (pname) {
            case GL_BUFFER_SIZE:
                return (int) Math.min(Integer.MAX_VALUE, bufferSize(target));
            case GL_BUFFER_USAGE:
                return GL_STATIC_DRAW;
            case GL_BUFFER_ACCESS:
                return GL_READ_WRITE;
            default:
                return 0;
        }
    }

    /**
     * Records a parameter of the texture bound to the target, see {@code glTexParameter*}.
     */
    public static void textureParameter(int target, int pname, double... values) {
        TEXTURE_PARAMETERS.put(parameterKey(target, pname), values.clone());
    }

    /**
     * @return the parameter of the texture bound to the target, the initial value if it was not set.
     */
    public static double[] textureParameter(int target, int pname) {
        double[] value = TEXTURE_PARAMETERS.get(parameterKey(target, pname));
        return value != null ? value.clone() : initialTextureParameter(pname);
    }

    private static double[] initialTextureParameter(int pname) {
        switch (pname) {
            case GL_TEXTURE_MIN_FILTER:
                return new double[]{GL_NEAREST_MIPMAP_LINEAR};
            case GL_TEXTURE_MAG_FILTER:
                return new double[]{GL_LINEAR};
            case GL_TEXTURE_WRAP_S:
            case GL_TEXTURE_WRAP_T:
            case GL_TEXTURE_WRAP_R:
                return new double[]{GL_REPEAT};
            case GL_TEXTURE_MAX_LEVEL:
                return new double[]{1000};
            case GL_TEXTURE_MIN_LOD:
                return new double[]{-1000};
            case GL_TEXTURE_MAX_LOD:
                return new double[]{1000};
            case GL_TEXTURE_COMPARE_FUNC:
                return new double[]{GL_LEQUAL};
            case GL_TEXTURE_SWIZZLE_R:
                return new double[]{GL_RED};
            case GL_TEXTURE_SWIZZLE_G:
                return new double[]{GL_GREEN};
            case GL_TEXTURE_SWIZZLE_B:
                return new double[]{GL_BLUE};
            case GL_TEXTURE_SWIZZLE_A:
                return new double[]{GL_ALPHA};
            case GL_TEXTURE_SWIZZLE_RGBA:
                return new double[]{GL_RED, GL_GREEN, GL_BLUE, GL_ALPHA};
            case GL_DEPTH_STENCIL_TEXTURE_MODE:
                return new double[]{GL_DEPTH_COMPONENT};
            case GL_TEXTURE_MAX_ANISOTROPY:
                return new double[]{1};
            case GL_TEXTURE_BORDER_COLOR:
                return new double[]{0, 0, 0, 0};
            default:
                return new double[]{0};
        }
    }

    private static long parameterKey(int target, int pname) {
        return ((long) GlState.boundTexture(target) << 32) | (pname & 0xFFFFFFFFL);
    }

    /**
     * Records an attachment of the framebuffer bound to the target.
     *
     * @param type {@code GL_TEXTURE}, {@code GL_RENDERBUFFER}, or {@code GL_NONE} with name 0 to detach.
     */
    public static void attach(int target, int attachment, int type, int name, int level, int layer) {
        long key = attachmentKey(target, attachment);
        if (name == 0) {
            ATTACHMENTS.remove(key);
        } else {
            ATTACHMENTS.put(key, new int[]{type, name, level, layer});
        }
    }

    /**
     * Like {@code glGetFramebufferAttachmentParameteri}: the default framebuffer has 8 bit RGBA color, a 24 bit depth
     * and an 8 bit stencil buffer, attachments of other framebuffers are what was attached, with the bit sizes of
     * the default framebuffer.
     */
    public static int attachmentValue(int target, int attachment, int pname) {
        boolean defaultFramebuffer = GlState.boundFramebuffer(target) == 0;
        int[] attached = defaultFramebuffer ? null : ATTACHMENTS.get(attachmentKey(target, attachment));
        if (!defaultFramebuffer && attached == null) {
            return pname == GL_FRAMEBUFFER_ATTACHMENT_OBJECT_TYPE ? GL_NONE : 0;
        }

        boolean depth = attachment == GL_DEPTH || attachment == GL_DEPTH_ATTACHMENT;
        boolean stencil = attachment == GL_STENCIL || attachment == GL_STENCIL_ATTACHMENT;
        boolean depthStencil = attachment == GL_DEPTH_STENCIL_ATTACHMENT;
        boolean color = !depth && !stencil && !depthStencil;
        switch (pname) {
            case GL_FRAMEBUFFER_ATTACHMENT_OBJECT_TYPE:
                return defaultFramebuffer ? GL_FRAMEBUFFER_DEFAULT : attached[0];
            case GL_FRAMEBUFFER_ATTACHMENT_OBJECT_NAME:
                return defaultFramebuffer ? 0 : attached[1];
            case GL_FRAMEBUFFER_ATTACHMENT_TEXTURE_LEVEL:
                return defaultFramebuffer ? 0 : attached[2];
            case GL_FRAMEBUFFER_ATTACHMENT_TEXTURE_LAYER:
                return defaultFramebuffer ? 0 : attached[3];
            case GL_FRAMEBUFFER_ATTACHMENT_RED_SIZE:
            case GL_FRAMEBUFFER_ATTACHMENT_GREEN_SIZE:
            case GL_FRAMEBUFFER_ATTACHMENT_BLUE_SIZE:
            case GL_FRAMEBUFFER_ATTACHMENT_ALPHA_SIZE:
                return color ? COLOR_BITS : 0;
            case GL_FRAMEBUFFER_ATTACHMENT_DEPTH_SIZE:
                return depth || depthStencil ? DEPTH_BITS : 0;
            case GL_FRAMEBUFFER_ATTACHMENT_STENCIL_SIZE:
                return stencil || depthStencil ? STENCIL_BITS : 0;
            case GL_FRAMEBUFFER_ATTACHMENT_COMPONENT_TYPE:
                return color || stencil ? GL_UNSIGNED_NORMALIZED : GL_FLOAT;
            case GL_FRAMEBUFFER_ATTACHMENT_COLOR_ENCODING:
                return GL_LINEAR;
            default:
                return 0;
        }
    }

    private static long attachmentKey(int target, int attachment) {
        return ((long) GlState.boundFramebuffer(target) << 32) | (attachment & 0xFFFFFFFFL);
    }

    /**
     * Records the storage of the bound renderbuffer, see {@code glRenderbufferStorage*}.
     */
    public static void renderbufferStorage(int internalformat, int samples, int width, int height) {
        RENDERBUFFERS.put(GlState.boundRenderbuffer(), new int[]{width, height, internalformat, samples});
    }

    public static int renderbufferValue(int pname) {
        int[] storage = RENDERBUFFERS.get(GlState.boundRenderbuffer());
        switch (pname) {
            case GL_RENDERBUFFER_WIDTH:
                return storage == null ? 0 : storage[0];
            case GL_RENDERBUFFER_HEIGHT:
                return storage == null ? 0 : storage[1];
            case GL_RENDERBUFFER_INTERNAL_FORMAT:
                return storage == null ? GL_RGBA4 : storage[2];
            case GL_RENDERBUFFER_SAMPLES:
                return storage == null ? 0 : storage[3];
            case GL_RENDERBUFFER_RED_SIZE:
            case GL_RENDERBUFFER_GREEN_SIZE:
            case GL_RENDERBUFFER_BLUE_SIZE:
            case GL_RENDERBUFFER_ALPHA_SIZE:
                return storage == null ? 0 : COLOR_BITS;
            case GL_RENDERBUFFER_DEPTH_SIZE:
                return storage == null ? 0 : DEPTH_BITS;
            case GL_RENDERBUFFER_STENCIL_SIZE:
                return storage == null ? 0 : STENCIL_BITS;
            default:
                return 0;
        }
    }

    /**
     * Like {@code glGetInternalformati}: every format is supported, renderable and filterable, with up to 4 samples.
     *
     * @return the values of the query, as many as {@code glGetInternalformativ} writes.
     */
    public static int[] internalformatValues(int target, int internalformat, int pname) {
        switch (pname) {
            case GL_INTERNALFORMAT_SUPPORTED:
                return new int[]{GL_TRUE};
            case GL_INTERNALFORMAT_PREFERRED:
                return new int[]{internalformat};
            case GL_NUM_SAMPLE_COUNTS:
                return new int[]{3};
            case GL_SAMPLES:
                return new int[]{SAMPLES, 2, 1};
            case GL_MAX_WIDTH:
            case GL_MAX_HEIGHT:
                return new int[]{GlValues.getInteger(GL_MAX_TEXTURE_SIZE)};
            case GL_MAX_DEPTH:
                return new int[]{GlValues.getInteger(GL_MAX_3D_TEXTURE_SIZE)};
            case GL_MAX_LAYERS:
                return new int[]{GlValues.getInteger(GL_MAX_ARRAY_TEXTURE_LAYERS)};
            case GL_COLOR_RENDERABLE:
            case GL_DEPTH_RENDERABLE:
            case GL_STENCIL_RENDERABLE:
            case GL_FRAMEBUFFER_RENDERABLE:
            case GL_FRAMEBUFFER_BLEND:
            case GL_FILTER:
            case GL_MIPMAP:
            case GL_SHADER_IMAGE_LOAD:
            case GL_SHADER_IMAGE_STORE:
            case GL_VERTEX_TEXTURE:
            case GL_FRAGMENT_TEXTURE:
            case GL_TEXTURE_VIEW:
                return new int[]{pname == GL_COLOR_RENDERABLE || pname == GL_DEPTH_RENDERABLE
                    || pname == GL_STENCIL_RENDERABLE ? GL_TRUE : GL_FULL_SUPPORT};
            default:
                return new int[]{0};
        }
    }

    private static long levelKey(int target, int level) {
        boolean cubeFace = target >= GL_TEXTURE_CUBE_MAP_POSITIVE_X && target <= GL_TEXTURE_CUBE_MAP_NEGATIVE_Z;
        int face = cubeFace ? target - GL_TEXTURE_CUBE_MAP_POSITIVE_X : 0;
        return ((long) GlState.boundTexture(target) << 32) | ((long) face << 16) | (level & 0xFFFF);
    }

}
