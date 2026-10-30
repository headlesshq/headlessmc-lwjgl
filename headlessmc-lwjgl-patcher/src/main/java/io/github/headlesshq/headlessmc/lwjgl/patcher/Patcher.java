package io.github.headlesshq.headlessmc.lwjgl.patcher;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.tree.ClassNode;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.regex.Pattern;

/**
 * Patches a set of LWJGL module jars into headless jars.
 */
public final class Patcher {
    private static final String RUNTIME_OWNER = "io/github/headlesshq/headlessmc/";
    private static final String RUNTIME_SEGMENT = "/runtime/";
    private static final String SERVICES = "META-INF/services/";
    private static final String API_MODULE = "io.github.headlesshq.headlessmc.lwjgl.api";
    private static final Pattern DROPPED_RESOURCE = Pattern.compile(
        "META-INF/INDEX\\.LIST|META-INF/[^/]+\\.(SF|RSA|DSA|EC)|META-INF/.+\\.sha1|META-INF/versions/.+");

    private final Options options;
    private final ClassPool pool = new ClassPool();
    private final Map<String, Module> modules = new LinkedHashMap<>();
    private final List<String> errors = new ArrayList<>();

    public Patcher(Options options) {
        this.options = options;
    }

    /**
     * Runs the patcher.
     *
     * @return the errors, empty if the patched jars have been written.
     * @throws IOException if reading or writing fails.
     */
    public List<String> run() throws IOException {
        for (Map.Entry<String, Path> input : options.inputs().entrySet()) {
            modules.put(input.getKey(), read(input.getKey(), input.getValue()));
        }

        Synthetics synthetics = new Synthetics(pool);
        ApiUnion union = new ApiUnion(pool, synthetics);
        addCompatibilityApi(union);
        errors.addAll(union.errors());

        List<ClassNode> runtimeClasses = readRuntime();
        HandlerIndex handlers = new HandlerIndex(runtimeClasses, pool);
        errors.addAll(handlers.errors());
        Catalog catalog = new Catalog(Rules.create(handlers, pool, synthetics), union);
        ClassPatcher classPatcher = new ClassPatcher(pool, catalog, synthetics);
        Verifier verifier = new Verifier();

        for (Module module : modules.values()) {
            for (ClassNode node : module.classes) {
                String path = node.name + ".class";
                byte[] bytes = classPatcher.patch(module.name, node);
                verifier.verify(module.name + ":" + path, bytes);
                module.output.put(path, bytes);
            }
        }

        errors.addAll(verifier.errors());
        for (Module module : modules.values()) {
            checkLock(catalog, module);
        }

        if (!errors.isEmpty()) {
            return errors;
        }

        Files.createDirectories(options.output());
        Map<String, String> packageOwners = packageOwners();
        for (Module module : modules.values()) {
            write(module, packageOwners);
            System.out.println(module.name + ": " + catalog.summary(module.name));
        }

        return errors;
    }

    private void addCompatibilityApi(ApiUnion union) throws IOException {
        List<CompatInput> inputs = new ArrayList<>(options.compat());
        inputs.sort(Comparator.comparing(CompatInput::version, Patcher::compareVersions).reversed());
        for (CompatInput input : inputs) {
            Map<String, List<ClassNode>> added = union.addClasses(input.version(), input.module(), readBaseClasses(input.jar()));
            for (Map.Entry<String, List<ClassNode>> entry : added.entrySet()) {
                Module module = modules.get(entry.getKey());
                if (module == null) {
                    errors.add("LWJGL " + input.version() + " classes of " + entry.getKey() + " have no matching module");
                    continue;
                }

                for (ClassNode node : entry.getValue()) {
                    module.classes.add(node);
                    module.compatPackages.add(ApiUnion.packageName(node.name));
                }
            }
        }

        for (CompatInput input : inputs) {
            union.addMembers(input.version(), readBaseClasses(input.jar()));
        }
    }

