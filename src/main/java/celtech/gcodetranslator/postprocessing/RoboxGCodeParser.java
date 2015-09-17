package celtech.gcodetranslator.postprocessing;

import celtech.gcodetranslator.postprocessing.nodes.CommentNode;
import celtech.gcodetranslator.postprocessing.nodes.ExtrusionNode;
import celtech.gcodetranslator.postprocessing.nodes.FillSectionNode;
import celtech.gcodetranslator.postprocessing.nodes.GCodeDirectiveNode;
import celtech.gcodetranslator.postprocessing.nodes.GCodeEventNode;
import celtech.gcodetranslator.postprocessing.nodes.InnerPerimeterSectionNode;
import celtech.gcodetranslator.postprocessing.nodes.LayerChangeDirectiveNode;
import celtech.gcodetranslator.postprocessing.nodes.LayerNode;
import celtech.gcodetranslator.postprocessing.nodes.MCodeNode;
import celtech.gcodetranslator.postprocessing.nodes.OuterPerimeterSectionNode;
import celtech.gcodetranslator.postprocessing.nodes.PreambleNode;
import celtech.gcodetranslator.postprocessing.nodes.RetractNode;
import celtech.gcodetranslator.postprocessing.nodes.ObjectDelineationNode;
import celtech.gcodetranslator.postprocessing.nodes.OrphanObjectDelineationNode;
import celtech.gcodetranslator.postprocessing.nodes.OrphanSectionNode;
import celtech.gcodetranslator.postprocessing.nodes.SkinSectionNode;
import celtech.gcodetranslator.postprocessing.nodes.SupportInterfaceSectionNode;
import celtech.gcodetranslator.postprocessing.nodes.SupportSectionNode;
import celtech.gcodetranslator.postprocessing.nodes.TravelNode;
import celtech.gcodetranslator.postprocessing.nodes.UnrecognisedLineNode;
import celtech.gcodetranslator.postprocessing.nodes.UnretractNode;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;
import org.parboiled.Action;
import org.parboiled.BaseParser;
import org.parboiled.Context;
import org.parboiled.Rule;
import org.parboiled.annotations.SuppressSubnodes;
import org.parboiled.support.StringVar;
import org.parboiled.support.Var;

/**
 *
 * @author Ian
 */
//@BuildParseTree
public class RoboxGCodeParser extends BaseParser<GCodeEventNode>
{

    private final Stenographer steno = StenographerFactory.getStenographer(RoboxGCodeParser.class.getName());
    private LayerNode thisLayer = new LayerNode();
    private int feedrateInForce = -1;
    protected Var<Integer> currentObject = new Var<>(-1);
    
    public void setFeedrateInForce(int feedrate)
    {
        this.feedrateInForce = feedrate;
    }

    public int getFeedrateInForce()
    {
        return feedrateInForce;
    }

    public LayerNode getLayerNode()
    {
        return thisLayer;
    }

    public void resetLayer()
    {
        thisLayer = new LayerNode();
    }

    public Rule Layer()
    {
        return Sequence(
                Sequence(";LAYER:", OneOrMore(Digit()),
                        (Action) (Context context1) ->
                        {
                            thisLayer.setLayerNumber(Integer.valueOf(context1.getMatch()));
                            return true;
                        },
                        Newline()
                ),
                //                Optional(
                //                        Sequence(
                //                                Preamble(),
                //                                (Action) (Context context1) ->
                //                                {
                //                                    if (!context1.getValueStack().isEmpty())
                //                                    {
                //                                        GCodeEventNode node = (GCodeEventNode) context1.getValueStack().pop();
                //                                        TreeUtils.addChild(thisLayer, node);
                //                                    }
                //                                    return true;
                //                                }
                //                        )
                //                ),
                OneOrMore(
                        FirstOf(
                                ObjectSection(),
                                OrphanObjectSection(),
                                ChildDirective()
                        ),
                        (Action) (Context context1) ->
                        {
                            if (!context1.getValueStack().isEmpty())
                            {
                                GCodeEventNode node = (GCodeEventNode) context1.getValueStack().pop();
                                thisLayer.addChildAtEnd(node);
                            }
                            return true;
                        }
                )
        );
    }

