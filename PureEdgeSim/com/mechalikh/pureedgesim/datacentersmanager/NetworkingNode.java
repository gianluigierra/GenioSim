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
package com.mechalikh.pureedgesim.datacentersmanager;

import java.util.ArrayList;
import java.util.List;

import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode.LinkOrientation;
import com.mechalikh.pureedgesim.network.NetworkLink;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.simulationmanager.SimulationManager;

public abstract class NetworkingNode extends AbstractNode  {
	protected NetworkLink currentUpLink = NetworkLink.NULL;
	protected NetworkLink currentDownLink = NetworkLink.NULL;
	protected NetworkLink currentDeviceToDeviceWifiLink = NetworkLink.NULL;
	protected boolean isConnect; //Verify if the ONT Device is already connect to an edge device
	List<ComputingNode> vertexList = new ArrayList<>();
	List<NetworkLink> edgeList = new ArrayList<>();

	protected NetworkingNode(SimulationManager simulationManager) {
		super(simulationManager);
	}
	
	public boolean isConnect() {
		return isConnect;
	}
	
	public void setAsConnect(boolean isConnect) {
		this.isConnect = isConnect;
	}
	
	/**
	 * Returns the current network link of the specified type. For example, if the
	 * type is {@link LinkOrientation#UP_LINK}, it returns the link that is used currently
	 * to send data to the cloud or edge data centers. Used only when the type of
	 * this node is {@link SimulationParameters.TYPES#EDGE_DEVICE}.
	 * 
	 * @param linkType the type of the link to retrieve
	 * @return the current network link of the specified type
	 * @see #setCurrentLink(NetworkLink,LinkOrientation)
	 */
	public NetworkLink getCurrentLink(LinkOrientation linkType) {
		switch (linkType) {
		case UP_LINK:
			return this.currentUpLink;
		case DOWN_LINK:
			return this.currentDownLink;							//MODIFICA MIA, prima era uplink....ho dovuto debuggare
		case DEVICE_TO_DEVICE:
			return this.currentDeviceToDeviceWifiLink;
		default:
			throw new IllegalArgumentException("Invalid link type: " + linkType);
		}
	}

	/**
	 * Sets the network link that is used currently to send data to the cloud or
	 * edge data centers.Used only when the type of this node is
	 * {@link SimulationParameters.TYPES#EDGE_DEVICE}.
	 * 
	 * @param currentUpLink the network link.
	 * @see #getCurrentLink(LinkOrientation)
	 */
	public void setCurrentLink(NetworkLink link, LinkOrientation linkType) {
		switch (linkType) {
		case UP_LINK:
			this.currentUpLink = link;
			break;
		case DOWN_LINK:
			this.currentDownLink = link;							//MODIFICA MIA, prima era uplink....ho dovuto debuggare
			break;
		case DEVICE_TO_DEVICE:
			this.currentDeviceToDeviceWifiLink = link;
			break;
		default:
			throw new IllegalArgumentException("Invalid link type: " + linkType);
		}
	}

}
