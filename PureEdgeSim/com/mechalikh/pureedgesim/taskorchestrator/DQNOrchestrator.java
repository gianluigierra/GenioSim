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
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.simulationmanager.SimulationManager;
import com.mechalikh.pureedgesim.taskgenerator.Task;

public class DQNOrchestrator extends DefaultOrchestrator {
    private MultiLayerNetwork qNetwork;
    private MultiLayerNetwork targetNetwork;
    private ReplayBuffer replayBuffer;

    // Parametri DQN
    private double epsilon = SimulationParameters.epsilon;
    private double epsilonMin = SimulationParameters.epsilonMin;
    private double epsilonDecay = SimulationParameters.epsilonDecay;
    private double gamma = SimulationParameters.gamma;
    private double learningRate = SimulationParameters.learningRate;
    private int batchSize = SimulationParameters.batchSize;
    private int replayMemory = 10000;

    private int numberofreplayupdates = 0;
    private int numberofepsilonupdates = 0;
    private int numberoftasksorchestrated = 0;

    public DQNOrchestrator(SimulationManager simulationManager) {
        super(simulationManager);
        qNetwork = createNetwork();
        targetNetwork = createNetwork();
        replayBuffer = new ReplayBuffer(replayMemory);  // Memoria di replay
    }
    
