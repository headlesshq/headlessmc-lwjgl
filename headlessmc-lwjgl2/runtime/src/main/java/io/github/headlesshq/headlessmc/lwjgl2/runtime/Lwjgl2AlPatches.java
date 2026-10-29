package io.github.headlesshq.headlessmc.lwjgl2.runtime;

import io.github.headlesshq.headlessmc.lwjgl.runtime.Patch;
import io.github.headlesshq.headlessmc.lwjgl.runtime.openal.AlState;
import org.lwjgl.openal.ALCdevice;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

/**
 * Patches for LWJGL 2's {@code AL10} and {@code ALC10} that keep the state of sources, buffers and the listener in
 * {@link AlState}, e.g. so that paulscode's sound system sees its sources stop.
 */
public final class Lwjgl2AlPatches {
    private static final String AL10 = "org/lwjgl/openal/AL10";
    private static final String ALC10 = "org/lwjgl/openal/ALC10";

    private Lwjgl2AlPatches() {
        throw new AssertionError();
    }

    @Patch(owner = AL10)
    public static void alSourcePlay(int source) {
        AlState.play(source);
    }

    @Patch(owner = AL10)
    public static void alSourcePlay(IntBuffer sources) {
        for (int i = sources.position(); i < sources.limit(); i++) {
            AlState.play(sources.get(i));
        }
    }

    @Patch(owner = AL10)
    public static void alSourcePause(int source) {
        AlState.pause(source);
    }

    @Patch(owner = AL10)
    public static void alSourcePause(IntBuffer sources) {
        for (int i = sources.position(); i < sources.limit(); i++) {
            AlState.pause(sources.get(i));
        }
    }

    @Patch(owner = AL10)
    public static void alSourceStop(int source) {
        AlState.stop(source);
    }

    @Patch(owner = AL10)
    public static void alSourceStop(IntBuffer sources) {
        for (int i = sources.position(); i < sources.limit(); i++) {
            AlState.stop(sources.get(i));
        }
    }

    @Patch(owner = AL10)
    public static void alSourceRewind(int source) {
        AlState.rewind(source);
    }

    @Patch(owner = AL10)
    public static void alSourceRewind(IntBuffer sources) {
        for (int i = sources.position(); i < sources.limit(); i++) {
            AlState.rewind(sources.get(i));
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
    public static void alSourcei(int source, int pname, int value) {
        AlState.sourcei(source, pname, value);
    }

    @Patch(owner = AL10)
    public static void alSourcef(int source, int pname, float value) {
        AlState.sourcef(source, pname, value);
    }

    @Patch(owner = AL10)
    public static int alGetSourcei(int source, int pname) {
        return AlState.getSourcei(source, pname);
    }

    @Patch(owner = AL10)
    public static float alGetSourcef(int source, int pname) {
        return AlState.getSourcef(source, pname)[0];
    }

    @Patch(owner = AL10)
    public static void alBufferData(int buffer, int format, ByteBuffer data, int freq) {
        AlState.bufferData(buffer, format, data.remaining() * 1, freq);
    }

    @Patch(owner = AL10)
    public static void alBufferData(int buffer, int format, ShortBuffer data, int freq) {
        AlState.bufferData(buffer, format, data.remaining() * 2, freq);
    }

    @Patch(owner = AL10)
    public static void alBufferData(int buffer, int format, IntBuffer data, int freq) {
        AlState.bufferData(buffer, format, data.remaining() * 4, freq);
    }

    @Patch(owner = AL10)
    public static int alGetBufferi(int buffer, int pname) {
        return AlState.getBufferi(buffer, pname);
    }

    @Patch(owner = AL10)
    public static float alGetBufferf(int buffer, int pname) {
        return AlState.getBufferi(buffer, pname);
    }

    @Patch(owner = AL10)
    public static void alListenerf(int pname, float value) {
        AlState.listener(pname, value);
    }

    @Patch(owner = AL10)
    public static float alGetListenerf(int pname) {
        return AlState.getListener(pname)[0];
    }

    @Patch(owner = AL10)
    public static String alGetString(int pname) {
        return AlState.getString(pname);
    }

    @Patch(owner = ALC10)
    public static String alcGetString(ALCdevice device, int pname) {
        return AlState.alcGetString(pname);
    }

    @Patch(owner = ALC10)
    public static void alcGetInteger(ALCdevice device, int pname, IntBuffer integerdata) {
        int[] values = AlState.alcGetIntegers(pname);
        for (int i = 0; i < values.length && i < integerdata.remaining(); i++) {
            integerdata.put(integerdata.position() + i, values[i]);
        }
    }

}
