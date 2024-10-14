package com.mechalikh.pureedgesim.taskorchestrator;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import javax.swing.JOptionPane;

import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.simulationengine.Event;
import com.mechalikh.pureedgesim.simulationengine.OnSimulationEndListener;
import com.mechalikh.pureedgesim.simulationmanager.SimulationManager;
import com.mechalikh.pureedgesim.taskgenerator.Task;

import examples.ProgettoGenio.ProgettoGenio;

public class CustomOrchestrator extends DefaultOrchestrator implements OnSimulationEndListener{

    // Parametri DQN
    private DDQNAgent m_agent = null;

    //variabili per esecuzione
    private boolean IsTrainingOn = true;
    private List<Task> tasksList = new ArrayList<>();

    //questi servono solo per printare debug
    private boolean printNodeDestination = true;


    public CustomOrchestrator(SimulationManager simulationManager) {
        super(simulationManager);

        // int response = JOptionPane.showConfirmDialog(null, "Vuoi Caricare il modello?", "Carica modello", JOptionPane.YES_NO_OPTION);

        // if (response == JOptionPane.YES_OPTION) IsTrainingOn = false;
        // else IsTrainingOn = true;

        if ("DQN".equals(algorithmName) && IsTrainingOn) {
            m_agent = new DDQNAgent(this, simulationManager);
        }
        else if ("DQN".equals(algorithmName) && !IsTrainingOn){
            String modelPath = SimulationParameters.settingspath + SimulationParameters.simName + "/" + simulationManager.getScenario().getStringOrchArchitecture();
            m_agent = new DDQNAgent(modelPath + "/dqn_model_" +  SimulationParameters.simName, modelPath, this, simulationManager);

            System.out.println("Pesi della rete Q: " + m_agent.qNetwork.params());
            System.out.println("Pesi della rete Target: " +  m_agent.targetNetwork.params());

        }

    }

    @Override
    protected int findComputingNode(String[] architecture, Task task) {
        if ("ROUND_ROBIN".equals(algorithmName)) {
            return super.roundRobin(architecture, task);
		} else if ("GREEDY".equals(algorithmName)) {
            return greedyChoice(architecture, task);
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
		} else if ("DQN".equals(algorithmName)) {
			DoDDQN(architectureLayers, task);
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

    private double[] getCurrentState(List<ComputingNode> nodeList, Task task) {
        // Il nuovo stato conterrà 5 valori per ogni nodo e 2 variabili
        double[] state = new double[nodeList.size() * 4 + 2]; // 5 valori per nodo (CPU, FailureRate, sentTasks, RAM e Storage) + 4 variabili
    
        // Aggiungi i valori per ogni nodo nella nodeList
        for (int i = 0; i < nodeList.size(); i++) {
            ComputingNode node = nodeList.get(i);
    
            double availableCores = node.getAvailableCores();       // Ottieni il carico CPU del nodo
            //double failureRate = node.getFailureRate(); // Ottieni il failure rate del nodo
            double mipsPerCore = node.getMipsPerCore();   // Ottieni il numero di task orchestrati
            double ram = node.getAvailableRam();  //Ottieni la ram del nodo
            double storage = node.getAvailableStorage();  //Ottieni lo storage del nodo
    
            state[i * 4] = availableCores;          // Metti il valore di CPU load
            state[i * 4 + 1] = mipsPerCore;    // Metti il numero di sentTasks
            state[i * 4 + 2] = ram;          // Metti la ram 
            state[i * 4 + 3] = storage;      // Metti lo storage
            //state[i * 4 + 1] = failureRate;  // Metti il valore di failureRate
        }
    
        // Aggiungi i valori globali
        double taskSize = task.getFileSizeInBits();                     // size del task in bytes
        double maxLatency = task.getMaxLatency();                       //latency max del task
    
        int globalStartIndex = nodeList.size() * 4;  // Indice di partenza per i valori globali
        state[globalStartIndex] = maxLatency;         // Aggiungi epsilon
        state[globalStartIndex + 1] = taskSize;       // Aggiungi clock
    
        return state;
    }
    
    private int greedyChoice(String[] architecture, Task task){
        int selected = 0;
        double bestfit = Double.MAX_VALUE;
        double bestnumberofcores = 0;
        for(int i = 0; i < nodeList.size(); i++){
            //viene scelto il nodo con il miglio rapporto TaskOffloaded/coresTotali
            if( (super.historyMap.get(i)/nodeList.get(i).getNumberOfCPUCores() < bestfit) && offloadingIsPossible(task, nodeList.get(i), architecture)){
                bestnumberofcores = nodeList.get(i).getNumberOfCPUCores();
                bestfit = super.historyMap.get(i)/nodeList.get(i).getNumberOfCPUCores();
                selected = i;
            }
            //laddove si abbia un rapporto TaskOffloaded/coresTotali uguale prevale il nodo con il numero di cores maggiore
            else if((super.historyMap.get(i)/nodeList.get(i).getNumberOfCPUCores() == bestfit) && (bestnumberofcores < nodeList.get(i).getNumberOfCPUCores()) && offloadingIsPossible(task, nodeList.get(i), architecture)){
                bestnumberofcores = nodeList.get(i).getNumberOfCPUCores();
                bestfit = super.historyMap.get(i)/nodeList.get(i).getNumberOfCPUCores();
                selected = i;
            }
        }
        if("GREEDY".equals(algorithmName)) super.historyMap.put(selected, super.historyMap.get(selected) + 1);
        return selected;
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

        //increase the sentTasks of the VM
        //nodeList.get(action).increaseTask(task);                      //da mettere solo se si usa il MysimulationManager

		// Application has been deployed
		task.getEdgeDevice().setApplicationPlacementLocation(node);
		simLog.deepLog(simulationManager.getSimulation().clock() + ": " + this.getClass() + " Task: " + task.getId() + " assigned to " + node.getType() + " Computing Node: " + node.getId());
    }

    protected void DoDDQN(String[] architecture, Task task) {
        // aggiungi il task alla lista dei task orchestrati
        tasksList.add(task);

        // Ottiene lo stato attuale basato sui nodi disponibili
        double[] state = getCurrentState(nodeList, task);
    
        // Ciclo su tutte le possibili destinazioni per scegliere la migliore azione
        int action = m_agent.chooseAction(state, architecture, task);
        super.historyMap.put(action, super.historyMap.get(action) + 1);

        //setto stato e azione per il task
        task.setCurrentState(state);    
        task.setAction(action);
    
        // Esegui l'azione e avanza verso lo stato successivo
        PerformAction(action, task);
    }

    //il task viene eseguito e si preleva il nextState
	@Override
	public void notifyOrchestratorOfTaskExecution(Task task) {
        if(algorithmName.equals("DQN") && IsTrainingOn && this.tasksList.contains(task)) task.setNextState(getCurrentState(nodeList, task));
    }
    
    //il task ha finito e se previsto, si effettua l'update dell'Agent
	@Override
	public void resultsReturned(Task task) {
        if(algorithmName.equals("DQN") && IsTrainingOn && this.tasksList.contains(task)) m_agent.DDQN(task, isDone());
	}

    private boolean isDone() {
        // Definire la condizione di terminazione della simulazione
        return false;
    }

    @Override
    public void onSimulationEnd() {
        m_agent.IterationEnd();
        m_agent.saveAll();
    }

    public static void main(String[] args) {
		ProgettoGenio sim = new ProgettoGenio();
		sim.StartSimulation();
	}

}