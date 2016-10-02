package megabasterd;

import java.awt.Font;
import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import static java.util.logging.Logger.getLogger;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.table.DefaultTableModel;
import static megabasterd.DBTools.insertSettingValueInDB;
import static megabasterd.MainPanel.FONT_DEFAULT;
import static megabasterd.MainPanel.MEGA_API_KEY;
import static megabasterd.MainPanel.THREAD_POOL;
import static megabasterd.MainPanel.USER_AGENT;
import static megabasterd.MiscTools.Bin2BASE64;
import static megabasterd.MiscTools.i32a2bin;
import static megabasterd.MiscTools.swingReflectionInvoke;
import static megabasterd.MiscTools.swingReflectionInvokeAndWaitForReturn;
import static megabasterd.MiscTools.truncateText;
import static megabasterd.MiscTools.updateFont;


public final class SettingsDialog extends javax.swing.JDialog {
    
    private String _download_path;
    private boolean _settings_ok;
    private final Set<String> _deleted_accounts;
    private final MainPanel _main_panel;

    public boolean isSettings_ok() {
        return _settings_ok;
    }

    public Set<String> getDeleted_accounts() {
        return _deleted_accounts;
    }
    
    

    /**
     * Creates new form Settings
     * @param parent
     * @param modal
     */
    public SettingsDialog(javax.swing.JFrame parent, boolean modal) {
        super(parent, modal);
        initComponents();
 
        updateFont(change_download_dir_button, FONT_DEFAULT, Font.PLAIN);
        updateFont(down_dir_label, FONT_DEFAULT, Font.PLAIN);
        updateFont(ok_button, FONT_DEFAULT, Font.PLAIN);
        updateFont(cancel_button, FONT_DEFAULT, Font.PLAIN);
        updateFont(default_slots_down_label, FONT_DEFAULT, Font.PLAIN);
        updateFont(default_slots_down_spinner, FONT_DEFAULT, Font.PLAIN);
        updateFont(default_slots_up, FONT_DEFAULT, Font.PLAIN);
        updateFont(max_downloads_label, FONT_DEFAULT, Font.PLAIN);
        updateFont(max_downloads_spinner, FONT_DEFAULT, Font.PLAIN);
        updateFont(max_uploads_spinner, FONT_DEFAULT, Font.PLAIN);
        updateFont(verify_file_down_checkbox, FONT_DEFAULT, Font.PLAIN);
        updateFont(multi_slot_down_checkbox, FONT_DEFAULT, Font.PLAIN);
        updateFont(multi_slot_up_checkbox, FONT_DEFAULT, Font.PLAIN);
        updateFont(jTabbedPane1, FONT_DEFAULT, Font.PLAIN);
        updateFont(status, FONT_DEFAULT, Font.PLAIN);
        updateFont(remove_account_button, FONT_DEFAULT, Font.PLAIN);
        updateFont(add_account_button, FONT_DEFAULT, Font.PLAIN);
        updateFont(accounts_label, FONT_DEFAULT, Font.PLAIN);
        updateFont(defaut_slots_up_label, FONT_DEFAULT, Font.PLAIN);
        updateFont(max_uploads_label, FONT_DEFAULT, Font.PLAIN);
        
        updateFont(max_down_speed_label, FONT_DEFAULT, Font.PLAIN);
        updateFont(limit_download_speed_checkbox, FONT_DEFAULT, Font.PLAIN);
        updateFont(max_down_speed_spinner, FONT_DEFAULT, Font.PLAIN);
        
        updateFont(max_up_speed_spinner, FONT_DEFAULT, Font.PLAIN);
        updateFont(max_up_speed_label, FONT_DEFAULT, Font.PLAIN);
        updateFont(limit_upload_speed_checkbox, FONT_DEFAULT, Font.PLAIN);
        
        updateFont(default_dir_label, FONT_DEFAULT, Font.PLAIN);
        
        _main_panel = ((MainPanelView)parent).getMain_panel();
            
            String default_download_dir = DBTools.selectSettingValueFromDB("default_down_dir");
            
            if(default_download_dir == null) {
                default_download_dir = ".";
            }
            
            _download_path = default_download_dir;
            
            swingReflectionInvoke("setText", default_dir_label, truncateText(_download_path, 80));
            
            String slots = DBTools.selectSettingValueFromDB("default_slots_down");
            
            int default_slots=Download.WORKERS_DEFAULT;
            
            if(slots != null) {
                default_slots = Integer.parseInt(slots);
            }
            
            default_slots_down_spinner.setModel(new SpinnerNumberModel(default_slots, Download.MIN_WORKERS, Download.MAX_WORKERS, 1));
            ((JSpinner.DefaultEditor)default_slots_down_spinner.getEditor()).getTextField().setEditable(false);
            
            
            slots = DBTools.selectSettingValueFromDB("default_slots_up");
            
            default_slots=Upload.WORKERS_DEFAULT;
            
            if(slots != null) {
                default_slots = Integer.parseInt(slots);
            }
            
            default_slots_up.setModel(new SpinnerNumberModel(default_slots, Upload.MIN_WORKERS, Upload.MAX_WORKERS, 1));
            ((JSpinner.DefaultEditor)default_slots_up.getEditor()).getTextField().setEditable(false);
            
            String max_down = DBTools.selectSettingValueFromDB("max_downloads");
            
            int max_dl = Download.SIM_TRANSFERENCES_DEFAULT;
            
            if(max_down != null) {
                max_dl = Integer.parseInt(max_down);
            } 
            
            
            max_downloads_spinner.setModel(new SpinnerNumberModel(max_dl, 1, Download.MAX_SIM_TRANSFERENCES, 1));
            ((JSpinner.DefaultEditor)max_downloads_spinner.getEditor()).getTextField().setEditable(false);
            
            
            String max_up = DBTools.selectSettingValueFromDB("max_uploads");
            
            int max_ul = Upload.SIM_TRANSFERENCES_DEFAULT;
            
            if(max_up != null) {
                max_ul = Integer.parseInt(max_up);
            } 
            
            
            max_uploads_spinner.setModel(new SpinnerNumberModel(max_ul, 1, Upload.MAX_SIM_TRANSFERENCES, 1));
            ((JSpinner.DefaultEditor)max_uploads_spinner.getEditor()).getTextField().setEditable(false);
            
            
            
            boolean limit_dl_speed = Download.LIMIT_TRANSFERENCE_SPEED_DEFAULT;
            
            String limit_download_speed = DBTools.selectSettingValueFromDB("limit_download_speed");
            
            if(limit_download_speed != null) {
                limit_dl_speed = limit_download_speed.equals("yes");
            } 
            
            limit_download_speed_checkbox.setSelected(limit_dl_speed);
            
            if(!limit_dl_speed) {
                
                swingReflectionInvoke("setEnabled", max_down_speed_label, false);
                swingReflectionInvoke("setEnabled", max_down_speed_spinner, false);
            } else {
                swingReflectionInvoke("setEnabled", max_down_speed_label, true);
                swingReflectionInvoke("setEnabled", max_down_speed_spinner, true);
            }
            
            String max_dl_speed = DBTools.selectSettingValueFromDB("max_download_speed");
            
            int max_download_speed = Download.MAX_TRANSFERENCE_SPEED_DEFAULT;
            
            if(max_dl_speed != null) {
                max_download_speed = Integer.parseInt(max_dl_speed);
            } 
            
            
            max_down_speed_spinner.setModel(new SpinnerNumberModel(max_download_speed, 0, Integer.MAX_VALUE, 1));
            ((JSpinner.DefaultEditor)max_down_speed_spinner.getEditor()).getTextField().setEditable(true);
            
            
            boolean limit_ul_speed = Upload.LIMIT_TRANSFERENCE_SPEED_DEFAULT;
            
            String limit_upload_speed = DBTools.selectSettingValueFromDB("limit_upload_speed");
            
            if(limit_upload_speed != null) {
                limit_ul_speed = limit_upload_speed.equals("yes");
            } 
            
            limit_upload_speed_checkbox.setSelected(limit_ul_speed);
            
            if(!limit_ul_speed) {
                
                swingReflectionInvoke("setEnabled", max_up_speed_label, false);
                swingReflectionInvoke("setEnabled", max_up_speed_spinner, false);
            } else {
                swingReflectionInvoke("setEnabled", max_up_speed_label, true);
                swingReflectionInvoke("setEnabled", max_up_speed_spinner, true);
            }
            
            String max_ul_speed = DBTools.selectSettingValueFromDB("max_upload_speed");
            
            int max_upload_speed = Upload.MAX_TRANSFERENCE_SPEED_DEFAULT;
            
            if(max_ul_speed != null) {
                max_upload_speed = Integer.parseInt(max_ul_speed);
            }
           
            max_up_speed_spinner.setModel(new SpinnerNumberModel(max_upload_speed, 0, Integer.MAX_VALUE, 1));
            
            ((JSpinner.DefaultEditor)max_up_speed_spinner.getEditor()).getTextField().setEditable(true);
            
            
            boolean cbc_mac = Download.VERIFY_CBC_MAC_DEFAULT;
            
            String verify_file = DBTools.selectSettingValueFromDB("verify_down_file");
            
            if(verify_file != null) {
                cbc_mac = (verify_file.equals("yes"));
            } 
            
            verify_file_down_checkbox.setSelected(cbc_mac);
            
            boolean use_slots = Download.USE_SLOTS_DEFAULT;
            
            String use_slots_val = DBTools.selectSettingValueFromDB("use_slots_down");
            
            if(use_slots_val != null) {
                use_slots = use_slots_val.equals("yes");
            } 
            
            multi_slot_down_checkbox.setSelected(use_slots);
            
            if(!use_slots) {
                
                swingReflectionInvoke("setEnabled", default_slots_down_label, false);
                swingReflectionInvoke("setEnabled", default_slots_down_spinner, false);
            } else {
                swingReflectionInvoke("setEnabled", default_slots_down_label, true);
                swingReflectionInvoke("setEnabled", default_slots_down_spinner, true);
            }
            
            
            use_slots = Upload.USE_SLOTS_DEFAULT;
            
            use_slots_val = DBTools.selectSettingValueFromDB("use_slots_up");
            
            if(use_slots_val != null) {
                use_slots = use_slots_val.equals("yes");
            } 
            
            multi_slot_up_checkbox.setSelected(use_slots);
            
            if(!use_slots) {
                
                swingReflectionInvoke("setEnabled", defaut_slots_up_label, false);
                swingReflectionInvoke("setEnabled", default_slots_up, false);
            } else {
                swingReflectionInvoke("setEnabled", max_uploads_label, true);
                swingReflectionInvoke("setEnabled", default_slots_up, true);
            }

            DefaultTableModel model = (DefaultTableModel)jTable1.getModel();
            
            for (HashMap.Entry pair : _main_panel.getMega_accounts().entrySet()) {
            
                HashMap<String,Object> data = (HashMap)pair.getValue();

                String[] new_row_data = {(String)pair.getKey(), (String)data.get("password")};
                
                model.addRow(new_row_data);
            }
 
            swingReflectionInvoke("setEnabled", remove_account_button, (jTable1.getRowCount()>0));
            
            _deleted_accounts = new HashSet();
            
            _settings_ok = false;
  
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        ok_button = new javax.swing.JButton();
        cancel_button = new javax.swing.JButton();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        downloads_panel = new javax.swing.JPanel();
        default_slots_down_spinner = new javax.swing.JSpinner();
        max_downloads_label = new javax.swing.JLabel();
        max_downloads_spinner = new javax.swing.JSpinner();
        verify_file_down_checkbox = new javax.swing.JCheckBox();
        down_dir_label = new javax.swing.JLabel();
        change_download_dir_button = new javax.swing.JButton();
        default_slots_down_label = new javax.swing.JLabel();
        multi_slot_down_checkbox = new javax.swing.JCheckBox();
        limit_download_speed_checkbox = new javax.swing.JCheckBox();
        max_down_speed_label = new javax.swing.JLabel();
        max_down_speed_spinner = new javax.swing.JSpinner();
        default_dir_label = new javax.swing.JLabel();
        uploads_panel = new javax.swing.JPanel();
        accounts_scrollpane = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        accounts_label = new javax.swing.JLabel();
        remove_account_button = new javax.swing.JButton();
        add_account_button = new javax.swing.JButton();
        defaut_slots_up_label = new javax.swing.JLabel();
        max_uploads_label = new javax.swing.JLabel();
        default_slots_up = new javax.swing.JSpinner();
        max_uploads_spinner = new javax.swing.JSpinner();
        multi_slot_up_checkbox = new javax.swing.JCheckBox();
        max_up_speed_label = new javax.swing.JLabel();
        max_up_speed_spinner = new javax.swing.JSpinner();
        limit_upload_speed_checkbox = new javax.swing.JCheckBox();
        status = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Settings");

        ok_button.setFont(new java.awt.Font("Dialog", 1, 22)); // NOI18N
        ok_button.setText("OK");
        ok_button.setDoubleBuffered(true);
        ok_button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ok_buttonActionPerformed(evt);
            }
        });

        cancel_button.setFont(new java.awt.Font("Dialog", 1, 22)); // NOI18N
        cancel_button.setText("CANCEL");
        cancel_button.setDoubleBuffered(true);
        cancel_button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancel_buttonActionPerformed(evt);
            }
        });

        jTabbedPane1.setDoubleBuffered(true);
        jTabbedPane1.setFont(new java.awt.Font("Dialog", 1, 22)); // NOI18N

        default_slots_down_spinner.setFont(new java.awt.Font("Dialog", 1, 20)); // NOI18N
        default_slots_down_spinner.setDoubleBuffered(true);
        default_slots_down_spinner.setValue(2);

        max_downloads_label.setFont(new java.awt.Font("Dialog", 1, 20)); // NOI18N
        max_downloads_label.setText("Max sim downloads:");
        max_downloads_label.setDoubleBuffered(true);

        max_downloads_spinner.setFont(new java.awt.Font("Dialog", 1, 20)); // NOI18N
        max_downloads_spinner.setDoubleBuffered(true);

        verify_file_down_checkbox.setFont(new java.awt.Font("Dialog", 1, 20)); // NOI18N
        verify_file_down_checkbox.setText("Verify file integrity (when download is finished)");
        verify_file_down_checkbox.setDoubleBuffered(true);

        down_dir_label.setFont(new java.awt.Font("Dialog", 1, 20)); // NOI18N
        down_dir_label.setText("Default downloads directory:");
        down_dir_label.setDoubleBuffered(true);

        change_download_dir_button.setFont(new java.awt.Font("Dialog", 1, 20)); // NOI18N
        change_download_dir_button.setText("Change it");
        change_download_dir_button.setDoubleBuffered(true);
        change_download_dir_button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                change_download_dir_buttonActionPerformed(evt);
            }
        });

        default_slots_down_label.setFont(new java.awt.Font("Dialog", 1, 20)); // NOI18N
        default_slots_down_label.setText("Default slots per file:");
        default_slots_down_label.setDoubleBuffered(true);

        multi_slot_down_checkbox.setFont(new java.awt.Font("Dialog", 1, 20)); // NOI18N
        multi_slot_down_checkbox.setText("Use multi slot download mode (NOT recommended; download restart needed)");
        multi_slot_down_checkbox.setDoubleBuffered(true);
        multi_slot_down_checkbox.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                multi_slot_down_checkboxStateChanged(evt);
            }
        });

        limit_download_speed_checkbox.setFont(new java.awt.Font("Dialog", 1, 20)); // NOI18N
        limit_download_speed_checkbox.setText("Limit download speed");
        limit_download_speed_checkbox.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                limit_download_speed_checkboxStateChanged(evt);
            }
        });

        max_down_speed_label.setFont(new java.awt.Font("Dialog", 1, 20)); // NOI18N
        max_down_speed_label.setText("Max speed (KB/s):");

        max_down_speed_spinner.setFont(new java.awt.Font("Dialog", 1, 20)); // NOI18N

        default_dir_label.setFont(new java.awt.Font("Dialog", 1, 20)); // NOI18N

        javax.swing.GroupLayout downloads_panelLayout = new javax.swing.GroupLayout(downloads_panel);
        downloads_panel.setLayout(downloads_panelLayout);
        downloads_panelLayout.setHorizontalGroup(
            downloads_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(downloads_panelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(downloads_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(downloads_panelLayout.createSequentialGroup()
                        .addComponent(change_download_dir_button)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(default_dir_label, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(downloads_panelLayout.createSequentialGroup()
                        .addGroup(downloads_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(verify_file_down_checkbox)
                            .addGroup(downloads_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                .addGroup(javax.swing.GroupLayout.Alignment.LEADING, downloads_panelLayout.createSequentialGroup()
                                    .addComponent(max_down_speed_label)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(max_down_speed_spinner, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGroup(downloads_panelLayout.createSequentialGroup()
                                    .addComponent(max_downloads_label)
                                    .addGap(79, 79, 79)
                                    .addComponent(max_downloads_spinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGroup(downloads_panelLayout.createSequentialGroup()
                                    .addComponent(default_slots_down_label)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(default_slots_down_spinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                            .addComponent(limit_download_speed_checkbox)
                            .addComponent(multi_slot_down_checkbox)
                            .addComponent(down_dir_label))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        downloads_panelLayout.setVerticalGroup(
            downloads_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(downloads_panelLayout.createSequentialGroup()
                .addGap(14, 14, 14)
                .addComponent(down_dir_label)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(downloads_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(change_download_dir_button)
                    .addComponent(default_dir_label))
                .addGap(18, 18, 18)
                .addGroup(downloads_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(max_downloads_label)
                    .addComponent(max_downloads_spinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addComponent(multi_slot_down_checkbox)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(downloads_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(default_slots_down_label)
                    .addComponent(default_slots_down_spinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addComponent(limit_download_speed_checkbox)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(downloads_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(max_down_speed_label)
                    .addComponent(max_down_speed_spinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addComponent(verify_file_down_checkbox)
                .addContainerGap(221, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("Downloads", downloads_panel);

        jTable1.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Email", "Password"
            }
        ));
        jTable1.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        jTable1.setDoubleBuffered(true);
        jTable1.setRowHeight(24);
        accounts_scrollpane.setViewportView(jTable1);

        accounts_label.setFont(new java.awt.Font("Dialog", 1, 20)); // NOI18N
        accounts_label.setText("Your MEGA accounts:");
        accounts_label.setDoubleBuffered(true);

        remove_account_button.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        remove_account_button.setText("Remove selected");
        remove_account_button.setDoubleBuffered(true);
        remove_account_button.setEnabled(false);
        remove_account_button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                remove_account_buttonActionPerformed(evt);
            }
        });

        add_account_button.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        add_account_button.setText("Add account");
        add_account_button.setDoubleBuffered(true);
        add_account_button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                add_account_buttonActionPerformed(evt);
            }
        });

        defaut_slots_up_label.setFont(new java.awt.Font("Dialog", 1, 20)); // NOI18N
        defaut_slots_up_label.setText("Default slots per file:");
        defaut_slots_up_label.setDoubleBuffered(true);

        max_uploads_label.setFont(new java.awt.Font("Dialog", 1, 20)); // NOI18N
        max_uploads_label.setText("Max sim uploads:");
        max_uploads_label.setDoubleBuffered(true);

        default_slots_up.setFont(new java.awt.Font("Dialog", 1, 20)); // NOI18N
        default_slots_up.setDoubleBuffered(true);
        default_slots_up.setValue(2);

        max_uploads_spinner.setFont(new java.awt.Font("Dialog", 1, 20)); // NOI18N
        max_uploads_spinner.setDoubleBuffered(true);

        multi_slot_up_checkbox.setFont(new java.awt.Font("Dialog", 1, 20)); // NOI18N
        multi_slot_up_checkbox.setText("Use multi slot upload mode (RECOMMENDED; Upload restart needed)");
        multi_slot_up_checkbox.setDoubleBuffered(true);
        multi_slot_up_checkbox.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                multi_slot_up_checkboxStateChanged(evt);
            }
        });

        max_up_speed_label.setFont(new java.awt.Font("Dialog", 1, 20)); // NOI18N
        max_up_speed_label.setText("Max speed (KB/s):");

        max_up_speed_spinner.setFont(new java.awt.Font("Dialog", 1, 20)); // NOI18N

        limit_upload_speed_checkbox.setFont(new java.awt.Font("Dialog", 1, 20)); // NOI18N
        limit_upload_speed_checkbox.setText("Limit upload speed");
        limit_upload_speed_checkbox.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                limit_upload_speed_checkboxStateChanged(evt);
            }
        });

        javax.swing.GroupLayout uploads_panelLayout = new javax.swing.GroupLayout(uploads_panel);
        uploads_panel.setLayout(uploads_panelLayout);
        uploads_panelLayout.setHorizontalGroup(
            uploads_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(uploads_panelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(uploads_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(accounts_scrollpane, javax.swing.GroupLayout.DEFAULT_SIZE, 924, Short.MAX_VALUE)
                    .addGroup(uploads_panelLayout.createSequentialGroup()
                        .addGroup(uploads_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(accounts_label)
                            .addGroup(uploads_panelLayout.createSequentialGroup()
                                .addGroup(uploads_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                    .addComponent(defaut_slots_up_label)
                                    .addGroup(uploads_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(max_up_speed_label)
                                        .addComponent(limit_upload_speed_checkbox)))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(uploads_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                    .addComponent(default_slots_up, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(max_up_speed_spinner, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)))
                            .addGroup(uploads_panelLayout.createSequentialGroup()
                                .addComponent(max_uploads_label)
                                .addGap(123, 123, 123)
                                .addComponent(max_uploads_spinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(multi_slot_up_checkbox))
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(uploads_panelLayout.createSequentialGroup()
                        .addComponent(remove_account_button)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(add_account_button)))
                .addContainerGap())
        );
        uploads_panelLayout.setVerticalGroup(
            uploads_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(uploads_panelLayout.createSequentialGroup()
                .addGap(6, 6, 6)
                .addComponent(accounts_label)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(accounts_scrollpane, javax.swing.GroupLayout.PREFERRED_SIZE, 250, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(uploads_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(remove_account_button)
                    .addComponent(add_account_button))
                .addGap(18, 18, 18)
                .addGroup(uploads_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(max_uploads_label)
                    .addComponent(max_uploads_spinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addComponent(multi_slot_up_checkbox)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(uploads_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(defaut_slots_up_label)
                    .addComponent(default_slots_up, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addComponent(limit_upload_speed_checkbox)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(uploads_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(max_up_speed_label)
                    .addComponent(max_up_speed_spinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(25, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("Uploads", uploads_panel);

        status.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        status.setDoubleBuffered(true);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jTabbedPane1)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(status, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(ok_button, javax.swing.GroupLayout.PREFERRED_SIZE, 130, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(cancel_button, javax.swing.GroupLayout.PREFERRED_SIZE, 130, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jTabbedPane1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(ok_button)
                            .addComponent(cancel_button))
                        .addContainerGap())
                    .addGroup(layout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(status)
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void change_download_dir_buttonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_change_download_dir_buttonActionPerformed
        
        
        javax.swing.JFileChooser filechooser = new javax.swing.JFileChooser();
                 
        filechooser.setCurrentDirectory(new java.io.File(_download_path));
        filechooser.setDialogTitle("Default download directory");
        filechooser.setFileSelectionMode(javax.swing.JFileChooser.DIRECTORIES_ONLY);
        filechooser.setAcceptAllFileFilterUsed(false);
        
        if( filechooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION ) {
            
            File file = filechooser.getSelectedFile();
            
            _download_path = file.getAbsolutePath();
            
            swingReflectionInvoke("setText", default_dir_label, truncateText(_download_path, 80));
            
        }
    }//GEN-LAST:event_change_download_dir_buttonActionPerformed

    private void cancel_buttonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancel_buttonActionPerformed
        
        
        swingReflectionInvoke("setVisible", this, false);
    }//GEN-LAST:event_cancel_buttonActionPerformed

    private void ok_buttonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ok_buttonActionPerformed
        
        try {
            
            
            _settings_ok = true;
     
            insertSettingValueInDB("default_down_dir", _download_path);
            insertSettingValueInDB("default_slots_down", String.valueOf((int)swingReflectionInvokeAndWaitForReturn("getValue", default_slots_down_spinner)));
            insertSettingValueInDB("default_slots_up", String.valueOf((int)swingReflectionInvokeAndWaitForReturn("getValue", default_slots_up)));
            insertSettingValueInDB("use_slots_down", (boolean)swingReflectionInvokeAndWaitForReturn("isSelected", multi_slot_down_checkbox)?"yes":"no");
            insertSettingValueInDB("use_slots_up", (boolean)swingReflectionInvokeAndWaitForReturn("isSelected", multi_slot_up_checkbox)?"yes":"no");
            insertSettingValueInDB("max_downloads", String.valueOf((int)swingReflectionInvokeAndWaitForReturn("getValue", max_downloads_spinner)));
            insertSettingValueInDB("max_uploads", String.valueOf((int)swingReflectionInvokeAndWaitForReturn("getValue", max_uploads_spinner)));
            insertSettingValueInDB("verify_down_file", (boolean)swingReflectionInvokeAndWaitForReturn("isSelected", verify_file_down_checkbox)?"yes":"no");
            insertSettingValueInDB("limit_download_speed", (boolean)swingReflectionInvokeAndWaitForReturn("isSelected", limit_download_speed_checkbox)?"yes":"no");
            insertSettingValueInDB("max_download_speed", String.valueOf((int)swingReflectionInvokeAndWaitForReturn("getValue", max_down_speed_spinner)));
            insertSettingValueInDB("limit_upload_speed", (boolean)swingReflectionInvokeAndWaitForReturn("isSelected", limit_upload_speed_checkbox)?"yes":"no");
            insertSettingValueInDB("max_upload_speed", String.valueOf((int)swingReflectionInvokeAndWaitForReturn("getValue", max_up_speed_spinner)));

            
            final DefaultTableModel model = (DefaultTableModel)jTable1.getModel();
            
            swingReflectionInvoke("setText", status, "Checking your MEGA accounts, please wait...");
            
            swingReflectionInvoke("setEnabled", ok_button, false);
            
            swingReflectionInvoke("setEnabled", cancel_button, false);
            
            swingReflectionInvoke("setEnabled", remove_account_button, false);
            
            swingReflectionInvoke("setEnabled", add_account_button, false);
            
            swingReflectionInvoke("setEnabled", jTable1, false);
            
            final SettingsDialog dialog = this;
            
            THREAD_POOL.execute(new Runnable(){
                @Override
                public void run() {
                    
                    ArrayList<String> email_error = new ArrayList<>();
                    
                    for(int i=0; i<model.getRowCount(); i++) {
                        
                        String email = (String)model.getValueAt(i, 0);
                        
                        String pass = (String)model.getValueAt(i, 1);
                        
                        if(!email.isEmpty() && !pass.isEmpty()) {
                            
                            MegaAPI ma;
                            
                            if(dialog._main_panel.getMega_accounts().get(email) == null){
                                
                                ma = new MegaAPI(MEGA_API_KEY, USER_AGENT);
                                
                                try {
                                    ma.login(email, pass);
                                    
                                    dialog._main_panel.getMega_active_accounts().put(email, ma);
                                    
                                    DBTools.insertMegaAccount(email, pass, Bin2BASE64(i32a2bin(ma.getPassword_aes())), ma.getUser_hash());
                                    
                                } catch(Exception ex) {
                                    
                                    email_error.add(email);
                                    getLogger(SettingsDialog.class.getName()).log(Level.SEVERE, null, ex);
                                }
                                
                            } else {
                                
                                HashMap<String,Object> mega_account_data = (HashMap)dialog._main_panel.getMega_accounts().get(email);
                                
                                if(!mega_account_data.get("password").equals(pass)) {
                                    
                                    ma = new MegaAPI(MEGA_API_KEY, USER_AGENT);
                                    
                                    try {
                                        ma.login(email, pass);

                                        dialog._main_panel.getMega_active_accounts().put(email, ma);

                                        DBTools.insertMegaAccount(email, pass, Bin2BASE64(i32a2bin(ma.getPassword_aes())), ma.getUser_hash());
                                        
                                    } catch (Exception ex) {
                                        
                                        email_error.add(email);
                                        getLogger(SettingsDialog.class.getName()).log(Level.SEVERE, null, ex);
                                        
                                    }
                                }
                            }
                        }
                    }
                    
                    if(email_error.size() > 0) {
                        
                        String email_error_s = "";
                        
                        for(String s:email_error) {
                            
                            email_error_s+=s+"\n";
                        }
                        
                        swingReflectionInvoke("setText", dialog.status, "");
                        
                        JOptionPane.showMessageDialog(dialog, "There were errors with some accounts. Please, check them:\n\n"+email_error_s);
                        
                        swingReflectionInvoke("setEnabled", dialog.ok_button, true);
                        
                        swingReflectionInvoke("setEnabled", dialog.cancel_button, true);
                        
                        swingReflectionInvoke("setEnabled", dialog.remove_account_button, true);
                        
                        swingReflectionInvoke("setEnabled", dialog.add_account_button, true);
                        
                        swingReflectionInvoke("setEnabled", dialog.jTable1, true);
                        
                    } else {
                        swingReflectionInvoke("setVisible", dialog, false);
                    }

                }
            });
        } catch (SQLException ex) {
            getLogger(SettingsDialog.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_ok_buttonActionPerformed

    private void multi_slot_down_checkboxStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_multi_slot_down_checkboxStateChanged
        
        
        if(!(boolean)swingReflectionInvokeAndWaitForReturn("isSelected", multi_slot_down_checkbox)) {
                
                swingReflectionInvoke("setEnabled", default_slots_down_spinner, false);
                swingReflectionInvoke("setEnabled", default_slots_down_label, false);
        } else {
                swingReflectionInvoke("setEnabled", default_slots_down_spinner, true);
                swingReflectionInvoke("setEnabled", default_slots_down_label, true);
       }
    }//GEN-LAST:event_multi_slot_down_checkboxStateChanged

    private void remove_account_buttonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_remove_account_buttonActionPerformed
        

        DefaultTableModel model = (DefaultTableModel)jTable1.getModel();

        int selected = jTable1.getSelectedRow();

        while(selected >= 0) {

            String email = (String)model.getValueAt(jTable1.convertRowIndexToModel(selected),0);

            _deleted_accounts.add(email);

            model.removeRow(jTable1.convertRowIndexToModel(selected));

            selected = jTable1.getSelectedRow();
        }

        jTable1.clearSelection();

        if(model.getRowCount() == 0) {

            swingReflectionInvoke("setEnabled", remove_account_button, false);
        }
    }//GEN-LAST:event_remove_account_buttonActionPerformed

    private void add_account_buttonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_add_account_buttonActionPerformed
        

        DefaultTableModel model = (DefaultTableModel)jTable1.getModel();

        model.addRow(new Object[]{"",""});

        jTable1.clearSelection();

        swingReflectionInvoke("setEnabled", ok_button, true);
    }//GEN-LAST:event_add_account_buttonActionPerformed

    private void multi_slot_up_checkboxStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_multi_slot_up_checkboxStateChanged
        
        
        if(!(boolean)swingReflectionInvokeAndWaitForReturn("isSelected", multi_slot_up_checkbox)) {
                
                swingReflectionInvoke("setEnabled", defaut_slots_up_label, false);
                swingReflectionInvoke("setEnabled", default_slots_up, false);
        } else {
                swingReflectionInvoke("setEnabled", defaut_slots_up_label, true);
                swingReflectionInvoke("setEnabled", default_slots_up, true);
       }
    }//GEN-LAST:event_multi_slot_up_checkboxStateChanged

    private void limit_download_speed_checkboxStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_limit_download_speed_checkboxStateChanged
        
        
        if(!(boolean)swingReflectionInvokeAndWaitForReturn("isSelected", limit_download_speed_checkbox)) {
                
                swingReflectionInvoke("setEnabled", max_down_speed_label, false);
                swingReflectionInvoke("setEnabled", max_down_speed_spinner, false);
        } else {
                swingReflectionInvoke("setEnabled", max_down_speed_label, true);
                swingReflectionInvoke("setEnabled", max_down_speed_spinner, true);
       }
    }//GEN-LAST:event_limit_download_speed_checkboxStateChanged

    private void limit_upload_speed_checkboxStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_limit_upload_speed_checkboxStateChanged
        
        
        if(!(boolean)swingReflectionInvokeAndWaitForReturn("isSelected", limit_upload_speed_checkbox)) {
                
                swingReflectionInvoke("setEnabled", max_up_speed_label, false);
                swingReflectionInvoke("setEnabled", max_up_speed_spinner, false);
        } else {
                swingReflectionInvoke("setEnabled", max_up_speed_label, true);
                swingReflectionInvoke("setEnabled", max_up_speed_spinner, true);
       }
    }//GEN-LAST:event_limit_upload_speed_checkboxStateChanged

   
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel accounts_label;
    private javax.swing.JScrollPane accounts_scrollpane;
    private javax.swing.JButton add_account_button;
    private javax.swing.JButton cancel_button;
    private javax.swing.JButton change_download_dir_button;
    private javax.swing.JLabel default_dir_label;
    private javax.swing.JLabel default_slots_down_label;
    private javax.swing.JSpinner default_slots_down_spinner;
    private javax.swing.JSpinner default_slots_up;
    private javax.swing.JLabel defaut_slots_up_label;
    private javax.swing.JLabel down_dir_label;
    private javax.swing.JPanel downloads_panel;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JTable jTable1;
    private javax.swing.JCheckBox limit_download_speed_checkbox;
    private javax.swing.JCheckBox limit_upload_speed_checkbox;
    private javax.swing.JLabel max_down_speed_label;
    private javax.swing.JSpinner max_down_speed_spinner;
    private javax.swing.JLabel max_downloads_label;
    private javax.swing.JSpinner max_downloads_spinner;
    private javax.swing.JLabel max_up_speed_label;
    private javax.swing.JSpinner max_up_speed_spinner;
    private javax.swing.JLabel max_uploads_label;
    private javax.swing.JSpinner max_uploads_spinner;
    private javax.swing.JCheckBox multi_slot_down_checkbox;
    private javax.swing.JCheckBox multi_slot_up_checkbox;
    private javax.swing.JButton ok_button;
    private javax.swing.JButton remove_account_button;
    private javax.swing.JLabel status;
    private javax.swing.JPanel uploads_panel;
    private javax.swing.JCheckBox verify_file_down_checkbox;
    // End of variables declaration//GEN-END:variables
}
