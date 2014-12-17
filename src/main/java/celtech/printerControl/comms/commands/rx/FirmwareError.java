package celtech.printerControl.comms.commands.rx;

import celtech.Lookup;
import celtech.appManager.errorHandling.SystemErrorHandlerOptions;
import static celtech.appManager.errorHandling.SystemErrorHandlerOptions.OK_ABORT;
import static celtech.appManager.errorHandling.SystemErrorHandlerOptions.ABORT;
import static celtech.appManager.errorHandling.SystemErrorHandlerOptions.CLEAR_CONTINUE;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author Ian
 */
public enum FirmwareError
{
    /*
     Error flags as at firmware v689
     ERROR_SD_CARD 0
     ERROR_CHUNK_SEQUENCE 1
     ERROR_FILE_TOO_LARGE 2
     ERROR_GCODE_LINE_TOO_LONG 3
     ERROR_USB_RX 4
     ERROR_USB_TX 5
     ERROR_BAD_COMMAND 6
     ERROR_HEAD_EEPROM 7
     ERROR_BAD_FIRMWARE_FILE 8
     ERROR_FLASH_CHECKSUM 9
     ERROR_GCODE_BUFFER_OVERRUN 10
     ERROR_FILE_READ_CLOBBERED 11
     ERROR_MAX_GANTRY_ADJUSTMENT 12
     ERROR_REEL0_EEPROM 13
     ERROR_E_FILAMENT_SLIP 14
     ERROR_D_FILAMENT_SLIP 15
     ERROR_NOZZLE_FLUSH_NEEDED 16
     ERROR_Z_TOP_SWITCH 17
     ERROR_B_STUCK 18
     ERROR_REEL1_EEPROM 19
     ERROR_HEAD_POWER_EEPROM 20
     ERROR_HEAD_POWER_OVERTEMP 21
     */
    
     ERROR_SD_CARD("error.ERROR_SD_CARD", 0, OK_ABORT),
     ERROR_CHUNK_SEQUENCE("error.ERROR_CHUNK_SEQUENCE", 1, OK_ABORT),
     ERROR_FILE_TOO_LARGE("error.ERROR_FILE_TOO_LARGE", 2, OK_ABORT),
     ERROR_GCODE_LINE_TOO_LONG("error.ERROR_GCODE_LINE_TOO_LONG", 3, OK_ABORT),
     ERROR_USB_RX("error.ERROR_USB_RX", 4, OK_ABORT),
     ERROR_USB_TX("error.ERROR_USB_TX", 5, OK_ABORT),
     ERROR_BAD_COMMAND("error.ERROR_BAD_COMMAND", 6, OK_ABORT),
     ERROR_HEAD_EEPROM("error.ERROR_HEAD_EEPROM", 7, OK_ABORT),
     ERROR_BAD_FIRMWARE_FILE("error.ERROR_BAD_FIRMWARE_FILE", 8, OK_ABORT),
     ERROR_FLASH_CHECKSUM("error.ERROR_FLASH_CHECKSUM", 9, OK_ABORT),
     ERROR_GCODE_BUFFER_OVERRUN("error.ERROR_GCODE_BUFFER_OVERRUN", 10, OK_ABORT),
     ERROR_FILE_READ_CLOBBERED("error.ERROR_FILE_READ_CLOBBERED", 11, OK_ABORT),
     ERROR_MAX_GANTRY_ADJUSTMENT("error.ERROR_MAX_GANTRY_ADJUSTMENT", 12, OK_ABORT),
     ERROR_REEL0_EEPROM("error.ERROR_REEL0_EEPROM", 13, CLEAR_CONTINUE, ABORT),
     ERROR_E_FILAMENT_SLIP("error.ERROR_E_FILAMENT_SLIP", 14, CLEAR_CONTINUE, ABORT),
     ERROR_D_FILAMENT_SLIP("error.ERROR_D_FILAMENT_SLIP", 15, CLEAR_CONTINUE, ABORT),
     ERROR_NOZZLE_FLUSH_NEEDED("error.ERROR_NOZZLE_FLUSH_NEEDED", 16, CLEAR_CONTINUE, ABORT),
     ERROR_Z_TOP_SWITCH("error.ERROR_Z_TOP_SWITCH", 17, CLEAR_CONTINUE, ABORT),
     ERROR_B_STUCK("error.ERROR_B_STUCK", 18, OK_ABORT),
     ERROR_REEL1_EEPROM("error.ERROR_REEL1_EEPROM", 19, CLEAR_CONTINUE, ABORT),
     ERROR_HEAD_POWER_EEPROM("error.ERROR_HEAD_POWER_EEPROM", 20, CLEAR_CONTINUE, ABORT),
     ERROR_HEAD_POWER_OVERTEMP("error.ERROR_HEAD_POWER_OVERTEMP", 21, CLEAR_CONTINUE, ABORT),
     ERROR_UNKNOWN("error.ERROR_UNKNOWN", -1, OK_ABORT),
     ALL_ERRORS("", -99);

     private String errorText;
     private int bytePosition;
     private Set<SystemErrorHandlerOptions> options;

    private FirmwareError(String errorText, int bytePosition, SystemErrorHandlerOptions ... options)
    {
        this.errorText = errorText;
        this.bytePosition = bytePosition;
        this.options = new HashSet(Arrays.asList(options));
    }
    
    public String getLocalisedErrorTitle()
    {
        return Lookup.i18n(errorText);
    }
    
    public String getLocalisedErrorMessage()
    {
        return Lookup.i18n(errorText + ".message");
    }
    
    public int getBytePosition()
    {
        return bytePosition;
    }
    
    public static FirmwareError fromBytePosition(int bytePosition)
    {
        FirmwareError errorToReturn = FirmwareError.ERROR_UNKNOWN;
        
        for (FirmwareError error : FirmwareError.values())
        {
            if (error.getBytePosition() == bytePosition)
            {
                errorToReturn = error;
                break;
            }
        }
        
        return errorToReturn;
    }
}
