package com.ricex.cartracker.androidrequester.request.tracker;

import com.ricex.cartracker.androidrequester.request.AbstractRequest;
import com.ricex.cartracker.androidrequester.request.ApplicationPreferences;
import com.ricex.cartracker.androidrequester.request.exception.RequestException;
import com.ricex.cartracker.androidrequester.request.response.RequestResponse;
import com.ricex.cartracker.androidrequester.request.type.TripResponseType;
import com.ricex.cartracker.common.entity.Trip;

/**
 * Created by Mitchell on 2018-01-31.
 */

public class UpdateTripRequest extends AbstractRequest<Trip> {

    private final Trip trip;

    public UpdateTripRequest(ApplicationPreferences applicationPreferences, Trip trip) {
        super(applicationPreferences);
        this.trip = trip;
    }

    protected RequestResponse<Trip> executeRequest() throws RequestException {
        return putForObject(serverAddress + "/trip/", trip, new TripResponseType());
    }
}
