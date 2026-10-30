package io.github.headlesshq.headlessmc.lwjgl3.runtime.openal;

import io.github.headlesshq.headlessmc.lwjgl.runtime.Patch;
import io.github.headlesshq.headlessmc.lwjgl.runtime.openal.AlState;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.Collections;
import java.util.List;

/**
 * Patches that keep the state of sources, buffers and the listener in {@link AlState} and answer the queries for
 * it, the strings and the device attributes.
 */
public final class AlStatePatches {
    private static final String AL10 = "org/lwjgl/openal/AL10";
    private static final String ALC10 = "org/lwjgl/openal/ALC10";
    private static final String AL_UTIL = "org/lwjgl/openal/ALUtil";

    private AlStatePatches() {
        throw new AssertionError();
    }

    @Patch(owner = AL10)
    public static void alSourcePlay(int source) {
        AlState.play(source);
    }

    @Patch(owner = AL10)
    public static void alSourcePlayv(IntBuffer sources) {
        for (int i = sources.position(); i < sources.limit(); i++) {
            AlState.play(sources.get(i));
        }
    }

    @Patch(owner = AL10)
    public static void alSourcePlayv(int[] sources) {
        for (int source : sources) {
            AlState.play(source);
        }
    }

    @Patch(owner = AL10)
    public static void alSourcePause(int source) {
        AlState.pause(source);
    }

    @Patch(owner = AL10)
    public static void alSourcePausev(IntBuffer sources) {
        for (int i = sources.position(); i < sources.limit(); i++) {
            AlState.pause(sources.get(i));
        }
    }

    @Patch(owner = AL10)
    public static void alSourcePausev(int[] sources) {
        for (int source : sources) {
            AlState.pause(source);
        }
    }

    @Patch(owner = AL10)
    public static void alSourceStop(int source) {
        AlState.stop(source);
    }

    @Patch(owner = AL10)
    public static void alSourceStopv(IntBuffer sources) {
        for (int i = sources.position(); i < sources.limit(); i++) {
            AlState.stop(sources.get(i));
        }
    }

    @Patch(owner = AL10)
    public static void alSourceStopv(int[] sources) {
        for (int source : sources) {
            AlState.stop(source);
        }
    }

    @Patch(owner = AL10)
    public static void alSourceRewind(int source) {
        AlState.rewind(source);
    }

    @Patch(owner = AL10)
    public static void alSourceRewindv(IntBuffer sources) {
        for (int i = sources.position(); i < sources.limit(); i++) {
            AlState.rewind(sources.get(i));
        }
    }

    @Patch(owner = AL10)
    public static void alSourceRewindv(int[] sources) {
        for (int source : sources) {
            AlState.rewind(source);
        }
    }

    @Patch(owner = AL10)
    public static void alDeleteSources(int source) {
        AlState.deleteSource(source);
    }

    @Patch(owner = AL10)
    public static void alDeleteSources(IntBuffer sources) {
        for (int i = sources.position(); i < sources.limit(); i++) {
            AlState.deleteSource(sources.get(i));
        }
    }

    @Patch(owner = AL10)
    public static void alDeleteSources(int[] sources) {
        for (int source : sources) {
            AlState.deleteSource(source);
        }
    }

    @Patch(owner = AL10)
    public static void alDeleteBuffers(int buffer) {
        AlState.deleteBuffer(buffer);
    }

    @Patch(owner = AL10)
    public static void alDeleteBuffers(IntBuffer buffers) {
        for (int i = buffers.position(); i < buffers.limit(); i++) {
            AlState.deleteBuffer(buffers.get(i));
        }
    }

    @Patch(owner = AL10)
    public static void alDeleteBuffers(int[] buffers) {
        for (int buffer : buffers) {
            AlState.deleteBuffer(buffer);
        }
    }

    @Patch(owner = AL10)
    public static void alSourceQueueBuffers(int source, int buffer) {
        AlState.queue(source, buffer);
    }

    @Patch(owner = AL10)
    public static void alSourceQueueBuffers(int source, IntBuffer buffers) {
        for (int i = buffers.position(); i < buffers.limit(); i++) {
            AlState.queue(source, buffers.get(i));
        }
    }

