package com.ricex.cartracker.android.service;

import android.util.Log;

import com.ricex.cartracker.android.data.entity.RawReading;
import com.ricex.cartracker.android.data.entity.RawTrip;
import com.ricex.cartracker.android.data.entity.ReaderLog;
import com.ricex.cartracker.android.data.manager.RawReadingManager;
import com.ricex.cartracker.android.data.manager.RawTripManager;
import com.ricex.cartracker.android.data.manager.ReaderLogManager;
import com.ricex.cartracker.android.data.util.DatabaseHelper;
import com.ricex.cartracker.android.settings.CarTrackerSettings;
import com.ricex.cartracker.androidrequester.request.exception.InvalidRequestException;
import com.ricex.cartracker.androidrequester.request.exception.RequestException;
import com.ricex.cartracker.androidrequester.request.tracker.CarTrackerRequestFactory;
import com.ricex.cartracker.common.entity.Car;
import com.ricex.cartracker.common.entity.Trip;
import com.ricex.cartracker.common.entity.TripStatus;
import com.ricex.cartracker.common.viewmodel.BulkUploadResult;
import com.ricex.cartracker.common.viewmodel.BulkUploadViewModel;
import com.ricex.cartracker.common.viewmodel.entity.ReaderLogViewModel;
import com.ricex.cartracker.common.viewmodel.entity.ReadingViewModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Mitchell on 2016-11-01.
 */
public class WebServiceSyncer {

    private static final String LOG_TAG = "CT_WSS";

    private static final int BULK_UPLOAD_SIZE = 50;

    private ReaderLogManager logManager;
    private RawTripManager tripManager;
    private RawReadingManager readingManager;
    private CarTrackerSettings settings;

    private CarTrackerRequestFactory requestFactory;

    private Map<String, Car> carCache;

    public WebServiceSyncer(DatabaseHelper databaseHelper, CarTrackerSettings settings) {
        this.settings = settings;

        this.logManager = databaseHelper.getReaderLogManager();
        this.tripManager = databaseHelper.getTripManager();
        this.readingManager = databaseHelper.getReadingManager();

        this.requestFactory = new CarTrackerRequestFactory(settings);

        carCache = new HashMap<String, Car>();
    }


    public void fullSync() {
        syncLogs();
        syncTrips();
        syncReadings();
    }

    /** Synchronize all of the un-synchronized logs
     *
     */
    public void syncLogs() {
        List<ReaderLog> unsyncedLogs = logManager.getUnsynced();
        Map<Long, ReaderLog> unsyncedLogsMap = new HashMap<Long, ReaderLog>();

        if (null != unsyncedLogs && !unsyncedLogs.isEmpty()) {
            for (ReaderLog log : unsyncedLogs) {
                unsyncedLogsMap.put(log.getId(), log);
            }

            for (int startIndex = 0; startIndex < unsyncedLogs.size(); startIndex += BULK_UPLOAD_SIZE) {
                List<ReaderLog> toUpload = unsyncedLogs.subList(startIndex, Math.min(startIndex + BULK_UPLOAD_SIZE, unsyncedLogs.size()));
                List<BulkUploadViewModel<ReaderLogViewModel>> uploads = new ArrayList<BulkUploadViewModel<ReaderLogViewModel>>();

                for (ReaderLog log : toUpload) {
                    BulkUploadViewModel<ReaderLogViewModel> upload = new BulkUploadViewModel<ReaderLogViewModel>();
                    ReaderLogViewModel model = new ReaderLogViewModel();
                    model.setType(log.getType());
                    model.setMessage(log.getMessage());
                    model.setDate(log.getDate());
                    upload.setUuid(Long.toString(log.getId()));
                    upload.setData(model);

                    uploads.add(upload);

                }
                try {
                    List<BulkUploadResult> results = requestFactory.createBulkUploadReaderLogRequest(uploads).execute();
                    for (BulkUploadResult result : results) {
                        if (result.isSuccessful()) {
                            ReaderLog readerLog = unsyncedLogsMap.get(Long.parseLong(result.getUuid()));
                            if (null != readerLog) {
                                readerLog.setServerId(result.getId());
                                readerLog.setSyncedWithServer(true);
                            }
                        }
                    }
                    //update the logs
                    logManager.update(toUpload);
                }
                catch (RequestException e) {
                    Log.w(LOG_TAG, "Error occured while creating reader logs on the server!", e);
                }
            }
        }
    }

