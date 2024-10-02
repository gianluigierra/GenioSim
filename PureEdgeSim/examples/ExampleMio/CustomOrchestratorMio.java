package examples.ExampleMio;

import java.util.LinkedHashMap;
import java.util.Map;

import com.mechalikh.pureedgesim.taskorchestrator.DefaultOrchestrator;
import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.simulationengine.Event;
import com.mechalikh.pureedgesim.simulationmanager.SimulationManager;
import com.mechalikh.pureedgesim.taskgenerator.Task;
import java.util.Random;

public class CustomOrchestratorMio extends DefaultOrchestrator {

	protected Map<Integer, Integer> historyMap = new LinkedHashMap<>();

	public CustomOrchestratorMio(SimulationManager simulationManager) {
		super(simulationManager);
		// Initialize the history map
		for (int i = 0; i < nodeList.size(); i++)
			historyMap.put(i, 0);
	}

	protected int findComputingNode(String[] architecture, Task task) {
		if ("RANDOM".equals(algorithmName)) {
			return RANDOM(architecture, task);
		} else if ("RANDOM_PURO".equals(algorithmName)) {
			return RANDOM_PURO(architecture, task);
		} else if ("SOLO_PRIMO".equals(algorithmName)) {
			return SOLO_PRIMO(architecture, task);
		} else if ("ROUND_ROBIN".equals(algorithmName)) {
			return roundRobin(architecture, task);
		} else if ("TRADE_OFF".equals(algorithmName)) {
			return tradeOff(architecture, task);
		} else {
			throw new IllegalArgumentException(getClass().getSimpleName() + " - Unknown orchestration algorithm '"
					+ algorithmName + "', please check the simulation parameters file...");
		}
	}

	protected int tradeOff(String[] architecture, Task task) {
		int selected = -1;
		double min = -1;
		double newMin;// the computing node with minimum weight;
		ComputingNode node; // get best computing node for this task
		for (int i = 0; i < nodeList.size(); i++) {
			node = nodeList.get(i);
			if (offloadingIsPossible(task, node, architecture)) {
				// the weight below represent the priority, the less it is, the more it is //
				// suitable for offlaoding, you can change it as you want
				double weight = 1.2;
				// // this is an
				// edge server 'cloudlet', the latency is slightly high then edge // devices
				if (node.getType() == SimulationParameters.TYPES.CLOUD) {
					weight = 1.8; // this
					// is the cloud, it consumes more energy and results in high latency, so //
					// better to avoid it
				} else if (node.getType() == SimulationParameters.TYPES.EDGE_DEVICE) {
					weight = 1.3;// this is an edge
					// device, it results in an extremely low latency, but may // consume more
					// energy.
				}
				newMin = (historyMap.get(i) + 1) * weight * task.getLength() / node.getMipsPerCore();
				if (min == -1 || min > newMin) { // if it is the first
					// iteration, or if this computing node has more // cpu mips and // less waiting
					// tasks
					min = newMin; // set the first computing node as the best one
					selected = i;
				}
			}
		}
		if (selected != -1)
			historyMap.put(selected, historyMap.get(selected) + 1); // assign the tasks to the selected computing node
		return selected;
	}

	public int roundRobin(String[] architecture, Task task) {
		int selected = -1;
		int minTasksCount = -1; // Computing node with minimum assigned tasks.
		for (int i = 0; i < nodeList.size(); i++) {
			if (offloadingIsPossible(task, nodeList.get(i), architecture)
					&& (minTasksCount == -1 || minTasksCount > historyMap.get(i))) {
				minTasksCount = historyMap.get(i);
				// if this is the first time,
				// or new min found, so we choose it as the best computing node.
				selected = i;
			}
		}
		// Assign the tasks to the obtained computing node.
		historyMap.put(selected, minTasksCount + 1);

		return selected;
	}

	public int RANDOM(String[] architecture, Task task) {
		Random random = new Random();
		int rnd = random.nextInt(nodeList.size());
		if (offloadingIsPossible(task, nodeList.get(rnd), architecture)){
			historyMap.put(rnd, historyMap.get(rnd) + 1); // assign the tasks to the selected computing node
			return rnd;
		}
		
		return -1;
	}

	public int RANDOM_PURO(String[] architecture, Task task) {
		Random random = new Random();
		int count = 0;
		if(arrayContains(architectureLayers, "Cloud")) count++;
		if(arrayContains(architectureLayers, "Edge")) count++;
		if(arrayContains(architectureLayers, "Mist")) count++;
		int rnd = random.nextInt(count);
		if(rnd != 2){
			if (offloadingIsPossible(task, nodeList.get(rnd), architecture)) {	
				historyMap.put(rnd, historyMap.get(rnd) + 1); 
				return rnd;
			}
		}
		else if(rnd == 2){
			int selected = -1;
			int minTasksCount = -1; // Computing node with minimum assigned tasks.
			for (int i = 2; i < nodeList.size(); i++) {
				if (offloadingIsPossible(task, nodeList.get(i), architecture)
						&& (minTasksCount == -1 || minTasksCount > historyMap.get(i))) {
					minTasksCount = historyMap.get(i);
					// if this is the first time,
					// or new min found, so we choose it as the best computing node.
					selected = i;
				}
			}
			// Assign the tasks to the obtained computing node.
			historyMap.put(selected, minTasksCount + 1);
			return selected;
		}
		return -1;
	}
	
	public int SOLO_PRIMO(String[] architecture, Task task) {
		int selected = -1;
		if(offloadingIsPossible(task, nodeList.get(0), architecture)) selected = 0;
		if (selected != -1)
			historyMap.put(selected, historyMap.get(selected) + 1); // assign the tasks to the selected computing node
		return selected;
	}

	@Override
	public void resultsReturned(Task task) {
		// Do something with the task that has been finished

	}
 
	@Override
	public void processEvent(Event e) {
		// Process the scheduled events, if any.
		
	}

}
