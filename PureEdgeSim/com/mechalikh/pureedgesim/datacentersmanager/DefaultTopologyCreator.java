/**
 * 
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

import com.mechalikh.pureedgesim.network.NetworkLink.NetworkLinkTypes;

import org.jgrapht.graph.DirectedWeightedMultigraph;
import org.jgrapht.GraphPath;
import org.jgrapht.graph.DefaultWeightedEdge;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream; 

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode.LinkOrientation;
import com.mechalikh.pureedgesim.datacentersmanager.NuovaCartellaVM.*;
import com.mechalikh.pureedgesim.network.InfrastructureGraph;
import com.mechalikh.pureedgesim.network.NetworkLink;
import com.mechalikh.pureedgesim.network.NetworkLink.NetworkLinkTypes;
import com.mechalikh.pureedgesim.network.NetworkLinkCellularDown;
import com.mechalikh.pureedgesim.network.NetworkLinkCellularUp;
import com.mechalikh.pureedgesim.network.NetworkLinkEthernet;
import com.mechalikh.pureedgesim.network.NetworkLinkMan;
import com.mechalikh.pureedgesim.network.NetworkLinkFiber;
import com.mechalikh.pureedgesim.network.NetworkLinkHyperDown;
import com.mechalikh.pureedgesim.network.NetworkLinkHyperUp;
import com.mechalikh.pureedgesim.network.NetworkLinkWanDown;
import com.mechalikh.pureedgesim.network.NetworkLinkWanUp;
import com.mechalikh.pureedgesim.network.NetworkLinkWifiDeviceToDevice;
import com.mechalikh.pureedgesim.network.NetworkLinkWifiDown;
import com.mechalikh.pureedgesim.network.NetworkLinkWifiUp;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters.TYPES;
import com.mechalikh.pureedgesim.simulationmanager.SimulationManager;


/**
 * A class that generates a default topology graph for the simulation. It reads
 * the topology information from an XML file and creates the corresponding
 * infrastructure graph.
 * 
 * @author Charafeddine Mechalikh
 * @since PureEdgeSim 5.0
 */
public class DefaultTopologyCreator extends TopologyCreator {

	/**
	 * Creates a new DefaultTopologyCreator object with the specified simulation
	 * manager and computing nodes generator.
	 * 
	 * @param simulationManager       the simulation manager
	 * @param computingNodesGenerator the computing nodes generator
	 */
	public DefaultTopologyCreator(SimulationManager simulationManager,
			ComputingNodesGenerator computingNodesGenerator) {
		super(simulationManager, computingNodesGenerator);
	}

