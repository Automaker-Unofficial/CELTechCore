package celtech.coreUI.controllers.panels;

import celtech.Lookup;
import celtech.configuration.CustomSlicerType;
import celtech.coreUI.components.RestrictedNumberField;
import celtech.coreUI.components.RestrictedTextField;
import celtech.roboxbase.BaseLookup;
import celtech.roboxbase.configuration.datafileaccessors.HeadContainer;
import celtech.roboxbase.configuration.SlicerType;
import celtech.roboxbase.configuration.datafileaccessors.SlicerParametersContainer;
import celtech.roboxbase.configuration.fileRepresentation.HeadFile;
import celtech.roboxbase.configuration.fileRepresentation.NozzleData;
import celtech.roboxbase.configuration.fileRepresentation.SlicerMappings;
import celtech.roboxbase.configuration.fileRepresentation.SlicerParametersFile;
import celtech.roboxbase.configuration.slicer.FillPattern;
import celtech.roboxbase.configuration.slicer.NozzleParameters;
import celtech.roboxbase.configuration.slicer.SupportPattern;
import celtech.roboxbase.printerControl.model.Head;
import celtech.roboxbase.printerControl.model.Head.ValveType;
import celtech.roboxbase.printerControl.model.Printer;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.beans.binding.BooleanBinding;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Slider;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 * FXML Controller class
 *
 * @author Ian Hudson @ Liberty Systems Limited
 */
public class ProfileLibraryPanelController implements Initializable, MenuInnerPanel
{

    private final PseudoClass ERROR = PseudoClass.getPseudoClass("error");

    enum State
    {

        /**
         * Editing a new profile that has not yet been saved
         */
        NEW,
        /**
         * Editing a custom profile
         */
        CUSTOM,
        /**
         * Viewing a standard profile
         */
        ROBOX
    };

    private final ObjectProperty<ProfileLibraryPanelController.State> state = new SimpleObjectProperty<>();
    private final BooleanProperty isDirty = new SimpleBooleanProperty(false);

    private final BooleanProperty isEditable = new SimpleBooleanProperty(false);
    private final BooleanProperty canSave = new SimpleBooleanProperty(false);
    private final BooleanProperty canSaveAs = new SimpleBooleanProperty(false);
    private final BooleanProperty canDelete = new SimpleBooleanProperty(false);
    private final BooleanProperty isNameValid = new SimpleBooleanProperty(false);
    private String currentProfileName;
    private final StringProperty currentHeadType = new SimpleStringProperty();
    private final IntegerProperty numNozzleHeaters = new SimpleIntegerProperty();
    private final IntegerProperty numNozzles = new SimpleIntegerProperty();
    private final BooleanProperty hasValves = new SimpleBooleanProperty(false);

    private final Stenographer steno = StenographerFactory.getStenographer(
            ProfileLibraryPanelController.class.getName());

    @FXML
    private VBox container;

    @FXML
    private ComboBox<String> cmbHeadType;

    @FXML
    private ComboBox<SlicerParametersFile> cmbPrintProfile;

    @FXML
    private RestrictedNumberField fillDensity;

    @FXML
    private GridPane supportGrid;

    @FXML
    private RestrictedNumberField perimeterExtrusionWidth;

    @FXML
    private RestrictedNumberField enableFanIfLayerTimeBelow;

    @FXML
    private RestrictedNumberField supportOverhangThreshold;

    @FXML
    private ComboBox<String> supportNozzleChoice;

    @FXML
    private RestrictedNumberField firstLayerSpeed;

    @FXML
    private RestrictedNumberField perimeterSpeed;

    @FXML
    private GridPane coolingGrid;

    @FXML
    private GridPane speedGrid;

    @FXML
    private RestrictedTextField profileNameField;

    @FXML
    private ComboBox<String> supportInterfaceNozzleChoice;

    @FXML
    private RestrictedNumberField supportMaterialSpeed;

    @FXML
    private RestrictedNumberField minFanSpeed;

    @FXML
    private RestrictedNumberField solidLayersBottom;

    @FXML
    private RestrictedNumberField solidLayersTop;

    @FXML
    private ComboBox<String> perimeterNozzleChoice;

    @FXML
    private RestrictedNumberField smallPerimeterSpeed;

    @FXML
    private RestrictedNumberField disableFanForFirstNLayers;

    @FXML
    private Slider topSolidInfillExtrusionWidthSlider;

    @FXML
    private ComboBox<SupportPattern> supportPattern;

    @FXML
    private Slider supportExtrusionWidthSlider;

    @FXML
    private RestrictedNumberField bridgesSpeed;

    @FXML
    private RestrictedNumberField layerHeight;

    @FXML
    private GridPane extrusionGrid;

    @FXML
    private GridPane extrusionControls;

    @FXML
    private GridPane nozzleControls;

    @FXML
    private RestrictedNumberField supportPatternAngle;

    @FXML
    private RestrictedNumberField infillExtrusionWidth;

    @FXML
    private RestrictedNumberField slowFanIfLayerTimeBelow;

    @FXML
    private Slider firstLayerExtrusionWidthSlider;

    @FXML
    private CheckBox enableAutoCooling;

    @FXML
    private RestrictedNumberField solidInfillSpeed;

    @FXML
    private Label nozzlePartialOpenLabel1;

    @FXML
    private RestrictedNumberField nozzlePartialOpen0;

    @FXML
    private RestrictedNumberField nozzlePartialOpen1;

    @FXML
    private ComboBox<String> firstLayerNozzleChoice;

    @FXML
    private Slider solidInfillExtrusionWidthSlider;

    @FXML
    private ComboBox<String> fillNozzleChoice;

    @FXML
    private Label rightNozzleTitle;

    @FXML
    private Label leftNozzleTitle;

    @FXML
    private Label nozzleEjectionVolumeLabel1;

    @FXML
    private RestrictedNumberField nozzleEjectionVolume0;

    @FXML
    private RestrictedNumberField nozzleEjectionVolume1;

    @FXML
    private RestrictedNumberField numberOfPerimeters;

    @FXML
    private RestrictedNumberField topSolidInfillSpeed;

    @FXML
    private Slider infillExtrusionWidthSlider;

    @FXML
    private Slider perimeterExtrusionWidthSlider;

    @FXML
    private RestrictedNumberField gapFillSpeed;

    @FXML
    private ComboBox<FillPattern> fillPatternChoice;

    @FXML
    private RestrictedNumberField infillSpeed;

    @FXML
    private RestrictedNumberField minPrintSpeed;

    @FXML
    private RestrictedNumberField infillEveryN;

    @FXML
    private RestrictedNumberField solidInfillExtrusionWidth;

    @FXML
    private RestrictedNumberField firstLayerExtrusionWidth;

    @FXML
    private RestrictedNumberField supportPatternSpacing;

    @FXML
    private RestrictedNumberField maxFanSpeed;

    @FXML
    private RestrictedNumberField topSolidInfillExtrusionWidth;

    @FXML
    private RestrictedNumberField bridgesFanSpeed;

    @FXML
    private RestrictedNumberField interfaceSpeed;

    @FXML
    private RestrictedNumberField supportExtrusionWidth;

    @FXML
    private RestrictedNumberField externalPerimeterSpeed;

    @FXML
    private ComboBox<CustomSlicerType> slicerChooser;

    @FXML
    private RestrictedNumberField raftBaseLinewidth;

    @FXML
    private RestrictedNumberField raftAirGapLayer0;

    @FXML
    private RestrictedNumberField interfaceLayers;

    @FXML
    private RestrictedNumberField supportXYDistance;

    @FXML
    private RestrictedNumberField supportZDistance;

    @FXML
    private TextArea helpText;

    /**
     * **************************************************************************
     */
    // Retain a temporary parameters file so that non-GUI variables are retained.
    private SlicerParametersFile temporarySettingsFile = null;
    /**
     * **************************************************************************
     */

    private BooleanProperty profileNameInvalid = new SimpleBooleanProperty(false);

