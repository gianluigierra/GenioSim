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

import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.simulationengine.Event;
import com.mechalikh.pureedgesim.simulationmanager.SimulationManager;
import com.mechalikh.pureedgesim.taskgenerator.Container;

public class DefaultContainerOrchestrator extends ContainerOrchestrator {
	public Map<Integer, Integer> historyMap = new LinkedHashMap<>();
	public Map<Integer, List<Container>> ContainerMap = new LinkedHashMap<>();

	public DefaultContainerOrchestrator(SimulationManager simulationManager) {
		super(simulationManager);
		// Initialize the history map
		for (int i = 0; i < nodeList.size(); i++){
			historyMap.put(i, 0);
			ContainerMap.put(i, new ArrayList<Container>());
		}
	}

	protected int findComputingNode(String[] architecture, Container container) {

		//se il container è del tipo "shared"
		if(container.getSharedContainer())
			//controlla tra tutti i nodi 
			for(int i = 0; i < nodeList.size(); i++)
				//se è presente un container con lo stesso nome di App di quello appena arrivato
				for(Container cont : ContainerMap.get(i))
					if(cont.getAssociatedAppName().equals(container.getAssociatedAppName())){
						//setto il container come PLACED
						container.setStatus(Container.Status.PLACED);
						//returno la VM verso la quale è stato piazzato quel container
						return i;
					}
		
		//altrimenti piazzo il container
		if ("ROUND_ROBIN".equals(algorithmName)) {
			return roundRobin(architecture, container);
		} else if ("TRADE_OFF".equals(algorithmName)) {
			return tradeOff(architecture, container);
		} else if ("GREEDY".equals(algorithmName)) {
			return greedyChoice(architecture, container);
		} else {
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
				if(node.getType() == SimulationParameters.TYPES.VM_EDGE){
					weight = 1.2;
					// // this is an
					// edge server 'cloudlet', the latency is slightly high then edge // devices
				}else if (node.getType() == SimulationParameters.TYPES.VM_CLOUD) {
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
		if (selected != -1){
			historyMap.put(selected, historyMap.get(selected) + 1); // assign the tasks to the selected computing
			ContainerMap.get(selected).add(container);
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
		historyMap.put(selected, minTasksCount + 1);
		ContainerMap.get(selected).add(container);

		return selected;
	}

	protected int greedyChoice(String[] architecture, Container container){
        int selected = 0;
        double bestfit = Double.MAX_VALUE;
        double bestnumberofcores = 0;
        for(int i = 0; i < nodeList.size(); i++){
            //viene scelto il nodo con il miglio rapporto TaskOffloaded/coresTotali
            if( (historyMap.get(i)/nodeList.get(i).getNumberOfCPUCores() < bestfit) && placementIsPossible(container, nodeList.get(i), architecture)){
                bestnumberofcores = nodeList.get(i).getNumberOfCPUCores();
                bestfit = historyMap.get(i)/nodeList.get(i).getNumberOfCPUCores();
                selected = i;
            }
            //laddove si abbia un rapporto TaskOffloaded/coresTotali uguale prevale il nodo con il numero di cores maggiore
            else if((historyMap.get(i)/nodeList.get(i).getNumberOfCPUCores() == bestfit) && (bestnumberofcores < nodeList.get(i).getNumberOfCPUCores()) && placementIsPossible(container, nodeList.get(i), architecture)){
                bestnumberofcores = nodeList.get(i).getNumberOfCPUCores();
                bestfit = historyMap.get(i)/nodeList.get(i).getNumberOfCPUCores();
                selected = i;
            }
        }
        if("GREEDY".equals(algorithmName)){
			historyMap.put(selected, historyMap.get(selected) + 1);
			ContainerMap.get(selected).add(container);
		}
        return selected;
    }

	@Override
	public void resultsReturned(Container container) {
		// Viene triggerato quando il nodo dovrebbe avere ricevuto la notifica del download del container.

		//System.out.println("Sono il nodo e sono stato notificato del download del container " + container.getId() + " da parte del nodo " + container.getPlacementDestination().getName());
	}
 
	@Override
	public void processEvent(Event e) {
		// Process the scheduled events, if any.
		
	}

	@Override
	public void notifyOrchestratorOfContainerExecution(Container container) {
		//Arriva quando l'orchestratore viene notificato del placement del container
		
		//System.out.println("Sono l'orchestratore e sono stato notificato del download del container " + container.getId() + " da parte del nodo " + container.getPlacementDestination().getName());
	}

}
