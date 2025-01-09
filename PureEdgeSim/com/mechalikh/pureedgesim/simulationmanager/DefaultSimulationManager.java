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

import java.io.IOException;

import org.jgrapht.GraphPath;

import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode;
import com.mechalikh.pureedgesim.network.NetworkLink;
import com.mechalikh.pureedgesim.network.NetworkModel;
import com.mechalikh.pureedgesim.scenariomanager.Scenario;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters.TYPES;
import com.mechalikh.pureedgesim.simulationengine.Event;
import com.mechalikh.pureedgesim.simulationengine.OnSimulationStartListener;
import com.mechalikh.pureedgesim.simulationengine.PureEdgeSim;
import com.mechalikh.pureedgesim.simulationengine.SimEntity;
import com.mechalikh.pureedgesim.simulationvisualizer.SimulationVisualizer;
import com.mechalikh.pureedgesim.taskgenerator.Task;
import com.mechalikh.pureedgesim.taskgenerator.User;
import com.mechalikh.pureedgesim.taskorchestrator.DefaultOrchestrator;
import com.mechalikh.pureedgesim.taskorchestrator.Orchestrator;
import com.mechalikh.pureedgesim.taskgenerator.Container;
import com.mechalikh.pureedgesim.taskgenerator.DefaultContainerGenerator;
import com.mechalikh.pureedgesim.taskgenerator.DefaultTaskGenerator;
import com.mechalikh.pureedgesim.simulationengine.FutureQueue;

/**
 * The {@code SimulationManager} class represents the default implementation of
 * the simulation manager. It schedules the offlaoding of tasks, links the
 * different modules and manages the simulation.
 * <p>
 * When the {@link SimulationThread#startSimulation()} method is called, it
 * creates an instance of the SimulationManager, and launches the different
 * modules. Afterwards, it tells the SimulationManager to start the simulation,
 * by calling its {@link #startSimulation()} method. Once called, the simulation
 * engine will start, announcing the beginning of the simulation.
 * 
 * @see #startSimulation()
 * @see com.mechalikh.pureedgesim.simulationmanager.SimulationThread#startSimulation()
 * @see com.mechalikh.pureedgesim.simulationengine.PureEdgeSim#start()
 *
 * @author Charafeddine Mechalikh
 * @since PureEdgeSim 4.2
 */
public class DefaultSimulationManager extends SimulationManager implements OnSimulationStartListener {

	public static Boolean printDebug = false;

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
	public DefaultSimulationManager(SimLog simLog, PureEdgeSim pureEdgeSim, int simulationId, int iteration,
			Scenario scenario) {
		super(simLog, pureEdgeSim, simulationId, iteration, scenario);
	}

	/**
	 * Starts PureEdgeSim simulation engine
	 * 
	 * @see com.mechalikh.pureedgesim.simulationengine.PureEdgeSim#start()
	 */
	@Override
	public void startSimulation() {
		// Show real-time results during the simulation.
		if (SimulationParameters.displayRealTimeCharts && !SimulationParameters.parallelism_enabled)
			simulationVisualizer = new SimulationVisualizer(this);

		simLog.print("%s -  %s", getClass().getSimpleName(), scenario.toString());
		simulation.start();
	}