    private MultiLayerNetwork createNetwork() {
        int inputSize = getStateSize();
        int outputSize = getActionSize();

        MultiLayerNetwork network = new MultiLayerNetwork(new NeuralNetConfiguration.Builder()
            .updater(new Adam(learningRate))
            .list()
            .layer(new DenseLayer.Builder().nIn(inputSize).nOut(128)
                    .activation(Activation.RELU)
                    .weightInit(WeightInit.XAVIER)
                    .build())
            .layer(new DenseLayer.Builder().nIn(128).nOut(128)
                    .activation(Activation.RELU)
                    .weightInit(WeightInit.XAVIER)
                    .build())
            .layer(new OutputLayer.Builder(LossFunctions.LossFunction.MSE)
                    .activation(Activation.IDENTITY)
                    .nIn(128).nOut(outputSize)
                    .build())
            .build());

        network.init();
        return network;
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

    @Override
    // Find an offloading location for this task
	public void orchestrate(Task task) {
        if ("ROUND_ROBIN".equals(algorithmName) || "TRADE_OFF".equals(algorithmName)) {
			assignTaskToComputingNode(task, architectureLayers);
		} else if ("DQN".equals(algorithmName)) {
			DoDQN(architectureLayers, task);
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

    private int getActionSize() {
        // Il numero di azioni corrisponde al numero di nodi disponibili
        return nodeList.size();  // Restituisce il numero di nodi di calcolo disponibili
    } 

    private int getStateSize() {
        return 4;  // Stato con 6 variabili
    }
  
    private double[] getCurrentState(List<ComputingNode> nodeList, Task task) {
		double avgAvailableRam = getAvgAvailableRam(nodeList);
		double avgAvailableStorage = getAvgAvailableStorage(nodeList);
        double avgMipsPerCore = getAverageMipsPerCore(nodeList);
        double avgcpuLoad = getAvgCpuLoad(nodeList);
        //double tasksFailed = simulationManager.getSimulationLogger().tasksFailed;
    
        return new double[]{avgAvailableRam, avgcpuLoad, avgAvailableStorage, avgMipsPerCore};
    }

    private double[] getNextState(int nodeIndex, Task task) {
        // Aggiorna lo stato in base al nodo selezionato e al task assegnato
        ComputingNode node = nodeList.get(nodeIndex);
    
        double nodeRam = node.getAvailableRam();
        double nodeStorage = node.getAvailableStorage();
        double nodeMips = node.getTotalMipsCapacity();
        double nodecpuLoad = node.getAvgCpuUtilization();
        //double tasksFailed = simulationManager.getSimulationLogger().tasksFailed;
    
        return new double[]{nodeRam, nodeStorage, nodeMips, nodecpuLoad};
    }

    private double getAvgTasksQueue(List<ComputingNode> nodeList) {
        int AvgTasks = 0;
        for(int i = 0; i < nodeList.size(); i++){
            AvgTasks += nodeList.get(i).getTasksQueue().size();
        }
        return AvgTasks/nodeList.size();
    }

    private double getAverageMipsPerCore(List<ComputingNode> nodeList) {
        int AvgMips = 0;
        for(int i = 0; i < nodeList.size(); i++){
            AvgMips += nodeList.get(i).getMipsPerCore();
        }
        return AvgMips/nodeList.size();
    }
    
    private double getAvgAvailableStorage(List<ComputingNode> nodeList) {
        int AvgStorage = 0;
        for(int i = 0; i < nodeList.size(); i++){
            AvgStorage += nodeList.get(i).getAvailableStorage()/nodeList.get(i).getNumberOfCPUCores();
        }
        return AvgStorage/nodeList.size();
    }

    private double getAvgAvailableRam(List<ComputingNode> nodeList) {
        int AvgRamLoad = 0;
        for(int i = 0; i < nodeList.size(); i++){
            AvgRamLoad += nodeList.get(i).getAvailableRam()/nodeList.get(i).getNumberOfCPUCores();
        }
        return AvgRamLoad/nodeList.size();
    }

    private double getAvgCpuLoad(List<ComputingNode> nodeList) {
		double averageCpuUtilization = 0;
		for (ComputingNode node : nodeList) {
			averageCpuUtilization += node.getAvgCpuUtilization();
		}
		return averageCpuUtilization;
    }
 
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
        if (!simulationManager.getScenario().getStringOrchArchitecture().equals("EDGE_ONLY") && simulationManager.getSimulation().clock() <= SimulationParameters.neuralNetworkLearningSpeed * 3)
            return tradeOff(architecture, task);
        if (rand.nextDouble() < epsilon) {
            System.out.println("Scelta random, epsilon = " + epsilon);
            return randChoice(architecture, task);
        } else {
            System.out.print("Scelta dalla rete neurale, ");
            // Get the action index based on the Q-values
            int k = 0;
            while(k < nodeList.size()){
                int choice = getKthBestQAction(state, k); // Change the 1 to 2, 3, etc., for second, third best, etc.
                if (offloadingIsPossible(task, nodeList.get(choice), architectureLayers)) {
                    System.out.println("nodo: " + nodeList.get(choice).getName() + ", Dimensionequeue = " + nodeList.get(choice).getTasksQueue().size() + ", epsilon = " + epsilon); 
                    return choice;
                }
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
        //return qValues.get(k).getValue();


        //Sopra è l'implementazione corretta, questa effettua una scelta pseudo-randomica basata sui migliore 3 nodi.
        int exploreTopK = 3; // Esplora tra i primi 3 migliori Q-values
        Random rand = new Random();
        if (rand.nextDouble() < epsilon) {
            k = rand.nextInt(Math.min(exploreTopK, qValues.size()));
        }
        return qValues.get(k).getValue();
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
		simLog.deepLog(simulationManager.getSimulation().clock() + ": " + this.getClass() + " Task: " + task.getId() + " assigned to " + node.getType() + " Computing Node: " + node.getId());
    }

    private void updateNetwork(int batchSize) {
        List<Experience> batch = replayBuffer.sample(batchSize);  // Campionamento del buffer di replay
    
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
        //{avgAvailableRam, avgcpuLoad, avgAvailableStorage, avgTotalMips, maxLatency, fileSizeInBits};
        double avgAvailableRam = state[0];
        double avgcpuLoad = state[1];
        double avgAvailableStorage = state[2];
        double avgMipsPerCore = state[3];
    
        // Inizializza la ricompensa
        double reward = 0.0;
    
        // Ricompensa/penalizzazione basata sulla RAM disponibile
        if (avgAvailableRam < nodeList.get(action).getAvailableRam()) reward += 10;
        else if (avgAvailableRam > nodeList.get(action).getAvailableRam()) reward -= 10;
        
        // Penalizzazione per carico CPU elevato
        if (avgcpuLoad < nodeList.get(action).getAvgCpuUtilization()) reward -= 10;
        else if (avgcpuLoad > nodeList.get(action).getAvgCpuUtilization()) reward += 10;
    
        // Ricompensa per disponibilità di storage
        if (avgAvailableStorage < nodeList.get(action).getAvailableStorage()) reward += 10;  // Abbondante storage disponibile
        else if (avgAvailableStorage > nodeList.get(action).getAvailableStorage()) reward -= 10;  // Penalizzazione per storage quasi pieno
    
        // Ricompensa per nodi con potenza di calcolo elevata (MIPS)
        if (nodeList.get(action).getTotalMipsCapacity() > avgMipsPerCore)  reward += 10;
        else  if(nodeList.get(action).getTotalMipsCapacity() < avgMipsPerCore) reward -= 10;      

        //penalizzo il nodo per avere i task che non sono stati ancora eseguiti
        for(Task TaskI : nodeList.get(action).getTasksQueue()){
            double taskDelay = TaskI.getTotalDelay();
            double maxLatency = TaskI.getMaxLatency();
            if(taskDelay > maxLatency*0.9) reward -= 15 + (taskDelay - maxLatency * 0.9) * 10;  //Penalizzo il nodo se è al 90% del delay del task. Penalizza di più se è vicino al limite
        }

        //penalizzo il nodo per avere una coda di task più grande della media
        if(nodeList.get(action).getTasksQueue().size() > getAvgTasksQueue(nodeList)) reward -= (20 + (nodeList.get(action).getTasksQueue().size() - getAvgTasksQueue(nodeList)) * 5);
        else reward += 20;

        System.out.println("Nodo: "+nodeList.get(action).getName()+", reward: " + reward);
        return reward;
    }
    
    protected void DoDQN(String[] architecture, Task task) {
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
        if (replayBuffer.size() > batchSize && simulationManager.getSimulation().clock() % (SimulationParameters.neuralNetworkLearningSpeed / 2) ==0) {
            numberofreplayupdates++;
            updateNetwork(batchSize);
        }
    
        // Aggiorna epsilon per ridurre gradualmente l'esplorazione
        if (epsilon > epsilonMin && simulationManager.getSimulation().clock() > (SimulationParameters.neuralNetworkLearningSpeed * 3) && simulationManager.getSimulation().clock() % SimulationParameters.neuralNetworkLearningSpeed==0) {
            numberofepsilonupdates++;
            epsilon *= epsilonDecay;
        }
    
        // Aggiorna la rete target se necessario
        if (shouldUpdateTargetNetwork()) {        
            System.out.println("----------------------------------------------UPDATE FATTO----------------------------------------------");
            targetNetwork.setParams(qNetwork.params());
        }

        numberoftasksorchestrated++;

        if(simulationManager.getSimulation().clock() > 17000 ) {System.out.println("Tasks orchestrated: " + numberoftasksorchestrated + " , epsilon updates: " + numberofepsilonupdates + " , replay updates: " + numberofreplayupdates);}
    }


    private boolean isDone() {
        // Definire la condizione di terminazione della simulazione
        return false;
    }

    private int targetUpdateCounter = 0;

    private boolean shouldUpdateTargetNetwork() {
        // Definire la logica per determinare quando aggiornare la rete target
        targetUpdateCounter++;
        return targetUpdateCounter %  (SimulationParameters.neuralNetworkLearningSpeed * 10)== 0;  // (neuralNetworkLearningSpeed * 10) iterazioni
        //return true;  
    }

}