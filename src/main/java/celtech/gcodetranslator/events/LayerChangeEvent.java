package celtech.gcodetranslator.events;

/**
 *
 * @author Ian
 */
public class LayerChangeEvent extends GCodeParseEvent
{
    private double z;

    public double getZ()
    {
        return z;
    }

    public void setZ(double value)
    {
        this.z = value;
    }

    @Override
    public String renderForOutput()
    {
        String stringToReturn = "G1 Z" + String.format("%.3f", z);

        if (getFeedRate() > 0)
        {
            stringToReturn += " F" + String.format("%.3f", getFeedRate());
        }

        if (getComment() != null)
        {
            stringToReturn += " ; " + getComment();
        }

        return stringToReturn + "\n";
    }
}
