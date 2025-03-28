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
package com.mechalikh.pureedgesim.simulationmanager;

import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode;
import com.mechalikh.pureedgesim.datacentersmanager.ComputingNodesGenerator;
import com.mechalikh.pureedgesim.datacentersmanager.DefaultComputingNode;
import com.mechalikh.pureedgesim.datacentersmanager.DefaultComputingNodesGenerator;
import com.mechalikh.pureedgesim.datacentersmanager.DefaultTopologyCreator;
import com.mechalikh.pureedgesim.datacentersmanager.TopologyCreator;
import com.mechalikh.pureedgesim.locationmanager.DefaultMobilityModel;
import com.mechalikh.pureedgesim.locationmanager.MobilityModel;
import com.mechalikh.pureedgesim.network.DefaultNetworkModel;
import com.mechalikh.pureedgesim.network.NetworkModel;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.taskgenerator.DefaultTaskGenerator;
import com.mechalikh.pureedgesim.taskgenerator.DefaultContainerGenerator;
import com.mechalikh.pureedgesim.taskgenerator.TaskGenerator;
import com.mechalikh.pureedgesim.taskgenerator.ContainerGenerator;
import com.mechalikh.pureedgesim.taskorchestrator.DefaultOrchestrator;
import com.mechalikh.pureedgesim.taskorchestrator.DefaultContainerOrchestrator;
import com.mechalikh.pureedgesim.taskorchestrator.Orchestrator;
import com.mechalikh.pureedgesim.taskorchestrator.ContainerOrchestrator;

/**
 * An abstract class that represents the simulation environment
 * 
 * @author Charafeddine Mechalikh
 * @since PureEdgeSim 5.0
 */
public abstract class SimulationAbstract {

	//MODIFICA MIA, prima non era presente ONT_FILE, SDN_FILE
	public enum Files {
		SIMULATION_PARAMETERS, APPLICATIONS_FILE, EDGE_DATACENTERS_FILE, EDGE_DEVICES_FILE, CLOUD_FILE, ONT_FILE, SDN_FILE, USERS_FILE
	}

	/**
	 * The Mobility Model class that is used in the simulation.
	 * 
	 * @see #setCustomMobilityModel(Class)
	 */
	protected Class<? extends MobilityModel> mobilityModel = DefaultMobilityModel.class;

	/**
	 * The Computing Node class that is used in the simulation.
	 * 
	 * @see #setCustomComputingNode(Class)
	 */
	protected Class<? extends ComputingNode> computingNode = DefaultComputingNode.class;

	/**
	 * The Tasks Generator class that is used in the simulation.
	 * 
	 * @see #setCustomTaskGenerator(Class)
	 */
	protected Class<? extends TaskGenerator> tasksGenerator = DefaultTaskGenerator.class;

	/**
	 * The Container Generator class that is used in the simulation.
	 * 
	 * @see #setCustomContainerGenerator(Class)
	 */
	protected Class<? extends ContainerGenerator> containersGenerator = DefaultContainerGenerator.class;

	/**
	 * The Orchestrator class that is used in the simulation.
	 * 
	 * @see #setCustomEdgeOrchestrator(Class)
	 */
	protected Class<? extends Orchestrator> orchestrator = DefaultOrchestrator.class;

	/**
	 * The CloudOrchestrator class that is used in the simulation.
	 * 
	 * @see #setCustomEdgeOrchestrator(Class)
	 */
	protected Class<? extends ContainerOrchestrator> cloudOrchestrator = DefaultContainerOrchestrator.class;
	
	/**
	 * The Network Model class that is used in the simulation.
	 * 
	 * @see #setCustomNetworkModel(Class)
	 */
	protected Class<? extends NetworkModel> networkModel = DefaultNetworkModel.class;

	/**
	 * The Simulation Manager class that is used in the simulation.
	 * 
	 * @see #setCustomSimulationManager(Class)
	 */
	protected Class<? extends SimulationManager> simulationManager = DefaultSimulationManager.class;
	
	/**
	 * The Topology Creator class that is used in the simulation.
	 * 
	 * @see #setCustomTopologyCreator(Class)
	 */
	protected Class<? extends TopologyCreator> topologyCreator = DefaultTopologyCreator.class;
	
