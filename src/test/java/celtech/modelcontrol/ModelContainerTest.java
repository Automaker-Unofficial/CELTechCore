/*
 * Copyright 2015 CEL UK
 */
package celtech.modelcontrol;

import celtech.JavaFXConfiguredTest;
import celtech.coreUI.visualisation.metaparts.ModelLoadResult;
import celtech.services.modelLoader.ModelLoadResults;
import celtech.services.modelLoader.ModelLoaderTask;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import javafx.geometry.Point3D;
import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.optim.MaxEval;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.apache.commons.math3.optim.univariate.BrentOptimizer;
import org.apache.commons.math3.optim.univariate.SearchInterval;
import org.apache.commons.math3.optim.univariate.SimpleUnivariateValueChecker;
import org.apache.commons.math3.optim.univariate.UnivariateObjectiveFunction;
import org.apache.commons.math3.optim.univariate.UnivariatePointValuePair;
import static org.junit.Assert.fail;
import org.junit.Test;

/**
 *
 * @author tony
 */
public class ModelContainerTest extends JavaFXConfiguredTest
{

//    @Test
    public void testRotationApplication()
    {

        Vector3D xAxis = new Vector3D(1, 0, 0);
        Rotation Rx180 = new Rotation(xAxis, Math.PI / 2d);
        Vector3D yAxis = new Vector3D(0, 1, 0);
        Rotation Ry180 = new Rotation(yAxis, Math.PI / 2d);
        printRotation(Ry180);

        Rotation Rx180y180 = Rx180.applyTo(Ry180);
        printRotation(Rx180y180);

        Rotation Rx180Inverse = new Rotation(xAxis, -Math.PI / 2d);
        printRotation(Rx180Inverse.applyTo(Rx180y180));

    }
    
    private ModelContainer loadSTL(String stlLocation) throws InterruptedException, ExecutionException {
        List<File> modelFiles = new ArrayList<>();
        URL statisticsFile = this.getClass().getResource(stlLocation);
        modelFiles.add(new File(statisticsFile.getFile()));
        ModelLoaderTask modelLoaderTask = new ModelLoaderTask(modelFiles, null, true);
        Thread th = new Thread(modelLoaderTask);
        th.setDaemon(true);
        th.start();
        ModelLoadResults modelLoadResults = modelLoaderTask.get();
        ModelLoadResult modelLoadResult = modelLoadResults.getResults().get(0);
        ModelContainer modelContainer = modelLoadResult.getModelContainer();
        return modelContainer;
    }
    
    
    private void printRotation(Rotation R)
    {
        System.out.println("Axis " + R.getAxis() + " Angle " + Math.toDegrees(R.getAngle()));
    }

}