    Rule Preamble()
    {
        return Sequence(
                OneOrMore(CommentDirective()),
                new Action()
                {
                    @Override
                    public boolean run(Context context)
                    {
                        PreambleNode node = new PreambleNode();
                        while (context.getValueStack().iterator().hasNext())
                        {
                            node.getChildren().add(0, (GCodeEventNode) context.getValueStack().pop());
                        }
                        context.getValueStack().push(node);
                        return true;
                    }
                }
        );
    }

    // T1 or T12 or T123...
    Rule ObjectSection()
    {
        ObjectSectionActionClass objectSectionAction = new ObjectSectionActionClass();
        Var<Integer> objectNumber = new Var<>(0);

        return Sequence(
                Sequence('T', OneOrMore(Digit()),
                        objectNumber.set(Integer.valueOf(match())),
                        currentObject.set(Integer.valueOf(match())),
                        Newline()
                ),
                objectSectionAction,
                Optional(
                        Sequence(TravelDirective(),
                                new Action()
                                {
                                    @Override
                                    public boolean run(Context context)
                                    {
                                        objectSectionAction.getNode().addChildAtEnd((GCodeEventNode) context.getValueStack().pop());
                                        return true;
                                    }
                                }
                        )
                ),
                OneOrMore(
                        Sequence(
                                AnySection(),
                                new Action()
                                {
                                    @Override
                                    public boolean run(Context context)
                                    {
                                        objectSectionAction.getNode().addChildAtEnd((GCodeEventNode) context.getValueStack().pop());
                                        return true;
                                    }
                                }
                        )
                ),
                new Action()
                {
                    @Override
                    public boolean run(Context context
                    )
                    {
                        ObjectDelineationNode node = objectSectionAction.getNode();
                        node.setObjectNumber(objectNumber.get());
                        context.getValueStack().push(node);
                        return true;
                    }
                }
        );
    }

    // No preceding T command - can happen at the start of a file or start of a layer if the tool use is continued from the previous
    Rule OrphanObjectSection()
    {
        OrphanObjectSectionActionClass orphanObjectSectionAction = new OrphanObjectSectionActionClass();

        return Sequence(
                // Orphan - make this part of the current object
                IsASection(),
                orphanObjectSectionAction,
                OneOrMore(
                        Sequence(
                                FirstOf(
                                        TravelDirective(),
                                        AnySection()
                                ),
                                new Action()
                                {
                                    @Override
                                    public boolean run(Context context)
                                    {
                                        orphanObjectSectionAction.getNode().addChildAtEnd((GCodeEventNode) context.getValueStack().pop());
                                        return true;
                                    }
                                }
                        )
                ),
                new Action()
                {
                    @Override
                    public boolean run(Context context
                    )
                    {
                        OrphanObjectDelineationNode node = orphanObjectSectionAction.getNode();
                        node.setPotentialObjectNumber(currentObject.get());
                        context.getValueStack().push(node);
                        return true;
                    }
                }
        );
    }

    //Orphan section
    //No type
    Rule OrphanSection()
    {
        return Sequence(
                NotASection(),
                OneOrMore(ChildDirective()),
                new Action()
                {
                    @Override
                    public boolean run(Context context)
                    {
                        OrphanSectionNode node = new OrphanSectionNode();
                        while (context.getValueStack().iterator().hasNext())
                        {
                            node.addChildAtStart((GCodeEventNode) context.getValueStack().pop());
                        }
                        context.getValueStack().push(node);
                        return true;
                    }
                }
        );
    }

    //Cura fill Section
    //;TYPE:FILL
    Rule FillSection()
    {
        return Sequence(
                FillSectionNode.designator,
                Newline(),
                OneOrMore(ChildDirective()),
                new Action()
                {
                    @Override
                    public boolean run(Context context)
                    {
                        FillSectionNode node = new FillSectionNode();
                        while (context.getValueStack().iterator().hasNext())
                        {
                            node.addChildAtStart((GCodeEventNode) context.getValueStack().pop());
                        }
                        context.getValueStack().push(node);
                        return true;
                    }
                }
        );
    }

