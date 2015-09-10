package celtech.coreUI.components.Notifications;

import java.util.stream.Collectors;
import javafx.scene.Node;
import javafx.scene.layout.VBox;

/**
 *
 * @author Ian
 */
public class NotificationDisplay extends VBox
{

    private final VBox stepCountedNotifications = new VBox();
    private final VBox noteNotifications = new VBox();
    private final VBox cautionNotifications = new VBox();
    private final VBox warningNotifications = new VBox();

    public enum NotificationType
    {

        NOTE(0),
        WARNING(1),
        CAUTION(2);

        private final int value;

        private NotificationType(int value)
        {
            this.value = value;
        }

        public int getValue()
        {
            return value;
        }
    }

    public NotificationDisplay()
    {
        setFillWidth(true);
        stepCountedNotifications.setFillWidth(true);

        stepCountedNotifications.getChildren().add(warningNotifications);
        stepCountedNotifications.getChildren().add(cautionNotifications);
        stepCountedNotifications.getChildren().add(noteNotifications);

        getChildren().add(stepCountedNotifications);

//        Lookup.getSelectedPrinterProperty().addListener((ObservableValue<? extends Printer> ov, Printer oldSelection, Printer newSelection) ->
//        {
//            unbindFromPrinter();
//            if (newSelection != null)
//            {
//                bindToPrinter(newSelection);
//            }
//        });
//        notificationBars.addListener(new ListChangeListener<AppearingNotificationBar>()
//        {
//            @Override
//            public void onChanged(ListChangeListener.Change<? extends AppearingNotificationBar> change)
//            {
//                while(change.next())
//                {
//                    for (AppearingNotificationBar notificationBar : change.getRemoved())
//                    {
//                        notificationBar.startSlidingOutOfView();
//                    }
//                    
//                    for (AppearingNotificationBar notificationBar : change.getAddedSubList())
//                    {
//                        notificationBar.startSlidingInToView();
//                    }
//                }
//            }
//        });
    }

//    private void bindToPrinter(Printer printer)
//    {
//        if (this.printerInUse != null)
//        {
//            unbindFromPrinter();
//        }
//        this.printerInUse = printer;
//        stateDisplayBar = new PrintStatusBar(printer);
//        printPreparationDisplayBar = new PrintPreparationStatusBar(printer);
//        bedTemperatureDisplayBar = new BedHeaterStatusBar(printer.getPrinterAncillarySystems());
//        
//        printer.headProperty().addListener(headListener);
//        if (printer.headProperty().get() != null)
//        {
//            createNozzleHeaterBars(printer.headProperty().get());
//        }
//        
//        getChildren().addAll(printPreparationDisplayBar, bedTemperatureDisplayBar, stateDisplayBar);
//    }
//    
//    private void unbindFromPrinter()
//    {
//        if (printerInUse != null)
//        {
//            printerInUse.headProperty().removeListener(headListener);
//            
//            stateDisplayBar.unbindAll();
//            printPreparationDisplayBar.unbindAll();
//            bedTemperatureDisplayBar.unbindAll();
//            
//            if (nozzle1TemperatureDisplayBar != null)
//            {
//                nozzle1TemperatureDisplayBar.unbindAll();
//                nozzle1TemperatureDisplayBar = null;
//            }
//            
//            if (nozzle2TemperatureDisplayBar != null)
//            {
//                nozzle2TemperatureDisplayBar.unbindAll();
//                nozzle2TemperatureDisplayBar = null;
//            }
//            getChildren().clear();
//        }
//        printerInUse = null;
//    }
    public void displayTimedNotification(String title, String message, NotificationType notificationType)
    {
        TimedNotificationBar notificationBar = new TimedNotificationBar();
        notificationBar.setTitle(title);
        notificationBar.setMessage(message);
        notificationBar.setType(notificationType);
        notificationBar.show();
    }

    public void addNotificationBar(AppearingNotificationBar notificationBar)
    {
        if (!getChildren().contains(notificationBar))
        {
            getChildren().add(notificationBar);
        }
    }

    public void removeNotificationBar(AppearingNotificationBar notificationBar)
    {
        getChildren().remove(notificationBar);
    }

    public void addStepCountedNotificationBar(AppearingNotificationBar notificationBar)
    {
        switch (notificationBar.getType())
        {
            case NOTE:
                if (!noteNotifications.getChildren().contains(notificationBar))
                {
                    noteNotifications.getChildren().add(notificationBar);
                }
                break;
            case CAUTION:
                if (!cautionNotifications.getChildren().contains(notificationBar))
                {
                    cautionNotifications.getChildren().add(notificationBar);
                }
                break;
            case WARNING:
                if (!warningNotifications.getChildren().contains(notificationBar))
                {
                    warningNotifications.getChildren().add(notificationBar);
                }
                break;
            default:
                break;
        }

        updateNotificationNumbers();
    }

    public void removeStepCountedNotificationBar(AppearingNotificationBar notificationBar)
    {
        switch (notificationBar.getType())
        {
            case NOTE:
                noteNotifications.getChildren().remove(notificationBar);
                break;
            case CAUTION:
                cautionNotifications.getChildren().remove(notificationBar);
                break;
            case WARNING:
                warningNotifications.getChildren().remove(notificationBar);
                break;
            default:
                break;
        }

        updateNotificationNumbers();
    }

    private void updateNotificationNumbers()
    {
        int currentStep = 1;
        int totalNotifications = noteNotifications.getChildren().size()
                + cautionNotifications.getChildren().size()
                + warningNotifications.getChildren().size();

        for (Node bar : warningNotifications.getChildren())
        {
            AppearingNotificationBar notificationBar = (AppearingNotificationBar) bar;
            notificationBar.setXOfY(currentStep++, totalNotifications);
        }

        for (Node bar : cautionNotifications.getChildren())
        {
            AppearingNotificationBar notificationBar = (AppearingNotificationBar) bar;
            notificationBar.setXOfY(currentStep++, totalNotifications);
        }

        for (Node bar : noteNotifications.getChildren())
        {
            AppearingNotificationBar notificationBar = (AppearingNotificationBar) bar;
            notificationBar.setXOfY(currentStep++, totalNotifications);
        }
    }
}