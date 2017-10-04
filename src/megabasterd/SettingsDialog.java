package megabasterd;

import java.awt.Dialog;
import java.awt.Frame;
import java.io.File;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
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
import static megabasterd.MainPanel.THREAD_POOL;
import static megabasterd.MiscTools.BASE642Bin;
import static megabasterd.MiscTools.Bin2BASE64;
import static megabasterd.MiscTools.i32a2bin;
import static megabasterd.MiscTools.swingReflectionInvoke;
import static megabasterd.MiscTools.swingReflectionInvokeAndWait;
import static megabasterd.MiscTools.swingReflectionInvokeAndWaitForReturn;
import static megabasterd.MiscTools.truncateText;

public final class SettingsDialog extends javax.swing.JDialog {

    private String _download_path;
    private boolean _settings_ok;
    private final Set<String> _deleted_mega_accounts;
    private final Set<String> _deleted_elc_accounts;
    private final MainPanel _main_panel;
    private boolean _remember_master_pass;

    public boolean isSettings_ok() {
        return _settings_ok;
    }

    public Set<String> getDeleted_mega_accounts() {
        return _deleted_mega_accounts;
    }

    public Set<String> getDeleted_elc_accounts() {
        return _deleted_elc_accounts;
    }

    public boolean isRemember_master_pass() {
        return _remember_master_pass;
    }

