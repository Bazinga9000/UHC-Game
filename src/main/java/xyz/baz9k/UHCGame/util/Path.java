package xyz.baz9k.UHCGame.util;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.stream.Stream;

/**
 * Utility class for dealing with chained strings.
 * Example: "a.b.c.d.e.f.g.h"
 * <p> Nulls are ignored and filtered out of arguments.
 */
public final class Path implements Iterable<String> {
    private final String[] path;

    /**
     * @param splitString whether or not to break up path strings into node components
     * @param path the path
     */
    private Path(boolean splitString, String... path) {
        Objects.requireNonNull(path);
        if (splitString) {
            this.path = stream(path)
                .toArray(String[]::new);
        } else {
            this.path = path;
        }

    }

    public Path(String... path) {
        this(true, path);
    }

    public String toString() {
        return String.join(".", path);
    }

    public static Path join(Path... paths) {
        Objects.requireNonNull(paths);
        return new Path(false,
            Arrays.stream(paths)
                .filter(Objects::nonNull)
                .flatMap(p -> Arrays.stream(p.path))
                .toArray(String[]::new)
        );
    }

    public static String join(String... paths) {
        Objects.requireNonNull(paths);
        return new Path(paths).toString();
    }

    public Path append(String... nodes) {
        Objects.requireNonNull(nodes);
        Path other = new Path(nodes);
        return this.append(other);
    }

    public Path append(Path... paths) {
        Objects.requireNonNull(paths);

        Path[] allPaths = new Path[paths.length + 1];
        allPaths[0] = this;
        System.arraycopy(paths, 0, allPaths, 1, paths.length);

        return Path.join(allPaths);
    }

    public boolean isRoot() {
        return path.length == 0;
    }

    /**
     * Traverses a tree with named nodes.
     * @param <N> Class of every node
     * @param <B> Class of every branch node
     * @param root The top node to start traversing from
     * @param getChild Method to get from a branch node to a child
     * @return the descendant at the path, if it exists
     */
    @SuppressWarnings("unchecked")
    public <N, B extends N> Optional<N> traverse(B root, BiFunction<B, String, Optional<N>> getChild) {
        if (isRoot()) return Optional.of(root);

        Class<B> branchClass = (Class<B>) root.getClass();
        B node = root;
        
        // grab child via getChild method.
        // if it's not a branch type, then it can't have another child, so return empty
        for (int i = 0; i < path.length - 1; i++) {
            String childName = path[i];
            Optional<N> child = getChild.apply(node, childName);
            
            if (child.isPresent()) {
                N c = child.get();

                if (branchClass.isInstance(c)) {
                    node = branchClass.cast(c);
                    continue;
                }
            }

            return Optional.empty();
        }

        // we found the last parent, so get the last parent's child
        String childName = path[path.length - 1];
        return getChild.apply(node, childName);
    }

    /**
     * Traverses a nested map.
     * <p><code> 
     *     new Path("a.b.c.d").traverse(map);
     * </code>
     * is equivalent to
     * <code>
     *     ((Map&lt;?,?&gt;) ((Map&lt;?,?&gt;) ((Map&lt;?,?&gt;) map.get("a")).get("b")).get("c")).get("d")
     * </code>
     * @param root The top map to start traversing from
     * @return the descendant at the path, if it exists
     */
    public Optional<Object> traverse(Map<?, ?> root) {
        return traverse(root, (m, s) -> Optional.ofNullable(m.get(s)));
    }

    // pregenerated
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode(path);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Path other = (Path) obj;
        if (!Arrays.equals(path, other.path))
            return false;
        return true;
    }
    //
    
    private static Stream<String> stream(String[] path) {
        return Arrays.stream(path)
            .filter(Objects::nonNull)
            .flatMap(n -> Arrays.stream(n.split("\\.")));
    }
    
    public Stream<String> stream() {
        return stream(path);
    }

    @Override
    public Iterator<String> iterator() {
        return stream().iterator();
    }
}