    private final ObservableList<String> nozzleOptions = FXCollections.observableArrayList(
            "0.3mm", "0.4mm", "0.6mm", "0.8mm");
    private final ObservableList<FillPattern> fillPatternOptions = FXCollections.
            observableArrayList(
                    FillPattern.values());

    private final ChangeListener<String> dirtyStringListener
            = (ObservableValue<? extends String> ov, String t, String t1) ->
            {
                isDirty.set(true);
            };

    private final ChangeListener<Boolean> dirtyBooleanListener
            = (ObservableValue<? extends Boolean> ov, Boolean t, Boolean t1) ->
            {
                isDirty.set(true);
            };

    private final float minPoint8ExtrusionWidth = 0.5f;
    private final float defaultPoint8ExtrusionWidth = 0.8f;
    private final float maxPoint8ExtrusionWidth = 1.2f;
    private final float minPoint6ExtrusionWidth = 0.4f;
    private final float defaultPoint6ExtrusionWidth = 0.6f;
    private final float maxPoint6ExtrusionWidth = 0.8f;
    private final float minPoint4ExtrusionWidth = 0.2f;
    private final float defaultPoint4ExtrusionWidth = 0.4f;
    private final float maxPoint4ExtrusionWidth = 0.6f;
    private final float minPoint3ExtrusionWidth = 0.2f;
    private final float defaultPoint3ExtrusionWidth = 0.3f;
    private final float maxPoint3ExtrusionWidth = 0.6f;
    private final float minDualHeadExtrusionWidth = 0.4f;
    private final float maxDualHeadExtrusionWidth = 0.8f;
    private final float defaultDualHeadExtrusionWidth = 0.6f;

    private SlicerMappings slicerMappings;

    private Printer currentPrinter = null;

    private ChangeListener<Head> headChangeListener = new ChangeListener<Head>()
    {
        @Override
        public void changed(ObservableValue<? extends Head> ov, Head t, Head t1)
        {
            headHasChanged(t1);
        }
    };

    public ProfileLibraryPanelController()
    {
    }

