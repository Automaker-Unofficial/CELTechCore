/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.utils.threed.exporters;

import celtech.appManager.Project;
import celtech.configuration.ApplicationConfiguration;
import celtech.modelcontrol.ModelContainer;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import javafx.collections.ObservableFloatArray;
import javafx.geometry.Point3D;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.ObservableFaceArray;
import javafx.scene.shape.TriangleMesh;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author ianhudson
 */
public class STLOutputConverter
{

    private File fFile;
    private Stenographer steno = null;
    private Project project = null;
    private String printJobUUID = null;
    private String tempModelFilenameWithPath = null;

    /**
     *
     * @param project
     * @param printJobUUID
     */
    public STLOutputConverter(Project project, String printJobUUID)
    {
        steno = StenographerFactory.getStenographer(this.getClass().getName());

        this.project = project;
        this.printJobUUID = printJobUUID;
        tempModelFilenameWithPath = ApplicationConfiguration.getPrintSpoolDirectory() + printJobUUID
            + File.separator + printJobUUID + ApplicationConfiguration.stlTempFileExtension;

        fFile = new File(tempModelFilenameWithPath);
    }

    /**
     *
     */
    public void outputSTLFile()
    {
        final short blankSpace = (short) 0;

        try
        {
            final DataOutputStream dataOutput = new DataOutputStream(new FileOutputStream(fFile));

            try
            {
                int totalNumberOfFacets = 0;
                ByteBuffer headerByteBuffer = null;

                for (ModelContainer modelContainer : project.getLoadedModels())
                {
                    MeshView meshview = modelContainer.getMeshView();
                    TriangleMesh triangles = (TriangleMesh) meshview.getMesh();
                    ObservableFaceArray faceArray = triangles.getFaces();
                    int numberOfFacets = faceArray.size() / 6;
                    totalNumberOfFacets += numberOfFacets;
                }

                //File consists of:
                // 80 byte ascii header
                // Int containing number of facets
                ByteBuffer headerBuffer = ByteBuffer.allocate(80);
                headerBuffer.put(("Generated by " + ApplicationConfiguration.getTitleAndVersion()).
                    getBytes("UTF-8"));

                dataOutput.write(headerBuffer.array());

                byte outputByte = (byte) (totalNumberOfFacets & 0xff);
                dataOutput.write(outputByte);

                outputByte = (byte) ((totalNumberOfFacets >>> 8) & 0xff);
                dataOutput.write(outputByte);

                outputByte = (byte) ((totalNumberOfFacets >>> 16) & 0xff);
                dataOutput.write(outputByte);

                outputByte = (byte) ((totalNumberOfFacets >>> 24) & 0xff);
                dataOutput.write(outputByte);

                ByteBuffer dataBuffer = ByteBuffer.allocate(50);
                //Binary STL files are always assumed to be little endian
                dataBuffer.order(ByteOrder.LITTLE_ENDIAN);

                // Then for each facet:
                //  3 floats for facet normals
                //  3 x 3 floats for vertices (x,y,z * 3)
                //  2 byte spacer
                for (ModelContainer modelContainer : project.getLoadedModels())
                {
                    MeshView meshview = modelContainer.getMeshView();
                    TriangleMesh triangles = (TriangleMesh) meshview.getMesh();
                    ObservableFaceArray faceArray = triangles.getFaces();
                    ObservableFloatArray pointArray = triangles.getPoints();
                    int numberOfFacets = faceArray.size() / 6;
                    
                    for (int facetNumber = 0; facetNumber < numberOfFacets; facetNumber++)
                    {
                        dataBuffer.rewind();
                        // Output zero normals
                        dataBuffer.putFloat(0);
                        dataBuffer.putFloat(0);
                        dataBuffer.putFloat(0);

                        for (int vertexNumber = 0; vertexNumber < 3; vertexNumber++)
                        {
                            int vertexIndex = faceArray.get((facetNumber * 6) + (vertexNumber * 2));

                            Point3D vertex = modelContainer.
                                transformMeshToRealWorldCoordinates(
                                    pointArray.get(vertexIndex * 3),
                                    pointArray.get((vertexIndex * 3) + 1),
                                    pointArray.get((vertexIndex * 3) + 2));

                            dataBuffer.putFloat((float) vertex.getX());
                            dataBuffer.putFloat((float) vertex.getZ());
                            dataBuffer.putFloat(-(float) vertex.getY());

                        }
                        dataBuffer.putShort(blankSpace);

                        dataOutput.write(dataBuffer.array());
                    }

                }
            } catch (IOException ex)
            {
                steno.error("Error writing to file " + fFile + " :" + ex.toString());

            } finally
            {
                try
                {
                    if (dataOutput != null)
                    {
                        dataOutput.flush();
                        dataOutput.close();
                    }
                } catch (IOException ex)
                {
                    steno.error("Error closing file " + fFile + " :" + ex.toString());
                }
            }
        } catch (FileNotFoundException ex)
        {
            steno.error("Error opening STL output file " + fFile + " :" + ex.toString());
        }
    }
}
