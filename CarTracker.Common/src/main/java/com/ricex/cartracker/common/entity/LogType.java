package com.ricex.cartracker.common.entity;

import org.apache.commons.lang3.StringUtils;

public enum LogType {

	DEBUG (1, "Debug"),
	INFO (2, "Info"),
	WARN (3, "Warn"),
	ERROR (4, "Error");

	private final int id;

	private final String name;
	
	/** Converts a string to its corresponding Log Type based upon name
	 * 
	 * @param str
	 * @return
	 */
	
	public static LogType fromString(String str) {
		for (LogType type : values()) {
			if (StringUtils.equalsIgnoreCase(type.name, str)) {
				return type;
			}
		}
		return null;
	}


	/** Gets the Log Type that corresponds to given id
	 *
	 * @param id
	 * @return
	 */
	public static LogType fromId(int id) {
		for (LogType type : values()) {
			if (type.id == id) {
				return type;
			}
		}
		return null;
	}
	
	private LogType(int id, String name) {
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
