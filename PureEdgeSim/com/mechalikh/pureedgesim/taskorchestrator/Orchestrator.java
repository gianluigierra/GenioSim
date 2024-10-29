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
 *     @author Charaf Eddine Mechalikh
 **/
package com.mechalikh.pureedgesim.taskorchestrator;
import java.util.ArrayList;
import java.util.List;
import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode;
import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode.LinkOrientation;
import com.mechalikh.pureedgesim.datacentersmanager.NuovaCartellaVM.*;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.simulationengine.SimEntity;
import com.mechalikh.pureedgesim.simulationmanager.SimLog;
import com.mechalikh.pureedgesim.simulationmanager.SimulationManager;
import com.mechalikh.pureedgesim.taskgenerator.Task;

public abstract class Orchestrator extends SimEntity {
	public List<ComputingNode> nodeList = new ArrayList<>();					//modificato per visibilità degli agenti DQN, era protected
	protected SimulationManager simulationManager;
	protected SimLog simLog;
	protected String algorithmName;
	protected String architectureName;
	protected String[] architectureLayers;

	protected Orchestrator(SimulationManager simulationManager) {
		super(simulationManager.getSimulation());
		this.simulationManager = simulationManager;
		simulationManager.setEdgeOrchestrator(this);
		simLog = simulationManager.getSimulationLogger();
		algorithmName = simulationManager.getScenario().getStringOrchAlgorithm();
		architectureName = simulationManager.getScenario().getStringOrchArchitecture();
		initialize();
	}

	// Find an offloading location for this task
	public void orchestrate(Task task) {
		assignTaskToComputingNode(task, architectureLayers);
	}
	public void initialize() {
		if ("CLOUD_ONLY".equals(architectureName)) {
			cloudOnly();
		} else if ("MIST_ONLY".equals(architectureName)) {
			mistOnly();
		} else if ("EDGE_AND_CLOUD".equals(architectureName)) {
			edgeAndCloud();
		} else if ("ALL".equals(architectureName)) {
			all();
		} else if ("EDGE_ONLY".equals(architectureName)) {
			edgeOnly();
		} else if ("MIST_AND_CLOUD".equals(architectureName)) {
			mistAndCloud();
		} else if ("MIST_AND_EDGE".equals(architectureName)) {
			mistAndEdge();
		}
	}

	// If the orchestration scenario is MIST_ONLY send Tasks only to edge devices
	protected void mistOnly() {
		architectureLayers = new String[]{ "Mist" };
		nodeList = simulationManager.getDataCentersManager().getComputingNodesGenerator()
				.getMistOnlyListSensorsExcluded();
		
	}

	// If the orchestration scenario is CLOUD_ONLY send Tasks (cloudlets) only to
	// cloud virtual machines (vms)
	protected void cloudOnly() {
		architectureLayers = new String[] { "Cloud" };
		for(DataCenter datacenter : simulationManager.getDataCentersManager().getComputingNodesGenerator().getCloudOnlyList()){
			for(Host host : datacenter.getHostList()){
				nodeList.addAll(host.getVMList());
			}
		}
	}

	// If the orchestration scenario is EDGE_AND_CLOUD send Tasks only to edge data
	// centers or cloud virtual machines (vms)
	protected void edgeAndCloud() {
		architectureLayers = new String[]{ "Cloud", "Edge" };
		for(DataCenter datacenter : simulationManager.getDataCentersManager().getComputingNodesGenerator().getEdgeAndCloudList()){
			for(Host host : datacenter.getHostList()){
				nodeList.addAll(host.getVMList());
			}
		}		
	}

	// If the orchestration scenario is MIST_AND_CLOUD send Tasks only to edge
	// devices or cloud virtual machines (vms)
	protected void mistAndCloud() {
		architectureLayers = new String[] { "Cloud", "Mist" };
		nodeList = simulationManager.getDataCentersManager().getComputingNodesGenerator()
				.getMistAndCloudListSensorsExcluded();
		for(ComputingNode node : nodeList)
			if(node.getType() == SimulationParameters.TYPES.CLOUD)
				nodeList.remove(node);
		for(DataCenter datacenter : simulationManager.getDataCentersManager().getComputingNodesGenerator().getCloudOnlyList()){
			for(Host host : datacenter.getHostList()){
				nodeList.addAll(host.getVMList());
			}
		}
	}

	// If the orchestration scenario is EDGE_ONLY send Tasks only to edge data
	// centers
	protected void edgeOnly() {
		architectureLayers = new String[]{ "Edge" };
		for(DataCenter datacenter : simulationManager.getDataCentersManager().getComputingNodesGenerator().getEdgeOnlyList()){
			for(Host host : datacenter.getHostList()){
				nodeList.addAll(host.getVMList());
			}
		}
	}

	// If the orchestration scenario is MIST_AND_Edge send Tasks only to edge
	// devices or Fog virtual machines (vms)
	protected void mistAndEdge() {
		architectureLayers = new String[] { "Mist", "Edge" };
		nodeList = simulationManager.getDataCentersManager().getComputingNodesGenerator()
				.getMistAndEdgeListSensorsExcluded();
		for(ComputingNode node : nodeList)
			if(node.getType() == SimulationParameters.TYPES.EDGE_DATACENTER)
				nodeList.remove(node);
		for(DataCenter datacenter : simulationManager.getDataCentersManager().getComputingNodesGenerator().getEdgeOnlyList()){
			for(Host host : datacenter.getHostList()){
				nodeList.addAll(host.getVMList());
			}
		}
	}

