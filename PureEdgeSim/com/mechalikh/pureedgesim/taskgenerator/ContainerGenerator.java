package com.mechalikh.pureedgesim.taskgenerator;

import java.util.ArrayList;
import java.util.List;

import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode;
import com.mechalikh.pureedgesim.simulationengine.FutureQueue;
import com.mechalikh.pureedgesim.simulationmanager.SimulationManager;

public abstract class ContainerGenerator {
	/**
	 * The Container class that is used in the simulation.
	 * 
	 * @see #setCustomContainerClass(Class)
	 */

	protected Class<? extends Container> containerClass = DefaultContainer.class;

	/**
	 * The ordered list of placement requests.
	 * 
	 * @see #generate()
	 */
	protected FutureQueue<Container> containerList;

	/**
	 * The list of edge devices.
	 * 
	 * @see #generate()
	 */
	protected List<ComputingNode> devicesList = new ArrayList<>();

	/**
	 * The simulation manager.
	 * 
	 */
	protected SimulationManager simulationManager;

	/**
	 * Creates a Container generator.
	 */
	public ContainerGenerator(SimulationManager simulationManager) {
		containerList = new FutureQueue<>();
		setSimulationManager(simulationManager);
		devicesList
				.addAll(getSimulationManager().getDataCentersManager().getComputingNodesGenerator().getMistOnlyList());
	}

	/**
	 * Gets the list of all placement requests.
	 * 
	 * @return list of all generated placement requests.
	 */
	public FutureQueue<Container> getContainerList() {
		return containerList;
	}

	/**
	 * Gets the simulation manager.
	 * 
	 * @return simulation manager.
	 */
	public SimulationManager getSimulationManager() {
		return simulationManager;
	}

	/**
	 * Sets the simulation manager.
	 * 
	 * @param simulationManager the simulation manager.
	 */
	public void setSimulationManager(SimulationManager simulationManager) {
		this.simulationManager = simulationManager;
	}

	/**
	 * Generates the Container that will be placed during the simulation.
	 * 
	 */
	public abstract FutureQueue<Container> generate();

	/**
	 * Allows to use a custom Container class in the simulation. The class must extend
	 * the {@link Container} provided by PureEdgeSim.
	 * 
	 * @param container the custom container class to use.
	 */
	public void setCustomContainerClass(Class<? extends Container> container) {
		this.containerClass = container;
	}
}
