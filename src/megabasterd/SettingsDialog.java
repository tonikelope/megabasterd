package megabasterd;

import java.awt.Font;
import java.awt.Frame;
import java.io.File;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import static java.util.logging.Logger.getLogger;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import static javax.swing.JOptionPane.YES_NO_CANCEL_OPTION;
import static javax.swing.JOptionPane.showOptionDialog;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.table.DefaultTableModel;
import static megabasterd.DBTools.insertSettingValueInDB;
import static megabasterd.MainPanel.FONT_DEFAULT;
import static megabasterd.MainPanel.THREAD_POOL;
import static megabasterd.MiscTools.BASE642Bin;
import static megabasterd.MiscTools.Bin2BASE64;
import static megabasterd.MiscTools.i32a2bin;
import static megabasterd.MiscTools.swingReflectionInvoke;
import static megabasterd.MiscTools.swingReflectionInvokeAndWait;
import static megabasterd.MiscTools.swingReflectionInvokeAndWaitForReturn;
import static megabasterd.MiscTools.truncateText;
import static megabasterd.MiscTools.updateFont;


public final class SettingsDialog extends javax.swing.JDialog {
    
    private String _download_path;
    private boolean _settings_ok;
    private final Set<String> _deleted_accounts;
    private final MainPanel _main_panel;
    private boolean _remember_master_pass;

    public boolean isSettings_ok() {
        return _settings_ok;
    }

    public Set<String> getDeleted_accounts() {
        return _deleted_accounts;
    }

