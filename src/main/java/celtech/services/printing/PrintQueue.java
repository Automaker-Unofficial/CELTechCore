/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 *//*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.services.printing;

import celtech.appManager.Notifier;
import celtech.appManager.Project;
import celtech.appManager.ProjectMode;
import celtech.configuration.ApplicationConfiguration;
import celtech.coreUI.DisplayManager;
import celtech.printerControl.PrintJob;
import celtech.printerControl.Printer;
import celtech.printerControl.PrinterStatusEnumeration;
import celtech.printerControl.comms.RoboxCommsManager;
import celtech.printerControl.comms.commands.exceptions.RoboxCommsException;
import celtech.services.ControllableService;
import celtech.services.modelLoader.ModelLoaderService;
import celtech.services.postProcessor.GCodePostProcessingResult;
import celtech.services.postProcessor.PostProcessorService;
import celtech.services.slicer.PrintQualityEnumeration;
import celtech.services.slicer.RoboxProfile;
import celtech.services.slicer.SliceResult;
import celtech.services.slicer.SlicerService;
import celtech.utils.SystemUtils;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ResourceBundle;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author ianhudson
 */
public class PrintQueue implements ControllableService {

    private Stenographer steno = StenographerFactory.getStenographer(PrintQueue.class.getName());

    private Printer associatedPrinter = null;
    private PrinterStatusEnumeration printState = PrinterStatusEnumeration.IDLE;
    private PrinterStatusEnumeration lastStateBeforePause = PrinterStatusEnumeration.IDLE;
    private final PrintService printService = new PrintService();
    private final SlicerService slicerService = new SlicerService();
    private PostProcessorService gcodePostProcessorService = new PostProcessorService();
    private GCodePrintService gcodePrintService = new GCodePrintService();
    private IntegerProperty linesInPrintingFile = new SimpleIntegerProperty(0);
    /*
     * 
     */
    private EventHandler<WorkerStateEvent> cancelSliceEventHandler = null;
    private EventHandler<WorkerStateEvent> failedSliceEventHandler = null;
    private EventHandler<WorkerStateEvent> succeededSliceEventHandler = null;
    private EventHandler<WorkerStateEvent> cancelGCodePostProcessEventHandler = null;
    private EventHandler<WorkerStateEvent> failedGCodePostProcessEventHandler = null;
    private EventHandler<WorkerStateEvent> succeededGCodePostProcessEventHandler = null;
    private EventHandler<WorkerStateEvent> cancelPrintEventHandler = null;
    private EventHandler<WorkerStateEvent> failedPrintEventHandler = null;
    private EventHandler<WorkerStateEvent> succeededPrintEventHandler = null;
    /*
     * 
     */
    private final StringProperty printQueueStatusString = new SimpleStringProperty();
    private final StringProperty printProgressTitle = new SimpleStringProperty();
    private final StringProperty printProgressMessage = new SimpleStringProperty();
    private final BooleanProperty dialogRequired = new SimpleBooleanProperty(false);
    private final BooleanProperty printInProgress = new SimpleBooleanProperty(false);
    private final DoubleProperty primaryProgressPercent = new SimpleDoubleProperty(0);
    private final DoubleProperty secondaryProgressPercent = new SimpleDoubleProperty(0);
    private final BooleanProperty sendingDataToPrinter = new SimpleBooleanProperty(false);
    /*
     * 
     */
    private final ObservableList<Printer> printerStatusList = RoboxCommsManager.getInstance().getPrintStatusList();
    private ChangeListener<Number> printLineNumberListener = null;
    private ChangeListener<String> printJobIDListener = null;
    private final int numberOfLinesInGCode = 0;

    private ResourceBundle i18nBundle = null;
    private String printTransferSuccessfulNotification = null;
    private String printJobCancelledNotification = null;
    private String printJobCompletedNotification = null;
    private String printJobFailedNotification = null;
    private String sliceSuccessfulNotification = null;
    private String sliceFailedNotification = null;
    private String gcodePostProcessSuccessfulNotification = null;
    private String gcodePostProcessFailedNotification = null;
    private String detectedPrintInProgressNotification = null;
    private String notificationTitle = null;

