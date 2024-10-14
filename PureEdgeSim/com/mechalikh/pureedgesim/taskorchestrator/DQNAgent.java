package com.mechalikh.pureedgesim.taskorchestrator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.swing.JOptionPane;

import org.apache.commons.lang3.tuple.Pair;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.json.JSONObject;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.learning.config.Adam;
import org.nd4j.linalg.lossfunctions.LossFunctions;

import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.simulationmanager.SimulationManager;
import com.mechalikh.pureedgesim.taskgenerator.Task;

public class DQNAgent {
    
    public MultiLayerNetwork qNetwork;
    public MultiLayerNetwork targetNetwork;
    private ReplayBuffer replayBuffer;

    // Parametri DQN
    private double epsilon = SimulationParameters.epsilon;
    private double epsilonMin = SimulationParameters.epsilonMin;
    private double epsilonDecay = SimulationParameters.epsilonDecay;
    private double gamma = SimulationParameters.gamma;
    private double learningRate = SimulationParameters.learningRate;
    private int batchSize = SimulationParameters.networkBatchSize;
    private int replayMemory = 1000000;

    //variabili per esecuzione
    private int replayBufferfUpdateCounter = 0;
    private int epsilonUpdateCounter = 0;
    private int targetUpdateCounter = 0;
	private List<ComputingNode> nodeList = new ArrayList<>();
    private CustomOrchestrator simOrchestrator;
    private SimulationManager simulationManager;

    //questi servono solo per printare debug
    private boolean printNodeDestination = false;
    private boolean printVMtaskUsage = false;
    private int totalReward = 0;

    //per iniziare la simulazione da zero
    public DQNAgent(CustomOrchestrator orch, SimulationManager sm) {
        replayBuffer = new ReplayBuffer(replayMemory);
        simOrchestrator = orch;
        simulationManager = sm;
        nodeList = simOrchestrator.getNodeList();
        qNetwork = createNetwork();
        targetNetwork = createNetwork();
    }

    //per recuperare un agente e un replayBuffer precedente
    public DQNAgent(String pathToNetwork, String pathToRBuff, CustomOrchestrator orch, SimulationManager sm) {
        replayBuffer = loadReplayBuffer(pathToRBuff);
        //epsilon = loadEpsilon(pathToRBuff);
        simOrchestrator = orch;
        simulationManager = sm;
        nodeList = simOrchestrator.getNodeList();
        loadModel(pathToNetwork);
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

    private int getActionSize() {
        // Il numero di azioni corrisponde al numero di nodi disponibili
        return nodeList.size();  // Restituisce il numero di nodi di calcolo disponibili
    } 

    private int getStateSize() {
        return 4*nodeList.size() + 2;           //modificare se si passa alla versione con epsilon e clock
    }

    public int chooseAction(double[] state, String[] architecture, Task task) {
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
                if (simOrchestrator.offloadingIsPossible(task, nodeList.get(choice), simOrchestrator.getArchitectureLayers())) {
                    if(printNodeDestination) System.out.println("nodo: " + nodeList.get(choice).getName() + ", Dimensionequeue = " + nodeList.get(choice).getTasksQueue().size() + ", epsilon = " + epsilon);  
                    return choice;
                }
                k++;
            }
            return 0;       //non può verificarsi 
        }
    }

    private int randChoice(String[] architecture, Task task){
        Random rand = new Random();
        while(true){
            int random = rand.nextInt(getActionSize());
            if(simOrchestrator.offloadingIsPossible(task, nodeList.get(random), simOrchestrator.getArchitectureLayers()))
                return random;
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

        return qValues.get(k).getValue();
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

    private double grantReward(Task task){
        double reward = 0.0;

        //penalizzo il nodo se ha più task assegnati della media
        if(task.getStatus().equals(com.mechalikh.pureedgesim.taskgenerator.Task.Status.SUCCESS)) reward += 1;
        else if(task.getStatus().equals(com.mechalikh.pureedgesim.taskgenerator.Task.Status.FAILED))reward -=  1;

        if(printNodeDestination) System.out.println("Nodo: "+nodeList.get(task.getAction()).getName()+", reward: " + reward);
        this.totalReward += reward;
        return reward;
    }

    public void DQN(Task task, boolean isDone){

        double reward = grantReward(task);                                                             

        //update variabili DQN
        if(!SimulationParameters.greedyTraining) epsilonUpdateCounter++;

        if(replayBufferfUpdateCounter < Math.ceil(SimulationParameters.neuralNetworkLearningSpeed/2)) replayBufferfUpdateCounter++;

        // Aggiungi l'esperienza nel replay buffer
        replayBuffer.add(new Experience(task.getCurrentState(), task.getAction(), reward, task.getNextState(), isDone));
    
        // Aggiorna la rete neurale se il replay buffer ha abbastanza esperienze
        if (replayBuffer.size() > batchSize && replayBufferfUpdateCounter == Math.ceil(SimulationParameters.neuralNetworkLearningSpeed/2)){
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
            epsilonUpdateCounter = 0;
            epsilon *= epsilonDecay;
        }
    
        // Aggiorna la rete target se necessario
        if (shouldUpdateTargetNetwork()) {        
            if(printNodeDestination) System.out.println("----------------------------------------------UPDATE FATTO----------------------------------------------");
            targetNetwork.setParams(qNetwork.params());
        }

    }

    private boolean shouldUpdateTargetNetwork() {
        // Definire la logica per determinare quando aggiornare la rete target
        targetUpdateCounter++;
        return targetUpdateCounter%(SimulationParameters.neuralNetworkLearningSpeed * 10) == 0;  // (neuralNetworkLearningSpeed * 10) iterazioni
    }

    public void saveAll(){
        // int response = JOptionPane.showConfirmDialog(null, "Vuoi salvare il modello?", "Salva modello", JOptionPane.YES_NO_OPTION);
            
        // if (response == JOptionPane.YES_OPTION) {
            String modelPath = SimulationParameters.settingspath + SimulationParameters.simName + "/" + simulationManager.getScenario().getStringOrchArchitecture();
            saveModel(modelPath + "/dqn_model_" +  SimulationParameters.simName);
            saveEpsilon(modelPath);
            saveReplayBuffer(replayBuffer, modelPath);
        //}
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
            qNetwork = MultiLayerNetwork.load(file, false); // true per caricare anche l'updater
            System.out.println("Modello caricato con successo da: " + filePath);
            modelPath = filePath + "Target_Network.zip";
            file = new File(modelPath);
            targetNetwork = MultiLayerNetwork.load(file, false); // true per caricare anche l'updater
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

    public void IterationEnd() {
        for(int i = 0; i < nodeList.size(); i++){
            System.out.println("Nodo " + nodeList.get(i).getName());
            System.out.println("    tasks offloaded: " + nodeList.get(i).getSentTasks());
            System.out.println("    tasks orchestrated: " + simOrchestrator.historyMap.get(i));
            System.out.println("    totalReward: " + totalReward);
        }

    }
    
}