	/**
	 * The Computing Nodes Generator class that is used in the simulation.
	 * 
	 * @see #setCustomComputingNodesGenerator(Class)
	 */
	protected Class<? extends ComputingNodesGenerator> computingNodesGenerator = DefaultComputingNodesGenerator.class;
	
	/**
	 * Allows to use a custom computing node class in the simulation. The class must
	 * extend the {@link ComputingNode} provided by PureEdgeSim.
	 * 
	 * @param customComputingNode the custom class to use.
	 */
	public void setCustomComputingNode(Class<? extends ComputingNode> customComputingNode) {
		this.computingNode = customComputingNode;
	}

	/**
	 * Allows to use a custom tasks generator class in the simulation. The class
	 * must extend the {@link TaskGenerator} provided by PureEdgeSim.
	 * 
	 * @param taskGenerator the custom task generator class to use.
	 */
	public void setCustomTaskGenerator(Class<? extends TaskGenerator> taskGenerator) {
		this.tasksGenerator = taskGenerator;
	}

	/**
	 * Allows to use a custom containers generator class in the simulation. The class
	 * must extend the {@link ContainerGenerator} provided by PureEdgeSim.
	 * 
	 * @param taskGenerator the custom task generator class to use.
	 */
	public void setCustomContainerGenerator(Class<? extends ContainerGenerator> containerGenerator) {
		this.containersGenerator = containerGenerator;
	}

	/**
	 * Allows to use a custom orchestrator class in the simulation. The class must
	 * extend the {@link Orchestrator} provided by PureEdgeSim.
	 * 
	 * @param orchestrator the custom orchestrator class to use.
	 */
	public void setCustomEdgeOrchestrator(Class<? extends Orchestrator> orchestrator) {
		this.orchestrator = orchestrator;
	} 
	

	/**
	 * Allows to use a custom mobility model in the simulation. The class must
	 * extend the {@link MobilityModel} provided in PureEdgeSim.
	 * 
	 * @param mobilityModel the custom mobility model class to use.
	 */
	public void setCustomMobilityModel(Class<? extends MobilityModel> mobilityModel) {
		this.mobilityModel = mobilityModel;
	}

	/**
	 * Allows to use a custom network model in the simulation. The class must extend
	 * the {@link NetworkModel} provided by PureEdgeSim.
	 * 
	 * @param networkModel the custom network model class to use.
	 */
	public void setCustomNetworkModel(Class<? extends NetworkModel> networkModel) {
		this.networkModel = networkModel;
	}

	/**
	 * Allows to use a custom topology creator in the simulation. The class must extend
	 * the {@link TopologyCreator} provided by PureEdgeSim.
	 * 
	 * @param topologyCreator the custom topology creator class to use.
	 */
	public void setCustomTopologyCreator(Class<? extends TopologyCreator> topologyCreator) {
		this.topologyCreator = topologyCreator;
	}
	
	/**
	 * Allows to use a custom simulation manager class in the simulation. The class
	 * must extend the {@link DefaultSimulationManager} provided by PureEdgeSim.
	 * 
	 * @param simulationManager the custom simulation manager class to use.
	 */
	public void setCustomSimulationManager(Class<? extends SimulationManager> simulationManager) {
		this.simulationManager = simulationManager;
	}
	
	/**
	 * Allows to use a custom computing nodes generator in the simulation. The class must extend
	 * the {@link ComputingNodesGenerator} provided by PureEdgeSim.
	 * 
	 * @param computingNodesGenerator the custom computing nodes generator class to use.
	 */
	public void setCustomComputingNodesGenerator(Class<? extends ComputingNodesGenerator> computingNodesGenerator) {
		this.computingNodesGenerator = computingNodesGenerator;
	}

	/**
	 * Allows to change the output folder, in which the simulation results will be
	 * saved.
	 * 
	 * @param path the output folder to set.
	 */
	public void setCustomOutputFolder(String path) {
		SimulationParameters.outputFolder = path;
	}

