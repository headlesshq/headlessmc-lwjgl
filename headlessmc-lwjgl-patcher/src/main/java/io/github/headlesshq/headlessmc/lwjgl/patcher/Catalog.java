package io.github.headlesshq.headlessmc.lwjgl.patcher;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Resolves the behaviour of every patched method and records the result for the lock file.
 */
public final class Catalog {
    public static final String STATIC_INIT_RULE = "guarded-static-init";

    private final List<Rule> rules;
    private final ApiUnion union;
    private final Map<String, Map<String, ClassEntry>> modules = new TreeMap<>();

    public Catalog(List<Rule> rules, ApiUnion union) {
        this.rules = rules;
        this.union = union;
    }

    /**
     * @param module the module the method belongs to.
     * @param method the method.
     * @return the resolved behaviour.
     */
    public Behaviour resolve(String module, MethodInfo method) {
        for (Rule rule : rules) {
            Behaviour behaviour = rule.apply(method);
            if (behaviour == null || behaviour instanceof Behaviour.Keep && mustReplace(method)) {
                continue;
            }

            record(module, method, rule.name(), rule.listed() ? behaviour.describe() : null);
            return behaviour;
        }

        throw new IllegalStateException("No rule applies to " + method.key());
    }

    /**
     * Records a method whose behaviour is not decided by a rule, e.g. the guarded static initializer.
     */
    public void record(String module, MethodInfo method, String rule, String listedDescription) {
        ClassEntry entry = modules.computeIfAbsent(module, k -> new TreeMap<>())
            .computeIfAbsent(method.owner().name, k -> new ClassEntry());
        entry.counts.merge(rule, 1, Integer::sum);
        String origin = union.memberOrigin(method.key());
        if (origin != null) {
            String description = listedDescription == null ? rule : rule + ": " + listedDescription;
            entry.listed.put(method.name() + method.desc(), description + " [shim " + origin + "]");
        } else if (listedDescription != null) {
            entry.listed.put(method.name() + method.desc(), rule + ": " + listedDescription);
        }
    }

    /**
     * @param module the module.
     * @param header the comment written at the top.
     * @return the lock file content of the given module.
     */
    public String lock(String module, String header) {
        StringBuilder builder = new StringBuilder();
        for (String line : header.split("\n")) {
            builder.append("# ").append(line).append('\n');
        }

        Map<String, ClassEntry> classes = modules.getOrDefault(module, Map.of());
        for (Map.Entry<String, ClassEntry> classEntry : classes.entrySet()) {
            builder.append(classEntry.getKey());
            String classOrigin = union.classOrigin(classEntry.getKey());
            if (classOrigin != null) {
                builder.append(" shim-class=").append(classOrigin);
            }

            for (Map.Entry<String, Integer> count : classEntry.getValue().counts.entrySet()) {
                builder.append(' ').append(count.getKey()).append('=').append(count.getValue());
            }

            builder.append('\n');
            for (Map.Entry<String, String> listed : classEntry.getValue().listed.entrySet()) {
                builder.append("    ").append(listed.getKey()).append(' ').append(listed.getValue()).append('\n');
            }

            for (Map.Entry<String, String> field : union.fieldOrigins(classEntry.getKey()).entrySet()) {
                builder.append("    ").append(field.getKey()).append(" [shim ").append(field.getValue()).append("]\n");
            }
        }

        return builder.toString();
    }

    /**
     * @param module the module.
     * @return the number of methods per rule, for the build output.
     */
    public Map<String, Integer> summary(String module) {
        Map<String, Integer> result = new TreeMap<>();
        for (ClassEntry entry : modules.getOrDefault(module, Map.of()).values()) {
            entry.counts.forEach((rule, count) -> result.merge(rule, count, Integer::sum));
        }

        return result;
    }

    private static boolean mustReplace(MethodInfo method) {
        return method.isNative() || method.isAbstract() && !method.isOwnerInterface();
    }

    private static final class ClassEntry {
        private final Map<String, Integer> counts = new TreeMap<>();
        private final Map<String, String> listed = new TreeMap<>();
    }

}
