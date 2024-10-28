package com.mechalikh.pureedgesim.taskgenerator;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Random;
import java.util.stream.IntStream;

import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.simulationengine.FutureQueue;
import com.mechalikh.pureedgesim.simulationmanager.SimulationManager;

public class DefaultContainerGenerator extends ContainerGenerator {
	/**
	 * Used to generate random values.
	 * 
	 * @see #generate()
	 * @see #generateTasksForDevice(ComputingNode, int)
	 */
	protected Random random;
	protected int id = 0;
	protected double simulationTime;

	public DefaultContainerGenerator(SimulationManager simulationManager) {
		super(simulationManager);
		try {
			random = SecureRandom.getInstanceStrong();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Generates a queue of containers based on the simulation parameters.
	 *
	 * @return a queue of containers
	 */
	public FutureQueue<Container> generate() {

		// Remove devices that do not generate
		devicesList.removeIf(dev -> !dev.isGeneratingTasks());

		int devicesCount = devicesList.size();

		// Browse all applications
		IntStream.range(0, SimulationParameters.applicationList.size() - 1).forEach(app -> {
			int numberOfDevices = (int) (SimulationParameters.applicationList.get(app).getUsagePercentage()
					* devicesCount / 100);
			IntStream.range(0, numberOfDevices).mapToObj(i -> devicesList.remove(random.nextInt(devicesList.size())))
					.peek(dev -> dev.setApplicationType(app)).forEach(dev -> generateContainerForDevice(dev, app));                             
		});

		devicesList.forEach(dev -> generateContainerForDevice(dev, SimulationParameters.applicationList.size() - 1));
		return this.getContainerList();
	}

	/**
	 * Generates containers that will be placed during simulation for the given device
	 * and application.
	 * 
	 * @param device the device to generate containers for
	 * @param app    the application type
	 */
	protected void generateContainerForDevice(ComputingNode dev, int app) {               
		insert(0, app, dev);
	}

	/**
	 * Inserts a container into the containers list.
	 * 
	 * @param time   the time in seconds at which the container should be placed
	 * @param app    the application type of the container
	 * @param device the device that requests the container's placement
	 */
	protected void insert(int time, int app, ComputingNode dev) {
		Application appParams = SimulationParameters.applicationList.get(app);                  
		long containerSize = appParams.getContainerSizeInBits();
		String Name = appParams.getName();

		Container container = createContainer(++id).setAssociatedAppName(Name).setContainerSizeInBits(containerSize).setApplicationID(app).addEdgeDevice(dev);

		time += 0;
		container.setTime(time);
		container.getEdgeDevice(0).setApplicationType(app);									//setto qui l'app type perchè sopra non sembra funzionare nella lambda

		containerList.add(container);
		getSimulationManager().getSimulationLogger().deepLog("BasicContainersGenerator, Container " + id + " with execution time " + time + " (s) generated.");
		
	}

	/**
	 * 
	 * Creates a new instance of Container using the specified ID.
	 * 
	 * @param id the ID to assign to the new container
	 * @return the new Container instance
	 */
	protected Container createContainer(int id) {
		try {
            Container prova = new DefaultContainer(id);
            return prova;
			//return containerClass.getConstructor(int.class).newInstance(id);          //Non so perchè non funzioni
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

}
