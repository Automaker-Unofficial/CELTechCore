/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.appManager;

import celtech.configuration.ApplicationConfiguration;
import celtech.configuration.fileRepresentation.ProjectFile;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;
import org.codehaus.jackson.map.ObjectMapper;

/**
 *
 * @author Ian Hudson @ Liberty Systems Limited
 */
public class ProjectManager implements Savable, Serializable
{

    private static ProjectManager instance = null;
    private static List<Project> openProjects = new ArrayList<>();
    private final static String projectFileName = "projects.dat";
    private final static Stenographer steno = StenographerFactory.getStenographer(
        ProjectManager.class.getName());
    private final static ProjectFileFilter fileFilter = new ProjectFileFilter();
    private final static ObjectMapper mapper = new ObjectMapper();

    private ProjectManager()
    {
    }

    public static ProjectManager getInstance()
    {
        if (instance == null)
        {
            ProjectManager pm = loadState();
            if (pm != null)
            {
                instance = pm;
            } else
            {
                instance = new ProjectManager();
            }
        }

        return instance;
    }

    private static ProjectManager loadState()
    {
        File projectDirHandle = new File(ApplicationConfiguration.getProjectDirectory());

        if (!projectDirHandle.exists())
        {
            projectDirHandle.mkdirs();
        }

        ProjectManager pm = null;
        try
        {
            FileInputStream projectFile = new FileInputStream(
                ApplicationConfiguration.getProjectDirectory() + projectFileName);
            ObjectInputStream reader = new ObjectInputStream(projectFile);
            pm = new ProjectManager();
            int numberOfOpenProjects = reader.readInt();
            for (int counter = 0; counter < numberOfOpenProjects; counter++)
            {
                String projectPath = reader.readUTF();
                Project project = loadProject(projectPath);
                if (project != null)
                {
                    pm.projectOpened(project);
                } else
                {
                    steno.warning("Project Manager tried to load " + projectPath
                        + " but it couldn't be opened");
                }
            }
            reader.close();
        } catch (IOException ex)
        {
            steno.error("Failed to load project manager");
        }
        return pm;
    }

    public static Project loadProject(String projectPath)
    {
        File file = new File(projectPath);

        Project loadedProject = null;

        try
        {
            ProjectFile projectFile = mapper.readValue(file, ProjectFile.class);
            loadedProject = new Project(projectFile);

        } catch (IOException ex)
        {
            steno.error("Failed to load project " + projectPath);
        }

        return loadedProject;
    }

    @Override
    public boolean saveState()
    {
        boolean savedSuccessfully = false;

        try
        {
            ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(
                ApplicationConfiguration.getProjectDirectory() + projectFileName));
            out.writeInt(openProjects.size());
            for (Project project : openProjects)
            {
                out.writeUTF(project.getAbsolutePath());
            }
            out.close();
        } catch (FileNotFoundException ex)
        {
            steno.error("Failed to save project state");
        } catch (IOException ex)
        {
            steno.error("Couldn't write project manager state to file");
        }

        return savedSuccessfully;
    }

    public void projectOpened(Project project)
    {
        if (!openProjects.contains(project))
        {
            openProjects.add(project);
        }
    }

    public void projectClosed(Project project)
    {
        openProjects.remove(project);
    }

    public List<Project> getOpenProjects()
    {
        return openProjects;
    }

    public ObservableList<Project> getAvailableProjects()
    {
        ObservableList<Project> availableProjects = FXCollections.observableArrayList();

        File projectDir = new File(ApplicationConfiguration.getProjectDirectory());
        File[] projectFiles = projectDir.listFiles(fileFilter);
        for (File file : projectFiles)
        {
            try
            {
                FileInputStream projectFile = new FileInputStream(file);
                ObjectInputStream reader = new ObjectInputStream(projectFile);
                Project project = (Project) reader.readObject();
                availableProjects.add(project);
                reader.close();
            } catch (IOException ex)
            {
                steno.error("Failed to load project manager");
            } catch (ClassNotFoundException ex)
            {
                steno.error("Failure whilst loading available project headers");
            }
        }
        return availableProjects;
    }

    public Set<String> getOpenAndAvailableProjectNames()
    {
        Set<String> openAndAvailableProjectNames = new HashSet<>();
        for (Project project : openProjects)
        {
            openAndAvailableProjectNames.add(project.getProjectName());
        }
        for (Project project : getAvailableProjects())
        {
            openAndAvailableProjectNames.add(project.getProjectName());
        }
        return openAndAvailableProjectNames;
    }

}
