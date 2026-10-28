package io.github.headlesshq.headlessmc.lwjgl3.probe;

import org.lwjgl.BufferUtils;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.ALC;
import org.lwjgl.openal.ALC10;
import org.lwjgl.openal.ALCCapabilities;

import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.lwjgl.openal.AL10.*;
import static org.lwjgl.openal.ALC10.*;

/**
 * OpenAL: the default device with a current context, a source and a buffer with a few samples.
 * The workers run OpenAL Soft with {@code ALSOFT_DRIVERS=null}, so that nothing is played and the result does not
 * depend on the audio server.
 */
final class AlSuite extends Suite {
    private final Map<String, Long> handles = new LinkedHashMap<>();
    private final Map<String, Integer> objects = new LinkedHashMap<>();
    private Map<Integer, String> errors;
    private long device;

    @Override
    String name() {
        return "openal";
    }

    @Override
    void setUp() {
        device = alcOpenDevice((ByteBuffer) null);
        if (device == 0L) {
            throw new IllegalStateException("alcOpenDevice failed");
        }

        ALCCapabilities capabilities = ALC.createCapabilities(device);
        long context = alcCreateContext(device, (IntBuffer) null);
        if (context == 0L || !alcMakeContextCurrent(context)) {
            throw new IllegalStateException("Failed to create an OpenAL context");
        }

        AL.createCapabilities(capabilities);
        handles.put("ALCdevice *", device);
        handles.put("ALCcontext *", context);

        int buffer = alGenBuffers();
        alBufferData(buffer, AL_FORMAT_MONO16, BufferUtils.createShortBuffer(16), 44100);
        int source = alGenSources();
        alSourcei(source, AL_BUFFER, buffer);
        objects.put("buffer", buffer);
        objects.put("bufferName", buffer);
        objects.put("source", source);
        objects.put("sourceName", source);
        alGetError();
        alcGetError(device);
        errors = intConstants(List.of(AL10.class, ALC10.class));
    }

    @Override
    List<Class<?>> classes() {
        return classesOf(AL10.class, "org.lwjgl.openal");
    }

    @Override
    List<Class<?>> enumClasses() {
        return classesOf(AL10.class, "org.lwjgl.openal");
    }

    @Override
    List<String> contextEnums() {
        return List.of("AL_FORMAT_MONO16", "AL_FORMAT_STEREO16", "AL_GAIN", "AL_POSITION", "ALC_FREQUENCY");
    }

    @Override
    Map<String, Long> handles() {
        return handles;
    }

    @Override
    Map<String, Integer> objects() {
        return objects;
    }

    @Override
    List<String> strings() {
        return List.of("ALC_EXT_EFX", "AL_EXT_FLOAT32", "alGetString");
    }

    @Override
    String error(Method method) {
        int code = method.getName().startsWith("alc") ? alcGetError(device) : alGetError();
        return code == 0 ? null : errors.getOrDefault(code, Integer.toString(code));
    }

    @Override
    Map<String, Object> info() {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("ALC_DEVICE_SPECIFIER", alcGetString(device, ALC_DEVICE_SPECIFIER));
        info.put("AL_VENDOR", alGetString(AL_VENDOR));
        info.put("AL_RENDERER", alGetString(AL_RENDERER));
        info.put("AL_VERSION", alGetString(AL_VERSION));
        return info;
    }

    @Override
    Object capabilities(String internalName) {
        return switch (internalName) {
            case "org/lwjgl/openal/ALCapabilities" -> AL.getCapabilities();
            case "org/lwjgl/openal/ALCCapabilities" -> ALC.getCapabilities();
            default -> null;
        };
    }

    @Override
    Set<String> denied() {
        return Set.of(
            "alcCloseDevice", "alcMakeContextCurrent", "alcSetThreadContext", "alcDestroyContext",
            "alcCaptureCloseDevice", "alcSuspendContext", "alcDevicePauseSOFT", "alcResetDeviceSOFT",
            "alcReopenDeviceSOFT", "alcLoopbackOpenDeviceSOFT", "alcOpenDevice", "alcCaptureOpenDevice",
            "alcCreateContext"
        );
    }

}
