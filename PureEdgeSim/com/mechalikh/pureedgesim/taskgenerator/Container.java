package com.mechalikh.pureedgesim.taskgenerator;

import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode;
import com.mechalikh.pureedgesim.simulationengine.QueueElement;

import java.util.List;

public interface Container extends QueueElement {

	/**
	 * Enumeration for failure reasons of a Task.
	 */
	enum FailureReason {
		NO_PLACEMENT_DESTINATIONS, INSUFFICIENT_RESOURCES
	}

    /**
	 * Enumeration for status of a Container.
	 */
	enum Status {
		PLACED, NOT_PLACED
	}

    /**
	 * Returns the associated application name
	 * 
	 * @return the associated application name
	 */
	String getAssociatedAppName();

	/**
	 * Sets the associated application name
	 * 
	 * @param the associated application name
	 * @return the updated Container
	 */
	 Container setAssociatedAppName(String name);

     /**
      * 
      * Sets the time of the Container.
      * 
      * @param time the time to set
      */
     void setTime(double time);

     /**
      * 
      * Sets the duration of the Container.
      * 
      * @param time the time to set
      */
     void setDuration(double duration);
 
     /**
      * 
      * Gets the time of the Container.
      * 
      * @return the time of the task
      */
     double getTime();

     /**
      * 
      * Gets the duration of the Container.
      * 
      * @param time the time to set
      */
     double getDuration();

    /**
	 * Sets the ID of the Container.
	 * 
	 * @param id the ID of the Container
	 */
	void setId(int id);

	/**
	 * Returns the ID of the Container.
	 * 
	 * @return the ID of the Container
	 */
	int getId();

    /**
	 * 
	 * Gets the edge devices associated with the Container.
	 * 
	 * @return the edge devices associated with the Container
	 */
	List<ComputingNode> getEdgeDevices();

    /**
	 * 
	 * Gets the edge device associated with the Container.
	 * 
	 * @return the edge device associated with the Container
	 */
	ComputingNode getEdgeDevice(int index);

	/**
	 * 
	 * Sets the edge device associated with the Container.
	 * 
	 * @param device the edge device to set
	 * @return the updated Container object
	 */
	Container addEdgeDevice(ComputingNode device);

    /**
	 * 
	 * Sets the container size of the Container in bits.
	 * 
	 * @param containerSize the container size to set in bits
	 * @return the updated Container object
	 */
	Container setContainerSizeInBits(long containerSize);	
    
    /**
     * 
     * Gets the container size of the Container in bits.
     * 
     * @return the container size of the Container in bits
     */
    long getContainerSizeInBits();	
   
    /**
     * 
     * Gets the container size of the Container in megabytes.
     * 
     * @return the container size of the Container in megabytes
     */
    double getContainerSizeInMBytes();

	/**
	 * 
	 * Gets the offloading destination associated with the Container.
	 * 
	 * @return the offloading destination associated with the Container
	 */
	ComputingNode getPlacementDestination();

	/**
	 * 
	 * Sets the offloading destination associated with the Container.
	 * 
	 * @param applicationPlacementLocation the offloading destination to set
	 */
	void setPlacementDestination(ComputingNode applicationPlacementLocation);

	/**
	 * 
	 * Sets the file size of the Container request in bits.
	 * 
	 * @param requestSize the file size of the task request to set in bits
	 * @return the updated Task object
	 */
	Container setFileSizeInBits(long requestSize);

	/**
	 * 
	 * Gets the file size of the Container request in bits.
	 * 
	 * @param requestSize the file size of the task request to set in bits
	 * @return the updated Task object
	 */
	long getFileSizeInBits();

	/**
	 * 
	 * Sets wether the container must be shared among edge devices.
	 * 
	 * @param sharedContainer.
	 * @return this Container.
	 */
	Container setSharedContainer(boolean sharedContainer);

	/**
	 * 
	 * Gets wether the container must be shared among edge devices.
	 * 
	 * @param requestSize.
	 * @return this Container.
	 */
	public boolean getSharedContainer();

	/**
	 * 
	 * Sets the status of the Container.
	 * 
	 * @param status the status of the Container
	 */
	void setStatus(Status status);

	/**
	 * 
	 * Gets the status of the task.
	 * 
	 * @return the status of the task
	 */
	Status getStatus();

	/**
	 * 
	 * Sets the orchestrator node of the Container.
	 * 
	 * @param orchestrator the orchestrator node of the Container
	 */
	void setOrchestrator(ComputingNode orchestrator);

	/**
	 * 
	 * Gets the orchestrator associated with the Container.
	 * 
	 * @return the orchestrator associated with the Container
	 */
	ComputingNode getOrchestrator();

  	/**
	 * 
	 * Gets the ID of the application associated with the Container.
	 * 
	 * @return the ID of the application associated with the Container
	 */
	int getApplicationID();

	/**
	 * 
	 * Sets the ID of the application associated with the Container.
	 * 
	 * @param applicationID the ID of the application to set
	 * @return the updated Container object
	 */
	Container setApplicationID(int applicationID);
	
	/**
	* 
	* Gets the reason for task failure.
	* 
	* @return the reason for task failure
	*/
   FailureReason getFailureReason();

   /**
	* 
	* Sets the reason for task failure.
	* 
	* @param reason the reason for task failure to set
	*/
   void setFailureReason(FailureReason reason);

    /**
	 * 
	 * Sets the serial number of the Container.
	 * 
	 * @param l the serial number of the Container
	 */
	void setSerial(long l);

	/**
	 * 
	 * Gets the serial number of the Container.
	 * 
	 * @return the serial number of the Container
	 */
	long getSerial();
    
}
