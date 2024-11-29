/**
 *     PureEdgeSim:  A Simulation Framework for Performance Evaluation of Cloud, Edge and Mist Computing Environments 
 *
 *     This file is part of PureEdgeSim Project.
 *
 *     PureEdgeSim is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     PureEdgeSim is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with PureEdgeSim. If not, see <http://www.gnu.org/licenses/>.
 *     
 *     @author Charafeddine Mechalikh
 **/
package com.mechalikh.pureedgesim.taskorchestrator;

import java.util.LinkedHashMap;
import java.util.Map;

import org.jgrapht.GraphPath;

import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode;
import com.mechalikh.pureedgesim.network.NetworkLink;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.simulationengine.Event;
import com.mechalikh.pureedgesim.simulationengine.OnSimulationEndListener;
import com.mechalikh.pureedgesim.simulationmanager.SimulationManager;
import com.mechalikh.pureedgesim.taskgenerator.Container;
import com.mechalikh.pureedgesim.taskgenerator.Task;

public class DefaultOrchestrator extends Orchestrator implements OnSimulationEndListener{
	public Map<Integer, Integer> historyMap = new LinkedHashMap<>();

	public DefaultOrchestrator(SimulationManager simulationManager) {
		super(simulationManager);
		// Initialize the history map
		for (int i = 0; i < nodeList.size(); i++)
			historyMap.put(i, 0);
	}

	//Questa funzione associa il task alla VM che contiene il container associato al task. (che al mercato mio padre comprò)
	public int findVmAssociatedWithTask(Task task){
		//se l'applicazione non è shared
		if(!SimulationParameters.applicationList.get(task.getApplicationID()).getSharedContainer()){
			//Ciclo tra tutti i container
			for(Container container : containerList){
				//se il nome dell'app associata al task ==  a quello associato al container
				if(task.getAssociatedAppName().equals(container.getAssociatedAppName())){
					//ciclo tra tutti gli edge device di quel container
					for(ComputingNode edgeDevice : container.getEdgeDevices()){
						//se il dispositivo che ha generato il container == dispositivo che ha generato il task
						if(task.getEdgeDevice().equals(edgeDevice))
							//prelevo la VM associata a quel container
							for(int i = 0; i < nodeList.size(); i++)
								if(nodeList.get(i).equals(container.getPlacementDestination()))
									return i;	
					}
				}
			}
		}
		//se invece è shared
		else{
			//applico l'algoritmo specificato nel SimulationParameters.xml
			if(algorithmName.equals("ROUND_ROBIN")) 
				return roundRobin(task);
			else if(algorithmName.equals("DISTANCE_ROUND_ROBIN")) 
				return distanceRoundRobin(task);
			else if(algorithmName.equals("LATENCY_ROUND_ROBIN")) 
				return latencyRoundRobin(task);	
		}
		return -1;
	}

	//round robin per i container shared
	public int roundRobin(Task task){
		int selected = -1;
		int minTasksCount = -1; // Computing node with minimum assigned tasks.
		for (int i = 0; i < containerList.size(); i++) {
			if(containerList.get(i).getAssociatedAppName().equals(task.getAssociatedAppName())
											&& 
				(minTasksCount == -1 || minTasksCount > sharedHistoryMap.get(i))
			  ) 
			{
				minTasksCount = sharedHistoryMap.get(i);
				// if this is the first time,
				// or new min found, so we choose it as the best computing node.
				selected = i;
			}
		}
		// Assign the tasks to the obtained computing node.
		sharedHistoryMap.put(selected, minTasksCount + 1);

		//trovato il container nella lista devo trovare la VM ad esso associato.
		for(int i = 0; i < nodeList.size(); i++)
			if(nodeList.get(i).equals(containerList.get(selected).getPlacementDestination()))
				return i;

		//caso nel quale non trovo la VM (non può verificarsi)
		return -1;
	}

	//seleziono il device più vicino sul quale è piazzato il container shared. Se ve ne sono vari scelgo quello col numero di task inferiore (roundRobin)
	public int distanceRoundRobin(Task task){
		int selected = -1;
		int minTasksCount = -1; // Computing node with minimum assigned tasks.
		double minLen = Double.MAX_VALUE;
		for (int i = 0; i < containerList.size(); i++) {
			//caso nel quale la distanza è inferiore, quindi prendo a prescindere il nodo più vicino. 
			if(containerList.get(i).getAssociatedAppName().equals(task.getAssociatedAppName())
			&& (minLen > containerList.get(i).getPlacementDestination().getMobilityModel().distanceTo(task.getEdgeDevice()))
			//controllo che tanto l'edgeDevice quanto il nodo destinazione afferiscano allo stesso DataCenter														decommentare se si vuole usarla in modalità "latency"
			//&& (task.getEdgeDevice().getEdgeOrchestrator().equals(containerList.get(i).getPlacementDestination().getEdgeOrchestrator()))
			) {
				minTasksCount = sharedHistoryMap.get(i);
				// if this is the first time,
				// or new min found, so we choose it as the best computing node.
				selected = i;
				minLen = containerList.get(i).getPlacementDestination().getMobilityModel().distanceTo(task.getEdgeDevice());
			}
			//caso nel quale la distanza è uguale e quindi vedo se questo nodo ha meno container assegnati. Nel caso scelgo questo
			else if(containerList.get(i).getAssociatedAppName().equals(task.getAssociatedAppName())
			&& (minLen == containerList.get(i).getPlacementDestination().getMobilityModel().distanceTo(task.getEdgeDevice()))
			//controllo che tanto l'edgeDevice quanto il nodo destinazione afferiscano allo stesso DataCenter														decommentare se si vuole usarla in modalità "latency"
			//&& (task.getEdgeDevice().getEdgeOrchestrator().equals(containerList.get(i).getPlacementDestination().getEdgeOrchestrator()))
			&& (minTasksCount == -1 || minTasksCount > sharedHistoryMap.get(i))){
				minTasksCount = sharedHistoryMap.get(i);
				// if this is the first time,
				// or new min found, so we choose it as the best computing node.
				selected = i;
				minLen = containerList.get(i).getPlacementDestination().getMobilityModel().distanceTo(task.getEdgeDevice());
			  }
		}
		// Assign the tasks to the obtained computing node.
		sharedHistoryMap.put(selected, minTasksCount + 1);

		//trovato il container nella lista devo trovare la VM ad esso associato.
		for(int i = 0; i < nodeList.size(); i++)
			if(nodeList.get(i).equals(containerList.get(selected).getPlacementDestination()))
				return i;

		//caso nel quale non trovo la VM (non può verificarsi)
		return -1;
	}

