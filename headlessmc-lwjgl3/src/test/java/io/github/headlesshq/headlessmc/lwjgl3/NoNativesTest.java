package io.github.headlesshq.headlessmc.lwjgl3;

import org.junit.jupiter.api.Test;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryUtil;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class NoNativesTest {
    @Test
    void noLwjglLibraryIsMapped() throws IOException {
        File maps = new File("/proc/self/maps");
        assumeTrue(maps.exists(), "Requires /proc/self/maps");

        MemoryUtil.memAlloc(16);
        GLFW.glfwInit();
        GLFW.glfwMakeContextCurrent(GLFW.glfwCreateWindow(1, 1, "", 0L, 0L));
        GL.createCapabilities();

        List<String> lwjglLibraries = Files.readAllLines(maps.toPath(), StandardCharsets.UTF_8).stream()
            .filter(line -> line.contains(".so"))
            .filter(line -> line.contains("lwjgl") || line.contains("glfw") || line.contains("openal") || line.contains("SDL"))
            .collect(Collectors.toList());
        assertTrue(lwjglLibraries.isEmpty(), "Native libraries were loaded:\n" + String.join("\n", lwjglLibraries));
    }

}
