package celtech.utils.threed.importers.stl;

import celtech.appManager.Project;
import celtech.configuration.PrintBed;
import celtech.coreUI.visualisation.ApplicationMaterials;
import celtech.coreUI.visualisation.metaparts.Face;
import celtech.coreUI.visualisation.metaparts.FloatArrayList;
import celtech.coreUI.visualisation.metaparts.ModelLoadResult;
import celtech.coreUI.visualisation.metaparts.Part;

import celtech.coreUI.visualisation.modelDisplay.ModelBounds;
import celtech.modelcontrol.ModelContainer;
import celtech.services.modelLoader.ModelLoaderTask;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;
import javafx.beans.property.DoubleProperty;
import javafx.geometry.BoundingBox;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

/**
 *
 * @author Ian Hudson @ Liberty Systems Limited
 */
public class STLImporter
{

    private final Stenographer steno = StenographerFactory.getStenographer(STLImporter.class.
        getName());
    private ModelLoaderTask parentTask = null;
    private DoubleProperty percentProgressProperty = null;
    private final String spacePattern = "[ ]+";

    /**
     *
     * @param parentTask
     * @param modelFile
     * @param targetProject
     * @param percentProgressProperty
     * @return
     */
    public ModelLoadResult loadFile(ModelLoaderTask parentTask, File modelFile,
        Project targetProject, DoubleProperty percentProgressProperty)
    {
        this.parentTask = parentTask;
        this.percentProgressProperty = percentProgressProperty;
        boolean fileIsBinary;
        boolean modelIsTooLarge = false;

        Part loadedPart = null;

        steno.info("Starting STL load");

        //Note that FileReader is used, not File, since File is not Closeable
        try
        {
            Scanner scanner = new Scanner(new FileReader(modelFile));
            fileIsBinary = isFileBinary(modelFile);
            int lineNumber = 1;

            if (!fileIsBinary)
            {
                steno.debug("I have an ASCII file");
            } else
            {
                steno.debug("I'm guessing I have a binary file");
                fileIsBinary = true;
            }

            try
            {
                if (fileIsBinary)
                {
                    loadedPart = processBinarySTLData(modelFile);

                } else
                {

                    loadedPart = processAsciiSTLData(modelFile);
                }
            } catch (STLFileParsingException ex)
            {
                steno.error("File parsing exception whilst processing " + modelFile.getName()
                    + " : " + ex + " on line " + lineNumber);
            } finally
            {
                //ensure the underlying stream is always closed
                //this only has any effect if the item passed to the Scanner
                //constructor implements Closeable (which it does in this case).
                scanner.close();
            }

        } catch (FileNotFoundException ex)
        {
            steno.error("Couldn't find or open " + modelFile.getName());
        }
        if (parentTask == null || (!parentTask.isCancelled()))
        {
            ModelLoadResult result = new ModelLoadResult(modelFile.getAbsolutePath(),
                                                         modelFile.getName(), targetProject,
                                                         loadedPart);
            return result;
        } else
        {
            return null;
        }
    }

    @SuppressWarnings("empty-statement")
    private int getLines(File aFile)
    {
        LineNumberReader reader = null;
        try
        {
            reader = new LineNumberReader(new FileReader(aFile));
            while ((reader.readLine()) != null);
            return reader.getLineNumber();
        } catch (Exception ex)
        {
            return -1;
        } finally
        {
            if (reader != null)
            {
                try
                {
                    reader.close();
                } catch (IOException ex)
                {
                    steno.error("Failed to close file during line number read: " + ex);
                }
            }
        }
    }

    private boolean isFileBinary(File stlFile)
    {
        boolean fileIsBinary = false;
        BufferedInputStream inputFileStream;
        ByteBuffer dataBuffer;
        byte[] facetBytes = new byte[4];     // Holds the number of faces

        try
        {
            inputFileStream = new BufferedInputStream(new FileInputStream(stlFile));
            inputFileStream.mark(4000);
            byte[] asciiHeaderBytes = new byte[80];
            int bytesRead = inputFileStream.read(asciiHeaderBytes);
            String asciiHeader = new String(asciiHeaderBytes, "UTF-8");
            steno.debug("The header was: " + asciiHeader);

            bytesRead = inputFileStream.read(facetBytes);                      // We get the 4 bytes
            dataBuffer = ByteBuffer.wrap(facetBytes);   // ByteBuffer for reading correctly the int
            dataBuffer.order(ByteOrder.nativeOrder());    // Set the right order
            int numberOfFacets = dataBuffer.getInt();

            int filesize = (numberOfFacets * 50) + 84;
            inputFileStream.reset();

            if (stlFile.length() == filesize)
            {
                fileIsBinary = true;
            }
        } catch (IOException ex)
        {
            steno.error("Failed to determine whether " + stlFile.getName() + " was binary or ascii."
                + ex.toString());
        }

        return fileIsBinary;
    }

