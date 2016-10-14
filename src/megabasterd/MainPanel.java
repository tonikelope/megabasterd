package megabasterd;

import java.awt.AWTException;
import java.awt.Color;
import static java.awt.EventQueue.invokeLater;
import java.awt.Font;
import static java.awt.Font.BOLD;
import static java.awt.Frame.NORMAL;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import static java.awt.SystemTray.getSystemTray;
import static java.awt.Toolkit.getDefaultToolkit;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import static java.awt.event.WindowEvent.WINDOW_CLOSING;
import java.io.IOException;
import static java.lang.Integer.parseInt;
import static java.lang.System.exit;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import static java.util.concurrent.Executors.newCachedThreadPool;
import java.util.logging.Level;
import static java.util.logging.Level.SEVERE;
import java.util.logging.Logger;
import static java.util.logging.Logger.getLogger;
import javax.swing.JOptionPane;
import static javax.swing.JOptionPane.QUESTION_MESSAGE;
import static javax.swing.JOptionPane.YES_NO_CANCEL_OPTION;
import static javax.swing.JOptionPane.showOptionDialog;
import static megabasterd.DBTools.deleteUpload;
import static megabasterd.DBTools.selectDownloads;
import static megabasterd.DBTools.selectMegaAccounts;
import static megabasterd.DBTools.selectSettingValueFromDB;
import static megabasterd.DBTools.selectUploads;
import static megabasterd.DBTools.setupSqliteTables;
import static megabasterd.MiscTools.BASE642Bin;
import static megabasterd.MiscTools.Bin2BASE64;
import static megabasterd.MiscTools.bin2i32a;
import static megabasterd.MiscTools.createAndRegisterFont;
import static megabasterd.MiscTools.setNimbusLookAndFeel;
import static megabasterd.MiscTools.swingReflectionInvoke;
import static megabasterd.MiscTools.swingReflectionInvokeAndWait;
import static megabasterd.MiscTools.swingReflectionInvokeAndWaitForReturn;
import static megabasterd.Transference.LIMIT_TRANSFERENCE_SPEED_DEFAULT;
import static megabasterd.Transference.MAX_TRANSFERENCE_SPEED_DEFAULT;


 /**
 *
 * @author tonikelope
 */
public final class MainPanel {
    
    public static final String VERSION="1.33";
    public static final int CONNECTION_TIMEOUT = 30000;
    public static final int THROTTLE_SLICE_SIZE=16*1024;
    public static final int STREAMER_PORT = 1337;
    public static final int WATCHDOG_PORT = 1338;
    public static final String ICON_FILE = "mbasterd_mini.png";
    public static final ExecutorService THREAD_POOL = newCachedThreadPool();
    public static final Font FONT_DEFAULT = createAndRegisterFont("Gochi.ttf");
    
    public static void main(String args[]) {
        
        setNimbusLookAndFeel();
        
        final MainPanel main_panel = new MainPanel();
        
        invokeLater(new Runnable() {
            @Override
            public void run() {
                main_panel.getView().setVisible(true);
            }
        });
    }

    private volatile MainPanelView _view=null; //lazy init
    private final GlobalSpeedMeter _global_dl_speed, _global_up_speed;
    private final DownloadManager _download_manager;
    private final UploadManager _upload_manager;
    private final StreamThrottlerSupervisor _stream_supervisor;
    private int _max_dl, _max_ul, _default_slots_down, _default_slots_up, _max_dl_speed, _max_up_speed;
    private boolean _use_slots_down, _use_slots_up, _limit_download_speed, _limit_upload_speed;
    private String _default_download_path;
    private HashMap<String, Object> _mega_accounts;
    private final HashMap<String, MegaAPI> _mega_active_accounts;
    private TrayIcon _trayicon;
    private final ClipboardSpy _clipboardspy;
    private KissVideoStreamServer _streamserver;
    private byte[] _mega_master_pass;
    private String _mega_master_pass_hash;
    private String _mega_master_pass_salt;
    
