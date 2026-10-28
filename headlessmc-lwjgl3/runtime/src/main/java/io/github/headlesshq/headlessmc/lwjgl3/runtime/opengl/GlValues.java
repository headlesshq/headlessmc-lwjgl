package io.github.headlesshq.headlessmc.lwjgl3.runtime.opengl;

import io.github.headlesshq.headlessmc.lwjgl3.runtime.Config;
import io.github.headlesshq.headlessmc.lwjgl3.runtime.Log;
import io.github.headlesshq.headlessmc.lwjgl3.runtime.WindowConfig;
import org.lwjgl.opengl.GL46C;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The values returned by {@code glGet*} queries, keyed by the name of the GL constant.
 * <p>Defaults resemble a desktop OpenGL 4.6 driver. Every value can be overridden with the system property
 * {@code hmc.lwjgl3.gl.<GL_CONSTANT_NAME>}, e.g. {@code -Dhmc.lwjgl3.gl.GL_MAX_TEXTURE_SIZE=8192}, values with
 * multiple components are comma separated. Queries for unknown parameters return 0 and are reported once.</p>
 */
public final class GlValues {
    private static final Map<String, int[]> DEFAULTS = new LinkedHashMap<>();
    private static final Map<Integer, String> NAMES = new HashMap<>();
    private static final Map<Integer, int[]> VALUES = new HashMap<>();

