package org.jd.gui.util;

import org.jd.gui.api.model.Container;

import javax.tools.*;
import java.io.*;
import java.net.URI;
import java.util.*;

/**
 * Compiles Java sources in memory and exposes the resulting class files as container entries.
 */
public class InMemoryClassFiles {
    public static final String SHAPE =
            "package test;\n" +
            "public sealed interface Shape permits Circle, Square { double area(); }\n";
    public static final String CIRCLE =
            "package test;\n" +
            "public record Circle(double radius) implements Shape {\n" +
            "    public double area() { return Math.PI * radius * radius; }\n" +
            "}\n";
    public static final String SQUARE =
            "package test;\n" +
            "public final class Square implements Shape {\n" +
            "    private final double side;\n" +
            "    public Square(double side) { this.side = side; }\n" +
            "    public double area() { return side * side; }\n" +
            "    public static String describe(Shape shape) {\n" +
            "        String text = \"\"\"\n" +
            "            Shape description\n" +
            "            \"\"\";\n" +
            "        java.util.function.Supplier<String> supplier = () -> text + shape.area();\n" +
            "        if (shape instanceof Circle c) return \"circle \" + c.radius() + supplier.get();\n" +
            "        return switch (shape.getClass().getSimpleName()) { case \"Square\" -> \"square\"; default -> \"unknown\"; };\n" +
            "    }\n" +
            "    public enum Kind { SMALL, LARGE }\n" +
            "}\n";

    /**
     * @return the feature version of the running JVM (8, 11, 17, 21, ...)
     */
    public static int runtimeFeatureVersion() {
        String version = System.getProperty("java.specification.version");
        return Integer.parseInt(version.startsWith("1.") ? version.substring(2) : version);
    }

    /**
     * @return map [internal class name : class file bytes]
     */
    public static Map<String, byte[]> compile(int release) {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        Map<String, ByteArrayOutputStream> outputs = new TreeMap<>();
        List<JavaFileObject> sources = Arrays.asList(source("Shape", SHAPE), source("Circle", CIRCLE), source("Square", SQUARE));
        StandardJavaFileManager standardFileManager = compiler.getStandardFileManager(null, null, null);
        JavaFileManager fileManager = new ForwardingJavaFileManager<StandardJavaFileManager>(standardFileManager) {
            @Override
            public JavaFileObject getJavaFileForOutput(Location location, String className, JavaFileObject.Kind kind, FileObject sibling) {
                return new SimpleJavaFileObject(URI.create("mem:///" + className.replace('.', '/') + kind.extension), kind) {
                    @Override
                    public OutputStream openOutputStream() {
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        outputs.put(className.replace('.', '/'), baos);
                        return baos;
                    }
                };
            }
        };
        StringWriter diagnostics = new StringWriter();
        List<String> options = Arrays.asList("--release", String.valueOf(release));

        if (!compiler.getTask(diagnostics, fileManager, null, options, null, sources).call()) {
            throw new IllegalStateException("Compilation failed: " + diagnostics);
        }

        Map<String, byte[]> classFiles = new TreeMap<>();
        outputs.forEach((name, baos) -> classFiles.put(name, baos.toByteArray()));
        return classFiles;
    }

    protected static JavaFileObject source(String name, String code) {
        return new SimpleJavaFileObject(URI.create("string:///test/" + name + ".java"), JavaFileObject.Kind.SOURCE) {
            @Override public CharSequence getCharContent(boolean ignoreEncodingErrors) { return code; }
        };
    }

    /**
     * @return a directory entry "test" containing one entry per class file
     */
    public static Container.Entry toDirectoryEntry(Map<String, byte[]> classFiles) {
        return new MemoryEntry(null, "test", null, classFiles);
    }

    public static class MemoryEntry implements Container.Entry {
        protected final MemoryEntry parent;
        protected final String path;
        protected final byte[] content;
        protected final List<Container.Entry> children = new ArrayList<>();

        MemoryEntry(MemoryEntry parent, String path, byte[] content, Map<String, byte[]> classFiles) {
            this.parent = parent;
            this.path = path;
            this.content = content;

            if (classFiles != null) {
                classFiles.forEach((name, bytes) -> children.add(new MemoryEntry(this, name + ".class", bytes, null)));
            }
        }

        public Container.Entry getChild(String path) {
            for (Container.Entry child : children) {
                if (child.getPath().equals(path)) {
                    return child;
                }
            }
            throw new NoSuchElementException(path);
        }

        @Override public Container getContainer() { return null; }
        @Override public Container.Entry getParent() { return parent; }
        @Override public URI getUri() { return URI.create("mem:///" + path); }
        @Override public String getPath() { return path; }
        @Override public boolean isDirectory() { return content == null; }
        @Override public long length() { return content == null ? 0 : content.length; }
        @Override public InputStream getInputStream() { return new ByteArrayInputStream(content); }
        @Override public Collection<Container.Entry> getChildren() { return children; }
    }
}
