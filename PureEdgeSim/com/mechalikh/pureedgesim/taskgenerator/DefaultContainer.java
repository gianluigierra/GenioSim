package com.mechalikh.pureedgesim.taskgenerator;

import java.util.List;
import java.util.ArrayList;

import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode;

public class DefaultContainer extends ContainerAbstract{

   /**
    * The associated application name
    */
   protected String AssociatedAppName;
   
   /**
    * The time required for the placement of the Container in seconds.
    */
   protected double placementTime;
   
   /**
    * The the container will be placed for in seconds.
    */
   protected double placementDuration;

   /**
    * The edgeDevices where the Container will be offloaded.
    */
   protected List<ComputingNode> edgeDevices = new ArrayList<ComputingNode>();

   /**
    * The VM where the Container will be offloaded to.
    */
   protected ComputingNode computingNode = ComputingNode.NULL;

   /**
    * The size of the container where the Container is encapsulated in bits.
    * Questa variabile viene usata prima per effettuare il placement nell VM: availableStorage -= containerSize;
    * Poi viene usata per effettuarne l'esecuzione nella VM: availableRam -= containerSize; 
    */
   protected long containerSize;

   /**
    * The ID of the application that this Container belongs to.
    */
   protected int applicationID;

   /**
	* The reason of failure for the task, if any.
	*/
   protected FailureReason failureReason;

   /**
    * The status of the Container.
    */
   protected Status status = Status.NOT_PLACED;

   /**
	* The size of the input file for the task in bits.
	*/
   protected long fileSize;

	/**
	 * Wether the container must be shared or not
	 */
	protected boolean sharedContainer;

   /**
	* The orchestrator node that will manage the placement of this Container.
	*/
   protected ComputingNode orchestrator = ComputingNode.NULL;

   /**
    * Constructs a DefaultContainer object with a specified Container ID.
    *
    * @param id The ID of the Container.
    */
    public DefaultContainer(int id) {
        super(id);
    }

	/**
    * Constructs a DefaultContainer object with a Container
    *
    * @param container The container to copy.
    */
    public DefaultContainer(Container container) {
        super(container.getId());
		this.addEdgeDevice(container.getEdgeDevice(container.getEdgeDevices().size()-1));
		this.setApplicationID(container.getApplicationID());
		this.setAssociatedAppName(container.getAssociatedAppName());
		this.setContainerSizeInBits(container.getContainerSizeInBits());					 
		this.setFileSizeInBits(container.getFileSizeInBits());
		this.setOrchestrator(container.getOrchestrator());
		this.setPlacementDestination(container.getPlacementDestination());
		this.setSerial(container.getSerial());
		this.setSharedContainer(container.getSharedContainer());
		this.setStatus(container.getStatus());
		this.setTime(container.getTime());
		this.setDuration(container.getDuration());
    }

	/**
	 * Returns the associated application name .
	 *
	 * @return the associated application name 
	 */
    @Override
    public String getAssociatedAppName() {
		return AssociatedAppName;
    }

	/**
	 * Sets the associated application name
	 *
	 * @param the associated application name
	 */
    @Override
    public Container setAssociatedAppName(String name) {
		this.AssociatedAppName = name;
		return this;
    }
	
	/**
	 * Sets the offloading time for the Container.
	 *
	 * @param time The offloading time in seconds.
	 */
	@Override
	public void setTime(double time) {
		this.placementTime = time;
	}
	
	/**
	 * Sets the duration of placement for the Container.
	 *
	 * @param duration The duration time in seconds.
	 */
	@Override
	public void setDuration(double duration) {
		this.placementDuration = duration;
	}

	/**
	 * Returns the offloading time for the Container.
	 *
	 * @return The offloading time in seconds.
	 */
	@Override
	public double getTime() {
		return placementTime;
	}

	/**
	 * Returns the duration time for the Container.
	 *
	 * @return The offloading time in seconds.
	 */
	@Override
	public double getDuration() {
		return placementDuration;
	}

	/**
	 * Returns the edge devices assigned to the Container.
	 *
	 * @return The edge devices assigned to the Container.
	 */
    @Override
    public List<ComputingNode> getEdgeDevices() {
        return this.edgeDevices;
    }

	/**
	 * Returns the edge device specified by an index assigned to the Container.
	 *
	 * @return The edge device assigned to the Container.
	 */
    @Override
    public ComputingNode getEdgeDevice(int index) {
        return this.edgeDevices.get(index);
    }

