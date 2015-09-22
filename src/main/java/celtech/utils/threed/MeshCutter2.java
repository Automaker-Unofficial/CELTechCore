/*
 * Copyright 2015 CEL UK
 */
package celtech.utils.threed;

import static celtech.utils.threed.MeshSeparator.setTextureAndSmoothing;
import static celtech.utils.threed.MeshUtils.copyMesh;
import static celtech.utils.threed.NonManifoldLoopDetector.identifyNonManifoldLoops;
import static celtech.utils.threed.OpenFaceCloser.closeOpenFace;
import com.sun.javafx.scene.shape.ObservableFaceArrayImpl;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javafx.geometry.Point3D;
import javafx.scene.shape.ObservableFaceArray;
import javafx.scene.shape.TriangleMesh;
import org.apache.commons.math3.util.Pair;


/**
 *
 * @author tony
 */
public class MeshCutter2
{

    public enum TopBottom
    {

        TOP, BOTTOM
    };


    public interface BedToLocalConverter
    {

        Point3D localToBed(Point3D point);

        Point3D bedToLocal(Point3D point);
    }

    static Point3D makePoint3D(TriangleMesh mesh, int vertexIndex)
    {
        float x = mesh.getPoints().get(vertexIndex * 3);
        float y = mesh.getPoints().get(vertexIndex * 3 + 1);
        float z = mesh.getPoints().get(vertexIndex * 3 + 2);
        return new Point3D(x, y, z);
    }

    /**
     * Cut the given mesh into two, at the given height.
     */
    public static List<TriangleMesh> cut(TriangleMesh mesh, float cutHeight,
        BedToLocalConverter bedToLocalConverter)
    {

        System.out.println("cut at " + cutHeight);

        CutResult cutResult = getUncoveredMesh(mesh, cutHeight, bedToLocalConverter,
                                               TopBottom.TOP);

        TriangleMesh topMesh = closeOpenFace(cutResult, cutHeight, bedToLocalConverter);
        MeshUtils.removeUnusedAndDuplicateVertices(topMesh);
        setTextureAndSmoothing(topMesh, topMesh.getFaces().size() / 6);

        Optional<MeshUtils.MeshError> error = MeshUtils.validate(topMesh, bedToLocalConverter);
        if (error.isPresent())
        {
            System.out.println("Error in TOP mesh: " + error.toString());
            throw new RuntimeException("Invalid mesh: " + error.toString());
        }

        cutResult = getUncoveredMesh(mesh, cutHeight, bedToLocalConverter,
                                     TopBottom.BOTTOM);

        TriangleMesh bottomMesh = closeOpenFace(cutResult, cutHeight, bedToLocalConverter);
        MeshUtils.removeUnusedAndDuplicateVertices(bottomMesh);
        setTextureAndSmoothing(bottomMesh, bottomMesh.getFaces().size() / 6);

        error = MeshUtils.validate(bottomMesh, bedToLocalConverter);
        if (error.isPresent())
        {
            System.out.println("Error in BOTTOM mesh: " + error.toString());
            throw new RuntimeException("Invalid mesh: " + error.toString());
        }

        List<TriangleMesh> meshes = new ArrayList<>();
        meshes.add(topMesh);
        meshes.add(bottomMesh);

        return meshes;
    }

    static CutResult getUncoveredMesh(TriangleMesh mesh,
        float cutHeight, BedToLocalConverter bedToLocalConverter,
        TopBottom topBottom)
    {

        TriangleMesh childMesh = makeSplitMesh(mesh,
                                               cutHeight, bedToLocalConverter, topBottom);

        boolean orientable = MeshUtils.testMeshIsOrientable(childMesh);
        if (!orientable)
        {
            throw new RuntimeException("uncovered cut mesh is not orientable!");
        }

        // XXX remove duplicate vertices before trying to identify non-manifold edges ??
        Set<List<ManifoldEdge>> loops = identifyNonManifoldLoops(childMesh, bedToLocalConverter);
        System.out.println(loops.size() + " non manifold loops identified");

        // debugging code to visualise non manifold edges and loops that were found
//        visualiseEdgeLoops(
//            NonManifoldLoopDetector.getNonManifoldEdges(childMesh, bedToLocalConverter), loops);
        
        Set<PolygonIndices> polygonIndices = convertEdgesToVertices(loops);

        polygonIndices = removeSequentialDuplicateVertices(polygonIndices);

        CutResult cutResultUpper = new CutResult(childMesh, polygonIndices,
                                                 bedToLocalConverter, topBottom);
        return cutResultUpper;
    }

    /**
     * Given the mesh, cut faces and intersection points, create the child mesh. Copy the original
     * mesh, remove all the cut faces and replace with a new set of faces using the new intersection
     * points. Remove all the faces from above the cut faces.
     */
    static TriangleMesh makeSplitMesh(TriangleMesh mesh,
        float cutHeight, MeshCutter2.BedToLocalConverter bedToLocalConverter,
        MeshCutter2.TopBottom topBottom)
    {
        TriangleMesh childMesh = copyMesh(mesh);

        Set<Integer> facesToRemove = new HashSet<>();

        for (int i = 0; i < mesh.getFaces().size() / 6; i++)
        {
            TriangleCutter.splitFaceAndAddLowerFacesToMesh(childMesh, facesToRemove, i, cutHeight,
                                                           bedToLocalConverter, topBottom);
        }

        removeFaces(childMesh, facesToRemove);

//        MeshDebug.showFace(mesh, 0);
        return childMesh;
    }

