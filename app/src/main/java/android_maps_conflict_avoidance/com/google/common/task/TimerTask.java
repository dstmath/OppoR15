package android_maps_conflict_avoidance.com.google.common.task;

import android_maps_conflict_avoidance.com.google.common.Config;

public class TimerTask extends Task {
    private int count;
    private long deadline;
    private long delay;
    private boolean isFixedRate;
    private long period;
    private long scheduled;

    public TimerTask(TaskRunner runner, Runnable runnable) {
        this(runner, runnable, null);
    }

    public TimerTask(TaskRunner runner, Runnable runnable, String name) {
        super(runner, runnable, name);
        this.delay = 0;
        this.deadline = -1;
        this.period = -1;
        this.scheduled = -1;
        this.count = -1;
        this.isFixedRate = false;
    }

    public void schedule() {
        synchronized (this) {
            if ((this.count == -1 || this.count > 0) && this.scheduled == -1) {
                if (this.deadline == -1) {
                    this.scheduled = Config.getInstance().getClock().currentTimeMillis() + this.delay;
                } else {
                    this.scheduled = this.deadline + this.delay;
                }
            }
        }
        this.runner.scheduleTask(this);
    }

    synchronized void scheduleInternal() {
        if (this.scheduled != -1) {
            this.runner.scheduleTimerTaskInternal(this);
        }
    }

    synchronized int cancelInternal() {
        int i;
        if (this.runner.cancelTaskInternal(this)) {
            this.scheduled = -1;
            i = this.count;
        } else if (this.scheduled != -1) {
            this.scheduled = -1;
            i = this.count;
        } else {
            i = 0;
        }
        return i;
    }

    public synchronized long getScheduledTime() {
        return this.scheduled;
    }

    public synchronized boolean isUnscheduled() {
        return this.scheduled == -1;
    }

    public synchronized void setDelay(long delay) {
        this.delay = delay;
    }

    void runInternal() {
        synchronized (this) {
            if (this.period == -1) {
                this.scheduled = -1;
            } else {
                if (this.count > 0) {
                    this.count--;
                }
                if (this.count == 0) {
                    this.scheduled = -1;
                } else if (this.isFixedRate) {
                    this.scheduled += this.period;
                } else {
                    this.scheduled = Config.getInstance().getClock().currentTimeMillis() + this.period;
                }
            }
        }
        super.runInternal();
        this.runner.scheduleTask(this);
    }
}
