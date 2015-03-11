package celtech.printerControl.model;

import celtech.Lookup;
import celtech.appManager.Project;
import celtech.configuration.ApplicationConfiguration;
import celtech.configuration.PauseStatus;
import celtech.configuration.SlicerType;
import celtech.configuration.fileRepresentation.SlicerParametersFile;
import celtech.gcodetranslator.PrintJobStatistics;
import celtech.printerControl.PrintJob;
import celtech.printerControl.PrinterStatus;
import celtech.printerControl.comms.commands.exceptions.RoboxCommsException;
import celtech.printerControl.comms.commands.rx.ListFilesResponse;
import celtech.services.ControllableService;
import celtech.services.postProcessor.GCodePostProcessingResult;
import celtech.services.postProcessor.PostProcessorService;
import celtech.services.printing.GCodePrintResult;
import celtech.services.printing.GCodePrintService;
import celtech.services.roboxmoviemaker.MovieMakerTask;
import celtech.services.slicer.AbstractSlicerService;
import celtech.services.slicer.PrintQualityEnumeration;
import celtech.services.slicer.SliceResult;
import celtech.configuration.slicer.SlicerConfigWriter;
import celtech.configuration.slicer.SlicerConfigWriterFactory;
import celtech.printerControl.MacroType;
import celtech.printerControl.comms.commands.MacroLoadException;
import celtech.printerControl.comms.commands.GCodeMacros;
import celtech.printerControl.comms.commands.MacroPrintException;
import celtech.services.slicer.SlicerService;
import celtech.utils.SystemUtils;
import celtech.utils.threed.ThreeDUtils;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;
import org.apache.commons.io.FileDeleteStrategy;
import org.apache.commons.io.FileUtils;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

/**
 *
 * @author ianhudson
 */
public class PrintEngine implements ControllableService
{

    private final Stenographer steno = StenographerFactory.getStenographer(
        PrintEngine.class.getName());

    private Printer associatedPrinter = null;
    private AbstractSlicerService slicerService;
    private final PostProcessorService gcodePostProcessorService = new PostProcessorService();
    private final GCodePrintService gcodePrintService = new GCodePrintService();
    private final IntegerProperty linesInPrintingFile = new SimpleIntegerProperty(0);

    /**
     * Indicates if ETC data is available for the current print
     */
    private final BooleanProperty etcAvailable = new SimpleBooleanProperty(false);
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
    private EventHandler<WorkerStateEvent> scheduledPrintEventHandler = null;

    private final StringProperty printProgressTitle = new SimpleStringProperty();
    private final StringProperty printProgressMessage = new SimpleStringProperty();
    private final BooleanProperty dialogRequired = new SimpleBooleanProperty(
        false);
    private final BooleanProperty printInProgress = new SimpleBooleanProperty(
        false);
    private final DoubleProperty primaryProgressPercent = new SimpleDoubleProperty(
        0);
    private final DoubleProperty secondaryProgressPercent = new SimpleDoubleProperty(
        0);
    private final BooleanProperty sendingDataToPrinter = new SimpleBooleanProperty(
        false);
    private final ObjectProperty<Date> printJobStartTime = new SimpleObjectProperty<>();
    /*
     * 
     */
    private ChangeListener<Number> printLineNumberListener = null;
    private ChangeListener<String> printJobIDListener = null;

    private final Map<String, Project> printJobsAgainstProjects = new HashMap<>();

    private boolean consideringPrintRequest = false;
    ETCCalculator etcCalculator;
    /**
     * progressETC holds the number of seconds predicted for the ETC of the print
     */
    private final IntegerProperty progressETC = new SimpleIntegerProperty();
    /**
     * The current layer being processed
     */
    private final IntegerProperty progressCurrentLayer = new SimpleIntegerProperty();
    /**
     * The total number of layers in the model being printed
     */
    private final IntegerProperty progressNumLayers = new SimpleIntegerProperty();

    /**
     * The movie maker task
     */
    private MovieMakerTask movieMakerTask = null;

    public PrintEngine(Printer associatedPrinter)
    {
        this(associatedPrinter, new SlicerService());
    }

