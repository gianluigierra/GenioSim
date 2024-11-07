package com.mechalikh.pureedgesim.taskorchestrator.DQN;

import java.util.List;
import java.util.ArrayList;

import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.simulationengine.OnSimulationEndListener;
import com.mechalikh.pureedgesim.simulationmanager.SimulationManager;
import com.mechalikh.pureedgesim.taskgenerator.Task;
import com.mechalikh.pureedgesim.taskorchestrator.DefaultOrchestrator;

public class CustomOrchestrator extends DefaultOrchestrator implements OnSimulationEndListener{

    // Parametri DQN
    private DQNAgentAbstract m_agent = null;

    //variabili per esecuzione
    public boolean IsTrainingOn;
    private List<Task> tasksList = new ArrayList<>();

    //questi servono solo per printare debug
    private boolean printNodeDestination = false;
    private boolean printTaskNodes = true;
    public static int totalreward;
    public static double failureRate;
    public static String algName;


    public CustomOrchestrator(SimulationManager simulationManager) {
        super(simulationManager);
        this.IsTrainingOn = SimulationParameters.neuralTraining;
        algName = algorithmName;
        
        if (algorithmName.contains("DQN")){
            String modelPath = SimulationParameters.settingspath + SimulationParameters.simName + "/" + simulationManager.getScenario().getStringOrchArchitecture();
            if(algorithmName.equals("DQN1")) m_agent = new DQNAgent1(modelPath + "/" + algorithmName + "_model_" +  SimulationParameters.simName, this, simulationManager);
            else if(algorithmName.equals("DQN2")) m_agent = new DQNAgent2(modelPath + "/" + algorithmName + "_model_" +  SimulationParameters.simName, this, simulationManager);
            else if(algorithmName.equals("DQN3")) m_agent = new DQNAgent3(modelPath + "/" + algorithmName + "_model_" +  SimulationParameters.simName, this, simulationManager);
            else if(algorithmName.equals("DQN1_2")) m_agent = new DQNAgent1_2(modelPath + "/" + algorithmName + "_model_" +  SimulationParameters.simName, this, simulationManager); 
        }
        CustomOrchestrator.totalreward = 0;
    }

    @Override
    protected int findComputingNode(String[] architecture, Task task) {

        //aggiungi il task alla lista di quelli orchestrati dall'orchestratore
        tasksList.add(task);

        if ("ROUND_ROBIN".equals(algorithmName)) {
            return super.roundRobin(architecture, task);
		} else if ("GREEDY".equals(algorithmName)) {
            return super.greedyChoice(architecture, task);
		}  else if ("TRADE_OFF".equals(algorithmName)) {
            return super.tradeOff(architecture, task);
		} else {
			throw new IllegalArgumentException(getClass().getSimpleName() + " - Unknown orchestration algorithm '"
					+ algorithmName + "', please check the simulation parameters file...");
		}
    }

    @Override
    // Find an offloading location for this task
	public void orchestrate(Task task) {
        if ("ROUND_ROBIN".equals(algorithmName) || "TRADE_OFF".equals(algorithmName) || "GREEDY".equals(algorithmName)) {
			assignTaskToComputingNode(task, architectureLayers);
		} else if (algorithmName.contains("DQN")) {
			DoDQN(architectureLayers, task);
		} else {
			throw new IllegalArgumentException(getClass().getSimpleName() + " - Unknown orchestration algorithm '"
					+ algorithmName + "', please check the simulation parameters file...");
		}
	}

    @Override
    protected void assignTaskToComputingNode(Task task, String[] architectureLayers) {

		int nodeIndex = findComputingNode(architectureLayers, task);
        if(printNodeDestination) System.out.println("Nodo: " + nodeList.get(nodeIndex).getName());

		if (nodeIndex != -1) {
            PerformAction(nodeIndex, task);
		}
	}

