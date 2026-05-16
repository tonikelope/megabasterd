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
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
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
    private JSpinner _batch_size_spinner;
    private JButton _save_working_button;

    private JButton _save_button;
    private JButton _cancel_button;

    // Held so we can shut the probe pool down cleanly when the dialog is
    // disposed mid-test; without this the JVM would wait for every 3s TCP
    // connect to complete before exiting. (#753)
    private volatile ExecutorService _probe_executor;

    // Addresses that passed the most recent test, in submission order.
    // Drives the "Save working proxies to custom list" action: any
    // working entry here is round-tripped back into custom_proxy_list
    // with its SOCKS marker / auth trailer preserved (looked up from
    // the live manager state at save time). (#753)
    private final List<String> _last_working_addrs = new ArrayList<>();

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
        setTitle(I18n.tr("ui.quota.title"));

        JPanel content = new JPanel(new BorderLayout(10, 10));
        content.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // --- Top: numeric / boolean settings (GridBag for tidy alignment) ---
        JPanel settings_panel = new JPanel(new GridBagLayout());
        settings_panel.setBorder(BorderFactory.createTitledBorder(I18n.tr("ui.quota.recovery_panel_title")));

        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(4, 6, 4, 6);
        g.anchor = GridBagConstraints.WEST;
        g.fill = GridBagConstraints.HORIZONTAL;

        _auto_resume_ip_checkbox = new JCheckBox(I18n.tr("ui.quota.auto_resume_ip"));
        _auto_resume_ip_checkbox.setToolTipText(I18n.tr("ui.quota.auto_resume_ip.tooltip"));
        g.gridx = 0;
        g.gridy = 0;
        g.gridwidth = 2;
        settings_panel.add(_auto_resume_ip_checkbox, g);
        g.gridwidth = 1;

        JLabel l1 = new JLabel(I18n.tr("ui.quota.stall_timeout_label"));
        l1.setToolTipText(I18n.tr("ui.quota.stall_timeout.tooltip"));
        g.gridx = 0;
        g.gridy = 1;
        settings_panel.add(l1, g);
        _quota_stall_spinner = new JSpinner(new SpinnerNumberModel(Transference.QUOTA_STALL_TIMEOUT_DEFAULT, 30, 3600, 30));
        ((JSpinner.DefaultEditor) _quota_stall_spinner.getEditor()).getTextField().setEditable(true);
        g.gridx = 1;
        g.gridy = 1;
        settings_panel.add(_quota_stall_spinner, g);

        JLabel l2 = new JLabel(I18n.tr("ui.quota.recheck_window_label"));
        l2.setToolTipText(I18n.tr("ui.quota.recheck_window.tooltip"));
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
        diag_panel.setBorder(BorderFactory.createTitledBorder(I18n.tr("ui.quota.diagnostics_panel_title")));

        JPanel ip_row = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        ip_row.add(new JLabel(I18n.tr("ui.quota.current_ip_label")));
        _current_ip_label = new JLabel(I18n.tr("ui.quota.ip_checking"));
        _current_ip_label.setFont(new Font(Font.MONOSPACED, Font.BOLD, 14));
        ip_row.add(_current_ip_label);
        _refresh_ip_button = new JButton(I18n.tr("ui.quota.refresh_ip_button"));
        _refresh_ip_button.addActionListener(e -> refreshPublicIp());
        ip_row.add(_refresh_ip_button);
        diag_panel.add(ip_row, BorderLayout.NORTH);

        JPanel test_row = new JPanel(new BorderLayout(0, 4));
        JPanel test_btn_panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        _test_proxy_button = new JButton(I18n.tr("ui.quota.test_proxy_button"));
        _test_proxy_button.setToolTipText(I18n.tr("ui.quota.test_proxy.tooltip"));
        _test_proxy_button.addActionListener(e -> testProxyList());
        test_btn_panel.add(_test_proxy_button);

        JLabel batch_label = new JLabel(I18n.tr("ui.quota.batch_size_label"));
        batch_label.setToolTipText(I18n.tr("ui.quota.batch_size.tooltip"));
        test_btn_panel.add(batch_label);
        // Default to 20 concurrent TCP probes. Most home networks
        // handle this easily; aggressive users with fat lists can crank
        // it up to 100. Below 1 makes no sense; above 100 starts to
        // hit the JVM's default file-descriptor budget on Linux.
        _batch_size_spinner = new JSpinner(new SpinnerNumberModel(20, 1, 100, 1));
        _batch_size_spinner.setToolTipText(I18n.tr("ui.quota.batch_size.tooltip"));
        ((JSpinner.DefaultEditor) _batch_size_spinner.getEditor()).getTextField().setEditable(true);
        ((JSpinner.DefaultEditor) _batch_size_spinner.getEditor()).getTextField().setColumns(3);
        test_btn_panel.add(_batch_size_spinner);

        _save_working_button = new JButton(I18n.tr("ui.quota.save_working_button"));
        _save_working_button.setToolTipText(I18n.tr("ui.quota.save_working.tooltip"));
        _save_working_button.setEnabled(false);
        _save_working_button.addActionListener(e -> saveWorkingProxies());
        test_btn_panel.add(_save_working_button);

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
        _save_button = new JButton(I18n.tr("ui.quota.save_button"));
        _save_button.setBackground(new Color(60, 140, 60));
        _save_button.setForeground(Color.WHITE);
        _save_button.addActionListener(e -> {
            saveToDB();
            dispose();
        });

        _cancel_button = new JButton(I18n.tr("ui.quota.cancel_button"));
        _cancel_button.addActionListener(e -> dispose());

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        footer.add(_cancel_button);
        footer.add(_save_button);

        JLabel help = new JLabel(I18n.tr("ui.quota.help"));
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
            _current_ip_label.setText(I18n.tr("ui.quota.ip_checking"));
            _refresh_ip_button.setEnabled(false);
        });

        // Force a fresh fetch (bypass the 30 s cache) so the user can verify
        // their VPN took effect.
        MainPanel.invalidatePublicIpCache();
        String ip = MainPanel.getCachedPublicIp();

        MiscTools.GUIRun(() -> {
            _current_ip_label.setText(ip != null ? ip : I18n.tr("ui.quota.ip_unavailable"));
            _refresh_ip_button.setEnabled(true);
        });
    }

    /**
     * Result of probing one proxy address. {@code ok} reflects the TCP
     * connect success; {@code dt_ms} is wall time for the probe (useful
     * for spotting slow-but-reachable proxies). On failure {@code fail_class}
     * carries the simple class name of the thrown exception.
     */
    private static final class ProbeResult {
        final boolean ok;
        final long dt_ms;
        final String fail_class;
        ProbeResult(boolean ok, long dt_ms, String fail_class) {
            this.ok = ok;
            this.dt_ms = dt_ms;
            this.fail_class = fail_class;
        }
    }

    /**
     * Parses the configured SmartProxy list (custom or URL-fetched) and
     * attempts a 3 s TCP connect to each entry. The probes run on a
     * fixed-size thread pool whose size is taken from the batch-size
     * spinner (1..100, default 20), so a 300-entry list finishes in
     * ~45 s instead of the ~15 min the sequential version took. Output
     * is printed in submission order to keep the report readable.
     * Runs on a worker thread; the test button is disabled for the
     * duration. (#753)
     */
    private void testProxyList() {
        _test_proxy_button.setEnabled(false);
        _batch_size_spinner.setEnabled(false);
        _save_working_button.setEnabled(false);
        _last_working_addrs.clear();
        _proxy_test_output.setText(I18n.tr("ui.quota.test.loading") + "\n");

        final int batch_size = (Integer) _batch_size_spinner.getValue();

        new Thread(() -> {
            ExecutorService exec = null;
            try {
                // Grab whatever's currently in the live proxy manager. That
                // accounts for both the custom-list and URL-fetched paths
                // without us re-implementing the parser. If the manager
                // hasn't refreshed yet, force one.
                SmartMegaProxyManager pm = MainPanel.getProxy_manager();
                if (pm == null) {
                    appendOutput(I18n.tr("ui.quota.test.smartproxy_not_initialised") + "\n");
                    return;
                }

                // Snapshot the whole pool through a dedicated accessor.
                // The previous approach drained getProxy() with an
                // ever-growing exclusion list, which (a) hid currently-banned
                // proxies from the user (getProxy filters them out, so a
                // recently-failed proxy was invisible to the test) and
                // (b) needed an arbitrary safety_cap that silently truncated
                // pools larger than the cap. (#753 audit)
                List<String[]> snapshot;
                try {
                    snapshot = pm.getProxySnapshot();
                } catch (Exception ex) {
                    appendOutput(I18n.tr("ui.quota.test.getproxy_threw", ex.getMessage()) + "\n");
                    return;
                }

                if (snapshot.isEmpty()) {
                    appendOutput(I18n.tr("ui.quota.test.no_proxies") + "\n");
                    return;
                }

                LinkedHashMap<String, Boolean> results = new LinkedHashMap<>();
                List<String> addrs = new ArrayList<>(snapshot.size());
                for (String[] entry : snapshot) {
                    if (entry == null || entry[0] == null || results.containsKey(entry[0])) {
                        continue;
                    }
                    results.put(entry[0], null);
                    addrs.add(entry[0]);
                }
                appendOutput(I18n.tr("ui.quota.test.testing_count", addrs.size(), batch_size) + "\n");
                appendOutput("--------------------------------------------------------------\n");

                exec = Executors.newFixedThreadPool(batch_size, r -> {
                    Thread t = new Thread(r, "QuotaRecoverySettings-Probe");
                    t.setDaemon(true);
                    return t;
                });
                _probe_executor = exec;

                // Pre-classify malformed entries on the spawning thread so
                // we don't waste a probe worker on them, and keep their
                // skip lines in stable order with the rest of the report.
                // skip_reason[i] != null => never enqueued, print skip line
                // at slot i; futures[i] holds the probe Future otherwise.
                List<Future<ProbeResult>> futures = new ArrayList<>(addrs.size());
                List<String> skip_reason = new ArrayList<>(addrs.size());

                for (String addr : addrs) {
                    String[] parts = addr.split(":");
                    String why = null;
                    int port = -1;
                    if (parts.length != 2) {
                        why = "malformed";
                    } else {
                        try {
                            port = Integer.parseInt(parts[1]);
                            if (port < 1 || port > 65535) {
                                why = "bad_port:" + parts[1];
                            }
                        } catch (NumberFormatException ex) {
                            why = "bad_port:" + parts[1];
                        }
                    }
                    if (why != null) {
                        futures.add(null);
                        skip_reason.add(why);
                    } else {
                        final String host = parts[0];
                        final int p = port;
                        futures.add(exec.submit(() -> probe(host, p)));
                        skip_reason.add(null);
                    }
                }

                int ok = 0, fail = 0;
                for (int i = 0; i < futures.size(); i++) {
                    String addr = addrs.get(i);
                    String padded_addr = String.format("%-32s", addr);
                    Future<ProbeResult> f = futures.get(i);

                    if (f == null) {
                        String why = skip_reason.get(i);
                        if (why.equals("malformed")) {
                            appendOutput(I18n.tr("ui.quota.test.row_skip_malformed", padded_addr) + "\n");
                        } else {
                            // why = "bad_port:NNN"
                            appendOutput(I18n.tr("ui.quota.test.row_skip_bad_port", padded_addr, why.substring("bad_port:".length())) + "\n");
                        }
                        fail++;
                        results.put(addr, false);
                        continue;
                    }

                    ProbeResult r;
                    try {
                        r = f.get();
                    } catch (java.util.concurrent.CancellationException ex) {
                        appendOutput(I18n.tr("ui.quota.test.cancelled") + "\n");
                        return;
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        appendOutput(I18n.tr("ui.quota.test.cancelled") + "\n");
                        return;
                    } catch (java.util.concurrent.ExecutionException ex) {
                        // The probe lambda catches its own exceptions and
                        // returns a failed ProbeResult, so reaching this
                        // branch means something genuinely odd happened.
                        String cause = ex.getCause() == null ? ex.getClass().getSimpleName() : ex.getCause().getClass().getSimpleName();
                        appendOutput(I18n.tr("ui.quota.test.row_fail", padded_addr, cause, 0L) + "\n");
                        fail++;
                        results.put(addr, false);
                        continue;
                    }
                    if (r.ok) {
                        appendOutput(I18n.tr("ui.quota.test.row_ok", padded_addr, r.dt_ms) + "\n");
                        ok++;
                        results.put(addr, true);
                        _last_working_addrs.add(addr);
                    } else {
                        appendOutput(I18n.tr("ui.quota.test.row_fail", padded_addr, r.fail_class, r.dt_ms) + "\n");
                        fail++;
                        results.put(addr, false);
                    }
                }
                appendOutput("--------------------------------------------------------------\n");
                appendOutput(I18n.tr("ui.quota.test.summary", ok, fail) + "\n");
            } finally {
                if (exec != null) {
                    exec.shutdownNow();
                    try {
                        exec.awaitTermination(2, TimeUnit.SECONDS);
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }
                }
                _probe_executor = null;
                MiscTools.GUIRun(() -> {
                    _test_proxy_button.setEnabled(true);
                    _batch_size_spinner.setEnabled(true);
                    _save_working_button.setEnabled(!_last_working_addrs.isEmpty());
                });
            }
        }, "QuotaRecoverySettings-ProxyTest").start();
    }

    /**
     * Writes the proxies that passed the most recent test back into
     * the {@code custom_proxy_list} DB setting, replacing whatever inline
     * IP:PORT entries were there. Lines starting with {@code #} (remote
     * URL sources) and blank lines are preserved verbatim. Auth trailers
     * and SOCKS markers come from live manager state, so a round-trip
     * through the parser produces an equivalent entry. (#753)
     */
    private void saveWorkingProxies() {
        if (_last_working_addrs.isEmpty()) {
            return;
        }
        SmartMegaProxyManager pm = MainPanel.getProxy_manager();
        if (pm == null) {
            appendOutput(I18n.tr("ui.quota.test.smartproxy_not_initialised") + "\n");
            return;
        }

        int ans = JOptionPane.showConfirmDialog(this,
                I18n.tr("ui.quota.save_working.confirm", _last_working_addrs.size()),
                I18n.tr("ui.quota.save_working.title"),
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE);
        if (ans != JOptionPane.OK_OPTION) {
            return;
        }

        _save_working_button.setEnabled(false);
        final List<String> snapshot = new ArrayList<>(_last_working_addrs);

        new Thread(() -> {
            try {
                int written = pm.saveWorkingProxiesToCustomList(snapshot);
                appendOutput(I18n.tr("ui.quota.save_working.done", written) + "\n");
            } catch (SQLException ex) {
                LOG.log(Level.WARNING, "Save working proxies failed: {0}", ex.getMessage());
                appendOutput(I18n.tr("ui.quota.save_working.error", ex.getMessage()) + "\n");
            } finally {
                MiscTools.GUIRun(() -> _save_working_button.setEnabled(!_last_working_addrs.isEmpty()));
            }
        }, "QuotaRecoverySettings-SaveWorking").start();
    }

    private static ProbeResult probe(String host, int port) {
        long t0 = System.currentTimeMillis();
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress(host, port), 3000);
            return new ProbeResult(true, System.currentTimeMillis() - t0, null);
        } catch (Exception ex) {
            return new ProbeResult(false, System.currentTimeMillis() - t0, ex.getClass().getSimpleName());
        }
    }

    private void appendOutput(String s) {
        MiscTools.GUIRun(() -> {
            _proxy_test_output.append(s);
            _proxy_test_output.setCaretPosition(_proxy_test_output.getDocument().getLength());
        });
    }

    @Override
    public void dispose() {
        // If the user closes the dialog while a test is running, kill the
        // probe pool so the JVM doesn't wait up to 3 s per outstanding
        // connect attempt. (#753)
        ExecutorService exec = _probe_executor;
        if (exec != null) {
            exec.shutdownNow();
        }
        super.dispose();
    }
}
