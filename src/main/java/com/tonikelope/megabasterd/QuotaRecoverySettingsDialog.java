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
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;

/**
 * Handwritten settings dialog for the 509 / quota / SmartProxy knobs added in
 * #751. Lives outside the main NetBeans-form SettingsDialog so we don't have to
 * extend a 5400-line generated form to surface three new settings; the
 * trade-off is that this dialog has a simpler visual style than the tabbed
 * SettingsDialog. Reachable from the main menu's Edit submenu.
 *
 * @author tonikelope
 */
public class QuotaRecoverySettingsDialog extends JDialog {

    private static final Logger LOG = Logger.getLogger(QuotaRecoverySettingsDialog.class.getName());

    private final MainPanel _main_panel;

    private JCheckBox _auto_resume_ip_checkbox;
    private JSpinner _quota_stall_spinner;
    private JSpinner _recheck_509_spinner;

    private JLabel _current_ip_label;
    private JButton _refresh_ip_button;

    private JTextArea _proxy_test_output;
    private JButton _test_proxy_button;

    private JButton _save_button;
    private JButton _cancel_button;

    public QuotaRecoverySettingsDialog(java.awt.Frame parent, boolean modal, MainPanel main_panel) {
        super(parent, modal);
        _main_panel = main_panel;

        MiscTools.GUIRunAndWait(() -> {
            initComponents();
            updateFonts(this, GUI_FONT, main_panel.getZoom_factor());
            translateLabels(this);
            loadFromDB();
            pack();
        });
    }

    private void initComponents() {

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setTitle("Quota recovery & SmartProxy");

        JPanel content = new JPanel(new BorderLayout(10, 10));
        content.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // --- Top: numeric / boolean settings (GridBag for tidy alignment) ---
        JPanel settings_panel = new JPanel(new GridBagLayout());
        settings_panel.setBorder(BorderFactory.createTitledBorder("Recovery behaviour"));

        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(4, 6, 4, 6);
        g.anchor = GridBagConstraints.WEST;
        g.fill = GridBagConstraints.HORIZONTAL;

        _auto_resume_ip_checkbox = new JCheckBox("Auto-resume downloads on IP change (VPN)");
        _auto_resume_ip_checkbox.setToolTipText("<html>When enabled, ChunkDownloader workers in 509 backoff<br>"
                + "re-check the public IP every 30 s and break out of the backoff<br>"
                + "immediately if the IP changed -- e.g. you activated a VPN.<br>"
                + "Disable only if your network has unstable IP detection.</html>");
        g.gridx = 0;
        g.gridy = 0;
        g.gridwidth = 2;
        settings_panel.add(_auto_resume_ip_checkbox, g);
        g.gridwidth = 1;

        JLabel l1 = new JLabel("Quota stall watchdog timeout (s):");
        l1.setToolTipText("<html>When at least one worker is in 509 backoff and no<br>"
                + "chunk has landed for this many seconds, the watchdog flags<br>"
                + "the download for auto-retry. Lower = recover faster after a<br>"
                + "real quota; higher = tolerate brief 509 hiccups. (30 .. 3600)</html>");
        g.gridx = 0;
        g.gridy = 1;
        settings_panel.add(l1, g);
        _quota_stall_spinner = new JSpinner(new SpinnerNumberModel(Transference.QUOTA_STALL_TIMEOUT_DEFAULT, 30, 3600, 30));
        ((JSpinner.DefaultEditor) _quota_stall_spinner.getEditor()).getTextField().setEditable(true);
        g.gridx = 1;
        g.gridy = 1;
        settings_panel.add(_quota_stall_spinner, g);

        JLabel l2 = new JLabel("SmartProxy 509 recheck window (s):");
        l2.setToolTipText("<html>How long after a 509 the SmartProxy logic stays armed<br>"
                + "for this download even if individual chunks succeed.<br>"
                + "MEGA's bandwidth quotas reset on a window the user can't<br>"
                + "observe directly; was hardcoded to 3600 s before. (60 .. 86400)</html>");
        g.gridx = 0;
        g.gridy = 2;
        settings_panel.add(l2, g);
        _recheck_509_spinner = new JSpinner(new SpinnerNumberModel(SmartMegaProxyManager.RECHECK_509_WINDOW_DEFAULT, 60, 86400, 60));
        ((JSpinner.DefaultEditor) _recheck_509_spinner.getEditor()).getTextField().setEditable(true);
        g.gridx = 1;
        g.gridy = 2;
        settings_panel.add(_recheck_509_spinner, g);

        content.add(settings_panel, BorderLayout.NORTH);

        // --- Middle: live IP + proxy test ---
        JPanel diag_panel = new JPanel(new BorderLayout(8, 8));
        diag_panel.setBorder(BorderFactory.createTitledBorder("Diagnostics"));

        JPanel ip_row = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        ip_row.add(new JLabel("Current public IP:"));
        _current_ip_label = new JLabel("(checking...)");
        _current_ip_label.setFont(new Font(Font.MONOSPACED, Font.BOLD, 14));
        ip_row.add(_current_ip_label);
        _refresh_ip_button = new JButton("Refresh");
        _refresh_ip_button.addActionListener(e -> refreshPublicIp());
        ip_row.add(_refresh_ip_button);
        diag_panel.add(ip_row, BorderLayout.NORTH);

        JPanel test_row = new JPanel(new BorderLayout(0, 4));
        JPanel test_btn_panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        _test_proxy_button = new JButton("Test SmartProxy list");
        _test_proxy_button.setToolTipText("<html>Parse the configured SmartProxy list (or remote URL)<br>"
                + "and attempt a 3 s TCP connect to each entry. Reports which<br>"
                + "are reachable. Does not actually proxy a MEGA request.</html>");
        _test_proxy_button.addActionListener(e -> testProxyList());
        test_btn_panel.add(_test_proxy_button);
        test_row.add(test_btn_panel, BorderLayout.NORTH);

        _proxy_test_output = new JTextArea(8, 50);
        _proxy_test_output.setEditable(false);
        _proxy_test_output.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        _proxy_test_output.setLineWrap(false);
        JScrollPane test_scroll = new JScrollPane(_proxy_test_output);
        test_scroll.setPreferredSize(new Dimension(520, 180));
        test_row.add(test_scroll, BorderLayout.CENTER);
        diag_panel.add(test_row, BorderLayout.CENTER);

        content.add(diag_panel, BorderLayout.CENTER);

        // --- Bottom: Save / Cancel ---
        _save_button = new JButton("Save");
        _save_button.setBackground(new Color(60, 140, 60));
        _save_button.setForeground(Color.WHITE);
        _save_button.addActionListener(e -> {
            saveToDB();
            dispose();
        });

        _cancel_button = new JButton("Cancel");
        _cancel_button.addActionListener(e -> dispose());

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        footer.add(_cancel_button);
        footer.add(_save_button);

        JLabel help = new JLabel("<html><i>These settings only affect HTTP 509 (MEGA bandwidth quota) recovery; "
                + "the main SmartProxy list itself lives in Settings &rarr; SmartProxy.</i></html>");
        help.setForeground(new Color(110, 110, 110));
        help.setHorizontalAlignment(SwingConstants.CENTER);
        help.setBorder(BorderFactory.createEmptyBorder(8, 4, 0, 4));

        // Nest footer + help in one SOUTH slot. Separate SOUTH and PAGE_END
        // constraints collide because the relative PAGE_END overrides the
        // absolute SOUTH in BorderLayout, which hides the Save/Cancel row.
        JPanel south = new JPanel(new BorderLayout());
        south.add(footer, BorderLayout.CENTER);
        south.add(help, BorderLayout.SOUTH);
        content.add(south, BorderLayout.SOUTH);

        setContentPane(content);
        pack();

        // Kick off the public-IP fetch on a worker thread so the dialog shows
        // up immediately instead of waiting for the HTTP roundtrip.
        new Thread(this::refreshPublicIp, "QuotaRecoverySettings-IPInit").start();
    }

