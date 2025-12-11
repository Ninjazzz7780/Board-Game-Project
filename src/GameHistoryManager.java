import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class GameHistoryManager {
    private final String FILE_NAME = "leaderboard.txt";
    private Map<String, Integer> winData;

    public GameHistoryManager() {
        winData = new HashMap<>();
        loadData();
    }

    // Menambah kemenangan untuk player tertentu
    public void addWin(String playerName) {
        winData.put(playerName, winData.getOrDefault(playerName, 0) + 1);
        saveData();
    }

    // Mengambil data yang sudah diurutkan (Terbanyak menang di atas)
    public List<Map.Entry<String, Integer>> getSortedHistory() {
        return winData.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .collect(Collectors.toList());
    }

    // Membaca file txt
    private void loadData() {
        File file = new File(FILE_NAME);
        if (!file.exists()) return;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(":");
                if (parts.length == 2) {
                    winData.put(parts[0], Integer.parseInt(parts[1]));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Menyimpan ke file txt
    private void saveData() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(FILE_NAME))) {
            for (Map.Entry<String, Integer> entry : winData.entrySet()) {
                writer.write(entry.getKey() + ":" + entry.getValue());
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}