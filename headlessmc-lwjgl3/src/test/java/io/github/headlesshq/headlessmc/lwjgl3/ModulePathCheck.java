package io.github.headlesshq.headlessmc.lwjgl3;

import org.lwjgl.glfw.GLFW;

/**
 * Runs the {@link StartupTest} calls and the {@link InputTest} with the patched jars and the API on the module path,
 * see the {@code testModulePath} task.
 * Uses reflection for the module API, because the tests are compiled for Java 8.
 */
public final class ModulePathCheck {
    private ModulePathCheck() {
        throw new AssertionError();
    }

    public static void main(String[] args) throws ReflectiveOperationException {
        Object module = Class.class.getMethod("getModule").invoke(GLFW.class);
        Object name = module.getClass().getMethod("getName").invoke(module);
        if (!"org.lwjgl.glfw".equals(name)) {
            throw new IllegalStateException("GLFW is not loaded as a named module: " + module);
        }

        StartupTest test = new StartupTest();
        test.memory();
        test.glfwAndOpenGl();
        test.sdl();
        test.openAl();
        new InputTest().glfw();
        System.out.println("Patched LWJGL works on the module path as " + module);
    }

}
