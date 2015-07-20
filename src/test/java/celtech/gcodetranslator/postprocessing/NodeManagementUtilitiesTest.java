package celtech.gcodetranslator.postprocessing;

import celtech.JavaFXConfiguredTest;
import celtech.appManager.Project;
import celtech.gcodetranslator.postprocessing.nodes.ExtrusionNode;
import celtech.gcodetranslator.postprocessing.nodes.FillSectionNode;
import celtech.gcodetranslator.postprocessing.nodes.LayerNode;
import celtech.gcodetranslator.postprocessing.nodes.NodeProcessingException;
import celtech.gcodetranslator.postprocessing.nodes.ObjectDelineationNode;
import celtech.gcodetranslator.postprocessing.nodes.OrphanObjectDelineationNode;
import celtech.gcodetranslator.postprocessing.nodes.OuterPerimeterSectionNode;
import celtech.gcodetranslator.postprocessing.nodes.RetractNode;
import celtech.gcodetranslator.postprocessing.nodes.ToolSelectNode;
import celtech.gcodetranslator.postprocessing.nodes.UnretractNode;
import celtech.gcodetranslator.postprocessing.nodes.providers.MovementProvider;
import celtech.services.slicer.PrintQualityEnumeration;
import java.util.Optional;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Ian
 */
public class NodeManagementUtilitiesTest extends JavaFXConfiguredTest
{

    @Test
    public void testRemoveUnretractNodes()
    {
        LayerNode testLayer = new LayerNode();
        UnretractNode unretractNode1 = new UnretractNode();
        UnretractNode unretractNode2 = new UnretractNode();
        UnretractNode unretractNode3 = new UnretractNode();

        testLayer.addChildAtEnd(unretractNode1);
        testLayer.addChildAtEnd(unretractNode2);
        testLayer.addChildAtEnd(unretractNode3);

        PostProcessorFeatureSet ppFeatures = new PostProcessorFeatureSet();
        ppFeatures.enableFeature(PostProcessorFeature.REMOVE_ALL_UNRETRACTS);
        ppFeatures.enableFeature(PostProcessorFeature.OPEN_NOZZLE_FULLY_AT_START);
        ppFeatures.enableFeature(PostProcessorFeature.CLOSES_ON_RETRACT);
        ppFeatures.enableFeature(PostProcessorFeature.CLOSE_ON_TASK_CHANGE);

        NodeManagementUtilities nodeManagementUtilities = new NodeManagementUtilities(ppFeatures);

        assertEquals(3, testLayer.getChildren().size());

        nodeManagementUtilities.removeUnretractNodes(testLayer);

        assertEquals(0, testLayer.getChildren().size());
    }

