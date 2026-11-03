package io.github.headlesshq.headlessmc.lwjgl.agent.layer;

import java.io.File;
import java.lang.module.Configuration;
import java.lang.module.ModuleFinder;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Builds a module layer from the classpath like the BootstrapLauncher of Forge and NeoForge does, which reads
 * {@code legacyClassPath} or {@code java.class.path} after the agent rewrote them, and starts GLFW in it.
 * See the {@code agentCheckModuleLayer} task.
 */
public final class ModuleLayerCheck {
    private ModuleLayerCheck() {
        throw new AssertionError();
    }

    public static void main(String[] args) throws Exception {
        String classPath = System.getProperty("legacyClassPath", System.getProperty("java.class.path"));
        Path[] jars = Arrays.stream(classPath.split(File.pathSeparator))
            .filter(entry -> entry.endsWith(".jar"))
            .map(Path::of)
            .toArray(Path[]::new);
        ModuleFinder finder = ModuleFinder.of(jars);
        Set<String> modules = finder.findAll().stream().map(module -> module.descriptor().name()).collect(Collectors.toSet());
        Configuration configuration = ModuleLayer.boot().configuration().resolveAndBind(finder, ModuleFinder.of(), modules);
        ModuleLayer layer = ModuleLayer.boot().defineModulesWithOneLoader(configuration, ClassLoader.getPlatformClassLoader());

        Class<?> glfw = layer.findLoader("org.lwjgl.glfw").loadClass("org.lwjgl.glfw.GLFW");
        Module module = glfw.getModule();
        if (!"org.lwjgl.glfw".equals(module.getName()) || module.getLayer() != layer) {
            throw new IllegalStateException("GLFW is not loaded from the layer: " + module);
        }

        String location = glfw.getProtectionDomain().getCodeSource().getLocation().toString();
        if (!location.contains("agent-checks")) {
            throw new IllegalStateException("GLFW is not loaded from the patched jar: " + location);
        }

        if (!(Boolean) glfw.getMethod("glfwInit").invoke(null)) {
            throw new IllegalStateException("glfwInit");
        }

        long window = (Long) glfw.getMethod("glfwCreateWindow", int.class, int.class, CharSequence.class, long.class, long.class)
            .invoke(null, 854, 480, "Layer", 0L, 0L);
        if (window == 0L) {
            throw new IllegalStateException("glfwCreateWindow");
        }

        System.out.println("The agent patched LWJGL in the module layer, loaded from " + location);
    }

}
