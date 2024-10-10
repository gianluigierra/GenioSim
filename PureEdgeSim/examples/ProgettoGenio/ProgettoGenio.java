package examples.ProgettoGenio;

import com.mechalikh.pureedgesim.datacentersmanager.DefaultTopologyCreator;
import com.mechalikh.pureedgesim.network.DefaultNetworkModel;
import com.mechalikh.pureedgesim.simulationmanager.DefaultSimulationManager;
import com.mechalikh.pureedgesim.simulationmanager.Simulation;
import com.mechalikh.pureedgesim.taskgenerator.DefaultTaskGenerator;
import com.mechalikh.pureedgesim.taskorchestrator.*;

public class ProgettoGenio {

	// Below is the path for the settings folder of this example
	private static String settingsPath = "PureEdgeSim/examples/ProgettoGenio/ProgettoGenio_settings/";

	// The custom output folder is
	private static String outputPath = "PureEdgeSim/examples/ProgettoGenio/ProgettoGenio_output/";

	public ProgettoGenio() {
		// Create a PureEdgeSim simulation
		Simulation sim = new Simulation();

		// changing the default output folder
		sim.setCustomOutputFolder(outputPath);

		// changing the simulation settings folder
		sim.setCustomSettingsFolder(settingsPath);

		// cambio la modalità di esecuzione
		String exampleMode = "E_Health";

		switch (exampleMode) {
			case "Smart_Lights":
				sim.setSimulationParameterProperties(settingsPath, "simulation_parameters_Smart_Lights.properties");
				sim.setApplicationsXML(settingsPath, "applications_Smart_Lights.xml");
				sim.setEdgeDatacentersXML(settingsPath, "edge_datacenters_Smart_Lights.xml");
				sim.setSimulationName(exampleMode);
				break;
		
			case "E_Health":
				sim.setSimulationParameterProperties(settingsPath, "simulation_parameters_E_Health.properties");
				sim.setApplicationsXML(settingsPath, "applications_E_Health.xml");
				sim.setEdgeDatacentersXML(settingsPath, "edge_datacenters_E_Health.xml");
				sim.setSimulationName(exampleMode);
				break;
		
			case "Video_Surveillance":
				sim.setSimulationParameterProperties(settingsPath, "simulation_parameters_Video_Surveillance.properties");
				sim.setApplicationsXML(settingsPath, "applications_Video_Surveillance.xml");
				sim.setEdgeDatacentersXML(settingsPath, "edge_datacenters_Video_Surveillance.xml");
				sim.setSimulationName(exampleMode);
				break;
		}

		// To change the mobility model
		//sim.setCustomMobilityModel(Example2CustomMobilityModel.class);
		
		// To change the computing node class
		//sim.setCustomComputingNode(Example4CustomComputingNode.class);
		
		// To change the tasks generator
		sim.setCustomTaskGenerator(DefaultTaskGenerator.class); 
		
		// To change the network model
		sim.setCustomNetworkModel(DefaultNetworkModel.class); 
		
		// To change the simulation manager
		sim.setCustomSimulationManager(DefaultSimulationManager.class); 
		
		// To change the topology
		sim.setCustomTopologyCreator(DefaultTopologyCreator.class); 
		
        //cambio l'orchestratore con quello creatto da me
	    sim.setCustomEdgeOrchestrator(DQNOrchestrator.class);
		//sim.setCustomEdgeOrchestrator(examples.ExampleMio.DQNOrchestrator.class);
		
		/* to use the default one you can simply delete or comment those lines */

		// Finally, you can launch the simulation
		sim.launchSimulation();
		
	}

	public static void main(String[] args) {
		new ProgettoGenio();
	}

}


