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

import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode;
import com.mechalikh.pureedgesim.energy.EnergyModelNetworkLink;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.simulationengine.Event;
import com.mechalikh.pureedgesim.simulationengine.SimEntity;
import com.mechalikh.pureedgesim.simulationmanager.DefaultSimulationManager;
import com.mechalikh.pureedgesim.simulationmanager.SimulationManager;

/**
 * Link between two compute nodes in the infrastructure graph
 */
public class NetworkLink extends SimEntity {
	public static final int UPDATE_PROGRESS = 1;
	protected double latency = 0;
	protected double bandwidth = 0;
	protected List<TransferProgress> transferProgressList = new ArrayList<>();
	protected List<ContainerTransferProgress> containerRequestTransferProgressList = new ArrayList<>();
	protected ComputingNode src = ComputingNode.NULL;
	protected ComputingNode dst = ComputingNode.NULL;
	protected SimulationManager simulationManager;
	protected double usedBandwidth = 0;
	protected List<Bandwidth> UsedBandwidthList = new ArrayList<>();
	protected double totalTrasferredData = 0;
	protected EnergyModelNetworkLink energyModel = EnergyModelNetworkLink.NULL;
	protected boolean scheduled = false;

	public enum NetworkLinkTypes {
		WAN, FIBER, MAN, LAN, IGNORE, HYPER
	}

	protected NetworkLinkTypes type;

	public static final NetworkLink NULL = new NetworkLinkNull();

	public NetworkLink(ComputingNode src, ComputingNode dst, SimulationManager simulationManager,
			NetworkLinkTypes type) {
		super(simulationManager.getSimulation());
		this.simulationManager = simulationManager;
		this.src = src;
		this.dst = dst;
		this.setType(type);
	}

	public NetworkLink() {
	}

	public double getLatency() {
		return latency;
	}

	public NetworkLink setLatency(double latency) {
		this.latency = latency;
		return this;
	}

	public NetworkLink setBandwidth(double bandwidth) {
		this.bandwidth = bandwidth;
		return this;
	}

	public ComputingNode getSrc() {
		return src;
	}

	public void setSrc(ComputingNode src) {
		this.src = src;
	}

	public ComputingNode getDst() {
		return dst;
	}

	public void setDst(ComputingNode node) {
		this.dst = node;
	}

	@Override
	public void processEvent(Event evt) {
		if (evt.getTag() == UPDATE_PROGRESS) {
			// Update the progress of the current transfers and their allocated bandwidth
			updateTransfersProgress();
			if (this.transferProgressList.size() != 0 || this.containerRequestTransferProgressList.size() != 0)
				schedule(this, SimulationParameters.networkUpdateInterval, UPDATE_PROGRESS);
			else
				scheduled = false;
		}

	}

	protected void updateTransfersProgress() {
		usedBandwidth = 0;
		for (Bandwidth bandwidth : UsedBandwidthList) {
			bandwidth.usedBandwidth = 0;
		}

		double allocatedBandwidth = 0;

		for (int i = 0; i < transferProgressList.size(); i++) {

			if (!SimulationParameters.BandwidthAllocationOnApplicationType) {
				allocatedBandwidth = getBandwidth(transferProgressList.size());
			} else {
				allocatedBandwidth = getBandwidthOnType(transferProgressList.get(i));
			}

			// Allocate bandwidth
			usedBandwidth += transferProgressList.get(i).getRemainingFileSize();

			if (this.getType() == NetworkLinkTypes.FIBER)
				UpdateBandwidth(transferProgressList.get(i));

			transferProgressList.get(i).setCurrentBandwidth(allocatedBandwidth);
			updateTransfer(transferProgressList.get(i));
		}

		for (int i = 0; i < containerRequestTransferProgressList.size(); i++) {

			if (!SimulationParameters.BandwidthAllocationOnApplicationType) {
				allocatedBandwidth = getBandwidth(containerRequestTransferProgressList.size());
			} else {
				allocatedBandwidth = getBandwidthOnContainerRequestType(containerRequestTransferProgressList.get(i));
			}

			// Allocate bandwidth
			usedBandwidth += containerRequestTransferProgressList.get(i).getRemainingFileSize();

			if (this.getType() == NetworkLinkTypes.FIBER)
				UpdateContainerRequestBandwidth(containerRequestTransferProgressList.get(i));

			containerRequestTransferProgressList.get(i).setCurrentBandwidth(allocatedBandwidth);
			updateContainerRequestTransfer(containerRequestTransferProgressList.get(i));
		}

	}

