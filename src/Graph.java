import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class Graph {
    private Map<Integer, List<Edge>> adjacencies;

    public class Edge {
        public int target;
        public int weight;
        public Edge(int target, int weight) {
            this.target = target;
            this.weight = weight;
        }
    }

    public Graph(int boardSize) {
        adjacencies = new HashMap<>();
        for (int i = 0; i < boardSize; i++) {
            adjacencies.put(i, new LinkedList<>());
        }
    }

    // METHOD BARU: Build graph yang aware terhadap tangga
    public void buildGraphWithLinks(int boardSize, Map<Integer, Integer> links) {
        for (int i = 0; i < boardSize - 1; i++) {
            // Setiap node punya 6 kemungkinan langkah dadu (1-6)
            for (int dice = 1; dice <= 6; dice++) {
                int target = i + dice;

                if (target < boardSize) {
                    int finalTarget = target;

                    // Jika target adalah kaki tangga, langsung lompat ke ujung tangga
                    if (links.containsKey(target)) {
                        finalTarget = links.get(target);
                    }

                    // Tambahkan edge dengan bobot 1 (1 langkah dadu)
                    adjacencies.get(i).add(new Edge(finalTarget, 1));
                }
            }
        }
    }

    public Map<Integer, List<Edge>> getAdjacencies() {
        return adjacencies;
    }
}