    private static List<ClassNode> readBaseClasses(Path jar) throws IOException {
        List<ClassNode> result = new ArrayList<>();
        try (JarInputStream in = new JarInputStream(Files.newInputStream(jar))) {
            JarEntry entry;
            while ((entry = in.getNextJarEntry()) != null) {
                String path = entry.getName();
                if (entry.isDirectory() || !path.endsWith(".class") || path.endsWith("module-info.class")
                    || path.startsWith("META-INF/")) {
                    continue;
                }

                ClassNode node = new ClassNode();
                new ClassReader(in.readAllBytes()).accept(node, 0);
                result.add(node);
            }
        }

        return result;
    }

    /**
     * Compares versions like {@code 3.3.3} or {@code 2.9.2-nightly-20140822} part by part, numeric parts as numbers.
     */
    static int compareVersions(String first, String second) {
        String[] a = first.split("[.-]");
        String[] b = second.split("[.-]");
        for (int i = 0; i < Math.max(a.length, b.length); i++) {
            if (i >= a.length || i >= b.length) {
                return Integer.compare(a.length, b.length);
            }

            int difference = a[i].matches("\\d+") && b[i].matches("\\d+")
                ? Long.compare(Long.parseLong(a[i]), Long.parseLong(b[i]))
                : a[i].compareTo(b[i]);
            if (difference != 0) {
                return difference;
            }
        }

        return 0;
    }

    private Module read(String name, Path jar) throws IOException {
        Module module = new Module(name);
        try (JarInputStream in = new JarInputStream(Files.newInputStream(jar))) {
            module.manifest = in.getManifest() == null ? new Manifest() : in.getManifest();
            JarEntry entry;
            while ((entry = in.getNextJarEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }

                String path = entry.getName();
                byte[] bytes = in.readAllBytes();
                if (path.endsWith("module-info.class")) {
                    module.moduleInfos.put(path, bytes);
                } else if (DROPPED_RESOURCE.matcher(path).matches()) {
                    continue;
                } else if (path.endsWith(".class")) {
                    ClassNode node = new ClassNode();
                    new ClassReader(bytes).accept(node, 0);
                    pool.add(name, node);
                    module.classes.add(node);
                } else {
                    module.resources.put(path, bytes);
                }
            }
        }

        return module;
    }

    /**
     * Reads the runtime jars and assigns every runtime package to the module it is embedded into: the common runtime
     * package and the root package of a flavour runtime ({@code ...lwjgl3.runtime}) go into the core module
     * {@code lwjgl}, a sub package ({@code ...lwjgl3.runtime.glfw}) into the module of the same name
     * ({@code lwjgl-glfw}). Sub packages of the common runtime go into the core module if the flavour has no such
     * module, like LWJGL 2, which has OpenGL in its core jar.
     */
    private List<ClassNode> readRuntime() throws IOException {
        List<ClassNode> result = new ArrayList<>();
        for (Path runtime : options.runtimes()) {
            try (JarInputStream in = new JarInputStream(Files.newInputStream(runtime))) {
                JarEntry entry;
                while ((entry = in.getNextJarEntry()) != null) {
                    String path = entry.getName();
                    if (!entry.isDirectory() && path.startsWith(SERVICES) && modules.containsKey("lwjgl")) {
                        modules.get("lwjgl").resources.put(path, in.readAllBytes());
                        continue;
                    }

                    int runtimeRoot = path.indexOf(RUNTIME_SEGMENT);
                    if (entry.isDirectory() || !path.startsWith(RUNTIME_OWNER) || runtimeRoot < 0) {
                        continue;
                    }

                    byte[] bytes = in.readAllBytes();
                    if (path.endsWith(".class")) {
                        ClassNode node = new ClassNode();
                        new ClassReader(bytes).accept(node, ClassReader.SKIP_CODE);
                        result.add(node);
                    }

                    String pkg = path.substring(0, path.lastIndexOf('/'));
                    int subStart = runtimeRoot + RUNTIME_SEGMENT.length();
                    String sub = pkg.length() > subStart ? pkg.substring(subStart) : "";
                    String moduleName = sub.isEmpty() ? "lwjgl" : "lwjgl-" + sub;
                    if (path.startsWith(Behaviour.RUNTIME) && !modules.containsKey(moduleName)) {
                        moduleName = "lwjgl";
                    }

                    Module module = modules.get(moduleName);
                    if (module == null) {
                        errors.add("Runtime package " + pkg + " has no matching LWJGL module " + moduleName);
                        continue;
                    }

                    module.output.put(path, bytes);
                    module.runtimePackages.add(pkg);
                }
            }
        }

        return result;
    }