    @Test
    public void testRehomeOrphanObjects_startOfFile()
    {
        LayerNode testLayer = new LayerNode();
        OrphanObjectDelineationNode orphan1 = new OrphanObjectDelineationNode();
        orphan1.setPotentialObjectNumber(0);

        ObjectDelineationNode object1 = new ObjectDelineationNode();
        object1.setObjectNumber(1);

        OuterPerimeterSectionNode outer = new OuterPerimeterSectionNode();
        FillSectionNode fill = new FillSectionNode();

        ExtrusionNode extrusionNode1 = new ExtrusionNode();
        ExtrusionNode extrusionNode2 = new ExtrusionNode();
        ExtrusionNode extrusionNode3 = new ExtrusionNode();
        ExtrusionNode extrusionNode4 = new ExtrusionNode();
        ExtrusionNode extrusionNode5 = new ExtrusionNode();
        ExtrusionNode extrusionNode6 = new ExtrusionNode();

        outer.addChildAtEnd(extrusionNode1);
        outer.addChildAtEnd(extrusionNode2);
        outer.addChildAtEnd(extrusionNode3);

        fill.addChildAtEnd(extrusionNode4);
        fill.addChildAtEnd(extrusionNode5);
        fill.addChildAtEnd(extrusionNode6);

        orphan1.addChildAtEnd(outer);
        object1.addChildAtEnd(fill);

        testLayer.addChildAtEnd(orphan1);
        testLayer.addChildAtEnd(object1);

        PostProcessorFeatureSet ppFeatures = new PostProcessorFeatureSet();
        ppFeatures.enableFeature(PostProcessorFeature.REMOVE_ALL_UNRETRACTS);
        ppFeatures.enableFeature(PostProcessorFeature.OPEN_NOZZLE_FULLY_AT_START);
        ppFeatures.enableFeature(PostProcessorFeature.CLOSES_ON_RETRACT);
        ppFeatures.enableFeature(PostProcessorFeature.CLOSE_ON_TASK_CHANGE);

        NodeManagementUtilities nodeManagementUtilities = new NodeManagementUtilities(ppFeatures);
        LayerPostProcessResult lastLayerParseResult = new LayerPostProcessResult(Optional.empty(), testLayer, 0, 0, 0, 0, null, null, -1);

        assertEquals(2, testLayer.getChildren().size());
        assertSame(outer, orphan1.getChildren().get(0));

        nodeManagementUtilities.rehomeOrphanObjects(testLayer, lastLayerParseResult);

        assertEquals(2, testLayer.getChildren().size());
        assertTrue(testLayer.getChildren().get(0) instanceof ObjectDelineationNode);
        ObjectDelineationNode resultNode = (ObjectDelineationNode) testLayer.getChildren().get(0);
        assertEquals(0, resultNode.getObjectNumber());

        assertSame(outer, resultNode.getChildren().get(0));

        assertSame(object1, testLayer.getChildren().get(1));
        assertSame(fill, object1.getChildren().get(0));
    }

    @Test
    public void testRehomeOrphanObjects_sameObjectAsLastLayer()
    {
        LayerNode testLayer = new LayerNode();
        testLayer.setLayerNumber(0);
        OrphanObjectDelineationNode orphan1 = new OrphanObjectDelineationNode();
        orphan1.setPotentialObjectNumber(10);

        ObjectDelineationNode object1 = new ObjectDelineationNode();
        object1.setObjectNumber(0);

        OuterPerimeterSectionNode outer = new OuterPerimeterSectionNode();
        FillSectionNode fill = new FillSectionNode();

        ExtrusionNode extrusionNode1 = new ExtrusionNode();
        ExtrusionNode extrusionNode2 = new ExtrusionNode();
        ExtrusionNode extrusionNode3 = new ExtrusionNode();
        ExtrusionNode extrusionNode4 = new ExtrusionNode();
        ExtrusionNode extrusionNode5 = new ExtrusionNode();
        ExtrusionNode extrusionNode6 = new ExtrusionNode();

        outer.addChildAtEnd(extrusionNode1);
        outer.addChildAtEnd(extrusionNode2);
        outer.addChildAtEnd(extrusionNode3);

        fill.addChildAtEnd(extrusionNode4);
        fill.addChildAtEnd(extrusionNode5);
        fill.addChildAtEnd(extrusionNode6);

        orphan1.addChildAtEnd(outer);
        object1.addChildAtEnd(fill);

        testLayer.addChildAtEnd(orphan1);
        testLayer.addChildAtEnd(object1);

        PostProcessorFeatureSet ppFeatures = new PostProcessorFeatureSet();
        ppFeatures.enableFeature(PostProcessorFeature.REMOVE_ALL_UNRETRACTS);
        ppFeatures.enableFeature(PostProcessorFeature.OPEN_NOZZLE_FULLY_AT_START);
        ppFeatures.enableFeature(PostProcessorFeature.CLOSES_ON_RETRACT);
        ppFeatures.enableFeature(PostProcessorFeature.CLOSE_ON_TASK_CHANGE);

        Project testProject = new Project();
        testProject.getPrinterSettings().setSettingsName("BothNozzles");
        testProject.setPrintQuality(PrintQualityEnumeration.CUSTOM);

        NodeManagementUtilities nodeManagementUtilities = new NodeManagementUtilities(ppFeatures);
        LayerPostProcessResult lastLayerParseResult = new LayerPostProcessResult(Optional.empty(), testLayer, 0, 0, 0, 10, null, null, -1);

        assertEquals(2, testLayer.getChildren().size());
        assertSame(outer, orphan1.getChildren().get(0));

        nodeManagementUtilities.rehomeOrphanObjects(testLayer, lastLayerParseResult);

        assertEquals(2, testLayer.getChildren().size());
        assertTrue(testLayer.getChildren().get(0) instanceof ObjectDelineationNode);
        ObjectDelineationNode resultNode = (ObjectDelineationNode) testLayer.getChildren().get(0);
        assertEquals(10, resultNode.getObjectNumber());

        assertSame(outer, resultNode.getChildren().get(0));

        assertSame(object1, testLayer.getChildren().get(1));
        assertSame(fill, object1.getChildren().get(0));
    }

