package com.mechalikh.pureedgesim.taskgenerator;

import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode;
import com.mechalikh.pureedgesim.simulationengine.QueueElement;

import java.util.List;

public interface Container extends QueueElement {

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
      * Gets the time of the Container.
      * 
      * @return the time of the task
      */
     double getTime();

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
