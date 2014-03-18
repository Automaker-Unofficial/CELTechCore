/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.coreUI.visualisation;

import com.leapmotion.leap.CircleGesture;
import com.leapmotion.leap.Controller;
import com.leapmotion.leap.Finger;
import com.leapmotion.leap.FingerList;
import com.leapmotion.leap.Frame;
import com.leapmotion.leap.Gesture;
import com.leapmotion.leap.Gesture.State;
import com.leapmotion.leap.GestureList;
import com.leapmotion.leap.Hand;
import com.leapmotion.leap.KeyTapGesture;
import com.leapmotion.leap.Listener;
import com.leapmotion.leap.ScreenTapGesture;
import com.leapmotion.leap.SwipeGesture;
import com.leapmotion.leap.Vector;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author Ian Hudson @ Liberty Systems Limited
 */
public class LeapMotionListener extends Listener
{

    private final Stenographer steno = StenographerFactory.getStenographer(LeapMotionListener.class.getName());
    private ThreeDViewManager viewmanager;
    private double lastRoll = 0;
    private double lastYaw = 0;

    public LeapMotionListener(ThreeDViewManager viewmanager)
    {
        this.viewmanager = viewmanager;
    }

    @Override
    public void onInit(Controller controller)
    {
        steno.info("Initialized");
    }

    @Override
    public void onConnect(Controller controller)
    {
        steno.info("Connected");
        controller.enableGesture(Gesture.Type.TYPE_SWIPE);
        controller.enableGesture(Gesture.Type.TYPE_CIRCLE);
        controller.enableGesture(Gesture.Type.TYPE_SCREEN_TAP);
        controller.enableGesture(Gesture.Type.TYPE_KEY_TAP);
    }

    @Override
    public void onDisconnect(Controller controller)
    {
        //Note: not dispatched when running in a debugger.
        steno.info("Disconnected");
    }

    @Override
    public void onExit(Controller controller)
    {
        steno.info("Exited");
    }

    @Override
    public void onFrame(Controller controller)
    {
        // Get the most recent frame and report some basic information
        Frame frame = controller.frame();
//        steno.info("Frame id: " + frame.id()
//                + ", timestamp: " + frame.timestamp()
//                + ", hands: " + frame.hands().count()
//                + ", fingers: " + frame.fingers().count()
//                + ", tools: " + frame.tools().count()
//                + ", gestures " + frame.gestures().count());

        if (!frame.hands().isEmpty())
        {
            // Get the first hand
            Hand hand = frame.hands().get(0);

            // Check if the hand has any fingers
            FingerList fingers = hand.fingers();
            if (!fingers.isEmpty())
            {
                // Calculate the hand's average finger tip position
                Vector avgPos = Vector.zero();
                for (Finger finger : fingers)
                {
                    avgPos = avgPos.plus(finger.tipPosition());
                }
                avgPos = avgPos.divide(fingers.count());
                steno.info("Hand has " + fingers.count()
                        + " fingers, average finger tip position: " + avgPos);
            }

            // Get the hand's sphere radius and palm position
            steno.info("Hand sphere radius: " + hand.sphereRadius()
                    + " mm, palm position: " + hand.palmPosition());

            // Get the hand's normal vector and direction
            Vector normal = hand.palmNormal();
            Vector direction = hand.direction();

            // Calculate the hand's pitch, roll, and yaw angles
            double roll = Math.toDegrees(normal.roll());
            double yaw = Math.toDegrees(direction.yaw());
            steno.info("Hand pitch: " + Math.toDegrees(direction.pitch()) + " degrees, "
                    + "roll: " + Math.toDegrees(normal.roll()) + " degrees, "
                    + "yaw: " + Math.toDegrees(direction.yaw()) + " degrees");
            if (roll - lastRoll > 1 || yaw - lastYaw > 1)
            {
                viewmanager.rotateCameraAroundAxesTo(roll, yaw);
                lastRoll = roll;
                lastYaw = yaw;
            }
        }

        GestureList gestures = frame.gestures();
        for (int i = 0; i < gestures.count(); i++)
        {
            Gesture gesture = gestures.get(i);

            switch (gesture.type())
            {
                case TYPE_CIRCLE:
                    CircleGesture circle = new CircleGesture(gesture);

                    // Calculate clock direction using the angle between circle normal and pointable
                    String clockwiseness;
                    if (circle.pointable().direction().angleTo(circle.normal()) <= Math.PI / 4)
                    {
                        // Clockwise if angle is less than 90 degrees
                        clockwiseness = "clockwise";
                    } else
                    {
                        clockwiseness = "counterclockwise";
                    }

                    // Calculate angle swept since last frame
                    double sweptAngle = 0;
                    if (circle.state() != State.STATE_START)
                    {
                        CircleGesture previousUpdate = new CircleGesture(controller.frame(1).gesture(circle.id()));
                        sweptAngle = (circle.progress() - previousUpdate.progress()) * 2 * Math.PI;
                    }

                    steno.info("Circle id: " + circle.id()
                            + ", " + circle.state()
                            + ", progress: " + circle.progress()
                            + ", radius: " + circle.radius()
                            + ", angle: " + Math.toDegrees(sweptAngle)
                            + ", " + clockwiseness);
                    break;
                case TYPE_SWIPE:
                    SwipeGesture swipe = new SwipeGesture(gesture);
                    steno.info("Swipe id: " + swipe.id()
                            + ", " + swipe.state()
                            + ", position: " + swipe.position()
                            + ", direction: " + swipe.direction()
                            + ", speed: " + swipe.speed());
                    break;
                case TYPE_SCREEN_TAP:
                    ScreenTapGesture screenTap = new ScreenTapGesture(gesture);
                    steno.info("Screen Tap id: " + screenTap.id()
                            + ", " + screenTap.state()
                            + ", position: " + screenTap.position()
                            + ", direction: " + screenTap.direction());
                    break;
                case TYPE_KEY_TAP:
                    KeyTapGesture keyTap = new KeyTapGesture(gesture);
                    steno.info("Key Tap id: " + keyTap.id()
                            + ", " + keyTap.state()
                            + ", position: " + keyTap.position()
                            + ", direction: " + keyTap.direction());
                    break;
                default:
                    steno.info("Unknown gesture type.");
                    break;
            }
        }

        if (!frame.hands().isEmpty() || !gestures.isEmpty())
        {
            steno.info("unknown");
        }
    }
}