    public PrintEngine(Printer associatedPrinter,
        AbstractSlicerService slicerService)
    {
        this.associatedPrinter = associatedPrinter;
        this.slicerService = slicerService;

        cancelSliceEventHandler = (WorkerStateEvent t) ->
        {
            steno.info(t.getSource().getTitle() + " has been cancelled");
            try
            {
                associatedPrinter.cancel(null);
            } catch (PrinterException ex)
            {
                steno.error("Couldn't abort on slice cancel");
            }
        };

        failedSliceEventHandler = (WorkerStateEvent t) ->
        {
            steno.info(t.getSource().getTitle() + " has failed");
            Lookup.getSystemNotificationHandler().showSliceFailedNotification();
            try
            {
                associatedPrinter.cancel(null);
            } catch (PrinterException ex)
            {
                steno.error("Couldn't abort on slice fail");
            }
        };

        succeededSliceEventHandler = (WorkerStateEvent t) ->
        {
            SliceResult result = (SliceResult) (t.getSource().getValue());

            if (result.isSuccess())
            {
                steno.info(t.getSource().getTitle() + " has succeeded");
                gcodePostProcessorService.reset();
                gcodePostProcessorService.setPrintJobUUID(
                    result.getPrintJobUUID());
                gcodePostProcessorService.setSettings(result.getSettings());
                gcodePostProcessorService.setPrinterToUse(
                    result.getPrinterToUse());
                gcodePostProcessorService.start();

                Lookup.getSystemNotificationHandler().showSliceSuccessfulNotification();

                associatedPrinter.setPrinterStatus(PrinterStatus.POST_PROCESSING);
            } else
            {
                Lookup.getSystemNotificationHandler().showSliceFailedNotification();
                try
                {
                    associatedPrinter.cancel(null);
                } catch (PrinterException ex)
                {
                    steno.error("Couldn't abort on slice fail");
                }
            }
        };

        cancelGCodePostProcessEventHandler = (WorkerStateEvent t) ->
        {
            steno.info(t.getSource().getTitle() + " has been cancelled");
            try
            {
                associatedPrinter.cancel(null);
            } catch (PrinterException ex)
            {
                steno.error("Couldn't abort on post process cancel");
            }
        };

        failedGCodePostProcessEventHandler = (WorkerStateEvent t) ->
        {
            steno.info(t.getSource().getTitle() + " has failed");
            Lookup.getSystemNotificationHandler().showGCodePostProcessFailedNotification();
            try
            {
                associatedPrinter.cancel(null);
            } catch (PrinterException ex)
            {
                steno.error("Couldn't abort on post process fail");
            }
        };

        succeededGCodePostProcessEventHandler = (WorkerStateEvent t) ->
        {
            GCodePostProcessingResult result = (GCodePostProcessingResult) (t.getSource().
                getValue());

            if (result.getRoboxiserResult().isSuccess())
            {
                steno.info(t.getSource().getTitle() + " has succeeded");
                String jobUUID = result.getPrintJobUUID();

                Project project = printJobsAgainstProjects.get(jobUUID);
                project.setLastPrintJobID(jobUUID);

                PrintJobStatistics printJobStatistics = result.getRoboxiserResult().
                    getPrintJobStatistics();

                makeETCCalculator(printJobStatistics, associatedPrinter);

                gcodePrintService.reset();
                gcodePrintService.setCurrentPrintJobID(jobUUID);
                gcodePrintService.setStartFromSequenceNumber(0);
                gcodePrintService.setModelFileToPrint(result.getOutputFilename());
                gcodePrintService.setPrinterToUse(result.getPrinterToUse());
                gcodePrintService.start();

                printJobStartTime.set(new Date());

                Lookup.getSystemNotificationHandler().showGCodePostProcessSuccessfulNotification();

                associatedPrinter.setPrinterStatus(PrinterStatus.SENDING_TO_PRINTER);
            } else
            {
                Lookup.getSystemNotificationHandler().showGCodePostProcessFailedNotification();
                try
                {
                    associatedPrinter.cancel(null);
                } catch (PrinterException ex)
                {
                    steno.error("Couldn't abort on post process fail");
                }

            }
        };

        cancelPrintEventHandler = (WorkerStateEvent t) ->
        {
            steno.info(t.getSource().getTitle() + " has been cancelled");
            Lookup.getSystemNotificationHandler().showPrintJobCancelledNotification();
        };

        failedPrintEventHandler = (WorkerStateEvent t) ->
        {
            steno.error(t.getSource().getTitle() + " has failed");
            Lookup.getSystemNotificationHandler().showPrintJobFailedNotification();
            try
            {
                associatedPrinter.cancel(null);
            } catch (PrinterException ex)
            {
                steno.error("Couldn't abort on print job fail");
            }
        };

        succeededPrintEventHandler = (WorkerStateEvent t) ->
        {
            GCodePrintResult result = (GCodePrintResult) (t.getSource().getValue());
            if (result.isSuccess())
            {
                steno.info(t.getSource().getTitle() + " has succeeded");
                if (associatedPrinter.macroTypeProperty().isNotNull().get())
                {
                    associatedPrinter.setPrinterStatus(PrinterStatus.EXECUTING_MACRO);
                    //Remove the print job from disk
                    String printjobFilename = ApplicationConfiguration.
                        getPrintSpoolDirectory() + result.getPrintJobID();
                    File directoryToDelete = new File(printjobFilename);
                    try
                    {
                        FileDeleteStrategy.FORCE.delete(directoryToDelete);
                    } catch (IOException ex)
                    {
                        steno.error(
                            "Error whilst deleting macro print directory "
                            + printjobFilename + " exception - "
                            + ex.getMessage());
                    }
                } else
                {
                    Lookup.getSystemNotificationHandler().showPrintTransferSuccessfulNotification(
                        associatedPrinter.getPrinterIdentity().printerFriendlyNameProperty().get());
                    associatedPrinter.setPrinterStatus(PrinterStatus.PRINTING);
                }
            } else
            {
                Lookup.getSystemNotificationHandler().showPrintTransferFailedNotification(
                    associatedPrinter.getPrinterIdentity().printerFriendlyNameProperty().get());
                steno.error("Submission of job to printer failed");
                try
                {
                    //TODO - can't submit in this case...?
                    associatedPrinter.cancel(null);
                } catch (PrinterException ex)
                {
                    steno.error("Couldn't abort on print job failed to submit");
                }
            }
            sendingDataToPrinter.set(false);
        };

        printJobIDListener = (ObservableValue<? extends String> ov, String oldValue, String newValue) ->
        {
            detectAlreadyPrinting();
        };

        scheduledPrintEventHandler = (WorkerStateEvent t) ->
        {
            steno.info(t.getSource().getTitle() + " has been scheduled");
            if (associatedPrinter.macroTypeProperty().get() == MacroType.GCODE_PRINT
                || associatedPrinter.macroTypeProperty().get() == null)
            {
                Lookup.getSystemNotificationHandler().showPrintTransferInitiatedNotification();
            }
        };

        printLineNumberListener = new ChangeListener<Number>()
        {
            @Override
            public void changed(ObservableValue<? extends Number> ov,
                Number oldValue,
                Number newValue)
            {
//                steno.info("Line number is " + newValue.toString() + " and was " + oldValue.toString());
//                System.out.println("Line number changed to " + newValue);
                switch (associatedPrinter.printerStatusProperty().get())
                {
                    case IDLE:
//Ignore this state...
                        break;
                    case PRINTING:
                    case SENDING_TO_PRINTER:
                        if (etcAvailable.get())
                        {
                            updateETCUsingETCCalculator(newValue);
                        } else
                        {
                            updateETCUsingLineNumber(newValue);
                        }
                        break;
                    case EXECUTING_MACRO:
                        updateETCUsingLineNumber(newValue);
                        break;
                    default:
                        break;
                }
            }

            private void updateETCUsingLineNumber(Number newValue)
            {
                if (linesInPrintingFile.get() > 0)
                {
                    double percentDone = newValue.doubleValue()
                        / linesInPrintingFile.doubleValue();
                    primaryProgressPercent.set(percentDone);
                }
            }
        };

        slicerService.setOnCancelled(cancelSliceEventHandler);

        slicerService.setOnFailed(failedSliceEventHandler);

        slicerService.setOnSucceeded(succeededSliceEventHandler);

        gcodePostProcessorService.setOnCancelled(
            cancelGCodePostProcessEventHandler);

        gcodePostProcessorService.setOnFailed(failedGCodePostProcessEventHandler);

        gcodePostProcessorService.setOnSucceeded(
            succeededGCodePostProcessEventHandler);

        gcodePrintService.setOnScheduled(scheduledPrintEventHandler);

        gcodePrintService.setOnCancelled(cancelPrintEventHandler);

        gcodePrintService.setOnFailed(failedPrintEventHandler);

        gcodePrintService.setOnSucceeded(succeededPrintEventHandler);

        associatedPrinter.printJobLineNumberProperty().addListener(printLineNumberListener);
        associatedPrinter.printJobIDProperty().addListener(printJobIDListener);
    }

