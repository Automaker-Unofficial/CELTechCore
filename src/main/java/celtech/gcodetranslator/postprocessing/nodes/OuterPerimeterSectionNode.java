package celtech.gcodetranslator.postprocessing.nodes;

/**
 *
 * @author Ian
 */
public class OuterPerimeterSectionNode extends SectionNode
{

    public static final String designator = ";TYPE:WALL-OUTER";

    public OuterPerimeterSectionNode()
    {
    }

    @Override
    public String renderForOutput()
    {
        return designator + " " + super.renderForOutput();
    }
}