    /**
     * Initialises the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb)
    {
        Lookup.getSelectedPrinterProperty().addListener(new ChangeListener<Printer>()
        {

            @Override
            public void changed(ObservableValue<? extends Printer> ov, Printer t, Printer t1)
            {
                bindToPrinter(t1);
            }
        });

        if (Lookup.getSelectedPrinterProperty().get() != null)
        {
            bindToPrinter(Lookup.getSelectedPrinterProperty().get());
        }

        slicerMappings = BaseLookup.getSlicerMappings();

        canSave.bind(isNameValid.and(isDirty.and(
                state.isEqualTo(State.NEW).
                or(state.isEqualTo(State.CUSTOM)))));

        canSaveAs.bind(state.isNotEqualTo(State.NEW));

        canDelete.bind(state.isNotEqualTo(State.NEW).and(state.isNotEqualTo(State.ROBOX)));

        isEditable.bind(state.isNotEqualTo(State.ROBOX));

        setupWidgetChangeListeners();

        setupHeadType();

        setupPrintProfileCombo();

        selectFirstPrintProfile();

        setupWidgetEditableBindings();

        setupFirstLayerNozzleChoice();

        setupPerimeterNozzleChoice();

        setupFillNozzleChoice();

        setupSupportNozzleChoice();

        setupSlicerChooser();

        supportPattern.setItems(FXCollections.observableArrayList(SupportPattern.values()));

        supportInterfaceNozzleChoice.setItems(nozzleOptions);

        fillPatternChoice.setItems(fillPatternOptions);

        FXMLUtilities.addColonsToLabels(container);

        setupWidgetsForHeadType();
    }

    private void headHasChanged(Head head)
    {
        if (head != null)
        {
            if (isDirty.get())
            {
                whenSavePressed();
            }
            cmbHeadType.getSelectionModel().select(head.typeCodeProperty().get());
        }
    }

    private void bindToPrinter(Printer printer)
    {
        if (currentPrinter != null)
        {
            currentPrinter.headProperty().removeListener(headChangeListener);
        }

        if (printer != null)
        {
            printer.headProperty().addListener(headChangeListener);

            if (printer.headProperty().get() != null)
            {
                headHasChanged(printer.headProperty().get());
            }
        }

        currentPrinter = printer;
    }

    private void setupHeadType()
    {

        for (HeadFile head : HeadContainer.getCompleteHeadList())
        {
            cmbHeadType.getItems().add(head.getTypeCode());
        }

        cmbHeadType.valueProperty().addListener((ObservableValue<? extends String> observable, String oldValue, String newValue) ->
        {
            HeadFile headDetails = HeadContainer.getHeadByID(newValue);
            currentHeadType.set(newValue);
            numNozzleHeaters.set(headDetails.getNozzleHeaters().size());
            numNozzles.set(headDetails.getNozzles().size());

            List<Float> nozzleSizes = headDetails.getNozzles()
                                                 .stream()
                                                 .map(n -> n.getDiameter())
                                                 .collect(Collectors.toList());

            List<String> nozzleSizeStrings = nozzleSizes.stream()
                                                        .map(n -> n.toString() + "mm")
                                                        .collect(Collectors.toList());
            
            nozzleOptions.setAll(nozzleSizeStrings);
            
            hasValves.set(headDetails.getValves() == ValveType.FITTED);
            if (hasValves.get())
            {
                nozzleEjectionVolumeLabel1.setText(BaseLookup.i18n("nozzle.ejectionVolume"));
                nozzleEjectionVolume0.getTooltip().setText(BaseLookup.i18n("profileLibraryHelp.nozzleEjectionVolume"));
                nozzleEjectionVolume1.getTooltip().setText(BaseLookup.i18n("profileLibraryHelp.nozzleEjectionVolume"));
            }
            else
            {
                nozzleEjectionVolumeLabel1.setText(BaseLookup.i18n("nozzle.retractionVolume"));
                nozzleEjectionVolume0.getTooltip().setText(BaseLookup.i18n("profileLibraryHelp.nozzleRetractionVolume"));
                nozzleEjectionVolume1.getTooltip().setText(BaseLookup.i18n("profileLibraryHelp.nozzleRetractionVolume"));
            }
            
            repopulateCmbPrintProfile();
            selectFirstPrintProfile();
            setSliderLimits(newValue);
        });

        cmbHeadType.setValue(HeadContainer.defaultHeadID);
    }

    private void bringValueWithinDualHeadTypeLimits(RestrictedNumberField field)
    {
        float currentWidth = field.getAsFloat();
        if (currentWidth < minDualHeadExtrusionWidth || currentWidth
                > maxDualHeadExtrusionWidth)
        {
            field.setValue(
                    defaultDualHeadExtrusionWidth);
        }
    }

    private void setSliderLimits(String headType)
    {
        int numNozzleHeaters = HeadContainer.getHeadByID(headType).getNozzleHeaters().size();
        switch (numNozzleHeaters)
        {
            case 1:
                setFirstLayerExtrusionWidthLimits(
                        firstLayerNozzleChoice.getSelectionModel().getSelectedIndex());
                setSupportExtrusionWidthLimits(
                        supportNozzleChoice.getSelectionModel().getSelectedIndex());
                setInfillExtrusionWidthLimits(
                        fillNozzleChoice.getSelectionModel().getSelectedIndex());
                setPerimeterExtrusionWidthLimits(
                        perimeterNozzleChoice.getSelectionModel().getSelectedIndex());
                break;
            case 2:
                bringValueWithinDualHeadTypeLimits(firstLayerExtrusionWidth);
                bringValueWithinDualHeadTypeLimits(supportExtrusionWidth);
                bringValueWithinDualHeadTypeLimits(infillExtrusionWidth);
                bringValueWithinDualHeadTypeLimits(solidInfillExtrusionWidth);
                bringValueWithinDualHeadTypeLimits(topSolidInfillExtrusionWidth);
                bringValueWithinDualHeadTypeLimits(perimeterExtrusionWidth);

                firstLayerExtrusionWidthSlider.setMin(minDualHeadExtrusionWidth);
                firstLayerExtrusionWidthSlider.setMax(maxDualHeadExtrusionWidth);
                supportExtrusionWidthSlider.setMin(minDualHeadExtrusionWidth);
                supportExtrusionWidthSlider.setMax(maxDualHeadExtrusionWidth);
                infillExtrusionWidthSlider.setMin(minDualHeadExtrusionWidth);
                infillExtrusionWidthSlider.setMax(maxDualHeadExtrusionWidth);
                solidInfillExtrusionWidthSlider.setMin(minDualHeadExtrusionWidth);
                solidInfillExtrusionWidthSlider.setMax(maxDualHeadExtrusionWidth);
                topSolidInfillExtrusionWidthSlider.setMin(minDualHeadExtrusionWidth);
                topSolidInfillExtrusionWidthSlider.setMax(maxDualHeadExtrusionWidth);
                perimeterExtrusionWidthSlider.setMin(minDualHeadExtrusionWidth);
                perimeterExtrusionWidthSlider.setMax(maxDualHeadExtrusionWidth);
                break;

        }
    }

    private void setupWidgetsForHeadType()
    {
        BooleanBinding disableNozzleChoice = numNozzleHeaters.isEqualTo(2).or(numNozzles.isEqualTo(1)); // Two heaters implies two materials.
        firstLayerNozzleChoice.disableProperty().bind(disableNozzleChoice);
        perimeterNozzleChoice.disableProperty().bind(disableNozzleChoice);
        fillNozzleChoice.disableProperty().bind(disableNozzleChoice);
        supportNozzleChoice.disableProperty().bind(disableNozzleChoice);
        supportInterfaceNozzleChoice.disableProperty().bind(disableNozzleChoice);
        //rightNozzleTitle.visibleProperty().bind(numNozzles.greaterThan(1));
        leftNozzleTitle.visibleProperty().bind(numNozzles.greaterThan(1));
        nozzleEjectionVolume0.visibleProperty().bind(numNozzles.greaterThan(1));
        nozzlePartialOpen0.visibleProperty().bind(hasValves.and(numNozzles.greaterThan(1)));
        nozzlePartialOpenLabel1.visibleProperty().bind(hasValves);
        nozzlePartialOpen1.visibleProperty().bind(hasValves);
    }

    private void setupPrintProfileCombo()
    {
        cmbPrintProfile.setCellFactory(
                (ListView<SlicerParametersFile> param) -> new PrintProfileCell());

        cmbPrintProfile.setButtonCell(cmbPrintProfile.getCellFactory().call(null));

        repopulateCmbPrintProfile();

        cmbPrintProfile.valueProperty().addListener(
                (ObservableValue<? extends SlicerParametersFile> observable, SlicerParametersFile oldValue, SlicerParametersFile newValue) ->
                {
                    selectPrintProfile();
                });

        selectPrintProfile();
    }

    private void selectFirstPrintProfile()
    {
        if (cmbPrintProfile.getItems().size() > 0)
        {
            cmbPrintProfile.setValue(cmbPrintProfile.getItems().get(0));
        }
    }

    public void setAndSelectPrintProfile(SlicerParametersFile printProfile)
    {
        if (SlicerParametersContainer.getCompleteProfileList().contains(printProfile))
        {
            cmbHeadType.setValue(printProfile.getHeadType());
            cmbPrintProfile.setValue(printProfile);
        } else
        {
            cmbPrintProfile.getSelectionModel().selectFirst();
        }
    }

    private void selectPrintProfile()
    {
        SlicerParametersFile printProfile = cmbPrintProfile.getValue();

        if (printProfile == null)
        {
            return;
        }
        currentProfileName = printProfile.getProfileName();
        updateWidgetsFromSettingsFile(printProfile);
        boolean isStandardProfile = SlicerParametersContainer.applicationProfileListContainsProfile(
                printProfile.getProfileName());
        if (!isStandardProfile)
        {
            state.set(State.CUSTOM);
        } else
        {
            state.set(State.ROBOX);
        }
        isDirty.set(false);
    }

    private void repopulateCmbPrintProfile()
    {
        try
        {
            ObservableList<SlicerParametersFile> parametersFiles = SlicerParametersContainer.getCompleteProfileList();
            String headType = cmbHeadType.getValue();
            List filesForHeadType = parametersFiles.stream().
                    filter(profile -> profile.getHeadType() != null && profile.getHeadType().equals(
                                    headType)).
                    collect(Collectors.toList());
            cmbPrintProfile.setItems(FXCollections.observableArrayList(filesForHeadType));
        } catch (NoClassDefFoundError exception)
        {
            // this should only happen in SceneBuilder            
        }
    }

    private void setupSlicerChooser()
    {
        slicerChooser.setItems(FXCollections.observableArrayList(CustomSlicerType.values()));

        slicerChooser.valueProperty().addListener(new ChangeListener<CustomSlicerType>()
        {
            @Override
            public void changed(ObservableValue<? extends CustomSlicerType> ov,
                    CustomSlicerType lastSlicer, CustomSlicerType newSlicer)
            {
                if (lastSlicer != newSlicer)
                {
                    updateFieldsForSelectedSlicer(newSlicer.getSlicerType());
                }
            }
        });
    }

    private void setupFirstLayerNozzleChoice()
    {
        firstLayerNozzleChoice.setItems(nozzleOptions);
        firstLayerNozzleChoice.getSelectionModel().selectedIndexProperty().addListener(
                (ObservableValue<? extends Number> observable, Number oldValue, Number newValue) ->
                {
                    setFirstLayerExtrusionWidthLimits(newValue);
                });
    }

private void setExtrusionWidthLimits(Number newValue, ObservableList<String> widthOptions, RestrictedNumberField widthField, Slider widthSlider)
    {
        float currentWidth = widthField.getAsFloat();
        int selIndex = newValue.intValue();
        if (selIndex < 0)
            selIndex = 0;
        String widthOption = widthOptions.get(selIndex);
        Optional<NozzleData> ond = HeadContainer.getHeadByID(cmbHeadType.getValue())
                                  .getNozzles()
                                  .stream()
                                  .filter(nn -> (nn.getMinExtrusionWidth() > 0.0) && (Float.toString(nn.getDiameter()) + " mm").equals(widthOption))
                                  .findFirst();
        // At Java 9, Optional has an ifPresentOrElse() method. But for the moment this is the easiest way to do this.
        if (ond.isPresent())
        {
            NozzleData nd = ond.get();
            float minExtrusionWidth = nd.getMinExtrusionWidth();
            float maxExtrusionWidth = nd.getMaxExtrusionWidth();
            if (currentWidth < minExtrusionWidth ||
                currentWidth > maxExtrusionWidth)
            {
                widthField.setValue(nd.getDefaultExtrusionWidth());
            }
            widthSlider.setMin(minExtrusionWidth);
            widthSlider.setMax(maxExtrusionWidth);
        }
        else
        {
            switch (widthOption)
            {
                case "0.3mm":
                    // The point 3 nozzle has been selected
                    if (currentWidth < minPoint3ExtrusionWidth ||
                        currentWidth > maxPoint3ExtrusionWidth)
                    {
                        widthField.setValue(defaultPoint3ExtrusionWidth);
                    }
                    widthSlider.setMin(minPoint3ExtrusionWidth);
                    widthSlider.setMax(maxPoint3ExtrusionWidth);
                    break;
                case "0.4mm":
                    // The point 4 nozzle has been selected
                    if (currentWidth < minPoint4ExtrusionWidth ||
                        currentWidth > maxPoint4ExtrusionWidth)
                    {
                        widthField.setValue(defaultPoint4ExtrusionWidth);
                    }
                    widthSlider.setMin(minPoint4ExtrusionWidth);
                    widthSlider.setMax(maxPoint4ExtrusionWidth);
                    break;
                case "0.6mm":
                    // The point 6 nozzle has been selected
                    if (currentWidth < minPoint6ExtrusionWidth ||
                        currentWidth > maxPoint6ExtrusionWidth)
                    {
                        widthField.setValue(defaultPoint6ExtrusionWidth);
                    }
                    widthSlider.setMin(minPoint6ExtrusionWidth);
                    widthSlider.setMax(maxPoint6ExtrusionWidth);
                    break;
                case "0.8mm":
                    // The point 8 nozzle has been selected
                    if (currentWidth < minPoint8ExtrusionWidth ||
                        currentWidth > maxPoint8ExtrusionWidth)
                    {
                        widthField.setValue(defaultPoint8ExtrusionWidth);
                    }
                    widthSlider.setMin(minPoint8ExtrusionWidth);
                    widthSlider.setMax(maxPoint8ExtrusionWidth);
                    break;
                    
                default:
                    float nozzleWidth = Float.parseFloat(widthOption);
                    widthField.setValue(nozzleWidth);
                    widthSlider.setMin(nozzleWidth);
                    widthSlider.setMax(nozzleWidth);
                    break;
            }
        }
    }

    private void setFirstLayerExtrusionWidthLimits(Number newValue)
    {
        setExtrusionWidthLimits(newValue, nozzleOptions, firstLayerExtrusionWidth, firstLayerExtrusionWidthSlider);
    }
    
    private void setupSupportNozzleChoice()
    {
        supportNozzleChoice.setItems(nozzleOptions);

        supportNozzleChoice.getSelectionModel()
                .selectedIndexProperty().addListener(
                        (ObservableValue<? extends Number> observable, Number oldValue, Number newValue) ->
                        {
                            setSupportExtrusionWidthLimits(newValue);
                        });
    }

    private void setSupportExtrusionWidthLimits(Number newValue)
    {
        setExtrusionWidthLimits(newValue, nozzleOptions, supportExtrusionWidth, supportExtrusionWidthSlider);
    }

    private void setupFillNozzleChoice()
    {
        fillNozzleChoice.setItems(nozzleOptions);
        fillNozzleChoice.getSelectionModel().selectedIndexProperty().addListener(
                (ObservableValue<? extends Number> observable, Number oldValue, Number newValue) ->
                {
                    setInfillExtrusionWidthLimits(newValue);
                });
    }

    private void setInfillExtrusionWidthLimits(Number newValue)
    {
        setExtrusionWidthLimits(newValue, nozzleOptions, infillExtrusionWidth, infillExtrusionWidthSlider);
        setExtrusionWidthLimits(newValue, nozzleOptions, solidInfillExtrusionWidth, solidInfillExtrusionWidthSlider);
        setExtrusionWidthLimits(newValue, nozzleOptions, topSolidInfillExtrusionWidth, topSolidInfillExtrusionWidthSlider);
    }

    private void setupPerimeterNozzleChoice()
    {
        perimeterNozzleChoice.setItems(nozzleOptions);
        perimeterNozzleChoice.getSelectionModel().selectedIndexProperty().addListener(
                (ObservableValue<? extends Number> observable, Number oldValue, Number newValue) ->
                {
                    setPerimeterExtrusionWidthLimits(newValue);
                });
    }

    private void setPerimeterExtrusionWidthLimits(Number newValue)
    {
        setExtrusionWidthLimits(newValue, nozzleOptions, perimeterExtrusionWidth, perimeterExtrusionWidthSlider);
    }

    private void setupWidgetEditableBindings()
    {
        profileNameField.disableProperty().bind(isEditable.not());
        slicerChooser.disableProperty().bind(isEditable.not());
        coolingGrid.disableProperty().bind(isEditable.not());
        extrusionGrid.disableProperty().bind(isEditable.not());
        extrusionControls.disableProperty().bind(isEditable.not());
        nozzleControls.disableProperty().bind(isEditable.not());
        supportGrid.disableProperty().bind(isEditable.not());
        speedGrid.disableProperty().bind(isEditable.not());
    }

    private void setupWidgetChangeListeners()
    {
        profileNameField.textProperty().addListener(
                (ObservableValue<? extends String> observable, String oldValue, String newValue) ->
                {
                    if (!validateProfileName())
                    {
                        isNameValid.set(false);
                        profileNameField.pseudoClassStateChanged(ERROR, true);
                    } else
                    {
                        isNameValid.set(true);
                        profileNameField.pseudoClassStateChanged(ERROR, false);
                    }
                });

        //Dirty listeners...
        profileNameField.textProperty().addListener(dirtyStringListener);

        slicerChooser.valueProperty().addListener(
                (ObservableValue<? extends CustomSlicerType> observable, CustomSlicerType oldValue, CustomSlicerType newValue) ->
                {
                    isDirty.set(true);
                });

        //Nozzle Page
        firstLayerExtrusionWidthSlider.valueProperty().addListener(new ChangeListener<Number>()
        {
            @Override
            public void changed(ObservableValue<? extends Number> ov, Number t, Number t1)
            {
                firstLayerExtrusionWidth.setValue(t1.doubleValue());
            }
        });

        firstLayerExtrusionWidth.valueChangedProperty().addListener(new ChangeListener<Boolean>()
        {
            @Override
            public void changed(ObservableValue<? extends Boolean> ov, Boolean t, Boolean t1)
            {
                firstLayerExtrusionWidthSlider.setValue(firstLayerExtrusionWidth.getAsDouble());
            }
        });

        firstLayerExtrusionWidth.textProperty().addListener(dirtyStringListener);
        firstLayerNozzleChoice.getSelectionModel().selectedItemProperty().addListener(
                dirtyStringListener);

        perimeterExtrusionWidthSlider.valueProperty().addListener(new ChangeListener<Number>()
        {
            @Override
            public void changed(ObservableValue<? extends Number> ov, Number t, Number t1)
            {
                perimeterExtrusionWidth.setValue(t1.doubleValue());
            }
        });

        perimeterExtrusionWidth.valueChangedProperty().addListener(new ChangeListener<Boolean>()
        {
            @Override
            public void changed(ObservableValue<? extends Boolean> ov, Boolean t, Boolean t1)
            {
                perimeterExtrusionWidthSlider.setValue(perimeterExtrusionWidth.getAsDouble());
            }
        });

        perimeterExtrusionWidth.textProperty().addListener(dirtyStringListener);
        perimeterNozzleChoice.getSelectionModel().selectedItemProperty().addListener(
                dirtyStringListener);

        infillExtrusionWidthSlider.valueProperty().addListener(new ChangeListener<Number>()
        {
            @Override
            public void changed(ObservableValue<? extends Number> ov, Number t, Number t1)
            {
                infillExtrusionWidth.setValue(t1.doubleValue());
            }
        });

        infillExtrusionWidth.valueChangedProperty().addListener(new ChangeListener<Boolean>()
        {
            @Override
            public void changed(ObservableValue<? extends Boolean> ov, Boolean t, Boolean t1)
            {
                infillExtrusionWidthSlider.setValue(infillExtrusionWidth.getAsDouble());
            }
        });
        infillExtrusionWidth.textProperty().addListener(dirtyStringListener);

        solidInfillExtrusionWidthSlider.valueProperty().addListener(new ChangeListener<Number>()
        {
            @Override
            public void changed(ObservableValue<? extends Number> ov, Number t, Number t1)
            {
                solidInfillExtrusionWidth.setValue(t1.doubleValue());
            }
        });

        solidInfillExtrusionWidth.valueChangedProperty().addListener(new ChangeListener<Boolean>()
        {
            @Override
            public void changed(ObservableValue<? extends Boolean> ov, Boolean t, Boolean t1)
            {
                solidInfillExtrusionWidthSlider.setValue(solidInfillExtrusionWidth.getAsDouble());
            }
        });
        solidInfillExtrusionWidth.textProperty().addListener(dirtyStringListener);
        fillNozzleChoice.getSelectionModel().selectedItemProperty().addListener(dirtyStringListener);

        topSolidInfillExtrusionWidthSlider.valueProperty().addListener(new ChangeListener<Number>()
        {
            @Override
            public void changed(ObservableValue<? extends Number> ov, Number t, Number t1)
            {
                topSolidInfillExtrusionWidth.setValue(t1.doubleValue());
            }
        });

        topSolidInfillExtrusionWidth.valueChangedProperty().addListener(new ChangeListener<Boolean>()
        {
            @Override
            public void changed(ObservableValue<? extends Boolean> ov, Boolean t, Boolean t1)
            {
                topSolidInfillExtrusionWidthSlider.setValue(topSolidInfillExtrusionWidth.getAsDouble());
            }
        });
        topSolidInfillExtrusionWidth.textProperty().addListener(dirtyStringListener);

        supportExtrusionWidthSlider.valueProperty().addListener(new ChangeListener<Number>()
        {
            @Override
            public void changed(ObservableValue<? extends Number> ov, Number t, Number t1)
            {
                supportExtrusionWidth.setValue(t1.doubleValue());
            }
        });

        supportExtrusionWidth.valueChangedProperty().addListener(new ChangeListener<Boolean>()
        {
            @Override
            public void changed(ObservableValue<? extends Boolean> ov, Boolean t, Boolean t1)
            {
                supportExtrusionWidthSlider.setValue(supportExtrusionWidth.getAsDouble());
            }
        });
        supportExtrusionWidth.textProperty()
                .addListener(dirtyStringListener);
        supportNozzleChoice.getSelectionModel()
                .selectedItemProperty().addListener(dirtyStringListener);

        supportInterfaceNozzleChoice.getSelectionModel()
                .selectedItemProperty().addListener(dirtyStringListener);

        fillDensity.textProperty().addListener(dirtyStringListener);
        slowFanIfLayerTimeBelow.textProperty().addListener(dirtyStringListener);
        enableFanIfLayerTimeBelow.textProperty().addListener(dirtyStringListener);
        solidInfillSpeed.textProperty().addListener(dirtyStringListener);
        supportOverhangThreshold.textProperty().addListener(dirtyStringListener);
        solidLayersTop.textProperty().addListener(dirtyStringListener);
        solidLayersBottom.textProperty().addListener(dirtyStringListener);
        numberOfPerimeters.textProperty().addListener(dirtyStringListener);
        topSolidInfillSpeed.textProperty().addListener(dirtyStringListener);
        firstLayerSpeed.textProperty().addListener(dirtyStringListener);
        perimeterSpeed.textProperty().addListener(dirtyStringListener);
        gapFillSpeed.textProperty().addListener(dirtyStringListener);
        fillPatternChoice.getSelectionModel().selectedItemProperty().addListener(
                (ObservableValue<? extends FillPattern> observable, FillPattern oldValue, FillPattern newValue) ->
                {
                    isDirty.set(true);
                });

        supportMaterialSpeed.textProperty()
                .addListener(dirtyStringListener);
        infillSpeed.textProperty()
                .addListener(dirtyStringListener);
        minPrintSpeed.textProperty()
                .addListener(dirtyStringListener);
        minFanSpeed.textProperty()
                .addListener(dirtyStringListener);
        infillEveryN.textProperty()
                .addListener(dirtyStringListener);
        supportPatternSpacing.textProperty()
                .addListener(dirtyStringListener);
        smallPerimeterSpeed.textProperty()
                .addListener(dirtyStringListener);
        maxFanSpeed.textProperty()
                .addListener(dirtyStringListener);
        disableFanForFirstNLayers.textProperty()
                .addListener(dirtyStringListener);
        bridgesFanSpeed.textProperty()
                .addListener(dirtyStringListener);
        bridgesSpeed.textProperty()
                .addListener(dirtyStringListener);
        interfaceSpeed.textProperty()
                .addListener(dirtyStringListener);
        layerHeight.textProperty()
                .addListener(dirtyStringListener);
        externalPerimeterSpeed.textProperty()
                .addListener(dirtyStringListener);
        supportPatternAngle.textProperty()
                .addListener(dirtyStringListener);

        enableAutoCooling.selectedProperty()
                .addListener(dirtyBooleanListener);

        perimeterNozzleChoice.getSelectionModel()
                .selectedItemProperty().addListener(dirtyStringListener);
        fillNozzleChoice.getSelectionModel()
                .selectedItemProperty().addListener(dirtyStringListener);
        supportNozzleChoice.getSelectionModel()
                .selectedItemProperty().addListener(dirtyStringListener);
        supportInterfaceNozzleChoice.getSelectionModel()
                .selectedItemProperty().addListener(dirtyStringListener);
        supportPattern.getSelectionModel()
                .selectedItemProperty().addListener(
                        (ObservableValue<? extends SupportPattern> observable, SupportPattern oldValue, SupportPattern newValue) ->
                        {
                            isDirty.set(true);
                        });

        nozzleEjectionVolume0.textProperty().addListener(dirtyStringListener);
        nozzlePartialOpen0.textProperty().addListener(dirtyStringListener);
        nozzleEjectionVolume1.textProperty().addListener(dirtyStringListener);
        nozzlePartialOpen1.textProperty().addListener(dirtyStringListener);

        raftBaseLinewidth.textProperty().addListener(dirtyStringListener);
        raftAirGapLayer0.textProperty().addListener(dirtyStringListener);
        interfaceLayers.textProperty().addListener(dirtyStringListener);
        supportXYDistance.textProperty().addListener(dirtyStringListener);
        supportZDistance.textProperty().addListener(dirtyStringListener);
    }

    private void updateWidgetsFromSettingsFile(SlicerParametersFile parametersFile)
    {
        temporarySettingsFile = parametersFile;

        profileNameField.setText(parametersFile.getProfileName());
        SlicerType slicerType = parametersFile.getSlicerOverride();
        if (slicerType != null)
        {
            slicerChooser.setValue(CustomSlicerType.customTypefromSettings(slicerType));
        } else
        {
            slicerChooser.setValue(CustomSlicerType.Default);
        }
        // Extrusion tab
        layerHeight.setValue(parametersFile.getLayerHeight_mm());
        fillDensity.setValue(parametersFile.getFillDensity_normalised());
        fillPatternChoice.setValue(parametersFile.getFillPattern());
        infillEveryN.setValue(parametersFile.getFillEveryNLayers());
        solidLayersTop.setValue(parametersFile.getSolidLayersAtTop());
        solidLayersBottom.setValue(parametersFile.getSolidLayersAtBottom());
        numberOfPerimeters.setValue(parametersFile.getNumberOfPerimeters());

        //Nozzle tab
        firstLayerExtrusionWidth.setValue(
                parametersFile.getFirstLayerExtrusionWidth_mm());
        firstLayerNozzleChoice.getSelectionModel().select(parametersFile.getFirstLayerNozzle());

        perimeterExtrusionWidth.setValue(
                parametersFile.getPerimeterExtrusionWidth_mm());
        perimeterNozzleChoice.getSelectionModel().select(parametersFile.getPerimeterNozzle());

        infillExtrusionWidth.setValue(parametersFile.getFillExtrusionWidth_mm());
        solidInfillExtrusionWidth.setValue(
                parametersFile.getSolidFillExtrusionWidth_mm());
        fillNozzleChoice.getSelectionModel().select(parametersFile.getFillNozzle());
        topSolidInfillExtrusionWidth.setValue(
                parametersFile.getTopSolidFillExtrusionWidth_mm());

        supportExtrusionWidth.setValue(parametersFile.getSupportExtrusionWidth_mm());
        supportNozzleChoice.getSelectionModel().select(parametersFile.getSupportNozzle());

        supportInterfaceNozzleChoice.getSelectionModel().select(
                parametersFile.getSupportInterfaceNozzle());

        //Support tab
        supportOverhangThreshold.setValue(
                parametersFile.getSupportOverhangThreshold_degrees());
        supportPattern.valueProperty().set(parametersFile.getSupportPattern().get(slicerType));
        supportPatternSpacing.setValue(parametersFile.getSupportPatternSpacing_mm());
        supportPatternAngle.setValue(parametersFile.getSupportPatternAngle_degrees());
        raftBaseLinewidth.setValue(parametersFile.getRaftBaseLinewidth_mm());
        raftAirGapLayer0.setValue(parametersFile.getRaftAirGapLayer0_mm());
        interfaceLayers.setValue(parametersFile.getInterfaceLayers());
        supportXYDistance.setValue(parametersFile.getSupportXYDistance_mm());
        supportZDistance.setValue(parametersFile.getSupportZDistance_mm());

        //Speed tab
        firstLayerSpeed.setValue(parametersFile.getFirstLayerSpeed_mm_per_s());
        perimeterSpeed.setValue(parametersFile.getPerimeterSpeed_mm_per_s());
        smallPerimeterSpeed.setValue(parametersFile.getSmallPerimeterSpeed_mm_per_s());
        externalPerimeterSpeed.setValue(
                parametersFile.getExternalPerimeterSpeed_mm_per_s());
        infillSpeed.setValue(parametersFile.getFillSpeed_mm_per_s());
        solidInfillSpeed.setValue(parametersFile.getSolidFillSpeed_mm_per_s());
        topSolidInfillSpeed.setValue(parametersFile.getTopSolidFillSpeed_mm_per_s());
        supportMaterialSpeed.setValue(parametersFile.getSupportSpeed_mm_per_s());
        bridgesSpeed.setValue(parametersFile.getBridgeSpeed_mm_per_s());
        interfaceSpeed.setValue(parametersFile.getInterfaceSpeed_mm_per_s());
        gapFillSpeed.setValue(parametersFile.getGapFillSpeed_mm_per_s());

        //Cooling tab
        enableAutoCooling.selectedProperty().set(parametersFile.getEnableCooling());
        minFanSpeed.setValue(parametersFile.getMinFanSpeed_percent());
        maxFanSpeed.setValue(parametersFile.getMaxFanSpeed_percent());
        bridgesFanSpeed.setValue(parametersFile.getBridgeFanSpeed_percent());
        disableFanForFirstNLayers.setValue(parametersFile.getDisableFanFirstNLayers());
        enableFanIfLayerTimeBelow.setValue(
                parametersFile.getCoolIfLayerTimeLessThan_secs());
        slowFanIfLayerTimeBelow.setValue(
                parametersFile.getSlowDownIfLayerTimeLessThan_secs());
        minPrintSpeed.setValue(parametersFile.getMinPrintSpeed_mm_per_s());

        // nozzle
        if (numNozzles.get() > 1)
        {
            nozzleEjectionVolume0.setValue(parametersFile.getNozzleParameters()
                .get(0).getEjectionVolume());
            nozzleEjectionVolume1.setValue(parametersFile.getNozzleParameters()
                .get(1).getEjectionVolume());
            if (hasValves.get())
            {
                nozzlePartialOpen0.setValue(parametersFile.getNozzleParameters()
                    .get(0).getPartialBMinimum());
                nozzlePartialOpen1.setValue(parametersFile.getNozzleParameters()
                    .get(1).getPartialBMinimum());
            }
            else
            {
                nozzlePartialOpen0.setValue(0.5);
                nozzlePartialOpen1.setValue(0.5);
            }
        }
        else
        {
            // If there is a single nozzle, then it is the RIGHT nozzle (i.e. nozzle 1).
            nozzleEjectionVolume1.setValue(parametersFile.getNozzleParameters()
                .get(0).getEjectionVolume());
            if (hasValves.get())
            {
                nozzlePartialOpen1.setValue(parametersFile.getNozzleParameters()
                    .get(0).getPartialBMinimum());
            }
            else
            {
                nozzlePartialOpen1.setValue(0.5);
            }
            
            nozzleEjectionVolume0.setValue(0.0);
            nozzlePartialOpen0.setValue(0.5);
        }
        updateFieldsForSelectedSlicer(parametersFile.getSlicerOverride());
    }

    /**
     * Enable/Disable fields appropriately according to the selected slicer.
     *
     * @param slicerType
     */
    private void updateFieldsForSelectedSlicer(SlicerType slicerType)
    {
        if (slicerType == null)
        {
            slicerType = Lookup.getUserPreferences().getSlicerType();
        }

        //Belt and braces
        if (slicerType == null)
        {
            slicerType = SlicerType.Cura;
        }

        layerHeight.setDisable(!slicerMappings.isMapped(slicerType, "layerHeight_mm"));
        fillDensity.setDisable(!slicerMappings.isMapped(slicerType, "fillDensity_normalised"));
        infillEveryN.setDisable(!slicerMappings.isMapped(slicerType, "fillEveryNLayers"));
        solidLayersTop.setDisable(!slicerMappings.isMapped(slicerType, "solidLayersAtTop"));
        solidLayersBottom.setDisable(!slicerMappings.isMapped(slicerType, "solidLayersAtBottom"));
        numberOfPerimeters.setDisable(!slicerMappings.isMapped(slicerType, "numberOfPerimeters"));
        fillPatternChoice.setDisable(!slicerMappings.isMapped(slicerType, "fillPattern"));

        FillPattern currentFillPattern = fillPatternChoice.getValue();
        if (slicerType == SlicerType.Slic3r)
        {
            fillPatternChoice.setItems(FXCollections.observableArrayList(FillPattern.values()));
            fillPatternChoice.setValue(currentFillPattern);
        } else
        {
            // For Cura (and any other non-Slic3r slicers we only have a LINE fill option
            fillPatternChoice.getItems().clear();
            fillPatternChoice.getItems().add(FillPattern.LINE);
            fillPatternChoice.setValue(FillPattern.LINE);
        }

        //Nozzle tab
        firstLayerExtrusionWidth.setDisable(!slicerMappings.isMapped(slicerType,
                "firstLayerExtrusionWidth_mm"));
        perimeterExtrusionWidth.setDisable(!slicerMappings.isMapped(slicerType,
                "perimeterExtrusionWidth_mm"));
        infillExtrusionWidth.setDisable(
                !slicerMappings.isMapped(slicerType, "fillExtrusionWidth_mm"));
        solidInfillExtrusionWidth.setDisable(!slicerMappings.isMapped(slicerType,
                "solidFillExtrusionWidth_mm"));
        solidInfillExtrusionWidthSlider.setDisable(!slicerMappings.isMapped(slicerType,
                "solidFillExtrusionWidth_mm"));
        topSolidInfillExtrusionWidth.setDisable(!slicerMappings.isMapped(slicerType,
                "topSolidFillExtrusionWidth_mm"));
        topSolidInfillExtrusionWidthSlider.setDisable(!slicerMappings.isMapped(slicerType,
                "topSolidFillExtrusionWidth_mm"));
        supportExtrusionWidth.setDisable(!slicerMappings.isMapped(slicerType,
                "supportExtrusionWidth_mm"));

        //Support tab
        supportOverhangThreshold.setDisable(!slicerMappings.isMapped(slicerType,
                "supportOverhangThreshold_degrees"));
        supportPatternSpacing.setDisable(!slicerMappings.isMapped(slicerType,
                "supportPatternSpacing_mm"));
        supportPatternAngle.setDisable(!slicerMappings.isMapped(slicerType,
                "supportPatternAngle_degrees"));

        SupportPattern currentSupportPattern = supportPattern.getValue();
        supportPattern.getItems().clear();
        SupportPattern[] supportPatternsForSlicer = SupportPattern.valuesForSlicer(slicerType);
        supportPattern.setItems(FXCollections.observableArrayList(supportPatternsForSlicer));
        supportPattern.setValue(currentSupportPattern);

        if(Arrays.asList(supportPatternsForSlicer).contains(currentSupportPattern)) 
        {
            supportPattern.setValue(currentSupportPattern);
        } else
        {
            supportPattern.setValue(supportPatternsForSlicer[0]);
        }

        //Speed tab
        firstLayerSpeed.setDisable(!slicerMappings.isMapped(slicerType, "firstLayerSpeed_mm_per_s"));
        perimeterSpeed.setDisable(!slicerMappings.isMapped(slicerType, "perimeterSpeed_mm_per_s"));
        smallPerimeterSpeed.setDisable(!slicerMappings.isMapped(slicerType,
                "smallPerimeterSpeed_mm_per_s"));
        externalPerimeterSpeed.setDisable(!slicerMappings.isMapped(slicerType,
                "externalPerimeterSpeed_mm_per_s"));
        infillSpeed.setDisable(!slicerMappings.isMapped(slicerType, "fillSpeed_mm_per_s"));
        solidInfillSpeed.setDisable(!slicerMappings.isMapped(slicerType, "solidFillSpeed_mm_per_s"));
        topSolidInfillSpeed.setDisable(!slicerMappings.isMapped(slicerType,
                "topSolidFillSpeed_mm_per_s"));
        supportMaterialSpeed.setDisable(
                !slicerMappings.isMapped(slicerType, "supportSpeed_mm_per_s"));
        bridgesSpeed.setDisable(!slicerMappings.isMapped(slicerType, "bridgeSpeed_mm_per_s"));
        gapFillSpeed.setDisable(!slicerMappings.isMapped(slicerType, "gapFillSpeed_mm_per_s"));

        //Cooling tab
        minFanSpeed.setDisable(!slicerMappings.isMapped(slicerType, "minFanSpeed_percent"));
        maxFanSpeed.setDisable(!slicerMappings.isMapped(slicerType, "maxFanSpeed_percent"));
        bridgesFanSpeed.setDisable(!slicerMappings.isMapped(slicerType, "bridgeFanSpeed_percent"));
        disableFanForFirstNLayers.setDisable(!slicerMappings.isMapped(slicerType,
                "disableFanFirstNLayers"));
        enableFanIfLayerTimeBelow.setDisable(!slicerMappings.isMapped(slicerType,
                "coolIfLayerTimeLessThan_secs"));
        slowFanIfLayerTimeBelow.setDisable(!slicerMappings.isMapped(slicerType,
                "slowDownIfLayerTimeLessThan_secs"));
        minPrintSpeed.setDisable(!slicerMappings.isMapped(slicerType, "minPrintSpeed_mm_per_s"));
    }

