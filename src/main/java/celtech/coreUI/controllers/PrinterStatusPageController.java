package celtech.coreUI.controllers;

import celtech.Lookup;
import celtech.configuration.ApplicationConfiguration;
import celtech.roboxbase.configuration.Filament;
import celtech.roboxbase.comms.remote.PauseStatus;
import celtech.roboxbase.PrinterColourMap;
import celtech.roboxbase.configuration.datafileaccessors.FilamentContainer;
import celtech.coreUI.components.HyperlinkedLabel;
import celtech.coreUI.components.JogButton;
import celtech.coreUI.controllers.utilityPanels.OuterPanelController;
import celtech.roboxbase.BaseLookup;
import celtech.roboxbase.printerControl.PrinterStatus;
import celtech.roboxbase.printerControl.model.Head;
import celtech.roboxbase.printerControl.model.Printer;
import celtech.roboxbase.printerControl.model.PrinterException;
import celtech.roboxbase.printerControl.model.PrinterListChangesListener;
import celtech.roboxbase.printerControl.model.Reel;
import java.io.IOException;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.ResourceBundle;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.MapChangeListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;
import celtech.coreUI.DisplayManager;

/**
 * FXML Controller class
 *
 * @author Ian Hudson @ Liberty Systems Limited
 */
public class PrinterStatusPageController implements Initializable, PrinterListChangesListener
{

    private final Stenographer steno = StenographerFactory.getStenographer(
            PrinterStatusPageController.class.getName());
    private Printer printerToUse = null;
    private ChangeListener<Color> printerColourChangeListener = null;
    private ChangeListener<PrinterStatus> printerStatusChangeListener = null;
    private ChangeListener<PauseStatus> pauseStatusChangeListener = null;

    private String transferringDataString = null;

    private PrinterColourMap colourMap = PrinterColourMap.getInstance();

    private NumberFormat threeDPformatter;
    private NumberFormat fiveDPformatter;

    @FXML
    private AnchorPane container;

    @FXML
    private StackPane statusPane;

    @FXML
    private ImageView baseNoReels;

    @FXML
    private ImageView baseReel2;

    @FXML
    private ImageView baseReel1;

    @FXML
    private ImageView reel1Background;
    private ColorAdjust reel1BackgroundColourEffect = new ColorAdjust();

    @FXML
    private ImageView reel2Background;
    private ColorAdjust reel2BackgroundColourEffect = new ColorAdjust();

    @FXML
    private ImageView baseReelBoth;

    @FXML
    private ImageView doorClosed;

    @FXML
    private ImageView doorOpen;

    @FXML
    private ImageView singleMaterialHead;

    @FXML
    private ImageView ambientLight;

    private ColorAdjust ambientColourEffect = new ColorAdjust();

    @FXML
    private ImageView dualMaterialHead;

    @FXML
    private ImageView bed;

    @FXML
    private Group temperatureWarning;

    @FXML
    private VBox extruder1Controls;

    @FXML
    private VBox extruder2Controls;

    @FXML
    private HBox xAxisControls;

    @FXML
    private VBox yAxisControls;

    @FXML
    private VBox zAxisControls;

    @FXML
    private VBox disconnectedText;

    @FXML
    private HyperlinkedLabel disconnectedLinkedText;

    private Node[] advancedControls = null;

    private Printer lastSelectedPrinter = null;

    private VBox vBoxLeft = new VBox();
    private VBox vBoxRight = new VBox();
    private VBox gcodePanel = null;
    private VBox diagnosticPanel = null;
    private VBox projectPanel = null;
    private VBox printAdjustmentsPanel = null;
    private VBox parentPanel = null;

    private BooleanProperty selectedPrinterIsPrinting = new SimpleBooleanProperty(false);
    private BooleanProperty projectPanelShouldBeVisible = new SimpleBooleanProperty(true);
    private BooleanProperty projectPanelVisibility = new SimpleBooleanProperty(false);

    private final MapChangeListener<Integer, Filament> effectiveFilamentListener = (MapChangeListener.Change<? extends Integer, ? extends Filament> change) ->
    {
        setupBaseDisplay();
    };

    private final ChangeListener<Boolean> filamentLoadedListener = (ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) ->
    {
        setupBaseDisplay();
    };

    @FXML
    void jogButton(ActionEvent event)
    {
        JogButton button = (JogButton) event.getSource();

        try
        {
            printerToUse.jogAxis(button.getAxis(), button.getDistance(), button.getFeedRate(),
                    button.getUseG1());
        } catch (PrinterException ex)
        {
            steno.error("Failed to jog printer - " + ex.getMessage());
        }
    }

