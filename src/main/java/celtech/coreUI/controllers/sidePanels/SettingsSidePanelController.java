/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 *//*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.coreUI.controllers.sidePanels;

import celtech.appManager.ApplicationMode;
import celtech.appManager.ApplicationStatus;
import celtech.configuration.ApplicationConfiguration;
import celtech.configuration.Filament;
import celtech.configuration.FilamentContainer;
import celtech.configuration.MaterialType;
import celtech.configuration.PrintProfileContainer;
import celtech.coreUI.DisplayManager;
import celtech.coreUI.components.MaterialChoiceListCell;
import celtech.coreUI.components.ModalDialog;
import celtech.coreUI.components.ProfileChoiceListCell;
import celtech.coreUI.controllers.SettingsScreenState;
import celtech.coreUI.controllers.popups.PopupCommandReceiver;
import celtech.coreUI.controllers.utilityPanels.MaterialDetailsController;
import celtech.coreUI.controllers.utilityPanels.ProfileDetailsController;
import celtech.printerControl.Printer;
import celtech.printerControl.comms.RoboxCommsManager;
import celtech.services.slicer.PrintQualityEnumeration;
import celtech.services.slicer.SlicerSettings;
import celtech.utils.SystemUtils;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Slider;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Callback;
import javafx.util.StringConverter;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 * FXML Controller class
 *
 * @author Ian Hudson @ Liberty Systems Limited
 */
public class SettingsSidePanelController implements Initializable, SidePanelManager, PopupCommandReceiver
{

    private Stenographer steno = StenographerFactory.getStenographer(SettingsSidePanelController.class.getName());
    private ObservableList<Printer> printerStatusList = null;
    private SettingsScreenState settingsScreenState = null;
    private ApplicationStatus applicationStatus = null;
    private DisplayManager displayManager = null;

    @FXML
    private ComboBox<Printer> printerChooser;

    @FXML
    private ComboBox<Filament> materialChooser;

    @FXML
    private ComboBox<SlicerSettings> customProfileChooser;

    @FXML
    private Label customSettingsLabel;

    @FXML
    private Slider qualityChooser;

    @FXML
    private VBox supportVBox;

    @FXML
    private VBox customProfileVBox;

    @FXML
    private ToggleGroup supportMaterialGroup;

    @FXML
    private RadioButton noSupportRadioButton;

    @FXML
    private RadioButton autoSupportRadioButton;

    @FXML
    void go(MouseEvent event)
    {
        settingsScreenState.getSettings().writeToFile("/tmp/settings.dat");
    }

    private SlicerSettings draftSettings = PrintProfileContainer.getSettingsByProfileName(ApplicationConfiguration.draftSettingsProfileName);
    private SlicerSettings normalSettings = PrintProfileContainer.getSettingsByProfileName(ApplicationConfiguration.normalSettingsProfileName);
    private SlicerSettings fineSettings = PrintProfileContainer.getSettingsByProfileName(ApplicationConfiguration.fineSettingsProfileName);
    private SlicerSettings customSettings = null;
    private SlicerSettings lastSettings = null;

    private ChangeListener<Toggle> nozzleSelectionListener = null;
    private ChangeListener<Filament> filamentChangeListener = null;
    private ChangeListener<Boolean> reelDataChangedListener = null;

    private ObservableList<Filament> availableFilaments = FXCollections.observableArrayList();
    private ObservableList<SlicerSettings> availableProfiles = FXCollections.observableArrayList();

    private Printer currentPrinter = null;
    private Filament currentlyLoadedFilament = null;

    private VBox createMaterialPage = null;
    private ModalDialog createMaterialDialogue = null;
    private int saveMaterialAction = 0;
    private int cancelMaterialSaveAction = 0;

    private VBox createProfilePage = null;
    private ModalDialog createProfileDialogue = null;
    private int saveProfileAction = 0;
    private int cancelProfileSaveAction = 0;

    private SettingsSlideOutPanelController slideOutController = null;

