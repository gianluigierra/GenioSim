package com.mechalikh.pureedgesim.taskorchestrator;

import java.util.Random;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Scanner;
import org.json.JSONObject;
import java.io.FileWriter;
import java.io.FileReader;

import javax.swing.JOptionPane;

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
import com.mechalikh.pureedgesim.simulationengine.OnSimulationEndListener;
import com.mechalikh.pureedgesim.simulationmanager.SimulationManager;
import com.mechalikh.pureedgesim.taskgenerator.Task;

public class DQNOrchestrator extends DefaultOrchestrator implements OnSimulationEndListener{
    private MultiLayerNetwork qNetwork;
    private MultiLayerNetwork targetNetwork;
    private ReplayBuffer replayBuffer;

    // Parametri DQN
    private double epsilon = SimulationParameters.epsilon;
    private double epsilonMin = SimulationParameters.epsilonMin;
    private double epsilonDecay = SimulationParameters.epsilonDecay;
    private double gamma = SimulationParameters.gamma;
    private double learningRate = SimulationParameters.learningRate;
    private int batchSize = SimulationParameters.networkBatchSize;
    private int replayMemory = 10000;

    //variabili per esecuzione
    private int replayBufferfUpdateCounter = 0;
    private int epsilonUpdateCounter = 0;
    private int targetUpdateCounter = 0;
    private int numberoftasksorchestrated = 0;
    private boolean usePreviousModel = true;
    private boolean saveThisModel = true;

    //questi servono solo per printare debug
    private int numberofreplayupdates = 0;
    private int numberofepsilonupdates = 0;
    private boolean printNodeDestination = true;
    private boolean printVMtaskUsage = true;


    public DQNOrchestrator(SimulationManager simulationManager) {
        super(simulationManager);

        if ("DQN".equals(algorithmName) && usePreviousModel) {
            int response = JOptionPane.showConfirmDialog(null, "Vuoi Caricare il modello?", "Carica modello", JOptionPane.YES_NO_OPTION);
            
            if (response == JOptionPane.YES_OPTION) {
                String modelPath = SimulationParameters.settingspath + SimulationParameters.simName + "/dqn_model_" + SimulationParameters.simName;
                loadModel(modelPath);
                modelPath = SimulationParameters.settingspath + SimulationParameters.simName;
                this.replayBuffer = loadReplayBuffer(modelPath);    //carico il replayBuffer
                this.epsilon = loadEpsilon(modelPath);  //carico la epsilon
            }
            else {
                qNetwork = createNetwork();
                targetNetwork = createNetwork();
            }
            replayBuffer = new ReplayBuffer(replayMemory);
        }
        else if ("DQN".equals(algorithmName) && !usePreviousModel){
            qNetwork = createNetwork();
            targetNetwork = createNetwork();
            replayBuffer = new ReplayBuffer(replayMemory);
        }

    }
    