	// If the orchestration scenario is ALL send Tasks to any virtual machine (vm)
	// or device
	protected void all() {
		architectureLayers = new String[]{ "Cloud", "Edge", "Mist" };
		nodeList = simulationManager.getDataCentersManager().getComputingNodesGenerator()
				.getAllNodesListSensorsExcluded();		
				
		for(ComputingNode node : nodeList)
		if(node.getType() == SimulationParameters.TYPES.CLOUD)
			nodeList.remove(node);

		for(ComputingNode node : nodeList)
		if(node.getType() == SimulationParameters.TYPES.EDGE_DATACENTER)
			nodeList.remove(node);

		for(DataCenter datacenter : simulationManager.getDataCentersManager().getComputingNodesGenerator().getEdgeOnlyList()){
			for(Host host : datacenter.getHostList()){
				nodeList.addAll(host.getVMList());
			}
		}
		for(DataCenter datacenter : simulationManager.getDataCentersManager().getComputingNodesGenerator().getCloudOnlyList()){
			for(Host host : datacenter.getHostList()){
				nodeList.addAll(host.getVMList());
			}
		}
	}

	protected abstract int findComputingNode(String[] architectureLayers, Task task);

	protected void assignTaskToComputingNode(Task task, String[] architectureLayers) {

		int nodeIndex = findComputingNode(architectureLayers, task);

		if (nodeIndex != -1) {
			ComputingNode node = nodeList.get(nodeIndex);
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
	}

	protected void checkComputingNode(ComputingNode computingNode) {
		if (computingNode.isSensor())
			throw new IllegalArgumentException(
					getClass().getSimpleName() + " - The forbidden happened! The orchestration algorithm \"" + algorithmName
							+ "\" has selected a sensor as an offloading destination. Kindly check it.");
	}

	protected boolean sameLocation(ComputingNode device1, ComputingNode device2, int RANGE) {
		if (device2.getType() == SimulationParameters.TYPES.CLOUD)
			return true;
		double distance = device1.getMobilityModel().distanceTo(device2);
		return (distance <= RANGE);
	}

	protected boolean arrayContains(String[] architectureLayers, String value) {
		for (String s : architectureLayers) {
			if (s.equals(value))
				return true;
		}
		return false;
	}

	public boolean offloadingIsPossible(Task task, ComputingNode node, String[] architectureLayers) {					//modificato per visibilità degli agenti DQN, era protected
		boolean offloadingpossible = true;
		if(!offloadingpossible){
			SimulationParameters.TYPES nodeType = node.getType();

		
			System.out.print("\n");
			System.out.println("Nodo destinazione di offload = " + node.getName());
			System.out.println("UpLink dell'edge device associato al task = " + task.getEdgeDevice().getCurrentLink(LinkOrientation.UP_LINK).getDst().getName());
			System.out.print("\n\n");
			System.out.println("Nodo destinazione di offload = " + node.getName());
			System.out.println("UpLink dell'orchestrator associato al task = " + task.getOrchestrator().getCurrentLink(LinkOrientation.UP_LINK).getDst().getName());
			System.out.print("\n\n");
			System.out.println("Nodo destinazione di offload = " + node.getName());
			System.out.println("Orchestrator associato al task = " + task.getOrchestrator().getType());
			System.out.print("\n\n\n\n");

			return (
				
					(arrayContains(architectureLayers, "Cloud") && nodeType == SimulationParameters.TYPES.VM_CLOUD) // cloud computing
																
																|| 
					
					(
						arrayContains(architectureLayers, "Edge") && nodeType == SimulationParameters.TYPES.VM_EDGE // Edge computing. Compare destination (edge data server) and origin (edge device) locations, if they are in same area offload to this edge data server
																	&& 
						(
							(node == task.getEdgeDevice().getCurrentLink(LinkOrientation.UP_LINK).getDst()) // or compare the location of the orchestrator
																		|| 
							(node == task.getOrchestrator().getCurrentLink(LinkOrientation.UP_LINK).getDst())
						)
					)

																|| 

					//Non prendiamo in considerazione scenari Mist quindi questo non si verifica mai.
					(
						arrayContains(architectureLayers, "Mist") && nodeType == SimulationParameters.TYPES.EDGE_DEVICE // Mist computing. Compare destination (edge device) location and origin (edge device) location, if they are in same area offload to this device
																	&& 
						(
							sameLocation(node, task.getEdgeDevice(), SimulationParameters.edgeDevicesRange) // or compare the location of their orchestrators
																		|| 
							(SimulationParameters.enableOrchestrators && sameLocation(node, task.getOrchestrator(), SimulationParameters.edgeDevicesRange))
						)
																	&& 
																!node.isDead() 
																	&& 
																!node.isSensor()
					)
				
				);
		}
		else return offloadingpossible;
	}

	public abstract void resultsReturned(Task task);

	public String[] getArchitectureLayers(){
		return architectureLayers;
	}

	public List<ComputingNode> getNodeList(){
		return nodeList;
	}

	public abstract void notifyOrchestratorOfTaskExecution(Task task);
	
}
