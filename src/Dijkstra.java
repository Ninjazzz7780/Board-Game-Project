import java.util.*;

public class Dijkstra {
    public static List<Integer> findShortestPath(Graph graph, int start, int end) {
        Map<Integer, Integer> predecessors = new HashMap<>();
        Map<Integer, Integer> distances = new HashMap<>();
        PriorityQueue<Integer> pq = new PriorityQueue<>(Comparator.comparingInt(distances::get));

        for (int node : graph.getAdjacencies().keySet()) {
            distances.put(node, Integer.MAX_VALUE);
        }
        distances.put(start, 0);
        pq.add(start);

        while (!pq.isEmpty()) {
            int current = pq.poll();
            if (current == end) break;

            if (graph.getAdjacencies().get(current) != null) {
                for (Graph.Edge edge : graph.getAdjacencies().get(current)) {
                    int newDistance = distances.get(current) + edge.weight;
                    if (newDistance < distances.get(edge.target)) {
                        distances.put(edge.target, newDistance);
                        predecessors.put(edge.target, current);
                        pq.remove(edge.target);
                        pq.add(edge.target);
                    }
                }
            }
        }

        List<Integer> path = new LinkedList<>();
        Integer step = end;

        // Cek jika path tidak ditemukan (step tidak ada di predecessors dan bukan start)
        if (!predecessors.containsKey(step) && step != start) return Collections.emptyList();

        while (step != null) {
            path.add(0, step);
            step = predecessors.get(step);
        }

        if (path.isEmpty() || path.get(0) != start) return Collections.emptyList();

        return path;
    }
}