    @Test
    public void testFindAvailableExtrusion_forwards()
    {
        ExtrusionNode extrusionNode1 = new ExtrusionNode();
        extrusionNode1.getExtrusion().setE(1.0f);

        ExtrusionNode extrusionNode2 = new ExtrusionNode();
        extrusionNode2.getExtrusion().setE(2.0f);

        ExtrusionNode extrusionNode3 = new ExtrusionNode();
        extrusionNode3.getExtrusion().setE(3.0f);

        ExtrusionNode extrusionNode4 = new ExtrusionNode();
        extrusionNode4.getExtrusion().setE(4.0f);

        ExtrusionNode extrusionNode5 = new ExtrusionNode();
        extrusionNode5.getExtrusion().setE(5.0f);

        ExtrusionNode extrusionNode6 = new ExtrusionNode();
        extrusionNode6.getExtrusion().setE(6.0f);

        FillSectionNode fill1 = new FillSectionNode();
        fill1.addChildAtEnd(extrusionNode1);
        fill1.addChildAtEnd(extrusionNode2);
        fill1.addChildAtEnd(extrusionNode3);
        fill1.addChildAtEnd(extrusionNode4);
        fill1.addChildAtEnd(extrusionNode5);
        fill1.addChildAtEnd(extrusionNode6);

        PostProcessorFeatureSet ppFeatures = new PostProcessorFeatureSet();
        ppFeatures.enableFeature(PostProcessorFeature.REMOVE_ALL_UNRETRACTS);
        ppFeatures.enableFeature(PostProcessorFeature.OPEN_NOZZLE_FULLY_AT_START);
        ppFeatures.enableFeature(PostProcessorFeature.CLOSES_ON_RETRACT);
        ppFeatures.enableFeature(PostProcessorFeature.CLOSE_ON_TASK_CHANGE);

        NodeManagementUtilities nodeManagementUtilities = new NodeManagementUtilities(ppFeatures);

        try
        {
            double result1 = nodeManagementUtilities.findAvailableExtrusion(extrusionNode1, true);
            assertEquals(20, result1, 0.1);

            double result2 = nodeManagementUtilities.findAvailableExtrusion(extrusionNode4, true);
            assertEquals(11, result2, 0.1);

            double result3 = nodeManagementUtilities.findAvailableExtrusion(extrusionNode6, true);
            assertEquals(0, result3, 0.1);
        } catch (NodeProcessingException ex)
        {
            fail("Got exception during test " + ex);
        }
    }

