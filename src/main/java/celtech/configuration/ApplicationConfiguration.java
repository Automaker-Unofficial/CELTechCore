package celtech.configuration;

import celtech.appManager.ProjectMode;
import celtech.roboxbase.configuration.BaseConfiguration;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Properties;
import javafx.geometry.Pos;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author Ian Hudson @ Liberty Systems Limited
 */
public class ApplicationConfiguration
{

    private static final Stenographer steno = StenographerFactory.getStenographer(
            ApplicationConfiguration.class.getName());

    public static final String resourcePath = "/celtech/resources/";

    public static final String modelResourcePath = resourcePath + "models/";

    public static final String imageResourcePath = resourcePath + "images/";

    public static final String fxmlResourcePath = resourcePath + "fxml/";

    public static final String fxmlPanelResourcePath = resourcePath + "fxml/panels/";

    public static final String fxmlDiagramsResourcePath = resourcePath + "fxml/diagrams/";

    public static final String fxmlButtonsResourcePath = resourcePath + "fxml/buttons/";

    public static final String fxmlUtilityPanelResourcePath = resourcePath + "fxml/utilityPanels/";

    public static final String fxmlPopupResourcePath = resourcePath + "fxml/popups/";

    public static final String fontResourcePath = resourcePath + "fonts/";

    public static final String cssResourcePath = resourcePath + "css/";

    public static final String timeAndCostFileSubpath = "TimeCostTemp/";

    private static final String mainCSSFile = cssResourcePath + "JMetroDarkTheme.css";

    private static final String dialogsCSSFile = cssResourcePath + "dialogsOverride.css";

    public static final double DEFAULT_WIDTH = 1440;

    public static final double DEFAULT_HEIGHT = 900;

    public static final double DESIRED_ASPECT_RATIO = DEFAULT_WIDTH / DEFAULT_HEIGHT;

    private static String projectFileStorageDirectory = null;
    public static String projectFileDirectoryPath = "Projects";

    public static final String projectFileExtension = ".robox";
    public static final String projectModelsFileExtension = ".models";
    public static final String demoPrintFilename = "demoPrint.gcode";
    private static final String supportedProjectFileExtension = projectFileExtension.replaceFirst(
            "\\.", "");

    public static final String[] supportedModelExtensions =
    {
        "stl",
        "obj",
        "zip"
    };

    public static final String[] supported2DModelExtensions =
    {
//        "svg"
    };

    public static final String[] supportedProcessedModelExtensions =
    {
//        "gcode"
    };

    public static final String printFileExtension = ".prt";

    private static String myMiniFactoryDownloadsDirectory = null;

    public static final float bedHotAboveDegrees = 60.0f;

    public static final String projectDataFilename = "projects.dat";
    private static final String userLocaleItem = "Locale";

    public static final Color xAxisColour = Color.RED;

    public static final Color zAxisColour = Color.GREEN;

    public static final Duration notificationDisplayDelay = Duration.seconds(5);

    public static final Pos notificationPosition = Pos.BOTTOM_RIGHT;

    /**
     * These variables are used to position the head correctly over the bed The
     * actual travel of the mechanical system is not the same as the theoretical
     * travel (to allow for door opening positions etc)
     */
    public static final int xPrintOffset = 6;
    public static final int yPrintOffset = 6;

    public static String getProjectDirectory()
    {

        projectFileStorageDirectory = BaseConfiguration.getUserStorageDirectory() + projectFileDirectoryPath + '/';

        File dirHandle = new File(projectFileStorageDirectory);

        if (!dirHandle.exists())
        {
            dirHandle.mkdirs();
        }

        return projectFileStorageDirectory;
    }

