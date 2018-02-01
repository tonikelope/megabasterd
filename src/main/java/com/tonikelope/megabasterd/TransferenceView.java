package com.tonikelope.megabasterd;

/**
 *
 * @author tonikelope
 */
public interface TransferenceView {

    void pause();

    void stop(String status);

    void resume();

    void updateSpeed(String speed, Boolean visible);

    void updateProgressBar(long progress, double bar_rate);

    void updateProgressBar(int value);

    void updateSlotsStatus();

    int getSlots();

    void printStatusNormal(String msg);

    void printStatusOK(String msg);

    void printStatusError(String msg);

}