    static {
        put("GL_MAJOR_VERSION", GlInfo.majorVersion());
        put("GL_MINOR_VERSION", GlInfo.minorVersion());
        put("GL_NUM_EXTENSIONS", GlInfo.extensions().size());
        put("GL_CONTEXT_PROFILE_MASK", 1);
        put("GL_CONTEXT_FLAGS", 0);

        put("GL_RED_BITS", 8);
        put("GL_GREEN_BITS", 8);
        put("GL_BLUE_BITS", 8);
        put("GL_ALPHA_BITS", 8);
        put("GL_DEPTH_BITS", 24);
        put("GL_STENCIL_BITS", 8);
        put("GL_DOUBLEBUFFER", 1);
        put("GL_SUBPIXEL_BITS", 8);
        put("GL_SAMPLE_BUFFERS", 0);
        put("GL_SAMPLES", 0);
        put("GL_PACK_ALIGNMENT", 4);
        put("GL_UNPACK_ALIGNMENT", 4);
        put("GL_VIEWPORT", 0, 0, WindowConfig.WIDTH, WindowConfig.HEIGHT);
        put("GL_SCISSOR_BOX", 0, 0, WindowConfig.WIDTH, WindowConfig.HEIGHT);

        put("GL_MAX_TEXTURE_SIZE", 16384);
        put("GL_MAX_3D_TEXTURE_SIZE", 2048);
        put("GL_MAX_CUBE_MAP_TEXTURE_SIZE", 16384);
        put("GL_MAX_RECTANGLE_TEXTURE_SIZE", 16384);
        put("GL_MAX_ARRAY_TEXTURE_LAYERS", 2048);
        put("GL_MAX_TEXTURE_BUFFER_SIZE", 134217728);
        put("GL_MAX_RENDERBUFFER_SIZE", 16384);
        put("GL_MAX_VIEWPORT_DIMS", 16384, 16384);
        put("GL_MAX_VIEWPORTS", 16);
        put("GL_VIEWPORT_SUBPIXEL_BITS", 8);
        put("GL_MAX_FRAMEBUFFER_WIDTH", 16384);
        put("GL_MAX_FRAMEBUFFER_HEIGHT", 16384);
        put("GL_MAX_FRAMEBUFFER_LAYERS", 2048);
        put("GL_MAX_FRAMEBUFFER_SAMPLES", 8);
        put("GL_MAX_COLOR_ATTACHMENTS", 8);
        put("GL_MAX_DRAW_BUFFERS", 8);
        put("GL_MAX_DUAL_SOURCE_DRAW_BUFFERS", 1);
        put("GL_MAX_SAMPLES", 8);
        put("GL_MAX_COLOR_TEXTURE_SAMPLES", 8);
        put("GL_MAX_DEPTH_TEXTURE_SAMPLES", 8);
        put("GL_MAX_INTEGER_SAMPLES", 8);
        put("GL_MAX_SAMPLE_MASK_WORDS", 1);
        put("GL_MAX_TEXTURE_LOD_BIAS", 15);
        put("GL_MAX_TEXTURE_MAX_ANISOTROPY", 16);

        put("GL_MAX_TEXTURE_IMAGE_UNITS", 32);
        put("GL_MAX_VERTEX_TEXTURE_IMAGE_UNITS", 32);
        put("GL_MAX_GEOMETRY_TEXTURE_IMAGE_UNITS", 32);
        put("GL_MAX_TESS_CONTROL_TEXTURE_IMAGE_UNITS", 32);
        put("GL_MAX_TESS_EVALUATION_TEXTURE_IMAGE_UNITS", 32);
        put("GL_MAX_COMPUTE_TEXTURE_IMAGE_UNITS", 32);
        put("GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS", 192);
        put("GL_MAX_TEXTURE_UNITS", 4);
        put("GL_MAX_TEXTURE_COORDS", 8);
        put("GL_MAX_IMAGE_UNITS", 8);
        put("GL_MAX_COMBINED_IMAGE_UNIFORMS", 48);

        put("GL_MAX_VERTEX_ATTRIBS", 16);
        put("GL_MAX_VERTEX_ATTRIB_BINDINGS", 16);
        put("GL_MAX_VERTEX_ATTRIB_RELATIVE_OFFSET", 2047);
        put("GL_MAX_VERTEX_ATTRIB_STRIDE", 2048);
        put("GL_MAX_ELEMENTS_VERTICES", 1048576);
        put("GL_MAX_ELEMENTS_INDICES", 1048576);
        put("GL_MAX_ELEMENT_INDEX", Integer.MAX_VALUE);
        put("GL_MAX_VERTEX_OUTPUT_COMPONENTS", 128);
        put("GL_MAX_FRAGMENT_INPUT_COMPONENTS", 128);
        put("GL_MAX_VARYING_COMPONENTS", 124);
        put("GL_MAX_VARYING_VECTORS", 31);
        put("GL_MAX_VARYING_FLOATS", 124);
        put("GL_MAX_CLIP_DISTANCES", 8);
        put("GL_MAX_CULL_DISTANCES", 8);
        put("GL_MAX_COMBINED_CLIP_AND_CULL_DISTANCES", 8);

        put("GL_MAX_VERTEX_UNIFORM_COMPONENTS", 4096);
        put("GL_MAX_FRAGMENT_UNIFORM_COMPONENTS", 4096);
        put("GL_MAX_GEOMETRY_UNIFORM_COMPONENTS", 4096);
        put("GL_MAX_COMPUTE_UNIFORM_COMPONENTS", 4096);
        put("GL_MAX_VERTEX_UNIFORM_VECTORS", 1024);
        put("GL_MAX_FRAGMENT_UNIFORM_VECTORS", 1024);
        put("GL_MAX_UNIFORM_LOCATIONS", 4096);
        put("GL_MAX_UNIFORM_BLOCK_SIZE", 65536);
        put("GL_MAX_UNIFORM_BUFFER_BINDINGS", 84);
        put("GL_MAX_VERTEX_UNIFORM_BLOCKS", 14);
        put("GL_MAX_FRAGMENT_UNIFORM_BLOCKS", 14);
        put("GL_MAX_GEOMETRY_UNIFORM_BLOCKS", 14);
        put("GL_MAX_COMPUTE_UNIFORM_BLOCKS", 14);
        put("GL_MAX_COMBINED_UNIFORM_BLOCKS", 84);
        put("GL_UNIFORM_BUFFER_OFFSET_ALIGNMENT", 256);
        put("GL_MAX_SHADER_STORAGE_BUFFER_BINDINGS", 16);
        put("GL_MAX_SHADER_STORAGE_BLOCK_SIZE", 134217728);
        put("GL_MAX_COMBINED_SHADER_STORAGE_BLOCKS", 96);
        put("GL_SHADER_STORAGE_BUFFER_OFFSET_ALIGNMENT", 32);
        put("GL_MAX_ATOMIC_COUNTER_BUFFER_BINDINGS", 8);
        put("GL_TEXTURE_BUFFER_OFFSET_ALIGNMENT", 16);
        put("GL_MIN_MAP_BUFFER_ALIGNMENT", 64);
        put("GL_MAX_TRANSFORM_FEEDBACK_BUFFERS", 4);
        put("GL_MAX_TRANSFORM_FEEDBACK_SEPARATE_ATTRIBS", 4);
        put("GL_MAX_TRANSFORM_FEEDBACK_SEPARATE_COMPONENTS", 4);
        put("GL_MAX_TRANSFORM_FEEDBACK_INTERLEAVED_COMPONENTS", 128);
        put("GL_MAX_PROGRAM_TEXEL_OFFSET", 7);
        put("GL_MIN_PROGRAM_TEXEL_OFFSET", -8);

        put("GL_MAX_GEOMETRY_OUTPUT_VERTICES", 1024);
        put("GL_MAX_GEOMETRY_TOTAL_OUTPUT_COMPONENTS", 1024);
        put("GL_MAX_PATCH_VERTICES", 32);
        put("GL_MAX_TESS_GEN_LEVEL", 64);
        put("GL_MAX_COMPUTE_WORK_GROUP_INVOCATIONS", 1024);
        put("GL_MAX_COMPUTE_WORK_GROUP_COUNT", 2147483647, 65535, 65535);
        put("GL_MAX_COMPUTE_WORK_GROUP_SIZE", 1024, 1024, 64);
        put("GL_MAX_COMPUTE_SHARED_MEMORY_SIZE", 49152);

        put("GL_MAX_LABEL_LENGTH", 256);
        put("GL_MAX_DEBUG_MESSAGE_LENGTH", 1024);
        put("GL_MAX_DEBUG_LOGGED_MESSAGES", 64);
        put("GL_MAX_DEBUG_GROUP_STACK_DEPTH", 64);
        put("GL_MAX_SERVER_WAIT_TIMEOUT", Integer.MAX_VALUE);

        put("GL_MAX_LIGHTS", 8);
        put("GL_MAX_CLIP_PLANES", 8);
        put("GL_MAX_MODELVIEW_STACK_DEPTH", 32);
        put("GL_MAX_PROJECTION_STACK_DEPTH", 4);
        put("GL_MAX_TEXTURE_STACK_DEPTH", 10);
        put("GL_MAX_ATTRIB_STACK_DEPTH", 16);
        put("GL_MAX_CLIENT_ATTRIB_STACK_DEPTH", 16);
        put("GL_MAX_NAME_STACK_DEPTH", 64);
        put("GL_MAX_LIST_NESTING", 64);
        put("GL_MAX_EVAL_ORDER", 30);
        put("GL_MAX_PIXEL_MAP_TABLE", 65536);

        Map<String, Integer> constants = constants();
        for (Map.Entry<String, Integer> constant : constants.entrySet()) {
            NAMES.putIfAbsent(constant.getValue(), constant.getKey());
        }

        for (Map.Entry<String, int[]> entry : DEFAULTS.entrySet()) {
            Integer pname = constants.get(entry.getKey());
            if (pname == null) {
                Log.debug("Unknown GL constant in defaults: " + entry.getKey());
                continue;
            }

            VALUES.put(pname, configured(entry.getKey(), entry.getValue()));
        }

        for (Map.Entry<String, Integer> constant : constants.entrySet()) {
            String override = Config.get("gl." + constant.getKey());
            if (override != null) {
                VALUES.put(constant.getValue(), parse(constant.getKey(), override, new int[]{0}));
            }
        }
    }