	/**
	 * Defines the logic to be performed by the simulation manager when the
	 * simulation starts.
	 */
	@Override
	public void onSimulationStart() {
		// Initialize logger variables.
		simLog.setCurrentOrchPolicy(scenario.getStringOrchArchitecture());

		simLog.print("%s - Simulation: %d  , iteration: %d", getClass().getSimpleName(), getSimulationId(),
				getIteration());

		//schedule the containers placement (all of them)
		for(int i = containerList.size(); i > 0; i--){
			if(!containerList.first().getSharedContainer()) schedule(this, containerList.first().getTime() - simulation.clock(), SEND_TO_CLOUD_ORCH, containerList.first());
			else {
				for(ComputingNode cn : dataCentersManager.getComputingNodesGenerator().getOrchestratorsList())
					if(cn.getName().equals("CloudDC")) containerList.first().setOrchestrator(cn);
				schedule(this, containerList.first().getTime() - simulation.clock(), SEND_CONTAINER_FROM_CLOUD_ORCH_TO_VM, containerList.first());
			}
			containerList.remove(containerList.first());
		}

		//creates and schedules the tasks for the shared applications
		for(int i = 0; i < SimulationParameters.applicationList.size(); i++){
			if(SimulationParameters.applicationList.get(i).getSharedContainer()){
				for(User user : SimulationParameters.applicationList.get(i).getUsersList()){
					generateAndScheduleNewTasks(user.getComputingNode());
				}
			}
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

	/**
	 * Processes events or services that are available for the simulation manager.
	 * This method is invoked by the {@link PureEdgeSim} class whenever there is an
	 * event in the deferred queue, which needs to be processed by the entity.
	 * 
	 * @see com.mechalikh.pureedgesim.simulationengine.SimEntity#processEvent(Event
	 *      ev)
	 *
	 * @param ev information about the event just happened
	 */
	@Override
	public void processEvent(Event ev) {
		Task task;
		Container container;
		switch (ev.getTag()) {
		case NEXT_BATCH:
			// Schedule this batch.
			for (int i = 0; i < Math.min(taskList.size(), SimulationParameters.batchSize); i++) {
				schedule(this, taskList.first().getTime() - simulation.clock(), SEND_TO_EDGE_ORCH, taskList.first());
				taskList.remove(taskList.first());
			}
			// Schedule the offloading of next batch
			if (taskList.size() > 0)
				schedule(this, taskList.first().getTime() - simulation.clock(), NEXT_BATCH);
			break;
		case SEND_TO_EDGE_ORCH:
			// Send the offloading request to the closest orchestrator.
			task = (Task) ev.getData();
			sendTaskToEdgeOrchestrator(task);
			sentTasks++;
			break;

		case SEND_TO_CLOUD_ORCH:
			container = (Container) ev.getData();
			//System.out.println("Inviata la richiesta di placement al cloud: " + container.getId());
			sendContainerRequestToCloudOrchestrator(container);
			break;
		case SEND_UNPLACEMENT_TO_CLOUD_ORCH:
			container = (Container) ev.getData();
			//if(DefaultContainerGenerator.debugContainer) System.out.println("Ho ricevuto la richiesta di Unplacement per il dispositivo " + container.getEdgeDevice(container.getEdgeDevices().size()-1).getName() + " al tempo: " + simulation.clock() + ", associata all'utente " + container.getEdgeDevice(container.getEdgeDevices().size()-1).getUser() + " dell'applicazione " + SimulationParameters.applicationList.get(container.getEdgeDevice(container.getEdgeDevices().size()-1).getApplicationType()).getName());
			sendContainerUnplacementRequestToCloudOrchestrator(container);
			break;
		case SEND_TASK_FROM_EDGE_ORCH_TO_DESTINATION:
			// The offlaoding decision was made, send the request from the orchestrator to
			// the offloading destination.
			task = (Task) ev.getData();
			sendFromEdgeOrchToDestination(task);
			break;

		case SEND_CONTAINER_FROM_CLOUD_ORCH_TO_VM:
			// The placement decision was made, send the request from the orchestrator to
			// the placement destination.
			sendFromCloudOrchToDestination((Container) ev.getData());
			break;
		case SEND_UNPLACEMENT_FROM_CLOUD_ORCH_TO_VM:
			//The CloudOrchestrator ordered the unplacement of a Container based on an EdgeDevice request.
			//So I remove the container from the list of the orchestrator
			cloudOrchestrator.removeContainerFromVM((Container) ev.getData());
			sendUnplacementFromCloudOrchToDestination((Container) ev.getData());
			break;
		case EXECUTE_TASK:
			// Offlaoding request received by the destination, execute the task.
			task = (Task) ev.getData();
			if (taskFailed(task, 2))
				return;
			task.getOffloadingDestination().submitTask(task);
			edgeOrchestrator.notifyOrchestratorOfTaskExecution(task);
			break;
		case DOWNLOAD_CONTAINER:
			// Placemenet request received by the destination, place the container.
			container = (Container) ev.getData();
			container.getPlacementDestination().submitContainerPlacement(container);
			cloudOrchestrator.notifyOrchestratorOfContainerExecution(container);
			break;

		case UNPLACEMENT_CONTAINER:
			// Placemenet request received by the destination, place the container.
			container = (Container) ev.getData();
			container.getPlacementDestination().submitContainerUnPlacement(container);
			break;
		case TRANSFER_RESULTS_TO_EDGE_ORCH:
			// Task execution finished, transfer the results to the orchestrator.
			task = (Task) ev.getData();
			finishedTasks.add(task);
			sendResultsToEdgeOchestrator(task);
			break;

		case TRANSFER_RESULTS_TO_CLOUD_ORCH:
			// Task execution finished, transfer the results to the orchestrator.
			sendResultsToCloudOchestrator((Container) ev.getData());
			break;
		case TRANSFER_UNPLACEMENT_RESULTS_TO_CLOUD_ORCH:
			// Task execution finished, transfer the results to the orchestrator.
			sendUnplacementResultsToCloudOchestrator((Container) ev.getData());
			break;
		case RESULTS_FROM_CLOUD_TO_EDGE_ORCH:
			//Container placement has finished and i notified the orchestrator.
			edgeOrchestrator.setContainerToVM((Container) ev.getData());
			break;
		case UNPLACEMENT_RESULTS_FROM_CLOUD_TO_EDGE_ORCH:
			//Container unplacement has finished and i notified the EdgeOrchestrator.
			//So I remove the container from the list of the orchestrator
			edgeOrchestrator.removeContainerFromVM((Container) ev.getData());
			break;
		case TASK_RESULT_RETURN_FINISHED:
			// Results returned to edge device.
			task = (Task) ev.getData();
			if (taskFailed(task, 3))
				return;

			edgeOrchestrator.resultsReturned(task);
			tasksCount++;
			break;

		case PLACEMENT_RESULT_RETURN_FINISHED:
			// Results returned to edge device.
			container = (Container) ev.getData();
			placedContainers.add(container);
			cloudOrchestrator.resultsReturned(container);

			//se il container è shared allora fermo qui
			if(container.getSharedContainer()) break;
			//altrimenti procedo con unplacement container e generazione task

			//schedulo l'unplacement del container
			schedule(this, container.getDuration() - simulation.clock(), SEND_UNPLACEMENT_TO_CLOUD_ORCH, container);

			//inizio a generare i task per l'edge device che ha ottenuto il placement e li schedulo
			generateAndScheduleNewTasks(container.getEdgeDevice(container.getEdgeDevices().size()-1));
			break;

		case UNPLACEMENT_RESULT_RETURN_FINISHED:
			// Results returned to edge device.
			container = (Container) ev.getData();
			placedContainers.remove(container);
			break;
		case SHOW_PROGRESS:
			// Calculate the simulation progress.
			int progress;
			if(simLog.getGeneratedTasks() > 0) progress = 100 * tasksCount / simLog.getGeneratedTasks();
			else progress = 0;
			if (oldProgress != progress) {
				oldProgress = progress;
				if (progress % 10 == 0 || (progress % 10 < 5) && lastWrittenNumber + 10 < progress) {
					lastWrittenNumber = progress - progress % 10;
					if (lastWrittenNumber != 100)
						simLog.printSameLine(" " + lastWrittenNumber + " ", "red");
				} else
					simLog.printSameLine("#", "red");
			}
			schedule(this, SimulationParameters.simulationDuration / 100, SHOW_PROGRESS);
			break;

		case UPDATE_REAL_TIME_CHARTS:
			// Update simulation Map, network utilization, and the other real-time charts.
			simulationVisualizer.updateCharts();

			// Schedule the next update.
			schedule(this, SimulationParameters.chartsUpdateInterval, UPDATE_REAL_TIME_CHARTS);
			break;

		case PRINT_LOG:

			// Whether to wait or not, if some tasks have not been executed yet.
			if (SimulationParameters.waitForAllTasksToFinish && (tasksCount / simLog.getGeneratedTasks()) < 1) {
				// 1 = 100% , 0,9= 90%
				// Some tasks may take hours to be executed that's why we don't wait until
				// all of them get executed, but we only wait for 99% of tasks to be executed at
				// least, to end the simulation. that's why we set it to " < 0.99"
				// especially when 1% doesn't affect the simulation results that much, change
				// this value to lower ( 95% or 90%) in order to make simulation faster. however
				// this may affect the results.
				schedule(this, 10, PRINT_LOG);
				break;
			}

			simLog.printSameLine(" 100% ]", "red");

			if (SimulationParameters.displayRealTimeCharts && !SimulationParameters.parallelism_enabled) {

				// Close real time charts after the end of the simulation.
				if (SimulationParameters.autoCloseRealTimeCharts)
					simulationVisualizer.close();
				try {
					// Save those charts in bitmap and vector formats.
					if (SimulationParameters.saveCharts)
						simulationVisualizer.saveCharts();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			// Show results and stop the simulation.
			simLog.showIterationResults(finishedTasks);

			// Terminate the simulation.
			simulation.terminate();
			break;
		default:
			simLog.print("%s - Unknown event type", this.getClass().getSimpleName());
			break;
		}

	}

	protected void generateAndScheduleNewTasks(ComputingNode cn){
		//inizio a generare i task per l'edge device che ha ottenuto il placement
		DefaultTaskGenerator tasksGenerator = new DefaultTaskGenerator(this);
		FutureQueue<Task> taskListProvvisoria = tasksGenerator.generateNewTasks(cn);
		//System.out.println("Il device " + cn.getName() + " ha generato " + taskListProvvisoria.size() + " tasks per lo user " + SimulationParameters.applicationList.get(cn.getApplicationType()).getUsersList().get(cn.getUser()).getType() + " relativi all'applicazione: " +  SimulationParameters.applicationList.get(cn.getApplicationType()).getName());
		simLog.setGeneratedTasks(simLog.getGeneratedTasks() + taskListProvvisoria.size());
		//schedulo i task generati
		while(!taskListProvvisoria.isEmpty()){
			schedule(this, taskListProvvisoria.first().getTime() - simulation.clock(), SimulationManager.SEND_TO_EDGE_ORCH, taskListProvvisoria.first());
			taskListProvvisoria.remove(taskListProvvisoria.first());
		}
	}

	/**
	 * Returns the task execution results to the orchestrator.
	 *
	 * @param task The task that has been executed.
	 */
	protected void sendResultsToEdgeOchestrator(Task task) {

		//inserisco la latency finale del Task nel vettore di logging
		getSimulationLogger().applicationLatencyMap.get(task.getAssociatedAppName()).set(0,  getSimulationLogger().applicationLatencyMap.get(task.getAssociatedAppName()).get(0) + task.getTotalDelay());
		getSimulationLogger().applicationLatencyMap.get(task.getAssociatedAppName()).set(1,  getSimulationLogger().applicationLatencyMap.get(task.getAssociatedAppName()).get(1) + 1);

		if (taskFailed(task, 2))
			return;
		// If the task was offloaded
		if (task.getEdgeDevice() != task.getOffloadingDestination())
			scheduleNow(getNetworkModel(), NetworkModel.SEND_RESULT_TO_EDGE_ORCH, task);
		else // The task has been executed locally / no offloading
			scheduleNow(this, TASK_RESULT_RETURN_FINISHED, task);

		// Update tasks execution and waiting delays
		simLog.getTasksExecutionInfos(task);
	}

	/**
	 * Returns the Container placement execution to the orchestrator.
	 *
	 * @param container The container that has been placed.
	 */
	protected void sendResultsToCloudOchestrator(Container container) {
		scheduleNow(getNetworkModel(), NetworkModel.SEND_RESULT_TO_CLOUD_ORCH, container);
	}

	/**
	 * Returns the Container placement execution to the orchestrator.
	 *
	 * @param container The container that has been placed.
	 */
	protected void sendUnplacementResultsToCloudOchestrator(Container container) {
		scheduleNow(getNetworkModel(), NetworkModel.SEND_UNPLACEMENT_RESULT_TO_CLOUD_ORCH, container);
	}

	/**
	 * Sends the task from the orchestrator to the offloading destination.
	 *
	 * @param task The task that has been offlaoded.
	 */
	protected void sendFromEdgeOrchToDestination(Task task) {
		if (taskFailed(task, 1))
			return;
			
		// Find the resource node for executing the task.
		edgeOrchestrator.orchestrate(task);

		// Stop if no resource is available for this task, the offloading is failed.
		if (task.getOffloadingDestination() == ComputingNode.NULL) {

			task.setFailureReason(Task.FailureReason.NO_OFFLOADING_DESTINATIONS);
			simLog.incrementTasksFailedLackOfRessources(task);
			tasksCount++;
			failedTasksCount++;
			return;
		}

		simLog.taskSentFromOrchToDest(task);

		// Send the task from the orchestrator to the destination
		scheduleNow(getNetworkModel(), NetworkModel.SEND_REQUEST_FROM_EDGE_ORCH_TO_DESTINATION, task);

	}

	/**
	 * Sends the Container from the orchestrator to the placement destination.
	 *
	 * @param task The task that has been offlaoded.
	 */
	protected void sendFromCloudOrchToDestination(Container container) {
			
		// Find the best resource node for executing the task.
		cloudOrchestrator.orchestrate(container);

		// Stop if no resource is available for this task, the offloading is failed.
		if (container.getPlacementDestination() == ComputingNode.NULL) {

			container.setFailureReason(Container.FailureReason.NO_PLACEMENT_DESTINATIONS);
			System.out.println("Non ho trovato destinazione di placement per il container " + container.getId() + " con size: " + container.getContainerSizeInBits());
			return;
		}

		//simLog.taskSentFromOrchToDest(task);

		// Send the task from the orchestrator to the destination
		scheduleNow(getNetworkModel(), NetworkModel.SEND_CONTAINER_FROM_CLOUD_ORCH_TO_DESTINATION, container);

	}
	
	/**
	 * Sends the Container from the orchestrator to the placement destination.
	 *
	 * @param task The task that has been offlaoded.
	 */
	protected void sendUnplacementFromCloudOrchToDestination(Container container) {
		cloudOrchestrator.findVmAssociatedWithContainer(container);
		scheduleNow(getNetworkModel(), NetworkModel.SEND_CONTAINER_UNPLACEMENT_FROM_CLOUD_ORCH_TO_DESTINATION, container);
	}
	
	/**
	 * Sends the task to the orchestrator in order to make the offloading decision.
	 *
	 * @param task The task that needs to be offloaded.
	 */
	protected void sendTaskToEdgeOrchestrator(Task task) {
		if (taskFailed(task, 0))
			return;

		// if (SimulationParameters.enableOrchestrators)
		// 	task.setOrchestrator(task.getEdgeDevice().getOrchestrator());
		// simLog.incrementTasksSent();

		task.setOrchestrator(task.getEdgeDevice().getEdgeOrchestrator());
		simLog.incrementTasksSent();

		scheduleNow(networkModel, NetworkModel.SEND_REQUEST_FROM_DEVICE_TO_EDGE_ORCH, task);
	}

	/**
	 * Sends the Container request to the cloud orchestrator in order to make the placement decision.
	 *
	 * @param container The container that needs to be placed.
	 */
	protected void sendContainerRequestToCloudOrchestrator(Container container) {
		container.setOrchestrator(container.getEdgeDevice(0).getCloudOrchestrator());
		scheduleNow(networkModel, NetworkModel.SEND_REQUEST_FROM_DEVICE_TO_CLOUD_ORCH, container);
	}

	/**
	 * Sends the Container Unplacement request to the cloud orchestrator.
	 *
	 * @param container The container that needs to be placed.
	 */
	protected void sendContainerUnplacementRequestToCloudOrchestrator(Container container) {
		scheduleNow(networkModel, NetworkModel.SEND_UNPLACEMENT_REQUEST_FROM_DEVICE_TO_CLOUD_ORCH, container);
	}

	/**
	 * Used to get the task failure rate.
	 * 
	 * @return The failure rate.
	 */
	public double getFailureRate() {
		double result = ((double) failedTasksCount * 100) / Math.max(1, sentTasks);
		//failedTasksCount = 0;															//MODIFICA MIA
		//sentTasks = 0;																//MODIFICA MIA
		return result;
	}

	/**
	 * Sets the task as failed and provides the reason of failure.
	 *
	 * @param task  The task that has been offloaded.
	 * @param phase At which phase the task has been failed.
	 * 
	 * @return task execution status.
	 */
	public boolean taskFailed(Task task, int phase) {
		// task not generated because device died
		if (task.getEdgeDevice().isDead()) {
			simLog.incrementFailedBeacauseDeviceDead(task);
			task.setFailureReason(Task.FailureReason.FAILED_BECAUSE_DEVICE_DEAD);
			return setFailed(task, phase);
		}
		// or if the orchestrator died
		if (phase == 1 && task.getOrchestrator() != ComputingNode.NULL && task.getOrchestrator().isDead()) {
			task.setFailureReason(Task.FailureReason.FAILED_BECAUSE_DEVICE_DEAD);
			simLog.incrementFailedBeacauseDeviceDead(task);
			return setFailed(task, phase);
		}
		// or the destination device is dead
		if (phase == 2 && ((ComputingNode) task.getOffloadingDestination()).isDead()) {
			task.setFailureReason(Task.FailureReason.FAILED_BECAUSE_DEVICE_DEAD);
			simLog.incrementFailedBeacauseDeviceDead(task);
			return setFailed(task, phase);
		}
		// If storage and ram are not sufficient to perform the task.															// //non si può verificare perchè istanzio i container prima
		// if (phase == 2 && (task.getOffloadingDestination().getAvailableStorage() < task.getContainerSizeInMBytes()			// //di quando istanzio i task
		// 		|| task.getOffloadingDestination().getAvailableRam() < task.getContainerSizeInMBytes())) {
		// 	task.setFailureReason(Task.FailureReason.INSUFFICIENT_RESOURCES);
		// 	simLog.incrementTasksFailedLackOfRessources(task);
		// 	return setFailed(task, phase);
		// }
		// A simple representation of task failure due to
		// device mobility, if the offloading destination location doesn't match
		// the edge device location (that generated this task)
		if (phase == 1 && task.getOrchestrator() != ComputingNode.NULL
				&& task.getOrchestrator().getType() != SimulationParameters.TYPES.CLOUD
				&& !sameLocation(task.getEdgeDevice(), task.getOrchestrator())) {
			task.setFailureReason(Task.FailureReason.FAILED_DUE_TO_DEVICE_MOBILITY);
			simLog.incrementTasksFailedMobility(task);
			return setFailed(task, phase);
		}
		if (phase == 2 && task.getOffloadingDestination() != ComputingNode.NULL
				&& task.getOffloadingDestination().getType() != SimulationParameters.TYPES.VM_CLOUD						//modifica effettuata perchè venivano conteggiati erroneamente
				&& (!sameLocation(task.getEdgeDevice(), task.getOrchestrator())											//i task failures per la mobility. Prima era CLOUD anzichè VM_CLOUD
						|| !sameLocation(task.getOrchestrator(), task.getOffloadingDestination()))) {
			task.setFailureReason(Task.FailureReason.FAILED_DUE_TO_DEVICE_MOBILITY);
			simLog.incrementTasksFailedMobility(task);
			return setFailed(task, phase);
		}
		// The task is failed due to long delay
		if (phase == 3 && task.getTotalDelay() >= task.getMaxLatency()) {
			task.setFailureReason(Task.FailureReason.FAILED_DUE_TO_LATENCY);

			//printa il percorso e i motivi del fallimento per latency
			if(printDebug){
				System.out.println("---------------------task: " + task.getId() + " associatedAppName: " + task.getAssociatedAppName() + ", waitingtime: " + task.getWatingTime() + " getActualNetworkTime: " + task.getActualNetworkTime() + " getActualCpuTime: " + task.getActualCpuTime());
				GraphPath<ComputingNode, NetworkLink> path = getDataCentersManager().getTopology().getPath(task.getEdgeDevice(), task.getEdgeDevice().getEdgeOrchestrator());
				System.out.println("from: " + task.getEdgeDevice().getName() + ", to: " + task.getOffloadingDestination().getName());
				System.out.print("[");
				for(ComputingNode cn : path.getVertexList()) System.out.print(cn.getName() + ", ");
				path = getDataCentersManager().getTopology().getPath(task.getEdgeDevice().getEdgeOrchestrator(), task.getOffloadingDestination());
				path.getVertexList().remove(0);
				for(ComputingNode cn : path.getVertexList()) System.out.print(cn.getName() + ", ");
				System.out.print("]");
				System.out.println("");
				System.out.println("Path latency approssimata = " + ((DefaultOrchestrator) edgeOrchestrator).getPathLatencyFromDeviceToOffloadingDestination(task, task.getEdgeDevice(), task.getOffloadingDestination()));
			}

			simLog.incrementTasksFailedLatency(task);
			return setFailed(task, phase);
		}
		return false;
	}

	/**
	 * Sets the task as failed.
	 *
	 * @param task  The task that has been offloaded.
	 * @param phase
	 */
	protected boolean setFailed(Task task, int phase) {

		// Keep record of the failed and returned tasks
		failedTasksCount++;
		tasksCount++;

		task.getOffloadingDestination().incrementTasksFailed();									//aggiunto per visualizzare in tempo reale i task falliti dalle VM

		// Return the execution results, only if the task has been sent to the
		// orchestrator previously.
		if (phase > 1)
			edgeOrchestrator.resultsReturned(task);
		return true;
	}

	/**
	 * Checks if computing nodes can communicate directly (1 hop).
	 * 
	 * @return true if these computing nodes can communicate through a single hop.
	 */
	protected boolean sameLocation(ComputingNode Dev1, ComputingNode Dev2) {
		if (Dev1.getType() == TYPES.CLOUD || Dev2.getType() == TYPES.CLOUD)
			return true;
		double distance = Dev1.getMobilityModel().distanceTo(Dev2);
		int RANGE = SimulationParameters.edgeDevicesRange;
		if (Dev1.getType() != Dev2.getType()) // One of them is an edge data center and the other is an edge device
			RANGE = SimulationParameters.edgeDataCentersRange;
		return (distance < RANGE);
	}

}