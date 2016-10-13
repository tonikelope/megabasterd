package megabasterd;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 *
 * @author tonikelope
 */
public interface Transference {
    
    int MIN_WORKERS = 2;
    int MAX_WORKERS = 20;
    int MAX_SIM_TRANSFERENCES=20;
    boolean USE_SLOTS_DEFAULT = false;
    int WORKERS_DEFAULT = 4;
    int SIM_TRANSFERENCES_DEFAULT=2;
    boolean LIMIT_TRANSFERENCE_SPEED_DEFAULT=false;
    int MAX_TRANSFERENCE_SPEED_DEFAULT=5;
    
    void start();
   
    void stop();
    
    void pause();
    
    void restart();
    
    void close();
    
    boolean isPaused();
    
    boolean isStopped();
    
    void checkSlotsAndWorkers();
    
    ConcurrentLinkedQueue<Integer> getPartialProgress();
    
    long getProgress();
    
    void setProgress(long progress);
    
    String getFile_name();
    
    long getFile_size();
    
    SpeedMeter getSpeed_meter();
    
    ProgressMeter getProgress_meter();
    
    MainPanel getMain_panel();
    
    TransferenceView getView();

}