    /**
     * Creates new form Settings
     *
     * @param parent
     * @param modal
     */
    public SettingsDialog(javax.swing.JFrame parent, boolean modal) {
        super(parent, modal);
        initComponents();

        _main_panel = ((MainPanelView) parent).getMain_panel();

        String default_download_dir = DBTools.selectSettingValueFromDB("default_down_dir");

        if (default_download_dir == null) {
            default_download_dir = ".";
        }

        _download_path = default_download_dir;

        swingReflectionInvoke("setText", default_dir_label, truncateText(_download_path, 80));

        String slots = DBTools.selectSettingValueFromDB("default_slots_down");

        int default_slots = Download.WORKERS_DEFAULT;

        if (slots != null) {
            default_slots = Integer.parseInt(slots);
        }

        swingReflectionInvokeAndWait("setModel", default_slots_down_spinner, new SpinnerNumberModel(default_slots, Download.MIN_WORKERS, Download.MAX_WORKERS, 1));

        swingReflectionInvoke("setEditable", ((JSpinner.DefaultEditor) default_slots_down_spinner.getEditor()).getTextField(), false);

        slots = DBTools.selectSettingValueFromDB("default_slots_up");

        default_slots = Upload.WORKERS_DEFAULT;

        if (slots != null) {
            default_slots = Integer.parseInt(slots);
        }

        swingReflectionInvokeAndWait("setModel", default_slots_up_spinner, new SpinnerNumberModel(default_slots, Upload.MIN_WORKERS, Upload.MAX_WORKERS, 1));

        swingReflectionInvoke("setEditable", ((JSpinner.DefaultEditor) default_slots_up_spinner.getEditor()).getTextField(), false);

        String max_down = DBTools.selectSettingValueFromDB("max_downloads");

        int max_dl = Download.SIM_TRANSFERENCES_DEFAULT;

        if (max_down != null) {
            max_dl = Integer.parseInt(max_down);
        }

        swingReflectionInvokeAndWait("setModel", max_downloads_spinner, new SpinnerNumberModel(max_dl, 1, Download.MAX_SIM_TRANSFERENCES, 1));
        swingReflectionInvoke("setEditable", ((JSpinner.DefaultEditor) max_downloads_spinner.getEditor()).getTextField(), false);

        String max_up = DBTools.selectSettingValueFromDB("max_uploads");

        int max_ul = Upload.SIM_TRANSFERENCES_DEFAULT;

        if (max_up != null) {
            max_ul = Integer.parseInt(max_up);
        }

        swingReflectionInvokeAndWait("setModel", max_uploads_spinner, new SpinnerNumberModel(max_ul, 1, Upload.MAX_SIM_TRANSFERENCES, 1));
        swingReflectionInvoke("setEditable", ((JSpinner.DefaultEditor) max_uploads_spinner.getEditor()).getTextField(), false);

        boolean limit_dl_speed = Download.LIMIT_TRANSFERENCE_SPEED_DEFAULT;

        String limit_download_speed = DBTools.selectSettingValueFromDB("limit_download_speed");

        if (limit_download_speed != null) {
            limit_dl_speed = limit_download_speed.equals("yes");
        }

        limit_download_speed_checkbox.setSelected(limit_dl_speed);

        if (!limit_dl_speed) {

            swingReflectionInvoke("setEnabled", max_down_speed_label, false);
            swingReflectionInvoke("setEnabled", max_down_speed_spinner, false);
        } else {
            swingReflectionInvoke("setEnabled", max_down_speed_label, true);
            swingReflectionInvoke("setEnabled", max_down_speed_spinner, true);
        }

        String max_dl_speed = DBTools.selectSettingValueFromDB("max_download_speed");

        int max_download_speed = Download.MAX_TRANSFERENCE_SPEED_DEFAULT;

        if (max_dl_speed != null) {
            max_download_speed = Integer.parseInt(max_dl_speed);
        }

        swingReflectionInvokeAndWait("setModel", max_down_speed_spinner, new SpinnerNumberModel(max_download_speed, 1, Integer.MAX_VALUE, 5));

        swingReflectionInvoke("setEditable", ((JSpinner.DefaultEditor) max_down_speed_spinner.getEditor()).getTextField(), true);

        boolean limit_ul_speed = Upload.LIMIT_TRANSFERENCE_SPEED_DEFAULT;

        String limit_upload_speed = DBTools.selectSettingValueFromDB("limit_upload_speed");

        if (limit_upload_speed != null) {
            limit_ul_speed = limit_upload_speed.equals("yes");
        }

        limit_upload_speed_checkbox.setSelected(limit_ul_speed);

        if (!limit_ul_speed) {

            swingReflectionInvoke("setEnabled", max_up_speed_label, false);
            swingReflectionInvoke("setEnabled", max_up_speed_spinner, false);
        } else {
            swingReflectionInvoke("setEnabled", max_up_speed_label, true);
            swingReflectionInvoke("setEnabled", max_up_speed_spinner, true);
        }

        String max_ul_speed = DBTools.selectSettingValueFromDB("max_upload_speed");

        int max_upload_speed = Upload.MAX_TRANSFERENCE_SPEED_DEFAULT;

        if (max_ul_speed != null) {
            max_upload_speed = Integer.parseInt(max_ul_speed);
        }

        swingReflectionInvokeAndWait("setModel", max_up_speed_spinner, new SpinnerNumberModel(max_upload_speed, 1, Integer.MAX_VALUE, 5));

        swingReflectionInvoke("setEditable", ((JSpinner.DefaultEditor) max_up_speed_spinner.getEditor()).getTextField(), true);

        boolean cbc_mac = Download.VERIFY_CBC_MAC_DEFAULT;

        String verify_file = DBTools.selectSettingValueFromDB("verify_down_file");

        if (verify_file != null) {
            cbc_mac = (verify_file.equals("yes"));
        }

        verify_file_down_checkbox.setSelected(cbc_mac);

        boolean use_slots = Download.USE_SLOTS_DEFAULT;

        String use_slots_val = DBTools.selectSettingValueFromDB("use_slots_down");

        if (use_slots_val != null) {
            use_slots = use_slots_val.equals("yes");
        }

        multi_slot_down_checkbox.setSelected(use_slots);

        if (!use_slots) {

            swingReflectionInvoke("setEnabled", default_slots_down_label, false);
            swingReflectionInvoke("setEnabled", default_slots_down_spinner, false);
        } else {
            swingReflectionInvoke("setEnabled", default_slots_down_label, true);
            swingReflectionInvoke("setEnabled", default_slots_down_spinner, true);
        }

        use_slots = Upload.USE_SLOTS_DEFAULT;

        use_slots_val = DBTools.selectSettingValueFromDB("use_slots_up");

        if (use_slots_val != null) {
            use_slots = use_slots_val.equals("yes");
        }

        multi_slot_up_checkbox.setSelected(use_slots);

        if (!use_slots) {

            swingReflectionInvoke("setEnabled", default_slots_up_label, false);
            swingReflectionInvoke("setEnabled", default_slots_up_spinner, false);
        } else {
            swingReflectionInvoke("setEnabled", max_uploads_label, true);
            swingReflectionInvoke("setEnabled", default_slots_up_spinner, true);
        }

        boolean use_mega_account = Download.USE_MEGA_ACCOUNT_DOWN;

        String use_mega_acc = DBTools.selectSettingValueFromDB("use_mega_account_down");

        String mega_account = null;

        if (use_mega_acc != null) {

            use_mega_account = use_mega_acc.equals("yes");

            mega_account = DBTools.selectSettingValueFromDB("mega_account_down");
        }

        if (use_mega_account) {

            swingReflectionInvoke("setSelected", use_mega_account_down_checkbox, true);

        } else {

            swingReflectionInvoke("setSelected", use_mega_account_down_checkbox, false);
            swingReflectionInvoke("setEnabled", use_mega_account_down_combobox, false);
            swingReflectionInvoke("setEnabled", use_mega_label, false);
        }

        DefaultTableModel mega_model = (DefaultTableModel) swingReflectionInvokeAndWaitForReturn("getModel", mega_accounts_table);

        DefaultTableModel elc_model = (DefaultTableModel) swingReflectionInvokeAndWaitForReturn("getModel", elc_accounts_table);

        swingReflectionInvoke("setSelected", encrypt_pass_checkbox, (_main_panel.getMaster_pass_hash() != null));

        swingReflectionInvoke("setEnabled", remove_mega_account_button, (mega_model.getRowCount() > 0));

        swingReflectionInvoke("setEnabled", remove_elc_account_button, (elc_model.getRowCount() > 0));

        if (_main_panel.getMaster_pass_hash() != null) {

            if (_main_panel.getMaster_pass() == null) {

                swingReflectionInvoke("setEnabled", encrypt_pass_checkbox, false);

                swingReflectionInvoke("setEnabled", remove_mega_account_button, false);

                swingReflectionInvoke("setEnabled", remove_elc_account_button, false);

                swingReflectionInvoke("setEnabled", add_mega_account_button, false);

                swingReflectionInvoke("setEnabled", add_elc_account_button, false);

                swingReflectionInvoke("setVisible", unlock_accounts_button, true);

                for (Object k : _main_panel.getMega_accounts().keySet()) {

                    String[] new_row_data = {(String) k, "**************************"};

                    swingReflectionInvoke("addRow", mega_model, new Object[]{new_row_data});
                }

                for (Object k : _main_panel.getElc_accounts().keySet()) {

                    String[] new_row_data = {(String) k, "**************************", "**************************"};

                    swingReflectionInvoke("addRow", elc_model, new Object[]{new_row_data});
                }

                swingReflectionInvoke("setEnabled", mega_accounts_table, false);

                swingReflectionInvoke("setEnabled", elc_accounts_table, false);

            } else {

                swingReflectionInvoke("setVisible", unlock_accounts_button, false);

                for (Map.Entry pair : _main_panel.getMega_accounts().entrySet()) {

                    HashMap<String, Object> data = (HashMap) pair.getValue();

                    String pass = null;

                    try {

                        pass = new String(CryptTools.aes_cbc_decrypt_pkcs7(BASE642Bin((String) data.get("password")), _main_panel.getMaster_pass(), CryptTools.AES_ZERO_IV));

                    } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException ex) {
                        Logger.getLogger(SettingsDialog.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (Exception ex) {
                        Logger.getLogger(SettingsDialog.class.getName()).log(Level.SEVERE, null, ex);
                    }

                    String[] new_row_data = {(String) pair.getKey(), pass};

                    swingReflectionInvokeAndWait("addRow", mega_model, new Object[]{new_row_data});
                }

                for (Map.Entry pair : _main_panel.getElc_accounts().entrySet()) {

                    HashMap<String, Object> data = (HashMap) pair.getValue();

                    String user = null, apikey = null;

                    try {

                        user = new String(CryptTools.aes_cbc_decrypt_pkcs7(BASE642Bin((String) data.get("user")), _main_panel.getMaster_pass(), CryptTools.AES_ZERO_IV));

                        apikey = new String(CryptTools.aes_cbc_decrypt_pkcs7(BASE642Bin((String) data.get("apikey")), _main_panel.getMaster_pass(), CryptTools.AES_ZERO_IV));

                    } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException ex) {
                        Logger.getLogger(SettingsDialog.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (Exception ex) {
                        Logger.getLogger(SettingsDialog.class.getName()).log(Level.SEVERE, null, ex);
                    }

                    String[] new_row_data = {(String) pair.getKey(), user, apikey};

                    swingReflectionInvokeAndWait("addRow", elc_model, new Object[]{new_row_data});
                }

                mega_model = (DefaultTableModel) swingReflectionInvokeAndWaitForReturn("getModel", mega_accounts_table);

                elc_model = (DefaultTableModel) swingReflectionInvokeAndWaitForReturn("getModel", elc_accounts_table);

                swingReflectionInvoke("setEnabled", remove_mega_account_button, (mega_model.getRowCount() > 0));

                swingReflectionInvoke("setEnabled", remove_elc_account_button, (elc_model.getRowCount() > 0));
            }

        } else {

            swingReflectionInvoke("setVisible", unlock_accounts_button, false);

            for (Map.Entry pair : _main_panel.getMega_accounts().entrySet()) {

                HashMap<String, Object> data = (HashMap) pair.getValue();

                String[] new_row_data = {(String) pair.getKey(), (String) data.get("password")};

                swingReflectionInvokeAndWait("addRow", mega_model, new Object[]{new_row_data});
            }

            for (Map.Entry pair : _main_panel.getElc_accounts().entrySet()) {

                HashMap<String, Object> data = (HashMap) pair.getValue();

                String[] new_row_data = {(String) pair.getKey(), (String) data.get("user"), (String) data.get("apikey")};

                swingReflectionInvokeAndWait("addRow", elc_model, new Object[]{new_row_data});
            }

            swingReflectionInvoke("setEnabled", remove_mega_account_button, (mega_model.getRowCount() > 0));

            swingReflectionInvoke("setEnabled", remove_elc_account_button, (elc_model.getRowCount() > 0));

        }

        boolean use_mc_reverse = false;

        String megacrypter_reverse = DBTools.selectSettingValueFromDB("megacrypter_reverse");

        String megacrypter_reverse_p = null;

        if (megacrypter_reverse != null) {

            use_mc_reverse = megacrypter_reverse.equals("yes");

            megacrypter_reverse_p = DBTools.selectSettingValueFromDB("megacrypter_reverse_port");
        }

        if (use_mc_reverse) {

            swingReflectionInvoke("setSelected", megacrypter_reverse_checkbox, true);
            swingReflectionInvokeAndWait("setModel", megacrypter_reverse_port_spinner, new SpinnerNumberModel(Integer.parseInt(megacrypter_reverse_p), 1024, 65535, 1));
            swingReflectionInvoke("setEditable", ((JSpinner.DefaultEditor) megacrypter_reverse_port_spinner.getEditor()).getTextField(), true);
            swingReflectionInvoke("setEnabled", megacrypter_reverse_port_spinner, true);
            swingReflectionInvoke("setEnabled", megacrypter_reverse_warning_label, true);
        } else {

            swingReflectionInvoke("setSelected", megacrypter_reverse_checkbox, false);
            swingReflectionInvokeAndWait("setModel", megacrypter_reverse_port_spinner, new SpinnerNumberModel(Integer.parseInt(megacrypter_reverse_p), 1024, 65535, 1));
            swingReflectionInvoke("setEditable", ((JSpinner.DefaultEditor) megacrypter_reverse_port_spinner.getEditor()).getTextField(), true);
            swingReflectionInvoke("setEnabled", megacrypter_reverse_port_label, false);
            swingReflectionInvoke("setEnabled", megacrypter_reverse_port_spinner, false);
            swingReflectionInvoke("setEnabled", megacrypter_reverse_warning_label, false);
        }

        boolean use_proxy = false;

        String use_proxy_val = DBTools.selectSettingValueFromDB("use_proxy");

        if (use_proxy_val != null) {
            use_proxy = (use_proxy_val.equals("yes"));
        }

        use_proxy_checkbox.setSelected(use_proxy);

        swingReflectionInvoke("setText", proxy_host_textfield, DBTools.selectSettingValueFromDB("proxy_host"));

        swingReflectionInvoke("setText", proxy_port_textfield, DBTools.selectSettingValueFromDB("proxy_port"));

        swingReflectionInvoke("setText", proxy_user_textfield, DBTools.selectSettingValueFromDB("proxy_user"));

        swingReflectionInvoke("setText", proxy_pass_textfield, DBTools.selectSettingValueFromDB("proxy_pass"));

        _remember_master_pass = true;

        _deleted_mega_accounts = new HashSet();

        _deleted_elc_accounts = new HashSet();

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
        jSeparator1 = new javax.swing.JSeparator();
        jSeparator2 = new javax.swing.JSeparator();
        jSeparator3 = new javax.swing.JSeparator();
        jSeparator4 = new javax.swing.JSeparator();
        rec_download_slots_label = new javax.swing.JLabel();
        use_mega_account_down_checkbox = new javax.swing.JCheckBox();
        use_mega_account_down_combobox = new javax.swing.JComboBox<>();
        jSeparator7 = new javax.swing.JSeparator();
        use_mega_label = new javax.swing.JLabel();
        megacrypter_reverse_checkbox = new javax.swing.JCheckBox();
        megacrypter_reverse_port_label = new javax.swing.JLabel();
        megacrypter_reverse_warning_label = new javax.swing.JLabel();
        megacrypter_reverse_port_spinner = new javax.swing.JSpinner();
        uploads_panel = new javax.swing.JPanel();
        default_slots_up_label = new javax.swing.JLabel();
        max_uploads_label = new javax.swing.JLabel();
        default_slots_up_spinner = new javax.swing.JSpinner();
        max_uploads_spinner = new javax.swing.JSpinner();
        multi_slot_up_checkbox = new javax.swing.JCheckBox();
        max_up_speed_label = new javax.swing.JLabel();
        max_up_speed_spinner = new javax.swing.JSpinner();
        limit_upload_speed_checkbox = new javax.swing.JCheckBox();
        jSeparator5 = new javax.swing.JSeparator();
        jSeparator6 = new javax.swing.JSeparator();
        rec_upload_slots_label = new javax.swing.JLabel();
        accounts_panel = new javax.swing.JPanel();
        mega_accounts_scrollpane = new javax.swing.JScrollPane();
        mega_accounts_table = new javax.swing.JTable();
        mega_accounts_label = new javax.swing.JLabel();
        remove_mega_account_button = new javax.swing.JButton();
        add_mega_account_button = new javax.swing.JButton();
        encrypt_pass_checkbox = new javax.swing.JCheckBox();
        delete_all_accounts_button = new javax.swing.JButton();
        unlock_accounts_button = new javax.swing.JButton();
        elc_accounts_scrollpane = new javax.swing.JScrollPane();
        elc_accounts_table = new javax.swing.JTable();
        elc_accounts_label = new javax.swing.JLabel();
        remove_elc_account_button = new javax.swing.JButton();
        add_elc_account_button = new javax.swing.JButton();
        jPanel1 = new javax.swing.JPanel();
        proxy_panel = new javax.swing.JPanel();
        proxy_host_label = new javax.swing.JLabel();
        proxy_host_textfield = new javax.swing.JTextField();
        proxy_port_label = new javax.swing.JLabel();
        proxy_port_textfield = new javax.swing.JTextField();
        use_proxy_checkbox = new javax.swing.JCheckBox();
        proxy_warning_label = new javax.swing.JLabel();
        proxy_auth_panel = new javax.swing.JPanel();
        proxy_user_label = new javax.swing.JLabel();
        proxy_user_textfield = new javax.swing.JTextField();
        proxy_pass_label = new javax.swing.JLabel();
        proxy_pass_textfield = new javax.swing.JPasswordField();
        status = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Settings");
        setResizable(false);

        ok_button.setFont(new java.awt.Font("Ubuntu", 1, 22)); // NOI18N
        ok_button.setText("OK");
        ok_button.setDoubleBuffered(true);
        ok_button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ok_buttonActionPerformed(evt);
            }
        });

