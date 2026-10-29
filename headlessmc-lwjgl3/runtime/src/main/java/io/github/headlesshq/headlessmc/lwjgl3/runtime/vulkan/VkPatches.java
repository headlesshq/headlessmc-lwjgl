package io.github.headlesshq.headlessmc.lwjgl3.runtime.vulkan;

import io.github.headlesshq.headlessmc.lwjgl.runtime.Config;
import io.github.headlesshq.headlessmc.lwjgl.runtime.Dummies;
import io.github.headlesshq.headlessmc.lwjgl.runtime.Patch;
import org.lwjgl.system.FunctionProvider;
import org.lwjgl.system.SharedLibrary;

/**
 * Patches for {@code org.lwjgl.vulkan.VK}: by default there is no Vulkan loader, like on a machine without Vulkan
 * drivers, so that applications that prefer Vulkan, like Minecraft 26.3, fall back to OpenGL, which HeadlessMc
 * emulates. With {@code hmc.lwjgl3.vulkan=true} the loader is available and every Vulkan function is a dummy.
 */
public final class VkPatches {
    private static final String VK = "org/lwjgl/vulkan/VK";
    private static final boolean ENABLED = Config.getBoolean("vulkan", false);

    private static volatile boolean created;

    private VkPatches() {
        throw new AssertionError();
    }

    @Patch(owner = VK)
    public static void create() {
        if (!ENABLED) {
            throw new IllegalStateException("There is no Vulkan loader, HeadlessMc emulates OpenGL");
        }

        created = true;
    }

    @Patch(owner = VK)
    public static void create(String libName) {
        create();
    }

    @Patch(owner = VK)
    public static void create(FunctionProvider functionProvider) {
        create();
    }

    @Patch(owner = VK)
    public static void destroy() {
        created = false;
    }

    @Patch(owner = VK)
    public static FunctionProvider getFunctionProvider() {
        if (!created) {
            throw new IllegalStateException("Vulkan has not been loaded.");
        }

        return (FunctionProvider) Dummies.proxy(SharedLibrary.class);
    }

}