    public PrintQueue(Printer associatedPrinter) {
        this.associatedPrinter = associatedPrinter;

        i18nBundle = DisplayManager.getLanguageBundle();
        printTransferSuccessfulNotification = i18nBundle.getString("notification.printTransferredSuccessfully");
        printJobCancelledNotification = i18nBundle.getString("notification.printJobCancelled");
        printJobCompletedNotification = i18nBundle.getString("notification.printJobCompleted");
        printJobFailedNotification = i18nBundle.getString("notification.printJobFailed");
        sliceSuccessfulNotification = i18nBundle.getString("notification.sliceSuccessful");
        sliceFailedNotification = i18nBundle.getString("notification.sliceFailed");
        gcodePostProcessSuccessfulNotification = i18nBundle.getString("notification.gcodePostProcessSuccessful");
        gcodePostProcessFailedNotification = i18nBundle.getString("notification.gcodePostProcessFailed");
        notificationTitle = i18nBundle.getString("notification.PrintQueueTitle");
        detectedPrintInProgressNotification = i18nBundle.getString("notification.activePrintDetected");

        cancelSliceEventHandler = new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent t) {
                steno.info(t.getSource().getTitle() + " has been cancelled");
            }
        };

        failedSliceEventHandler = new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent t) {
                steno.info(t.getSource().getTitle() + " has failed");
                setPrintStatus(PrinterStatusEnumeration.IDLE);
                Notifier.showErrorNotification(notificationTitle, sliceFailedNotification);
            }
        };

        succeededSliceEventHandler = new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent t) {
                SliceResult result = (SliceResult) (t.getSource().getValue());

                if (result.isSuccess()) {
                    steno.info(t.getSource().getTitle() + " has succeeded");
                    gcodePostProcessorService.reset();
                    gcodePostProcessorService.setPrintJobUUID(result.getPrintJobUUID());
                    gcodePostProcessorService.setSettings(result.getSettings());
                    gcodePostProcessorService.setPrinterToUse(result.getPrinterToUse());
                    gcodePostProcessorService.start();

                    Notifier.showInformationNotification(notificationTitle, sliceSuccessfulNotification);
                    setPrintStatus(PrinterStatusEnumeration.POST_PROCESSING);
                } else {
                    Notifier.showErrorNotification(notificationTitle, sliceFailedNotification);
                    setPrintStatus(PrinterStatusEnumeration.IDLE);
                }
            }
        };

        cancelGCodePostProcessEventHandler = new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent t) {
                steno.info(t.getSource().getTitle() + " has been cancelled");
            }
        };

        failedGCodePostProcessEventHandler = new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent t) {
                steno.info(t.getSource().getTitle() + " has failed");
                setPrintStatus(PrinterStatusEnumeration.IDLE);
                Notifier.showErrorNotification(notificationTitle, gcodePostProcessFailedNotification);
            }
        };

        succeededGCodePostProcessEventHandler = (WorkerStateEvent t) -> {
            GCodePostProcessingResult result = (GCodePostProcessingResult) (t.getSource().getValue());
            
            if (result.isSuccess()) {
                steno.info(t.getSource().getTitle() + " has succeeded");
                String jobUUID = result.getPrintJobUUID();
                
                gcodePrintService.reset();
                gcodePrintService.setCurrentPrintJobID(jobUUID);
                gcodePrintService.setModelFileToPrint(result.getOutputFilename());
                gcodePrintService.setPrinterToUse(result.getPrinterToUse());
                gcodePrintService.start();
                
                File gcodeFromPrintJob = new File(result.getOutputFilename());
                int numberOfLines = SystemUtils.countLinesInFile(gcodeFromPrintJob, ";");
                linesInPrintingFile.set(numberOfLines);
                Notifier.showInformationNotification(notificationTitle, gcodePostProcessSuccessfulNotification);
                setPrintStatus(PrinterStatusEnumeration.SENDING_TO_PRINTER);
            } else {
                Notifier.showErrorNotification(notificationTitle, gcodePostProcessFailedNotification);
                setPrintStatus(PrinterStatusEnumeration.IDLE);
            }
        };

        cancelPrintEventHandler = new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent t) {
                steno.info(t.getSource().getTitle() + " has been cancelled");
                Notifier.showInformationNotification(notificationTitle, printJobCancelledNotification);
                setPrintStatus(PrinterStatusEnumeration.IDLE);
            }
        };

        failedPrintEventHandler = new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent t) {
                steno.error(t.getSource().getTitle() + " has failed");
                Notifier.showErrorNotification(notificationTitle, printJobFailedNotification);
                setPrintStatus(PrinterStatusEnumeration.IDLE);
            }
        };

        succeededPrintEventHandler = new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent t) {
                boolean succeeded = (boolean) (t.getSource().getValue());
                if (succeeded) {
                    steno.info(t.getSource().getTitle() + " has succeeded");
                    Notifier.showInformationNotification(notificationTitle, printTransferSuccessfulNotification + " " + associatedPrinter.getPrinterFriendlyName());
                    setPrintStatus(PrinterStatusEnumeration.PRINTING);
                } else {
                    Notifier.showErrorNotification(notificationTitle, printJobFailedNotification);
                    steno.error("Submission of job to printer failed");
                    abortPrint();
                }
                sendingDataToPrinter.set(false);
            }
        };

        printJobIDListener = new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> ov, String oldValue, String newValue) {
//                steno.info("Print job ID number is " + newValue + " and was " + oldValue);

                detectAlreadyPrinting();
            }
        };

        printLineNumberListener = new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> ov, Number oldValue, Number newValue) {
//                steno.info("Line number is " + newValue.toString() + " and was " + oldValue.toString());
                switch (printState) {
                    case IDLE:
                    case SENDING_TO_PRINTER:
//Ignore this state...
                        break;
                    case PRINTING:
                        if (linesInPrintingFile.get() > 0) {
                            double percentDone = newValue.doubleValue() / linesInPrintingFile.doubleValue();
                            primaryProgressPercent.set(percentDone);
                        }
                        break;
                    default:
                        break;
                }
            }
        };

        slicerService.setOnCancelled(cancelSliceEventHandler);

        slicerService.setOnFailed(failedSliceEventHandler);

        slicerService.setOnSucceeded(succeededSliceEventHandler);

        gcodePostProcessorService.setOnCancelled(cancelGCodePostProcessEventHandler);

        gcodePostProcessorService.setOnFailed(failedGCodePostProcessEventHandler);

        gcodePostProcessorService.setOnSucceeded(succeededGCodePostProcessEventHandler);

        gcodePrintService.setOnCancelled(cancelPrintEventHandler);

        gcodePrintService.setOnFailed(failedPrintEventHandler);

        gcodePrintService.setOnSucceeded(succeededPrintEventHandler);

        setPrintStatus(PrinterStatusEnumeration.IDLE);

        associatedPrinter.printJobLineNumberProperty().addListener(printLineNumberListener);
        associatedPrinter.printJobIDProperty().addListener(printJobIDListener);
    }

    private void detectAlreadyPrinting() {
        boolean roboxIsPrinting = false;

        if (associatedPrinter != null) {
            String printJobID = associatedPrinter.printJobIDProperty().get();
            if (printJobID != null) {
                if (printJobID.codePointAt(0) != 0) {
                    roboxIsPrinting = true;
                }
            }

            switch (printState) {
                case IDLE:
                    if (roboxIsPrinting) {
                        //We've detected a print job when we're idle...
                        //Try to find the print job and determine how many lines there were in it

                        File gcodeFromPrintJob = new File(ApplicationConfiguration.getPrintSpoolDirectory() + associatedPrinter.getPrintJobID() + File.separator + associatedPrinter.getPrintJobID() + ApplicationConfiguration.gcodeTempFileExtension);
                        int numberOfLines = SystemUtils.countLinesInFile(gcodeFromPrintJob, ";");
                        linesInPrintingFile.set(numberOfLines);

                        double percentDone = -1;
                        if (linesInPrintingFile.get() > 0) {
                            percentDone = (double) associatedPrinter.getPrintJobLineNumber() / numberOfLines;
                        }

                        primaryProgressPercent.set(percentDone);
//                            fxToJMEInterface.exposeGCodeModel(percentDone);
                        Notifier.showInformationNotification(notificationTitle, detectedPrintInProgressNotification);

                        if (associatedPrinter.pausedProperty().get() == true) {
                            setPrintStatus(PrinterStatusEnumeration.PAUSED);
                        } else {
                            setPrintStatus(PrinterStatusEnumeration.PRINTING);
                        }
                    }
                    break;
                case SENDING_TO_PRINTER:
                case PRINTING:
                    if (roboxIsPrinting == false) {
//                            fxToJMEInterface.exposeGCodeModel(0);
                        setPrintStatus(PrinterStatusEnumeration.IDLE);
                    }
                    break;
                default:
                    break;
            }
        }
    }

    public void shutdown() {
    }

    /*
     * Properties
     */
    public synchronized boolean printProject(Project project, PrintQualityEnumeration printQuality, RoboxProfile settings) {
        boolean acceptedPrintRequest = false;

        if (printState == PrinterStatusEnumeration.IDLE) {
            //Create the print job directory
            String printUUID = SystemUtils.generate16DigitID();

            String printJobDirectoryName = ApplicationConfiguration.getPrintSpoolDirectory() + printUUID;

            //TODO PUT SOMETHING HERE TO GET RID OF OLD PRINT FILES
            File printJobDirectory = new File(printJobDirectoryName);
            printJobDirectory.mkdirs();

            PrintJob printJob = new PrintJob(printUUID, printQuality, settings);

            if (project.getProjectMode() == ProjectMode.MESH) {

                //Write out the slicer config
                settings.filament_diameterProperty().set((float) 1.1283791670955125738961589031215);
                settings.writeToFile(printJobDirectoryName + File.separator + printUUID + ApplicationConfiguration.printProfileFileExtension);

                setPrintStatus(PrinterStatusEnumeration.SLICING);
                slicerService.reset();
                slicerService.setProject(project);
                slicerService.setSettings(settings);
                slicerService.setPrintJobUUID(printUUID);
                slicerService.setPrinterToUse(associatedPrinter);
                slicerService.start();

//            fxToJMEInterface.clearGCodeDisplay();
                // Do we need to slice?
                acceptedPrintRequest = true;
            } else if (project.getProjectMode() == ProjectMode.GCODE) {
                String printjobFilename = ApplicationConfiguration.getPrintSpoolDirectory() + printUUID + File.separator + printUUID + ApplicationConfiguration.gcodeTempFileExtension;
                String fileToCopyname = project.getGCodeFilename();
                File printjobFile = new File(printjobFilename);
                File fileToCopy = new File(fileToCopyname);
                try {
                    Files.copy(fileToCopy.toPath(), printjobFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

                    gcodePrintService.reset();
                    gcodePrintService.setCurrentPrintJobID(printUUID);
                    gcodePrintService.setModelFileToPrint(printjobFilename);
                    gcodePrintService.setPrinterToUse(associatedPrinter);
                    gcodePrintService.start();
                    setPrintStatus(PrinterStatusEnumeration.SENDING_TO_PRINTER);
                    acceptedPrintRequest = true;
                } catch (IOException ex) {
                    steno.error("Error whilt preparing for print. Can't copy " + fileToCopyname + " to " + printjobFilename);
                }
            }
        }

        return acceptedPrintRequest;
    }

    public PrinterStatusEnumeration getPrintStatus() {
        return printState;
    }

    private void setPrintStatus(PrinterStatusEnumeration newState) {
        switch (newState) {
            case IDLE:
                printProgressMessage.unbind();
                setPrintProgressMessage("");
                primaryProgressPercent.unbind();
                setPrimaryProgressPercent(0);
                secondaryProgressPercent.unbind();
                setSecondaryProgressPercent(0);
                sendingDataToPrinter.set(false);
                setPrintInProgress(false);
                setDialogRequired(false);
                if (associatedPrinter != null) {
                    associatedPrinter.printJobIDProperty().unbind();
                    associatedPrinter.printJobLineNumberProperty().unbind();
                }
                break;
            case SLICING:
                printProgressMessage.unbind();
                printProgressMessage.bind(slicerService.messageProperty());
                primaryProgressPercent.unbind();
                setPrimaryProgressPercent(0);
                primaryProgressPercent.bind(slicerService.progressProperty());
                secondaryProgressPercent.unbind();
                setSecondaryProgressPercent(0);
                sendingDataToPrinter.set(false);
                setPrintInProgress(true);
                setDialogRequired(true);
                break;
            case POST_PROCESSING:
                printProgressMessage.unbind();
                printProgressMessage.bind(gcodePostProcessorService.messageProperty());
                primaryProgressPercent.unbind();
                setPrimaryProgressPercent(0);
                primaryProgressPercent.bind(gcodePostProcessorService.progressProperty());
                secondaryProgressPercent.unbind();
                sendingDataToPrinter.set(false);
                setSecondaryProgressPercent(0);
                setPrintInProgress(true);
                setDialogRequired(true);
                break;
            case SENDING_TO_PRINTER:
                printProgressMessage.unbind();
                printProgressMessage.bind(gcodePrintService.messageProperty());
                primaryProgressPercent.unbind();
                setPrimaryProgressPercent(0);
                secondaryProgressPercent.unbind();
                setSecondaryProgressPercent(0);
                secondaryProgressPercent.bind(gcodePrintService.progressProperty());
                sendingDataToPrinter.set(true);
                setPrintInProgress(true);
                setDialogRequired(true);
                break;
            case PAUSED:
                setPrintInProgress(true);
                setDialogRequired(false);
                break;
            case PRINTING:
                printProgressMessage.unbind();
                primaryProgressPercent.unbind();
                if (printState != PrinterStatusEnumeration.PAUSED) {
                    setPrimaryProgressPercent(0);
                }
                printProgressMessage.set("");
                setPrintInProgress(true);
                setDialogRequired(false);
                break;
            default:
                break;
        }
        setPrintProgressTitle(newState.getDescription());
        printState = newState;
    }

    private void setDialogRequired(boolean value) {
        dialogRequired.set(value);
    }

    private void setPrintInProgress(boolean value) {
        printInProgress.set(value);
    }

    public BooleanProperty printInProgressProperty() {
        return printInProgress;
    }

    private void setPrintProgressMessage(String value) {
        printProgressMessage.set(value);
    }

    private void setPrintProgressTitle(String value) {
        printProgressTitle.set(value);
    }

    private void setPrimaryProgressPercent(double value) {
        primaryProgressPercent.set(value);
    }

    private void setSecondaryProgressPercent(double value) {
        secondaryProgressPercent.set(value);
    }

    public ReadOnlyDoubleProperty secondaryProgressProperty() {
        return secondaryProgressPercent;
    }

    public ReadOnlyBooleanProperty sendingDataToPrinterProperty() {
        return sendingDataToPrinter;
    }

    @Override
    public ReadOnlyBooleanProperty runningProperty() {
        return dialogRequired;
    }

    @Override
    public ReadOnlyStringProperty messageProperty() {
        return printProgressMessage;
    }

    @Override
    public ReadOnlyDoubleProperty progressProperty() {
        return primaryProgressPercent;
    }

    @Override
    public ReadOnlyStringProperty titleProperty() {
        return printProgressTitle;
    }

    public void pausePrint() {
        switch (printState) {
            case SENDING_TO_PRINTER:
            case PRINTING:
                lastStateBeforePause = printState;
                try {
                    associatedPrinter.transmitPausePrint();
                    setPrintStatus(PrinterStatusEnumeration.PAUSED);
                } catch (RoboxCommsException ex) {
                    steno.error("Robox comms exception when sending pause print command " + ex);
                }
                break;
            default:
                steno.warning("Attempt to pause print in print state " + printState);
                break;
        }
    }

    public void resumePrint() {
        if (associatedPrinter.pausedProperty().get() == true) {
            try {
                associatedPrinter.transmitResumePrint();

                setPrintStatus(lastStateBeforePause);
            } catch (RoboxCommsException ex) {
                steno.error("Robox comms exception when sending resume print command " + ex);
            }
        }
    }

    public boolean abortPrint() {
        boolean cancelledRun = false;

        switch (printState) {
            case SLICING:
                if (slicerService.isRunning()) {
                    slicerService.cancelRun();
                    setPrintStatus(PrinterStatusEnumeration.IDLE);
                    cancelledRun = true;
                }
                break;
            case POST_PROCESSING:
                if (gcodePostProcessorService.isRunning()) {
                    gcodePostProcessorService.cancelRun();
                    setPrintStatus(PrinterStatusEnumeration.IDLE);
                    cancelledRun = true;
                }
                break;
            case PAUSED:
            case SENDING_TO_PRINTER:
            case PRINTING:
                if (gcodePrintService.isRunning()) {
                    gcodePrintService.cancelRun();
                }
                try {
                    associatedPrinter.transmitAbortPrint();
                } catch (RoboxCommsException ex) {
                    steno.error("Robox comms exception when sending abort print command " + ex);
                }
                setPrintStatus(PrinterStatusEnumeration.IDLE);
//                fxToJMEInterface.clearGCodeDisplay();
                cancelledRun = true;
                break;
            default:
                steno.warning("Attempt to abort print in print state " + printState);
                break;
        }

        if (cancelledRun) {
            try {
                associatedPrinter.transmitStoredGCode("abort_print");
            } catch (RoboxCommsException ex) {
                steno.error("Robox comms exception when sending abort print gcode " + ex);
            }
        }
        return cancelledRun;
    }

    @Override
    public boolean cancelRun() {
        return false;
    }

    public void printerHasPaused() {
        setPrintStatus(PrinterStatusEnumeration.PAUSED);
    }

    public void printerIsPrinting() {
        setPrintStatus(PrinterStatusEnumeration.PRINTING);
    }

    public ReadOnlyIntegerProperty linesInPrintingFileProperty() {
        return linesInPrintingFile;
    }
}
