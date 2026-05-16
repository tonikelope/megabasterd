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

import static com.tonikelope.megabasterd.MainPanel.GUI_FONT;
import static com.tonikelope.megabasterd.MiscTools.translateLabels;
import static com.tonikelope.megabasterd.MiscTools.updateFonts;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;
import javax.swing.border.LineBorder;

/**
 *
 * @author tonikelope
 */
public class WarningExitMessage extends javax.swing.JDialog {

    private final MainPanel _main_panel;
    private final boolean _restart;

    private JLabel _title_label;
    private JLabel _remaining_label;
    private JLabel _downloads_label;
    private JLabel _uploads_label;
    private JLabel _db_label;
    private JLabel _timer_label;
    private JProgressBar _progress_bar;
    private JButton _exit_button;

    private volatile int _initial_workers = 0;

    public WarningExitMessage(java.awt.Frame parent, boolean modal, MainPanel main_panel, boolean restart) {
        super(parent, modal);
        _main_panel = main_panel;
        _restart = restart;

        MiscTools.GUIRunAndWait(() -> {
            initComponents();
            updateFonts(this, GUI_FONT, main_panel.getZoom_factor());
            translateLabels(this);
            pack();
        });
    }

    private void initComponents() {

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("Exit");
        setUndecorated(true);

        JPanel content = new JPanel(new BorderLayout(10, 10));
        content.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(Color.BLACK, 1, true),
                BorderFactory.createEmptyBorder(15, 15, 15, 15)));

        // --- Header ---
        _title_label = new JLabel("MegaBasterd is stopping transferences safely, please wait...");
        _title_label.setFont(new Font("Dialog", Font.BOLD, 16));
        content.add(_title_label, BorderLayout.NORTH);

        // --- Center: counts + progress bar ---
        JPanel center = new JPanel();
        center.setLayout(new GridLayout(0, 1, 4, 4));

        _remaining_label = new JLabel("Transferences remaining: --");
        _remaining_label.setFont(new Font("Dialog", Font.BOLD, 18));
        _remaining_label.setHorizontalAlignment(SwingConstants.CENTER);
        center.add(_remaining_label);

        _progress_bar = new JProgressBar(0, 100);
        _progress_bar.setStringPainted(true);
        _progress_bar.setString("Draining workers...");
        _progress_bar.setPreferredSize(new Dimension(420, 22));
        center.add(_progress_bar);

        _downloads_label = new JLabel("↓ Downloads: --   (-- workers)");
        _downloads_label.setFont(new Font("Dialog", Font.PLAIN, 13));
        center.add(_downloads_label);

        _uploads_label = new JLabel("↑ Uploads: --   (-- workers)");
        _uploads_label.setFont(new Font("Dialog", Font.PLAIN, 13));
        center.add(_uploads_label);

        _db_label = new JLabel("Saving queue to database...");
        _db_label.setFont(new Font("Dialog", Font.ITALIC, 12));
        center.add(_db_label);

        _timer_label = new JLabel(" ");
        _timer_label.setFont(new Font("Dialog", Font.PLAIN, 12));
        _timer_label.setHorizontalAlignment(SwingConstants.CENTER);
        center.add(_timer_label);

        content.add(center, BorderLayout.CENTER);

        // --- Footer: EXIT NOW ---
        _exit_button = new JButton("EXIT NOW");
        _exit_button.setBackground(new Color(255, 0, 0));
        _exit_button.setForeground(Color.WHITE);
        _exit_button.setFont(new Font("Dialog", Font.BOLD, 16));
        _exit_button.addActionListener((evt) -> exit_buttonActionPerformed());

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        footer.add(_exit_button);
        content.add(footer, BorderLayout.SOUTH);

        setContentPane(content);
        pack();
    }

    /**
     * Pushed by MainPanel's shutdown loop every iteration. All field updates
     * happen on the EDT, so callers can fire this from a worker thread.
     *
     * @param dl_count Number of running downloads remaining.
     * @param dl_workers Total chunk-downloader workers still alive across all
     * downloads.
     * @param ul_count Number of running uploads remaining.
     * @param ul_workers Total chunk-uploader workers still alive across all
     * uploads.
     * @param elapsed_ms Milliseconds since shutdown started.
     * @param timeout_ms Hard-timeout after which MainPanel forces exit.
     */
    public void updateStatus(int dl_count, int dl_workers, int ul_count, int ul_workers, long elapsed_ms, long timeout_ms) {

        int total_workers = dl_workers + ul_workers;
        int total_transfers = dl_count + ul_count;

        // The initial worker count is captured on the FIRST call so the
        // progress bar can show a meaningful "drained" percentage. Without
        // a baseline, percent would jump around as workers exit in batches.
        if (_initial_workers == 0 && total_workers > 0) {
            _initial_workers = total_workers;
        }

        final int pct;
        if (_initial_workers > 0) {
            int drained = _initial_workers - total_workers;
            if (drained < 0) {
                drained = 0;
            }
            pct = Math.min(100, (int) Math.round(100.0 * drained / _initial_workers));
        } else {
            pct = 100;
        }

        final long remaining_ms = Math.max(0, timeout_ms - elapsed_ms);

        MiscTools.GUIRun(() -> {
            _remaining_label.setText(I18n.tr("ui.dynamic.transferences_remaining", total_transfers, total_workers));
            _downloads_label.setText(I18n.tr("ui.dynamic.downloads_summary", dl_count, dl_workers));
            _uploads_label.setText(I18n.tr("ui.dynamic.uploads_summary", ul_count, ul_workers));
            _progress_bar.setValue(pct);
            _progress_bar.setString(I18n.tr("ui.dynamic.drained_percent", pct));
            _timer_label.setText(I18n.tr("ui.dynamic.force_exit_timer", elapsed_ms / 1000, remaining_ms / 1000));
        });
    }

    /**
     * Mark the "queue saved to DB" line as done. Called from MainPanel once the
     * upfront persistence step has finished.
     */
    public void setDbSaved(boolean ok) {
        MiscTools.GUIRun(() -> {
            if (ok) {
                _db_label.setText("Queue saved to database ✓");
                _db_label.setForeground(new Color(0, 128, 0));
            } else {
                _db_label.setText("Failed to save queue to database!");
                _db_label.setForeground(Color.RED);
            }
        });
    }

    private void exit_buttonActionPerformed() {
        _main_panel.byebyenow(_restart);
    }
}
