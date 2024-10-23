package com.mechalikh.pureedgesim.taskorchestrator.DQN;
import com.mechalikh.pureedgesim.taskgenerator.Task;

public abstract class DQNAgentAbstract {
    
    public void DQN(Task task, boolean isDone){}

    public int chooseAction(double[] state, String[] architecture, Task task) {return 0;}

    public int getKthBestQAction(double[] state, int k){return 0;}

    public void saveAll(){}

    public int getTotalReward(){return 0;}

    public void IterationEnd() {};

    public double grantReward(Task task){return 0;}

}
