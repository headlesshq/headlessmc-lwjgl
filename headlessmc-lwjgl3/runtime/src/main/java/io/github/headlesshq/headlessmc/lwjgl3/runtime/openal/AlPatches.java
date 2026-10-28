package io.github.headlesshq.headlessmc.lwjgl3.runtime.openal;

import io.github.headlesshq.headlessmc.lwjgl3.runtime.Dummies;
import io.github.headlesshq.headlessmc.lwjgl3.runtime.Patch;
import org.lwjgl.PointerBuffer;
import org.lwjgl.openal.ALCCapabilities;
import org.lwjgl.openal.ALCapabilities;

import java.util.function.IntFunction;

/**
 * Patches for {@code org.lwjgl.openal}: capabilities of the virtual audio device.
 * Only the core OpenAL 1.0/1.1 and ALC 1.0/1.1 features are reported as supported.
 */
public final class AlPatches {
    private static final String AL = "org/lwjgl/openal/AL";
    private static final String ALC = "org/lwjgl/openal/ALC";

    private AlPatches() {
        throw new AssertionError();
    }

    @Patch(owner = ALC)
    public static ALCCapabilities createCapabilities(long device) {
        return Dummies.fillCapabilities(Dummies.create(ALCCapabilities.class), AlPatches::isCoreAlc);
    }

    @Patch(owner = ALC)
    public static ALCCapabilities createCapabilities(long device, IntFunction<PointerBuffer> bufferFactory) {
        return createCapabilities(device);
    }

    @Patch(owner = AL)
    public static ALCapabilities createCapabilities(ALCCapabilities alcCapabilities) {
        return Dummies.fillCapabilities(Dummies.create(ALCapabilities.class), AlPatches::isCoreAl);
    }

    @Patch(owner = AL)
    public static ALCapabilities createCapabilities(ALCCapabilities alcCapabilities, IntFunction<PointerBuffer> bufferFactory) {
        return createCapabilities(alcCapabilities);
    }

    private static boolean isCoreAlc(String field) {
        return "OpenALC10".equals(field) || "OpenALC11".equals(field);
    }

    private static boolean isCoreAl(String field) {
        return "OpenAL10".equals(field) || "OpenAL11".equals(field);
    }

}
