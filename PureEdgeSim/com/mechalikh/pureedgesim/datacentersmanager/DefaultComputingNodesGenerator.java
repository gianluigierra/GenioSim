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

import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom; 
import java.util.Random;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.mechalikh.pureedgesim.datacentersmanager.NuovaCartellaVM.*;
import com.mechalikh.pureedgesim.energy.EnergyModelComputingNode;
import com.mechalikh.pureedgesim.locationmanager.Location;
import com.mechalikh.pureedgesim.locationmanager.MobilityModel;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters.TYPES;
import com.mechalikh.pureedgesim.simulationmanager.SimulationManager;

public class DefaultComputingNodesGenerator extends ComputingNodesGenerator {

	public DefaultComputingNodesGenerator(SimulationManager simulationManager,
			Class<? extends MobilityModel> mobilityModelClass, Class<? extends ComputingNode> computingNodeClass) {
		super(simulationManager, mobilityModelClass, computingNodeClass);
	}
	
	@Override
		/**
	 * Generates all computing nodes, including the Cloud data centers, the edge
	 * ones, and the edge devices.
	 */
	public void generateDatacentersAndDevices() {

		// Generate Edge and Cloud data centers.
		generateDataCenters(SimulationParameters.cloudDataCentersFile, SimulationParameters.TYPES.CLOUD); 

		generateDataCenters(SimulationParameters.edgeDataCentersFile, SimulationParameters.TYPES.EDGE_DATACENTER); 

		// Generate edge devices.
		generateEdgeDevices();
		
		//Generate ONT devices
		generateONTDevices(SimulationParameters.OntFile, SimulationParameters.TYPES.ONT);

		//Generate SDN device
		generateSDNDevice(SimulationParameters.SdnFile, SimulationParameters.TYPES.SDN);

		getSimulationManager().getSimulationLogger()
				.print("%s - Datacenters and devices were generated", getClass().getSimpleName());
				
	}

	/**
	 * Generates SDN device
	 */
	public void generateSDNDevice(String file, TYPES type) {
		
		try(InputStream SDN_FILE = new FileInputStream(file)) {
			
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			dbFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(SDN_FILE);
			NodeList SDN_nodeList = doc.getElementsByTagName("SDNDevice");
			Element SDN_Element = (Element) SDN_nodeList.item(0);
			
			//Generates SDN 
												
			ComputingNode computingNode = createSDNNode(SDN_Element, type);
			allNodesList.add(computingNode);
			ONTandServer_List.add(computingNode);
			SDN = computingNode;
			
		} catch (Exception e) {
			e.printStackTrace();
		}	
		
	}

