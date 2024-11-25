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
package com.mechalikh.pureedgesim.network;

import java.util.ArrayList;
import java.util.List;

import org.jgrapht.GraphPath;

import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode;
import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode.LinkOrientation;
import com.mechalikh.pureedgesim.energy.EnergyModelComputingNode;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters.TYPES;
import com.mechalikh.pureedgesim.simulationengine.Event;
import com.mechalikh.pureedgesim.simulationmanager.DefaultSimulationManager;
import com.mechalikh.pureedgesim.simulationmanager.SimulationManager;
import com.mechalikh.pureedgesim.taskgenerator.Task;
import com.mechalikh.pureedgesim.taskgenerator.Container;

public class DefaultNetworkModel extends NetworkModel {

	private boolean printDebug = false;

	public DefaultNetworkModel(SimulationManager simulationManager) {
		super(simulationManager);
	}

	@Override
	public void processEvent(Event ev) {
		switch (ev.getTag()) {
		case SEND_REQUEST_FROM_DEVICE_TO_EDGE_ORCH:
			// Send the offloading request to the orchestrator
			sendRequestFromDeviceToEdgeOrch((Task) ev.getData());
			break;
		case SEND_REQUEST_FROM_DEVICE_TO_CLOUD_ORCH:
			// Send the placement request to the orchestrator
			sendRequestFromDeviceToCloudOrch((Container) ev.getData());
			break;
		case SEND_UNPLACEMENT_REQUEST_FROM_DEVICE_TO_CLOUD_ORCH:
			// Send the placement request to the orchestrator
			sendUnplacementRequestFromDeviceToCloudOrch((Container) ev.getData());
			break;
		case SEND_REQUEST_FROM_EDGE_ORCH_TO_DESTINATION:
			// Forward the offloading request from orchestrator to offloading destination
			sendRequestFromEdgeOrchToDest((Task) ev.getData());
			break;
		case SEND_CONTAINER_FROM_CLOUD_ORCH_TO_DESTINATION:
			// Forward the placement request from orchestrator to placement destination
			sendContainerFromCloudOrchToDest((Container) ev.getData());
			break;
		case SEND_CONTAINER_UNPLACEMENT_FROM_CLOUD_ORCH_TO_DESTINATION:
			// Forward the placement request from orchestrator to placement destination
			sendContainerUnplacementFromCloudOrchToDest((Container) ev.getData());
			break;
		case SEND_RESULT_TO_EDGE_ORCH:
			// Send the execution results to the orchestrator
			sendResultFromDevToEdgeOrch((Task) ev.getData());
			break;
		case SEND_RESULT_TO_CLOUD_ORCH:
			// Send the execution results to the orchestrator
			sendResultFromDevToCloudOrch((Container) ev.getData());
			break;
		case SEND_UNPLACEMENT_RESULT_TO_CLOUD_ORCH:
			// Send the execution results to the orchestrator
			sendUnplacementResultFromDevToCloudOrch((Container) ev.getData());
			break;
		case SEND_RESULT_FROM_CLOUD_TO_EDGE_ORCH:
			// Send the placement destination to the SDN
			sendResultFromCloudToEdgeOrch((Container) ev.getData());
			break;
		case SEND_UNPLACEMENT_RESULT_FROM_CLOUD_TO_EDGE_ORCH:
			// Send the placement destination to the SDN
			sendUnplacementResultFromCloudToEdgeOrch((Container) ev.getData());
			break;
		case SEND_RESULT_FROM_EDGE_ORCH_TO_DEV:
			// Transfer the execution results from the orchestrators to the device
			sendResultFromEdgeOrchToDev((Task) ev.getData());
			break;
		case SEND_RESULT_FROM_CLOUD_ORCH_TO_DEV:
			// Transfer the execution results from the orchestrators to the device
			sendResultFromCloudOrchToDev((Container) ev.getData());
			break;	
		case SEND_UNPLACEMENT_RESULT_FROM_CLOUD_ORCH_TO_DEV:
			// Transfer the execution results from the orchestrators to the device
			sendUnplacementResultFromCloudOrchToDev((Container) ev.getData());
			break;	
		case TRANSFER_FINISHED:
			// Transfer the execution results from the orchestrators to the device
			transferFinished((TransferProgress) ev.getData());
			break;
		case CONTAINER_TRANSFER_FINISHED:
			// Transfer the execution results from the orchestrators to the device
			containerTransferFinished((ContainerTransferProgress) ev.getData());
			break;
		default:
			break;
		}
	}

