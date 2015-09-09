package celtech.printerControl.comms.commands;

import celtech.Lookup;
import celtech.configuration.ApplicationConfiguration;
import celtech.printerControl.model.Head;
import celtech.utils.SystemUtils;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;
import org.apache.commons.io.FileUtils;

/**
 *
 * @author ianhudson
 */
public class GCodeMacros
{

    private static final String safetyFeaturesOnDirectory = "Safety_Features_ON";
    private static final String safetyFeaturesOffDirectory = "Safety_Features_OFF";

    private static final Stenographer steno = StenographerFactory.getStenographer(GCodeMacros.class.
            getName());
    private static final String macroDefinitionString = "Macro:";

    public interface FilenameEncoder
    {

        public String getFilenameCode();
    }

    public enum SafetyIndicator implements FilenameEncoder
    {

        // Safeties off
        SAFETIES_OFF("U"),
        // Safeties on
        SAFETIES_ON("S"),
        DONT_CARE(null);

        private final String filenameCode;

        private SafetyIndicator(String filenameCode)
        {
            this.filenameCode = filenameCode;
        }

        @Override
        public String getFilenameCode()
        {
            return filenameCode;
        }
    }

    public enum NozzleUseIndicator implements FilenameEncoder
    {

        // Nozzle 0 only
        NOZZLE_0("N0"),
        // Nozzle 1 only
        NOZZLE_1("N1"),
        //Both nozzles
        BOTH("NB"),
        DONT_CARE(null);

        private final String filenameCode;

        private NozzleUseIndicator(String filenameCode)
        {
            this.filenameCode = filenameCode;
        }

        @Override
        public String getFilenameCode()
        {
            return filenameCode;
        }
    }

    /**
     *
     * @param macroName - this can include the macro execution directive at the
     * start of the line
     * @param headType
     * @param nozzleUse
     * @param safeties
     * @return
     * @throws java.io.IOException
     * @throws celtech.printerControl.comms.commands.MacroLoadException
     */
    public static ArrayList<String> getMacroContents(String macroName,
            Head.HeadType headType,
            NozzleUseIndicator nozzleUse,
            SafetyIndicator safeties) throws IOException, MacroLoadException
    {
        ArrayList<String> contents = new ArrayList<>();
        ArrayList<String> parentMacros = new ArrayList<>();

        if (Lookup.getUserPreferences().isSafetyFeaturesOn())
        {
            contents.add("; Printed with safety features ON");
        } else
        {
            contents.add("; Printed with safety features OFF");
        }

        appendMacroContents(contents, parentMacros, macroName,
                headType, nozzleUse, safeties);

        return contents;
    }

    private static String cleanMacroName(String macroName)
    {
        return macroName.replaceFirst(macroDefinitionString, "").trim();
    }

    /**
     *
     * @param macroName
     * @return
     */
    private static ArrayList<String> appendMacroContents(ArrayList<String> contents,
            final ArrayList<String> parentMacros,
            final String macroName,
            Head.HeadType headType,
            NozzleUseIndicator nozzleUse,
            SafetyIndicator safeties) throws IOException, MacroLoadException
    {
        String cleanedMacroName = cleanMacroName(macroName);

        if (!parentMacros.contains(cleanedMacroName))
        {
            steno.debug("Processing macro: " + cleanedMacroName);
            contents.add(";");
            contents.add("; Macro Start - " + cleanedMacroName);
            contents.add(";");

            parentMacros.add(cleanedMacroName);

            FileReader fileReader = null;

            try
            {
                fileReader = new FileReader(GCodeMacros.getFilename(cleanedMacroName,
                        headType,
                        nozzleUse,
                        safeties
                ));
                Scanner scanner = new Scanner(fileReader);

                while (scanner.hasNextLine())
                {
                    String line = scanner.nextLine();
                    line = line.trim();

                    if (isMacroExecutionDirective(line))
                    {
                        String subMacroName = line.replaceFirst(macroDefinitionString, "").trim();
                        if (subMacroName != null)
                        {
                            steno.debug("Sub-macro " + subMacroName + " detected");

                            appendMacroContents(contents, parentMacros, subMacroName,
                                    headType,
                                    nozzleUse,
                                    safeties);
                        }
                    } else
                    {
                        contents.add(line);
                    }
                }
            } catch (FileNotFoundException ex)
            {
                throw new MacroLoadException("Failure to load contents of macro file " + macroName
                        + " : " + ex.getMessage());
            } finally
            {
                if (fileReader != null)
                {
                    fileReader.close();
                }
            }

            parentMacros.remove(macroName);
        } else
        {
            StringBuilder messageBuffer = new StringBuilder();
            messageBuffer.append("Macro circular dependency detected in chain: ");
            parentMacros.forEach(macro ->
            {
                messageBuffer.append(macro);
                messageBuffer.append("->");
            });
            messageBuffer.append(macroName);

            throw new MacroLoadException(messageBuffer.toString());
        }

        contents.add(";");
        contents.add("; Macro End - " + macroName);
        contents.add(";");

        return contents;
    }

