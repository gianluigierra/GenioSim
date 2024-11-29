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
import java.util.List;
import java.util.ArrayList;

import org.jgrapht.GraphPath;

import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode;
import com.mechalikh.pureedgesim.datacentersmanager.NuovaCartellaVM.VM;
import com.mechalikh.pureedgesim.network.NetworkLink;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.simulationengine.Event;
import com.mechalikh.pureedgesim.simulationmanager.SimulationManager;
import com.mechalikh.pureedgesim.taskgenerator.Container;

public class DefaultContainerOrchestrator extends ContainerOrchestrator {
	//questa lista contiene solo i nodi "grossi", ossia i DataCenter (Edge o Cloud) e gli ONT
	protected List<ComputingNode> bigNodeList = new ArrayList<>();
	public Map<Integer, List<Container>> bigNodeSharedHistoryMap = new LinkedHashMap<>();			//usata per valutare quanti container shared sono istanziati su un DC
	public Map<Integer, List<Container>> bigNodeHistoryMap = new LinkedHashMap<>();  				//usata per valutare quanti container non shared sono istanziati su un DC
	public Map<Integer, Integer> historyMap = new LinkedHashMap<>();								//usata per valutare quanti container sono istanziati su un nodo di nodeList

	public DefaultContainerOrchestrator(SimulationManager simulationManager) {
		super(simulationManager);
		//inizializzo la bigNodeList
		for (ComputingNode cn : nodeList) {
			ComputingNode DC = null;
			if(cn.getType().equals(SimulationParameters.TYPES.VM_CLOUD) || cn.getType().equals(SimulationParameters.TYPES.VM_EDGE)){
				DC = ((VM) cn).getHost().getDataCenter();
			}

			if(DC != null && !bigNodeList.contains(DC))
				bigNodeList.add(DC);
		}
		// Initialize the history map
		for (int i = 0; i < nodeList.size(); i++) {
			historyMap.put(i, 0);
		}
		// Initialize the bigNodeHistoryMaps
		for (int i = 0; i < bigNodeList.size(); i++) {
			bigNodeSharedHistoryMap.put(i, new ArrayList<Container>());
			bigNodeHistoryMap.put(i, new ArrayList<Container>());
		}
	}

	// rimuovo i container dalle historyMaps (historyMap, bigNodeHistoryMap)
	public void removeContainerFromVM(Container container) {
		for (int i = 0; i < nodeList.size(); i++) {
			if (nodeList.get(i).equals(container.getPlacementDestination())) {
				historyMap.put(i, historyMap.get(i) - 1);
			}
		}
		for(int i = 0; i < bigNodeList.size(); i++){
			if(bigNodeHistoryMap.get(i).contains(container))
				bigNodeHistoryMap.get(i).remove(container);
		}
		//System.out.println("Ho effettuato l'unplacement del container " + container.getId() + " relativo all'app " + container.getAssociatedAppName() 
		//+ " piazzato sul nodo " + container.getPlacementDestination().getName());
	}

	protected int findComputingNode(String[] architecture, Container container) {

		// altrimenti piazzo il container
		if ("ROUND_ROBIN".equals(algorithmName)) {
			return roundRobin(architecture, container);
		} else if ("TRADE_OFF".equals(algorithmName)) {
			return tradeOff(architecture, container);
		} else if ("GREEDY".equals(algorithmName)) {
			return greedyChoice(architecture, container);
		}else if (algorithmName.contains("DISTANCE")) {
			return selectBestDistanceDataCenter(architecture, container);
		} else if (algorithmName.contains("LATENCY")) {
			return selectBestLatencyDataCenter(architecture, container);
		}
		else {
			throw new IllegalArgumentException(getClass().getSimpleName() + " - Unknown orchestration algorithm '"
					+ algorithmName + "', please check the simulation parameters file...");
		}
	}

