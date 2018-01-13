package com.ricex.cartracker.common.entity;

import org.apache.commons.lang3.StringUtils;

public enum TripStatus {

	NEW (1, "New"),
	STARTED (2, "Started"),
	FINISHED (3, "Finished"),
	PROCESSED (4, "Processed"),
	FAILED (5, "FAILED");
	
	/** Converts a string to its corresponding Trip Status based upon name
	 * 
	 * @param str
	 * @return
	 */
	public static TripStatus fromString(String str) {
		for (TripStatus status : values()) {
			if (StringUtils.equalsIgnoreCase(status.name, str)) {
				return status;
			}			
		}
		
		return null;
	}

	/** Gets the Trip Status by its id
	 *
	 * @param id
	 * @return
	 */
	public static TripStatus fromId(int id) {
		for (TripStatus status : values()) {
			if (status.id == id) {
				return status;
			}
		}
		return null;
	}

	/** The id of the Trip Status */
	private final int id;

	/** The human readable name of the Trip Status */
	private final String name;
	
	private TripStatus(int id, String name) {
		this.id = id;
		this.name = name;
	}
	
	public String toString() {
		return name;
	}

	public int getId() {
		return id;
	}
	
}
