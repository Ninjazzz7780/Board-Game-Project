// File: PlayerQueue.java
import java.util.LinkedList;
import java.util.Queue;

class PlayerQueue {
    private LinkedList<Player> queue;

    public PlayerQueue() {
        this.queue = new LinkedList<>();
    }

    public void enqueue(Player player) {
        queue.offer(player);
    }

    public Player poll() {
        return queue.poll();
    }

    public Player peek() {
        return queue.peek();
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }

    public int size() {
        return queue.size();
    }

    public LinkedList<Player> getQueue() {
        return queue;
    }
}