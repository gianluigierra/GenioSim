package examples.ProgettoGenio;

import java.util.ArrayList;
import java.util.List;

import com.mechalikh.pureedgesim.simulationmanager.*;
import com.mechalikh.pureedgesim.simulationmanager.Simulation;
import com.mechalikh.pureedgesim.taskorchestrator.*;
import com.mechalikh.pureedgesim.taskorchestrator.DQN.CustomOrchestrator;

public class ProgettoGenio {

	// Below is the path for the settings folder of this example
	private static String settingsPath = "PureEdgeSim/examples/ProgettoGenio/ProgettoGenio_settings/";

	// The custom output folder is
	private static String outputPath = "PureEdgeSim/examples/ProgettoGenio/ProgettoGenio_output/";

	public ProgettoGenio(){

		// Create a PureEdgeSim simulation
		Simulation sim = new Simulation();

		// changing the default output folder
		sim.setCustomOutputFolder(outputPath);

		// changing the simulation settings folder
		sim.setCustomSettingsFolder(settingsPath);

		// cambio la modalità di esecuzione
		String exampleMode = "Smart_Lights";

		switch (exampleMode) {
			case "Smart_Lights":
				sim.setSimulationParameterProperties(settingsPath, "Smart_Lights/simulation_parameters_Smart_Lights.properties");
				sim.setApplicationsXML(settingsPath, "Smart_Lights/applications_Smart_Lights.xml");
				sim.setEdgeDatacentersXML(settingsPath, "Smart_Lights/edge_datacenters_Smart_Lights.xml");
				sim.setCloudDatacentersXML(settingsPath, "Smart_Lights/cloud_Smart_Lights.xml");
				sim.setSimulationName(exampleMode);
				break;
		
			case "E_Health":
				sim.setSimulationParameterProperties(settingsPath, "E_Health/simulation_parameters_E_Health.properties");
				sim.setApplicationsXML(settingsPath, "E_Health/applications_E_Health.xml");
				sim.setEdgeDatacentersXML(settingsPath, "E_Health/edge_datacenters_E_Health.xml");
				sim.setCloudDatacentersXML(settingsPath, "E_Health/cloud_E_Health.xml");
				sim.setSimulationName(exampleMode);
				break;
		
			case "Video_Surveillance":
				sim.setSimulationParameterProperties(settingsPath, "Video_Surveillance/simulation_parameters_Video_Surveillance.properties");
				sim.setApplicationsXML(settingsPath, "Video_Surveillance/applications_Video_Surveillance.xml");
				sim.setEdgeDatacentersXML(settingsPath, "Video_Surveillance/edge_datacenters_Video_Surveillance.xml");
				sim.setCloudDatacentersXML(settingsPath, "Video_Surveillance/cloud_Video_Surveillance.xml");
				sim.setSimulationName(exampleMode);
				break;
		}

        //cambio l'orchestratore con quello creatto da me
	    sim.setCustomEdgeOrchestrator(CustomOrchestrator.class);

		//cambio il simulationManager con quello modificato da me
		//sim.setCustomSimulationManager(MySimulationManager.class);

		//inizia la simulazione
		sim.launchSimulation();
		
	}

	public static void main(String[] args) {

        List<Integer> rewardsList = new ArrayList<>();
        List<Double> failureRateList = new ArrayList<>();
        double avgFailureRate = 0;
		int numberOfIterations = 100;
		String algName = "";

		//10 iterazioni dell'algoritmo
		for(int i = 0; i < numberOfIterations; i++){
			System.out.println("\n\nIterazione numero: " + (i+1));
			new ProgettoGenio();
			rewardsList.add(CustomOrchestrator.totalreward);
			failureRateList.add(CustomOrchestrator.failureRate);
			avgFailureRate += CustomOrchestrator.failureRate;
			algName = CustomOrchestrator.algName;
		}
		if(algName.contains("DQN")){
			System.out.println("Rewards list: ");
			for(int i = 0; i < rewardsList.size(); i++){
				System.out.println("    -Iterazione " + (i+1) +", reward: " + rewardsList.get(i) + ", success rate: " + failureRateList.get(i));
			}
		}
		System.out.println("Avg success rate: " + avgFailureRate/numberOfIterations);

		
	}

}