        cancel_button.setFont(new java.awt.Font("Ubuntu", 1, 22)); // NOI18N
        cancel_button.setText("CANCEL");
        cancel_button.setDoubleBuffered(true);
        cancel_button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancel_buttonActionPerformed(evt);
            }
        });

        jTabbedPane1.setDoubleBuffered(true);
        jTabbedPane1.setFont(new java.awt.Font("Ubuntu", 1, 22)); // NOI18N

        default_slots_down_spinner.setFont(new java.awt.Font("Dialog", 0, 20)); // NOI18N
        default_slots_down_spinner.setDoubleBuffered(true);
        default_slots_down_spinner.setValue(2);

        max_downloads_label.setFont(new java.awt.Font("Ubuntu", 1, 20)); // NOI18N
        max_downloads_label.setText("Max parallel downloads:");
        max_downloads_label.setDoubleBuffered(true);

        max_downloads_spinner.setFont(new java.awt.Font("Dialog", 0, 20)); // NOI18N
        max_downloads_spinner.setDoubleBuffered(true);

        verify_file_down_checkbox.setFont(new java.awt.Font("Ubuntu", 1, 20)); // NOI18N
        verify_file_down_checkbox.setText("Verify file integrity (when download is finished)");
        verify_file_down_checkbox.setDoubleBuffered(true);

        down_dir_label.setFont(new java.awt.Font("Ubuntu", 1, 20)); // NOI18N
        down_dir_label.setText("Default downloads directory:");
        down_dir_label.setDoubleBuffered(true);

        change_download_dir_button.setFont(new java.awt.Font("Ubuntu", 1, 20)); // NOI18N
        change_download_dir_button.setText("Change it");
        change_download_dir_button.setDoubleBuffered(true);
        change_download_dir_button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                change_download_dir_buttonActionPerformed(evt);
            }
        });

        default_slots_down_label.setFont(new java.awt.Font("Ubuntu", 1, 20)); // NOI18N
        default_slots_down_label.setText("Default slots per file:");
        default_slots_down_label.setDoubleBuffered(true);

        multi_slot_down_checkbox.setFont(new java.awt.Font("Ubuntu", 1, 20)); // NOI18N
        multi_slot_down_checkbox.setText("Use multi slot download mode (download restart needed)");
        multi_slot_down_checkbox.setDoubleBuffered(true);
        multi_slot_down_checkbox.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                multi_slot_down_checkboxStateChanged(evt);
            }
        });

        limit_download_speed_checkbox.setFont(new java.awt.Font("Ubuntu", 1, 20)); // NOI18N
        limit_download_speed_checkbox.setText("Limit download speed");
        limit_download_speed_checkbox.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                limit_download_speed_checkboxStateChanged(evt);
            }
        });

        max_down_speed_label.setFont(new java.awt.Font("Ubuntu", 1, 20)); // NOI18N
        max_down_speed_label.setText("Max speed (KB/s):");

        max_down_speed_spinner.setFont(new java.awt.Font("Dialog", 0, 20)); // NOI18N

        default_dir_label.setFont(new java.awt.Font("Ubuntu", 1, 20)); // NOI18N

        rec_download_slots_label.setFont(new java.awt.Font("Ubuntu", 2, 14)); // NOI18N
        rec_download_slots_label.setText("Note: it is recommended not to enable MULTI SLOT (unless you want to download +5GB file without PRO account, in which case you will MUST USE multi slot). ");

        use_mega_account_down_checkbox.setFont(new java.awt.Font("Ubuntu", 1, 20)); // NOI18N
        use_mega_account_down_checkbox.setText("Use MEGA account for download/stream");
        use_mega_account_down_checkbox.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                use_mega_account_down_checkboxStateChanged(evt);
            }
        });

        use_mega_account_down_combobox.setFont(new java.awt.Font("Ubuntu", 0, 20)); // NOI18N

        use_mega_label.setFont(new java.awt.Font("Ubuntu", 0, 16)); // NOI18N
        use_mega_label.setText("Default account:");

        megacrypter_reverse_checkbox.setFont(new java.awt.Font("Ubuntu", 1, 20)); // NOI18N
        megacrypter_reverse_checkbox.setText("Use Megacrypter reverse mode");
        megacrypter_reverse_checkbox.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                megacrypter_reverse_checkboxStateChanged(evt);
            }
        });

        megacrypter_reverse_port_label.setFont(new java.awt.Font("Ubuntu", 0, 16)); // NOI18N
        megacrypter_reverse_port_label.setText("TCP Port:");

        megacrypter_reverse_warning_label.setFont(new java.awt.Font("Ubuntu", 2, 14)); // NOI18N
        megacrypter_reverse_warning_label.setText("Note: you MUST OPEN this port in your router/firewall.");

        megacrypter_reverse_port_spinner.setFont(new java.awt.Font("Ubuntu", 0, 16)); // NOI18N

        javax.swing.GroupLayout downloads_panelLayout = new javax.swing.GroupLayout(downloads_panel);
        downloads_panel.setLayout(downloads_panelLayout);
        downloads_panelLayout.setHorizontalGroup(
            downloads_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(downloads_panelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(downloads_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jSeparator3, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jSeparator1)
                    .addGroup(downloads_panelLayout.createSequentialGroup()
                        .addComponent(change_download_dir_button)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(default_dir_label, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addComponent(jSeparator2)
                    .addComponent(jSeparator4)
                    .addComponent(jSeparator7)
                    .addGroup(downloads_panelLayout.createSequentialGroup()
                        .addGroup(downloads_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(down_dir_label)
                            .addComponent(multi_slot_down_checkbox)
                            .addComponent(rec_download_slots_label)
                            .addComponent(limit_download_speed_checkbox)
                            .addGroup(downloads_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                .addGroup(downloads_panelLayout.createSequentialGroup()
                                    .addComponent(max_downloads_label)
                                    .addGap(120, 120, 120)
                                    .addComponent(max_downloads_spinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGroup(downloads_panelLayout.createSequentialGroup()
                                    .addGap(12, 12, 12)
                                    .addGroup(downloads_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                        .addGroup(downloads_panelLayout.createSequentialGroup()
                                            .addComponent(max_down_speed_label)
                                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                            .addComponent(max_down_speed_spinner, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE))
                                        .addGroup(downloads_panelLayout.createSequentialGroup()
                                            .addComponent(default_slots_down_label)
                                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                            .addComponent(default_slots_down_spinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))))
                            .addComponent(use_mega_account_down_checkbox)
                            .addComponent(verify_file_down_checkbox)
                            .addGroup(downloads_panelLayout.createSequentialGroup()
                                .addComponent(use_mega_label)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(use_mega_account_down_combobox, javax.swing.GroupLayout.PREFERRED_SIZE, 569, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(downloads_panelLayout.createSequentialGroup()
                                .addComponent(megacrypter_reverse_port_label)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(megacrypter_reverse_port_spinner, javax.swing.GroupLayout.PREFERRED_SIZE, 93, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(megacrypter_reverse_warning_label))
                            .addComponent(megacrypter_reverse_checkbox))
                        .addGap(0, 5, Short.MAX_VALUE)))
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
                .addGap(10, 10, 10)
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(downloads_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(max_downloads_label)
                    .addComponent(max_downloads_spinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator2, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(multi_slot_down_checkbox)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(downloads_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(default_slots_down_label)
                    .addComponent(default_slots_down_spinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(rec_download_slots_label)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator3, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(4, 4, 4)
                .addComponent(limit_download_speed_checkbox)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(downloads_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(max_down_speed_spinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(max_down_speed_label))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator4, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(verify_file_down_checkbox)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator7, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(use_mega_account_down_checkbox)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(downloads_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(use_mega_account_down_combobox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(use_mega_label))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(megacrypter_reverse_checkbox)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(downloads_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(megacrypter_reverse_port_label)
                    .addComponent(megacrypter_reverse_warning_label)
                    .addComponent(megacrypter_reverse_port_spinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(25, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("Downloads", downloads_panel);

        default_slots_up_label.setFont(new java.awt.Font("Ubuntu", 1, 20)); // NOI18N
        default_slots_up_label.setText("Default slots per file:");
        default_slots_up_label.setDoubleBuffered(true);

        max_uploads_label.setFont(new java.awt.Font("Ubuntu", 1, 20)); // NOI18N
        max_uploads_label.setText("Max parallel uploads:");
        max_uploads_label.setDoubleBuffered(true);

        default_slots_up_spinner.setFont(new java.awt.Font("Ubuntu", 0, 20)); // NOI18N
        default_slots_up_spinner.setDoubleBuffered(true);
        default_slots_up_spinner.setValue(2);

        max_uploads_spinner.setFont(new java.awt.Font("Ubuntu", 0, 20)); // NOI18N
        max_uploads_spinner.setDoubleBuffered(true);

        multi_slot_up_checkbox.setFont(new java.awt.Font("Ubuntu", 1, 20)); // NOI18N
        multi_slot_up_checkbox.setText("Use multi slot upload mode (upload restart needed)");
        multi_slot_up_checkbox.setDoubleBuffered(true);
        multi_slot_up_checkbox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                multi_slot_up_checkboxActionPerformed(evt);
            }
        });

        max_up_speed_label.setFont(new java.awt.Font("Ubuntu", 1, 20)); // NOI18N
        max_up_speed_label.setText("Max speed (KB/s):");

        max_up_speed_spinner.setFont(new java.awt.Font("Ubuntu", 0, 20)); // NOI18N

        limit_upload_speed_checkbox.setFont(new java.awt.Font("Ubuntu", 1, 20)); // NOI18N
        limit_upload_speed_checkbox.setText("Limit upload speed");
        limit_upload_speed_checkbox.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                limit_upload_speed_checkboxStateChanged(evt);
            }
        });

        rec_upload_slots_label.setFont(new java.awt.Font("Ubuntu", 2, 16)); // NOI18N
        rec_upload_slots_label.setText("Note: MULTI-SLOT it's more robust against upload errors but it might be slower.");

        javax.swing.GroupLayout uploads_panelLayout = new javax.swing.GroupLayout(uploads_panel);
        uploads_panel.setLayout(uploads_panelLayout);
        uploads_panelLayout.setHorizontalGroup(
            uploads_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(uploads_panelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(uploads_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jSeparator5)
                    .addComponent(jSeparator6)
                    .addGroup(uploads_panelLayout.createSequentialGroup()
                        .addGroup(uploads_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(rec_upload_slots_label)
                            .addComponent(multi_slot_up_checkbox)
                            .addGroup(uploads_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                .addGroup(uploads_panelLayout.createSequentialGroup()
                                    .addGap(12, 12, 12)
                                    .addComponent(default_slots_up_label)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(default_slots_up_spinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGroup(uploads_panelLayout.createSequentialGroup()
                                    .addComponent(max_uploads_label)
                                    .addGap(120, 120, 120)
                                    .addComponent(max_uploads_spinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                            .addGroup(uploads_panelLayout.createSequentialGroup()
                                .addGap(12, 12, 12)
                                .addComponent(max_up_speed_label)
                                .addGap(98, 98, 98)
                                .addComponent(max_up_speed_spinner, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(limit_upload_speed_checkbox))
                        .addGap(0, 406, Short.MAX_VALUE)))
                .addContainerGap())
        );
        uploads_panelLayout.setVerticalGroup(
            uploads_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(uploads_panelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(uploads_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(max_uploads_label)
                    .addComponent(max_uploads_spinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator5, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(multi_slot_up_checkbox)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(uploads_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(default_slots_up_spinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(default_slots_up_label))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(rec_upload_slots_label)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator6, javax.swing.GroupLayout.PREFERRED_SIZE, 11, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(limit_upload_speed_checkbox)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(uploads_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(max_up_speed_label)
                    .addComponent(max_up_speed_spinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(314, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("Uploads", uploads_panel);

        mega_accounts_table.setFont(new java.awt.Font("Ubuntu", 0, 18)); // NOI18N
        mega_accounts_table.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Email", "Password"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }
        });
        mega_accounts_table.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        mega_accounts_table.setDoubleBuffered(true);
        mega_accounts_table.setRowHeight(24);
        mega_accounts_scrollpane.setViewportView(mega_accounts_table);

        mega_accounts_label.setFont(new java.awt.Font("Ubuntu", 1, 20)); // NOI18N
        mega_accounts_label.setText("Your MEGA accounts:");
        mega_accounts_label.setDoubleBuffered(true);

        remove_mega_account_button.setFont(new java.awt.Font("Ubuntu", 1, 18)); // NOI18N
        remove_mega_account_button.setText("Remove selected");
        remove_mega_account_button.setDoubleBuffered(true);
        remove_mega_account_button.setEnabled(false);
        remove_mega_account_button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                remove_mega_account_buttonActionPerformed(evt);
            }
        });

        add_mega_account_button.setFont(new java.awt.Font("Ubuntu", 1, 18)); // NOI18N
        add_mega_account_button.setText("Add account");
        add_mega_account_button.setDoubleBuffered(true);
        add_mega_account_button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                add_mega_account_buttonActionPerformed(evt);
            }
        });

        encrypt_pass_checkbox.setFont(new java.awt.Font("Ubuntu", 1, 20)); // NOI18N
        encrypt_pass_checkbox.setText("Encrypt on disk sensitive information");
        encrypt_pass_checkbox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                encrypt_pass_checkboxActionPerformed(evt);
            }
        });

        delete_all_accounts_button.setBackground(new java.awt.Color(255, 51, 0));
        delete_all_accounts_button.setFont(new java.awt.Font("Ubuntu", 1, 20)); // NOI18N
        delete_all_accounts_button.setForeground(new java.awt.Color(255, 255, 255));
        delete_all_accounts_button.setText("RESET ACCOUNTS");
        delete_all_accounts_button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                delete_all_accounts_buttonActionPerformed(evt);
            }
        });

        unlock_accounts_button.setBackground(new java.awt.Color(0, 153, 51));
        unlock_accounts_button.setFont(new java.awt.Font("Ubuntu", 1, 20)); // NOI18N
        unlock_accounts_button.setForeground(new java.awt.Color(255, 255, 255));
        unlock_accounts_button.setText("Unlock accounts");
        unlock_accounts_button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                unlock_accounts_buttonActionPerformed(evt);
            }
        });

        elc_accounts_scrollpane.setDoubleBuffered(true);
        elc_accounts_scrollpane.setFont(new java.awt.Font("Ubuntu", 0, 12)); // NOI18N

        elc_accounts_table.setFont(new java.awt.Font("Ubuntu", 0, 18)); // NOI18N
        elc_accounts_table.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Host", "User", "API-KEY"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class, java.lang.String.class
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }
        });
        elc_accounts_table.setRowHeight(24);
        elc_accounts_scrollpane.setViewportView(elc_accounts_table);

        elc_accounts_label.setFont(new java.awt.Font("Ubuntu", 1, 20)); // NOI18N
        elc_accounts_label.setText("Your ELC accounts:");
        elc_accounts_label.setDoubleBuffered(true);

        remove_elc_account_button.setFont(new java.awt.Font("Ubuntu", 1, 18)); // NOI18N
        remove_elc_account_button.setText("Remove selected");
        remove_elc_account_button.setDoubleBuffered(true);
        remove_elc_account_button.setEnabled(false);
        remove_elc_account_button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                remove_elc_account_buttonActionPerformed(evt);
            }
        });

        add_elc_account_button.setFont(new java.awt.Font("Ubuntu", 1, 18)); // NOI18N
        add_elc_account_button.setText("Add account");
        add_elc_account_button.setDoubleBuffered(true);
        add_elc_account_button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                add_elc_account_buttonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout accounts_panelLayout = new javax.swing.GroupLayout(accounts_panel);
        accounts_panel.setLayout(accounts_panelLayout);
        accounts_panelLayout.setHorizontalGroup(
            accounts_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(accounts_panelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(accounts_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(mega_accounts_scrollpane)
                    .addComponent(elc_accounts_scrollpane)
                    .addGroup(accounts_panelLayout.createSequentialGroup()
                        .addComponent(delete_all_accounts_button)
                        .addGap(18, 18, 18)
                        .addComponent(unlock_accounts_button)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(encrypt_pass_checkbox))
                    .addGroup(accounts_panelLayout.createSequentialGroup()
                        .addComponent(remove_mega_account_button)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(add_mega_account_button))
                    .addGroup(accounts_panelLayout.createSequentialGroup()
                        .addGroup(accounts_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(mega_accounts_label)
                            .addComponent(elc_accounts_label))
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(accounts_panelLayout.createSequentialGroup()
                        .addComponent(remove_elc_account_button)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(add_elc_account_button)))
                .addContainerGap())
        );
        accounts_panelLayout.setVerticalGroup(
            accounts_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(accounts_panelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(accounts_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(unlock_accounts_button)
                    .addComponent(delete_all_accounts_button)
                    .addComponent(encrypt_pass_checkbox))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(mega_accounts_label)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(mega_accounts_scrollpane, javax.swing.GroupLayout.PREFERRED_SIZE, 140, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(accounts_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(remove_mega_account_button)
                    .addComponent(add_mega_account_button))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(elc_accounts_label)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(elc_accounts_scrollpane, javax.swing.GroupLayout.PREFERRED_SIZE, 140, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(accounts_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(remove_elc_account_button)
                    .addComponent(add_elc_account_button))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("Accounts", accounts_panel);

        proxy_panel.setBorder(javax.swing.BorderFactory.createTitledBorder("Proxy settings"));

        proxy_host_label.setFont(new java.awt.Font("Ubuntu", 1, 20)); // NOI18N
        proxy_host_label.setText("Host:");
        proxy_host_label.setDoubleBuffered(true);
        proxy_host_label.setEnabled(false);

        proxy_host_textfield.setFont(new java.awt.Font("Ubuntu", 0, 20)); // NOI18N
        proxy_host_textfield.setDoubleBuffered(true);
        proxy_host_textfield.setEnabled(false);
        proxy_host_textfield.addMouseListener(new ContextMenuMouseListener());

        proxy_port_label.setFont(new java.awt.Font("Ubuntu", 1, 20)); // NOI18N
        proxy_port_label.setText("Port:");
        proxy_port_label.setDoubleBuffered(true);
        proxy_port_label.setEnabled(false);

        proxy_port_textfield.setFont(new java.awt.Font("Ubuntu", 0, 20)); // NOI18N
        proxy_port_textfield.setDoubleBuffered(true);
        proxy_port_textfield.setEnabled(false);
        proxy_port_textfield.addMouseListener(new ContextMenuMouseListener());

        use_proxy_checkbox.setFont(new java.awt.Font("Ubuntu", 1, 20)); // NOI18N
        use_proxy_checkbox.setText("Use HTTP(S) PROXY (app restart required)");
        use_proxy_checkbox.setDoubleBuffered(true);
        use_proxy_checkbox.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                use_proxy_checkboxStateChanged(evt);
            }
        });

        proxy_warning_label.setFont(new java.awt.Font("Ubuntu", 0, 16)); // NOI18N
        proxy_warning_label.setText("Warning: Megabasterd will use this proxy for ALL connections.");
        proxy_warning_label.setEnabled(false);

        proxy_auth_panel.setBorder(javax.swing.BorderFactory.createTitledBorder("Authentication"));

        proxy_user_label.setFont(new java.awt.Font("Ubuntu", 1, 20)); // NOI18N
        proxy_user_label.setText("Username:");
        proxy_user_label.setDoubleBuffered(true);
        proxy_user_label.setEnabled(false);

        proxy_user_textfield.setFont(new java.awt.Font("Ubuntu", 0, 20)); // NOI18N
        proxy_user_textfield.setDoubleBuffered(true);
        proxy_user_textfield.setEnabled(false);
        proxy_user_textfield.addMouseListener(new ContextMenuMouseListener());

        proxy_pass_label.setFont(new java.awt.Font("Ubuntu", 1, 20)); // NOI18N
        proxy_pass_label.setText("Password:");
        proxy_pass_label.setDoubleBuffered(true);
        proxy_pass_label.setEnabled(false);

        proxy_pass_textfield.setFont(new java.awt.Font("Ubuntu", 0, 20)); // NOI18N
        proxy_pass_textfield.setText("jPasswordField1");
        proxy_pass_textfield.setEnabled(false);

        javax.swing.GroupLayout proxy_auth_panelLayout = new javax.swing.GroupLayout(proxy_auth_panel);
        proxy_auth_panel.setLayout(proxy_auth_panelLayout);
        proxy_auth_panelLayout.setHorizontalGroup(
            proxy_auth_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(proxy_auth_panelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(proxy_user_label)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(proxy_user_textfield, javax.swing.GroupLayout.PREFERRED_SIZE, 280, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(proxy_pass_label)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(proxy_pass_textfield, javax.swing.GroupLayout.PREFERRED_SIZE, 280, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        proxy_auth_panelLayout.setVerticalGroup(
            proxy_auth_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(proxy_auth_panelLayout.createSequentialGroup()
                .addGroup(proxy_auth_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(proxy_user_label)
                    .addComponent(proxy_user_textfield, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(proxy_pass_label)
                    .addComponent(proxy_pass_textfield, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout proxy_panelLayout = new javax.swing.GroupLayout(proxy_panel);
        proxy_panel.setLayout(proxy_panelLayout);
        proxy_panelLayout.setHorizontalGroup(
            proxy_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(proxy_panelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(proxy_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(proxy_panelLayout.createSequentialGroup()
                        .addGroup(proxy_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(use_proxy_checkbox)
                            .addComponent(proxy_warning_label))
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(proxy_panelLayout.createSequentialGroup()
                        .addGroup(proxy_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, proxy_panelLayout.createSequentialGroup()
                                .addComponent(proxy_host_label)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(proxy_host_textfield, javax.swing.GroupLayout.PREFERRED_SIZE, 300, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(proxy_port_label)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(proxy_port_textfield, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(0, 0, Short.MAX_VALUE))
                            .addComponent(proxy_auth_panel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addContainerGap())))
        );
        proxy_panelLayout.setVerticalGroup(
            proxy_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, proxy_panelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(use_proxy_checkbox)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(proxy_warning_label)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(proxy_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(proxy_host_label)
                    .addComponent(proxy_host_textfield, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(proxy_port_label)
                    .addComponent(proxy_port_textfield, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(proxy_auth_panel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(proxy_panel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(proxy_panel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(331, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("Advanced", jPanel1);

        status.setFont(new java.awt.Font("Ubuntu", 1, 20)); // NOI18N
        status.setForeground(new java.awt.Color(9, 109, 235));
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
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(ok_button, javax.swing.GroupLayout.PREFERRED_SIZE, 130, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(cancel_button, javax.swing.GroupLayout.PREFERRED_SIZE, 130, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jTabbedPane1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
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

        if (filechooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {

            File file = filechooser.getSelectedFile();

            _download_path = file.getAbsolutePath();

            default_dir_label.setText(truncateText(_download_path, 80));

        }
    }//GEN-LAST:event_change_download_dir_buttonActionPerformed

    private void cancel_buttonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancel_buttonActionPerformed

        this.setVisible(false);
    }//GEN-LAST:event_cancel_buttonActionPerformed

    private void ok_buttonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ok_buttonActionPerformed

        try {

            _settings_ok = true;

            if ((boolean) swingReflectionInvokeAndWaitForReturn("isEmpty", proxy_host_textfield.getText())) {

                swingReflectionInvokeAndWait("setSelected", use_proxy_checkbox, false);
            }

            insertSettingValueInDB("default_down_dir", _download_path);
            insertSettingValueInDB("default_slots_down", String.valueOf((int) swingReflectionInvokeAndWaitForReturn("getValue", default_slots_down_spinner)));
            insertSettingValueInDB("default_slots_up", String.valueOf((int) swingReflectionInvokeAndWaitForReturn("getValue", default_slots_up_spinner)));
            insertSettingValueInDB("use_slots_down", (boolean) swingReflectionInvokeAndWaitForReturn("isSelected", multi_slot_down_checkbox) ? "yes" : "no");
            insertSettingValueInDB("use_slots_up", (boolean) swingReflectionInvokeAndWaitForReturn("isSelected", multi_slot_up_checkbox) ? "yes" : "no");
            insertSettingValueInDB("max_downloads", String.valueOf((int) swingReflectionInvokeAndWaitForReturn("getValue", max_downloads_spinner)));
            insertSettingValueInDB("max_uploads", String.valueOf((int) swingReflectionInvokeAndWaitForReturn("getValue", max_uploads_spinner)));
            insertSettingValueInDB("verify_down_file", (boolean) swingReflectionInvokeAndWaitForReturn("isSelected", verify_file_down_checkbox) ? "yes" : "no");
            insertSettingValueInDB("limit_download_speed", (boolean) swingReflectionInvokeAndWaitForReturn("isSelected", limit_download_speed_checkbox) ? "yes" : "no");
            insertSettingValueInDB("max_download_speed", String.valueOf((int) swingReflectionInvokeAndWaitForReturn("getValue", max_down_speed_spinner)));
            insertSettingValueInDB("limit_upload_speed", (boolean) swingReflectionInvokeAndWaitForReturn("isSelected", limit_upload_speed_checkbox) ? "yes" : "no");
            insertSettingValueInDB("max_upload_speed", String.valueOf((int) swingReflectionInvokeAndWaitForReturn("getValue", max_up_speed_spinner)));
            insertSettingValueInDB("use_mega_account_down", (boolean) swingReflectionInvokeAndWaitForReturn("isSelected", use_mega_account_down_checkbox) ? "yes" : "no");
            insertSettingValueInDB("mega_account_down", (String) swingReflectionInvokeAndWaitForReturn("getSelectedItem", use_mega_account_down_combobox));
            insertSettingValueInDB("megacrypter_reverse", (boolean) swingReflectionInvokeAndWaitForReturn("isSelected", megacrypter_reverse_checkbox) ? "yes" : "no");
            insertSettingValueInDB("megacrypter_reverse_port", String.valueOf((int) swingReflectionInvokeAndWaitForReturn("getValue", megacrypter_reverse_port_spinner)));

            boolean old_use_proxy = false;

            String use_proxy_val = DBTools.selectSettingValueFromDB("use_proxy");

            if (use_proxy_val != null) {
                old_use_proxy = (use_proxy_val.equals("yes"));
            }

            boolean use_proxy = (boolean) swingReflectionInvokeAndWaitForReturn("isSelected", use_proxy_checkbox);

            String old_proxy_host = DBTools.selectSettingValueFromDB("proxy_host");

            if (old_proxy_host == null) {

                old_proxy_host = "";
            }

            String proxy_host = ((String) swingReflectionInvokeAndWaitForReturn("getText", proxy_host_textfield)).trim();

            String old_proxy_port = DBTools.selectSettingValueFromDB("proxy_port");

            if (old_proxy_port == null) {

                old_proxy_port = "";
            }

            String proxy_port = ((String) swingReflectionInvokeAndWaitForReturn("getText", proxy_port_textfield)).trim();

            String old_proxy_user = DBTools.selectSettingValueFromDB("proxy_user");

            if (old_proxy_user == null) {

                old_proxy_user = "";
            }

            String proxy_user = ((String) swingReflectionInvokeAndWaitForReturn("getText", proxy_user_textfield)).trim();

            String old_proxy_pass = DBTools.selectSettingValueFromDB("proxy_pass");

            if (old_proxy_pass == null) {

                old_proxy_pass = "";
            }

            String proxy_pass = new String((char[]) swingReflectionInvokeAndWaitForReturn("getPassword", proxy_pass_textfield));

            insertSettingValueInDB("use_proxy", use_proxy ? "yes" : "no");
            insertSettingValueInDB("proxy_host", proxy_host);
            insertSettingValueInDB("proxy_port", proxy_port);
            insertSettingValueInDB("proxy_user", proxy_user);
            insertSettingValueInDB("proxy_pass", proxy_pass);

            if (use_proxy != old_use_proxy || !proxy_host.equals(old_proxy_host) || !proxy_port.equals(old_proxy_port) || !proxy_user.equals(old_proxy_user) || !proxy_pass.equals(old_proxy_pass)) {

                _main_panel.setRestart(true);
            }

            ok_button.setEnabled(false);

            cancel_button.setEnabled(false);

            remove_mega_account_button.setEnabled(false);

            add_mega_account_button.setEnabled(false);

            delete_all_accounts_button.setEnabled(false);

            encrypt_pass_checkbox.setEnabled(false);

            if (elc_accounts_table.isEnabled()) {

                DefaultTableModel model = (DefaultTableModel) elc_accounts_table.getModel();

                for (int i = 0; i < model.getRowCount(); i++) {

                    String host_table = ((String) model.getValueAt(i, 0)).trim().replaceAll("^(https?://)?([^/]+).*$", "$2");

                    String user_table = (String) model.getValueAt(i, 1);

                    String apikey_table = (String) model.getValueAt(i, 2);

                    if (!host_table.isEmpty() && !user_table.isEmpty() && !apikey_table.isEmpty()) {

                        if (_main_panel.getElc_accounts().get(host_table) == null) {

                            if (_main_panel.getMaster_pass_hash() != null) {

                                user_table = Bin2BASE64(CryptTools.aes_cbc_encrypt_pkcs7(user_table.getBytes(), _main_panel.getMaster_pass(), CryptTools.AES_ZERO_IV));

                                apikey_table = Bin2BASE64(CryptTools.aes_cbc_encrypt_pkcs7(apikey_table.getBytes(), _main_panel.getMaster_pass(), CryptTools.AES_ZERO_IV));
                            }

                            DBTools.insertELCAccount(host_table, user_table, apikey_table);

                        } else {

                            HashMap<String, Object> elc_account_data = (HashMap) _main_panel.getElc_accounts().get(host_table);

                            String user = (String) elc_account_data.get("user");

                            String apikey = (String) elc_account_data.get("apikey");

                            if (_main_panel.getMaster_pass() != null) {

                                try {

                                    user = new String(CryptTools.aes_cbc_decrypt_pkcs7(BASE642Bin(user), _main_panel.getMaster_pass(), CryptTools.AES_ZERO_IV));

                                    apikey = new String(CryptTools.aes_cbc_decrypt_pkcs7(BASE642Bin(apikey), _main_panel.getMaster_pass(), CryptTools.AES_ZERO_IV));

                                } catch (Exception ex) {
                                    Logger.getLogger(SettingsDialog.class.getName()).log(Level.SEVERE, null, ex);
                                }
                            }

                            if (!user.equals(user_table) || !apikey.equals(apikey_table)) {

                                user = user_table;

                                apikey = apikey_table;

                                if (_main_panel.getMaster_pass() != null) {

                                    user = Bin2BASE64(CryptTools.aes_cbc_encrypt_pkcs7(user_table.getBytes(), _main_panel.getMaster_pass(), CryptTools.AES_ZERO_IV));

                                    apikey = Bin2BASE64(CryptTools.aes_cbc_encrypt_pkcs7(apikey_table.getBytes(), _main_panel.getMaster_pass(), CryptTools.AES_ZERO_IV));

                                }

                                DBTools.insertELCAccount(host_table, user, apikey);
                            }
                        }
                    }
                }
            }

            if (mega_accounts_table.isEnabled()) {

                mega_accounts_table.setEnabled(false);

                final DefaultTableModel model = (DefaultTableModel) mega_accounts_table.getModel();

                status.setText("Checking your MEGA accounts, please wait...");

                ok_button.setEnabled(false);

                cancel_button.setEnabled(false);

                remove_mega_account_button.setEnabled(false);

                add_mega_account_button.setEnabled(false);

                delete_all_accounts_button.setEnabled(false);

                mega_accounts_table.setEnabled(false);

                encrypt_pass_checkbox.setEnabled(false);

                final Dialog tthis = this;

                THREAD_POOL.execute(new Runnable() {
                    @Override
                    public void run() {

                        ArrayList<String> email_error = new ArrayList<>();

                        for (int i = 0; i < model.getRowCount(); i++) {

                            String email = (String) model.getValueAt(i, 0);

                            String pass = (String) model.getValueAt(i, 1);

                            if (!email.isEmpty() && !pass.isEmpty()) {

                                MegaAPI ma;

                                if (_main_panel.getMega_accounts().get(email) == null) {

                                    ma = new MegaAPI();

                                    try {
                                        ma.login(email, pass);

                                        _main_panel.getMega_active_accounts().put(email, ma);

                                        String password = pass, password_aes = Bin2BASE64(i32a2bin(ma.getPassword_aes())), user_hash = ma.getUser_hash();

                                        if (_main_panel.getMaster_pass_hash() != null) {

                                            password = Bin2BASE64(CryptTools.aes_cbc_encrypt_pkcs7(pass.getBytes(), _main_panel.getMaster_pass(), CryptTools.AES_ZERO_IV));

                                            password_aes = Bin2BASE64(CryptTools.aes_cbc_encrypt_pkcs7(i32a2bin(ma.getPassword_aes()), _main_panel.getMaster_pass(), CryptTools.AES_ZERO_IV));

                                            user_hash = Bin2BASE64(CryptTools.aes_cbc_encrypt_pkcs7(MiscTools.UrlBASE642Bin(ma.getUser_hash()), _main_panel.getMaster_pass(), CryptTools.AES_ZERO_IV));
                                        }

                                        DBTools.insertMegaAccount(email, password, password_aes, user_hash);

                                    } catch (Exception ex) {

                                        email_error.add(email);
                                        getLogger(SettingsDialog.class.getName()).log(Level.SEVERE, null, ex);
                                    }

                                } else {

                                    HashMap<String, Object> mega_account_data = (HashMap) _main_panel.getMega_accounts().get(email);

                                    String password = (String) mega_account_data.get("password");

                                    if (_main_panel.getMaster_pass() != null) {

                                        try {

                                            password = new String(CryptTools.aes_cbc_decrypt_pkcs7(BASE642Bin(password), _main_panel.getMaster_pass(), CryptTools.AES_ZERO_IV));

                                        } catch (Exception ex) {
                                            Logger.getLogger(SettingsDialog.class.getName()).log(Level.SEVERE, null, ex);
                                        }
                                    }

                                    if (!password.equals(pass)) {

                                        ma = new MegaAPI();

                                        try {
                                            ma.login(email, pass);

                                            _main_panel.getMega_active_accounts().put(email, ma);

                                            password = pass;

                                            String password_aes = Bin2BASE64(i32a2bin(ma.getPassword_aes())), user_hash = ma.getUser_hash();

                                            if (_main_panel.getMaster_pass() != null) {

                                                password = Bin2BASE64(CryptTools.aes_cbc_encrypt_pkcs7(pass.getBytes(), _main_panel.getMaster_pass(), CryptTools.AES_ZERO_IV));

                                                password_aes = Bin2BASE64(CryptTools.aes_cbc_encrypt_pkcs7(i32a2bin(ma.getPassword_aes()), _main_panel.getMaster_pass(), CryptTools.AES_ZERO_IV));

                                                user_hash = Bin2BASE64(CryptTools.aes_cbc_encrypt_pkcs7(MiscTools.UrlBASE642Bin(ma.getUser_hash()), _main_panel.getMaster_pass(), CryptTools.AES_ZERO_IV));
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

                        if (email_error.size() > 0) {

                            String email_error_s = "";

                            for (String s : email_error) {

                                email_error_s += s + "\n";
                            }

                            swingReflectionInvoke("setText", status, "");

                            JOptionPane.showMessageDialog(tthis, "There were errors with some accounts. Please, check them:\n\n" + email_error_s, "Error", JOptionPane.ERROR_MESSAGE);

                            swingReflectionInvoke("setEnabled", ok_button, true);

                            swingReflectionInvoke("setEnabled", cancel_button, true);

                            swingReflectionInvoke("setEnabled", remove_mega_account_button, true);

                            swingReflectionInvoke("setEnabled", add_mega_account_button, true);

                            swingReflectionInvoke("setEnabled", mega_accounts_table, true);

                            swingReflectionInvoke("setEnabled", delete_all_accounts_button, true);

                            swingReflectionInvoke("setEnabled", encrypt_pass_checkbox, true);

                        } else {
                            swingReflectionInvoke("setVisible", tthis, false);
                        }

                    }
                });

            } else {

                this.setVisible(false);
            }

        } catch (SQLException ex) {
            getLogger(SettingsDialog.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException ex) {
            Logger.getLogger(SettingsDialog.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception ex) {
            Logger.getLogger(SettingsDialog.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_ok_buttonActionPerformed

    private void multi_slot_down_checkboxStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_multi_slot_down_checkboxStateChanged

        if (!multi_slot_down_checkbox.isSelected()) {

            default_slots_down_spinner.setEnabled(false);
            default_slots_down_label.setEnabled(false);

        } else {

            default_slots_down_spinner.setEnabled(true);
            default_slots_down_label.setEnabled(true);
        }
    }//GEN-LAST:event_multi_slot_down_checkboxStateChanged

    private void remove_mega_account_buttonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_remove_mega_account_buttonActionPerformed

        DefaultTableModel model = (DefaultTableModel) mega_accounts_table.getModel();

        int selected = mega_accounts_table.getSelectedRow();

        while (selected >= 0) {

            String email = (String) model.getValueAt(mega_accounts_table.convertRowIndexToModel(selected), 0);

            _deleted_mega_accounts.add(email);

            model.removeRow(mega_accounts_table.convertRowIndexToModel(selected));

            selected = mega_accounts_table.getSelectedRow();
        }

        mega_accounts_table.clearSelection();

        if (model.getRowCount() == 0) {

            remove_mega_account_button.setEnabled(false);
        }
    }//GEN-LAST:event_remove_mega_account_buttonActionPerformed

    private void add_mega_account_buttonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_add_mega_account_buttonActionPerformed

        DefaultTableModel model = (DefaultTableModel) mega_accounts_table.getModel();

        model.addRow(new Object[]{"", ""});

        mega_accounts_table.clearSelection();

        remove_mega_account_button.setEnabled(true);
    }//GEN-LAST:event_add_mega_account_buttonActionPerformed

    private void limit_download_speed_checkboxStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_limit_download_speed_checkboxStateChanged

        if (!limit_download_speed_checkbox.isSelected()) {

            max_down_speed_label.setEnabled(false);
            max_down_speed_spinner.setEnabled(false);
        } else {
            max_down_speed_label.setEnabled(true);
            max_down_speed_spinner.setEnabled(true);
        }
    }//GEN-LAST:event_limit_download_speed_checkboxStateChanged

    private void encrypt_pass_checkboxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_encrypt_pass_checkboxActionPerformed

        encrypt_pass_checkbox.setEnabled(false);

        final Dialog tthis = this;

        THREAD_POOL.execute(new Runnable() {
            @Override
            public void run() {

                SetMasterPasswordDialog dialog = new SetMasterPasswordDialog((Frame) getParent(), true, _main_panel.getMaster_pass_salt());

                swingReflectionInvokeAndWait("setLocationRelativeTo", dialog, tthis);

                swingReflectionInvokeAndWait("setVisible", dialog, true);

                byte[] old_master_pass = null;

                if (_main_panel.getMaster_pass() != null) {

                    old_master_pass = new byte[_main_panel.getMaster_pass().length];

                    System.arraycopy(_main_panel.getMaster_pass(), 0, old_master_pass, 0, _main_panel.getMaster_pass().length);
                }

                String old_master_pass_hash = _main_panel.getMaster_pass_hash();

                if (dialog.isPass_ok()) {

                    try {

                        if (dialog.getNew_pass() != null && dialog.getNew_pass().length > 0) {

                            _main_panel.setMaster_pass_hash(dialog.getNew_pass_hash());

                            _main_panel.setMaster_pass(dialog.getNew_pass());

                        } else {

                            _main_panel.setMaster_pass_hash(null);

                            _main_panel.setMaster_pass(null);
                        }

                        dialog.deleteNewPass();

                        insertSettingValueInDB("master_pass_hash", _main_panel.getMaster_pass_hash());

                        for (Map.Entry pair : _main_panel.getMega_accounts().entrySet()) {

                            HashMap<String, Object> data = (HashMap) pair.getValue();

                            String email, password, password_aes, user_hash;

                            email = (String) pair.getKey();

                            if (old_master_pass_hash != null) {

                                password = new String(CryptTools.aes_cbc_decrypt_pkcs7(BASE642Bin((String) data.get("password")), old_master_pass, CryptTools.AES_ZERO_IV));

                                password_aes = Bin2BASE64(CryptTools.aes_cbc_decrypt_pkcs7(BASE642Bin((String) data.get("password_aes")), old_master_pass, CryptTools.AES_ZERO_IV));

                                user_hash = Bin2BASE64(CryptTools.aes_cbc_decrypt_pkcs7(BASE642Bin((String) data.get("user_hash")), old_master_pass, CryptTools.AES_ZERO_IV));

                            } else {

                                password = (String) data.get("password");

                                password_aes = (String) data.get("password_aes");

                                user_hash = (String) data.get("user_hash");
                            }

                            if (_main_panel.getMaster_pass() != null) {

                                password = Bin2BASE64(CryptTools.aes_cbc_encrypt_pkcs7(password.getBytes(), _main_panel.getMaster_pass(), CryptTools.AES_ZERO_IV));

                                password_aes = Bin2BASE64(CryptTools.aes_cbc_encrypt_pkcs7(BASE642Bin(password_aes), _main_panel.getMaster_pass(), CryptTools.AES_ZERO_IV));

                                user_hash = Bin2BASE64(CryptTools.aes_cbc_encrypt_pkcs7(BASE642Bin(user_hash), _main_panel.getMaster_pass(), CryptTools.AES_ZERO_IV));
                            }

                            data.put("password", password);

                            data.put("password_aes", password_aes);

                            data.put("user_hash", user_hash);

                            DBTools.insertMegaAccount(email, password, password_aes, user_hash);
                        }

                        for (Map.Entry pair : _main_panel.getElc_accounts().entrySet()) {

                            HashMap<String, Object> data = (HashMap) pair.getValue();

                            String host, user, apikey;

                            host = (String) pair.getKey();

                            if (old_master_pass_hash != null) {

                                user = new String(CryptTools.aes_cbc_decrypt_pkcs7(BASE642Bin((String) data.get("user")), old_master_pass, CryptTools.AES_ZERO_IV));

                                apikey = new String(CryptTools.aes_cbc_decrypt_pkcs7(BASE642Bin((String) data.get("apikey")), old_master_pass, CryptTools.AES_ZERO_IV));

                            } else {

                                user = (String) data.get("user");

                                apikey = (String) data.get("apikey");

                            }

                            if (_main_panel.getMaster_pass() != null) {

                                user = Bin2BASE64(CryptTools.aes_cbc_encrypt_pkcs7(user.getBytes(), _main_panel.getMaster_pass(), CryptTools.AES_ZERO_IV));

                                apikey = Bin2BASE64(CryptTools.aes_cbc_encrypt_pkcs7(apikey.getBytes(), _main_panel.getMaster_pass(), CryptTools.AES_ZERO_IV));
                            }

                            data.put("user", user);

                            data.put("apikey", apikey);

                            DBTools.insertELCAccount(host, user, apikey);
                        }

                    } catch (Exception ex) {
                        Logger.getLogger(SettingsDialog.class.getName()).log(Level.SEVERE, null, ex);
                    }

                }

                encrypt_pass_checkbox.setSelected((_main_panel.getMaster_pass_hash() != null));

                dialog.dispose();

                swingReflectionInvoke("setEnabled", encrypt_pass_checkbox, true);

            }
        });

    }//GEN-LAST:event_encrypt_pass_checkboxActionPerformed

    private void unlock_accounts_buttonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_unlock_accounts_buttonActionPerformed

        unlock_accounts_button.setEnabled(false);

        final Dialog tthis = this;

        THREAD_POOL.execute(new Runnable() {
            @Override
            public void run() {

                GetMasterPasswordDialog dialog = new GetMasterPasswordDialog((Frame) getParent(), true, _main_panel.getMaster_pass_hash(), _main_panel.getMaster_pass_salt());

                swingReflectionInvokeAndWait("setLocationRelativeTo", dialog, tthis);

                swingReflectionInvokeAndWait("setVisible", dialog, true);

                if (dialog.isPass_ok()) {

                    _main_panel.setMaster_pass(dialog.getPass());

                    dialog.deletePass();

                    DefaultTableModel mega_model = new DefaultTableModel(new Object[][]{}, new String[]{"Email", "Password"});

                    DefaultTableModel elc_model = new DefaultTableModel(new Object[][]{}, new String[]{"Host", "User", "API KEY"});

                    swingReflectionInvokeAndWait("setModel", mega_accounts_table, mega_model);

                    swingReflectionInvokeAndWait("setModel", elc_accounts_table, elc_model);

                    swingReflectionInvoke("setEnabled", encrypt_pass_checkbox, true);

                    swingReflectionInvoke("setEnabled", mega_accounts_table, true);

                    swingReflectionInvoke("setEnabled", elc_accounts_table, true);

                    swingReflectionInvoke("setEnabled", remove_mega_account_button, true);

                    swingReflectionInvoke("setEnabled", remove_elc_account_button, true);

                    swingReflectionInvoke("setEnabled", add_mega_account_button, true);

                    swingReflectionInvoke("setEnabled", add_elc_account_button, true);

                    swingReflectionInvoke("setVisible", unlock_accounts_button, false);

                    swingReflectionInvoke("setEnabled", delete_all_accounts_button, true);

                    for (Map.Entry pair : _main_panel.getMega_accounts().entrySet()) {

                        HashMap<String, Object> data = (HashMap) pair.getValue();

                        String pass = null;

                        try {

                            pass = new String(CryptTools.aes_cbc_decrypt_pkcs7(BASE642Bin((String) data.get("password")), _main_panel.getMaster_pass(), CryptTools.AES_ZERO_IV));

                        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException ex) {
                            Logger.getLogger(SettingsDialog.class.getName()).log(Level.SEVERE, null, ex);
                        } catch (Exception ex) {
                            Logger.getLogger(SettingsDialog.class.getName()).log(Level.SEVERE, null, ex);
                        }

                        String[] new_row_data = {(String) pair.getKey(), pass};

                        mega_model.addRow(new_row_data);
                    }

                    for (Map.Entry pair : _main_panel.getElc_accounts().entrySet()) {

                        HashMap<String, Object> data = (HashMap) pair.getValue();

                        String user = null, apikey = null;

                        try {

                            user = new String(CryptTools.aes_cbc_decrypt_pkcs7(BASE642Bin((String) data.get("user")), _main_panel.getMaster_pass(), CryptTools.AES_ZERO_IV));

                            apikey = new String(CryptTools.aes_cbc_decrypt_pkcs7(BASE642Bin((String) data.get("apikey")), _main_panel.getMaster_pass(), CryptTools.AES_ZERO_IV));

                        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException ex) {
                            Logger.getLogger(SettingsDialog.class.getName()).log(Level.SEVERE, null, ex);
                        } catch (Exception ex) {
                            Logger.getLogger(SettingsDialog.class.getName()).log(Level.SEVERE, null, ex);
                        }

                        String[] new_row_data = {(String) pair.getKey(), user, apikey};

                        elc_model.addRow(new_row_data);
                    }

                }

                _remember_master_pass = dialog.getRemember_checkbox().isSelected();

                dialog.dispose();

                swingReflectionInvoke("setEnabled", unlock_accounts_button, true);

            }
        });

    }//GEN-LAST:event_unlock_accounts_buttonActionPerformed

    private void delete_all_accounts_buttonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_delete_all_accounts_buttonActionPerformed

        Object[] options = {"No",
            "Yes"};

        int n = showOptionDialog(this,
                "Master password will be reset and all your accounts will be removed. (This can't be undone)\n\nDo you want to continue?",
                "Warning!", YES_NO_CANCEL_OPTION, javax.swing.JOptionPane.WARNING_MESSAGE,
                null,
                options,
                options[0]);

        if (n == 1) {

            try {
                encrypt_pass_checkbox.setEnabled(true);

                mega_accounts_table.setEnabled(true);

                elc_accounts_table.setEnabled(true);

                remove_mega_account_button.setEnabled(true);

                remove_elc_account_button.setEnabled(true);

                add_mega_account_button.setEnabled(true);

                add_elc_account_button.setEnabled(true);

                unlock_accounts_button.setVisible(false);

                delete_all_accounts_button.setVisible(true);

                DefaultTableModel new_mega_model = new DefaultTableModel(new Object[][]{}, new String[]{"Email", "Password"});

                DefaultTableModel new_elc_model = new DefaultTableModel(new Object[][]{}, new String[]{"Host", "User", "API KEY"});

                mega_accounts_table.setModel(new_mega_model);

                elc_accounts_table.setModel(new_elc_model);

                for (Map.Entry pair : _main_panel.getMega_accounts().entrySet()) {

                    try {
                        DBTools.deleteMegaAccount((String) pair.getKey());
                    } catch (SQLException ex) {
                        Logger.getLogger(SettingsDialog.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }

                for (Map.Entry pair : _main_panel.getElc_accounts().entrySet()) {

                    try {
                        DBTools.deleteELCAccount((String) pair.getKey());
                    } catch (SQLException ex) {
                        Logger.getLogger(SettingsDialog.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }

                _main_panel.setMaster_pass_hash(null);

                _main_panel.setMaster_pass(null);

                insertSettingValueInDB("master_pass_hash", null);

                encrypt_pass_checkbox.setSelected(false);

                _main_panel.getMega_accounts().clear();

                _main_panel.getMega_active_accounts().clear();

                _main_panel.getElc_accounts().clear();

            } catch (SQLException ex) {
                Logger.getLogger(SettingsDialog.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

    }//GEN-LAST:event_delete_all_accounts_buttonActionPerformed

    private void remove_elc_account_buttonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_remove_elc_account_buttonActionPerformed
        DefaultTableModel model = (DefaultTableModel) elc_accounts_table.getModel();

        int selected = elc_accounts_table.getSelectedRow();

        while (selected >= 0) {

            String host = (String) model.getValueAt(elc_accounts_table.convertRowIndexToModel(selected), 0);

            _deleted_elc_accounts.add(host);

            model.removeRow(elc_accounts_table.convertRowIndexToModel(selected));

            selected = elc_accounts_table.getSelectedRow();
        }

        elc_accounts_table.clearSelection();

        if (model.getRowCount() == 0) {

            remove_elc_account_button.setEnabled(false);
        }
    }//GEN-LAST:event_remove_elc_account_buttonActionPerformed

    private void add_elc_account_buttonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_add_elc_account_buttonActionPerformed

        DefaultTableModel model = (DefaultTableModel) elc_accounts_table.getModel();

        model.addRow(new Object[]{"", "", ""});

        elc_accounts_table.clearSelection();

        remove_elc_account_button.setEnabled(true);

    }//GEN-LAST:event_add_elc_account_buttonActionPerformed

    private void use_proxy_checkboxStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_use_proxy_checkboxStateChanged

        if (!use_proxy_checkbox.isSelected()) {

            proxy_host_label.setEnabled(false);
            proxy_host_textfield.setEnabled(false);
            proxy_port_label.setEnabled(false);
            proxy_port_textfield.setEnabled(false);
            proxy_user_label.setEnabled(false);
            proxy_user_textfield.setEnabled(false);
            proxy_pass_label.setEnabled(false);
            proxy_pass_textfield.setEnabled(false);
            proxy_warning_label.setEnabled(false);

        } else {

            proxy_host_label.setEnabled(true);
            proxy_host_textfield.setEnabled(true);
            proxy_port_label.setEnabled(true);
            proxy_port_textfield.setEnabled(true);
            proxy_user_label.setEnabled(true);
            proxy_user_textfield.setEnabled(true);
            proxy_pass_label.setEnabled(true);
            proxy_pass_textfield.setEnabled(true);
            proxy_warning_label.setEnabled(true);
        }

    }//GEN-LAST:event_use_proxy_checkboxStateChanged

    private void limit_upload_speed_checkboxStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_limit_upload_speed_checkboxStateChanged

        if (!limit_upload_speed_checkbox.isSelected()) {

            max_up_speed_label.setEnabled(false);
            max_up_speed_spinner.setEnabled(false);

        } else {
            max_up_speed_label.setEnabled(true);
            max_up_speed_spinner.setEnabled(true);
        }
    }//GEN-LAST:event_limit_upload_speed_checkboxStateChanged

    private void multi_slot_up_checkboxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_multi_slot_up_checkboxActionPerformed
        // TODO add your handling code here:

        if (!multi_slot_up_checkbox.isSelected()) {

            default_slots_up_spinner.setEnabled(false);
            default_slots_up_label.setEnabled(false);

        } else {

            default_slots_up_spinner.setEnabled(true);
            default_slots_up_label.setEnabled(true);
        }

    }//GEN-LAST:event_multi_slot_up_checkboxActionPerformed

    private void use_mega_account_down_checkboxStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_use_mega_account_down_checkboxStateChanged

        if (!use_mega_account_down_checkbox.isSelected()) {

            use_mega_account_down_combobox.setEnabled(false);

            use_mega_label.setEnabled(false);

        } else {

            use_mega_account_down_combobox.setEnabled(true);

            use_mega_label.setEnabled(true);

            use_mega_account_down_combobox.removeAllItems();

            if (_main_panel.getMega_accounts().size() > 0) {

                for (Object o : _main_panel.getMega_accounts().keySet()) {

                    use_mega_account_down_combobox.addItem((String) o);

                }

                String use_mega_account_down = DBTools.selectSettingValueFromDB("mega_account_down");

                if (use_mega_account_down != null) {

                    use_mega_account_down_combobox.setSelectedItem(use_mega_account_down);
                }

            } else {

                use_mega_account_down_combobox.setEnabled(false);

                use_mega_label.setEnabled(false);

                use_mega_account_down_checkbox.setSelected(false);
            }
        }
    }//GEN-LAST:event_use_mega_account_down_checkboxStateChanged

    private void megacrypter_reverse_checkboxStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_megacrypter_reverse_checkboxStateChanged
        // TODO add your handling code here:

        if (megacrypter_reverse_checkbox.isSelected()) {

            megacrypter_reverse_port_label.setEnabled(true);
            megacrypter_reverse_port_spinner.setEnabled(true);
            megacrypter_reverse_warning_label.setEnabled(true);
        } else {
            megacrypter_reverse_port_label.setEnabled(false);
            megacrypter_reverse_port_spinner.setEnabled(false);
            megacrypter_reverse_warning_label.setEnabled(false);
        }

    }//GEN-LAST:event_megacrypter_reverse_checkboxStateChanged

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel accounts_panel;
    private javax.swing.JButton add_elc_account_button;
    private javax.swing.JButton add_mega_account_button;
    private javax.swing.JButton cancel_button;
    private javax.swing.JButton change_download_dir_button;
    private javax.swing.JLabel default_dir_label;
    private javax.swing.JLabel default_slots_down_label;
    private javax.swing.JSpinner default_slots_down_spinner;
    private javax.swing.JLabel default_slots_up_label;
    private javax.swing.JSpinner default_slots_up_spinner;
    private javax.swing.JButton delete_all_accounts_button;
    private javax.swing.JLabel down_dir_label;
    private javax.swing.JPanel downloads_panel;
    private javax.swing.JLabel elc_accounts_label;
    private javax.swing.JScrollPane elc_accounts_scrollpane;
    private javax.swing.JTable elc_accounts_table;
    private javax.swing.JCheckBox encrypt_pass_checkbox;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JSeparator jSeparator3;
    private javax.swing.JSeparator jSeparator4;
    private javax.swing.JSeparator jSeparator5;
    private javax.swing.JSeparator jSeparator6;
    private javax.swing.JSeparator jSeparator7;
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
    private javax.swing.JLabel mega_accounts_label;
    private javax.swing.JScrollPane mega_accounts_scrollpane;
    private javax.swing.JTable mega_accounts_table;
    private javax.swing.JCheckBox megacrypter_reverse_checkbox;
    private javax.swing.JLabel megacrypter_reverse_port_label;
    private javax.swing.JSpinner megacrypter_reverse_port_spinner;
    private javax.swing.JLabel megacrypter_reverse_warning_label;
    private javax.swing.JCheckBox multi_slot_down_checkbox;
    private javax.swing.JCheckBox multi_slot_up_checkbox;
    private javax.swing.JButton ok_button;
    private javax.swing.JPanel proxy_auth_panel;
    private javax.swing.JLabel proxy_host_label;
    private javax.swing.JTextField proxy_host_textfield;
    private javax.swing.JPanel proxy_panel;
    private javax.swing.JLabel proxy_pass_label;
    private javax.swing.JPasswordField proxy_pass_textfield;
    private javax.swing.JLabel proxy_port_label;
    private javax.swing.JTextField proxy_port_textfield;
    private javax.swing.JLabel proxy_user_label;
    private javax.swing.JTextField proxy_user_textfield;
    private javax.swing.JLabel proxy_warning_label;
    private javax.swing.JLabel rec_download_slots_label;
    private javax.swing.JLabel rec_upload_slots_label;
    private javax.swing.JButton remove_elc_account_button;
    private javax.swing.JButton remove_mega_account_button;
    private javax.swing.JLabel status;
    private javax.swing.JButton unlock_accounts_button;
    private javax.swing.JPanel uploads_panel;
    private javax.swing.JCheckBox use_mega_account_down_checkbox;
    private javax.swing.JComboBox<String> use_mega_account_down_combobox;
    private javax.swing.JLabel use_mega_label;
    private javax.swing.JCheckBox use_proxy_checkbox;
    private javax.swing.JCheckBox verify_file_down_checkbox;
    // End of variables declaration//GEN-END:variables
}