    private void loadFromDB() {
        String auto_resume = DBTools.selectSettingValue("auto_resume_ip_change");
        // Default: ON. Most users want this behaviour; the toggle is for
        // niche setups (e.g. NAT-behind-NAT where the public IP detection
        // would mis-trigger).
        _auto_resume_ip_checkbox.setSelected(auto_resume == null || auto_resume.equals("yes"));

        String stall = DBTools.selectSettingValue("quota_stall_timeout");
        int stall_v = Transference.QUOTA_STALL_TIMEOUT_DEFAULT;
        if (stall != null) {
            try {
                int v = Integer.parseInt(stall);
                if (v >= 30 && v <= 3600) {
                    stall_v = v;
                }
            } catch (NumberFormatException ignore) {
            }
        }
        _quota_stall_spinner.setValue(stall_v);

        String recheck = DBTools.selectSettingValue("smart_proxy_509_recheck_window");
        int recheck_v = SmartMegaProxyManager.RECHECK_509_WINDOW_DEFAULT;
        if (recheck != null) {
            try {
                int v = Integer.parseInt(recheck);
                if (v >= 60 && v <= 86400) {
                    recheck_v = v;
                }
            } catch (NumberFormatException ignore) {
            }
        }
        _recheck_509_spinner.setValue(recheck_v);
    }

