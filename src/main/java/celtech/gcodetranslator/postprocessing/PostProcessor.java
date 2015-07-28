package celtech.gcodetranslator.postprocessing;

import celtech.Lookup;
import celtech.appManager.Project;
import celtech.configuration.fileRepresentation.HeadFile;
import celtech.configuration.fileRepresentation.SlicerParametersFile;
import celtech.gcodetranslator.GCodeOutputWriter;
import celtech.gcodetranslator.NozzleProxy;
import celtech.gcodetranslator.PrintJobStatistics;
import celtech.gcodetranslator.RoboxiserResult;
import celtech.gcodetranslator.postprocessing.nodes.GCodeEventNode;
import celtech.gcodetranslator.postprocessing.nodes.LayerNode;
import celtech.gcodetranslator.postprocessing.nodes.SectionNode;
import celtech.gcodetranslator.postprocessing.nodes.ToolSelectNode;
import celtech.gcodetranslator.postprocessing.nodes.nodeFunctions.DurationCalculationException;
import celtech.gcodetranslator.postprocessing.nodes.nodeFunctions.SupportsPrintTimeCalculation;
import celtech.gcodetranslator.postprocessing.nodes.providers.ExtrusionProvider;
import celtech.gcodetranslator.postprocessing.nodes.providers.FeedrateProvider;
import celtech.gcodetranslator.postprocessing.nodes.providers.MovementProvider;
import celtech.gcodetranslator.postprocessing.nodes.providers.Renderable;
import celtech.utils.SystemUtils;
import celtech.utils.Time.TimeUtils;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import javafx.beans.property.DoubleProperty;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;
import org.parboiled.Parboiled;
import org.parboiled.parserunners.BasicParseRunner;
import org.parboiled.support.ParsingResult;

/**
 *
 * @author Ian
 */
public class PostProcessor
{

    private final Stenographer steno = StenographerFactory.getStenographer(PostProcessor.class.getName());

    private final String unretractTimerName = "Unretract";
    private final String orphanTimerName = "Orphans";
    private final String nozzleControlTimerName = "NozzleControl";
    private final String perRetractTimerName = "PerRetract";
    private final String closeTimerName = "Close";
    private final String unnecessaryToolchangeTimerName = "UnnecessaryToolchange";
    private final String openTimerName = "Open";
    private final String assignExtrusionTimerName = "AssignExtrusion";
    private final String layerResultTimerName = "LayerResult";
    private final String parseLayerTimerName = "ParseLayer";
    private final String writeOutputTimerName = "WriteOutput";
    private final String countLinesTimerName = "CountLines";

    private final String gcodeFileToProcess;
    private final String gcodeOutputFile;
    private final HeadFile headFile;
    private final Project project;
    private final SlicerParametersFile slicerParametersFile;
    private final DoubleProperty taskProgress;

    private final List<NozzleProxy> nozzleProxies = new ArrayList<>();

    private final PostProcessorFeatureSet featureSet;

    private PostProcessingMode postProcessingMode = PostProcessingMode.TASK_BASED_NOZZLE_SELECTION;

    protected List<Integer> layerNumberToLineNumber;
    protected List<Double> layerNumberToPredictedDuration;
    protected double predictedDuration = 0;

    private final UtilityMethods postProcessorUtilityMethods;
    private final NodeManagementUtilities nodeManagementUtilities;
    private final NozzleAssignmentUtilities nozzleControlUtilities;
    private final CloseLogic closeLogic;
    private final NozzleManagementUtilities nozzleUtilities;

    private final TimeUtils timeUtils = new TimeUtils();

