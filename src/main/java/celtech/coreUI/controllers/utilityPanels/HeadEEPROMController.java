package celtech.coreUI.controllers.utilityPanels;

import celtech.Lookup;
import celtech.coreUI.components.ModalDialog;
import celtech.coreUI.components.RestrictedTextField;
import celtech.printerControl.comms.commands.exceptions.RoboxCommsException;
import celtech.printerControl.comms.commands.rx.HeadEEPROMDataResponse;
import celtech.printerControl.model.Head;
import celtech.printerControl.model.Printer;
import celtech.printerControl.model.PrinterException;
import celtech.printerControl.model.Reel;
import celtech.utils.PrinterListChangesListener;
import celtech.utils.PrinterUtils;
import java.net.URL;
import java.text.ParseException;
import java.util.ResourceBundle;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.layout.GridPane;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author Ian
 */
public class HeadEEPROMController implements Initializable, PrinterListChangesListener
{

    Stenographer steno = StenographerFactory.getStenographer(HeadEEPROMController.class.getName());

    @FXML
    private RestrictedTextField nozzle2ZOverrun;

    @FXML
    private RestrictedTextField headThermistorTCal;

    @FXML
    private RestrictedTextField headThermistorBeta;

    @FXML
    private RestrictedTextField lastFilamentTemperature0;

    @FXML
    private RestrictedTextField lastFilamentTemperature1;

    @FXML
    private RestrictedTextField nozzle1ZOverrun;

    @FXML
    private RestrictedTextField headMaxTemperature;

    @FXML
    private RestrictedTextField nozzle2XOffset;

    @FXML
    private RestrictedTextField nozzle1YOffset;

    @FXML
    private RestrictedTextField nozzle2YOffset;

    @FXML
    private RestrictedTextField nozzle1XOffset;

    @FXML
    private RestrictedTextField headHourCounter;

    @FXML
    private RestrictedTextField nozzle2BOffset;

    @FXML
    private RestrictedTextField headUniqueID;

    @FXML
    private RestrictedTextField headTypeCode;

    @FXML
    private RestrictedTextField nozzle1BOffset;

    @FXML
    private RestrictedTextField headType;

    @FXML
    private Button writeOffsetsButton;

    @FXML
    private GridPane headEEPROMOffsets;

    private ModalDialog eepromCommsError = null;

    private final BooleanProperty offsetFieldsDirty = new SimpleBooleanProperty();

    private Printer selectedPrinter;

    @FXML
    void resetToDefaults(ActionEvent event)
    {
        try
        {
            selectedPrinter.resetHeadToDefaults();
        } catch (PrinterException ex)
        {
            steno.error("Unable to repair head");
        }
    }

