package com.mechalikh.pureedgesim.taskgenerator;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Random;
import java.util.List;
import java.util.ArrayList;
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
	public static boolean debugContainer = false;

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

		// Browse all applications
		IntStream.range(0, SimulationParameters.applicationList.size()).forEach(app -> {

			int numberOfDevices = (int) (SimulationParameters.applicationList.get(app).getUsersList().size());

			if(!SimulationParameters.applicationList.get(app).getSharedContainer()){
				//caso nel quale i container non sono shared
				IntStream.range(0, numberOfDevices).mapToObj(i -> devicesList.remove(random.nextInt(devicesList.size())))
						.peek(dev -> dev.setApplicationType(app)).forEach(dev -> generateContainerForDevice(dev, app));    
			}  
			else{
				//caso nel quale i container sono shared;
				List<ComputingNode> deviceAppList = new ArrayList<ComputingNode>();
					
				IntStream.range(0, numberOfDevices).mapToObj(i -> devicesList.remove(random.nextInt(devicesList.size())))
				.peek(dev -> dev.setApplicationType(app)).forEach(dev -> deviceAppList.add(dev));
				
				generateContainerForDeviceList(deviceAppList, app);
				
			}
			
		});

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
		for(int i = 0; i < SimulationParameters.applicationList.get(app).getUsersList().size(); i++){
			if(!SimulationParameters.applicationList.get(app).getUsersList().get(i).isDeployed()){
				//setto lo user come deployato
				SimulationParameters.applicationList.get(app).getUsersList().get(i).setIsDeployed(true);
				//associo il dispositivo allo user
				SimulationParameters.applicationList.get(app).getUsersList().get(i).setComputingNode(dev);
				//associo lo user al dispositivo
				dev.setUser(i); 
				//creo i container (piazzo quelli correnti ed eventualmente quelli futuri)
				insert(0, app, i, dev);					//caso non shared
				return;
			}
		}

	}

	/**
	 * Generates a container that will be placed at the start of the simulation for the given devices
	 * 
	 * @param devList the devices list to generate the container for
	 * @param app    the application type
	 */
	protected void generateContainerForDeviceList(List<ComputingNode> devList, int app) {
		int devIterator = 0;
		//seleziono uno alla volta gli User dalla lista degli user non Deployati per l'app scelta   
		for(int i = 0; i < SimulationParameters.applicationList.get(app).getUsersList().size(); i++){
			if(!SimulationParameters.applicationList.get(app).getUsersList().get(i).isDeployed()){
				//setto lo user come deployato
				SimulationParameters.applicationList.get(app).getUsersList().get(i).setIsDeployed(true);
				//associo il dispositivo allo user
				SimulationParameters.applicationList.get(app).getUsersList().get(i).setComputingNode(devList.get(devIterator));
				//associo lo user al dispositivo
				devList.get(devIterator).setUser(i); 
				//incremento l'iterator della devList
				devIterator++;
			}
		}
		//creo i container
		insertShared(0, app, devList);			//caso shared
		return;
	}

	/**
	 * Inserts a container into the containers list.
	 * 
	 * @param time   the time in seconds at which the container should be placed
	 * @param app    the application type of the container
	 * @param i 	 the user type of the container
	 * @param dev 	 the device that requests the container's placement
	 */
	protected void insert(int time, int app, int i, ComputingNode dev) {
		Application appParams = SimulationParameters.applicationList.get(app);                  
		long containerSize = appParams.getContainerSizeInBits();
		long containerRequestSize = appParams.getContainerRequestSize();
		String Name = appParams.getName();

		User user = appParams.getUsersList().get(i);
		int start = 60*user.getStart();						//user.getStart è in minuti, noi lo vogliamo in secondi 
		double duration = 60*user.getDuration();			//user.getDuration è in minuti, noi lo vogliamo in secondi 
		int interval = 60*user.getInterval();				//user.getInterval è in minuti, noi lo vogliamo in secondi 
		
		time += start;
		while(time < SimulationParameters.simulationDuration){
			id++;
			Container container = createContainer(id).setAssociatedAppName(Name).setContainerSizeInBits(containerSize)
												   .setFileSizeInBits(containerRequestSize).setSharedContainer(false)
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
	 * Inserts a shared container into the containers list.
	 * 
	 * @param time   the time in seconds at which the container should be placed
	 * @param app    the application type of the container
	 * @param devList the devices List that is associated to the container's placement
	 */
	protected void insertShared(int time, int app, List<ComputingNode> devList) {
		Application appParams = SimulationParameters.applicationList.get(app);                  
		long containerSize = appParams.getContainerSizeInBits();
		long containerRequestSize = appParams.getContainerRequestSize();
		String Name = appParams.getName();
		int copies = SimulationParameters.applicationList.get(app).getContainerCopies();

		for(int i = 0; i < copies; i++){
			time = 0;
			id++;
			Container container = createContainer(id).setAssociatedAppName(Name).setContainerSizeInBits(containerSize)
													.setFileSizeInBits(containerRequestSize).setSharedContainer(true)
													.setApplicationID(app);
			container.getEdgeDevices().addAll(devList);			
			for(ComputingNode cn : container.getEdgeDevices())
				cn.setApplicationType(app);
			container.setTime(time);
			time += SimulationParameters.simulationDuration/60;
			container.setDuration(time);

			//aggiungo tanti container quante sono le copie richieste
			containerList.add(container);

			getSimulationManager().getSimulationLogger().deepLog("BasicContainersGenerator, Container " + id + " with placement time " + time + " (s) generated.");

			if(debugContainer) System.out.println("Ho generato la "+(i+1)+" richiesta di placement al tempo: " + container.getTime()  + " dell'applicazione " + SimulationParameters.applicationList.get(app).name);	
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
			return containerClass.getConstructor(int.class).newInstance(id);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

}
