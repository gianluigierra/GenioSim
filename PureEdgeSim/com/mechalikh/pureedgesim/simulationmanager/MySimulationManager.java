package com.mechalikh.pureedgesim.simulationmanager;

import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode;
import com.mechalikh.pureedgesim.network.NetworkModel;
import com.mechalikh.pureedgesim.scenariomanager.Scenario;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.simulationengine.PureEdgeSim;
import com.mechalikh.pureedgesim.taskgenerator.Task;


public class MySimulationManager extends DefaultSimulationManager {

	/**
	 * Simulation progress parameters.
	 **/
	protected int lastWrittenNumber = 0;
	protected int oldProgress = -1;

	/**
	 * The number of failed tasks.
	 **/
	protected int failedTasksCount = 0;

	/**
	 * The total number of executed tasks.
	 **/
	protected int tasksCount = 0;

	/**
	 * Used to show the tasks success rate in the live chart
	 **/
	protected int sentTasks = 0;

	/**
	 * Initializes the simulation manager.
	 * 
	 * @see com.mechalikh.pureedgesim.simulationmanager.SimulationThread#startSimulation()
	 * 
	 * @param simLog       The simulation logger
	 * @param pureEdgeSim  The CloudSim simulation engine.
	 * @param simulationId The simulation ID
	 * @param iteration    Which simulation run
	 * @param scenario     The scenario is composed of the algorithm and
	 *                     architecture that are being used, and the number of edge
	 *                     devices.
	 */
	public MySimulationManager(SimLog simLog, PureEdgeSim pureEdgeSim, int simulationId, int iteration,
			Scenario scenario) {
		super(simLog, pureEdgeSim, simulationId, iteration, scenario);
	}

    /**
	 * Defines the logic to be performed by the simulation manager when the
	 * simulation starts.
	 */
	@Override
	public void onSimulationStart() {
		// Initialize logger variables.
		simLog.setGeneratedTasks(taskList.size());
		simLog.setCurrentOrchPolicy(scenario.getStringOrchArchitecture());

		simLog.print("%s - Simulation: %d  , iteration: %d", getClass().getSimpleName(), getSimulationId(),
				getIteration());

        //schedula tutti i task in una botta
		while(!taskList.isEmpty()){
			schedule(this, taskList.first().getTime() - simulation.clock(), SEND_TO_EDGE_ORCH, taskList.first());
			taskList.remove(taskList.first());
		}

		// Scheduling the end of the simulation.
		schedule(this, SimulationParameters.simulationDuration, PRINT_LOG);

		// Schedule the update of real-time charts.
		if (SimulationParameters.displayRealTimeCharts && !SimulationParameters.parallelism_enabled)
			scheduleNow(this, UPDATE_REAL_TIME_CHARTS);

		// Show simulation progress.
		scheduleNow(this, SHOW_PROGRESS);

		simLog.printSameLine("Simulation progress : [", "red");
	}

    //QUESTA è LA FUNZIONE RESPONSABILE DI NON ORCHESTRARE TUTTI I TASK
	protected void sendFromOrchToDestination(Task task) {
		if (taskFailed(task, 1))
			return;
			
		// Find the best resource node for executing the task.
		edgeOrchestrator.orchestrate(task);

		// Stop if no resource is available for this task, the offloading is failed.
		if (task.getOffloadingDestination() == ComputingNode.NULL) {

			task.setFailureReason(Task.FailureReason.NO_OFFLOADING_DESTINATIONS);
			simLog.incrementTasksFailedLackOfRessources(task);
			tasksCount++;
			return;
		}

		simLog.taskSentFromOrchToDest(task);

		// Send the task from the orchestrator to the destination
		scheduleNow(getNetworkModel(), NetworkModel.SEND_REQUEST_FROM_EDGE_ORCH_TO_DESTINATION, task);

	}

}