	protected int tradeOff(String[] architecture, Container container) {
		int selected = -1;
		double min = -1;
		double newMin;// the computing node with minimum weight;
		ComputingNode node; // get best computing node for this task
		for (int i = 0; i < nodeList.size(); i++) {
			node = nodeList.get(i);
			if (placementIsPossible(container, node, architecture)) {
				// the weight below represent the priority, the less it is, the more it is //
				// suitable for offlaoding, you can change it as you want
				double weight = 0.0;
				if (node.getType() == SimulationParameters.TYPES.VM_EDGE) {
					weight = 1.2;
					// // this is an
					// edge server 'cloudlet', the latency is slightly high then edge // devices
				} else if (node.getType() == SimulationParameters.TYPES.VM_CLOUD) {
					weight = 1.8; // this
					// is the cloud, it consumes more energy and results in high latency, so //
					// better to avoid it
				} else if (node.getType() == SimulationParameters.TYPES.ONT) {
					weight = 1.3;// this is an edge
					// device, it results in an extremely low latency, but may // consume more
					// energy.
				}
				newMin = (historyMap.get(i) + 1) * weight * container.getContainerSizeInBits() / node.getMipsPerCore();
				if (min == -1 || min > newMin) { // if it is the first
					// iteration, or if this computing node has more // cpu mips and // less waiting
					// tasks
					min = newMin; // set the first computing node as the best one
					selected = i;
				}
			}
		}
		if (selected != -1) {
			historyMap.put(selected, historyMap.get(selected) + 1); // assign the tasks to the selected computing
		}
		// node
		return selected;
	}

	protected int roundRobin(String[] architecture, Container container) {
		int selected = -1;
		int minTasksCount = -1; // Computing node with minimum assigned tasks.
		for (int i = 0; i < nodeList.size(); i++) {
			if (placementIsPossible(container, nodeList.get(i), architecture)
					&& (minTasksCount == -1 || minTasksCount > historyMap.get(i))) {
				minTasksCount = historyMap.get(i);
				// if this is the first time,
				// or new min found, so we choose it as the best computing node.
				selected = i;
			}
		}
		// Assign the tasks to the obtained computing node.
		if (selected != -1) {
			historyMap.put(selected, historyMap.get(selected) + 1); // assign the tasks to the selected computing
		}

		return selected;
	}

	protected int greedyChoice(String[] architecture, Container container) {
		int selected = -1;
		double bestfit = Double.MAX_VALUE;
		double bestnumberofcores = 0;
		for (int i = 0; i < nodeList.size(); i++) {
			// viene scelto il nodo con il miglio rapporto TaskOffloaded/coresTotali
			if ((historyMap.get(i) / nodeList.get(i).getNumberOfCPUCores() < bestfit)
					&& placementIsPossible(container, nodeList.get(i), architecture)) {
				bestnumberofcores = nodeList.get(i).getNumberOfCPUCores();
				bestfit = historyMap.get(i) / nodeList.get(i).getNumberOfCPUCores();
				selected = i;
			}
			// laddove si abbia un rapporto TaskOffloaded/coresTotali uguale prevale il nodo
			// con il numero di cores maggiore
			else if ((historyMap.get(i) / nodeList.get(i).getNumberOfCPUCores() == bestfit)
					&& (bestnumberofcores < nodeList.get(i).getNumberOfCPUCores())
					&& placementIsPossible(container, nodeList.get(i), architecture)) {
				bestnumberofcores = nodeList.get(i).getNumberOfCPUCores();
				bestfit = historyMap.get(i) / nodeList.get(i).getNumberOfCPUCores();
				selected = i;
			}
		}
		if (selected != -1) {
			historyMap.put(selected, historyMap.get(selected) + 1); // assign the tasks to the selected computing
		}

		return selected;
	}

