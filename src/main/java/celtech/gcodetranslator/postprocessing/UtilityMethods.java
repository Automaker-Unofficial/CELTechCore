package celtech.gcodetranslator.postprocessing;

import celtech.appManager.Project;
import celtech.gcodetranslator.GCodeOutputWriter;
import celtech.gcodetranslator.NozzleProxy;
import celtech.gcodetranslator.postprocessing.nodes.ExtrusionNode;
import celtech.gcodetranslator.postprocessing.nodes.GCodeEventNode;
import celtech.gcodetranslator.postprocessing.nodes.LayerNode;
import celtech.gcodetranslator.postprocessing.nodes.NodeProcessingException;
import celtech.gcodetranslator.postprocessing.nodes.NozzleValvePositionNode;
import celtech.gcodetranslator.postprocessing.nodes.ReplenishNode;
import celtech.gcodetranslator.postprocessing.nodes.ToolSelectNode;
import celtech.gcodetranslator.postprocessing.nodes.providers.NozzlePosition;
import celtech.gcodetranslator.postprocessing.nodes.providers.NozzlePositionProvider;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 *
 * @author Ian
 */
public class UtilityMethods
{

    private final PostProcessorFeatureSet ppFeatureSet;
    private final NodeManagementUtilities nodeManagementUtilities;
    private final CloseLogic closeLogic;

    public UtilityMethods(final PostProcessorFeatureSet ppFeatureSet,
            final Project project)
    {
        this.ppFeatureSet = ppFeatureSet;
        nodeManagementUtilities = new NodeManagementUtilities(ppFeatureSet);
        this.closeLogic = new CloseLogic(project, ppFeatureSet);
    }

    protected void suppressUnnecessaryToolChangesAndInsertToolchangeCloses(LayerNode layerNode,
            LayerPostProcessResult lastLayerPostProcessResult,
            List<NozzleProxy> nozzleProxies)
    {
        List<ToolSelectNode> toolSelectNodes = layerNode.stream()
                .filter(node -> node instanceof ToolSelectNode)
                .map(ToolSelectNode.class::cast)
                .collect(Collectors.toList());

        int lastToolNumber = -1;

        if (lastLayerPostProcessResult.getNozzleStateAtEndOfLayer()
                .isPresent())
        {
            lastToolNumber = lastLayerPostProcessResult.getNozzleStateAtEndOfLayer().get().getNozzleReferenceNumber();
        }

        for (ToolSelectNode toolSelectNode : toolSelectNodes)
        {
            if (lastToolNumber == toolSelectNode.getToolNumber())
            {
                toolSelectNode.suppressNodeOutput(true);
            } else
            {
                // The tool number has changed
                // Close the nozzle if it isn't already...
                //Insert a close at the end if there isn't already a close following the last extrusion
                Optional<ExtrusionNode> lastExtrusion = toolSelectNode.streamChildrenAndMeBackwards()
                        .filter(node -> node instanceof ExtrusionNode)
                        .findFirst()
                        .map(ExtrusionNode.class::cast);

                if (lastExtrusion.isPresent())
                {
                    if (!lastExtrusion.get().getNozzlePosition().isBSet()
                            || lastExtrusion.get().getNozzlePosition().getB() > 0)
                    {
                        try
                        {
                            //We need to close
                            double availableExtrusion = nodeManagementUtilities.findAvailableExtrusion(lastExtrusion.get(), false);

                            closeLogic.insertNozzleCloses(availableExtrusion, lastExtrusion.get(), nozzleProxies.get(toolSelectNode.getToolNumber()));

                        } catch (NodeProcessingException ex)
                        {
                            throw new RuntimeException("Error locating available extrusion during tool select normalisation", ex);
                        }
                    }
                }
            }

            lastToolNumber = toolSelectNode.getToolNumber();
        }
    }