    public void syncTrips() {
        List<RawTrip> unsyncedTrips = tripManager.getUnsynced();

        if (null != unsyncedTrips && !unsyncedTrips.isEmpty()) {
            for (RawTrip unsyncedTrip : unsyncedTrips) {


                Car car = getCarByVin(unsyncedTrip.getCarVin());
                if (null == car) {
                    Log.w(LOG_TAG, "Couldn't create trip, car with vin: " +
                                    unsyncedTrip.getCarVin() + " couldn't be fetched/created.");
                    continue;
                }

                Trip trip = new Trip();
                trip.setStartDate(unsyncedTrip.getStartDate());
                trip.setEndDate(unsyncedTrip.getEndDate());
                trip.setCarId(car.getId());
                trip.setStatus(TripStatus.NEW);

                try {
                    trip = requestFactory.createCreateTripRequest(trip).execute();

                    unsyncedTrip.setServerId(trip.getId());
                    unsyncedTrip.setSyncedWithServer(true);

                    tripManager.update(unsyncedTrip);

                    syncReadings(unsyncedTrip);

                    // Mark the trip's status as finished once we have synced its readings
                    trip.setStatus(TripStatus.FINISHED);
                    requestFactory.createUpdateTripRequest(trip).execute();
                }
                catch (RequestException e) {
                    Log.w(LOG_TAG, "Error occurred while creating a trip on the server!", e);
                }

            }
        }
    }
    /** Gets the car with the given VIN.
     *
     *  Will fetch the car with the given VIN from the server, if it doesn't exist
     *      it will create a new car.
     *
     * @param vin The vin of the car
     * @return The car with the given VIN, null if error occured
     */
    protected Car getCarByVin(String vin) {
        if (carCache.containsKey(vin)) {
            return carCache.get(vin);
        }

        Car car = null;

        try {
            car = requestFactory.createGetCarRequest(vin).execute();
        }
        catch (InvalidRequestException e) {
            car = new Car();
            car.setVin(vin);
            car = createCar(car);
        }
        catch (RequestException e) {
            Log.w(LOG_TAG, "Error occured while fetching car by vin", e);
        }

        //add the car to the cache if we found/created one
        if (null != car) {
            carCache.put(vin, car);
        }

        return car;
    }

    /** Creates the given car
     *
     * @param car The car to create
     * @return The created car from the server, or null if an error ocured
     */
    protected Car createCar(Car car) {
        try {
            return requestFactory.createCreateCarRequest(car).execute();
        }
        catch (RequestException e) {
            Log.w(LOG_TAG, "Error occured while creating the given car", e);
            return null;
        }
    }

    /** Syncs any unsynced readings for synced trips
     *
     */
    public void syncReadings() {
        List<RawTrip> tripsWithUnscynedReadings = tripManager.getTripsWithUnsyncedReadings();

        if (null != tripsWithUnscynedReadings && !tripsWithUnscynedReadings.isEmpty()) {
            for (RawTrip trip : tripsWithUnscynedReadings) {
                syncReadings(trip);
            }
        }
    }

    /** Syncs the unsynced readings for the given trip
     *
     * @param trip
     */
    protected void syncReadings(RawTrip trip) {
        List<RawReading> unsyncedReadings = readingManager.getUnsyncedForTrip(trip.getId());
        Map<Long, RawReading> unsyncedReadingsMap = new HashMap<Long, RawReading>();

        for (RawReading reading : unsyncedReadings) {
            unsyncedReadingsMap.put(reading.getId(), reading);
        }

        for (int startIndex = 0; startIndex < unsyncedReadings.size(); startIndex += BULK_UPLOAD_SIZE) {
            List<RawReading> toUpload = unsyncedReadings.subList(startIndex, Math.min(startIndex + BULK_UPLOAD_SIZE, unsyncedReadings.size()));
            List<BulkUploadViewModel<ReadingViewModel>> uploads = new ArrayList<BulkUploadViewModel<ReadingViewModel>>();

            for (RawReading rawReading : toUpload) {
                BulkUploadViewModel<ReadingViewModel> upload = new BulkUploadViewModel<ReadingViewModel>();
                ReadingViewModel model = new ReadingViewModel();

                upload.setUuid(Long.toString(rawReading.getId()));
                upload.setData(model);
                model.setReadDate(rawReading.getReadDate());
                model.setTripId(trip.getServerId());
                model.setLatitude(rawReading.getLatitude());
                model.setLongitude(rawReading.getLongitude());
                model.setAirIntakeTemperature(rawReading.getAirIntakeTemperature());
                model.setAmbientAirTemperature(rawReading.getAmbientAirTemperature());
                model.setEngineCoolantTemperature(rawReading.getEngineCoolantTemperature());
                model.setOilTemperature(rawReading.getOilTemperature());
                model.setEngineRPM(rawReading.getEngineRPM());
                model.setSpeed(rawReading.getSpeed());
                model.setMassAirFlow(rawReading.getMassAirFlow());
                model.setThrottlePosition(rawReading.getThrottlePosition());
                model.setFuelType(rawReading.getFuelType());
                model.setFuelLevel(rawReading.getFuelLevel());

                uploads.add(upload);
            }

            try {
                List<BulkUploadResult> results = requestFactory.createBulkUploadReadingRequest(trip.getServerId(), uploads).execute();
                for (BulkUploadResult result : results) {
                    if (result.isSuccessful()) {
                        RawReading reading = unsyncedReadingsMap.get(Long.parseLong(result.getUuid()));
                        if (null != reading) {
                            reading.setServerId(result.getId());
                            reading.setSyncedWithServer(true);
                        }
                    }
                }
                //update the readings with thier server id + synced to server flag
                readingManager.update(toUpload);
            }
            catch (RequestException e) {
                Log.w(LOG_TAG, "Error occured while creating readings on the server!", e);
            }


        }

    }


}