    public PostProcessor(String gcodeFileToProcess,
            String gcodeOutputFile,
            HeadFile headFile,
            Project project,
            SlicerParametersFile settings,
            PostProcessorFeatureSet postProcessorFeatureSet,
            String headType,
            DoubleProperty taskProgress)
    {
        this.gcodeFileToProcess = gcodeFileToProcess;
        this.gcodeOutputFile = gcodeOutputFile;
        this.headFile = headFile;
        this.project = project;
        this.featureSet = postProcessorFeatureSet;

        this.slicerParametersFile = settings;

        this.taskProgress = taskProgress;

        nozzleProxies.clear();

        for (int nozzleIndex = 0;
                nozzleIndex < slicerParametersFile.getNozzleParameters()
                .size(); nozzleIndex++)
        {
            NozzleProxy proxy = new NozzleProxy(slicerParametersFile.getNozzleParameters().get(nozzleIndex));
            proxy.setNozzleReferenceNumber(nozzleIndex);
            nozzleProxies.add(proxy);
        }

        if (headFile.getTypeCode().equals("RBX01-DM"))
        {
            switch (project.getPrinterSettings().getPrintSupportOverride())
            {
                case NO_SUPPORT:
                case OBJECT_MATERIAL:
                    postProcessingMode = PostProcessingMode.USE_OBJECT_MATERIAL;
                    break;
                case MATERIAL_1:
                    postProcessingMode = PostProcessingMode.SUPPORT_IN_FIRST_MATERIAL;
                    break;
                case MATERIAL_2:
                    postProcessingMode = PostProcessingMode.SUPPORT_IN_SECOND_MATERIAL;
                    break;
            }
        } else
        {
            postProcessingMode = PostProcessingMode.TASK_BASED_NOZZLE_SELECTION;
        }

        postProcessorUtilityMethods = new UtilityMethods(featureSet, project, settings, headType);
        nodeManagementUtilities = new NodeManagementUtilities(featureSet);
        nozzleControlUtilities = new NozzleAssignmentUtilities(nozzleProxies, slicerParametersFile, headFile, featureSet, project, postProcessingMode);
        closeLogic = new CloseLogic(project, slicerParametersFile, featureSet, headType);
        nozzleUtilities = new NozzleManagementUtilities(nozzleProxies, slicerParametersFile, headFile);
    }