	protected void UpdateBandwidth(TransferProgress T) {
		boolean found = false;
		for (Bandwidth bandwidth : UsedBandwidthList) {
			if (bandwidth.ApplicationName.equals(T.getTask().getAssociatedAppName())) {
				bandwidth.usedBandwidth += T.getRemainingFileSize();
				found = true;
				break;
			}
		}
		if (!found) {
			UsedBandwidthList
					.add(new Bandwidth(T.getTask().getAssociatedAppName(), bandwidth, T.getRemainingFileSize()));
			// System.out.println("Nuovo oggetto bandwidth relativo all'app" +
			// T.getTask().getAssociatedAppName() + "aggiunto al link: " + this.getId());
		}
	}

	protected void UpdateContainerRequestBandwidth(ContainerTransferProgress T) {
		boolean found = false;
		for (Bandwidth bandwidth : UsedBandwidthList) {
			if (bandwidth.ApplicationName.equals(T.getContainer().getAssociatedAppName())) {
				bandwidth.usedBandwidth += T.getRemainingFileSize();
				found = true;
				break;
			}
		}
		if (!found) {
			UsedBandwidthList
					.add(new Bandwidth(T.getContainer().getAssociatedAppName(), bandwidth, T.getRemainingFileSize()));
			// System.out.println("Nuovo oggetto bandwidth relativo all'app" +
			// T.getTask().getAssociatedAppName() + "aggiunto al link: " + this.getId());
		}
	}

	protected double getBandwidth(double remainingTasksCount) {
		return (bandwidth / (remainingTasksCount > 0 ? remainingTasksCount : 1));
	}

	protected double getBandwidthOnType(TransferProgress task) {
		double BandwidthValue = 0;

		if (SimulationParameters.AllocationValue.equals("LATENCY")) {

			double latency = task.getTask().getMaxLatency();

			if (latency <= 0.05) {
				BandwidthValue = bandwidth * 50 / 100;
			} else if (latency > 0.05 && latency <= 0.2) {
				BandwidthValue = bandwidth * 25 / 100;
			} else if (latency > 0.2) {
				BandwidthValue = bandwidth * 10 / 100;
			}

		} else {
			double task_length = task.getTask().getLength();

			if (task_length > 1000) {
				BandwidthValue = bandwidth * 50 / 100;
			} else if (task_length > 100 && task_length <= 1000) {
				BandwidthValue = bandwidth * 25 / 100;
			} else if (task_length <= 100) {
				BandwidthValue = bandwidth * 10 / 100;
			}

		}

		return BandwidthValue;
	}

	protected double getBandwidthOnContainerRequestType(ContainerTransferProgress task) {
		double BandwidthValue = 0;

		double container_size = task.getContainer().getFileSizeInBits();

		if (container_size > 1000) {
			BandwidthValue = bandwidth * 50 / 100;
		} else if (container_size > 100 && container_size <= 1000) {
			BandwidthValue = bandwidth * 25 / 100;
		} else if (container_size <= 100) {
			BandwidthValue = bandwidth * 10 / 100;
		}

		return BandwidthValue;
	}

