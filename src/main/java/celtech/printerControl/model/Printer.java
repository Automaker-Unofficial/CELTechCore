package celtech.printerControl.model;

import celtech.appManager.Project;
import celtech.configuration.BusyStatus;
import celtech.configuration.EEPROMState;
import celtech.configuration.Filament;
import celtech.configuration.Macro;
import celtech.configuration.MaterialType;
import celtech.configuration.PauseStatus;
import celtech.configuration.PrinterEdition;
import celtech.configuration.PrinterModel;
import celtech.coreUI.controllers.PrinterSettings;
import celtech.printerControl.PrinterStatus;
import celtech.printerControl.comms.commands.exceptions.RoboxCommsException;
import celtech.printerControl.comms.commands.rx.AckResponse;
import celtech.printerControl.comms.commands.rx.FirmwareError;
import celtech.printerControl.comms.commands.rx.FirmwareResponse;
import celtech.printerControl.comms.commands.rx.HeadEEPROMDataResponse;
import celtech.printerControl.comms.commands.rx.ListFilesResponse;
import celtech.printerControl.comms.commands.rx.PrinterIDResponse;
import celtech.printerControl.comms.commands.rx.ReelEEPROMDataResponse;
import celtech.printerControl.comms.commands.rx.RoboxRxPacket;
import celtech.printerControl.comms.commands.rx.SendFile;
import celtech.printerControl.comms.commands.rx.StatusResponse;
import celtech.printerControl.comms.events.ErrorConsumer;
import celtech.printerControl.comms.events.RoboxResponseConsumer;
import celtech.printerControl.model.calibration.NozzleHeightStateTransitionManager;
import celtech.printerControl.model.calibration.NozzleOpeningStateTransitionManager;
import celtech.printerControl.model.calibration.XAndYStateTransitionManager;
import celtech.services.printing.DatafileSendAlreadyInProgress;
import celtech.services.printing.DatafileSendNotInitialised;
import celtech.utils.AxisSpecifier;
import celtech.utils.tasks.Cancellable;
import celtech.utils.tasks.TaskResponder;
import java.util.List;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.scene.paint.Color;

/**
 *
 * @author Ian
 */
public interface Printer extends RoboxResponseConsumer
{

    enum NozzleHeaters
    {

        NOZZLE_HEATER_0, NOZZLE_HEATER_1, NOZZLE_HEATER_BOTH;
    }

    public ReadOnlyObjectProperty<Head> headProperty();

    /**
     *
     * @param gcodeToSend
     */
    public void addToGCodeTranscript(String gcodeToSend);

    /*
     * Cancel
     */
    public ReadOnlyBooleanProperty canCancelProperty();

    /*
     * Print
     */
    public ReadOnlyBooleanProperty canPrintProperty();

    /*
     * Can open or close a nozzle
     */
    public ReadOnlyBooleanProperty canOpenCloseNozzleProperty();

    /**
     * Can perform a nozzle height calibration
     */
    public ReadOnlyBooleanProperty canCalibrateNozzleHeightProperty();

    /**
     * Can perform an XY alignment calibration
     */
    public ReadOnlyBooleanProperty canCalibrateXYAlignmentProperty();

    /**
     * Can perform a nozzle opening calibration
     */
    public ReadOnlyBooleanProperty canCalibrateNozzleOpeningProperty();

    /*
     * Purge
     */
    public ReadOnlyBooleanProperty canPurgeHeadProperty();

    public void resetPurgeTemperature(PrinterSettings printerSettings);

    public PurgeStateTransitionManager startPurge() throws PrinterException;

    /*
     * Calibrate head
     */
    public ReadOnlyBooleanProperty canCalibrateHeadProperty();

    public XAndYStateTransitionManager startCalibrateXAndY() throws PrinterException;

    public NozzleHeightStateTransitionManager startCalibrateNozzleHeight() throws PrinterException;

    public NozzleOpeningStateTransitionManager startCalibrateNozzleOpening() throws PrinterException;

    public NozzleHeightStateTransitionManager getNozzleHeightCalibrationStateManager();

    public NozzleOpeningStateTransitionManager getNozzleOpeningCalibrationStateManager();

    public XAndYStateTransitionManager getNozzleAlignmentCalibrationStateManager();

    /*
     * Remove head
     */
    public ReadOnlyBooleanProperty canRemoveHeadProperty();

    public void cancel(TaskResponder responder) throws PrinterException;

    public void gotoNozzlePosition(float position);

