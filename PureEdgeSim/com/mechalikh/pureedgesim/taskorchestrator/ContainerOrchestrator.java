package com.mechalikh.pureedgesim.taskorchestrator;
import java.util.ArrayList;
import java.util.List;
import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode;
import com.mechalikh.pureedgesim.datacentersmanager.NuovaCartellaVM.*;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.simulationengine.SimEntity;
import com.mechalikh.pureedgesim.simulationmanager.SimLog;
import com.mechalikh.pureedgesim.simulationmanager.SimulationManager;
import com.mechalikh.pureedgesim.taskgenerator.Container;

public abstract class ContainerOrchestrator extends SimEntity {
	static protected List<ComputingNode> nodeList = new ArrayList<>();					//modificato per visibilità degli agenti DQN, era protected
	protected SimulationManager simulationManager;
	protected SimLog simLog;
	protected String algorithmName;
	protected String architectureName;
	protected String[] architectureLayers;

	protected ContainerOrchestrator(SimulationManager simulationManager) {
		super(simulationManager.getSimulation());
		this.simulationManager = simulationManager;
		simulationManager.setCloudOrchestrator(this);
		simLog = simulationManager.getSimulationLogger();
		algorithmName = simulationManager.getScenario().getStringOrchAlgorithm();
		architectureName = simulationManager.getScenario().getStringOrchArchitecture();
		initialize();
	}

	// Find an offloading location for this task
	public void orchestrate(Container container) {
		assignContainerToComputingNode(container, architectureLayers);
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

	protected abstract int findComputingNode(String[] architectureLayers, Container container);

	protected void assignContainerToComputingNode(Container container, String[] architectureLayers) {

		int nodeIndex = findComputingNode(architectureLayers, container);

		if (nodeIndex != -1) {
			ComputingNode node = nodeList.get(nodeIndex);
			try {
				checkComputingNode(node);
			} catch (Exception e) {
				e.printStackTrace();
			}

			if(Orchestrator.printDebug) System.out.println("ORCHESTRATORE CLOUD: ho associato " + nodeList.get(nodeIndex).getName() + " al container generato dal " + container.getEdgeDevice(0).getName() + ", shared = " + container.getSharedContainer());
			// Send this task to this computing node
			container.setPlacementDestination(node);

			// Application has been deployed
			container.getEdgeDevice(container.getEdgeDevices().size() - 1).setApplicationPlacementLocation(node);
			simLog.deepLog(simulationManager.getSimulation().clock() + ": " + this.getClass() + " Task: " + container.getId()
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

	public boolean placementIsPossible(Container container, ComputingNode node, String[] architectureLayers) {					//modificato per visibilità degli agenti DQN, era protected
		if(container.getContainerSizeInMBytes() <= node.getAvailableRam() && container.getContainerSizeInMBytes() <= node.getAvailableStorage())
			return true;
		else
			return false;
		
	}

	public abstract void resultsReturned(Container container);

	public String[] getArchitectureLayers(){
		return architectureLayers;
	}

	static public List<ComputingNode> getNodeList(){
		return nodeList;
	}

	public abstract void notifyOrchestratorOfContainerExecution(Container container);
	
}
