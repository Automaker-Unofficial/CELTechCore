/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.services.slicer;

import celtech.configuration.SlicerType;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author ianhudson
 */
class SlicerOutputGobbler extends Thread
{

    private InputStream is = null;
    private String type = null;
    private Stenographer steno = StenographerFactory.getStenographer(this.getClass().getName());
    ;
    private SlicerTask taskToUpdate = null;
    private SlicerType slicerType = null;

    SlicerOutputGobbler(SlicerTask taskToUpdate, InputStream is, String type, SlicerType slicerType)
    {
        this.taskToUpdate = taskToUpdate;
        this.is = is;
        this.type = type;
        this.slicerType = slicerType;
        this.setName("SlicerOutputGobbler-" + type);
    }

    @Override
    public void run()
    {
        if (slicerType == SlicerType.Slic3r)
        {
            setLoadProgress("Slicing meshes", 5);
        }

        try
        {
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String line = null;
            while ((line = br.readLine()) != null)
            {
                steno.debug(type + ">" + line);
                if (slicerType == SlicerType.Slic3r)
                {
                    if (line.contains("Processing triangulated mesh"))
                    {
                        setLoadProgress("Slicing perimeters", 10);
                    } else if (line.contains("Generating perimeters"))
                    {
                        setLoadProgress("Slicing solid surfaces", 20);
                    } else if (line.contains("Detecting solid surfaces"))
                    {
                        setLoadProgress("Slicing infill", 30);
                    } else if (line.contains("Preparing infill surfaces"))
                    {
                        setLoadProgress("Slicing horizontal shells", 40);
                    } else if (line.contains("Generating horizontal shells"))
                    {
                        setLoadProgress("Slicing infill", 50);
                    } else if (line.contains("Combining infill"))
                    {
                        setLoadProgress("Slicing layers", 60);
                    } else if (line.contains("Infilling layers"))
                    {
                        setLoadProgress("Slicing skirt", 70);
                    } else if (line.contains("Generating skirt"))
                    {
                        setLoadProgress("Slice exporting", 80);
                    } else if (line.startsWith("Exporting"))
                    {
                    } else if (line.startsWith("Done"))
                    {
                        setLoadProgress("Slicing complete", 100);
                    }
                } else if (slicerType == SlicerType.Cura)
                {
                    if (line.startsWith("Progress"))
                    {
                        String[] lineParts = line.split(":");
                        if (lineParts.length == 4)
                        {
                            String task = lineParts[1];
                            int progressInt = Integer.valueOf(lineParts[2]);
                            setLoadProgress(task, progressInt);
                        }
                    }
                }
            }
        } catch (IOException ioe)
        {
            steno.error(ioe.getMessage());
        }
    }

    private void setLoadProgress(final String loadMessage, final int percentProgress)
    {
        taskToUpdate.progressUpdateFromSlicer(loadMessage, percentProgress);
    }
}