    @Patch(owner = AL10)
    public static void alSourceQueueBuffers(int source, int[] buffers) {
        for (int buffer : buffers) {
            AlState.queue(source, buffer);
        }
    }

    @Patch(owner = AL10)
    public static int alSourceUnqueueBuffers(int source) {
        return AlState.unqueue(source);
    }

    @Patch(owner = AL10)
    public static void alSourceUnqueueBuffers(int source, IntBuffer buffers) {
        for (int i = buffers.position(); i < buffers.limit(); i++) {
            buffers.put(i, AlState.unqueue(source));
        }
    }

    @Patch(owner = AL10)
    public static void alSourceUnqueueBuffers(int source, int[] buffers) {
        for (int i = 0; i < buffers.length; i++) {
            buffers[i] = AlState.unqueue(source);
        }
    }

    @Patch(owner = AL10)
    public static void alSourcei(int source, int param, int value) {
        AlState.sourcei(source, param, value);
    }

    @Patch(owner = AL10)
    public static void alSourcef(int source, int param, float value) {
        AlState.sourcef(source, param, value);
    }

    @Patch(owner = AL10)
    public static void alSourcefv(int source, int param, FloatBuffer values) {
        AlState.sourcef(source, param, floats(values));
    }

    @Patch(owner = AL10)
    public static void alSourcefv(int source, int param, float[] values) {
        AlState.sourcef(source, param, values);
    }

    @Patch(owner = AL10)
    public static int alGetSourcei(int source, int param) {
        return AlState.getSourcei(source, param);
    }

    @Patch(owner = AL10)
    public static void alGetSourcei(int source, int param, IntBuffer value) {
        value.put(value.position(), AlState.getSourcei(source, param));
    }

    @Patch(owner = AL10)
    public static void alGetSourcei(int source, int param, int[] value) {
        value[0] = AlState.getSourcei(source, param);
    }

    @Patch(owner = AL10)
    public static void alGetSourceiv(int source, int param, IntBuffer values) {
        value(values, AlState.getSourcei(source, param));
    }

    @Patch(owner = AL10)
    public static void alGetSourceiv(int source, int param, int[] values) {
        values[0] = AlState.getSourcei(source, param);
    }

    @Patch(owner = AL10)
    public static float alGetSourcef(int source, int param) {
        return AlState.getSourcef(source, param)[0];
    }

    @Patch(owner = AL10)
    public static void alGetSourcef(int source, int param, FloatBuffer value) {
        value.put(value.position(), AlState.getSourcef(source, param)[0]);
    }

    @Patch(owner = AL10)
    public static void alGetSourcef(int source, int param, float[] value) {
        value[0] = AlState.getSourcef(source, param)[0];
    }

    @Patch(owner = AL10)
    public static void alGetSourcefv(int source, int param, FloatBuffer values) {
        put(values, AlState.getSourcef(source, param));
    }

    @Patch(owner = AL10)
    public static void alGetSourcefv(int source, int param, float[] values) {
        put(values, AlState.getSourcef(source, param));
    }

    @Patch(owner = AL10)
    public static void alBufferData(int buffer, int format, ByteBuffer data, int frequency) {
        AlState.bufferData(buffer, format, data.remaining() * 1, frequency);
    }

    @Patch(owner = AL10)
    public static void alBufferData(int buffer, int format, ShortBuffer data, int frequency) {
        AlState.bufferData(buffer, format, data.remaining() * 2, frequency);
    }

    @Patch(owner = AL10)
    public static void alBufferData(int buffer, int format, IntBuffer data, int frequency) {
        AlState.bufferData(buffer, format, data.remaining() * 4, frequency);
    }

    @Patch(owner = AL10)
    public static void alBufferData(int buffer, int format, FloatBuffer data, int frequency) {
        AlState.bufferData(buffer, format, data.remaining() * 4, frequency);
    }

    @Patch(owner = AL10)
    public static void alBufferData(int buffer, int format, short[] data, int frequency) {
        AlState.bufferData(buffer, format, data.length * 2, frequency);
    }

    @Patch(owner = AL10)
    public static void alBufferData(int buffer, int format, int[] data, int frequency) {
        AlState.bufferData(buffer, format, data.length * 4, frequency);
    }

    @Patch(owner = AL10)
    public static void alBufferData(int buffer, int format, float[] data, int frequency) {
        AlState.bufferData(buffer, format, data.length * 4, frequency);
    }

