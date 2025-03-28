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
import java.util.Map;
import java.util.HashMap;

import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.simulationengine.SimEntity;
import com.mechalikh.pureedgesim.simulationmanager.SimulationManager;

/**
 * The main class of the Network module, that handles all network events, and
 * updates the network topology status.
 *
 * @author Charafeddine Mechalikh
 * @since PureEdgeSim 5.0
 */
public abstract class NetworkModel extends SimEntity {
	public static final int SEND_REQUEST_FROM_EDGE_ORCH_TO_DESTINATION = 1;
	public static final int SEND_CONTAINER_FROM_CLOUD_ORCH_TO_DESTINATION = 11;
	public static final int SEND_CONTAINER_UNPLACEMENT_FROM_CLOUD_ORCH_TO_DESTINATION = 21;
	protected static final int TRANSFER_FINISHED = 2;
	protected static final int CONTAINER_TRANSFER_FINISHED = 12;
	public static final int DOWNLOAD_CONTAINER = 3;
	public static final int SEND_REQUEST_FROM_DEVICE_TO_EDGE_ORCH = 4;
	public static final int SEND_REQUEST_FROM_DEVICE_TO_CLOUD_ORCH = 14;
	public static final int SEND_UNPLACEMENT_REQUEST_FROM_DEVICE_TO_CLOUD_ORCH = 24;
	public static final int SEND_RESULT_TO_EDGE_ORCH = 6;
	public static final int SEND_RESULT_TO_CLOUD_ORCH = 16;
	public static final int SEND_UNPLACEMENT_RESULT_TO_CLOUD_ORCH = 26;
	public static final int SEND_RESULT_FROM_CLOUD_TO_EDGE_ORCH = 18;
	public static final int SEND_UNPLACEMENT_RESULT_FROM_CLOUD_TO_EDGE_ORCH = 28;
	public static final int SEND_RESULT_FROM_EDGE_ORCH_TO_DEV = 7;
	public static final int SEND_RESULT_FROM_CLOUD_ORCH_TO_DEV = 17;
	public static final int SEND_UNPLACEMENT_RESULT_FROM_CLOUD_ORCH_TO_DEV = 27;
	// the list where the current (and the previous)
	// transferred files are stored
	protected SimulationManager simulationManager;
	protected NetworkLinkWanUp wanUp;
	protected NetworkLinkWanDown wanDown;
	protected List<NetworkLink> FiberUpList = new ArrayList<>();
	protected List<NetworkLink> FiberDownList = new ArrayList<>();
	protected Map<String,Double> appBandwidthUpMap = new HashMap<>();
	protected Map<String,Double> appBandwidthDownMap = new HashMap<>();

	protected NetworkModel(SimulationManager simulationManager) {
		super(simulationManager.getSimulation());
		setSimulationManager(simulationManager);
		simulationManager.setNetworkModel(this);
	}

	protected void setSimulationManager(SimulationManager simulationManager) {
		this.simulationManager = simulationManager;
	}

	protected abstract void updateEdgeDevicesRemainingEnergy(TransferProgress transfer, ComputingNode origin,
			ComputingNode destination);

	protected abstract void transferFinished(TransferProgress transfer);

	public void setWanLinks(NetworkLinkWanUp wanUp, NetworkLinkWanDown wanDown) {
		this.wanUp = wanUp;
		this.wanDown = wanDown;
	}
	
	public Map<String,Double>/* Double*/ getFiberUpUtilization() {
		//double totalUsedBandwidth = FiberUpList.stream().mapToDouble(NetworkLink::getUsedBandwidth).sum();
		
		for (Map.Entry<String, Double> entry : appBandwidthUpMap.entrySet()) {
            entry.setValue(0.0);
        }
		    
		for (NetworkLink link : FiberUpList) {
			for(Bandwidth bandwidth : link.getUsedBandwidthList()) {
				appBandwidthUpMap.put(bandwidth.ApplicationName, appBandwidthUpMap.getOrDefault(bandwidth.ApplicationName, 0.0) + bandwidth.getCustomUsedBandwidth());
			}
		}
		/*
		for (Map.Entry<String, Double> entry : appBandwidthUpMap.entrySet()) {
            System.out.println("Chiave: " + entry.getKey() + ", Valore: " + entry.getValue());
        }
		*/
		return appBandwidthUpMap;
		 //return totalUsedBandwidth;
	}
	
	public Map<String,Double>/* Double*/ getFiberDownUtilization() {
		//double totalUsedBandwidth = FiberDownList.stream().mapToDouble(NetworkLink::getUsedBandwidth).sum();
		
		for (Map.Entry<String, Double> entry : appBandwidthDownMap.entrySet()) {
            entry.setValue(0.0);
        }
		
		for (NetworkLink link : FiberDownList) {
			for(Bandwidth bandwidth : link.UsedBandwidthList) {
				appBandwidthDownMap.put(bandwidth.ApplicationName, appBandwidthDownMap.getOrDefault(bandwidth.ApplicationName, 0.0) + bandwidth.getCustomUsedBandwidth());
			}
		}
		return appBandwidthDownMap;
		//return totalUsedBandwidth;
	}

	public double getWanUpUtilization() {
		if (!SimulationParameters.useOneSharedWanLink)
			throw new IllegalArgumentException(getClass().getSimpleName()
					+ " - The \"one_shared_wan_network\" option needs to be enabled in simulation_parameters.properties file in  in order to call \"getWanUpUtilization()\"");
		return wanUp.getUsedBandwidth();
	}

	public double getWanDownUtilization() {
		if (!SimulationParameters.useOneSharedWanLink)
			throw new IllegalArgumentException(getClass().getSimpleName()
					+ " - The \"one_shared_wan_network\" option needs to be enabled in simulation_parameters.properties file in order to call \"getWanDownUtilization()\"");
		return wanDown.getUsedBandwidth();
	}
	
	public List<NetworkLink> getFiberUp() {
		return this.FiberUpList;
	}
	
	public List<NetworkLink> getFiberDown() {
		return this.FiberDownList;
	}

}
