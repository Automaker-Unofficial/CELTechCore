/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.printerControl.comms.commands.tx;

import celtech.comms.remote.TxPacketTypeEnum;
import celtech.comms.remote.RoboxTxPacket;


/**
 *
 * @author ianhudson
 */
public class InitiatePrint extends RoboxTxPacket
{

    /**
     *
     */
    public InitiatePrint()
    {
        super(TxPacketTypeEnum.INITIATE_PRINT, false, false);
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