    public RoboxiserResult processInput()
    {
        RoboxiserResult result = new RoboxiserResult();

        BufferedReader fileReader = null;
        GCodeOutputWriter writer = null;

        float finalEVolume = 0;
        float finalDVolume = 0;
        double timeForPrint_secs = 0;

        layerNumberToLineNumber = new ArrayList<>();
        layerNumberToPredictedDuration = new ArrayList<>();

        predictedDuration = 0;

        int layerCounter = -1;

        OutputUtilities outputUtilities = new OutputUtilities();

        timeUtils.timerStart(this, "PostProcessor");
        steno.info("Beginning post-processing operation");

        //Cura has line delineators like this ';LAYER:1'
        try
        {
            File inputFile = new File(gcodeFileToProcess);
            timeUtils.timerStart(this, countLinesTimerName);
            int linesInGCodeFile = SystemUtils.countLinesInFile(inputFile);
            timeUtils.timerStop(this, countLinesTimerName);

            int linesRead = 0;
            double lastPercentSoFar = 0;

            fileReader = new BufferedReader(new FileReader(inputFile));

            writer = Lookup.getPostProcessorOutputWriterFactory().create(gcodeOutputFile);

            outputUtilities.prependPrePrintHeader(writer);

            StringBuilder layerBuffer = new StringBuilder();
            LayerPostProcessResult lastLayerParseResult = new LayerPostProcessResult(Optional.empty(), null, 0, 0, 0, 0, null, null, -1);

            for (String lineRead = fileReader.readLine(); lineRead != null; lineRead = fileReader.readLine())
            {
                linesRead++;
                double percentSoFar = ((double) linesRead / (double) linesInGCodeFile) * 100;
                if (percentSoFar - lastPercentSoFar >= 1)
                {
                    if (taskProgress != null)
                    {
                        taskProgress.set(percentSoFar);
                    }
                    lastPercentSoFar = percentSoFar;
                }

                lineRead = lineRead.trim();
                if (lineRead.matches(";LAYER:[0-9]+"))
                {
                    if (layerCounter >= 0)
                    {
                        //Parse anything that has gone before
                        LayerPostProcessResult parseResult = parseLayer(layerBuffer, lastLayerParseResult, writer);
                        finalEVolume += parseResult.getEVolume();
                        finalDVolume += parseResult.getDVolume();
                        timeForPrint_secs += parseResult.getTimeForLayer();

                        //Now output the LAST layer - it was held until now in case it needed to be modified before output
                        timeUtils.timerStart(this, writeOutputTimerName);
                        outputUtilities.writeLayerToFile(lastLayerParseResult.getLayerData(), writer);
                        timeUtils.timerStop(this, writeOutputTimerName);
                        postProcessorUtilityMethods.updateLayerToLineNumber(lastLayerParseResult, layerNumberToLineNumber, writer);
                        predictedDuration += postProcessorUtilityMethods.updateLayerToPredictedDuration(lastLayerParseResult, layerNumberToPredictedDuration, writer);

                        if (parseResult.getNozzleStateAtEndOfLayer().isPresent())
                        {
                            lastLayerParseResult = parseResult;
                            if (lastLayerParseResult.getLayerData().getLayerNumber() == 1)
                            {
                                outputUtilities.outputTemperatureCommands(writer);
                            }
                        } else
                        {
                            lastLayerParseResult = new LayerPostProcessResult(lastLayerParseResult.getNozzleStateAtEndOfLayer(),
                                    parseResult.getLayerData(),
                                    parseResult.getEVolume(),
                                    parseResult.getDVolume(),
                                    parseResult.getTimeForLayer(),
                                    parseResult.getLastObjectNumber().orElse(-1),
                                    null,
                                    null,
                                    lastLayerParseResult.getLastFeedrateInForce());

                        }
                    }

                    layerCounter++;
                    layerBuffer = new StringBuilder();
                    // Make sure this layer command is at the start
                    layerBuffer.append(lineRead);
                    layerBuffer.append('\n');
                } else if (!lineRead.equals(""))
                {
                    //Ignore blank lines
                    // stash it in the buffer
                    layerBuffer.append(lineRead);
                    layerBuffer.append('\n');
                }
            }

            //This catches the last layer - if we had no data it won't do anything
            LayerPostProcessResult parseResult = parseLayer(layerBuffer, lastLayerParseResult, writer);

            finalEVolume += parseResult.getEVolume();
            finalDVolume += parseResult.getDVolume();
            timeForPrint_secs += parseResult.getTimeForLayer();

            //Now output the LAST layer - it was held until now in case it needed to be modified before output
            timeUtils.timerStart(this, writeOutputTimerName);
            outputUtilities.writeLayerToFile(lastLayerParseResult.getLayerData(), writer);
            timeUtils.timerStop(this, writeOutputTimerName);
            postProcessorUtilityMethods.updateLayerToLineNumber(lastLayerParseResult, layerNumberToLineNumber, writer);
            predictedDuration += postProcessorUtilityMethods.updateLayerToPredictedDuration(lastLayerParseResult, layerNumberToPredictedDuration, writer);

            //Now output the final result
            timeUtils.timerStart(this, writeOutputTimerName);
            outputUtilities.writeLayerToFile(parseResult.getLayerData(), writer);
            timeUtils.timerStop(this, writeOutputTimerName);
            postProcessorUtilityMethods.updateLayerToLineNumber(parseResult, layerNumberToLineNumber, writer);
            predictedDuration += postProcessorUtilityMethods.updateLayerToPredictedDuration(lastLayerParseResult, layerNumberToPredictedDuration, writer);

            timeUtils.timerStart(this, writeOutputTimerName);
            outputUtilities.appendPostPrintFooter(writer, finalEVolume, finalDVolume, timeForPrint_secs);
            timeUtils.timerStop(this, writeOutputTimerName);

            /**
             * TODO: layerNumberToLineNumber uses lines numbers from the GCode
             * file so are a little less than the line numbers for each layer
             * after roboxisation. As a quick fix for now set the line number of
             * the last layer to the actual maximum line number.
             */
            layerNumberToLineNumber.set(layerNumberToLineNumber.size() - 1,
                    writer.getNumberOfLinesOutput());
            int numLines = writer.getNumberOfLinesOutput();

            PrintJobStatistics roboxisedStatistics = new PrintJobStatistics(
                    numLines,
                    finalEVolume,
                    finalDVolume,
                    0,
                    layerNumberToLineNumber,
                    layerNumberToPredictedDuration,
                    predictedDuration);

            result.setRoboxisedStatistics(roboxisedStatistics);

            outputPostProcessingTimerReport();

            timeUtils.timerStop(this, "PostProcessor");
            steno.info("Post-processing took " + timeUtils.timeTimeSoFar_ms(this, "PostProcessor") + "ms");

            result.setSuccess(true);
        } catch (IOException ex)
        {
            steno.error("Error reading post-processor input file: " + gcodeFileToProcess);
        } catch (RuntimeException ex)
        {
            if (ex.getCause() != null)
            {
                steno.error("Fatal postprocessing error on layer " + layerCounter + " got exception: " + ex.getCause().getMessage());
            } else
            {
                steno.error("Fatal postprocessing error on layer " + layerCounter);
            }
            ex.printStackTrace();
        } finally
        {
            if (fileReader != null)
            {
                try
                {
                    fileReader.close();
                } catch (IOException ex)
                {
                    steno.error("Failed to close post processor input file - " + gcodeFileToProcess);
                }
            }

            if (writer != null)
            {
                try
                {
                    writer.close();
                } catch (IOException ex)
                {
                    steno.error("Failed to close post processor output file - " + gcodeOutputFile);
                }
            }
        }

        return result;
    }

