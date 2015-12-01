package celtech.gcodetranslator.postprocessing;

import celtech.appManager.Project;
import celtech.configuration.fileRepresentation.SlicerParametersFile;
import celtech.gcodetranslator.DidntFindEventException;
import celtech.gcodetranslator.postprocessing.nodes.ExtrusionNode;
import celtech.gcodetranslator.postprocessing.nodes.GCodeEventNode;
import celtech.gcodetranslator.postprocessing.nodes.providers.Movement;
import celtech.gcodetranslator.postprocessing.nodes.providers.MovementProvider;
import celtech.utils.Math.MathUtils;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import org.apache.commons.math3.geometry.euclidean.twod.Line;
import org.apache.commons.math3.geometry.euclidean.twod.Segment;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;

/**
 *
 * @author Ian
 */
public class CloseUtilities
{

    private final Project project;

    private final float maxDistanceFromEndPoint;
    private final int maxNumberOfIntersectionsToConsider;

    public CloseUtilities(Project project, SlicerParametersFile settings, String headType)
    {
        this.project = project;
        maxNumberOfIntersectionsToConsider = settings.getNumberOfPerimeters();
        maxDistanceFromEndPoint = settings.getPerimeterExtrusionWidth_mm()
                * 1.01f * maxNumberOfIntersectionsToConsider;
    }

    protected Optional<IntersectionResult> findClosestMovementNode(
            Segment finalSegment,
            List<GCodeEventNode> inScopeEvents,
            boolean intersectOrthogonally
    )
    {
        GCodeEventNode closestNode = null;
        Vector2D intersectionPoint = null;
        Optional<IntersectionResult> result = Optional.empty();

        if (finalSegment != null)
        {
            // We can work out how to split this extrusion
            //Get an orthogonal to the extrusion we're considering
            Vector2D startPoint = finalSegment.getStart();
            Vector2D endPoint = finalSegment.getEnd();
            // We want the orthogonal line to be closer to the specified end point rather than the prior point
            Vector2D vectorFromPriorToThis = endPoint.subtract(startPoint);
            Vector2D halfwayBetweenPriorAndThisPoint = startPoint.add(vectorFromPriorToThis.scalarMultiply(0.5));

            Segment segmentToIntersectWith = null;
            Vector2D segmentToIntersectWithMeasurementPoint = null;

            if (intersectOrthogonally)
            {
                segmentToIntersectWith = MathUtils.getOrthogonalLineToLinePoints(maxDistanceFromEndPoint, halfwayBetweenPriorAndThisPoint, endPoint);

                segmentToIntersectWithMeasurementPoint = MathUtils.findMidPoint(segmentToIntersectWith.getStart(),
                        segmentToIntersectWith.getEnd());
            } else
            {
                Vector2D normalisedVectorToEndOfExtrusion = endPoint.subtract(startPoint).normalize();
                Vector2D scaledVectorToEndOfExtrusion = normalisedVectorToEndOfExtrusion.scalarMultiply(maxDistanceFromEndPoint);

                Vector2D segmentEndPoint = endPoint.add(scaledVectorToEndOfExtrusion);

                Line intersectionLine = new Line(endPoint, segmentEndPoint, 1e-12);
                segmentToIntersectWith = new Segment(endPoint, segmentEndPoint, intersectionLine);

                segmentToIntersectWithMeasurementPoint = endPoint;
            }

            GCodeEventNode lastNodeConsidered = null;

            double closestDistanceSoFar = 999;

            Iterator<GCodeEventNode> inScopeEventIterator = inScopeEvents.iterator();

            while (inScopeEventIterator.hasNext())
            {
                GCodeEventNode inScopeEvent = inScopeEventIterator.next();

                if (inScopeEvent instanceof MovementProvider)
                {
                    MovementProvider movementProvider = (MovementProvider) inScopeEvent;
                    Vector2D extrusionPoint = movementProvider.getMovement().toVector2D();

                    if (lastNodeConsidered != null)
                    {
                        Vector2D lastPoint = ((MovementProvider) lastNodeConsidered).getMovement().toVector2D();
                        Segment segmentUnderConsideration = new Segment(lastPoint,
                                extrusionPoint,
                                new Line(lastPoint, extrusionPoint, 1e-12));

                        Vector2D tempIntersectionPoint = MathUtils.getSegmentIntersection(
                                segmentToIntersectWith, segmentUnderConsideration);

                        if (tempIntersectionPoint != null)
//                                && inScopeEvent != startNode
//                                && inScopeEvent != endNode)
                        {
                            double distanceFromMidPoint = tempIntersectionPoint.distance(
                                    segmentToIntersectWithMeasurementPoint);

                            if (distanceFromMidPoint < closestDistanceSoFar)
                            {
                                //Which node was closest - the last one or this one?
                                if (tempIntersectionPoint.distance(lastPoint)
                                        < tempIntersectionPoint.distance(extrusionPoint))
                                {
                                    closestNode = lastNodeConsidered;
                                } else
                                {
                                    closestNode = inScopeEvent;
                                }
                                closestDistanceSoFar = distanceFromMidPoint;
                                intersectionPoint = tempIntersectionPoint;
                            }
                        }
                    }

                    lastNodeConsidered = inScopeEvent;
                }
            }
        }

        if (closestNode != null
                && intersectionPoint != null)
        {
            result = Optional.of(new IntersectionResult(closestNode, intersectionPoint, 0));
        }

        return result;
    }
}