    private void displayScaleChanged(DisplayManager.DisplayScalingMode scalingMode)
    {
        switch (scalingMode)
        {
            case VERY_SHORT:
            case SHORT:
                projectPanelShouldBeVisible.set(false);
                break;
            case NORMAL:
                projectPanelShouldBeVisible.set(true);
                break;
        }
        resizePrinterDisplay(parentPanel);
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
        BaseLookup.getPrinterListChangesNotifier().addListener(this);

        Lookup.getUserPreferences().advancedModeProperty().addListener(
                (ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) ->
                {
                    setAdvancedControlsVisibility();
                });

        threeDPformatter = DecimalFormat.getNumberInstance(Locale.UK);
        threeDPformatter.setMaximumFractionDigits(3);
        threeDPformatter.setGroupingUsed(false);

        fiveDPformatter = DecimalFormat.getNumberInstance(Locale.UK);
        fiveDPformatter.setMaximumFractionDigits(5);
        fiveDPformatter.setGroupingUsed(false);

        transferringDataString = Lookup.i18n("PrintQueue.SendingToPrinter");

        printerColourChangeListener = (ObservableValue<? extends Color> observable, Color oldValue, Color newValue) ->
        {
            setupAmbientLight();
        };

        printerStatusChangeListener = (ObservableValue<? extends PrinterStatus> observable, PrinterStatus oldValue, PrinterStatus newValue) ->
        {
            setAdvancedControlsVisibility();
        };

        pauseStatusChangeListener = (ObservableValue<? extends PauseStatus> observable, PauseStatus oldValue, PauseStatus newValue) ->
        {
            setAdvancedControlsVisibility();
        };

        doorClosed.setVisible(false);
        doorOpen.setVisible(false);

        ambientLight.setEffect(ambientColourEffect);
        reel1Background.setEffect(reel1BackgroundColourEffect);
        reel2Background.setEffect(reel2BackgroundColourEffect);

        temperatureWarning.setVisible(false);

        advancedControls = new Node[]
        {
            xAxisControls,
            yAxisControls,
            zAxisControls
        };

        setupBaseDisplay();
        setupAmbientLight();
        setupHead();

        AnchorPane.setTopAnchor(vBoxLeft, 30.0);
        AnchorPane.setBottomAnchor(vBoxLeft, 30.0);
        loadInsetPanels();

        Lookup.getSelectedPrinterProperty().addListener(
                new ChangeListener<Printer>()
                {
                    @Override
                    public void changed(ObservableValue<? extends Printer> ov,
                            Printer t, Printer selectedPrinter)
                    {
                        printerToUse = selectedPrinter;

                        setupBaseDisplay();

                        if (selectedPrinter == null)
                        {
                            unbindFromSelectedPrinter();

                            setupBaseDisplay();
                            setupAmbientLight();

                            temperatureWarning.setVisible(false);
                        } else
                        {
                            unbindFromSelectedPrinter();

                            setupBaseDisplay();
                            setupAmbientLight();
                            selectedPrinter.getPrinterIdentity().printerColourProperty().addListener(
                                    printerColourChangeListener);

                            temperatureWarning.visibleProperty().bind(
                                    selectedPrinter.getPrinterAncillarySystems().bedTemperatureProperty()
                                    .greaterThan(ApplicationConfiguration.bedHotAboveDegrees));

                            selectedPrinter.printerStatusProperty().addListener(
                                    printerStatusChangeListener);
                            selectedPrinter.pauseStatusProperty().addListener(
                                    pauseStatusChangeListener);
                            doorOpen.visibleProperty().bind(selectedPrinter.
                                    getPrinterAncillarySystems().doorOpenProperty());
                            doorClosed.visibleProperty().bind(selectedPrinter.
                                    getPrinterAncillarySystems().doorOpenProperty().not());

                            selectedPrinter.effectiveFilamentsProperty().addListener(effectiveFilamentListener);

                            selectedPrinter.extrudersProperty().forEach(extruder ->
                                    {
                                        extruder.filamentLoadedProperty().addListener(filamentLoadedListener);
                            });
                        }

                        lastSelectedPrinter = selectedPrinter;
                    }
                });

        disconnectedLinkedText.replaceText(Lookup.i18n("printerStatus.noPrinterAttached"));
        
        projectPanelVisibility.bind(projectPanelShouldBeVisible.and(selectedPrinterIsPrinting));

        DisplayManager.getInstance().getDisplayScalingModeProperty().addListener(new ChangeListener<DisplayManager.DisplayScalingMode>()
        {

            @Override
            public void changed(ObservableValue<? extends DisplayManager.DisplayScalingMode> ov, DisplayManager.DisplayScalingMode t, DisplayManager.DisplayScalingMode t1)
            {
                displayScaleChanged(t1);
    }
        });

        displayScaleChanged(DisplayManager.getInstance().getDisplayScalingModeProperty().get());
    }

