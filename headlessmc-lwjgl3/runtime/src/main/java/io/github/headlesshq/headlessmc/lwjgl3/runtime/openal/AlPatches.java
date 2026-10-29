package io.github.headlesshq.headlessmc.lwjgl3.runtime.openal;

import io.github.headlesshq.headlessmc.lwjgl.runtime.Dummies;
import io.github.headlesshq.headlessmc.lwjgl.runtime.Patch;
import org.lwjgl.PointerBuffer;
import org.lwjgl.openal.ALCCapabilities;
import org.lwjgl.openal.ALCapabilities;

import java.util.function.IntFunction;

/**
 * Patches for {@code org.lwjgl.openal}: capabilities of the virtual audio device. Like OpenAL Soft, the device
 * supports OpenAL 1.0/1.1, ALC 1.0/1.1 and every extension, e.g. Minecraft requires
 * {@code AL_EXT_source_distance_model}.
 */
public final class AlPatches {
    private static final String AL = "org/lwjgl/openal/AL";
    private static final String ALC = "org/lwjgl/openal/ALC";

    private AlPatches() {
        throw new AssertionError();
    }

    @Patch(owner = ALC)
    public static ALCCapabilities createCapabilities(long device) {
        return Dummies.fillCapabilities(Dummies.create(ALCCapabilities.class), field -> true);
    }

    @Patch(owner = ALC)
    public static ALCCapabilities createCapabilities(long device, IntFunction<PointerBuffer> bufferFactory) {
        return createCapabilities(device);
    }

    @Patch(owner = AL)
    public static ALCapabilities createCapabilities(ALCCapabilities alcCapabilities) {
        return Dummies.fillCapabilities(Dummies.create(ALCapabilities.class), field -> true);
    }

    @Patch(owner = AL)
    public static ALCapabilities createCapabilities(ALCCapabilities alcCapabilities, IntFunction<PointerBuffer> bufferFactory) {
        return createCapabilities(alcCapabilities);
    }

}
