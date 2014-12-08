package celtech.coreUI.controllers.panels;

import celtech.Lookup;
import celtech.configuration.PrinterColourMap;
import celtech.coreUI.components.PrinterIDDialog;
import celtech.coreUI.components.material.MaterialComponent;
import celtech.coreUI.components.printerstatus.PrinterComponent;
import celtech.printerControl.model.Printer;
import celtech.printerControl.model.Head;
import celtech.printerControl.model.PrinterAncillarySystems;
import celtech.printerControl.model.PrinterException;
import celtech.printerControl.model.PrinterIdentity;
import celtech.printerControl.model.Reel;
import celtech.utils.PrinterListChangesListener;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Side;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 * FXML Controller class
 *
 * @author Ian Hudson @ Liberty Systems Limited
 */
public class PrinterStatusSidePanelController implements Initializable, SidePanelManager,
    PrinterListChangesListener
{

    private final Stenographer steno = StenographerFactory.getStenographer(
        PrinterStatusSidePanelController.class.getName());

    @FXML
    private MaterialComponent material1;

    @FXML
    private MaterialComponent material2;

    @FXML
    private HBox materialContainer1;

    @FXML
    private HBox materialContainer2;

    @FXML
    private HBox temperatureChartXLabels;

    @FXML
    private GridPane legendContainer;

    @FXML
    protected LineChart<Number, Number> temperatureChart;

    @FXML
    private GridPane printerStatusGrid;

    @FXML
    private NumberAxis temperatureAxis;
    @FXML
    private NumberAxis timeAxis;

    @FXML
    private Label legendNozzle;

    @FXML
    private Label legendBed;

    @FXML
    private Label legendAmbient;

    @FXML
    private Slider speedMultiplierSlider;

    @FXML
    private HBox speedSliderHBox;

    private PrinterIDDialog printerIDDialog = null;

    private Printer selectedPrinter = null;
    private final Map<Printer, PrinterComponent> printerComponentsByPrinter = new HashMap<>();

    private final int MAX_DATA_POINTS = 210;

    private final ObservableList<Printer> connectedPrinters = Lookup.getConnectedPrinters();

    private LineChart.Series<Number, Number> currentAmbientTemperatureHistory = null;

    private ChartManager chartManager;

    private final ListChangeListener<XYChart.Data<Number, Number>> graphDataPointChangeListener = new ListChangeListener<XYChart.Data<Number, Number>>()
    {
        @Override
        public void onChanged(
            ListChangeListener.Change<? extends XYChart.Data<Number, Number>> change)
        {
            while (change.next())
            {
                if (change.wasAdded() || change.wasRemoved())
                {
                    timeAxis.setLowerBound(currentAmbientTemperatureHistory.getData().size()
                        - MAX_DATA_POINTS);
                    timeAxis.setUpperBound(currentAmbientTemperatureHistory.getData().size());
                } else if (change.wasReplaced())
                {
                } else if (change.wasUpdated())
                {
                }
            }
        }
    };

    private final PrinterColourMap colourMap = PrinterColourMap.getInstance();

    private final ChangeListener<Number> speedMultiplierListener = new ChangeListener<Number>()
    {
        @Override
        public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue)
        {
            try
            {
                selectedPrinter.changeFeedRateMultiplierDuringPrint(newValue.doubleValue());
            } catch (PrinterException ex)
            {
                steno.error("Error setting feed rate multiplier - " + ex.getMessage());
            }
        }
    };

    private final ChangeListener<Number> feedRateChangeListener = new ChangeListener<Number>()
    {
        @Override
        public void changed(
            ObservableValue<? extends Number> observable, Number oldValue, Number newValue)
        {
            speedMultiplierSlider.setValue(newValue.doubleValue());
        }
    };

    /**
     * Initializes the controller class.
     *
     * @param url
     * @param rb
     */
    @Override
    public void initialize(URL url, ResourceBundle rb)
    {
        chartManager = new ChartManager(temperatureChart);

        printerIDDialog = new PrinterIDDialog();

        materialContainer2.setVisible(false);
        speedSliderHBox.setVisible(false);

        initialiseTemperatureChart();
        initialisePrinterStatusGrid();
        controlDetailsVisibility();

        Lookup.getPrinterListChangesNotifier().addListener(this);

    }

    private void initialiseTemperatureChart()
    {
        timeAxis = new NumberAxis(0, MAX_DATA_POINTS, 30);
        timeAxis.setForceZeroInRange(false);
        timeAxis.setAutoRanging(true);

        temperatureAxis = new NumberAxis();
        temperatureAxis.setAutoRanging(false);

        temperatureChart.setAnimated(false);
        temperatureChart.setLegendVisible(false);
        temperatureChart.setLegendSide(Side.RIGHT);

        temperatureChart.setVisible(false);
    }

    private void initialisePrinterStatusGrid()
    {
        clearAndAddAllPrintersToGrid();
    }

    private void clearAndAddAllPrintersToGrid()
    {
        removeAllPrintersFromGrid();
        int row = 0;
        int column = 0;
        int columnsPerRow = 2;
        if (connectedPrinters.size() > 4)
        {
            columnsPerRow = 3;
        }
        for (Printer printer : connectedPrinters)
        {
            PrinterComponent printerComponent = createPrinterComponentForPrinter(printer);
            addPrinterComponentToGrid(printerComponent, row, column);
            column += 1;
            if (column == columnsPerRow)
            {
                column = 0;
                row += 1;
            }
        }
        // UGH shouldnt need this here but can't get PrinterComponent / Grid to negotiate size
        if (connectedPrinters.size() > 1 && connectedPrinters.size() <= 2)
        {
            printerStatusGrid.setPrefSize(260, 120);
        } else if (connectedPrinters.size() > 2 && connectedPrinters.size() <= 4)
        {
            printerStatusGrid.setPrefSize(260, 260);
        } else if (connectedPrinters.size() > 4 && connectedPrinters.size() < 7)
        {
            printerStatusGrid.setPrefSize(260, 180);
        } else
        {
            printerStatusGrid.setPrefSize(260, 260);
        }
    }

    /**
     * Add the given printer component to the given grid coordinates.
     */
    private void addPrinterComponentToGrid(PrinterComponent printerComponent, int row,
        int column)
    {
        PrinterComponent.Size size;
        if (connectedPrinters.size() > 6)
        {
            size = PrinterComponent.Size.SIZE_SMALL;
        } else if (connectedPrinters.size() > 1)
        {
            size = PrinterComponent.Size.SIZE_MEDIUM;
        } else
        {
            size = PrinterComponent.Size.SIZE_LARGE;
        }
        printerComponent.setSize(size);
        printerStatusGrid.add(printerComponent, column, row);
    }

    private void removeAllPrintersFromGrid()
    {
        for (Printer printer : connectedPrinters)
        {
            PrinterComponent printerComponent = printerComponentsByPrinter.get(printer);
            removePrinterComponentFromGrid(printerComponent);
        }
    }

    /**
     * Remove the given printer from the display. Update the selected printer to one of the remaining printers.
     */
    private void removePrinter(Printer printer)
    {
        PrinterComponent printerComponent = printerComponentsByPrinter.get(printer);
        removePrinterComponentFromGrid(printerComponent);
    }

    /**
     * Select any one of the active printers. If there are no printers left then select 'null'
     */
    private void selectOnePrinter()
    {
        if (connectedPrinters.size() > 0)
        {
            selectPrinter(connectedPrinters.get(0));
        } else
        {
            selectPrinter(null);
            Lookup.setCurrentlySelectedPrinter(null);
        }
    }

    /**
     * Make the given printer the selected printer.
     *
     * @param printer
     */
    private void selectPrinter(Printer printer)
    {
        if (selectedPrinter != null)
        {
            PrinterComponent printerComponent = printerComponentsByPrinter.get(selectedPrinter);
            printerComponent.setSelected(false);
            unbindPrinter(selectedPrinter);
            if (selectedPrinter.headProperty().get() != null)
            {
                unbindHeadProperties(selectedPrinter.headProperty().get());
            }

            selectedPrinter.reelsProperty().entrySet().stream().forEach(
                entry ->
                {
                    unbindReelProperties(entry.getKey(), entry.getValue());
                });
        }

        if (printer != null)
        {
            PrinterComponent printerComponent = printerComponentsByPrinter.get(printer);
            printerComponent.setSelected(true);
            Lookup.setCurrentlySelectedPrinter(printer);
            bindDetails(printer);
            if (printer.headProperty().get() != null)
            {
                bindHeadProperties(printer.headProperty().get());
            }

            printer.reelsProperty().entrySet().stream().forEach(
                entry ->
                {
                    bindReelProperties(entry.getKey(), entry.getValue());
                    updateReelMaterial(entry.getKey(), entry.getValue());
                });
        }
        controlDetailsVisibility();

        selectedPrinter = printer;
    }

    /**
     * This is called when the user clicks on the printer component for the given printer, and handles click (select printer) and double-click (go to edit printer details).
     *
     * @param event
     */
    private void handlePrinterClicked(MouseEvent event, Printer printer)
    {
        if (event.getClickCount() == 1)
        {
            selectPrinter(printer);
        }
        if (event.getClickCount() > 1)
        {
            showEditPrinterDetails(printer);
        }
    }

    /**
     * Show the printerIDDialog for the given printer.
     */
    private void showEditPrinterDetails(Printer printer)
    {
        if (printer != null)
        {
            printerIDDialog.setPrinterToUse(printer);
            PrinterIdentity printerIdentity = printer.getPrinterIdentity();
            printerIDDialog.setChosenDisplayColour(colourMap.printerToDisplayColour(
                printerIdentity.printerColourProperty().get()));
            printerIDDialog.setChosenPrinterName(printerIdentity.printerFriendlyNameProperty().get());

            boolean okPressed = printerIDDialog.show();

            if (okPressed)
            {
                try
                {
                    printer.updatePrinterName(printerIDDialog.getChosenPrinterName());
                    printer.updatePrinterDisplayColour(colourMap.displayToPrinterColour(
                        printerIDDialog.getChosenDisplayColour()));
                } catch (PrinterException ex)
                {
                    steno.error("Error writing printer ID");
                }
            }
        }
    }

    /**
     * Create the PrinterComponent for the given printer and set up any listeners on component events.
     */
    private PrinterComponent createPrinterComponentForPrinter(Printer printer)
    {
        PrinterComponent printerComponent = new PrinterComponent(printer);
        printerComponent.setOnMouseClicked((MouseEvent event) ->
        {
            handlePrinterClicked(event, printer);
        });
        printerComponentsByPrinter.put(printer, printerComponent);
        return printerComponent;
    }

    /**
     * Remove the given printer from the grid.
     *
     * @param printerComponent
     */
    private void removePrinterComponentFromGrid(PrinterComponent printerComponent)
    {
        printerStatusGrid.getChildren().remove(printerComponent);
    }

    private void bindDetails(Printer printer)
    {
        if (selectedPrinter != null)
        {
            unbindPrinter(selectedPrinter);
        }

        if (printer != null)
        {
            bindPrinter(printer);
        }
    }

    private void bindPrinter(Printer printer)
    {
        PrinterAncillarySystems ancillarySystems = printer.getPrinterAncillarySystems();
        currentAmbientTemperatureHistory = ancillarySystems.getAmbientTemperatureHistory();
        
        chartManager.setLegendLabels(legendNozzle, legendBed, legendAmbient);

        chartManager.setAmbientData(ancillarySystems.getAmbientTemperatureHistory());
        chartManager.setBedData(ancillarySystems.getBedTemperatureHistory());
        chartManager.setAmbientTemperatureProperty(ancillarySystems.ambientTemperatureProperty());
        chartManager.setBedTemperatureProperty(ancillarySystems.bedTemperatureProperty());

        chartManager.setTargetAmbientTemperatureProperty(
            ancillarySystems.ambientTargetTemperatureProperty());
        chartManager.setBedHeaterModeProperty(ancillarySystems.bedHeaterModeProperty());
        chartManager.setTargetBedTemperatureProperty(ancillarySystems.bedTargetTemperatureProperty());
        chartManager.setTargetBedFirstLayerTemperatureProperty(ancillarySystems.bedFirstLayerTargetTemperatureProperty());

        speedSliderHBox.visibleProperty().bind(printer.canChangeFilamentInfoProperty());
        speedMultiplierSlider.valueProperty().addListener(speedMultiplierListener);

        materialContainer2.visibleProperty().bind(printer.getPrinterAncillarySystems().dualReelAdaptorPresentProperty());
        
        printer.getPrinterAncillarySystems().feedRateMultiplierProperty().addListener(feedRateChangeListener);
    }

    private void unbindPrinter(Printer printer)
    {
        if (printer.headProperty().get() != null)
        {
            unbindHeadProperties(printer.headProperty().get());
        }

        printer.reelsProperty().entrySet().stream().forEach(
            entry ->
            {
                unbindReelProperties(entry.getKey(), entry.getValue());
            });

        chartManager.clearAmbientData();
        chartManager.clearBedData();
        chartManager.clearLegendLabels();
        currentAmbientTemperatureHistory = null;

        speedSliderHBox.visibleProperty().unbind();
        speedSliderHBox.setVisible(false);
        speedMultiplierSlider.valueProperty().removeListener(speedMultiplierListener);

        materialContainer2.visibleProperty().unbind();

        printer.getPrinterAncillarySystems().feedRateMultiplierProperty().removeListener(feedRateChangeListener);
    }

    private ChangeListener<Object> reelListener;

    private void unbindReelProperties(int reelNumber, Reel reel)
    {
        if (reel != null)
        {
            reel.friendlyFilamentNameProperty().removeListener(reelListener);
            reel.displayColourProperty().removeListener(reelListener);
            reel.remainingFilamentProperty().removeListener(reelListener);
            reel.diameterProperty().removeListener(reelListener);
            reel.materialProperty().removeListener(reelListener);
        }
    }

    private void bindReelProperties(int reelNumber, Reel reel)
    {
        if (reel != null)
        {
            reelListener = (ObservableValue<? extends Object> observable, Object oldValue, Object newValue) ->
            {
                updateReelMaterial(reelNumber, reel);
            };
            reel.friendlyFilamentNameProperty().addListener(reelListener);
            reel.displayColourProperty().addListener(reelListener);
            reel.remainingFilamentProperty().addListener(reelListener);
            reel.diameterProperty().addListener(reelListener);
            reel.materialProperty().addListener(reelListener);
        }
    }

    private void unbindHeadProperties(Head head)
    {
        chartManager.clearNozzleData();
    }

    private void bindHeadProperties(Head head)
    {
        head.getNozzleHeaters().get(0).getNozzleTemperatureHistory().getData().addListener(
            graphDataPointChangeListener);

        chartManager.setNozzleTemperatureProperty(head.getNozzleHeaters().get(0).nozzleTemperatureProperty());
        chartManager.setNozzleData(head.getNozzleHeaters().get(0).getNozzleTemperatureHistory());
        chartManager.setTargetNozzleTemperatureProperty(
            head.getNozzleHeaters().get(0).nozzleTargetTemperatureProperty());
        chartManager.setTargetFirstLayerNozzleTemperatureProperty(
            head.getNozzleHeaters().get(0).nozzleFirstLayerTargetTemperatureProperty());
        chartManager.setNozzleHeaterModeProperty(head.getNozzleHeaters().get(0).heaterModeProperty());
    }

    /**
     * Update the material component with the appropriate details.
     */
    private void updateReelMaterial(int reelNumber, Reel reel)
    {
        MaterialComponent materialComponent = reelNumber == 0 ? material1 : material2;
        if (reel == null)
        {
            materialComponent.showFilamentNotLoaded();
        } else
        {
            materialComponent.setMaterial(reelNumber, reel.materialProperty().get(),
                                          reel.friendlyFilamentNameProperty().get(),
                                          reel.displayColourProperty().get(),
                                          reel.remainingFilamentProperty().get(),
                                          reel.diameterProperty().get());
        }
//            material1.showReelNotFormatted();
    }

    private void controlDetailsVisibility()
    {
        boolean visible = connectedPrinters.size() > 0;

        temperatureChart.setVisible(visible);
        temperatureChartXLabels.setVisible(visible);
        materialContainer1.setVisible(visible);

        legendContainer.setVisible(visible);
    }

    @Override
    public void configure(Initializable slideOutController)
    {
    }

    @Override
    public void whenPrinterAdded(Printer printer)
    {
        clearAndAddAllPrintersToGrid();
        selectPrinter(printer);
        controlDetailsVisibility();
        updateReelMaterial(0, null);
        updateReelMaterial(1, null);
    }

    @Override
    public void whenPrinterRemoved(Printer printer)
    {
        removePrinter(printer);
        clearAndAddAllPrintersToGrid();
        selectOnePrinter();
        controlDetailsVisibility();
    }

    @Override
    public void whenHeadAdded(Printer printer)
    {
        if (printer == selectedPrinter)
        {
            Head head = printer.headProperty().get();
            bindHeadProperties(head);
        }
    }

    @Override
    public void whenHeadRemoved(Printer printer, Head head)
    {
        if (printer == selectedPrinter)
        {
            unbindHeadProperties(head);
        }
    }

    @Override
    public void whenReelAdded(Printer printer, int reelIndex)
    {
        if (printer == selectedPrinter)
        {
            Reel reel = printer.reelsProperty().get(reelIndex);
            bindReelProperties(reelIndex, reel);
            updateReelMaterial(reelIndex, reel);
        }
    }

    @Override
    public void whenReelRemoved(Printer printer, Reel reel, int reelNumber)
    {
        if (printer == selectedPrinter)
        {
            unbindReelProperties(reelNumber, reel);
            updateReelMaterial(reelNumber, null);
        }
    }

    @Override
    public void whenReelChanged(Printer printer, Reel reel)
    {
    }

}
