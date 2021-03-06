package com.ricex.cartracker.android.service.reader;

import android.bluetooth.BluetoothSocket;
import android.preference.PreferenceManager;
import android.util.Log;

import com.github.pires.obd.commands.ObdCommand;
import com.github.pires.obd.commands.SpeedCommand;
import com.github.pires.obd.commands.control.VinCommand;
import com.github.pires.obd.commands.engine.MassAirFlowCommand;
import com.github.pires.obd.commands.engine.OilTempCommand;
import com.github.pires.obd.commands.engine.RPMCommand;
import com.github.pires.obd.commands.engine.ThrottlePositionCommand;
import com.github.pires.obd.commands.fuel.FindFuelTypeCommand;
import com.github.pires.obd.commands.fuel.FuelLevelCommand;
import com.github.pires.obd.commands.protocol.EchoOffCommand;
import com.github.pires.obd.commands.protocol.LineFeedOffCommand;
import com.github.pires.obd.commands.protocol.ObdResetCommand;
import com.github.pires.obd.commands.protocol.SelectProtocolCommand;
import com.github.pires.obd.commands.protocol.TimeoutCommand;
import com.github.pires.obd.commands.temperature.AirIntakeTemperatureCommand;
import com.github.pires.obd.commands.temperature.AmbientAirTemperatureCommand;
import com.github.pires.obd.commands.temperature.EngineCoolantTemperatureCommand;
import com.github.pires.obd.enums.AvailableCommandNames;
import com.github.pires.obd.enums.ObdProtocols;
import com.github.pires.obd.exceptions.NoDataException;
import com.github.pires.obd.exceptions.UnsupportedCommandException;
import com.ricex.cartracker.android.model.OBDReading;
import com.ricex.cartracker.android.obd.ObdCommandExecutor;
import com.ricex.cartracker.android.obd.device.BluetoothObdDevice;
import com.ricex.cartracker.android.obd.device.ObdDevice;
import com.ricex.cartracker.android.obd.device.ObdDeviceConnectionFailedException;
import com.ricex.cartracker.android.service.OBDCommandJob;
import com.ricex.cartracker.android.service.OBDCommandStatus;
import com.ricex.cartracker.android.service.logger.ServiceLogger;
import com.ricex.cartracker.android.settings.CarTrackerSettings;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Mitchell on 2/13/2016.
 */
public class BluetoothOBDReader implements OBDReader {

    private static final String LOG_TAG = "BTOBDReader";


    private final ObdCommandExecutor commandExecutor;

    private CarTrackerSettings settings;

    private ServiceLogger logger;

    public BluetoothOBDReader(ServiceLogger logger, CarTrackerSettings settings) {
        this.logger = logger;
        this.settings = settings;
        ObdDevice device = new BluetoothObdDevice(settings.getBluetoothDeviceAddress());
        this.commandExecutor = new ObdCommandExecutor(device, logger);
    }

    public boolean initialize() {
        //check if we are already connected, if so there is nothing to do
        if (commandExecutor.isInitialized()) {
            return true;
        }

        logger.info(LOG_TAG, "Initializing Bluetooth OBD Reader!");
        return commandExecutor.initialize();
    }

    public OBDReading read() throws ConnectionLostException {
        List<OBDCommandJob> jobs = createJobs();

        for (OBDCommandJob job : jobs) {
            executeOBDJob(job);

            if (OBDCommandStatus.CONNECTION_LOST.equals(job.getStatus())) {
                throw new ConnectionLostException("Connection to the OBD device lost");
            }
        }
        return readDataFromJobs(jobs);
    }

    /** Retreives the VIN of the car from the reader
     *
     * @return The car's VIN or null if read failed
     */
    public String getCarVin() {
        OBDCommandJob vinJob = new OBDCommandJob(new VinCommand());
        executeOBDJob(vinJob);

        String vin = null;
        if (vinJob.getStatus() == OBDCommandStatus.FINISHED) {
            vin = vinJob.getCommand().getCalculatedResult();
        }
        else {
            logger.warn(LOG_TAG, "Failed to fetch VIN for car");
        }

        return vin;
    }

    @Override
    public boolean reconnect() {
        return commandExecutor.reconnect();
    }

