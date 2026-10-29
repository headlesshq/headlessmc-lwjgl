package io.github.headlesshq.headlessmc.lwjgl.runtime.opengl;

import io.github.headlesshq.headlessmc.lwjgl.runtime.Config;
import io.github.headlesshq.headlessmc.lwjgl.runtime.Log;
import org.lwjgl.opengl.ARBVertexProgram;
import org.lwjgl.opengl.ATIMeminfo;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.NVXGPUMemoryInfo;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import static org.lwjgl.opengl.GL46C.*;

/**
 * The values returned by {@code glGet*} queries, keyed by the name of the GL constant.
 * <p>State the application sets, like the viewport or bindings, comes from {@link GlState}.
 * Defaults are the values recorded from a real driver by the probe ({@value #RECORDED}), completed by values that
 * resemble a desktop OpenGL 4.6 driver. Every value can be overridden with the system property
 * {@code hmc.lwjgl3.gl.<GL_CONSTANT_NAME>}, e.g. {@code -Dhmc.lwjgl3.gl.GL_MAX_TEXTURE_SIZE=8192}, values with
 * multiple components are comma separated. Queries for unknown parameters return 0 and are reported once.</p>
 */
public final class GlValues {
    private static final String RECORDED = "gl-values.properties";
    /**
     * Values that follow the configuration or the window, the recorded ones are ignored.
     */
    private static final Set<String> FORCED = new HashSet<>(Arrays.asList(
        "GL_MAJOR_VERSION", "GL_MINOR_VERSION", "GL_NUM_EXTENSIONS", "GL_CONTEXT_PROFILE_MASK", "GL_CONTEXT_FLAGS",
        "GL_VIEWPORT", "GL_SCISSOR_BOX"));
    private static final Set<Integer> NORMALIZED = new HashSet<>(Arrays.asList(
        GL_DEPTH_RANGE, GL_DEPTH_CLEAR_VALUE, GL_COLOR_CLEAR_VALUE, GL_BLEND_COLOR, GL11.GL_CURRENT_COLOR,
        GL11.GL_ACCUM_CLEAR_VALUE, GL11.GL_FOG_COLOR, GL11.GL_LIGHT_MODEL_AMBIENT, GL11.GL_CURRENT_RASTER_COLOR,
        GL11.GL_CURRENT_NORMAL, GL11.GL_MODELVIEW_MATRIX, GL11.GL_PROJECTION_MATRIX, GL11.GL_TEXTURE_MATRIX,
        GL13.GL_TRANSPOSE_MODELVIEW_MATRIX, GL13.GL_TRANSPOSE_PROJECTION_MATRIX, GL13.GL_TRANSPOSE_TEXTURE_MATRIX,
        ARBVertexProgram.GL_CURRENT_MATRIX_ARB, ARBVertexProgram.GL_TRANSPOSE_CURRENT_MATRIX_ARB,
        GL14.GL_CURRENT_SECONDARY_COLOR));
    private static final Map<String, double[]> DEFAULTS = new LinkedHashMap<>();
    private static final Map<Integer, String> NAMES = new HashMap<>();
    private static final Map<Integer, double[]> VALUES = new HashMap<>();

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

        put("GL_GPU_MEMORY_INFO_DEDICATED_VIDMEM_NVX", 8388608);
        put("GL_GPU_MEMORY_INFO_TOTAL_AVAILABLE_MEMORY_NVX", 8388608);
        put("GL_GPU_MEMORY_INFO_CURRENT_AVAILABLE_VIDMEM_NVX", 8388608);
        put("GL_GPU_MEMORY_INFO_EVICTION_COUNT_NVX", 0);
        put("GL_GPU_MEMORY_INFO_EVICTED_MEMORY_NVX", 0);
        put("GL_VBO_FREE_MEMORY_ATI", 8388608, 8388608, 0, 0);
        put("GL_TEXTURE_FREE_MEMORY_ATI", 8388608, 8388608, 0, 0);
        put("GL_RENDERBUFFER_FREE_MEMORY_ATI", 8388608, 8388608, 0, 0);