	protected int selectBestDistanceDataCenter(String[] architecture, Container container) {
		int selected = -1;

		//devo determinare i primi "copies" dispositivi di placement più vicini in media a tutti i device. (Laddove il container non sia shared, copies = 1)
		int copies = SimulationParameters.applicationList.get(container.getApplicationID()).getContainerCopies();
		List<ComputingNode> copiesList = new ArrayList<ComputingNode>(copies);
		//ciclo tra tutti i dispositivi di placement per determinare una lista
		for (int j = 0; j < copies; j++) {
			double minAverageTotalDistance = Double.MAX_VALUE;
			int closestNode = -1;				
			//ciclo tra tutti i dispositivi di placement per determinare ogni volta il più vicino (se non presente in lista)
			for (int i = 0; i < bigNodeList.size(); i++) {
				double averageTotalDistance = 0;
				//valuto la distanza con tutti gli edgeDevice associati al container (Laddove il container non sia shared, c'è solo un edgeDevice)
				for(ComputingNode cn : container.getEdgeDevices()){
					averageTotalDistance += cn.getMobilityModel().distanceTo(bigNodeList.get(i));
				}
				averageTotalDistance = averageTotalDistance / container.getEdgeDevices().size();

				if(averageTotalDistance < minAverageTotalDistance && !copiesList.contains(bigNodeList.get(i))){
					closestNode = i;
					minAverageTotalDistance = averageTotalDistance;
				}
			}
			if(closestNode != -1)
				copiesList.add(bigNodeList.get(closestNode));
		}

		//adesso devo scegliere in quale Nodo vado ad allocare questo Container sulla base delle copie già presenti sulla base di un RoundRobin
		ComputingNode bestNode = null;
		int bigNodeSelected = -1;
		int minPlacement = Integer.MAX_VALUE;
		for(ComputingNode cn : copiesList){
			int num = 0;
			for(int i = 0; i < bigNodeList.size(); i++){
				if(cn.equals(bigNodeList.get(i))){

					//se container è shared allora devo prendere il numero di container shared
					if(container.getSharedContainer())
						num = numOfSharedContainerInList(container, bigNodeSharedHistoryMap.get(i)); 
					//altrimenti quelli non shared
					else
						num = bigNodeHistoryMap.get(i).size();

					//System.out.println("Nodo " + cn.getName() + ", numero di copie del container shared in lista = " + num);
					if(num < minPlacement){
						bigNodeSelected = i;
						minPlacement = num;
						bestNode = cn;
					}
				}
			}
		}
				
		if(algorithmName.contains("ROUND_ROBIN"))
			selected = DCRoundRobin(architecture, container, bestNode);				
		else if(algorithmName.contains("TRADE_OFF"))
		 	selected = DCTradeOff(architecture, container, bestNode);	
		else if(algorithmName.contains("GREEDY"))
		 	selected = DCGreedyChoice(architecture, container, bestNode);		
	
		if (selected != -1) {
			//System.out.println("Ho scelto il dispositivo " + nodeList.get(selected).getName() + " come placement per il container " 
			//+ container.getId() + " relativo all'app " + container.getAssociatedAppName());
			historyMap.put(selected, historyMap.get(selected) + 1); // assign the tasks to the selected computing

			//adesso devo comportarmi diversamente a seconda che il Container sia Shared oppure no:
			//caso shared
			if(!container.getSharedContainer())
				//aggiungo il container alla HistoryMap di quel DC
				bigNodeHistoryMap.get(bigNodeSelected).add(container);
			else								
				//aggiungo il container alla sharedHistoryMap di quel DC
				bigNodeSharedHistoryMap.get(bigNodeSelected).add(container);
		}

		return selected;
	}