    public void closeNozzleFully() throws PrinterException;

    public void ejectFilament(int extruderNumber, TaskResponder responder) throws PrinterException;

    public ObservableList<Extruder> extrudersProperty();

    public AckResponse formatHeadEEPROM() throws PrinterException;

    public AckResponse formatReelEEPROM(int reelNumber) throws PrinterException;

    public ObservableList<String> gcodeTranscriptProperty();

    public ReadOnlyBooleanProperty canPauseProperty();

    public ReadOnlyBooleanProperty canResumeProperty();

    public int getDataFileSequenceNumber();

    public void resetDataFileSequenceNumber();

    public void setDataFileSequenceNumberStartPoint(int startingSequenceNumber);

    public PrintEngine getPrintEngine();

    public PrinterAncillarySystems getPrinterAncillarySystems();

    public PrinterIdentity getPrinterIdentity();

    /*
     * Door open
     */
    public ReadOnlyBooleanProperty canOpenDoorProperty();

    public void goToOpenDoorPosition(TaskResponder responder) throws PrinterException;

    public void goToOpenDoorPositionDontWait(TaskResponder responder) throws PrinterException;

    public void goToTargetBedTemperature();

    public void goToTargetNozzleHeaterTemperature(int nozzleHeaterNumber);

    public void goToZPosition(double position);

    public void goToXYPosition(double xPosition, double yPosition);

    public void goToXYZPosition(double xPosition, double yPosition, double zPosition);

    public void homeX();

    public void homeY();

    public void homeZ();

    public void probeX();

    public float getXDelta() throws PrinterException;

    public void probeY();

    public float getYDelta() throws PrinterException;

    public void probeZ();

    public float getZDelta() throws PrinterException;

    public TemperatureAndPWMData getTemperatureAndPWMData() throws PrinterException;

    public void levelGantryRaw();

    public boolean initialiseDataFileSend(String fileID, boolean jobCanBeReprinted) throws DatafileSendAlreadyInProgress, RoboxCommsException;

    public SendFile requestSendFileReport() throws RoboxCommsException;

    /**
     *
     * @param jobUUID
     * @throws RoboxCommsException
     */
    public void initiatePrint(String jobUUID) throws RoboxCommsException;

    public boolean isPrintInitiated();

    public void jogAxis(AxisSpecifier axis, float distance, float feedrate, boolean use_G1) throws PrinterException;

    public void openNozzleFully() throws PrinterException;

    public void openNozzleFullyExtra() throws PrinterException;

    public void pause() throws PrinterException;

    public void printProject(Project project) throws PrinterException;

    public ReadOnlyObjectProperty<PrinterStatus> printerStatusProperty();

    @Override
    public void processRoboxResponse(RoboxRxPacket rxPacket);

    public FirmwareResponse readFirmwareVersion() throws PrinterException;

    public HeadEEPROMDataResponse readHeadEEPROM() throws RoboxCommsException;

    public PrinterIDResponse readPrinterID() throws PrinterException;

    public ReelEEPROMDataResponse readReelEEPROM(int reelNumber, boolean dontPublishResponseEvent) throws RoboxCommsException;

    public ObservableMap<Integer, Reel> reelsProperty();

    public void removeHead(TaskResponder responder) throws PrinterException;

    public void resume() throws PrinterException;

    /**
     *
     * @param blockUntilFinished
     * @param cancellable
     * @throws PrinterException
     */
    public void homeAllAxes(boolean blockUntilFinished, Cancellable cancellable) throws PrinterException;

    /**
     *
     * @param nozzleHeaters
     * @param blockUntilFinished
     * @param cancellable
     * @throws PrinterException
     */
    public void purgeMaterial(NozzleHeaters nozzleHeaters, boolean blockUntilFinished, Cancellable cancellable) throws PrinterException;

    public void miniPurge(boolean blockUntilFinished, Cancellable cancellable, int nozzleNumber) throws PrinterException;

    public void testX(boolean blockUntilFinished, Cancellable cancellable) throws PrinterException;

    public void testY(boolean blockUntilFinished, Cancellable cancellable) throws PrinterException;

    public void testZ(boolean blockUntilFinished, Cancellable cancellable) throws PrinterException;

    public void speedTest(boolean blockUntilFinished, Cancellable cancellable) throws PrinterException;

    /**
     *
     * @param blockUntilFinished
     * @param cancellable
     * @throws PrinterException
     */
    public void levelGantry(boolean blockUntilFinished, Cancellable cancellable) throws PrinterException;