    private SlicerParametersFile getPrintProfile()
    {
        SlicerParametersFile settingsToUpdate = null;

        if (temporarySettingsFile != null)
        {
            settingsToUpdate = temporarySettingsFile;
        } else
        {
            settingsToUpdate = makeNewSlicerParametersFile();
        }
        settingsToUpdate.setSlicerOverride(slicerChooser.getValue().getSlicerType());
        settingsToUpdate.setProfileName(profileNameField.getText());
        // Extrusion tab
        settingsToUpdate.setLayerHeight_mm(layerHeight.getAsFloat());
        settingsToUpdate.setFillDensity_normalised(fillDensity.getAsFloat());
        settingsToUpdate.setFillPattern(fillPatternChoice.valueProperty().get());
        settingsToUpdate.setFillEveryNLayers(infillEveryN.getAsInt());
        settingsToUpdate.setSolidLayersAtTop(solidLayersTop.getAsInt());
        settingsToUpdate.setSolidLayersAtBottom(solidLayersBottom.getAsInt());
        settingsToUpdate.setNumberOfPerimeters(numberOfPerimeters.getAsInt());

        //Nozzle tab
        settingsToUpdate.setFirstLayerExtrusionWidth_mm(
                firstLayerExtrusionWidth.getAsFloat());
        settingsToUpdate.setFirstLayerNozzle(
                firstLayerNozzleChoice.getSelectionModel().getSelectedIndex());

        settingsToUpdate.setPerimeterExtrusionWidth_mm(
                perimeterExtrusionWidth.getAsFloat());
        settingsToUpdate.setPerimeterNozzle(
                perimeterNozzleChoice.getSelectionModel().getSelectedIndex());

        settingsToUpdate.setFillExtrusionWidth_mm(infillExtrusionWidth.getAsFloat());
        settingsToUpdate.setSolidFillExtrusionWidth_mm(
                solidInfillExtrusionWidth.getAsFloat());
        settingsToUpdate.setFillNozzle(fillNozzleChoice.getSelectionModel().getSelectedIndex());
        settingsToUpdate.setTopSolidFillExtrusionWidth_mm(
                topSolidInfillExtrusionWidth.getAsFloat());

        settingsToUpdate.setSupportExtrusionWidth_mm(
                supportExtrusionWidth.getAsFloat());
        settingsToUpdate.
                setSupportNozzle(supportNozzleChoice.getSelectionModel().getSelectedIndex());

        settingsToUpdate.setSupportInterfaceNozzle(
                supportInterfaceNozzleChoice.getSelectionModel().getSelectedIndex());

        //Support tab
        settingsToUpdate.setSupportInterfaceNozzle(
                supportInterfaceNozzleChoice.getSelectionModel().getSelectedIndex());
        settingsToUpdate.setSupportOverhangThreshold_degrees(
                supportOverhangThreshold.getAsInt());
        // Support Pattern
        HashMap<SlicerType, SupportPattern> supportPatternMap = new HashMap<>();
        SupportPattern supportPatternValue = supportPattern.getValue();
        supportPatternValue.getSlicerTypes().forEach(type -> 
                supportPatternMap.put(type, supportPatternValue));
        settingsToUpdate.setSupportPattern(supportPatternMap);
        
        settingsToUpdate.setSupportPatternSpacing_mm(
                supportPatternSpacing.getAsFloat());
        settingsToUpdate.
                setSupportPatternAngle_degrees(supportPatternAngle.getAsInt());
        settingsToUpdate.setRaftBaseLinewidth_mm(raftBaseLinewidth.getAsFloat());
        settingsToUpdate.setRaftAirGapLayer0_mm(raftAirGapLayer0.getAsFloat());
        settingsToUpdate.setInterfaceLayers(interfaceLayers.getAsInt());
        settingsToUpdate.setSupportXYDistance_mm(supportXYDistance.getAsFloat());
        settingsToUpdate.setSupportZDistance_mm(supportZDistance.getAsFloat());

        //Speed tab
        settingsToUpdate.setFirstLayerSpeed_mm_per_s(firstLayerSpeed.getAsInt());
        settingsToUpdate.setPerimeterSpeed_mm_per_s(perimeterSpeed.getAsInt());
        settingsToUpdate.setSmallPerimeterSpeed_mm_per_s(
                smallPerimeterSpeed.getAsInt());
        settingsToUpdate.setExternalPerimeterSpeed_mm_per_s(
                externalPerimeterSpeed.getAsInt());
        settingsToUpdate.setFillSpeed_mm_per_s(infillSpeed.getAsInt());
        settingsToUpdate.setSolidFillSpeed_mm_per_s(solidInfillSpeed.getAsInt());
        settingsToUpdate.setTopSolidFillSpeed_mm_per_s(topSolidInfillSpeed.getAsInt());
        settingsToUpdate.setSupportSpeed_mm_per_s(supportMaterialSpeed.getAsInt());
        settingsToUpdate.setBridgeSpeed_mm_per_s(bridgesSpeed.getAsInt());
        settingsToUpdate.setInterfaceSpeed_mm_per_s(interfaceSpeed.getAsInt());
        settingsToUpdate.setGapFillSpeed_mm_per_s(gapFillSpeed.getAsInt());

        //Cooling tab
        settingsToUpdate.setEnableCooling(enableAutoCooling.selectedProperty().get());
        settingsToUpdate.setMinFanSpeed_percent(minFanSpeed.getAsInt());
        settingsToUpdate.setMaxFanSpeed_percent(maxFanSpeed.getAsInt());
        settingsToUpdate.setBridgeFanSpeed_percent(bridgesFanSpeed.getAsInt());
        settingsToUpdate.setDisableFanFirstNLayers(
                disableFanForFirstNLayers.getAsInt());
        settingsToUpdate.setCoolIfLayerTimeLessThan_secs(
                enableFanIfLayerTimeBelow.getAsInt());
        settingsToUpdate.setSlowDownIfLayerTimeLessThan_secs(
                slowFanIfLayerTimeBelow.getAsInt());
        settingsToUpdate.setMinPrintSpeed_mm_per_s(minPrintSpeed.getAsInt());

        // Nozzle
        settingsToUpdate.getNozzleParameters().get(0).setEjectionVolume(
                nozzleEjectionVolume0.getAsFloat());
        settingsToUpdate.getNozzleParameters().get(0).setPartialBMinimum(
                nozzlePartialOpen0.getAsFloat());
        if (numNozzles.get() > 1)
        {
            settingsToUpdate.getNozzleParameters().get(0).setEjectionVolume(
                nozzleEjectionVolume0.getAsFloat());
            settingsToUpdate.getNozzleParameters().get(0).setPartialBMinimum(
                nozzlePartialOpen0.getAsFloat());
            settingsToUpdate.getNozzleParameters().get(1).setEjectionVolume(
                    nozzleEjectionVolume1.getAsFloat());
            settingsToUpdate.getNozzleParameters().get(1).setPartialBMinimum(
                    nozzlePartialOpen1.getAsFloat());
        }
        else
        {
            // Only one nozzle.
            // Copy the values from the RIGHT nozzle (i.e. nozzle 1).
            settingsToUpdate.getNozzleParameters().get(0).setEjectionVolume(
                    nozzleEjectionVolume1.getAsFloat());
            settingsToUpdate.getNozzleParameters().get(0).setPartialBMinimum(
                    nozzlePartialOpen1.getAsFloat());
            settingsToUpdate.getNozzleParameters().get(1).setEjectionVolume(
                   nozzleEjectionVolume1.getAsFloat());
            settingsToUpdate.getNozzleParameters().get(1).setPartialBMinimum(
                    nozzlePartialOpen1.getAsFloat());
        }

        return settingsToUpdate;
    }