	protected void updateTransfer(TransferProgress transfer) {

		double oldRemainingSize = transfer.getRemainingFileSize();

		// Update progress (remaining file size)
		if (SimulationParameters.realisticNetworkModel)
			transfer.setRemainingFileSize(transfer.getRemainingFileSize()
					- (SimulationParameters.networkUpdateInterval * transfer.getCurrentBandwidth()));
		else
			transfer.setRemainingFileSize(0);

		double transferDelay = (oldRemainingSize - transfer.getRemainingFileSize()) / transfer.getCurrentBandwidth();

		//calcolo la latency associata alla distanza:
		double x1 = transfer.getVertexList().get(0).getMobilityModel().getCurrentLocation().getXPos();
		double y1 = transfer.getVertexList().get(0).getMobilityModel().getCurrentLocation().getYPos();									//questi sono i due datacenter interessati, io devo tenere conto dei due vertici comunicanti
		double x2 = transfer.getVertexList().get(1).getMobilityModel().getCurrentLocation().getXPos();
		double y2 = transfer.getVertexList().get(1).getMobilityModel().getCurrentLocation().getYPos();
		double distance = Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
		// Velocità di propagazione della luce in fibra ottica (200,000 km/s)
		double propagationSpeed = 200000; // in km/s
		double distanceInKm = distance / 1000; // Conversione distanza in km
		// Latenza basata sulla distanza
		double distanceLatency = distanceInKm / propagationSpeed; // in secondi
		// System.out.print("Latenza su distanza" + distanceLatency + "\n");
		double latency_const = latency;
		transferDelay += (distanceLatency + latency_const);

		// System.out.println("Task " + transfer.getTask().getId() + " associato a " + transfer.getTask().getEdgeDevice().getType() + " " + transfer.getTask().getEdgeDevice().getId() 
		// 					+ " ho aggiunto: \n		-il delay " + transferDelay + " derivante dalla \n		-latency " + latency + " \n		-la distanza in metri " + distance + "dalla quale si ottiene la latency_dist " + distanceLatency
		// 					+ "\n		--precedente delay = " + (transferDelay - distanceLatency - latency_const)
		// 					+ "\n		per spostarmi: \n		da " + transfer.getVertexList().get(0).getType() + " " + transfer.getVertexList().get(0).getId()
		// 					+ "\n		verso " + transfer.getVertexList().get(1).getType() + " " + transfer.getVertexList().get(1).getId());

		// Set the task network delay to decide whether it has failed due to latency or
		// not.
		transfer.getTask().addActualNetworkTime(transferDelay);

		// Update network usage delay
		if (type == NetworkLinkTypes.LAN)
			transfer.setLanNetworkUsage(transfer.getLanNetworkUsage() + transferDelay);
		
		// Update FIBER network usage delay
		else if (type == NetworkLinkTypes.FIBER)
			transfer.setFiberNetworkUsage(transfer.getFiberNetworkUsage() + transferDelay);

		// Update MAN network usage delay
		else if (type == NetworkLinkTypes.MAN)
			transfer.setManNetworkUsage(transfer.getManNetworkUsage() + transferDelay);

		// Update WAN network usage delay
		else if (type == NetworkLinkTypes.WAN)
			transfer.setWanNetworkUsage(transfer.getWanNetworkUsage() + transferDelay);

		if (transfer.getRemainingFileSize() <= 0) { // Transfer finished
			transfer.setRemainingFileSize(0); // if < 0 set it to 0
			transferFinished(transfer);
		}
	}