    //Cura skin Section
    //;TYPE:SKIN
    Rule SkinSection()
    {
        return Sequence(
                SkinSectionNode.designator,
                Newline(),
                OneOrMore(ChildDirective()),
                new Action()
                {
                    @Override
                    public boolean run(Context context)
                    {
                        SkinSectionNode node = new SkinSectionNode();
                        while (context.getValueStack().iterator().hasNext())
                        {
                            node.addChildAtStart((GCodeEventNode) context.getValueStack().pop());
                        }
                        context.getValueStack().push(node);
                        return true;
                    }
                }
        );
    }

    //Cura support Section
    Rule SupportSection()
    {
        return Sequence(
                SupportSectionNode.designator,
                Newline(),
                OneOrMore(ChildDirective()),
                new Action()
                {
                    @Override
                    public boolean run(Context context)
                    {
                        SupportSectionNode node = new SupportSectionNode();
                        while (context.getValueStack().iterator().hasNext())
                        {
                            node.addChildAtStart((GCodeEventNode) context.getValueStack().pop());
                        }
                        context.getValueStack().push(node);
                        return true;
                    }
                }
        );
    }

    //Cura support interface Section
    Rule SupportInterfaceSection()
    {
        return Sequence(
                SupportInterfaceSectionNode.designator,
                Newline(),
                OneOrMore(ChildDirective()),
                new Action()
                {
                    @Override
                    public boolean run(Context context)
                    {
                        SupportInterfaceSectionNode node = new SupportInterfaceSectionNode();
                        while (context.getValueStack().iterator().hasNext())
                        {
                            node.addChildAtStart((GCodeEventNode) context.getValueStack().pop());
                        }
                        context.getValueStack().push(node);
                        return true;
                    }
                }
        );
    }

//Cura outer perimeter section
    //;TYPE:WALL-OUTER
    Rule OuterPerimeterSection()
    {
        return Sequence(
                OuterPerimeterSectionNode.designator,
                Newline(),
                OneOrMore(ChildDirective()),
                new Action()
                {
                    @Override
                    public boolean run(Context context)
                    {
                        OuterPerimeterSectionNode node = new OuterPerimeterSectionNode();
                        while (context.getValueStack().iterator().hasNext())
                        {
                            node.addChildAtStart((GCodeEventNode) context.getValueStack().pop());
                        }
                        context.getValueStack().push(node);
                        return true;
                    }
                }
        );
    }

    //Cura inner perimeter section
    //;TYPE:WALL-INNER
    Rule InnerPerimeterSection()
    {
        return Sequence(
                InnerPerimeterSectionNode.designator,
                Newline(),
                OneOrMore(ChildDirective()),
                new Action()
                {
                    @Override
                    public boolean run(Context context)
                    {
                        InnerPerimeterSectionNode node = new InnerPerimeterSectionNode();
                        while (context.getValueStack().iterator().hasNext())
                        {
                            node.addChildAtStart((GCodeEventNode) context.getValueStack().pop());
                        }
                        context.getValueStack().push(node);
                        return true;
                    }
                }
        );
    }

    Rule NotASection()
    {
        return Sequence(
                TestNot(FillSectionNode.designator),
                TestNot(InnerPerimeterSectionNode.designator),
                TestNot(OuterPerimeterSectionNode.designator),
                TestNot(SkinSectionNode.designator),
                TestNot(SupportInterfaceSectionNode.designator),
                TestNot(SupportSectionNode.designator));
    }

    Rule AnySection()
    {
        return FirstOf(
                FillSection(),
                InnerPerimeterSection(),
                OuterPerimeterSection(),
                SkinSection(),
                SupportInterfaceSection(),
                SupportSection(),
                OrphanSection());
    }

