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
package examples.Example_1; 
import examples.Examples_PES.*;
import com.mechalikh.pureedgesim.datacentersmanager.DefaultTopologyCreator;
import com.mechalikh.pureedgesim.network.DefaultNetworkModel;
import com.mechalikh.pureedgesim.simulationmanager.DefaultSimulationManager;
import com.mechalikh.pureedgesim.simulationmanager.Simulation;
import com.mechalikh.pureedgesim.taskgenerator.DefaultTaskGenerator;
import com.mechalikh.pureedgesim.taskorchestrator.CustomOrchestrator;
import com.mechalikh.pureedgesim.taskorchestrator.DefaultOrchestrator; 

public class Example1 {
	/**
	 * This is a simple example showing how to launch simulation using custom
	 * mobility model, energy model, custom edge orchestrator, custom tasks
	 * generator, and custom edge devices. By removing them, it will use the
	 * default models provided by PureEdgeSim. 
	 * 
	 * @author Charafeddine Mechalikh
	 * @since  PureEdgeSim 2.2
	 */

	// Below is the path for the settings folder of this example
	private static String settingsPath = "PureEdgeSim/examples/Example_1/Example1_settings/";

	// The custom output folder is
	private static String outputPath = "PureEdgeSim/examples/Example_1/Example1_output/";

	public Example1() {	
		//Create a PureEdgeSim simulation
		Simulation sim = new Simulation();

		// changing the default output folder
		sim.setCustomOutputFolder(outputPath);

		// changing the simulation settings folder
		sim.setCustomSettingsFolder(settingsPath);

		// To change the mobility model
		sim.setCustomMobilityModel(Example2CustomMobilityModel.class);

		// To change the tasks orchestrator
		sim.setCustomEdgeOrchestrator(DefaultOrchestrator.class);
		
		// To change the computing node class
		sim.setCustomComputingNode(Example4CustomComputingNode.class);
		
		// To change the tasks generator
		sim.setCustomTaskGenerator(DefaultTaskGenerator.class); 
		
		// To change the network model
		sim.setCustomNetworkModel(DefaultNetworkModel.class); 
		
		// To change the simulation manager
		sim.setCustomSimulationManager(DefaultSimulationManager.class); 
		
		// To change the topology
		sim.setCustomTopologyCreator(DefaultTopologyCreator.class); 
		
		// To change the taskOrchestrator
	    sim.setCustomEdgeOrchestrator(CustomOrchestrator.class);

		/* to use the default one you can simply delete or comment those lines */

		// Finally, you can launch the simulation
		sim.launchSimulation();
	}

	public static void main(String[] args) {
		new Example1();
	}

}