    /**
     * Create the ETCCalculator based on the given PrintJobStatistics.
     */
    private void makeETCCalculator(PrintJobStatistics printJobStatistics,
        Printer associatedPrinter)
    {
        int numberOfLines = printJobStatistics.getNumberOfLines();
        linesInPrintingFile.set(numberOfLines);
        List<Double> layerNumberToPredictedDuration = printJobStatistics.
            getLayerNumberToPredictedDuration();
        List<Integer> layerNumberToLineNumber = printJobStatistics.getLayerNumberToLineNumber();
        etcCalculator = new ETCCalculator(associatedPrinter,
                                          layerNumberToPredictedDuration, layerNumberToLineNumber);

        progressNumLayers.set(layerNumberToLineNumber.size());
        primaryProgressPercent.unbind();
        primaryProgressPercent.set(0);
        progressETC.set(etcCalculator.getETCPredicted(0));
        etcAvailable.set(true);
    }

    private void updateETCUsingETCCalculator(Number newValue)
    {
        int lineNumber = newValue.intValue();
        primaryProgressPercent.set(etcCalculator.getPercentCompleteAtLine(lineNumber));
        progressETC.set(etcCalculator.getETCPredicted(lineNumber));
        progressCurrentLayer.set(etcCalculator.getCompletedLayerNumberForLineNumber(lineNumber));
    }