    Rule IsASection()
    {
        return FirstOf(
                Test(FillSectionNode.designator),
                Test(InnerPerimeterSectionNode.designator),
                Test(OuterPerimeterSectionNode.designator),
                Test(SkinSectionNode.designator),
                Test(SupportSectionNode.designator),
                Test(SupportInterfaceSectionNode.designator));
    }

    // ;Blah blah blah\n
    Rule CommentDirective()
    {
        StringVar comment = new StringVar();

        return Sequence(
                TestNot(FillSectionNode.designator),
                TestNot(InnerPerimeterSectionNode.designator),
                TestNot(OuterPerimeterSectionNode.designator),
                TestNot(SkinSectionNode.designator),
                TestNot(SupportSectionNode.designator),
                TestNot(SupportInterfaceSectionNode.designator),
                ';', ZeroOrMore(NotNewline()),
                comment.set(match()),
                Newline(),
                new Action()
                {
                    @Override
                    public boolean run(Context context)
                    {
                        CommentNode node = new CommentNode(comment.get());
                        context.getValueStack().push(node);
                        return true;
                    }
                }
        );
    }

    // M14 or M104
    Rule MCode()
    {
        Var<Integer> mValue = new Var<>();
        Var<Integer> sValue = new Var<>();

        return Sequence(
                Sequence('M', OneToThreeDigits(),
                        mValue.set(Integer.valueOf(match()))
                ),
                Optional(
                        Sequence(
                                " S", ZeroOrMore(Digit()),
                                sValue.set(Integer.valueOf(match()))
                        )
                ),
                Newline(),
                new Action()
                {
                    @Override
                    public boolean run(Context context)
                    {
                        MCodeNode node = new MCodeNode();
                        node.setMNumber(mValue.get());
                        if (sValue.isSet())
                        {
                            node.setSNumber(sValue.get());
                        }
                        context.getValueStack().push(node);
                        return true;
                    }
                }
        );
    }

    // G3 or G12
    Rule GCodeDirective()
    {
        Var<Integer> gcodeValue = new Var<>();

        return Sequence('G', OneOrTwoDigits(),
                gcodeValue.set(Integer.valueOf(match())),
                Newline(),
                new Action()
                {
                    @Override
                    public boolean run(Context context)
                    {
                        GCodeDirectiveNode node = new GCodeDirectiveNode();
                        node.setGValue(gcodeValue.get());
                        context.getValueStack().push(node);
                        return true;
                    }
                });
    }

    //Retract
    // G1 F1800 E-0.50000
    Rule RetractDirective()
    {
        Var<Float> dValue = new Var<>();
        Var<Float> eValue = new Var<>();
        Var<Integer> fValue = new Var<>();

        return Sequence("G1 ",
                Optional(
                        Feedrate(fValue)
                ),
                OneOrMore(
                        FirstOf(
                                Sequence("D", NegativeFloatingPointNumber(),
                                        dValue.set(Float.valueOf(match())),
                                        Optional(' ')
                                ),
                                Sequence("E", NegativeFloatingPointNumber(),
                                        eValue.set(Float.valueOf(match())),
                                        Optional(' '))
                        )
                ),
                Newline(),
                new Action()
                {
                    @Override
                    public boolean run(Context context)
                    {
                        RetractNode node = new RetractNode();
                        if (dValue.isSet())
                        {
                            node.getExtrusion().setD(dValue.get());
                        }
                        if (eValue.isSet())
                        {
                            node.getExtrusion().setE(eValue.get());
                        }
                        if (fValue.isSet())
                        {
                            node.getFeedrate().setFeedRate_mmPerMin(fValue.get());
                        }
                        context.getValueStack().push(node);
                        return true;
                    }
                }
        );
    }

