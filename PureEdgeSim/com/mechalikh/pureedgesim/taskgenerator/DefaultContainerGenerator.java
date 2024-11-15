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
	public static boolean debugContainer = true;

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
		//seleziono uno User dalla lista degli user non Deployati per l'app scelta   
		User primoUser = new User();

		for(int i = 0; i < SimulationParameters.applicationList.get(app).getUsersList().size(); i++){
			if(!SimulationParameters.applicationList.get(app).getUsersList().get(i).isDeployed()){
				primoUser = SimulationParameters.applicationList.get(app).getUsersList().get(i);
				//setto lo user come deployato
				primoUser.setIsDeployed(true); 
				//associo il dispositivo allo user   
				primoUser.setComputingNode(dev); 
				////associo lo user al dispositivo
				dev.setUser(i); 
				//creo i container (piazzo quelli correnti ed eventualmente quelli futuri)
				insert(0, app, i, dev);
				return;
			}
		}

	}

	/**
	 * Inserts a container into the containers list.
	 * 
	 * @param time   the time in seconds at which the container should be placed
	 * @param app    the application type of the container
	 * @param device the device that requests the container's placement
	 */
	protected void insert(int time, int app, int i, ComputingNode dev) {
		Application appParams = SimulationParameters.applicationList.get(app);                  
		long containerSize = appParams.getContainerSizeInBits();
		long containerRequestSize = appParams.getContainerRequestSize();
		boolean sharedContainer = appParams.getSharedContainer();
		String Name = appParams.getName();

		User user = appParams.getUsersList().get(i);
		int start = 60*user.getStart();						//user.getStart è in minuti, noi lo vogliamo in secondi 
		double duration = 60*user.getDuration();			//user.getDuration è in minuti, noi lo vogliamo in secondi 
		int interval = 60*user.getInterval();				//user.getInterval è in minuti, noi lo vogliamo in secondi 
		
		time += start;
		while(time < SimulationParameters.simulationDuration){
			id++;
			Container container = createContainer(id).setAssociatedAppName(Name).setContainerSizeInBits(containerSize)
												   .setFileSizeInBits(containerRequestSize).setSharedContainer(sharedContainer)
												   .setApplicationID(app).addEdgeDevice(dev);
			container.getEdgeDevice(0).setApplicationType(app);
			container.setTime(time);
			containerList.add(container);
			getSimulationManager().getSimulationLogger().deepLog("BasicContainersGenerator, Container " + id + " with placement time " + time + " (s) generated.");

			if(debugContainer) System.out.println("Ho generato la richiesta di placement per il dispositivo " + dev.getName() + " al tempo: " + time + ", associata all'utente " + i + " dell'applicazione " + SimulationParameters.applicationList.get(app).name);

			time += duration;

			container.setDuration(time);

			if(debugContainer) System.out.println("Ho generato la richiesta di Unplacement per il dispositivo " + dev.getName() + " al tempo: " + time + ", associata all'utente " + i + " dell'applicazione " + SimulationParameters.applicationList.get(app).name);

			getSimulationManager().getSimulationLogger().deepLog("BasicContainersGenerator, Container " + id + " with unplacement time " + time + " (s) generated.");

			time += interval;
		}
		
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
