package celtech.appManager;

import celtech.Lookup;
import celtech.configuration.ApplicationConfiguration;
import celtech.configuration.fileRepresentation.ModelContainerProjectFile;
import celtech.configuration.fileRepresentation.ProjectFile;
import celtech.modelcontrol.Groupable;
import celtech.modelcontrol.ItemState;
import celtech.modelcontrol.ModelContainer;
import celtech.modelcontrol.ModelGroup;
import celtech.roboxbase.configuration.SlicerType;
import celtech.roboxbase.configuration.fileRepresentation.PrinterSettingsOverrides;
import celtech.modelcontrol.ProjectifiableThing;
import celtech.modelcontrol.ResizeableThreeD;
import celtech.modelcontrol.ResizeableTwoD;
import celtech.modelcontrol.ScaleableThreeD;
import celtech.modelcontrol.ScaleableTwoD;
import celtech.modelcontrol.TranslateableThreeD;
import celtech.modelcontrol.TranslateableTwoD;
import celtech.roboxbase.services.slicer.PrintQualityEnumeration;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author Ian Hudson @ Liberty Systems Limited
 */
public abstract class Project
{

    public static class ProjectLoadException extends Exception
    {

        public ProjectLoadException(String message)
        {
            super(message);
        }
    }

    private int version = -1;

    private static final Stenographer steno = StenographerFactory.getStenographer(Project.class.getName());
    protected static final ObjectMapper mapper = new ObjectMapper();

    protected Set<ProjectChangesListener> projectChangesListeners;

    protected BooleanProperty canPrint;
    protected BooleanProperty customSettingsNotChosen;

    protected final PrinterSettingsOverrides printerSettings;

    protected final StringProperty projectNameProperty;
    protected ObjectProperty<Date> lastModifiedDate;

    protected boolean suppressProjectChanged = false;

    protected ObjectProperty<ProjectMode> mode = new SimpleObjectProperty<>(ProjectMode.NONE);

    protected ObservableList<ProjectifiableThing> topLevelThings;

    protected String lastPrintJobID = "";

    public Project()
    {
        topLevelThings = FXCollections.observableArrayList();

        initialise();

        canPrint = new SimpleBooleanProperty(true);
        customSettingsNotChosen = new SimpleBooleanProperty(true);
        lastModifiedDate = new SimpleObjectProperty<>();
        projectChangesListeners = new HashSet<>();

        printerSettings = new PrinterSettingsOverrides();
        Date now = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("-hhmmss-ddMMYY");
        projectNameProperty = new SimpleStringProperty(Lookup.i18n("projectLoader.untitled")
                + formatter.format(now));
        lastModifiedDate.set(now);
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true);

        customSettingsNotChosen.bind(
                printerSettings.printQualityProperty().isEqualTo(PrintQualityEnumeration.CUSTOM)
                .and(printerSettings.getSettingsNameProperty().isEmpty()));
        // Cannot print if quality is CUSTOM and no custom settings have been chosen
        canPrint.bind(customSettingsNotChosen.not());

        printerSettings.getDataChanged().addListener(
                (ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) ->
                {
                    projectModified();
                    fireWhenPrinterSettingsChanged(printerSettings);
                });