    private GlValues() {
        throw new AssertionError();
    }

    /**
     * @param pname the GL constant of the queried state.
     * @return all components of the value, an array with a single 0 for unknown parameters.
     */
    public static int[] get(int pname) {
        int[] values = VALUES.get(pname);
        if (values == null) {
            String name = NAMES.get(pname);
            Log.warnOnce("gl.get:" + pname, "Unknown glGet parameter " + (name == null ? "" : name + " ")
                                                + "(0x" + Integer.toHexString(pname).toUpperCase() + "), returning 0. "
                                                + "Configure it with -Dhmc.lwjgl3.gl." + (name == null ? "<name>" : name) + "=<value>");
            return new int[]{0};
        }

        return values;
    }

    public static int getInteger(int pname) {
        return get(pname)[0];
    }

    /**
     * @param pname the GL constant.
     * @return the name of the constant, or {@code null} if it is unknown.
     */
    public static String name(int pname) {
        return NAMES.get(pname);
    }

    /**
     * @return the names of all parameters with a default value, for tests and diagnostics.
     */
    public static List<String> defaultNames() {
        return Collections.unmodifiableList(Arrays.asList(DEFAULTS.keySet().toArray(new String[0])));
    }

    private static void put(String name, int... values) {
        DEFAULTS.put(name, values);
    }

