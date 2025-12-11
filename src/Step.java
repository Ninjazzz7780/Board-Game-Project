class Step {
    private String direction;
    private int steps;
    private int position;

    public Step(String direction, int steps, int position) {
        this.direction = direction;
        this.steps = steps;
        this.position = position;
    }

    public String getDirection() {
        return direction;
    }

    public int getSteps() {
        return steps;
    }

    public int getPosition() {
        return position;
    }
}