    public void makeETCCalculatorForJobOfUUID(String printJobID)
    {
        PrintJob printJob = PrintJob.readJobFromDirectory(printJobID);
        try
        {
            makeETCCalculator(printJob.getStatistics(), associatedPrinter);
        } catch (IOException ex)
        {
            etcAvailable.set(false);
        }
    }

    /**
     *
     */
    public void shutdown()
    {
        stopAllServices();
    }

    /*
     * Properties
     */
    /**
     *
     * @param project
     * @param printQuality
     * @param settings
     * @return
     */
    public synchronized boolean printProject(Project project, PrintQualityEnumeration printQuality,
        SlicerParametersFile settings)
    {
        boolean acceptedPrintRequest = false;
        etcAvailable.set(false);

        if (associatedPrinter.printerStatusProperty().get() == PrinterStatus.IDLE)
        {
            boolean printFromScratchRequired = false;

            if (project.getLastPrintJobID() != null)
            {
                String jobUUID = project.getLastPrintJobID();
                PrintJob printJob = PrintJob.readJobFromDirectory(jobUUID);

                //Reprint the last job
                //Is it still on the printer?
                try
                {
                    ListFilesResponse listFilesResponse = associatedPrinter.
                        transmitListFiles();
                    if (listFilesResponse.getPrintJobIDs().contains(jobUUID))
                    {
                        acceptedPrintRequest = reprintDirectFromPrinter(printJob);
                    } else
                    {
                        //Need to send the file to the printer
                        //Is it still on disk?

                        if (printJob.roboxisedFileExists())
                        {
                            acceptedPrintRequest = reprintFileFromDisk(printJob);
                        } else
                        {
                            printFromScratchRequired = true;
                            steno.error(
                                "Print job " + jobUUID
                                + " not found on printer or disk - going ahead with print from scratch");
                        }
                    }

                    try
                    {
                        makeETCCalculator(printJob.getStatistics(), associatedPrinter);
                    } catch (IOException ex)
                    {
                        etcAvailable.set(false);
                    }
                } catch (RoboxCommsException ex)
                {
                    printFromScratchRequired = true;
                    steno.error(
                        "Error whilst attempting to list files on printer - going ahead with print from scratch");
                }
            } else
            {
                printFromScratchRequired = true;
            }

            if (printFromScratchRequired)
            {
                acceptedPrintRequest = printFromScratch(printQuality, settings,
                                                        project,
                                                        acceptedPrintRequest);
            }

//            movieMakerTask = new MovieMakerTask(project.getUUID(), associatedPrinter);
//            movieMakerTask.setOnSucceeded(new EventHandler<WorkerStateEvent>()
//            {
//
//                @Override
//                public void handle(WorkerStateEvent event)
//                {
//                    steno.info("Movie maker succeeded");
//                }
//            });
//            movieMakerTask.setOnFailed(new EventHandler<WorkerStateEvent>()
//            {
//
//                @Override
//                public void handle(WorkerStateEvent event)
//                {
//                    steno.info("Movie maker failed");
//                }
//            });
//            movieMakerTask.setOnCancelled(new EventHandler<WorkerStateEvent>()
//            {
//
//                @Override
//                public void handle(WorkerStateEvent event)
//                {
//                    steno.info("Movie maker was cancelled");
//                }
//            });
//
//            TaskController.getInstance().manageTask(movieMakerTask);
//
//            Thread movieThread = new Thread(movieMakerTask);
//            movieThread.setName("Movie Maker - " + project.getUUID());
//            movieThread.start();
        }

        return acceptedPrintRequest;
    }

