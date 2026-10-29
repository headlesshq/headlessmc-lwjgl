package io.github.headlesshq.headlessmc.lwjgl.patcher;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Fails the build if a japicmp XML report contains a binary incompatible change that is not explicitly allowed.
 * <pre>
 * --allow /path/to/api-compatibility.allow
 * --report 3.3.3=/path/to/report.xml (repeatable)
 * </pre>
 * Every line of the allow file is {@code CHANGE_TYPE class [member]}, as printed by this gate, {@code #} starts a
 * comment.
 */
public final class ApiCompatibilityGate {
    private ApiCompatibilityGate() {
        throw new AssertionError();
    }

    public static void main(String[] args) throws Exception {
        Path allowFile = null;
        List<String[]> reports = new ArrayList<>();
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--allow" -> allowFile = Path.of(args[++i]);
                case "--report" -> reports.add(args[++i].split("=", 2));
                default -> throw new IllegalArgumentException("Unknown argument " + args[i]);
            }
        }

        Set<String> allowed = new LinkedHashSet<>();
        if (allowFile != null && Files.exists(allowFile)) {
            for (String line : Files.readAllLines(allowFile)) {
                int comment = line.indexOf('#');
                String entry = (comment < 0 ? line : line.substring(0, comment)).trim().replaceAll("\\s+", " ");
                if (!entry.isEmpty()) {
                    allowed.add(entry);
                }
            }
        }

        Set<String> used = new TreeSet<>();
        List<String> violations = new ArrayList<>();
        for (String[] report : reports) {
            for (String change : changes(Path.of(report[1]))) {
                if (allowed.contains(change)) {
                    used.add(change);
                } else {
                    violations.add("LWJGL " + report[0] + ": " + change);
                }
            }
        }

        for (String entry : allowed) {
            if (!used.contains(entry)) {
                System.out.println("Unused entry in " + allowFile + ": " + entry);
            }
        }

        if (!violations.isEmpty()) {
            violations.forEach(System.err::println);
            System.err.println(violations.size() + " binary incompatible change(s), re-add the API in ApiUnion or allow them in " + allowFile);
            System.exit(1);
        }
    }

    private static List<String> changes(Path report) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        Document document = factory.newDocumentBuilder().parse(report.toFile());
        List<String> result = new ArrayList<>();
        List<Element> classes = new ArrayList<>();
        for (Element container : children(document.getDocumentElement(), "classes")) {
            classes.addAll(children(container, "class"));
        }

        for (Element type : classes) {
            String className = type.getAttribute("fullyQualifiedName");
            collect(result, type, className, "");
            for (String memberTag : List.of("method", "constructor", "field")) {
                for (Element member : elements(type, memberTag)) {
                    collect(result, member, className, describe(member, memberTag));
                }
            }

            for (Element itf : elements(type, "interface")) {
                collect(result, itf, className, "implements " + itf.getAttribute("fullyQualifiedName"));
            }

            for (Element superclass : children(type, "superclass")) {
                collect(result, superclass, className, "extends " + superclass.getAttribute("superclassOld"));
            }
        }

        return result;
    }

    private static String describe(Element member, String tag) {
        if ("field".equals(tag)) {
            return member.getAttribute("name");
        }

        List<String> parameters = new ArrayList<>();
        for (Element parameter : elements(member, "parameter")) {
            parameters.add(parameter.getAttribute("type"));
        }

        String name = "constructor".equals(tag) ? "<init>" : member.getAttribute("name");
        return name + "(" + String.join(",", parameters) + ")";
    }

    private static void collect(List<String> result, Element element, String className, String member) {
        for (Element changes : children(element, "compatibilityChanges")) {
            for (Element change : children(changes, "compatibilityChange")) {
                if ("false".equals(change.getAttribute("binaryCompatible"))) {
                    result.add((change.getAttribute("type") + " " + className + " " + member).trim());
                }
            }
        }
    }

    private static List<Element> elements(Element parent, String tag) {
        List<Element> result = new ArrayList<>();
        for (Element container : children(parent, tag + "s")) {
            result.addAll(children(container, tag));
        }

        return result;
    }

    private static List<Element> children(Element parent, String tag) {
        List<Element> result = new ArrayList<>();
        for (Node node = parent.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (node instanceof Element element && element.getTagName().equals(tag)) {
                result.add(element);
            }
        }

        return result;
    }

}