    private void setupBaseDisplay()
    {
        if (printerToUse == null)
        {
            baseNoReels.setVisible(false);
            baseReel1.setVisible(false);
            baseReel2.setVisible(false);
            baseReelBoth.setVisible(false);
            bed.setVisible(false);
            vBoxLeft.setVisible(false);
            vBoxRight.setVisible(false);
            disconnectedText.setVisible(true);
        } else
        {
            vBoxLeft.setVisible(true);
            vBoxRight.setVisible(true);
            disconnectedText.setVisible(false);

            if (((printerToUse.extrudersProperty().get(0).filamentLoadedProperty().get() && printerToUse.extrudersProperty().get(0).isFittedProperty().get())
                    || (printerToUse.effectiveFilamentsProperty().containsKey(0) && printerToUse.effectiveFilamentsProperty().get(0) != FilamentContainer.UNKNOWN_FILAMENT))
                    && ((printerToUse.extrudersProperty().get(1).filamentLoadedProperty().get() && printerToUse.extrudersProperty().get(1).isFittedProperty().get())
                    || (printerToUse.effectiveFilamentsProperty().containsKey(1) && printerToUse.effectiveFilamentsProperty().get(1) != FilamentContainer.UNKNOWN_FILAMENT)))
            {
                baseNoReels.setVisible(false);
                baseReel1.setVisible(false);
                baseReel2.setVisible(false);
                baseReelBoth.setVisible(true);
                bed.setVisible(true);
            } else if (((printerToUse.extrudersProperty().get(0).filamentLoadedProperty().get() && printerToUse.extrudersProperty().get(0).isFittedProperty().get())
                    || (printerToUse.effectiveFilamentsProperty().containsKey(0) && printerToUse.effectiveFilamentsProperty().get(0) != FilamentContainer.UNKNOWN_FILAMENT))
                    && (!(printerToUse.extrudersProperty().get(1).filamentLoadedProperty().get() && printerToUse.extrudersProperty().get(1).isFittedProperty().get())
                    || (!printerToUse.effectiveFilamentsProperty().containsKey(1) && printerToUse.effectiveFilamentsProperty().get(1) != FilamentContainer.UNKNOWN_FILAMENT)))
            {
                baseNoReels.setVisible(false);
                baseReel1.setVisible(true);
                baseReel2.setVisible(false);
                baseReelBoth.setVisible(false);
                bed.setVisible(true);
            } else if (((printerToUse.extrudersProperty().get(1).filamentLoadedProperty().get() && printerToUse.extrudersProperty().get(1).isFittedProperty().get())
                    || (printerToUse.effectiveFilamentsProperty().containsKey(1) && printerToUse.effectiveFilamentsProperty().get(1) != FilamentContainer.UNKNOWN_FILAMENT))
                    && (!(printerToUse.extrudersProperty().get(0).filamentLoadedProperty().get() && printerToUse.extrudersProperty().get(0).isFittedProperty().get())
                    || (!printerToUse.effectiveFilamentsProperty().containsKey(0) && printerToUse.effectiveFilamentsProperty().get(0) != FilamentContainer.UNKNOWN_FILAMENT)))
            {
                baseNoReels.setVisible(false);
                baseReel1.setVisible(false);
                baseReel2.setVisible(true);
                baseReelBoth.setVisible(false);
                bed.setVisible(true);
            } else
            {
                baseNoReels.setVisible(true);
                baseReel1.setVisible(false);
                baseReel2.setVisible(false);
                baseReelBoth.setVisible(false);
                bed.setVisible(true);
            }
        }

        setupReel1Colour();
        setupReel2Colour();
        
        setAdvancedControlsVisibility();

        resizePrinterDisplay(parentPanel);
    }

    private void setColorAdjustFromDesiredColour(ColorAdjust effect, Color desiredColor)
    {
        effect.setHue(hueConverter(desiredColor.getHue()));
        effect.setBrightness(desiredColor.getBrightness() - 1);
        effect.setSaturation(desiredColor.getSaturation());
//        steno.info("Colour - h=" + hueConverter(desiredColor.getHue()) + " s=" + desiredColor.getSaturation() + " b" + desiredColor.getBrightness());
    }