    @Test
    public void testFindAvailableExtrusion_backwards()
    {
        ExtrusionNode extrusionNode1 = new ExtrusionNode();
        extrusionNode1.getExtrusion().setE(1.0f);

        ExtrusionNode extrusionNode2 = new ExtrusionNode();
        extrusionNode2.getExtrusion().setE(2.0f);

        ExtrusionNode extrusionNode3 = new ExtrusionNode();
        extrusionNode3.getExtrusion().setE(3.0f);

        ExtrusionNode extrusionNode4 = new ExtrusionNode();
        extrusionNode4.getExtrusion().setE(4.0f);

        ExtrusionNode extrusionNode5 = new ExtrusionNode();
        extrusionNode5.getExtrusion().setE(5.0f);

        ExtrusionNode extrusionNode6 = new ExtrusionNode();
        extrusionNode6.getExtrusion().setE(6.0f);

        FillSectionNode fill1 = new FillSectionNode();
        fill1.addChildAtEnd(extrusionNode1);
        fill1.addChildAtEnd(extrusionNode2);
        fill1.addChildAtEnd(extrusionNode3);
        fill1.addChildAtEnd(extrusionNode4);
        fill1.addChildAtEnd(extrusionNode5);
        fill1.addChildAtEnd(extrusionNode6);

        PostProcessorFeatureSet ppFeatures = new PostProcessorFeatureSet();
        ppFeatures.enableFeature(PostProcessorFeature.REMOVE_ALL_UNRETRACTS);
        ppFeatures.enableFeature(PostProcessorFeature.OPEN_NOZZLE_FULLY_AT_START);
        ppFeatures.enableFeature(PostProcessorFeature.CLOSES_ON_RETRACT);
        ppFeatures.enableFeature(PostProcessorFeature.CLOSE_ON_TASK_CHANGE);

        NodeManagementUtilities nodeManagementUtilities = new NodeManagementUtilities(ppFeatures);

        try
        {
            double result1 = nodeManagementUtilities.findAvailableExtrusion(extrusionNode1, false);
            assertEquals(0, result1, 0.1);

            double result2 = nodeManagementUtilities.findAvailableExtrusion(extrusionNode4, false);
            assertEquals(6, result2, 0.1);

            double result3 = nodeManagementUtilities.findAvailableExtrusion(extrusionNode6, false);
            assertEquals(15, result3, 0.1);
        } catch (NodeProcessingException ex)
        {
            fail("Got exception during test " + ex);
        }
    }

    @Test
    public void testNextExtrusion()
    {
        FillSectionNode fill1 = new FillSectionNode();

        ExtrusionNode extrusionNode1 = new ExtrusionNode();
        extrusionNode1.getExtrusion().setE(1.0f);

        ExtrusionNode extrusionNode2 = new ExtrusionNode();
        extrusionNode2.getExtrusion().setE(2.0f);

        ExtrusionNode extrusionNode3 = new ExtrusionNode();
        extrusionNode3.getExtrusion().setE(3.0f);

        ExtrusionNode extrusionNode4 = new ExtrusionNode();
        extrusionNode4.getExtrusion().setE(4.0f);

        ExtrusionNode extrusionNode5 = new ExtrusionNode();
        extrusionNode5.getExtrusion().setE(5.0f);

        ExtrusionNode extrusionNode6 = new ExtrusionNode();
        extrusionNode6.getExtrusion().setE(6.0f);

        fill1.addChildAtEnd(extrusionNode1);
        fill1.addChildAtEnd(extrusionNode2);
        fill1.addChildAtEnd(extrusionNode3);
        fill1.addChildAtEnd(extrusionNode4);
        fill1.addChildAtEnd(extrusionNode5);
        fill1.addChildAtEnd(extrusionNode6);

        FillSectionNode fill2 = new FillSectionNode();

        ExtrusionNode extrusionNode7 = new ExtrusionNode();
        extrusionNode7.getExtrusion().setE(1.0f);

        ExtrusionNode extrusionNode8 = new ExtrusionNode();
        extrusionNode8.getExtrusion().setE(2.0f);

        ExtrusionNode extrusionNode9 = new ExtrusionNode();
        extrusionNode9.getExtrusion().setE(3.0f);

        ExtrusionNode extrusionNode10 = new ExtrusionNode();
        extrusionNode10.getExtrusion().setE(4.0f);

        ExtrusionNode extrusionNode11 = new ExtrusionNode();
        extrusionNode11.getExtrusion().setE(5.0f);

        ExtrusionNode extrusionNode12 = new ExtrusionNode();
        extrusionNode12.getExtrusion().setE(6.0f);

        fill2.addChildAtEnd(extrusionNode7);
        fill2.addChildAtEnd(extrusionNode8);
        fill2.addChildAtEnd(extrusionNode9);
        fill2.addChildAtEnd(extrusionNode10);
        fill2.addChildAtEnd(extrusionNode11);
        fill2.addChildAtEnd(extrusionNode12);

        ToolSelectNode ts1 = new ToolSelectNode();
        ts1.addChildAtEnd(fill1);
        ts1.addChildAtEnd(fill2);

        PostProcessorFeatureSet ppFeatures = new PostProcessorFeatureSet();
        ppFeatures.enableFeature(PostProcessorFeature.REMOVE_ALL_UNRETRACTS);
        ppFeatures.enableFeature(PostProcessorFeature.OPEN_NOZZLE_FULLY_AT_START);
        ppFeatures.enableFeature(PostProcessorFeature.CLOSES_ON_RETRACT);
        ppFeatures.enableFeature(PostProcessorFeature.CLOSE_ON_TASK_CHANGE);

        NodeManagementUtilities nodeManagementUtilities = new NodeManagementUtilities(ppFeatures);

        try
        {
            Optional<ExtrusionNode> result1 = nodeManagementUtilities.findNextExtrusion(ts1, extrusionNode1);
            assertTrue(result1.isPresent());
            assertSame(extrusionNode2, result1.get());

            Optional<ExtrusionNode> result2 = nodeManagementUtilities.findNextExtrusion(ts1, extrusionNode6);
            assertTrue(result2.isPresent());
            assertSame(extrusionNode7, result2.get());
        } catch (NodeProcessingException ex)
        {
            fail("Got exception during test " + ex);
        }
    }

