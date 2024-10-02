package examples.ExampleMio;

import com.mechalikh.pureedgesim.datacentersmanager.DefaultTopologyCreator;
import com.mechalikh.pureedgesim.network.DefaultNetworkModel;
import com.mechalikh.pureedgesim.simulationmanager.DefaultSimulationManager;
import com.mechalikh.pureedgesim.simulationmanager.Simulation;
import com.mechalikh.pureedgesim.taskgenerator.DefaultTaskGenerator;

public class ExampleMio {

	// Below is the path for the settings folder of this example
	private static String settingsPath = "PureEdgeSim/examples/ExampleMio/ExampleMio_settings/";
	//private static String settingsPath = "PureEdgeSim/examples/Example_1/Example1_settings/";

	// The custom output folder is
	private static String outputPath = "PureEdgeSim/examples/ExampleMio/ExampleMio_output/";

	public ExampleMio() {
		// Create a PureEdgeSim simulation
		Simulation sim = new Simulation();

		// changing the default output folder
		sim.setCustomOutputFolder(outputPath);

		// changing the simulation settings folder
		sim.setCustomSettingsFolder(settingsPath);

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
	    //sim.setCustomEdgeOrchestrator(DQNOrchestrator.class);
		
		/* to use the default one you can simply delete or comment those lines */

		// Finally, you can launch the simulation
		sim.launchSimulation();
		
	}

	public static void main(String[] args) {
		new ExampleMio();
	}

}


