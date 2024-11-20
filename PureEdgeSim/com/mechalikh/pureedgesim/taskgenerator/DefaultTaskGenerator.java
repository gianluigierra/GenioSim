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
package com.mechalikh.pureedgesim.taskgenerator;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Random;
import java.util.stream.IntStream;

import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.simulationengine.FutureQueue;
import com.mechalikh.pureedgesim.simulationmanager.SimulationManager;

public class DefaultTaskGenerator extends TaskGenerator {
	/**
	 * Used to generate random values.
	 * 
	 * @see #generate()
	 * @see #generateTasksForDevice(ComputingNode, int)
	 */
	protected Random random;
	protected int id = 0;
	protected double simulationTime;
	protected double currentTime;

	public DefaultTaskGenerator(SimulationManager simulationManager) {
		super(simulationManager);
		try {
			random = SecureRandom.getInstanceStrong();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Generates a queue of tasks for the node which container got placed.
	 *
	 * @return a queue of tasks
	 */
	public FutureQueue<Task> generateNewTasks(ComputingNode computingNode) {
		// Get simulation time in minutes (excluding the current simulation time)
		simulationTime = SimulationParameters.simulationDuration / 60;
		// Get clock time in minutes
		currentTime = simulationManager.getSimulation().clockInMinutes();

		//se l'applicazione associata al dispositivo è shared uso questa funzione
		if(!SimulationParameters.applicationList.get(computingNode.getApplicationType()).getSharedContainer()) generateTasksForDevice(computingNode, computingNode.getApplicationType());
		//atrimenti li genero per la durata della simulazione
		else generateTasksForDeviceForAllSimulation(computingNode, computingNode.getApplicationType());

		return this.getTaskList();
	}

	/**
	 * Generates tasks that will be offloaded during simulation for the given device
	 * and application.
	 * 
	 * @param device the device to generate tasks for
	 * @param app    the application type
	 */
	protected void generateTasksForDeviceForAllSimulation(ComputingNode dev, int app) {
		currentTime += (int) SimulationParameters.applicationList.get(app).getUsersList().get(dev.getUser()).getStart();
		while((int) currentTime <= SimulationParameters.simulationDuration/60){
			int remainingTime = 0;
			//calcolo il remaining time della simulazione, nel caso che debba iniziare ad offloadare tasks poco prima che questa finisca
			if((int) SimulationParameters.applicationList.get(app).getUsersList().get(dev.getUser()).getDuration() + (int) currentTime <= SimulationParameters.simulationDuration/60) 
				remainingTime = (int) SimulationParameters.applicationList.get(app).Users.get(dev.getUser()).getDuration() + (int) currentTime;
			else 
				remainingTime = (int) SimulationParameters.simulationDuration/60;
			
			if(SimulationParameters.applicationList.get(app).getUsersList().get(dev.getUser()).getAccessPatter().equals("random"))
				IntStream.range((int) currentTime, remainingTime)
				//First get time in seconds
				.forEach(st -> insert((st * 60)
						// Then pick up random second in this minute "st". Shift the time by a random
						// value
						+ random.nextInt(1,15), app, dev.getUser(), dev));
			
			currentTime += (int) SimulationParameters.applicationList.get(app).getUsersList().get(dev.getUser()).getDuration() + (int) SimulationParameters.applicationList.get(app).getUsersList().get(dev.getUser()).getInterval();
		}
	}

	/**
	 * Generates tasks that will be offloaded during simulation for the given device
	 * and application.
	 * 
	 * @param device the device to generate tasks for
	 * @param app    the application type
	 */
	protected void generateTasksForDevice(ComputingNode dev, int app) {
		int remainingTime = 0;
		//calcolo il remaining time della simulazione, nel caso che debba iniziare ad offloadare tasks poco prima che questa finisca
		if((int) SimulationParameters.applicationList.get(app).getUsersList().get(dev.getUser()).getDuration() + (int) currentTime <= SimulationParameters.simulationDuration/60) 
			remainingTime = (int) SimulationParameters.applicationList.get(app).Users.get(dev.getUser()).getDuration() + (int) currentTime;
		else 
			remainingTime = (int) SimulationParameters.simulationDuration/60;
		
		if(SimulationParameters.applicationList.get(app).getUsersList().get(dev.getUser()).getAccessPatter().equals("random"))
			IntStream.range((int) currentTime, remainingTime)
			//First get time in seconds
			.forEach(st -> insert((st * 60)
					// Then pick up random second in this minute "st". Shift the time by a random
					// value
					+ random.nextInt(1,15), app, dev.getUser(), dev));
	}

	/**
	 * Inserts a task into the task list.
	 * 
	 * @param time   the time in seconds at which the task should be executed
	 * @param app    the application type of the task
	 * @param device the device that generates the task
	 */
	protected void insert(double time, int app, int u, ComputingNode dev) {
		Application appParams = SimulationParameters.applicationList.get(app);
		long requestSize = appParams.getRequestSize();
		long outputSize = appParams.getResultsSize();
		double maxLatency = appParams.getLatency();
		long length = (long) appParams.getTaskLength();
		String Name = appParams.getName();

		User user = appParams.getUsersList().get(u);
		double duration = user.getDuration();			//duration = tempo in minuti
		int rate = user.getRate();						//rate = task al minuto
		double taskDuration = 60.0 / rate;

		for (int i = 0; i < rate; i++) {
			Task task = createTask(++id).setType(appParams.getType()).setFileSizeInBits(requestSize).setAssociatedAppName(Name)
					.setOutputSizeInBits(outputSize).setApplicationID(app)
					.setMaxLatency(maxLatency).setLength(length).setEdgeDevice(dev);

			time += taskDuration;

			//nel caso che stia provando ad effettuare l'offloading di un task che eccede la durata del placement del container
			//lo offloado all'inizio del placement con uno scostamento pari ad extratime = durata di eccesso.
			if(time >= (currentTime*60 + duration*60)) {
				double extraTime = time - (currentTime*60 + duration*60);	
				time = 60*(int) currentTime;
				time += extraTime;
				//nel caso che si possa creare un evento passato sommo al tempo corrente un valore randomico
				if(time <=  simulationManager.getSimulation().clock()){
					int randomDuration = random.nextInt(1, 15);
					time += randomDuration;
				}
			}

			task.setTime(time);			
			
			//System.out.println("Ho generato un task al tempo: " + task.getTime());

			taskList.add(task);
			getSimulationManager().getSimulationLogger().deepLog("BasicTasksGenerator, Task " + id + " with execution time " + time + " (s) generated.");
		}
	}

	/**
	 * 
	 * Creates a new instance of Task using the specified ID.
	 * 
	 * @param id the ID to assign to the new task
	 * @return the new Task instance
	 */
	protected Task createTask(int id) {
		try {
			return taskClass.getConstructor(int.class).newInstance(id);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

}