    private void checkLock(Catalog catalog, Module module) throws IOException {
        String header = "Behaviour catalog of org.lwjgl:" + module.name + ":" + options.lwjglVersion() + "\n"
            + "Every method of every class is patched by the first matching rule, counted per class.\n"
            + "Methods resolved by rules that only guess are listed individually and have to be reviewed.\n"
            + "Regenerate with: ./gradlew updateCatalogLock";
        String lock = catalog.lock(module.name, header);
        Path file = options.lockDirectory().resolve(module.name + ".lock");
        if (options.updateLock()) {
            Files.createDirectories(options.lockDirectory());
            Files.writeString(file, lock);
            return;
        }

        String existing = Files.exists(file) ? Files.readString(file) : "";
        if (!existing.equals(lock)) {
            Set<String> expected = new TreeSet<>(existing.lines().toList());
            Set<String> actual = new TreeSet<>(lock.lines().toList());
            StringBuilder diff = new StringBuilder();
            int shown = 0;
            for (String line : actual) {
                if (!expected.contains(line) && shown++ < 40) {
                    diff.append("\n  + ").append(line);
                }
            }

            for (String line : expected) {
                if (!actual.contains(line) && shown++ < 80) {
                    diff.append("\n  - ").append(line);
                }
            }

            errors.add("Behaviour catalog of " + module.name + " differs from " + file
                           + ", review the changes and run ./gradlew updateCatalogLock:" + diff);
        }
    }

    /**
     * @return the name of the module containing each package of a modular jar, keyed by internal package name.
     */
    private Map<String, String> packageOwners() {
        Map<String, String> result = new HashMap<>();
        for (Module module : modules.values()) {
            String moduleInfo = ModuleInfoPatcher.select(module.moduleInfos);
            if (moduleInfo != null) {
                String name = ModuleInfoPatcher.name(module.moduleInfos.get(moduleInfo));
                for (String pkg : packages(module)) {
                    result.put(pkg, name);
                }
            }
        }

        return result;
    }

    /**
     * The runtime embedded into one module can use the runtime embedded into another, e.g. the SDL patches use the
     * OpenGL state of the common runtime embedded into {@code lwjgl-opengl}. The original descriptor does not require
     * that module, so every module referenced by a patched class is required, statically, to keep it optional.
     *
     * @return the modules the classes of the module reference, the input API for the core module.
     */
    private static Set<String> requires(Module module, Map<String, String> packageOwners) {
        Set<String> result = new TreeSet<>();
        if ("lwjgl".equals(module.name)) {
            result.add(API_MODULE);
        }

        Remapper collector = new Remapper(Opcodes.ASM9) {
            @Override
            public String map(String internalName) {
                int index = internalName.lastIndexOf('/');
                String owner = index < 0 ? null : packageOwners.get(internalName.substring(0, index));
                if (owner != null) {
                    result.add(owner);
                }

                return internalName;
            }
        };

        for (Map.Entry<String, byte[]> entry : module.output.entrySet()) {
            if (entry.getKey().endsWith(".class")) {
                // the remapper only visits the code of methods its delegate visits
                new ClassReader(entry.getValue()).accept(new ClassRemapper(new ClassNode(), collector), 0);
            }
        }

        return result;
    }

