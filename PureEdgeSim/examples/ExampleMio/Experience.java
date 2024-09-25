package examples.ExampleMio;

public class Experience {
    public double[] state;
    public int action;
    public double reward;
    public double[] nextState;
    public boolean done;

    public Experience(double[] state, int action, double reward, double[] nextState, boolean done) {
        this.state = state;
        this.action = action;
        this.reward = reward;
        this.nextState = nextState;
        this.done = done;
    }
}