/*
 * Copyright 2014 CEL UK
 */
package celtech.coreUI.components.printerstatus;

import celtech.Lookup;
import celtech.configuration.PauseStatus;
import celtech.configuration.PrinterColourMap;
import celtech.coreUI.StandardColours;
import celtech.printerControl.model.Printer;
import celtech.printerControl.PrinterStatus;
import static celtech.printerControl.comms.commands.ColourStringConverter.colourToString;
import static celtech.utils.StringMetrics.getWidthOfString;
import com.sun.javafx.tk.FontMetrics;
import com.sun.javafx.tk.Toolkit;
import java.io.IOException;
import java.net.URL;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXMLLoader;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

/**
 *
 * @author tony
 */
public class PrinterComponent extends Pane
{

    private boolean selected = false;
    private Size currentSize;
    private double sizePixels = 80;
    private int fontSize;
    private boolean inInterruptibleState;

    public enum Size
    {

        SIZE_SMALL(80, 10),
        SIZE_MEDIUM(120, 20),
        SIZE_LARGE(260, 0);

        private final int size;
        private final int spacing;

        private Size(int size, int spacing)
        {
            this.size = size;
            this.spacing = spacing;
        }

        public int getSize()
        {
            return size;
        }

        public int getSpacing()
        {
            return spacing;
        }
    }

    public enum Status
    {

        NO_INDICATOR(""),
        READY("printerStatus.idle"),
        PRINTING("printerStatus.printing"),
        PAUSED("printerStatus.paused"),
        NOTIFICATION(""),
        ERROR("");

        private final String i18nString;

        private Status(String i18nString)
        {
            this.i18nString = i18nString;
        }

        /**
         *
         * @return
         */
        public String getI18nString()
        {
            String stringToOutput = "";

            if (!i18nString.equals(""))
            {
                stringToOutput = Lookup.i18n(i18nString);
            }
            return stringToOutput;
        }

        /**
         *
         * @return
         */
        @Override
        public String toString()
        {
            return getI18nString();
        }
    }

    @FXML
    private Text name;

    @FXML
    private Pane innerPane;

    @FXML
    private WhiteProgressBarComponent progressBar;

    @FXML
    private PrinterSVGComponent printerSVG;
    private final Printer printer;
    private final ComponentIsolationInterface isolationInterface;

    private ChangeListener<String> nameListener = (ObservableValue<? extends String> observable, String oldValue, String newValue) ->
    {
        setName(newValue);
    };
    private ChangeListener<Color> colorListener = (ObservableValue<? extends Color> observable, Color oldValue, Color newValue) ->
    {
        setColour(newValue);
    };
    private ChangeListener<Number> progressListener = (ObservableValue<? extends Number> observable, Number oldValue, Number newValue) ->
    {
        setProgress((double) newValue);
    };

    public PrinterComponent(Printer printer, ComponentIsolationInterface isolationInterface)
    {
        this.printer = printer;
        this.isolationInterface = isolationInterface;

        URL fxml = getClass().getResource("/celtech/resources/fxml/printerstatus/printer.fxml");
        FXMLLoader fxmlLoader = new FXMLLoader(fxml);
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);

        try
        {
            fxmlLoader.load();
        } catch (IOException exception)
        {
            throw new RuntimeException(exception);
        }

