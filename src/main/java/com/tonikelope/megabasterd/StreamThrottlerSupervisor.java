package com.tonikelope.megabasterd;

import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author tonikelope
 */
public final class StreamThrottlerSupervisor implements Runnable, SecureMultiThreadNotifiable {

    private static final Logger LOG = Logger.getLogger(StreamThrottlerSupervisor.class.getName());

    private ConcurrentLinkedQueue<Integer> _input_slice_queue, _output_slice_queue;

    private final int _slice_size;

    private volatile int _maxBytesPerSecInput;

    private volatile int _maxBytesPerSecOutput;

    private volatile boolean _queue_swapping;

    private final Object _secure_notify_lock;

    private final Object _timer_lock;

    private final ConcurrentHashMap<Thread, Boolean> _notified_threads;

    public StreamThrottlerSupervisor(int maxBytesPerSecInput, int maxBytesPerSecOutput, int slice_size) {

        _secure_notify_lock = new Object();

        _timer_lock = new Object();

        _queue_swapping = false;

        _maxBytesPerSecInput = maxBytesPerSecInput;

        _maxBytesPerSecOutput = maxBytesPerSecOutput;

        _slice_size = slice_size;

        _input_slice_queue = new ConcurrentLinkedQueue<>();

        _output_slice_queue = new ConcurrentLinkedQueue<>();

        _notified_threads = new ConcurrentHashMap<>();

    }

    public int getMaxBytesPerSecInput() {
        return _maxBytesPerSecInput;
    }

    public void setMaxBytesPerSecInput(int maxBytesPerSecInput) {
        _maxBytesPerSecInput = maxBytesPerSecInput;
    }

    public int getMaxBytesPerSecOutput() {
        return _maxBytesPerSecOutput;
    }

    public void setMaxBytesPerSecOutput(int maxBytesPerSecOutput) {
        _maxBytesPerSecOutput = maxBytesPerSecOutput;
    }

    public ConcurrentLinkedQueue<Integer> getInput_slice_queue() {
        return _input_slice_queue;
    }

    public ConcurrentLinkedQueue<Integer> getOutput_slice_queue() {
        return _output_slice_queue;
    }

    public boolean isQueue_swapping() {
        return _queue_swapping;
    }

    @Override
    public void secureWait() {

        synchronized (_secure_notify_lock) {

            Thread current_thread = Thread.currentThread();

            if (!_notified_threads.containsKey(current_thread)) {

                _notified_threads.put(current_thread, false);
            }

            while (!_notified_threads.get(current_thread)) {

                try {
                    _secure_notify_lock.wait();
                } catch (InterruptedException ex) {
                    LOG.log(Level.SEVERE, ex.getMessage());
                }
            }

            _notified_threads.put(current_thread, false);
        }
    }

    @Override
    public void secureNotifyAll() {

        synchronized (_secure_notify_lock) {

            for (Map.Entry<Thread, Boolean> entry : _notified_threads.entrySet()) {

                entry.setValue(true);
            }

            _secure_notify_lock.notifyAll();
        }
    }

    @Override
    public void run() {

        Timer timer = new Timer();

        TimerTask task = new TimerTask() {

            @Override
            public void run() {
                synchronized (_timer_lock) {

                    _timer_lock.notify();
                }
            }
        };

        ConcurrentLinkedQueue<Integer> old_input_queue, new_input_queue, old_output_queue, new_output_queue;

        old_input_queue = new ConcurrentLinkedQueue<>();

        old_output_queue = new ConcurrentLinkedQueue<>();

        new_input_queue = _resetSliceQueue(old_input_queue, _maxBytesPerSecInput);

        new_output_queue = _resetSliceQueue(old_output_queue, _maxBytesPerSecOutput);

        timer.schedule(task, 0, 1000);

        while (true) {

            _queue_swapping = true;

            old_input_queue = _input_slice_queue;

            old_output_queue = _output_slice_queue;

            _input_slice_queue = new_input_queue;

            _output_slice_queue = new_output_queue;

            _queue_swapping = false;

            secureNotifyAll();

            new_input_queue = _resetSliceQueue(old_input_queue, _maxBytesPerSecInput);

            new_output_queue = _resetSliceQueue(old_output_queue, _maxBytesPerSecOutput);

            synchronized (_timer_lock) {

                try {
                    _timer_lock.wait();
                } catch (InterruptedException ex) {
                    LOG.log(Level.SEVERE, ex.getMessage());
                }
            }
        }
    }

    private ConcurrentLinkedQueue<Integer> _resetSliceQueue(ConcurrentLinkedQueue<Integer> queue, int max_bytes) {

        if (max_bytes > 0) {

            queue.clear();

            int slice_num = (int) Math.floor((double) max_bytes / _slice_size);

            for (int i = 0; i < slice_num; i++) {
                queue.add(_slice_size);
            }

            if (max_bytes % _slice_size != 0) {

                queue.add(max_bytes % _slice_size);
            }
        }

        return queue;
    }

}