    protected void insertNozzleOpenFullyBeforeEvent(ExtrusionNode node)
    {
        // Insert a replenish if required
        if (ppFeatureSet.isEnabled(PostProcessorFeature.REPLENISH_BEFORE_OPEN))
        {
            if (node.getElidedExtrusion() > 0)
            {
                ReplenishNode replenishNode = new ReplenishNode();
                replenishNode.getExtrusion().setE((float) node.getElidedExtrusion());
                replenishNode.setCommentText("Replenishing elided extrusion");
                node.addSiblingBefore(replenishNode);
            }
        }

        NozzleValvePositionNode newNozzleValvePositionNode = new NozzleValvePositionNode();
        newNozzleValvePositionNode.getNozzlePosition().setB(1);
        node.addSiblingBefore(newNozzleValvePositionNode);
    }

    /**
     * A 'brute force' walkthrough of all nozzle position providers This method
     * inserts full opens if it encounters extrusion nodes without B position
     * which are preceded by a partially open or closed nozzle
     *
     * @param layerNode
     * @param lastLayerPostProcessResult
     *
     */
    protected void insertOpenNodes(final LayerNode layerNode, final LayerPostProcessResult lastLayerPostProcessResult)
    {
        List<ToolSelectNode> toolSelectNodes = layerNode.stream()
                .filter(node -> node instanceof ToolSelectNode)
                .map(ToolSelectNode.class::cast)
                .collect(Collectors.toList());

        int lastNozzleNumber = -1;

        for (ToolSelectNode toolSelectNode : toolSelectNodes)
        {
            List<NozzlePositionProvider> nozzlePositionProviders = toolSelectNode.stream()
                    .filter(node -> node instanceof NozzlePositionProvider)
                    .map(NozzlePositionProvider.class::cast)
                    .collect(Collectors.toList());

            double lastBPosition = 0;

            if (lastNozzleNumber != toolSelectNode.getToolNumber())
            {
                for (NozzlePositionProvider nozzlePositionProvider : nozzlePositionProviders)
                {
                    NozzlePosition nozzlePosition = (NozzlePosition) nozzlePositionProvider.getNozzlePosition();
                    if (nozzlePosition.isBSet())
                    {
                        lastBPosition = nozzlePosition.getB();
                    } else if (nozzlePositionProvider instanceof ExtrusionNode)
                    {
                        if (lastBPosition < 1)
                        {
                            //The nozzle needs to be opened
                            try
                            {
                                GCodeEventNode nextExtrusionNode = nodeManagementUtilities.findNextExtrusion((GCodeEventNode) nozzlePositionProvider).orElseThrow(NodeProcessingException::new);
                                insertNozzleOpenFullyBeforeEvent((ExtrusionNode) nextExtrusionNode);
                                lastBPosition = 1.0;
                            } catch (NodeProcessingException ex)
                            {
                                throw new RuntimeException("Failed to insert open nodes on layer " + layerNode.getLayerNumber(), ex);
                            }
                        }
                    }
                }
            }

            lastNozzleNumber = toolSelectNode.getToolNumber();
        }
    }

    protected void updateLayerToLineNumber(LayerPostProcessResult lastLayerParseResult,
            List<Integer> layerNumberToLineNumber,
            GCodeOutputWriter writer)
    {
        if (lastLayerParseResult.getLayerData()
                != null)
        {
            int layerNumber = lastLayerParseResult.getLayerData().getLayerNumber();
            if (layerNumber >= 0)
            {
                layerNumberToLineNumber.add(layerNumber, writer.getNumberOfLinesOutput());
            }
        }
    }

    protected double updateLayerToPredictedDuration(LayerPostProcessResult lastLayerParseResult,
            List<Double> layerNumberToPredictedDuration,
            GCodeOutputWriter writer)
    {
        double predictedDuration = 0;

        if (lastLayerParseResult.getLayerData() != null)
        {
            int layerNumber = lastLayerParseResult.getLayerData().getLayerNumber();
            if (layerNumber >= 0)
            {
                layerNumberToPredictedDuration.add(layerNumber, lastLayerParseResult.getTimeForLayer());
                predictedDuration += lastLayerParseResult.getTimeForLayer();
            }
        }

        return predictedDuration;
    }
}