	/**
	 * Generates the topology graph by reading the topology information from an XML
	 * file and creating the corresponding infrastructure graph.
	 */
	@Override
	public void generateTopologyGraph() {
		// Create a WAN link to connect all the edge devices to the cloud data center
		ComputingNode wanNode = createWanLink();
		
		
		//Connect each edge device to the closest ONT using their selected Connectivity method (LAN)
		for (ComputingNode edgeDevice : computingNodesGenerator.getMistOnlyList()) {
			ComputingNode closestONT = ComputingNode.NULL;
			double shortestDistanceToONT = Double.MAX_VALUE;
			for (ComputingNode ONTDevice : computingNodesGenerator.getONT_List()) {
				//Check if the ONT Device is already connect to an edge device
				if (!ONTDevice.isConnect()) {
					double DistanceToONT = edgeDevice.getMobilityModel().distanceTo(ONTDevice);
					if (DistanceToONT < shortestDistanceToONT) {  
						shortestDistanceToONT = DistanceToONT;
						closestONT = ONTDevice;
					}
				}
			}
			connect(edgeDevice, closestONT, NetworkLinkTypes.LAN);
			edgeDevice.getCurrentLink(LinkOrientation.UP_LINK).setDst(closestONT);
			edgeDevice.getCurrentLink(LinkOrientation.DOWN_LINK).setSrc(closestONT);
			closestONT.setAsConnect(true);
		}

		//Connect each ONT to the cloud data center using WAN
		for(ComputingNode ONTDevice : computingNodesGenerator.getONT_List()) {
			NetworkLink ONTup;
			NetworkLink ONTdown;
			ONTup = new NetworkLinkWanUp(ONTDevice, wanNode, simulationManager, NetworkLinkTypes.WAN);
			ONTdown = new NetworkLinkWanDown(wanNode, ONTDevice, simulationManager, NetworkLinkTypes.WAN);
			infrastructureTopology.addLink(ONTup);
			infrastructureTopology.addLink(ONTdown);
		}
		
		// Generate the topology of edge data centers from an XML file
		generateTopologyFromXmlFile();

		//Connetto ciascun SDN al cloud mediante WAN link
		for (ComputingNode node : simulationManager.getDataCentersManager().getComputingNodesGenerator().getOrchestratorsList()) {
			if(node.getType().equals( SimulationParameters.TYPES.SDN)){
				infrastructureTopology.addLink(new NetworkLinkWanUp(node, wanNode, simulationManager, NetworkLinkTypes.WAN));
				infrastructureTopology.addLink(new NetworkLinkWanDown(wanNode, node, simulationManager, NetworkLinkTypes.WAN));
			}
		}
		// infrastructureTopology.addLink(new NetworkLinkWanUp(wanNode.getEdgeOrchestrator(), wanNode, simulationManager, NetworkLinkTypes.WAN));
		// infrastructureTopology.addLink(new NetworkLinkWanDown(wanNode, wanNode.getEdgeOrchestrator(), simulationManager, NetworkLinkTypes.WAN));

		//Connetto l'SDN più vicino a ciascun EdgeDC mediante Fiber
		for(ComputingNode edgeDC : computingNodesGenerator.getEdgeOnlyList()){
			infrastructureTopology.addLink(new NetworkLinkFiber(edgeDC, edgeDC.getEdgeOrchestrator(), simulationManager, NetworkLinkTypes.FIBER));
			infrastructureTopology.addLink(new NetworkLinkFiber(edgeDC.getEdgeOrchestrator(), edgeDC, simulationManager, NetworkLinkTypes.FIBER));
		}

		//Connetto ogni EdgeDC con il Cloud
		for(ComputingNode edgeDC : computingNodesGenerator.getEdgeOnlyList()){
			infrastructureTopology.addLink(new NetworkLinkWanUp(edgeDC, wanNode, simulationManager, NetworkLinkTypes.WAN));
			infrastructureTopology.addLink(new NetworkLinkWanDown(wanNode, edgeDC, simulationManager, NetworkLinkTypes.WAN));
		}

		//Connect each EdgeDC and CloudDC with his Hosts and VMs via Hypervisor.
		for(DataCenter datacenter : computingNodesGenerator.getEdgeAndCloudList()){

			for(Host host : datacenter.getHostList()){
				infrastructureTopology.addLink(new NetworkLinkHyperUp(datacenter, host, simulationManager, NetworkLinkTypes.HYPER));
				infrastructureTopology.addLink(new NetworkLinkHyperDown(host, datacenter, simulationManager, NetworkLinkTypes.HYPER));
				
				datacenter.getCurrentLink(LinkOrientation.UP_LINK).setDst(host);
				datacenter.getCurrentLink(LinkOrientation.DOWN_LINK).setSrc(host);

				for(VM vm : host.getVMList()){
					infrastructureTopology.addLink(new NetworkLinkHyperUp(vm, host, simulationManager, NetworkLinkTypes.HYPER));
					infrastructureTopology.addLink(new NetworkLinkHyperDown(host, vm, simulationManager, NetworkLinkTypes.HYPER));
				
					host.getCurrentLink(LinkOrientation.UP_LINK).setDst(vm);
					host.getCurrentLink(LinkOrientation.DOWN_LINK).setSrc(vm);
				}

			}

		}
		
		//Connect each ONT to the associated SDN
		for (ComputingNode ONTDevice : computingNodesGenerator.getONT_List()) {
			NetworkLink ONTup;
			NetworkLink ONTdown;
			ONTup = new NetworkLinkFiber(ONTDevice, ONTDevice.getEdgeOrchestrator(), simulationManager, NetworkLinkTypes.FIBER);
			ONTdown = new NetworkLinkFiber(ONTDevice.getEdgeOrchestrator(), ONTDevice, simulationManager, NetworkLinkTypes.FIBER);
			infrastructureTopology.addLink(ONTup);
			infrastructureTopology.addLink(ONTdown);
			simulationManager.getNetworkModel().getFiberUp().add(ONTup);
			simulationManager.getNetworkModel().getFiberDown().add(ONTdown);
		}
		
		
		//QUESTI DUE SOTTO SERVONO PER STAMPARE LA TOPOLOGIA
		
		// DirectedWeightedMultigraph<ComputingNode, NetworkLink> graph = infrastructureTopology.getGraph();
		// System.out.println("Nodi:");
        // for (ComputingNode node : graph.vertexSet()) {
        //     System.out.println(" - " + node.getName() + ", id " + node.getId());
        // }

        // // Stampa tutti gli archi
        // System.out.println("\nCollegamenti:");
        // for (NetworkLink edge : graph.edgeSet()) {
        //     ComputingNode sourceNode = graph.getEdgeSource(edge);
        //     ComputingNode targetNode = graph.getEdgeTarget(edge);
        //     NetworkLinkTypes tipo = edge.getType();

        //     System.out.println(" - " + sourceNode.getName() + " -> " + targetNode.getName() + ", Rete: " + tipo);
        // }
		
		
		// Save the shortest paths between all computing nodes
		//infrastructureTopology.savePathsToMap(computingNodesGenerator.getEdgeAndCloudList());
		//infrastructureTopology.savePathsToMap(computingNodesGenerator.getONTandServer_List());
		//infrastructureTopology.savePathsToMap(computingNodesGenerator.getAllNodesList());							//se tolgo questo lo creo nel defaultnetworkmodel ad ogni richiesta
		//infrastructureTopology.savePathsToMap(computingNodesGenerator.getONTandVM_List());
	}

