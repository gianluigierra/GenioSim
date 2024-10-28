package com.mechalikh.pureedgesim.taskgenerator;

import java.util.Objects;

public abstract class ContainerAbstract implements Container, Comparable<Container> {

	/**
	 * 
	 * The unique identifier of this Container
	 */
	protected int id;
	/**
	 * 
	 * The serial number of this Container
	 */
	protected long serial;

	/**
	 * 
	 * Constructs a new ContainerAbstract instance with the given ID.
	 * 
	 * @param id the Container ID to set
	 */
	protected ContainerAbstract(int id) {
		this.setId(id);
	}

    /**
	 * 
	 * Sets the ID of this Container to the specified value.
	 * 
	 * @param id the ID to set for this Container
	 */
	@Override
	public void setId(int id) {
		this.id = id;
	}

	/**
	 * 
	 * Returns the ID of this Container.
	 * 
	 * @return the ID of this Container
	 */
	@Override
	public int getId() {
		return id;
	}

	/**
	 * 
	 * Sets the serial number of this Container to the specified value.
	 * 
	 * @param l the serial number to set for this Container
	 */
	public void setSerial(long l) {
		this.serial = l;
	}

	/**
	 * 
	 * Compares this Container with the specified Container for order. Returns a negative
	 * integer, zero, or a positive integer as this Container is less than, equal to, or
	 * greater than the specified Container.
	 * 
	 * @param that the Container to be compared
	 * 
	 * @return a negative integer, zero, or a positive integer as this Container is less
	 *         than, equal to, or greater than the specified Container
	 */
	@Override
	public int compareTo(final Container that) {
		if (that.equals(null)) {
			return 1;
		}

		if (this.equals(that)) {
			return 0;
		}

		int res = Double.compare(this.getTime(), that.getTime());
		if (res != 0) {
			return res;
		}

		return Long.compare(serial, that.getSerial());
	}

	/**
	 * 
	 * Indicates whether some other object is "equal to" this one.
	 * 
	 * @param obj the object to compare to
	 * @return true if this object is the same as the obj argument; false otherwise
	 */
	@Override
	public boolean equals(final Object obj) {
		if (this == obj)
			return true;
		if (obj == null || getClass() != obj.getClass())
			return false;
		final Container that = (Container) obj;
		return Double.compare(that.getTime(), getTime()) == 0 && getSerial() == that.getSerial();
	}
	/**
	 * 
	 * Returns a hash code value for the object. The hash code is generated based on
	 * the time and serial number of the Container.
	 * 
	 * @return the hash code value for the object.
	 */
	@Override
	public int hashCode() {
		return Objects.hash(getSerial());
	}

	/**
	 * 
	 * Returns the serial number of the Container.
	 * 
	 * @return the serial number of the Container.
	 */
	public long getSerial() {
		return this.serial;
	}
    
}
