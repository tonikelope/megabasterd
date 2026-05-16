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
    /**
     * Shorter watchdog timeout used when the stall is specifically because MEGA
     * returned HTTP 509 (bandwidth quota). The generic 600 s wait is far too
     * long for a quota stall: the user often clears it within a minute by
     * activating a VPN, and the IP-change-aware backoff (see
     * ChunkDownloader.B2) usually breaks out long before this fires. But when
     * the user has no VPN script and SmartProxy is disabled, this lets the
     * watchdog flag the download as auto-retryable in 3 min instead of 10.
     * Configurable via setting "quota_stall_timeout". (#751)
     */
    int QUOTA_STALL_TIMEOUT_DEFAULT = 180;
    boolean LIMIT_TRANSFERENCE_SPEED_DEFAULT = false;
    int MAX_TRANSFERENCE_SPEED_DEFAULT = 5;
    int MAX_WAIT_WORKERS_SHUTDOWN = 15;
    Integer[] FATAL_API_ERROR_CODES = {-2, -4, -8, -9, -14, -15, -16, -17, 22, 23, 24};
    /**
     * Subset of {@link #FATAL_API_ERROR_CODES} that arms the cleanup-path
     * auto-restart. The download stops, surfaces the explanation popup, then
     * re-arms itself after {@code RESTART_COUNTDOWN_SECS} (or
     * {@code RESTART_COUNTDOWN_SECS_OVERQUOTA} for -17, since MEGA's quota
     * window is on the order of tens of minutes -- a 3 s retry loop would
     * just hammer MEGA without giving the quota a chance to clear).
     */
    Integer[] FATAL_API_ERROR_CODES_WITH_RETRY = {-4, -17};
    int RESTART_COUNTDOWN_SECS = 3;
    int RESTART_COUNTDOWN_SECS_OVERQUOTA = 60;

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
