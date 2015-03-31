package celtech.coreUI.controllers.utilityPanels;

import celtech.Lookup;
import celtech.configuration.CustomSlicerType;
import celtech.configuration.SlicerType;
import celtech.configuration.datafileaccessors.SlicerParametersContainer;
import celtech.configuration.fileRepresentation.SlicerMappings;
import celtech.configuration.fileRepresentation.SlicerParametersFile;
import celtech.configuration.slicer.FillPattern;
import celtech.configuration.slicer.NozzleParameters;
import celtech.configuration.slicer.SupportPattern;
import celtech.coreUI.components.RestrictedNumberField;
import celtech.coreUI.components.RestrictedTextField;
import celtech.coreUI.controllers.panels.ExtrasMenuInnerPanel;
import celtech.coreUI.controllers.panels.FXMLUtilities;
import celtech.utils.DeDuplicator;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
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
public class ProfileDetailsController implements Initializable, ExtrasMenuInnerPanel
{

    enum Fields
    {

        NAME("name"), SLICER_CHOOSER("slicerChooser"), LAYER_HEIGHT("layerHeight"),
        FILL_DENSITY("fillDensity"), FILL_PATTERN("fillPattern"), INFILL_EVERYN("infillEveryN"),
        SOLID_LAYERS_TOP("solidLayersTop"), SOLID_LAYERS_BOTTOM("solidLayersBottom"),
        NUMBER_OF_PERIMETERS("numberOfPerimeters"), BRIM_WIDTH("brimWidth"),
        FIRST_LAYER_EXTRUSION_WIDTH("firstLayerExtrusionWidth"),
        PERIMETER_EXTRUSION_WIDTH("perimeterExtrusionWidth"),
        INFILL_EXTRUSION_WIDTH("infillExtrusionWidth"),
        SOLID_INFILL_EXTRUSION_WIDTH("solidInfillExtrusionWidth"),
        TOP_SOLID_INFILL_EXTRUSION_WIDTH("topSolidInfillExtrusionWidth"),
        SUPPORT_EXTRUSION_WIDTH("supportExtrusionWidth"),
        NOZZLE_OPEN_VOLUME("nozzleOpenVolume"), NOZZLE_EJECTION_VOLUME("nozzleEjectionVolume"),
        NOZZLE_PARTIAL_OPEN("nozzlePartialOpen"),
        SUPPORT_MATERIAL_ENABLED("supportMaterialEnabled"),
        SUPPORT_OVERHANG_THRESHOLD("supportOverhangThreshold"),
        FORCED_SUPPORT_LAYERS("forcedSupportLayers"), SUPPORT_PATTERN("supportPattern"),
        SUPPORT_PATTERN_SPACING("supportPatternSpacing"),
        SUPPORT_PATTERN_ANGLE("supportPatternAngle"),
        FIRST_LAYER_SPEED("firstLayerSpeed"), PERIMETER_SPEED("perimeterSpeed"),
        SMALL_PERIMETER_SPEED("smallPerimeterSpeed"),
        EXTERNAL_PERIMETER_SPEED("externalPerimeterSpeed"), INFILL_SPEED("infillSpeed"),
        SOLID_INFILL_SPEED("solidInfillSpeed"),
        TOP_SOLID_INFILL_SPEED("TopSolidInfillSpeed"), SUPPORT_MATERIAL_SPEED("supportMaterialSpeed"),
        BRIDGES_SPEED("bridgesSpeed"), GAP_FILL_SPEED("gapFillSpeed"),
        ENABLE_AUTO_COOLING("enableAutoCooling"), MIN_FAN_SPEED("minFanSpeed"),
        MAX_FAN_SPEED("maxFanSpeed"), BRIDGES_FAN_SPEED("bridgesFanSpeed"),
        DISABLE_FAN_FIRST_N_LAYERS("disableFanFirstNLayers"),
        ENABLE_FAN_LAYER_TIME_BELOW("enableFanLayerTimeBelow"),
        SLOW_FAN_LAYER_TIME_BELOW("slowFanLayerTimeBelow"),
        MIN_PRINT_SPEED("minPrintSpeed");

        private final String helpTextId;

        Fields(String helpTextId)
        {
            this.helpTextId = helpTextId;
        }

        String getHelpText()
        {
            return Lookup.i18n("profileLibraryHelp." + helpTextId);
        }
    }

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

    private final ObjectProperty<ProfileDetailsController.State> state = new SimpleObjectProperty<>();
    private final BooleanProperty isDirty = new SimpleBooleanProperty(false);

    private final BooleanProperty isEditable = new SimpleBooleanProperty(false);
    private final BooleanProperty canSave = new SimpleBooleanProperty(false);
    private final BooleanProperty canSaveAs = new SimpleBooleanProperty(false);
    private final BooleanProperty canDelete = new SimpleBooleanProperty(false);
    private String currentProfileName;

    private final Stenographer steno = StenographerFactory.getStenographer(ProfileDetailsController.class.getName());

    @FXML
    private VBox container;

    @FXML
    private ComboBox<SlicerParametersFile> cmbPrintProfile;

    @FXML
    private RestrictedNumberField fillDensity;

    @FXML
    private RestrictedNumberField nozzleOpenVolume0;

    @FXML
    private RestrictedNumberField nozzleOpenVolume1;

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
    private RestrictedNumberField brimWidth;

    @FXML
    private RestrictedNumberField minFanSpeed;

    @FXML
    private RestrictedNumberField solidLayersBottom;

    @FXML
    private RestrictedNumberField forcedSupportLayers;

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
    private RestrictedNumberField nozzleEjectionVolume0;

    @FXML
    private RestrictedNumberField nozzleEjectionVolume1;

    @FXML
    private CheckBox supportMaterialEnabled;

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
    private RestrictedNumberField supportExtrusionWidth;

