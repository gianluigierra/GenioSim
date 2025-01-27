package com.mechalikh.pureedgesim.taskorchestrator.DQN;

import java.util.ArrayList;
import java.util.Arrays;
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
    protected int stateSize = 5;

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

        //inizializzo il vettore di stato
        stateVector = new double[simOrchestrator.getNodeList().size()*stateSize + 3];
        for(int i = 0; i < this.simOrchestrator.getNodeList().size(); i++){
            ComputingNode node = this.simOrchestrator.getNodeList().get(i);
    
            stateVector[i*stateSize] = node.getNumberOfCPUCores();                  // Ottieni il numero di cores
            stateVector[i*stateSize+1] = node.getMipsPerCore();                     // Ottieni il numero di task orchestrati
            stateVector[i*stateSize+2] = node.getAvailableRam();                    // Ottieni la ram del nodo
            stateVector[i*stateSize+3] = node.getAvailableStorage();                // Ottieni lo storage del nodo
            stateVector[i*stateSize+4] = 0;                                         // 0 container piazzati sul nodo
        }
    }

    public int DoDQN(String[] architecture, Container container) {
        // aggiungi il task alla lista dei task orchestrati
        containerList.add(container);

        // Ottiene lo stato attuale basato sui nodi disponibili
        double[] state = Arrays.copyOf(stateVector, stateVector.length);
    
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

        // Ottiene lo stato attuale basato sui nodi disponibili dopo l'azione eseguita
        double[] nextState = Arrays.copyOf(stateVector, stateVector.length);

        //effettuo DQN ed aggiorno l'Agent
        m_agent.DQN(container, state, nextState, action, IsTrainingOn);

        return action;
    }
    
    private void getNextState(int action, Container container) {

        double ram = stateVector[action * stateSize + 2] - container.getContainerSizeInMBytes();                        //modifico la ram disponibile del nodo
        double storage = stateVector[action * stateSize + 3] - container.getContainerSizeInMBytes();                    //modifico lo storage disponibile sul nodo
        double containersPlaced = stateVector[action * stateSize + 4]  + 1;                                             //modifico il numero di container piazzati sul nodo
    
        stateVector[action * stateSize + 2] = ram;                             // Metti il numero di sentTasks
        stateVector[action * stateSize + 3] = storage;                         // Metti la ram 
        stateVector[action * stateSize + 4] = containersPlaced;                // Metti i container piazzati
    
        // Aggiungi i valori globali
        double containerSize = container.getContainerSizeInBits();                                                                     // size del task in bytes
        double maxLatency = SimulationParameters.applicationList.get(container.getApplicationID()).getLatency();                       //latency max del task
        int appID = container.getApplicationID();                                                                                      //id dell'applicazione
    
        int globalStartIndex = this.simOrchestrator.getNodeList().size() * stateSize;                     // Indice di partenza per i valori globali
        stateVector[globalStartIndex] = maxLatency;                                                       // Aggiungi maxlatency del task
        stateVector[globalStartIndex + 1] = containerSize;                                                // Aggiungi tasksize
        stateVector[globalStartIndex + 2] = appID;                                                        // Aggiungi IDApp
    }

    private void PerformAction(int action, Container container) {
        // Implementare la logica per eseguire l'azione (orchestrazione del task)
        ComputingNode node = simOrchestrator.getNodeList().get(action);

        // Send this container to this computing node
		container.setPlacementDestination(node);

        //simula nello stateVector l'esito del placement
        getNextState(action, container);

		// Application has been deployed
		simulationManager.getSimulationLogger().deepLog(simulationManager.getSimulation().clock() + ": " + this.getClass() + " Task: " + container.getId() + " assigned to " + node.getType() + " Computing Node: " + node.getId());
    }

    public void notifyOrchestratorOfContainerExecution(Container container) {
	}
    
    public void removeContainerFromHistoryMap(Container container){
        //rimuove il container dalla historymap
        containerStateHistoryMap.remove(container);
        //aggiunge allo state vector i parametri del container
        int action = -1;
        for(int i = 0; i < simOrchestrator.getNodeList().size(); i++){
            for(Container cont : simOrchestrator.getNodeList().get(i).getContainerList())
                if(cont.equals(container))
                    action = i;
        }
        double ram = stateVector[action * stateSize + 2] + container.getContainerSizeInMBytes();                        //Ottieni la ram del nodo
        double storage = stateVector[action * stateSize + 3] + container.getContainerSizeInMBytes();                    //Ottieni lo storage del nodo
        double containersPlaced = stateVector[action * stateSize + 3] - 1;                                              //Ottieni container piazzati
    
        stateVector[action * stateSize + 2] = ram;                             // Metti il numero di sentTasks
        stateVector[action * stateSize + 3] = storage;                         // Metti la ram 
        stateVector[action * stateSize + 4] = containersPlaced;                // Metti container piazazti
    }

    public void simulationEnd(){
        if(printTaskNodes) m_agent.IterationEnd();
        if(IsTrainingOn) m_agent.saveAll();
        System.out.println("TotalReward: " + m_agent.getTotalReward());
    }

}
