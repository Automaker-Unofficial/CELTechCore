/*
 * Copyright 2014 CEL UK
 */
package celtech.utils;

import celtech.printerControl.model.Printer;
import java.util.ArrayList;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 *
 * @author tony
 */
public class PrinterListChangesNotifierTest
{

    @Test
    public void testWhenPrinterAdded()
    {
        ObservableList<Printer> printers = FXCollections.observableArrayList();
        PrinterListChangesNotifier notifier = new PrinterListChangesNotifier(printers);
        TestPrinterListChangesListener plcListener = new TestPrinterListChangesListener();
        notifier.addListener(plcListener);
        
        assertEquals(0, plcListener.addedPrinters.size());
        TestPrinter printer = new TestPrinter();
        printers.add(printer);
        assertEquals(1, plcListener.addedPrinters.size());
    }
    
    @Test
    public void testWhenPrinterAddedAndRemoved()
    {
        ObservableList<Printer> printers = FXCollections.observableArrayList();
        PrinterListChangesNotifier notifier = new PrinterListChangesNotifier(printers);
        TestPrinterListChangesListener plcListener = new TestPrinterListChangesListener();
        notifier.addListener(plcListener);
        
        TestPrinter printer = new TestPrinter();
        printers.add(printer);
        printers.remove(printer);
        assertEquals(0, plcListener.addedPrinters.size());
    }   
    
    @Test
    public void testWhenPrinterAddedThenHeadAdded()
    {
        ObservableList<Printer> printers = FXCollections.observableArrayList();
        PrinterListChangesNotifier notifier = new PrinterListChangesNotifier(printers);
        TestPrinterListChangesListener plcListener = new TestPrinterListChangesListener();
        notifier.addListener(plcListener);
        
        assertEquals(0, plcListener.printersWithHeadAdded.size());
        TestPrinter printer = new TestPrinter();
        printers.add(printer);
        printer.addHead();
        assertEquals(1, plcListener.printersWithHeadAdded.size());
    }   
    
    @Test
    public void testWhenPrinterAddedThenHeadRemoved()
    {
        ObservableList<Printer> printers = FXCollections.observableArrayList();
        PrinterListChangesNotifier notifier = new PrinterListChangesNotifier(printers);
        TestPrinterListChangesListener plcListener = new TestPrinterListChangesListener();
        notifier.addListener(plcListener);
        
        assertEquals(0, plcListener.printersWithHeadRemoved.size());
        TestPrinter printer = new TestPrinter();
        printers.add(printer);
        printer.addHead();
        printer.removeHead();
        assertEquals(1, plcListener.printersWithHeadRemoved.size());
    }    
    
    @Test
    public void testWhenPrinterAddedThenHeadRemovedWithThreePrinters()
    {
        ObservableList<Printer> printers = FXCollections.observableArrayList();
        PrinterListChangesNotifier notifier = new PrinterListChangesNotifier(printers);
        TestPrinterListChangesListener plcListener = new TestPrinterListChangesListener();
        notifier.addListener(plcListener);
        
        assertEquals(0, plcListener.printersWithHeadRemoved.size());
        TestPrinter printer1 = new TestPrinter();
        TestPrinter printer2 = new TestPrinter();
        TestPrinter printer3 = new TestPrinter();
        printers.add(printer1);
        printers.add(printer2);
        printers.add(printer3);
        printer1.addHead();
        printer2.addHead();
        printer2.removeHead();
        assertEquals(1, plcListener.printersWithHeadRemoved.size());
        assertEquals(printer2, plcListener.printersWithHeadRemoved.get(0));
        
        assertEquals(2, plcListener.printersWithHeadAdded.size());
        assertEquals(printer1, plcListener.printersWithHeadAdded.get(0));
        assertEquals(printer2, plcListener.printersWithHeadAdded.get(1));
    }      
    
    @Test
    public void testWhenPrinterAddedThenReelAdded()
    {
        ObservableList<Printer> printers = FXCollections.observableArrayList();
        PrinterListChangesNotifier notifier = new PrinterListChangesNotifier(printers);
        TestPrinterListChangesListener plcListener = new TestPrinterListChangesListener();
        notifier.addListener(plcListener);
        
        assertEquals(0, plcListener.printersWithHeadAdded.size());
        TestPrinter printer = new TestPrinter();
        printers.add(printer);
        printer.addReel(0);
        assertEquals(1, plcListener.printersWithReelAdded.size());
    }       

    private static class TestPrinterListChangesListener implements PrinterListChangesListener
    {
        
        public List<Printer> addedPrinters = new ArrayList<>();
        public List<Printer> printersWithHeadAdded = new ArrayList<>();
        public List<Printer> printersWithHeadRemoved = new ArrayList<>();
        public List<Printer> printersWithReelAdded = new ArrayList<>();
        public List<Printer> printersWithReelRemoved = new ArrayList<>();        

        @Override
        public void whenPrinterAdded(Printer printer)
        {
            addedPrinters.add(printer);
        }

        @Override
        public void whenPrinterRemoved(Printer printer)
        {
            addedPrinters.remove(printer);
        }

        @Override
        public void whenHeadAdded(Printer printer)
        {
            printersWithHeadAdded.add(printer);
        }

        @Override
        public void whenHeadRemoved(Printer printer)
        {
            printersWithHeadRemoved.add(printer);
        }

        @Override
        public void whenReelAdded(Printer printer, int reelIndex)
        {
            printersWithReelAdded.add(printer);
        }

        @Override
        public void whenReelRemoved(Printer printer, int reelIndex)
        {
            printersWithReelRemoved.add(printer);
        }

        @Override
        public void whenPrinterIdentityChanged(Printer printer)
        {

        }
    }


}