    private boolean printFromScratch(PrintQualityEnumeration printQuality,
        SlicerParametersFile settings, Project project, boolean acceptedPrintRequest)
    {
        //Create the print job directory
        String printUUID = SystemUtils.generate16DigitID();
        String printJobDirectoryName = ApplicationConfiguration.
            getPrintSpoolDirectory() + printUUID;
        File printJobDirectory = new File(printJobDirectoryName);
        printJobDirectory.mkdirs();
        //Erase old print job directories
        File printSpoolDirectory = new File(
            ApplicationConfiguration.getPrintSpoolDirectory());
        File[] filesOnDisk = printSpoolDirectory.listFiles();
        if (filesOnDisk.length > ApplicationConfiguration.maxPrintSpoolFiles)
        {
            int filesToDelete = filesOnDisk.length
                - ApplicationConfiguration.maxPrintSpoolFiles;
            Arrays.sort(filesOnDisk, (File f1, File f2) -> Long.valueOf(
                        f1.lastModified()).compareTo(f2.lastModified()));
            for (int i = 0; i < filesToDelete; i++)
            {
                FileUtils.deleteQuietly(filesOnDisk[i]);
            }
        }

//        if (project.getProjectMode() == ProjectMode.MESH)
//        {
            //Write out the slicer config
            SlicerType slicerTypeToUse = null;
            if (settings.getSlicerOverride() != null)
            {
                slicerTypeToUse = settings.getSlicerOverride();
            } else
            {
                slicerTypeToUse = Lookup.getUserPreferences().getSlicerType();
            }

            SlicerConfigWriter configWriter = SlicerConfigWriterFactory.getConfigWriter(
                slicerTypeToUse);

            //We need to tell the slicers where the centre of the printed objects is - otherwise everything is put in the centre of the bed...
            Vector3D centreOfPrintedObject = project.getCentreOfLoadedModels();
            configWriter.setPrintCentre((float) (centreOfPrintedObject.getX()
                + ApplicationConfiguration.xPrintOffset),
                                        (float) (centreOfPrintedObject.getZ()
                                        + ApplicationConfiguration.yPrintOffset));
            configWriter.generateConfigForSlicer(settings,
                                                 printJobDirectoryName
                                                 + File.separator
                                                 + printUUID
                                                 + ApplicationConfiguration.printProfileFileExtension);

            associatedPrinter.setPrinterStatus(PrinterStatus.SLICING);
            slicerService.reset();
            slicerService.setProject(project);
            slicerService.setSettings(settings);
            slicerService.setPrintJobUUID(printUUID);
            slicerService.setPrinterToUse(associatedPrinter);
            slicerService.start();

            printJobsAgainstProjects.put(printUUID, project);

            // Do we need to slice?
            acceptedPrintRequest = true;
//        } else if (project.getProjectMode() == ProjectMode.GCODE)
//        {
//            String printjobFilename = ApplicationConfiguration.
//                getPrintSpoolDirectory() + printUUID + File.separator
//                + printUUID + ApplicationConfiguration.gcodeTempFileExtension;
//            String fileToCopyname = project.getGCodeFilename();
//            File printjobFile = new File(printjobFilename);
//            File fileToCopy = new File(fileToCopyname);
//            try
//            {
//                Files.copy(fileToCopy.toPath(), printjobFile.toPath(),
//                           StandardCopyOption.REPLACE_EXISTING);
//                associatedPrinter.setPrinterStatus(PrinterStatus.SENDING_TO_PRINTER);
//
//                gcodePrintService.reset();
//                gcodePrintService.setCurrentPrintJobID(printUUID);
//                gcodePrintService.setModelFileToPrint(printjobFilename);
//                gcodePrintService.setPrinterToUse(associatedPrinter);
//                gcodePrintService.start();
//                acceptedPrintRequest = true;
//            } catch (IOException ex)
//            {
//                steno.error(
//                    "Error whilst preparing for print. Can't copy "
//                    + fileToCopyname + " to " + printjobFilename);
//            }
//        }
        return acceptedPrintRequest;
    }

    private boolean reprintFileFromDisk(PrintJob printJob, int startFromLineNumber)
    {
        String gCodeFileName = printJob.getRoboxisedFileLocation();
        String jobUUID = printJob.getJobUUID();
        boolean acceptedPrintRequest;
        try
        {
            linesInPrintingFile.set(printJob.getStatistics().getNumberOfLines());
        } catch (IOException ex)
        {
            steno.error("Couldn't get job statistics for job " + jobUUID);
        }
        associatedPrinter.setPrinterStatus(PrinterStatus.SENDING_TO_PRINTER);
        steno.info("Respooling job " + jobUUID + " to printer from line " + startFromLineNumber);
        gcodePrintService.reset();
        gcodePrintService.setCurrentPrintJobID(jobUUID);
        gcodePrintService.setStartFromSequenceNumber(startFromLineNumber);
        gcodePrintService.setModelFileToPrint(gCodeFileName);
        gcodePrintService.setPrinterToUse(associatedPrinter);
        gcodePrintService.start();
        acceptedPrintRequest = true;
        return acceptedPrintRequest;
    }

    private boolean reprintFileFromDisk(PrintJob printJob)
    {
        return reprintFileFromDisk(printJob, 0);
    }

    private boolean reprintDirectFromPrinter(PrintJob printJob) throws RoboxCommsException
    {
        boolean acceptedPrintRequest;
        //Reprint directly from printer
        steno.info("Printing job " + printJob.getJobUUID() + " from printer store");
        Lookup.getSystemNotificationHandler().showReprintStartedNotification();

        if (printJob.roboxisedFileExists())
        {
            try
            {
                linesInPrintingFile.set(printJob.getStatistics().getNumberOfLines());
            } catch (IOException ex)
            {
                steno.error("Couldn't get job statistics for job " + printJob.getJobUUID());
            }
        }
        associatedPrinter.initiatePrint(printJob.getJobUUID());
        associatedPrinter.setPrinterStatus(PrinterStatus.PRINTING);
        acceptedPrintRequest = true;
        return acceptedPrintRequest;
    }

    private void setPrintInProgress(boolean value)
    {
        printInProgress.set(value);
    }

    /**
     *
     * @return
     */
    public BooleanProperty printInProgressProperty()
    {
        return printInProgress;
    }

    private void setPrintProgressMessage(String value)
    {
        printProgressMessage.set(value);
    }