	/**
	 * This function creates a WAN link between the cloud data center and the
	 * infrastructure node.
	 * 
	 * @return the WAN node used for linking edge devices to the cloud.
	 */
	protected ComputingNode createWanLink() {

		// To do so, first let's get the cloud data center.
		// If you have more than one data center, you will need to link them all
		ComputingNode cloud = computingNodesGenerator.getCloudOnlyList().get(0);

		// If we want all data to be sent over the same wan network.
		if (SimulationParameters.useOneSharedWanLink) {
			// We need to create another node to link with the cloud.
			ComputingNode metroRouter = new Router(simulationManager);
			metroRouter.setName("metroRouter");
			metroRouter.setType(TYPES.ROUTER);

			// After that, we can link it with the cloud. We select type IGNORE to avoid
			// measuring energy consumption twice.
			NetworkLinkWanUp wanUp = new NetworkLinkWanUp(metroRouter, cloud, simulationManager,
					NetworkLinkTypes.IGNORE);
			NetworkLinkWanDown wanDown = new NetworkLinkWanDown(cloud, metroRouter, simulationManager,
					NetworkLinkTypes.IGNORE);
			infrastructureTopology.addLink(wanUp);
			infrastructureTopology.addLink(wanDown);

			metroRouter.setMobilityModel(cloud.getMobilityModel());

			// To enable the real time WAN chart, and use the WAN bandwidth in orchestration
			// algorithms like in Example 8:
			simulationManager.getNetworkModel().setWanLinks(wanUp, wanDown);
			return metroRouter;
		} else
			return cloud;

	}