	protected void updateContainerRequestTransfer(ContainerTransferProgress transfer) {

		double oldRemainingSize = transfer.getRemainingFileSize();

		// Update progress (remaining file size)
		if (SimulationParameters.realisticNetworkModel)
			transfer.setRemainingFileSize(transfer.getRemainingFileSize()
					- (SimulationParameters.networkUpdateInterval * transfer.getCurrentBandwidth()));
		else
			transfer.setRemainingFileSize(0);

		double transferDelay = (oldRemainingSize - transfer.getRemainingFileSize()) / transfer.getCurrentBandwidth();

		// Update network usage delay
		if (type == NetworkLinkTypes.LAN)
			transfer.setLanNetworkUsage(transfer.getLanNetworkUsage() + transferDelay);

		// Update FIBER network usage delay
		else if (type == NetworkLinkTypes.FIBER)
			transfer.setFiberNetworkUsage(transfer.getFiberNetworkUsage() + transferDelay);

		// Update MAN network usage delay
		else if (type == NetworkLinkTypes.MAN)
			transfer.setManNetworkUsage(transfer.getManNetworkUsage() + transferDelay);

		// Update WAN network usage delay
		else if (type == NetworkLinkTypes.WAN)
			transfer.setWanNetworkUsage(transfer.getWanNetworkUsage() + transferDelay);

		if (transfer.getRemainingFileSize() <= 0) { // Transfer finished
			transfer.setRemainingFileSize(0); // if < 0 set it to 0
			containerTransferFinished(transfer);
		}
	}

	protected void transferFinished(TransferProgress transfer) {

		this.transferProgressList.remove(transfer);

		// Add the network link latency to the task network delay
		transfer.getTask().addActualNetworkTime(0); //originariamente settato su latency, ora settato su 0 poichè la latency la sommo nella funzione updateTransfer

		// Remove the previous hop (data has been transferred one hop)
		transfer.getVertexList().remove(0);
		transfer.getEdgeList().remove(0);

		// Data has reached the destination
		if (transfer.getVertexList().size() == 1) {
			// Update logger parameters
			simulationManager.getSimulationLogger().updateNetworkUsage(transfer);

			schedule(simulationManager.getNetworkModel(), latency, NetworkModel.TRANSFER_FINISHED, transfer);
		} else {
			// Still did not reach destination, send it to the next hop
			transfer.setRemainingFileSize(transfer.getFileSize());
			transfer.getEdgeList().get(0).addTaskTransfer(transfer);
		}
	}

	protected void containerTransferFinished(ContainerTransferProgress transfer) {

		this.containerRequestTransferProgressList.remove(transfer);

		// Remove the previous hop (data has been transferred one hop)
		transfer.getVertexList().remove(0);
		transfer.getEdgeList().remove(0);

		// Data has reached the destination
		if (transfer.getVertexList().size() == 1) {
			// Update logger parameters
			simulationManager.getSimulationLogger().updateContainerNetworkUsage(transfer);

			schedule(simulationManager.getNetworkModel(), latency, NetworkModel.CONTAINER_TRANSFER_FINISHED, transfer);
		} else {
			// Still did not reach destination, send it to the next hop
			transfer.setRemainingFileSize(transfer.getFileSize());
			transfer.getEdgeList().get(0).addContainerRequestTransfer(transfer);
		}
	}

	public double getUsedBandwidth() {
		// Return bandwidth usage in bits per second
		return Math.min(bandwidth, usedBandwidth);
	}

	public NetworkLinkTypes getType() {
		return type;
	}

	public void setType(NetworkLinkTypes type) {
		this.type = type;
	}

	public void addTaskTransfer(TransferProgress transfer) {
		// Used by the energy model to get the total energy consumed by this network
		// link
		totalTrasferredData += transfer.getFileSize();
		transferProgressList.add(transfer);

		if (!scheduled) {
			scheduleNow(this, UPDATE_PROGRESS);
			scheduled = true;
		}
	}

	public void addContainerRequestTransfer(ContainerTransferProgress transfer) {
		// Used by the energy model to get the total energy consumed by this network
		// link
		totalTrasferredData += transfer.getFileSize();
		containerRequestTransferProgressList.add(transfer);

		if (!scheduled) {
			scheduleNow(this, UPDATE_PROGRESS);
			scheduled = true;
		}
	}

	public EnergyModelNetworkLink getEnergyModel() {
		return energyModel;
	}

	protected void setEnergyModel(EnergyModelNetworkLink energyModel) {
		this.energyModel = energyModel;
	}

	public double getTotalTransferredData() {
		return totalTrasferredData;
	}

	public List<Bandwidth> getUsedBandwidthList() {
		return this.UsedBandwidthList;
	}

}
