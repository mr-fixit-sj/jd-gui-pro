package org.jd.gui.service.indexer;

import org.jd.gui.api.model.Container;
import org.jd.gui.api.model.Indexes;
import org.jd.gui.api.model.Type;
import org.jd.gui.service.type.ClassFileTypeFactoryProvider;
import org.jd.gui.util.InMemoryClassFiles;
import org.jd.gui.util.container.JarContainerEntryUtil;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;

import java.util.*;
import java.util.stream.Collectors;

import static org.jd.gui.util.InMemoryClassFiles.runtimeFeatureVersion;

public class ClassFileIndexerProviderTest {
    @Test
    public void testIndexJava17ClassFiles() {
        assertIndexed(compile(17));
    }

    @Test
    public void testIndexClassFilesOfRunningJdk() {
        assertIndexed(compile(runtimeFeatureVersion()));
    }

    @Test
    public void testIndexClassFilesNewerThanAsm() {
        Map<String, byte[]> classFiles = compile(17);
        // Simulate class files produced by a future JDK
        classFiles.values().forEach(bytes -> { bytes[6] = 0; bytes[7] = (byte)99; });
        assertIndexed(classFiles);
    }

    @Test
    public void testMakeTypes() {
        InMemoryClassFiles.MemoryEntry directory = (InMemoryClassFiles.MemoryEntry)InMemoryClassFiles.toDirectoryEntry(compile(17));
        ClassFileTypeFactoryProvider provider = new ClassFileTypeFactoryProvider();

        Type square = provider.make(null, directory.getChild("test/Square.class"), null);
        Assert.assertNotNull(square);
        Assert.assertEquals("test/Square", square.getName());
        Assert.assertTrue(methodNames(square).containsAll(Arrays.asList("<init>", "area", "describe")));
        Assert.assertEquals(Collections.singletonList("side"), square.getFields().stream().map(Type.Field::getName).collect(Collectors.toList()));
        Assert.assertEquals(1, square.getInnerTypes().size());
        Assert.assertEquals("Square.Kind", square.getInnerTypes().iterator().next().getDisplayTypeName());

        Type circle = provider.make(null, directory.getChild("test/Circle.class"), null);
        Assert.assertNotNull(circle);
        Assert.assertEquals("java/lang/Record", circle.getSuperName());
        Assert.assertTrue(methodNames(circle).containsAll(Arrays.asList("radius", "area")));

        // Types are cached per entry, so look up the inner type through a fresh provider
        Type kind = new ClassFileTypeFactoryProvider().make(null, directory.getChild("test/Square.class"), "test/Square$Kind");
        Assert.assertNotNull(kind);
        Assert.assertEquals("test/Square$Kind", kind.getName());
    }

    @Test
    public void testRemoveInnerTypeEntries() {
        Container.Entry directory = InMemoryClassFiles.toDirectoryEntry(compile(17));
        List<String> paths = JarContainerEntryUtil.removeInnerTypeEntries(directory.getChildren()).stream()
                .map(Container.Entry::getPath).collect(Collectors.toList());

        Assert.assertEquals(Arrays.asList("test/Circle.class", "test/Shape.class", "test/Square.class"), paths);
    }

    protected static Map<String, byte[]> compile(int release) {
        Assume.assumeTrue("Requires JDK " + release + "+", runtimeFeatureVersion() >= release);
        return InMemoryClassFiles.compile(release);
    }

    protected static List<String> methodNames(Type type) {
        return type.getMethods().stream().map(Type.Method::getName).collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    protected static void assertIndexed(Map<String, byte[]> classFiles) {
        Container.Entry directory = InMemoryClassFiles.toDirectoryEntry(classFiles);
        MemoryIndexes indexes = new MemoryIndexes();
        ClassFileIndexerProvider provider = new ClassFileIndexerProvider();

        for (Container.Entry entry : directory.getChildren()) {
            provider.index(null, entry, indexes);
        }

        Assert.assertTrue(indexes.keys("typeDeclarations").containsAll(Arrays.asList("test/Shape", "test/Circle", "test/Square", "test/Square$Kind")));
        Assert.assertTrue(indexes.keys("constructorDeclarations").containsAll(Arrays.asList("test/Circle", "test/Square")));
        Assert.assertTrue(indexes.keys("methodDeclarations").containsAll(Arrays.asList("area", "radius", "describe")));
        Assert.assertTrue(indexes.keys("fieldDeclarations").containsAll(Arrays.asList("radius", "side")));
        Assert.assertTrue(indexes.keys("typeReferences").containsAll(Arrays.asList("test/Circle", "java/util/function/Supplier")));
        Assert.assertTrue(indexes.keys("methodReferences").containsAll(Arrays.asList("area", "radius", "get", "getSimpleName")));
        Assert.assertTrue(indexes.keys("strings").containsAll(Arrays.asList("Shape description\n", "square")));

        Map<String, Collection> subTypeNames = indexes.getIndex("subTypeNames");
        Assert.assertTrue(subTypeNames.get("test/Shape").containsAll(Arrays.asList("test/Circle", "test/Square")));
        Assert.assertTrue(subTypeNames.get("java/lang/Record").contains("test/Circle"));
    }

    @SuppressWarnings("unchecked")
    protected static class MemoryIndexes implements Indexes {
        protected final Map<String, Map<String, Collection>> indexes = new HashMap<>();

        @Override
        public Map<String, Collection> getIndex(String name) {
            return indexes.computeIfAbsent(name, k -> new HashMap<String, Collection>() {
                @Override public Collection get(Object key) { return computeIfAbsent((String)key, x -> new ArrayList()); }
            });
        }

        public Set<String> keys(String name) { return getIndex(name).keySet(); }
    }
}
