package celtech.services.calibration;

import celtech.configuration.ApplicationConfiguration;
import celtech.coreUI.DisplayManager;
import java.net.URL;
import java.util.Optional;

/**
 *
 * @author Ian
 */
public enum NozzleOpeningCalibrationState 
{   
    IDLE("calibrationPanel.readyToBeginNozzleOpeningCalibration", "Nozzle Opening Illustrations_Step 1.fxml"),

    HEATING("calibrationPanel.heating", ""),

    NO_MATERIAL_CHECK("calibrationPanel.valvesClosedNoMaterial", "Nozzle Opening Illustrations_Step 3.fxml"),
    
    T0_EXTRUDING("calibrationPanel.isMaterialExtrudingNozzle0", ""),
    
    T1_EXTRUDING("calibrationPanel.isMaterialExtrudingNozzle1", ""),
    
    HEAD_CLEAN_CHECK_AFTER_EXTRUDE("calibrationPanel.ensureHeadIsCleanBMessage", "Nozzle Opening Illustrations_Step 5 and 7.fxml"),

    PRE_CALIBRATION_PRIMING_FINE("calibrationPanel.primingNozzle", ""),

    CALIBRATE_FINE_NOZZLE("calibrationPanel.calibrationCommencedMessageFine", "Nozzle Opening Illustrations_Step 4.fxml"),
    
    INCREMENT_FINE_NOZZLE_POSITION("", ""),

    PRE_CALIBRATION_PRIMING_FILL("calibrationPanel.primingNozzle", ""),

    CALIBRATE_FILL_NOZZLE("calibrationPanel.calibrationCommencedMessageFill", "Nozzle Opening Illustrations_Step 6.fxml"),
    
    INCREMENT_FILL_NOZZLE_POSITION("", ""),

    HEAD_CLEAN_CHECK_FILL_NOZZLE("calibrationPanel.ensureHeadIsCleanBMessage", "Nozzle Opening Illustrations_Step 5 and 7.fxml"),

    CONFIRM_NO_MATERIAL("calibrationPanel.valvesClosedNoMaterialPostCalibration", "Nozzle Opening Illustrations_Step 8.fxml"),

//    CONFIRM_MATERIAL_EXTRUDING("calibrationPanel.valvesOpenMaterialExtruding", ""),

    FINISHED("calibrationPanel.calibrationSucceededMessage", "Nozzle Opening Illustrations_Step 10.fxml"),
    
    CANCELLED("", ""),
    
    DONE("", ""),

    FAILED("calibrationPanel.nozzleCalibrationFailed", "Nozzle Opening Illustrations_Failure.fxml");

    private String stepTitleResource = null;
    private String diagramName;

    private NozzleOpeningCalibrationState(String stepTitleResource, String diagramName)
    {
        this.stepTitleResource = stepTitleResource;
        this.diagramName = diagramName;
    }
    
    public Optional<URL> getDiagramFXMLFileName() {
        if (diagramName.equals(""))
        {
            return Optional.empty();
        }
        return Optional.of(getClass().getResource(
            ApplicationConfiguration.fxmlDiagramsResourcePath
            + "nozzleopening" + "/" + diagramName));
    }

    public String getStepTitle()
    {
        if (stepTitleResource == null || stepTitleResource.equals(""))
        {
            return "";
        } else
        {
            return DisplayManager.getLanguageBundle().getString(stepTitleResource);
        }
    }
}
