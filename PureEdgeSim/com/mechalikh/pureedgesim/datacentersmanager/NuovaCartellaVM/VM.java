package com.mechalikh.pureedgesim.datacentersmanager.NuovaCartellaVM;

import java.util.ArrayList;
import java.util.List;

import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.simulationengine.Event;
import com.mechalikh.pureedgesim.simulationmanager.SimulationManager;
import com.mechalikh.pureedgesim.taskgenerator.Container;
import com.mechalikh.pureedgesim.taskgenerator.DefaultContainer;
import com.mechalikh.pureedgesim.taskgenerator.Task;
import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode;
import com.mechalikh.pureedgesim.datacentersmanager.LocationAwareNode;

public class VM extends LocationAwareNode {
	protected int applicationType;
	protected boolean isSensor = false;
	protected double availableStorage = 0; // in Megabytes
	protected double storage = 0; // in Megabytes
	protected boolean isIdle = true;
	protected int tasks = 0;
	protected int totalTasks = 0;
	protected double totalMipsCapacity;
	protected double mipsPerCore;
	protected int numberOfCPUCores;
	protected int availableCores;
	protected List<Task> tasksQueue = new ArrayList<>();
	protected List<Container> containerList = new ArrayList<>();
	protected double availableRam; // in Megabytes
	protected double ram; // in Megabytes
	protected static final int EXECUTION_FINISHED = 2;
	private int tasksFailed = 0;
	private double failureRate = 0;
	public int sentTasks = 0;
	private static boolean sharedQueue = false;	//variabile booleana per fare condividere la queue dei task tra tutte le VM del dato datacenter
	protected Host Host;	//the host of this VM

	public VM(SimulationManager simulationManager, double mipsPerCore, int numberOfCPUCores,
			double storage, double ram, Host host) {
		super(simulationManager);
		setStorage(storage);
		setAvailableStorage(storage);
		setTotalMipsCapacity(mipsPerCore * numberOfCPUCores);
		this.mipsPerCore = mipsPerCore;
		setRam(ram);
		setAvailableRam(ram);
		setNumberOfCPUCores(numberOfCPUCores);
		this.availableCores = numberOfCPUCores;
		this.Host = host;

		if(sharedQueue) tasksQueue = this.getHost().getDataCenter().getTasksQueue();
	}

	public Host getHost(){
		return this.Host;
	}

	@Override
	public void processEvent(Event e) {
		super.processEvent(e);
		if (e.getTag() == EXECUTION_FINISHED){
			this.Host.processEvent(e);
			executionFinished(e);
		}
	}

	public void increaseTask(Task task){
		this.sentTasks++;
	}

	public double getSentTasks(){
		return sentTasks;
	}

	public void incrementTasksFailed(){
		this.tasksFailed++;
		this.failureRate = ((double) tasksFailed * 100) / Math.max(1, sentTasks);
		this.Host.incrementTasksFailed();
	}

	public double getFailureRate(){
		return failureRate;
	}

	public int getAvailableCores(){
		return availableCores;
	}

	public double getNumberOfCPUCores() {
		return numberOfCPUCores;
	}

	public void setNumberOfCPUCores(int numberOfCPUCores) {
		this.numberOfCPUCores = numberOfCPUCores;
	}

	public int getApplicationType() {
		return applicationType;
	}

	public void setApplicationType(int applicationType) {
		this.applicationType = applicationType;
	}

	public double getAvailableStorage() {
		return availableStorage;
	}

	public void setAvailableStorage(double d) {
		this.availableStorage = d;
	}

	public double getAvgCpuUtilization() {
		if (this.getTotalMipsCapacity() == 0)
			return 0;
		Double utilization = (totalTasks * 100.0)
				/ (getTotalMipsCapacity() * simulationManager.getSimulation().clock());

		return Math.min(100, utilization);
	}

	public double getCurrentCpuUtilization() {
		if (this.getTotalMipsCapacity() == 0)
			return 0;
		Double utilization = tasks * 100.0 / getTotalMipsCapacity();
		return utilization > 100 ? 100 : utilization;
	}