    private void setPrintProgressTitle(String value)
    {
        printProgressTitle.set(value);
    }

    private void setPrimaryProgressPercent(double value)
    {
        primaryProgressPercent.set(value);
    }

    private void setSecondaryProgressPercent(double value)
    {
        secondaryProgressPercent.set(value);
    }

    /**
     *
     * @return
     */
    public ReadOnlyDoubleProperty secondaryProgressProperty()
    {
        return secondaryProgressPercent;
    }

    /**
     *
     * @return
     */
    public ReadOnlyBooleanProperty sendingDataToPrinterProperty()
    {
        return sendingDataToPrinter;
    }

    /**
     *
     * @return
     */
    @Override
    public ReadOnlyBooleanProperty runningProperty()
    {
        return dialogRequired;
    }

    /**
     *
     * @return
     */
    @Override
    public ReadOnlyStringProperty messageProperty()
    {
        return printProgressMessage;
    }

    /**
     *
     * @return
     */
    @Override
    public ReadOnlyDoubleProperty progressProperty()
    {
        return primaryProgressPercent;
    }

    /**
     *
     * @return
     */
    @Override
    public ReadOnlyStringProperty titleProperty()
    {
        return printProgressTitle;
    }

    /**
     *
     * @return
     */
    @Override
    public boolean cancelRun()
    {
        return false;
    }

    /**
     *
     * @return
     */
    public ReadOnlyIntegerProperty linesInPrintingFileProperty()
    {
        return linesInPrintingFile;
    }

    /**
     *
     * @param filename
     * @param useSDCard
     * @return
     */
    protected boolean printGCodeFile(final String filename, final boolean useSDCard) throws MacroPrintException
    {
        boolean acceptedPrintRequest = false;
        consideringPrintRequest = true;

        //Create the print job directory
        String printUUID = createPrintJobDirectory();

        tidyPrintSpoolDirectory();

        String printjobFilename = ApplicationConfiguration.getPrintSpoolDirectory()
            + printUUID + File.separator + printUUID
            + ApplicationConfiguration.gcodeTempFileExtension;

        if (associatedPrinter.macroTypeProperty().isNotNull().get()
            && associatedPrinter.macroTypeProperty().get() == MacroType.GCODE_PRINT)
        {
            associatedPrinter.setPrinterStatus(PrinterStatus.SENDING_TO_PRINTER);
        }

        File printjobFile = new File(printjobFilename);
        FileReader reader = null;

        try
        {
            reader = new FileReader(filename);
            Scanner scanner = new Scanner(reader);

            while (scanner.hasNext())
            {
                String line = scanner.nextLine();
                if (GCodeMacros.isMacroExecutionDirective(line))
                {
                    FileUtils.writeLines(printjobFile, GCodeMacros.getMacroContents(line));
                } else
                {
                    FileUtils.writeStringToFile(printjobFile, line + "\r", true);
                }
            }
            reader.close();
        } catch (IOException | MacroLoadException ex)
        {
            throw new MacroPrintException(ex.getMessage());
        } finally
        {
            try
            {
                if (reader != null)
                {
                    reader.close();
                }
            } catch (IOException ex)
            {
                steno.error("Failed to create GCode print job");
            }
        }

        Lookup.getTaskExecutor().runOnGUIThread(() ->
        {
            int numberOfLines = SystemUtils.countLinesInFile(printjobFile, ";");
            linesInPrintingFile.set(numberOfLines);
            gcodePrintService.reset();
            gcodePrintService.setPrintUsingSDCard(useSDCard);
            gcodePrintService.setCurrentPrintJobID(printUUID);
            gcodePrintService.setModelFileToPrint(printjobFilename);
            gcodePrintService.setPrinterToUse(associatedPrinter);
            gcodePrintService.start();
            consideringPrintRequest = false;
        });

        acceptedPrintRequest = true;

        return acceptedPrintRequest;
    }

    private void tidyPrintSpoolDirectory()
    {
        //Erase old print job directories
        File printSpoolDirectory = new File(
            ApplicationConfiguration.getPrintSpoolDirectory());
        File[] filesOnDisk = printSpoolDirectory.listFiles();
        if (filesOnDisk.length > ApplicationConfiguration.maxPrintSpoolFiles)
        {
            int filesToDelete = filesOnDisk.length
                - ApplicationConfiguration.maxPrintSpoolFiles;
            Arrays.sort(filesOnDisk,
                        (File f1, File f2) -> Long.valueOf(f1.lastModified()).compareTo(
                            f2.lastModified()));
            for (int i = 0; i < filesToDelete; i++)
            {
                try
                {
                    FileUtils.deleteDirectory(filesOnDisk[i]);
                } catch (IOException ex)
                {
                    steno.error("Error whilst deleting "
                        + filesOnDisk[i].toString());
                }
            }
        }
    }