    /**
     *
     * @param blockUntilFinished
     * @param cancellable
     * @throws PrinterException
     */
    public void levelGantryTwoPoints(boolean blockUntilFinished, Cancellable cancellable) throws PrinterException;

    /**
     *
     * @param blockUntilFinished
     * @param cancellable
     * @throws PrinterException
     */
    public void levelY(boolean blockUntilFinished, Cancellable cancellable) throws PrinterException;

    /**
     *
     * @param blockUntilFinished
     * @param cancellable
     * @throws PrinterException
     */
    public void ejectStuckMaterialE(boolean blockUntilFinished, Cancellable cancellable) throws PrinterException;

    /**
     *
     * @param blockUntilFinished
     * @param cancellable
     * @throws PrinterException
     */
    public void ejectStuckMaterialD(boolean blockUntilFinished, Cancellable cancellable) throws PrinterException;

    /**
     *
     * @param nozzleNumber
     * @param blockUntilFinished
     * @param cancellable
     * @throws PrinterException
     */
    public void cleanNozzle(int nozzleNumber, boolean blockUntilFinished, Cancellable cancellable) throws PrinterException;

    /**
     *
     * @param macro
     * @param cancellable
     * @throws PrinterException
     */
    public void runCommissioningTest(Macro macro, Cancellable cancellable) throws PrinterException;

    /**
     * This method 'prints' a GCode file. A print job is created and the printer
     * will manage extrusion dynamically. The printer will register as an error
     * handler for the duration of the 'print'.
     *
     * @see executeMacro executeMacro - if you wish to run a macro rather than
     * execute a print job
     * @param fileName
     * @param monitorForErrors Indicates whether the printer should
     * automatically manage error handling (e.g. auto reduction of print speed)
     * @throws PrinterException
     */
    public void executeGCodeFile(String fileName, boolean monitorForErrors) throws PrinterException;

    public void executeGCodeFileWithoutPurgeCheck(String fileName, boolean monitorForErrors) throws PrinterException;

    public void callbackWhenNotBusy(TaskResponder responder);

    public void selectNozzle(int nozzleNumber) throws PrinterException;

    public void sendDataFileChunk(String hexDigits, boolean lastPacket, boolean appendCRLF) throws DatafileSendNotInitialised, RoboxCommsException;

    public void sendRawGCode(String gCode, boolean addToTranscript);

    public void setAmbientLEDColour(Color colour) throws PrinterException;

    public void setAmbientTemperature(int targetTemperature);

    public void setBedFirstLayerTargetTemperature(int targetTemperature);

    public void setBedTargetTemperature(int targetTemperature);

    public void setNozzleHeaterTargetTemperature(int nozzleHeaterNumber, int targetTemperature);

    public void setReelLEDColour(Color colour) throws PrinterException;

    public void shutdown(boolean shutdownCommandInterface);

    public void switchAllNozzleHeatersOff();

    public void switchBedHeaterOff();

    public void switchNozzleHeaterOff(int heaterNumber);

    public void switchOffHeadFan() throws PrinterException;

    public void switchOffHeadLEDs() throws PrinterException;

    public void switchOnHeadFan() throws PrinterException;

    public void switchOnHeadLEDs() throws PrinterException;

    public void switchToAbsoluteMoveMode();

    public void switchToRelativeMoveMode();

    public ListFilesResponse transmitListFiles() throws RoboxCommsException;

    public AckResponse transmitReportErrors() throws RoboxCommsException;

    public void transmitResetErrors() throws RoboxCommsException;

    /*
     * Higher level controls
     */
    public void transmitSetTemperatures(double nozzle0FirstLayerTarget, double nozzle0Target,
            double nozzle1FirstLayerTarget, double nozzle1Target,
            double bedFirstLayerTarget, double bedTarget, double ambientTarget) throws RoboxCommsException;

    public StatusResponse transmitStatusRequest() throws RoboxCommsException;

    public boolean transmitUpdateFirmware(final String firmwareID) throws PrinterException;

    public AckResponse transmitWriteHeadEEPROM(String headTypeCode, String headUniqueID,
            float maximumTemperature, float thermistorBeta, float thermistorTCal, float nozzle1XOffset,
            float nozzle1YOffset,
            float nozzle1ZOffset, float nozzle1BOffset,
            String filament0ID, String filament1ID, float nozzle2XOffset, float nozzle2YOffset,
            float nozzle2ZOffset, float nozzle2BOffset, float lastFilamentTemperature0,
            float lastFilamentTemperature1, float hourCounter) throws RoboxCommsException;