    public boolean isRemember_master_pass() {
        return _remember_master_pass;
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
        updateFont(status, FONT_DEFAULT, Font.BOLD);
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
        updateFont(encrypt_pass_checkbox, FONT_DEFAULT, Font.PLAIN);
        updateFont(unlock_accounts_button, FONT_DEFAULT, Font.PLAIN);
        updateFont(delete_all_accounts_button, FONT_DEFAULT, Font.PLAIN);
        
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
            
            encrypt_pass_checkbox.setSelected((_main_panel.getMega_master_pass_hash() != null));

            swingReflectionInvoke("setEnabled", remove_account_button, (mega_accounts_table.getRowCount()>0));
            
            DefaultTableModel model = (DefaultTableModel)mega_accounts_table.getModel();
            
            if(_main_panel.getMega_master_pass_hash() != null) {
                
                if(_main_panel.getMega_master_pass() == null) {
                    
                    swingReflectionInvoke("setEnabled", encrypt_pass_checkbox, false);

                    swingReflectionInvoke("setEnabled", remove_account_button, false);
                    
                    swingReflectionInvoke("setEnabled", add_account_button, false);

                    swingReflectionInvoke("setVisible", unlock_accounts_button, true);
                    
                    for (Object k: _main_panel.getMega_accounts().keySet()) {
   
                        String[] new_row_data = {(String)k, "**************************"};

                        model.addRow(new_row_data);
                    }
                    
                    swingReflectionInvoke("setEnabled", mega_accounts_table, false);
                    
                    swingReflectionInvoke("setEnabled", remove_account_button, false);
                
                } else {
                    
                    swingReflectionInvoke("setVisible", unlock_accounts_button, false);
                   
                    for (HashMap.Entry pair : _main_panel.getMega_accounts().entrySet()) {

                    HashMap<String,Object> data = (HashMap)pair.getValue();
                    
                        String pass = null;
                    
                        try {

                            pass = new String(CryptTools.aes_cbc_decrypt_pkcs7(BASE642Bin((String)data.get("password")), _main_panel.getMega_master_pass(), CryptTools.AES_ZERO_IV));

                        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException ex) {
                            Logger.getLogger(SettingsDialog.class.getName()).log(Level.SEVERE, null, ex);
                        } catch (Exception ex) {
                            Logger.getLogger(SettingsDialog.class.getName()).log(Level.SEVERE, null, ex);
                        }

                        String[] new_row_data = {(String)pair.getKey(), pass};

                        model.addRow(new_row_data);
                    }
                    
                    swingReflectionInvoke("setEnabled", remove_account_button, (mega_accounts_table.getRowCount()>0));
                }
 
            } else {
                
                swingReflectionInvoke("setVisible", unlock_accounts_button, false);
              
                for (HashMap.Entry pair : _main_panel.getMega_accounts().entrySet()) {
            
                    HashMap<String,Object> data = (HashMap)pair.getValue();

                    String[] new_row_data = {(String)pair.getKey(), (String)data.get("password")};

                    model.addRow(new_row_data);
                }
                
                swingReflectionInvoke("setEnabled", remove_account_button, (mega_accounts_table.getRowCount()>0));
            }
            
            _remember_master_pass = true;

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
        mega_accounts_table = new javax.swing.JTable();
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
        encrypt_pass_checkbox = new javax.swing.JCheckBox();
        unlock_accounts_button = new javax.swing.JButton();
        delete_all_accounts_button = new javax.swing.JButton();
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
                .addContainerGap(225, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("Downloads", downloads_panel);

        mega_accounts_table.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        mega_accounts_table.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Email", "Password"
            }
        ));
        mega_accounts_table.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        mega_accounts_table.setDoubleBuffered(true);
        mega_accounts_table.setRowHeight(24);
        accounts_scrollpane.setViewportView(mega_accounts_table);

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

        encrypt_pass_checkbox.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        encrypt_pass_checkbox.setText("Encrypt passwords (on disk)");
        encrypt_pass_checkbox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                encrypt_pass_checkboxActionPerformed(evt);
            }
        });

        unlock_accounts_button.setBackground(new java.awt.Color(0, 153, 51));
        unlock_accounts_button.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        unlock_accounts_button.setForeground(new java.awt.Color(255, 255, 255));
        unlock_accounts_button.setText("Unlock");
        unlock_accounts_button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                unlock_accounts_buttonActionPerformed(evt);
            }
        });

        delete_all_accounts_button.setBackground(new java.awt.Color(255, 51, 0));
        delete_all_accounts_button.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        delete_all_accounts_button.setForeground(new java.awt.Color(255, 255, 255));
        delete_all_accounts_button.setText("RESET ACCOUNTS");
        delete_all_accounts_button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                delete_all_accounts_buttonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout uploads_panelLayout = new javax.swing.GroupLayout(uploads_panel);
        uploads_panel.setLayout(uploads_panelLayout);
        uploads_panelLayout.setHorizontalGroup(
            uploads_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(uploads_panelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(uploads_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(accounts_scrollpane)
                    .addGroup(uploads_panelLayout.createSequentialGroup()
                        .addGroup(uploads_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
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
                        .addGap(0, 110, Short.MAX_VALUE))
                    .addGroup(uploads_panelLayout.createSequentialGroup()
                        .addComponent(remove_account_button)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(add_account_button))
                    .addGroup(uploads_panelLayout.createSequentialGroup()
                        .addComponent(accounts_label)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(unlock_accounts_button)
                        .addGap(18, 18, 18)
                        .addComponent(delete_all_accounts_button)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(encrypt_pass_checkbox)))
                .addContainerGap())
        );
        uploads_panelLayout.setVerticalGroup(
            uploads_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(uploads_panelLayout.createSequentialGroup()
                .addGap(6, 6, 6)
                .addGroup(uploads_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(accounts_label)
                    .addComponent(encrypt_pass_checkbox)
                    .addComponent(unlock_accounts_button)
                    .addComponent(delete_all_accounts_button))
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
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("Uploads", uploads_panel);

        status.setFont(new java.awt.Font("Dialog", 1, 20)); // NOI18N
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
                        .addComponent(status, javax.swing.GroupLayout.PREFERRED_SIZE, 657, javax.swing.GroupLayout.PREFERRED_SIZE)
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
                .addComponent(jTabbedPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 598, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(ok_button)
                    .addComponent(cancel_button)
                    .addComponent(status))
                .addContainerGap())
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

            
            if(mega_accounts_table.isEnabled()) {
                
                final DefaultTableModel model = (DefaultTableModel)mega_accounts_table.getModel();
            
            swingReflectionInvoke("setText", status, "Checking your MEGA accounts, please wait...");
            
            swingReflectionInvoke("setEnabled", ok_button, false);
            
            swingReflectionInvoke("setEnabled", cancel_button, false);
            
            swingReflectionInvoke("setEnabled", remove_account_button, false);
            
            swingReflectionInvoke("setEnabled", add_account_button, false);
            
            swingReflectionInvoke("setEnabled", delete_all_accounts_button, false);
            
            swingReflectionInvoke("setEnabled", mega_accounts_table, false);
            
            swingReflectionInvoke("setEnabled", encrypt_pass_checkbox, false);
            
            
            final SettingsDialog tthis = this;
            
            THREAD_POOL.execute(new Runnable(){
                @Override
                public void run() {
                    
                    ArrayList<String> email_error = new ArrayList<>();
                    
                    for(int i=0; i<model.getRowCount(); i++) {
                        
                        String email = (String)model.getValueAt(i, 0);
                        
                        String pass = (String)model.getValueAt(i, 1);
                        
                        if(!email.isEmpty() && !pass.isEmpty()) {
                            
                            MegaAPI ma;
                            
                            if(_main_panel.getMega_accounts().get(email) == null){
                                
                                ma = new MegaAPI();
                                
                                try {
                                    ma.login(email, pass);
                                    
                                    _main_panel.getMega_active_accounts().put(email, ma);
          
                                    String password=pass, password_aes=Bin2BASE64(i32a2bin(ma.getPassword_aes())), user_hash=ma.getUser_hash();

                                    if(_main_panel.getMega_master_pass_hash() != null) {

                                        password =Bin2BASE64(CryptTools.aes_cbc_encrypt_pkcs7(pass.getBytes(), _main_panel.getMega_master_pass(), CryptTools.AES_ZERO_IV));

                                        password_aes =Bin2BASE64(CryptTools.aes_cbc_encrypt_pkcs7(i32a2bin(ma.getPassword_aes()), _main_panel.getMega_master_pass(), CryptTools.AES_ZERO_IV));

                                        user_hash =Bin2BASE64(CryptTools.aes_cbc_encrypt_pkcs7(BASE642Bin(ma.getUser_hash()), _main_panel.getMega_master_pass(), CryptTools.AES_ZERO_IV));
                                    }
                                    
                                    DBTools.insertMegaAccount(email, password, password_aes, user_hash);
                                    
                                } catch(Exception ex) {
                                    
                                    email_error.add(email);
                                    getLogger(SettingsDialog.class.getName()).log(Level.SEVERE, null, ex);
                                }
                                
                            } else {
                                
                                HashMap<String,Object> mega_account_data = (HashMap)_main_panel.getMega_accounts().get(email);
                                
                                String password;
                                
                                password = (String)mega_account_data.get("password");
                                
                                if(_main_panel.getMega_master_pass() != null) {
                                    
                                    try {
                                        
                                        password = new String(CryptTools.aes_cbc_decrypt_pkcs7(BASE642Bin(password), _main_panel.getMega_master_pass(), CryptTools.AES_ZERO_IV));
           
                                    } catch (Exception ex) {
                                        Logger.getLogger(SettingsDialog.class.getName()).log(Level.SEVERE, null, ex);
                                    }
                                }

                                if(!password.equals(pass)) {
                                    
                                    ma = new MegaAPI();
                                    
                                    try {
                                        ma.login(email, pass);

                                        _main_panel.getMega_active_accounts().put(email, ma);
                                        
                                        password = pass;
                                        
                                        String password_aes=Bin2BASE64(i32a2bin(ma.getPassword_aes())), user_hash=ma.getUser_hash();
                                        
                                        if(_main_panel.getMega_master_pass() != null) {
                                            
                                            password =Bin2BASE64(CryptTools.aes_cbc_encrypt_pkcs7(pass.getBytes(), _main_panel.getMega_master_pass(), CryptTools.AES_ZERO_IV));
                                                    
                                            password_aes = Bin2BASE64(CryptTools.aes_cbc_encrypt_pkcs7(i32a2bin(ma.getPassword_aes()), _main_panel.getMega_master_pass(), CryptTools.AES_ZERO_IV));
                                            
                                            user_hash = Bin2BASE64(CryptTools.aes_cbc_encrypt_pkcs7(BASE642Bin(ma.getUser_hash()), _main_panel.getMega_master_pass(), CryptTools.AES_ZERO_IV));
                                        }

                                        DBTools.insertMegaAccount(email, password, password_aes, user_hash);
                                        
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
                        
                        swingReflectionInvoke("setText", status, "");
                        
                        JOptionPane.showMessageDialog(tthis, "There were errors with some accounts. Please, check them:\n\n"+email_error_s, "Error", JOptionPane.ERROR_MESSAGE);
                        
                        swingReflectionInvoke("setEnabled", ok_button, true);
                        
                        swingReflectionInvoke("setEnabled", cancel_button, true);
                        
                        swingReflectionInvoke("setEnabled", remove_account_button, true);
                        
                        swingReflectionInvoke("setEnabled", add_account_button, true);
                        
                        swingReflectionInvoke("setEnabled", mega_accounts_table, true);
                        
                        swingReflectionInvoke("setEnabled", delete_all_accounts_button, true);
                        
                        swingReflectionInvoke("setEnabled", encrypt_pass_checkbox, true);
                        
                    } else {
                        swingReflectionInvoke("setVisible", tthis, false);
                    }
                   
                }
            });
                
            } else {
                swingReflectionInvoke("setVisible", this, false);
            }
            
            
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
       
        DefaultTableModel model = (DefaultTableModel)mega_accounts_table.getModel();

        int selected = mega_accounts_table.getSelectedRow();

        while(selected >= 0) {

            String email = (String)model.getValueAt(mega_accounts_table.convertRowIndexToModel(selected),0);

            _deleted_accounts.add(email);

            model.removeRow(mega_accounts_table.convertRowIndexToModel(selected));

            selected = mega_accounts_table.getSelectedRow();
        }

        mega_accounts_table.clearSelection();

        if(model.getRowCount() == 0) {

            swingReflectionInvoke("setEnabled", remove_account_button, false);
        }
    }//GEN-LAST:event_remove_account_buttonActionPerformed

    private void add_account_buttonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_add_account_buttonActionPerformed
        
        DefaultTableModel model = (DefaultTableModel)mega_accounts_table.getModel();

        model.addRow(new Object[]{"",""});

        mega_accounts_table.clearSelection();

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

    private void encrypt_pass_checkboxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_encrypt_pass_checkboxActionPerformed
        
        swingReflectionInvoke("setEnabled", encrypt_pass_checkbox, false);
        
        final SettingsDialog tthis = this;
        
        THREAD_POOL.execute(new Runnable(){
                @Override
                public void run() {
                
                    SetMegaMasterPasswordDialog dialog = new SetMegaMasterPasswordDialog((Frame)getParent(),true, _main_panel.getMega_master_pass_salt());
        
                    swingReflectionInvokeAndWait("setLocationRelativeTo", dialog, tthis);

                    swingReflectionInvokeAndWait("setVisible", dialog, true);

                    byte[] old_mega_master_pass = _main_panel.getMega_master_pass();

                    String old_mega_master_pass_hash = _main_panel.getMega_master_pass_hash();

                    if(dialog.isPass_ok()) {

                        try {

                            if(dialog.getNew_pass() != null && dialog.getNew_pass().length > 0) {

                                _main_panel.setMega_master_pass_hash(dialog.getNew_pass_hash());

                                _main_panel.setMega_master_pass(dialog.getNew_pass());

                            } else {

                                _main_panel.setMega_master_pass_hash(null);

                                _main_panel.setMega_master_pass(null);
                            }

                            dialog.deletePass();

                            insertSettingValueInDB("mega_master_pass_hash", _main_panel.getMega_master_pass_hash());

                            for (HashMap.Entry pair : _main_panel.getMega_accounts().entrySet()) {

                                HashMap<String,Object> data = (HashMap)pair.getValue();

                                String email, password, password_aes, user_hash;

                                email = (String)pair.getKey();

                                if(old_mega_master_pass_hash != null) {

                                    password = new String(CryptTools.aes_cbc_decrypt_pkcs7(BASE642Bin((String)data.get("password")) , old_mega_master_pass, CryptTools.AES_ZERO_IV));

                                    password_aes =Bin2BASE64(CryptTools.aes_cbc_decrypt_pkcs7(BASE642Bin((String)data.get("password_aes")), old_mega_master_pass, CryptTools.AES_ZERO_IV));

                                    user_hash =Bin2BASE64(CryptTools.aes_cbc_decrypt_pkcs7(BASE642Bin((String)data.get("user_hash")), old_mega_master_pass, CryptTools.AES_ZERO_IV));

                                } else {

                                    password = (String)data.get("password");

                                    password_aes = (String)data.get("password_aes");

                                    user_hash = (String)data.get("user_hash");
                                }

                                if(_main_panel.getMega_master_pass() != null) {

                                    password =Bin2BASE64(CryptTools.aes_cbc_encrypt_pkcs7(password.getBytes(), _main_panel.getMega_master_pass(), CryptTools.AES_ZERO_IV));

                                    password_aes =Bin2BASE64(CryptTools.aes_cbc_encrypt_pkcs7(BASE642Bin(password_aes), _main_panel.getMega_master_pass(), CryptTools.AES_ZERO_IV));

                                    user_hash =Bin2BASE64(CryptTools.aes_cbc_encrypt_pkcs7(BASE642Bin(user_hash), _main_panel.getMega_master_pass(), CryptTools.AES_ZERO_IV));
                                }

                                data.put("password", password);

                                data.put("password_aes", password_aes);

                                data.put("user_hash", user_hash);

                                DBTools.insertMegaAccount(email, password, password_aes, user_hash);
                            }

                        } catch (Exception ex) {
                            Logger.getLogger(SettingsDialog.class.getName()).log(Level.SEVERE, null, ex);
                        }

                    }

                    encrypt_pass_checkbox.setSelected((_main_panel.getMega_master_pass_hash() != null));

                    dialog.dispose();
                
                    swingReflectionInvoke("setEnabled", encrypt_pass_checkbox, true);
                    
                }});
  
    }//GEN-LAST:event_encrypt_pass_checkboxActionPerformed

    private void unlock_accounts_buttonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_unlock_accounts_buttonActionPerformed
        
        swingReflectionInvoke("setEnabled", unlock_accounts_button, false);
        
        final SettingsDialog tthis = this;
        
        THREAD_POOL.execute(new Runnable(){
                @Override
                public void run() {
                
                    GetMegaMasterPasswordDialog dialog = new GetMegaMasterPasswordDialog((Frame)getParent(),true, _main_panel.getMega_master_pass_hash(), _main_panel.getMega_master_pass_salt());
        
                    swingReflectionInvokeAndWait("setLocationRelativeTo", dialog, tthis);

                    swingReflectionInvokeAndWait("setVisible", dialog, true);

                    if(dialog.isPass_ok()) {

                        _main_panel.setMega_master_pass(dialog.getPass());

                        dialog.deletePass();

                        DefaultTableModel model = new DefaultTableModel(new Object [][] {},new String [] {"Email", "Password"});

                        mega_accounts_table.setModel(model);

                        swingReflectionInvoke("setEnabled", encrypt_pass_checkbox, true);

                        swingReflectionInvoke("setEnabled", mega_accounts_table, true);

                        swingReflectionInvoke("setEnabled", remove_account_button, true);

                        swingReflectionInvoke("setEnabled", add_account_button, true);

                        swingReflectionInvoke("setVisible", unlock_accounts_button, false);

                        swingReflectionInvoke("setEnabled", delete_all_accounts_button, true);

                        for (HashMap.Entry pair : _main_panel.getMega_accounts().entrySet()) {

                        HashMap<String,Object> data = (HashMap)pair.getValue();

                        String pass = null;

                        try {

                            pass = new String(CryptTools.aes_cbc_decrypt_pkcs7(BASE642Bin((String)data.get("password")), _main_panel.getMega_master_pass(), CryptTools.AES_ZERO_IV));

                        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException ex) {
                            Logger.getLogger(SettingsDialog.class.getName()).log(Level.SEVERE, null, ex);
                        } catch (Exception ex) {
                            Logger.getLogger(SettingsDialog.class.getName()).log(Level.SEVERE, null, ex);
                        }

                        String[] new_row_data = {(String)pair.getKey(), pass};

                        model.addRow(new_row_data);
                        }

                    }

                    _remember_master_pass = dialog.getRemember_checkbox().isSelected();

                    dialog.dispose();
                    
                    swingReflectionInvoke("setEnabled", unlock_accounts_button, true);
                
                }});
  
    }//GEN-LAST:event_unlock_accounts_buttonActionPerformed

    private void delete_all_accounts_buttonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_delete_all_accounts_buttonActionPerformed
        
        swingReflectionInvoke("setEnabled", delete_all_accounts_button, false);
        
        final SettingsDialog tthis = this;
        
        THREAD_POOL.execute(new Runnable(){
                @Override
                public void run() {
                
                    Object[] options = {"No",
                            "Yes"};
        
            int n = showOptionDialog(tthis,
            "MEGA master password will be reset and all your MEGA accounts will be removed. (This can't be undone)\n\nDo you want to continue?",
            "Warning!", YES_NO_CANCEL_OPTION, javax.swing.JOptionPane.WARNING_MESSAGE,
            null,
            options,
            options[0]);
        
        if(n == 1) {
            
            try {
                swingReflectionInvoke("setEnabled", encrypt_pass_checkbox, true);
                
                swingReflectionInvoke("setEnabled", mega_accounts_table, true);
                
                swingReflectionInvoke("setEnabled", remove_account_button, true);
                
                swingReflectionInvoke("setEnabled", add_account_button, true);
                
                swingReflectionInvoke("setVisible", unlock_accounts_button, false);
                
                swingReflectionInvoke("setVisible", delete_all_accounts_button, true);

                DefaultTableModel new_model = new DefaultTableModel(new Object [][] {},new String [] {"Email", "Password"});
                
                mega_accounts_table.setModel(new_model);

                for (HashMap.Entry pair : _main_panel.getMega_accounts().entrySet()) {
                    
                    try {
                        DBTools.deleteMegaAccount((String) pair.getKey());
                    } catch (SQLException ex) {
                        Logger.getLogger(SettingsDialog.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                
                _main_panel.setMega_master_pass_hash(null);
                
                _main_panel.setMega_master_pass(null);
                
                insertSettingValueInDB("mega_master_pass_hash", null);
                
                encrypt_pass_checkbox.setSelected(false);
                
                _main_panel.getMega_accounts().clear();
                
                _main_panel.getMega_active_accounts().clear();
                
            } catch (SQLException ex) {
                Logger.getLogger(SettingsDialog.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        swingReflectionInvoke("setEnabled", delete_all_accounts_button, true);
                
                
        }});

    }//GEN-LAST:event_delete_all_accounts_buttonActionPerformed

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
    private javax.swing.JButton delete_all_accounts_button;
    private javax.swing.JLabel down_dir_label;
    private javax.swing.JPanel downloads_panel;
    private javax.swing.JCheckBox encrypt_pass_checkbox;
    private javax.swing.JTabbedPane jTabbedPane1;
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
    private javax.swing.JTable mega_accounts_table;
    private javax.swing.JCheckBox multi_slot_down_checkbox;
    private javax.swing.JCheckBox multi_slot_up_checkbox;
    private javax.swing.JButton ok_button;
    private javax.swing.JButton remove_account_button;
    private javax.swing.JLabel status;
    private javax.swing.JButton unlock_accounts_button;
    private javax.swing.JPanel uploads_panel;
    private javax.swing.JCheckBox verify_file_down_checkbox;
    // End of variables declaration//GEN-END:variables
}