	//invia la richiesta di offloading del task verso la destinazione
	public void sendTask(ComputingNode from, ComputingNode to, Task task, double fileSize, TransferProgress.Type type) {
		List<ComputingNode> vertexList = new ArrayList<>(5);
		List<NetworkLink> edgeList = new ArrayList<>(5);

		GraphPath<ComputingNode, NetworkLink> path;
		long id = simulationManager.getDataCentersManager().getTopology().getUniqueId(from.getId(), to.getId());

		if (simulationManager.getDataCentersManager().getTopology().getPathsMap().containsKey(id)) {
			path = simulationManager.getDataCentersManager().getTopology().getPathsMap().get(id);

			//it happened that the path had length 0 so it caused an error. In this case we establish a new path
			if(path.getLength() == 0){
				path = simulationManager.getDataCentersManager().getTopology().getPath(from, to);
				simulationManager.getDataCentersManager().getTopology().getPathsMap().put(id, path);
			}
			if(printDebug){
				System.out.println("from: " + from.getName() + ", to: " + to.getName());
				System.out.print("[");
				for(ComputingNode cn : path.getVertexList()) System.out.print(cn.getName() + ", ");
				System.out.print("]");
				System.out.println("");
			}
		} else {
			path = simulationManager.getDataCentersManager().getTopology().getPath(from, to);
			simulationManager.getDataCentersManager().getTopology().getPathsMap().put(id, path);
			if(printDebug){
				System.out.println("from: " + from.getName() + ", to: " + to.getName());
				System.out.print("[");
				for(ComputingNode cn : path.getVertexList()) System.out.print(cn.getName() + ", ");
				System.out.print("]");
				System.out.println("");
			}
		}
		vertexList.addAll(path.getVertexList());
		edgeList.addAll(path.getEdgeList());
		
		edgeList.get(0).addTaskTransfer(new TransferProgress(task, fileSize, type).setVertexList(vertexList).setEdgeList(edgeList));
	}
	
	//invia la richiesta di placement del container verso il cloud
	public void sendContainer(ComputingNode from, ComputingNode to, Container container, double fileSize, ContainerTransferProgress.Type type){
		List<ComputingNode> vertexList = new ArrayList<>(5);
		List<NetworkLink> edgeList = new ArrayList<>(5);GraphPath<ComputingNode, NetworkLink> path;
		
		//long id = simulationManager.getDataCentersManager().getTopology().getUniqueId(from.getName() + from.getId(), to.getName() + to.getId());
		long id = simulationManager.getDataCentersManager().getTopology().getUniqueId(from.getId(), to.getId());

		if (simulationManager.getDataCentersManager().getTopology().getPathsMap().containsKey(id)) {
			path = simulationManager.getDataCentersManager().getTopology().getPathsMap().get(id);

			//it happened that the path had length 0 so it caused an error. In this case we establish a new path
			if(path.getLength() == 0){
				path = simulationManager.getDataCentersManager().getTopology().getPath(from, to);
				simulationManager.getDataCentersManager().getTopology().getPathsMap().put(id, path);
			}
			if(printDebug){
				System.out.println("from: " + from.getName() + ", to: " + to.getName());
				System.out.print("[");
				for(ComputingNode cn : path.getVertexList()) System.out.print(cn.getName() + ", ");
				System.out.print("]");
				System.out.println("");
			}

		} else {
			path = simulationManager.getDataCentersManager().getTopology().getPath(from, to);
			simulationManager.getDataCentersManager().getTopology().getPathsMap().put(id, path);
			if(printDebug){
				System.out.println("from: " + from.getName() + ", to: " + to.getName());
				System.out.print("[");
				for(ComputingNode cn : path.getVertexList()) System.out.print(cn.getName() + ", ");
				System.out.print("]");
				System.out.println("");
			}
		}
		vertexList.addAll(path.getVertexList());
		edgeList.addAll(path.getEdgeList());

		edgeList.get(0).addContainerRequestTransfer(
				new ContainerTransferProgress(container, fileSize, type).setVertexList(vertexList).setEdgeList(edgeList));
	}

