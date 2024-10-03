package com.mechalikh.pureedgesim.taskorchestrator;

import java.util.Random;
import java.util.ArrayList;
import java.util.List;

import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.apache.commons.lang3.tuple.Pair;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.weights.WeightInit;
import org.nd4j.linalg.learning.config.Adam;
import org.nd4j.linalg.lossfunctions.LossFunctions;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode;
import com.mechalikh.pureedgesim.simulationmanager.SimulationManager;
import com.mechalikh.pureedgesim.taskgenerator.Task;

public class DQNOrchestrator extends DefaultOrchestrator {
    private MultiLayerNetwork qNetwork;
    private MultiLayerNetwork targetNetwork;
    private ReplayBuffer replayBuffer;

    // Parametri DQN
    private double epsilon = 1.0;
    private double epsilonMin = 0.1;
    private double epsilonDecay = 0.995;
    private double gamma = 0.99;
    private double learningRate = 0.001;

    public DQNOrchestrator(SimulationManager simulationManager) {
        super(simulationManager);
        qNetwork = createNetwork();
        targetNetwork = createNetwork();
        replayBuffer = new ReplayBuffer(1000);  // Memoria di replay
    }

    @Override
    protected int findComputingNode(String[] architecture, Task task) {
        if ("ROUND_ROBIN".equals(algorithmName)) {
            return super.roundRobin(architecture, task);
		} else if ("TRADE_OFF".equals(algorithmName)) {
            return super.tradeOff(architecture, task);
		} else {
			throw new IllegalArgumentException(getClass().getSimpleName() + " - Unknown orchestration algorithm '"
					+ algorithmName + "', please check the simulation parameters file...");
		}
    }

    private MultiLayerNetwork createNetwork() {
        int inputSize = getStateSize();
        int outputSize = getActionSize();

        MultiLayerNetwork network = new MultiLayerNetwork(new NeuralNetConfiguration.Builder()
            .updater(new Adam(learningRate))
            .list()
            .layer(new DenseLayer.Builder().nIn(inputSize).nOut(64)
                    .activation(Activation.RELU)
                    .weightInit(WeightInit.XAVIER)
                    .build())
            .layer(new DenseLayer.Builder().nIn(64).nOut(64)
                    .activation(Activation.RELU)
                    .weightInit(WeightInit.XAVIER)
                    .build())
            .layer(new OutputLayer.Builder(LossFunctions.LossFunction.MSE)
                    .activation(Activation.IDENTITY)
                    .nIn(64).nOut(outputSize)
                    .build())
            .build());

        network.init();
        return network;
    }

    private int getActionSize() {
        // Il numero di azioni corrisponde al numero di nodi disponibili
        return nodeList.size();  // Restituisce il numero di nodi di calcolo disponibili
    } 

    private double getTasksQueue(List<ComputingNode> nodeList) {
        int AvgTasks = 0;
        for(int i = 0; i < nodeList.size(); i++){
            AvgTasks += nodeList.get(i).getTasksQueue().size();
        }
        return AvgTasks/nodeList.size();
    }

    private double getAverageMips(List<ComputingNode> nodeList) {
        int AvgMips = 0;
        for(int i = 0; i < nodeList.size(); i++){
            AvgMips += nodeList.get(i).getTotalMipsCapacity();
        }
        return AvgMips/nodeList.size();
    }
    
    private double getAvailableStorage(List<ComputingNode> nodeList) {
        int AvgStorage = 0;
        for(int i = 0; i < nodeList.size(); i++){
            AvgStorage += nodeList.get(i).getAvailableStorage();
        }
        return AvgStorage/nodeList.size();
    }

    private double getAvailableRam(List<ComputingNode> nodeList) {
        int AvgRamLoad = 0;
        for(int i = 0; i < nodeList.size(); i++){
            AvgRamLoad += nodeList.get(i).getAvailableRam();
        }
        return AvgRamLoad/nodeList.size();
    }

