package com.ricex.cartracker.common.viewmodel;

import java.io.Serializable;
import java.util.List;

public class PagedEntity<T> implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 3964102662989362006L;

	private final List<T> data;

	private final int skip;

	private final int take;

	private final long total;

	public PagedEntity(List<T> data, int skip, int take, long total) {
		this.data = data;
		this.skip = skip;
		this.take = take;
		this.total = total;
	}

	/**
	 * @return the data
	 */
	public List<T> getData() {
		return data;
	}

	/**
	 * @return How many records were skipped (start at)
	 */
	public int getSkip() {
		return skip;
	}

	/**
	 * @return How many records to take
	 */
	public int getTake() {
		return take;
	}

	/**
	 * @return The total number of records
	 */
	public long getTotal() {
		return total;
	}



}