    public static ArrayList<String> getSupportedFileExtensionWildcards(ProjectMode projectMode)
    {
        ArrayList<String> returnVal = new ArrayList<>();

        switch (projectMode)
        {
            case NONE:
                for (String extension : supportedModelExtensions)
                {
                    returnVal.add("*." + extension);
                }
                for (String extension : supported2DModelExtensions)
                {
                    returnVal.add("*." + extension);
                }
                for (String extension : supportedProcessedModelExtensions)
                {
                    returnVal.add("*." + extension);
                }
                break;
            case MESH:
                for (String extension : supportedModelExtensions)
                {
                    returnVal.add("*." + extension);
                }
                break;
            case SVG:
                for (String extension : supported2DModelExtensions)
                {
                    returnVal.add("*." + extension);
                }
                break;
            default:
                break;
        }

        return returnVal;
    }

    public static ArrayList<String> getSupportedFileExtensions(ProjectMode projectMode)
    {
        ArrayList<String> returnVal = new ArrayList<>();

        switch (projectMode)
        {
            case NONE:
                for (String extension : supportedModelExtensions)
                {
                    returnVal.add(extension);
                }
                for (String extension : supported2DModelExtensions)
                {
                    returnVal.add(extension);
                }
                returnVal.add(supportedProjectFileExtension);
                break;
            case MESH:
                for (String extension : supportedModelExtensions)
                {
                    returnVal.add(extension);
                }
                break;
            case SVG:
                for (String extension : supported2DModelExtensions)
                {
                    returnVal.add(extension);
                }
                break;
            default:
                break;
        }

        return returnVal;
    }

    public static Locale getUserPreferredLocale()
    {
        Locale localeToReturn = null;

        if (BaseConfiguration.getApplicationMemory(userLocaleItem) != null)
        {
            localeToReturn = Locale.forLanguageTag(BaseConfiguration.getApplicationMemory(
                    userLocaleItem));
        } else
        {
            localeToReturn = Locale.forLanguageTag(BaseConfiguration.getApplicationInstallationLanguage());
        }

        return localeToReturn;
    }

    public static void setUserPreferredLocale(Locale locale)
    {
        BaseConfiguration.setApplicationMemory(userLocaleItem, locale.getLanguage());
    }

    public static String getLastDirectory(DirectoryMemoryProperty memoryProperty)
    {
        String directory = BaseConfiguration.getApplicationMemory(memoryProperty.name());
        if (directory == null)
        {
            directory = BaseConfiguration.getUserStorageDirectory();
            BaseConfiguration.setApplicationMemory(memoryProperty.name(), directory);
        }
        
        return directory;
    }

    public static void setLastDirectory(DirectoryMemoryProperty memoryProperty, String value)
    {
        BaseConfiguration.setApplicationMemory(memoryProperty.name(), value);
    }

    /**
     * This method supplies the application-specific download directory
     * component for updates It is a hack and should be removed...
     *
     * @param applicationName
     * @return
     */
    public static String getDownloadModifier(String applicationName)
    {
        if (applicationName.equals("CEL AutoMaker"))
        {
            return "0abc523fc24";
        } else if (applicationName.equals("CEL ReelProgrammer"))
        {
            return "14f690bc22c";
        } else if (applicationName.equals("CEL Commissionator"))
        {
            return "1532f2c4ab";
        } else
        {
            return null;
        }
    }

    public static String getMainCSSFile()
    {
        return ApplicationConfiguration.class.getResource(mainCSSFile).toExternalForm();
    }

    public static String getDialogsCSSFile()
    {
        return ApplicationConfiguration.class.getResource(dialogsCSSFile).toExternalForm();
    }

    public static String getMyMiniFactoryDownloadDirectory()
    {
        if (myMiniFactoryDownloadsDirectory == null)
        {
            myMiniFactoryDownloadsDirectory = BaseConfiguration.getUserStorageDirectory() + "MyMiniFactory" + '/';

            File dirHandle = new File(myMiniFactoryDownloadsDirectory);

            if (!dirHandle.exists())
            {
                dirHandle.mkdirs();
            }
        }

        return myMiniFactoryDownloadsDirectory;
    }
}
