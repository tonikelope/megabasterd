package megabasterd;

/**
 *
 * @author tonikelope
 */
public interface TransferenceView {
    
    void pause();
    void stop();
    void resume();
    void updateSpeed(String speed, Boolean visible);
    void updateRemainingTime(String rem_time, Boolean visible);
    void updateProgressBar(long progress, double bar_rate);
    void printStatusNormal(String msg);
    void printStatusOK(String msg);
    void printStatusError(String msg);

}