    private LayerPostProcessResult parseLayer(StringBuilder layerBuffer, LayerPostProcessResult lastLayerParseResult, GCodeOutputWriter writer)
    {
        LayerPostProcessResult parseResultAtEndOfThisLayer = null;

        // Parse the last layer if it exists...
        if (layerBuffer.length() > 0)
        {
            GCodeParser gcodeParser = Parboiled.createParser(GCodeParser.class
            );

            if (lastLayerParseResult != null)
            {
                gcodeParser.setFeedrateInForce(lastLayerParseResult.getLastFeedrateInForce());
            }

            BasicParseRunner runner = new BasicParseRunner<>(gcodeParser.Layer());
            timeUtils.timerStart(this, parseLayerTimerName);
            ParsingResult result = runner.run(layerBuffer.toString());
            timeUtils.timerStop(this, parseLayerTimerName);

            if (result.hasErrors() || !result.matched)
            {
                throw new RuntimeException("Parsing failure");
            } else
            {
                LayerNode layerNode = gcodeParser.getLayerNode();
                int lastFeedrate = gcodeParser.getFeedrateInForce();
                parseResultAtEndOfThisLayer = postProcess(layerNode, lastLayerParseResult);
                parseResultAtEndOfThisLayer.setLastFeedrateInForce(lastFeedrate);
            }
        } else
        {
            parseResultAtEndOfThisLayer = lastLayerParseResult;
        }

        return parseResultAtEndOfThisLayer;
    }