    //Unetract
    // G1 F1800 E0.50000
    Rule UnretractDirective()
    {
        Var<Float> dValue = new Var<>();
        Var<Float> eValue = new Var<>();
        Var<Integer> fValue = new Var<>();

        return Sequence("G1 ",
                Optional(
                        Feedrate(fValue)
                ),
                OneOrMore(
                        FirstOf(
                                Sequence("D", PositiveFloatingPointNumber(),
                                        dValue.set(Float.valueOf(match())),
                                        Optional(' ')
                                ),
                                Sequence("E", PositiveFloatingPointNumber(),
                                        eValue.set(Float.valueOf(match())),
                                        Optional(' '))
                        )
                ),
                Newline(),
                new Action()
                {
                    @Override
                    public boolean run(Context context
                    )
                    {
                        UnretractNode node = new UnretractNode();
                        if (dValue.isSet())
                        {
                            node.getExtrusion().setD(dValue.get());
                        }
                        if (eValue.isSet())
                        {
                            node.getExtrusion().setE(eValue.get());
                        }
                        if (fValue.isSet())
                        {
                            node.getFeedrate().setFeedRate_mmPerMin(fValue.get());
                        }
                        context.getValueStack().push(node);
                        return true;
                    }
                }
        );
    }

    //Travel
    // G0 F12000 X88.302 Y42.421 Z1.020
    Rule TravelDirective()
    {
        Var<Integer> fValue = new Var<>();
        Var<Double> xValue = new Var<>();
        Var<Double> yValue = new Var<>();
        Var<Double> zValue = new Var<>();

        return Sequence("G0 ",
                Optional(
                        Feedrate(fValue)
                ),
                OneOrMore(
                        FirstOf(
                                Sequence("X", FloatingPointNumber(),
                                        xValue.set(Double.valueOf(match())),
                                        Optional(' ')
                                ),
                                Sequence("Y", FloatingPointNumber(),
                                        yValue.set(Double.valueOf(match())),
                                        Optional(' ')
                                ),
                                Sequence("Z", FloatingPointNumber(),
                                        zValue.set(Double.valueOf(match())),
                                        Optional(' ')
                                )
                        )
                ),
                Newline(),
                new Action()
                {
                    @Override
                    public boolean run(Context context)
                    {
                        TravelNode node = new TravelNode();

                        if (fValue.isSet())
                        {
                            node.getFeedrate().setFeedRate_mmPerMin(fValue.get());
                        }

                        if (xValue.isSet())
                        {
                            node.getMovement().setX(xValue.get());
                        }

                        if (yValue.isSet())
                        {
                            node.getMovement().setY(yValue.get());
                        }

                        if (zValue.isSet())
                        {
                            node.getMovement().setZ(zValue.get());
                        }

                        context.getValueStack().push(node);
                        return true;
                    }
                }
        );
    }

    //Extrusion
    // G1 F840 X88.700 Y44.153 E5.93294
    Rule ExtrusionDirective()
    {
        Var<Integer> fValue = new Var<>();
        Var<Double> xValue = new Var<>();
        Var<Double> yValue = new Var<>();
        Var<Double> zValue = new Var<>();
        Var<Float> eValue = new Var<>();
        Var<Float> dValue = new Var<>();

        return Sequence("G1 ",
                Optional(
                        Feedrate(fValue)
                ),
                Optional(
                        Sequence("X", FloatingPointNumber(),
                                xValue.set(Double.valueOf(match())),
                                ' ',
                                "Y", FloatingPointNumber(),
                                yValue.set(Double.valueOf(match())),
                                Optional(' ')
                        )
                ),
                OneOrMore(
                        FirstOf(
                                Sequence("D", PositiveFloatingPointNumber(),
                                        dValue.set(Float.valueOf(match())),
                                        Optional(' ')
                                ),
                                Sequence("E", PositiveFloatingPointNumber(),
                                        eValue.set(Float.valueOf(match())),
                                        Optional(' ')
                                )
                        )
                ),
                Newline(),
                new Action()
                {
                    @Override
                    public boolean run(Context context)
                    {
                        ExtrusionNode node = new ExtrusionNode();

                        if (fValue.isSet())
                        {
                            node.getFeedrate().setFeedRate_mmPerMin(fValue.get());
                        }

                        if (xValue.isSet())
                        {
                            node.getMovement().setX(xValue.get());
                        }

                        if (yValue.isSet())
                        {
                            node.getMovement().setY(yValue.get());
                        }

                        if (zValue.isSet())
                        {
                            node.getMovement().setZ(zValue.get());
                        }

                        if (dValue.isSet())
                        {
                            node.getExtrusion().setD(dValue.get());
                        }

                        if (eValue.isSet())
                        {
                            node.getExtrusion().setE(eValue.get());
                        }

                        context.getValueStack()
                        .push(node);

                        return true;
                    }
                }
        );
    }