	protected int selectBestLatencyDataCenter(String[] architecture, Container container) {
		int selected = -1;

		//devo determinare i primi "copies" dispositivi di placement con latency migliore in media per tutti i device. (Laddove il container non sia shared, copies = 1)
		int copies = SimulationParameters.applicationList.get(container.getApplicationID()).getContainerCopies();
		List<ComputingNode> copiesList = new ArrayList<ComputingNode>(copies);
		//ciclo tra tutti i dispositivi di placement per determinare una lista
		for (int j = 0; j < copies; j++) {
			double minAverageLatency = Double.MAX_VALUE;
			int closestNode = -1;				
			//ciclo tra tutti i dispositivi di placement per determinare ogni volta il più vicino (se non presente in lista)
			for (int i = 0; i < bigNodeList.size(); i++) {
				double averageLatency = 0;
				//valuto la distanza con tutti gli edgeDevice associati al container (Laddove il container non sia shared, c'è solo un edgeDevice)
				for(ComputingNode cn : container.getEdgeDevices()){
					ComputingNode from = cn;
					ComputingNode to = bigNodeList.get(i);
					GraphPath<ComputingNode, NetworkLink> path = simulationManager.getDataCentersManager().getTopology().getPath(from, to);
					averageLatency += path.getWeight();
				}
				averageLatency = averageLatency / container.getEdgeDevices().size();

				if(averageLatency < minAverageLatency && !copiesList.contains(bigNodeList.get(i))){
					closestNode = i;
					minAverageLatency = averageLatency;
				}
			}
			if(closestNode != -1)
				copiesList.add(bigNodeList.get(closestNode));
		}

		//adesso devo scegliere in quale Nodo vado ad allocare questo Container sulla base delle copie già presenti sulla base di un RoundRobin
		ComputingNode bestNode = null;
		int bigNodeSelected = -1;
		int minPlacement = Integer.MAX_VALUE;
		for(ComputingNode cn : copiesList){
			int num = 0;
			for(int i = 0; i < bigNodeList.size(); i++){
				if(cn.equals(bigNodeList.get(i))){

					//se container è shared allora devo prendere il numero di container shared
					if(container.getSharedContainer())
						num = numOfSharedContainerInList(container, bigNodeSharedHistoryMap.get(i)); 
					//altrimenti quelli non shared
					else
						num = bigNodeHistoryMap.get(i).size();

					//System.out.println("Nodo " + cn.getName() + ", numero di copie del container shared in lista = " + num);
					if(num < minPlacement){
						bigNodeSelected = i;
						minPlacement = num;
						bestNode = cn;
					}
				}
			}
		}
				
		if(algorithmName.contains("ROUND_ROBIN"))
			selected = DCRoundRobin(architecture, container, bestNode);				
		else if(algorithmName.contains("TRADE_OFF"))
		 	selected = DCTradeOff(architecture, container, bestNode);	
		else if(algorithmName.contains("GREEDY"))
		 	selected = DCGreedyChoice(architecture, container, bestNode);		
	
		if (selected != -1) {
			//System.out.println("Ho scelto il dispositivo " + nodeList.get(selected).getName() + " come placement per il container " 
			//+ container.getId() + " relativo all'app " + container.getAssociatedAppName());
			historyMap.put(selected, historyMap.get(selected) + 1); // assign the tasks to the selected computing

			//adesso devo comportarmi diversamente a seconda che il Container sia Shared oppure no:
			//caso shared
			if(!container.getSharedContainer())
				//aggiungo il container alla HistoryMap di quel DC
				bigNodeHistoryMap.get(bigNodeSelected).add(container);
			else								
				//aggiungo il container alla sharedHistoryMap di quel DC
				bigNodeSharedHistoryMap.get(bigNodeSelected).add(container);
		}

		return selected;
	}

	protected int numOfSharedContainerInList(Container container, List<Container> ContainerList){
		int num = 0;
		for(Container cont : ContainerList){
			if(cont.getAssociatedAppName().equals(container.getAssociatedAppName()))
				num++;
		}
		return num;
	}

	protected int DCRoundRobin(String[] architecture, Container container, ComputingNode bestNode){
		int selected = -1;
		int minTasksCount = -1; // Computing node with minimum assigned tasks.
		//ciclo tra tutti i device capaci di fare placement
		for(int i = 0; i < nodeList.size(); i++){

			//se il nodo afferisce all'OLT scelto allora lo valuto
			if(nodeList.get(i).getEdgeOrchestrator().equals(bestNode.getEdgeOrchestrator()) 
			&& (placementIsPossible(container, nodeList.get(i), architecture) )
			&& (minTasksCount == -1 || minTasksCount > historyMap.get(i))){
				minTasksCount = historyMap.get(i);
				// if this is the first time,
				// or new min found, so we choose it as the best computing node.
				selected = i;
			}
		}
		return selected;
	}