    private void write(Module module, Map<String, String> packageOwners) throws IOException {
        Attributes attributes = module.manifest.getMainAttributes();
        attributes.putIfAbsent(Attributes.Name.MANIFEST_VERSION, "1.0");
        attributes.remove(Attributes.Name.SEALED);
        attributes.putValue("HeadlessMc-LWJGL3-Version", options.version());
        attributes.putValue("HeadlessMc-LWJGL-Version", options.lwjglVersion());
        module.manifest.getEntries().clear();

        Path target = options.output().resolve(module.name + ".jar");
        try (OutputStream os = Files.newOutputStream(target); JarOutputStream out = new JarOutputStream(os, module.manifest)) {
            Set<String> directories = new LinkedHashSet<>();
            String moduleInfo = ModuleInfoPatcher.select(module.moduleInfos);
            if (moduleInfo != null) {
                Set<String> exported = new TreeSet<>(module.runtimePackages);
                exported.addAll(module.compatPackages);
                byte[] bytes = module.moduleInfos.get(moduleInfo);
                Set<String> requires = requires(module, packageOwners);
                requires.remove(ModuleInfoPatcher.name(bytes));
                byte[] patched = ModuleInfoPatcher.patch(bytes, exported, packages(module), requires);
                put(out, directories, moduleInfo, patched);
            }

            for (Map.Entry<String, byte[]> entry : module.output.entrySet()) {
                put(out, directories, entry.getKey(), entry.getValue());
            }

            for (Map.Entry<String, byte[]> entry : module.resources.entrySet()) {
                put(out, directories, entry.getKey(), entry.getValue());
            }
        }
    }

    private static Set<String> packages(Module module) {
        Set<String> result = new TreeSet<>();
        for (String path : module.output.keySet()) {
            if (path.endsWith(".class") && path.contains("/")) {
                result.add(path.substring(0, path.lastIndexOf('/')));
            }
        }

        for (String path : module.resources.keySet()) {
            if (!path.startsWith("META-INF/") && path.contains("/")) {
                result.add(path.substring(0, path.lastIndexOf('/')));
            }
        }

        return result;
    }

    private static void put(JarOutputStream out, Set<String> directories, String path, byte[] bytes) throws IOException {
        for (int index = path.indexOf('/'); index >= 0; index = path.indexOf('/', index + 1)) {
            String directory = path.substring(0, index + 1);
            if (directories.add(directory)) {
                out.putNextEntry(new JarEntry(directory));
                out.closeEntry();
            }
        }

        JarEntry entry = new JarEntry(path);
        entry.setTime(0L);
        out.putNextEntry(entry);
        try (InputStream in = new ByteArrayInputStream(bytes)) {
            in.transferTo(out);
        }

        out.closeEntry();
    }

    /**
     * Configuration of a patcher run.
     *
     * @param inputs the LWJGL jars, keyed by module name, e.g. {@code lwjgl-opengl}.
     * @param runtimes the runtime jars, the common runtime and the runtime of the LWJGL flavour.
     * @param output the directory to write the patched jars to.
     * @param lockDirectory the directory containing the behaviour catalog lock files.
     * @param updateLock whether to write the lock files instead of checking them.
     * @param lwjglVersion the version of the input LWJGL jars.
     * @param version the version of headlessmc-lwjgl3.
     * @param compat the jars of older LWJGL versions whose public API is re-added.
     */
    public record Options(Map<String, Path> inputs, List<Path> runtimes, Path output, Path lockDirectory, boolean updateLock,
                          String lwjglVersion, String version, List<CompatInput> compat) {
    }

    /**
     * A module jar of an older LWJGL version.
     *
     * @param version the LWJGL version, e.g. {@code 3.3.3}.
     * @param module the module name, e.g. {@code lwjgl-opengl}.
     * @param jar the jar.
     */
    public record CompatInput(String version, String module, Path jar) {
    }

    private static final class Module {
        private final String name;
        private final List<ClassNode> classes = new ArrayList<>();
        private final Map<String, byte[]> moduleInfos = new LinkedHashMap<>();
        private final Map<String, byte[]> resources = new LinkedHashMap<>();
        private final Map<String, byte[]> output = new LinkedHashMap<>();
        private final Set<String> runtimePackages = new TreeSet<>();
        private final Set<String> compatPackages = new TreeSet<>();
        private Manifest manifest;

        private Module(String name) {
            this.name = name;
        }
    }

}
