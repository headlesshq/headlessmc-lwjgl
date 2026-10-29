package io.github.headlesshq.headlessmc.lwjgl.patcher;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * The ordered rules that decide the behaviour of every patched method. The first rule that applies wins.
 * <p>Rules that only express a guess are {@link Rule#listed() listed}, so that every method they resolve appears in
 * the lock file and gets reviewed individually.</p>
 */
public final class Rules {
    private static final String CUSTOM_BUFFER = "org/lwjgl/system/CustomBuffer";
    private static final String STRUCT = "org/lwjgl/system/Struct";
    private static final String STRUCT_BUFFER = "org/lwjgl/system/StructBuffer";
    private static final String CALLBACK_INTERFACE = "org/lwjgl/system/CallbackI";

    /**
     * Pure Java classes without native code, of LWJGL 3 and LWJGL 2.
     */
    private static final Set<String> KEPT_CLASSES = Set.of(
        "org/lwjgl/Version",
        "org/lwjgl/system/Configuration",
        "org/lwjgl/system/MathUtil",
        "org/lwjgl/system/Struct$Member",
        "org/lwjgl/system/Struct$Layout",
        "org/lwjgl/LWJGLUtil",
        "org/lwjgl/opengl/DisplayMode",
        "org/lwjgl/opengl/PixelFormat",
        "org/lwjgl/opengl/ContextAttribs",
        "org/lwjgl/util/Color",
        "org/lwjgl/util/Dimension",
        "org/lwjgl/util/Point",
        "org/lwjgl/util/Rectangle"
    );

    private static final Set<String> KEPT_CLASS_PREFIXES = Set.of(
        "org/lwjgl/Version$",
        "org/lwjgl/system/Configuration$",
        "org/lwjgl/LWJGLUtil$",
        "org/lwjgl/input/OpenGLPackageAccess",
        "org/lwjgl/util/vector/"
    );

    /**
     * Pure Java lookups in classes that are patched otherwise, as {@code owner.name}: LWJGL 2's key names, built by the
     * static initializer, and the reflective lookup of LWJGL 2's OpenCL info utilities.
     */
    private static final Set<String> KEPT_METHODS = Set.of(
        "org/lwjgl/input/Keyboard.getKeyName",
        "org/lwjgl/input/Keyboard.getKeyIndex",
        "org/lwjgl/input/Keyboard.getKeyCount",
        "org/lwjgl/opencl/CLPlatform.getInfoUtilInstance"
    );

    private static final Set<String> STRUCT_ALLOCATIONS = Set.of("malloc", "calloc", "create", "mallocStack", "callocStack");

    private static final Set<String> STRUCT_LAYOUT_HELPERS = Set.of("addNestedMembers", "align", "getBytes");

    private static final Set<String> CUSTOM_BUFFER_BOOKKEEPING = Set.of(
        "address", "address0", "capacity", "position", "limit", "remaining", "hasRemaining",
        "mark", "reset", "clear", "flip", "rewind", "sizeof"
    );

    /**
     * Functions whose return value signals success, with the value that means success.
     */
    private static final Map<String, Object> SUCCESS = Map.ofEntries(
        Map.entry("glfwInit", true),
        Map.entry("alcMakeContextCurrent", true),
        Map.entry("alcCloseDevice", true),
        Map.entry("alcCaptureCloseDevice", true),
        Map.entry("alcSetThreadContext", true),
        Map.entry("alcResetDeviceSOFT", true),
        Map.entry("alcIsExtensionPresent", false),
        Map.entry("glCheckFramebufferStatus", 0x8CD5),
        Map.entry("glCheckFramebufferStatusEXT", 0x8CD5),
        Map.entry("glCheckNamedFramebufferStatus", 0x8CD5),
        Map.entry("glClientWaitSync", 0x911A),
        Map.entry("glUnmapBuffer", true),
        Map.entry("glUnmapNamedBuffer", true),
        Map.entry("glIsEnabled", false)
    );

    private static final Pattern SDL_QUERY = Pattern.compile(
        "SDL_(Has\\w*|Is\\w*|Should\\w*|Window(Has|Is)\\w*|\\w*Enabled|\\w*Active|\\w*Visible|PollEvent|WaitEvent\\w*|"
            + "TextInputActive|ScreenKeyboardShown|CursorVisible|GetBooleanProperty|EventEnabled)");
    private static final Pattern ID = Pattern.compile(
        "n?((gl|al|alc)(Gen|Create)[A-Z]\\w*|glFenceSync|glfwCreate\\w+|SDL_Create\\w+|SDL_GL_CreateContext|"
            + "alcOpenDevice|alcCaptureOpenDevice|alcCreateContext|alcLoopbackOpenDeviceSOFT)");
    private static final Pattern ALLOCATION = Pattern.compile(
        "n?(mem(Alloc|Calloc|Realloc|AlignedAlloc)\\w*|memAddress\\w*|memGlobalRef\\w*|malloc|calloc|realloc|aligned_alloc|"
            + "je_\\w*(alloc)\\w*|rpmalloc\\w*|rpcalloc|rprealloc|rpaligned_\\w*|SDL_(malloc|calloc|realloc|aligned_alloc))");
    private static final Pattern VALUES = Pattern.compile("bytes|shorts|ints|longs|floats|doubles");
    private static final List<String> SIZE_NAMES = List.of(
        "size", "capacity", "length", "count", "num", "elements", "bufSize", "nmemb", "n", "len"
    );

    private Rules() {
        throw new AssertionError();
    }

    /**
     * @param handlers the runtime handlers.
     * @param pool the classes that are being patched.
     * @param factories decides which classes have a synthetic dummy factory.
     * @return the ordered rules.
     */
    public static List<Rule> create(HandlerIndex handlers, ClassPool pool, Synthetics factories) {
        return List.of(
            rule("handler", false, handlers::get),
            rule("keep-constructor", false, m -> "<init>".equals(m.name()) ? new Behaviour.Keep() : null),
            rule("keep-abstract-interface-method", false, m -> m.isOwnerInterface() && m.isAbstract() ? new Behaviour.Keep() : null),
            rule("keep-enum", false, m -> m.isOwnerEnum() ? new Behaviour.Keep() : null),
            rule("keep-pure-java-class", false, m -> isKeptClass(m.owner().name) ? new Behaviour.Keep() : null),
            rule("keep-pure-java-method", false, m -> KEPT_METHODS.contains(m.owner().name + "." + m.name()) ? new Behaviour.Keep() : null),
            rule("keep-struct-layout", false, m -> STRUCT.equals(m.owner().name) && m.isStatic()
                && (m.name().startsWith("__") || STRUCT_LAYOUT_HELPERS.contains(m.name())) ? new Behaviour.Keep() : null),
            rule("keep-unsafe-lookup", false, m -> m.isStatic() && "getUnsafeInstance".equals(m.name())
                && "()Lsun/misc/Unsafe;".equals(m.desc()) ? new Behaviour.Keep() : null),
            rule("keep-buffer-bookkeeping", false, m -> !m.isStatic() && CUSTOM_BUFFER_BOOKKEEPING.contains(m.name())
                && (pool.isAssignable(m.owner().name, CUSTOM_BUFFER)) ? new Behaviour.Keep() : null),
            rule("keep-pointer-address", false, m -> !m.isStatic() && "address".equals(m.name()) && "()J".equals(m.desc())
                && ("org/lwjgl/system/Pointer$Default".equals(m.owner().name) || "org/lwjgl/system/Callback".equals(m.owner().name))
                ? new Behaviour.Keep() : null),
            rule("keep-struct-sizeof", false, m -> !m.isStatic() && "sizeof".equals(m.name()) && "()I".equals(m.desc())
                && pool.isAssignable(m.owner().name, STRUCT) ? new Behaviour.Keep() : null),
            rule("keep-struct", false, m -> isStructCode(m, pool) ? new Behaviour.Keep() : null),
            rule("identity", false, Rules::identity),
            rule("keep-to-string", false, m -> !m.isStatic() && "toString".equals(m.name())
                && "()Ljava/lang/String;".equals(m.desc()) ? new Behaviour.Keep() : null),
            rule("success", false, Rules::success),
            rule("sdl-success", false, Rules::sdlSuccess),
            rule("id", false, m -> isIdType(m.returnType()) && ID.matcher(m.name()).matches() ? new Behaviour.NextId() : null),
            rule("fake-address", false, m -> m.returnType().getSort() == Type.LONG && ALLOCATION.matcher(m.name()).matches()
                ? new Behaviour.Constant(Behaviour.FAKE_ADDRESS) : null),
            rule("fluent", false, m -> fluent(m, pool)),
            rule("values-buffer", false, Rules::valuesBuffer),
            rule("nio-buffer", false, Rules::nioBuffer),
            rule("nio-buffer-empty", true, m -> nioFactory(m.returnType()) != null ? new Behaviour.NioBuffer(nioFactory(m.returnType()), -1) : null),
            rule("lwjgl-buffer", false, m -> lwjglBuffer(m, pool, factories, true)),
            rule("lwjgl-buffer-empty", true, m -> lwjglBuffer(m, pool, factories, false)),
            rule("struct", false, m -> newObject(m, pool, factories, true)),
            rule("object", true, m -> newObject(m, pool, factories, false)),
            rule("interface-proxy", true, m -> interfaceProxy(m, pool)),
            rule("struct-member", false, m -> (pool.isAssignable(m.owner().name, STRUCT) || pool.isAssignable(m.owner().name, STRUCT_BUFFER))
                ? new Behaviour.TypeDefault() : null),
            rule("void", false, m -> m.returnType().getSort() == Type.VOID ? new Behaviour.TypeDefault() : null),
            rule("default", true, m -> new Behaviour.TypeDefault())
        );
    }

    private static boolean isKeptClass(String name) {
        if (KEPT_CLASSES.contains(name)) {
            return true;
        }

        for (String prefix : KEPT_CLASS_PREFIXES) {
            if (name.startsWith(prefix)) {
                return true;
            }
        }

        return false;
    }

    private static Behaviour identity(MethodInfo method) {
        if (method.isStatic()) {
            return null;
        }

        if ("equals".equals(method.name()) && "(Ljava/lang/Object;)Z".equals(method.desc())) {
            return new Behaviour.Identity("equals");
        }

        if ("hashCode".equals(method.name()) && "()I".equals(method.desc())) {
            return new Behaviour.Identity("hashCode");
        }

        return null;
    }

    private static Behaviour success(MethodInfo method) {
        Object value = SUCCESS.get(method.name());
        if (value == null) {
            return null;
        }

        int sort = method.returnType().getSort();
        boolean matches = value instanceof Boolean
            ? sort == Type.BOOLEAN || sort == Type.INT
            : sort == Type.INT || sort == Type.LONG;
        return matches ? new Behaviour.Constant(value) : null;
    }

    private static Behaviour sdlSuccess(MethodInfo method) {
        if (!method.owner().name.startsWith("org/lwjgl/sdl/") || method.returnType().getSort() != Type.BOOLEAN
            || !method.name().startsWith("SDL_") || SDL_QUERY.matcher(method.name()).matches()) {
            return null;
        }

        return new Behaviour.Constant(true);
    }

    private static boolean isIdType(Type type) {
        return type.getSort() == Type.INT || type.getSort() == Type.LONG;
    }

    /**
     * Structs, struct buffers and their base classes are pure Java on top of the {@code MemoryUtil} accessors and
     * are kept, except for the methods that allocate memory, which return structs with memory from the runtime.
     */
    private static boolean isStructCode(MethodInfo method, ClassPool pool) {
        String owner = method.owner().name;
        boolean struct = pool.isAssignable(owner, STRUCT) || pool.isAssignable(owner, STRUCT_BUFFER)
            || owner.startsWith(STRUCT_BUFFER + "$");
        return struct && !isStructAllocation(method, pool);
    }

    private static boolean isStructAllocation(MethodInfo method, ClassPool pool) {
        Type type = method.returnType();
        if (!method.isStatic() || !STRUCT_ALLOCATIONS.contains(method.name()) || type.getSort() != Type.OBJECT
            || !pool.isAssignable(type.getInternalName(), STRUCT) && !pool.isAssignable(type.getInternalName(), STRUCT_BUFFER)) {
            return false;
        }

        for (Type argument : method.argumentTypes()) {
            if (argument.getSort() == Type.LONG || "java/nio/ByteBuffer".equals(argument.getInternalName())) {
                return false;
            }
        }

        return true;
    }

    private static Behaviour fluent(MethodInfo method, ClassPool pool) {
        Type type = method.returnType();
        if (method.isStatic() || type.getSort() != Type.OBJECT || "java/lang/Object".equals(type.getInternalName())) {
            return null;
        }

        return pool.isAssignable(method.owner().name, type.getInternalName()) ? new Behaviour.ReturnThis() : null;
    }

    private static Behaviour valuesBuffer(MethodInfo method) {
        String factory = nioFactory(method.returnType());
        if (factory == null || !VALUES.matcher(method.name()).matches() || !method.name().equals(factory)) {
            return null;
        }

        Type element = nioElement(factory);
        Type[] arguments = method.argumentTypes();
        if (arguments.length == 0) {
            return null;
        }

        if (arguments.length == 1 && arguments[0].getDescriptor().equals("[" + element.getDescriptor())) {
            return new Behaviour.ValuesBuffer(element);
        }

        for (Type argument : arguments) {
            if (!argument.equals(element)) {
                return null;
            }
        }

        return new Behaviour.ValuesBuffer(element);
    }

    private static Behaviour nioBuffer(MethodInfo method) {
        String factory = nioFactory(method.returnType());
        if (factory == null) {
            return null;
        }

        int size = sizeParameter(method);
        return size < 0 ? null : new Behaviour.NioBuffer(factory, size);
    }

    private static Behaviour lwjglBuffer(MethodInfo method, ClassPool pool, Synthetics factories, boolean sized) {
        Type type = method.returnType();
        if (type.getSort() != Type.OBJECT || !pool.isAssignable(type.getInternalName(), CUSTOM_BUFFER)
            || !factories.hasBufferFactory(type.getInternalName())) {
            return null;
        }

        int size = sizeParameter(method);
        if (sized == size < 0) {
            return null;
        }

        return new Behaviour.LwjglBuffer(type.getInternalName(), size);
    }

    private static Behaviour newObject(MethodInfo method, ClassPool pool, Synthetics factories, boolean struct) {
        Type type = method.returnType();
        if (type.getSort() != Type.OBJECT || !factories.hasFactory(type.getInternalName())) {
            return null;
        }

        return pool.isAssignable(type.getInternalName(), STRUCT) == struct ? new Behaviour.NewObject(type.getInternalName()) : null;
    }

    private static Behaviour interfaceProxy(MethodInfo method, ClassPool pool) {
        Type type = method.returnType();
        if (type.getSort() != Type.OBJECT) {
            return null;
        }

        String name = type.getInternalName();
        if (!pool.contains(name) || !pool.isInterface(name) || pool.isAssignable(name, CALLBACK_INTERFACE)
            || (pool.get(name).access & Opcodes.ACC_ANNOTATION) != 0) {
            return null;
        }

        return new Behaviour.ProxyObject(name);
    }

    /**
     * @param method a method returning a buffer.
     * @return the index of the parameter holding the requested capacity, or -1.
     */
    static int sizeParameter(MethodInfo method) {
        Type[] arguments = method.argumentTypes();
        for (String candidate : SIZE_NAMES) {
            for (int i = 0; i < arguments.length; i++) {
                if (isSizeType(arguments[i]) && candidate.equals(method.parameterName(i))) {
                    return i;
                }
            }
        }

        if (arguments.length == 1 && isSizeType(arguments[0])) {
            return 0;
        }

        return -1;
    }

    private static boolean isSizeType(Type type) {
        return type.getSort() == Type.INT || type.getSort() == Type.LONG;
    }

    static String nioFactory(Type type) {
        if (type.getSort() != Type.OBJECT) {
            return null;
        }

        return switch (type.getInternalName()) {
            case "java/nio/ByteBuffer" -> "bytes";
            case "java/nio/ShortBuffer" -> "shorts";
            case "java/nio/CharBuffer" -> "chars";
            case "java/nio/IntBuffer" -> "ints";
            case "java/nio/LongBuffer" -> "longs";
            case "java/nio/FloatBuffer" -> "floats";
            case "java/nio/DoubleBuffer" -> "doubles";
            default -> null;
        };
    }

    private static Type nioElement(String factory) {
        return switch (factory) {
            case "bytes" -> Type.BYTE_TYPE;
            case "shorts" -> Type.SHORT_TYPE;
            case "ints" -> Type.INT_TYPE;
            case "longs" -> Type.LONG_TYPE;
            case "floats" -> Type.FLOAT_TYPE;
            case "doubles" -> Type.DOUBLE_TYPE;
            default -> Type.CHAR_TYPE;
        };
    }

    private static Rule rule(String name, boolean listed, Function<MethodInfo, Behaviour> function) {
        return new Rule() {
            @Override
            public String name() {
                return name;
            }

            @Override
            public boolean listed() {
                return listed;
            }

            @Override
            public Behaviour apply(MethodInfo method) {
                return function.apply(method);
            }
        };
    }

}