    private MultiLayerNetwork createNetwork() {
        int inputSize = getStateSize();
        int outputSize = getActionSize();

        int size = 64;

        MultiLayerNetwork network = new MultiLayerNetwork(new NeuralNetConfiguration.Builder()
            .updater(new Adam(learningRate))
            .seed(1234)
            .list()
            .layer(new DenseLayer.Builder().nIn(inputSize).nOut(size)
                    .activation(Activation.RELU)
                    .weightInit(WeightInit.XAVIER)
                    .build())
            .layer(new DenseLayer.Builder().nIn(size).nOut(size)
                    .activation(Activation.RELU)
                    .weightInit(WeightInit.XAVIER)
                    .build())
            .layer(new OutputLayer.Builder(LossFunctions.LossFunction.MSE)
                    .activation(Activation.IDENTITY)
                    .nIn(size).nOut(outputSize)
                    .build())
            .build());

        network.init();
        return network;
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

    private int getActionSize() {
        // Il numero di azioni corrisponde al numero di nodi disponibili
        return nodeList.size();  // Restituisce il numero di nodi di calcolo disponibili
    } 

    private int getStateSize() {
        //return 6;  // Stato con 6 variabili
        //return 3 * nodeList.size() + 4;
        return 5*nodeList.size() + 2;           //modificare se si passa alla versione con epsilon e clock
    }
  
    private double[] getCurrentState(List<ComputingNode> nodeList, Task task) {
		double avgAvailableRam = getAvgAvailableRam(nodeList);
		double avgAvailableStorage = getAvgAvailableStorage(nodeList);
        double avgMipsTotal = getAverageMipsTotal(nodeList);
        double avgcpuLoad = getAvgCpuLoad(nodeList);
        double avgfailurerate = getAvgFailureRate(nodeList);
        //double tasksFailed = simulationManager.getSimulationLogger().tasksFailed;
    
        return new double[]{avgAvailableRam, avgcpuLoad, avgAvailableStorage, avgMipsTotal, avgfailurerate};
    }

    private double[] getCurrentState2(List<ComputingNode> nodeList, Task task) {
		double avgAvailableRam = getAvgAvailableRam(nodeList);
		double avgAvailableStorage = getAvgAvailableStorage(nodeList);
        double avgcpuLoad = getAvgCpuLoad(nodeList);
        double avgfailurerate = getAvgFailureRate(nodeList);
        double avgsenttasks = getAverageOrchestratedTasks(nodeList);
        double epsilon = this.epsilon;
        double clock = simulationManager.getSimulation().clock();
    
        return new double[]{avgAvailableRam, avgcpuLoad, avgAvailableStorage, avgsenttasks, avgfailurerate, epsilon, clock};
    }

    private double[] getCurrentState3(List<ComputingNode> nodeList, Task task) {
        // Il nuovo stato conterrà 3 valori per ogni nodo e 4 variabili globali
        double[] state = new double[nodeList.size() * 3 + 4]; // 3 valori per nodo (CPU, FailureRate, sentTasks) + 4 variabili globali
    
        // Aggiungi i valori per ogni nodo nella nodeList
        for (int i = 0; i < nodeList.size(); i++) {
            ComputingNode node = nodeList.get(i);
    
            double cpuLoad = node.getAvgCpuUtilization();       // Ottieni il carico CPU del nodo
            double failureRate = node.getFailureRate(); // Ottieni il failure rate del nodo
            double sentTasks = super.historyMap.get(i);   // Ottieni il numero di task orchestrati
    
            state[i * 3] = cpuLoad;          // Metti il valore di CPU load
            state[i * 3 + 1] = failureRate;  // Metti il valore di failureRate
            state[i * 3 + 2] = sentTasks;    // Metti il numero di sentTasks
        }
    
        // Aggiungi i valori globali
        double avgAvailableRam = getAvgAvailableRam(nodeList);          // Media della RAM disponibile
        double avgAvailableStorage = getAvgAvailableStorage(nodeList);  // Media dello storage disponibile
        double epsilon = this.epsilon;                                  // Epsilon attuale
        double clock = simulationManager.getSimulation().clock();       // Tempo corrente della simulazione
    
        int globalStartIndex = nodeList.size() * 3;  // Indice di partenza per i valori globali
        state[globalStartIndex] = avgAvailableRam;     // Aggiungi avgAvailableRam
        state[globalStartIndex + 1] = avgAvailableStorage; // Aggiungi avgAvailableStorage
        state[globalStartIndex + 2] = epsilon;         // Aggiungi epsilon
        state[globalStartIndex + 3] = clock;           // Aggiungi clock
    
        return state;
    }

    private double[] getCurrentState4(List<ComputingNode> nodeList, Task task) {
        // Il nuovo stato conterrà 5 valori per ogni nodo e 2 variabili
        double[] state = new double[nodeList.size() * 5 + 2]; // 5 valori per nodo (CPU, FailureRate, sentTasks, RAM e Storage) + 4 variabili
    
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
            state[i * 5 + 3] = ram;    // Metti la ram 
            state[i * 5 + 4] = storage;    // Metti lo storage
        }
    
        // Aggiungi i valori globali
        double epsilon = this.epsilon;                                  // Epsilon attuale
        double clock = simulationManager.getSimulation().clock();       // Tempo corrente della simulazione
        double taskSize = task.getFileSizeInBits();                     // size del task in bytes
        double maxLatency = task.getMaxLatency();                       //latency max del task
    
        int globalStartIndex = nodeList.size() * 5;  // Indice di partenza per i valori globali
        state[globalStartIndex] = maxLatency;         // Aggiungi epsilon
        state[globalStartIndex + 1] = taskSize;       // Aggiungi clock
        //state[globalStartIndex + 2] = epsilon;    // Aggiungi tasksize
        //state[globalStartIndex + 3] = clock;  // Aggiungi maxlatency
    
        return state;
    }
    