	public boolean isIdle() {
		return isIdle;
	}

	public void setIdle(boolean isIdle) {
		this.isIdle = isIdle;
	}

	public void addCpuUtilization(Task task) {
		tasks += task.getLength();
		totalTasks += task.getLength();
		setIdle(false);
	}

	public void removeCpuUtilization(Task task) {
		tasks -= task.getLength();
		if (tasks <= 0)
			setIdle(true);
	}

	public boolean isSensor() {
		return isSensor;
	}

	public void setAsSensor(boolean isSensor) {
		this.isSensor = isSensor;
	}

	public List<Task> getTasksQueue() {
		return tasksQueue;
	}

	public double getTotalStorage() {
		return storage;
	}

	public void setStorage(double storage) {
		this.storage = storage;
	}

	public double getTotalMipsCapacity() {
		return totalMipsCapacity;
	}

	public void setTotalMipsCapacity(double totalMipsCapacity) {
		this.totalMipsCapacity = totalMipsCapacity;
	}

	/**
	 * Gets the total amount of RAM (in Megabytes) that this computing node has.
	 * 
	 * @return the total amount of RAM.
	 * 
	 * @see #getAvailableRam()
	 * @see #setAvailableRam(long)
	 * @see #setRam(long)
	 */
	public double getRamCapacity() {
		return this.ram;
	}

	/**
	 * Gets the amount of RAM (in Megabytes) that is available on this computing
	 * node.
	 * 
	 * @return the amount of available RAM.
	 * 
	 * @see #getRamCapacity()
	 * @see #setAvailableRam(long)
	 * @see #setRam(long)
	 */
	public double getAvailableRam() {
		return this.availableRam;
	}

	/**
	 * Sets the total amount of RAM (in Megabytes) that this computing node has.
	 * 
	 * @param ram the total RAM on this computing node.
	 * 
	 * @see #getAvailableRam()
	 * @see #setAvailableRam(long)
	 * @see #getRamCapacity()
	 */
	public void setRam(double ram) {
		this.ram = ram;
	}

	/**
	 * Sets the amount of RAM (in Megabytes) that is available on this computing
	 * node.
	 * 
	 * @param ram the available RAM.
	 * 
	 * @see #getAvailableRam()
	 * @see #getRamCapacity()
	 * @see #setRam(long)
	 */
	public void setAvailableRam(double ram) {
		this.availableRam = ram;
	}

	@Override
	public void submitTask(Task task) {

		//incremento i sentTasks per calcolare il failureRate
		this.sentTasks++;

		// The task to be executed has been received, save the arrival time
		task.setArrivalTime(getSimulation().clock());

		// Update the amount of available storage
		//this.setAvailableStorage(this.availableStorage - task.getContainerSizeInMBytes());

		this.Host.submitTask(task);

		// If a CPU core and enough RAM are available, execute task directly
		if (availableCores > 0 && this.getAvailableRam() >= getAssociatedContainerSizeInMBytes(task)) {
			startExecution(task);
		}
		// Otherwise, add it to the execution queue
		else
			getTasksQueue().add(task);
	}

	private boolean isContainerApplicationInList(Container container){
		for(Container cont : containerList)
			if(container.getAssociatedAppName().equals(cont.getAssociatedAppName()))
				return true;
		return false;
	}

	@Override
	public void submitContainerPlacement(Container container) {

		container.setStatus(Container.Status.PLACED);

		//se il container è del tipo shared ed è già stato piazzato allora aggiungo l'edge device al container già piazzato
		if(container.getSharedContainer() && isContainerApplicationInList(container)){
			for(Container cont : containerList)
				if(cont.getAssociatedAppName().equals(container.getAssociatedAppName())){
					cont.addEdgeDevice(container.getEdgeDevice(container.getEdgeDevices().size()-1));
					//associo il container precedente a quello già piazzato
					container = cont;
				}
		}
		//altrimenti
		else{
			// Aggiungo il container alla lista
			containerList.add(container);
			// Update the amount of available storage
			this.setAvailableStorage(this.availableStorage - container.getContainerSizeInMBytes());
			this.Host.submitContainerPlacement(container);
		}

		Container container_provvisorio = new DefaultContainer(container);
							 
		scheduleNow(simulationManager, SimulationManager.TRANSFER_RESULTS_TO_CLOUD_ORCH, container_provvisorio);

		//Alternativa a quanto fatto sotto. Decommentare le cose nel DefaultSimulationManager
		//scheduleNow(simulationManager, SimulationManager.TRANSFER_RESULTS_TO_CLOUD_ORCH, container);
	}
	