    private LayerPostProcessResult postProcess(LayerNode layerNode, LayerPostProcessResult lastLayerParseResult)
    {
        // We never want unretracts
        timeUtils.timerStart(this, unretractTimerName);
        nodeManagementUtilities.removeUnretractNodes(layerNode);
        timeUtils.timerStop(this, unretractTimerName);

        timeUtils.timerStart(this, orphanTimerName);
        nodeManagementUtilities.rehomeOrphanObjects(layerNode, lastLayerParseResult);
        timeUtils.timerStop(this, orphanTimerName);

        int lastObjectNumber = -1;

        timeUtils.timerStart(this, nozzleControlTimerName);
        switch (postProcessingMode)
        {
            case TASK_BASED_NOZZLE_SELECTION:
                lastObjectNumber = nozzleControlUtilities.insertNozzleControlSectionsByTask(layerNode, lastLayerParseResult);
                break;
            case USE_OBJECT_MATERIAL:
                lastObjectNumber = nozzleControlUtilities.insertNozzleControlSectionsByObject(layerNode, lastLayerParseResult);
                break;
            default:
                break;
        }
        timeUtils.timerStop(this, nozzleControlTimerName);

        timeUtils.timerStart(this, perRetractTimerName);
        nodeManagementUtilities.calculatePerRetractExtrusionAndNode(layerNode);
        timeUtils.timerStop(this, perRetractTimerName);

        timeUtils.timerStart(this, closeTimerName);
        closeLogic.insertCloseNodes(layerNode, lastLayerParseResult, nozzleProxies);
        timeUtils.timerStop(this, closeTimerName);

        timeUtils.timerStart(this, unnecessaryToolchangeTimerName);
        postProcessorUtilityMethods.suppressUnnecessaryToolChangesAndInsertToolchangeCloses(layerNode, lastLayerParseResult, nozzleProxies);
        timeUtils.timerStop(this, unnecessaryToolchangeTimerName);

        //NEED CODE TO ADD CLOSE AT END OF LAST LAYER IF NOT ALREADY THERE
        timeUtils.timerStart(this, openTimerName);
        postProcessorUtilityMethods.insertOpenNodes(layerNode, lastLayerParseResult);
        timeUtils.timerStop(this, openTimerName);

        timeUtils.timerStart(this, assignExtrusionTimerName);
        nozzleControlUtilities.assignExtrusionToCorrectExtruder(layerNode);
        timeUtils.timerStop(this, assignExtrusionTimerName);

        timeUtils.timerStart(this, layerResultTimerName);
        LayerPostProcessResult postProcessResult = determineLayerPostProcessResult(layerNode, lastLayerParseResult);
        postProcessResult.setLastObjectNumber(lastObjectNumber);
        timeUtils.timerStop(this, layerResultTimerName);

        return postProcessResult;
    }

