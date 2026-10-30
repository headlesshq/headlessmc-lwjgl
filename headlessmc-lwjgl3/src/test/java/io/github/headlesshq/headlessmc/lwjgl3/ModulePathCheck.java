package io.github.headlesshq.headlessmc.lwjgl3;

import io.github.headlesshq.headlessmc.lwjgl.api.HeadlessLwjgl;
import org.lwjgl.glfw.GLFW;

/**
 * Runs the {@link StartupTest} calls, the {@link InputTest} and the {@link StructTest} with the patched jars and the API
 * on the module path,
 * see the {@code testModulePath} task, and with the API as an automatic module, see the {@code testModuleLayer} task.
 * Uses reflection for the module API, because the tests are compiled for Java 8.
 */
public final class ModulePathCheck {
    private static final String API_MODULE = "io.github.headlesshq.headlessmc.lwjgl.api";

    private ModulePathCheck() {
        throw new AssertionError();
    }

    public static void main(String[] args) throws ReflectiveOperationException {
        Object module = Class.class.getMethod("getModule").invoke(GLFW.class);
        Object name = module.getClass().getMethod("getName").invoke(module);
        if (!"org.lwjgl.glfw".equals(name)) {
            throw new IllegalStateException("GLFW is not loaded as a named module: " + module);
        }

        Object descriptor = module.getClass().getMethod("getDescriptor").invoke(module);
        if (!(Boolean) descriptor.getClass().getMethod("isOpen").invoke(descriptor)) {
            throw new IllegalStateException("The patched module is not open: " + descriptor);
        }

        Object api = Class.class.getMethod("getModule").invoke(HeadlessLwjgl.class);
        Object apiName = api.getClass().getMethod("getName").invoke(api);
        if (!API_MODULE.equals(apiName)) {
            throw new IllegalStateException("The API is not loaded as " + API_MODULE + ": " + api);
        }

        StartupTest test = new StartupTest();
        test.memory();
        test.glfwAndOpenGl();
        test.sdl();
        test.openAl();
        new InputTest().glfw();
        new InputTest().sdl();
        StructTest structs = new StructTest();
        structs.fieldsCanBeReadBack();
        structs.buffersHaveDistinctElements();
        structs.fakeAddressesAreHarmless();
        structs.videoMode();
        System.out.println("Patched LWJGL works on the module path as " + module);
    }

}