        Lookup.getUserPreferences().getSlicerTypeProperty().addListener(
                (ObservableValue<? extends SlicerType> observable, SlicerType oldValue, SlicerType newValue) ->
                {
                    projectModified();
                });

    }

    protected abstract void initialise();

    public final void setProjectName(String value)
    {
        projectNameProperty.set(value);
    }

    public final String getProjectName()
    {
        return projectNameProperty.get();
    }

    public final StringProperty projectNameProperty()
    {
        return projectNameProperty;
    }

    public final String getAbsolutePath()
    {
        return ApplicationConfiguration.getProjectDirectory() + File.separator
                + projectNameProperty.get()
                + ApplicationConfiguration.projectFileExtension;
    }

    protected abstract void load(ProjectFile projectFile, String basePath) throws ProjectLoadException;

    public static final Project loadProject(String basePath)
    {
        Project project = null;
        File file = new File(basePath + ApplicationConfiguration.projectFileExtension);

        try
        {
            ProjectFile projectFile = mapper.readValue(file, ProjectFile.class);

            if (projectFile instanceof ModelContainerProjectFile)
            {
                project = new ModelContainerProject();
                project.load(projectFile, basePath);
            }
//            else if (projectFile instanceof SVG)
//            {
//                
//            }
        } catch (Exception ex)
        {
            steno.exception("Unable to load project file at " + basePath, ex);
        }
        return project;
    }

    protected abstract void save(String basePath);

    public static final void saveProject(Project project)
    {
        String basePath = ApplicationConfiguration.getProjectDirectory() + File.separator
                + project.getProjectName();
        project.save(basePath);
    }

    @Override
    public String toString()
    {
        return projectNameProperty.get();
    }

    public final PrintQualityEnumeration getPrintQuality()
    {
        return printerSettings.getPrintQuality();
    }

    public final void setPrintQuality(PrintQualityEnumeration printQuality)
    {
        if (printerSettings.getPrintQuality() != printQuality)
        {
            projectModified();
            printerSettings.setPrintQuality(printQuality);
        }
    }

    public final PrinterSettingsOverrides getPrinterSettings()
    {
        return printerSettings;
    }

    public abstract void addModel(ProjectifiableThing projectifiableThing);

    public abstract void removeModels(Set<ProjectifiableThing> projectifiableThings);

    public final void addProjectChangesListener(ProjectChangesListener projectChangesListener)
    {
        projectChangesListeners.add(projectChangesListener);
    }

    public final void removeProjectChangesListener(ProjectChangesListener projectChangesListener)
    {
        projectChangesListeners.remove(projectChangesListener);
    }

    public final ObjectProperty<Date> getLastModifiedDate()
    {
        return lastModifiedDate;
    }

    public final BooleanProperty canPrintProperty()
    {
        return canPrint;
    }

    public final BooleanProperty customSettingsNotChosenProperty()
    {
        return customSettingsNotChosen;
    }

    /**
     * ProjectChangesListener allows other objects to observe when models are
     * added or removed etc to the project.
     */
    public interface ProjectChangesListener
    {

        /**
         * This should be fired when a model is added to the project.
         *
         * @param projectifiableThing
         */
        void whenModelAdded(ProjectifiableThing projectifiableThing);

        /**
         * This should be fired when a model is removed from the project.
         *
         * @param projectifiableThing
         */
        void whenModelsRemoved(Set<ProjectifiableThing> projectifiableThing);

        /**
         * This should be fired when the project is auto laid out.
         */
        void whenAutoLaidOut();

        /**
         * This should be fired when one or more models have been moved, rotated
         * or scaled etc. If possible try to fire just once for any given group
         * change.
         *
         * @param projectifiableThing
         */
        void whenModelsTransformed(Set<ProjectifiableThing> projectifiableThing);

        /**
         * This should be fired when certain details of the model change.
         * Currently this is only: - associatedExtruder
         *
         * @param modelContainer
         * @param propertyName
         */
        void whenModelChanged(ProjectifiableThing modelContainer, String propertyName);

        /**
         * This should be fired whenever the PrinterSettings of the project
         * changes.
         *
         * @param printerSettings
         */
        void whenPrinterSettingsChanged(PrinterSettingsOverrides printerSettings);
    }

    public abstract void autoLayout();

    /**
     * Scale X, Y and Z by the given factor, apply the given ratio to the given
     * scale. I.e. the ratio is not an absolute figure to be applied to the
     * models but a ratio to be applied to the current scale.
     *
     * @param projectifiableThings
     * @param ratio
     */
    public final void scaleXYZRatioSelection(Set<ScaleableThreeD> projectifiableThings, double ratio)
    {
        for (ScaleableThreeD projectifiableThing : projectifiableThings)
        {
            projectifiableThing.setXScale(projectifiableThing.getXScale() * ratio);
            projectifiableThing.setYScale(projectifiableThing.getYScale() * ratio);
            projectifiableThing.setZScale(projectifiableThing.getZScale() * ratio);
        }
        projectModified();
        fireWhenModelsTransformed((Set) projectifiableThings);
    }

    /**
     * Scale X, Y and Z by the given factor, apply the given ratio to the given
     * scale. I.e. the ratio is not an absolute figure to be applied to the
     * models but a ratio to be applied to the current scale.
     *
     * @param projectifiableThings
     * @param ratio
     */
    public final void scaleXYRatioSelection(Set<ScaleableTwoD> projectifiableThings, double ratio)
    {
        for (ScaleableTwoD projectifiableThing : projectifiableThings)
        {
            projectifiableThing.setXScale(projectifiableThing.getXScale() * ratio);
            projectifiableThing.setYScale(projectifiableThing.getYScale() * ratio);
        }
        projectModified();
        fireWhenModelsTransformed((Set) projectifiableThings);
    }

    public final void scaleXModels(Set<ScaleableTwoD> projectifiableThings, double newScale,
            boolean preserveAspectRatio)
    {
        if (preserveAspectRatio)
        {
            // this only happens for non-multiselect
            assert (projectifiableThings.size() == 1);
            ScaleableTwoD projectifiableThing = projectifiableThings.iterator().next();
            double ratio = newScale / projectifiableThing.getXScale();
            if (projectifiableThing instanceof ScaleableThreeD)
            {
                scaleXYZRatioSelection((Set) projectifiableThings, ratio);
            } else
            {
                scaleXYRatioSelection(projectifiableThings, ratio);
            }
        } else
        {
            for (ScaleableTwoD projectifiableThing : projectifiableThings)
            {
                {
                    projectifiableThing.setXScale(newScale);
                }
            }
        }
        projectModified();
        fireWhenModelsTransformed((Set) projectifiableThings);
    }

    public final void scaleYModels(Set<ScaleableTwoD> projectifiableThings, double newScale,
            boolean preserveAspectRatio)
    {
        if (preserveAspectRatio)
        {
            // this only happens for non-multiselect
            assert (projectifiableThings.size() == 1);
            ScaleableTwoD projectifiableThing = projectifiableThings.iterator().next();
            double ratio = newScale / projectifiableThing.getYScale();

            if (projectifiableThing instanceof ScaleableThreeD)
            {
                scaleXYZRatioSelection((Set) projectifiableThings, ratio);
            } else
            {
                scaleXYRatioSelection(projectifiableThings, ratio);
            }
        } else
        {
            for (ScaleableTwoD projectifiableThing : projectifiableThings)
            {
                {
                    projectifiableThing.setYScale(newScale);
                }
            }
        }
        projectModified();
        fireWhenModelsTransformed((Set) projectifiableThings);
    }

    public final void scaleZModels(Set<ScaleableThreeD> projectifiableThings, double newScale,
            boolean preserveAspectRatio)
    {
        if (preserveAspectRatio)
        {
            // this only happens for non-multiselect
            assert (projectifiableThings.size() == 1);
            ScaleableThreeD projectifiableThing = projectifiableThings.iterator().next();
            double ratio = newScale / projectifiableThing.getZScale();
            scaleXYZRatioSelection(projectifiableThings, ratio);
        } else
        {
            for (ScaleableThreeD projectifiableThing : projectifiableThings)
            {
                {
                    projectifiableThing.setZScale(newScale);
                }
            }
        }
        projectModified();
        fireWhenModelsTransformed((Set) projectifiableThings);
    }

    public void translateModelsBy(Set<TranslateableTwoD> modelContainers, double x, double y)
    {
        for (TranslateableTwoD model : modelContainers)
        {
            model.translateBy(x, y);
        }
        projectModified();

        fireWhenModelsTransformed((Set) modelContainers);
    }

    public void translateModelsTo(Set<TranslateableTwoD> modelContainers, double x, double y)
    {
        for (TranslateableTwoD model : modelContainers)
        {
            model.translateTo(x, y);
        }
        projectModified();
        fireWhenModelsTransformed((Set) modelContainers);
    }

    public void translateModelsXTo(Set<TranslateableTwoD> modelContainers, double x)
    {
        for (TranslateableTwoD model : modelContainers)
        {
            model.translateXTo(x);
        }
        projectModified();

        fireWhenModelsTransformed((Set) modelContainers);
    }

    public void translateModelsZTo(Set<TranslateableThreeD> modelContainers, double z)
    {
        for (TranslateableThreeD model : modelContainers)
        {
            model.translateZTo(z);
        }
        projectModified();

        fireWhenModelsTransformed((Set) modelContainers);
    }

    public void resizeModelsDepth(Set<ResizeableThreeD> modelContainers, double depth)
    {
        for (ResizeableThreeD model : modelContainers)
        {
            model.resizeDepth(depth);
        }
        projectModified();

        fireWhenModelsTransformed((Set) modelContainers);
    }

    public void resizeModelsHeight(Set<ResizeableTwoD> modelContainers, double height)
    {
        for (ResizeableTwoD model : modelContainers)
        {
            model.resizeHeight(height);
        }
        projectModified();

        fireWhenModelsTransformed((Set) modelContainers);
    }

    public void resizeModelsWidth(Set<ResizeableTwoD> modelContainers, double width)
    {
        for (ResizeableTwoD model : modelContainers)
        {
            model.resizeWidth(width);
        }
        projectModified();

        fireWhenModelsTransformed((Set) modelContainers);
    }

    public abstract Set<ProjectifiableThing> getAllModels();

    public final Set<ItemState> getModelStates()
    {
        Set<ItemState> states = new HashSet<>();
        for (ProjectifiableThing model : getAllModels())
        {
            states.add(model.getState());
        }
        return states;
    }

    public final void setModelStates(Set<ItemState> modelStates)
    {
        Set<ProjectifiableThing> modelContainers = new HashSet<>();
        for (ItemState modelState : modelStates)
        {
            for (ProjectifiableThing model : getAllModels())
            {
                if (model.getModelId() == modelState.modelId)
                {
                    model.setState(modelState);
                    modelContainers.add(model);
                }
            }
        }
        projectModified();
        fireWhenModelsTransformed(modelContainers);
    }

    public final ReadOnlyObjectProperty<ProjectMode> getModeProperty()
    {
        return mode;
    }

    public ProjectMode getMode()
    {
        return mode.get();
    }

    public final void setMode(ProjectMode mode)
    {
        this.mode.set(mode);
    }

    protected final void projectModified()
    {
        if (!suppressProjectChanged)
        {
            lastPrintJobID = "";
            lastModifiedDate.set(new Date());
        }
    }

    abstract protected void fireWhenModelsTransformed(Set<ProjectifiableThing> projectifiableThings);

    abstract protected void fireWhenPrinterSettingsChanged(PrinterSettingsOverrides printerSettings);

    public int getNumberOfProjectifiableElements()
    {
        return getAllModels().size();
    }

    public ObservableList<ProjectifiableThing> getTopLevelThings()
    {
        return topLevelThings;
    }

    public void setLastPrintJobID(String lastPrintJobID)
    {
        this.lastPrintJobID = lastPrintJobID;
    }

    public String getLastPrintJobID()
    {
        return lastPrintJobID;
    }

    public ModelGroup group(Set<Groupable> modelContainers)
    {
        Set<ProjectifiableThing> projectifiableThings = (Set) modelContainers;

        removeModels(projectifiableThings);
        ModelGroup modelGroup = createNewGroup(modelContainers);
        addModel(modelGroup);
        return modelGroup;
    }

    public ModelGroup group(Set<Groupable> modelContainers, int groupModelId)
    {
        Set<ProjectifiableThing> projectifiableThings = (Set) modelContainers;

        removeModels(projectifiableThings);
        ModelGroup modelGroup = createNewGroup(modelContainers, groupModelId);
        addModel(modelGroup);
        return modelGroup;
    }

    /**
     * Create a new group from models that are not yet in the project.
     *
     * @param modelContainers
     * @param groupModelId
     * @return
     */
    public ModelGroup createNewGroup(Set<Groupable> modelContainers, int groupModelId)
    {
        checkNotAlreadyInGroup(modelContainers);
        ModelGroup modelGroup = new ModelGroup((Set) modelContainers, groupModelId);
        modelGroup.checkOffBed();
        return modelGroup;
    }

    public void ungroup(Set<? extends ModelContainer> modelContainers)
    {
        for (ModelContainer modelContainer : modelContainers)
        {
            if (modelContainer instanceof ModelGroup)
            {
                ModelGroup modelGroup = (ModelGroup) modelContainer;
                Set<ProjectifiableThing> modelGroups = new HashSet<>();
                modelGroups.add(modelGroup);
                removeModels(modelGroups);
                for (ModelContainer childModelContainer : modelGroup.getChildModelContainers())
                {
                    addModel(childModelContainer);
                    childModelContainer.setBedCentreOffsetTransform();
                    childModelContainer.applyGroupTransformToThis(modelGroup);
                    childModelContainer.checkOffBed();
                }
            }
        }
    }

    /**
     * Create a new group from models that are not yet in the project.
     *
     * @param modelContainers
     * @return
     */
    public ModelGroup createNewGroup(Set<Groupable> modelContainers)
    {
        checkNotAlreadyInGroup(modelContainers);

        ModelGroup modelGroup = new ModelGroup((Set) modelContainers);
        modelGroup.checkOffBed();
        modelGroup.notifyScreenExtentsChange();
        return modelGroup;
    }

    protected abstract void checkNotAlreadyInGroup(Set<Groupable> modelContainers);

    /**
     * Create a new group from models that are not yet in the project, and add
     * model listeners to all descendent children.
     *
     * @param modelContainers
     * @return
     */
    public abstract ModelGroup createNewGroupAndAddModelListeners(Set<Groupable> modelContainers);

    @JsonIgnore
    public void invalidate()
    {
        projectModified();
    }
}