    private boolean validateProfileName()
    {
        boolean valid = true;
        String profileNameText = profileNameField.getText();

        if (profileNameText.equals(""))
        {
            valid = false;
        } else
        {
            ObservableList<SlicerParametersFile> existingProfileList = SlicerParametersContainer.
                    getCompleteProfileList();
            for (SlicerParametersFile settings : existingProfileList)
            {
                if (!settings.getProfileName().equals(currentProfileName)
                        && settings.getProfileName().equals(profileNameText))
                {
                    valid = false;
                    break;
                }
            }
        }
        return valid;
    }

    /**
     * Validate the data in the widgets and return false if it is invalid else
     * return true.
     */
    private boolean validateData()
    {
        boolean valid = true;
        if (!validateProfileName())
        {
            valid = false;
        }
        return valid;
    }

    public class PrintProfileCell extends ListCell<SlicerParametersFile>
    {

        @Override
        protected void updateItem(SlicerParametersFile item, boolean empty)
        {
            super.updateItem(item, empty);
            if (item != null && !empty)
            {
                setText(item.getProfileName());
            } else
            {
                setText("");
            }
        }
    }

    void whenSavePressed()
    {
        assert (state.get() != ProfileLibraryPanelController.State.ROBOX);
        if (!validateData())
        {
            return;
        }
        SlicerParametersFile parametersFile = getPrintProfile();
        SlicerParametersContainer.saveProfile(parametersFile);
        isDirty.set(false);
        repopulateCmbPrintProfile();
        state.set(ProfileLibraryPanelController.State.CUSTOM);
        cmbPrintProfile.setValue(SlicerParametersContainer.getSettings(
                parametersFile.getProfileName(), parametersFile.getHeadType()));
    }