	protected void transferFinished(TransferProgress transfer) {

		// If it is an offloading request that is sent to the orchestrator
		if (transfer.getTransferType() == TransferProgress.Type.REQUEST) {
			// in case this node is the orchestrator

			if (transfer.getVertexList().get(0) == transfer.getTask().getOrchestrator()) {
				updateEdgeDevicesRemainingEnergy(transfer, transfer.getTask().getEdgeDevice(),
						transfer.getTask().getOrchestrator());
			}
			offloadingRequestRecievedByOrchestrator(transfer);
		}
		// If it is a task (or offloading request) that is sent to the destination
		else if (transfer.getTransferType() == TransferProgress.Type.TASK) {
			// in case this node is the destination
			if (transfer.getVertexList().get(0) == transfer.getTask().getOffloadingDestination()) {
				updateEdgeDevicesRemainingEnergy(transfer, transfer.getTask().getEdgeDevice(),
						transfer.getTask().getOffloadingDestination());
			}

			executeTaskOrDownloadContainer(transfer);
		}
		// If the transfer of execution results to the orchestrator has finished
		else if (transfer.getTransferType() == TransferProgress.Type.RESULTS_TO_ORCH) {
			returnResultsToDeviceFromEdge(transfer);
			updateEdgeDevicesRemainingEnergy(transfer, transfer.getTask().getOffloadingDestination(),
					transfer.getTask().getOrchestrator());
		}
		// Results transferred to the device
		else {
			resultsReturnedToDeviceFromEdge(transfer);
			updateEdgeDevicesRemainingEnergy(transfer, transfer.getTask().getOrchestrator(),
					transfer.getTask().getEdgeDevice());
		}

	}

	protected void containerTransferFinished(ContainerTransferProgress transfer) {

		// If it is a placement request that is sent to the orchestrator
		if (transfer.getTransferType() == ContainerTransferProgress.Type.REQUEST) {
			//if (transfer.getVertexList().get(0) == transfer.getContainer().getOrchestrator()) 
			//	updateEdgeDevicesRemainingEnergy(transfer, transfer.getContainer().getEdgeDevice(0), transfer.getContainer().getOrchestrator());
			offloadingRequestRecievedByCloudOrchestrator(transfer);
		}
		// If it is an Unplacement request that is sent to the orchestrator
		else if (transfer.getTransferType() == ContainerTransferProgress.Type.UNPLACEMENT_REQUEST) {
			//if (transfer.getVertexList().get(0) == transfer.getContainer().getOrchestrator()) 
			//	updateEdgeDevicesRemainingEnergy(transfer, transfer.getContainer().getEdgeDevice(0), transfer.getContainer().getOrchestrator());
			unplacementRequestRecievedByCloudOrchestrator(transfer);
		}
		// If the container has been downloaded, then execute the task now
		else if (transfer.getTransferType() == ContainerTransferProgress.Type.CONTAINER) {
			containerDownloadFinished(transfer);
		}
		// container unplacement request arrived
		else if (transfer.getTransferType() == ContainerTransferProgress.Type.CONTAINER_UNPLACEMENT) {
			containerUnplacementFinished(transfer);
		}
		// If the transfer of execution results to the orchestrator has finished
		else if (transfer.getTransferType() == ContainerTransferProgress.Type.RESULTS_TO_ORCH) {
			//invio il risultato al device che ha richiesto il placement
			if(!transfer.getContainer().getSharedContainer()) returnResultsToDeviceFromCloud(transfer);
			//Avviso l'SDN che ho piazzato il container
			returnResultsFromCloudToEdgeOrch(transfer);
			//updateEdgeDevicesRemainingEnergy(transfer, transfer.getContainer().getPlacementDestination(), transfer.getContainer().getOrchestrator());
		}
		// If the transfer of execution results to the orchestrator has finished
		else if (transfer.getTransferType() == ContainerTransferProgress.Type.UNPLACEMENT_RESULTS_TO_ORCH) {
			//invio il risultato al device che ha richiesto il placement
			returnUnplacementResultsToDeviceFromCloud(transfer);
			//Avviso l'SDN che ho piazzato il container
			returnUnplacementResultsFromCloudToEdgeOrch(transfer);
			//updateEdgeDevicesRemainingEnergy(transfer, transfer.getContainer().getPlacementDestination(), transfer.getContainer().getOrchestrator());
		}
		// If the transfer of execution results to the orchestrator has finished
		else if (transfer.getTransferType() == ContainerTransferProgress.Type.RESULT_FROM_CLOUD_TO_EDGE) {
			resultsReturnedFromCloudToEdge(transfer);
			//updateEdgeDevicesRemainingEnergy(transfer, transfer.getContainer().getPlacementDestination(), transfer.getContainer().getOrchestrator());
		}
		// If the transfer of execution results to the orchestrator has finished
		else if (transfer.getTransferType() == ContainerTransferProgress.Type.UNPLACEMENT_RESULT_FROM_CLOUD_TO_EDGE) {
			unplacementResultsReturnedFromCloudToEdge(transfer);
			//updateEdgeDevicesRemainingEnergy(transfer, transfer.getContainer().getPlacementDestination(), transfer.getContainer().getOrchestrator());
		}
		// Results transferred to the device
		else if (transfer.getTransferType() == ContainerTransferProgress.Type.UNPLACEMENT_RESULTS_TO_DEV) {
			unplacementResultsReturnedToDeviceFromCloud(transfer);
			//updateEdgeDevicesRemainingEnergy(transfer, transfer.getContainer().getOrchestrator(), transfer.getContainer().getEdgeDevice(0));
		}
		// Results transferred to the device
		else {
			resultsReturnedToDeviceFromCloud(transfer);
			//updateEdgeDevicesRemainingEnergy(transfer, transfer.getContainer().getOrchestrator(), transfer.getContainer().getEdgeDevice(0));
		}

	}

