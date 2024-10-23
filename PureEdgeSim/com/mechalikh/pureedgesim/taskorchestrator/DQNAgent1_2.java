package com.mechalikh.pureedgesim.taskorchestrator;

//import di esecuzione/tipi
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

//import per deeplearning
import org.apache.commons.lang3.tuple.Pair;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.learning.config.Adam;
import org.nd4j.linalg.lossfunctions.LossFunctions;

//import di simulazione
import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.simulationmanager.SimulationManager;
import com.mechalikh.pureedgesim.taskgenerator.Task;

public class DQNAgent1_2 extends DQNAgentAbstract{
    
    // Oggetti DQN
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
    private int neuralNetworkSize = 64;
    private int replayMemory = 100000;

    //variabili per esecuzione
    private int epsilonUpdateCounter = 0;
    private int targetUpdateCounter = 0;
    private CustomOrchestrator simOrchestrator;
    private SimulationManager simulationManager;
    private String modelPath;

    //questi servono solo per printare debug
    private boolean printNodeDestination = false;
    private int printEpsilon = 1;
    private boolean doPrintEpsilon = SimulationParameters.neuralTraining;
    private int totalReward = 0;

    //per iniziare la simulazione da zero
    public DQNAgent1_2(CustomOrchestrator orch, SimulationManager sm) {
        replayBuffer = new ReplayBuffer(replayMemory);
        simOrchestrator = orch;
        simulationManager = sm;
        qNetwork = createNetwork();
        targetNetwork = createNetwork();
    }

    //per recuperare un agente
    public DQNAgent1_2(String pathToNetwork, CustomOrchestrator orch, SimulationManager sm) {
        replayBuffer = new ReplayBuffer(replayMemory);
        modelPath = pathToNetwork;
        simOrchestrator = orch;
        simulationManager = sm;
        loadModel(pathToNetwork);
    }
    
    private MultiLayerNetwork createNetwork() {
        int inputSize = getStateSize();
        int outputSize = getActionSize();

        MultiLayerNetwork network = new MultiLayerNetwork(new NeuralNetConfiguration.Builder()
            .updater(new Adam(learningRate))
            .seed(1234)
            .list()
            .layer(new DenseLayer.Builder().nIn(inputSize).nOut(neuralNetworkSize)
                    .activation(Activation.RELU)
                    .weightInit(WeightInit.XAVIER)
                    .build())
            .layer(new DenseLayer.Builder().nIn(neuralNetworkSize).nOut(neuralNetworkSize)
                    .activation(Activation.RELU)
                    .weightInit(WeightInit.XAVIER)
                    .build())
            .layer(new DenseLayer.Builder().nIn(neuralNetworkSize).nOut(neuralNetworkSize)
                    .activation(Activation.RELU)
                    .weightInit(WeightInit.XAVIER)
                    .build())
            .layer(new OutputLayer.Builder(LossFunctions.LossFunction.MSE)
                    .activation(Activation.IDENTITY)
                    .nIn(neuralNetworkSize).nOut(outputSize)
                    .build())
            .build());

        network.init();
        return network;
    }

    private int getActionSize() {
        // Il numero di azioni corrisponde al numero di nodi disponibili
        return simOrchestrator.nodeList.size();  // Restituisce il numero di nodi di calcolo disponibili
    } 

    private int getStateSize() {
        return 6*simOrchestrator.nodeList.size() + 2;           
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
            while(k < simOrchestrator.nodeList.size()){
                int choice = getKthBestQAction(state, k); // Change the 1 to 2, 3, etc., for second, third best, etc.
                if (simOrchestrator.offloadingIsPossible(task, simOrchestrator.nodeList.get(choice), simOrchestrator.getArchitectureLayers())) {
                    if(printNodeDestination) System.out.println("nodo: " + simOrchestrator.nodeList.get(choice).getName() + ", Dimensionequeue = " + simOrchestrator.nodeList.get(choice).getTasksQueue().size() + ", epsilon = " + epsilon);  
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
            if(simOrchestrator.offloadingIsPossible(task, simOrchestrator.nodeList.get(random), simOrchestrator.getArchitectureLayers()))
                return random;
        }
    }

    public int getKthBestQAction(double[] state, int k) {
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

        // // Reshape the input to be a 2D array with shape [1, state.length]
        // INDArray input = Nd4j.create(state).reshape(1, state.length);
        // INDArray output = qNetwork.output(input);
        // return Nd4j.argMax(output).getInt(0);
    }

    private void updateNetwork(int batchSize) {
        List<Experience> batch = replayBuffer.sample(batchSize);  // Campionamento del buffer di replay
    
        //facciamo sta prova:
        INDArray inputs = Nd4j.create(batchSize, getStateSize());  // Array per tutti gli input del batch
        INDArray targets = Nd4j.create(batchSize, getActionSize()); // Array per tutti i target del batch

        int i = 0;
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

            inputs.putRow(i, input);  // Aggiungi lo stato all'array di input
            targets.putRow(i, target); // Aggiungi il target all'array di target

            //qNetwork.fit(input, target);

            i++;
        }
        
        // Addestra la rete con l'array di stati correnti e i valori target aggiornati
        qNetwork.fit(inputs, targets);
    }