    void whenNewPressed()
    {
        state.set(ProfileLibraryPanelController.State.NEW);
        SlicerParametersFile slicerParametersFile = makeNewSlicerParametersFile();
        slicerParametersFile.setProfileName("");
        updateWidgetsFromSettingsFile(slicerParametersFile);
    }

    void whenSaveAsPressed()
    {

        isNameValid.set(false);
        state.set(ProfileLibraryPanelController.State.NEW);
        SlicerParametersFile slicerParametersFile
                = SlicerParametersContainer.getSettings(currentProfileName, currentHeadType.get()).clone();

        updateWidgetsFromSettingsFile(slicerParametersFile);
        profileNameField.requestFocus();
        profileNameField.selectAll();
        currentProfileName = "";
        profileNameField.pseudoClassStateChanged(ERROR, true);
    }

    private SlicerParametersFile makeNewSlicerParametersFile()
    {
        // WARNING - THIS WILL GIVE EMPTY VALUES FOR SOME PARAMETERS - USE WITH CAUTION!!!! //
        SlicerParametersFile slicerParametersFile = new SlicerParametersFile();
        slicerParametersFile.setNozzleParameters(new ArrayList<>());
        slicerParametersFile.getNozzleParameters().add(new NozzleParameters());
        slicerParametersFile.getNozzleParameters().add(new NozzleParameters());
        slicerParametersFile.setFillPattern(FillPattern.LINE);
        HashMap<SlicerType, SupportPattern> supportPatterns = new HashMap<>();
        supportPatterns.put(SlicerType.Cura, SupportPattern.RECTILINEAR);
        supportPatterns.put(SlicerType.Cura3, SupportPattern.LINES);
        slicerParametersFile.setSupportPattern(supportPatterns);
        return slicerParametersFile;
    }