    private double[] getNextState(int nodeIndex, Task task) {
        // Aggiorna lo stato in base al nodo selezionato e al task assegnato
        ComputingNode node = nodeList.get(nodeIndex);
    
        double nodeRam = node.getAvailableRam();
        double nodeStorage = node.getAvailableStorage();
        double nodeMips = node.getMipsPerCore();
        double nodecpuLoad = node.getAvgCpuUtilization();
        double nodefailurerate = node.getFailureRate();
        //double tasksFailed = simulationManager.getSimulationLogger().tasksFailed;
    
        return new double[]{nodeRam, nodeStorage, nodeMips, nodecpuLoad, nodefailurerate};
    }

    private double getAverageOrchestratedTasks(List<ComputingNode> nodeList){
        double AvgSentTasks = 0;
        for(int i = 0; i < nodeList.size(); i++){
            AvgSentTasks += super.historyMap.get(i);
        }
        return AvgSentTasks/nodeList.size();
    }

    private double getAvgFailureRate(List<ComputingNode> nodeList) {
        double AvgFailureRate = 0;
        for(ComputingNode cn : nodeList){
            AvgFailureRate += cn.getFailureRate();
        }
        return AvgFailureRate/nodeList.size();
    }

    private double getAvgTasksQueue(List<ComputingNode> nodeList) {
        int AvgTasks = 0;
        for(int i = 0; i < nodeList.size(); i++){
            AvgTasks += nodeList.get(i).getTasksQueue().size();
        }
        return AvgTasks/nodeList.size();
    }

