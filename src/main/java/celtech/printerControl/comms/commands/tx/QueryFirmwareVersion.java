/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.printerControl.comms.commands.tx;

/**
 *
 * @author ianhudson
 */
public class QueryFirmwareVersion extends RoboxTxPacket
{

    /**
     *
     */
    public QueryFirmwareVersion()
    {
        super(TxPacketTypeEnum.QUERY_FIRMWARE_VERSION, false, false);
    }

    /**
     *
     * @param byteData
     * @return
     */
    @Override
    public boolean populatePacket(byte[] byteData)
    {
        return false;
    }
}