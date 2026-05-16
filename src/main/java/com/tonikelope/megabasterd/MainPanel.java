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

import static com.tonikelope.megabasterd.DBTools.*;
import static com.tonikelope.megabasterd.MiscTools.*;
import com.tonikelope.megabasterd.SmartMegaProxyManager.SmartProxyAuthenticator;
import static com.tonikelope.megabasterd.Transference.*;
import java.awt.AWTException;
import java.awt.Color;
import static java.awt.EventQueue.invokeLater;
import java.awt.Font;
import static java.awt.Frame.NORMAL;
import static java.awt.SystemTray.getSystemTray;
import static java.awt.Toolkit.getDefaultToolkit;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import static java.awt.event.WindowEvent.WINDOW_CLOSING;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import static java.lang.Integer.parseInt;
import static java.lang.System.exit;
import java.net.Authenticator;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import static java.util.concurrent.Executors.newCachedThreadPool;
import java.util.logging.Level;
import static java.util.logging.Level.SEVERE;
import java.util.logging.Logger;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import static javax.swing.JOptionPane.QUESTION_MESSAGE;
import static javax.swing.JOptionPane.WARNING_MESSAGE;
import static javax.swing.JOptionPane.YES_NO_CANCEL_OPTION;
import static javax.swing.JOptionPane.showOptionDialog;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 *
 * @author tonikelope
 */
public final class MainPanel {

    public static final String VERSION = "8.34";
    public static final boolean FORCE_SMART_PROXY = false; //TRUE FOR DEBUGING SMART PROXY
    public static final int THROTTLE_SLICE_SIZE = 16 * 1024;
    public static final int DEFAULT_BYTE_BUFFER_SIZE = 16 * 1024;
    public static final int STREAMER_PORT = 1337;
    public static final int WATCHDOG_PORT = 1338;
    public static final int DEFAULT_MEGA_PROXY_PORT = 9999;
    public static final int RUN_COMMAND_TIME = 120;
    public static final String DEFAULT_LANGUAGE = "EN";
    public static final boolean DEFAULT_SMART_PROXY = false;
    public static final double FORCE_GARBAGE_COLLECTION_MAX_MEMORY_PERCENT = 0.7;
    public static Font GUI_FONT = new JLabel().getFont();
    public static final float ZOOM_FACTOR = 0.8f;
    public static final String DEFAULT_USER_AGENT = "Mozilla/5.0 (X11; Linux x86_64; rv:61.0) Gecko/20100101 Firefox/61.0";
    public static final String ICON_FILE = "/images/pica_roja_big.png";
    public static final ExecutorService THREAD_POOL = newCachedThreadPool(_megabasterdDaemonThreadFactory());