    private void setupReel1Colour()
    {
        if (printerToUse == null
                || !printerToUse.effectiveFilamentsProperty().containsKey(0)
                || printerToUse.effectiveFilamentsProperty().get(0) == null
                || printerToUse.effectiveFilamentsProperty().get(0) == FilamentContainer.UNKNOWN_FILAMENT)
        {
            reel1Background.setVisible(false);
        } else
        {
            Color reel1Colour = printerToUse.effectiveFilamentsProperty().get(0).getDisplayColour();
            setColorAdjustFromDesiredColour(reel1BackgroundColourEffect, reel1Colour);
            reel1Background.setVisible(true);
        }
    }

    private void setupReel2Colour()
    {
        if (printerToUse == null
                || !printerToUse.effectiveFilamentsProperty().containsKey(1)
                || printerToUse.effectiveFilamentsProperty().get(1) == null
                || printerToUse.effectiveFilamentsProperty().get(1) == FilamentContainer.UNKNOWN_FILAMENT)
        {
            reel2Background.setVisible(false);
        } else
        {
            Color reel2Colour = printerToUse.effectiveFilamentsProperty().get(1).getDisplayColour();
            setColorAdjustFromDesiredColour(reel2BackgroundColourEffect, reel2Colour);
            reel2Background.setVisible(true);
        }
    }

    private double hueConverter(double hueCyl)
    {
        double returnedHue = 0;
        if (hueCyl <= 180)
        {
            returnedHue = hueCyl / 180.0;
        } else
        {
            returnedHue = -(360 - hueCyl) / 180.0;
        }
        return returnedHue;
    }

    private void setupAmbientLight()
    {
        if (printerToUse == null)
        {
            ambientLight.setVisible(false);
        } else
        {
            Color ambientColour = colourMap.printerToDisplayColour(printerToUse.getPrinterIdentity().printerColourProperty().get());
            setColorAdjustFromDesiredColour(ambientColourEffect, ambientColour);
            ambientLight.setVisible(true);
        }
    }

    private void setupHead()
    {
        if (printerToUse == null
                || printerToUse.headProperty().get() == null)
        {
            singleMaterialHead.setVisible(false);
            dualMaterialHead.setVisible(false);
        } else
        {
            if (printerToUse.headProperty().get().headTypeProperty().get() == Head.HeadType.SINGLE_MATERIAL_HEAD)
            {
                singleMaterialHead.setVisible(true);
                dualMaterialHead.setVisible(false);
            } else if (printerToUse.headProperty().get().headTypeProperty().get() == Head.HeadType.DUAL_MATERIAL_HEAD)
            {
                singleMaterialHead.setVisible(false);
                dualMaterialHead.setVisible(true);
            } else
            {
                singleMaterialHead.setVisible(false);
                dualMaterialHead.setVisible(false);
            }
        }
    }

    private void setAdvancedControlsVisibility()
    {
        boolean visible = false;

        if (printerToUse != null
                && Lookup.getUserPreferences().isAdvancedMode())
        {
            switch (printerToUse.printerStatusProperty().get())
            {
                case IDLE:
                    visible = true;
                    selectedPrinterIsPrinting.set(false);
                    break;
                case PRINTING_PROJECT:
                    selectedPrinterIsPrinting.set(true);
                    break;
                default:
                    selectedPrinterIsPrinting.set(false);
                    break;
            }

            switch (printerToUse.pauseStatusProperty().get())
            {
                case PAUSED:
                    visible = true;
                    break;
                case PAUSE_PENDING:
                case RESUME_PENDING:
                    visible = false;
                    break;
                default:
                    break;
            }
        } else
        {
            selectedPrinterIsPrinting.set(false);
        }

        for (Node node : advancedControls)
        {
            node.setVisible(visible);
        }

        extruder1Controls.setVisible(Lookup.getUserPreferences().advancedModeProperty().get()
                && visible
                && printerToUse.extrudersProperty().get(0).filamentLoadedProperty().get()
                && printerToUse.extrudersProperty().get(0).isFittedProperty().get());
        extruder2Controls.setVisible(Lookup.getUserPreferences().advancedModeProperty().get()
                && visible
                && printerToUse.extrudersProperty().get(1).filamentLoadedProperty().get()
                && printerToUse.extrudersProperty().get(1).isFittedProperty().get());
    }

