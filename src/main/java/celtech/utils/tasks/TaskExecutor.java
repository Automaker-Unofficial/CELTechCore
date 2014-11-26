package celtech.utils.tasks;

/**
 *
 * @author Ian
 */
public interface TaskExecutor
{
    
    /**
     * A point interface (ie only one method) that takes no arguments and returns void.
     */
    public interface NoArgsConsumer {
        void run() throws Exception;
    }
    
    public void respondOnGUIThread(TaskResponder responder, boolean success, String message);
    public void respondOnGUIThread(TaskResponder responder, boolean success, String message, Object returnedObject);
    public void respondOnCurrentThread(TaskResponder responder, boolean success, String message);
    public void runOnGUIThread(Runnable runnable);

    /**
     * Run the given action in a background thread, using the given success and failure handlers.
     * @param action The function to call that performs the action.
     * @param successHandler This will be run if the function succeeds (i.e. does not raise an Exception).
     * @param failureHandler This will be run if the function fails (i.e. raises an Exception).
     * @param taskName The name of the task, which is used as the name of the thread
     */
    public void runAsTask(NoArgsConsumer action, NoArgsConsumer successHandler,
        NoArgsConsumer failureHandler, String taskName);
}
