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
 *     @since PureEdgeSim 5.0
 **/
package com.mechalikh.pureedgesim.taskgenerator;

import java.util.List;
import java.util.ArrayList;

/**
 * This class represents an application that can be executed on a computing
 * node.
 * 
 * @author Charafeddine Mechalikh
 **/

public class Application {

	/**
	 * The name of this application
	 */
	protected String name;
	
	/**
	 * The users associated with this App
	 */
	protected List<User> Users = new ArrayList<>();

	/**
	 * The latency of the application, in seconds
	 */
	protected double latency;

	/**
	 * The size of the container that this application runs in, in bits
	 */
	protected long containerSize;

	/**
	 * whether the container that this application runs in can be shared across multiple devices
	 */
	protected boolean sharedContainer;

	/**
	 * if the container is shared then we specify how many copies of it we want
	 */
	protected int copies;

	/**
	 * The size of the request of the container that this application runs in, in bits
	 */
	protected long containerRequestSize;

	/**
	 * The min size of the request that is sent to the application, in bits
	 */
	protected long requestSizeMinValue;

	/**
	 * The max size of the request that is sent to the application, in bits
	 */
	protected long requestSizeMaxValue;

	/**
	 * The size of the results that the application returns, in bits
	 */
	protected long resultsSize;

	/**
	 * The length of time it takes for the application to execute, in MI
	 * (Mega-Instructions)
	 */
	protected double taskLength;

	/**
	 * The type of application
	 */
	protected String type;

	/**
	 * The number of bits in one megabyte.
	 */
	private static final double BITS_IN_MB = 8000000.0;

	/**
	 * Constructs a new Application object.
	 *
	 * @param type            the type of the application
	 * @param rate            the rate at which requests are generated for this
	 *                        application
	 * @param latency         the latency of the application, in seconds
	 * @param containerSize   the size of the container that this application runs
	 *                        in, in bits
	 * @param requestSize     the size of the request that is sent to the
	 *                        application, in bits
	 * @param resultsSize     the size of the results that the application returns,
	 *                        in bits
	 * @param taskLength      the length of time it takes for the application to
	 *                        execute, in MI (Mega-Instructions)
	 */
	public Application(String name, String type, double latency, long containerSize, long containerRequestSize, boolean shared, int copies,
			long minRequestSize, long maxRequestSize, long resultsSize, double taskLength) {
		setName(name);
		setType(type);
		setLatency(latency);
		setContainerSize(containerSize);
		setContainerRequestSize(containerRequestSize);
		setSharedContainer(shared);
		setContainerCopies(copies);
		setMinRequestSize(minRequestSize);
		setMaxRequestSize(maxRequestSize);
		setResultsSize(resultsSize);
		setTaskLength(taskLength);
	}

	/**
	 * Gets the name of this application.
	 *
	 * @return the name of this application
	 */
	public String getName() {
		return name;
	}

	/**
	 * Sets the name of this application.
	 *
	 * @param rate the name of this application
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Adds a User to this application
	 *
	 * @param user the user which makes requests of this application
	 */
	public void addUser(User user) {
		this.Users.add(user);
	}

	/**
	 * Adds a User to this application
	 *
	 * @param user the user which makes requests of this application
	 */
	public List<User> getUsersList() {
		return this.Users;
	}

	/**
	 * 
	 * Returns the size of the container in bits.
	 * 
	 * @return the size of the container in bits
	 */
	public long getContainerSizeInBits() {
		return containerSize;
	}

	/**
	 * 
	 * Sets the size of the container in bits.
	 * 
	 * @param containerSize the size of the container in bits
	 */
	public void setContainerSize(long containerSize) {
		this.containerSize = containerSize;
	}

	/**
	 * 
	 * Returns the size of the request in bits.
	 * 
	 * @return the size of the request in bits
	 */
	public long getContainerRequestSize() {
		return containerRequestSize;
	}

	/**
	 * 
	 * Sets the size of the request in bits.
	 * 
	 * @param containerRequestSize the size of the request in bits
	 */
	public void setContainerRequestSize(long containerRequestSize) {
		this.containerRequestSize = containerRequestSize;
	}

