package celtech.printerControl.comms;

/**
 *
 * @author Ian Hudson @ Liberty Systems Limited
 */
public interface PrinterStatusConsumer
{
    /**
     *
     * @param printerHandle
     */
    public void printerConnected(DetectedDevice printerHandle);

    /**
     *
     * @param printerHandle
     */
    public void failedToConnect(DetectedDevice printerHandle);

    /**
     *
     * @param printerHandle
     */
    public void disconnected(DetectedDevice printerHandle);  
}
