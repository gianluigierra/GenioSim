package examples.ProgettoGenio;

import java.io.*;
import java.nio.file.*;
import java.util.*;

import java.awt.Robot;
import java.awt.event.KeyEvent;

import com.mechalikh.pureedgesim.simulationmanager.*;

public class ProgettoGenio {

	// Below is the path for the settings folder of this example
	private static String settingsPath = "PureEdgeSim/examples/ProgettoGenio/ProgettoGenio_settings/";

	// The custom output folder is
	private static String outputPath = "PureEdgeSim/examples/ProgettoGenio/ProgettoGenio_output/";

	// cambio la modalità di esecuzione
	private static String exampleMode = "SmartCity";

	/*
	Algoritmi container:
		ROUND_ROBIN,TRADE_OFF,GREEDY,MULTI_OBIETTIVO,LATENCY_ROUND_ROBIN,LATENCY_TRADE_OFF,LATENCY_MULTI_OBIETTIVO,RATE_ROUND_ROBIN,RATE_TRADE_OFF,RATE_GREEDY,RATE_MULTI_OBIETTIVO
	Algoritmi tasks:
		ROUND_ROBIN,BEST_LATENCY,BEST_DELAY
	Modalità:
		static,dinamic
	*/
	public static void main(String[] args) {
		new ProgettoGenio("ROUND_ROBIN", "ROUND_ROBIN");
		//new ProgettoGenio();
	}

	public ProgettoGenio(){

		// Create a PureEdgeSim simulation
		Simulation sim = new Simulation();

		startSim(sim);
	}

	public ProgettoGenio(String containerOrg, String taskOrg){

		// Create a PureEdgeSim simulation
		Simulation sim = new Simulation();

		// setto gli algoritmi
		String filePath = "PureEdgeSim/examples/ProgettoGenio/ProgettoGenio_settings/"; // Percorso al file da modificare
		filePath += exampleMode + "/simulation_parameters_" + exampleMode + ".properties";

		try{
			List<String> lines = Files.readAllLines(Paths.get(filePath));

			// Modifica le righe che corrispondono alle proprietà specificate
			List<String> updatedLines = new ArrayList<>();
			for (String line : lines) {
				if (line.contains("task_orchestration_algorithm = ")) {
					line = "task_orchestration_algorithm = " + taskOrg; // Sostituisci il valore
				}
				if (line.contains("container_orchestration_algorithms = ")) {
					line = "container_orchestration_algorithms = " + containerOrg; // Sostituisci il valore
				}
				updatedLines.add(line);
			}

			// Riscrivi il file con le righe aggiornate
			Files.write(Paths.get(filePath), updatedLines);
        } catch (IOException ex) {
            ex.printStackTrace();
        }

		startSim(sim);
	}

	public void startSim(Simulation sim){

		// changing the default output folder
		sim.setCustomOutputFolder(outputPath);

		// changing the simulation settings folder
		sim.setCustomSettingsFolder(settingsPath);

		switch (exampleMode) {
			case "SmartCity":
				sim.setSimulationParameterProperties(settingsPath, "SmartCity/simulation_parameters_SmartCity.properties");
				sim.setApplicationsXML(settingsPath, "SmartCity/applications_SmartCity.xml");
				sim.setUsersXML(settingsPath, "SmartCity/users.xml");
				sim.setEdgeDatacentersXML(settingsPath, "SmartCity/edge_datacenters_SmartCity.xml");
				sim.setCloudDatacentersXML(settingsPath, "SmartCity/cloud_SmartCity.xml");
				sim.setSimulationName(exampleMode);
				break;
		
			case "E_Health":
				sim.setSimulationParameterProperties(settingsPath, "E_Health/simulation_parameters_E_Health.properties");
				sim.setApplicationsXML(settingsPath, "E_Health/applications_E_Health.xml");
				sim.setUsersXML(settingsPath, "E_Health/users.xml");
				sim.setEdgeDatacentersXML(settingsPath, "E_Health/edge_datacenters_E_Health.xml");
				sim.setCloudDatacentersXML(settingsPath, "E_Health/cloud_E_Health.xml");
				sim.setSimulationName(exampleMode);
				break;
		
			case "Smart_Building":
				sim.setSimulationParameterProperties(settingsPath, "Smart_Building/simulation_parameters_Smart_Building.properties");
				sim.setApplicationsXML(settingsPath, "Smart_Building/applications_Smart_Building.xml");
				sim.setUsersXML(settingsPath, "Smart_Building/users.xml");
				sim.setEdgeDatacentersXML(settingsPath, "Smart_Building/edge_datacenters_Smart_Building.xml");
				sim.setCloudDatacentersXML(settingsPath, "Smart_Building/cloud_Smart_Building.xml");
				sim.setSimulationName(exampleMode);
				break;

			case "Scenario_Misto":
				sim.setSimulationParameterProperties(settingsPath, "Scenario_Misto/simulation_parameters_Scenario_Misto.properties");
				sim.setApplicationsXML(settingsPath, "Scenario_Misto/applications_Scenario_Misto.xml");
				sim.setUsersXML(settingsPath, "Scenario_Misto/users_Scenario_Misto.xml");
				sim.setEdgeDatacentersXML(settingsPath, "Scenario_Misto/edge_datacenters_Scenario_Misto.xml");
				sim.setCloudDatacentersXML(settingsPath, "Scenario_Misto/cloud_Scenario_Misto.xml");
				sim.setONTsXML(settingsPath, "Scenario_Misto/ONT_Scenario_Misto.xml");
				sim.setSimulationName(exampleMode);
				break;	

			case "Scenario_1":
				sim.setSimulationParameterProperties(settingsPath, "Scenario_1/simulation_parameters_Scenario_1.properties");
				sim.setApplicationsXML(settingsPath, "Scenario_1/applications_Scenario_1.xml");
				sim.setUsersXML(settingsPath, "Scenario_1/users_Scenario_1.xml");
				sim.setEdgeDatacentersXML(settingsPath, "Scenario_1/edge_datacenters_Scenario_1.xml");
				sim.setCloudDatacentersXML(settingsPath, "Scenario_1/cloud_Scenario_1.xml");
				sim.setONTsXML(settingsPath, "Scenario_1/ONT_Scenario_1.xml");
				sim.setSimulationName(exampleMode);
				break;	
		}

        //cambio l'orchestratore con quello creatto da me
	    //sim.setCustomEdgeOrchestrator(CustomOrchestrator.class);

		//inizia la simulazione
		sim.launchSimulation();
	}

}