	/**
	 * 
	 * Returns how many copies of the container we want.
	 * 
	 * @return the boolean
	 */
	public int getContainerCopies() {
		return copies;
	}

	/**
	 * 
	 * Sets how many copies of the container we want.
	 * 
	 * @param sharedContainer the boolean
	 */
	public void setContainerCopies(int copies) {
		this.copies = copies;
	}

	/**
	 * 
	 * Returns wether the container can be shared or not.
	 * 
	 * @return the boolean
	 */
	public boolean getSharedContainer() {
		return sharedContainer;
	}

	/**
	 * 
	 * Sets wether the container can be shared or not.
	 * 
	 * @param sharedContainer the boolean
	 */
	public void setSharedContainer(boolean sharedContainer) {
		this.sharedContainer = sharedContainer;
	}

	/**
	 * 
	 * Returns the max size of the request in bits.
	 * 
	 * @return the size of the request in bits
	 */
	public long getMinRequestSize() {
		return requestSizeMinValue;
	}

	/**
	 * 
	 * Sets the min size of the request in bits.
	 * 
	 * @param requestSize the size of the request in bits
	 */
	public void setMinRequestSize(long minRequestSize) {
		this.requestSizeMinValue = minRequestSize;
	}

		/**
	 * 
	 * Returns the max size of the request in bits.
	 * 
	 * @return the size of the request in bits
	 */
	public long getMaxRequestSize() {
		return requestSizeMaxValue;
	}

	/**
	 * 
	 * Sets the max size of the request in bits.
	 * 
	 * @param requestSize the size of the request in bits
	 */
	public void setMaxRequestSize(long maxRequestSize) {
		this.requestSizeMaxValue = maxRequestSize;
	}

	/**
	 * 
	 * Returns the length of the task in MI.
	 * 
	 * @return the length of the task in MI
	 */
	public double getTaskLength() {
		return taskLength;
	}

	/**
	 * 
	 * Sets the length of the task in MI.
	 * 
	 * @param taskLength the length of the task in MI
	 */
	public void setTaskLength(double taskLength) {
		this.taskLength = taskLength;
	}

	/**
	 * 
	 * Returns the size of the results in bits.
	 * 
	 * @return the size of the results in bits
	 */
	public long getResultsSize() {
		return resultsSize;
	}

	/**
	 * 
	 * Sets the size of the results in bits.
	 * 
	 * @param resultsSize the size of the results in bits
	 */
	public void setResultsSize(long resultsSize) {
		this.resultsSize = resultsSize;
	}

	/**
	 * 
	 * Returns the latency of the application in seconds.
	 * 
	 * @return the latency of the application in seconds
	 */
	public double getLatency() {
		return latency;
	}

	/**
	 * 
	 * Sets the latency of the application in seconds.
	 * 
	 * @param latency the latency of the application in seconds
	 */
	public void setLatency(double latency) {
		this.latency = latency;
	}

	/**
	 * 
	 * Returns the type of the application.
	 * 
	 * @return the type of the application
	 */
	public String getType() {
		return type;
	}

	/**
	 * 
	 * Sets the type of the application.
	 * 
	 * @param type the type of the application
	 */
	public void setType(String type) {
		this.type = type;
	}

	/**
	 * 
	 * Returns the size of the container in megabytes.
	 * 
	 * @return the size of the container in megabytes
	 */
	public double getContainerSizeInMBytes() {
		return containerSize / BITS_IN_MB;
	}

	/**
	 * 
	 * Returns a string representation of the Application object.
	 * 
	 * @return a string representation of the Application object
	 */
	@Override
	public String toString() {
		return "Application [type=" + type + ", latency=" + latency + ", containerSize="
				+ containerSize + ", minRequestSize=" + requestSizeMinValue + ", maxRequestSize=" + requestSizeMaxValue + ", resultsSize=" + resultsSize + ", taskLength="
				+ taskLength + "]";
	}

}
