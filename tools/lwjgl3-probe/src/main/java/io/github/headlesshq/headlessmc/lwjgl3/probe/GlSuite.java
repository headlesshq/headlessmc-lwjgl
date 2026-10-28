package io.github.headlesshq.headlessmc.lwjgl3.probe;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11C;

import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL46C.*;

/**
 * GLFW and OpenGL: a hidden GLFW window with a current OpenGL context and one object of every common kind.
 */
final class GlSuite extends Suite {
    private static final String VERTEX_SHADER = """
        #version 150
        in vec4 a_position;
        uniform vec4 u_value;
        void main() { gl_Position = a_position + u_value; }
        """;
    private static final String FRAGMENT_SHADER = """
        #version 150
        out vec4 color;
        uniform vec4 u_value;
        void main() { color = u_value; }
        """;

    private final Map<String, Long> handles = new LinkedHashMap<>();
    private final Map<String, Integer> objects = new LinkedHashMap<>();
    private Map<Integer, String> glfwErrors;

    @Override
    String name() {
        return "glfw-opengl";
    }

    @Override
    void setUp() {
        if (!glfwInit()) {
            throw new IllegalStateException("glfwInit failed");
        }

        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        long window = glfwCreateWindow(854, 480, "HeadlessMc LWJGL probe", 0L, 0L);
        if (window == 0L) {
            throw new IllegalStateException("glfwCreateWindow failed");
        }

        glfwMakeContextCurrent(window);
        GL.createCapabilities();
        handles.put("GLFWwindow *", window);
        handles.put("GLFWmonitor *", glfwGetPrimaryMonitor());
        handles.put("GLFWcursor *", glfwCreateStandardCursor(GLFW_ARROW_CURSOR));

        int texture = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, texture);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, 4, 4, 0, GL_RGBA, GL_UNSIGNED_BYTE, (ByteBuffer) null);
        int buffer = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, buffer);
        glBufferData(GL_ARRAY_BUFFER, 64L, GL_STATIC_DRAW);
        int vertexShader = shader(GL_VERTEX_SHADER, VERTEX_SHADER);
        int fragmentShader = shader(GL_FRAGMENT_SHADER, FRAGMENT_SHADER);
        int program = glCreateProgram();
        glAttachShader(program, vertexShader);
        glAttachShader(program, fragmentShader);
        glLinkProgram(program);
        glUseProgram(program);
        int vertexArray = glGenVertexArrays();
        glBindVertexArray(vertexArray);
        int renderbuffer = glGenRenderbuffers();
        glBindRenderbuffer(GL_RENDERBUFFER, renderbuffer);
        glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH24_STENCIL8, 4, 4);
        int framebuffer = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, framebuffer);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, texture, 0);
        glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_STENCIL_ATTACHMENT, GL_RENDERBUFFER, renderbuffer);
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        int sampler = glGenSamplers();
        glBindSampler(0, sampler);
        int query = glGenQueries();
        glBeginQuery(GL_SAMPLES_PASSED, query);
        glEndQuery(GL_SAMPLES_PASSED);
        glFinish();
        handles.put("GLsync", glFenceSync(GL_SYNC_GPU_COMMANDS_COMPLETE, 0));

        objects.put("texture", texture);
        objects.put("buffer", buffer);
        objects.put("program", program);
        objects.put("shader", vertexShader);
        objects.put("framebuffer", framebuffer);
        objects.put("renderbuffer", renderbuffer);
        objects.put("array", vertexArray);
        objects.put("vaobj", vertexArray);
        objects.put("sampler", sampler);
        objects.put("id", query);
        while (glGetError() != GL_NO_ERROR) {
            glFinish();
        }

        glfwGetError(null);
        glfwErrors = intConstants(List.of(GLFW.class));
    }

    private static int shader(int type, String source) {
        int shader = glCreateShader(type);
        glShaderSource(shader, source);
        glCompileShader(shader);
        if (glGetShaderi(shader, GL_COMPILE_STATUS) != GL_TRUE) {
            throw new IllegalStateException("Shader compilation failed: " + glGetShaderInfoLog(shader));
        }

        return shader;
    }

    @Override
    List<Class<?>> classes() {
        List<Class<?>> result = new ArrayList<>();
        for (Class<?> type : classesOf(GL11C.class, "org.lwjgl.opengl")) {
            String name = type.getSimpleName();
            if (!name.startsWith("WGL") && !name.startsWith("CGL") && !name.startsWith("GLX")) {
                result.add(type);
            }
        }

        result.add(GLFW.class);
        return result;
    }

    @Override
    List<Class<?>> enumClasses() {
        List<Class<?>> result = new ArrayList<>();
        List<Class<?>> rest = new ArrayList<>();
        for (Class<?> type : classesOf(GL11C.class, "org.lwjgl.opengl")) {
            (type.getSimpleName().matches("GL\\d+C") ? result : rest).add(type);
        }

        result.addAll(rest);
        result.add(GLFW.class);
        return result;
    }

    @Override
    List<String> contextEnums() {
        return List.of(
            "GL_TEXTURE_2D", "GL_TEXTURE_CUBE_MAP", "GL_TEXTURE_2D_ARRAY", "GL_ARRAY_BUFFER", "GL_ELEMENT_ARRAY_BUFFER",
            "GL_UNIFORM_BUFFER", "GL_FRAMEBUFFER", "GL_READ_FRAMEBUFFER", "GL_DRAW_FRAMEBUFFER", "GL_RENDERBUFFER",
            "GL_COLOR_ATTACHMENT0", "GL_DEPTH_STENCIL_ATTACHMENT", "GL_BACK_LEFT", "GL_FRONT_AND_BACK",
            "GL_VERTEX_SHADER", "GL_FRAGMENT_SHADER", "GL_SAMPLES_PASSED", "GL_TIME_ELAPSED", "GL_RGBA8", "GL_RGBA",
            "GL_DEPTH24_STENCIL8", "GL_UNSIGNED_BYTE", "GL_FLOAT", "GL_LOW_FLOAT", "GL_HIGH_FLOAT", "GL_TEXTURE0",
            "GL_UNIFORM", "GL_PROGRAM_INPUT", "GL_PROGRAM_OUTPUT", "GL_FRAGMENT_SHADER_BIT", "GL_DEBUG_SOURCE_API"
        );
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
        return List.of("u_value", "a_position", "GL_ARB_debug_output");
    }

    @Override
    String error(Method method) {
        if (method.getDeclaringClass() == GLFW.class) {
            int code = glfwGetError(null);
            return code == GLFW_NO_ERROR ? null : glfwErrors.getOrDefault(code, Integer.toString(code));
        }

        int code = glGetError();
        if (code == GL_NO_ERROR) {
            return null;
        }

        while (glGetError() != GL_NO_ERROR) {
            glFinish();
        }

        return switch (code) {
            case GL_INVALID_ENUM -> "GL_INVALID_ENUM";
            case GL_INVALID_VALUE -> "GL_INVALID_VALUE";
            case GL_INVALID_OPERATION -> "GL_INVALID_OPERATION";
            case GL_INVALID_FRAMEBUFFER_OPERATION -> "GL_INVALID_FRAMEBUFFER_OPERATION";
            case GL_OUT_OF_MEMORY -> "GL_OUT_OF_MEMORY";
            case GL_STACK_OVERFLOW -> "GL_STACK_OVERFLOW";
            case GL_STACK_UNDERFLOW -> "GL_STACK_UNDERFLOW";
            default -> Integer.toString(code);
        };
    }

    @Override
    Map<String, Object> info() {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("glfw", glfwGetVersionString());
        info.put("platform", glfwGetPlatform());
        info.put("GL_VENDOR", glGetString(GL_VENDOR));
        info.put("GL_RENDERER", glGetString(GL_RENDERER));
        info.put("GL_VERSION", glGetString(GL_VERSION));
        info.put("GL_SHADING_LANGUAGE_VERSION", glGetString(GL_SHADING_LANGUAGE_VERSION));
        return info;
    }

    @Override
    Object capabilities(String internalName) {
        return "org/lwjgl/opengl/GLCapabilities".equals(internalName) ? GL.getCapabilities() : null;
    }

    @Override
    Set<String> denied() {
        return Set.of(
            "glfwTerminate", "glfwDestroyWindow", "glfwMakeContextCurrent", "glfwSetWindowMonitor",
            "glfwGetClipboardString", "glfwSetClipboardString", "glfwGetX11SelectionString", "glfwWaitEvents",
            "glfwWaitEventsTimeout", "glfwInitAllocator", "glfwSetErrorCallback", "glfwCreateWindow"
        );
    }

}