    /**
     *
     * @param macroName
     * @param useSDCard
     * @return
     * @throws celtech.printerControl.comms.commands.MacroPrintException
     */
    protected boolean runMacroPrintJob(String macroName, boolean useSDCard) throws MacroPrintException
    {
        boolean acceptedPrintRequest = false;
        consideringPrintRequest = true;

        String printUUID = createPrintJobDirectory();

        tidyPrintSpoolDirectory();

        String printjobFilename = ApplicationConfiguration.getPrintSpoolDirectory()
            + printUUID + File.separator + printUUID
            + ApplicationConfiguration.gcodeTempFileExtension;

        File printjobFile = new File(printjobFilename);

        try
        {
            ArrayList<String> macroContents = GCodeMacros.getMacroContents(macroName);
            // Write the contents of the macro file to the print area
            FileUtils.writeLines(printjobFile, macroContents);
        } catch (IOException ex)
        {
            throw new MacroPrintException("Error writing macro print job file: "
                + printjobFilename + " : "
                + ex.getMessage());
        } catch (MacroLoadException ex)
        {
            throw new MacroPrintException("Error whilst generating macro - " + ex.getMessage());
        }

        Lookup.getTaskExecutor().runOnGUIThread(() ->
        {
            int numberOfLines = SystemUtils.countLinesInFile(printjobFile, ";");
            linesInPrintingFile.set(numberOfLines);
            gcodePrintService.reset();
            gcodePrintService.setPrintUsingSDCard(useSDCard);
            gcodePrintService.setStartFromSequenceNumber(0);
            gcodePrintService.setCurrentPrintJobID(printUUID);
            gcodePrintService.setModelFileToPrint(printjobFilename);
            gcodePrintService.setPrinterToUse(associatedPrinter);
            gcodePrintService.start();
            consideringPrintRequest = false;
        });

        acceptedPrintRequest = true;

        return acceptedPrintRequest;
    }

    private String createPrintJobDirectory()
    {
        //Create the print job directory
        String printUUID = SystemUtils.generate16DigitID();
        String printJobDirectoryName = ApplicationConfiguration.getPrintSpoolDirectory()
            + printUUID;
        File printJobDirectory = new File(printJobDirectoryName);
        printJobDirectory.mkdirs();
        return printUUID;
    }

    /**
     *
     * @return
     */
    public boolean isConsideringPrintRequest()
    {
        return consideringPrintRequest;
    }

    /**
     * @return the progressETC
     */
    public IntegerProperty progressETCProperty()
    {
        return progressETC;
    }

    public ReadOnlyBooleanProperty etcAvailableProperty()
    {
        return etcAvailable;
    }

    public ReadOnlyIntegerProperty progressCurrentLayerProperty()
    {
        return progressCurrentLayer;
    }

    public ReadOnlyIntegerProperty progressNumLayersProperty()
    {
        return progressNumLayers;
    }

    protected void goToIdle()
    {
        Lookup.getTaskExecutor().runOnGUIThread(() ->
        {
            if (movieMakerTask != null)
            {
                if (movieMakerTask.isRunning())
                {
                    movieMakerTask.shutdown();
                }
                movieMakerTask = null;
            }
            printProgressMessage.unbind();
            setPrintProgressMessage("");
            primaryProgressPercent.unbind();
            setPrimaryProgressPercent(0);
            secondaryProgressPercent.unbind();
            setSecondaryProgressPercent(0);
            sendingDataToPrinter.set(false);
            setPrintInProgress(false);
            setPrintProgressTitle(Lookup.i18n("PrintQueue.Idle"));
        });
    }

    void goToSlicing()
    {
        Lookup.getTaskExecutor().runOnGUIThread(() ->
        {
            printProgressMessage.unbind();
            printProgressMessage.bind(slicerService.messageProperty());
            primaryProgressPercent.unbind();
            setPrimaryProgressPercent(0);
            primaryProgressPercent.bind(slicerService.progressProperty());
            secondaryProgressPercent.unbind();
            setSecondaryProgressPercent(0);
            sendingDataToPrinter.set(false);
            setPrintInProgress(true);
            setPrintProgressTitle(Lookup.i18n("PrintQueue.Slicing"));
        });
    }

    void goToPostProcessing()
    {
        Lookup.getTaskExecutor().runOnGUIThread(() ->
        {
            printProgressMessage.unbind();
            printProgressMessage.bind(
                gcodePostProcessorService.messageProperty());
            primaryProgressPercent.unbind();
            setPrimaryProgressPercent(0);
            primaryProgressPercent.bind(
                gcodePostProcessorService.progressProperty());
            secondaryProgressPercent.unbind();
            sendingDataToPrinter.set(false);
            setSecondaryProgressPercent(0);
            setPrintInProgress(true);
            setPrintProgressTitle(Lookup.i18n("PrintQueue.PostProcessing"));
        });
    }