    /**
     * Macros are stored in a single directory They are named as follows:
     * <baseMacroName>_<[S|U]>_<headType>_<[nozzle0Used|nozzle1Used]>
     * e.g. macroA_S_RBX01-SM - is a macro that should be used for safe mode
     * when using head RBX01-SM
     *
     * @param macroName
     * @param headType
     * @param nozzleUse
     * @param safeties
     * @return
     */
    public static String getFilename(String macroName,
            Head.HeadType headType,
            NozzleUseIndicator nozzleUse,
            SafetyIndicator safeties)
    {
        StringBuilder fileNameBuffer = new StringBuilder();

        fileNameBuffer.append(ApplicationConfiguration.getCommonApplicationDirectory());
        fileNameBuffer.append(ApplicationConfiguration.macroFileSubpath);

        FilenameFilter filter = new MacroFilenameFilter(macroName, null, NozzleUseIndicator.DONT_CARE, SafetyIndicator.DONT_CARE);

        File macroDirectory = new File(ApplicationConfiguration.getCommonApplicationDirectory() + ApplicationConfiguration.macroFileSubpath);

        File[] macroFiles = macroDirectory.listFiles(filter);

        fileNameBuffer.append(macroName);
        fileNameBuffer.append(ApplicationConfiguration.macroFileExtension);

        if (macroFiles.length > 0)
        {
            steno.info("Found " + macroFiles.length + " macro files:");
            for (int counter = 0; counter < macroFiles.length; counter++)
            {
                steno.info(macroFiles[counter].getName());
            }
            return macroFiles[0].getAbsolutePath();
        } else
        {
            return null;
        }
    }

    public static boolean isMacroExecutionDirective(String input)
    {
        return input.startsWith(macroDefinitionString);
    }

    private String getMacroNameFromDirective(String macroDirective)
    {
        String macroName = null;
        String[] parts = macroDirective.split(":");
        if (parts.length == 2)
        {
            macroName = parts[1].trim();
        } else
        {
            steno.error("Saw macro directive but couldn't understand it: " + macroDirective);
        }
        return macroName;
    }

    public static int getNumberOfOperativeLinesInMacro(String macroDirective)
    {
        int linesInMacro = 0;
        String macro = cleanMacroName(macroDirective);
        if (macro != null)
        {
            try
            {
                List<String> contents = getMacroContents(macro, null, NozzleUseIndicator.DONT_CARE, SafetyIndicator.DONT_CARE);
                for (String line : contents)
                {
                    if (line.trim().startsWith(";") == false && line.equals("") == false)
                    {
                        linesInMacro++;
                    }
                }
            } catch (IOException | MacroLoadException ex)
            {
                steno.error("Error trying to get number of lines in macro " + macro);
            }
        }

        return linesInMacro;
    }
}
