package megabasterd;

import java.awt.Dialog;
import java.awt.Frame;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
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
import static megabasterd.DBTools.*;
import static megabasterd.MainPanel.*;
import static megabasterd.MiscTools.*;

/**
 *
 * @author tonikelope
 */
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
        return Collections.unmodifiableSet(_deleted_mega_accounts);
    }

    public Set<String> getDeleted_elc_accounts() {
        return Collections.unmodifiableSet(_deleted_elc_accounts);
    }

    public boolean isRemember_master_pass() {
        return _remember_master_pass;
    }

    public SettingsDialog(MainPanelView parent, boolean modal) {

        super(parent, modal);

        _main_panel = parent.getMain_panel();

        initComponents();

        updateFonts(this.getRootPane(), DEFAULT_FONT, _main_panel.getZoom_factor());

        smart_proxy_url_text.addMouseListener(new ContextMenuMouseListener());

        downloads_scroll_pane.getVerticalScrollBar().setUnitIncrement(20);

        downloads_scroll_pane.getHorizontalScrollBar().setUnitIncrement(20);

        String zoom_factor = DBTools.selectSettingValue("font_zoom");

        int int_zoom_factor = Math.round(_main_panel.getZoom_factor() * 100);

        if (zoom_factor != null) {
            int_zoom_factor = Integer.parseInt(zoom_factor);
        }

        zoom_spinner.setModel(new SpinnerNumberModel(int_zoom_factor, 50, 250, 10));
        ((JSpinner.DefaultEditor) zoom_spinner.getEditor()).getTextField().setEditable(false);

        String default_download_dir = DBTools.selectSettingValue("default_down_dir");

        if (default_download_dir == null) {
            default_download_dir = ".";
        }

        _download_path = default_download_dir;

        default_dir_label.setText(truncateText(_download_path, 80));

        String slots = DBTools.selectSettingValue("default_slots_down");

        int default_slots = Download.WORKERS_DEFAULT;

        if (slots != null) {
            default_slots = Integer.parseInt(slots);
        }

        default_slots_down_spinner.setModel(new SpinnerNumberModel(default_slots, Download.MIN_WORKERS, Download.MAX_WORKERS, 1));

        ((JSpinner.DefaultEditor) default_slots_down_spinner.getEditor()).getTextField().setEditable(false);

        slots = DBTools.selectSettingValue("default_slots_up");

        default_slots = Upload.WORKERS_DEFAULT;

        if (slots != null) {
            default_slots = Integer.parseInt(slots);
        }

        default_slots_up_spinner.setModel(new SpinnerNumberModel(default_slots, Upload.MIN_WORKERS, Upload.MAX_WORKERS, 1));
        ((JSpinner.DefaultEditor) default_slots_up_spinner.getEditor()).getTextField().setEditable(false);

        String max_down = DBTools.selectSettingValue("max_downloads");

        int max_dl = Download.SIM_TRANSFERENCES_DEFAULT;

        if (max_down != null) {
            max_dl = Integer.parseInt(max_down);
        }

        max_downloads_spinner.setModel(new SpinnerNumberModel(max_dl, 1, Download.MAX_SIM_TRANSFERENCES, 1));
        ((JSpinner.DefaultEditor) max_downloads_spinner.getEditor()).getTextField().setEditable(false);

        String max_up = DBTools.selectSettingValue("max_uploads");

        int max_ul = Upload.SIM_TRANSFERENCES_DEFAULT;

        if (max_up != null) {
            max_ul = Integer.parseInt(max_up);
        }

        max_uploads_spinner.setModel(new SpinnerNumberModel(max_ul, 1, Upload.MAX_SIM_TRANSFERENCES, 1));
        ((JSpinner.DefaultEditor) max_uploads_spinner.getEditor()).getTextField().setEditable(false);

        boolean limit_dl_speed = Download.LIMIT_TRANSFERENCE_SPEED_DEFAULT;

        String limit_download_speed = DBTools.selectSettingValue("limit_download_speed");

        if (limit_download_speed != null) {
            limit_dl_speed = limit_download_speed.equals("yes");
        }

        limit_download_speed_checkbox.setSelected(limit_dl_speed);

        max_down_speed_label.setEnabled(limit_dl_speed);

        max_down_speed_spinner.setEnabled(limit_dl_speed);

        String max_dl_speed = DBTools.selectSettingValue("max_download_speed");

        int max_download_speed = Download.MAX_TRANSFERENCE_SPEED_DEFAULT;

        if (max_dl_speed != null) {
            max_download_speed = Integer.parseInt(max_dl_speed);
        }

        max_down_speed_spinner.setModel(new SpinnerNumberModel(max_download_speed, 1, Integer.MAX_VALUE, 5));

        ((JSpinner.DefaultEditor) max_down_speed_spinner.getEditor()).getTextField().setEditable(true);

        boolean limit_ul_speed = Upload.LIMIT_TRANSFERENCE_SPEED_DEFAULT;

        String limit_upload_speed = DBTools.selectSettingValue("limit_upload_speed");

        if (limit_upload_speed != null) {
            limit_ul_speed = limit_upload_speed.equals("yes");
        }

        limit_upload_speed_checkbox.setSelected(limit_ul_speed);

        max_up_speed_label.setEnabled(limit_ul_speed);

        max_up_speed_spinner.setEnabled(limit_ul_speed);

        String max_ul_speed = DBTools.selectSettingValue("max_upload_speed");

        int max_upload_speed = Upload.MAX_TRANSFERENCE_SPEED_DEFAULT;

        if (max_ul_speed != null) {
            max_upload_speed = Integer.parseInt(max_ul_speed);
        }

        max_up_speed_spinner.setModel(new SpinnerNumberModel(max_upload_speed, 1, Integer.MAX_VALUE, 5));

        ((JSpinner.DefaultEditor) max_up_speed_spinner.getEditor()).getTextField().setEditable(true);

        boolean cbc_mac = Download.VERIFY_CBC_MAC_DEFAULT;

        String verify_file = DBTools.selectSettingValue("verify_down_file");

        if (verify_file != null) {
            cbc_mac = (verify_file.equals("yes"));
        }

        verify_file_down_checkbox.setSelected(cbc_mac);

        boolean use_slots = Download.USE_SLOTS_DEFAULT;

        String use_slots_val = DBTools.selectSettingValue("use_slots_down");

        if (use_slots_val != null) {
            use_slots = use_slots_val.equals("yes");
        }

        multi_slot_down_checkbox.setSelected(use_slots);

        default_slots_down_label.setEnabled(use_slots);
        default_slots_down_spinner.setEnabled(use_slots);
        rec_download_slots_label.setEnabled(use_slots);

        use_slots = Upload.USE_SLOTS_DEFAULT;

        use_slots_val = DBTools.selectSettingValue("use_slots_up");

        if (use_slots_val != null) {
            use_slots = use_slots_val.equals("yes");
        }

        multi_slot_up_checkbox.setSelected(use_slots);

        default_slots_up_label.setEnabled(use_slots);
        default_slots_up_spinner.setEnabled(use_slots);
        rec_upload_slots_label.setEnabled(use_slots);

        boolean use_mega_account = Download.USE_MEGA_ACCOUNT_DOWN;

        String use_mega_acc = DBTools.selectSettingValue("use_mega_account_down");

        String mega_account = null;

        if (use_mega_acc != null) {

            use_mega_account = use_mega_acc.equals("yes");

            mega_account = DBTools.selectSettingValue("mega_account_down");
        }

        if (use_mega_account) {

            use_mega_label.setEnabled(true);
            use_mega_account_down_checkbox.setSelected(true);
            use_mega_account_down_combobox.setEnabled(true);
            use_mega_account_down_combobox.setSelectedItem(mega_account);

        } else {

            use_mega_label.setEnabled(false);
            use_mega_account_down_checkbox.setSelected(false);
            use_mega_account_down_combobox.setEnabled(false);
        }

        DefaultTableModel mega_model = (DefaultTableModel) mega_accounts_table.getModel();

        DefaultTableModel elc_model = (DefaultTableModel) elc_accounts_table.getModel();

        encrypt_pass_checkbox.setSelected(_main_panel.getMaster_pass_hash() != null);

        remove_mega_account_button.setEnabled(mega_model.getRowCount() > 0);

        remove_elc_account_button.setEnabled(elc_model.getRowCount() > 0);

        if (_main_panel.getMaster_pass_hash() != null) {

            if (_main_panel.getMaster_pass() == null) {

                encrypt_pass_checkbox.setEnabled(false);

                remove_mega_account_button.setEnabled(false);

                remove_elc_account_button.setEnabled(false);

                add_mega_account_button.setEnabled(false);

                add_elc_account_button.setEnabled(false);

                unlock_accounts_button.setVisible(true);

                for (Object k : _main_panel.getMega_accounts().keySet()) {

                    String[] new_row_data = {(String) k, "**************************"};

                    mega_model.addRow(new_row_data);
                }

                for (Object k : _main_panel.getElc_accounts().keySet()) {

                    String[] new_row_data = {(String) k, "**************************", "**************************"};

                    elc_model.addRow(new_row_data);
                }

                mega_accounts_table.setEnabled(false);

                elc_accounts_table.setEnabled(false);

            } else {

                unlock_accounts_button.setVisible(false);

                for (Map.Entry pair : _main_panel.getMega_accounts().entrySet()) {

                    HashMap<String, Object> data = (HashMap) pair.getValue();

                    String pass = null;

                    try {

                        pass = new String(CryptTools.aes_cbc_decrypt_pkcs7(BASE642Bin((String) data.get("password")), _main_panel.getMaster_pass(), CryptTools.AES_ZERO_IV));

                    } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException ex) {
                        Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
                    } catch (Exception ex) {
                        Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
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
                        Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
                    } catch (Exception ex) {
                        Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
                    }

                    String[] new_row_data = {(String) pair.getKey(), user, apikey};

                    elc_model.addRow(new_row_data);
                }

                mega_model = (DefaultTableModel) mega_accounts_table.getModel();

                elc_model = (DefaultTableModel) elc_accounts_table.getModel();

                remove_mega_account_button.setEnabled(mega_model.getRowCount() > 0);

                remove_elc_account_button.setEnabled(elc_model.getRowCount() > 0);

            }

        } else {

            unlock_accounts_button.setVisible(false);

            for (Map.Entry pair : _main_panel.getMega_accounts().entrySet()) {

                HashMap<String, Object> data = (HashMap) pair.getValue();

                String[] new_row_data = {(String) pair.getKey(), (String) data.get("password")};

                mega_model.addRow(new_row_data);
            }

            for (Map.Entry pair : _main_panel.getElc_accounts().entrySet()) {

                HashMap<String, Object> data = (HashMap) pair.getValue();

                String[] new_row_data = {(String) pair.getKey(), (String) data.get("user"), (String) data.get("apikey")};

                elc_model.addRow(new_row_data);
            }

            remove_mega_account_button.setEnabled((mega_model.getRowCount() > 0));

            remove_elc_account_button.setEnabled((elc_model.getRowCount() > 0));

        }

        boolean use_mc_reverse = false;

        String megacrypter_reverse = DBTools.selectSettingValue("megacrypter_reverse");

        String megacrypter_reverse_p = String.valueOf(MainPanel.DEFAULT_MEGA_PROXY_PORT);

        if (megacrypter_reverse != null) {

            use_mc_reverse = megacrypter_reverse.equals("yes");

            if (megacrypter_reverse_p != null) {

                megacrypter_reverse_p = DBTools.selectSettingValue("megacrypter_reverse_port");
            }
        }

        if (use_mc_reverse) {

            megacrypter_reverse_checkbox.setSelected(true);
            megacrypter_reverse_port_spinner.setModel(new SpinnerNumberModel(Integer.parseInt(megacrypter_reverse_p), 1024, 65535, 1));
            ((JSpinner.DefaultEditor) megacrypter_reverse_port_spinner.getEditor()).getTextField().setEditable(true);
            megacrypter_reverse_port_spinner.setEnabled(true);
            megacrypter_reverse_warning_label.setEnabled(true);

        } else {

            megacrypter_reverse_checkbox.setSelected(false);
            megacrypter_reverse_port_spinner.setModel(new SpinnerNumberModel(Integer.parseInt(megacrypter_reverse_p), 1024, 65535, 1));
            ((JSpinner.DefaultEditor) megacrypter_reverse_port_spinner.getEditor()).getTextField().setEditable(true);
            megacrypter_reverse_port_label.setEnabled(false);
            megacrypter_reverse_port_spinner.setEnabled(false);
            megacrypter_reverse_warning_label.setEnabled(false);

        }

        boolean use_smart_proxy = false;

        String smart_proxy = DBTools.selectSettingValue("smart_proxy");

        String smart_proxy_url = "";

        if (smart_proxy != null) {

            use_smart_proxy = smart_proxy.equals("yes");

            smart_proxy_url = DBTools.selectSettingValue("smart_proxy_url");
        }

        if (use_smart_proxy) {

            smart_proxy_checkbox.setSelected(true);
            smart_proxy_url_label.setEnabled(true);
            smart_proxy_url_text.setEnabled(true);
            multi_slot_down_checkbox.setSelected(true);
            rec_smart_proxy_label.setEnabled(true);
            smart_proxy_url_text.setText(smart_proxy_url);

        } else {

            smart_proxy_checkbox.setSelected(false);
            smart_proxy_url_label.setEnabled(false);
            smart_proxy_url_text.setEnabled(false);
            rec_smart_proxy_label.setEnabled(false);
            smart_proxy_url_text.setText(smart_proxy_url);

        }

        boolean use_proxy = false;

        String use_proxy_val = DBTools.selectSettingValue("use_proxy");

        if (use_proxy_val != null) {
            use_proxy = (use_proxy_val.equals("yes"));
        }

        use_proxy_checkbox.setSelected(use_proxy);

        proxy_host_textfield.setText(DBTools.selectSettingValue("proxy_host"));

        proxy_port_textfield.setText(DBTools.selectSettingValue("proxy_port"));

        proxy_user_textfield.setText(DBTools.selectSettingValue("proxy_user"));

        proxy_pass_textfield.setText(DBTools.selectSettingValue("proxy_pass"));

        _remember_master_pass = true;

        _deleted_mega_accounts = new HashSet();

        _deleted_elc_accounts = new HashSet();

        _settings_ok = false;

        pack();

    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        save_button = new javax.swing.JButton();
        cancel_button = new javax.swing.JButton();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        downloads_panel = new javax.swing.JPanel();
        downloads_scroll_pane = new javax.swing.JScrollPane();
        jPanel3 = new javax.swing.JPanel();
        jSeparator8 = new javax.swing.JSeparator();
        megacrypter_reverse_warning_label = new javax.swing.JLabel();
        rec_download_slots_label = new javax.swing.JLabel();
        jSeparator3 = new javax.swing.JSeparator();
        megacrypter_reverse_checkbox = new javax.swing.JCheckBox();
        limit_download_speed_checkbox = new javax.swing.JCheckBox();
        jSeparator10 = new javax.swing.JSeparator();
        max_downloads_label = new javax.swing.JLabel();
        smart_proxy_checkbox = new javax.swing.JCheckBox();
        max_down_speed_spinner = new javax.swing.JSpinner();
        verify_file_down_checkbox = new javax.swing.JCheckBox();
        use_mega_account_down_checkbox = new javax.swing.JCheckBox();
        smart_proxy_url_label = new javax.swing.JLabel();
        max_downloads_spinner = new javax.swing.JSpinner();
        jSeparator7 = new javax.swing.JSeparator();
        jSeparator4 = new javax.swing.JSeparator();
        jSeparator2 = new javax.swing.JSeparator();
        use_mega_account_down_combobox = new javax.swing.JComboBox<>();
        jSeparator1 = new javax.swing.JSeparator();
        change_download_dir_button = new javax.swing.JButton();
        max_down_speed_label = new javax.swing.JLabel();
        megacrypter_reverse_port_label = new javax.swing.JLabel();
        default_dir_label = new javax.swing.JLabel();
        smart_proxy_url_text = new javax.swing.JTextField();
        default_slots_down_label = new javax.swing.JLabel();
        use_mega_label = new javax.swing.JLabel();
        multi_slot_down_checkbox = new javax.swing.JCheckBox();
        default_slots_down_spinner = new javax.swing.JSpinner();
        megacrypter_reverse_port_spinner = new javax.swing.JSpinner();
        down_dir_label = new javax.swing.JLabel();
        rec_smart_proxy_label = new javax.swing.JLabel();
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
        jLabel1 = new javax.swing.JLabel();
        advanced_panel = new javax.swing.JPanel();
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
        import_settings_button = new javax.swing.JButton();
        export_settings_button = new javax.swing.JButton();
        jSeparator9 = new javax.swing.JSeparator();
        zoom_label = new javax.swing.JLabel();
        zoom_spinner = new javax.swing.JSpinner();
        rec_zoom_label = new javax.swing.JLabel();
        jSeparator11 = new javax.swing.JSeparator();
        status = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Settings");

        save_button.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        save_button.setText("SAVE");
        save_button.setDoubleBuffered(true);
        save_button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                save_buttonActionPerformed(evt);
            }
        });

        cancel_button.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        cancel_button.setText("CANCEL");
        cancel_button.setDoubleBuffered(true);
        cancel_button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancel_buttonActionPerformed(evt);
            }
        });

        jTabbedPane1.setDoubleBuffered(true);
        jTabbedPane1.setFont(new java.awt.Font("Dialog", 1, 20)); // NOI18N

        downloads_scroll_pane.setBorder(null);

        megacrypter_reverse_warning_label.setFont(new java.awt.Font("Dialog", 2, 14)); // NOI18N
        megacrypter_reverse_warning_label.setText("Note: you MUST \"OPEN\" this port in your router/firewall.");

        rec_download_slots_label.setFont(new java.awt.Font("Dialog", 2, 14)); // NOI18N
        rec_download_slots_label.setText("Note: if you want to download without using a MEGA account (or using a FREE one) you SHOULD enable MULTI SLOT. ");

        megacrypter_reverse_checkbox.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        megacrypter_reverse_checkbox.setText("Use Megacrypter reverse mode");
        megacrypter_reverse_checkbox.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                megacrypter_reverse_checkboxStateChanged(evt);
            }
        });

        limit_download_speed_checkbox.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        limit_download_speed_checkbox.setText("Limit download speed");
        limit_download_speed_checkbox.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                limit_download_speed_checkboxStateChanged(evt);
            }
        });

        max_downloads_label.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        max_downloads_label.setText("Max parallel downloads:");
        max_downloads_label.setDoubleBuffered(true);

        smart_proxy_checkbox.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        smart_proxy_checkbox.setText("Use SmartProxy");
        smart_proxy_checkbox.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                smart_proxy_checkboxStateChanged(evt);
            }
        });

        max_down_speed_spinner.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N

        verify_file_down_checkbox.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        verify_file_down_checkbox.setText("Verify file integrity (when download is finished)");
        verify_file_down_checkbox.setDoubleBuffered(true);

        use_mega_account_down_checkbox.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        use_mega_account_down_checkbox.setText("Use MEGA accounts for download/stream");
        use_mega_account_down_checkbox.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                use_mega_account_down_checkboxStateChanged(evt);
            }
        });

        smart_proxy_url_label.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        smart_proxy_url_label.setText("URL:");

        max_downloads_spinner.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        max_downloads_spinner.setDoubleBuffered(true);

        use_mega_account_down_combobox.setFont(new java.awt.Font("Dialog", 0, 20)); // NOI18N

        change_download_dir_button.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        change_download_dir_button.setText("Change it");
        change_download_dir_button.setDoubleBuffered(true);
        change_download_dir_button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                change_download_dir_buttonActionPerformed(evt);
            }
        });

        max_down_speed_label.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        max_down_speed_label.setText("Max speed (KB/s):");

        megacrypter_reverse_port_label.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        megacrypter_reverse_port_label.setText("TCP Port:");

        default_dir_label.setFont(new java.awt.Font("Dialog", 2, 18)); // NOI18N
        default_dir_label.setText("default dir");

        smart_proxy_url_text.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N

        default_slots_down_label.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        default_slots_down_label.setText("Default slots per file:");
        default_slots_down_label.setDoubleBuffered(true);

        use_mega_label.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        use_mega_label.setText("Mega account (default):");

        multi_slot_down_checkbox.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        multi_slot_down_checkbox.setText("Use multi slot download mode (download restart required)");
        multi_slot_down_checkbox.setDoubleBuffered(true);
        multi_slot_down_checkbox.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                multi_slot_down_checkboxStateChanged(evt);
            }
        });

        default_slots_down_spinner.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        default_slots_down_spinner.setDoubleBuffered(true);
        default_slots_down_spinner.setValue(2);

        megacrypter_reverse_port_spinner.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N

        down_dir_label.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        down_dir_label.setText("Default downloads directory:");
        down_dir_label.setDoubleBuffered(true);

        rec_smart_proxy_label.setFont(new java.awt.Font("Dialog", 2, 14)); // NOI18N
        rec_smart_proxy_label.setText("Note: MULTI-SLOT REQUIRED. Be patient while MegaBasterd filters down proxies. MegaBasterd will try first to download chunk without proxy.");

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jSeparator1, javax.swing.GroupLayout.Alignment.TRAILING)
            .addComponent(jSeparator2, javax.swing.GroupLayout.Alignment.TRAILING)
            .addComponent(jSeparator10, javax.swing.GroupLayout.Alignment.TRAILING)
            .addComponent(jSeparator8, javax.swing.GroupLayout.Alignment.TRAILING)
            .addComponent(jSeparator7, javax.swing.GroupLayout.Alignment.TRAILING)
            .addComponent(jSeparator4)
            .addComponent(jSeparator3, javax.swing.GroupLayout.Alignment.TRAILING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGap(33, 33, 33)
                .addComponent(smart_proxy_url_label)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(smart_proxy_url_text))
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(max_downloads_label)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(max_downloads_spinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(down_dir_label)
                            .addGroup(jPanel3Layout.createSequentialGroup()
                                .addComponent(change_download_dir_button)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(default_dir_label))))
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addGap(27, 27, 27)
                        .addComponent(default_slots_down_label)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(default_slots_down_spinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(rec_download_slots_label)
                            .addComponent(multi_slot_down_checkbox)))
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addGap(27, 27, 27)
                        .addComponent(use_mega_label)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(use_mega_account_down_combobox, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addGap(27, 27, 27)
                        .addComponent(megacrypter_reverse_port_label)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(megacrypter_reverse_port_spinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(megacrypter_reverse_checkbox)
                            .addComponent(use_mega_account_down_checkbox)
                            .addComponent(verify_file_down_checkbox)
                            .addComponent(limit_download_speed_checkbox)
                            .addGroup(jPanel3Layout.createSequentialGroup()
                                .addGap(15, 15, 15)
                                .addComponent(max_down_speed_label)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(max_down_speed_spinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(megacrypter_reverse_warning_label)
                            .addComponent(smart_proxy_checkbox))))
                .addContainerGap(614, Short.MAX_VALUE))
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(rec_smart_proxy_label)
                .addGap(0, 0, Short.MAX_VALUE))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(down_dir_label)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(change_download_dir_button)
                    .addComponent(default_dir_label))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(max_downloads_label)
                    .addComponent(max_downloads_spinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator2, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(multi_slot_down_checkbox)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(default_slots_down_label)
                    .addComponent(default_slots_down_spinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(rec_download_slots_label)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator3, javax.swing.GroupLayout.PREFERRED_SIZE, 12, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(limit_download_speed_checkbox)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
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
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(use_mega_account_down_combobox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(use_mega_label))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jSeparator8, javax.swing.GroupLayout.PREFERRED_SIZE, 8, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(megacrypter_reverse_checkbox)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(megacrypter_reverse_port_spinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(megacrypter_reverse_port_label))
                .addGap(7, 7, 7)
                .addComponent(megacrypter_reverse_warning_label)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator10, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(smart_proxy_checkbox)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(smart_proxy_url_text, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(smart_proxy_url_label))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(rec_smart_proxy_label)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        downloads_scroll_pane.setViewportView(jPanel3);

        javax.swing.GroupLayout downloads_panelLayout = new javax.swing.GroupLayout(downloads_panel);
        downloads_panel.setLayout(downloads_panelLayout);
        downloads_panelLayout.setHorizontalGroup(
            downloads_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(downloads_panelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(downloads_scroll_pane, javax.swing.GroupLayout.DEFAULT_SIZE, 947, Short.MAX_VALUE)
                .addContainerGap())
        );
        downloads_panelLayout.setVerticalGroup(
            downloads_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(downloads_panelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(downloads_scroll_pane, javax.swing.GroupLayout.DEFAULT_SIZE, 473, Short.MAX_VALUE)
                .addContainerGap())
        );

        jTabbedPane1.addTab("Downloads", new javax.swing.ImageIcon(getClass().getResource("/megabasterd/icons/icons8-download-from-ftp-30.png")), downloads_panel); // NOI18N

        default_slots_up_label.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        default_slots_up_label.setText("Default slots per file:");
        default_slots_up_label.setDoubleBuffered(true);

        max_uploads_label.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        max_uploads_label.setText("Max parallel uploads:");
        max_uploads_label.setDoubleBuffered(true);

        default_slots_up_spinner.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        default_slots_up_spinner.setDoubleBuffered(true);
        default_slots_up_spinner.setValue(2);

        max_uploads_spinner.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        max_uploads_spinner.setDoubleBuffered(true);

        multi_slot_up_checkbox.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        multi_slot_up_checkbox.setText("Use multi slot upload mode (upload restart required)");
        multi_slot_up_checkbox.setDoubleBuffered(true);
        multi_slot_up_checkbox.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                multi_slot_up_checkboxStateChanged(evt);
            }
        });

        max_up_speed_label.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        max_up_speed_label.setText("Max speed (KB/s):");

        max_up_speed_spinner.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N

        limit_upload_speed_checkbox.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        limit_upload_speed_checkbox.setText("Limit upload speed");
        limit_upload_speed_checkbox.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                limit_upload_speed_checkboxStateChanged(evt);
            }
        });

        rec_upload_slots_label.setFont(new java.awt.Font("Dialog", 2, 14)); // NOI18N
        rec_upload_slots_label.setText("Note: MULTI-SLOT could be faster in certain situations but it might consume more CPU/RAM.");

        javax.swing.GroupLayout uploads_panelLayout = new javax.swing.GroupLayout(uploads_panel);
        uploads_panel.setLayout(uploads_panelLayout);
        uploads_panelLayout.setHorizontalGroup(
            uploads_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(uploads_panelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(uploads_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(uploads_panelLayout.createSequentialGroup()
                        .addGap(21, 21, 21)
                        .addComponent(default_slots_up_label)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(default_slots_up_spinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(uploads_panelLayout.createSequentialGroup()
                        .addGroup(uploads_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jSeparator5)
                            .addComponent(jSeparator6)
                            .addGroup(uploads_panelLayout.createSequentialGroup()
                                .addGroup(uploads_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(uploads_panelLayout.createSequentialGroup()
                                        .addComponent(max_uploads_label)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(max_uploads_spinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addComponent(rec_upload_slots_label)
                                    .addComponent(multi_slot_up_checkbox)
                                    .addGroup(uploads_panelLayout.createSequentialGroup()
                                        .addGap(21, 21, 21)
                                        .addComponent(max_up_speed_label)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(max_up_speed_spinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addComponent(limit_upload_speed_checkbox))
                                .addGap(0, 298, Short.MAX_VALUE)))
                        .addContainerGap())))
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
                    .addComponent(default_slots_up_label)
                    .addComponent(default_slots_up_spinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(rec_upload_slots_label)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator6, javax.swing.GroupLayout.PREFERRED_SIZE, 11, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(limit_upload_speed_checkbox)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(uploads_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(max_up_speed_label)
                    .addComponent(max_up_speed_spinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(277, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("Uploads", new javax.swing.ImageIcon(getClass().getResource("/megabasterd/icons/icons8-upload-to-ftp-30.png")), uploads_panel); // NOI18N

        mega_accounts_table.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
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
        mega_accounts_table.setRowHeight((int)(24*_main_panel.getZoom_factor()));
        mega_accounts_scrollpane.setViewportView(mega_accounts_table);

        mega_accounts_label.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        mega_accounts_label.setText("Your MEGA accounts:");
        mega_accounts_label.setDoubleBuffered(true);

        remove_mega_account_button.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        remove_mega_account_button.setText("Remove selected");
        remove_mega_account_button.setDoubleBuffered(true);
        remove_mega_account_button.setEnabled(false);
        remove_mega_account_button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                remove_mega_account_buttonActionPerformed(evt);
            }
        });

        add_mega_account_button.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        add_mega_account_button.setText("Add account");
        add_mega_account_button.setDoubleBuffered(true);
        add_mega_account_button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                add_mega_account_buttonActionPerformed(evt);
            }
        });

        encrypt_pass_checkbox.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        encrypt_pass_checkbox.setText("Encrypt on disk sensitive information");
        encrypt_pass_checkbox.setDoubleBuffered(true);
        encrypt_pass_checkbox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                encrypt_pass_checkboxActionPerformed(evt);
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

        unlock_accounts_button.setBackground(new java.awt.Color(0, 153, 51));
        unlock_accounts_button.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        unlock_accounts_button.setForeground(new java.awt.Color(255, 255, 255));
        unlock_accounts_button.setText("Unlock accounts");
        unlock_accounts_button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                unlock_accounts_buttonActionPerformed(evt);
            }
        });

        elc_accounts_scrollpane.setDoubleBuffered(true);

        elc_accounts_table.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
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
        elc_accounts_table.setRowHeight((int)(24*_main_panel.getZoom_factor()));
        elc_accounts_scrollpane.setViewportView(elc_accounts_table);

        elc_accounts_label.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        elc_accounts_label.setText("Your ELC accounts:");
        elc_accounts_label.setDoubleBuffered(true);

        remove_elc_account_button.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        remove_elc_account_button.setText("Remove selected");
        remove_elc_account_button.setDoubleBuffered(true);
        remove_elc_account_button.setEnabled(false);
        remove_elc_account_button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                remove_elc_account_buttonActionPerformed(evt);
            }
        });

        add_elc_account_button.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        add_elc_account_button.setText("Add account");
        add_elc_account_button.setDoubleBuffered(true);
        add_elc_account_button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                add_elc_account_buttonActionPerformed(evt);
            }
        });

        jLabel1.setFont(new java.awt.Font("Dialog", 2, 14)); // NOI18N
        jLabel1.setText("Note: you can use a (optional) alias for your email addresses -> bob@supermail.com#bob_mail");
        jLabel1.setDoubleBuffered(true);

        javax.swing.GroupLayout accounts_panelLayout = new javax.swing.GroupLayout(accounts_panel);
        accounts_panel.setLayout(accounts_panelLayout);
        accounts_panelLayout.setHorizontalGroup(
            accounts_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(accounts_panelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(accounts_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(mega_accounts_scrollpane)
                    .addGroup(accounts_panelLayout.createSequentialGroup()
                        .addComponent(delete_all_accounts_button)
                        .addGap(18, 18, 18)
                        .addComponent(unlock_accounts_button)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(encrypt_pass_checkbox))
                    .addGroup(accounts_panelLayout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addGap(193, 193, 193))
                    .addGroup(accounts_panelLayout.createSequentialGroup()
                        .addComponent(remove_mega_account_button)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(add_mega_account_button))
                    .addComponent(elc_accounts_scrollpane)
                    .addGroup(accounts_panelLayout.createSequentialGroup()
                        .addComponent(remove_elc_account_button)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(add_elc_account_button))
                    .addGroup(accounts_panelLayout.createSequentialGroup()
                        .addGroup(accounts_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(mega_accounts_label)
                            .addComponent(elc_accounts_label))
                        .addGap(0, 0, Short.MAX_VALUE)))
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
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(mega_accounts_scrollpane, javax.swing.GroupLayout.DEFAULT_SIZE, 134, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(accounts_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(remove_mega_account_button)
                    .addComponent(add_mega_account_button))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(elc_accounts_label)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(elc_accounts_scrollpane, javax.swing.GroupLayout.DEFAULT_SIZE, 134, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(accounts_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(remove_elc_account_button)
                    .addComponent(add_elc_account_button))
                .addContainerGap())
        );

        jTabbedPane1.addTab("Accounts", new javax.swing.ImageIcon(getClass().getResource("/megabasterd/icons/icons8-customer-30.png")), accounts_panel); // NOI18N

        proxy_panel.setBorder(javax.swing.BorderFactory.createTitledBorder("Proxy settings"));

        proxy_host_label.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        proxy_host_label.setText("Host:");
        proxy_host_label.setDoubleBuffered(true);
        proxy_host_label.setEnabled(false);

        proxy_host_textfield.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        proxy_host_textfield.setDoubleBuffered(true);
        proxy_host_textfield.setEnabled(false);
        proxy_host_textfield.addMouseListener(new ContextMenuMouseListener());

        proxy_port_label.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        proxy_port_label.setText("Port:");
        proxy_port_label.setDoubleBuffered(true);
        proxy_port_label.setEnabled(false);

        proxy_port_textfield.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        proxy_port_textfield.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        proxy_port_textfield.setDoubleBuffered(true);
        proxy_port_textfield.setEnabled(false);
        proxy_port_textfield.addMouseListener(new ContextMenuMouseListener());

        use_proxy_checkbox.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        use_proxy_checkbox.setText("Use HTTP(S) PROXY");
        use_proxy_checkbox.setDoubleBuffered(true);
        use_proxy_checkbox.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                use_proxy_checkboxStateChanged(evt);
            }
        });

        proxy_warning_label.setFont(new java.awt.Font("Dialog", 2, 14)); // NOI18N
        proxy_warning_label.setText("Note: MegaBasterd will use this proxy for ALL connections (restart required).");
        proxy_warning_label.setEnabled(false);

        proxy_auth_panel.setBorder(javax.swing.BorderFactory.createTitledBorder("Authentication"));

        proxy_user_label.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        proxy_user_label.setText("Username:");
        proxy_user_label.setDoubleBuffered(true);
        proxy_user_label.setEnabled(false);

        proxy_user_textfield.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        proxy_user_textfield.setDoubleBuffered(true);
        proxy_user_textfield.setEnabled(false);
        proxy_user_textfield.addMouseListener(new ContextMenuMouseListener());

        proxy_pass_label.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        proxy_pass_label.setText("Password:");
        proxy_pass_label.setDoubleBuffered(true);
        proxy_pass_label.setEnabled(false);

        proxy_pass_textfield.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
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
                .addComponent(proxy_user_textfield, javax.swing.GroupLayout.DEFAULT_SIZE, 239, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(proxy_pass_label)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(proxy_pass_textfield, javax.swing.GroupLayout.DEFAULT_SIZE, 401, Short.MAX_VALUE))
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
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, proxy_panelLayout.createSequentialGroup()
                                .addComponent(proxy_host_label)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(proxy_host_textfield)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(proxy_port_label)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(proxy_port_textfield, javax.swing.GroupLayout.PREFERRED_SIZE, 161, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(proxy_auth_panel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addContainerGap())
                    .addGroup(proxy_panelLayout.createSequentialGroup()
                        .addGroup(proxy_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(use_proxy_checkbox)
                            .addComponent(proxy_warning_label))
                        .addGap(0, 0, Short.MAX_VALUE))))
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

        import_settings_button.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        import_settings_button.setText("IMPORT SETTINGS");
        import_settings_button.setDoubleBuffered(true);
        import_settings_button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                import_settings_buttonActionPerformed(evt);
            }
        });

        export_settings_button.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        export_settings_button.setText("EXPORT SETTINGS");
        export_settings_button.setDoubleBuffered(true);
        export_settings_button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                export_settings_buttonActionPerformed(evt);
            }
        });

        zoom_label.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        zoom_label.setText("Font ZOOM (%):");
        zoom_label.setDoubleBuffered(true);

        zoom_spinner.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        zoom_spinner.setDoubleBuffered(true);

        rec_zoom_label.setFont(new java.awt.Font("Dialog", 2, 14)); // NOI18N
        rec_zoom_label.setText("Note: restart required.");
        rec_zoom_label.setDoubleBuffered(true);

        javax.swing.GroupLayout advanced_panelLayout = new javax.swing.GroupLayout(advanced_panel);
        advanced_panel.setLayout(advanced_panelLayout);
        advanced_panelLayout.setHorizontalGroup(
            advanced_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(advanced_panelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(advanced_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(advanced_panelLayout.createSequentialGroup()
                        .addGroup(advanced_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(advanced_panelLayout.createSequentialGroup()
                                .addComponent(zoom_label)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(zoom_spinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(rec_zoom_label))
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addComponent(proxy_panel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jSeparator9)
                    .addComponent(jSeparator11)
                    .addGroup(advanced_panelLayout.createSequentialGroup()
                        .addComponent(import_settings_button)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(export_settings_button)))
                .addContainerGap())
        );
        advanced_panelLayout.setVerticalGroup(
            advanced_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(advanced_panelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(proxy_panel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jSeparator9, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(5, 5, 5)
                .addGroup(advanced_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(zoom_label)
                    .addComponent(zoom_spinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(rec_zoom_label)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jSeparator11, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(advanced_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(import_settings_button)
                    .addComponent(export_settings_button))
                .addContainerGap(160, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("Advanced", new javax.swing.ImageIcon(getClass().getResource("/megabasterd/icons/icons8-administrative-tools-30.png")), advanced_panel); // NOI18N

        status.setFont(new java.awt.Font("Dialog", 3, 14)); // NOI18N
        status.setForeground(new java.awt.Color(102, 102, 102));

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
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(save_button)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cancel_button)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jTabbedPane1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(cancel_button)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(status)
                            .addComponent(save_button))
                        .addContainerGap())))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void cancel_buttonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancel_buttonActionPerformed

        setVisible(false);
    }//GEN-LAST:event_cancel_buttonActionPerformed

    private void save_buttonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_save_buttonActionPerformed

        try {

            _settings_ok = true;

            if (proxy_host_textfield.getText().isEmpty()) {

                use_proxy_checkbox.setSelected(false);
            }

            HashMap<String, Object> settings = new HashMap<>();

            settings.put("default_down_dir", _download_path);
            settings.put("default_slots_down", String.valueOf(default_slots_down_spinner.getValue()));
            settings.put("default_slots_up", String.valueOf(default_slots_up_spinner.getValue()));
            settings.put("use_slots_down", multi_slot_down_checkbox.isSelected() ? "yes" : "no");
            settings.put("use_slots_up", multi_slot_up_checkbox.isSelected() ? "yes" : "no");
            settings.put("max_downloads", String.valueOf(max_downloads_spinner.getValue()));
            settings.put("max_uploads", String.valueOf(max_uploads_spinner.getValue()));
            settings.put("verify_down_file", verify_file_down_checkbox.isSelected() ? "yes" : "no");
            settings.put("limit_download_speed", limit_download_speed_checkbox.isSelected() ? "yes" : "no");
            settings.put("max_download_speed", String.valueOf(max_down_speed_spinner.getValue()));
            settings.put("limit_upload_speed", limit_upload_speed_checkbox.isSelected() ? "yes" : "no");
            settings.put("max_upload_speed", String.valueOf(max_up_speed_spinner.getValue()));
            settings.put("use_mega_account_down", use_mega_account_down_checkbox.isSelected() ? "yes" : "no");
            settings.put("mega_account_down", use_mega_account_down_combobox.getSelectedItem());
            settings.put("megacrypter_reverse", megacrypter_reverse_checkbox.isSelected() ? "yes" : "no");
            settings.put("megacrypter_reverse_port", String.valueOf(megacrypter_reverse_port_spinner.getValue()));
            settings.put("smart_proxy", smart_proxy_checkbox.isSelected() ? "yes" : "no");
            settings.put("smart_proxy_url", smart_proxy_url_text.getText());

            String old_zoom = DBTools.selectSettingValue("font_zoom");

            if (old_zoom == null) {

                old_zoom = "100";
            }

            String zoom = String.valueOf(zoom_spinner.getValue());

            boolean old_use_proxy = false;

            String use_proxy_val = DBTools.selectSettingValue("use_proxy");

            if (use_proxy_val != null) {
                old_use_proxy = (use_proxy_val.equals("yes"));
            }

            boolean use_proxy = use_proxy_checkbox.isSelected();

            String old_proxy_host = DBTools.selectSettingValue("proxy_host");

            if (old_proxy_host == null) {

                old_proxy_host = "";
            }

            String proxy_host = proxy_host_textfield.getText().trim();

            String old_proxy_port = DBTools.selectSettingValue("proxy_port");

            if (old_proxy_port == null) {

                old_proxy_port = "";
            }

            String proxy_port = proxy_port_textfield.getText().trim();

            String old_proxy_user = DBTools.selectSettingValue("proxy_user");

            if (old_proxy_user == null) {

                old_proxy_user = "";
            }

            String proxy_user = proxy_user_textfield.getText().trim();

            String old_proxy_pass = DBTools.selectSettingValue("proxy_pass");

            if (old_proxy_pass == null) {

                old_proxy_pass = "";
            }

            String proxy_pass = new String(proxy_pass_textfield.getPassword());

            settings.put("use_proxy", use_proxy ? "yes" : "no");
            settings.put("proxy_host", proxy_host);
            settings.put("proxy_port", proxy_port);
            settings.put("proxy_user", proxy_user);
            settings.put("proxy_pass", proxy_pass);
            settings.put("font_zoom", zoom);

            insertSettingsValues(settings);

            if (!zoom.equals(old_zoom)
                    || use_proxy != old_use_proxy
                    || !proxy_host.equals(old_proxy_host)
                    || !proxy_port.equals(old_proxy_port)
                    || !proxy_user.equals(old_proxy_user)
                    || !proxy_pass.equals(old_proxy_pass)) {

                _main_panel.setRestart(true);
            }

            save_button.setEnabled(false);

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
                                    Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
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

                final DefaultTableModel model = (DefaultTableModel) mega_accounts_table.getModel();

                final int model_row_count = model.getRowCount();

                status.setText("Checking your MEGA accounts, please wait...");

                save_button.setEnabled(false);

                cancel_button.setEnabled(false);

                remove_mega_account_button.setEnabled(false);

                remove_elc_account_button.setEnabled(false);

                add_mega_account_button.setEnabled(false);

                add_elc_account_button.setEnabled(false);

                delete_all_accounts_button.setEnabled(false);

                mega_accounts_table.setEnabled(false);

                elc_accounts_table.setEnabled(false);

                encrypt_pass_checkbox.setEnabled(false);

                pack();

                final Dialog tthis = this;

                THREAD_POOL.execute(new Runnable() {
                    @Override
                    public void run() {

                        ArrayList<String> email_error = new ArrayList<>();

                        for (int i = 0; i < model_row_count; i++) {

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

                                            user_hash = Bin2BASE64(CryptTools.aes_cbc_encrypt_pkcs7(UrlBASE642Bin(ma.getUser_hash()), _main_panel.getMaster_pass(), CryptTools.AES_ZERO_IV));
                                        }

                                        DBTools.insertMegaAccount(email, password, password_aes, user_hash);

                                    } catch (Exception ex) {

                                        email_error.add(email);
                                        Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
                                    }

                                } else {

                                    HashMap<String, Object> mega_account_data = (HashMap) _main_panel.getMega_accounts().get(email);

                                    String password = (String) mega_account_data.get("password");

                                    if (_main_panel.getMaster_pass() != null) {

                                        try {

                                            password = new String(CryptTools.aes_cbc_decrypt_pkcs7(BASE642Bin(password), _main_panel.getMaster_pass(), CryptTools.AES_ZERO_IV));

                                        } catch (Exception ex) {
                                            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
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

                                                user_hash = Bin2BASE64(CryptTools.aes_cbc_encrypt_pkcs7(UrlBASE642Bin(ma.getUser_hash()), _main_panel.getMaster_pass(), CryptTools.AES_ZERO_IV));
                                            }

                                            DBTools.insertMegaAccount(email, password, password_aes, user_hash);

                                        } catch (Exception ex) {

                                            email_error.add(email);
                                            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);

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

                            final String final_email_error = email_error_s;

                            swingInvoke(new Runnable() {
                                @Override
                                public void run() {

                                    status.setText("");

                                    pack();

                                    JOptionPane.showMessageDialog(tthis, "There were errors with some accounts. Please, check them:\n\n" + final_email_error, "Error", JOptionPane.ERROR_MESSAGE);

                                    save_button.setEnabled(true);

                                    cancel_button.setEnabled(true);

                                    remove_mega_account_button.setEnabled(mega_accounts_table.getModel().getRowCount() > 0);

                                    remove_elc_account_button.setEnabled(elc_accounts_table.getModel().getRowCount() > 0);

                                    add_mega_account_button.setEnabled(true);

                                    add_elc_account_button.setEnabled(true);

                                    mega_accounts_table.setEnabled(true);

                                    elc_accounts_table.setEnabled(true);

                                    delete_all_accounts_button.setEnabled(true);

                                    encrypt_pass_checkbox.setEnabled(true);

                                }
                            });

                        } else {
                            swingInvoke(new Runnable() {
                                @Override
                                public void run() {
                                    status.setText("");
                                    JOptionPane.showMessageDialog(tthis, "Settings successfully saved!", "Settings saved", JOptionPane.INFORMATION_MESSAGE);
                                    setVisible(false);
                                }
                            });
                        }
                    }
                });

            } else {

                JOptionPane.showMessageDialog(this, "Settings successfully saved!", "Settings saved", JOptionPane.INFORMATION_MESSAGE);
                setVisible(false);
            }

        } catch (SQLException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
        } catch (Exception ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_save_buttonActionPerformed

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

    private void encrypt_pass_checkboxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_encrypt_pass_checkboxActionPerformed

        encrypt_pass_checkbox.setEnabled(false);

        final Dialog tthis = this;

        swingInvoke(new Runnable() {
            @Override
            public void run() {

                SetMasterPasswordDialog dialog = new SetMasterPasswordDialog((Frame) getParent(), true, _main_panel.getMaster_pass_salt(), _main_panel);

                dialog.setLocationRelativeTo(tthis);

                dialog.setVisible(true);

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

                        insertSettingValue("master_pass_hash", _main_panel.getMaster_pass_hash());

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
                        Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
                    }

                }

                encrypt_pass_checkbox.setSelected((_main_panel.getMaster_pass_hash() != null));

                dialog.dispose();

                encrypt_pass_checkbox.setEnabled(true);

            }
        });

    }//GEN-LAST:event_encrypt_pass_checkboxActionPerformed

    private void unlock_accounts_buttonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_unlock_accounts_buttonActionPerformed

        unlock_accounts_button.setEnabled(false);

        final Dialog tthis = this;

        swingInvoke(new Runnable() {
            @Override
            public void run() {

                GetMasterPasswordDialog dialog = new GetMasterPasswordDialog((Frame) getParent(), true, _main_panel.getMaster_pass_hash(), _main_panel.getMaster_pass_salt(), _main_panel);

                dialog.setLocationRelativeTo(tthis);

                dialog.setVisible(true);

                if (dialog.isPass_ok()) {

                    _main_panel.setMaster_pass(dialog.getPass());

                    dialog.deletePass();

                    DefaultTableModel mega_model = new DefaultTableModel(new Object[][]{}, new String[]{"Email", "Password"});

                    DefaultTableModel elc_model = new DefaultTableModel(new Object[][]{}, new String[]{"Host", "User", "API KEY"});

                    mega_accounts_table.setModel(mega_model);

                    elc_accounts_table.setModel(elc_model);

                    encrypt_pass_checkbox.setEnabled(true);

                    mega_accounts_table.setEnabled(true);

                    elc_accounts_table.setEnabled(true);

                    remove_mega_account_button.setEnabled(true);

                    remove_elc_account_button.setEnabled(true);

                    add_mega_account_button.setEnabled(true);

                    add_elc_account_button.setEnabled(true);

                    unlock_accounts_button.setVisible(false);

                    delete_all_accounts_button.setEnabled(true);

                    for (Map.Entry pair : _main_panel.getMega_accounts().entrySet()) {

                        HashMap<String, Object> data = (HashMap) pair.getValue();

                        String pass = null;

                        try {

                            pass = new String(CryptTools.aes_cbc_decrypt_pkcs7(BASE642Bin((String) data.get("password")), _main_panel.getMaster_pass(), CryptTools.AES_ZERO_IV));

                        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException ex) {
                            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
                        } catch (Exception ex) {
                            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
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
                            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
                        } catch (Exception ex) {
                            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
                        }

                        String[] new_row_data = {(String) pair.getKey(), user, apikey};

                        elc_model.addRow(new_row_data);
                    }

                }

                _remember_master_pass = dialog.getRemember_checkbox().isSelected();

                dialog.dispose();

                unlock_accounts_button.setEnabled(true);

            }
        });

    }//GEN-LAST:event_unlock_accounts_buttonActionPerformed

    private void delete_all_accounts_buttonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_delete_all_accounts_buttonActionPerformed

        Object[] options = {"No",
            "Yes"};

        int n = showOptionDialog(this,
                "Master password will be reset and all your accounts will be removed. (THIS CAN'T BE UNDONE)\n\nDo you want to continue?",
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
                        Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
                    }
                }

                for (Map.Entry pair : _main_panel.getElc_accounts().entrySet()) {

                    try {
                        DBTools.deleteELCAccount((String) pair.getKey());
                    } catch (SQLException ex) {
                        Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
                    }
                }

                _main_panel.setMaster_pass_hash(null);

                _main_panel.setMaster_pass(null);

                insertSettingValue("master_pass_hash", null);

                encrypt_pass_checkbox.setSelected(false);

                _main_panel.getMega_accounts().clear();

                _main_panel.getMega_active_accounts().clear();

                _main_panel.getElc_accounts().clear();

            } catch (SQLException ex) {
                Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
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

        proxy_host_label.setEnabled(use_proxy_checkbox.isSelected());
        proxy_host_textfield.setEnabled(use_proxy_checkbox.isSelected());
        proxy_port_label.setEnabled(use_proxy_checkbox.isSelected());
        proxy_port_textfield.setEnabled(use_proxy_checkbox.isSelected());
        proxy_user_label.setEnabled(use_proxy_checkbox.isSelected());
        proxy_user_textfield.setEnabled(use_proxy_checkbox.isSelected());
        proxy_pass_label.setEnabled(use_proxy_checkbox.isSelected());
        proxy_pass_textfield.setEnabled(use_proxy_checkbox.isSelected());
        proxy_warning_label.setEnabled(use_proxy_checkbox.isSelected());
    }//GEN-LAST:event_use_proxy_checkboxStateChanged

    private void limit_upload_speed_checkboxStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_limit_upload_speed_checkboxStateChanged

        max_up_speed_label.setEnabled(limit_upload_speed_checkbox.isSelected());
        max_up_speed_spinner.setEnabled(limit_upload_speed_checkbox.isSelected());
    }//GEN-LAST:event_limit_upload_speed_checkboxStateChanged

    private void multi_slot_up_checkboxStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_multi_slot_up_checkboxStateChanged
        // TODO add your handling code here:
        if (!multi_slot_up_checkbox.isSelected()) {

            default_slots_up_spinner.setEnabled(false);
            default_slots_up_label.setEnabled(false);
            rec_upload_slots_label.setEnabled(false);

        } else {

            default_slots_up_spinner.setEnabled(true);
            default_slots_up_label.setEnabled(true);
            multi_slot_up_checkbox.setSelected(true);
            rec_upload_slots_label.setEnabled(true);
        }
    }//GEN-LAST:event_multi_slot_up_checkboxStateChanged

    private void import_settings_buttonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_import_settings_buttonActionPerformed

        JFileChooser filechooser = new JFileChooser();
        filechooser.setCurrentDirectory(new File(_download_path));
        filechooser.setDialogTitle("Select settings file");

        if (filechooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {

            File file = filechooser.getSelectedFile();

            try {

                try (FileInputStream fis = new FileInputStream(file); ObjectInputStream ois = new ObjectInputStream(fis)) {

                    HashMap<String, Object> settings = (HashMap<String, Object>) ois.readObject();

                    insertSettingsValues((HashMap<String, Object>) settings.get("settings"));

                    insertMegaAccounts((HashMap<String, Object>) settings.get("mega_accounts"));

                    insertELCAccounts((HashMap<String, Object>) settings.get("elc_accounts"));

                    _main_panel.loadUserSettings();

                    JOptionPane.showMessageDialog(this, "Settings successfully imported!", "Settings imported", JOptionPane.INFORMATION_MESSAGE);

                    setVisible(false);

                } catch (SQLException | ClassNotFoundException ex) {
                    Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
                }

            } catch (IOException ex) {
                Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
            }
        }

    }//GEN-LAST:event_import_settings_buttonActionPerformed

    private void export_settings_buttonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_export_settings_buttonActionPerformed

        JOptionPane.showMessageDialog(this, "Only SAVED settings will be exported!", "Warning", JOptionPane.WARNING_MESSAGE);

        JFileChooser filechooser = new JFileChooser();
        filechooser.setCurrentDirectory(new File(_download_path));
        filechooser.setDialogTitle("Save as");

        if (filechooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {

            File file = filechooser.getSelectedFile();

            try {

                if (file.exists()) {
                    file.delete();
                }

                file.createNewFile();

                try (FileOutputStream fos = new FileOutputStream(file); ObjectOutputStream oos = new ObjectOutputStream(fos)) {

                    HashMap<String, Object> settings = new HashMap<>();

                    settings.put("settings", selectSettingsValues());

                    settings.put("mega_accounts", selectMegaAccounts());

                    settings.put("elc_accounts", selectELCAccounts());

                    oos.writeObject(settings);

                    JOptionPane.showMessageDialog(this, "Settings successfully exported!", "Settings exported", JOptionPane.INFORMATION_MESSAGE);

                    setVisible(false);

                } catch (SQLException ex) {
                    Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
                }

            } catch (IOException ex) {
                Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
            }
        }
    }//GEN-LAST:event_export_settings_buttonActionPerformed

    private void multi_slot_down_checkboxStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_multi_slot_down_checkboxStateChanged

        if (!multi_slot_down_checkbox.isSelected() && !smart_proxy_checkbox.isSelected()) {

            default_slots_down_spinner.setEnabled(false);
            default_slots_down_label.setEnabled(false);
            rec_download_slots_label.setEnabled(false);

        } else {

            default_slots_down_spinner.setEnabled(true);
            default_slots_down_label.setEnabled(true);
            multi_slot_down_checkbox.setSelected(true);
            rec_download_slots_label.setEnabled(true);
        }
    }//GEN-LAST:event_multi_slot_down_checkboxStateChanged

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

        pack();
    }//GEN-LAST:event_change_download_dir_buttonActionPerformed

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

                String use_mega_account_down = DBTools.selectSettingValue("mega_account_down");

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

    private void smart_proxy_checkboxStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_smart_proxy_checkboxStateChanged

        if (smart_proxy_checkbox.isSelected()) {

            smart_proxy_url_label.setEnabled(true);
            smart_proxy_url_text.setEnabled(true);
            rec_smart_proxy_label.setEnabled(true);
            multi_slot_down_checkbox.setSelected(true);

        } else {
            smart_proxy_url_label.setEnabled(false);
            smart_proxy_url_text.setEnabled(false);
            rec_smart_proxy_label.setEnabled(false);
        }
    }//GEN-LAST:event_smart_proxy_checkboxStateChanged

    private void limit_download_speed_checkboxStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_limit_download_speed_checkboxStateChanged

        if (!limit_download_speed_checkbox.isSelected()) {

            max_down_speed_label.setEnabled(false);
            max_down_speed_spinner.setEnabled(false);
        } else {
            max_down_speed_label.setEnabled(true);
            max_down_speed_spinner.setEnabled(true);
        }
    }//GEN-LAST:event_limit_download_speed_checkboxStateChanged

    private void megacrypter_reverse_checkboxStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_megacrypter_reverse_checkboxStateChanged

        megacrypter_reverse_port_label.setEnabled(megacrypter_reverse_checkbox.isSelected());
        megacrypter_reverse_port_spinner.setEnabled(megacrypter_reverse_checkbox.isSelected());
        megacrypter_reverse_warning_label.setEnabled(megacrypter_reverse_checkbox.isSelected());
    }//GEN-LAST:event_megacrypter_reverse_checkboxStateChanged

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel accounts_panel;
    private javax.swing.JButton add_elc_account_button;
    private javax.swing.JButton add_mega_account_button;
    private javax.swing.JPanel advanced_panel;
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
    private javax.swing.JScrollPane downloads_scroll_pane;
    private javax.swing.JLabel elc_accounts_label;
    private javax.swing.JScrollPane elc_accounts_scrollpane;
    private javax.swing.JTable elc_accounts_table;
    private javax.swing.JCheckBox encrypt_pass_checkbox;
    private javax.swing.JButton export_settings_button;
    private javax.swing.JButton import_settings_button;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator10;
    private javax.swing.JSeparator jSeparator11;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JSeparator jSeparator3;
    private javax.swing.JSeparator jSeparator4;
    private javax.swing.JSeparator jSeparator5;
    private javax.swing.JSeparator jSeparator6;
    private javax.swing.JSeparator jSeparator7;
    private javax.swing.JSeparator jSeparator8;
    private javax.swing.JSeparator jSeparator9;
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
    private javax.swing.JLabel rec_smart_proxy_label;
    private javax.swing.JLabel rec_upload_slots_label;
    private javax.swing.JLabel rec_zoom_label;
    private javax.swing.JButton remove_elc_account_button;
    private javax.swing.JButton remove_mega_account_button;
    private javax.swing.JButton save_button;
    private javax.swing.JCheckBox smart_proxy_checkbox;
    private javax.swing.JLabel smart_proxy_url_label;
    private javax.swing.JTextField smart_proxy_url_text;
    private javax.swing.JLabel status;
    private javax.swing.JButton unlock_accounts_button;
    private javax.swing.JPanel uploads_panel;
    private javax.swing.JCheckBox use_mega_account_down_checkbox;
    private javax.swing.JComboBox<String> use_mega_account_down_combobox;
    private javax.swing.JLabel use_mega_label;
    private javax.swing.JCheckBox use_proxy_checkbox;
    private javax.swing.JCheckBox verify_file_down_checkbox;
    private javax.swing.JLabel zoom_label;
    private javax.swing.JSpinner zoom_spinner;
    // End of variables declaration//GEN-END:variables
}