    private static java.util.concurrent.ThreadFactory _megabasterdDaemonThreadFactory() {
        return new java.util.concurrent.ThreadFactory() {
            private final java.util.concurrent.atomic.AtomicLong counter = new java.util.concurrent.atomic.AtomicLong(1);

            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "MegaBasterd-Worker-" + counter.getAndIncrement());
                // Daemon so the JVM can exit cleanly via System.exit even if a
                // pool worker is still parked in secureWait. Non-daemon used to
                // keep the JVM alive on shutdown when one of the long-running
                // background loops (smart proxy refresh, memory monitor,
                // watchdog accept) missed the _exit flag.
                t.setDaemon(true);
                return t;
            }
        };
    }
    public static volatile String MEGABASTERD_HOME_DIR = System.getProperty("user.home");
    private static String _proxy_host;
    private static int _proxy_port;
    private static boolean _use_proxy;
    private static String _proxy_user;
    private static String _proxy_pass;
    private static boolean _use_smart_proxy;
    private static boolean _run_command;
    private static String _run_command_path;
    private static String _font;
    private static SmartMegaProxyManager _proxy_manager;
    private static volatile String _language;
    private static String _new_version;
    private static Boolean _resume_uploads;
    private static Boolean _resume_downloads;
    public static volatile long LAST_EXTERNAL_COMMAND_TIMESTAMP;

    /**
     * Shared cached public IP for the 509-recovery / VPN-aware retry path.
     * Workers call getCachedPublicIp() during 509 backoff to notice that the
     * user changed their public IP (e.g. activated a VPN). Without a cache, N
     * parallel workers all hitting the public-IP services every backoff slice
     * would (a) burn budget on those services and (b) potentially desync
     * because each worker would get a fresh fetch with its own timing. One
     * cache, one fetcher at a time, TTL 30 s. (#751)
     */
    private static volatile String _cached_public_ip = null;
    private static volatile long _cached_public_ip_ts = 0L;
    private static final Object _public_ip_lock = new Object();
    private static final long PUBLIC_IP_CACHE_TTL_MS = 30_000L;

    /**
     * Returns the most recent public IPv4 we've seen, refreshing via
     * MiscTools.getMyPublicIP() (which rotates HTTPS sources) at most once per
     * {@link #PUBLIC_IP_CACHE_TTL_MS}. Returns null if no fetch has ever
     * succeeded; otherwise returns the previously-cached value when the current
     * fetch attempt fails (treating it as "no IP change detected" rather than
     * poisoning callers with null). Safe to call concurrently from any worker
     * thread. (#751)
     */
    public static String getCachedPublicIp() {
        long now = System.currentTimeMillis();
        if (_cached_public_ip != null && now - _cached_public_ip_ts < PUBLIC_IP_CACHE_TTL_MS) {
            return _cached_public_ip;
        }
        synchronized (_public_ip_lock) {
            // Recheck after lock: another thread may have refreshed while
            // we were waiting.
            now = System.currentTimeMillis();
            if (_cached_public_ip != null && now - _cached_public_ip_ts < PUBLIC_IP_CACHE_TTL_MS) {
                return _cached_public_ip;
            }
            String fresh = MiscTools.getMyPublicIP();
            if (fresh != null) {
                _cached_public_ip = fresh;
            }
            // Always advance the timestamp -- otherwise consecutive failed
            // fetches would hammer the IP services on every backoff slice.
            _cached_public_ip_ts = System.currentTimeMillis();
            return _cached_public_ip;
        }
    }

    /**
     * Force-invalidate the public-IP cache so the next getCachedPublicIp() call
     * will trigger a fresh fetch. Used by the 509 path after running the user's
     * external command (which may have switched VPN), so we don't keep
     * returning the pre-VPN IP and miss the change. (#751)
     */
    public static void invalidatePublicIpCache() {
        _cached_public_ip_ts = 0L;
    }

    private static final Logger LOG = Logger.getLogger(MainPanel.class.getName());
    private static volatile boolean CHECK_RUNNING = true;

    public static void main(String args[]) {

        if (args.length > 0) {

            if (args.length > 1) {
                try {
                    Logger.getLogger(MainPanel.class.getName()).log(Level.INFO, "{0} Waiting {1} seconds before start...", new Object[]{Thread.currentThread().getName(), args[1]});

                    if (Long.parseLong(args[1]) >= 0) {
                        Thread.sleep(Long.parseLong(args[1]) * 1000);
                    } else {
                        CHECK_RUNNING = false;
                    }
                } catch (InterruptedException ex) {
                    Logger.getLogger(MainPanel.class.getName()).log(Level.SEVERE, ex.getMessage());
                }
            }

        }

        File f = new File(getCurrentJarParentPath() + "/.megabasterd_portable");

        if (f.exists()) {
            MEGABASTERD_HOME_DIR = f.getParentFile().getAbsolutePath();
        }

        try {

            setupSqliteTables();

        } catch (SQLException ex) {
            Logger.getLogger(MainPanel.class.getName()).log(SEVERE, null, ex);
        }

        setNimbusLookAndFeel("yes".equals(DBTools.selectSettingValue("dark_mode")));

        if ("yes".equals(DBTools.selectSettingValue("upload_log"))) {
            MiscTools.createUploadLogDir();
        }

        // 8.27 migration: users who upgraded from pre-afb3936 builds (when
        // VERIFY_CBC_MAC_DEFAULT was false) have "verify_down_file" = "no"
        // persisted in their settings DB from any time they opened Settings
        // and clicked Apply. Without this fix-up the 8.25 default-on never
        // applied to them and corrupted downloads slipped through silently.
        // Run once per install and remember it with a sentinel key.
        try {
            if (DBTools.selectSettingValue("verify_down_file_migrated_v827") == null) {
                if ("no".equals(DBTools.selectSettingValue("verify_down_file"))) {
                    DBTools.insertSettingValue("verify_down_file", "yes");
                }
                DBTools.insertSettingValue("verify_down_file_migrated_v827", "yes");
            }
        } catch (SQLException ex) {
            Logger.getLogger(MainPanel.class.getName()).log(SEVERE, "verify_down_file migration failed", ex);
        }

        MiscTools.purgeOrphanThumbnails();

        final MainPanel main_panel = new MainPanel();

        invokeLater(() -> {
            main_panel.getView().setVisible(true);
        });
    }

    public static boolean isRun_command() {
        return _run_command;
    }

    public static String getRun_command_path() {
        return _run_command_path;
    }

    public static String getFont() {
        return _font;
    }

    public static String getNew_version() {
        return _new_version;
    }

    public static String getLanguage() {
        return _language;
    }

    public static String getProxy_user() {
        return _proxy_user;
    }

    public static String getProxy_pass() {
        return _proxy_pass;
    }

    public static void setProxy_manager(SmartMegaProxyManager proxy_manager) {
        _proxy_manager = proxy_manager;
    }

    public static String getProxy_host() {
        return _proxy_host;
    }

    public static int getProxy_port() {
        return _proxy_port;
    }

    public static boolean isUse_proxy() {
        return _use_proxy;
    }

    public static boolean isUse_smart_proxy() {
        return _use_smart_proxy;
    }

    public static SmartMegaProxyManager getProxy_manager() {
        return _proxy_manager;
    }

    private volatile MainPanelView _view;
    private final SpeedMeter _global_dl_speed, _global_up_speed;
    private final DownloadManager _download_manager;
    private final UploadManager _upload_manager;
    private final StreamThrottlerSupervisor _stream_supervisor;
    private int _max_dl, _max_ul, _default_slots_down, _default_slots_up, _max_dl_speed, _max_up_speed;
    private boolean _use_slots_down, _limit_download_speed, _limit_upload_speed, _use_mega_account_down, _init_paused, _debug_file;
    private String _mega_account_down;
    private String _default_download_path;
    private boolean _use_custom_chunks_dir;
    private String _custom_chunks_dir;
    private HashMap<String, Object> _mega_accounts;
    private HashMap<String, Object> _elc_accounts;
    private final HashMap<String, MegaAPI> _mega_active_accounts;
    private TrayIcon _trayicon;
    private final ClipboardSpy _clipboardspy;
    private KissVideoStreamServer _streamserver;
    private byte[] _master_pass;
    private String _master_pass_hash;
    private String _master_pass_salt;
    private boolean _restart;
    private MegaProxyServer _mega_proxy_server;
    private int _megacrypter_reverse_port;
    private boolean _megacrypter_reverse;
    private float _zoom_factor;
    private volatile boolean _exit;

    public MainPanel() {

        _new_version = null;

        _exit = false;

        LAST_EXTERNAL_COMMAND_TIMESTAMP = -1;

        _restart = false;

        _elc_accounts = new HashMap<>();

        _master_pass = null;

        _mega_active_accounts = new HashMap<>();

        _proxy_host = null;

        _proxy_port = 3128;

        _proxy_user = null;

        _proxy_pass = null;

        _use_proxy = false;

        _use_smart_proxy = false;

        _proxy_manager = null;

        _resume_uploads = false;

        _resume_downloads = false;

        // Capture java.util.logging records into an in-memory queue so the
        // "DEBUG LOG" tab built below (after the view is up) can show them.
        // Done BEFORE loadUserSettings so early init records are not lost.
        // We do NOT touch System.out / System.err and we do NOT raise the
        // root level -- the existing filter still applies. (#751 / D)
        DebugLogBus.installJULHandler();

        loadUserSettings();

        if (_debug_file) {

            try {
                String debug_path = MainPanel.MEGABASTERD_HOME_DIR + "/MEGABASTERD_DEBUG.log";

                // 1) Redirect System.out / System.err so plain println / e.printStackTrace
                //    output also lands in the debug file.
                PrintStream fileOut = new PrintStream(new FileOutputStream(debug_path, true), true, "UTF-8");
                System.setOut(fileOut);
                System.setErr(fileOut);

                // 2) Install a java.util.logging FileHandler on the root logger.
                //    Without this, JUL keeps writing to the ConsoleHandler that
                //    cached System.err at JUL-init time -- which happened well
                //    before the System.setErr() above. Result: pre-this-fix,
                //    "Debug file" in Settings appeared to do nothing because
                //    every LOG.log(...) call went to the original stderr (i.e.,
                //    discarded on javaw / Windows).
                java.util.logging.FileHandler fh = new java.util.logging.FileHandler(debug_path, true);
                fh.setEncoding("UTF-8");
                fh.setLevel(Level.ALL);
                fh.setFormatter(new java.util.logging.SimpleFormatter());

                java.util.logging.Logger root = java.util.logging.Logger.getLogger("");
                root.setLevel(Level.ALL);
                root.addHandler(fh);

                // Lower the ConsoleHandler level so important stuff still shows
                // on stdout for users running from a terminal.
                for (java.util.logging.Handler h : root.getHandlers()) {
                    if (h instanceof java.util.logging.ConsoleHandler) {
                        h.setLevel(Level.INFO);
                    }
                }

                Logger.getLogger(MainPanel.class.getName()).log(Level.INFO, "Debug log started -> {0}", debug_path);

            } catch (IOException ex) {
                Logger.getLogger(MainPanel.class.getName()).log(Level.SEVERE, "Failed to install debug log handler", ex);
            }
        }

        System.out.println(System.getProperty("os.name") + "" + System.getProperty("java.vm.name") + " " + System.getProperty("java.version") + " " + System.getProperty("java.home"));

        UIManager.put("OptionPane.messageFont", GUI_FONT.deriveFont(15f * getZoom_factor()));

        UIManager.put("OptionPane.buttonFont", GUI_FONT.deriveFont(15f * getZoom_factor()));

        UIManager.put("OptionPane.cancelButtonText", LabelTranslatorSingleton.getInstance().translate("Cancel"));

        UIManager.put("OptionPane.yesButtonText", LabelTranslatorSingleton.getInstance().translate("Yes"));

        UIManager.put("OptionPane.okButtonText", LabelTranslatorSingleton.getInstance().translate("OK"));

        _view = new MainPanelView(this);

        // Wire up the quota-recovery settings dialog into the Edit menu.
        // Done programmatically (not via NetBeans form) so we don't have
        // to edit MainPanelView.form to surface three new settings. (#751 / C1)
        //
        // To match the visual size of the sibling menu items, we have to
        // both (a) seed the baseline font to the same Dialog/0/18 that
        // MainPanelView's generated initComponents sets on every other
        // *_menu item, AND (b) call MiscTools.updateFonts() on the new
        // item so it gets GUI_FONT scaled by zoom_factor exactly like the
        // siblings did during MainPanelView construction. Skipping (a)
        // made updateFonts derive from the Swing default JMenuItem font
        // (~12 pt), leaving the new item visibly smaller than the rest.
        try {
            javax.swing.JMenuItem quota_menu = new javax.swing.JMenuItem("Quota recovery & SmartProxy (509)");
            quota_menu.setFont(new java.awt.Font("Dialog", java.awt.Font.PLAIN, 18));
            quota_menu.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/icons8-services-30.png")));
            quota_menu.addActionListener((evt) -> {
                QuotaRecoverySettingsDialog d = new QuotaRecoverySettingsDialog(_view, true, this);
                d.setLocationRelativeTo(_view);
                d.setVisible(true);
            });
            _view.getEdit_menu().add(quota_menu);
            MiscTools.updateFonts(quota_menu, GUI_FONT, getZoom_factor());
        } catch (Exception ex) {
            Logger.getLogger(MainPanel.class.getName()).log(Level.WARNING,
                    "Could not wire quota-recovery menu: {0}", ex.getMessage());
        }

        // "DEBUG LOG" tab. Everything inside GUIRunAndWait so it runs on the
        // EDT; addTab on a JTabbedPane after the view is realised has to be
        // EDT-safe. DebugLogBus.installJULHandler() ran above, so the queue
        // has been buffering since startup -- bind() will start a Timer that
        // drains the queue into the textarea every 300 ms, batched. (#751 / D)
        MiscTools.GUIRunAndWait(() -> {
            try {
                javax.swing.JTextArea debug_area = new javax.swing.JTextArea();
                debug_area.setEditable(false);
                debug_area.setLineWrap(false);
                debug_area.setBackground(new java.awt.Color(30, 30, 30));
                debug_area.setForeground(new java.awt.Color(220, 220, 220));
                debug_area.setCaretColor(new java.awt.Color(220, 220, 220));
                debug_area.setSelectionColor(new java.awt.Color(70, 90, 130));
                debug_area.setFont(new java.awt.Font(java.awt.Font.MONOSPACED, java.awt.Font.PLAIN, 12));

                javax.swing.JScrollPane debug_scroll = new javax.swing.JScrollPane(debug_area);
                debug_scroll.getViewport().setBackground(new java.awt.Color(30, 30, 30));

                javax.swing.JButton clear_btn = new javax.swing.JButton("Clear");
                clear_btn.setToolTipText("Clear the DEBUG LOG tab.");
                clear_btn.addActionListener((evt) -> {
                    int ans = javax.swing.JOptionPane.showConfirmDialog(_view,
                            "Clear the DEBUG LOG buffer? Existing entries will be lost from the tab.",
                            "Clear debug log", javax.swing.JOptionPane.YES_NO_OPTION,
                            javax.swing.JOptionPane.WARNING_MESSAGE);
                    if (ans == javax.swing.JOptionPane.YES_OPTION) {
                        debug_area.setText("");
                    }
                });

                javax.swing.JButton copy_btn = new javax.swing.JButton("Copy all");
                copy_btn.setToolTipText("Copy the entire DEBUG LOG buffer to the clipboard.");
                copy_btn.addActionListener((evt) -> {
                    try {
                        java.awt.Toolkit.getDefaultToolkit().getSystemClipboard()
                                .setContents(new java.awt.datatransfer.StringSelection(debug_area.getText()), null);
                    } catch (Exception ignore) {
                    }
                });

                javax.swing.JButton save_btn = new javax.swing.JButton("Save to file...");
                save_btn.setToolTipText("Write the current DEBUG LOG buffer to a file on disk.");
                save_btn.addActionListener((evt) -> {
                    javax.swing.JFileChooser chooser = new javax.swing.JFileChooser();
                    chooser.setDialogTitle("Save DEBUG LOG to file");
                    String stamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date());
                    chooser.setSelectedFile(new java.io.File("megabasterd_debug_" + stamp + ".log"));
                    if (chooser.showSaveDialog(_view) == javax.swing.JFileChooser.APPROVE_OPTION) {
                        java.io.File target = chooser.getSelectedFile();
                        try (java.io.OutputStreamWriter w = new java.io.OutputStreamWriter(
                                new java.io.FileOutputStream(target), java.nio.charset.StandardCharsets.UTF_8)) {
                            w.write(debug_area.getText());
                        } catch (Exception ex) {
                            javax.swing.JOptionPane.showMessageDialog(_view,
                                    "Could not save log: " + ex.getMessage(),
                                    "Save failed", javax.swing.JOptionPane.ERROR_MESSAGE);
                        }
                    }
                });

                javax.swing.JPanel toolbar = new javax.swing.JPanel(
                        new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 6, 4));
                toolbar.add(save_btn);
                toolbar.add(copy_btn);
                toolbar.add(clear_btn);

                javax.swing.JPanel debug_panel = new javax.swing.JPanel(new java.awt.BorderLayout());
                debug_panel.add(toolbar, java.awt.BorderLayout.NORTH);
                debug_panel.add(debug_scroll, java.awt.BorderLayout.CENTER);

                _view.getjTabbedPane1().addTab("DEBUG LOG",
                        new javax.swing.ImageIcon(getClass().getResource("/images/icons8-services-30.png")),
                        debug_panel);

                // Deliberately NOT calling MiscTools.updateFonts on the
                // textarea: the recursive font derive would replace the
                // monospaced 12pt with a proportional family scaled by
                // zoom_factor, which makes stack traces unreadable. The
                // toolbar buttons use the platform default which is good
                // enough.

                DebugLogBus.bind(debug_area);
            } catch (Exception ex) {
                Logger.getLogger(MainPanel.class.getName()).log(Level.WARNING,
                        "Could not wire DEBUG LOG tab: {0}", ex.getMessage());
            }
        });

        if (CHECK_RUNNING && checkAppIsRunning()) {

            System.exit(0);
        }

        try {
            trayIcon();
        } catch (AWTException ex) {
            Logger.getLogger(MainPanel.class.getName()).log(Level.SEVERE, ex.getMessage());
        }

        THREAD_POOL.execute((_download_manager = new DownloadManager(this)));

        THREAD_POOL.execute((_upload_manager = new UploadManager(this)));

        THREAD_POOL.execute((_global_dl_speed = new SpeedMeter(_download_manager, getView().getGlobal_speed_down_label(), getView().getDown_remtime_label())));

        THREAD_POOL.execute((_global_up_speed = new SpeedMeter(_upload_manager, getView().getGlobal_speed_up_label(), getView().getUp_remtime_label())));

        THREAD_POOL.execute((_stream_supervisor = new StreamThrottlerSupervisor(_limit_download_speed ? _max_dl_speed * 1024 : 0, _limit_upload_speed ? _max_up_speed * 1024 : 0, THROTTLE_SLICE_SIZE)));

        THREAD_POOL.execute((_clipboardspy = new ClipboardSpy()));

        try {
            _streamserver = new KissVideoStreamServer(this);
            _streamserver.start(STREAMER_PORT, "/video");
        } catch (IOException ex) {
            Logger.getLogger(MainPanel.class.getName()).log(SEVERE, null, ex);
        }

        _check_old_version();

        THREAD_POOL.execute(() -> {
            _new_version = checkNewVersion(AboutDialog.MEGABASTERD_URL);

            if (_new_version != null) {

                JOptionPane.showMessageDialog(getView(), LabelTranslatorSingleton.getInstance().translate("MegaBasterd NEW VERSION is available! -> ") + _new_version);
            }
        });

        if (_megacrypter_reverse) {

            _mega_proxy_server = new MegaProxyServer(this, UUID.randomUUID().toString(), _megacrypter_reverse_port);

            THREAD_POOL.execute(_mega_proxy_server);

        } else {
            _mega_proxy_server = null;

        }

        if (_use_smart_proxy) {

            MainPanel tthis = this;

            THREAD_POOL.execute(() -> {
                Authenticator.setDefault(new SmartProxyAuthenticator());

                String lista_proxy = DBTools.selectSettingValue("custom_proxy_list");

                String url_list = MiscTools.findFirstRegex("^#(http.+)$", lista_proxy.trim(), 1);

                _proxy_manager = new SmartMegaProxyManager(url_list, tthis);
            });

        } else {

            getView().updateSmartProxyStatus("SmartProxy: OFF");

        }

        MiscTools.GUIRun(() -> {
            getView().getGlobal_speed_down_label().setForeground(_limit_download_speed ? new Color(255, 0, 0) : new Color(0, 128, 255));

            getView().getGlobal_speed_up_label().setForeground(_limit_upload_speed ? new Color(255, 0, 0) : new Color(0, 128, 255));
        });

        THREAD_POOL.execute(() -> {
            Runtime instance = Runtime.getRuntime();
            String last_text = null;
            while (!_exit) {
                long used_memory = instance.totalMemory() - instance.freeMemory();
                long max_memory = instance.maxMemory();
                String text = "JVM-RAM used: " + MiscTools.formatBytes(used_memory) + " / " + MiscTools.formatBytes(max_memory);
                // Skip setText if the rendered string is unchanged -- the EDT
                // doesn't need a repaint event for "same value".
                if (!text.equals(last_text)) {
                    last_text = text;
                    final String t = text;
                    MiscTools.GUIRun(() -> _view.getMemory_status().setText(t));
                }
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        });

        resumeDownloads();

        resumeUploads();

    }

    public static Boolean getResume_uploads() {
        return _resume_uploads;
    }

    public static void setResume_uploads(Boolean resume_uploads) {
        MainPanel._resume_uploads = resume_uploads;
    }

    public static Boolean getResume_downloads() {
        return _resume_downloads;
    }

    public static void setResume_downloads(Boolean resume_downloads) {
        MainPanel._resume_downloads = resume_downloads;
    }

    public boolean isUse_custom_chunks_dir() {
        return _use_custom_chunks_dir;
    }

    public String getCustom_chunks_dir() {
        return _custom_chunks_dir;
    }

    public boolean isExit() {
        return _exit;
    }

    public void setExit(boolean _exit) {
        this._exit = _exit;
    }

    public float getZoom_factor() {
        return _zoom_factor;
    }

    public MegaProxyServer getMega_proxy_server() {
        return _mega_proxy_server;
    }

    public boolean isMegacrypter_reverse() {
        return _megacrypter_reverse;
    }

    public int getMegacrypter_reverse_port() {
        return _megacrypter_reverse_port;
    }

    public void setMega_proxy_server(MegaProxyServer mega_proxy_server) {
        _mega_proxy_server = mega_proxy_server;
    }

    public boolean isUse_mega_account_down() {
        return _use_mega_account_down;
    }

    public String getMega_account_down() {
        return _mega_account_down;
    }

    public boolean isRestart() {
        return _restart;
    }

    public void setRestart(boolean restart) {
        _restart = restart;
    }

    public HashMap<String, Object> getElc_accounts() {
        return _elc_accounts;
    }

    public TrayIcon getTrayicon() {
        return _trayicon;
    }

    public String getMaster_pass_hash() {
        return _master_pass_hash;
    }

    public void setMaster_pass_hash(String master_pass_hash) {
        _master_pass_hash = master_pass_hash;
    }

    public String getMaster_pass_salt() {
        return _master_pass_salt;
    }

    public byte[] getMaster_pass() {
        return _master_pass;
    }

    public void setMaster_pass(byte[] pass) {

        if (_master_pass != null) {

            Arrays.fill(_master_pass, (byte) 0);

            _master_pass = null;
        }

        if (pass != null) {

            _master_pass = new byte[pass.length];

            System.arraycopy(pass, 0, _master_pass, 0, pass.length);
        }
    }

    public MainPanelView getView() {

        while (_view == null) {
            try {
                Thread.sleep(250);
            } catch (InterruptedException ex) {
                Logger.getLogger(MainPanel.class.getName()).log(Level.SEVERE, ex.getMessage());
            }
        }

        return _view;
    }

    public SpeedMeter getGlobal_dl_speed() {
        return _global_dl_speed;
    }

    public SpeedMeter getGlobal_up_speed() {
        return _global_up_speed;
    }

    public DownloadManager getDownload_manager() {
        return _download_manager;
    }

    public UploadManager getUpload_manager() {
        return _upload_manager;
    }

    public StreamThrottlerSupervisor getStream_supervisor() {
        return _stream_supervisor;
    }

    public int getMax_dl() {
        return _max_dl;
    }

    public int getMax_ul() {
        return _max_ul;
    }

    public int getDefault_slots_down() {
        return _default_slots_down;
    }

    public int getDefault_slots_up() {
        return _default_slots_up;
    }

    public boolean isUse_slots_down() {
        return _use_slots_down;
    }

    public String getDefault_download_path() {
        return _default_download_path;
    }

    public HashMap<String, Object> getMega_accounts() {
        return _mega_accounts;
    }

    public HashMap<String, MegaAPI> getMega_active_accounts() {
        return _mega_active_accounts;
    }

    public TrayIcon getIcon() {
        return _trayicon;
    }

    public ClipboardSpy getClipboardspy() {
        return _clipboardspy;
    }

    public KissVideoStreamServer getStreamserver() {
        return _streamserver;
    }

    public int getMax_dl_speed() {
        return _max_dl_speed;
    }

    public int getMax_up_speed() {
        return _max_up_speed;
    }

    public boolean isLimit_download_speed() {
        return _limit_download_speed;
    }

    public boolean isLimit_upload_speed() {
        return _limit_upload_speed;
    }

    public boolean isInit_paused() {
        return _init_paused;
    }

    public void loadUserSettings() {

        String use_custom_chunks_dir = DBTools.selectSettingValue("use_custom_chunks_dir");

        if (use_custom_chunks_dir != null) {

            if (use_custom_chunks_dir.equals("yes")) {

                _use_custom_chunks_dir = true;

                _custom_chunks_dir = DBTools.selectSettingValue("custom_chunks_dir");

            } else {

                _use_custom_chunks_dir = false;

                _custom_chunks_dir = DBTools.selectSettingValue("custom_chunks_dir");
            }

        } else {

            _custom_chunks_dir = null;
        }

        String zoom_factor = selectSettingValue("font_zoom");

        if (zoom_factor != null) {
            _zoom_factor = Float.parseFloat(zoom_factor) / 100;
        } else {
            _zoom_factor = ZOOM_FACTOR;
        }

        String _font = selectSettingValue("font");

        if (_font != null) {
            if (_font.equals("DEFAULT")) {

                GUI_FONT = new JLabel().getFont();

            } else {

                GUI_FONT = createAndRegisterFont("/fonts/NotoSansCJK-Regular.ttc");

            }
        } else {

            GUI_FONT = createAndRegisterFont("/fonts/NotoSansCJK-Regular.ttc");
        }

        String def_slots = selectSettingValue("default_slots_down");

        if (def_slots != null) {
            _default_slots_down = parseInt(def_slots);
        } else {
            _default_slots_down = Download.WORKERS_DEFAULT;
        }

        def_slots = selectSettingValue("default_slots_up");

        if (def_slots != null) {
            _default_slots_up = parseInt(def_slots);
        } else {
            _default_slots_up = Upload.WORKERS_DEFAULT;
        }

        String use_slots = selectSettingValue("use_slots_down");

        if (use_slots != null) {
            _use_slots_down = use_slots.equals("yes");
        } else {
            _use_slots_down = Download.USE_SLOTS_DEFAULT;
        }

        String max_downloads = selectSettingValue("max_downloads");

        if (max_downloads != null) {
            _max_dl = parseInt(max_downloads);
        } else {
            _max_dl = Download.SIM_TRANSFERENCES_DEFAULT;
        }

        String max_uploads = selectSettingValue("max_uploads");

        if (max_uploads != null) {
            _max_ul = parseInt(max_uploads);
        } else {
            _max_ul = Upload.SIM_TRANSFERENCES_DEFAULT;
        }

        _default_download_path = selectSettingValue("default_down_dir");

        if (_default_download_path == null) {
            _default_download_path = ".";
        }

        String limit_dl_speed = selectSettingValue("limit_download_speed");

        if (limit_dl_speed != null) {

            _limit_download_speed = limit_dl_speed.equals("yes");

        } else {

            _limit_download_speed = LIMIT_TRANSFERENCE_SPEED_DEFAULT;
        }

        String limit_ul_speed = selectSettingValue("limit_upload_speed");

        if (limit_ul_speed != null) {

            _limit_upload_speed = limit_ul_speed.equals("yes");

        } else {

            _limit_upload_speed = LIMIT_TRANSFERENCE_SPEED_DEFAULT;
        }

        String max_download_speed = selectSettingValue("max_download_speed");

        if (max_download_speed != null) {
            _max_dl_speed = parseInt(max_download_speed);
        } else {
            _max_dl_speed = MAX_TRANSFERENCE_SPEED_DEFAULT;
        }

        String max_upload_speed = selectSettingValue("max_upload_speed");

        if (max_upload_speed != null) {
            _max_up_speed = parseInt(max_upload_speed);
        } else {
            _max_up_speed = MAX_TRANSFERENCE_SPEED_DEFAULT;
        }

        String init_paused_string = DBTools.selectSettingValue("start_frozen");

        if (init_paused_string != null) {

            _init_paused = init_paused_string.equals("yes");
        } else {
            _init_paused = false;
        }

        try {
            _mega_accounts = selectMegaAccounts();
            _elc_accounts = selectELCAccounts();
        } catch (SQLException ex) {
            Logger.getLogger(MainPanel.class.getName()).log(SEVERE, null, ex);
        }

        _mega_account_down = DBTools.selectSettingValue("mega_account_down");

        String use_account;

        _use_mega_account_down = ((use_account = DBTools.selectSettingValue("use_mega_account_down")) != null && use_account.equals("yes"));

        _master_pass_hash = DBTools.selectSettingValue("master_pass_hash");

        _master_pass_salt = DBTools.selectSettingValue("master_pass_salt");

        if (_master_pass_salt == null) {

            try {

                _master_pass_salt = Bin2BASE64(genRandomByteArray(CryptTools.MASTER_PASSWORD_PBKDF2_SALT_BYTE_LENGTH));

                DBTools.insertSettingValue("master_pass_salt", _master_pass_salt);

            } catch (SQLException ex) {
                Logger.getLogger(MainPanel.class.getName()).log(Level.SEVERE, ex.getMessage());
            }
        }

        String use_proxy = selectSettingValue("use_proxy");

        if (use_proxy != null) {
            _use_proxy = use_proxy.equals("yes");
        } else {
            _use_proxy = false;
        }

        if (_use_proxy) {

            _proxy_host = DBTools.selectSettingValue("proxy_host");

            String proxy_port = DBTools.selectSettingValue("proxy_port");

            _proxy_port = MiscTools.parseIntOr(proxy_port, 8080);

            _proxy_user = DBTools.selectSettingValue("proxy_user");

            _proxy_pass = DBTools.selectSettingValue("proxy_pass");
        }

        String run_command_string = DBTools.selectSettingValue("run_command");

        if (run_command_string != null) {

            _run_command = run_command_string.equals("yes");
        }

        String old_run_command_path = _run_command_path;

        _run_command_path = DBTools.selectSettingValue("run_command_path");

        if (_run_command && old_run_command_path != null && !old_run_command_path.equals(_run_command_path)) {
            LAST_EXTERNAL_COMMAND_TIMESTAMP = -1;
        }

        String use_megacrypter_reverse = selectSettingValue("megacrypter_reverse");

        if (use_megacrypter_reverse != null) {
            _megacrypter_reverse = use_megacrypter_reverse.equals("yes");
        } else {
            _megacrypter_reverse = false;
        }

        if (_megacrypter_reverse) {

            String reverse_port = DBTools.selectSettingValue("megacrypter_reverse_port");

            _megacrypter_reverse_port = MiscTools.parseIntOr(reverse_port, DEFAULT_MEGA_PROXY_PORT);
        }

        String use_smart_proxy = selectSettingValue("smart_proxy");

        if (use_smart_proxy != null) {
            _use_smart_proxy = use_smart_proxy.equals("yes");
        } else {
            _use_smart_proxy = DEFAULT_SMART_PROXY;
        }

        _language = DBTools.selectSettingValue("language");

        if (_language == null) {
            _language = DEFAULT_LANGUAGE;
        }

        String debug_file = selectSettingValue("debug_file");

        if (debug_file != null) {
            _debug_file = debug_file.equals("yes");
        } else {
            _debug_file = false;
        }

        String api_key = DBTools.selectSettingValue("mega_api_key");

        if (api_key != null && !"".equals(api_key)) {

            MegaAPI.API_KEY = api_key.trim();

        } else {
            MegaAPI.API_KEY = null;
        }

    }

    public static synchronized void run_external_command() {

        if (_run_command && (LAST_EXTERNAL_COMMAND_TIMESTAMP == -1 || LAST_EXTERNAL_COMMAND_TIMESTAMP + RUN_COMMAND_TIME * 1000 < System.currentTimeMillis())) {

            if (_run_command_path != null && !_run_command_path.equals("")) {
                try {
                    String cmd = _run_command_path;
                    java.io.File f = new java.io.File(cmd);
                    java.util.List<String> argv;
                    if (f.exists()) {
                        // Treat the whole setting as a single binary path; this
                        // makes "C:\Program Files\foo\bar.exe" work without the
                        // old whitespace-split bug. If you need args, wrap in a
                        // .bat/.sh script.
                        argv = java.util.Collections.singletonList(cmd);
                    } else {
                        // Backwards-compat: command isn't a real file, fall back
                        // to legacy whitespace-token splitting so existing
                        // "command arg1 arg2" configs keep working.
                        argv = java.util.Arrays.asList(cmd.trim().split("\\s+"));
                    }
                    new ProcessBuilder(argv).inheritIO().start();
                } catch (IOException ex) {
                    Logger.getLogger(MainPanel.class.getName()).log(Level.SEVERE, ex.getMessage());
                }

                LAST_EXTERNAL_COMMAND_TIMESTAMP = System.currentTimeMillis();
            }
        }
    }

    public boolean checkByeBye() {

        boolean exit = true;

        if (!_streamserver.getWorking_threads().isEmpty()) {

            Object[] options = {"No",
                LabelTranslatorSingleton.getInstance().translate("Yes")};

            int n = showOptionDialog(getView(),
                    LabelTranslatorSingleton.getInstance().translate("It seems MegaBasterd is streaming video. Do you want to exit?"),
                    LabelTranslatorSingleton.getInstance().translate("Warning!"), YES_NO_CANCEL_OPTION, QUESTION_MESSAGE,
                    null,
                    options,
                    options[0]);

            if (n == 0) {

                exit = false;
            }

        } else if (!getDownload_manager().getTransference_preprocess_global_queue().isEmpty() || !getDownload_manager().getTransference_provision_queue().isEmpty() || !getUpload_manager().getTransference_preprocess_global_queue().isEmpty() || !getUpload_manager().getTransference_provision_queue().isEmpty()) {

            Object[] options = {"No",
                LabelTranslatorSingleton.getInstance().translate("Yes")};

            int n = showOptionDialog(getView(),
                    LabelTranslatorSingleton.getInstance().translate("It seems MegaBasterd is provisioning down/uploads.\n\nIf you exit now, unprovisioned down/uploads will be lost.\n\nDo you want to continue?"),
                    LabelTranslatorSingleton.getInstance().translate("Warning!"), YES_NO_CANCEL_OPTION, WARNING_MESSAGE,
                    null,
                    options,
                    options[0]);

            if (n == 0) {

                exit = false;
            }

        }

        return exit;
    }

    public void byebyenow(boolean restart) {

        MiscTools.purgeFolderCache();

        synchronized (DBTools.class) {

            try {
                DBTools.vaccum();
            } catch (SQLException ex) {
                Logger.getLogger(MainPanel.class.getName()).log(Level.SEVERE, ex.getMessage());
            }

            // Close the shared SQLite connection cleanly so the WAL is
            // checkpointed before process exit / restart. Without this the
            // -wal / -shm sidecar files can linger and a restart can race
            // a still-held file handle (worst on Windows).
            try {
                SqliteSingleton.getInstance().shutdown();
            } catch (Exception ex) {
                Logger.getLogger(MainPanel.class.getName()).log(Level.WARNING, "SqliteSingleton shutdown: {0}", ex.getMessage());
            }

            // Release the single-instance file lock so a restart can re-acquire
            // it immediately (Windows file-sharing semantics can otherwise delay
            // the new instance and make the user see "click restart, nothing
            // happens").
            try {
                if (_single_instance_lock != null) {
                    _single_instance_lock.release();
                }
                if (_single_instance_raf != null) {
                    _single_instance_raf.close();
                }
            } catch (IOException ex) {
                Logger.getLogger(MainPanel.class.getName()).log(Level.FINE, "Releasing single-instance lock: {0}", ex.getMessage());
            }

            if (restart) {
                restartApplication();
            } else {
                exit(0);
            }

        }
    }

    public void byebyenow(boolean restart, boolean delete_db) {

        synchronized (DBTools.class) {

            if (delete_db) {

                // Close the connection before deleting the file to avoid
                // a Windows-shared-handle race.
                try {
                    SqliteSingleton.getInstance().shutdown();
                } catch (Exception ex) {
                    Logger.getLogger(MainPanel.class.getName()).log(Level.WARNING, "SqliteSingleton shutdown: {0}", ex.getMessage());
                }

                File db_file = new File(MainPanel.MEGABASTERD_HOME_DIR + "/.megabasterd" + MainPanel.VERSION + "/" + SqliteSingleton.SQLITE_FILE);

                db_file.delete();

            } else {
                try {
                    DBTools.vaccum();
                } catch (SQLException ex) {
                    Logger.getLogger(MainPanel.class.getName()).log(Level.SEVERE, ex.getMessage());
                }

                try {
                    SqliteSingleton.getInstance().shutdown();
                } catch (Exception ex) {
                    Logger.getLogger(MainPanel.class.getName()).log(Level.WARNING, "SqliteSingleton shutdown: {0}", ex.getMessage());
                }
            }

            if (restart) {
                restartApplication();
            } else {
                exit(0);
            }

        }
    }

    private void _check_old_version() {

        try {

            if (!new File(MainPanel.MEGABASTERD_HOME_DIR + "/.megabasterd" + MainPanel.VERSION + "/.old_version_check").exists()) {

                new File(MainPanel.MEGABASTERD_HOME_DIR + "/.megabasterd" + MainPanel.VERSION + "/.old_version_check").createNewFile();

                File directory = new File(MainPanel.MEGABASTERD_HOME_DIR);

                String version_major = findFirstRegex("([0-9]+)\\.[0-9]+$", VERSION, 1);

                String version_minor = findFirstRegex("[0-9]+\\.([0-9]+)$", VERSION, 1);

                String old_version_major = "0";

                String old_version_minor = "0";

                String old_version = "0.0";

                File old_backups_dir = new File(MainPanel.MEGABASTERD_HOME_DIR + "/.megabasterd_old_backups");

                if (!old_backups_dir.exists()) {
                    old_backups_dir.mkdir();
                }

                for (File file : directory.listFiles()) {

                    try {
                        if (file.isDirectory() && file.canRead() && file.getName().startsWith(".megabasterd") && !file.getName().endsWith("backups")) {

                            String current_dir_version = MiscTools.findFirstRegex("[0-9.]+$", file.getName(), 0);

                            if (current_dir_version != null && !current_dir_version.equals(VERSION)) {

                                old_version_major = findFirstRegex("([0-9]+)\\.[0-9]+$", old_version, 1);
                                old_version_minor = findFirstRegex("[0-9]+\\.([0-9]+)$", old_version, 1);

                                String current_dir_major = findFirstRegex("([0-9]+)\\.[0-9]+$", current_dir_version, 1);
                                String current_dir_minor = findFirstRegex("[0-9]+\\.([0-9]+)$", current_dir_version, 1);

                                if (Integer.parseInt(current_dir_major) > Integer.parseInt(old_version_major) || (Integer.parseInt(current_dir_major) == Integer.parseInt(old_version_major) && Integer.parseInt(current_dir_minor) > Integer.parseInt(old_version_minor))) {
                                    old_version = current_dir_version;
                                    old_version_major = current_dir_major;
                                    old_version_minor = current_dir_minor;
                                }

                                Files.move(Paths.get(file.getAbsolutePath()), Paths.get(old_backups_dir.getAbsolutePath() + "/" + file.getName()), StandardCopyOption.REPLACE_EXISTING);
                            }

                        }
                    } catch (Exception e) {
                    }

                }

                if (!old_version.equals("0.0") && (Integer.parseInt(version_major) > Integer.parseInt(old_version_major) || (Integer.parseInt(version_major) == Integer.parseInt(old_version_major) && Integer.parseInt(version_minor) > Integer.parseInt(old_version_minor)))) {
                    Object[] options = {"No",
                        LabelTranslatorSingleton.getInstance().translate("Yes")};

                    int n = showOptionDialog(getView(),
                            LabelTranslatorSingleton.getInstance().translate("An older version (" + old_version + ") of MegaBasterd has been detected.\nDo you want to import all current settings and transfers from the previous version?\nWARNING: INCOMPATIBILITIES MAY EXIST BETWEEN VERSIONS."),
                            LabelTranslatorSingleton.getInstance().translate("Warning!"), YES_NO_CANCEL_OPTION, JOptionPane.INFORMATION_MESSAGE,
                            null,
                            options,
                            options[0]);

                    if (n == 1) {
                        Files.copy(Paths.get(MainPanel.MEGABASTERD_HOME_DIR + "/.megabasterd_old_backups/.megabasterd" + old_version + "/" + SqliteSingleton.SQLITE_FILE), Paths.get(MainPanel.MEGABASTERD_HOME_DIR + "/.megabasterd" + MainPanel.VERSION + "/" + SqliteSingleton.SQLITE_FILE), StandardCopyOption.REPLACE_EXISTING);

                        JOptionPane.showMessageDialog(getView(), LabelTranslatorSingleton.getInstance().translate("MegaBasterd will restart"), LabelTranslatorSingleton.getInstance().translate("Restart required"), JOptionPane.WARNING_MESSAGE);

                        restartApplication();
                    }
                }
            }

        } catch (IOException ex) {
            Logger.getLogger(MainPanel.class.getName()).log(Level.SEVERE, ex.getMessage());
        }

    }

    public void byebye(boolean restart) {

        _byebye(restart, true);
    }

    public void byebye(boolean restart, boolean restart_warning) {

        _byebye(restart, restart_warning);
    }

    private void _byebye(boolean restart, boolean restart_warning) {

        if (!_exit && checkByeBye()) {

            if (restart && restart_warning) {
                JOptionPane.showMessageDialog(getView(), LabelTranslatorSingleton.getInstance().translate("MegaBasterd will restart"), LabelTranslatorSingleton.getInstance().translate("Restart required"), JOptionPane.WARNING_MESSAGE);
            }

            _exit = true;

            getView().getPause_all_down_button().setEnabled(false);

            getView().getPause_all_up_button().setEnabled(false);

            getView().setEnabled(false);

            if (!_download_manager.getTransference_running_list().isEmpty() || !_upload_manager.getTransference_running_list().isEmpty() || !_download_manager.getTransference_waitstart_queue().isEmpty() || !_upload_manager.getTransference_waitstart_queue().isEmpty()) {

                // Hard cap on the graceful drain. Without this the dialog can sit
                // indefinitely waiting for workers that are blocked inside a 60s
                // HTTP read timeout (or repeatedly on consecutive 509 backoffs);
                // the user ends up clicking EXIT NOW anyway. After this much wall
                // time we just call byebyenow ourselves -- the queue has already
                // been persisted upfront, so nothing is lost. (#751)
                final long SHUTDOWN_HARD_TIMEOUT_MS = 30_000L;

                final WarningExitMessage exit_message = new WarningExitMessage(getView(), true, this, restart);

                THREAD_POOL.execute(() -> {

                    final long start = System.currentTimeMillis();

                    // -------------------------------------------------------------
                    // 1) Snapshot the queue UPFRONT and persist it once.
                    //    The previous code rebuilt this snapshot every iteration
                    //    of the drain loop, which meant a download that exited
                    //    between two iterations dropped out of the persisted set
                    //    (its URL never made it to download_queue). Doing it once
                    //    here -- before any transference has had a chance to exit
                    //    -- guarantees the full resume set survives across the
                    //    app restart. (#751)
                    // -------------------------------------------------------------
                    ArrayList<String> downloads_queue = new ArrayList<>();
                    for (Transference t : _download_manager.getTransference_running_list()) {
                        downloads_queue.add(((Download) t).getUrl());
                    }
                    for (Transference t : _download_manager.getTransference_waitstart_queue()) {
                        downloads_queue.add(((Download) t).getUrl());
                    }

                    ArrayList<String> uploads_queue = new ArrayList<>();
                    for (Transference t : _upload_manager.getTransference_running_list()) {
                        uploads_queue.add(t.getFile_name());
                    }
                    for (Transference t : _upload_manager.getTransference_waitstart_queue()) {
                        uploads_queue.add(t.getFile_name());
                    }

                    // Save per-upload progress (mac data) up front too, so a
                    // resume picks up close to the byte that was being chunked
                    // when the user clicked exit.
                    for (Transference t : _upload_manager.getTransference_running_list()) {
                        Upload upload = (Upload) t;
                        try {
                            DBTools.updateUploadProgress(upload.getFile_name(), upload.getMa().getFull_email(), upload.getProgress(), upload.getTemp_mac_data() != null ? upload.getTemp_mac_data() : null);
                        } catch (SQLException ex) {
                            Logger.getLogger(MainPanel.class.getName()).log(Level.SEVERE, ex.getMessage());
                        }
                    }

                    boolean db_ok = true;
                    try {
                        DBTools.truncateDownloadsQueue();
                        DBTools.insertDownloadsQueue(downloads_queue);
                        DBTools.truncateUploadsQueue();
                        DBTools.insertUploadsQueue(uploads_queue);
                    } catch (SQLException ex) {
                        db_ok = false;
                        Logger.getLogger(MainPanel.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    exit_message.setDbSaved(db_ok);

                    // -------------------------------------------------------------
                    // 2) Wake every worker and cut in-flight I/O.
                    //    ChunkDownloader.RESET_CURRENT_CHUNK() closes the current
                    //    chunk InputStream, which forces any blocking read() /
                    //    getResponseCode() to throw IOException promptly instead
                    //    of waiting out the 60s HTTP_READ_TIMEOUT. The worker's
                    //    outer-loop then sees main_panel.isExit() == true and
                    //    exits. Without this, the drain loop could legitimately
                    //    sit for ~60s per stuck worker. (#751)
                    // -------------------------------------------------------------
                    for (Transference trans : _download_manager.getTransference_running_list()) {
                        Download download = (Download) trans;
                        if (download.isPaused()) {
                            download.pause();
                        }
                        MiscTools.GUIRun(() -> {
                            download.getView().printStatusNormal("Stopping download safely before exit MegaBasterd, please wait...");
                            download.getView().getSlots_spinner().setEnabled(false);
                            download.getView().getPause_button().setEnabled(false);
                            download.getView().getCopy_link_button().setEnabled(false);
                            download.getView().getOpen_folder_button().setEnabled(false);
                            download.getView().getFile_size_label().setEnabled(false);
                            download.getView().getFile_name_label().setEnabled(false);
                            download.getView().getSpeed_label().setEnabled(false);
                            download.getView().getSlots_label().setEnabled(false);
                            download.getView().getProgress_pbar().setEnabled(false);
                        });
                        for (ChunkDownloader cd : download.getChunkworkers()) {
                            try {
                                cd.RESET_CURRENT_CHUNK();
                            } catch (Exception ignore) {
                            }
                            cd.secureNotify();
                        }
                    }
                    for (Transference trans : _upload_manager.getTransference_running_list()) {
                        Upload upload = (Upload) trans;
                        if (upload.getMac_generator() != null) {
                            upload.getMac_generator().secureNotify();
                        }
                        if (upload.isPaused()) {
                            upload.pause();
                        }
                        MiscTools.GUIRun(() -> {
                            upload.getView().printStatusNormal("Stopping upload safely before exit MegaBasterd, please wait...");
                            upload.getView().getSlots_spinner().setEnabled(false);
                            upload.getView().getPause_button().setEnabled(false);
                            upload.getView().getFolder_link_button().setEnabled(false);
                            upload.getView().getFile_link_button().setEnabled(false);
                            upload.getView().getFile_size_label().setEnabled(false);
                            upload.getView().getFile_name_label().setEnabled(false);
                            upload.getView().getSpeed_label().setEnabled(false);
                            upload.getView().getSlots_label().setEnabled(false);
                            upload.getView().getProgress_pbar().setEnabled(false);
                        });
                        for (ChunkUploader cu : upload.getChunkworkers()) {
                            cu.secureNotify();
                        }
                    }

                    // -------------------------------------------------------------
                    // 3) Drain loop with live status push and hard timeout.
                    // -------------------------------------------------------------
                    boolean wait;
                    boolean timed_out = false;
                    do {
                        int dl_count = 0, dl_workers = 0, ul_count = 0, ul_workers = 0;

                        for (Transference trans : _download_manager.getTransference_running_list()) {
                            Download download = (Download) trans;
                            dl_count++;
                            dl_workers += download.getChunkworkers().size();
                        }
                        for (Transference trans : _upload_manager.getTransference_running_list()) {
                            Upload upload = (Upload) trans;
                            ul_count++;
                            ul_workers += upload.getChunkworkers().size();
                        }

                        wait = (dl_workers > 0 || ul_workers > 0);

                        long elapsed = System.currentTimeMillis() - start;
                        exit_message.updateStatus(dl_count, dl_workers, ul_count, ul_workers, elapsed, SHUTDOWN_HARD_TIMEOUT_MS);

                        if (elapsed >= SHUTDOWN_HARD_TIMEOUT_MS) {
                            timed_out = true;
                            break;
                        }

                        if (wait) {
                            try {
                                Thread.sleep(500);
                            } catch (InterruptedException ex) {
                                Thread.currentThread().interrupt();
                                Logger.getLogger(MainPanel.class.getName()).log(Level.SEVERE, ex.getMessage());
                                break;
                            }
                        }
                    } while (wait);

                    if (timed_out) {
                        Logger.getLogger(MainPanel.class.getName()).log(Level.WARNING,
                                "Shutdown drain hit {0}ms hard timeout -- forcing exit", SHUTDOWN_HARD_TIMEOUT_MS);
                    }

                    byebyenow(restart);
                });

                exit_message.setLocationRelativeTo(getView());

                exit_message.setVisible(true);

            } else {
                byebyenow(restart);
            }
        }
    }

    private static java.io.RandomAccessFile _single_instance_raf;
    private static java.nio.channels.FileLock _single_instance_lock;

    private boolean checkAppIsRunning() {

        // First, try to obtain an exclusive file lock on a per-user
        // sentinel file. This is the canonical Java single-instance
        // pattern and is immune to "some other process already bound
        // port 1338" (see GH #717).
        try {
            File lock_file = new File(MEGABASTERD_HOME_DIR + "/.megabasterd" + VERSION + "/.megabasterd.lock");
            lock_file.getParentFile().mkdirs();
            _single_instance_raf = new java.io.RandomAccessFile(lock_file, "rw");
            _single_instance_lock = _single_instance_raf.getChannel().tryLock();

            if (_single_instance_lock == null) {
                // Another MegaBasterd instance already holds the lock.
                // Try to ping its watchdog so it pops to the foreground;
                // if that fails because the port is taken by something
                // else, no harm done -- still return "running".
                _pingWatchdog();
                try {
                    _single_instance_raf.close();
                } catch (IOException ignore) {
                }
                return true;
            }
        } catch (Exception ex) {
            Logger.getLogger(MainPanel.class.getName()).log(Level.WARNING, "Single-instance file lock failed: {0}", ex.getMessage());
            // Fall through to start a watchdog listener if we can.
        }

        // We are the only running instance. Spin up a watchdog listener
        // so future invocations can ask us to come to the foreground.
        // Try the configured port first, then a few alternates so a
        // foreign process holding 1338 doesn't break us.
        for (int port = WATCHDOG_PORT; port < WATCHDOG_PORT + 10; port++) {
            try {
                final ServerSocket serverSocket = new ServerSocket(port, 0, InetAddress.getLoopbackAddress());

                THREAD_POOL.execute(() -> {
                    while (!serverSocket.isClosed()) {
                        try {
                            try (Socket peer = serverSocket.accept()) {
                                MiscTools.GUIRun(() -> {
                                    getView().setExtendedState(NORMAL);
                                    getView().setVisible(true);
                                });
                            }
                        } catch (IOException ex1) {
                            if (!serverSocket.isClosed()) {
                                Logger.getLogger(MainPanel.class.getName()).log(Level.FINE, ex1.getMessage());
                            }
                            return;
                        }
                    }
                });
                break;
            } catch (Exception ex) {
                Logger.getLogger(MainPanel.class.getName()).log(Level.FINE, "Watchdog port {0} busy, trying next", port);
            }
        }

        return false;
    }

    private void _pingWatchdog() {
        for (int port = WATCHDOG_PORT; port < WATCHDOG_PORT + 10; port++) {
            try (Socket clientSocket = new Socket(InetAddress.getLoopbackAddress(), port)) {
                return;
            } catch (Exception ex) {
                // try next port
            }
        }
    }

    public void resumeDownloads() {

        if (!getResume_downloads()) {

            MiscTools.GUIRun(() -> {
                getView().getStatus_down_label().setText(LabelTranslatorSingleton.getInstance().translate("Checking if there are previous downloads, please wait..."));
            });

            final MainPanel tthis = this;

            THREAD_POOL.execute(() -> {
                int conta_downloads = 0, tot_downloads = -1;
                try {

                    ArrayList<String> downloads_queue = DBTools.selectDownloadsQueue();

                    HashMap<String, HashMap<String, Object>> res = selectDownloads();

                    tot_downloads = res.size();

                    Iterator downloads_queue_iterator = downloads_queue.iterator();

                    while (downloads_queue_iterator.hasNext()) {

                        try {

                            String url = (String) downloads_queue_iterator.next();

                            HashMap<String, Object> o = res.remove(url);

                            if (o != null) {

                                String email = (String) o.get("email");

                                if (_mega_accounts.get(email) == null) {
                                    email = null;
                                }

                                MegaAPI ma = new MegaAPI();

                                if (email == null || !tthis.isUse_mega_account_down() || (ma = checkMegaAccountLoginAndShowMasterPassDialog(tthis, getView(), email)) != null) {

                                    Download download = new Download(tthis, ma, (String) url, (String) o.get("path"), (String) o.get("filename"), (String) o.get("filekey"), (Long) o.get("filesize"), (String) o.get("filepass"), (String) o.get("filenoexpire"), _use_slots_down, false, (String) o.get("custom_chunks_dir"), false);

                                    getDownload_manager().getTransference_provision_queue().add(download);

                                    conta_downloads++;

                                    downloads_queue_iterator.remove();
                                } else {
                                    tot_downloads--;
                                }
                            }

                        } catch (Exception ex) {
                            Logger.getLogger(MainPanel.class.getName()).log(SEVERE, null, ex);
                        }
                    }

                    DBTools.truncateDownloadsQueue();

                    if (!downloads_queue.isEmpty()) {
                        DBTools.insertDownloadsQueue(downloads_queue);
                    }

                    if (!res.isEmpty()) {

                        for (Map.Entry<String, HashMap<String, Object>> entry : res.entrySet()) {

                            try {

                                String email = (String) entry.getValue().get("email");

                                if (_mega_accounts.get(email) == null) {
                                    email = null;
                                }

                                MegaAPI ma = new MegaAPI();

                                if (email == null || !tthis.isUse_mega_account_down() || (ma = checkMegaAccountLoginAndShowMasterPassDialog(tthis, getView(), email)) != null) {

                                    Download download = new Download(tthis, ma, (String) entry.getKey(), (String) entry.getValue().get("path"), (String) entry.getValue().get("filename"), (String) entry.getValue().get("filekey"), (Long) entry.getValue().get("filesize"), (String) entry.getValue().get("filepass"), (String) entry.getValue().get("filenoexpire"), _use_slots_down, false, (String) entry.getValue().get("custom_chunks_dir"), false);

                                    getDownload_manager().getTransference_provision_queue().add(download);

                                    conta_downloads++;

                                } else {

                                    tot_downloads--;
                                }

                            } catch (Exception ex) {
                                Logger.getLogger(MainPanel.class.getName()).log(SEVERE, null, ex);
                            }

                        }

                    }

                } catch (Exception ex) {
                    Logger.getLogger(MainPanel.class.getName()).log(SEVERE, null, ex);
                }

                if (conta_downloads > 0) {

                    if (conta_downloads == tot_downloads) {
                        setResume_downloads(true);
                    }

                    _download_manager.setSort_wait_start_queue(false);
                    getDownload_manager().secureNotify();
                    MiscTools.GUIRun(() -> {
                        getView().getjTabbedPane1().setSelectedIndex(0);
                    });

                } else {
                    setResume_downloads(true);
                }

                MiscTools.GUIRun(() -> {
                    getView().getStatus_down_label().setText("");
                });
            });

        }

    }

    public void trayIcon() throws AWTException {

        if (java.awt.SystemTray.isSupported()) {

            JPopupMenu menu = new JPopupMenu();

            Font new_font = GUI_FONT;

            menu.setFont(new_font.deriveFont(Font.BOLD, Math.round(14 * ZOOM_FACTOR)));

            JMenuItem messageItem = new JMenuItem(LabelTranslatorSingleton.getInstance().translate("Restore window"));

            messageItem.addActionListener((ActionEvent e) -> {

                getView().setExtendedState(NORMAL);

                getView().setVisible(true);

                getView().revalidate();

                getView().repaint();

            });

            menu.add(messageItem);

            JMenuItem closeItem = new JMenuItem(LabelTranslatorSingleton.getInstance().translate("EXIT"));

            closeItem.addActionListener((ActionEvent e) -> {
                if (!getView().isVisible()) {

                    getView().setExtendedState(NORMAL);
                    getView().setVisible(true);
                    getView().revalidate();
                    getView().repaint();

                }

                byebye(false);
            });

            menu.add(closeItem);

            _trayicon = new TrayIcon(getDefaultToolkit().getImage(getClass().getResource(ICON_FILE)), "MegaBasterd", null);

            _trayicon.setToolTip("MegaBasterd " + VERSION);

            _trayicon.setImageAutoSize(true);

            _trayicon.addMouseListener(new MouseAdapter() {
                public void mouseReleased(MouseEvent e) {

                    if (SwingUtilities.isRightMouseButton(e)) {
                        menu.setLocation(e.getX(), e.getY());
                        menu.setInvoker(menu);
                        menu.setVisible(true);
                    } else {
                        if (!getView().isVisible()) {
                            getView().setExtendedState(NORMAL);
                            getView().setVisible(true);
                            getView().revalidate();
                            getView().repaint();

                        } else {

                            getView().dispatchEvent(new WindowEvent(getView(), WINDOW_CLOSING));
                        }
                    }

                }
            });

            getSystemTray().add(_trayicon);

        }

    }

    public void resumeUploads() {

        if (!getResume_uploads()) {

            MiscTools.GUIRun(() -> {
                getView().getStatus_up_label().setText(LabelTranslatorSingleton.getInstance().translate("Checking if there are previous uploads, please wait..."));
            });

            final MainPanel tthis = this;

            THREAD_POOL.execute(() -> {
                int conta_uploads = 0, tot_uploads = -1;
                try {

                    ArrayList<String> uploads_queue = DBTools.selectUploadsQueue();

                    HashMap<String, HashMap<String, Object>> res = selectUploads();

                    tot_uploads = res.size();

                    Iterator uploads_queue_iterator = uploads_queue.iterator();

                    while (uploads_queue_iterator.hasNext()) {

                        try {
                            String filename = (String) uploads_queue_iterator.next();

                            HashMap<String, Object> o = res.remove(filename);

                            if (o != null) {

                                String email = (String) o.get("email");

                                if (_mega_accounts.get(email) != null) {

                                    MegaAPI ma;

                                    if ((ma = checkMegaAccountLoginAndShowMasterPassDialog(tthis, getView(), email)) != null) {

                                        Upload upload = new Upload(tthis, ma, (String) filename, (String) o.get("parent_node"), (String) o.get("ul_key") != null ? bin2i32a(BASE642Bin((String) o.get("ul_key"))) : null, (String) o.get("url"), (String) o.get("root_node"), BASE642Bin((String) o.get("share_key")), (String) o.get("folder_link"), false);

                                        getUpload_manager().getTransference_provision_queue().add(upload);

                                        conta_uploads++;

                                        uploads_queue_iterator.remove();

                                    }

                                } else {

                                    deleteUpload((String) o.get("filename"), email);

                                    tot_uploads--;

                                    uploads_queue_iterator.remove();
                                }

                            }

                        } catch (Exception ex) {
                            Logger.getLogger(MainPanel.class.getName()).log(SEVERE, null, ex);
                        }
                    }

                    DBTools.truncateUploadsQueue();

                    if (!uploads_queue.isEmpty()) {
                        DBTools.insertUploadsQueue(uploads_queue);
                    }

                    if (!res.isEmpty()) {

                        for (Map.Entry<String, HashMap<String, Object>> entry : res.entrySet()) {

                            try {
                                String email = (String) entry.getValue().get("email");

                                if (_mega_accounts.get(email) != null) {

                                    MegaAPI ma;

                                    if ((ma = checkMegaAccountLoginAndShowMasterPassDialog(tthis, getView(), email)) != null) {

                                        Upload upload = new Upload(tthis, ma, (String) entry.getKey(), (String) entry.getValue().get("parent_node"), (String) entry.getValue().get("ul_key") != null ? bin2i32a(BASE642Bin((String) entry.getValue().get("ul_key"))) : null, (String) entry.getValue().get("url"), (String) entry.getValue().get("root_node"), BASE642Bin((String) entry.getValue().get("share_key")), (String) entry.getValue().get("folder_link"), false);

                                        getUpload_manager().getTransference_provision_queue().add(upload);

                                        conta_uploads++;
                                    }

                                } else {

                                    deleteUpload((String) entry.getValue().get("filename"), email);

                                    tot_uploads--;
                                }

                            } catch (Exception ex) {
                                Logger.getLogger(MainPanel.class.getName()).log(SEVERE, null, ex);
                            }
                        }

                    }

                } catch (Exception ex) {
                    Logger.getLogger(MainPanel.class.getName()).log(SEVERE, null, ex);
                }

                if (conta_uploads > 0) {

                    if (conta_uploads == tot_uploads) {
                        setResume_uploads(true);
                    }

                    _upload_manager.setSort_wait_start_queue(false);
                    getUpload_manager().secureNotify();
                    MiscTools.GUIRun(() -> {
                        getView().getjTabbedPane1().setSelectedIndex(1);
                    });
                } else {
                    setResume_uploads(true);
                }

                MiscTools.GUIRun(() -> {
                    getView().getStatus_up_label().setText("");
                });
            }
            );
        }
    }

}