    //Layer change
    // G[01] Z1.020
    Rule LayerChangeDirective()
    {
        return Sequence('G', FirstOf('0', '1'), ' ',
                Sequence("Z", FloatingPointNumber()),
                push(new LayerChangeDirectiveNode()),
                Newline());
    }

    @SuppressSubnodes
    Rule ChildDirective()
    {
        return FirstOf(CommentDirective(),
                MCode(),
                LayerChangeDirective(),
                GCodeDirective(),
                RetractDirective(),
                UnretractDirective(),
                TravelDirective(),
                ExtrusionDirective(),
                UnrecognisedLine()
        );
    }

    @SuppressSubnodes
    Rule OneOrTwoDigits()
    {
        return FirstOf(TwoDigits(), Digit());
    }

    @SuppressSubnodes
    Rule TwoOrThreeDigits()
    {
        return FirstOf(ThreeDigits(), TwoDigits());
    }

    @SuppressSubnodes
    Rule OneToThreeDigits()
    {
        return FirstOf(ThreeDigits(), TwoDigits(), Digit());
    }

    @SuppressSubnodes
    Rule TwoDigits()
    {
        return Sequence(Digit(), Digit());
    }

    @SuppressSubnodes
    Rule ThreeDigits()
    {
        return Sequence(Digit(), Digit(), Digit());
    }

    @SuppressSubnodes
    Rule Digit()
    {
        return CharRange('0', '9');
    }

    @SuppressSubnodes
    Rule FloatingPointNumber()
    {
        return FirstOf(
                NegativeFloatingPointNumber(),
                PositiveFloatingPointNumber()
        );
    }

    @SuppressSubnodes
    Rule PositiveFloatingPointNumber()
    {
        //Positive float e.g. 1.23
        return Sequence(
                OneOrMore(Digit()),
                Ch('.'),
                OneOrMore(Digit()));
    }

    @SuppressSubnodes
    Rule NegativeFloatingPointNumber()
    {
        //Negative float e.g. -1.23
        return Sequence(
                Ch('-'),
                OneOrMore(Digit()),
                Ch('.'),
                OneOrMore(Digit()));
    }

    @SuppressSubnodes
    Rule Feedrate(Var<Integer> feedrate)
    {
        return FirstOf(
                Sequence(
                        'F', OneOrMore(Digit()),
                        feedrate.set(Integer.valueOf(match())),
                        Optional(' '),
                        new Action()
                        {
                            @Override
                            public boolean run(Context context)
                            {
                                feedrateInForce = feedrate.get();
                                return true;
                            }
                        }
                ),
                new Action()
                {
                    @Override
                    public boolean run(Context context)
                    {
                        feedrate.set(feedrateInForce);
                        return true;
                    }
                }
        );
    }

    //Anything else we didn't parse... must always be the last thing we look for
    // blah blah \n
    //we mustn't match a line beginning with T as this is the start of an object
    @SuppressSubnodes
    Rule UnrecognisedLine()
    {
        return Sequence(
                NotASection(),
                OneOrMore(NoneOf("T\n")),
                new Action()
                {
                    @Override
                    public boolean run(Context context)
                    {
                        String line = context.getMatch();
                        context.getValueStack().push(new UnrecognisedLineNode(line));
                        return true;
                    }
                },
                Newline()
        );
    }

    @SuppressSubnodes
    Rule Newline()
    {
        return Ch('\n');
    }

    @SuppressSubnodes
    Rule NotNewline()
    {
        return NoneOf("\n");
    }
}