    void goToSendingToPrinter()
    {
        Lookup.getTaskExecutor().runOnGUIThread(() ->
        {
            printProgressMessage.unbind();
            printProgressMessage.bind(gcodePrintService.messageProperty());
            primaryProgressPercent.unbind();
            setPrimaryProgressPercent(0);
            secondaryProgressPercent.unbind();
            setSecondaryProgressPercent(0);
            secondaryProgressPercent.bind(
                gcodePrintService.progressProperty());
            sendingDataToPrinter.set(true);
            setPrintInProgress(true);
            setPrintProgressTitle(Lookup.i18n("PrintQueue.SendingToPrinter"));
        });
    }

    void goToPause()
    {
        Lookup.getTaskExecutor().runOnGUIThread(() ->
        {
            setPrintInProgress(true);
            setPrintProgressTitle(Lookup.i18n("PrintQueue.Paused"));
        });
    }

    void goToPrinting()
    {
        Lookup.getTaskExecutor().runOnGUIThread(() ->
        {
            printProgressMessage.unbind();
            primaryProgressPercent.unbind();
            if (associatedPrinter.printerStatusProperty().get() != PrinterStatus.PAUSED)
            {
                setPrimaryProgressPercent(0);
            }
            printProgressMessage.set("");
            setPrintInProgress(true);
            setPrintProgressTitle(Lookup.i18n("PrintQueue.Printing"));
        });
    }

    void goToExecutingMacro()
    {
        Lookup.getTaskExecutor().runOnGUIThread(() ->
        {
            printProgressMessage.unbind();
            primaryProgressPercent.unbind();
            if (associatedPrinter.printerStatusProperty().get() != PrinterStatus.PAUSED)
            {
                setPrimaryProgressPercent(0);
            }
            printProgressMessage.set("");
            setPrintInProgress(true);
            setPrintProgressTitle(associatedPrinter.macroTypeProperty().get().getI18nString());
        });
    }

    /**
     * Stop all services, in the GUI thread. Block current thread until the routine has completed.
     */
    protected void stopAllServices()
    {

        Callable<Boolean> stopServices = new Callable()
        {
            @Override
            public Boolean call() throws Exception
            {
                steno.info("Shutdown print services...");
                if (slicerService.isRunning())
                {
                    steno.info("Shutdown slicer service...");
                    slicerService.cancelRun();
                }
                if (gcodePostProcessorService.isRunning())
                {
                    steno.info("Shutdown PP...");
                    gcodePostProcessorService.cancelRun();
                }
                if (gcodePrintService.isRunning())
                {
                    steno.info("Shutdown print service...");
                    gcodePrintService.cancelRun();
                }
                steno.info("Shutdown print services complete");
                return true;
            }
        };
        FutureTask<Boolean> stopServicesTask = new FutureTask<>(stopServices);
        Lookup.getTaskExecutor().runOnGUIThread(stopServicesTask);
        try
        {
            stopServicesTask.get();
        } catch (InterruptedException | ExecutionException ex)
        {
            steno.error("Error while stopping services");
        }
    }

    public boolean reEstablishTransfer(String printJobID, int expectedSequenceNumber)
    {
        PrintJob printJob = PrintJob.readJobFromDirectory(printJobID);
        boolean acceptedPrintRequest = false;

        if (printJob.roboxisedFileExists())
        {
            acceptedPrintRequest = reprintFileFromDisk(printJob, expectedSequenceNumber);
            Lookup.getSystemNotificationHandler().removePrintTransferFailedNotification();
        }

        return acceptedPrintRequest;
    }

    private void detectAlreadyPrinting()
    {
        boolean roboxIsPrinting = false;

        if (associatedPrinter != null)
        {
            String printJobID = associatedPrinter.printJobIDProperty().get();
            if (printJobID != null)
            {
                if (printJobID.codePointAt(0) != 0)
                {
                    roboxIsPrinting = true;
                    makeETCCalculatorForJobOfUUID(printJobID);
                }
            }

            switch (associatedPrinter.printerStatusProperty().get())
            {
                case IDLE:
                    if (roboxIsPrinting)
                    {
                        makeETCCalculatorForJobOfUUID(printJobID);
                        Lookup.getSystemNotificationHandler().
                            showDetectedPrintInProgressNotification();
                        steno.info("Printer "
                            + associatedPrinter.getPrinterIdentity().printerFriendlyName.get()
                            + " is printing");

                        if (associatedPrinter.pauseStatusProperty().get() == PauseStatus.PAUSED)
                        {
                            associatedPrinter.setPrinterStatus(PrinterStatus.PAUSED);
                        } else
                        {
                            associatedPrinter.setPrinterStatus(PrinterStatus.PRINTING);
                        }
                    }
                    break;
                case SENDING_TO_PRINTER:
                case PRINTING:
                case EXECUTING_MACRO:
                    if (roboxIsPrinting == false)
                    {
                        associatedPrinter.setPrinterStatus(PrinterStatus.IDLE);
                    }
                    break;
                default:
                    break;
            }
        }
    }
}