	public void sendRequestFromEdgeOrchToDest(Task task) {
		if (task.getOrchestrator() != task.getOffloadingDestination()
				&& task.getOffloadingDestination() != task.getEdgeDevice())

			sendTask(task.getOrchestrator(), task.getOffloadingDestination(), task, task.getFileSizeInBits(),
					TransferProgress.Type.TASK);
		else // The device will execute the task locally
			executeTaskOrDownloadContainer(
					new TransferProgress(task, task.getFileSizeInBits(), TransferProgress.Type.TASK));
	}

	public void sendContainerFromCloudOrchToDest(Container container) {
		//se il container non è già stato piazzato allora invio tutto il container
		if(container.getStatus().equals(Container.Status.NOT_PLACED)) sendContainer(container.getOrchestrator(), container.getPlacementDestination(), container, container.getContainerSizeInBits(), ContainerTransferProgress.Type.CONTAINER);
		//se il container è già stato piazzato poichè è "shared" allora non invio tutto il container ma solo la request
		else sendContainer(container.getOrchestrator(), container.getPlacementDestination(), container, container.getFileSizeInBits(), ContainerTransferProgress.Type.CONTAINER);
	}

	public void sendContainerUnplacementFromCloudOrchToDest(Container container) {
		sendContainer(container.getOrchestrator(), container.getPlacementDestination(), container, container.getFileSizeInBits(), ContainerTransferProgress.Type.CONTAINER_UNPLACEMENT);
	}

	public void sendResultFromEdgeOrchToDev(Task task) {
		if (task.getOrchestrator() != task.getEdgeDevice())
			sendTask(task.getOrchestrator(), task.getEdgeDevice(), task, task.getOutputSizeInBits(),
					TransferProgress.Type.RESULTS_TO_DEV);
		else
			scheduleNow(simulationManager, DefaultSimulationManager.TASK_RESULT_RETURN_FINISHED, task);

	}

	public void sendResultFromCloudOrchToDev(Container container) {
		sendContainer(container.getOrchestrator(), container.getEdgeDevice(0), container, container.getFileSizeInBits(), ContainerTransferProgress.Type.RESULTS_TO_DEV);
	}

	public void sendUnplacementResultFromCloudOrchToDev(Container container) {
		sendContainer(container.getOrchestrator(), container.getEdgeDevice(0), container, container.getFileSizeInBits(), ContainerTransferProgress.Type.UNPLACEMENT_RESULTS_TO_DEV);
	}

