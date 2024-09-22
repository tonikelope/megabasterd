/*
 __  __                  _               _               _ 
|  \/  | ___  __ _  __ _| |__   __ _ ___| |_ ___ _ __ __| |
| |\/| |/ _ \/ _` |/ _` | '_ \ / _` / __| __/ _ \ '__/ _` |
| |  | |  __/ (_| | (_| | |_) | (_| \__ \ ||  __/ | | (_| |
|_|  |_|\___|\__, |\__,_|_.__/ \__,_|___/\__\___|_|  \__,_|
             |___/                                         
© Perpetrated by tonikelope since 2016
 */
package com.tonikelope.megabasterd;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 *
 * @author tonikelope
 */
public interface Transference {

    int MIN_WORKERS = 1;
    int MAX_WORKERS = 20;
    int HTTP_PROXY_TIMEOUT = 5000;
    int HTTP_CONNECT_TIMEOUT = 60000;
    int HTTP_READ_TIMEOUT = 60000;
    int MAX_SIM_TRANSFERENCES = 50;
    int SIM_TRANSFERENCES_DEFAULT = 4;
    int PROGRESS_WATCHDOG_TIMEOUT = 600;
    boolean LIMIT_TRANSFERENCE_SPEED_DEFAULT = false;
    int MAX_TRANSFERENCE_SPEED_DEFAULT = 5;
    int MAX_WAIT_WORKERS_SHUTDOWN = 15;
    Integer[] FATAL_API_ERROR_CODES = {-2, -4, -8, -14, -15, -16, -17, 22, 23, 24};
    Integer[] FATAL_API_ERROR_CODES_WITH_RETRY = {-4};

    void start();

    void stop();

    void pause();

    void restart();

    void close();

    boolean isPriority();

    boolean isPaused();

    boolean isStopped();

    boolean isFrozen();

    boolean isRestart();

    boolean isCanceled();

    boolean isClosed();

    void unfreeze();

    void upWaitQueue();

    void downWaitQueue();

    void bottomWaitQueue();

    void topWaitQueue();

    void checkSlotsAndWorkers();

    ConcurrentLinkedQueue<Long> getPartialProgress();

    long getProgress();

    void setProgress(long progress);

    long getSpeed();

    void setSpeed(long speed);

    String getFile_name();

    long getFile_size();

    ProgressMeter getProgress_meter();

    MainPanel getMain_panel();

    TransferenceView getView();

    boolean isStatusError();

    int getSlotsCount();

    int getPausedWorkers();

    int getTotWorkers();

}