	protected int DCTradeOff(String[] architecture, Container container, ComputingNode bestNode){
		int selected = -1;
		double min = -1;
		double newMin;// the computing node with minimum weight;
		ComputingNode node; // get best computing node for this task
		//giro tra tutti i nodi controllando però che afferiscano allo stesso OLT specificato
		for (int i = 0; i < nodeList.size(); i++) {
			node = nodeList.get(i);
			if (placementIsPossible(container, node, architecture)) {
				// the weight below represent the priority, the less it is, the more it is //
				// suitable for offlaoding, you can change it as you want
				double weight = 0.0;
				if (node.getType() == SimulationParameters.TYPES.VM_EDGE && nodeList.get(i).getEdgeOrchestrator().equals(bestNode.getEdgeOrchestrator())) {
					weight = 1.5;
					// // this is an
					// edge server 'cloudlet', the latency is slightly high then edge // devices
				} else if (node.getType() == SimulationParameters.TYPES.VM_CLOUD && nodeList.get(i).getEdgeOrchestrator().equals(bestNode.getEdgeOrchestrator())) {
					weight = 8; // this
					// is the cloud, it consumes more energy and results in high latency, so //
					// better to avoid it
				} else if (nodeList.get(i).getType().equals(SimulationParameters.TYPES.ONT) && nodeList.get(i).getEdgeOrchestrator().equals(bestNode.getEdgeOrchestrator())) {
					weight = 1;
					if(container.getSharedContainer())
						weight = 8;
					// this is an edge
					// device, it results in an extremely low latency, but may // consume more
					// energy.
				}
				if(weight != 0.0){
					newMin = (historyMap.get(i) + 1) * weight * container.getContainerSizeInBits() / node.getMipsPerCore();
					if (min == -1 || min > newMin) { // if it is the first
						// iteration, or if this computing node has more // cpu mips and // less waiting
						// tasks
						min = newMin; // set the first computing node as the best one
						selected = i;
					}
				}
			}
		}
		// node
		return selected;
	}

	protected int DCGreedyChoice(String[] architecture, Container container, ComputingNode bestNode) {
		int selected = -1;
		double bestfit = Double.MAX_VALUE;
		double bestnumberofcores = 0;
		//giro tra tutti i nodi controllando però che afferiscano allo stesso OLT specificato
		for (int i = 0; i < nodeList.size(); i++) {
			// viene scelto il nodo con il miglio rapporto TaskOffloaded/coresTotali
			if ((historyMap.get(i) / nodeList.get(i).getNumberOfCPUCores() < bestfit)
					&& nodeList.get(i).getEdgeOrchestrator().equals(bestNode.getEdgeOrchestrator())
					&& placementIsPossible(container, nodeList.get(i), architecture)) {
				bestnumberofcores = nodeList.get(i).getNumberOfCPUCores();
				bestfit = historyMap.get(i) / nodeList.get(i).getNumberOfCPUCores();
				selected = i;
			}
			// laddove si abbia un rapporto TaskOffloaded/coresTotali uguale prevale il nodo
			// con il numero di cores maggiore
			else if ((historyMap.get(i) / nodeList.get(i).getNumberOfCPUCores() == bestfit)
					&& (bestnumberofcores < nodeList.get(i).getNumberOfCPUCores())
					&& nodeList.get(i).getEdgeOrchestrator().equals(bestNode.getEdgeOrchestrator())
					&& placementIsPossible(container, nodeList.get(i), architecture)) {
				bestnumberofcores = nodeList.get(i).getNumberOfCPUCores();
				bestfit = historyMap.get(i) / nodeList.get(i).getNumberOfCPUCores();
				selected = i;
			}
		}
		if (selected != -1) {
			historyMap.put(selected, historyMap.get(selected) + 1); // assign the tasks to the selected computing
		}

		return selected;
	}

	@Override
	public void resultsReturned(Container container) {
		// Viene triggerato quando il nodo dovrebbe avere ricevuto la notifica del
		// download del container.

		// System.out.println("Sono il nodo e sono stato notificato del download del
		// container " + container.getId() + " da parte del nodo " +
		// container.getPlacementDestination().getName());
	}
 
	@Override
	public void processEvent(Event e) {
		// Process the scheduled events, if any.

	}

	@Override
	public void notifyOrchestratorOfContainerExecution(Container container) {
		// Arriva quando l'orchestratore viene notificato del placement del container

		// System.out.println("Sono l'orchestratore e sono stato notificato del download
		// del container " + container.getId() + " da parte del nodo " +
		// container.getPlacementDestination().getName());
	}

}