    public MainPanel() {
                
        if(checkAppIsRunning()) {
            
            System.exit(0);
        }
        
        try {
            
            trayIcon();
            
        } catch (AWTException ex) {
            getLogger(MainPanelView.class.getName()).log(SEVERE, null, ex);
        }
        
        try {
            
            setupSqliteTables();
            
        } catch (SQLException ex) {
            getLogger(MainPanel.class.getName()).log(SEVERE, null, ex);
        }
        
        loadUserSettings();
        
        _mega_master_pass = null;
        
        _mega_active_accounts = new HashMap<>();
        
        THREAD_POOL.execute((_global_dl_speed = new GlobalSpeedMeter(getView().getGlobal_speed_down_label())));
        
        THREAD_POOL.execute((_global_up_speed = new GlobalSpeedMeter(getView().getGlobal_speed_up_label())));
        
        THREAD_POOL.execute((_download_manager = new DownloadManager(this)));
        
        THREAD_POOL.execute((_upload_manager = new UploadManager(this)));
        
        THREAD_POOL.execute((_stream_supervisor = new StreamThrottlerSupervisor(_limit_download_speed?_max_dl_speed*1024:0, _limit_upload_speed?_max_up_speed*1024:0, THROTTLE_SLICE_SIZE)));
        
        THREAD_POOL.execute((_clipboardspy = new ClipboardSpy()));
        
        swingReflectionInvoke("setForeground", getView().getGlobal_speed_down_label(), _limit_download_speed?new Color(255,0,0):new Color(0,128,255));
         
        swingReflectionInvoke("setForeground", getView().getGlobal_speed_up_label(), _limit_upload_speed?new Color(255,0,0):new Color(0,128,255));
        
        resumeDownloads();
        
        resumeUploads();
        
        _streamserver = new KissVideoStreamServer(this);
        
        try {
            _streamserver.start(STREAMER_PORT, "/video");
        } catch (IOException ex) {
            getLogger(MainPanel.class.getName()).log(SEVERE, null, ex);
        }

        THREAD_POOL.execute(new Runnable(){
            @Override
            public void run() {
                
                String new_version = MiscTools.checkNewVersion("lYsRWaQB", "uVhntmyKcVECRaOxAbcL4A");
                
                if(new_version != null) {
                    
                    JOptionPane.showMessageDialog(getView(), "MegaBasterd NEW VERSION ("+new_version+") is available!\n\n(HELP > ABOUT for more info)");
                }
            }});
    }

    public TrayIcon getTrayicon() {
        return _trayicon;
    }
 
    public String getMega_master_pass_hash() {
        return _mega_master_pass_hash;
    }

    public void setMega_master_pass_hash(String mega_master_pass_hash) {
        _mega_master_pass_hash = mega_master_pass_hash;
    }

    public String getMega_master_pass_salt() {
        return _mega_master_pass_salt;
    }


    public byte[] getMega_master_pass() {
        return _mega_master_pass;
    }

    public void setMega_master_pass(byte[] pass) {
        
        if(_mega_master_pass != null) {

            Arrays.fill(_mega_master_pass, (byte)0);
            
            _mega_master_pass = null;
        }

        if(pass != null) {
            
            _mega_master_pass = new byte[pass.length];
            
            System.arraycopy(pass, 0, _mega_master_pass, 0, pass.length);
        }
    }
 
    public MainPanelView getView() {
        
        MainPanelView result = _view;
        
        if (result == null) {
            
            synchronized(this) {
                
                result = _view;
                
                if (result == null) {
                    
                    _view = result = new MainPanelView(this);
                    
                }
            }
        }
        
        return result;
    }

    public GlobalSpeedMeter getGlobal_dl_speed() {
        return _global_dl_speed;
    }

