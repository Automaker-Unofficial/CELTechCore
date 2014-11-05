package celtech.coreUI.controllers.utilityPanels;

import celtech.Lookup;
import celtech.appManager.Notifier;
import celtech.configuration.ApplicationConfiguration;
import celtech.configuration.DirectoryMemoryProperty;
import celtech.coreUI.DisplayManager;
import celtech.coreUI.components.GCodeMacroButton;
import celtech.coreUI.components.ProgressDialog;
import celtech.printerControl.PrinterStatus;
import celtech.printerControl.comms.commands.rx.FirmwareResponse;
import celtech.printerControl.model.Printer;
import celtech.printerControl.model.PrinterException;
import celtech.services.firmware.FirmwareLoadResult;
import celtech.services.firmware.FirmwareLoadService;
import celtech.services.printing.GCodePrintResult;
import celtech.services.printing.GCodePrintService;
import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 * FXML Controller class
 *
 * @author Ian
 */
public class MaintenancePanelController implements Initializable
{

    private static final Stenographer steno = StenographerFactory.getStenographer(MaintenancePanelController.class.getName());
    private Printer connectedPrinter = null;
    private ResourceBundle i18nBundle = null;

    private ProgressDialog firmwareUpdateProgress = null;
    private final FirmwareLoadService firmwareLoadService = new FirmwareLoadService();
    private FileChooser firmwareFileChooser = new FileChooser();

    private static Stage needleValvecalibrationStage = null;
    private static Stage offsetCalibrationStage = null;

    private ProgressDialog gcodeUpdateProgress = null;
    private FileChooser gcodeFileChooser = new FileChooser();
    private final GCodePrintService gcodePrintService = new GCodePrintService();

    private ChangeListener<PrinterStatus> printerStatusListener = new ChangeListener<PrinterStatus>()
    {
        @Override
        public void changed(ObservableValue<? extends PrinterStatus> observable, PrinterStatus oldValue, PrinterStatus newValue)
        {
            setButtonVisibility();
        }
    };