    private double[] getCurrentState1(List<ComputingNode> nodeList, Task task) {
        double[] state = new double[nodeList.size() * 6 + 2]; // 6 valori per nodo (CPU disponibili, FailureRate, mipspercore, CPU, RAM e Storage) + 2 variabili
    
        // Aggiungi i valori per ogni nodo nella nodeList
        for (int i = 0; i < nodeList.size(); i++) {
            ComputingNode node = nodeList.get(i);
    
            double availableCores = node.getAvailableCores();           // Ottieni il carico CPU del nodo
            double failureRate = node.getFailureRate();                 // Ottieni il failure rate del nodo
            double mipsPerCore = node.getMipsPerCore();                 // Ottieni il numero di task orchestrati
            double avgCpuUtilization = node.getAvgCpuUtilization();     // Ottieni l'utilizzo medio di CPU
            double ram = node.getAvailableRam();                        //Ottieni la ram del nodo
            double storage = node.getAvailableStorage();                //Ottieni lo storage del nodo
    
            state[i * 6] = availableCores;                              // Metti il valore di CPU load
            state[i * 6 + 1] = mipsPerCore;                             // Metti il numero di sentTasks
            state[i * 6 + 2] = ram;                                     // Metti la ram 
            state[i * 6 + 3] = storage;                                 // Metti lo storage
            state[i * 6 + 4] = avgCpuUtilization;                       // Metti il consumio medio di CPU
            state[i * 6 + 5] = failureRate;                             // Metti il failure rate medio 
        }
    
        // Aggiungi i valori globali
        double taskSize = task.getFileSizeInBits();                     // size del task in bytes
        double maxLatency = task.getMaxLatency();                       //latency max del task
    
        int globalStartIndex = nodeList.size() * 6;                     // Indice di partenza per i valori globali
        state[globalStartIndex] = maxLatency;                           // Aggiungi maxlatency del task
        state[globalStartIndex + 1] = taskSize;                         // Aggiungi tasksize
    
        return state;
    }

    private double[] getCurrentState2(List<ComputingNode> nodeList, Task task) {
        double[] state = new double[nodeList.size() * 5 + 2]; // 5 valori per nodo (CPU, FailureRate, sentTasks, RAM e Storage) + 2 variabili
    
        // Aggiungi i valori per ogni nodo nella nodeList
        for (int i = 0; i < nodeList.size(); i++) {
            ComputingNode node = nodeList.get(i);
    
            double cpuLoad = node.getAvgCpuUtilization();       // Ottieni il carico CPU del nodo
            double failureRate = node.getFailureRate(); // Ottieni il failure rate del nodo
            double sentTasks = super.historyMap.get(i);   // Ottieni il numero di task orchestrati
            double ram = node.getAvailableRam();  //Ottieni la ram del nodo
            double storage = node.getAvailableStorage();  //Ottieni lo storage del nodo
    
            state[i * 5] = cpuLoad;          // Metti il valore di CPU load
            state[i * 5 + 1] = failureRate;  // Metti il valore di failureRate
            state[i * 5 + 2] = sentTasks;    // Metti il numero di sentTasks
            state[i * 5 + 3] = ram;         // Metti la ram 
            state[i * 5 + 4] = storage;    // Metti lo storage
        }
    
        // Aggiungi i valori globali
        double taskSize = task.getFileSizeInBits();                     // size del task in bytes
        double maxLatency = task.getMaxLatency();                       //latency max del task
    
        int globalStartIndex = nodeList.size() * 5;  // Indice di partenza per i valori globali
        state[globalStartIndex] = maxLatency;         // Aggiungi maxlatency del task
        state[globalStartIndex + 1] = taskSize;       // Aggiungi tasksize
    
        return state;
    }

    private void PerformAction(int action, Task task) {
        // Implementare la logica per eseguire l'azione (orchestrazione del task)
        ComputingNode node = nodeList.get(action);
        try {
            checkComputingNode(node);
        } catch (Exception e) {
            e.printStackTrace();
        }
        // Send this task to this computing node
		task.setOffloadingDestination(node);

		// Application has been deployed
		simLog.deepLog(simulationManager.getSimulation().clock() + ": " + this.getClass() + " Task: " + task.getId() + " assigned to " + node.getType() + " Computing Node: " + node.getId());
    }

