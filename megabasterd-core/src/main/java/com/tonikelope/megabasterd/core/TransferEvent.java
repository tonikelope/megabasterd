package com.tonikelope.megabasterd.core;

public final class TransferEvent implements CoreEvent {

    public enum Direction {
        DOWNLOAD,
        UPLOAD
    }

    public enum Action {
        SNAPSHOT,
        ADDED,
        STARTED,
        PAUSED,
        RESUMED,
        FINISHED,
        CANCELED,
        FAILED
    }

    private final long timestampMillis;
    private final Direction direction;
    private final Action action;
    private final int preprocessingCount;
    private final int provisionCount;
    private final int waitStartCount;
    private final int runningCount;
    private final int finishedCount;
    private final int removingCount;
    private final String statusText;

    public TransferEvent(Direction direction, Action action, int preprocessingCount,
            int provisionCount, int waitStartCount, int runningCount,
            int finishedCount, int removingCount, String statusText) {
        this(System.currentTimeMillis(), direction, action, preprocessingCount,
                provisionCount, waitStartCount, runningCount, finishedCount,
                removingCount, statusText);
    }

    public TransferEvent(long timestampMillis, Direction direction, Action action,
            int preprocessingCount, int provisionCount, int waitStartCount,
            int runningCount, int finishedCount, int removingCount, String statusText) {
        this.timestampMillis = timestampMillis;
        this.direction = direction != null ? direction : Direction.DOWNLOAD;
        this.action = action != null ? action : Action.SNAPSHOT;
        this.preprocessingCount = preprocessingCount;
        this.provisionCount = provisionCount;
        this.waitStartCount = waitStartCount;
        this.runningCount = runningCount;
        this.finishedCount = finishedCount;
        this.removingCount = removingCount;
        this.statusText = statusText != null ? statusText : "";
    }

    @Override
    public String type() {
        return "transfer";
    }

    @Override
    public long timestampMillis() {
        return timestampMillis;
    }

    public Direction direction() {
        return direction;
    }

    public Action action() {
        return action;
    }

    public int preprocessingCount() {
        return preprocessingCount;
    }

    public int provisionCount() {
        return provisionCount;
    }

    public int waitStartCount() {
        return waitStartCount;
    }

    public int runningCount() {
        return runningCount;
    }

    public int finishedCount() {
        return finishedCount;
    }

    public int removingCount() {
        return removingCount;
    }

    public String statusText() {
        return statusText;
    }
}