    private MaterialDetailsController materialDetailsController = null;
    private ProfileDetailsController profileDetailsController = null;

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb)
    {
        applicationStatus = ApplicationStatus.getInstance();
        displayManager = DisplayManager.getInstance();
        settingsScreenState = SettingsScreenState.getInstance();
        printerStatusList = RoboxCommsManager.getInstance().getPrintStatusList();

        try
        {
            FXMLLoader createMaterialPageLoader = new FXMLLoader(getClass().getResource(ApplicationConfiguration.fxmlUtilityPanelResourcePath + "materialDetails.fxml"), DisplayManager.getLanguageBundle());
            createMaterialPage = createMaterialPageLoader.load();
            materialDetailsController = createMaterialPageLoader.getController();
            materialDetailsController.updateMaterialData(new Filament("", MaterialType.ABS, null,
                    0, 0, 0, 0, 0, 0, 0, 0, Color.ALICEBLUE, true));
            materialDetailsController.showButtons(false);

            createMaterialDialogue = new ModalDialog(DisplayManager.getLanguageBundle().getString("sidePanel_settings.createMaterialDialogueTitle"));
            createMaterialDialogue.setContent(createMaterialPage);
            saveMaterialAction = createMaterialDialogue.addButton(DisplayManager.getLanguageBundle().getString("genericFirstLetterCapitalised.Save"), materialDetailsController.getProfileNameInvalidProperty());
            cancelMaterialSaveAction = createMaterialDialogue.addButton(DisplayManager.getLanguageBundle().getString("genericFirstLetterCapitalised.Cancel"));

        } catch (Exception ex)
        {
            steno.error("Failed to load material creation page");
        }

        FilamentContainer.getUserFilamentList().addListener(new ListChangeListener<Filament>()
        {
            @Override
            public void onChanged(ListChangeListener.Change<? extends Filament> c)
            {
                updateFilamentList();
            }
        });

        try
        {
            FXMLLoader createProfilePageLoader = new FXMLLoader(getClass().getResource(ApplicationConfiguration.fxmlUtilityPanelResourcePath + "profileDetails.fxml"), DisplayManager.getLanguageBundle());
            createProfilePage = createProfilePageLoader.load();
            profileDetailsController = createProfilePageLoader.getController();
            profileDetailsController.updateProfileData(new SlicerSettings(true));
            profileDetailsController.showButtons(false);

            createProfileDialogue = new ModalDialog(DisplayManager.getLanguageBundle().getString("sidePanel_settings.createProfileDialogueTitle"));
            createProfileDialogue.setContent(createProfilePage);
            saveProfileAction = createProfileDialogue.addButton(DisplayManager.getLanguageBundle().getString("genericFirstLetterCapitalised.Save"), profileDetailsController.getProfileNameInvalidProperty());
            cancelProfileSaveAction = createProfileDialogue.addButton(DisplayManager.getLanguageBundle().getString("genericFirstLetterCapitalised.Cancel"));
        } catch (Exception ex)
        {
            steno.error("Failed to load profile creation page");
        }

        qualityChooser.setLabelFormatter(new StringConverter<Double>()
        {
            @Override
            public String toString(Double n)
            {
                PrintQualityEnumeration selectedQuality = PrintQualityEnumeration.fromEnumPosition(n.intValue());
                return selectedQuality.getFriendlyName();
            }

            @Override
            public Double fromString(String s)
            {
                PrintQualityEnumeration selectedQuality = PrintQualityEnumeration.valueOf(s);
                return (double) selectedQuality.getEnumPosition();
            }
        });

        settingsScreenState.setPrintQuality(PrintQualityEnumeration.DRAFT);
        settingsScreenState.setSettings(draftSettings);
        if (draftSettings.support_materialProperty().get() == true)
        {
            autoSupportRadioButton.selectedProperty().set(true);
        } else
        {
            noSupportRadioButton.selectedProperty().set(true);
        }

        qualityChooser.valueProperty().addListener(new ChangeListener<Number>()
        {
            @Override
            public void changed(ObservableValue<? extends Number> ov, Number t, Number t1)
            {
                PrintQualityEnumeration quality = PrintQualityEnumeration.fromEnumPosition(t1.intValue());
                settingsScreenState.setPrintQuality(quality);

                SlicerSettings settings = null;

                switch (quality)
                {
                    case DRAFT:
                        settings = draftSettings;
                        break;
                    case NORMAL:
                        settings = normalSettings;
                        break;
                    case FINE:
                        settings = fineSettings;
                        break;
                    case CUSTOM:
                        settings = customSettings;
                        break;
                    default:
                        break;
                }

                if (settings != null)
                {
                    if (settings.support_materialProperty().get() == true)
                    {
                        autoSupportRadioButton.selectedProperty().set(true);
                    } else
                    {
                        noSupportRadioButton.selectedProperty().set(true);
                    }
                }

                slideOutController.updateProfileData(settings);
                settingsScreenState.setSettings(settings);
            }
        });

        qualityChooser.setValue(PrintQualityEnumeration.DRAFT.getEnumPosition());

        customProfileVBox.visibleProperty().bind(qualityChooser.valueProperty().isEqualTo(PrintQualityEnumeration.CUSTOM.getEnumPosition()));

        Callback<ListView<SlicerSettings>, ListCell<SlicerSettings>> profileChooserCellFactory
                = new Callback<ListView<SlicerSettings>, ListCell<SlicerSettings>>()
                {
                    @Override
                    public ListCell<SlicerSettings> call(ListView<SlicerSettings> list)
                    {
                        return new ProfileChoiceListCell();
                    }
                };

        customProfileChooser.setCellFactory(profileChooserCellFactory);
        customProfileChooser.setButtonCell(profileChooserCellFactory.call(null));
        customProfileChooser.setItems(availableProfiles);

        updateProfileList();

        customProfileChooser.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<SlicerSettings>()
        {
            @Override
            public void changed(ObservableValue<? extends SlicerSettings> observable, SlicerSettings oldValue, SlicerSettings newValue)
            {
                if (newValue == PrintProfileContainer.createNewProfile)
                {
                    showCreateProfileDialogue();
                } else if (newValue != null)
                {
                    slideOutController.updateProfileData(newValue);
                    customSettings = newValue;
                    if (PrintQualityEnumeration.fromEnumPosition((int) qualityChooser.getValue()) == PrintQualityEnumeration.CUSTOM)
                    {
                        settingsScreenState.setSettings(newValue);
                    }
                } else if (newValue == null)
                {
                    customProfileChooser.getSelectionModel().selectFirst();
                }
            }
        });

        PrintProfileContainer.getUserProfileList().addListener(new ListChangeListener<SlicerSettings>()
        {
            @Override
            public void onChanged(ListChangeListener.Change<? extends SlicerSettings> c)
            {
                updateProfileList();
            }
        });
        printerChooser.setItems(printerStatusList);

        printerChooser.getSelectionModel()
                .clearSelection();

        printerChooser.getItems().addListener(new ListChangeListener<Printer>()
        {
            @Override
            public void onChanged(ListChangeListener.Change<? extends Printer> change
            )
            {
                while (change.next())
                {
                    if (change.wasAdded())
                    {
                        for (Printer addedPrinter : change.getAddedSubList())
                        {
                            Platform.runLater(new Runnable()
                            {

                                @Override
                                public void run()
                                {
                                    printerChooser.setValue(addedPrinter);
                                }
                            });
                        }
                    } else if (change.wasRemoved())
                    {
                        for (Printer removedPrinter : change.getRemoved())
                        {
                            if (printerChooser.getItems().isEmpty())
                            {
                                Platform.runLater(new Runnable()
                                {
                                    @Override
                                    public void run()
                                    {
                                        printerChooser.getSelectionModel().select(null);
                                    }
                                });
                            } else
                            {
                                Platform.runLater(new Runnable()
                                {
                                    @Override
                                    public void run()
                                    {
                                        printerChooser.getSelectionModel().selectFirst();
                                    }
                                });
                            }
                        }
                    } else if (change.wasReplaced())
                    {
                        steno.info("Replace");
                    } else if (change.wasUpdated())
                    {
                        steno.info("Update");

                    }
                }
            }
        }
        );

        printerChooser.getSelectionModel()
                .selectedItemProperty().addListener(new ChangeListener<Printer>()
                        {
                            @Override
                            public void changed(ObservableValue<? extends Printer> ov, Printer lastSelectedPrinter, Printer selectedPrinter
                            )
                            {
                                if (lastSelectedPrinter != null)
                                {
                                    lastSelectedPrinter.reelDataChangedProperty().removeListener(reelDataChangedListener);
                                    lastSelectedPrinter.loadedFilamentProperty().removeListener(filamentChangeListener);
                                }
                                if (selectedPrinter != null && selectedPrinter != lastSelectedPrinter)
                                {
                                    currentPrinter = selectedPrinter;
                                    selectedPrinter.reelDataChangedProperty().addListener(reelDataChangedListener);
                                    selectedPrinter.loadedFilamentProperty().addListener(filamentChangeListener);
                                }

                                if (selectedPrinter == null)
                                {
                                    currentPrinter = null;
                                }

                                settingsScreenState.setSelectedPrinter(selectedPrinter);

                            }
                }
                );

        Callback<ListView<Filament>, ListCell<Filament>> materialChooserCellFactory
                = new Callback<ListView<Filament>, ListCell<Filament>>()
                {
                    @Override
                    public ListCell<Filament> call(ListView<Filament> list)
                    {
                        return new MaterialChoiceListCell();
                    }
                };

        materialChooser.setCellFactory(materialChooserCellFactory);
        materialChooser.setButtonCell(materialChooserCellFactory.call(null));
        materialChooser.setItems(availableFilaments);

        materialChooser.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Filament>()
        {
            @Override
            public void changed(ObservableValue<? extends Filament> observable, Filament oldValue, Filament newValue)
            {
                if (newValue == FilamentContainer.createNewFilament)
                {
                    showCreateMaterialDialogue();
                } else
                {
                    slideOutController.updateFilamentData(newValue);
                }

            }
        });

        filamentChangeListener = new ChangeListener<Filament>()
        {
            @Override
            public void changed(ObservableValue<? extends Filament> ov, Filament t, Filament t1)
            {
                currentlyLoadedFilament = t1;
                updateFilamentList();
            }
        };

        reelDataChangedListener = new ChangeListener<Boolean>()
        {
            @Override
            public void changed(ObservableValue<? extends Boolean> ov, Boolean t, Boolean t1)
            {
//                updateMaterialTextBoxes();
            }
        };

        supportVBox.visibleProperty().bind(qualityChooser.valueProperty().isNotEqualTo(PrintQualityEnumeration.CUSTOM.getEnumPosition()));

        supportMaterialGroup.selectedToggleProperty().addListener(new ChangeListener<Toggle>()
        {
            @Override
            public void changed(ObservableValue<? extends Toggle> observable, Toggle oldValue, Toggle newValue)
            {
                if (newValue == noSupportRadioButton)
                {
                    settingsScreenState.getSettings().setSupport_material(false);
                } else if (newValue == autoSupportRadioButton)
                {
                    settingsScreenState.getSettings().setSupport_material(true);
                }
            }
        });
    }

    private void updateProfileList()
    {
        availableProfiles.clear();
        availableProfiles.addAll(PrintProfileContainer.getUserProfileList());
        availableProfiles.add(PrintProfileContainer.createNewProfile);
    }

    private void updateFilamentList()
    {
        availableFilaments.clear();

        if (currentlyLoadedFilament != null)
        {
            availableFilaments.add(currentlyLoadedFilament);
            materialChooser.getSelectionModel().select(currentlyLoadedFilament);
        }

        availableFilaments.addAll(FilamentContainer.getUserFilamentList());
        availableFilaments.add(FilamentContainer.createNewFilament);
    }

    private void populatePrinterChooser()
    {
        for (Printer printer : printerStatusList)
        {
            printerChooser.getItems().add(printer);
        }
    }

    @Override
    public void configure(Initializable slideOutController)
    {
        this.slideOutController = (SettingsSlideOutPanelController) slideOutController;
        this.slideOutController.provideReceiver(this);
        this.slideOutController.updateProfileData(settingsScreenState.getSettings());
        updateFilamentList();
        updateProfileList();
    }

    @Override
    public void triggerSaveAs()
    {
        SlicerSettings settings = settingsScreenState.getSettings().clone();
        String originalProfileName = settings.getProfileName();
        String filename = SystemUtils.getIncrementalFilenameOnly(ApplicationConfiguration.getUserPrintProfileDirectory(), originalProfileName, ApplicationConfiguration.printProfileFileExtension);
        settings.getProfileNameProperty().set(filename);
        settings.setMutable(true);
        profileDetailsController.updateProfileData(settings);
        showCreateProfileDialogue();
    }

    private int showCreateMaterialDialogue()
    {
        int response = createMaterialDialogue.show();
        if (response == saveMaterialAction)
        {
            Filament filamentToSave = materialDetailsController.getMaterialData();
            FilamentContainer.saveFilament(filamentToSave);

//            String profileNameToSave = profileDetailsController.getProfileName();
//            SlicerSettings settingsToSave = profileDetailsController.getProfileData();
//            settingsToSave.getProfileNameProperty().set(profileNameToSave);
//            PrintProfileContainer.saveProfile(settingsToSave);
//            updateProfileList();
//            for (SlicerSettings settings : availableProfiles)
//            {
//                if (settings.getProfileName().equals(profileNameToSave))
//                {
//                    customProfileChooser.getSelectionModel().select(settings);
//                    break;
//                }
//            }
//            qualityChooser.adjustValue(PrintQualityEnumeration.CUSTOM.getEnumPosition());
        }

        return response;
    }

    private int showCreateProfileDialogue()
    {
        int response = createProfileDialogue.show();
        if (response == saveProfileAction)
        {
            String profileNameToSave = profileDetailsController.getProfileName();
            SlicerSettings settingsToSave = profileDetailsController.getProfileData();
            settingsToSave.getProfileNameProperty().set(profileNameToSave);
            PrintProfileContainer.saveProfile(settingsToSave);
            updateProfileList();
            for (SlicerSettings settings : availableProfiles)
            {
                if (settings.getProfileName().equals(profileNameToSave))
                {
                    customProfileChooser.getSelectionModel().select(settings);
                    break;
                }
            }
            qualityChooser.adjustValue(PrintQualityEnumeration.CUSTOM.getEnumPosition());
        }

        return response;
    }
}
