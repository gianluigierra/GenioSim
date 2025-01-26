package com.mechalikh.pureedgesim.taskorchestrator.DQN;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.simulationmanager.SimulationManager;
import com.mechalikh.pureedgesim.taskgenerator.Container;
import com.mechalikh.pureedgesim.taskorchestrator.DefaultContainerOrchestrator;

public class DQNhelper {

    // Parametri DQN
    private DQNAgentAbstract m_agent = null;
    protected double [] stateVector;
    protected int stateSize = 1;

    //variabili per esecuzione
    public boolean IsTrainingOn;
    private List<Container> containerList = new ArrayList<>();
    private Map<Container, double[]> containerStateHistoryMap = new LinkedHashMap<>();

    //questi servono solo per printare debug
    private boolean printTaskNodes = true;
    public static int totalreward;
    public static double failureRate;
    public static String algorithmName;

    //moduli del simulatore
    protected DefaultContainerOrchestrator simOrchestrator = null;
    protected SimulationManager simulationManager;

    public DQNhelper(DefaultContainerOrchestrator simOrchestrator, SimulationManager simulationManager){
        this.simulationManager = simulationManager;
        this.simOrchestrator = simOrchestrator;
        this.IsTrainingOn = SimulationParameters.neuralTraining;
        algorithmName = SimulationParameters.taskOrchestrationAlgorithm;
        String modelPath = SimulationParameters.settingspath + SimulationParameters.simName + "/";
        m_agent = new DQNAgent(this, modelPath);
        stateVector = new double[simOrchestrator.getNodeList().size()*stateSize];
    }


    public int DoDQN(String[] architecture, Container container) {
        // aggiungi il task alla lista dei task orchestrati
        containerList.add(container);

        // Ottiene lo stato attuale basato sui nodi disponibili
        double[] state;
        state = getCurrentState(simOrchestrator.getNodeList(), container);
    
        // Ciclo su tutte le possibili destinazioni per scegliere la migliore azione
        int action;
        if(IsTrainingOn) 
            action = m_agent.chooseAction(state, architecture, container);
        else 
            action = m_agent.getKthBestQAction(state, 0);

        simOrchestrator.historyMap.put(action, simOrchestrator.historyMap.get(action) + 1);

        //setto stato, l'azione corrisponde all'indice del nodo sul quale è stato posizionato il container.
        containerStateHistoryMap.put(container, state);
    
        // Esegui l'azione e avanza verso lo stato successivo
        PerformAction(action, container);

        return action;
    
    }
    
    private double[] getCurrentState(List<ComputingNode> nodeList, Container container) {
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
        double containerSize = container.getContainerSizeInBits();                                                                                     // size del task in bytes
        double maxLatency = SimulationParameters.applicationList.get(container.getApplicationID()).getLatency();                       //latency max del task
    
        int globalStartIndex = nodeList.size() * 6;                     // Indice di partenza per i valori globali
        state[globalStartIndex] = maxLatency;                           // Aggiungi maxlatency del task
        state[globalStartIndex + 1] = containerSize;                    // Aggiungi tasksize
    
        return state;
    }

    private void PerformAction(int action, Container container) {
        // Implementare la logica per eseguire l'azione (orchestrazione del task)
        ComputingNode node = simOrchestrator.getNodeList().get(action);

        // Send this container to this computing node
		container.setPlacementDestination(node);

		// Application has been deployed
		simulationManager.getSimulationLogger().deepLog(simulationManager.getSimulation().clock() + ": " + this.getClass() + " Task: " + container.getId() + " assigned to " + node.getType() + " Computing Node: " + node.getId());
    }

    public void notifyOrchestratorOfContainerExecution(Container container) {
        double[] nextstate = getCurrentState(simOrchestrator.getNodeList(), container);
        int action = -1;
        for(int i = 0; i < simOrchestrator.getNodeList().size(); i++){
            for(Container cont : simOrchestrator.getNodeList().get(i).getContainerList())
                if(cont.equals(container))
                    action = i;
        }
        m_agent.DQN(container, containerStateHistoryMap.get(container), nextstate, action, true);
	}
    
    public void removeContainerFromHistoryMap(Container container){
        containerStateHistoryMap.remove(container);
    }

    public void simulationEnd(){
        if(printTaskNodes) m_agent.IterationEnd();
        if(IsTrainingOn) m_agent.saveAll();
        System.out.println("TotalReward: " + m_agent.getTotalReward());
    }

}
