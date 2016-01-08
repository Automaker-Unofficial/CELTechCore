package celtech.appManager;

import celtech.appManager.PurgeResponse;
import celtech.appManager.SystemNotificationManager;
import celtech.configuration.fileRepresentation.HeadFile;
import celtech.printerControl.comms.commands.rx.FirmwareError;
import celtech.printerControl.model.Printer;
import celtech.services.firmware.FirmwareLoadResult;
import celtech.services.firmware.FirmwareLoadService;
import celtech.utils.tasks.TaskResponder;
import java.util.Optional;
import java.util.Set;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author ianhudson
 */
public class ConsoleSystemNotificationManager implements SystemNotificationManager
{

    private final Stenographer steno = StenographerFactory.getStenographer(ConsoleSystemNotificationManager.class.getName());

    @Override
    public void showInformationNotification(String title, String message)
    {
        outputNotification(title, message);
    }

    @Override
    public void showWarningNotification(String title, String message)
    {
        outputNotification(title, message);
    }

    @Override
    public void showErrorNotification(String title, String message)
    {
        outputNotification(title, message);
    }

    @Override
    public boolean askUserToUpdateFirmware()
    {
        outputNotification("Firmware update query");
        return true;
    }

    @Override
    public void processErrorPacketFromPrinter(FirmwareError error, Printer printer)
    {
        outputNotification("Firmware error", error.name() + " : " + printer.getPrinterIdentity().printerFriendlyNameProperty().get());
    }

    @Override
    public void showCalibrationDialogue()
    {
        outputNotification("Calibration dialogue");
    }

    @Override
    public void showFirmwareUpgradeStatusNotification(FirmwareLoadResult result)
    {
        outputNotification("Firmware load result");
    }

    @Override
    public void showGCodePostProcessFailedNotification()
    {
        outputNotification("Post process failed");
    }

    @Override
    public void showGCodePostProcessSuccessfulNotification()
    {
        outputNotification("Post process success");
    }

    @Override
    public void showHeadUpdatedNotification()
    {
        outputNotification("Head updated");
    }

    @Override
    public void showPrintJobCancelledNotification()
    {
        outputNotification("Print job cancelled");
    }

    @Override
    public void showPrintJobFailedNotification()
    {
        outputNotification("Print job failed");
    }

    @Override
    public void showPrintTransferInitiatedNotification()
    {
        outputNotification("Print transfer initiated");
    }

    @Override
    public void showPrintTransferSuccessfulNotification(String printerName)
    {
        outputNotification("Print transfer succeeded");
    }

    @Override
    public void showPrintTransferFailedNotification(String printerName)
    {
        outputNotification("Print transfer failed");
    }

    @Override
    public void removePrintTransferFailedNotification()
    {
        outputNotification("Remove printer transfer notification");
    }

    @Override
    public void showReprintStartedNotification()
    {
        outputNotification("Reprint started");
    }

    @Override
    public void showSDCardNotification()
    {
        outputNotification("SD card");
    }

    @Override
    public void showSliceFailedNotification()
    {
        outputNotification("Slice failed");
    }

    @Override
    public void showSliceSuccessfulNotification()
    {
        outputNotification("Slice successful");
    }

    @Override
    public void configureFirmwareProgressDialog(FirmwareLoadService firmwareLoadService)
    {
        outputNotification("Configure firmware progress");
    }

    @Override
    public void showNoSDCardDialog()
    {
        outputNotification("No SD card");
    }

    @Override
    public void showNoPrinterIDDialog(Printer printer)
    {
        outputNotification("No printer ID");
    }

    @Override
    public boolean showOpenDoorDialog()
    {
        outputNotification("Open door dialog");
        return false;
    }

    @Override
    public boolean showModelTooBigDialog(String modelFilename)
    {
        outputNotification("Model too big dialog");
        return false;
    }

    @Override
    public boolean showApplicationUpgradeDialog(String applicationName)
    {
        outputNotification("Application upgrade dialog");
        return true;
    }

    @Override
    public PurgeResponse showPurgeDialog()
    {
        outputNotification("Purge dialog");
        return PurgeResponse.DONT_PRINT;
    }

    @Override
    public PurgeResponse showPurgeDialog(boolean allowAutoPrint)
    {
        outputNotification("Purge dialog - autoprint");
        return PurgeResponse.DONT_PRINT;
    }

    @Override
    public boolean showJobsTransferringShutdownDialog()
    {
        outputNotification("Jobs transferring shutdown dialog");
        return false;
    }

    @Override
    public void showProgramInvalidHeadDialog(TaskResponder<HeadFile> taskResponse)
    {
        outputNotification("Program invalid head dialog");
    }

    @Override
    public void showHeadNotRecognisedDialog(String printerName)
    {
        outputNotification("Head not recognised dialog");
    }

    @Override
    public Optional<PrinterErrorChoice> showPrinterErrorDialog(String title, String message, boolean showContinueOption, boolean showAbortOption, boolean showRetryOption, boolean showOKOption)
    {
        outputNotification("Printer error dialog", title + ":" + message);
        return Optional.empty();
    }

    @Override
    public void showReelNotRecognisedDialog(String printerName)
    {
        outputNotification("Reel not recognised", printerName);
    }

    @Override
    public void showReelUpdatedNotification()
    {
        outputNotification("Reel updated");
    }

    @Override
    public void askUserToClearBed()
    {
        outputNotification("Ask user to clear bed");
    }

    @Override
    public boolean confirmAdvancedMode()
    {
        outputNotification("Confirm advanced mode dialog");
        return false;
    }

    @Override
    public void showKeepPushingFilamentNotification()
    {
        outputNotification("Keep pushing filament");
    }

    @Override
    public void hideKeepPushingFilamentNotification()
    {
        //Nothing output as this gets called periodically by the printer manager
    }

    @Override
    public void showEjectFailedDialog(Printer printer, int nozzleNumber)
    {
        outputNotification("Eject failed dialog");
    }

    @Override
    public void showFilamentMotionCheckBanner()
    {
        outputNotification("Filament motion check");
    }

    @Override
    public void hideFilamentMotionCheckBanner()
    {
        outputNotification("Hide filament motion check");
    }

    @Override
    public void showFilamentStuckMessage()
    {
        outputNotification("Filament stuck");
    }

    @Override
    public void showLoadFilamentNowMessage()
    {
        outputNotification("Load filament");
    }

    @Override
    public boolean showModelIsInvalidDialog(Set<String> modelNames)
    {
        outputNotification("Model is invalid");
        return false;
    }

    @Override
    public void clearAllDialogsOnDisconnect()
    {
        outputNotification("Clear all dialogs");
    }

    private void outputNotification(String message)
    {
        steno.info("NOTIFICATION: " + message);
    }

    private void outputNotification(String title, String message)
    {
        outputNotification(title + " : " + message);
    }

}