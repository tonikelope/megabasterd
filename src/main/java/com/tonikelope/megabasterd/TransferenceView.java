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
