/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.services.postProcessor;

import celtech.gcodetranslator.GCodeRoboxiser;
import celtech.gcodetranslator.RoboxiserResult;
import celtech.printerControl.PrintJob;
import celtech.printerControl.model.HardwarePrinter;
import celtech.services.slicer.RoboxProfile;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;

/**
 *
 * @author Ian
 */
public class PostProcessorTask extends Task<GCodePostProcessingResult>
{

    private String printJobUUID = null;
    private RoboxProfile settings = null;
    private HardwarePrinter printerToUse = null;
    private DoubleProperty taskProgress = new SimpleDoubleProperty(0);

    /**
     *
     * @param printJobUUID
     * @param settings
     * @param printerToUse
     */
    public PostProcessorTask(String printJobUUID, RoboxProfile settings,
        HardwarePrinter printerToUse)
    {
        this.printJobUUID = printJobUUID;
        this.settings = settings;
        this.printerToUse = printerToUse;
    }

    @Override
    protected GCodePostProcessingResult call() throws Exception
    {
        updateMessage("");
        updateProgress(0, 100);

        GCodeRoboxiser roboxiser = new GCodeRoboxiser();
        PrintJob printJob = PrintJob.readJobFromDirectory(printJobUUID);
        String gcodeFileToProcess = printJob.getGCodeFileLocation();
        String gcodeOutputFile = printJob.getRoboxisedFileLocation();

        taskProgress.addListener(new ChangeListener<Number>()
        {
            @Override
            public void changed(ObservableValue<? extends Number> observable,
                Number oldValue, Number newValue)
            {
                updateProgress(newValue.doubleValue(), 100.0);
            }
        });

        RoboxiserResult roboxiserResult = roboxiser.roboxiseFile(
            gcodeFileToProcess, gcodeOutputFile, settings, taskProgress);
        roboxiserResult.getPrintJobStatistics().writeToFile(printJob.getStatisticsFileLocation());

        GCodePostProcessingResult postProcessingResult = new GCodePostProcessingResult(
            printJobUUID, gcodeOutputFile, printerToUse, roboxiserResult);

        return postProcessingResult;
    }

}