    void whenDeletePressed()
    {
        if (state.get() != ProfileLibraryPanelController.State.NEW)
        {
            SlicerParametersContainer.deleteUserProfile(currentProfileName, currentHeadType.get());
        }
        repopulateCmbPrintProfile();
        selectFirstPrintProfile();
    }

    @Override
    public String getMenuTitle()
    {
        return "extrasMenu.printProfile";
    }

    @Override
    public List<MenuInnerPanel.OperationButton> getOperationButtons()
    {
        List<MenuInnerPanel.OperationButton> operationButtons = new ArrayList<>();
        MenuInnerPanel.OperationButton saveButton = new MenuInnerPanel.OperationButton()
        {
            @Override
            public String getTextId()
            {
                return "genericFirstLetterCapitalised.Save";
            }

            @Override
            public String getFXMLName()
            {
                return "saveButton";
            }

            @Override
            public String getTooltipTextId()
            {
                return "genericFirstLetterCapitalised.Save";
            }

            @Override
            public void whenClicked()
            {
                whenSavePressed();
            }

            @Override
            public BooleanProperty whenEnabled()
            {
                return canSave;
            }

        };
        operationButtons.add(saveButton);
        MenuInnerPanel.OperationButton saveAsButton = new MenuInnerPanel.OperationButton()
        {
            @Override
            public String getTextId()
            {
                return "genericFirstLetterCapitalised.SaveAs";
            }

            @Override
            public String getFXMLName()
            {
                return "saveAsButton";
            }

            @Override
            public String getTooltipTextId()
            {
                return "genericFirstLetterCapitalised.SaveAs";
            }

            @Override
            public void whenClicked()
            {
                whenSaveAsPressed();
            }

            @Override
            public BooleanProperty whenEnabled()
            {
                return canSaveAs;
            }

        };
        operationButtons.add(saveAsButton);
        MenuInnerPanel.OperationButton deleteButton = new MenuInnerPanel.OperationButton()
        {
            @Override
            public String getTextId()
            {
                return "genericFirstLetterCapitalised.Delete";
            }

            @Override
            public String getFXMLName()
            {
                return "deleteButton";
            }

            @Override
            public String getTooltipTextId()
            {
                return "genericFirstLetterCapitalised.Delete";
            }

            @Override
            public void whenClicked()
            {
                whenDeletePressed();
            }

            @Override
            public BooleanProperty whenEnabled()
            {
                return canDelete;
            }

        };
        operationButtons.add(deleteButton);
        return operationButtons;
    }
}