    private LayerPostProcessResult determineLayerPostProcessResult(LayerNode layerNode, LayerPostProcessResult lastLayerPostProcessResult)
    {
        Optional<NozzleProxy> lastNozzleInUse = nozzleUtilities.determineNozzleStateAtEndOfLayer(layerNode);

        Iterator<GCodeEventNode> layerIterator = layerNode.treeSpanningIterator(null);

        float eValue = 0;
        float dValue = 0;
        double timeForLayer = 0;
        int lastFeedrate = -1;

        SupportsPrintTimeCalculation lastMovementProvider = null;

        while (layerIterator.hasNext())
        {
            GCodeEventNode foundNode = layerIterator.next();

            if (foundNode instanceof ExtrusionProvider)
            {
                ExtrusionProvider extrusionProvider = (ExtrusionProvider) foundNode;
                eValue += extrusionProvider.getExtrusion().getE();
                dValue += extrusionProvider.getExtrusion().getD();
            }

            if (foundNode instanceof SupportsPrintTimeCalculation)
            {
                SupportsPrintTimeCalculation timeCalculationNode = (SupportsPrintTimeCalculation) foundNode;

                if (lastMovementProvider != null)
                {
                    try
                    {
                        double time = lastMovementProvider.timeToReach((MovementProvider) foundNode);
                        timeForLayer += time;
                    } catch (DurationCalculationException ex)
                    {
                        if (ex.getFromNode() instanceof Renderable
                                && ex.getToNode() instanceof Renderable)
                        {
                            steno.error("Unable to calculate duration correctly for nodes source:"
                                    + ((Renderable) ex.getFromNode()).renderForOutput()
                                    + " destination:"
                                    + ((Renderable) ex.getToNode()).renderForOutput());
                        } else
                        {
                            steno.error("Unable to calculate duration correctly for nodes source:"
                                    + ex.getFromNode().getMovement().renderForOutput()
                                    + " destination:"
                                    + ex.getToNode().getMovement().renderForOutput());
                        }

                        throw new RuntimeException("Unable to calculate duration correctly on layer "
                                + layerNode.getLayerNumber(), ex);
                    }
                }
                lastMovementProvider = timeCalculationNode;
                if (((FeedrateProvider) lastMovementProvider).getFeedrate().getFeedRate_mmPerMin() < 0)
                {
                    ((FeedrateProvider) lastMovementProvider).getFeedrate().setFeedRate_mmPerMin(lastLayerPostProcessResult.getLastFeedrateInForce());
                }
                lastFeedrate = ((FeedrateProvider)lastMovementProvider).getFeedrate().getFeedRate_mmPerMin();
            }
        }

        SectionNode lastSectionNode = null;
        ToolSelectNode lastToolSelectNode = null;

        if (layerNode != null)
        {
            GCodeEventNode lastEventOnLastLayer = layerNode.getAbsolutelyTheLastEvent();
            if (lastEventOnLastLayer != null)
            {
                Optional<GCodeEventNode> potentialLastSection = lastEventOnLastLayer.getParent();
                if (potentialLastSection.isPresent() && potentialLastSection.get() instanceof SectionNode)
                {
                    lastSectionNode = (SectionNode) potentialLastSection.get();

                    Optional<GCodeEventNode> potentialToolSelectNode = lastSectionNode.getParent();
                    if (potentialToolSelectNode.isPresent() && potentialToolSelectNode.get() instanceof ToolSelectNode)
                    {
                        lastToolSelectNode = (ToolSelectNode) potentialToolSelectNode.get();
                    }
                }
            }
        }

        if (lastSectionNode == null)
        {
            lastSectionNode = lastLayerPostProcessResult.getLastSectionNodeInForce();
        }

        if (lastToolSelectNode == null)
        {
            lastToolSelectNode = lastLayerPostProcessResult.getLastToolSelectInForce();
        }

        return new LayerPostProcessResult(lastNozzleInUse, layerNode, eValue, dValue, timeForLayer, -1,
                lastSectionNode, lastToolSelectNode, lastFeedrate);
    }

    private void outputPostProcessingTimerReport()
    {
        steno.info("Post Processor Timer Report");
        steno.info("============");
        steno.info(unretractTimerName + " " + timeUtils.timeTimeSoFar_ms(this, unretractTimerName));
        steno.info(orphanTimerName + " " + timeUtils.timeTimeSoFar_ms(this, orphanTimerName));
        steno.info(nozzleControlTimerName + " " + timeUtils.timeTimeSoFar_ms(this, nozzleControlTimerName));
        steno.info(perRetractTimerName + " " + timeUtils.timeTimeSoFar_ms(this, perRetractTimerName));
        steno.info(closeTimerName + " " + timeUtils.timeTimeSoFar_ms(this, closeTimerName));
        steno.info(unnecessaryToolchangeTimerName + " " + timeUtils.timeTimeSoFar_ms(this, unnecessaryToolchangeTimerName));
        steno.info(openTimerName + " " + timeUtils.timeTimeSoFar_ms(this, openTimerName));
        steno.info(assignExtrusionTimerName + " " + timeUtils.timeTimeSoFar_ms(this, assignExtrusionTimerName));
        steno.info(layerResultTimerName + " " + timeUtils.timeTimeSoFar_ms(this, layerResultTimerName));
        steno.info(parseLayerTimerName + " " + timeUtils.timeTimeSoFar_ms(this, parseLayerTimerName));
        steno.info(writeOutputTimerName + " " + timeUtils.timeTimeSoFar_ms(this, writeOutputTimerName));
        steno.info("============");
    }
}