    /**
     * Remove the given faces from the mesh.
     */
    private static void removeFaces(TriangleMesh mesh, Set<Integer> facesToRemove)
    {

        ObservableFaceArray newFaceArray = new ObservableFaceArrayImpl();
        for (int faceIndex = 0; faceIndex < mesh.getFaces().size() / 6; faceIndex++)
        {
            if (facesToRemove.contains(faceIndex))
            {
                continue;
            }
            int[] vertices = new int[6];
            vertices[0] = mesh.getFaces().get(faceIndex * 6);
            vertices[2] = mesh.getFaces().get(faceIndex * 6 + 2);
            vertices[4] = mesh.getFaces().get(faceIndex * 6 + 4);
            newFaceArray.addAll(vertices);
        }
        mesh.getFaces().setAll(newFaceArray);
        setTextureAndSmoothing(mesh, mesh.getFaces().size() / 6);
    }

    static Map<PolygonIndices, List<ManifoldEdge>> debugLoopToEdges = new HashMap<>();

    static Set<PolygonIndices> convertEdgesToVertices(Set<List<ManifoldEdge>> loops)
    {
        Set<PolygonIndices> polygonIndicesSet = new HashSet<>();
        for (List<ManifoldEdge> loop : loops)
        {
            PolygonIndices polygonIndices = convertEdgesToPolygonIndices(loop).getFirst();
            polygonIndicesSet.add(polygonIndices);
            debugLoopToEdges.put(polygonIndices, loop);
        }
        return polygonIndicesSet;
    }

    /**
     * Convert the edges to a list of vertex indices. Each edge may be going forwards or backwards,
     * we can't assume a given direction or any consistency in direction.
     *
     * @param loop
     * @return
     */
    static Pair<PolygonIndices, List<Point3D>> convertEdgesToPolygonIndices(List<ManifoldEdge> loop)
    {
        PolygonIndices polygonIndices = new PolygonIndices();
        List<Point3D> points = new ArrayList<>();

        int previousVertexId = -1;
        int firstVertexId = -1;
        ManifoldEdge edge0 = loop.get(0);
        ManifoldEdge edge1 = loop.get(1);
        if (edge0.v0 == edge1.v0 || edge0.v0 == edge1.v1)
        {
            // second vertex is edge0.v0
            firstVertexId = edge0.v1;
            polygonIndices.add(firstVertexId);
            points.add(edge0.point1);
        } else
        {
            // second vertex is edge0.v1
            firstVertexId = edge0.v0;
            polygonIndices.add(firstVertexId);
            points.add(edge0.point0);
        }

        for (ManifoldEdge edge : loop)
        {
            if (previousVertexId == -1)
            {
                // this is the first edge
                previousVertexId = firstVertexId;
            }
            if (edge.v0 == previousVertexId)
            {
                polygonIndices.add(edge.v1);
                points.add(edge.point1);
                previousVertexId = edge.v1;
            } else
            {
                polygonIndices.add(edge.v0);
                points.add(edge.point0);
                previousVertexId = edge.v0;
            }
        }
        return new Pair<>(polygonIndices, points);
    }
    
//    /**
//     * Get all loops of faces and their matching vertices along the cutting plane. Each loop of
//     * vertices must be ordered according to adjacency, so that we can get a correct perimeter for
//     * forming the cover surface. Any new vertices that are required must be added to the mesh.
//     */
//    static Set<LoopOfVerticesAndCutFaces> getLoopsOfVertices(TriangleMesh mesh, float cutHeight,
//        BedToLocalConverter bedToLocalConverter)
//    {
//        boolean[] faceVisited = new boolean[mesh.getFaces().size() / 6];
//        boolean[] perimeterVertex = new boolean[mesh.getPoints().size()];
//        Map<Integer, Set<Integer>> facesWithVertices = makeFacesWithVertex(mesh);
//
//        Set<LoopOfVerticesAndCutFaces> loopsOfFaces = new HashSet<>();
//
//        while (true)
//        {
//            Optional<LoopOfVerticesAndCutFaces> loopOfFaces
//                = getNextLoopOfVertices(faceVisited, perimeterVertex, mesh, cutHeight,
//                                        facesWithVertices, bedToLocalConverter);
//            if (loopOfFaces.isPresent() && loopOfFaces.get().loopOfVertices.size() > 2)
//            {
//                loopsOfFaces.add(loopOfFaces.get());
//            } else
//            {
//                if (!loopOfFaces.isPresent())
//                {
//                    break;
//                }
//            }
//        }
//
//        return loopsOfFaces;
//    }
//    

    private static Set<PolygonIndices> removeSequentialDuplicateVertices(
        Set<PolygonIndices> polygonIndices)
    {
        Set<PolygonIndices> polygonIndicesClean = new HashSet<>();
        for (PolygonIndices loop : polygonIndices)
        {
            PolygonIndices cleanLoop = new PolygonIndices();
            int previousVertexIndex = -1;
            for (Integer vertexIndex : loop)
            {
                if (vertexIndex != previousVertexIndex)
                {
                    cleanLoop.add(vertexIndex);
                }
                previousVertexIndex = vertexIndex;
            }
            if (cleanLoop.get(0) == cleanLoop.get(cleanLoop.size() - 1))
            {
                cleanLoop.remove(cleanLoop.size() - 1);
            }
            polygonIndicesClean.add(cleanLoop);
        }

        return polygonIndicesClean;
    }

}