    @Test
    public void testFindPriorExtrusion()
    {
        FillSectionNode fill1 = new FillSectionNode();

        ExtrusionNode extrusionNode1 = new ExtrusionNode();
        extrusionNode1.getExtrusion().setE(1.0f);

        ExtrusionNode extrusionNode2 = new ExtrusionNode();
        extrusionNode2.getExtrusion().setE(2.0f);

        ExtrusionNode extrusionNode3 = new ExtrusionNode();
        extrusionNode3.getExtrusion().setE(3.0f);

        ExtrusionNode extrusionNode4 = new ExtrusionNode();
        extrusionNode4.getExtrusion().setE(4.0f);

        ExtrusionNode extrusionNode5 = new ExtrusionNode();
        extrusionNode5.getExtrusion().setE(5.0f);

        ExtrusionNode extrusionNode6 = new ExtrusionNode();
        extrusionNode6.getExtrusion().setE(6.0f);

        fill1.addChildAtEnd(extrusionNode1);
        fill1.addChildAtEnd(extrusionNode2);
        fill1.addChildAtEnd(extrusionNode3);
        fill1.addChildAtEnd(extrusionNode4);
        fill1.addChildAtEnd(extrusionNode5);
        fill1.addChildAtEnd(extrusionNode6);

        FillSectionNode fill2 = new FillSectionNode();

        ExtrusionNode extrusionNode7 = new ExtrusionNode();
        extrusionNode7.getExtrusion().setE(1.0f);

        ExtrusionNode extrusionNode8 = new ExtrusionNode();
        extrusionNode8.getExtrusion().setE(2.0f);

        ExtrusionNode extrusionNode9 = new ExtrusionNode();
        extrusionNode9.getExtrusion().setE(3.0f);

        ExtrusionNode extrusionNode10 = new ExtrusionNode();
        extrusionNode10.getExtrusion().setE(4.0f);

        ExtrusionNode extrusionNode11 = new ExtrusionNode();
        extrusionNode11.getExtrusion().setE(5.0f);

        ExtrusionNode extrusionNode12 = new ExtrusionNode();
        extrusionNode12.getExtrusion().setE(6.0f);

        fill2.addChildAtEnd(extrusionNode7);
        fill2.addChildAtEnd(extrusionNode8);
        fill2.addChildAtEnd(extrusionNode9);
        fill2.addChildAtEnd(extrusionNode10);
        fill2.addChildAtEnd(extrusionNode11);
        fill2.addChildAtEnd(extrusionNode12);

        ToolSelectNode ts1 = new ToolSelectNode();
        ts1.addChildAtEnd(fill1);
        ts1.addChildAtEnd(fill2);

        PostProcessorFeatureSet ppFeatures = new PostProcessorFeatureSet();
        ppFeatures.enableFeature(PostProcessorFeature.REMOVE_ALL_UNRETRACTS);
        ppFeatures.enableFeature(PostProcessorFeature.OPEN_NOZZLE_FULLY_AT_START);
        ppFeatures.enableFeature(PostProcessorFeature.CLOSES_ON_RETRACT);
        ppFeatures.enableFeature(PostProcessorFeature.CLOSE_ON_TASK_CHANGE);

        NodeManagementUtilities nodeManagementUtilities = new NodeManagementUtilities(ppFeatures);

        try
        {
            Optional<ExtrusionNode> result1 = nodeManagementUtilities.findPriorExtrusion(extrusionNode12);
            assertTrue(result1.isPresent());
            assertSame(extrusionNode11, result1.get());

//            Optional<ExtrusionNode> result2 = nodeManagementUtilities.findNextExtrusion(ts1, extrusionNode6);
//            assertTrue(result2.isPresent());
//            assertSame(extrusionNode7, result2.get());
        } catch (NodeProcessingException ex)
        {
            fail("Got exception during test " + ex);
        }
    }