    @FXML
    private RestrictedNumberField externalPerimeterSpeed;

    @FXML
    private ComboBox<CustomSlicerType> slicerChooser;

    @FXML
    private TextArea helpText;

    private BooleanProperty profileNameInvalid = new SimpleBooleanProperty(false);

    private final ObservableList<String> forceNozzleFirstLayerOptions = FXCollections.observableArrayList();
    private final ObservableList<String> nozzleOptions = FXCollections.observableArrayList(
        "0.3mm", "0.8mm");
    private final ObservableList<FillPattern> fillPatternOptions = FXCollections.observableArrayList(
        FillPattern.values());

    private final ObservableList<SupportPattern> supportPatternOptions = FXCollections.observableArrayList(
        SupportPattern.values());

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
    private final float minPoint3ExtrusionWidth = 0.2f;
    private final float defaultPoint3ExtrusionWidth = 0.3f;
    private final float maxPoint3ExtrusionWidth = 0.6f;

    private SlicerMappings slicerMappings;

    public ProfileDetailsController()
    {
    }

    /**
     * Initialises the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb)
    {
        slicerMappings = Lookup.getSlicerMappings();

        canSave.bind(isDirty.and(
            state.isEqualTo(State.NEW).
            or(state.isEqualTo(State.CUSTOM))));

        canSaveAs.bind(state.isNotEqualTo(State.NEW));
        
        canDelete.bind(state.isNotEqualTo(State.ROBOX));

        isEditable.bind(state.isNotEqualTo(State.ROBOX));

        setupWidgetChangeListeners();

        setupPrintProfileCombo();

        selectFirstPrintProfile();

        setupWidgetEditableBindings();

        setupFirstLayerNozzleChoice();

        setupPerimeterNozzleChoice();

        setupFillNozzleChoice();

        setupSupportNozzleChoice();

        setupSlicerChooser();

        forceNozzleFirstLayerOptions.addAll(nozzleOptions);

        supportInterfaceNozzleChoice.setItems(nozzleOptions);

        fillPatternChoice.setItems(fillPatternOptions);

        supportPattern.setItems(supportPatternOptions);

        FXMLUtilities.addColonsToLabels(container);

        setupHelpTextListeners();
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
                selectPrintProfile(newValue);
            });
    }

    private void selectFirstPrintProfile()
    {
        cmbPrintProfile.setValue(cmbPrintProfile.getItems().get(0));
    }
    
    public void setAndSelectPrintProfile(SlicerParametersFile printProfile) {
        if (SlicerParametersContainer.getCompleteProfileList().contains(printProfile)) {
            cmbPrintProfile.setValue(printProfile);
        } else {
            cmbPrintProfile.getSelectionModel().selectFirst();
        }    
    }

    private void selectPrintProfile(SlicerParametersFile printProfile)
    {
        if (printProfile == null)
        {
            return;
        }
        currentProfileName = printProfile.getProfileName();
        updateWidgets(printProfile);
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
//            allFilaments.addAll(FilamentContainer.getUserFilamentList().sorted(
//                (Filament o1, Filament o2)
//                -> o1.getFriendlyFilamentName().compareTo(o2.getFriendlyFilamentName())));
            cmbPrintProfile.setItems(SlicerParametersContainer.getCompleteProfileList());
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
                    updateFieldDisabledState(newSlicer.getSlicerType());
                }
            }
        });
    }

    private void setupFirstLayerNozzleChoice()
    {
        firstLayerNozzleChoice.setItems(forceNozzleFirstLayerOptions);
        firstLayerNozzleChoice.getSelectionModel().selectedIndexProperty().addListener(
            (ObservableValue<? extends Number> observable, Number oldValue, Number newValue) ->
            {
                float currentWidth = firstLayerExtrusionWidth.floatValueProperty().get();
                switch (newValue.intValue())
                {
                    case 0:
                        // The point 3 nozzle has been selected
                        if (currentWidth < minPoint3ExtrusionWidth || currentWidth
                        > maxPoint3ExtrusionWidth)
                        {
                            firstLayerExtrusionWidth.floatValueProperty().set(
                                defaultPoint3ExtrusionWidth);
                        }
                        firstLayerExtrusionWidthSlider.setMin(minPoint3ExtrusionWidth);
                        firstLayerExtrusionWidthSlider.setMax(maxPoint3ExtrusionWidth);
                        break;
                    case 1:
                        // The point 8 nozzle has been selected
                        if (currentWidth < minPoint8ExtrusionWidth || currentWidth
                        > maxPoint8ExtrusionWidth)
                        {
                            firstLayerExtrusionWidth.floatValueProperty().set(
                                defaultPoint8ExtrusionWidth);
                        }
                        firstLayerExtrusionWidthSlider.setMin(minPoint8ExtrusionWidth);
                        firstLayerExtrusionWidthSlider.setMax(maxPoint8ExtrusionWidth);
                        break;
                }
            });
    }

    private void setupSupportNozzleChoice()
    {
        supportNozzleChoice.setItems(nozzleOptions);

        supportNozzleChoice.getSelectionModel()
            .selectedIndexProperty().addListener(
                (ObservableValue<? extends Number> observable, Number oldValue, Number newValue) ->
                {
                    float currentWidth = supportExtrusionWidth.floatValueProperty().get();
                    switch (newValue.intValue())
                    {
                        case 0:
                            // The point 3 nozzle has been selected
                            if (currentWidth < minPoint3ExtrusionWidth || currentWidth
                            > maxPoint3ExtrusionWidth)
                            {
                                supportExtrusionWidth.floatValueProperty().set(
                                    defaultPoint3ExtrusionWidth);
                            }
                            supportExtrusionWidthSlider.setMin(minPoint3ExtrusionWidth);
                            supportExtrusionWidthSlider.setMax(maxPoint3ExtrusionWidth);
                            break;
                        case 1:
                            // The point 8 nozzle has been selected
                            if (currentWidth < minPoint8ExtrusionWidth || currentWidth
                            > maxPoint8ExtrusionWidth)
                            {
                                supportExtrusionWidth.floatValueProperty().set(
                                    defaultPoint8ExtrusionWidth);
                            }
                            supportExtrusionWidthSlider.setMin(minPoint8ExtrusionWidth);
                            supportExtrusionWidthSlider.setMax(maxPoint8ExtrusionWidth);
                            break;
                    }
                });
    }

    private void setupFillNozzleChoice()
    {
        fillNozzleChoice.setItems(nozzleOptions);
        fillNozzleChoice.getSelectionModel().selectedIndexProperty().addListener(
            (ObservableValue<? extends Number> observable, Number oldValue, Number newValue) ->
            {
                float currentInfillWidth = infillExtrusionWidth.floatValueProperty().get();
                float currentSolidInfillWidth = solidInfillExtrusionWidth.floatValueProperty().get();
                float currentTopSolidInfillWidth = topSolidInfillExtrusionWidth.floatValueProperty().get();
                switch (newValue.intValue())
                {
                    case 0:
                        // The point 3 nozzle has been selected
                        if (currentInfillWidth < minPoint3ExtrusionWidth || currentInfillWidth
                        > maxPoint3ExtrusionWidth)
                        {
                            infillExtrusionWidth.floatValueProperty().set(
                                defaultPoint3ExtrusionWidth);
                        }
                        if (currentSolidInfillWidth < minPoint3ExtrusionWidth
                        || currentSolidInfillWidth
                        > maxPoint3ExtrusionWidth)
                        {
                            solidInfillExtrusionWidth.floatValueProperty().set(
                                defaultPoint3ExtrusionWidth);
                        }
                        if (currentTopSolidInfillWidth < minPoint3ExtrusionWidth
                        || currentTopSolidInfillWidth
                        > maxPoint3ExtrusionWidth)
                        {
                            topSolidInfillExtrusionWidth.floatValueProperty().set(
                                defaultPoint3ExtrusionWidth);
                        }

                        infillExtrusionWidthSlider.setMin(minPoint3ExtrusionWidth);
                        infillExtrusionWidthSlider.setMax(maxPoint3ExtrusionWidth);
                        solidInfillExtrusionWidthSlider.setMin(minPoint3ExtrusionWidth);
                        solidInfillExtrusionWidthSlider.setMax(maxPoint3ExtrusionWidth);
                        topSolidInfillExtrusionWidthSlider.setMin(minPoint3ExtrusionWidth);
                        topSolidInfillExtrusionWidthSlider.setMax(maxPoint3ExtrusionWidth);
                        break;
                    case 1:
                        // The point 8 nozzle has been selected
                        if (currentInfillWidth < minPoint8ExtrusionWidth || currentInfillWidth
                        > maxPoint8ExtrusionWidth)
                        {
                            infillExtrusionWidth.floatValueProperty().set(
                                defaultPoint8ExtrusionWidth);
                        }
                        if (currentSolidInfillWidth < minPoint8ExtrusionWidth
                        || currentSolidInfillWidth
                        > maxPoint8ExtrusionWidth)
                        {
                            solidInfillExtrusionWidth.floatValueProperty().set(
                                defaultPoint8ExtrusionWidth);
                        }
                        if (currentTopSolidInfillWidth < minPoint8ExtrusionWidth
                        || currentTopSolidInfillWidth
                        > maxPoint8ExtrusionWidth)
                        {
                            topSolidInfillExtrusionWidth.floatValueProperty().set(
                                defaultPoint8ExtrusionWidth);
                        }
                        infillExtrusionWidthSlider.setMin(minPoint8ExtrusionWidth);
                        infillExtrusionWidthSlider.setMax(maxPoint8ExtrusionWidth);
                        solidInfillExtrusionWidthSlider.setMin(minPoint8ExtrusionWidth);
                        solidInfillExtrusionWidthSlider.setMax(maxPoint8ExtrusionWidth);
                        topSolidInfillExtrusionWidthSlider.setMin(minPoint8ExtrusionWidth);
                        topSolidInfillExtrusionWidthSlider.setMax(maxPoint8ExtrusionWidth);
                        break;
                }
            });
    }

    private void setupPerimeterNozzleChoice()
    {
        perimeterNozzleChoice.setItems(nozzleOptions);
        perimeterNozzleChoice.getSelectionModel().selectedIndexProperty().addListener(
            (ObservableValue<? extends Number> observable, Number oldValue, Number newValue) ->
            {
                float currentWidth = perimeterExtrusionWidth.floatValueProperty().get();
                switch (newValue.intValue())
                {
                    case 0:
                        // The point 3 nozzle has been selected
                        if (currentWidth < minPoint3ExtrusionWidth || currentWidth
                        > maxPoint3ExtrusionWidth)
                        {
                            perimeterExtrusionWidth.floatValueProperty().set(
                                defaultPoint3ExtrusionWidth);
                        }
                        perimeterExtrusionWidthSlider.setMin(minPoint3ExtrusionWidth);
                        perimeterExtrusionWidthSlider.setMax(maxPoint3ExtrusionWidth);
                        break;
                    case 1:
                        // The point 8 nozzle has been selected
                        if (currentWidth < minPoint8ExtrusionWidth || currentWidth
                        > maxPoint8ExtrusionWidth)
                        {
                            perimeterExtrusionWidth.floatValueProperty().set(
                                defaultPoint8ExtrusionWidth);
                        }
                        perimeterExtrusionWidthSlider.setMin(minPoint8ExtrusionWidth);
                        perimeterExtrusionWidthSlider.setMax(maxPoint8ExtrusionWidth);
                        break;
                }
            });
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

    private void setupHelpTextListeners()
    {
        profileNameField.focusedProperty().addListener(
            (ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) ->
            {
                showHelpText(Fields.NAME);
            });
        slicerChooser.focusedProperty().addListener(
            (ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) ->
            {
                showHelpText(Fields.SLICER_CHOOSER);
            });
        layerHeight.focusedProperty().addListener(
            (ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) ->
            {
                showHelpText(Fields.LAYER_HEIGHT);
            });
        fillDensity.focusedProperty().addListener(
            (ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) ->
            {
                showHelpText(Fields.FILL_DENSITY);
            });
        fillPatternChoice.focusedProperty().addListener(
            (ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) ->
            {
                showHelpText(Fields.FILL_PATTERN);
            });
        infillEveryN.focusedProperty().addListener(
            (ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) ->
            {
                showHelpText(Fields.INFILL_EVERYN);
            });
        solidLayersTop.focusedProperty().addListener(
            (ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) ->
            {
                showHelpText(Fields.SOLID_LAYERS_TOP);
            });
        solidLayersBottom.focusedProperty().addListener(
            (ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) ->
            {
                showHelpText(Fields.SOLID_LAYERS_BOTTOM);
            });
        numberOfPerimeters.focusedProperty().addListener(
            (ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) ->
            {
                showHelpText(Fields.NUMBER_OF_PERIMETERS);
            });
        brimWidth.focusedProperty().addListener(
            (ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) ->
            {
                showHelpText(Fields.BRIM_WIDTH);
            });
        firstLayerExtrusionWidth.focusedProperty().addListener(
            (ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) ->
            {
                showHelpText(Fields.FIRST_LAYER_EXTRUSION_WIDTH);
            });
        perimeterExtrusionWidth.focusedProperty().addListener(
            (ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) ->
            {
                showHelpText(Fields.PERIMETER_EXTRUSION_WIDTH);
            });
        infillExtrusionWidth.focusedProperty().addListener(
            (ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) ->
            {
                showHelpText(Fields.INFILL_EXTRUSION_WIDTH);
            });
        solidInfillExtrusionWidth.focusedProperty().addListener(
            (ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) ->
            {
                showHelpText(Fields.SOLID_INFILL_EXTRUSION_WIDTH);
            });
        topSolidInfillExtrusionWidth.focusedProperty().addListener(
            (ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) ->
            {
                showHelpText(Fields.TOP_SOLID_INFILL_EXTRUSION_WIDTH);
            });
        supportExtrusionWidth.focusedProperty().addListener(
            (ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) ->
            {
                showHelpText(Fields.SUPPORT_EXTRUSION_WIDTH);
            });
        nozzleOpenVolume0.focusedProperty().addListener(
            (ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) ->
            {
                showHelpText(Fields.NOZZLE_OPEN_VOLUME);
            });
        nozzleOpenVolume1.focusedProperty().addListener(
            (ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) ->
            {
                showHelpText(Fields.NOZZLE_OPEN_VOLUME);
            });
        nozzleEjectionVolume0.focusedProperty().addListener(
            (ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) ->
            {
                showHelpText(Fields.NOZZLE_EJECTION_VOLUME);
            });
        nozzleEjectionVolume1.focusedProperty().addListener(
            (ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) ->
            {
                showHelpText(Fields.NOZZLE_EJECTION_VOLUME);
            });
        nozzlePartialOpen0.focusedProperty().addListener(
            (ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) ->
            {
                showHelpText(Fields.NOZZLE_PARTIAL_OPEN);
            });
        nozzlePartialOpen1.focusedProperty().addListener(
            (ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) ->
            {
                showHelpText(Fields.NOZZLE_PARTIAL_OPEN);
            });
        supportMaterialEnabled.focusedProperty().addListener(
            (ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) ->
            {
                showHelpText(Fields.SUPPORT_MATERIAL_ENABLED);
            });
        supportOverhangThreshold.focusedProperty().addListener(
            (ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) ->
            {
                showHelpText(Fields.SUPPORT_OVERHANG_THRESHOLD);
            });
        forcedSupportLayers.focusedProperty().addListener(
            (ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) ->
            {
                showHelpText(Fields.FORCED_SUPPORT_LAYERS);
            });
        supportPattern.focusedProperty().addListener(
            (ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) ->
            {
                showHelpText(Fields.SUPPORT_PATTERN);
            });
        supportPatternSpacing.focusedProperty().addListener(
            (ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) ->
            {
                showHelpText(Fields.SUPPORT_PATTERN_SPACING);
            });
        supportPatternAngle.focusedProperty().addListener(
            (ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) ->
            {
                showHelpText(Fields.SUPPORT_PATTERN_ANGLE);
            });
        firstLayerSpeed.focusedProperty().addListener(
            (ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) ->
            {
                showHelpText(Fields.FIRST_LAYER_SPEED);
            });
        perimeterSpeed.focusedProperty().addListener(
            (ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) ->
            {
                showHelpText(Fields.PERIMETER_SPEED);
            });
        smallPerimeterSpeed.focusedProperty().addListener(
            (ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) ->
            {
                showHelpText(Fields.SMALL_PERIMETER_SPEED);
            });
        externalPerimeterSpeed.focusedProperty().addListener(
            (ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) ->
            {
                showHelpText(Fields.EXTERNAL_PERIMETER_SPEED);
            });
        infillSpeed.focusedProperty().addListener(
            (ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) ->
            {
                showHelpText(Fields.INFILL_SPEED);
            });
        solidInfillSpeed.focusedProperty().addListener(
            (ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) ->
            {
                showHelpText(Fields.SOLID_INFILL_SPEED);
            });
        topSolidInfillSpeed.focusedProperty().addListener(
            (ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) ->
            {
                showHelpText(Fields.TOP_SOLID_INFILL_SPEED);
            });
        supportMaterialSpeed.focusedProperty().addListener(
            (ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) ->
            {
                showHelpText(Fields.SUPPORT_MATERIAL_SPEED);
            });
        bridgesSpeed.focusedProperty().addListener(
            (ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) ->
            {
                showHelpText(Fields.BRIDGES_SPEED);
            });
        gapFillSpeed.focusedProperty().addListener(
            (ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) ->
            {
                showHelpText(Fields.GAP_FILL_SPEED);
            });
        enableAutoCooling.focusedProperty().addListener(
            (ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) ->
            {
                showHelpText(Fields.ENABLE_AUTO_COOLING);
            });
        minFanSpeed.focusedProperty().addListener(
            (ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) ->
            {
                showHelpText(Fields.MIN_FAN_SPEED);
            });
        maxFanSpeed.focusedProperty().addListener(
            (ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) ->
            {
                showHelpText(Fields.MAX_FAN_SPEED);
            });
        bridgesFanSpeed.focusedProperty().addListener(
            (ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) ->
            {
                showHelpText(Fields.BRIDGES_FAN_SPEED);
            });
        disableFanForFirstNLayers.focusedProperty().addListener(
            (ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) ->
            {
                showHelpText(Fields.DISABLE_FAN_FIRST_N_LAYERS);
            });
        enableFanIfLayerTimeBelow.focusedProperty().addListener(
            (ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) ->
            {
                showHelpText(Fields.ENABLE_FAN_LAYER_TIME_BELOW);
            });
        slowFanIfLayerTimeBelow.focusedProperty().addListener(
            (ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) ->
            {
                showHelpText(Fields.SLOW_FAN_LAYER_TIME_BELOW);
            });
        minPrintSpeed.focusedProperty().addListener(
            (ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) ->
            {
                showHelpText(Fields.MIN_PRINT_SPEED);
            });
    }

    private void showHelpText(Fields field)
    {
        helpText.setText(field.getHelpText());
    }

    private void setupWidgetChangeListeners()
    {
        //Dirty listeners...
        profileNameField.textProperty().addListener(dirtyStringListener);

        slicerChooser.valueProperty().addListener(
            (ObservableValue<? extends CustomSlicerType> observable, CustomSlicerType oldValue, CustomSlicerType newValue) ->
            {
                isDirty.set(true);
            });

        //Nozzle Page
        firstLayerExtrusionWidthSlider.valueProperty()
            .bindBidirectional(firstLayerExtrusionWidth.floatValueProperty());
        firstLayerExtrusionWidth.textProperty().addListener(dirtyStringListener);
        firstLayerNozzleChoice.getSelectionModel().selectedItemProperty().addListener(
            dirtyStringListener);

        perimeterExtrusionWidthSlider.valueProperty().bindBidirectional(
            perimeterExtrusionWidth.floatValueProperty());
        perimeterExtrusionWidth.textProperty().addListener(dirtyStringListener);
        perimeterNozzleChoice.getSelectionModel().selectedItemProperty().addListener(
            dirtyStringListener);

        infillExtrusionWidthSlider.valueProperty().bindBidirectional(
            infillExtrusionWidth.floatValueProperty());
        infillExtrusionWidth.textProperty().addListener(dirtyStringListener);
        solidInfillExtrusionWidthSlider.valueProperty().bindBidirectional(
            solidInfillExtrusionWidth.floatValueProperty());
        solidInfillExtrusionWidth.textProperty().addListener(dirtyStringListener);
        fillNozzleChoice.getSelectionModel().selectedItemProperty().addListener(dirtyStringListener);
        topSolidInfillExtrusionWidthSlider.valueProperty().bindBidirectional(
            topSolidInfillExtrusionWidth.floatValueProperty());
        topSolidInfillExtrusionWidth.textProperty().addListener(dirtyStringListener);

        supportExtrusionWidthSlider.valueProperty()
            .bindBidirectional(supportExtrusionWidth.floatValueProperty());
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
        supportMaterialEnabled.textProperty().addListener(dirtyStringListener);
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
        brimWidth.textProperty()
            .addListener(dirtyStringListener);
        infillSpeed.textProperty()
            .addListener(dirtyStringListener);
        minPrintSpeed.textProperty()
            .addListener(dirtyStringListener);
        minFanSpeed.textProperty()
            .addListener(dirtyStringListener);
        infillEveryN.textProperty()
            .addListener(dirtyStringListener);
        forcedSupportLayers.textProperty()
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
        layerHeight.textProperty()
            .addListener(dirtyStringListener);
        externalPerimeterSpeed.textProperty()
            .addListener(dirtyStringListener);
        supportPatternAngle.textProperty()
            .addListener(dirtyStringListener);

        supportMaterialEnabled.selectedProperty()
            .addListener(dirtyBooleanListener);
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

        nozzleOpenVolume0.textProperty().addListener(dirtyStringListener);
        nozzleEjectionVolume0.textProperty().addListener(dirtyStringListener);
        nozzlePartialOpen0.textProperty().addListener(dirtyStringListener);
        nozzleOpenVolume1.textProperty().addListener(dirtyStringListener);
        nozzleEjectionVolume1.textProperty().addListener(dirtyStringListener);
        nozzlePartialOpen1.textProperty().addListener(dirtyStringListener);
    }

    private void updateWidgets(SlicerParametersFile parametersFile)
    {

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
        layerHeight.floatValueProperty().set(parametersFile.getLayerHeight_mm());
        fillDensity.floatValueProperty().set(parametersFile.getFillDensity_normalised());
        fillPatternChoice.valueProperty().set(parametersFile.getFillPattern());
        infillEveryN.intValueProperty().set(parametersFile.getFillEveryNLayers());
        solidLayersTop.intValueProperty().set(parametersFile.getSolidLayersAtTop());
        solidLayersBottom.intValueProperty().set(parametersFile.getSolidLayersAtBottom());
        numberOfPerimeters.intValueProperty().set(parametersFile.getNumberOfPerimeters());
        brimWidth.intValueProperty().set(parametersFile.getBrimWidth_mm());

        //Nozzle tab
        firstLayerExtrusionWidth.floatValueProperty().set(
            parametersFile.getFirstLayerExtrusionWidth_mm());
        firstLayerNozzleChoice.getSelectionModel().select(parametersFile.getFirstLayerNozzle());

        perimeterExtrusionWidth.floatValueProperty().set(
            parametersFile.getPerimeterExtrusionWidth_mm());
        perimeterNozzleChoice.getSelectionModel().select(parametersFile.getPerimeterNozzle());

        infillExtrusionWidth.floatValueProperty().set(parametersFile.getFillExtrusionWidth_mm());
        solidInfillExtrusionWidth.floatValueProperty().set(
            parametersFile.getSolidFillExtrusionWidth_mm());
        fillNozzleChoice.getSelectionModel().select(parametersFile.getFillNozzle());
        topSolidInfillExtrusionWidth.floatValueProperty().set(
            parametersFile.getTopSolidFillExtrusionWidth_mm());

        supportExtrusionWidth.floatValueProperty().set(parametersFile.getSupportExtrusionWidth_mm());
        supportNozzleChoice.getSelectionModel().select(parametersFile.getSupportNozzle());

        supportInterfaceNozzleChoice.getSelectionModel().select(
            parametersFile.getSupportInterfaceNozzle());

        //Support tab
        supportMaterialEnabled.selectedProperty().set(parametersFile.getGenerateSupportMaterial());
        supportOverhangThreshold.intValueProperty().set(
            parametersFile.getSupportOverhangThreshold_degrees());
        forcedSupportLayers.intValueProperty().set(parametersFile.getForcedSupportForFirstNLayers());
        supportPattern.valueProperty().set(parametersFile.getSupportPattern());
        supportPatternSpacing.floatValueProperty().set(parametersFile.getSupportPatternSpacing_mm());
        supportPatternAngle.intValueProperty().set(parametersFile.getSupportPatternAngle_degrees());

        //Speed tab
        firstLayerSpeed.intValueProperty().set(parametersFile.getFirstLayerSpeed_mm_per_s());
        perimeterSpeed.intValueProperty().set(parametersFile.getPerimeterSpeed_mm_per_s());
        smallPerimeterSpeed.intValueProperty().set(parametersFile.getSmallPerimeterSpeed_mm_per_s());
        externalPerimeterSpeed.intValueProperty().set(
            parametersFile.getExternalPerimeterSpeed_mm_per_s());
        infillSpeed.intValueProperty().set(parametersFile.getFillSpeed_mm_per_s());
        solidInfillSpeed.intValueProperty().set(parametersFile.getSolidFillSpeed_mm_per_s());
        topSolidInfillSpeed.intValueProperty().set(parametersFile.getTopSolidFillSpeed_mm_per_s());
        supportMaterialSpeed.intValueProperty().set(parametersFile.getSupportSpeed_mm_per_s());
        bridgesSpeed.intValueProperty().set(parametersFile.getBridgeSpeed_mm_per_s());
        gapFillSpeed.intValueProperty().set(parametersFile.getGapFillSpeed_mm_per_s());

        //Cooling tab
        enableAutoCooling.selectedProperty().set(parametersFile.getEnableCooling());
        minFanSpeed.intValueProperty().set(parametersFile.getMinFanSpeed_percent());
        maxFanSpeed.intValueProperty().set(parametersFile.getMaxFanSpeed_percent());
        bridgesFanSpeed.intValueProperty().set(parametersFile.getBridgeFanSpeed_percent());
        disableFanForFirstNLayers.intValueProperty().set(parametersFile.getDisableFanFirstNLayers());
        enableFanIfLayerTimeBelow.intValueProperty().set(
            parametersFile.getCoolIfLayerTimeLessThan_secs());
        slowFanIfLayerTimeBelow.intValueProperty().set(
            parametersFile.getSlowDownIfLayerTimeLessThan_secs());
        minPrintSpeed.intValueProperty().set(parametersFile.getMinPrintSpeed_mm_per_s());

        // nozzle
        nozzleOpenVolume0.floatValueProperty().set(
            parametersFile.getNozzleParameters().get(0).getOpenOverVolume());
        nozzleEjectionVolume0.floatValueProperty().set(parametersFile.getNozzleParameters().get(
            0).getEjectionVolume());
        nozzlePartialOpen0.floatValueProperty().set(parametersFile.getNozzleParameters().get(
            0).getPartialBMinimum());
        nozzleOpenVolume1.floatValueProperty().set(
            parametersFile.getNozzleParameters().get(1).getOpenOverVolume());
        nozzleEjectionVolume1.floatValueProperty().set(parametersFile.getNozzleParameters().get(
            1).getEjectionVolume());
        nozzlePartialOpen1.floatValueProperty().set(parametersFile.getNozzleParameters().get(
            1).getPartialBMinimum());

        updateFieldDisabledState(parametersFile.getSlicerOverride());
    }

    private void updateFieldDisabledState(SlicerType slicerType)
    {
        if (slicerType == null)
        {
            slicerType = Lookup.getUserPreferences().getSlicerType();
        }

        layerHeight.setDisable(!slicerMappings.isMapped(slicerType, "layerHeight_mm"));
        fillDensity.setDisable(!slicerMappings.isMapped(slicerType, "fillDensity_normalised"));
        infillEveryN.setDisable(!slicerMappings.isMapped(slicerType, "fillEveryNLayers"));
        solidLayersTop.setDisable(!slicerMappings.isMapped(slicerType, "solidLayersAtTop"));
        solidLayersBottom.setDisable(!slicerMappings.isMapped(slicerType, "solidLayersAtBottom"));
        numberOfPerimeters.setDisable(!slicerMappings.isMapped(slicerType, "numberOfPerimeters"));
        brimWidth.setDisable(!slicerMappings.isMapped(slicerType, "brimWidth_mm"));

        //Nozzle tab
        firstLayerExtrusionWidth.setDisable(!slicerMappings.isMapped(slicerType,
                                                                     "firstLayerExtrusionWidth_mm"));
        perimeterExtrusionWidth.setDisable(!slicerMappings.isMapped(slicerType,
                                                                    "perimeterExtrusionWidth_mm"));
        infillExtrusionWidth.setDisable(
            !slicerMappings.isMapped(slicerType, "fillExtrusionWidth_mm"));
        solidInfillExtrusionWidth.setDisable(!slicerMappings.isMapped(slicerType,
                                                                      "solidFillExtrusionWidth_mm"));
        topSolidInfillExtrusionWidth.setDisable(!slicerMappings.isMapped(slicerType,
                                                                         "topSolidFillExtrusionWidth_mm"));
        supportExtrusionWidth.setDisable(!slicerMappings.isMapped(slicerType,
                                                                  "supportExtrusionWidth_mm"));

        //Support tab
        supportOverhangThreshold.setDisable(!slicerMappings.isMapped(slicerType,
                                                                     "supportOverhangThreshold_degrees"));
        forcedSupportLayers.setDisable(!slicerMappings.isMapped(slicerType,
                                                                "forcedSupportForFirstNLayers"));
        supportPatternSpacing.setDisable(!slicerMappings.isMapped(slicerType,
                                                                  "supportPatternSpacing_mm"));
        supportPatternAngle.setDisable(!slicerMappings.isMapped(slicerType,
                                                                "supportPatternAngle_degrees"));

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
        SlicerParametersFile settingsToUpdate = makeNewSlicerParametersFile();
        settingsToUpdate.setSlicerOverride(slicerChooser.getValue().getSlicerType());
        settingsToUpdate.setProfileName(profileNameField.getText());
        // Extrusion tab
        settingsToUpdate.setLayerHeight_mm(layerHeight.floatValueProperty().get());
        settingsToUpdate.setFillDensity_normalised(fillDensity.floatValueProperty().get());
        settingsToUpdate.setFillPattern(fillPatternChoice.valueProperty().get());
        settingsToUpdate.setFillEveryNLayers(infillEveryN.intValueProperty().get());
        settingsToUpdate.setSolidLayersAtTop(solidLayersTop.intValueProperty().get());
        settingsToUpdate.setSolidLayersAtBottom(solidLayersBottom.intValueProperty().get());
        settingsToUpdate.setNumberOfPerimeters(numberOfPerimeters.intValueProperty().get());
        settingsToUpdate.setBrimWidth_mm(brimWidth.intValueProperty().get());

        //Nozzle tab
        settingsToUpdate.setFirstLayerExtrusionWidth_mm(
            firstLayerExtrusionWidth.floatValueProperty().get());
        settingsToUpdate.setFirstLayerNozzle(
            firstLayerNozzleChoice.getSelectionModel().getSelectedIndex());

        settingsToUpdate.setPerimeterExtrusionWidth_mm(
            perimeterExtrusionWidth.floatValueProperty().get());
        settingsToUpdate.setPerimeterNozzle(
            perimeterNozzleChoice.getSelectionModel().getSelectedIndex());

        settingsToUpdate.setFillExtrusionWidth_mm(infillExtrusionWidth.floatValueProperty().get());
        settingsToUpdate.setSolidFillExtrusionWidth_mm(
            solidInfillExtrusionWidth.floatValueProperty().get());
        settingsToUpdate.setFillNozzle(fillNozzleChoice.getSelectionModel().getSelectedIndex());
        settingsToUpdate.setTopSolidFillExtrusionWidth_mm(
            topSolidInfillExtrusionWidth.floatValueProperty().get());

        settingsToUpdate.setSupportExtrusionWidth_mm(
            supportExtrusionWidth.floatValueProperty().get());
        settingsToUpdate.setSupportNozzle(supportNozzleChoice.getSelectionModel().getSelectedIndex());

        settingsToUpdate.setSupportInterfaceNozzle(
            supportInterfaceNozzleChoice.getSelectionModel().getSelectedIndex());

        //Support tab
        settingsToUpdate.setGenerateSupportMaterial(supportMaterialEnabled.isSelected());
        settingsToUpdate.setSupportInterfaceNozzle(
            supportInterfaceNozzleChoice.getSelectionModel().getSelectedIndex());
        settingsToUpdate.setSupportOverhangThreshold_degrees(
            supportOverhangThreshold.intValueProperty().get());
        settingsToUpdate.setForcedSupportForFirstNLayers(
            forcedSupportLayers.intValueProperty().get());
        settingsToUpdate.setSupportPattern(supportPattern.valueProperty().get());
        settingsToUpdate.setSupportPatternSpacing_mm(
            supportPatternSpacing.floatValueProperty().get());
        settingsToUpdate.setSupportPatternAngle_degrees(supportPatternAngle.intValueProperty().get());

        //Speed tab
        settingsToUpdate.setFirstLayerSpeed_mm_per_s(firstLayerSpeed.intValueProperty().get());
        settingsToUpdate.setPerimeterSpeed_mm_per_s(perimeterSpeed.intValueProperty().get());
        settingsToUpdate.setSmallPerimeterSpeed_mm_per_s(
            smallPerimeterSpeed.intValueProperty().get());
        settingsToUpdate.setExternalPerimeterSpeed_mm_per_s(
            externalPerimeterSpeed.intValueProperty().get());
        settingsToUpdate.setFillSpeed_mm_per_s(infillSpeed.intValueProperty().get());
        settingsToUpdate.setSolidFillSpeed_mm_per_s(solidInfillSpeed.intValueProperty().get());
        settingsToUpdate.setTopSolidFillSpeed_mm_per_s(topSolidInfillSpeed.intValueProperty().get());
        settingsToUpdate.setSupportSpeed_mm_per_s(supportMaterialSpeed.intValueProperty().get());
        settingsToUpdate.setBridgeSpeed_mm_per_s(bridgesSpeed.intValueProperty().get());
        settingsToUpdate.setGapFillSpeed_mm_per_s(gapFillSpeed.intValueProperty().get());

        //Cooling tab
        settingsToUpdate.setEnableCooling(enableAutoCooling.selectedProperty().get());
        settingsToUpdate.setMinFanSpeed_percent(minFanSpeed.intValueProperty().get());
        settingsToUpdate.setMaxFanSpeed_percent(maxFanSpeed.intValueProperty().get());
        settingsToUpdate.setBridgeFanSpeed_percent(bridgesFanSpeed.intValueProperty().get());
        settingsToUpdate.setDisableFanFirstNLayers(
            disableFanForFirstNLayers.intValueProperty().get());
        settingsToUpdate.setCoolIfLayerTimeLessThan_secs(
            enableFanIfLayerTimeBelow.intValueProperty().get());
        settingsToUpdate.setSlowDownIfLayerTimeLessThan_secs(
            slowFanIfLayerTimeBelow.intValueProperty().get());
        settingsToUpdate.setMinPrintSpeed_mm_per_s(minPrintSpeed.intValueProperty().get());

        // Nozzle
        settingsToUpdate.getNozzleParameters().get(0).setOpenOverVolume(
            nozzleOpenVolume0.floatValueProperty().get());
        settingsToUpdate.getNozzleParameters().get(0).setEjectionVolume(
            nozzleEjectionVolume0.floatValueProperty().get());
        settingsToUpdate.getNozzleParameters().get(0).setPartialBMinimum(
            nozzlePartialOpen0.floatValueProperty().get());
        settingsToUpdate.getNozzleParameters().get(1).setOpenOverVolume(
            nozzleOpenVolume1.floatValueProperty().get());
        settingsToUpdate.getNozzleParameters().get(1).setEjectionVolume(
            nozzleEjectionVolume1.floatValueProperty().get());
        settingsToUpdate.getNozzleParameters().get(1).setPartialBMinimum(
            nozzlePartialOpen1.floatValueProperty().get());

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
            ObservableList<SlicerParametersFile> existingProfileList = SlicerParametersContainer.getUserProfileList();
            for (SlicerParametersFile settings : existingProfileList)
            {
                if (! settings.getProfileName().equals(currentProfileName)
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
     * Validate the data in the widgets and return false if it is invalid else return true.
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
        assert (state.get() != ProfileDetailsController.State.ROBOX);
        if (!validateData())
        {
            return;
        }
        SlicerParametersFile parametersFile = getPrintProfile();
        SlicerParametersContainer.saveProfile(parametersFile);
        repopulateCmbPrintProfile();
        cmbPrintProfile.setValue(SlicerParametersContainer.getSettingsByProfileName(
            parametersFile.getProfileName()));
    }

    void whenNewPressed()
    {
        state.set(ProfileDetailsController.State.NEW);
        SlicerParametersFile slicerParametersFile = makeNewSlicerParametersFile();
        slicerParametersFile.setProfileName("");
        updateWidgets(slicerParametersFile);
    }
    
    void whenSaveAsPressed()
    {
        state.set(ProfileDetailsController.State.NEW);
        SlicerParametersFile slicerParametersFile = SlicerParametersContainer.getSettingsByProfileName(
            currentProfileName).clone();
//        slicerParametersFile.setProfileName("XXX");
        updateWidgets(slicerParametersFile);
        profileNameField.requestFocus();
        profileNameField.selectAll();
    }    

    private SlicerParametersFile makeNewSlicerParametersFile()
    {
        SlicerParametersFile slicerParametersFile = new SlicerParametersFile();
        slicerParametersFile.setNozzleParameters(new ArrayList<>());
        slicerParametersFile.getNozzleParameters().add(new NozzleParameters());
        slicerParametersFile.getNozzleParameters().add(new NozzleParameters());
        slicerParametersFile.setFillPattern(FillPattern.LINE);
        slicerParametersFile.setSupportPattern(SupportPattern.RECTILINEAR);
        return slicerParametersFile;
    }

    void whenCopyPressed()
    {
        SlicerParametersFile parametersFile = SlicerParametersContainer.getSettingsByProfileName(
            currentProfileName).clone();
        Set<String> allCurrentNames = new HashSet<>();
        SlicerParametersContainer.getCompleteProfileList().forEach(
            (SlicerParametersFile printProfile) ->
            {
                allCurrentNames.add(printProfile.getProfileName());
            });
        String newName = DeDuplicator.suggestNonDuplicateName(parametersFile.getProfileName(),
                                                              allCurrentNames);
        parametersFile.setProfileName(newName);
        SlicerParametersContainer.saveProfile(parametersFile);
        repopulateCmbPrintProfile();
        cmbPrintProfile.setValue(SlicerParametersContainer.getSettingsByProfileName(newName));
    }

    void whenDeletePressed()
    {
        if (state.get() != ProfileDetailsController.State.NEW)
        {
            SlicerParametersContainer.deleteProfile(currentProfileName);
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
    public List<ExtrasMenuInnerPanel.OperationButton> getOperationButtons()
    {
        List<ExtrasMenuInnerPanel.OperationButton> operationButtons = new ArrayList<>();
        ExtrasMenuInnerPanel.OperationButton saveButton = new ExtrasMenuInnerPanel.OperationButton()
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
        ExtrasMenuInnerPanel.OperationButton saveAsButton = new ExtrasMenuInnerPanel.OperationButton()
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
        ExtrasMenuInnerPanel.OperationButton deleteButton = new ExtrasMenuInnerPanel.OperationButton()
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