    @FXML
    /**
     * Write the values from the text fields onto the actual head. If the unique id is already
     * stored on the head then do not overwrite it.
     */
    void writeHeadConfig(ActionEvent event)
    {
        try
        {
            String headTypeCodeText = headTypeCode.getText();
            Float headMaxTemperatureVal = headMaxTemperature.getFloatValue();
            Float headThermistorBetaVal = headThermistorBeta.getFloatValue();
            Float headThermistorTCalVal = headThermistorTCal.getFloatValue();
            Float nozzle1XOffsetVal = nozzle1XOffset.getFloatValue();
            Float nozzle1YOffsetVal = nozzle1YOffset.getFloatValue();
            Float nozzle1BOffsetVal = nozzle1BOffset.getFloatValue();
            Float nozzle2XOffsetVal = nozzle2XOffset.getFloatValue();
            Float nozzle2YOffsetVal = nozzle2YOffset.getFloatValue();
            Float nozzle2BOffsetVal = nozzle2BOffset.getFloatValue();
            Float lastFilamentTemperatureVal0 = lastFilamentTemperature0.getFloatValue();
            Float lastFilamentTemperatureVal1 = lastFilamentTemperature1.getFloatValue();
            Float headHourCounterVal = headHourCounter.getFloatValue();

            float nozzle1ZOffsetCalculated = PrinterUtils.deriveNozzle1ZOffsetsFromOverrun(
                nozzle1ZOverrun.getFloatValue(), nozzle2ZOverrun.getFloatValue());
            float nozzle2ZOffsetCalculated = PrinterUtils.deriveNozzle2ZOffsetsFromOverrun(
                nozzle1ZOverrun.getFloatValue(), nozzle2ZOverrun.getFloatValue());

            // N.B. this call must come after reading the data in the fields because
            // reading the head eeprom results in the fields being updated with current head
            // data (i.e. fields will lose edited values)
            HeadEEPROMDataResponse headDataResponse = selectedPrinter.readHeadEEPROM();
            String uniqueId = headDataResponse.getUniqueID();
            if (uniqueId.length() == 0)
            {
                uniqueId = headUniqueID.getText();
            }

            selectedPrinter.transmitWriteHeadEEPROM(
                headTypeCodeText, uniqueId, headMaxTemperatureVal, headThermistorBetaVal,
                headThermistorTCalVal, nozzle1XOffsetVal, nozzle1YOffsetVal,
                nozzle1ZOffsetCalculated, nozzle1BOffsetVal,
                "", "",
                nozzle2XOffsetVal, nozzle2YOffsetVal,
                nozzle2ZOffsetCalculated, nozzle2BOffsetVal,
                lastFilamentTemperatureVal0, lastFilamentTemperatureVal1, headHourCounterVal);
            offsetFieldsDirty.set(false);
//            selectedPrinter.readHeadEEPROM();
//            updateFieldsFromAttachedHead(selectedPrinter.headProperty().get());
        } catch (RoboxCommsException ex)
        {
            steno.error("Error writing head EEPROM");
            eepromCommsError.setMessage(Lookup.i18n(
                "eeprom.headWriteError"));
            eepromCommsError.show();
        } catch (ParseException ex)
        {
            steno.error("Parse error getting head data");
        }
    }

    void readPrinterID(ActionEvent event)
    {
        try
        {
            selectedPrinter.readPrinterID();
        } catch (PrinterException ex)
        {
            steno.error("Error reading printer ID");
        }
    }