        Map<String, Integer> constants = constants();
        for (Map.Entry<String, Integer> constant : constants.entrySet()) {
            NAMES.putIfAbsent(constant.getValue(), constant.getKey());
        }

        for (Map.Entry<String, double[]> entry : DEFAULTS.entrySet()) {
            Integer pname = constants.get(entry.getKey());
            if (pname == null) {
                Log.debug("Unknown GL constant in defaults: " + entry.getKey());
                continue;
            }

            VALUES.put(pname, configured(entry.getKey(), entry.getValue()));
        }

        for (Map.Entry<Integer, double[]> entry : recorded().entrySet()) {
            String name = NAMES.get(entry.getKey());
            if (name == null || !FORCED.contains(name) && Config.get("gl." + name) == null) {
                VALUES.put(entry.getKey(), entry.getValue());
            }
        }

        for (Map.Entry<String, Integer> constant : constants.entrySet()) {
            String override = Config.get("gl." + constant.getKey());
            if (override != null) {
                VALUES.put(constant.getValue(), parse(constant.getKey(), override, new double[]{0.0}));
            }
        }
    }

    private GlValues() {
        throw new AssertionError();
    }

    /**
     * @param pname the GL constant of the queried state.
     * @return all components of the value rounded to integers, an array with a single 0 for unknown parameters.
     */
    public static int[] get(int pname) {
        double[] values = getDoubles(pname);
        boolean normalized = NORMALIZED.contains(pname);
        int[] result = new int[values.length];
        for (int i = 0; i < values.length; i++) {
            result[i] = (int) Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, toInteger(values[i], normalized)));
        }

        return result;
    }

    /**
     * Integer queries of normalized values, like {@code GL_DEPTH_RANGE} or {@code GL_COLOR_CLEAR_VALUE}, map
     * {@code [-1, 1]} linearly onto the integer range, as the OpenGL specification requires. Other values are rounded
     * to the nearest integer, halves away from zero, like real drivers do.
     */
    private static long toInteger(double value, boolean normalized) {
        if (normalized) {
            return (long) (value * Integer.MAX_VALUE);
        }

        return value < 0 ? -Math.round(-value) : Math.round(value);
    }

    /**
     * @param pname the GL constant of the queried state.
     * @return all components of the value, an array with a single 0 for unknown parameters.
     */
    public static double[] getDoubles(int pname) {
        double[] state = GlState.get(pname);
        if (state != null) {
            return state;
        }

        double[] values = VALUES.get(pname);
        if (values == null) {
            String name = NAMES.get(pname);
            Log.warnOnce("gl.get:" + pname, "Unknown glGet parameter " + (name == null ? "" : name + " ")
                                                + "(0x" + Integer.toHexString(pname).toUpperCase() + "), returning 0. "
                                                + "Configure it with -Dhmc.lwjgl3.gl." + (name == null ? "<name>" : name) + "=<value>");
            return new double[]{0.0};
        }

        return values;
    }

    public static int getInteger(int pname) {
        return get(pname)[0];
    }

    public static long getLong(int pname) {
        return getLongs(pname)[0];
    }

    /**
     * @param pname the GL constant of the queried state.
     * @return all components of the value as 64 bit integers.
     */
    public static long[] getLongs(int pname) {
        double[] values = getDoubles(pname);
        boolean normalized = NORMALIZED.contains(pname);
        long[] result = new long[values.length];
        for (int i = 0; i < values.length; i++) {
            result[i] = toInteger(values[i], normalized);
        }

        return result;
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
        double[] converted = new double[values.length];
        for (int i = 0; i < values.length; i++) {
            converted[i] = values[i];
        }

        DEFAULTS.put(name, converted);
    }

    /**
     * Reads the values recorded from a real driver by the probe, see {@code tools/lwjgl3-probe}.
     * The keys are the hexadecimal values of the parameters, since not every parameter has a name in the core GL classes.
     */
    private static Map<Integer, double[]> recorded() {
        Map<Integer, double[]> result = new LinkedHashMap<>();
        Properties properties = new Properties();
        try (InputStream in = GlValues.class.getResourceAsStream(RECORDED)) {
            if (in == null) {
                Log.debug("No recorded GL values found");
                return result;
            }

            properties.load(in);
        } catch (IOException e) {
            Log.warn("Failed to read the recorded GL values: " + e);
            return result;
        }

        for (String name : properties.stringPropertyNames()) {
            try {
                result.put(Integer.decode(name), parse(name, properties.getProperty(name), new double[]{0.0}));
            } catch (NumberFormatException e) {
                Log.warn("Invalid recorded GL parameter " + name);
            }
        }

        return result;
    }

    private static double[] configured(String name, double[] defaultValue) {
        String value = Config.get("gl." + name);
        return value == null ? defaultValue : parse(name, value, defaultValue);
    }

    private static double[] parse(String name, String value, double[] defaultValue) {
        try {
            String[] parts = value.split(",");
            double[] result = new double[parts.length];
            for (int i = 0; i < parts.length; i++) {
                String part = parts[i].trim();
                result[i] = part.startsWith("0x") || part.startsWith("-0x") ? Long.decode(part) : Double.parseDouble(part);
            }

            return result;
        } catch (NumberFormatException e) {
            Log.warn("Invalid value for " + Config.PREFIX + "gl." + name + ": " + value);
            return defaultValue;
        }
    }

    /**
     * @return the core classes declaring the GL constants: the {@code GL46C} hierarchy of LWJGL 3, or the
     * {@code GL11} to {@code GL44} classes of LWJGL 2.
     */
    private static List<Class<?>> constantClasses() {
        List<Class<?>> result = new ArrayList<>();
        Class<?> core = load("org.lwjgl.opengl.GL46C");
        if (core != null) {
            for (Class<?> type = core; type != null && type != Object.class; type = type.getSuperclass()) {
                result.add(type);
            }

            return result;
        }

        for (String version : new String[]{"11", "12", "13", "14", "15", "20", "21", "30", "31", "32", "33", "40", "41", "42", "43", "44"}) {
            Class<?> type = load("org.lwjgl.opengl.GL" + version);
            if (type != null) {
                result.add(type);
            }
        }

        return result;
    }

    private static Class<?> load(String name) {
        try {
            return Class.forName(name, false, GlValues.class.getClassLoader());
        } catch (ClassNotFoundException | LinkageError e) {
            return null;
        }
    }

    private static Map<String, Integer> constants() {
        Map<String, Integer> result = new HashMap<>();
        for (Class<?> type : constantClasses()) {
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
        result.putIfAbsent("GL_GPU_MEMORY_INFO_DEDICATED_VIDMEM_NVX", NVXGPUMemoryInfo.GL_GPU_MEMORY_INFO_DEDICATED_VIDMEM_NVX);
        result.putIfAbsent("GL_GPU_MEMORY_INFO_TOTAL_AVAILABLE_MEMORY_NVX", NVXGPUMemoryInfo.GL_GPU_MEMORY_INFO_TOTAL_AVAILABLE_MEMORY_NVX);
        result.putIfAbsent("GL_GPU_MEMORY_INFO_CURRENT_AVAILABLE_VIDMEM_NVX", NVXGPUMemoryInfo.GL_GPU_MEMORY_INFO_CURRENT_AVAILABLE_VIDMEM_NVX);
        result.putIfAbsent("GL_GPU_MEMORY_INFO_EVICTION_COUNT_NVX", NVXGPUMemoryInfo.GL_GPU_MEMORY_INFO_EVICTION_COUNT_NVX);
        result.putIfAbsent("GL_GPU_MEMORY_INFO_EVICTED_MEMORY_NVX", NVXGPUMemoryInfo.GL_GPU_MEMORY_INFO_EVICTED_MEMORY_NVX);
        result.putIfAbsent("GL_VBO_FREE_MEMORY_ATI", ATIMeminfo.GL_VBO_FREE_MEMORY_ATI);
        result.putIfAbsent("GL_TEXTURE_FREE_MEMORY_ATI", ATIMeminfo.GL_TEXTURE_FREE_MEMORY_ATI);
        result.putIfAbsent("GL_RENDERBUFFER_FREE_MEMORY_ATI", ATIMeminfo.GL_RENDERBUFFER_FREE_MEMORY_ATI);
        return result;
    }

}
