package io.github.headlesshq.headlessmc.lwjgl.agent;

import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

class LwjglJarTest {
    private static String path(String... parts) {
        return String.join(File.separator, parts);
    }

    @Test
    void mavenLayout() {
        LwjglJar glfw = LwjglJar.of(path("libraries", "org", "lwjgl", "lwjgl-glfw", "3.3.3", "lwjgl-glfw-3.3.3.jar"));
        assertNotNull(glfw);
        assertEquals(Flavor.LWJGL3, glfw.flavor());
        assertEquals("lwjgl3/lwjgl-glfw.jar", glfw.patchedJar());

        LwjglJar unsafe = LwjglJar.of(path("libraries", "org", "lwjgl", "lwjgl", "3.4.1", "lwjgl-3.4.1-unsafe.jar"));
        assertNotNull(unsafe);
        assertEquals("lwjgl3/lwjgl.jar", unsafe.patchedJar());

        LwjglJar util = LwjglJar.of(path("libraries", "org", "lwjgl", "lwjgl", "lwjgl_util", "2.9.4-nightly-20150209",
            "lwjgl_util-2.9.4-nightly-20150209.jar"));
        assertNotNull(util);
        assertEquals("lwjgl2/lwjgl_util.jar", util.patchedJar());

        assertNull(LwjglJar.of(path("libraries", "org", "lwjgl", "lwjgl-glfw", "3.3.3", "lwjgl-glfw-3.3.3-natives-linux.jar")));
        assertNull(LwjglJar.of(path("libraries", "org", "lwjgl", "lwjgl", "lwjgl-platform", "2.9.4-nightly-20150209",
            "lwjgl-platform-2.9.4-nightly-20150209-natives-linux.jar")));
        assertNull(LwjglJar.of(path("libraries", "org", "joml", "joml", "1.10.5", "joml-1.10.5.jar")));
    }

    @Test
    void fileNames() {
        assertEquals("lwjgl3/lwjgl-opengl.jar", LwjglJar.of(path("lib", "lwjgl-opengl-3.2.2.jar")).patchedJar());
        assertEquals("lwjgl3/lwjgl.jar", LwjglJar.of("lwjgl-3.3.3.jar").patchedJar());
        assertEquals("lwjgl2/lwjgl.jar", LwjglJar.of("lwjgl-2.9.4-nightly-20150209.jar").patchedJar());
        assertEquals("lwjgl2/lwjgl_util.jar", LwjglJar.of("lwjgl_util-2.9.0.jar").patchedJar());
        assertNull(LwjglJar.of("lwjgl-3.3.3-natives-windows.jar"));
        assertNull(LwjglJar.of("lwjgl-glfw-3.3.3"));
        assertNull(LwjglJar.of("jinput-2.0.5.jar"));
    }

    @Test
    void flavors() {
        assertEquals(Flavor.LWJGL3, Flavor.of("LWJGL3"));
        assertEquals(Flavor.LWJGL2, Flavor.of(" lwjgl2"));
        assertThrows(IllegalArgumentException.class, () -> Flavor.of("lwjgl4"));
    }

}
