package top.swkfk.compiler.utils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 无向图的工具栏，并提供了一些基本的操作
 * @param <T> 顶点存储数据的类型
 */
final public class UndirectedGraph<T> {
    private final Set<T> vertices;
    private final Map<T, Set<T>> edges;

    public UndirectedGraph() {
        this.vertices = new HashSet<>();
        this.edges = new HashMap<>();
    }

    public void addVertex(T vertex) {
        if (!vertices.contains(vertex)) {
            vertices.add(vertex);
            edges.put(vertex, new HashSet<>());
        }
    }

    public void addEdge(T from, T to) {
        edges.get(from).add(to);
        edges.get(to).add(from);
    }

    public Set<T> getVertices() {
        return vertices;
    }

    public Set<T> getEdges(T vertex) {
        return edges.get(vertex);
    }

    public void removeVertex(T vertex) {
        vertices.remove(vertex);
        edges.remove(vertex);
        for (T v : vertices) {
            edges.get(v).remove(vertex);
        }
    }

    public UndirectedGraph<T> copy() {
        UndirectedGraph<T> copy = new UndirectedGraph<>();
        for (T vertex : vertices) {
            copy.addVertex(vertex);
        }
        for (T vertex : vertices) {
            for (T edge : edges.get(vertex)) {
                copy.addEdge(vertex, edge);
            }
        }
        return copy;
    }

    @Override
    public String toString() {
        return "UndirectedGraph{" +
            "vertices=" + vertices +
            ", edges=" + edges +
            '}';
    }
}
