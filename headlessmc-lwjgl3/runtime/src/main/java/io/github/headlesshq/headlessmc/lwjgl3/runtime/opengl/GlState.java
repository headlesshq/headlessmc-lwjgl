package io.github.headlesshq.headlessmc.lwjgl3.runtime.opengl;

import io.github.headlesshq.headlessmc.lwjgl3.runtime.Input;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.lwjgl.opengl.GL46C.*;

/**
 * The OpenGL state that the application sets and can query back: viewport, scissor box, enabled capabilities,
 * bindings, blend/depth/raster state and pixel store parameters, with the initial values of the OpenGL specification.
 * Takes precedence over {@link GlValues}, which only holds implementation limits.
 * The viewport and scissor box follow the window size until the application sets them.
 * There is a single state for all contexts and threads, like the single window of a headless game.
 */
public final class GlState {
    private static final Map<Integer, Integer> TEXTURE_BINDINGS = new HashMap<>();
    private static final Map<Integer, Integer> BUFFER_BINDINGS = new HashMap<>();
    private static final Set<Integer> CAPABILITIES = new HashSet<>();
    private static final Map<Integer, double[]> VALUES = new HashMap<>();
    private static final Set<Integer> ENABLED = new HashSet<>();
    private static final Map<Long, Integer> BOUND_TEXTURES = new HashMap<>();
    private static final Map<Integer, Integer> BOUND_SAMPLERS = new HashMap<>();