    private ChangeListener<Boolean> filamentLoadedListener = new ChangeListener<Boolean>()
    {
        @Override
        public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue)
        {
            setButtonVisibility();
        }
    };

    @FXML
    private AnchorPane container;

    @FXML
    private GCodeMacroButton YTestButton;

    @FXML
    private Button PurgeMaterialButton;

    @FXML
    private Button loadFirmwareGCodeMacroButton;

    @FXML
    private GCodeMacroButton T1CleanButton;

    @FXML
    private GCodeMacroButton EjectStuckMaterialButton;

    @FXML
    private GCodeMacroButton SpeedTestButton;

    @FXML
    private Button sendGCodeStreamGCodeMacroButton;

    @FXML
    private GCodeMacroButton XTestButton;

    @FXML
    private GCodeMacroButton Level_YButton;

    @FXML
    private GCodeMacroButton T0CleanButton;

    @FXML
    private Label currentFirmwareField;

    @FXML
    private GCodeMacroButton LevelGantryButton;

    @FXML
    private Button sendGCodeSDGCodeMacroButton;

    @FXML
    private GCodeMacroButton ZTestButton;

    @FXML
    void macroButtonPress(ActionEvent event)
    {
        if (event.getSource() instanceof GCodeMacroButton)
        {
            GCodeMacroButton button = (GCodeMacroButton) event.getSource();
            String macroName = button.getMacroName();

            if (macroName != null)
            {
                try
                {
                    connectedPrinter.executeMacro(macroName);
                } catch (PrinterException ex)
                {
                    steno.error("Error sending macro : " + macroName);
                }
            }
        }
    }

    @FXML
    void macroButtonPressNoPurgeCheck(ActionEvent event)
    {
        if (event.getSource() instanceof GCodeMacroButton)
        {
            GCodeMacroButton button = (GCodeMacroButton) event.getSource();
            String macroName = button.getMacroName();

            if (macroName != null)
            {
                try
                {
                    connectedPrinter.executeMacroWithoutPurgeCheck(macroName);
                } catch (PrinterException ex)
                {
                    steno.error("Error sending macro : " + macroName);
                }
            }
        }
    }

    @FXML
    void loadFirmware(ActionEvent event)
    {
        firmwareFileChooser.setInitialFileName("Untitled");

        firmwareFileChooser.setInitialDirectory(new File(ApplicationConfiguration.getLastDirectory(DirectoryMemoryProperty.FIRMWARE)));

        final File file = firmwareFileChooser.showOpenDialog(DisplayManager.getMainStage());
        if (file != null)
        {
            firmwareLoadService.reset();
            firmwareLoadService.setPrinterToUse(connectedPrinter);
            firmwareLoadService.setFirmwareFileToLoad(file.getAbsolutePath());
            firmwareLoadService.start();
            ApplicationConfiguration.setLastDirectory(DirectoryMemoryProperty.FIRMWARE, file.getParentFile().getAbsolutePath());
        }
    }

    void readFirmwareVersion()
    {
        try
        {
            FirmwareResponse response = connectedPrinter.readFirmwareVersion();
            if (response != null)
            {
                currentFirmwareField.setText(response.getFirmwareRevision());
            }
        } catch (PrinterException ex)
        {
            steno.error("Error reading firmware version");
        }
    }

    @FXML
    void sendGCodeStream(ActionEvent event)
    {
        gcodeFileChooser.setInitialFileName("Untitled");

        gcodeFileChooser.setInitialDirectory(new File(ApplicationConfiguration.getLastDirectory(DirectoryMemoryProperty.MACRO)));

        final File file = gcodeFileChooser.showOpenDialog(container.getScene().getWindow());
        if (file != null)
        {
            if (connectedPrinter.printerStatusProperty().get() == PrinterStatus.IDLE)
            {
                gcodePrintService.reset();
                gcodePrintService.setPrintUsingSDCard(false);
                gcodePrintService.setPrinterToUse(connectedPrinter);
                gcodePrintService.setModelFileToPrint(file.getAbsolutePath());
                gcodePrintService.start();
            }
            ApplicationConfiguration.setLastDirectory(DirectoryMemoryProperty.MACRO, file.getParentFile().getAbsolutePath());
        }
    }

    @FXML
    void sendGCodeSD(ActionEvent event)
    {
        gcodeFileChooser.setInitialFileName("Untitled");

        gcodeFileChooser.setInitialDirectory(new File(ApplicationConfiguration.getLastDirectory(DirectoryMemoryProperty.MACRO)));

        final File file = gcodeFileChooser.showOpenDialog(container.getScene().getWindow());

        if (file != null)
        {
            try
            {
                connectedPrinter.executeGCodeFile(file.getAbsolutePath());
            } catch (PrinterException ex)
            {
                steno.error("Error sending SD job");
            }
            ApplicationConfiguration.setLastDirectory(DirectoryMemoryProperty.GCODE, file.getParentFile().getAbsolutePath());
        }
    }

    @FXML
    void purge(ActionEvent event)
    {
        DisplayManager.getInstance().getPurgeInsetPanelController().purge(connectedPrinter);
    }

    /**
     * Initializes the controller class.
     *
     * @param url
     * @param rb
     */
    @Override
    public void initialize(URL url, ResourceBundle rb)
    {
        i18nBundle = DisplayManager.getLanguageBundle();

        Platform.runLater(new Runnable()
        {
            @Override
            public void run()
            {
                gcodeUpdateProgress = new ProgressDialog(gcodePrintService);
                firmwareUpdateProgress = new ProgressDialog(firmwareLoadService);
            }
        });

        currentFirmwareField.setStyle("-fx-font-weight: bold;");

        gcodeFileChooser.setTitle(DisplayManager.getLanguageBundle().getString("maintenancePanel.gcodeFileChooserTitle"));
        gcodeFileChooser.getExtensionFilters()
            .addAll(
                new FileChooser.ExtensionFilter(DisplayManager.getLanguageBundle().getString("maintenancePanel.gcodeFileDescription"), "*.gcode"));

        gcodePrintService.setOnSucceeded(new EventHandler<WorkerStateEvent>()
        {
            @Override
            public void handle(WorkerStateEvent t)
            {
                GCodePrintResult result = (GCodePrintResult) (t.getSource().getValue());
                if (result.isSuccess())
                {
                    Notifier.showInformationNotification(DisplayManager.getLanguageBundle().getString("maintenancePanel.gcodePrintSuccessTitle"),
                                                         DisplayManager.getLanguageBundle().getString("maintenancePanel.gcodePrintSuccessMessage"));
                } else
                {
                    Notifier.showErrorNotification(DisplayManager.getLanguageBundle().getString("maintenancePanel.gcodePrintFailedTitle"),
                                                   DisplayManager.getLanguageBundle().getString("maintenancePanel.gcodePrintFailedMessage"));

                    steno.warning("In gcode print succeeded but with failure flag");
                }
            }
        });

        gcodePrintService.setOnFailed(new EventHandler<WorkerStateEvent>()
        {
            @Override
            public void handle(WorkerStateEvent t)
            {
                Notifier.showErrorNotification(DisplayManager.getLanguageBundle().getString("maintenancePanel.gcodePrintFailedTitle"),
                                               DisplayManager.getLanguageBundle().getString("maintenancePanel.gcodePrintFailedMessage"));
            }
        });

        firmwareFileChooser.setTitle(DisplayManager.getLanguageBundle().getString("maintenancePanel.firmwareFileChooserTitle"));
        firmwareFileChooser.getExtensionFilters()
            .addAll(
                new FileChooser.ExtensionFilter(DisplayManager.getLanguageBundle().getString("maintenancePanel.firmwareFileDescription"), "*.bin"));

        firmwareLoadService.setOnSucceeded((WorkerStateEvent t) ->
        {
            FirmwareLoadResult result = (FirmwareLoadResult) t.getSource().getValue();
            Lookup.getSystemNotificationHandler().showFirmwareUpgradeStatusNotification(result);
        });

        firmwareLoadService.setOnFailed((WorkerStateEvent t) ->
        {
            FirmwareLoadResult result = (FirmwareLoadResult) t.getSource().getValue();
            Lookup.getSystemNotificationHandler().showFirmwareUpgradeStatusNotification(result);
        });

        Lookup.currentlySelectedPrinterProperty().addListener((ObservableValue<? extends Printer> observable, Printer oldValue, Printer newValue) ->
        {
            if (connectedPrinter != null)
            {
                connectedPrinter.printerStatusProperty().removeListener(printerStatusListener);
            }

            connectedPrinter = newValue;

            if (connectedPrinter != null)
            {
                readFirmwareVersion();
                connectedPrinter.printerStatusProperty().addListener(printerStatusListener);
                //TODO modify for multiple extruders
                connectedPrinter.extrudersProperty().get(0).filamentLoadedProperty().addListener(filamentLoadedListener);
                connectedPrinter.extrudersProperty().get(1).filamentLoadedProperty().addListener(filamentLoadedListener);
                setButtonVisibility();
            }
        });
    }

    private void setButtonVisibility()
    {
        boolean printingdisabled = false;
        boolean noFilamentOrPrintingdisabled = false;

        if (connectedPrinter == null)
        {
            printingdisabled = true;
            noFilamentOrPrintingdisabled = true;
        } else
        {
            printingdisabled = connectedPrinter.printerStatusProperty().get() != PrinterStatus.IDLE;
            //TODO modify for multiple extruders
            noFilamentOrPrintingdisabled = printingdisabled
                || (connectedPrinter.extrudersProperty().get(0).filamentLoadedProperty().get() == false
                && connectedPrinter.extrudersProperty().get(1).filamentLoadedProperty().get() == false);
        }

        YTestButton.setDisable(printingdisabled);

        PurgeMaterialButton.setDisable(noFilamentOrPrintingdisabled);

        T1CleanButton.setDisable(noFilamentOrPrintingdisabled);

        EjectStuckMaterialButton.setDisable(noFilamentOrPrintingdisabled);

        SpeedTestButton.setDisable(printingdisabled);

        sendGCodeStreamGCodeMacroButton.setDisable(printingdisabled);

        XTestButton.setDisable(printingdisabled);

        Level_YButton.setDisable(printingdisabled);

        T0CleanButton.setDisable(noFilamentOrPrintingdisabled);

        LevelGantryButton.setDisable(printingdisabled);

        sendGCodeSDGCodeMacroButton.setDisable(printingdisabled);

        ZTestButton.setDisable(printingdisabled);
    }
}
