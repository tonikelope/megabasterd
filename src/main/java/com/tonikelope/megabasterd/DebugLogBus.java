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

import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JViewport;
import javax.swing.Timer;
import javax.swing.text.Document;

/**
 * In-process pipe that captures java.util.logging records and feeds them to
 * the "DEBUG LOG" tab in the main panel. Designed for the lowest-risk
 * possible coupling to the rest of MegaBasterd:
 *
 *   - We attach ONE JUL Handler to the root logger and that is it. We do
 *     NOT redirect System.out / System.err. A previous attempt teed those
 *     too and the resulting double-loop (ConsoleHandler -> System.err ->
 *     our tee -> our queue + our JUL handler -> same record again, plus
 *     a SwingUtilities.invokeLater per byte) flooded the EDT during
 *     startup and froze the UI.
 *   - We do NOT raise the root logger's Level. Whatever filter was in
 *     place before still applies; we just observe what's already flowing.
 *   - All log records arrive off-EDT and go onto a lock-free deque
 *     (capped at QUEUE_MAX entries; oldest is dropped on overflow). A
 *     single javax.swing.Timer on the EDT drains the deque every
 *     DRAIN_INTERVAL_MS, doing at most ONE textarea append per tick. The
 *     EDT therefore handles at most ~3 layout passes per second from
 *     this subsystem regardless of log volume.
 *   - The textarea is hard-capped at TEXTAREA_CAP_CHARS; the head is
 *     trimmed after each drain so memory stays bounded.
 *   - Auto-scroll-to-bottom only happens if the user is already at the
 *     bottom (within AUTOSCROLL_SLACK pixels). If they scrolled up to
 *     read something, the timer leaves their viewport alone.
 *
 * Public lifecycle: {@link #installJULHandler()} is safe to call from any
 * thread, idempotent, and should happen as early as possible (the queue
 * starts buffering immediately, so even pre-UI records are not lost).
 * {@link #bind(JTextArea)} must be called on the EDT after the textarea
 * has been added to its tab; it starts the drain timer.
 *
 * @author tonikelope
 */
public final class DebugLogBus {

    private DebugLogBus() {
    }

    private static final int QUEUE_MAX = 5_000;
    private static final int DRAIN_INTERVAL_MS = 300;
    private static final int DRAIN_BATCH_MAX = 500;
    private static final int TEXTAREA_CAP_CHARS = 500_000;
    /**
     * If the vertical scrollbar's current value is within this many pixels
     * of "fully scrolled down", we keep auto-scrolling on each drain. If
     * the user has scrolled higher (to look at older entries), we leave
     * their viewport alone.
     */
    private static final int AUTOSCROLL_SLACK_PX = 30;

    private static final ConcurrentLinkedDeque<String> PENDING = new ConcurrentLinkedDeque<>();
    private static final AtomicInteger PENDING_SIZE = new AtomicInteger(0);

    private static volatile JTextArea TEXTAREA = null;
    private static volatile Handler INSTALLED_HANDLER = null;
    private static volatile Timer DRAIN_TIMER = null;

    /**
     * Push a pre-formatted log line into the queue. Safe to call from any
     * thread. Drops the oldest entry if the queue is already at capacity --
     * we prefer to lose history than to OOM.
     */
    public static void enqueue(String line) {
        if (line == null || line.isEmpty()) {
            return;
        }
        // Bounded queue: drop oldest before adding.
        while (PENDING_SIZE.get() >= QUEUE_MAX) {
            if (PENDING.pollFirst() != null) {
                PENDING_SIZE.decrementAndGet();
            } else {
                break;
            }
        }
        PENDING.add(line);
        PENDING_SIZE.incrementAndGet();
    }

    /**
     * Add a JUL Handler that pushes every formatted record onto the queue.
     * Idempotent. Does NOT change the root logger's Level -- we just observe
     * whatever filter is currently in effect, so noisy callers don't
     * suddenly become visible.
     */
    public static synchronized void installJULHandler() {
        if (INSTALLED_HANDLER != null) {
            return;
        }
        Handler h = new Handler() {
            {
                setLevel(Level.ALL);
                setFormatter(new SimpleFormatter());
            }

            @Override
            public void publish(LogRecord record) {
                if (!isLoggable(record)) {
                    return;
                }
                try {
                    enqueue(getFormatter().format(record));
                } catch (Throwable ignore) {
                    // Never throw from inside a JUL handler.
                }
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        };
        Logger.getLogger("").addHandler(h);
        INSTALLED_HANDLER = h;
    }

    /**
     * Bind the textarea and start the drain timer. MUST be called on the
     * EDT (the timer fires on the EDT; the bind itself touches Swing
     * state so it has to be EDT-safe). Subsequent calls replace the
     * textarea (and restart the timer if needed).
     */
    public static void bind(JTextArea ta) {
        if (ta == null) {
            return;
        }
        TEXTAREA = ta;
        if (DRAIN_TIMER == null) {
            Timer t = new Timer(DRAIN_INTERVAL_MS, e -> drainOnce());
            t.setRepeats(true);
            t.setCoalesce(true);
            DRAIN_TIMER = t;
            t.start();
        }
    }

    /**
     * One drain tick: copy up to DRAIN_BATCH_MAX queued lines into the
     * textarea with a single append, trim the head if past cap, optionally
     * scroll to the bottom. Skips silently when there's no work or the
     * textarea is gone.
     */
    private static void drainOnce() {
        JTextArea ta = TEXTAREA;
        if (ta == null || PENDING_SIZE.get() == 0) {
            return;
        }

        StringBuilder sb = new StringBuilder(4096);
        int drained = 0;
        String line;
        while (drained < DRAIN_BATCH_MAX && (line = PENDING.pollFirst()) != null) {
            PENDING_SIZE.decrementAndGet();
            sb.append(line);
            drained++;
        }
        if (sb.length() == 0) {
            return;
        }

        // Track autoscroll BEFORE we append (otherwise the maximum has
        // already shifted and the check is meaningless).
        boolean stick_to_bottom = isAtBottom(ta);

        ta.append(sb.toString());

        // Trim head.
        Document doc = ta.getDocument();
        int len = doc.getLength();
        if (len > TEXTAREA_CAP_CHARS) {
            try {
                doc.remove(0, len - TEXTAREA_CAP_CHARS);
            } catch (Exception ignore) {
            }
        }

        if (stick_to_bottom) {
            ta.setCaretPosition(ta.getDocument().getLength());
        }
    }

    private static boolean isAtBottom(JTextArea ta) {
        java.awt.Container parent = ta.getParent();
        if (!(parent instanceof JViewport)) {
            return true;
        }
        java.awt.Container gp = parent.getParent();
        if (!(gp instanceof JScrollPane)) {
            return true;
        }
        JScrollBar vbar = ((JScrollPane) gp).getVerticalScrollBar();
        if (vbar == null) {
            return true;
        }
        return vbar.getValue() + vbar.getVisibleAmount()
                >= vbar.getMaximum() - AUTOSCROLL_SLACK_PX;
    }
}