    private boolean isStationary(){
        for(ComputingNode cn : simOrchestrator.nodeList)
            if(cn.getAvgCpuUtilization() == 0) return true;
        return false;
    }

    public double grantReward(Task task){
        double reward = 0.0;

        //penalizzo il nodo se ha più task assegnati della media
        if(task.getStatus().equals(com.mechalikh.pureedgesim.taskgenerator.Task.Status.SUCCESS)) reward += 1;
        else reward -= 1;

        //penalizzo il nodo se esistono VM che hanno cpuUtilization pari a zero
        if(isStationary() && simulationManager.getSimulation().clockInMinutes() > 0.7) reward -= 10;

        if(printNodeDestination) System.out.println("Nodo: "+simOrchestrator.nodeList.get(task.getAction()).getName()+", reward: " + reward);                                                            

        //aggiorno il counter della epsilon
        epsilonUpdateCounter++;

        this.totalReward += reward;

        return reward;
    }

    public void DQN(Task task, boolean isDone){

        double reward = grantReward(task); 

        // Aggiungi l'esperienza nel replay buffer
        replayBuffer.add(new Experience(task.getCurrentState(), task.getAction(), reward, task.getNextState(), isDone));

        if (replayBuffer.size() > batchSize){
            updateNetwork(batchSize);
        }
     
        //printa la epsilon ogni 5% della simulazione
        if(simulationManager.getSimulation().clock()>(SimulationParameters.simulationDuration/20*printEpsilon) && doPrintEpsilon) {
            System.out.print("# Epsilon: " + (int) (epsilon*100) + "% #");
            printEpsilon++;
        }

        //aggiorna la epsilon
        if (epsilonUpdateCounter == Math.ceil(SimulationParameters.neuralNetworkLearningSpeed/20) && epsilon > epsilonMin){      
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
        targetUpdateCounter++;
        return targetUpdateCounter%SimulationParameters.neuralNetworkLearningSpeed == 0;  //aggiorno ogni neuralNetworkLearningSpeed iterazioni
    }

    public void saveAll(){
        saveModel(modelPath);
    }

    public void saveModel(String filePath) {
        try {
            String modelPath = filePath + "Q_Network.zip";
            File file = new File(modelPath);
            qNetwork.save(file, SimulationParameters.neuralTraining); // true per includere anche l'updater, come Adam
            System.out.println("Modello salvato con successo in: " + filePath);
            modelPath = filePath + "Target_Network.zip";
            file = new File(modelPath);
            targetNetwork.save(file, SimulationParameters.neuralTraining); // true per includere anche l'updater, come Adam
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
            qNetwork = MultiLayerNetwork.load(file, SimulationParameters.neuralTraining); // true per caricare anche l'updater
            System.out.println("Modello caricato con successo da: " + filePath);
            modelPath = filePath + "Target_Network.zip";
            file = new File(modelPath);
            targetNetwork = MultiLayerNetwork.load(file, SimulationParameters.neuralTraining); // true per caricare anche l'updater
            System.out.println("Modello caricato con successo da: " + filePath);
        } catch (IOException e) {
            //e.printStackTrace();
            System.out.println("Errore nel caricamento del modello.");
            qNetwork = createNetwork();
            targetNetwork = createNetwork();
            System.out.println("Modello creato da zero");
        }
    }

    public int getTotalReward(){
        return totalReward;
    }

    public void IterationEnd() {
        for(int i = 0; i < simOrchestrator.nodeList.size(); i++){
            System.out.println("Nodo " + simOrchestrator.nodeList.get(i).getName());
            System.out.println("    tasks offloaded: " + simOrchestrator.nodeList.get(i).getSentTasks());
            System.out.println("    tasks orchestrated: " + simOrchestrator.historyMap.get(i));
        }

    }
    
}