	@Override
	public List<Container> getContainerList(){
		return this.containerList;
	}

	protected void startExecution(Task task) {

		// Update the CPU utilization.
		addCpuUtilization(task);
		// Update the amount of RAM.
		//setAvailableRam(this.getAvailableRam() - task.getContainerSizeInMBytes());
		setAvailableRam(this.getAvailableRam() - getAssociatedContainerSizeInMBytes(task));
		// Update the number of available cores.
		availableCores--;

		this.Host.startExecution(task);

		// Record when the execution has started.
		task.setExecutionStartTime(getSimulation().clock());

		/*
		 * Arguably, the correct way to get energy consumption measurement is to place
		 * the following line of code within the processEvent(Event e) method of the
		 * EnergyAwareComputingNode:
		 * 
		 * getEnergyModel().updateCpuEnergyConsumption(getCurrentCpuUtilization());
		 * 
		 * I mean, this makes sense right?. The problem with this is that it will depend
		 * on the update interval. To get more accurate results, you need to set the
		 * update interval as low as possible, this will in turn increase the simulation
		 * duration, which is clearly not convenient. One way around it, is to make the
		 * measurement here, when the task is being executed. The problem with this is
		 * that if we don't receive a task, the static energy consumption will not be
		 * measured. So the best approach is to measure the dynamic one here, and add
		 * the static one there.
		 */
		getEnergyModel().updateDynamicEnergyConsumption(task.getLength(), this.getTotalMipsCapacity());

		// Schedule when the execution will be finished.
		schedule(this, (task.getLength() / mipsPerCore), EXECUTION_FINISHED, task);
	}

	public double getMipsPerCore() {
		return mipsPerCore;
	}

	double getAssociatedContainerSizeInMBytes(Task task){
		//per ogni container nella lista
		for(Container container : containerList)
			//controlla tra i suoi edgeDevice
			for(ComputingNode computingnode : container.getEdgeDevices())
				//se vi è uno che corrisponde al generatore del task
				if(task.getEdgeDevice().equals(computingnode))
					return container.getContainerSizeInMBytes();

		return 0.0;
	}

	protected void executionFinished(Event e) {

		// The execution of one task has been finished, free one more CPU core.
		availableCores++;
		// Free the RAM that has been used by the finished task.
		//setAvailableRam(this.getAvailableRam() + ((Task) e.getData()).getContainerSizeInMBytes());
		setAvailableRam(this.getAvailableRam() + getAssociatedContainerSizeInMBytes((Task) e.getData()));
		// Free the storage that has been used by the finished task.
		//setAvailableStorage(this.getAvailableStorage() + ((Task) e.getData()).getContainerSizeInMBytes());
		// Update CPU utilization.
		removeCpuUtilization((Task) e.getData());

		// Save the execution end time for later use.
		((Task) e.getData()).setExecutionFinishTime(this.getSimulation().clock());

		// Notify the simulation manager that a task has been finished, and it's time to
		// return the execution results.
		scheduleNow(simulationManager, SimulationManager.TRANSFER_RESULTS_TO_EDGE_ORCH, e.getData());

		// If there are tasks waiting for execution
		if (!getTasksQueue().isEmpty()) {

			// Execute the first task in the queue on the available core.
			Task task = getTasksQueue().get(0);

			// Remove the task from the queue.
			getTasksQueue().remove(0);

			// Execute the task.
			startExecution(task);
		}

	}

}