    @Test
    public void testFindPriorMovementInPreviousSection()
    {
        FillSectionNode fill1 = new FillSectionNode();

        ExtrusionNode extrusionNode1 = new ExtrusionNode();
        extrusionNode1.getExtrusion().setE(1.0f);

        ExtrusionNode extrusionNode2 = new ExtrusionNode();
        extrusionNode2.getExtrusion().setE(2.0f);

        ExtrusionNode extrusionNode3 = new ExtrusionNode();
        extrusionNode3.getExtrusion().setE(3.0f);

        ExtrusionNode extrusionNode4 = new ExtrusionNode();
        extrusionNode4.getExtrusion().setE(4.0f);

        ExtrusionNode extrusionNode5 = new ExtrusionNode();
        extrusionNode5.getExtrusion().setE(5.0f);

        ExtrusionNode extrusionNode6 = new ExtrusionNode();
        extrusionNode6.getExtrusion().setE(6.0f);

        fill1.addChildAtEnd(extrusionNode1);
        fill1.addChildAtEnd(extrusionNode2);
        fill1.addChildAtEnd(extrusionNode3);
        fill1.addChildAtEnd(extrusionNode4);
        fill1.addChildAtEnd(extrusionNode5);
        fill1.addChildAtEnd(extrusionNode6);

        FillSectionNode fill2 = new FillSectionNode();

        ExtrusionNode extrusionNode7 = new ExtrusionNode();
        extrusionNode7.getExtrusion().setE(1.0f);

        ExtrusionNode extrusionNode8 = new ExtrusionNode();
        extrusionNode8.getExtrusion().setE(2.0f);

        ExtrusionNode extrusionNode9 = new ExtrusionNode();
        extrusionNode9.getExtrusion().setE(3.0f);

        ExtrusionNode extrusionNode10 = new ExtrusionNode();
        extrusionNode10.getExtrusion().setE(4.0f);

        ExtrusionNode extrusionNode11 = new ExtrusionNode();
        extrusionNode11.getExtrusion().setE(5.0f);

        ExtrusionNode extrusionNode12 = new ExtrusionNode();
        extrusionNode12.getExtrusion().setE(6.0f);

        fill2.addChildAtEnd(extrusionNode7);
        fill2.addChildAtEnd(extrusionNode8);
        fill2.addChildAtEnd(extrusionNode9);
        fill2.addChildAtEnd(extrusionNode10);
        fill2.addChildAtEnd(extrusionNode11);
        fill2.addChildAtEnd(extrusionNode12);

        ToolSelectNode ts1 = new ToolSelectNode();
        ts1.addChildAtEnd(fill1);
        ts1.addChildAtEnd(fill2);

        PostProcessorFeatureSet ppFeatures = new PostProcessorFeatureSet();
        ppFeatures.enableFeature(PostProcessorFeature.REMOVE_ALL_UNRETRACTS);
        ppFeatures.enableFeature(PostProcessorFeature.OPEN_NOZZLE_FULLY_AT_START);
        ppFeatures.enableFeature(PostProcessorFeature.CLOSES_ON_RETRACT);
        ppFeatures.enableFeature(PostProcessorFeature.CLOSE_ON_TASK_CHANGE);

        NodeManagementUtilities nodeManagementUtilities = new NodeManagementUtilities(ppFeatures);

        try
        {
            Optional<MovementProvider> result1 = nodeManagementUtilities.findPriorMovementInPreviousSection(extrusionNode7);

            assertTrue(result1.isPresent());
            assertSame(extrusionNode6, result1.get());

        } catch (NodeProcessingException ex)
        {
            fail("Got exception during test " + ex);
        }
    }

