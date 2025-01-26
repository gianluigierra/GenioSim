package com.mechalikh.pureedgesim.taskorchestrator.DQN;
import com.mechalikh.pureedgesim.taskgenerator.Container;

public abstract class DQNAgentAbstract {
    
    public void DQN(Container container, double[] state, double[] nextState, int action, boolean isDone){}

    public int chooseAction(double[] state, String[] architecture, Container container) {return 0;}

    public int getKthBestQAction(double[] state, int k){return 0;}

    public void saveAll(){}

    public int getTotalReward(){return 0;}

    public void IterationEnd() {};

    public double grantReward(Container container){return 0;}

}