    /**
     * Initialises the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb)
    {
        try
        {
            eepromCommsError = new ModalDialog();
            eepromCommsError.setTitle(Lookup.i18n("eeprom.error"));
            eepromCommsError.addButton(Lookup.i18n("dialogs.OK"));

            setUpWriteEnabledAfterEdits();

            Lookup.getCurrentlySelectedPrinterProperty().addListener(
                (ObservableValue<? extends Printer> observable, Printer oldValue, Printer newValue) ->
                {
                    if (newValue != oldValue)
                    {
                        setSelectedPrinter(newValue);

                    }
                });

            Lookup.getPrinterListChangesNotifier().addListener(this);

            if (Lookup.getCurrentlySelectedPrinterProperty().get() != null)
            {
                setSelectedPrinter(
                    Lookup.getCurrentlySelectedPrinterProperty().get());
            }

        } catch (Exception ex)
        {
            ex.printStackTrace();
        }

    }

    private void setUpWriteEnabledAfterEdits()
    {
        ChangeListener offsetsChangedListener = (ChangeListener<String>) (ObservableValue<? extends String> observable, String oldValue, String newValue) ->
        {
            offsetFieldsDirty.set(true);
        };

        nozzle1BOffset.textProperty().addListener(offsetsChangedListener);
        nozzle1XOffset.textProperty().addListener(offsetsChangedListener);
        nozzle1YOffset.textProperty().addListener(offsetsChangedListener);
        nozzle1ZOverrun.textProperty().addListener(offsetsChangedListener);
        nozzle2BOffset.textProperty().addListener(offsetsChangedListener);
        nozzle2XOffset.textProperty().addListener(offsetsChangedListener);
        nozzle2YOffset.textProperty().addListener(offsetsChangedListener);
        nozzle2ZOverrun.textProperty().addListener(offsetsChangedListener);

        writeOffsetsButton.disableProperty().bind(Bindings.not(offsetFieldsDirty));

    }

    private void updateFieldsFromAttachedHead(Head head)
    {
        headTypeCode.setText(head.typeCodeProperty().get().trim());
        headType.setText(head.nameProperty().get().trim());
        headUniqueID.setText(head.uniqueIDProperty().get().trim());
        lastFilamentTemperature0.setText(String.format("%.0f",
                           head.getNozzleHeaters().get(0).lastFilamentTemperatureProperty().get()));
        if (head.getNozzleHeaters().size() > 1)
        {
            lastFilamentTemperature1.setText(String.format("%.0f",
                           head.getNozzleHeaters().get(1).lastFilamentTemperatureProperty().get()));
        }
        headHourCounter.setText(String.format("%.2f", head.headHoursProperty().get()));
        //TODO modify to work with multiple heaters
        headMaxTemperature.setText(String.format("%.0f",
                                                 head.getNozzleHeaters().get(0).maximumTemperatureProperty().get()));
        //TODO modify to work with multiple heaters
        headThermistorBeta.setText(String.format("%.2f",
                                                 head.getNozzleHeaters().get(0).betaProperty().get()));
        //TODO modify to work with multiple heaters
        headThermistorTCal.setText(String.format("%.2f",
                                                 head.getNozzleHeaters().get(0).tCalProperty().get()));
        nozzle1BOffset.setText(String.format("%.2f",
                                             head.getNozzles().get(0).bOffsetProperty().get()));
        nozzle1XOffset.setText(String.format("%.2f",
                                             head.getNozzles().get(0).xOffsetProperty().get()));
        nozzle1YOffset.setText(String.format("%.2f",
                                             head.getNozzles().get(0).yOffsetProperty().get()));
        nozzle2BOffset.setText(String.format("%.2f",
                                             head.getNozzles().get(1).bOffsetProperty().get()));
        nozzle2XOffset.setText(String.format("%.2f",
                                             head.getNozzles().get(1).xOffsetProperty().get()));
        nozzle2YOffset.setText(String.format("%.2f",
                                             head.getNozzles().get(1).yOffsetProperty().get()));

        //TODO modify to deal with variable numbers of nozzle
        float nozzle1Offset = head.getNozzles().get(0).zOffsetProperty().get();
        float nozzle2Offset = head.getNozzles().get(1).zOffsetProperty().get();
        float nozzle1ZOverrunValue = PrinterUtils.deriveNozzle1OverrunFromOffsets(nozzle1Offset,
                                                                                  nozzle2Offset);
        float nozzle2ZOverrunValue = PrinterUtils.deriveNozzle2OverrunFromOffsets(nozzle1Offset,
                                                                                  nozzle2Offset);

        nozzle1ZOverrun.setText(String.format("%.2f", nozzle1ZOverrunValue));
        nozzle2ZOverrun.setText(String.format("%.2f", nozzle2ZOverrunValue));
        offsetFieldsDirty.set(false);
    }

    private void updateFieldsForNoHead()
    {
        headTypeCode.setText("");
        headType.setText("");
        headUniqueID.setText("");
        //TODO modify to work with multiple heaters
        lastFilamentTemperature0.setText("");
        lastFilamentTemperature1.setText("");
        headHourCounter.setText("");
        //TODO modify to work with multiple heaters
        headMaxTemperature.setText("");
        //TODO modify to work with multiple heaters
        headThermistorBeta.setText("");
        //TODO modify to work with multiple heaters
        headThermistorTCal.setText("");
        nozzle1BOffset.setText("");
        nozzle1XOffset.setText("");
        nozzle1YOffset.setText("");
        nozzle2BOffset.setText("");
        nozzle2XOffset.setText("");
        nozzle2YOffset.setText("");

        nozzle1ZOverrun.setText("");
        nozzle2ZOverrun.setText("");
        offsetFieldsDirty.set(false);
    }

    private void setSelectedPrinter(Printer printer)
    {
        updateFieldsForNoHead();
        if (selectedPrinter != null && selectedPrinter.headProperty().get() != null)
        {
            removeHeadChangeListeners(selectedPrinter.headProperty().get());
        }
        selectedPrinter = printer;
        if (printer != null && printer.headProperty().get() != null)
        {
            Head head = printer.headProperty().get();
            updateFieldsFromAttachedHead(head);
            updateHeadUniqueId();
            listenForHeadChanges(head);
        }
    }

    private void updateHeadUniqueId()
    {
        if (headUniqueID.getText().length() == 0)
        {
            headUniqueID.setDisable(false);
        } else
        {
            headUniqueID.setDisable(true);
        }
    }

    @Override
    public void whenPrinterAdded(Printer printer)
    {
        headEEPROMOffsets.disableProperty().bind(
            Lookup.getUserPreferences().advancedModeProperty().not());
    }

    @Override
    public void whenPrinterRemoved(Printer printer)
    {
        headEEPROMOffsets.disableProperty().unbind();
    }

    @Override
    public void whenHeadAdded(Printer printer)
    {
        if (printer == selectedPrinter)
        {
            Head head = printer.headProperty().get();
            updateFieldsFromAttachedHead(head);
            listenForHeadChanges(head);
            updateHeadUniqueId();
        }
    }

    @Override
    public void whenHeadRemoved(Printer printer, Head head)
    {
        if (printer == selectedPrinter)
        {
            updateFieldsForNoHead();
            updateHeadUniqueId();
            removeHeadChangeListeners(head);
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

    @Override
    public void whenExtruderAdded(Printer printer, int extruderIndex)
    {
    }

    @Override
    public void whenExtruderRemoved(Printer printer, int extruderIndex)
    {
    }

    private ChangeListener<Object> headChangeListener;

    private void listenForHeadChanges(Head head)
    {
        headChangeListener = (ObservableValue<? extends Object> observable, Object oldValue, Object newValue) ->
        {
            updateFieldsFromAttachedHead(head);
        };
        head.getNozzles().get(0).xOffsetProperty().addListener(headChangeListener);
        head.getNozzles().get(0).yOffsetProperty().addListener(headChangeListener);
        head.getNozzles().get(0).zOffsetProperty().addListener(headChangeListener);
        head.getNozzles().get(0).bOffsetProperty().addListener(headChangeListener);
        head.getNozzles().get(1).xOffsetProperty().addListener(headChangeListener);
        head.getNozzles().get(1).yOffsetProperty().addListener(headChangeListener);
        head.getNozzles().get(1).zOffsetProperty().addListener(headChangeListener);
        head.getNozzles().get(1).bOffsetProperty().addListener(headChangeListener);
        //TODO modify for multiple heaters
        head.getNozzleHeaters().get(0).lastFilamentTemperatureProperty().addListener(
            headChangeListener);
    }

    private void removeHeadChangeListeners(Head head)
    {
        if (headChangeListener != null)
        {
            head.getNozzles().get(0).xOffsetProperty().removeListener(headChangeListener);
            head.getNozzles().get(0).yOffsetProperty().removeListener(headChangeListener);
            head.getNozzles().get(0).zOffsetProperty().removeListener(headChangeListener);
            head.getNozzles().get(0).bOffsetProperty().removeListener(headChangeListener);
            head.getNozzles().get(1).xOffsetProperty().removeListener(headChangeListener);
            head.getNozzles().get(1).yOffsetProperty().removeListener(headChangeListener);
            head.getNozzles().get(1).zOffsetProperty().removeListener(headChangeListener);
            head.getNozzles().get(1).bOffsetProperty().removeListener(headChangeListener);
        }
    }

}
