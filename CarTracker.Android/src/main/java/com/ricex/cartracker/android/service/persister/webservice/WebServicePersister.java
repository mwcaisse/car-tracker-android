package com.ricex.cartracker.android.service.persister.webservice;

import android.util.Log;

import com.ricex.cartracker.android.model.OBDReading;
import com.ricex.cartracker.android.service.persister.Persister;
import com.ricex.cartracker.android.settings.CarTrackerSettings;
import com.ricex.cartracker.androidrequester.request.exception.RequestException;
import com.ricex.cartracker.androidrequester.request.tracker.BulkUploadReadingRequest;
import com.ricex.cartracker.androidrequester.request.tracker.EndTripRequest;
import com.ricex.cartracker.androidrequester.request.tracker.StartTripRequest;
import com.ricex.cartracker.common.entity.Trip;
import com.ricex.cartracker.common.viewmodel.BulkUploadResult;
import com.ricex.cartracker.common.viewmodel.BulkUploadViewModel;
import com.ricex.cartracker.common.viewmodel.entity.ReadingViewModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Created by Mitchell on 2/18/2016.
 */
public class WebServicePersister implements Persister {

    private static final String LOG_TAG = "ODBWEBSERVICEPERSISTER";

    private CarTrackerSettings settings;

    private Object monitor;

    private Object waitMonitor;

    private boolean running;

    private Map<String, PersisterReadingUpload> uploads;

    private Trip trip;

    public WebServicePersister(CarTrackerSettings settings) {
        this.settings = settings;

        monitor = new Object();
        waitMonitor = new Object();
        running = false;

        uploads = new HashMap<String, PersisterReadingUpload>();
    }

    @Override
    public void run() {

        running = true;

        while (running) {
            try {

                synchronized (waitMonitor) {
                    waitMonitor.wait();
                }
                //we were awoke, check if we are still running
                if (!running) {
                    break;
                }

                //we have been notified, most likely uploads is above the limit

                if (uploads.size() > 0) {
                    Log.i(LOG_TAG, "Starting a reading upload!");
                    List<BulkUploadViewModel<ReadingViewModel>> readingUploads;
                    synchronized (monitor) {
                        readingUploads = getReadingUploads();
                    }

                    try {
                        List<BulkUploadResult> results = new BulkUploadReadingRequest(settings, trip, readingUploads).execute();

                        synchronized (monitor) {
                            for (BulkUploadResult result : results) {
                                if (result.isSuccessful()) {
                                    uploads.remove(result.getUuid());
                                }
                                else {
                                    PersisterReadingUpload upload = uploads.get(result.getUuid());
                                    if (null != upload) {
                                        upload.setTries(upload.getTries() + 1);
                                    }
                                }
                            }
                        }
                    }
                    catch (RequestException e) {
                        Log.e(LOG_TAG, "Error uploading reading data!", e);
                        continue;
                    }
                }


            }
            catch (InterruptedException e) {
                //eh do nothing?
            }
        }

        endTrip();


    }

    public List<BulkUploadViewModel<ReadingViewModel>> getReadingUploads() {
        List<BulkUploadViewModel<ReadingViewModel>> readingUploads = new ArrayList<BulkUploadViewModel<ReadingViewModel>>();

        for (PersisterReadingUpload upload : uploads.values()) {
            BulkUploadViewModel<ReadingViewModel> readingUpload = new BulkUploadViewModel<ReadingViewModel>();
            ReadingViewModel model = new ReadingViewModel();

            readingUpload.setUuid(upload.getUid());
            readingUpload.setData(model);
            model.setReadDate(upload.getReading().getReadDate());
            model.setTripId(trip.getId());
            model.setAirIntakeTemperature(convertStringToDouble(upload.getReading().getAirIntakeTemp()));
            model.setAmbientAirTemperature(convertStringToDouble(upload.getReading().getAmbientAirTemp()));
            model.setEngineCoolantTemperature(convertStringToDouble(upload.getReading().getEngineCoolantTemp()));
            model.setOilTemperature(convertStringToDouble(upload.getReading().getOilTemp()));
            model.setEngineRPM(convertStringToDouble(upload.getReading().getEngineRPM()));
            model.setSpeed(convertStringToDouble(upload.getReading().getSpeed()));
            model.setMassAirFlow(convertStringToDouble(upload.getReading().getMaf()));
            model.setThrottlePosition(convertStringToDouble(upload.getReading().getThrottlePosition()));
            model.setFuelType(upload.getReading().getFuelType());
            model.setFuelLevel(convertStringToDouble(upload.getReading().getFuelLevel()));

            //if there is a location associated with the reading, add it to the upload
            if (null != upload.getReading().getLocation()) {
                model.setLatitude(upload.getReading().getLocation().getLatitude());
                model.setLongitude(upload.getReading().getLocation().getLongitude());
            }

            readingUploads.add(readingUpload);
        }

        return readingUploads;
    }


    private double convertStringToDouble(String val) {
        val.replaceAll("[^\\d.-]", "");
        try {
            return Double.parseDouble(val);
        }
        catch (NumberFormatException e) {
            return 0;
        }
    }

    public void start(String vin) {
        try {
            trip = new StartTripRequest(settings, vin).execute();
        }
        catch (RequestException e) {
            Log.e(LOG_TAG, "Error starting trip!", e);
        }
    }

    public void endTrip() {
        if (null != trip) {
            try {
                new EndTripRequest(settings, trip).execute();
            } catch (RequestException e) {
                Log.e(LOG_TAG, "Error ending trip!", e);
            }
        }
    }

    /** Persists the given OBDReading to the Web Service
     *
     * @param reading The reading to persist
     */
    public void persist(OBDReading reading) {
        PersisterReadingUpload upload = new PersisterReadingUpload();
        upload.setUid(UUID.randomUUID().toString());
        upload.setReading(reading);
        upload.setTries(0);
        upload.setStatus(0);

        synchronized (monitor) {
            uploads.put(upload.getUid(), upload);
            if (uploads.size() > 20) {
                synchronized (waitMonitor) {
                    waitMonitor.notify();
                }
            }
        }
    }

    public void stop() {
        running = false;
        synchronized (waitMonitor) {
            waitMonitor.notify();
        }
    }
}