    private void saveToDB() {
        try {
            DBTools.insertSettingValue("auto_resume_ip_change", _auto_resume_ip_checkbox.isSelected() ? "yes" : "no");
            DBTools.insertSettingValue("quota_stall_timeout", String.valueOf((Integer) _quota_stall_spinner.getValue()));
            DBTools.insertSettingValue("smart_proxy_509_recheck_window", String.valueOf((Integer) _recheck_509_spinner.getValue()));

            // Force the live SmartProxy manager to re-read its DB-backed
            // settings so the new recheck_509_window takes effect on the
            // next 509 without restart.
            SmartMegaProxyManager pm = MainPanel.getProxy_manager();
            if (pm != null) {
                pm.refreshSmartProxySettings();
            }
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, "Could not save quota settings: {0}", ex.getMessage());
        }
    }

    private void refreshPublicIp() {
        MiscTools.GUIRun(() -> {
            _current_ip_label.setText("(checking...)");
            _refresh_ip_button.setEnabled(false);
        });

        // Force a fresh fetch (bypass the 30 s cache) so the user can verify
        // their VPN took effect.
        MainPanel.invalidatePublicIpCache();
        String ip = MainPanel.getCachedPublicIp();

        MiscTools.GUIRun(() -> {
            _current_ip_label.setText(ip != null ? ip : "(unavailable)");
            _refresh_ip_button.setEnabled(true);
        });
    }

    /**
     * Parses the configured SmartProxy list (custom or URL-fetched) and
     * attempts a 3 s TCP connect to each entry. Writes the results into the
     * output text area. Runs on a worker thread; the test button is disabled
     * for the duration.
     */
    private void testProxyList() {
        _test_proxy_button.setEnabled(false);
        _proxy_test_output.setText("Loading proxy list...\n");

        new Thread(() -> {
            try {
                // Grab whatever's currently in the live proxy manager. That
                // accounts for both the custom-list and URL-fetched paths
                // without us re-implementing the parser. If the manager
                // hasn't refreshed yet, force one.
                SmartMegaProxyManager pm = MainPanel.getProxy_manager();
                if (pm == null) {
                    appendOutput("[ERROR] SmartProxy is not initialised. Enable it in Settings -> SmartProxy first.\n");
                    return;
                }

                // The manager exposes neither the raw list nor a direct
                // iterator (kept private to avoid concurrent-mod hazards).
                // We probe by repeatedly asking getProxy() with an
                // ever-growing exclusion list; this hits the same code
                // path real workers use.
                ArrayList<String> excluded = new ArrayList<>();
                LinkedHashMap<String, Boolean> results = new LinkedHashMap<>();

                int safety_cap = 256;
                while (safety_cap-- > 0) {
                    String[] entry;
                    try {
                        entry = pm.getProxy(excluded);
                    } catch (Exception ex) {
                        appendOutput("[ERROR] getProxy threw: " + ex.getMessage() + "\n");
                        break;
                    }
                    if (entry == null || entry[0] == null) {
                        break;
                    }
                    String addr = entry[0];
                    if (results.containsKey(addr)) {
                        // getProxy rotated back -- list exhausted.
                        break;
                    }
                    excluded.add(addr);
                    results.put(addr, null);
                }

                if (results.isEmpty()) {
                    appendOutput("[INFO] No proxies in the live SmartProxy pool (list may be empty, stale, or all banned).\n");
                    return;
                }

                appendOutput("Testing " + results.size() + " proxy entries (3 s TCP connect timeout each)...\n");
                appendOutput("--------------------------------------------------------------\n");

                int ok = 0, fail = 0;
                for (String addr : new ArrayList<>(results.keySet())) {
                    String[] parts = addr.split(":");
                    if (parts.length != 2) {
                        appendOutput(String.format("  %-32s  [SKIP] malformed%n", addr));
                        fail++;
                        results.put(addr, false);
                        continue;
                    }
                    int port;
                    try {
                        port = Integer.parseInt(parts[1]);
                        if (port < 1 || port > 65535) {
                            throw new NumberFormatException("range");
                        }
                    } catch (NumberFormatException ex) {
                        appendOutput(String.format("  %-32s  [SKIP] bad port (%s)%n", addr, parts[1]));
                        fail++;
                        results.put(addr, false);
                        continue;
                    }

                    long t0 = System.currentTimeMillis();
                    try (Socket s = new Socket()) {
                        s.connect(new InetSocketAddress(parts[0], port), 3000);
                        long dt = System.currentTimeMillis() - t0;
                        appendOutput(String.format("  %-32s  [OK]   %d ms%n", addr, dt));
                        ok++;
                        results.put(addr, true);
                    } catch (Exception ex) {
                        long dt = System.currentTimeMillis() - t0;
                        appendOutput(String.format("  %-32s  [FAIL] %s (%d ms)%n", addr, ex.getClass().getSimpleName(), dt));
                        fail++;
                        results.put(addr, false);
                    }
                }
                appendOutput("--------------------------------------------------------------\n");
                appendOutput("Summary: " + ok + " reachable, " + fail + " failed.\n");
            } finally {
                MiscTools.GUIRun(() -> _test_proxy_button.setEnabled(true));
            }
        }, "QuotaRecoverySettings-ProxyTest").start();
    }

    private void appendOutput(String s) {
        MiscTools.GUIRun(() -> {
            _proxy_test_output.append(s);
            _proxy_test_output.setCaretPosition(_proxy_test_output.getDocument().getLength());
        });
    }
}