    /**
     *
     * @param parent
     */
    public void configure(VBox parent)
    {
        parentPanel = parent;

        parent.widthProperty().addListener(new ChangeListener<Number>()
        {
            @Override
            public void changed(ObservableValue<? extends Number> observable,
                    Number oldValue, Number newValue)
            {
                resizePrinterDisplay(parentPanel);
            }
        });
        parent.heightProperty().addListener(new ChangeListener<Number>()
        {
            @Override
            public void changed(ObservableValue<? extends Number> observable,
                    Number oldValue, Number newValue)
            {
                resizePrinterDisplay(parentPanel);
            }
        });
    }

    private void resizePrinterDisplay(VBox parent)
    {
        if (parent != null)
        {
        final double beginWidth = 1500;
        final double beginHeight = 1106;
        final double aspect = beginWidth / beginHeight;
            boolean lhPanelVisible = gcodePanel.isVisible() || diagnosticPanel.isVisible();
            boolean rhPanelVisible = projectPanel.isVisible() || printAdjustmentsPanel.isVisible();
            double fudgeFactor = (baseReel2.isVisible() || baseReelBoth.isVisible()) ? 600 : 300;
            double lefthandPanelWidthToSubtract = (lhPanelVisible || rhPanelVisible) ? fudgeFactor : 0.0;
            double parentWidth = parent.getWidth() - lefthandPanelWidthToSubtract;
        double parentHeight = parent.getHeight();
        double displayAspect = parentWidth / parentHeight;

        double newWidth = 0;
        double newHeight = 0;

        if (displayAspect >= aspect)
        {
            // Drive from height
            newWidth = parentHeight * aspect;
            newHeight = parentHeight;
        } else
        {
            //Drive from width
            newHeight = parentWidth / aspect;
            newWidth = parentWidth;
        }

        double xScale = Double.max((newWidth / beginWidth), 0.4);
        double yScale = Double.max((newHeight / beginHeight), 0.4);

        statusPane.setScaleX(xScale);
        statusPane.setScaleY(yScale);
    }
    }

    private void unbindFromSelectedPrinter()
    {
        if (lastSelectedPrinter != null)
        {
            lastSelectedPrinter.getPrinterIdentity().printerColourProperty().removeListener(
                    printerColourChangeListener);
            lastSelectedPrinter.printerStatusProperty().removeListener(printerStatusChangeListener);
            lastSelectedPrinter.pauseStatusProperty().removeListener(pauseStatusChangeListener);
            lastSelectedPrinter.effectiveFilamentsProperty().removeListener(effectiveFilamentListener);
            lastSelectedPrinter.extrudersProperty().forEach(extruder ->
            {
                extruder.filamentLoadedProperty().removeListener(filamentLoadedListener);
            });
        }

        temperatureWarning.visibleProperty().unbind();
        temperatureWarning.setVisible(false);

        doorOpen.visibleProperty().unbind();
        doorOpen.setVisible(false);
        doorClosed.visibleProperty().unbind();
        doorClosed.setVisible(false);
    }

    private VBox loadInsetPanel(String innerPanelFXMLName, String title,
            BooleanProperty visibleProperty,
            ObservableValue<Boolean> appearanceConditions,
            VBox parentPanel,
            int position)
    {
        URL insetPanelURL = getClass().getResource(
                ApplicationConfiguration.fxmlUtilityPanelResourcePath + innerPanelFXMLName);
        FXMLLoader loader = new FXMLLoader(insetPanelURL, BaseLookup.getLanguageBundle());
        VBox wrappedPanel = null;
        try
        {
            VBox insetPanel = loader.load();
            if (title != null)
            {
                wrappedPanel = wrapPanelInOuterPanel(insetPanel, title, visibleProperty);
                if (appearanceConditions != null)
                {
                wrappedPanel.visibleProperty().bind(appearanceConditions);
                }

                final VBox panelToChangeHeightOf = wrappedPanel;
                panelVisibilityAction((visibleProperty != null) ? visibleProperty.getValue() : false, panelToChangeHeightOf, parentPanel, position);
                wrappedPanel.visibleProperty().addListener(new ChangeListener<Boolean>()
                {
                    @Override
                    public void changed(ObservableValue<? extends Boolean> ov, Boolean t, Boolean visible)
                    {
                        panelVisibilityAction(visible, panelToChangeHeightOf, parentPanel, position);
                    }
                });
            } else
            {
                wrappedPanel = insetPanel;
            }
        } catch (IOException ex)
        {
            steno.exception("Unable to load inset panel: " + innerPanelFXMLName, ex);
        }
        return wrappedPanel;
    }