    private double getAverageMipsTotal(List<ComputingNode> nodeList) {
        int AvgMips = 0;
        for(int i = 0; i < nodeList.size(); i++){
            AvgMips += nodeList.get(i).getTotalMipsCapacity();
        }
        return AvgMips/nodeList.size();
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

    //funzione greedy per iniziare il DQN
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

    private int chooseAction(double[] state, String[] architecture, Task task) {

        //i primi task vengono eseguiti con un algoritmo greedy, per fornire i pesi iniziali alla rete neurale
        //if (numberoftasksorchestrated <= SimulationParameters.tasksForGreedyTraining && SimulationParameters.greedyTraining)

        //usato per testare
        // if(numberoftasksorchestrated <= SimulationParameters.tasksForGreedyTraining && !SimulationParameters.greedyTraining)
        //     return tradeOff(architecture, task);
        //else 
            if(SimulationParameters.greedyTraining)
            return tradeOff(architecture, task);
     
        Random rand = new Random();
        if (rand.nextDouble() < epsilon) {
            //esplorazione
            if(printNodeDestination) System.out.println("Scelta random, epsilon = " + epsilon);
            return randChoice(architecture, task);
        } else {
            //exploitation
            if(printNodeDestination) System.out.print("Scelta dalla rete neurale, ");
            // Get the action index based on the Q-values
            int k = 0;
            while(k < nodeList.size()){
                int choice = getKthBestQAction(state, k); // Change the 1 to 2, 3, etc., for second, third best, etc.
                if (offloadingIsPossible(task, nodeList.get(choice), architectureLayers)) {
                    if(printNodeDestination) System.out.println("nodo: " + nodeList.get(choice).getName() + ", Dimensionequeue = " + nodeList.get(choice).getTasksQueue().size() + ", epsilon = " + epsilon);  
                    super.historyMap.put(choice, super.historyMap.get(choice) + 1);
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
        // int exploreTopK = 3; // Esplora tra i primi nodeList.size()/3 migliori Q-values. Analogamente si potrebbe mettere: = nodeList.size()/3
        // Random rand = new Random();
        // if (rand.nextDouble() < epsilon) {
        //     k = rand.nextInt(Math.min(exploreTopK, qValues.size()));                     
        // }
        // int random = rand.nextInt(100);
        // if(50 < random && random < 100) k = 0;
        // else if (20 < random && random < 50) k = 1;
        // else k = 3;
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

        //increase the sentTasks of the VM
        //nodeList.get(action).increaseTask(task);                      //da mettere solo se si usa il MysimulationManager

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
        //{avgAvailableRam, avgcpuLoad, avgAvailableStorage, avgMipsPerCore, avgfailurerate};
        double avgAvailableRam = state[0];
        double avgcpuLoad = state[1];
        double avgAvailableStorage = state[2];
        double avgMipsTotal = state[3];
        double avgfailurerate = state[4];
    
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
        if (nodeList.get(action).getTotalMipsCapacity() > avgMipsTotal)  reward += 10;
        else  if(nodeList.get(action).getTotalMipsCapacity() < avgMipsTotal) reward -= 10;    
        
        //penalizzo il nodo se il task viene messo in coda
        if(nodeList.get(action).getAvailableCores() == 0) reward -= 15;
        else reward += 10;

        //penalizzo il nodo per avere i task che non sono stati ancora eseguiti
        for(Task TaskI : nodeList.get(action).getTasksQueue()){
            double taskDelay = TaskI.getTotalDelay();
            double maxLatency = TaskI.getMaxLatency();
            if(taskDelay > maxLatency*0.9) reward -= 15 + (taskDelay - maxLatency * 0.9) * 10;  //Penalizzo il nodo se è al 90% del delay del task. Penalizza di più se è vicino al limite
        }

        //penalizzo il nodo per avere una coda di task più grande della media
        if(nodeList.get(action).getTasksQueue().size() > getAvgTasksQueue(nodeList)) reward -= (20 + (nodeList.get(action).getTasksQueue().size() - getAvgTasksQueue(nodeList)) * 5);
        else reward += 20;

        //penalizzo il nodo per avere un failure-rate più alto dei suoi vicini
        // if(nodeList.get(action).getFailureRate() > avgfailurerate) reward -= (nodeList.get(action).getFailureRate() - avgfailurerate)*nodeList.get(action).getNumberOfCPUCores()*2;
        // else reward += (10 + (nodeList.get(action).getFailureRate() - avgfailurerate)*nodeList.get(action).getNumberOfCPUCores()*2);

        if(printNodeDestination) System.out.println("Nodo: "+nodeList.get(action).getName()+", reward: " + reward);
        return reward;
    }
 
    private double grantReward2(int action, Task task){
        double reward = 0.0;

        //penalizzo il nodo se ha più task assegnati della media
        if(super.historyMap.get(action)  > getAverageOrchestratedTasks(nodeList)) reward -= Math.abs(super.historyMap.get(action)-getAverageOrchestratedTasks(nodeList));
        //lo premio se ne ha di meno
        else if (super.historyMap.get(action) < getAverageOrchestratedTasks(nodeList)) reward += Math.abs(super.historyMap.get(action)-getAverageOrchestratedTasks(nodeList));

        //premio il nodo se ha i mips più alti degli altri
        if(nodeList.get(action).getMipsPerCore() > getAverageMipsPerCore(nodeList)) reward += 1;

        if(printNodeDestination) System.out.println("Nodo: "+nodeList.get(action).getName()+", reward: " + reward);
        return reward;
    }

    private double grantReward3(int action, Task task){
        double reward = 0.0;

        //penalizzo il nodo se ha più task assegnati della media
        if(nodeList.get(action).getTasksQueue().size() == 0) reward +=1;
        else reward -= nodeList.get(action).getTasksQueue().size();

        if(printNodeDestination) System.out.println("Nodo: "+nodeList.get(action).getName()+", reward: " + reward);
        return reward;
    }

    protected void DoDQN(String[] architecture, Task task) {
        // Ottiene lo stato attuale basato sui nodi disponibili
        //double[] state = getCurrentState(nodeList, task);
        double[] state = getCurrentState4(nodeList, task);
    
        // Ciclo su tutte le possibili destinazioni per scegliere la migliore azione
        int action = chooseAction(state, architecture, task);
    
        // Esegui l'azione e ottieni lo stato successivo
        PerformAction(action, task);
        //double reward = grantReward(action, task);
        double reward = grantReward3(action, task);                                                             //NB: grantreward va con getcurrentstate, grantreward2 va con getcurrentstate2 e 3, grantreward3 va con getcurrentstate4
        //double[] nextState = getNextState(action, task);  // Ottieni lo stato successivo dopo l'azione
        double [] nextState = getCurrentState4(nodeList, task);

        //update variabili DQN
        if(!SimulationParameters.greedyTraining) epsilonUpdateCounter++;
        //else if(numberoftasksorchestrated > SimulationParameters.tasksForGreedyTraining) epsilonUpdateCounter++;
        //epsilonUpdateCounter++;

        if(replayBufferfUpdateCounter < Math.ceil(SimulationParameters.neuralNetworkLearningSpeed/2)) replayBufferfUpdateCounter++;

        // Aggiungi l'esperienza nel replay buffer
        replayBuffer.add(new Experience(state, action, reward, nextState, isDone()));
    
        // Aggiorna la rete neurale se il replay buffer ha abbastanza esperienze
        if (replayBuffer.size() > batchSize && replayBufferfUpdateCounter == Math.ceil(SimulationParameters.neuralNetworkLearningSpeed/2)){
            numberofreplayupdates++;
            replayBufferfUpdateCounter = 0;
            updateNetwork(batchSize);
        }
     
        if(printVMtaskUsage && simulationManager.getSimulation().clock()%580 == 0) {
            System.out.println("Epsilon: " + epsilon);
            printVMtaskUsage = false;
        }
        if(simulationManager.getSimulation().clock()%580 != 0)
            printVMtaskUsage = true;

        // Aggiorna epsilon per ridurre gradualmente l'esplorazione
        if (epsilonUpdateCounter == SimulationParameters.neuralNetworkLearningSpeed && epsilon > epsilonMin){
            numberofepsilonupdates++;
            epsilonUpdateCounter = 0;
            epsilon *= epsilonDecay;
        }
    
        // Aggiorna la rete target se necessario
        if (shouldUpdateTargetNetwork()) {        
            if(printNodeDestination) System.out.println("----------------------------------------------UPDATE FATTO----------------------------------------------");
            targetNetwork.setParams(qNetwork.params());
        }

        numberoftasksorchestrated++;

    }


    private boolean isDone() {
        // Definire la condizione di terminazione della simulazione
        return false;
    }

    private boolean shouldUpdateTargetNetwork() {
        // Definire la logica per determinare quando aggiornare la rete target
        targetUpdateCounter++;
        return targetUpdateCounter%(SimulationParameters.neuralNetworkLearningSpeed * 10) == 0;  // (neuralNetworkLearningSpeed * 10) iterazioni
        //return true;  
    }

    public void saveModel(String filePath) {
        try {
            String modelPath = filePath + "Q_Network.zip";
            File file = new File(modelPath);
            qNetwork.save(file, true); // true per includere anche l'updater, come Adam
            System.out.println("Modello salvato con successo in: " + filePath);
            modelPath = filePath + "Target_Network.zip";
            file = new File(modelPath);
            targetNetwork.save(file, true); // true per includere anche l'updater, come Adam
            System.out.println("Modello salvato con successo in: " + filePath);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Errore nel salvataggio del modello.");
        }
    }

    public void loadModel(String filePath) {
        try {
            String modelPath = filePath + "Q_Network.zip";
            File file = new File(modelPath);
            qNetwork = MultiLayerNetwork.load(file, true); // true per caricare anche l'updater
            System.out.println("Modello caricato con successo da: " + filePath);
            modelPath = filePath + "Target_Network.zip";
            file = new File(modelPath);
            targetNetwork = MultiLayerNetwork.load(file, true); // true per caricare anche l'updater
            System.out.println("Modello caricato con successo da: " + filePath);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Errore nel caricamento del modello.");
        }
    }

    public void saveReplayBuffer(ReplayBuffer replayBuffer, String filePath) {
        try {
            String replayPath = filePath + "/replay.ser";
            FileOutputStream fileOut = new FileOutputStream(replayPath);
            ObjectOutputStream out = new ObjectOutputStream(fileOut);
            out.writeObject(replayBuffer); // Serializza il replay buffer
            out.close();
            fileOut.close();
            System.out.println("Replay buffer salvato in " + replayPath);
        } catch (IOException i) {
            i.printStackTrace();
        }
    }

    public ReplayBuffer loadReplayBuffer(String filePath) {
        ReplayBuffer replayBuffer = null;
        try {
            String replayPath = filePath + "/replay.ser";
            FileInputStream fileIn = new FileInputStream(replayPath);
            ObjectInputStream in = new ObjectInputStream(fileIn);
            replayBuffer = (ReplayBuffer) in.readObject(); // Carica il replay buffer
            in.close();
            fileIn.close();
            System.out.println("Replay buffer caricato da " + replayPath);
        } catch (IOException | ClassNotFoundException i) {
            i.printStackTrace();
        }
        return replayBuffer;
    }

    public void saveEpsilon(String filePath) {
        try {
            String epsilonPath = filePath + "/Epsilon.json";
            JSONObject json = new JSONObject();
            json.put("epsilon", epsilon);

            FileWriter file = new FileWriter(epsilonPath);
            file.write(json.toString());
            file.flush();
            file.close();

            System.out.println("Epsilon salvato in: " + epsilonPath);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Errore nel salvataggio del valore di epsilon.");
        }
    }

    public double loadEpsilon(String filePath) {
        double epsilon = -1; // Valore di default, nel caso in cui qualcosa vada storto
        try {
            String epsilonPath = filePath + "/Epsilon.json";
            BufferedReader reader = new BufferedReader(new FileReader(epsilonPath));
            StringBuilder jsonContent = new StringBuilder();
            String line;
            
            // Legge tutto il contenuto del file JSON
            while ((line = reader.readLine()) != null) {
                jsonContent.append(line);
            }
            reader.close();

            // Crea un oggetto JSON dal contenuto del file
            JSONObject json = new JSONObject(jsonContent.toString());

            // Estrae il valore di epsilon
            epsilon = json.getDouble("epsilon");
            System.out.println("Epsilon caricato dal file: " + epsilon);

        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Errore nel caricamento del file epsilon.");
        }

        return epsilon;
    }

    @Override
    public void onSimulationEnd() {
        for(int i = 0; i < nodeList.size(); i++){
            System.out.println("Nodo " + nodeList.get(i).getName());
            System.out.println("    tasks offloaded: " + nodeList.get(i).getSentTasks());
            System.out.println("    tasks orchestrated: " + super.historyMap.get(i));
        }
        //System.out.println("Tasks orchestrated: " + numberoftasksorchestrated + " , epsilon updates: " + numberofepsilonupdates + " , replay updates: " + numberofreplayupdates);

        if ("DQN".equals(algorithmName) && saveThisModel) {
            int response = JOptionPane.showConfirmDialog(null, "Vuoi salvare il modello?", "Salva modello", JOptionPane.YES_NO_OPTION);
            
            if (response == JOptionPane.YES_OPTION) {
                String modelPath = SimulationParameters.settingspath + SimulationParameters.simName + "/dqn_model_" + SimulationParameters.simName;
                saveModel(modelPath);
                modelPath = SimulationParameters.settingspath + SimulationParameters.simName;
                saveReplayBuffer(replayBuffer, modelPath);
                saveEpsilon(modelPath);
            }
        }

    }
    

    

}