    public GlobalSpeedMeter getGlobal_up_speed() {
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

    public boolean isUse_slots_up() {
        return _use_slots_up;
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

    
    public void loadUserSettings()
    {
        String def_slots = selectSettingValueFromDB("default_slots_down");
        
        if(def_slots != null) {
            _default_slots_down = parseInt(def_slots);
        } else {
            _default_slots_down = Download.WORKERS_DEFAULT;
        }
        
        def_slots = selectSettingValueFromDB("default_slots_up");
        
        if(def_slots != null) {
            _default_slots_up = parseInt(def_slots);
        } else {
            _default_slots_up = Upload.WORKERS_DEFAULT;
        }
        
        String use_slots = selectSettingValueFromDB("use_slots_down");
        
        if(use_slots != null) {
            _use_slots_down = use_slots.equals("yes");
        } else {
            _use_slots_down = Download.USE_SLOTS_DEFAULT;
        }
        
        use_slots = selectSettingValueFromDB("use_slots_up");
        
        if(use_slots != null) {
            _use_slots_up = use_slots.equals("yes");
        } else {
            _use_slots_up = Upload.USE_SLOTS_DEFAULT;
        }

        String max_downloads = selectSettingValueFromDB("max_downloads");

        if(max_downloads != null) {
            _max_dl = parseInt(max_downloads);
        } else {
            _max_dl=Download.SIM_TRANSFERENCES_DEFAULT;
        }
        
        String max_uploads = selectSettingValueFromDB("max_uploads");

        if(max_uploads != null) {
            _max_ul = parseInt(max_uploads);
        } else {
            _max_ul=Upload.SIM_TRANSFERENCES_DEFAULT;
        }

        _default_download_path = selectSettingValueFromDB("default_down_dir");

        if(_default_download_path == null) {
            _default_download_path = ".";
        }
        
        String limit_dl_speed = selectSettingValueFromDB("limit_download_speed");

        if(limit_dl_speed != null) {
            
            _limit_download_speed = limit_dl_speed.equals("yes");
            
        } else {
            
            _limit_download_speed = LIMIT_TRANSFERENCE_SPEED_DEFAULT;
        }
       
        String limit_ul_speed = selectSettingValueFromDB("limit_upload_speed");

        if(limit_ul_speed != null) {
            
            _limit_upload_speed = limit_ul_speed.equals("yes");
            
        } else {
            
            _limit_upload_speed = LIMIT_TRANSFERENCE_SPEED_DEFAULT;
        }

        
        String max_download_speed = selectSettingValueFromDB("max_download_speed");

        if(max_download_speed != null) {
            _max_dl_speed = parseInt(max_download_speed);
        } else {
            _max_dl_speed=MAX_TRANSFERENCE_SPEED_DEFAULT;
        }

        String max_upload_speed = selectSettingValueFromDB("max_upload_speed");

        if(max_upload_speed != null) {
            _max_up_speed = parseInt(max_upload_speed);
        } else {
            _max_up_speed=MAX_TRANSFERENCE_SPEED_DEFAULT;
        }
        
        try {
            _mega_accounts = selectMegaAccounts();
        } catch (SQLException ex) {
            getLogger(MainPanel.class.getName()).log(SEVERE, null, ex);
        }
        
        _mega_master_pass_hash = DBTools.selectSettingValueFromDB("mega_master_pass_hash");
        
        _mega_master_pass_salt = DBTools.selectSettingValueFromDB("mega_master_pass_salt");
        
        if(_mega_master_pass_salt == null) {
            
            try {
                
                _mega_master_pass_salt = MiscTools.Bin2BASE64(MiscTools.genRandomByteArray(CryptTools.PBKDF2_SALT_BYTE_LENGTH));
                
                DBTools.insertSettingValueInDB("mega_master_pass_salt", _mega_master_pass_salt);
                
            } catch (Exception ex) {
                Logger.getLogger(MainPanel.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

    }
    
    public void _byebye() {
        
        boolean exit = true;
        
        if(!_streamserver.getWorking_threads().isEmpty()) {
            
            Object[] options = {"No",
                            "Yes"};
        
            int n = showOptionDialog(getView(),
            "It seems MegaBasterd is streaming video. Do you want to exit?",
            "Warning!", YES_NO_CANCEL_OPTION, QUESTION_MESSAGE,
            null,
            options,
            options[0]);
            
            if(n==0) {
                
                exit=false;
            }
            
        } else if(!getDownload_manager().getTransference_provision_queue().isEmpty() || !getUpload_manager().getTransference_provision_queue().isEmpty()) {
            
            Object[] options = {"No",
                            "Yes"};
        
            int n = showOptionDialog(getView(),
            "It seems MegaBasterd is provisioning down/uploads.\nIf you exit now, unprovisioned down/uploads will be lost.\nDo you want to continue?",
            "Warning!", YES_NO_CANCEL_OPTION, QUESTION_MESSAGE,
            null,
            options,
            options[0]);
            
            if(n==0) {
                
                exit=false;
            }
            
        }
        
        if(exit) {
            
            try {
                DBTools.vaccum();
            } catch (SQLException ex) {
                Logger.getLogger(MainPanel.class.getName()).log(Level.SEVERE, null, ex);
            }
           
            exit(0);
        }
    }
    
    private boolean checkAppIsRunning()
    {
        boolean app_is_running = false;
        
        try {
            
            final ServerSocket serverSocket = new ServerSocket(WATCHDOG_PORT, 0, InetAddress.getLoopbackAddress());

            THREAD_POOL.execute(new Runnable(){
            
                @Override
                public void run() {
                    
                    final ServerSocket socket = serverSocket;
                    
                    while(true) {
                        
                        try {
                            socket.accept();
                            
                            swingReflectionInvoke("setExtendedState", getView(), NORMAL);
                            
                            swingReflectionInvoke("setVisible", getView(), true);
                            
                        } catch (IOException ex) {
                            getLogger(MainPanel.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }

                    }});

        } catch (IOException ex) {
            
            app_is_running = true;
            
            try {
                
                Socket clientSocket = new Socket(InetAddress.getLoopbackAddress(), WATCHDOG_PORT);
                
                clientSocket.close();

            } catch (IOException ex1) {
                
                getLogger(MainPanel.class.getName()).log(Level.SEVERE, null, ex1);
            }
        }
        
        return app_is_running;
    }
    
    private void resumeDownloads() {
        
        swingReflectionInvoke("setText", getView().getStatus_down_label(), "Resuming previous downloads, please wait...");

        final MainPanel tthis =this;
        
        THREAD_POOL.execute(new Runnable(){
            
            @Override
            public void run() {

            int conta_downloads = 0;
            
            try {
            
                ArrayList<HashMap<String,Object>> res = selectDownloads();

                for(HashMap<String,Object> o:res) {
                    
                    Download download = new Download(tthis, (String)o.get("url"), (String)o.get("path"), (String)o.get("filename"), (String)o.get("filekey"), (Long)o.get("filesize"), (String)o.get("filepass"), (String)o.get("filenoexpire"), _use_slots_down, _default_slots_down, false);
                    
                    getDownload_manager().getTransference_provision_queue().add(download);
                    
                    conta_downloads++;
                }
                
            } catch (SQLException ex) {
                
                    getLogger(MainPanelView.class.getName()).log(SEVERE, null, ex);
            }
 
            if(conta_downloads>0) {
                
                getDownload_manager().secureNotify();

                getView().getjTabbedPane1().setSelectedIndex(0);

            }

            swingReflectionInvoke("setText", getView().getStatus_down_label(), "");
                                
                                
                                }});
        
    }
    
    public boolean trayIcon() throws AWTException {
        
        if (!java.awt.SystemTray.isSupported()) {
            
            return false;
        }

        PopupMenu menu = new PopupMenu();

        menu.setFont(FONT_DEFAULT.deriveFont(BOLD, 18));

        MenuItem messageItem = new MenuItem("Restore window");

        messageItem.addActionListener(new ActionListener() {

        @Override
        public void actionPerformed(ActionEvent e) {

            swingReflectionInvoke("setExtendedState", getView(), NORMAL);

            swingReflectionInvoke("setVisible", getView(), true);

          }
        });

        menu.add(messageItem);

        MenuItem closeItem = new MenuItem("EXIT");

        closeItem.addActionListener(new ActionListener() {

          @Override
          public void actionPerformed(ActionEvent e) {

            _byebye();

          }
          
        });

        menu.add(closeItem);

        ActionListener actionListener = new ActionListener() {

        @Override
        public void actionPerformed( ActionEvent e ) {

            if(!(boolean)swingReflectionInvokeAndWaitForReturn("isVisible", getView()))
            {   
                swingReflectionInvoke("setExtendedState", getView(), NORMAL);

                swingReflectionInvoke("setVisible", getView(), true);
            } 
            else
            {
                swingReflectionInvoke("dispatchEvent", getView(), new WindowEvent(getView(), WINDOW_CLOSING));
            }
        }

        };
        
        _trayicon = new TrayIcon(getDefaultToolkit().getImage(getClass().getResource(ICON_FILE)), "MegaBasterd", menu);

        _trayicon.setToolTip("MegaBasterd " + VERSION);

        _trayicon.setImageAutoSize(true);

        _trayicon.addActionListener(actionListener);

        getSystemTray().add(_trayicon);

        return true;
    
}
    
 
    private void resumeUploads() {
        
        swingReflectionInvoke("setText", getView().getStatus_up_label(), "Resuming previous uploads, please wait...");
        
        final MainPanel tthis =this;
        
        THREAD_POOL.execute(new Runnable(){
                                @Override
                                public void run() {
                                    
                                                try {
                                                    
                                                    int conta_uploads = 0;
                                                    
                                                    boolean remember_pass = true;
                                                    
                                                    ArrayList<HashMap<String,Object>> res = selectUploads();
     
                                                        for(HashMap<String,Object> o:res) {
                                                            
                                                            try{
                                                            
                                                            
                                                            String email = (String)o.get("email");
                                                            
                                                            MegaAPI ma;
                                                            
                                                            if(_mega_accounts.get(email) != null) {
                                                                
                                                                final HashMap<String,Object> account_info = (HashMap)_mega_accounts.get(email);
                                                                
                                                                ma = _mega_active_accounts.get(email);
                                                                
                                                                if(ma == null) {
                                                                    
                                                                    
                                                                        ma = new MegaAPI();
                                                                        
                                                                        String password_aes, user_hash;
                                                                        
                                                                            if(getMega_master_pass_hash() != null) {

                                                                                if(getMega_master_pass() == null) {
                                                                                    
                                                                                    getView().getjTabbedPane1().setSelectedIndex(1);
                                                                                    
                                                                                    GetMegaMasterPasswordDialog dialog = new GetMegaMasterPasswordDialog(getView(), true, getMega_master_pass_hash(), getMega_master_pass_salt());

                                                                                    swingReflectionInvokeAndWait("setLocationRelativeTo", dialog, getView());

                                                                                    swingReflectionInvokeAndWait("setVisible", dialog, true);

                                                                                    if(dialog.isPass_ok()) {

                                                                                        setMega_master_pass(dialog.getPass());

                                                                                        dialog.deletePass();
                                                                                        
                                                                                        remember_pass = dialog.getRemember_checkbox().isSelected();

                                                                                        dialog.dispose();

                                                                                        password_aes =Bin2BASE64(CryptTools.aes_cbc_decrypt_pkcs7(BASE642Bin((String)account_info.get("password_aes")), getMega_master_pass(), CryptTools.AES_ZERO_IV));

                                                                                        user_hash =Bin2BASE64(CryptTools.aes_cbc_decrypt_pkcs7(BASE642Bin((String)account_info.get("user_hash")), getMega_master_pass(), CryptTools.AES_ZERO_IV));

                                                                                    } else {

                                                                                        dialog.dispose();

                                                                                        throw new Exception();
                                                                                    }

                                                                                } else {

                                                                                    password_aes =Bin2BASE64(CryptTools.aes_cbc_decrypt_pkcs7(BASE642Bin((String)account_info.get("password_aes")), getMega_master_pass(), CryptTools.AES_ZERO_IV));

                                                                                    user_hash =Bin2BASE64(CryptTools.aes_cbc_decrypt_pkcs7(BASE642Bin((String)account_info.get("user_hash")), getMega_master_pass(), CryptTools.AES_ZERO_IV));

                                                                                }

                                                                            } else {

                                                                                password_aes = (String)account_info.get("password_aes");

                                                                                user_hash = (String)account_info.get("user_hash");
                                                                            }

                                                                            ma.fastLogin(email,bin2i32a(BASE642Bin(password_aes)), user_hash);

                                                                            _mega_active_accounts.put(email, ma);

                                                                            
                                                                }
                                                                
                                                                Upload upload = new Upload(tthis, ma, (String)o.get("filename"), (String)o.get("parent_node"), (String)o.get("ul_key")!=null?bin2i32a(BASE642Bin((String)o.get("ul_key"))):null, (String)o.get("url"), (String)o.get("root_node"), BASE642Bin((String)o.get("share_key")), (String)o.get("folder_link"), _use_slots_up, _default_slots_up, false);
                                                                
                                                                getUpload_manager().getTransference_provision_queue().add(upload);
                                                                
                                                                conta_uploads++;
                                                                
                                                            } else {
                                                                
                                                                deleteUpload((String)o.get("filename"), email);
                                                            }
                                                                
                                                            } catch (Exception ex) {
                                                                        getLogger(MainPanelView.class.getName()).log(SEVERE, null, ex);
                                                             }
                                                        }
                                                        
                                                    if(conta_uploads>0) {

                                                        getUpload_manager().secureNotify();
                                                        
                                                        getView().getjTabbedPane1().setSelectedIndex(1);
                                                        
                                                    }
                                                    
                                                    if(!remember_pass) {
                
                                                        setMega_master_pass(null);
                                                    }
                                                    
                                                    swingReflectionInvoke("setText", getView().getStatus_up_label(), "");
                                                    
                                                } catch (Exception ex) {
                                        getLogger(MainPanel.class.getName()).log(SEVERE, null, ex);
                                    }
                
                }});

    }
    
    
}