        initialise();
    }

    public void setStatus(Status status)
    {
        printerSVG.setStatus(status);
    }

    public void setName(String newName)
    {
        newName = fitNameToWidth(newName);
        nameTextProperty().set(newName);
    }

    public StringProperty nameTextProperty()
    {
        return name.textProperty();
    }

    /**
     * Initialise the component
     */
    private void initialise()
    {

        setStyle("-fx-background-color: white;");

        name.setFill(Color.WHITE);
        String nameText;

        if (printer != null)
        {
            nameText = printer.getPrinterIdentity().printerFriendlyNameProperty().get();
            setColour(printer.getPrinterIdentity().printerColourProperty().get());
            printer.getPrinterIdentity().printerFriendlyNameProperty().addListener(nameListener);
            printer.getPrinterIdentity().printerColourProperty().addListener(colorListener);
            printer.getPrintEngine().progressProperty().addListener(progressListener);
            printer.printerStatusProperty().addListener(
                    (ObservableValue<? extends PrinterStatus> observable, PrinterStatus oldValue, PrinterStatus newValue) ->
                    {
                        updateStatus(newValue, printer.pauseStatusProperty().get());
                    });

            printer.pauseStatusProperty().addListener(
                    (ObservableValue<? extends PauseStatus> observable, PauseStatus oldValue, PauseStatus newValue) ->
                    {
                        updateStatus(printer.printerStatusProperty().get(), newValue);
                    });
            
            printer.getPrintEngine().highIntensityCommsInProgressProperty().addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) ->
            {
                updateStatus(printer.printerStatusProperty().get(), printer.pauseStatusProperty().get());
            });

            updateStatus(printer.printerStatusProperty().get(), printer.pauseStatusProperty().get());
        } else
        {
            nameText = Lookup.i18n("sidePanel_printerStatus.notConnected");
            String style = "-fx-background-color: #" + colourToString(StandardColours.LIGHT_GREY) + ";";
            innerPane.setStyle(style);
            setStatus(Status.NO_INDICATOR);
        }

        nameText = fitNameToWidth(nameText);
        name.setText(nameText);

        setSize(Size.SIZE_LARGE);

        this.disabledProperty().addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) ->
        {
            if (newValue)
            {
                printerSVG.setOpacity(0.25);
            } else
            {
                printerSVG.setOpacity(1);
            }
        });
    }

    public void setProgress(double progress)
    {
        progressBar.setProgress(progress);
    }

    public void setColour(Color color)
    {
        PrinterColourMap colourMap = PrinterColourMap.getInstance();
        Color displayColour = colourMap.printerToDisplayColour(color);
        String colourHexString = "#" + colourToString(displayColour);
        String style = "-fx-background-color: " + colourHexString + ";";
        innerPane.setStyle(style);
    }

    public void setSelected(boolean select)
    {
        if (selected != select)
        {
            selected = select;
            redraw();
        }
    }

    public void setSize(Size size)
    {
        if (size != currentSize)
        {
            currentSize = size;
            redraw();
        }
    }

    private void updateStatus(PrinterStatus printerStatus, PauseStatus pauseStatus)
    {
        Status status;

        switch (printerStatus)
        {
            case IDLE:
            case RUNNING_MACRO_FILE:
            case REMOVING_HEAD:
            case OPENING_DOOR:
                status = Status.READY;
                inInterruptibleState = true;
                break;
            case PRINTING_PROJECT:
                status = Status.PRINTING;
                inInterruptibleState = true;
                break;
            default:
                status = Status.READY;
                inInterruptibleState = false;
                break;
        }

        if (pauseStatus == PauseStatus.PAUSED
                || pauseStatus == PauseStatus.PAUSE_PENDING)
        {
            status = Status.PAUSED;
        }

        if (printer.getPrintEngine().highIntensityCommsInProgressProperty().get())
        {
            inInterruptibleState = false;
        }

        setStatus(status);
        progressBar.setStatus(status);

        isolationInterface.interruptibilityUpdated(this);
    }

    /**
     * Redraw the component. Reposition child nodes according to selection state
     * and size.
     */
    private void redraw()
    {
        int progressBarWidth;
        int progressBarHeight;
        double progressBarYOffset;
        double nameLayoutY;
        int borderWidth;
        if (selected)
        {
            borderWidth = 3;
        } else
        {
            borderWidth = 0;
        }

        switch (currentSize)
        {
            case SIZE_SMALL:
                fontSize = 9;
                progressBarWidth = 65;
                progressBarHeight = 6;
                progressBarYOffset = 17;

                break;
            case SIZE_MEDIUM:
                fontSize = 14;
                progressBarWidth = 100;
                progressBarHeight = 9;
                progressBarYOffset = 26;
                break;
            default:
                fontSize = 30;
                progressBarWidth = 220;
                progressBarHeight = 20;
                progressBarYOffset = 55;
                break;
        }
        
        progressBar.setSize(currentSize);

        sizePixels = currentSize.getSize();

        setPrefWidth(sizePixels);
        setMinWidth(sizePixels);
        setMaxWidth(sizePixels);
        setMinHeight(sizePixels);
        setMaxHeight(sizePixels);
        setPrefHeight(sizePixels);

        double progressBarX = (sizePixels - progressBarWidth) / 2.0;
        double progressBarY = sizePixels - progressBarYOffset - progressBarHeight;

        innerPane.setMinWidth(sizePixels - borderWidth * 2);
        innerPane.setMaxWidth(sizePixels - borderWidth * 2);
        innerPane.setMinHeight(sizePixels - borderWidth * 2);
        innerPane.setMaxHeight(sizePixels - borderWidth * 2);
        innerPane.setTranslateX(borderWidth);
        innerPane.setTranslateY(borderWidth);
        printerSVG.setSize(sizePixels);
        progressBar.setLayoutX(progressBarX);
        progressBar.setLayoutY(progressBarY);
        progressBar.setControlWidth(progressBarWidth);
        progressBar.setControlHeight(progressBarHeight);

        for (Node child : innerPane.getChildren())
        {
            child.setTranslateX(-borderWidth);
            child.setTranslateY(-borderWidth);
        }

        name.setStyle("-fx-font-size: " + fontSize
                + "px !important; -fx-font-family: 'Source Sans Pro Regular';");
        name.setLayoutX(progressBarX);

        Font font = name.getFont();
        Font actualFont = new Font(font.getName(), fontSize);
        FontMetrics fontMetrics = Toolkit.getToolkit().getFontLoader().getFontMetrics(actualFont);

        nameLayoutY = sizePixels - (progressBarYOffset / 2) + fontMetrics.getDescent();
        name.setLayoutY(nameLayoutY);

        updateBounds();

        setPrefSize(sizePixels, sizePixels);

    }

    @Override
    public double computeMinHeight(double width)
    {
        return sizePixels;
    }

    @Override
    public double computeMinWidth(double height)
    {
        return sizePixels;
    }

    @Override
    public double computeMaxHeight(double width)
    {
        return sizePixels;
    }

    @Override
    public double computeMaxWidth(double height)
    {
        return sizePixels;
    }

    @Override
    public double computePrefHeight(double width)
    {
        return sizePixels;
    }

    @Override
    public double computePrefWidth(double height)
    {
        return sizePixels;
    }

    /**
     * Fit the printer name to the available space.
     */
    public String fitNameToWidth(String name)
    {

        int FONT_SIZE = 14;
        int AVAILABLE_WIDTH = 115;
        double stringWidth = getWidthOfString(name, FONT_SIZE);
        int i = 0;
        while (stringWidth > AVAILABLE_WIDTH)
        {
            name = name.substring(0, name.length() - 1);
            stringWidth = getWidthOfString(name, FONT_SIZE);
            if (i > 100)
            {
                break;
            }
            i++;
        }
        return name;
    }

    public boolean isInterruptible()
    {
        return inInterruptibleState;
    }
}