	/**
	 * Assigns an edge device to the Container.
	 *
	 * @param device The edge device to be assigned to the Container.
	 */
    @Override
    public Container addEdgeDevice(ComputingNode device) {
        edgeDevices.add(device);
        return this;
    }

	/**
	 * Sets the container size for the Container in bits.
	 *
	 * @param containerSize The container Container in bits.
	 */
    @Override
    public Container setContainerSizeInBits(long containerSize) {
		this.containerSize = containerSize;
		return this;
    }

	/**
	 * Returns the container size for the Container in bits.
	 *
	 * @return The container size in bits.
	 */
    @Override
    public long getContainerSizeInBits() {
		return containerSize;
    }

	/**
	 * Returns the container size for the Container in megabytes.
	 *
	 * @return The container size in megabytes.
	 */
    @Override
    public double getContainerSizeInMBytes() {
		return containerSize / 8000000.0;
    }

	/**
	 * Returns the orchestrator for the Container. (Always the cloud)
	 *
	 * @return The orchestrator for the Container.
	 */
	@Override
	public ComputingNode getOrchestrator() {
		return this.orchestrator;
	}

	/**
	 * Assigns an orchestrator to the Container.
	 *
	 * @param orchestrator The orchestrator to be assigned to the task.
	 */

	@Override
	public void setOrchestrator(ComputingNode orchestrator) {
		this.orchestrator = orchestrator;
	}

	/**
	 * 
	 * Returns the ID of the application.
	 * 
	 * @return the ID of the application.
	 */
    @Override
    public int getApplicationID() {
		return applicationID;
    }

	/**
	 * 
	 * Sets the ID of the application.
	 * 
	 * @param applicationID the ID of the application.
	 */
    @Override
    public Container setApplicationID(int applicationID) {
		this.applicationID = applicationID;
		return this;
    }

	/**
	 * 
	 * Returns the failure reason of the Container.
	 * 
	 * @return the failure reason of the Container.
	 */
	@Override
	public FailureReason getFailureReason() {
		return failureReason;
	}

	/**
	 * 
	 * Sets the failure reason of the task and updates its status to "FAILED".
	 * 
	 * @param reason the failure reason of the task.
	 */
	@Override
	public void setFailureReason(FailureReason reason) {
		this.setStatus(Container.Status.NOT_PLACED);
		this.failureReason = reason;
	}

	/**
	 * 
	 * Returns the offloading destination computing node.
	 * 
	 * @return the offloading destination computing node.
	 */
	@Override
	public ComputingNode getPlacementDestination() {
		return computingNode;
	}

	/**
	 * 
	 * Sets the offloading destination computing node.
	 * 
	 * @param applicationPlacementLocation the offloading destination computing
	 *                                     node.
	 */
	@Override
	public void setPlacementDestination(ComputingNode applicationPlacementLocation) {
		this.computingNode = applicationPlacementLocation;
	}

	/**
	 * 
	 * Sets the size of the file for the Container and returns this Container.
	 * 
	 * @param requestSize the size of the file for the Container.
	 * @return this Container.
	 */
	@Override
	public Container setFileSizeInBits(long requestSize) {
		this.fileSize = requestSize;
		return this;
	}

	/**
	 * 
	 * Gets the size of the file for the Container and returns this Container.
	 * 
	 * @param requestSize the size of the file for the Container.
	 * @return this Container.
	 */
	@Override
	public long getFileSizeInBits() {
		return this.fileSize;
	}

	/**
	 * 
	 * Sets wether the container must be shared among edge devices.
	 * 
	 * @param sharedContainer.
	 * @return this Container.
	 */
	@Override
	public Container setSharedContainer(boolean sharedContainer) {
		this.sharedContainer = sharedContainer;
		return this;
	}

	/**
	 * 
	 * Gets wether the container must be shared among edge devices.
	 * 
	 * @param requestSize.
	 * @return this Container.
	 */
	@Override
	public boolean getSharedContainer() {
		return this.sharedContainer;
	}

	/**
	 * 
	 * Sets the status of the Container.
	 * 
	 * @param status the status of the Container.
	 */
	@Override
	public void setStatus(Status status) {
		this.status = status;
	}

	/**
	 * 
	 * Returns the status of the Container.
	 * 
	 * @return the status of the Container.
	 */
	@Override
	public Status getStatus() {
		return status;
	}

}
