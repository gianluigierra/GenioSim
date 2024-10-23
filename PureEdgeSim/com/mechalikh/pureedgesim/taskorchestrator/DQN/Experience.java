package com.mechalikh.pureedgesim.taskorchestrator.DQN;

import java.io.Serializable;

public class Experience implements Serializable{
    private static final long serialVersionUID = 1L; // Consigliato per la serializzazione

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