    public AckResponse transmitWriteReelEEPROM(int reelNumber, Filament filament) throws RoboxCommsException;

    public void transmitWriteReelEEPROM(int reelNumber, String filamentID,
            float reelFirstLayerNozzleTemperature, float reelNozzleTemperature,
            float reelFirstLayerBedTemperature,
            float reelBedTemperature,
            float reelAmbientTemperature, float reelFilamentDiameter, float reelFilamentMultiplier,
            float reelFeedRateMultiplier, float reelRemainingFilament, String friendlyName,
            MaterialType materialType, Color displayColour) throws RoboxCommsException;

    public void updatePrinterDisplayColour(Color displayColour) throws PrinterException;

    public void updatePrinterName(String chosenPrinterName) throws PrinterException;

    public void updatePrinterModelAndEdition(PrinterModel model, PrinterEdition edition) throws PrinterException;

    public void updatePrinterWeek(String weekIdentifier) throws PrinterException;

    public void updatePrinterYear(String yearIdentifier) throws PrinterException;

    public void updatePrinterPONumber(String poIdentifier) throws PrinterException;

    public void updatePrinterSerialNumber(String serialIdentifier) throws PrinterException;

    public void updatePrinterIDChecksum(String checksum) throws PrinterException;

    /**
     *
     * @param headToWrite
     * @throws RoboxCommsException
     */
    public void writeHeadEEPROM(Head headToWrite) throws RoboxCommsException;

    void setPrinterStatus(PrinterStatus printerStatus);

    public ReadOnlyIntegerProperty printJobLineNumberProperty();

    public ReadOnlyStringProperty printJobIDProperty();

    public ReadOnlyObjectProperty<PauseStatus> pauseStatusProperty();

    public ReadOnlyBooleanProperty headPowerOnFlagProperty();

    public void resetHeadToDefaults() throws PrinterException;

    public void inhibitHeadIntegrityChecks(boolean inhibit);

    public void changeFeedRateMultiplier(double feedRate) throws PrinterException;

    public void changeFilamentInfo(String extruderLetter,
            double filamentDiameter,
            double extrusionMultiplier) throws PrinterException;

    public void registerErrorConsumer(ErrorConsumer errorConsumer,
            List<FirmwareError> errorsOfInterest);

    public void registerErrorConsumerAllErrors(ErrorConsumer errorConsumer);

    public void deregisterErrorConsumer(ErrorConsumer errorConsumer);

    public void connectionEstablished();

    public List<Integer> requestDebugData(boolean addToGCodeTranscript);

    public ReadOnlyObjectProperty<BusyStatus> busyStatusProperty();

    /**
     * Causes a reduction in feedrate until the minimum value is reached.
     * Returns false if the limit has not been reached and true if it has
     * (implying further action is needed by the caller)
     *
     * @param error
     * @return
     */
    public boolean doFilamentSlipActionWhilePrinting(FirmwareError error);

    public void extrudeUntilSlip(int extruderNumber) throws PrinterException;

    /**
     * This method is intended to be used by commissioning tools and should not
     * be called in normal operation. Causes the specified list of firmware
     * errors to be suppressed. The printer will not take any action if these
     * errors occur, beyond clearing the error flags in the firmware. This
     * method adds to the set of firmware errors that are being suppressed.
     *
     * @param firmwareErrors
     */
    public void suppressFirmwareErrors(FirmwareError... firmwareErrors);

    /**
     * This method is intended to be used by commissioning tools and should not
     * be called in normal operation. Cancel the suppression of firmware error
     * detection. All errors will be handled normally after calling this method.
     */
    public void cancelFirmwareErrorSuppression();

    /**
     * This method is intended to be used by commissioning tools and should not
     * be called in normal operation. Prevents the printer from repairing reel
     * or head eeprom data.
     *
     * @param suppress
     */
    public void suppressEEPROMErrorCorrection(boolean suppress);

    public void transferGCodeFileToPrinterAndCallbackWhenDone(String string, TaskResponder responder);

    public void loadFirmware(String firmwareFilePath);
    
    public ObservableList<EEPROMState> getReelEEPROMStateProperty();

    public void startComms();

    public void stopComms();
    
    public void overrideFilament(int reelNumber, Filament filament);
    
    public ObservableMap<Integer, Filament> effectiveFilamentsProperty();
}
