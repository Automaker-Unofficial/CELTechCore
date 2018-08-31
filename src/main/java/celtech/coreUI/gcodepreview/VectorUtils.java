package celtech.coreUI.gcodepreview;

import static java.lang.Math.abs;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

/**
 *
 * @author George Salter
 */
public class VectorUtils {
    static final double EPSILON = 5.0E-5;
    
    public static Vector3D subtractVectors(Vector3D v2, Vector3D v1) {
        return new Vector3D(v2.getX() - v1.getX(),
                            v2.getY() - v1.getY(),
                            v2.getZ() - v1.getZ());
    }
        
    public static Vector3D calculateCenterBetweenVectors(Vector3D from, Vector3D to) {
        return new Vector3D(0.5 * (from.getX() + to.getX()),
                            0.5 * (from.getY() + to.getY()),
                            0.5 * (from.getZ() + to.getZ()));
    }
    
    public static double calculateLengthBetweenVectors(Vector3D v1, Vector3D v2) {
        Vector3D positionDiff = subtractVectors(v2, v1);
        return positionDiff.getNorm();
    }
    
    public static double calculateRotationAroundYOfVectors(Vector3D from, Vector3D to) {
        Vector3D positionDiff = subtractVectors(to, from);
        if(abs(positionDiff.getX()) < EPSILON && abs(positionDiff.getZ()) < EPSILON) {
            return 0.0;
        }
        
       double angle = Vector3D.angle(new Vector3D(0, 0, 1), positionDiff);
        angle = angle - (double) Math.toRadians(90);
        if(from.getX() > to.getX()) {
            angle = -angle;
        }
        return angle;
    }
    
    public static double calculateRotationAroundZOfVectors(Vector3D from, Vector3D to) {
        Vector3D positionDiff = subtractVectors(to, from);
        if(abs(positionDiff.getY()) < EPSILON ) {
            return 0;
        }
        double angle = Vector3D.angle(new Vector3D(0, 1, 0), positionDiff);
        angle = angle - (double) Math.toRadians(90);
        if(from.getY() > to.getY()) {
            angle = -angle;
        }
        return angle;
    }
}
