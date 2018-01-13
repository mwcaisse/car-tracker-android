package com.ricex.cartracker.common.entity;

import org.apache.commons.lang3.StringUtils;

public enum TripPossiblePlaceType {

	START (1, "Start"),
	DESTINATION (2, "Destination");
	
	/** Converts a string to its corresponding Trip Possible Place Type based upon name
	 *
	 * @param str
	 * @return
	 */
	public static TripPossiblePlaceType fromString(String str) {
		for (TripPossiblePlaceType type : values()) {
			if (StringUtils.equalsIgnoreCase(type.name, str)) {
				return type;
			}
		}

		return null;
	}

	/** Gets the Trip Possible Place Type that corresponds to the given id
	 *
	 * @param id
	 * @return
	 */
	public static TripPossiblePlaceType fromId(int id) {
		for (TripPossiblePlaceType type : values()) {
			if (type.id == id) {
				return type;
			}
		}

		return null;
	}

	/** The id of this Trip Possible Place Type */
	private final int id;

	/** The human readable name of the Trip Possible Place Type */
	private final String name;
	
	private TripPossiblePlaceType(int id, String name) {
		this.id = id;
		this.name = name;
	}
	
	public String toString() {
		return name;
	}
}
