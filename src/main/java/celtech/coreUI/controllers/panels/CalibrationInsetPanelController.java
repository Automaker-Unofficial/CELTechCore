package celtech.coreUI.controllers.panels;

import celtech.Lookup;
import celtech.appManager.ApplicationMode;
import celtech.appManager.ApplicationStatus;
import celtech.configuration.ApplicationConfiguration;
import celtech.coreUI.DisplayManager;
import celtech.coreUI.components.VerticalMenu;
import celtech.coreUI.components.LargeProgress;
import celtech.coreUI.components.buttons.GraphicButtonWithLabel;
import celtech.printerControl.model.Head;
import celtech.printerControl.model.NozzleHeater;
import celtech.printerControl.model.Printer;
import celtech.printerControl.model.PrinterException;
import celtech.printerControl.model.Reel;
import celtech.printerControl.model.calibration.StateTransitionManager;
import celtech.services.calibration.CalibrationXAndYState;
import celtech.services.calibration.NozzleOffsetCalibrationState;
import celtech.services.calibration.NozzleOpeningCalibrationState;
import celtech.utils.PrinterListChangesListener;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Bounds;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author Ian
 */
public class CalibrationInsetPanelController implements Initializable,
    PrinterListChangesListener
{

    CalibrationMenuConfiguration calibrationMenuConfiguration = new CalibrationMenuConfiguration();
    CalibrationMode calibrationMode;
    CalibrationXAndYGUI calibrationXAndYGUI;
    CalibrationNozzleHeightGUI calibrationNozzleHeightGUI;
    CalibrationNozzleOpeningGUI calibrationNozzleOpeningGUI;
    StateTransitionManager stateManager;

    private ResourceBundle resources;

    private void resizeTopBorderPane()
    {
        topBorderPane.setPrefWidth(topPane.getWidth());
        topBorderPane.setPrefHeight(topPane.getHeight());
        topBorderPane.setMaxWidth(topPane.getWidth());
        topBorderPane.setMaxHeight(topPane.getHeight());
    }

    void resetMenuAndGoToChoiceMode()
    {
        calibrationMenu.reset();
        setCalibrationMode(CalibrationMode.CHOICE);
    }

    protected static enum ProgressVisibility
    {

        TEMP, PRINT, NONE;
    };

    private final Stenographer steno = StenographerFactory.getStenographer(
        CalibrationInsetPanelController.class.getName());

    @FXML
    protected VerticalMenu calibrationMenu;

    @FXML
    protected StackPane calibrateBottomMenu;

    @FXML
    protected Pane calibrationBottomArea;

    @FXML
    protected Pane altButtonContainer;

    @FXML
    protected LargeProgress calibrationProgressTemp;

    @FXML
    protected LargeProgress calibrationProgressPrint;

    @FXML
    protected Text stepNumber;

    @FXML
    protected Button buttonA;

    @FXML
    protected Button buttonB;

    @FXML
    protected GraphicButtonWithLabel nextButton;

    @FXML
    protected GraphicButtonWithLabel retryPrintButton;

    @FXML
    protected GraphicButtonWithLabel backToStatus;

    @FXML
    protected GraphicButtonWithLabel startCalibrationButton;

    @FXML
    protected GraphicButtonWithLabel cancelCalibrationButton;

    @FXML
    protected Label calibrationStatus;

    @FXML
    private VBox informationCentre;

    @FXML
    private BorderPane topBorderPane;

    @FXML
    private Pane topPane;

    @FXML
    private VBox diagramContainer;

    private Printer currentPrinter;
    private int targetTemperature;
    private double currentExtruderTemperature;
    private int targetETC;
    private double printPercent;
    private Pane diagramNode;
    DiagramController diagramController;
    private final Map<Node, Bounds> nodeToBoundsCache = new HashMap<>();

    @FXML
    void buttonAAction(ActionEvent event)
    {
        stateManager.followTransition(StateTransitionManager.GUIName.A_BUTTON);
    }

    @FXML
    void buttonBAction(ActionEvent event)
    {
        stateManager.followTransition(StateTransitionManager.GUIName.B_BUTTON);
    }

    @FXML
    void nextButtonAction(ActionEvent event)
    {
        stateManager.followTransition(StateTransitionManager.GUIName.NEXT);
    }

    @FXML
    void backToStatusAction(ActionEvent event)
    {
        if (calibrationMode == CalibrationMode.CHOICE)
        {
            ApplicationStatus.getInstance().setMode(ApplicationMode.STATUS);
        } else
        {
            stateManager.followTransition(StateTransitionManager.GUIName.BACK);
            ApplicationStatus.getInstance().setMode(ApplicationMode.STATUS);
        }
    }

    @FXML
    void startCalibration(ActionEvent event)
    {
        stateManager.followTransition(StateTransitionManager.GUIName.START);
    }

    @FXML
    void cancelCalibration(ActionEvent event)
    {
        try
        {
            stateManager.cancel();
        } catch (Exception ex)
        {
            steno.error("Error cancelling calibration: " + ex);
        }
        cancelCalibrationAction();
    }

    @FXML
    void retryCalibration(ActionEvent event)
    {
        stateManager.followTransition(StateTransitionManager.GUIName.RETRY);
    }

    public void cancelCalibrationAction()
    {
        setCalibrationMode(CalibrationMode.CHOICE);
    }

    protected void hideAllInputControlsExceptStepNumber()
    {
        backToStatus.setVisible(false);
        setCalibrationProgressVisible(CalibrationInsetPanelController.ProgressVisibility.NONE);
        retryPrintButton.setVisible(false);
        startCalibrationButton.setVisible(false);
        cancelCalibrationButton.setVisible(false);
        nextButton.setVisible(false);
        buttonB.setVisible(false);
        buttonA.setVisible(false);
        stepNumber.setVisible(true);
        hideSpinner();
        if (diagramNode != null)
        {
            diagramNode.setVisible(false);
        }
        stepNumber.setText("");
    }

    @Override
    public void initialize(URL location, ResourceBundle resources)
    {
        this.resources = resources;

        setupProgressBars();

        startCalibrationButton.installTag();
        setCalibrationMode(CalibrationMode.CHOICE);

        Lookup.getPrinterListChangesNotifier().addListener(this);

        calibrationMenuConfiguration.configureCalibrationMenu(calibrationMenu, this);

        addDiagramMoveScaleListeners();

        

    }

    private void addDiagramMoveScaleListeners()
    {
        topPane.widthProperty().addListener(
            (ObservableValue<? extends Number> observable, Number oldValue, Number newValue) ->
            {
                resizeTopBorderPane();
            });

        topPane.heightProperty().addListener(
            (ObservableValue<? extends Number> observable, Number oldValue, Number newValue) ->
            {
                resizeTopBorderPane();
            });

        diagramContainer.widthProperty().addListener(
            (ObservableValue<? extends Number> observable, Number oldValue, Number newValue) ->
            {
                resizeDiagram();
            });

        diagramContainer.heightProperty().addListener(
            (ObservableValue<? extends Number> observable, Number oldValue, Number newValue) ->
            {
                resizeDiagram();
            });

    }

    private void resizeDiagram()
    {
        if (diagramNode == null)
        {
            return;
        }

        double diagramWidth = nodeToBoundsCache.get(diagramNode).getWidth();
        double diagramHeight = nodeToBoundsCache.get(diagramNode).getHeight();

        double availableWidth = diagramContainer.getWidth();
        double availableHeight = diagramContainer.getHeight();

        double requiredScaleHeight = availableHeight / diagramHeight * 0.95;
        double requiredScaleWidth = availableWidth / diagramWidth * 0.95;
        double requiredScale = Math.min(requiredScaleHeight, requiredScaleWidth);
        requiredScale = Math.min(requiredScale, 1.3d);
        diagramController.setScale(requiredScale, diagramNode);

        diagramNode.setPrefWidth(0);
        diagramNode.setPrefHeight(0);

        double scaledDiagramWidth = diagramWidth * requiredScale;

        double xTranslate = 0;
        double yTranslate = 0;
//        
        xTranslate = -scaledDiagramWidth / 2;
        yTranslate -= availableHeight / 2.0;

        diagramNode.setTranslateX(xTranslate);
        diagramNode.setTranslateY(yTranslate);

    }

    /**
     * Create and return the diagram node for the given fxml file.
     *
     * @param diagramName
     * @return
     */
    private Node getDiagramNode(URL fxmlFileName)
    {
        Pane loadedDiagramNode = null;
        try
        {
            FXMLLoader loader = new FXMLLoader(fxmlFileName, resources);
            diagramController = new DiagramController();
            loader.setController(diagramController);
            loadedDiagramNode = loader.load();

            Bounds bounds = getBoundsOfNotYetDisplayedNode(loadedDiagramNode);
            steno.debug("diagram bounds are " + bounds);
            nodeToBoundsCache.put(loadedDiagramNode, bounds);
            diagramController.setStateTransitionManager(stateManager);

        } catch (IOException ex)
        {
            ex.printStackTrace();
            steno.error("Could not load diagram: " + fxmlFileName);
        }
        return loadedDiagramNode;

    }

    private Bounds getBoundsOfNotYetDisplayedNode(Pane loadedDiagramNode)
    {
        Group group = new Group(loadedDiagramNode);
        Scene scene = new Scene(group);
        scene.getStylesheets().add(ApplicationConfiguration.getMainCSSFile());
        group.applyCss();
        group.layout();
        Bounds bounds = loadedDiagramNode.getLayoutBounds();
        return bounds;
    }

    protected void showDiagram(URL fxmlLocation)
    {
        diagramNode = (Pane) getDiagramNode(fxmlLocation);
        if (diagramNode == null)
        {
            return;
        }

        diagramContainer.getChildren().clear();
        diagramContainer.getChildren().add(diagramNode);

        resizeDiagram();

        diagramNode.setVisible(true);
    }

    private final ChangeListener<Number> targetTemperatureListener = (ObservableValue<? extends Number> observable, Number oldValue, Number newValue) ->
    {
        targetTemperature = newValue.intValue();
        updateCalibrationProgressTemp();
    };

    private final ChangeListener<Number> extruderTemperatureListener
        = (ObservableValue<? extends Number> observable, Number oldValue, Number newValue) ->
        {
            currentExtruderTemperature = newValue.doubleValue();
            updateCalibrationProgressTemp();
        };

    private void updateCalibrationProgressTemp()
    {
        if (targetTemperature != 0 && calibrationProgressTemp.isVisible())
        {

            int currentTemp = (int) currentExtruderTemperature;
            if (currentTemp > targetTemperature)
            {
                currentTemp = targetTemperature;
            }

            String targetTempStr = targetTemperature + Lookup.i18n("misc.degreesC");
            String currentTempStr = currentTemp + Lookup.i18n("misc.degreesC");
            calibrationProgressTemp.setCurrentValue(currentTempStr);
            calibrationProgressTemp.setTargetValue(targetTempStr);
            calibrationProgressTemp.setProgress(currentExtruderTemperature / targetTemperature);
        }
    }

    private final ChangeListener<Number> targetETCListener = (ObservableValue<? extends Number> observable, Number oldValue, Number newValue) ->
    {
        targetETC = newValue.intValue();
        updateCalibrationProgressPrint();
    };

    private final ChangeListener<Number> printPercentListener
        = (ObservableValue<? extends Number> observable, Number oldValue, Number newValue) ->
        {
            printPercent = newValue.doubleValue();
            updateCalibrationProgressPrint();
        };

    private void updateCalibrationProgressPrint()
    {
        targetETC = currentPrinter.getPrintEngine().progressETCProperty().get();
        if (calibrationProgressPrint.isVisible())
        {
            String targetETCStr = targetETC + "s";
            String currentPrintPercentStr = ((int) (printPercent * 100)) + "%";
            calibrationProgressPrint.setCurrentValue(currentPrintPercentStr);
//            calibrationProgressPrint.setTargetValue(targetETCStr);
            calibrationProgressPrint.setProgress(printPercent);
        }
    }

    protected void setCalibrationProgressVisible(ProgressVisibility visibility)
    {
        calibrationProgressTemp.setVisible(false);
        calibrationProgressPrint.setVisible(false);
        calibrationBottomArea.getChildren().clear();
        if (visibility != ProgressVisibility.NONE)
        {
            if (visibility == ProgressVisibility.TEMP)
            {
                calibrationProgressTemp.setVisible(true);
                calibrationBottomArea.getChildren().add(calibrationProgressTemp);
            }
            if (visibility == ProgressVisibility.PRINT)
            {
                calibrationProgressPrint.setVisible(true);
                calibrationBottomArea.getChildren().add(calibrationProgressPrint);
            }
        }
        calibrationBottomArea.getChildren().add(calibrateBottomMenu);
    }

    private void switchToPrinter(Printer printer)
    {
        if (currentPrinter != null)
        {
            unbindPrinter(currentPrinter);
        }
        if (printer != null)
        {
            bindPrinter(printer);
        }
        currentPrinter = printer;
    }

    private void unbindPrinter(Printer printer)
    {
        removeHeadListeners(printer);
        removePrintProgressListeners(printer);
    }

    private void bindPrinter(Printer printer)
    {
        calibrationProgressTemp.setProgress(0);
        Head newHead = printer.headProperty().get();
        if (newHead != null)
        {
            NozzleHeater nozzleHeater = newHead.getNozzleHeaters().get(0);
            targetTemperature = nozzleHeater.nozzleTargetTemperatureProperty().get();
            nozzleHeater.nozzleTargetTemperatureProperty().addListener(targetTemperatureListener);
            nozzleHeater.nozzleTemperatureProperty().addListener(extruderTemperatureListener);
        }
        setupPrintProgressListeners(printer);
        configureStartButtonForMode(calibrationMode, printer);
    }

    private void removePrintProgressListeners(Printer printer)
    {
        printer.getPrintEngine().progressETCProperty().removeListener(targetETCListener);
        printer.getPrintEngine().progressProperty().removeListener(printPercentListener);
    }

    private void setupPrintProgressListeners(Printer printer)
    {
        printer.getPrintEngine().progressProperty().addListener(printPercentListener);
        printer.getPrintEngine().progressETCProperty().addListener(targetETCListener);
    }

    private void removeHeadListeners(Printer printer)
    {
        if (printer.headProperty().get() != null)
        {
            NozzleHeater nozzleHeater = printer.headProperty().get().getNozzleHeaters().get(0);
            nozzleHeater.nozzleTargetTemperatureProperty().removeListener(targetTemperatureListener);
            nozzleHeater.nozzleTemperatureProperty().removeListener(extruderTemperatureListener);
        }
    }

    private void configureStartButtonForMode(CalibrationMode calibrationMode, Printer printer)
    {
        if (printer == null) {
            return;
        }
        startCalibrationButton.getTag().removeAllConditionalText();
        startCalibrationButton.getTag().addConditionalText("dialogs.cantCalibrateHeadIsDetached",
                                           Bindings.isNull(printer.headProperty()));
        switch (calibrationMode)
        {
            case NOZZLE_OPENING:
                startCalibrationButton.disableProperty().bind(
                    printer.canCalibrateNozzleOpeningProperty().not());
                 startCalibrationButton.getTag().addConditionalText("dialogs.cantPrintNoFilamentMessage",
                                           printer.extrudersProperty().get(0).
                                           filamentLoadedProperty().not());
                 startCalibrationButton.getTag().addConditionalText("dialogs.cantCalibrateNoSmartReel",
                                           Bindings.isEmpty(printer.reelsProperty()));
                break;
            case NOZZLE_HEIGHT:
                startCalibrationButton.disableProperty().bind(
                    printer.canCalibrateNozzleHeightProperty().not());
                break;
            case X_AND_Y_OFFSET:
                startCalibrationButton.disableProperty().bind(
                    printer.canCalibrateXYAlignmentProperty().not());
                break;

        }
    }

    public void setCalibrationMode(CalibrationMode calibrationMode)
    {
        this.calibrationMode = calibrationMode;
        switchToPrinter(Lookup.getCurrentlySelectedPrinterProperty().get());
        configureStartButtonForMode(calibrationMode, currentPrinter);
        switch (calibrationMode)
        {
            case NOZZLE_OPENING:
            {
                try
                {
                    stateManager = currentPrinter.startCalibrateNozzleOpening();
                } catch (PrinterException ex)
                {
                    steno.warning("Can't switch to calibration: " + ex);
                    return;
                }
            }
            calibrationNozzleOpeningGUI = new CalibrationNozzleOpeningGUI(this, stateManager);
            calibrationNozzleOpeningGUI.setState(NozzleOpeningCalibrationState.IDLE);
            break;

            case NOZZLE_HEIGHT:
            {
                try
                {
                    stateManager = currentPrinter.startCalibrateNozzleHeight();
                } catch (PrinterException ex)
                {
                    steno.warning("Can't switch to calibration: " + ex);
                    return;
                }
            }
            calibrationNozzleHeightGUI = new CalibrationNozzleHeightGUI(this, stateManager);
            calibrationNozzleHeightGUI.setState(NozzleOffsetCalibrationState.IDLE);
            break;

            case X_AND_Y_OFFSET:
            {
                try
                {
                    stateManager = currentPrinter.startCalibrateXAndY();
                } catch (PrinterException ex)
                {
                    steno.warning("Can't switch to calibration: " + ex);
                    return;
                }
            }
            calibrationXAndYGUI = new CalibrationXAndYGUI(this, stateManager);
            calibrationXAndYGUI.setState(CalibrationXAndYState.IDLE);
            break;

            case CHOICE:
                setupChoice();
        }
    }

    private void setupChoice()
    {
        calibrationStatus.setText(Lookup.i18n("calibrationPanel.chooseCalibration"));
        calibrationMenu.reset();
        hideAllInputControlsExceptStepNumber();
        stepNumber.setVisible(false);
        backToStatus.setVisible(true);
        cancelCalibrationButton.setVisible(false);
    }

    private void setupProgressBars()
    {
//        calibrationProgressPrint.setTargetLegend(Lookup.i18n("calibrationPanel.approxBuildTime"));
        calibrationProgressPrint.setTargetLegend("");
        calibrationProgressPrint.setProgressDescription(Lookup.i18n("calibrationPanel.printingCaps"));
//        calibrationProgressPrint.setTargetValue("0");
        calibrationProgressPrint.setTargetValue("");

        calibrationProgressTemp.setTargetLegend(Lookup.i18n("calibrationPanel.targetTemperature"));
        calibrationProgressTemp.setProgressDescription(Lookup.i18n("calibrationPanel.heatingCaps"));
    }

    protected void showSpinner()
    {
        DisplayManager.getInstance().startSpinning(informationCentre);
    }

    protected void hideSpinner()
    {
        DisplayManager.getInstance().stopSpinning();
    }

    @Override
    public void whenPrinterAdded(Printer printer)
    {
    }

    @Override
    public void whenPrinterRemoved(Printer printer)
    {
        if (printer == currentPrinter)
        {
            cancelCalibrationAction();
        }
    }

    @Override
    public void whenHeadAdded(Printer printer)
    {
        if (printer == currentPrinter)
        {
            bindPrinter(printer);
        }
    }

    @Override
    public void whenHeadRemoved(Printer printer, Head head)
    {
        if (printer == currentPrinter)
        {
            cancelCalibrationAction();
        }
    }

    @Override
    public void whenReelAdded(Printer printer, int reelIndex)
    {
    }

    @Override
    public void whenReelRemoved(Printer printer, Reel reel, int reelIndex)
    {
    }

    @Override
    public void whenReelChanged(Printer printer, Reel reel)
    {
    }

}
