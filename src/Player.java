import java.util.Stack;

public class Player {
    private String name;
    private int position;
    private int point; // Tambahan: Sistem Poin
    private Stack<Step> stepStack;
    private String color;

    public Player(String name, String color) {
        this.name = name;
        this.position = 0;
        this.point = 0; // Inisialisasi poin
        this.stepStack = new Stack<>();
        this.color = color;
    }

    public void addStep(Step step) {
        stepStack.push(step);
    }

    public void setPosition(int newPosition) {
        this.position = Math.min(Math.max(newPosition, 0), 63);
    }

    // Tambahan: Metode untuk poin
    public void addPoint(int amount) {
        this.point += amount;
    }

    public int getPoint() {
        return point;
    }
    // ----------------------------

    public Step getLastStep() {
        return stepStack.isEmpty() ? null : stepStack.peek();
    }

    public void moveForward(int steps) {
        position = Math.min(position + steps, 63);
        addStep(new Step("forward", steps, position));
    }

    public void moveBackward(int steps) {
        position = Math.max(position - steps, 0);
        addStep(new Step("backward", steps, position));
    }

    public String getName() {
        return name;
    }

    public int getPosition() {
        return position;
    }

    public String getColor() {
        return color;
    }

    public Stack<Step> getStepStack() {
        return stepStack;
    }
}