    private void panelVisibilityAction(boolean visible, VBox panel, VBox parentPanel, int position)
    {
        if (visible)
        {
            if (!parentPanel.getChildren().contains(panel))
            {
                if (position <= parentPanel.getChildren().size())
                {
                    parentPanel.getChildren().add(position, panel);
                } else
                {
                    parentPanel.getChildren().add(panel);
                }
            }
        } else
        {
            parentPanel.getChildren().remove(panel);
        }
    }

    private VBox wrapPanelInOuterPanel(Node insetPanel, String title,
            BooleanProperty visibleProperty)
    {
        URL outerPanelURL = getClass().getResource(
                ApplicationConfiguration.fxmlUtilityPanelResourcePath + "outerStatusPanel.fxml");
        FXMLLoader loader = new FXMLLoader(outerPanelURL, BaseLookup.getLanguageBundle());
        VBox outerPanel = null;
        try
        {
            outerPanel = loader.load();
            OuterPanelController outerPanelController = loader.getController();
            outerPanelController.setInnerPanel(insetPanel);
            outerPanelController.setTitle(Lookup.i18n(title));
            outerPanelController.setPreferredVisibility(visibleProperty);
        } catch (IOException ex)
        {
            steno.exception("Unable to load outer panel", ex);
        }
        return outerPanel;
    }

    private void loadInsetPanels()
    {
        vBoxLeft.setSpacing(20);
        diagnosticPanel = loadInsetPanel("DiagnosticPanel.fxml", "diagnosticPanel.title",
                Lookup.getUserPreferences().showDiagnosticsProperty(),
                Lookup.getUserPreferences().showDiagnosticsProperty(), vBoxLeft, 0);

        gcodePanel = loadInsetPanel("GCodePanel.fxml", "gcodeEntry.title",
                Lookup.getUserPreferences().showGCodeProperty(),
                Lookup.getUserPreferences().showGCodeProperty().and(Lookup.getUserPreferences().advancedModeProperty()), vBoxLeft, 1);
        gcodePanel.visibleProperty().addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) ->
        {
            resizePrinterDisplay(parentPanel);
        });

        vBoxRight.setSpacing(20);
        projectPanel = loadInsetPanel("ProjectPanel.fxml", "projectPanel.title", null, projectPanelVisibility, vBoxRight, 0);
        projectPanel.visibleProperty().addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) ->
        {
            resizePrinterDisplay(parentPanel);
        });

        printAdjustmentsPanel = loadInsetPanel("tweakPanel.fxml", "printAdjustmentsPanel.title",
                Lookup.getUserPreferences().showAdjustmentsProperty(),
                Lookup.getUserPreferences().showAdjustmentsProperty().and(selectedPrinterIsPrinting), vBoxRight, 1);
        printAdjustmentsPanel.visibleProperty().addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) ->
        {
            resizePrinterDisplay(parentPanel);
        });

        container.getChildren().add(vBoxLeft);
        AnchorPane.setTopAnchor(vBoxLeft, 30.0);
        AnchorPane.setLeftAnchor(vBoxLeft, 30.0);
        AnchorPane.setBottomAnchor(vBoxLeft, 90.0);
        container.getChildren().add(vBoxRight);
        AnchorPane.setTopAnchor(vBoxRight, 30.0);
        AnchorPane.setRightAnchor(vBoxRight, 30.0);

    }

    @Override
    public void whenPrinterAdded(Printer printer)
    {
    }

    @Override
    public void whenPrinterRemoved(Printer printer)
    {
    }

    @Override
    public void whenHeadAdded(Printer printer)
    {
        setupHead();
    }

    @Override
    public void whenHeadRemoved(Printer printer, Head head)
    {
        setupHead();
    }

    @Override
    public void whenReelAdded(Printer printer, int reelIndex)
    {
        setupBaseDisplay();
    }

    @Override
    public void whenReelRemoved(Printer printer, Reel reel, int reelIndex)
    {
        setupBaseDisplay();
    }

    @Override
    public void whenReelChanged(Printer printer, Reel reel)
    {
        setupBaseDisplay();
    }

    @Override
    public void whenExtruderAdded(Printer printer, int extruderIndex)
    {
    }

    @Override
    public void whenExtruderRemoved(Printer printer, int extruderIndex)
    {
    }

}
