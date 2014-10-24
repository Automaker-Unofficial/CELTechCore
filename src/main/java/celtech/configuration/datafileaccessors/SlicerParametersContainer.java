package celtech.configuration.datafileaccessors;

import celtech.configuration.ApplicationConfiguration;
import celtech.configuration.PrintProfileFileFilter;
import celtech.configuration.fileRepresentation.SlicerParameters;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;

/**
 *
 * @author ianhudson
 */
public class SlicerParametersContainer
{
    
    private static final Stenographer steno = StenographerFactory.getStenographer(SlicerParametersContainer.class.getName());
    private static SlicerParametersContainer instance = null;
    private static final ObservableList<SlicerParameters> appProfileList = FXCollections.observableArrayList();
    private static final ObservableList<SlicerParameters> userProfileList = FXCollections.observableArrayList();
    private static final ObservableList<SlicerParameters> completeProfileList = FXCollections.observableArrayList();
    private static final ObservableMap<String, SlicerParameters> profileMap = FXCollections.observableHashMap();
    private static final ObjectMapper mapper = new ObjectMapper();

    /**
     *
     */
    public static final SlicerParameters createNewProfile = new SlicerParameters();

    /**
     * Return a read-only set of current profile names
     *
     * @return
     */
    public static Set<String> getProfileNames()
    {
        return Collections.unmodifiableSet(profileMap.keySet());
    }
    
    private SlicerParametersContainer()
    {
        mapper.configure(SerializationConfig.Feature.INDENT_OUTPUT, true);
        loadProfileData();
    }

    /**
     *
     * @param profileName
     * @return
     */
    public static String constructFilePath(String profileName)
    {
        return ApplicationConfiguration.getUserPrintProfileDirectory() + profileName + ApplicationConfiguration.printProfileFileExtension;
    }
    
    private static void loadProfileData()
    {
        completeProfileList.clear();
        appProfileList.clear();
        userProfileList.clear();
        
        File applicationDirHandle = new File(ApplicationConfiguration.getApplicationPrintProfileDirectory());
        File[] applicationprofiles = applicationDirHandle.listFiles(new PrintProfileFileFilter());
        ArrayList<SlicerParameters> profiles = ingestProfiles(applicationprofiles, false);
        appProfileList.addAll(profiles);
        completeProfileList.addAll(profiles);
        
        File userDirHandle = new File(ApplicationConfiguration.getUserPrintProfileDirectory());
        File[] userprofiles = userDirHandle.listFiles(new PrintProfileFileFilter());
        profiles = ingestProfiles(userprofiles, true);
        userProfileList.addAll(profiles);
        completeProfileList.addAll(profiles);
    }
    
    private static ArrayList<SlicerParameters> ingestProfiles(File[] userprofiles, boolean mutableProfiles)
    {
        ArrayList<SlicerParameters> profileList = new ArrayList<>();
        
        for (File profileFile : userprofiles)
        {
            SlicerParameters newSettings = new SlicerParameters();
            String profileName = profileFile.getName().replaceAll(ApplicationConfiguration.printProfileFileExtension, "");
            
            if (profileMap.containsKey(profileName) == false)
            {
                try
                {
                    newSettings = mapper.readValue(profileFile, SlicerParameters.class);
                    
                    profileList.add(newSettings);
                    profileMap.put(profileName, newSettings);
                } catch (IOException ex)
                {
                    steno.error("Error reading profile " + profileName);
                }
            } else
            {
                steno.warning("Profile with name " + profileName + " has already been loaded - ignoring " + profileFile.getAbsolutePath());
            }
        }
        
        return profileList;
    }

    /**
     *
     * @param settingsToSave
     */
    public static void saveProfile(SlicerParameters settingsToSave)
    {
        try
        {
            mapper.configure(SerializationConfig.Feature.INDENT_OUTPUT, true);
            mapper.writeValue(new File(constructFilePath(settingsToSave.getProfileName())), settingsToSave);
            loadProfileData();
        } catch (IOException ex)
        {
            steno.error("Error whilst saving profile " + settingsToSave.getProfileName());
        }
    }

    /**
     *
     * @param settingsToSave
     */
    public static void saveProfileWithoutReloading(SlicerParameters settingsToSave)
    {
        try
        {
            mapper.configure(SerializationConfig.Feature.INDENT_OUTPUT, true);
            mapper.writeValue(new File(constructFilePath(settingsToSave.getProfileName())), settingsToSave);
        } catch (IOException ex)
        {
            steno.error("Error whilst saving profile " + settingsToSave.getProfileName());
        }
    }

    /**
     *
     * @param profileName
     */
    public static void deleteProfile(String profileName)
    {
        File profileToDelete = new File(constructFilePath(profileName));
        profileToDelete.delete();
        loadProfileData();
    }

    /**
     *
     * @return
     */
    public static SlicerParametersContainer getInstance()
    {
        if (instance == null)
        {
            instance = new SlicerParametersContainer();
        }
        
        return instance;
    }

    /**
     *
     * @param profileName
     * @return
     */
    public static SlicerParameters getSettingsByProfileName(String profileName)
    {
        if (instance == null)
        {
            instance = new SlicerParametersContainer();
        }
        
        return profileMap.get(profileName);
    }

    /**
     *
     * @return
     */
    public static ObservableList<SlicerParameters> getCompleteProfileList()
    {
        if (instance == null)
        {
            instance = new SlicerParametersContainer();
        }
        
        return completeProfileList;
    }

    /**
     *
     * @return
     */
    public static ObservableList<SlicerParameters> getUserProfileList()
    {
        if (instance == null)
        {
            instance = new SlicerParametersContainer();
        }
        
        return userProfileList;
    }

    /**
     *
     * @return
     */
    public static ObservableList<SlicerParameters> getApplicationProfileList()
    {
        if (instance == null)
        {
            instance = new SlicerParametersContainer();
        }
        
        return appProfileList;
    }
}
