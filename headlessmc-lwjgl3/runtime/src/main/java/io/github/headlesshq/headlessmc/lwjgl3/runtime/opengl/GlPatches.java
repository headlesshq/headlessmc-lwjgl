package io.github.headlesshq.headlessmc.lwjgl3.runtime.opengl;

import io.github.headlesshq.headlessmc.lwjgl.runtime.Dummies;
import io.github.headlesshq.headlessmc.lwjgl.runtime.Patch;
import org.lwjgl.PointerBuffer;
import org.lwjgl.opengl.GLCapabilities;

import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.function.IntFunction;

/**
 * Patches for {@code org.lwjgl.opengl}: capabilities and state queries.
 */
public final class GlPatches {
    private static final String GL = "org/lwjgl/opengl/GL";
    private static final String GL11 = "org/lwjgl/opengl/GL11";
    private static final String GL11C = "org/lwjgl/opengl/GL11C";
    private static final String GL30 = "org/lwjgl/opengl/GL30";
    private static final String GL30C = "org/lwjgl/opengl/GL30C";
    private static final String GL32 = "org/lwjgl/opengl/GL32";
    private static final String GL32C = "org/lwjgl/opengl/GL32C";

    private static final int GL_VENDOR = 0x1F00;
    private static final int GL_RENDERER = 0x1F01;
    private static final int GL_VERSION = 0x1F02;
    private static final int GL_EXTENSIONS = 0x1F03;
    private static final int GL_SHADING_LANGUAGE_VERSION = 0x8B8C;

    private static final ThreadLocal<GLCapabilities> CURRENT = new ThreadLocal<>();
    private static volatile GLCapabilities last;

    private GlPatches() {
        throw new AssertionError();
    }

    @Patch(owner = GL)
    public static GLCapabilities createCapabilities() {
        GLCapabilities capabilities = Dummies.fillCapabilities(Dummies.create(GLCapabilities.class), GlInfo::isSupported);
        setCapabilities(capabilities);
        return capabilities;
    }

    @Patch(owner = GL)
    public static GLCapabilities createCapabilities(boolean forwardCompatible) {
        return createCapabilities();
    }

    @Patch(owner = GL)
    public static GLCapabilities createCapabilities(IntFunction<PointerBuffer> bufferFactory) {
        return createCapabilities();
    }

    @Patch(owner = GL)
    public static GLCapabilities createCapabilities(boolean forwardCompatible, IntFunction<PointerBuffer> bufferFactory) {
        return createCapabilities();
    }

    @Patch(owner = GL)
    public static void setCapabilities(GLCapabilities capabilities) {
        CURRENT.set(capabilities);
        if (capabilities != null) {
            last = capabilities;
        }
    }

    @Patch(owner = GL)
    public static GLCapabilities getCapabilities() {
        GLCapabilities capabilities = CURRENT.get();
        if (capabilities == null) {
            capabilities = last;
        }

        if (capabilities == null) {
            throw new IllegalStateException("No GLCapabilities instance set for the current thread, call GL.createCapabilities() first");
        }

        return capabilities;
    }

    @Patch(owner = {GL11C, GL11})
    public static int glGetInteger(int pname) {
        return GlValues.getInteger(pname);
    }

    @Patch(owner = {GL11C, GL11})
    public static void glGetIntegerv(int pname, IntBuffer params) {
        int[] values = GlValues.get(pname);
        for (int i = 0; i < values.length && params.position() + i < params.limit(); i++) {
            params.put(params.position() + i, values[i]);
        }
    }

    @Patch(owner = {GL11C, GL11})
    public static void glGetIntegerv(int pname, int[] params) {
        int[] values = GlValues.get(pname);
        System.arraycopy(values, 0, params, 0, Math.min(values.length, params.length));
    }

    @Patch(owner = {GL11C, GL11})
    public static float glGetFloat(int pname) {
        return (float) GlValues.getDoubles(pname)[0];
    }

    @Patch(owner = {GL11C, GL11})
    public static void glGetFloatv(int pname, FloatBuffer params) {
        double[] values = GlValues.getDoubles(pname);
        for (int i = 0; i < values.length && params.position() + i < params.limit(); i++) {
            params.put(params.position() + i, (float) values[i]);
        }
    }

    @Patch(owner = {GL11C, GL11})
    public static boolean glGetBoolean(int pname) {
        return GlValues.getDoubles(pname)[0] != 0.0;
    }

    @Patch(owner = {GL11C, GL11})
    public static void glGetFloatv(int pname, float[] params) {
        double[] values = GlValues.getDoubles(pname);
        for (int i = 0; i < values.length && i < params.length; i++) {
            params[i] = (float) values[i];
        }
    }

    @Patch(owner = {GL11C, GL11})
    public static double glGetDouble(int pname) {
        return GlValues.getDoubles(pname)[0];
    }

    @Patch(owner = {GL11C, GL11})
    public static void glGetDoublev(int pname, DoubleBuffer params) {
        double[] values = GlValues.getDoubles(pname);
        for (int i = 0; i < values.length && params.position() + i < params.limit(); i++) {
            params.put(params.position() + i, values[i]);
        }
    }

    @Patch(owner = {GL11C, GL11})
    public static void glGetDoublev(int pname, double[] params) {
        double[] values = GlValues.getDoubles(pname);
        System.arraycopy(values, 0, params, 0, Math.min(values.length, params.length));
    }

    @Patch(owner = {GL11C, GL11})
    public static void glGetBooleanv(int pname, ByteBuffer params) {
        double[] values = GlValues.getDoubles(pname);
        for (int i = 0; i < values.length && params.position() + i < params.limit(); i++) {
            params.put(params.position() + i, (byte) (values[i] != 0.0 ? 1 : 0));
        }
    }

    @Patch(owner = {GL32C, GL32})
    public static void glGetInteger64v(int pname, long[] params) {
        long[] values = GlValues.getLongs(pname);
        System.arraycopy(values, 0, params, 0, Math.min(values.length, params.length));
    }

    @Patch(owner = {GL32C, GL32})
    public static long glGetInteger64(int pname) {
        return GlValues.getLong(pname);
    }

    @Patch(owner = {GL32C, GL32})
    public static void glGetInteger64v(int pname, LongBuffer params) {
        long[] values = GlValues.getLongs(pname);
        for (int i = 0; i < values.length && params.position() + i < params.limit(); i++) {
            params.put(params.position() + i, values[i]);
        }
    }

    @Patch(owner = {GL11C, GL11})
    public static String glGetString(int name) {
        switch (name) {
            case GL_VENDOR:
                return GlInfo.vendor();
            case GL_RENDERER:
                return GlInfo.renderer();
            case GL_VERSION:
                return GlInfo.version();
            case GL_SHADING_LANGUAGE_VERSION:
                return GlInfo.shadingLanguageVersion();
            case GL_EXTENSIONS:
                return String.join(" ", GlInfo.extensions());
            default:
                return null;
        }
    }

    @Patch(owner = {GL30C, GL30})
    public static String glGetStringi(int name, int index) {
        if (name == GL_EXTENSIONS && index >= 0 && index < GlInfo.extensions().size()) {
            return GlInfo.extensions().get(index);
        }

        return null;
    }

}