    /*//commentata perchè posso usare quella definita in SimLog.java che era protected e messa public
    private double getCpuLoad(List<ComputingNode> nodeList) {
        int AvgCpuLoad = 0;
        // Implementare la logica per ottenere il carico CPU medio
        if(simulationManager.getSimulation().clock()%1800==0) System.out.println("Clock " + simulationManager.getSimulation().clock() + ": ");
        for(int i = 0; i < nodeList.size(); i++){
            if(simulationManager.getSimulation().clock() % 1800 == 0) System.out.println("Node "+ nodeList.get(i).getType() + " cpu utilization: " +nodeList.get(i).getAvgCpuUtilization());
            AvgCpuLoad += nodeList.get(i).getAvgCpuUtilization();
        }
        return AvgCpuLoad;
    }
    */
 
    private int randChoice(String[] architecture, Task task){
        Random rand = new Random();
        while(true){
            int random = rand.nextInt(getActionSize());
            if(offloadingIsPossible(task, nodeList.get(random), architectureLayers))
                return random;
        }
    }

    private int chooseAction(double[] state, String[] architecture, Task task) {
        Random rand = new Random();
        if (rand.nextDouble() < epsilon) {
            return randChoice(architecture, task);
        } else {
            int choice = getMaxQAction(state);
            if(offloadingIsPossible(task, nodeList.get(choice), architectureLayers)) return choice;
            else return randChoice(architecture, task);
        }
    }
 
    private int getMaxQAction(double[] state) {
        // Reshape the input to be a 2D array with shape [1, state.length]
        INDArray input = Nd4j.create(state).reshape(1, state.length);
        INDArray output = qNetwork.output(input);
        return Nd4j.argMax(output).getInt(0);
    }

    private int chooseAction2(double[] state, String[] architecture, Task task) {
        Random rand = new Random();
        if (rand.nextDouble() < epsilon) {
            return randChoice(architecture, task);
        } else {
            // Get the action index based on the Q-values
            int k = 0;
            while(k < nodeList.size()){
                int choice = getKthBestQAction(state, k); // Change the 1 to 2, 3, etc., for second, third best, etc.
                if (offloadingIsPossible(task, nodeList.get(choice), architectureLayers)) return choice;
                k++;
            }
            return 0;       //non può verificarsi 
        }
    }

    private int getKthBestQAction(double[] state, int k) {
        // Reshape the input to be a 2D array with shape [1, state.length]
        INDArray input = Nd4j.create(state).reshape(1, state.length);
        INDArray output = qNetwork.output(input);
        
        // Create a list to store Q-values with their corresponding indices
        List<Pair<Double, Integer>> qValues = new ArrayList<>();
        
        // Iterate through the output and store Q-values and indices as Pair
        for (int i = 0; i < output.length(); i++) {
            qValues.add(Pair.of(output.getDouble(i), i)); // Using Pair.of() method
        }
        
        // Sort the list based on Q-values in descending order
        qValues.sort((pair1, pair2) -> Double.compare(pair2.getKey(), pair1.getKey()));
        
        // Return the index of the kth best action (0-based index)
        return qValues.get(k).getValue();
    }

    
    @Override
    // Find an offloading location for this task
	public void orchestrate(Task task) {
        if ("ROUND_ROBIN".equals(algorithmName) || "TRADE_OFF".equals(algorithmName)) {
			assignTaskToComputingNode(task, architectureLayers);
		} else if ("DQN".equals(algorithmName)) {
			DoDQN2(architectureLayers, task);
		} else {
			throw new IllegalArgumentException(getClass().getSimpleName() + " - Unknown orchestration algorithm '"
					+ algorithmName + "', please check the simulation parameters file...");
		}
	}

    @Override
    protected void assignTaskToComputingNode(Task task, String[] architectureLayers) {

		int nodeIndex = findComputingNode(architectureLayers, task);

		if (nodeIndex != -1) {
            PerformAction(nodeIndex, task);
		}
	}