	//seleziono il device con migliore latency sul quale è piazzato il container shared. Se ve ne sono vari scelgo quello col numero di task inferiore (roundRobin)
	public int latencyRoundRobin(Task task){
		int selected = -1;
		int minTasksCount = -1; // Computing node with minimum assigned tasks.
		double minLatency = Double.MAX_VALUE;
		for (int i = 0; i < containerList.size(); i++) {
			ComputingNode from = task.getEdgeDevice();
			ComputingNode to = containerList.get(i).getPlacementDestination();
			long id = simulationManager.getDataCentersManager().getTopology().getUniqueId(from.getId(), to.getId());
			GraphPath<ComputingNode, NetworkLink> path;
			if(simulationManager.getDataCentersManager().getTopology().getPathsMap().containsKey(id))
				path = simulationManager.getDataCentersManager().getTopology().getPathsMap().get(id); 
			else{ 
				path = simulationManager.getDataCentersManager().getTopology().getPath(from, to);
				simulationManager.getDataCentersManager().getTopology().getPathsMap().put(id, path);
			}
			double thisLatency = path.getWeight();
			//caso nel quale la latency è inferiore, quindi prendo a prescindere il nodo più migliore. 
			if(containerList.get(i).getAssociatedAppName().equals(task.getAssociatedAppName())
			&& (minLatency > thisLatency)) {
				minTasksCount = sharedHistoryMap.get(i);
				// if this is the first time,
				// or new min found, so we choose it as the best computing node.
				selected = i;
				minLatency = thisLatency;
			}
			//caso nel quale la latency è uguale e quindi vedo se questo nodo ha meno container assegnati. Nel caso scelgo questo
			else if(containerList.get(i).getAssociatedAppName().equals(task.getAssociatedAppName())
			&& (minLatency == thisLatency)
			&& (minTasksCount == -1 || minTasksCount > sharedHistoryMap.get(i))){
				minTasksCount = sharedHistoryMap.get(i);
				// if this is the first time,
				// or new min found, so we choose it as the best computing node.
				selected = i;
				minLatency = thisLatency;
			  }
		}
		// Assign the tasks to the obtained computing node.
		sharedHistoryMap.put(selected, minTasksCount + 1);

		//trovato il container nella lista devo trovare la VM ad esso associato.
		for(int i = 0; i < nodeList.size(); i++)
			if(nodeList.get(i).equals(containerList.get(selected).getPlacementDestination()))
				return i;

		//caso nel quale non trovo la VM (non può verificarsi)
		return -1;
	}

	@Override
	public void resultsReturned(Task task) {
		// Do something with the task that has been finished

	}
 
	@Override
	public void processEvent(Event e) {
		// Process the scheduled events, if any.
		
	}

	@Override
	public void notifyOrchestratorOfTaskExecution(Task task) {
		
	}

	@Override
	public void onSimulationEnd() {

		// Map<ComputingNode, Integer> mappainutile = new LinkedHashMap<>();
		// for(ComputingNode cn :  simulationManager.getDataCentersManager().getComputingNodesGenerator().getEdgeOnlyList())
		// 	mappainutile.put(cn, 0);

		// //ciclo tra tutte le VM
		// for(ComputingNode cn : nodeList){
		// 	System.out.println("task eseguiti dal device " + cn.getName() + ": " + cn.getSentTasks());
		// }

		// //ciclo tra tutti gli edgeDevice
		// for(ComputingNode cn2 : simulationManager.getDataCentersManager().getComputingNodesGenerator().getMistOnlyList()){
		// 	ComputingNode bestNode = null;
		// 	double minDistance = Double.MAX_VALUE;
		// 	//ciclo tra tutti i DataCenter
		// 	for(ComputingNode cn :  simulationManager.getDataCentersManager().getComputingNodesGenerator().getEdgeOnlyList()){
		// 		if(cn2.getMobilityModel().distanceTo(cn) < minDistance){
		// 			bestNode = cn;
		// 			minDistance = cn2.getMobilityModel().distanceTo(cn);
		// 		}
		// 	}
		// 	mappainutile.put(bestNode, mappainutile.get(bestNode) + 1);
		// }

		// for(ComputingNode cn :  simulationManager.getDataCentersManager().getComputingNodesGenerator().getEdgeOnlyList())
		// 	System.out.println("DC " + cn.getName() + ", device associati " + mappainutile.get(cn));
	}

}