	public void sendResultFromDevToEdgeOrch(Task task) {
		if (task.getOffloadingDestination() != task.getOrchestrator())
			sendTask(task.getOffloadingDestination(), task.getOrchestrator(), task, task.getOutputSizeInBits(),
					TransferProgress.Type.RESULTS_TO_ORCH);
		else
			scheduleNow(this, DefaultNetworkModel.SEND_RESULT_FROM_EDGE_ORCH_TO_DEV, task);

	}

	public void sendResultFromDevToCloudOrch(Container container) {
		sendContainer(container.getPlacementDestination(), container.getOrchestrator(), container, container.getFileSizeInBits(), ContainerTransferProgress.Type.RESULTS_TO_ORCH);
	}

	public void sendUnplacementResultFromDevToCloudOrch(Container container) {
		sendContainer(container.getPlacementDestination(), container.getOrchestrator(), container, container.getFileSizeInBits(), ContainerTransferProgress.Type.UNPLACEMENT_RESULTS_TO_ORCH);
	}

	public void sendRequestFromDeviceToEdgeOrch(Task task) {
		sendTask(task.getEdgeDevice(), task.getOrchestrator(), task, task.getFileSizeInBits(),TransferProgress.Type.REQUEST);
	}
	
	public void sendRequestFromDeviceToCloudOrch(Container container) {
		sendContainer(container.getEdgeDevice(container.getEdgeDevices().size() - 1), container.getOrchestrator(), container, container.getFileSizeInBits(), ContainerTransferProgress.Type.REQUEST);
	}

	public void sendUnplacementRequestFromDeviceToCloudOrch(Container container) {
		sendContainer(container.getEdgeDevice(container.getEdgeDevices().size() - 1), container.getOrchestrator(), container, container.getFileSizeInBits(), ContainerTransferProgress.Type.UNPLACEMENT_REQUEST);
	}

	public void sendResultFromCloudToEdgeOrch(Container container) {
		//se il container non è shared invio all'edgeOrch associato al container
		if(!container.getSharedContainer())
			sendContainer(container.getOrchestrator(), container.getEdgeDevices().get(0).getEdgeOrchestrator(), container, container.getFileSizeInBits(), ContainerTransferProgress.Type.RESULT_FROM_CLOUD_TO_EDGE);
		//se invece il container è shared allora invio all'orchestratore associato a ciascun nodo relativo al container
		else {
			if(printDebug) System.out.println("Sto generando il flusso per il container " + container.getId() + " associato all'app " + container.getAssociatedAppName());
			List<ComputingNode> orchList = new ArrayList<ComputingNode>();
			for(ComputingNode cn : container.getEdgeDevices()){
				if(!orchList.contains(cn.getEdgeOrchestrator())){
					orchList.add(cn.getEdgeOrchestrator());
					sendContainer(container.getOrchestrator(), cn.getEdgeOrchestrator(), container, container.getFileSizeInBits(), ContainerTransferProgress.Type.RESULT_FROM_CLOUD_TO_EDGE);
				}
			}
		}
	}

	public void sendUnplacementResultFromCloudToEdgeOrch(Container container) {
		sendContainer(container.getOrchestrator(), container.getEdgeDevices().get(0).getEdgeOrchestrator(), container, container.getFileSizeInBits(), ContainerTransferProgress.Type.UNPLACEMENT_RESULT_FROM_CLOUD_TO_EDGE);
	}	

	protected void updateEdgeDevicesRemainingEnergy(TransferProgress transfer, ComputingNode origin,
			ComputingNode destination) {
		if (origin != ComputingNode.NULL && origin.getType() == TYPES.EDGE_DEVICE) {
			origin.getEnergyModel().updatewirelessEnergyConsumption(transfer.getFileSize(),
					EnergyModelComputingNode.TRANSMISSION);
		}
		if (destination.getType() == TYPES.EDGE_DEVICE)
			destination.getEnergyModel().updatewirelessEnergyConsumption(transfer.getFileSize(),
					EnergyModelComputingNode.RECEPTION);
	}

	protected void containerDownloadFinished(ContainerTransferProgress transfer) {
		scheduleNow(simulationManager, DefaultSimulationManager.DOWNLOAD_CONTAINER, transfer.getContainer());
	}

	protected void containerUnplacementFinished(ContainerTransferProgress transfer) {
		scheduleNow(simulationManager, DefaultSimulationManager.UNPLACEMENT_CONTAINER, transfer.getContainer());
	}