    static {
        TEXTURE_BINDINGS.put(GL_TEXTURE_1D, GL_TEXTURE_BINDING_1D);
        TEXTURE_BINDINGS.put(GL_TEXTURE_2D, GL_TEXTURE_BINDING_2D);
        TEXTURE_BINDINGS.put(GL_TEXTURE_3D, GL_TEXTURE_BINDING_3D);
        TEXTURE_BINDINGS.put(GL_TEXTURE_1D_ARRAY, GL_TEXTURE_BINDING_1D_ARRAY);
        TEXTURE_BINDINGS.put(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_BINDING_2D_ARRAY);
        TEXTURE_BINDINGS.put(GL_TEXTURE_RECTANGLE, GL_TEXTURE_BINDING_RECTANGLE);
        TEXTURE_BINDINGS.put(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_BINDING_CUBE_MAP);
        TEXTURE_BINDINGS.put(GL_TEXTURE_CUBE_MAP_ARRAY, GL_TEXTURE_BINDING_CUBE_MAP_ARRAY);
        TEXTURE_BINDINGS.put(GL_TEXTURE_BUFFER, GL_TEXTURE_BINDING_BUFFER);
        TEXTURE_BINDINGS.put(GL_TEXTURE_2D_MULTISAMPLE, GL_TEXTURE_BINDING_2D_MULTISAMPLE);
        TEXTURE_BINDINGS.put(GL_TEXTURE_2D_MULTISAMPLE_ARRAY, GL_TEXTURE_BINDING_2D_MULTISAMPLE_ARRAY);

        BUFFER_BINDINGS.put(GL_ARRAY_BUFFER, GL_ARRAY_BUFFER_BINDING);
        BUFFER_BINDINGS.put(GL_ELEMENT_ARRAY_BUFFER, GL_ELEMENT_ARRAY_BUFFER_BINDING);
        BUFFER_BINDINGS.put(GL_PIXEL_PACK_BUFFER, GL_PIXEL_PACK_BUFFER_BINDING);
        BUFFER_BINDINGS.put(GL_PIXEL_UNPACK_BUFFER, GL_PIXEL_UNPACK_BUFFER_BINDING);
        BUFFER_BINDINGS.put(GL_UNIFORM_BUFFER, GL_UNIFORM_BUFFER_BINDING);
        BUFFER_BINDINGS.put(GL_COPY_READ_BUFFER, GL_COPY_READ_BUFFER_BINDING);
        BUFFER_BINDINGS.put(GL_COPY_WRITE_BUFFER, GL_COPY_WRITE_BUFFER_BINDING);
        BUFFER_BINDINGS.put(GL_DRAW_INDIRECT_BUFFER, GL_DRAW_INDIRECT_BUFFER_BINDING);
        BUFFER_BINDINGS.put(GL_DISPATCH_INDIRECT_BUFFER, GL_DISPATCH_INDIRECT_BUFFER_BINDING);
        BUFFER_BINDINGS.put(GL_SHADER_STORAGE_BUFFER, GL_SHADER_STORAGE_BUFFER_BINDING);
        BUFFER_BINDINGS.put(GL_ATOMIC_COUNTER_BUFFER, GL_ATOMIC_COUNTER_BUFFER_BINDING);
        BUFFER_BINDINGS.put(GL_TRANSFORM_FEEDBACK_BUFFER, GL_TRANSFORM_FEEDBACK_BUFFER_BINDING);
        BUFFER_BINDINGS.put(GL_QUERY_BUFFER, GL_QUERY_BUFFER_BINDING);

        int[] capabilities = {
            GL_BLEND, GL_COLOR_LOGIC_OP, GL_CULL_FACE, GL_DEPTH_CLAMP, GL_DEPTH_TEST, GL_DITHER, GL_FRAMEBUFFER_SRGB,
            GL_LINE_SMOOTH, GL_MULTISAMPLE, GL_POLYGON_OFFSET_FILL, GL_POLYGON_OFFSET_LINE, GL_POLYGON_OFFSET_POINT,
            GL_POLYGON_SMOOTH, GL_PRIMITIVE_RESTART, GL_PRIMITIVE_RESTART_FIXED_INDEX, GL_PROGRAM_POINT_SIZE,
            GL_RASTERIZER_DISCARD, GL_SAMPLE_ALPHA_TO_COVERAGE, GL_SAMPLE_ALPHA_TO_ONE, GL_SAMPLE_COVERAGE,
            GL_SAMPLE_SHADING, GL_SAMPLE_MASK, GL_SCISSOR_TEST, GL_STENCIL_TEST, GL_TEXTURE_CUBE_MAP_SEAMLESS,
            GL_DEBUG_OUTPUT, GL_DEBUG_OUTPUT_SYNCHRONOUS, GL_CLIP_DISTANCE0, GL_CLIP_DISTANCE1, GL_CLIP_DISTANCE2,
            GL_CLIP_DISTANCE3, GL_CLIP_DISTANCE4, GL_CLIP_DISTANCE5, GL_CLIP_DISTANCE6, GL_CLIP_DISTANCE7,
            GL11.GL_LIGHTING, GL11.GL_ALPHA_TEST, GL11.GL_COLOR_MATERIAL, GL11.GL_TEXTURE_1D, GL11.GL_TEXTURE_2D,
            GL11.GL_FOG, GL11.GL_POINT_SMOOTH, GL11.GL_LINE_STIPPLE, GL11.GL_POLYGON_STIPPLE, GL11.GL_NORMALIZE,
            GL11.GL_TEXTURE_GEN_S, GL11.GL_TEXTURE_GEN_T, GL11.GL_TEXTURE_GEN_R, GL11.GL_TEXTURE_GEN_Q,
            GL11.GL_LIGHT0, GL11.GL_LIGHT1, GL11.GL_LIGHT2, GL11.GL_LIGHT3, GL11.GL_LIGHT4, GL11.GL_LIGHT5,
            GL11.GL_LIGHT6, GL11.GL_LIGHT7, GL12.GL_RESCALE_NORMAL
        };
        for (int capability : capabilities) {
            CAPABILITIES.add(capability);
        }

        ENABLED.add(GL_DITHER);
        ENABLED.add(GL_MULTISAMPLE);

        set(GL_ACTIVE_TEXTURE, GL_TEXTURE0);
        set(GL_BLEND_SRC_RGB, GL_ONE);
        set(GL_BLEND_SRC_ALPHA, GL_ONE);
        set(GL_BLEND_DST_RGB, GL_ZERO);
        set(GL_BLEND_DST_ALPHA, GL_ZERO);
        set(GL_BLEND_EQUATION_RGB, GL_FUNC_ADD);
        set(GL_BLEND_EQUATION_ALPHA, GL_FUNC_ADD);
        set(GL_BLEND_COLOR, 0, 0, 0, 0);
        set(GL_DEPTH_FUNC, GL_LESS);
        set(GL_DEPTH_WRITEMASK, GL_TRUE);
        set(GL_DEPTH_RANGE, 0, 1);
        set(GL_DEPTH_CLEAR_VALUE, 1);
        set(GL_COLOR_WRITEMASK, 1, 1, 1, 1);
        set(GL_COLOR_CLEAR_VALUE, 0, 0, 0, 0);
        set(GL_STENCIL_CLEAR_VALUE, 0);
        set(GL_STENCIL_WRITEMASK, 255);
        set(GL_STENCIL_BACK_WRITEMASK, 255);
        set(GL_STENCIL_FUNC, GL_ALWAYS);
        set(GL_STENCIL_REF, 0);
        set(GL_STENCIL_VALUE_MASK, 255);
        set(GL_STENCIL_BACK_VALUE_MASK, 255);
        set(GL_DRAW_BUFFER, GL_BACK);
        set(GL_DRAW_BUFFER0, GL_BACK);
        set(GL_READ_BUFFER, GL_BACK);
        set(GL_STENCIL_FAIL, GL_KEEP);
        set(GL_STENCIL_PASS_DEPTH_FAIL, GL_KEEP);
        set(GL_STENCIL_PASS_DEPTH_PASS, GL_KEEP);
        set(GL_CULL_FACE_MODE, GL_BACK);
        set(GL_FRONT_FACE, GL_CCW);
        set(GL_POLYGON_MODE, GL_FILL, GL_FILL);
        set(GL_POLYGON_OFFSET_FACTOR, 0);
        set(GL_POLYGON_OFFSET_UNITS, 0);
        set(GL_LINE_WIDTH, 1);
        set(GL_POINT_SIZE, 1);
        set(GL_LOGIC_OP_MODE, GL_COPY);
        set(GL_PROVOKING_VERTEX, GL_LAST_VERTEX_CONVENTION);
        set(GL_PACK_ALIGNMENT, 4);
        set(GL_UNPACK_ALIGNMENT, 4);
        for (int pname : new int[]{
            GL_PACK_ROW_LENGTH, GL_PACK_SKIP_ROWS, GL_PACK_SKIP_PIXELS, GL_PACK_IMAGE_HEIGHT, GL_PACK_SKIP_IMAGES,
            GL_PACK_SWAP_BYTES, GL_PACK_LSB_FIRST, GL_UNPACK_ROW_LENGTH, GL_UNPACK_SKIP_ROWS, GL_UNPACK_SKIP_PIXELS,
            GL_UNPACK_IMAGE_HEIGHT, GL_UNPACK_SKIP_IMAGES, GL_UNPACK_SWAP_BYTES, GL_UNPACK_LSB_FIRST,
            GL_CURRENT_PROGRAM, GL_RENDERBUFFER_BINDING, GL_VERTEX_ARRAY_BINDING, GL_DRAW_FRAMEBUFFER_BINDING,
            GL_READ_FRAMEBUFFER_BINDING, GL_PROGRAM_PIPELINE_BINDING, GL_TRANSFORM_FEEDBACK_BINDING}) {
            set(pname, 0);
        }

        for (int binding : BUFFER_BINDINGS.values()) {
            set(binding, 0);
        }
    }