    protected void DoDQN(String[] architecture, Task task) {
        // aggiungi il task alla lista dei task orchestrati
        tasksList.add(task);

        // Ottiene lo stato attuale basato sui nodi disponibili
        double[] state;
        if(algorithmName.equals("DQN1") || algorithmName.equals("DQN1_2")) state = getCurrentState1(nodeList, task);
        else state = getCurrentState2(nodeList, task);
    
        // Ciclo su tutte le possibili destinazioni per scegliere la migliore azione
        int action;
        if(IsTrainingOn) action = m_agent.chooseAction(state, architecture, task);
        //else if(tasksList.size() < nodeList.size() * 20) action = super.tradeOff(architecture, task);
        else action = m_agent.getKthBestQAction(state, 0);
        super.historyMap.put(action, super.historyMap.get(action) + 1);

        //setto stato e azione per il task
        task.setCurrentState(state);    
        task.setAction(action);
    
        // Esegui l'azione e avanza verso lo stato successivo
        PerformAction(action, task);

        //dal momento che il secondo algoritmo DQN non si basa sull'aspettare il termine del task devo avviarne l'esecuzione qua
        //if((algorithmName.equals("DQN2") || algorithmName.equals("DQN3")) && IsTrainingOn && this.tasksList.contains(task)) {                       //devo commentarla perchè non riesco ad addestrare a dovere l'algoritmo
        if((algorithmName.equals("DQN2") || algorithmName.equals("DQN3")) && this.tasksList.contains(task)) {
            task.setNextState(getCurrentState2(nodeList, task));
            m_agent.DQN(task, false);
        }
        //else if((algorithmName.equals("DQN2") || algorithmName.equals("DQN3")) && !IsTrainingOn && this.tasksList.contains(task)) m_agent.grantReward(task); //devo commentarla perchè non riesco ad addestrare a dovere l'algoritmo
    }

    //il task viene eseguito e si preleva il nextState
	@Override
	public void notifyOrchestratorOfTaskExecution(Task task) {
        if(this.tasksList.contains(task) && algorithmName.contains("DQN")) 
            if(algorithmName.equals("DQN1") || algorithmName.equals("DQN1_2"))task.setNextState(getCurrentState1(nodeList, task));
    }
    
    //il task ha finito e se previsto, si effettua l'update dell'Agent
	@Override
	public void resultsReturned(Task task) {
        //if(algorithmName.equals("DQN1") && IsTrainingOn && this.tasksList.contains(task)) m_agent.DQN(task, true);    //devo commentarla perchè non riesco ad addestrare a dovere l'algoritmo
        //else if(algorithmName.equals("DQN1") && !IsTrainingOn && this.tasksList.contains(task)) m_agent.grantReward(task);   //devo commentarla perchè non riesco ad addestrare a dovere l'algoritmo
        if((algorithmName.equals("DQN1")  || algorithmName.equals("DQN1_2"))&& this.tasksList.contains(task)) m_agent.DQN(task, true);
	}

    @Override
    public void onSimulationEnd() {

        System.out.println("Tasklist size = " + tasksList.size());
        List<Task> tasksListriusciti = new ArrayList<>();
        List<Task> tasksListfalliti = new ArrayList<>();
        for(Task task : tasksList){
            if(task.getStatus().equals(com.mechalikh.pureedgesim.taskgenerator.Task.Status.SUCCESS)) tasksListriusciti.add(task);
            else tasksListfalliti.add(task);
        }
        System.out.println("Tasklist riusciti size = " + tasksListriusciti.size());
        System.out.println("Tasklist falliti size = " + tasksListfalliti.size());

        if(algorithmName.contains("DQN")){
            if(printTaskNodes) m_agent.IterationEnd();
            if(IsTrainingOn) m_agent.saveAll();
            CustomOrchestrator.totalreward = m_agent.getTotalReward();
            System.out.println("TotalReward: " + m_agent.getTotalReward());
        }
        failureRate = 100 - simulationManager.getFailureRate();
    }

    

}