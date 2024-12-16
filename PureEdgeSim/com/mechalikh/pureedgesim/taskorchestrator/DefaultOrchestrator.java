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

import java.util.ArrayList;

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

	public DefaultOrchestrator(SimulationManager simulationManager) {
		super(simulationManager);
		// Initialize the tasks history map
		for (int i = 0; i < nodeList.size(); i++){
			tasksHistoryMap.put(i, new ArrayList<Task>());
		}
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
								if(nodeList.get(i).equals(container.getPlacementDestination())){
									//aggiungo il taks alla historyMap dei task associati a quel nodo
									tasksHistoryMap.get(i).add(task);
									return i;	
								}
					}
				}
			}
		}
		//se invece è shared
		else{
			//applico l'algoritmo specificato nel SimulationParameters.xml
			if(algorithmName.equals("ROUND_ROBIN")) 
				return roundRobin(task);
			else if(algorithmName.equals("BEST_LATENCY")) 
				return latencyRoundRobin(task);	
			else if(algorithmName.equals("BEST_DELAY")) 
				return bestDelay(task);	
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

		//determino il nodo associato a quel container
		int returnValue = findNodeAssociatedWithContainer(selected);
		//aggiungo il task alla historyMap dei task assegnati a quel nodo
		tasksHistoryMap.get(returnValue).add(task);

		return returnValue;
	}

	//seleziono il device con migliore latency sul quale è piazzato il container shared.
	//prendo il migliore in termini di latency dovuta solo alla rete. Se vi sono più nodi con la stessa latency allora applico roundRobin.
	public int latencyRoundRobin(Task task){
		int selected = -1;
		int minTasksCount = -1; // Computing node with minimum assigned tasks.
		double minLatency = Double.MAX_VALUE;
		for (int i = 0; i < containerList.size(); i++) {
			if(containerList.get(i).getAssociatedAppName().equals(task.getAssociatedAppName())){
				ComputingNode from = task.getEdgeDevice();
				ComputingNode to = containerList.get(i).getPlacementDestination();
				double thisLatency = getPathLatencyFromDeviceToOffloadingDestination(task, from, to);
				//caso nel quale la latency è inferiore, quindi prendo a prescindere il nodo migliore. 
				if((minLatency > thisLatency)) {
					minTasksCount = sharedHistoryMap.get(i);
					// if this is the first time,
					// or new min found, so we choose it as the best computing node.
					selected = i;
					minLatency = thisLatency;
				}
				//caso nel quale la latency è uguale e quindi vedo se questo nodo ha meno container assegnati. Nel caso scelgo questo
				else if((minLatency == thisLatency)
				&& (minTasksCount == -1 || minTasksCount > sharedHistoryMap.get(i))){
					minTasksCount = sharedHistoryMap.get(i);
					// if this is the first time,
					// or new min found, so we choose it as the best computing node.
					selected = i;
					minLatency = thisLatency;
				}
			}
		}
		// Assign the tasks to the obtained computing node.
		sharedHistoryMap.put(selected, minTasksCount + 1);

		//determino il nodo associato a quel container
		int returnValue = findNodeAssociatedWithContainer(selected);
		//aggiungo il task alla historyMap dei task assegnati a quel nodo
		tasksHistoryMap.get(returnValue).add(task);

		return returnValue;
	}
	
	//seleziono il device con migliore delay sul quale è piazzato il container shared.
	//seleziono il device migliore in termini di latency dovuta tanto alla rete quanto all'esecuzione dei task
	public int bestDelay(Task task){
		int selected = -1;
		int minTasksCount = -1; // Computing node with minimum assigned tasks.
		double bestLatency = Double.MAX_VALUE;
		//ciclo tra tutti i container shared
		for(int i = 0; i < containerList.size(); i++){
			//se il container è associato alla stessa app che ha generato il task
			if(containerList.get(i).getAssociatedAppName().equals(task.getAssociatedAppName())){
				//determino il path tra device e Destinazione
				ComputingNode from = task.getEdgeDevice();
				ComputingNode to = containerList.get(i).getPlacementDestination();
				double pathLatency = getPathLatencyFromDeviceToOffloadingDestination(task, from, to);
				//determino la latency approssimativa associata alle esecuzioni dei task presenti sul nodo
				double tasksLatency = 0;
				for(Task containerTask : tasksHistoryMap.get(findNodeAssociatedWithContainer(i))){
					tasksLatency += containerTask.getLength()/to.getMipsPerCore();
				}
				//aggiungo anche la latency associata all'esecuzione di questo task
				tasksLatency += task.getLength()/to.getMipsPerCore();
				//divido per il numero di core della destinazione
				tasksLatency = tasksLatency/to.getNumberOfCPUCores();

				//prendo il dispositivo con la latency migliore
				if(bestLatency > (pathLatency + tasksLatency)){
					selected = i;
					minTasksCount = sharedHistoryMap.get(i);
					bestLatency = pathLatency + tasksLatency;
				}
			}
		}
		// Assign the tasks to the obtained computing node.
		sharedHistoryMap.put(selected, minTasksCount + 1);

		//determino il nodo associato a quel container
		int associatedNode = findNodeAssociatedWithContainer(selected);
		//aggiungo il task alla historyMap dei task assegnati a quel nodo
		tasksHistoryMap.get(associatedNode).add(task);

		return associatedNode;
	}

	//recupera la latency del path associato ad un percorso ED-ONT-SDN-DESTINAZIONE-SDN-ONT-ED
	public double getPathLatencyFromDeviceToOffloadingDestination(Task task, ComputingNode node1, ComputingNode node2){

		//costruisco il path dall'Edge Device all'SDN 
		ComputingNode from = node1;
		ComputingNode to = node1.getEdgeOrchestrator();
		long id = simulationManager.getDataCentersManager().getTopology().getUniqueId(from.getId(), to.getId());
		GraphPath<ComputingNode, NetworkLink> path;
		if(simulationManager.getDataCentersManager().getTopology().getPathsMap().containsKey(id))
			path = simulationManager.getDataCentersManager().getTopology().getPathsMap().get(id); 
		else{ 
			path = simulationManager.getDataCentersManager().getTopology().getPath(from, to);
			simulationManager.getDataCentersManager().getTopology().getPathsMap().put(id, path);
		}
		//determino la latency approssimativa associata al percorso del task.
		double pathLatency = (path.getWeight() + task.getFileSizeInBits()/getAverageBandwidthFromDeviceToDestination(path) + (getPathDistanceFromDeviceToDestination(path)/1000)/200000);
		
		//ora costruisco il path dall'SDN al nodo destinazione
		from = to;
		to = node2;
		id = simulationManager.getDataCentersManager().getTopology().getUniqueId(from.getId(), to.getId());
		if(simulationManager.getDataCentersManager().getTopology().getPathsMap().containsKey(id))
			path = simulationManager.getDataCentersManager().getTopology().getPathsMap().get(id); 
		else{ 
			path = simulationManager.getDataCentersManager().getTopology().getPath(from, to);
			simulationManager.getDataCentersManager().getTopology().getPathsMap().put(id, path);
		}
		//determino la latency approssimativa associata al percorso del task.
		pathLatency += (path.getWeight() + task.getFileSizeInBits()/getAverageBandwidthFromDeviceToDestination(path) + (getPathDistanceFromDeviceToDestination(path)/1000)/200000);

		return pathLatency*2;
	}

	//returns the distance in meters of a given path
	private double getPathDistanceFromDeviceToDestination(GraphPath<ComputingNode, NetworkLink> path){
		double distance = 0.0;
		for(int i = 0; i < path.getVertexList().size()-1; i++){
			double x1 = path.getVertexList().get(i).getMobilityModel().getCurrentLocation().getXPos();
			double y1 = path.getVertexList().get(i).getMobilityModel().getCurrentLocation().getYPos();
			double x2 = path.getVertexList().get(i+1).getMobilityModel().getCurrentLocation().getXPos();
			double y2 = path.getVertexList().get(i+1).getMobilityModel().getCurrentLocation().getYPos();
			distance += Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
		}
		return distance;
	}

	//stima la banda media per il percorso da una sorgente ad una destinazione
	private double getAverageBandwidthFromDeviceToDestination(GraphPath<ComputingNode, NetworkLink> path){
		double bandwidth = 0.0;
		for(NetworkLink nl : path.getEdgeList()){
			if(nl.getType().equals(NetworkLink.NetworkLinkTypes.FIBER))
				bandwidth += SimulationParameters.FiberBandwidthBitsPerSecond;
			else if(nl.getType().equals(NetworkLink.NetworkLinkTypes.WAN))
				bandwidth += SimulationParameters.wanBandwidthBitsPerSecond;
			else if(nl.getType().equals(NetworkLink.NetworkLinkTypes.LAN)){
				if(path.getVertexList().get(path.getEdgeList().indexOf(nl)).getEnergyModel().getConnectivityType().equals("cellular"))
					bandwidth += SimulationParameters.cellularBandwidthBitsPerSecond;
				else if(path.getVertexList().get(path.getEdgeList().indexOf(nl)).getEnergyModel().getConnectivityType().equals("wifi"))
					bandwidth += SimulationParameters.wifiBandwidthBitsPerSecond;
				else if(path.getVertexList().get(path.getEdgeList().indexOf(nl)).getEnergyModel().getConnectivityType().equals("ethernet"))
					bandwidth += SimulationParameters.ethernetBandwidthBitsPerSecond;
			}
		}
		return bandwidth/path.getEdgeList().size();
	}

	//fornita la posizione di un container shared nella ContainerList (lista di container Shared) 
	//ne estrapolo la posizione del relativo nodo nella NodeList (Nodo sul quale è piazzato il container shared)
	private int findNodeAssociatedWithContainer(int containerPos){
		return nodeList.indexOf(containerList.get(containerPos).getPlacementDestination());
	}

	@Override
	public void resultsReturned(Task task) {
		// rimuovo il task dalla historyMap
		for(int i = 0; i < nodeList.size(); i++)
			if(nodeList.get(i).equals(task.getOffloadingDestination()))
				if(!tasksHistoryMap.get(i).remove(task))System.out.println("ho sbagliato qualcosa");
	}
 
	@Override
	public void processEvent(Event e) {
		// Process the scheduled events, if any.
		
	}

	@Override
	public void notifyOrchestratorOfTaskExecution(Task task) {
		
	}

	@Override
	public void onSimulationEnd() {}

}