	/**
	 * Allows to change the location from which the configuration files are loaded.
	 * 
	 * @param settingsFolder the new settings folder to use.
	 */
	public void setCustomSettingsFolder(String settingsFolder) {
		setCustomFilePath(settingsFolder + "simulation_parameters.properties", Files.SIMULATION_PARAMETERS);
		setCustomFilePath(settingsFolder + "applications.xml", Files.APPLICATIONS_FILE);
		setCustomFilePath(settingsFolder + "edge_datacenters.xml", Files.EDGE_DATACENTERS_FILE);
		setCustomFilePath(settingsFolder + "edge_devices.xml", Files.EDGE_DEVICES_FILE);
		setCustomFilePath(settingsFolder + "ONT.xml", Files.ONT_FILE);							//MODIFICA MIA, aggiunto 
		setCustomFilePath(settingsFolder + "SDN.xml", Files.SDN_FILE);							//MODIFICA MIA, aggiunto 
		setCustomFilePath(settingsFolder + "cloud.xml", Files.CLOUD_FILE);
		SimulationParameters.settingspath = settingsFolder;
	}
	
	/**
	 * changes the simulation name.
	 * 
	 * @param settingsFolder the new settings folder to use.
	 */
	public void setSimulationName(String simName){
		SimulationParameters.simName = simName;
	}

	/**
	 * Allows to specifically set the simulation_paramters.properties file.
	 * 
	 * @param settingsFolder the new settings folder to use.
	 */
	public void setSimulationParameterProperties(String settingsFolder, String simulationParametersProperties){
		setCustomFilePath(settingsFolder + simulationParametersProperties, Files.SIMULATION_PARAMETERS);
	}

	/**
	 * Allows to specifically set the applications.xml file.
	 * 
	 * @param settingsFolder the new settings folder to use.
	 */
	public void setApplicationsXML(String settingsFolder, String ApplicationsXML){
		setCustomFilePath(settingsFolder + ApplicationsXML, Files.APPLICATIONS_FILE);
	}

	/**
	 * Allows to specifically set the users.xml file.
	 * 
	 * @param settingsFolder the new settings folder to use.
	 */
	public void setUsersXML(String settingsFolder, String UsersXML){
		setCustomFilePath(settingsFolder + UsersXML, Files.USERS_FILE);
	}

	/**
	 * Allows to specifically set the edge_datacenters.xml file.
	 * 
	 * @param settingsFolder the new settings folder to use.
	 */
	public void setEdgeDatacentersXML(String settingsFolder, String EdgeDatacentersXML){
		setCustomFilePath(settingsFolder + EdgeDatacentersXML, Files.EDGE_DATACENTERS_FILE);
	}

	/**
	 * Allows to specifically set the cloud_datacenters.xml file.
	 * 
	 * @param settingsFolder the new settings folder to use.
	 */
	public void setCloudDatacentersXML(String settingsFolder, String CloudDatacentersXML){
		setCustomFilePath(settingsFolder + CloudDatacentersXML, Files.CLOUD_FILE);
	}

	/**
	 * Allows to specifically set the ont.xml file.
	 * 
	 * @param settingsFolder the new settings folder to use.
	 */
	public void setONTsXML(String settingsFolder, String ONTsXML){
		setCustomFilePath(settingsFolder + ONTsXML, Files.ONT_FILE);
	}

	/**
	 * Allows to set a custom path of each configuration file individually.
	 * 
	 * @param path the path from which the file will be loaded
	 * @param file the nature of the file to load
	 */
	public void setCustomFilePath(String path, Files file) {
		switch (file) {
		case SIMULATION_PARAMETERS:
			SimulationParameters.simulationParametersFile = path;
			break;
		case APPLICATIONS_FILE:
			SimulationParameters.applicationFile = path;
			break;
		case EDGE_DATACENTERS_FILE:
			SimulationParameters.edgeDataCentersFile = path;
			break;
		case EDGE_DEVICES_FILE:
			SimulationParameters.edgeDevicesFile = path;
			break;
		case CLOUD_FILE:
			SimulationParameters.cloudDataCentersFile = path;
			break;
		case ONT_FILE:																	//MODIFICA MIA
			SimulationParameters.OntFile = path;										//prima questo case non era presente
			break;																		//
		case SDN_FILE:																	//MODIFICA MIA
			SimulationParameters.SdnFile = path;										//prima questo case non era presente
			break;																		//
		case USERS_FILE:																	//MODIFICA MIA
			SimulationParameters.userFile = path;										//prima questo case non era presente
			break;																		//
		default:
			throw new IllegalArgumentException(getClass().getSimpleName() + " - Unknown file type");
		}
	}

}