    @Test
    public void testFindPriorMovement()
    {
        FillSectionNode fill1 = new FillSectionNode();

        ExtrusionNode extrusionNode1 = new ExtrusionNode();
        extrusionNode1.getExtrusion().setE(1.0f);

        ExtrusionNode extrusionNode2 = new ExtrusionNode();
        extrusionNode2.getExtrusion().setE(2.0f);

        ExtrusionNode extrusionNode3 = new ExtrusionNode();
        extrusionNode3.getExtrusion().setE(3.0f);

        ExtrusionNode extrusionNode4 = new ExtrusionNode();
        extrusionNode4.getExtrusion().setE(4.0f);

        ExtrusionNode extrusionNode5 = new ExtrusionNode();
        extrusionNode5.getExtrusion().setE(5.0f);

        ExtrusionNode extrusionNode6 = new ExtrusionNode();
        extrusionNode6.getExtrusion().setE(6.0f);

        fill1.addChildAtEnd(extrusionNode1);
        fill1.addChildAtEnd(extrusionNode2);
        fill1.addChildAtEnd(extrusionNode3);
        fill1.addChildAtEnd(extrusionNode4);
        fill1.addChildAtEnd(extrusionNode5);
        fill1.addChildAtEnd(extrusionNode6);

        FillSectionNode fill2 = new FillSectionNode();

        ExtrusionNode extrusionNode7 = new ExtrusionNode();
        extrusionNode7.getExtrusion().setE(1.0f);

        ExtrusionNode extrusionNode8 = new ExtrusionNode();
        extrusionNode8.getExtrusion().setE(2.0f);

        ExtrusionNode extrusionNode9 = new ExtrusionNode();
        extrusionNode9.getExtrusion().setE(3.0f);

        ExtrusionNode extrusionNode10 = new ExtrusionNode();
        extrusionNode10.getExtrusion().setE(4.0f);

        ExtrusionNode extrusionNode11 = new ExtrusionNode();
        extrusionNode11.getExtrusion().setE(5.0f);

        ExtrusionNode extrusionNode12 = new ExtrusionNode();
        extrusionNode12.getExtrusion().setE(6.0f);

        fill2.addChildAtEnd(extrusionNode7);
        fill2.addChildAtEnd(extrusionNode8);
        fill2.addChildAtEnd(extrusionNode9);
        fill2.addChildAtEnd(extrusionNode10);
        fill2.addChildAtEnd(extrusionNode11);
        fill2.addChildAtEnd(extrusionNode12);

        ToolSelectNode ts1 = new ToolSelectNode();
        ts1.addChildAtEnd(fill1);
        ts1.addChildAtEnd(fill2);

        PostProcessorFeatureSet ppFeatures = new PostProcessorFeatureSet();
        ppFeatures.enableFeature(PostProcessorFeature.REMOVE_ALL_UNRETRACTS);
        ppFeatures.enableFeature(PostProcessorFeature.OPEN_NOZZLE_FULLY_AT_START);
        ppFeatures.enableFeature(PostProcessorFeature.CLOSES_ON_RETRACT);
        ppFeatures.enableFeature(PostProcessorFeature.CLOSE_ON_TASK_CHANGE);

        NodeManagementUtilities nodeManagementUtilities = new NodeManagementUtilities(ppFeatures);

        try
        {
            Optional<MovementProvider> result1 = nodeManagementUtilities.findPriorMovement(extrusionNode8);

            assertTrue(result1.isPresent());
            assertSame(extrusionNode7, result1.get());

        } catch (NodeProcessingException ex)
        {
            fail("Got exception during test " + ex);
        }
    }