    private GlState() {
        throw new AssertionError();
    }

    /**
     * @param pname a {@code glGet} parameter.
     * @return the tracked value, or {@code null} if the parameter is not part of the tracked state.
     */
    public static synchronized double[] get(int pname) {
        if (pname == GL_VIEWPORT || pname == GL_SCISSOR_BOX) {
            double[] value = VALUES.get(pname);
            return value != null ? value.clone() : new double[]{0, 0, Input.width(), Input.height()};
        } else if (pname == GL_SAMPLER_BINDING) {
            Integer sampler = BOUND_SAMPLERS.get(activeUnit());
            return new double[]{sampler == null ? 0 : sampler};
        } else if (CAPABILITIES.contains(pname)) {
            return new double[]{ENABLED.contains(pname) ? 1 : 0};
        }

        for (Map.Entry<Integer, Integer> binding : TEXTURE_BINDINGS.entrySet()) {
            if (binding.getValue() == pname) {
                Integer texture = BOUND_TEXTURES.get(key(activeUnit(), binding.getKey()));
                return new double[]{texture == null ? 0 : texture};
            }
        }

        double[] value = VALUES.get(pname);
        return value == null ? null : value.clone();
    }

    public static synchronized void set(int pname, double... values) {
        VALUES.put(pname, values);
    }

    public static synchronized void setEnabled(int capability, boolean enabled) {
        if (!CAPABILITIES.contains(capability)) {
            CAPABILITIES.add(capability);
        }

        if (enabled) {
            ENABLED.add(capability);
        } else {
            ENABLED.remove(capability);
        }
    }

    public static synchronized boolean isEnabled(int capability) {
        return ENABLED.contains(capability);
    }

    public static synchronized void bindTexture(int target, int texture) {
        BOUND_TEXTURES.put(key(activeUnit(), target), texture);
    }

    /**
     * @param target a texture target, cube map faces count as {@code GL_TEXTURE_CUBE_MAP}.
     * @return the texture bound to the target on the active texture unit.
     */
    public static synchronized int boundTexture(int target) {
        if (target >= GL_TEXTURE_CUBE_MAP_POSITIVE_X && target <= GL_TEXTURE_CUBE_MAP_NEGATIVE_Z) {
            target = GL_TEXTURE_CUBE_MAP;
        }

        Integer texture = BOUND_TEXTURES.get(key(activeUnit(), target));
        return texture == null ? 0 : texture;
    }

    public static synchronized void bindSampler(int unit, int sampler) {
        BOUND_SAMPLERS.put(GL_TEXTURE0 + unit, sampler);
    }

    public static synchronized void bindBuffer(int target, int buffer) {
        Integer binding = BUFFER_BINDINGS.get(target);
        if (binding != null) {
            set(binding, buffer);
        }
    }

    public static synchronized void bindFramebuffer(int target, int framebuffer) {
        if (target == GL_FRAMEBUFFER || target == GL_DRAW_FRAMEBUFFER) {
            set(GL_DRAW_FRAMEBUFFER_BINDING, framebuffer);
        }

        if (target == GL_FRAMEBUFFER || target == GL_READ_FRAMEBUFFER) {
            set(GL_READ_FRAMEBUFFER_BINDING, framebuffer);
        }
    }

    private static int activeUnit() {
        double[] value = VALUES.get(GL_ACTIVE_TEXTURE);
        return value == null ? GL_TEXTURE0 : (int) value[0];
    }

    private static long key(int unit, int target) {
        return ((long) unit << 32) | (target & 0xFFFFFFFFL);
    }

}
