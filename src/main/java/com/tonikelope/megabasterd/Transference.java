package com.tonikelope.megabasterd;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 *
 * @author tonikelope
 */
public interface Transference {

    int MIN_WORKERS = 1;
    int MAX_WORKERS = 20;
    int HTTP_TIMEOUT = 15000;
    int HTTP_PROXY_TIMEOUT = 15000;
    int MAX_SIM_TRANSFERENCES = 20;
    int SIM_TRANSFERENCES_DEFAULT = 4;
    boolean LIMIT_TRANSFERENCE_SPEED_DEFAULT = false;
    int MAX_TRANSFERENCE_SPEED_DEFAULT = 5;
    int MAX_WAIT_WORKERS_SHUTDOWN = 15;
    Integer[] FATAL_ERROR_API_CODES = {-2, -8, -9, -10, -11, -12, -13, -14, -15, -16, 22, 23, 24, 25};

    void start();

    void stop();

    void pause();

    void restart();

    void close();

    boolean isPaused();

    boolean isStopped();

    boolean isFrozen();

    void unfreeze();

    void upWaitQueue();

    void downWaitQueue();

    void checkSlotsAndWorkers();

    ConcurrentLinkedQueue<Long> getPartialProgress();

    long getProgress();

    void setProgress(long progress);

    String getFile_name();

    long getFile_size();

    ProgressMeter getProgress_meter();

    MainPanel getMain_panel();

    TransferenceView getView();

    boolean isStatusError();

    int getSlotsCount();

}
