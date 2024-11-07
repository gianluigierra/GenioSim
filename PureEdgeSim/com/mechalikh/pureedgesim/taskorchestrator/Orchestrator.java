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
import com.mechalikh.pureedgesim.taskgenerator.Container;

public abstract class Orchestrator extends SimEntity {
	public List<ComputingNode> nodeList = new ArrayList<>();					//modificato per visibilità degli agenti DQN, era protected
	protected List<Container> containerList = new ArrayList<>();					//modificato per visibilità degli agenti DQN, era protected
	protected SimulationManager simulationManager;
	protected SimLog simLog;
	protected String algorithmName;
	protected String architectureName;
	protected String[] architectureLayers;

	protected static boolean printDebug = false;

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
		} else if ("FAR_EDGE_ONLY".equals(architectureName)) {
			farEdgeOnly();
		} else if ("EDGE_AND_CLOUD".equals(architectureName)) {
			edgeAndCloud();
		} else if ("ALL".equals(architectureName)) {
			all();
		} else if ("EDGE_ONLY".equals(architectureName)) {
			edgeOnly();
		} else if ("FAR_EDGE_AND_CLOUD".equals(architectureName)) {
			farEdgeAndCloud();
		} else if ("FAR_EDGE_AND_EDGE".equals(architectureName)) {
			farEdgeAndEdge();
		}
	}

	// If the orchestration scenario is FAR_EDGE_ONLY send Tasks only to edge devices
	protected void farEdgeOnly() {
		architectureLayers = new String[]{ "Far_Edge" };
		nodeList.addAll(simulationManager.getDataCentersManager().getComputingNodesGenerator().getONT_List());
		
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

	// If the orchestration scenario is FAR_EDGE_AND_CLOUD send Tasks only to edge
	// devices or cloud virtual machines (vms)
	protected void farEdgeAndCloud() {
		architectureLayers = new String[] { "Cloud", "Far_Edge" };
		nodeList.addAll(simulationManager.getDataCentersManager().getComputingNodesGenerator().getONT_List());
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

	// If the orchestration scenario is FAR_EDGE_AND_Edge send Tasks only to edge
	// devices or Fog virtual machines (vms)
	protected void farEdgeAndEdge() {
		architectureLayers = new String[] { "Far_Edge", "Edge" };
		nodeList.addAll(simulationManager.getDataCentersManager().getComputingNodesGenerator().getONT_List());
		for(DataCenter datacenter : simulationManager.getDataCentersManager().getComputingNodesGenerator().getEdgeOnlyList()){
			for(Host host : datacenter.getHostList()){
				nodeList.addAll(host.getVMList());
			}
		}
	}

	// If the orchestration scenario is ALL send Tasks to any virtual machine (vm)
	// or device
	protected void all() {
		architectureLayers = new String[]{ "Cloud", "Edge", "Far_Edge" };
		nodeList.addAll(simulationManager.getDataCentersManager().getComputingNodesGenerator().getONT_List());	

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

		int nodeIndex = 
			//findComputingNode(architectureLayers, task);
			findVmAssociatedWithTask(task);

		if (nodeIndex != -1) {
			ComputingNode node = nodeList.get(nodeIndex);
			try {
				checkComputingNode(node);
			} catch (Exception e) {
				e.printStackTrace();
			}

			if(printDebug) System.out.println("ORCHESTRATORE EDGE: ho associato la " + nodeList.get(nodeIndex).getName() + " al task generato dal " + task.getEdgeDevice().getName());
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

	public void setContainerToVM(Container container){
		//System.out.println("Sono l'SDN e ho ricevuto la richiesta di associare il container " + container.getId());
		containerList.add(container);
	}

	public int findVmAssociatedWithTask(Task task){
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
		return -1;
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