    @Override
    public boolean isConnected() {
        return commandExecutor.isConnected();
    }

    protected List<OBDCommandJob> createJobs() {
        List<OBDCommandJob> jobs = new ArrayList<OBDCommandJob>();

        jobs.add(new OBDCommandJob(new AirIntakeTemperatureCommand()));
        jobs.add(new OBDCommandJob(new AmbientAirTemperatureCommand()));
        jobs.add(new OBDCommandJob(new EngineCoolantTemperatureCommand()));
        jobs.add(new OBDCommandJob(new OilTempCommand()));
        jobs.add(new OBDCommandJob(new RPMCommand()));
        jobs.add(new OBDCommandJob(new SpeedCommand()));
        jobs.add(new OBDCommandJob(new MassAirFlowCommand()));
        jobs.add(new OBDCommandJob(new ThrottlePositionCommand()));
        jobs.add(new OBDCommandJob(new FindFuelTypeCommand()));
        jobs.add(new OBDCommandJob(new FuelLevelCommand()));
        jobs.add(new OBDCommandJob(new VinCommand()));

        return jobs;
    }

    protected OBDReading readDataFromJobs(List<OBDCommandJob> jobs) {
        OBDReading data = new OBDReading();

        for (OBDCommandJob job : jobs) {
            AvailableCommandNames commandName = parseCommandNameFromString(job.getCommand().getName());

            String results = "";

            if (job.getStatus() == OBDCommandStatus.FINISHED) {
                results = job.getCommand().getCalculatedResult();
            }
            else {
                //If there was an error executing task, set result to 0 for now.
                //TODO: Have a more verbose error handling scheme.
                results = "0";
                logger.warn(LOG_TAG, "Failed to collect data for command: " + commandName);
            }

            switch (commandName) {
                case AIR_INTAKE_TEMP:
                    data.setAirIntakeTemp(results);
                    break;
                case AMBIENT_AIR_TEMP:
                    data.setAmbientAirTemp(results);
                    break;
                case ENGINE_COOLANT_TEMP:
                    data.setEngineCoolantTemp(results);
                    break;
                case ENGINE_OIL_TEMP:
                    data.setOilTemp(results);
                    break;
                case ENGINE_RPM:
                    data.setEngineRPM(results);
                    break;
                case SPEED:
                    data.setSpeed(results);
                    break;
                case MAF:
                    data.setMaf(results);
                    break;
                case THROTTLE_POS:
                    data.setThrottlePosition(results);
                    break;
                case FUEL_TYPE:
                    data.setFuelType(results);
                    break;
                case FUEL_LEVEL:
                    data.setFuelLevel(results);
                    break;
                case VIN:
                    data.setVin(results);
                    break;
            }
        }

        return data;
    }

    /**
     *  Executes an ODB Job representing an OBD Command
     * @param job The job to execute
     */
    protected void executeOBDJob(OBDCommandJob job) {
        try {
            if (job.getStatus() == OBDCommandStatus.NEW) {
                job.setStatus(OBDCommandStatus.RUNNING);

                if (commandExecutor.isConnected()) {
                    commandExecutor.runOBDCommand(job.getCommand());

                    job.setStatus(OBDCommandStatus.FINISHED);
                    job.setHasData(true);
                }
                else {
                    job.setStatus(OBDCommandStatus.CONNECTION_LOST);
                }
            }

        }
        catch (UnsupportedCommandException e) {
            job.setStatus(OBDCommandStatus.NOT_SUPPORTED);
        }
        catch (IOException e) {
            logger.error("BTReader", "Executing job throw an IOException", e);
            job.setStatus(OBDCommandStatus.CONNECTION_LOST);
            commandExecutor.disconnect();
        }
        catch (NoDataException e) {
            job.setHasData(false);
        }
        catch (Exception e) {
            logger.error("BTReader", "Executing job throw an Exception", e);
            job.setStatus(OBDCommandStatus.EXECUTION_ERROR);
        }

    }

    protected AvailableCommandNames parseCommandNameFromString(String commandName) {
        for (AvailableCommandNames name : AvailableCommandNames.values()) {
            if (name.getValue().equals(commandName)) {
                return name;
            }
        }
        return null;
    }
}
