package celtech.coreUI.components.Notifications;

import celtech.Lookup;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;

/**
 *
 * @author Ian
 */
public class ConditionalNotificationBar extends AppearingNotificationBar
{
    private ObservableValue<Boolean> appearanceCondition;

    private final ChangeListener<Boolean> conditionChangeListener = new ChangeListener<Boolean>()
    {
        @Override
        public void changed(
                ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue)
        {
            System.out.println("A " + notificationDescription.getText());
            calculateVisibility();
        }
    };

    public ConditionalNotificationBar(String message, NotificationDisplay.NotificationType notificationType)
    {
        notificationDescription.setText(Lookup.i18n(message));
        setType(notificationType);
    }

    public void clearAppearanceCondition()
    {
        if (appearanceCondition != null)
        {
            appearanceCondition.removeListener(conditionChangeListener);
        }
        appearanceCondition = null;
        startSlidingOutOfView();
    }

    public ObservableValue<Boolean> getAppearanceCondition()
    {
        return appearanceCondition;
    }

    public void setAppearanceCondition(BooleanBinding appearanceCondition)
    {
        if (this.appearanceCondition != null)
        {
            this.appearanceCondition.removeListener(conditionChangeListener);
        }
        this.appearanceCondition = appearanceCondition;
        this.appearanceCondition.addListener(conditionChangeListener);
        calculateVisibility();
    }

    private void calculateVisibility()
    {
        if (appearanceCondition.getValue())
        {
            System.out.println("B " + notificationDescription.getText());
            show();
        } else
        {
            System.out.println("C " + notificationDescription.getText());
            startSlidingOutOfView();
        }
    }

    @Override
    public void show()
    {
        Lookup.getNotificationDisplay().addStepCountedNotificationBar(this);
        startSlidingInToView();
    }

    @Override
    public void finishedSlidingIntoView()
    {
    }

    @Override
    public void finishedSlidingOutOfView()
    {
        Lookup.getNotificationDisplay().removeStepCountedNotificationBar(this);
    }
}
