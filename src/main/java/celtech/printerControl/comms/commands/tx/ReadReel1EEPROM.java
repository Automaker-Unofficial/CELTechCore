package celtech.printerControl.comms.commands.tx;

import celtech.comms.remote.TxPacketTypeEnum;
import celtech.comms.remote.RoboxTxPacket;

/**
 *
 * @author ianhudson
 */
public class ReadReel1EEPROM extends RoboxTxPacket
{

    /**
     *
     */
    public ReadReel1EEPROM()
    {
        super(TxPacketTypeEnum.READ_REEL_1_EEPROM, false, false);
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