    private static int[] configured(String name, int[] defaultValue) {
        String value = Config.get("gl." + name);
        return value == null ? defaultValue : parse(name, value, defaultValue);
    }

    private static int[] parse(String name, String value, int[] defaultValue) {
        try {
            String[] parts = value.split(",");
            int[] result = new int[parts.length];
            for (int i = 0; i < parts.length; i++) {
                result[i] = Integer.decode(parts[i].trim());
            }

            return result;
        } catch (NumberFormatException e) {
            Log.warn("Invalid value for " + Config.PREFIX + "gl." + name + ": " + value);
            return defaultValue;
        }
    }

    private static Map<String, Integer> constants() {
        Map<String, Integer> result = new HashMap<>();
        for (Class<?> type = GL46C.class; type != null && type != Object.class; type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                int modifiers = field.getModifiers();
                if (Modifier.isStatic(modifiers) && Modifier.isPublic(modifiers) && field.getType() == int.class
                    && field.getName().startsWith("GL_")) {
                    try {
                        result.putIfAbsent(field.getName(), field.getInt(null));
                    } catch (IllegalAccessException e) {
                        throw new IllegalStateException(e);
                    }
                }
            }
        }

        result.putIfAbsent("GL_MAX_TEXTURE_UNITS", 0x84E2);
        result.putIfAbsent("GL_MAX_TEXTURE_COORDS", 0x8871);
        result.putIfAbsent("GL_MAX_LIGHTS", 0x0D31);
        result.putIfAbsent("GL_MAX_CLIP_PLANES", 0x0D32);
        result.putIfAbsent("GL_MAX_MODELVIEW_STACK_DEPTH", 0x0D36);
        result.putIfAbsent("GL_MAX_PROJECTION_STACK_DEPTH", 0x0D38);
        result.putIfAbsent("GL_MAX_TEXTURE_STACK_DEPTH", 0x0D39);
        result.putIfAbsent("GL_MAX_ATTRIB_STACK_DEPTH", 0x0D35);
        result.putIfAbsent("GL_MAX_CLIENT_ATTRIB_STACK_DEPTH", 0x0D3B);
        result.putIfAbsent("GL_MAX_NAME_STACK_DEPTH", 0x0D37);
        result.putIfAbsent("GL_MAX_LIST_NESTING", 0x0B31);
        result.putIfAbsent("GL_MAX_EVAL_ORDER", 0x0D30);
        result.putIfAbsent("GL_MAX_PIXEL_MAP_TABLE", 0x0D34);
        result.putIfAbsent("GL_RED_BITS", 0x0D52);
        result.putIfAbsent("GL_GREEN_BITS", 0x0D53);
        result.putIfAbsent("GL_BLUE_BITS", 0x0D54);
        result.putIfAbsent("GL_ALPHA_BITS", 0x0D55);
        result.putIfAbsent("GL_DEPTH_BITS", 0x0D56);
        result.putIfAbsent("GL_STENCIL_BITS", 0x0D57);
        result.putIfAbsent("GL_MAX_VARYING_FLOATS", 0x8B4B);
        return result;
    }

}