    @Patch(owner = AL10)
    public static int alGetBufferi(int buffer, int param) {
        return AlState.getBufferi(buffer, param);
    }

    @Patch(owner = AL10)
    public static void alGetBufferi(int buffer, int param, IntBuffer value) {
        value.put(value.position(), AlState.getBufferi(buffer, param));
    }

    @Patch(owner = AL10)
    public static void alGetBufferi(int buffer, int param, int[] value) {
        value[0] = AlState.getBufferi(buffer, param);
    }

    @Patch(owner = AL10)
    public static float alGetBufferf(int buffer, int param) {
        return AlState.getBufferi(buffer, param);
    }

    @Patch(owner = AL10)
    public static void alGetBufferf(int buffer, int param, FloatBuffer value) {
        value.put(value.position(), AlState.getBufferi(buffer, param));
    }

    @Patch(owner = AL10)
    public static void alGetBufferf(int buffer, int param, float[] value) {
        value[0] = AlState.getBufferi(buffer, param);
    }

    @Patch(owner = AL10)
    public static void alListenerf(int param, float value) {
        AlState.listener(param, value);
    }

    @Patch(owner = AL10)
    public static void alListenerfv(int param, FloatBuffer values) {
        AlState.listener(param, floats(values));
    }

    @Patch(owner = AL10)
    public static void alListenerfv(int param, float[] values) {
        AlState.listener(param, values);
    }

    @Patch(owner = AL10)
    public static float alGetListenerf(int param) {
        return AlState.getListener(param)[0];
    }

    @Patch(owner = AL10)
    public static void alGetListenerf(int param, FloatBuffer value) {
        value.put(value.position(), AlState.getListener(param)[0]);
    }

    @Patch(owner = AL10)
    public static void alGetListenerf(int param, float[] value) {
        value[0] = AlState.getListener(param)[0];
    }

    @Patch(owner = AL10)
    public static void alGetListenerfv(int param, FloatBuffer values) {
        put(values, AlState.getListener(param));
    }

    @Patch(owner = AL10)
    public static void alGetListenerfv(int param, float[] values) {
        put(values, AlState.getListener(param));
    }

    @Patch(owner = AL10)
    public static String alGetString(int paramName) {
        return AlState.getString(paramName);
    }

    @Patch(owner = ALC10)
    public static String alcGetString(long deviceHandle, int token) {
        return AlState.alcGetString(token);
    }

    /**
     * The device lists, like {@code ALC_ALL_DEVICES_SPECIFIER}, contain the one device, which Minecraft lists as its
     * audio devices.
     */
    @Patch(owner = AL_UTIL)
    public static List<String> getStringList(long deviceHandle, int token) {
        String value = AlState.alcGetString(token);
        return value == null ? null : Collections.singletonList(value);
    }

    @Patch(owner = ALC10)
    public static int alcGetInteger(long deviceHandle, int token) {
        return AlState.alcGetIntegers(token)[0];
    }

    @Patch(owner = ALC10)
    public static void alcGetIntegerv(long deviceHandle, int token, IntBuffer dest) {
        int[] values = AlState.alcGetIntegers(token);
        for (int i = 0; i < values.length && i < dest.remaining(); i++) {
            dest.put(dest.position() + i, values[i]);
        }
    }

    @Patch(owner = ALC10)
    public static void alcGetIntegerv(long deviceHandle, int token, int[] dest) {
        int[] values = AlState.alcGetIntegers(token);
        System.arraycopy(values, 0, dest, 0, Math.min(values.length, dest.length));
    }

    private static float[] floats(FloatBuffer buffer) {
        float[] values = new float[buffer.remaining()];
        for (int i = 0; i < values.length; i++) {
            values[i] = buffer.get(buffer.position() + i);
        }

        return values;
    }

    private static void put(FloatBuffer buffer, float[] values) {
        for (int i = 0; i < values.length && i < buffer.remaining(); i++) {
            buffer.put(buffer.position() + i, values[i]);
        }
    }

    private static void put(float[] array, float[] values) {
        System.arraycopy(values, 0, array, 0, Math.min(values.length, array.length));
    }

    private static void value(IntBuffer buffer, int value) {
        buffer.put(buffer.position(), value);
    }

}