	/**
	 * Generates the network topology from the edge data centers file.
	 */
	protected void generateTopologyFromXmlFile() {
		try (InputStream serversFile = new FileInputStream(SimulationParameters.edgeDataCentersFile)) {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();

			// Disable access to external entities in XML parsing, by disallowing DocType
			// declaration
			dbFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(serversFile);

			// Create the network topology
			NodeList networkLinks = doc.getElementsByTagName("link");
			for (int i = 0; i < networkLinks.getLength(); i++) {
				Element networkLink = (Element) networkLinks.item(i);
				createNetworkLink(networkLink);
			}
		} catch (ParserConfigurationException | SAXException | IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Creates a network link between two data centers.
	 *
	 * @param networkLinkElement the XML element containing the network link
	 *                           information. It should have "from" and "to" child
	 *                           elements with the names of the data centers to
	 *                           connect.
	 */
	protected void createNetworkLink(Element networkLinkElement) {
		ComputingNode dcFrom = getDataCenterByName(
				networkLinkElement.getElementsByTagName("from").item(0).getTextContent());
		ComputingNode dcTo = getDataCenterByName(
				networkLinkElement.getElementsByTagName("to").item(0).getTextContent());
		// Connect the data centers with a MAN link in both directions
		infrastructureTopology.addLink(new NetworkLinkMan(dcFrom, dcTo, simulationManager, NetworkLinkTypes.MAN));
		infrastructureTopology.addLink(new NetworkLinkMan(dcTo, dcFrom, simulationManager, NetworkLinkTypes.MAN));
	}

	/**
	 * Finds a data center in the list of edge data centers by name.
	 *
	 * @param name the name of the data center to find.
	 * @return the data center with the given name, or ComputingNode.NULL if it was
	 *         not found. Time complexity: O(n), where n is the number of edge data
	 *         centers.
	 */
	protected ComputingNode getDataCenterByName(String name) {
		for (ComputingNode dc : computingNodesGenerator.getEdgeOnlyList()) {
			if (dc.getName().equals(name)) {
				return dc;
			}
		}
		return ComputingNode.NULL;
	}

	/**
	 * Connects two computing nodes with a network link of the specified type.
	 *
	 * @param computingNode1 the first computing node to connect
	 * @param computingNode2 the second computing node to connect
	 * @param type           the type of network link to create
	 * @throws IllegalArgumentException if the connectivity type of either computing
	 *                                  node is not wifi, ethernet, or cellular
	 *                                  (case-sensitive)
	 */
	protected void connect(ComputingNode computingNode1, ComputingNode computingNode2, NetworkLinkTypes type)
			throws IllegalArgumentException {
		NetworkLink up;
		NetworkLink down;

		// Determine the connectivity type of the first computing node
		String connectivityType = computingNode1.getEnergyModel().getConnectivityType();

		// Create the appropriate network link based on the connectivity type
		switch (connectivityType) {
		case "wifi":
			up = new NetworkLinkWifiUp(computingNode1, computingNode2, simulationManager, type);
			down = new NetworkLinkWifiDown(computingNode2, computingNode1, simulationManager, type);
			break;
		case "cellular":
			up = new NetworkLinkCellularUp(computingNode1, computingNode2, simulationManager, type);
			down = new NetworkLinkCellularDown(computingNode2, computingNode1, simulationManager, type);
			break;
		case "ethernet":
			up = new NetworkLinkEthernet(computingNode1, computingNode2, simulationManager, type);
			down = new NetworkLinkEthernet(computingNode2, computingNode1, simulationManager, type);
			break;
		default:
			throw new IllegalArgumentException(
					getClass().getSimpleName() + " - Unknown connectivity type: " + connectivityType
							+ ". Available types for edge devices are: wifi, ethernet, and cellular (case sensitive)");
		}

		// Add the links to the topology
		infrastructureTopology.addLink(up);
		infrastructureTopology.addLink(down);

		// If this link is used to connect with the closest edge server, set the current
		// links of computingNode1
		if (type == NetworkLinkTypes.LAN) {
			computingNode1.setCurrentLink(up, LinkOrientation.UP_LINK);
			computingNode1.setCurrentLink(down, LinkOrientation.DOWN_LINK);
		}
	}

	/**
	 * Returns the simulation manager responsible for running the simulation.
	 *
	 * @return the simulation manager
	 */
	public SimulationManager getSimulationManager() {
		return simulationManager;
	}

	/**
	 * Returns the topology of the infrastructure as an {@link InfrastructureGraph}
	 * object.
	 *
	 * @return the infrastructure topology
	 */
	public InfrastructureGraph getInfrastructureTopology() {
		return infrastructureTopology;
	}

}