    // Metodo per eseguire l'azione 
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
			task.getEdgeDevice().setApplicationPlacementLocation(node);
			simLog.deepLog(simulationManager.getSimulation().clock() + ": " + this.getClass() + " Task: " + task.getId()
					+ " assigned to " + node.getType() + " Computing Node: " + node.getId());
    }

    private boolean isDone() {
        // Definire la condizione di terminazione della simulazione
        return false;
    }

    private boolean shouldUpdateTargetNetwork() {
        // Definire la logica per determinare quando aggiornare la rete target
        return true;  // Aggiornamento periodico
    }

    private double[] getCurrentState(List<ComputingNode> nodeList, Task task) {
		double avgRam = getAvailableRam(nodeList);
		double avgStorage = getAvailableStorage(nodeList);
        double avgMips = getAverageMips(nodeList);
        //double cpuLoad = getCpuLoad(nodeList);
        double cpuLoad = simulationManager.getSimulationLogger().getCpuUtilization(nodeList);
        double maxLatency = task.getMaxLatency();
        double fileSizeInBits = task.getFileSizeInBits();
        double tasksFailed = simulationManager.getSimulationLogger().tasksFailed;
    
        return new double[]{avgRam, cpuLoad, avgStorage, avgMips, maxLatency, fileSizeInBits, tasksFailed};
    }

    private int getStateSize() {
        // Il nostro stato è definito da 4 parametri: latenza, carico CPU, energia e larghezza di banda
        return 7;  // Stato con 4 variabili
    }

    //inizia da qui il tentativo 2
    protected void DoDQN2(String[] architecture, Task task) {
        // Ottiene lo stato attuale basato sui nodi disponibili
        double[] state = getCurrentState(nodeList, task);
    
        // Ciclo su tutte le possibili destinazioni per scegliere la migliore azione
        int action = chooseAction2(state, architecture, task);

        //System.out.println("Action: " + action);
    
        // Esegui l'azione e ottieni lo stato successivo
        PerformAction(action, task);
        double reward = grantReward(action, task);
        double[] nextState = getNextState(action, task);  // Ottieni lo stato successivo dopo l'azione
    
        // Aggiungi l'esperienza nel replay buffer
        replayBuffer.add(new Experience(state, action, reward, nextState, isDone()));
    
        // Aggiorna la rete neurale se il replay buffer ha abbastanza esperienze
        if (replayBuffer.size() > 32 && simulationManager.getSimulation().clock() % 10 == 0) {
            updateNetwork2();
        }
    
        // Aggiorna epsilon per ridurre gradualmente l'esplorazione
        if (epsilon > epsilonMin) {
            epsilon *= epsilonDecay;
        }
    
        // Aggiorna la rete target se necessario
        if (shouldUpdateTargetNetwork()) {
            targetNetwork.setParams(qNetwork.params());
        }
    }

    private double[] getNextState(int nodeIndex, Task task) {
        // Aggiorna lo stato in base al nodo selezionato e al task assegnato
        ComputingNode node = nodeList.get(nodeIndex);
    
        double avgRam = node.getAvailableRam();
        double avgStorage = node.getAvailableStorage();
        double avgMips = node.getTotalMipsCapacity();
        double cpuLoad = simulationManager.getSimulationLogger().getCpuUtilization(nodeList);
        //System.out.println(cpuLoad/nodeList.size());
        double maxLatency = task.getMaxLatency();
        double fileSizeInBits = task.getFileSizeInBits();
        double tasksFailed = simulationManager.getSimulationLogger().tasksFailed;
    
        return new double[]{avgRam, cpuLoad, avgStorage, avgMips, maxLatency, fileSizeInBits, tasksFailed};
    }

    private void updateNetwork2() {
        List<Experience> batch = replayBuffer.sample(16);  // Campionamento del buffer di replay
    
        for (Experience exp : batch) {
            double targetQ = exp.reward;
    
            if (!exp.done) {
                // Ottieni il valore massimo Q per il nextState utilizzando la rete target
                INDArray nextInput = Nd4j.create(exp.nextState).reshape(1, exp.nextState.length);
                INDArray nextQValues = targetNetwork.output(nextInput);  // Usa la rete target per predire
                double maxNextQ = nextQValues.max(1).getDouble(0);  // Prendi il massimo valore Q per il prossimo stato
                targetQ += gamma * maxNextQ;  // Formula di aggiornamento DQN
            }
    
            // Ottieni il valore Q corrente per lo stato attuale
            INDArray input = Nd4j.create(exp.state).reshape(1, exp.state.length);
            INDArray target = qNetwork.output(input);
    
            // Imposta il valore target per l'azione specifica
            target.putScalar(exp.action, targetQ);
    
            // Addestra la rete con lo stato corrente e il valore target aggiornato
            qNetwork.fit(input, target);
        }
    }

    private double grantReward(int action, Task task) {
        // Ottieni lo stato corrente basato su stato2
        double[] state = getCurrentState(nodeList, task);
        // {avgRam, cpuLoad, avgStorage, avgMips, maxLatency, fileSizeInBits}
        double avgRam = state[0];
        double cpuLoad = state[1];
        double avgStorage = state[2];
        double avgMips = state[3];
        double maxLatency = state[4];
        double fileSizeInBits = state[5];
        double tasksFailed = state[6];

        // if(simulationManager.getSimulation().clock()%5 == 0){
        //     System.out.println("System Clock: " + simulationManager.getSimulation().clock());
        //     System.out.println("avgRam: " + state[0]);
        //     System.out.println("CpuLoad: " + state[1]);
        //     System.out.println("avgStorage: " + state[3]);
        //     System.out.println("avgMips: " + state[4]);
        //     System.out.println("\n\n");
        // }
    
        // Inizializza la ricompensa
        double reward = 100.0;
    
        // Ricompensa/penalizzazione basata sulla RAM disponibile
        if (avgRam < nodeList.get(action).getAvailableRam()) reward += 20;
        else if (avgRam < nodeList.get(action).getAvailableRam()) reward -= 30;
        
        // Penalizzazione per carico CPU elevato
        if (cpuLoad < nodeList.get(action).getAvgCpuUtilization()) reward -= 15;
        else if (cpuLoad > nodeList.get(action).getAvgCpuUtilization()) reward += 15;
    
        // Ricompensa per disponibilità di storage
        if (avgStorage < nodeList.get(action).getAvailableStorage()) reward += 10;  // Abbondante storage disponibile
        else if (avgStorage > nodeList.get(action).getAvailableStorage()) reward -= 20;  // Penalizzazione per storage quasi pieno
    
        // Ricompensa per nodi con potenza di calcolo elevata (MIPS)
        if (nodeList.get(action).getTotalMipsCapacity() > avgMips)  reward += 30;
        else  if(nodeList.get(action).getTotalMipsCapacity() < avgMips) reward -= 25;      

        //penalizzo il nodo per avere i task che non sono stati ancora eseguiti
        for(Task TaskI : nodeList.get(action).getTasksQueue()){
            if(TaskI.getTotalDelay() > TaskI.getMaxLatency()*0.8) reward -= 10;
        }
    
        // Penalizzazione per latenza eccessiva
        if (maxLatency < task.getMaxLatency()) {  // Se la latenza rientra nel limite massimo
            reward += 0;
        } else {  // Penalizzazione se la latenza massima supera il limite consentito
            reward -= 0;
        }
    
        /* 

        // Penalizzazione per dimensioni del file eccessive
        if (nodeList.get(action).getAvailableStorage() > fileSizeInBits) {  // Se il file è più grande di 1 MB
            reward += 5;
        } else {  // Ricompensa per file più piccoli, più veloci da gestire
            reward -= 30;
        }

        if(tasksFailed < simulationManager.getSimulationLogger().tasksSent*0.15) {  // Se i fallimenti per latency superano il 0.1% di tutti i task inviati
            reward += 20;
        } else {  // Reward altrimenti
            reward -= 30;
        }

        */

        //System.out.println("Clock " + simulationManager.getSimulation().clock() + ", reward: " +reward);
    
        return reward;
    }
    

}