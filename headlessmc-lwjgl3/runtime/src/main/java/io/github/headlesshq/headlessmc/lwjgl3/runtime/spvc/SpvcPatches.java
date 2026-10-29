package io.github.headlesshq.headlessmc.lwjgl3.runtime.spvc;

import io.github.headlesshq.headlessmc.lwjgl.runtime.DummyBuffers;
import io.github.headlesshq.headlessmc.lwjgl.runtime.Patch;
import org.lwjgl.PointerBuffer;

/**
 * Patches for SPIRV-Cross: shaders are not reflected, every resource list is a valid, empty list.
 */
public final class SpvcPatches {
    private static final String SPVC = "org/lwjgl/util/spvc/Spvc";

    private SpvcPatches() {
        throw new AssertionError();
    }

    @Patch(owner = SPVC)
    public static int spvc_resources_get_resource_list_for_type(long resources, int type, PointerBuffer resource_list,
                                                                PointerBuffer resource_size) {
        resource_list.put(resource_list.position(), DummyBuffers.FAKE_ADDRESS);
        resource_size.put(resource_size.position(), 0L);
        return 0;
    }

    @Patch(owner = SPVC)
    public static int spvc_resources_get_builtin_resource_list_for_type(long resources, int type,
                                                                        PointerBuffer resource_list,
                                                                        PointerBuffer resource_size) {
        return spvc_resources_get_resource_list_for_type(resources, type, resource_list, resource_size);
    }

}
