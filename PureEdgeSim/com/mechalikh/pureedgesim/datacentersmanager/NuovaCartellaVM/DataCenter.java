package com.mechalikh.pureedgesim.datacentersmanager.NuovaCartellaVM;

import java.util.ArrayList;
import java.util.List;

import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.simulationengine.Event;
import com.mechalikh.pureedgesim.simulationmanager.SimulationManager;
import com.mechalikh.pureedgesim.taskgenerator.Container;
import com.mechalikh.pureedgesim.taskgenerator.Task;
import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode;
import com.mechalikh.pureedgesim.datacentersmanager.LocationAwareNode;
import com.mechalikh.pureedgesim.energy.EnergyModelComputingNode;
import com.mechalikh.pureedgesim.locationmanager.MobilityModel;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class DataCenter extends LocationAwareNode {

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
	private int sentTasks = 0;

	protected List<Host> HostList = new ArrayList<>();

	public DataCenter(SimulationManager simulationManager, Element datacenterElement) {
		super(simulationManager);
		createHosts(datacenterElement, simulationManager);
		setDataCenterInfo();
	}

	private void createHosts(Element datacenterElement, SimulationManager simulationManager) {

		NodeList hostNodeList = datacenterElement.getElementsByTagName("host");
		for (int j = 0; j < hostNodeList.getLength(); j++) {

			Node hostNode = hostNodeList.item(j);
			Element hostElement = (Element) hostNode;
			int numOfCores = Integer.parseInt(hostElement.getElementsByTagName("cores").item(0).getTextContent());
			double mips = Double.parseDouble(hostElement.getElementsByTagName("mips").item(0).getTextContent());
			double storage = Double.parseDouble(hostElement.getElementsByTagName("storage").item(0).getTextContent());
			double ram = Integer.parseInt(hostElement.getElementsByTagName("ram").item(0).getTextContent());

			// Create Hosts
			Host host = new Host(simulationManager, mips, numOfCores, storage, ram, this, hostElement);
			HostList.add(host);

		}

	}
	
	@Override 
	public void setEnergyModel(EnergyModelComputingNode emcn){
		this.energyModel = emcn;
		for(Host host : this.HostList){
			host.setEnergyModel(new EnergyModelComputingNode(0, 0));
		}
	}

	@Override 
	public void setAsOrchestrator(boolean isOrchestrator){
		this.isOrchestrator = isOrchestrator;
		for(Host host : this.HostList){
			host.setAsOrchestrator(false);
		}
	}

	@Override 
	public void setAsEdgeOrchestrator(boolean isOrchestrator){
		this.isEdgeOrchestrator = isOrchestrator;
		for(Host host : this.HostList){
			host.setAsEdgeOrchestrator(false);
		}
	}

	@Override 
	public void setAsCloudOrchestrator(boolean isOrchestrator){
		this.isCloudOrchestrator = isOrchestrator;
		for(Host host : this.HostList){
			host.setAsCloudOrchestrator(false);
		}
	}

	@Override 
	public void setMobilityModel(MobilityModel mobilityModel){
		this.mobilityModel = mobilityModel;
		for(Host host : this.HostList){
			host.setMobilityModel(mobilityModel);
		}
	}

	@Override
	public void setName(String name){
		this.name = name;
		for(Host host : this.HostList){
			if(this.getType() == SimulationParameters.TYPES.EDGE_DATACENTER) host.setName("Host Edge " + host.getId());
			else host.setName("Host Cloud " + host.getId());
		}
	}

	@Override
	public void setType(SimulationParameters.TYPES type){
		this.nodeType = type;
		for(Host host : this.HostList){
			if(this.getType() == SimulationParameters.TYPES.EDGE_DATACENTER) host.setType(SimulationParameters.TYPES.HOST_EDGE);
			else host.setType(SimulationParameters.TYPES.HOST_CLOUD);
		}
	}

	private void setDataCenterInfo(){
		double storage = 0, mipsPerCore = 0, ram = 0;
		int numberOfCPUCores = 0; 
		for(Host host : HostList){
			storage += host.storage;
			mipsPerCore += host.mipsPerCore;
			numberOfCPUCores += host.numberOfCPUCores;
			ram += host.ram;
		}
		setStorage(storage);
		setAvailableStorage(storage);
		this.mipsPerCore = mipsPerCore/HostList.size();
		setTotalMipsCapacity(this.mipsPerCore * numberOfCPUCores);
		setRam(ram);
		setAvailableRam(ram);
		setNumberOfCPUCores(numberOfCPUCores);
		this.availableCores = numberOfCPUCores;
	}

	public List<Host> getHostList(){
		return HostList;
	}

	@Override
	public void processEvent(Event e) {
		super.processEvent(e);
		if (e.getTag() == EXECUTION_FINISHED)
			executionFinished(e);
	}

	public void increaseTask(Task task){
		sentTasks++;
	}

	public void incrementTasksFailed(){
		this.tasksFailed++;
		this.failureRate = ((double) tasksFailed * 100) / Math.max(1, sentTasks);
	}

	public double getSentTasks(){
		return sentTasks;
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
		// Update the amount of available storage
		//this.setAvailableStorage(this.availableStorage - task.getContainerSizeInMBytes());
	}

	@Override
	public void submitContainerPlacement(Container container) {
		// Aggiungo il container alla lista
		containerList.add(container);
		// Update the amount of available storage
		this.setAvailableStorage(this.availableStorage - container.getContainerSizeInMBytes());
	}
	
	@Override
	public List<Container> getContainerList(){
		List<Container> lista = new ArrayList<Container>();
		return lista;
	}

	double getAssociatedContainerSizeInMBytes(Task task){
		for(Container container : containerList)
			for(ComputingNode computingnode : container.getEdgeDevices())
				if(task.getEdgeDevice().equals(computingnode))
					return container.getContainerSizeInMBytes();
		return 0.0;
	}

	protected void startExecution(Task task) {
		// Update the CPU utilization.
		addCpuUtilization(task);
		// Update the amount of RAM.
		setAvailableRam(this.getAvailableRam() - getAssociatedContainerSizeInMBytes(task));
		// Update the number of available cores.
		availableCores--;

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
	}

	public double getMipsPerCore() {
		return mipsPerCore;
	}

	protected void executionFinished(Event e) {

		// The execution of one task has been finished, free one more CPU core.
		availableCores++;
		// Free the RAM that has been used by the finished task.
		setAvailableRam(this.getAvailableRam() + getAssociatedContainerSizeInMBytes((Task) e.getData()));
		// Free the storage that has been used by the finished task.
		//setAvailableStorage(this.getAvailableStorage() + ((Task) e.getData()).getContainerSizeInMBytes());
		// Update CPU utilization.
		removeCpuUtilization((Task) e.getData());

	}

}