	protected void resultsReturnedFromCloudToEdge(ContainerTransferProgress transfer) {
		scheduleNow(simulationManager, DefaultSimulationManager.RESULTS_FROM_CLOUD_TO_EDGE_ORCH, transfer.getContainer());
	}

	protected void unplacementResultsReturnedFromCloudToEdge(ContainerTransferProgress transfer) {
		scheduleNow(simulationManager, DefaultSimulationManager.UNPLACEMENT_RESULTS_FROM_CLOUD_TO_EDGE_ORCH, transfer.getContainer());
	}

	protected void resultsReturnedToDeviceFromEdge(TransferProgress transfer) {
		scheduleNow(simulationManager, DefaultSimulationManager.TASK_RESULT_RETURN_FINISHED, transfer.getTask());
	}

	protected void resultsReturnedToDeviceFromCloud(ContainerTransferProgress transfer) {
		scheduleNow(simulationManager, DefaultSimulationManager.PLACEMENT_RESULT_RETURN_FINISHED, transfer.getContainer());
	}

	protected void unplacementResultsReturnedToDeviceFromCloud(ContainerTransferProgress transfer) {
		scheduleNow(simulationManager, DefaultSimulationManager.UNPLACEMENT_RESULT_RETURN_FINISHED, transfer.getContainer());
	}

	protected void returnResultsToDeviceFromEdge(TransferProgress transfer) {
		scheduleNow(this, NetworkModel.SEND_RESULT_FROM_EDGE_ORCH_TO_DEV, transfer.getTask());
	}	
	
	protected void returnResultsToDeviceFromCloud(ContainerTransferProgress transfer) {
		scheduleNow(this, NetworkModel.SEND_RESULT_FROM_CLOUD_ORCH_TO_DEV, transfer.getContainer());
	}

	protected void returnUnplacementResultsToDeviceFromCloud(ContainerTransferProgress transfer) {
		scheduleNow(this, NetworkModel.SEND_UNPLACEMENT_RESULT_FROM_CLOUD_ORCH_TO_DEV, transfer.getContainer());
	}

	protected void returnResultsFromCloudToEdgeOrch(ContainerTransferProgress transfer) {
		scheduleNow(this, NetworkModel.SEND_RESULT_FROM_CLOUD_TO_EDGE_ORCH, transfer.getContainer());
	}
	
	protected void returnUnplacementResultsFromCloudToEdgeOrch(ContainerTransferProgress transfer) {
		scheduleNow(this, NetworkModel.SEND_UNPLACEMENT_RESULT_FROM_CLOUD_TO_EDGE_ORCH, transfer.getContainer());
	}

	protected void executeTaskOrDownloadContainer(TransferProgress transfer) {
		if (SimulationParameters.enableRegistry && "CLOUD".equals(SimulationParameters.registryMode)
				&& !(transfer.getTask().getOffloadingDestination()).getType().equals(TYPES.CLOUD)) {
			// If the registry is enabled and the task is offloaded to the edge data centers
			// or the mist nodes (edge devices),
			// then download the container
			scheduleNow(this, DefaultNetworkModel.DOWNLOAD_CONTAINER, transfer.getTask());

		} else// if the registry is disabled, execute directly the task
			scheduleNow(simulationManager, DefaultSimulationManager.EXECUTE_TASK, transfer.getTask());
	}

	protected void offloadingRequestRecievedByOrchestrator(TransferProgress transfer) {
		// Find the offloading destination and execute the task
		scheduleNow(simulationManager, DefaultSimulationManager.SEND_TASK_FROM_EDGE_ORCH_TO_DESTINATION, transfer.getTask());
	}

	protected void offloadingRequestRecievedByCloudOrchestrator(ContainerTransferProgress transfer) {
		// Find the placement destination and download the container
		scheduleNow(simulationManager, DefaultSimulationManager.SEND_CONTAINER_FROM_CLOUD_ORCH_TO_VM, transfer.getContainer());
	}
	
	protected void unplacementRequestRecievedByCloudOrchestrator(ContainerTransferProgress transfer) {
		// Find the placement destination and download the container
		scheduleNow(simulationManager, DefaultSimulationManager.SEND_UNPLACEMENT_FROM_CLOUD_ORCH_TO_VM, transfer.getContainer());
	}
	
}
