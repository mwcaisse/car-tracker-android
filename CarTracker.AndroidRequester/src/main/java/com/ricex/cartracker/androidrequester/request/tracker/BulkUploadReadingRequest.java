package com.ricex.cartracker.androidrequester.request.tracker;

import java.util.List;

import com.ricex.cartracker.androidrequester.request.AbstractRequest;
import com.ricex.cartracker.androidrequester.request.ApplicationPreferences;
import com.ricex.cartracker.androidrequester.request.response.RequestResponse;
import com.ricex.cartracker.androidrequester.request.exception.RequestException;
import com.ricex.cartracker.androidrequester.request.type.BulkUploadResponseType;
import com.ricex.cartracker.common.entity.ReaderLog;
import com.ricex.cartracker.common.entity.Trip;
import com.ricex.cartracker.common.viewmodel.BulkUploadResult;
import com.ricex.cartracker.common.viewmodel.BulkUploadViewModel;
import com.ricex.cartracker.common.viewmodel.entity.ReaderLogViewModel;
import com.ricex.cartracker.common.viewmodel.entity.ReadingViewModel;

/** Performs a Bulk Upload of Readings
 * 
 * @author Mitchell Caisse
 *
 */

public class BulkUploadReadingRequest extends AbstractRequest<List<BulkUploadResult>> {

	private final long tripId;
	
	private final BulkUploadViewModel<ReadingViewModel>[] readings;
	
	/** Creates a new Bulk Upload Request for the given trip with the given readings
	 * 
	 * @param applicationPreferences
	 * @param trip
	 * @param readings
	 */
	
	public BulkUploadReadingRequest(ApplicationPreferences applicationPreferences, Trip trip,
									List<BulkUploadViewModel<ReadingViewModel>> readings) {

		this(applicationPreferences, trip.getId(), readings);
	}
	
	
	/** Creates a new Bulk Upload Request for the given trip with the given readings
	 * 
	 * @param applicationPreferences
	 * @param tripId
	 * @param readings
	 */
	
	public BulkUploadReadingRequest(ApplicationPreferences applicationPreferences, long tripId,
									List<BulkUploadViewModel<ReadingViewModel>> readings) {

		super(applicationPreferences);
		this.tripId = tripId;
		this.readings = readings.toArray(new BulkUploadViewModel[0]);
	}
	

	@Override
	protected RequestResponse<List<BulkUploadResult>> executeRequest() throws RequestException {
		return postForObject(serverAddress + "/trip/{tripId}/reading/bulk", readings,
				new BulkUploadResponseType(), tripId);
	}

}