	/**
	 * Generates ONT devices
	 */
	public void generateONTDevices(String file, TYPES type) {
		
		try(InputStream ONT_File = new FileInputStream(file)) {
			
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			dbFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(ONT_File);
			NodeList ONT_nodeList = doc.getElementsByTagName("ONTDevice");
			Element ONT_Element = (Element) ONT_nodeList.item(0);
			
			//Generates ONT in accordance with the number of edge devices (except sensors).
			
			for(int i = 0; i < mistOnlyList.size(); i++) {										//MODIFICA MIA, genero gli ONT anche vicino ai sensori. Prima era: (int i = 0; i < mistOnlyListSensorsExcluded.size(); i++)
				ComputingNode computingNode = createComputingNode(ONT_Element, type);			//appunto: nel defaultopologycreator i sensori si collegano agli ONT mediante wifi. 
				ONT_List.add(computingNode);
				ONTandServer_List.add(computingNode);
				ONTandVM_List.add(computingNode);
				//allNodesList.add(computingNode);
				//allNodesListSensorsExcluded.add(computingNode);
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}	
		
	}

	/**
	 * Generates edge devices
	 */
	public void generateEdgeDevices() {
		// Generate edge devices instances from edge devices types in xml file.
		try (InputStream devicesFile = new FileInputStream(SimulationParameters.edgeDevicesFile)) {

			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();

			// Disable access to external entities in XML parsing, by disallowing DocType
			// declaration
			dbFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(devicesFile);
			NodeList nodeList = doc.getElementsByTagName("device");
			Element edgeElement = null;

			// Load all devices types in edge_devices.xml file.
			for (int i = 0; i < nodeList.getLength(); i++) {
				Node edgeNode = nodeList.item(i);
				edgeElement = (Element) edgeNode;
				generateDevicesInstances(edgeElement);
			}

			// if percentage of generated devices is < 100%.
			if (mistOnlyList.size() < getSimulationManager().getScenario().getDevicesCount())
				getSimulationManager().getSimulationLogger().print("%s - Wrong percentages values (the sum is inferior than 100%), check edge_devices.xml file !", getClass().getSimpleName());
			// Add more devices.
			if (edgeElement != null) {
				int missingInstances = getSimulationManager().getScenario().getDevicesCount() - mistOnlyList.size();
				for (int k = 0; k < missingInstances; k++) {
					ComputingNode newDevice = createComputingNode(edgeElement, SimulationParameters.TYPES.EDGE_DEVICE);
					insertEdgeDevice(newDevice);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	/**
	 * Puts the newly generated edge device in corresponding lists.
	 */
	protected void insertEdgeDevice(ComputingNode newDevice) {
		mistOnlyList.add(newDevice);
		allNodesList.add(newDevice);
		if (!newDevice.isSensor()) {
			mistOnlyListSensorsExcluded.add(newDevice);
			mistAndCloudListSensorsExcluded.add(newDevice);
			mistAndEdgeListSensorsExcluded.add(newDevice);
			allNodesListSensorsExcluded.add(newDevice);
		}
	}
	
	/**
	 * Generates the required number of instances for each type of edge devices.
	 * 
	 * @param type The type of edge devices.
	 */
	protected void generateDevicesInstances(Element type) {

		int instancesPercentage = Integer.parseInt(type.getElementsByTagName("percentage").item(0).getTextContent());

		// Find the number of instances of this type of devices
		int devicesInstances = getSimulationManager().getScenario().getDevicesCount() * instancesPercentage / 100;

		for (int j = 0; j < devicesInstances; j++) {
			if (mistOnlyList.size() > getSimulationManager().getScenario().getDevicesCount()) {
				getSimulationManager().getSimulationLogger().print("%s - Wrong percentages values (the sum is superior than 100%), check edge_devices.xml file !",getClass().getSimpleName());
				break;
			}

			try {
				insertEdgeDevice(createComputingNode(type, SimulationParameters.TYPES.EDGE_DEVICE));
			} catch (NoSuchAlgorithmException | NoSuchMethodException | SecurityException | InstantiationException
					| IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				e.printStackTrace();
			}

		}
	}
	
	/**
	 * Generates the Cloud and Edge data centers.
	 * 
	 * @param file The configuration file.
	 * @param type The type, whether a CLOUD data center or an EDGE one.
	 */
	protected void generateDataCenters(String file, TYPES type) {
		// Fill list with edge data centers
		try (InputStream serversFile = new FileInputStream(file)) {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();

			// Disable access to external entities in XML parsing, by disallowing DocType
			// declaration
			dbFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(serversFile);
			NodeList datacenterList = doc.getElementsByTagName("datacenter");
			for (int i = 0; i < datacenterList.getLength(); i++) {
				Element datacenterElement = (Element) datacenterList.item(i);
				//ComputingNode computingNode = createComputingNode(datacenterElement, type);
				DataCenter DC = createDatacenterNode(datacenterElement, type);
				if (DC.getType() == TYPES.CLOUD) {
					cloudOnlyList.add(DC);
					mistAndCloudListSensorsExcluded.add(DC);
					if (SimulationParameters.enableOrchestrators
							&& SimulationParameters.deployOrchestrators == "CLOUD") {
						orchestratorsList.add(DC);
					}
				} else {
					edgeOnlyList.add(DC);
					mistAndEdgeListSensorsExcluded.add(DC);
					if (SimulationParameters.enableOrchestrators
							&& SimulationParameters.deployOrchestrators == "EDGE") {
						orchestratorsList.add(DC);
					}
				}
				allNodesList.add(DC);
				allNodesListSensorsExcluded.add(DC);
				edgeAndCloudList.add(DC);
				ONTandServer_List.add(DC);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Creates the computing nodes.
	 * 
	 * @see #generateDataCenters(String, TYPES)
	 * @see #generateDevicesInstances(Element)
	 * 
	 * @param datacenterElement The configuration file.
	 * @param type              The type, whether an MIST (edge) device, an EDGE
	 *                          data center, or a CLOUD one.
	 * @throws NoSuchAlgorithmException
	 * @throws SecurityException
	 * @throws NoSuchMethodException
	 * @throws InvocationTargetException
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 */
	protected ComputingNode createComputingNode(Element deviceElement, SimulationParameters.TYPES type)
			throws NoSuchAlgorithmException, NoSuchMethodException, SecurityException, InstantiationException,
			IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		// SecureRandom is preferred to generate random values.
		Random random = SecureRandom.getInstanceStrong();
		Boolean mobile = false;
		double speed = 0;
		double minPauseDuration = 0;
		double maxPauseDuration = 0;
		double minMobilityDuration = 0;
		double maxMobilityDuration = 0;
		int xPosition = -1;
		int yPosition = -1;
		double idleConsumption = Double
				.parseDouble(deviceElement.getElementsByTagName("idleConsumption").item(0).getTextContent());
		double maxConsumption = Double
				.parseDouble(deviceElement.getElementsByTagName("maxConsumption").item(0).getTextContent());
		Location deviceLocation = new Location(xPosition, yPosition);
		int numOfCores = Integer.parseInt(deviceElement.getElementsByTagName("cores").item(0).getTextContent());
		double mips = Double.parseDouble(deviceElement.getElementsByTagName("mips").item(0).getTextContent());
		double storage = Double.parseDouble(deviceElement.getElementsByTagName("storage").item(0).getTextContent());
		double ram = Double.parseDouble(deviceElement.getElementsByTagName("ram").item(0).getTextContent());

		Constructor<?> datacenterConstructor = computingNodeClass.getConstructor(SimulationManager.class, double.class,
				int.class, double.class, double.class);
		ComputingNode computingNode = (ComputingNode) datacenterConstructor.newInstance(getSimulationManager(), mips,
				numOfCores, storage, ram);

		computingNode.setAsOrchestrator(Boolean
				.parseBoolean(deviceElement.getElementsByTagName("isOrchestrator").item(0).getTextContent()));

		if (computingNode.isOrchestrator())
			orchestratorsList.add(computingNode);

		computingNode.setEnergyModel(new EnergyModelComputingNode(maxConsumption, idleConsumption));

	 	if (type == SimulationParameters.TYPES.EDGE_DEVICE) {
			computingNode.setName("Edge Device id " + DeviceCount);
			DeviceCount ++;
			mobile = Boolean.parseBoolean(deviceElement.getElementsByTagName("mobility").item(0).getTextContent());
			speed = Double.parseDouble(deviceElement.getElementsByTagName("speed").item(0).getTextContent());
			minPauseDuration = Double
					.parseDouble(deviceElement.getElementsByTagName("minPauseDuration").item(0).getTextContent());
			maxPauseDuration = Double
					.parseDouble(deviceElement.getElementsByTagName("maxPauseDuration").item(0).getTextContent());
			minMobilityDuration = Double.parseDouble(
				deviceElement.getElementsByTagName("minMobilityDuration").item(0).getTextContent());
			maxMobilityDuration = Double.parseDouble(
				deviceElement.getElementsByTagName("maxMobilityDuration").item(0).getTextContent());
			computingNode.getEnergyModel().setBattery(
					Boolean.parseBoolean(deviceElement.getElementsByTagName("battery").item(0).getTextContent()));
			computingNode.getEnergyModel().setBatteryCapacity(Double
					.parseDouble(deviceElement.getElementsByTagName("batteryCapacity").item(0).getTextContent()));
			computingNode.getEnergyModel().setIntialBatteryPercentage(Double.parseDouble(
				deviceElement.getElementsByTagName("initialBatteryLevel").item(0).getTextContent()));
			computingNode.getEnergyModel().setConnectivityType(
				deviceElement.getElementsByTagName("connectivity").item(0).getTextContent());
			computingNode.enableTaskGeneration(Boolean
					.parseBoolean(deviceElement.getElementsByTagName("generateTasks").item(0).getTextContent()));
			// Generate random location for edge devices
			deviceLocation = new Location(random.nextInt(SimulationParameters.simulationMapWidth),
					random.nextInt(SimulationParameters.simulationMapLength));
			getSimulationManager().getSimulationLogger()
					.deepLog("DefaultComputingNodesGenerator- Edge device:" + mistOnlyList.size() + "    location: ( "
							+ deviceLocation.getXPos() + "," + deviceLocation.getYPos() + " )");
			//SimLog.println(computingNode.getName() + " Location: (" + datacenterLocation.getXPos() + "," + datacenterLocation.getYPos() + " )");
			
		} else if (type == SimulationParameters.TYPES.ONT){
			String name = deviceElement.getAttribute("name");
			computingNode.setName(name + "id " + ONTcount);
			
			//set ONT location near the edge device 
			double xPos = mistOnlyList.get(ONTcount).getMobilityModel().getCurrentLocation().getXPos() + random.nextInt(2)+1;
			double yPos = mistOnlyList.get(ONTcount).getMobilityModel().getCurrentLocation().getYPos() + random.nextInt(2)+1;
			deviceLocation = new Location(xPos, yPos); 
			
			//SimLog.println(computingNode.getName() + " Location: (" + xPos + "," + yPos + ")");
			ONTcount++;
			
		}

		/**
		//Generates the ONT like a data center
		if (type == SimulationParameters.TYPES.ONT) {
			type = SimulationParameters.TYPES.EDGE_DATACENTER;
		}
		*/

		computingNode.setType(type);
		Constructor<?> mobilityConstructor = mobilityModelClass.getConstructor(SimulationManager.class, Location.class);
		MobilityModel mobilityModel = ((MobilityModel) mobilityConstructor.newInstance(simulationManager,
		deviceLocation)).setMobile(mobile).setSpeed(speed).setMinPauseDuration(minPauseDuration)
				.setMaxPauseDuration(maxPauseDuration).setMinMobilityDuration(minMobilityDuration)
				.setMaxMobilityDuration(maxMobilityDuration);

		computingNode.setMobilityModel(mobilityModel);

		return computingNode;
	}
	

	/**
	 * Creates the Datacenter nodes.
	 * 
	 * @see #generateDataCenters(String, TYPES)
	 * @see #generateDevicesInstances(Element)
	 * 
	 * @param datacenterElement The configuration file.
	 * @param type              The type, whether an MIST (edge) device, an EDGE
	 *                          data center, or a CLOUD one.
	 * @throws NoSuchAlgorithmException
	 * @throws SecurityException
	 * @throws NoSuchMethodException
	 * @throws InvocationTargetException
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 */
	protected DataCenter createDatacenterNode(Element datacenterElement, SimulationParameters.TYPES type)
			throws NoSuchAlgorithmException, NoSuchMethodException, SecurityException, InstantiationException,
			IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		// SecureRandom is preferred to generate random values.
		Boolean mobile = false;
		double speed = 0;
		double minPauseDuration = 0;
		double maxPauseDuration = 0;
		double minMobilityDuration = 0;
		double maxMobilityDuration = 0;
		int xPosition = -1;
		int yPosition = -1;
		double idleConsumption = Double
				.parseDouble(datacenterElement.getElementsByTagName("idleConsumption").item(0).getTextContent());
		double maxConsumption = Double
				.parseDouble(datacenterElement.getElementsByTagName("maxConsumption").item(0).getTextContent());

		Element location = (Element) datacenterElement.getElementsByTagName("location").item(0);
		xPosition = Integer.parseInt(location.getElementsByTagName("x_pos").item(0).getTextContent());
		yPosition = Integer.parseInt(location.getElementsByTagName("y_pos").item(0).getTextContent());
		Location datacenterLocation = new Location(xPosition, yPosition);

		Constructor<?> mobilityConstructor = mobilityModelClass.getConstructor(SimulationManager.class, Location.class);
		MobilityModel mobilityModel = ((MobilityModel) mobilityConstructor.newInstance(simulationManager,
				datacenterLocation)).setMobile(mobile).setSpeed(speed).setMinPauseDuration(minPauseDuration)
				.setMaxPauseDuration(maxPauseDuration).setMinMobilityDuration(minMobilityDuration)
				.setMaxMobilityDuration(maxMobilityDuration);

		DataCenter dataCenter = new DataCenter(simulationManager, datacenterElement);

		dataCenter.setMobilityModel(mobilityModel);
		
		dataCenter.setType(type);

		dataCenter.setAsOrchestrator(Boolean
				.parseBoolean(datacenterElement.getElementsByTagName("isOrchestrator").item(0).getTextContent()));

		if (dataCenter.isOrchestrator())
			orchestratorsList.add(dataCenter);

		dataCenter.setEnergyModel(new EnergyModelComputingNode(maxConsumption, idleConsumption));

		if (type == SimulationParameters.TYPES.EDGE_DATACENTER) {
			String name = datacenterElement.getAttribute("name");
			dataCenter.setName(name);

			for (int i = 0; i < edgeOnlyList.size(); i++)
				if (datacenterLocation.equals(edgeOnlyList.get(i).getMobilityModel().getCurrentLocation()))
					throw new IllegalArgumentException(
							" Each Edge Data Center must have a different location, check the \"edge_datacenters.xml\" file!");

			dataCenter.setPeriphery(
					Boolean.parseBoolean(datacenterElement.getElementsByTagName("periphery").item(0).getTextContent()));

		}
		else {
			//Imposto il nome del Cloud
			dataCenter.setName("CloudDC");
		}
		
		for(Host host : dataCenter.getHostList()){
			ONTandVM_List.addAll(host.getVMList());
		}

		return dataCenter;
	}

		/**
	 * Creates the SDN node.
	 * 
	 * @see #generateSDN(String, TYPES)
	 * @see #generateDevicesInstances(Element)
	 * 
	 * @param SDNElement The configuration file.
	 * @param type              The type, whether an MIST (edge) device, an EDGE
	 *                          data center, or a CLOUD one.
	 * @throws NoSuchAlgorithmException
	 * @throws SecurityException
	 * @throws NoSuchMethodException
	 * @throws InvocationTargetException
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 */
	protected SDN createSDNNode(Element SDNElement, SimulationParameters.TYPES type)
			throws NoSuchAlgorithmException, NoSuchMethodException, SecurityException, InstantiationException,
			IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		// SecureRandom is preferred to generate random values.
		Boolean mobile = false;
		double speed = 0;
		double minPauseDuration = 0;
		double maxPauseDuration = 0;
		double minMobilityDuration = 0;
		double maxMobilityDuration = 0;
		int xPosition = -1;
		int yPosition = -1;
		double idleConsumption = Double
				.parseDouble(SDNElement.getElementsByTagName("idleConsumption").item(0).getTextContent());
		double maxConsumption = Double
				.parseDouble(SDNElement.getElementsByTagName("maxConsumption").item(0).getTextContent());

		Element location = (Element) SDNElement.getElementsByTagName("location").item(0);
		xPosition = Integer.parseInt(location.getElementsByTagName("x_pos").item(0).getTextContent());
		yPosition = Integer.parseInt(location.getElementsByTagName("y_pos").item(0).getTextContent());
		Location SDNLocation = new Location(xPosition, yPosition);

		Constructor<?> mobilityConstructor = mobilityModelClass.getConstructor(SimulationManager.class, Location.class);
		MobilityModel mobilityModel = ((MobilityModel) mobilityConstructor.newInstance(simulationManager,
		SDNLocation)).setMobile(mobile).setSpeed(speed).setMinPauseDuration(minPauseDuration)
				.setMaxPauseDuration(maxPauseDuration).setMinMobilityDuration(minMobilityDuration)
				.setMaxMobilityDuration(maxMobilityDuration);

		SDN sdn = new SDN(simulationManager, 0.0, 0, 0.0, 0.0);

		sdn.setMobilityModel(mobilityModel);
		
		sdn.setType(type);

		sdn.setAsOrchestrator(true);

		if (sdn.isOrchestrator())
			orchestratorsList.add(sdn);

		sdn.setEnergyModel(new EnergyModelComputingNode(maxConsumption, idleConsumption));

		//Imposto il nome dell'SDN	
		String name = SDNElement.getAttribute("name");
		sdn.setName(name);

		return sdn;
	}

}
