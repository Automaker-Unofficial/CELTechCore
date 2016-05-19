package celtech.configuration.fileRepresentation;

import celtech.printerControl.model.Extruder;
import celtech.printerControl.model.Head.HeadType;
import celtech.printerControl.model.Head.ValveType;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.codehaus.jackson.annotate.JsonIgnore;

/**
 *
 * @author Ian
 */
public class HeadFile
{

    private int version = 2;
    private String typeCode;
    private HeadType type;
    private ValveType valves;

    private List<NozzleHeaterData> nozzleHeaters = new ArrayList<>();
    private List<NozzleData> nozzles = new ArrayList<>();

    public HeadType getType()
    {
        return type;
    }

    public void setType(HeadType type)
    {
        this.type = type;
    }

    public void setValves(ValveType valves)
    {
        this.valves = valves;
    }

    public ValveType getValves()
    {
        return valves;
    }

    public int getVersion()
    {
        return version;
    }

    public void setVersion(int version)
    {
        this.version = version;
    }

    public String getTypeCode()
    {
        return typeCode;
    }

    public void setTypeCode(String typeCode)
    {
        this.typeCode = typeCode;
    }

    public List<NozzleHeaterData> getNozzleHeaters()
    {
        return nozzleHeaters;
    }

    public void setNozzleHeaters(List<NozzleHeaterData> nozzleHeaters)
    {
        this.nozzleHeaters = nozzleHeaters;
    }

    public List<NozzleData> getNozzles()
    {
        return nozzles;
    }

    public void setNozzles(List<NozzleData> nozzles)
    {
        this.nozzles = nozzles;
    }

    @Override
    public String toString()
    {
        return typeCode;
    }

    @JsonIgnore
    public Optional<Integer> getNozzleNumberForExtruderNumber(int extruderNumber)
    {
        Optional returnVal = Optional.empty();

        for (int nozzleIndex = 0; nozzleIndex < nozzles.size(); nozzleIndex++)
        {
            if (nozzles.get(nozzleIndex).getAssociatedExtruder().equalsIgnoreCase(Extruder.getExtruderLetterForNumber(extruderNumber)))
            {
                returnVal = Optional.of(nozzleIndex);
                break;
            }
        }
        return returnVal;
    }
}