    /**
     *
     * @param stlFile
     * @return
     * @throws STLFileParsingException
     */
    protected Part processBinarySTLData(File stlFile) throws STLFileParsingException
    {
        Part partToReturn = null;
        DataInputStream inputFileStream;
        ByteBuffer dataBuffer;
        byte[] facetBytes = new byte[4];     // Holds the number of faces
        byte[] facetData = new byte[50]; // Each face has 50 bytes of data
        int progressPercent = 0;

        steno.info("Processing binary STL");

        HashMap<Vector3D, Integer> vertices = new HashMap<>();
        try
        {
            inputFileStream = new DataInputStream(new FileInputStream(stlFile));
            byte[] asciiHeaderBytes = new byte[80];
            inputFileStream.read(asciiHeaderBytes);
            String asciiHeader = new String(asciiHeaderBytes, "UTF-8");
            steno.debug("The header was: " + asciiHeader);

            inputFileStream.read(facetBytes);                      // We get the 4 bytes
            dataBuffer = ByteBuffer.wrap(facetBytes);   // ByteBuffer for reading correctly the int
            dataBuffer.order(ByteOrder.nativeOrder());    // Set the right order
            int numberOfFacets = dataBuffer.getInt();

            steno.debug("There are " + numberOfFacets + " facets");

            Set<Face> faces = new HashSet<>();

            int vertexCounter = 0;

            for (int facetNum = 0; facetNum < numberOfFacets; facetNum++)
            {
                if ((parentTask != null) && parentTask.isCancelled())
                {
                    break;
                }

                int progressUpdate = (int) (((double) facetNum / (double) numberOfFacets) * 100);
                if (progressUpdate != progressPercent)
                {
                    progressPercent = progressUpdate;
                    percentProgressProperty.set(progressPercent);
                }

                inputFileStream.read(facetData);              // We get the rest of the file
                dataBuffer = ByteBuffer.wrap(facetData);      // Now we have all the data in this ByteBuffer
                dataBuffer.order(ByteOrder.nativeOrder());

                // Read the Normal and place it 3 times (one for each vertex)
                dataBuffer.getFloat();
                dataBuffer.getFloat();
                dataBuffer.getFloat();

                Face newFace = new Face();

                for (int vertexNumber = 0; vertexNumber < 3; vertexNumber++)
                {
                    float inputVertexX, inputVertexY, inputVertexZ;

                    inputVertexX = dataBuffer.getFloat();
                    inputVertexY = dataBuffer.getFloat();
                    inputVertexZ = dataBuffer.getFloat();

                    Vector3D generatedVertex = new Vector3D(inputVertexX,
                                                            -inputVertexZ,
                                                            inputVertexY);

                    if (!vertices.containsKey(generatedVertex))
                    {
                        vertices.put(generatedVertex, vertexCounter);
                        newFace.setVertexIndex(vertexNumber, vertexCounter);
                        vertexCounter++;
                    } else
                    {
                        newFace.setVertexIndex(vertexNumber, vertices.get(generatedVertex));
                    }
                }

                // After each facet there are 2 bytes without information
                // In the last iteration we dont have to skip those bytes..
                if (facetNum != numberOfFacets - 1)
                {
                    dataBuffer.get();
                    dataBuffer.get();
                }
            }

            steno.info("Started with " + numberOfFacets * 3 + " vertices and now have "
                + vertices.size());

            partToReturn = new Part(vertices, faces);

        } catch (FileNotFoundException ex)
        {
            steno.error(ex.toString());
        } catch (IOException ex)
        {
            steno.error(ex.toString());
        }

        return partToReturn;
    }

    private Part processAsciiSTLData(File modelFile)
    {
        Part partToReturn = null;
        int linesInFile = getLines(modelFile);

        int progressPercent = 0;
        int lineNumber = 0;

        HashMap<Vector3D, Integer> graph = new HashMap<>();

        try
        {
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(
                modelFile)));

            String line = null;

            int vertexCounter = 0;
            int facetCounter = 0;

            while ((line = reader.readLine()) != null
                && !parentTask.isCancelled())
            {

                if (line.trim().startsWith("vertex"))
                {
                    facetCounter++;
                    Face newFace = new Face();

                    for (int vertexNumber = 0; vertexNumber < 3; vertexNumber++)
                    {
                        String[] lineBits = line.trim().split(spacePattern);

                        Vector3D generatedVertex = new Vector3D(
                            Float.valueOf(lineBits[1]),
                            -Float.valueOf(lineBits[3]),
                            Float.valueOf(lineBits[2]));

                        if (!graph.containsKey(generatedVertex))
                        {
                            graph.put(generatedVertex, vertexCounter);
                            newFace.setVertexIndex(vertexNumber, vertexCounter);
                            vertexCounter++;
                        } else
                        {
                            newFace.setVertexIndex(vertexNumber, graph.get(generatedVertex));
                        }

                        lineNumber++;

                        if (vertexNumber < 2)
                        {
                            line = reader.readLine();
                        }
                    }
                } else
                {
                    lineNumber++;
                }

                int progressUpdate = (int) (((double) lineNumber / (double) linesInFile) * 100);
                if (progressUpdate != progressPercent)
                {
                    progressPercent = progressUpdate;
                    percentProgressProperty.set(progressPercent);
                }
            }

            reader.close();

            steno.info("Started with " + facetCounter * 3 + " vertices and now have "
                + graph.size());

            partToReturn = new Part(graph, faces);
        } catch (FileNotFoundException ex)
        {
            steno.error("Failed to open STL file " + modelFile.getAbsolutePath() + " for reading");
        } catch (IOException ex)
        {
            steno.error("IO Exception on line " + lineNumber + " when reading STL file "
                + modelFile.getAbsolutePath());
        }

        return partToReturn;
    }
}