    @Test
    public void testCalculatePerRetractExtrusionAndNode()
    {
        FillSectionNode fill1 = new FillSectionNode();

        ExtrusionNode extrusionNode1 = new ExtrusionNode();
        extrusionNode1.getExtrusion().setE(1.0f);

        ExtrusionNode extrusionNode2 = new ExtrusionNode();
        extrusionNode2.getExtrusion().setE(2.0f);

        ExtrusionNode extrusionNode3 = new ExtrusionNode();
        extrusionNode3.getExtrusion().setE(3.0f);

        ExtrusionNode extrusionNode4 = new ExtrusionNode();
        extrusionNode4.getExtrusion().setE(4.0f);

        ExtrusionNode extrusionNode5 = new ExtrusionNode();
        extrusionNode5.getExtrusion().setE(5.0f);

        ExtrusionNode extrusionNode6 = new ExtrusionNode();
        extrusionNode6.getExtrusion().setE(6.0f);

        RetractNode retractNode1 = new RetractNode();

        fill1.addChildAtEnd(extrusionNode1);
        fill1.addChildAtEnd(extrusionNode2);
        fill1.addChildAtEnd(extrusionNode3);
        fill1.addChildAtEnd(extrusionNode4);
        fill1.addChildAtEnd(extrusionNode5);
        fill1.addChildAtEnd(extrusionNode6);
        fill1.addChildAtEnd(retractNode1);

        FillSectionNode fill2 = new FillSectionNode();

        ExtrusionNode extrusionNode7 = new ExtrusionNode();
        extrusionNode7.getExtrusion().setE(2.0f);

        ExtrusionNode extrusionNode8 = new ExtrusionNode();
        extrusionNode8.getExtrusion().setE(3.0f);

        ExtrusionNode extrusionNode9 = new ExtrusionNode();
        extrusionNode9.getExtrusion().setE(4.0f);

        ExtrusionNode extrusionNode10 = new ExtrusionNode();
        extrusionNode10.getExtrusion().setE(5.0f);

        ExtrusionNode extrusionNode11 = new ExtrusionNode();
        extrusionNode11.getExtrusion().setE(6.0f);

        ExtrusionNode extrusionNode12 = new ExtrusionNode();
        extrusionNode12.getExtrusion().setE(7.0f);

        RetractNode retractNode2 = new RetractNode();

        fill2.addChildAtEnd(extrusionNode7);
        fill2.addChildAtEnd(extrusionNode8);
        fill2.addChildAtEnd(extrusionNode9);
        fill2.addChildAtEnd(extrusionNode10);
        fill2.addChildAtEnd(extrusionNode11);
        fill2.addChildAtEnd(extrusionNode12);
        fill2.addChildAtEnd(retractNode2);

        ToolSelectNode ts1 = new ToolSelectNode();
        ts1.addChildAtEnd(fill1);
        ts1.addChildAtEnd(fill2);

        LayerNode layer = new LayerNode(0);
        layer.addChildAtEnd(ts1);

        PostProcessorFeatureSet ppFeatures = new PostProcessorFeatureSet();
        ppFeatures.enableFeature(PostProcessorFeature.REMOVE_ALL_UNRETRACTS);
        ppFeatures.enableFeature(PostProcessorFeature.OPEN_NOZZLE_FULLY_AT_START);
        ppFeatures.enableFeature(PostProcessorFeature.CLOSES_ON_RETRACT);
        ppFeatures.enableFeature(PostProcessorFeature.CLOSE_ON_TASK_CHANGE);

        NodeManagementUtilities nodeManagementUtilities = new NodeManagementUtilities(ppFeatures);

        nodeManagementUtilities.calculatePerRetractExtrusionAndNode(layer);

        assertNotNull(retractNode1.getPriorExtrusionNode());
        assertSame(extrusionNode6, retractNode1.getPriorExtrusionNode());
        assertEquals(21, retractNode1.getExtrusionSinceLastRetract(), 0.1);

        assertNotNull(retractNode2.getPriorExtrusionNode());
        assertSame(extrusionNode12, retractNode2.getPriorExtrusionNode());
        assertEquals(27, retractNode2.getExtrusionSinceLastRetract(), 0.1);
    }
}
