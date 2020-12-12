package com.tonikelope.megabasterd;

import static com.tonikelope.megabasterd.DBTools.*;
import static com.tonikelope.megabasterd.MainPanel.*;
import static com.tonikelope.megabasterd.MiscTools.*;
import java.awt.Dialog;
import java.awt.Frame;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Paths;
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
import javax.swing.DefaultRowSorter;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import static javax.swing.JOptionPane.YES_NO_CANCEL_OPTION;
import static javax.swing.JOptionPane.showOptionDialog;
import javax.swing.JSpinner;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.SpinnerNumberModel;
import static javax.swing.WindowConstants.DISPOSE_ON_CLOSE;
import javax.swing.table.DefaultTableModel;

/**
 *
 * @author tonikelope
 */
public class SettingsDialog extends javax.swing.JDialog {

    public static final String DEFAULT_SMART_PROXY_URL = "https://raw.githubusercontent.com/tonikelope/megabasterd/proxy_list/proxy_list.txt";
    private String _download_path;
    private String _custom_chunks_dir;
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

        _remember_master_pass = true;

        _deleted_mega_accounts = new HashSet();

        _deleted_elc_accounts = new HashSet();

        _settings_ok = false;

        MiscTools.GUIRunAndWait(() -> {

            initComponents();

            updateFonts(this, GUI_FONT, _main_panel.getZoom_factor());

            updateTitledBorderFont(((javax.swing.border.TitledBorder) proxy_panel.getBorder()), GUI_FONT, _main_panel.getZoom_factor());

            updateTitledBorderFont(((javax.swing.border.TitledBorder) proxy_auth_panel.getBorder()), GUI_FONT, _main_panel.getZoom_factor());

            translateLabels(this);

            panel_tabs.setTitleAt(0, LabelTranslatorSingleton.getInstance().translate("Downloads"));

            panel_tabs.setTitleAt(1, LabelTranslatorSingleton.getInstance().translate("Uploads"));

            panel_tabs.setTitleAt(2, LabelTranslatorSingleton.getInstance().translate("Accounts"));

            panel_tabs.setTitleAt(3, LabelTranslatorSingleton.getInstance().translate("Advanced"));

            downloads_scrollpane.getVerticalScrollBar().setUnitIncrement(20);

            downloads_scrollpane.getHorizontalScrollBar().setUnitIncrement(20);

            uploads_scrollpane.getVerticalScrollBar().setUnitIncrement(20);

            uploads_scrollpane.getHorizontalScrollBar().setUnitIncrement(20);

            advanced_scrollpane.getVerticalScrollBar().setUnitIncrement(20);

            advanced_scrollpane.getHorizontalScrollBar().setUnitIncrement(20);

            String zoom_factor = DBTools.selectSettingValue("font_zoom");

            int int_zoom_factor = Math.round(_main_panel.getZoom_factor() * 100);

            if (zoom_factor != null) {
                int_zoom_factor = Integer.parseInt(zoom_factor);
            }

            zoom_spinner.setModel(new SpinnerNumberModel(int_zoom_factor, 50, 250, 10));
            ((JSpinner.DefaultEditor) zoom_spinner.getEditor()).getTextField().setEditable(false);

            String use_custom_chunks_dir = DBTools.selectSettingValue("use_custom_chunks_dir");

            if (use_custom_chunks_dir != null) {

                if (use_custom_chunks_dir.equals("yes")) {

                    _custom_chunks_dir = DBTools.selectSettingValue("custom_chunks_dir");

                    custom_chunks_dir_current_label.setText(_custom_chunks_dir != null ? truncateText(_custom_chunks_dir, 80) : "");

                    custom_chunks_dir_checkbox.setSelected(true);

                    custom_chunks_dir_button.setEnabled(true);

                } else {

                    _custom_chunks_dir = DBTools.selectSettingValue("custom_chunks_dir");

                    custom_chunks_dir_current_label.setText(_custom_chunks_dir != null ? truncateText(_custom_chunks_dir, 80) : "");

                    custom_chunks_dir_checkbox.setSelected(false);

                    custom_chunks_dir_button.setEnabled(false);

                    custom_chunks_dir_current_label.setEnabled(false);
                }

            } else {

                _custom_chunks_dir = null;

                custom_chunks_dir_current_label.setText("");

                custom_chunks_dir_checkbox.setSelected(false);

                custom_chunks_dir_button.setEnabled(false);

                custom_chunks_dir_current_label.setEnabled(false);
            }

            String default_download_dir = DBTools.selectSettingValue("default_down_dir");

            default_download_dir = Paths.get(default_download_dir == null ? System.getProperty("user.home") : default_download_dir).toAbsolutePath().normalize().toString();

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

            use_mega_label.setEnabled(use_mega_account);
            use_mega_account_down_checkbox.setSelected(use_mega_account);
            use_mega_account_down_combobox.setEnabled(use_mega_account);
            use_mega_account_down_combobox.setSelectedItem(mega_account);

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

                            pass = new String(CryptTools.aes_cbc_decrypt_pkcs7(BASE642Bin((String) data.get("password")), _main_panel.getMaster_pass(), CryptTools.AES_ZERO_IV), "UTF-8");

                        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException ex) {
                            LOG.log(Level.SEVERE, ex.getMessage());
                        } catch (Exception ex) {
                            LOG.log(Level.SEVERE, ex.getMessage());
                        }

                        String[] new_row_data = {(String) pair.getKey(), pass};

                        mega_model.addRow(new_row_data);
                    }

                    for (Map.Entry pair : _main_panel.getElc_accounts().entrySet()) {

                        HashMap<String, Object> data = (HashMap) pair.getValue();

                        String user = null, apikey = null;

                        try {

                            user = new String(CryptTools.aes_cbc_decrypt_pkcs7(BASE642Bin((String) data.get("user")), _main_panel.getMaster_pass(), CryptTools.AES_ZERO_IV), "UTF-8");

                            apikey = new String(CryptTools.aes_cbc_decrypt_pkcs7(BASE642Bin((String) data.get("apikey")), _main_panel.getMaster_pass(), CryptTools.AES_ZERO_IV), "UTF-8");

                        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException ex) {
                            LOG.log(Level.SEVERE, ex.getMessage());
                        } catch (Exception ex) {
                            LOG.log(Level.SEVERE, ex.getMessage());
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

            mega_accounts_table.setAutoCreateRowSorter(true);
            DefaultRowSorter sorter_mega = ((DefaultRowSorter) mega_accounts_table.getRowSorter());
            ArrayList list_mega = new ArrayList();
            list_mega.add(new RowSorter.SortKey(0, SortOrder.ASCENDING));
            sorter_mega.setSortKeys(list_mega);
            sorter_mega.sort();

            elc_accounts_table.setAutoCreateRowSorter(true);
            DefaultRowSorter sorter_elc = ((DefaultRowSorter) elc_accounts_table.getRowSorter());
            ArrayList list_elc = new ArrayList();
            list_elc.add(new RowSorter.SortKey(0, SortOrder.ASCENDING));
            sorter_elc.setSortKeys(list_elc);
            sorter_elc.sort();

            boolean use_mc_reverse = false;

            String megacrypter_reverse = DBTools.selectSettingValue("megacrypter_reverse");

            String megacrypter_reverse_p = String.valueOf(MainPanel.DEFAULT_MEGA_PROXY_PORT);

            if (megacrypter_reverse != null) {

                use_mc_reverse = megacrypter_reverse.equals("yes");

                if (megacrypter_reverse_p != null) {

                    megacrypter_reverse_p = DBTools.selectSettingValue("megacrypter_reverse_port");
                }
            }

            megacrypter_reverse_checkbox.setSelected(use_mc_reverse);
            megacrypter_reverse_port_spinner.setModel(new SpinnerNumberModel(Integer.parseInt(megacrypter_reverse_p), 1024, 65535, 1));
            ((JSpinner.DefaultEditor) megacrypter_reverse_port_spinner.getEditor()).getTextField().setEditable(use_mc_reverse);
            megacrypter_reverse_port_spinner.setEnabled(use_mc_reverse);
            megacrypter_reverse_warning_label.setEnabled(use_mc_reverse);

            boolean use_smart_proxy = false;

            String smart_proxy = DBTools.selectSettingValue("smart_proxy");

            if (smart_proxy != null) {

                use_smart_proxy = smart_proxy.equals("yes");
            }

            smart_proxy_checkbox.setSelected(use_smart_proxy);
            rec_smart_proxy_label.setEnabled(use_smart_proxy);
            rec_smart_proxy_label1.setEnabled(use_smart_proxy);
            custom_proxy_list_label.setEnabled(use_smart_proxy);
            custom_proxy_textarea.setEnabled(use_smart_proxy);

            boolean run_command = false;

            String run_command_string = DBTools.selectSettingValue("run_command");

            if (run_command_string != null) {

                run_command = run_command_string.equals("yes");
            }

            run_command_checkbox.setSelected(run_command);

            run_command_textbox.setEnabled(run_command);

            run_command_textbox.setText(DBTools.selectSettingValue("run_command_path"));

            boolean init_paused = false;

            String init_paused_string = DBTools.selectSettingValue("start_frozen");

            if (init_paused_string != null) {

                init_paused = init_paused_string.equals("yes");
            }

            start_frozen_checkbox.setSelected(init_paused);

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

            boolean debug_file = false;

            String debug_file_val = DBTools.selectSettingValue("debug_file");

            if (debug_file_val != null) {
                debug_file = (debug_file_val.equals("yes"));
            }

            debug_file_checkbox.setSelected(debug_file);

            String font = DBTools.selectSettingValue("font");

            this.font_combo.addItem(LabelTranslatorSingleton.getInstance().translate("DEFAULT"));

            this.font_combo.addItem(LabelTranslatorSingleton.getInstance().translate("ALTERNATIVE"));

            if (font == null) {
                this.font_combo.setSelectedItem(LabelTranslatorSingleton.getInstance().translate("DEFAULT"));
            } else {
                this.font_combo.setSelectedItem(LabelTranslatorSingleton.getInstance().translate(font));
            }

            String language = DBTools.selectSettingValue("language");

            this.language_combo.addItem(LabelTranslatorSingleton.getInstance().translate("English"));

            this.language_combo.addItem(LabelTranslatorSingleton.getInstance().translate("Spanish"));

            if (language == null) {
                language = MainPanel.DEFAULT_LANGUAGE;
            }

            if (language.equals("EN")) {
                this.language_combo.setSelectedItem(LabelTranslatorSingleton.getInstance().translate("English"));
            } else if (language.equals("ES")) {
                this.language_combo.setSelectedItem(LabelTranslatorSingleton.getInstance().translate("Spanish"));
            }

            String custom_proxy_list = DBTools.selectSettingValue("custom_proxy_list");

            if (custom_proxy_list != null) {
                custom_proxy_textarea.setText(custom_proxy_list);
            }

            pack();
        });

    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jProgressBar1 = new javax.swing.JProgressBar();
        save_button = new javax.swing.JButton();
        cancel_button = new javax.swing.JButton();
        panel_tabs = new javax.swing.JTabbedPane();
        downloads_scrollpane = new javax.swing.JScrollPane();
        downloads_panel = new javax.swing.JPanel();
        megacrypter_reverse_warning_label = new javax.swing.JLabel();
        rec_download_slots_label = new javax.swing.JLabel();
        megacrypter_reverse_checkbox = new javax.swing.JCheckBox();
        limit_download_speed_checkbox = new javax.swing.JCheckBox();
        max_downloads_label = new javax.swing.JLabel();
        smart_proxy_checkbox = new javax.swing.JCheckBox();
        max_down_speed_spinner = new javax.swing.JSpinner();
        verify_file_down_checkbox = new javax.swing.JCheckBox();
        use_mega_account_down_checkbox = new javax.swing.JCheckBox();
        max_downloads_spinner = new javax.swing.JSpinner();
        use_mega_account_down_combobox = new javax.swing.JComboBox<>();
        change_download_dir_button = new javax.swing.JButton();
        max_down_speed_label = new javax.swing.JLabel();
        megacrypter_reverse_port_label = new javax.swing.JLabel();
        default_dir_label = new javax.swing.JLabel();
        default_slots_down_label = new javax.swing.JLabel();
        use_mega_label = new javax.swing.JLabel();
        multi_slot_down_checkbox = new javax.swing.JCheckBox();
        default_slots_down_spinner = new javax.swing.JSpinner();
        megacrypter_reverse_port_spinner = new javax.swing.JSpinner();
        down_dir_label = new javax.swing.JLabel();
        rec_smart_proxy_label = new javax.swing.JLabel();
        jSeparator5 = new javax.swing.JSeparator();
        jSeparator6 = new javax.swing.JSeparator();
        jSeparator7 = new javax.swing.JSeparator();
        jSeparator8 = new javax.swing.JSeparator();
        jSeparator9 = new javax.swing.JSeparator();
        jSeparator10 = new javax.swing.JSeparator();
        jSeparator11 = new javax.swing.JSeparator();
        jScrollPane1 = new javax.swing.JScrollPane();
        custom_proxy_textarea = new javax.swing.JTextArea();
        custom_proxy_list_label = new javax.swing.JLabel();
        rec_smart_proxy_label1 = new javax.swing.JLabel();
        uploads_scrollpane = new javax.swing.JScrollPane();
        uploads_panel = new javax.swing.JPanel();
        default_slots_up_label = new javax.swing.JLabel();
        max_uploads_label = new javax.swing.JLabel();
        default_slots_up_spinner = new javax.swing.JSpinner();
        max_uploads_spinner = new javax.swing.JSpinner();
        max_up_speed_label = new javax.swing.JLabel();
        max_up_speed_spinner = new javax.swing.JSpinner();
        limit_upload_speed_checkbox = new javax.swing.JCheckBox();
        rec_upload_slots_label = new javax.swing.JLabel();
        jSeparator3 = new javax.swing.JSeparator();
        jSeparator4 = new javax.swing.JSeparator();
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
        advanced_scrollpane = new javax.swing.JScrollPane();
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
        zoom_label = new javax.swing.JLabel();
        zoom_spinner = new javax.swing.JSpinner();
        rec_zoom_label = new javax.swing.JLabel();
        jButton1 = new javax.swing.JButton();
        jSeparator1 = new javax.swing.JSeparator();
        jLabel2 = new javax.swing.JLabel();
        language_combo = new javax.swing.JComboBox<>();
        font_label = new javax.swing.JLabel();
        font_combo = new javax.swing.JComboBox<>();
        custom_chunks_dir_button = new javax.swing.JButton();
        custom_chunks_dir_current_label = new javax.swing.JLabel();
        custom_chunks_dir_checkbox = new javax.swing.JCheckBox();
        jSeparator12 = new javax.swing.JSeparator();
        start_frozen_checkbox = new javax.swing.JCheckBox();
        jSeparator15 = new javax.swing.JSeparator();
        run_command_checkbox = new javax.swing.JCheckBox();
        run_command_textbox = new javax.swing.JTextField();
        run_command_textbox.addMouseListener(new ContextMenuMouseListener());
        run_command_test_button = new javax.swing.JButton();
        debug_file_checkbox = new javax.swing.JCheckBox();
        jSeparator2 = new javax.swing.JSeparator();
        status = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Settings");

        save_button.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        save_button.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/icons8-save-all-30.png"))); // NOI18N
        save_button.setText("SAVE");
        save_button.setDoubleBuffered(true);
        save_button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                save_buttonActionPerformed(evt);
            }
        });

        cancel_button.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        cancel_button.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/icons8-cancel-30.png"))); // NOI18N
        cancel_button.setText("CANCEL");
        cancel_button.setDoubleBuffered(true);
        cancel_button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancel_buttonActionPerformed(evt);
            }
        });

        panel_tabs.setDoubleBuffered(true);
        panel_tabs.setFont(new java.awt.Font("Dialog", 1, 24)); // NOI18N

        downloads_scrollpane.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153)));

        megacrypter_reverse_warning_label.setFont(new java.awt.Font("Dialog", 0, 14)); // NOI18N
        megacrypter_reverse_warning_label.setText("Note: you MUST \"OPEN\" this port in your router/firewall.");

        rec_download_slots_label.setFont(new java.awt.Font("Dialog", 0, 14)); // NOI18N
        rec_download_slots_label.setText("Note: if you want to download without using a MEGA PREMIUM account you SHOULD enable it. (Slots consume RAM, so use them moderately).");

        megacrypter_reverse_checkbox.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        megacrypter_reverse_checkbox.setText("Use MegaCrypter reverse mode");
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
        smart_proxy_checkbox.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                smart_proxy_checkboxMouseClicked(evt);
            }
        });

        max_down_speed_spinner.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N

        verify_file_down_checkbox.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        verify_file_down_checkbox.setText("Verify file integrity (when download is finished)");
        verify_file_down_checkbox.setDoubleBuffered(true);

        use_mega_account_down_checkbox.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        use_mega_account_down_checkbox.setText("Allow using MEGA accounts for download/streaming");
        use_mega_account_down_checkbox.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                use_mega_account_down_checkboxStateChanged(evt);
            }
        });

        max_downloads_spinner.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        max_downloads_spinner.setDoubleBuffered(true);

        use_mega_account_down_combobox.setFont(new java.awt.Font("Dialog", 0, 20)); // NOI18N

        change_download_dir_button.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        change_download_dir_button.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/icons8-folder-30.png"))); // NOI18N
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

        default_dir_label.setBackground(new java.awt.Color(153, 255, 153));
        default_dir_label.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        default_dir_label.setText("default dir");
        default_dir_label.setOpaque(true);

        default_slots_down_label.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        default_slots_down_label.setText("Default slots per file:");
        default_slots_down_label.setDoubleBuffered(true);

        use_mega_label.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        use_mega_label.setText("Default account:");

        multi_slot_down_checkbox.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        multi_slot_down_checkbox.setText("Use multi slot download mode");
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
        down_dir_label.setText("Download folder:");
        down_dir_label.setDoubleBuffered(true);

        rec_smart_proxy_label.setFont(new java.awt.Font("Dialog", 2, 14)); // NOI18N
        rec_smart_proxy_label.setText("Note: enable it in order to mitigate bandwidth limit. (Multi slot required).");

        custom_proxy_textarea.setColumns(20);
        custom_proxy_textarea.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        custom_proxy_textarea.setRows(5);
        custom_proxy_textarea.setDoubleBuffered(true);
        jScrollPane1.setViewportView(custom_proxy_textarea);
        custom_proxy_textarea.addMouseListener(new ContextMenuMouseListener());

        custom_proxy_list_label.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        custom_proxy_list_label.setText("Use this proxy list Format is [*]IP:PORT[@user_b64:password_b64]");

        rec_smart_proxy_label1.setFont(new java.awt.Font("Dialog", 1, 14)); // NOI18N
        rec_smart_proxy_label1.setText("WARNING: Using proxies or VPN to bypass MEGA's daily download limitation may violate its Terms of Use. USE THIS OPTION AT YOUR OWN RISK.");

        javax.swing.GroupLayout downloads_panelLayout = new javax.swing.GroupLayout(downloads_panel);
        downloads_panel.setLayout(downloads_panelLayout);
        downloads_panelLayout.setHorizontalGroup(
            downloads_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(downloads_panelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(downloads_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jSeparator11)
                    .addComponent(jSeparator9)
                    .addComponent(jSeparator8)
                    .addComponent(jSeparator7)
                    .addComponent(jSeparator6)
                    .addComponent(jSeparator5)
                    .addComponent(jSeparator10)
                    .addGroup(downloads_panelLayout.createSequentialGroup()
                        .addGroup(downloads_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(smart_proxy_checkbox)
                            .addGroup(downloads_panelLayout.createSequentialGroup()
                                .addComponent(max_downloads_label)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(max_downloads_spinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(multi_slot_down_checkbox)
                            .addComponent(limit_download_speed_checkbox)
                            .addGroup(downloads_panelLayout.createSequentialGroup()
                                .addGap(21, 21, 21)
                                .addGroup(downloads_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(rec_download_slots_label)
                                    .addGroup(downloads_panelLayout.createSequentialGroup()
                                        .addComponent(default_slots_down_label)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(default_slots_down_spinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addGroup(downloads_panelLayout.createSequentialGroup()
                                        .addComponent(max_down_speed_label)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(max_down_speed_spinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addGroup(downloads_panelLayout.createSequentialGroup()
                                        .addComponent(use_mega_label)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(use_mega_account_down_combobox, javax.swing.GroupLayout.PREFERRED_SIZE, 700, javax.swing.GroupLayout.PREFERRED_SIZE))))
                            .addGroup(downloads_panelLayout.createSequentialGroup()
                                .addGap(26, 26, 26)
                                .addGroup(downloads_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                    .addComponent(rec_smart_proxy_label, javax.swing.GroupLayout.PREFERRED_SIZE, 542, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(custom_proxy_list_label, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(jScrollPane1)
                                    .addComponent(rec_smart_proxy_label1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                            .addGroup(downloads_panelLayout.createSequentialGroup()
                                .addComponent(change_download_dir_button)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(down_dir_label)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(default_dir_label))
                            .addComponent(megacrypter_reverse_checkbox)
                            .addComponent(use_mega_account_down_checkbox)
                            .addComponent(verify_file_down_checkbox)
                            .addGroup(downloads_panelLayout.createSequentialGroup()
                                .addGap(21, 21, 21)
                                .addGroup(downloads_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(megacrypter_reverse_warning_label)
                                    .addGroup(downloads_panelLayout.createSequentialGroup()
                                        .addComponent(megacrypter_reverse_port_label)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(megacrypter_reverse_port_spinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))))
                        .addGap(0, 42, Short.MAX_VALUE)))
                .addContainerGap())
        );
        downloads_panelLayout.setVerticalGroup(
            downloads_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(downloads_panelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(downloads_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(down_dir_label)
                    .addComponent(default_dir_label)
                    .addComponent(change_download_dir_button))
                .addGap(18, 18, 18)
                .addComponent(jSeparator5, javax.swing.GroupLayout.PREFERRED_SIZE, 2, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(downloads_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(max_downloads_label)
                    .addComponent(max_downloads_spinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addComponent(jSeparator6, javax.swing.GroupLayout.PREFERRED_SIZE, 2, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(multi_slot_down_checkbox)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(downloads_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(default_slots_down_label)
                    .addComponent(default_slots_down_spinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(rec_download_slots_label)
                .addGap(18, 18, 18)
                .addComponent(jSeparator7, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(limit_download_speed_checkbox)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(downloads_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(max_down_speed_spinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(max_down_speed_label))
                .addGap(18, 18, 18)
                .addComponent(jSeparator8, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(verify_file_down_checkbox)
                .addGap(18, 18, 18)
                .addComponent(jSeparator9, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(12, 12, 12)
                .addComponent(use_mega_account_down_checkbox)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(downloads_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(use_mega_account_down_combobox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(use_mega_label))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jSeparator10, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(megacrypter_reverse_checkbox)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(downloads_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(megacrypter_reverse_port_spinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(megacrypter_reverse_port_label))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(megacrypter_reverse_warning_label)
                .addGap(18, 18, 18)
                .addComponent(jSeparator11, javax.swing.GroupLayout.PREFERRED_SIZE, 2, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(smart_proxy_checkbox)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(rec_smart_proxy_label)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(rec_smart_proxy_label1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(custom_proxy_list_label)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 318, Short.MAX_VALUE)
                .addContainerGap())
        );

        downloads_scrollpane.setViewportView(downloads_panel);

        panel_tabs.addTab("Downloads", new javax.swing.ImageIcon(getClass().getResource("/images/icons8-download-from-ftp-30.png")), downloads_scrollpane); // NOI18N

        uploads_scrollpane.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153)));

        default_slots_up_label.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
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

        rec_upload_slots_label.setFont(new java.awt.Font("Dialog", 0, 14)); // NOI18N
        rec_upload_slots_label.setText("Note: slots consume RAM, so use them moderately.");

        javax.swing.GroupLayout uploads_panelLayout = new javax.swing.GroupLayout(uploads_panel);
        uploads_panel.setLayout(uploads_panelLayout);
        uploads_panelLayout.setHorizontalGroup(
            uploads_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(uploads_panelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(uploads_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jSeparator3, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(rec_upload_slots_label, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jSeparator4)
                    .addGroup(uploads_panelLayout.createSequentialGroup()
                        .addGap(20, 20, 20)
                        .addComponent(max_up_speed_label)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(max_up_speed_spinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(limit_upload_speed_checkbox)
                    .addGroup(uploads_panelLayout.createSequentialGroup()
                        .addGroup(uploads_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(default_slots_up_label, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(max_uploads_label, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(uploads_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(default_slots_up_spinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(max_uploads_spinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addContainerGap())
        );
        uploads_panelLayout.setVerticalGroup(
            uploads_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(uploads_panelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(uploads_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(max_uploads_label)
                    .addComponent(max_uploads_spinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addComponent(jSeparator3, javax.swing.GroupLayout.PREFERRED_SIZE, 2, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(uploads_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(default_slots_up_label)
                    .addComponent(default_slots_up_spinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(rec_upload_slots_label)
                .addGap(18, 18, 18)
                .addComponent(jSeparator4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(limit_upload_speed_checkbox)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(uploads_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(max_up_speed_label)
                    .addComponent(max_up_speed_spinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        uploads_scrollpane.setViewportView(uploads_panel);

        panel_tabs.addTab("Uploads", new javax.swing.ImageIcon(getClass().getResource("/images/icons8-upload-to-ftp-30.png")), uploads_scrollpane); // NOI18N

        accounts_panel.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153)));

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
        mega_accounts_table.setDoubleBuffered(true);
        mega_accounts_table.setRowHeight((int)(24*_main_panel.getZoom_factor()));
        mega_accounts_scrollpane.setViewportView(mega_accounts_table);

        mega_accounts_label.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        mega_accounts_label.setText("Your MEGA accounts:");
        mega_accounts_label.setDoubleBuffered(true);

        remove_mega_account_button.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        remove_mega_account_button.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/icons8-trash-can-30.png"))); // NOI18N
        remove_mega_account_button.setText("Remove selected");
        remove_mega_account_button.setDoubleBuffered(true);
        remove_mega_account_button.setEnabled(false);
        remove_mega_account_button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                remove_mega_account_buttonActionPerformed(evt);
            }
        });

        add_mega_account_button.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        add_mega_account_button.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/icons8-plus-30.png"))); // NOI18N
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
        delete_all_accounts_button.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/icons8-trash-can-30.png"))); // NOI18N
        delete_all_accounts_button.setText("RESET ACCOUNTS");
        delete_all_accounts_button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                delete_all_accounts_buttonActionPerformed(evt);
            }
        });

        unlock_accounts_button.setBackground(new java.awt.Color(0, 153, 51));
        unlock_accounts_button.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        unlock_accounts_button.setForeground(new java.awt.Color(255, 255, 255));
        unlock_accounts_button.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/icons8-key-2-30.png"))); // NOI18N
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
        elc_accounts_table.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        elc_accounts_table.setDoubleBuffered(true);
        elc_accounts_table.setRowHeight((int)(24*_main_panel.getZoom_factor()));
        elc_accounts_scrollpane.setViewportView(elc_accounts_table);

        elc_accounts_label.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        elc_accounts_label.setText("Your ELC accounts:");
        elc_accounts_label.setDoubleBuffered(true);

        remove_elc_account_button.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        remove_elc_account_button.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/icons8-trash-can-30.png"))); // NOI18N
        remove_elc_account_button.setText("Remove selected");
        remove_elc_account_button.setDoubleBuffered(true);
        remove_elc_account_button.setEnabled(false);
        remove_elc_account_button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                remove_elc_account_buttonActionPerformed(evt);
            }
        });

        add_elc_account_button.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        add_elc_account_button.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/icons8-plus-30.png"))); // NOI18N
        add_elc_account_button.setText("Add account");
        add_elc_account_button.setDoubleBuffered(true);
        add_elc_account_button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                add_elc_account_buttonActionPerformed(evt);
            }
        });

        jLabel1.setFont(new java.awt.Font("Dialog", 0, 14)); // NOI18N
        jLabel1.setText("Note: you can use a (optional) alias for your email addresses -> bob@supermail.com#bob_mail (don't forget to save after entering your accounts).");
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
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(unlock_accounts_button)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(encrypt_pass_checkbox))
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
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addComponent(jLabel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
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
                .addComponent(mega_accounts_scrollpane, javax.swing.GroupLayout.DEFAULT_SIZE, 254, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(accounts_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(remove_mega_account_button)
                    .addComponent(add_mega_account_button))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(elc_accounts_label)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(elc_accounts_scrollpane, javax.swing.GroupLayout.DEFAULT_SIZE, 255, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(accounts_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(remove_elc_account_button)
                    .addComponent(add_elc_account_button))
                .addContainerGap())
        );

        panel_tabs.addTab("Accounts", new javax.swing.ImageIcon(getClass().getResource("/images/icons8-customer-30.png")), accounts_panel); // NOI18N

        advanced_scrollpane.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153)));

        proxy_panel.setBorder(javax.swing.BorderFactory.createTitledBorder((String)null));

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
        proxy_port_textfield.setHorizontalAlignment(javax.swing.JTextField.LEFT);
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

        proxy_warning_label.setFont(new java.awt.Font("Dialog", 0, 14)); // NOI18N
        proxy_warning_label.setText("Note: MegaBasterd will use this proxy for ALL connections.");
        proxy_warning_label.setEnabled(false);

        proxy_auth_panel.setBorder(javax.swing.BorderFactory.createTitledBorder((String)null));

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
                .addGap(6, 6, 6)
                .addComponent(proxy_user_textfield, javax.swing.GroupLayout.DEFAULT_SIZE, 297, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(proxy_pass_label)
                .addGap(6, 6, 6)
                .addComponent(proxy_pass_textfield, javax.swing.GroupLayout.DEFAULT_SIZE, 461, Short.MAX_VALUE)
                .addContainerGap())
        );
        proxy_auth_panelLayout.setVerticalGroup(
            proxy_auth_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(proxy_auth_panelLayout.createSequentialGroup()
                .addGap(0, 12, Short.MAX_VALUE)
                .addGroup(proxy_auth_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(proxy_user_label)
                    .addComponent(proxy_user_textfield, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(proxy_pass_label)
                    .addComponent(proxy_pass_textfield, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        javax.swing.GroupLayout proxy_panelLayout = new javax.swing.GroupLayout(proxy_panel);
        proxy_panel.setLayout(proxy_panelLayout);
        proxy_panelLayout.setHorizontalGroup(
            proxy_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(proxy_panelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(proxy_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(proxy_panelLayout.createSequentialGroup()
                        .addComponent(use_proxy_checkbox)
                        .addGap(0, 0, Short.MAX_VALUE))
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
                            .addComponent(proxy_auth_panel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(proxy_warning_label, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
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
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 18, Short.MAX_VALUE)
                .addComponent(proxy_auth_panel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        import_settings_button.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        import_settings_button.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/icons8-import-30.png"))); // NOI18N
        import_settings_button.setText("IMPORT SETTINGS");
        import_settings_button.setDoubleBuffered(true);
        import_settings_button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                import_settings_buttonActionPerformed(evt);
            }
        });

        export_settings_button.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        export_settings_button.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/icons8-export-30.png"))); // NOI18N
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

        rec_zoom_label.setFont(new java.awt.Font("Dialog", 0, 14)); // NOI18N
        rec_zoom_label.setText("Note: restart might be required.");
        rec_zoom_label.setDoubleBuffered(true);

        jButton1.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        jButton1.setForeground(new java.awt.Color(255, 0, 0));
        jButton1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/icons8-cancel-30.png"))); // NOI18N
        jButton1.setText("RESET MEGABASTERD");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        jLabel2.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        jLabel2.setText("Language:");

        language_combo.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N

        font_label.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        font_label.setText("Font:");

        font_combo.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N

        custom_chunks_dir_button.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        custom_chunks_dir_button.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/icons8-folder-30.png"))); // NOI18N
        custom_chunks_dir_button.setText("Change it");
        custom_chunks_dir_button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                custom_chunks_dir_buttonActionPerformed(evt);
            }
        });

        custom_chunks_dir_current_label.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N

        custom_chunks_dir_checkbox.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        custom_chunks_dir_checkbox.setText("Use custom temporary directory for chunks storage");
        custom_chunks_dir_checkbox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                custom_chunks_dir_checkboxActionPerformed(evt);
            }
        });

        start_frozen_checkbox.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        start_frozen_checkbox.setText("Freeze transferences before start");

        run_command_checkbox.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        run_command_checkbox.setText("Execute this command when MEGA download limit is reached:");
        run_command_checkbox.setDoubleBuffered(true);
        run_command_checkbox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                run_command_checkboxActionPerformed(evt);
            }
        });

        run_command_textbox.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        run_command_textbox.setDoubleBuffered(true);
        run_command_textbox.setEnabled(false);

        run_command_test_button.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        run_command_test_button.setText("Test");
        run_command_test_button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                run_command_test_buttonActionPerformed(evt);
            }
        });

        debug_file_checkbox.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        debug_file_checkbox.setText("Save debug info to file");

        javax.swing.GroupLayout advanced_panelLayout = new javax.swing.GroupLayout(advanced_panel);
        advanced_panel.setLayout(advanced_panelLayout);
        advanced_panelLayout.setHorizontalGroup(
            advanced_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(advanced_panelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(advanced_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jSeparator15, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jSeparator12, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jSeparator1)
                    .addGroup(advanced_panelLayout.createSequentialGroup()
                        .addGroup(advanced_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(advanced_panelLayout.createSequentialGroup()
                                .addComponent(font_label)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(font_combo, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                            .addGroup(advanced_panelLayout.createSequentialGroup()
                                .addComponent(zoom_label)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(zoom_spinner, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(0, 237, Short.MAX_VALUE))
                            .addGroup(advanced_panelLayout.createSequentialGroup()
                                .addComponent(jLabel2)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(language_combo, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                        .addGap(18, 18, 18)
                        .addGroup(advanced_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addGroup(advanced_panelLayout.createSequentialGroup()
                                .addComponent(import_settings_button)
                                .addGap(18, 18, 18)
                                .addComponent(export_settings_button))
                            .addComponent(jButton1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                    .addComponent(run_command_textbox, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(proxy_panel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(advanced_panelLayout.createSequentialGroup()
                        .addGroup(advanced_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(debug_file_checkbox)
                            .addComponent(start_frozen_checkbox)
                            .addComponent(custom_chunks_dir_checkbox)
                            .addGroup(advanced_panelLayout.createSequentialGroup()
                                .addComponent(custom_chunks_dir_button)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(custom_chunks_dir_current_label))
                            .addComponent(rec_zoom_label)
                            .addGroup(advanced_panelLayout.createSequentialGroup()
                                .addComponent(run_command_checkbox)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(run_command_test_button)))
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addComponent(jSeparator2))
                .addContainerGap())
        );
        advanced_panelLayout.setVerticalGroup(
            advanced_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(advanced_panelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(advanced_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(font_label)
                    .addComponent(font_combo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(import_settings_button)
                    .addComponent(export_settings_button))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(advanced_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(zoom_label)
                    .addComponent(zoom_spinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(advanced_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(language_combo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButton1))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, 8, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(custom_chunks_dir_checkbox)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(advanced_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(custom_chunks_dir_button)
                    .addComponent(custom_chunks_dir_current_label))
                .addGap(18, 18, 18)
                .addComponent(jSeparator12, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(start_frozen_checkbox)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jSeparator15, javax.swing.GroupLayout.PREFERRED_SIZE, 8, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(debug_file_checkbox)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jSeparator2, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(advanced_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(run_command_checkbox)
                    .addComponent(run_command_test_button))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(run_command_textbox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(proxy_panel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(rec_zoom_label)
                .addGap(34, 34, 34))
        );

        advanced_scrollpane.setViewportView(advanced_panel);

        panel_tabs.addTab("Advanced", new javax.swing.ImageIcon(getClass().getResource("/images/icons8-administrative-tools-30.png")), advanced_scrollpane); // NOI18N

        status.setFont(new java.awt.Font("Dialog", 3, 14)); // NOI18N
        status.setForeground(new java.awt.Color(102, 102, 102));

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(panel_tabs, javax.swing.GroupLayout.DEFAULT_SIZE, 1071, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(status, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(6, 6, 6)
                        .addComponent(save_button)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(cancel_button)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(panel_tabs, javax.swing.GroupLayout.DEFAULT_SIZE, 805, Short.MAX_VALUE)
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(cancel_button)
                        .addComponent(save_button))
                    .addComponent(status, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void cancel_buttonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancel_buttonActionPerformed

        setVisible(false);
    }//GEN-LAST:event_cancel_buttonActionPerformed

    private void save_buttonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_save_buttonActionPerformed

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

        panel_tabs.setEnabled(false);

        try {

            if (proxy_host_textfield.getText().isEmpty()) {

                use_proxy_checkbox.setSelected(false);
            }

            HashMap<String, Object> settings = new HashMap<>();

            settings.put("default_down_dir", _download_path);
            settings.put("default_slots_down", String.valueOf(default_slots_down_spinner.getValue()));
            settings.put("default_slots_up", String.valueOf(default_slots_up_spinner.getValue()));
            settings.put("use_slots_down", multi_slot_down_checkbox.isSelected() ? "yes" : "no");
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
            settings.put("start_frozen", start_frozen_checkbox.isSelected() ? "yes" : "no");
            settings.put("use_custom_chunks_dir", custom_chunks_dir_checkbox.isSelected() ? "yes" : "no");
            settings.put("debug_file", debug_file_checkbox.isSelected() ? "yes" : "no");
            settings.put("custom_chunks_dir", _custom_chunks_dir);
            settings.put("custom_proxy_list", custom_proxy_textarea.getText());
            settings.put("run_command", run_command_checkbox.isSelected() ? "yes" : "no");
            settings.put("run_command_path", run_command_textbox.getText());

            String old_font = DBTools.selectSettingValue("font");

            if (old_font == null) {
                old_font = "DEFAULT";
            }

            String font = (String) font_combo.getSelectedItem();

            if (font.equals(LabelTranslatorSingleton.getInstance().translate("DEFAULT"))) {
                font = "DEFAULT";
            } else if (font.equals(LabelTranslatorSingleton.getInstance().translate("ALTERNATIVE"))) {
                font = "ALTERNATIVE";
            }

            settings.put("font", font);

            String old_language = DBTools.selectSettingValue("language");

            if (old_language == null) {
                old_language = MainPanel.DEFAULT_LANGUAGE;
            }

            String language = (String) language_combo.getSelectedItem();

            if (language.equals(LabelTranslatorSingleton.getInstance().translate("English"))) {
                language = "EN";
            } else if (language.equals(LabelTranslatorSingleton.getInstance().translate("Spanish"))) {
                language = "ES";
            }

            settings.put("language", language);

            String old_zoom = DBTools.selectSettingValue("font_zoom");

            if (old_zoom == null) {

                old_zoom = String.valueOf(Math.round(100 * MainPanel.ZOOM_FACTOR));
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

            if (!font.equals(old_font)
                    || !language.equals(old_language)
                    || !zoom.equals(old_zoom)
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

                                user_table = Bin2BASE64(CryptTools.aes_cbc_encrypt_pkcs7(user_table.getBytes("UTF-8"), _main_panel.getMaster_pass(), CryptTools.AES_ZERO_IV));

                                apikey_table = Bin2BASE64(CryptTools.aes_cbc_encrypt_pkcs7(apikey_table.getBytes("UTF-8"), _main_panel.getMaster_pass(), CryptTools.AES_ZERO_IV));
                            }

                            DBTools.insertELCAccount(host_table, user_table, apikey_table);

                        } else {

                            HashMap<String, Object> elc_account_data = (HashMap) _main_panel.getElc_accounts().get(host_table);

                            String user = (String) elc_account_data.get("user");

                            String apikey = (String) elc_account_data.get("apikey");

                            if (_main_panel.getMaster_pass() != null) {

                                try {

                                    user = new String(CryptTools.aes_cbc_decrypt_pkcs7(BASE642Bin(user), _main_panel.getMaster_pass(), CryptTools.AES_ZERO_IV), "UTF-8");

                                    apikey = new String(CryptTools.aes_cbc_decrypt_pkcs7(BASE642Bin(apikey), _main_panel.getMaster_pass(), CryptTools.AES_ZERO_IV), "UTF-8");

                                } catch (Exception ex) {
                                    LOG.log(Level.SEVERE, ex.getMessage());
                                }
                            }

                            if (!user.equals(user_table) || !apikey.equals(apikey_table)) {

                                user = user_table;

                                apikey = apikey_table;

                                if (_main_panel.getMaster_pass() != null) {

                                    user = Bin2BASE64(CryptTools.aes_cbc_encrypt_pkcs7(user_table.getBytes("UTF-8"), _main_panel.getMaster_pass(), CryptTools.AES_ZERO_IV));

                                    apikey = Bin2BASE64(CryptTools.aes_cbc_encrypt_pkcs7(apikey_table.getBytes("UTF-8"), _main_panel.getMaster_pass(), CryptTools.AES_ZERO_IV));

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

                status.setText(LabelTranslatorSingleton.getInstance().translate("Checking your MEGA accounts, please wait..."));

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

                final Dialog tthis = this;

                THREAD_POOL.execute(() -> {
                    ArrayList<String> email_error = new ArrayList<>();
                    ArrayList<String> new_valid_mega_accounts = new ArrayList<>();
                    for (int i = 0; i < model_row_count; i++) {

                        String email = (String) model.getValueAt(i, 0);

                        String pass = (String) model.getValueAt(i, 1);

                        if (!email.isEmpty() && !pass.isEmpty()) {

                            new_valid_mega_accounts.add(email);

                            MegaAPI ma;

                            if (_main_panel.getMega_accounts().get(email) == null) {

                                ma = new MegaAPI();

                                try {

                                    String pincode = null;

                                    boolean error_2FA = false;

                                    if (ma.check2FA(email)) {

                                        Get2FACode dialog = new Get2FACode((Frame) getParent(), true, email, _main_panel);

                                        dialog.setLocationRelativeTo(tthis);

                                        dialog.setVisible(true);

                                        if (dialog.isCode_ok()) {
                                            pincode = dialog.getPin_code();
                                        } else {
                                            error_2FA = true;
                                        }
                                    }

                                    if (!error_2FA) {
                                        ma.login(email, pass, pincode);

                                        ByteArrayOutputStream bs = new ByteArrayOutputStream();

                                        try (ObjectOutputStream os = new ObjectOutputStream(bs)) {
                                            os.writeObject(ma);
                                        }

                                        if (_main_panel.getMaster_pass() != null) {

                                            DBTools.insertMegaSession(email, CryptTools.aes_cbc_encrypt_pkcs7(bs.toByteArray(), _main_panel.getMaster_pass(), CryptTools.AES_ZERO_IV), true);

                                        } else {

                                            DBTools.insertMegaSession(email, bs.toByteArray(), false);
                                        }

                                        _main_panel.getMega_active_accounts().put(email, ma);

                                        String password = pass, password_aes = Bin2BASE64(i32a2bin(ma.getPassword_aes())), user_hash = ma.getUser_hash();

                                        if (_main_panel.getMaster_pass_hash() != null) {

                                            password = Bin2BASE64(CryptTools.aes_cbc_encrypt_pkcs7(pass.getBytes("UTF-8"), _main_panel.getMaster_pass(), CryptTools.AES_ZERO_IV));

                                            password_aes = Bin2BASE64(CryptTools.aes_cbc_encrypt_pkcs7(i32a2bin(ma.getPassword_aes()), _main_panel.getMaster_pass(), CryptTools.AES_ZERO_IV));

                                            user_hash = Bin2BASE64(CryptTools.aes_cbc_encrypt_pkcs7(UrlBASE642Bin(ma.getUser_hash()), _main_panel.getMaster_pass(), CryptTools.AES_ZERO_IV));
                                        }

                                        DBTools.insertMegaAccount(email, password, password_aes, user_hash);
                                    } else {
                                        email_error.add(email);
                                    }

                                } catch (Exception ex) {

                                    email_error.add(email);
                                    LOG.log(Level.SEVERE, ex.getMessage());
                                }

                            } else {

                                HashMap<String, Object> mega_account_data = (HashMap) _main_panel.getMega_accounts().get(email);

                                String password = (String) mega_account_data.get("password");

                                if (_main_panel.getMaster_pass() != null) {

                                    try {

                                        password = new String(CryptTools.aes_cbc_decrypt_pkcs7(BASE642Bin(password), _main_panel.getMaster_pass(), CryptTools.AES_ZERO_IV), "UTF-8");

                                    } catch (Exception ex) {
                                        LOG.log(Level.SEVERE, ex.getMessage());
                                    }
                                }

                                if (!password.equals(pass)) {

                                    ma = new MegaAPI();

                                    try {

                                        String pincode = null;

                                        boolean error_2FA = false;

                                        if (ma.check2FA(email)) {

                                            Get2FACode dialog = new Get2FACode((Frame) getParent(), true, email, _main_panel);

                                            dialog.setLocationRelativeTo(tthis);

                                            dialog.setVisible(true);

                                            if (dialog.isCode_ok()) {
                                                pincode = dialog.getPin_code();
                                            } else {
                                                error_2FA = true;
                                            }
                                        }

                                        if (!error_2FA) {

                                            ma.login(email, pass, pincode);

                                            ByteArrayOutputStream bs = new ByteArrayOutputStream();

                                            try (ObjectOutputStream os = new ObjectOutputStream(bs)) {
                                                os.writeObject(ma);
                                            }

                                            if (_main_panel.getMaster_pass() != null) {

                                                DBTools.insertMegaSession(email, CryptTools.aes_cbc_encrypt_pkcs7(bs.toByteArray(), _main_panel.getMaster_pass(), CryptTools.AES_ZERO_IV), true);

                                            } else {

                                                DBTools.insertMegaSession(email, bs.toByteArray(), false);
                                            }

                                            _main_panel.getMega_active_accounts().put(email, ma);

                                            password = pass;

                                            String password_aes = Bin2BASE64(i32a2bin(ma.getPassword_aes())), user_hash = ma.getUser_hash();

                                            if (_main_panel.getMaster_pass() != null) {

                                                password = Bin2BASE64(CryptTools.aes_cbc_encrypt_pkcs7(pass.getBytes("UTF-8"), _main_panel.getMaster_pass(), CryptTools.AES_ZERO_IV));

                                                password_aes = Bin2BASE64(CryptTools.aes_cbc_encrypt_pkcs7(i32a2bin(ma.getPassword_aes()), _main_panel.getMaster_pass(), CryptTools.AES_ZERO_IV));

                                                user_hash = Bin2BASE64(CryptTools.aes_cbc_encrypt_pkcs7(UrlBASE642Bin(ma.getUser_hash()), _main_panel.getMaster_pass(), CryptTools.AES_ZERO_IV));
                                            }

                                            DBTools.insertMegaAccount(email, password, password_aes, user_hash);
                                        } else {
                                            email_error.add(email);
                                        }

                                    } catch (Exception ex) {

                                        email_error.add(email);
                                        LOG.log(Level.SEVERE, ex.getMessage());

                                    }
                                }
                            }
                        }
                    }
                    if (email_error.size() > 0) {
                        String email_error_s = "";
                        email_error_s = email_error.stream().map((s) -> s + "\n").reduce(email_error_s, String::concat);
                        final String final_email_error = email_error_s;
                        MiscTools.GUIRun(() -> {
                            status.setText("");

                            JOptionPane.showMessageDialog(tthis, LabelTranslatorSingleton.getInstance().translate("There were errors with some accounts (email and/or password are/is wrong). Please, check them:\n\n") + final_email_error, "Mega Account Check Error", JOptionPane.ERROR_MESSAGE);

                            save_button.setEnabled(true);

                            cancel_button.setEnabled(true);

                            panel_tabs.setEnabled(true);

                            remove_mega_account_button.setEnabled(mega_accounts_table.getModel().getRowCount() > 0);

                            remove_elc_account_button.setEnabled(elc_accounts_table.getModel().getRowCount() > 0);

                            add_mega_account_button.setEnabled(true);

                            add_elc_account_button.setEnabled(true);

                            mega_accounts_table.setEnabled(true);

                            elc_accounts_table.setEnabled(true);

                            delete_all_accounts_button.setEnabled(true);

                            encrypt_pass_checkbox.setEnabled(true);

                            setDefaultCloseOperation(DISPOSE_ON_CLOSE);
                        });
                    } else {
                        _main_panel.getMega_accounts().entrySet().stream().map((entry) -> entry.getKey()).filter((email) -> (!new_valid_mega_accounts.contains(email))).forEachOrdered((email) -> {
                            _deleted_mega_accounts.add(email);
                        });
                        MiscTools.GUIRun(() -> {
                            status.setText("");
                            JOptionPane.showMessageDialog(tthis, LabelTranslatorSingleton.getInstance().translate("Settings successfully saved!"), LabelTranslatorSingleton.getInstance().translate("Settings saved"), JOptionPane.INFORMATION_MESSAGE);
                            _settings_ok = true;
                            setDefaultCloseOperation(DISPOSE_ON_CLOSE);
                            setVisible(false);
                        });
                    }
                });

            } else {

                JOptionPane.showMessageDialog(this, LabelTranslatorSingleton.getInstance().translate("Settings successfully saved!"), LabelTranslatorSingleton.getInstance().translate("Settings saved"), JOptionPane.INFORMATION_MESSAGE);
                _settings_ok = true;
                setDefaultCloseOperation(DISPOSE_ON_CLOSE);
                setVisible(false);
            }

        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, ex.getMessage());
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException ex) {
            LOG.log(Level.SEVERE, ex.getMessage());
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, ex.getMessage());
        }
    }//GEN-LAST:event_save_buttonActionPerformed

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

        if (use_proxy_checkbox.isSelected()) {
            smart_proxy_checkbox.setSelected(false);
        }
    }//GEN-LAST:event_use_proxy_checkboxStateChanged

    private void import_settings_buttonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_import_settings_buttonActionPerformed

        Object[] options = {"No",
            LabelTranslatorSingleton.getInstance().translate("Yes")};

        int n = showOptionDialog(this,
                LabelTranslatorSingleton.getInstance().translate("All your current settings and accounts will be deleted after import. (It is recommended to export your current settings before importing). \n\nDo you want to continue?"),
                LabelTranslatorSingleton.getInstance().translate("IMPORT SETTINGS"), YES_NO_CANCEL_OPTION, javax.swing.JOptionPane.WARNING_MESSAGE,
                null,
                options,
                options[0]);

        if (n == 1) {
            JFileChooser filechooser = new JFileChooser();
            updateFonts(filechooser, GUI_FONT, (float) (_main_panel.getZoom_factor() * 1.25));
            filechooser.setCurrentDirectory(new File(_download_path));
            filechooser.setDialogTitle("Select settings file");

            if (filechooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {

                File file = filechooser.getSelectedFile();

                try {

                    try (InputStream fis = new BufferedInputStream(new FileInputStream(file)); ObjectInputStream ois = new ObjectInputStream(fis)) {

                        HashMap<String, Object> settings = (HashMap<String, Object>) ois.readObject();

                        insertSettingsValues((HashMap<String, Object>) settings.get("settings"));

                        insertMegaAccounts((HashMap<String, Object>) settings.get("mega_accounts"));

                        insertELCAccounts((HashMap<String, Object>) settings.get("elc_accounts"));

                        _main_panel.loadUserSettings();

                        JOptionPane.showMessageDialog(this, LabelTranslatorSingleton.getInstance().translate("Settings successfully imported!"), LabelTranslatorSingleton.getInstance().translate("Settings imported"), JOptionPane.INFORMATION_MESSAGE);

                        _settings_ok = true;

                        setVisible(false);

                    } catch (SQLException | ClassNotFoundException ex) {
                        LOG.log(Level.SEVERE, ex.getMessage());
                    }

                } catch (IOException ex) {
                    LOG.log(Level.SEVERE, ex.getMessage());
                }
            }
        }

    }//GEN-LAST:event_import_settings_buttonActionPerformed

    private void export_settings_buttonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_export_settings_buttonActionPerformed

        Object[] options = {"No",
            LabelTranslatorSingleton.getInstance().translate("Yes")};

        int n = showOptionDialog(this,
                LabelTranslatorSingleton.getInstance().translate("Only SAVED settings and accounts will be exported. (If you are unsure, it is better to save your current settings and then export them).\n\nDo you want to continue?"),
                LabelTranslatorSingleton.getInstance().translate("EXPORT SETTINGS"), YES_NO_CANCEL_OPTION, javax.swing.JOptionPane.WARNING_MESSAGE,
                null,
                options,
                options[0]);

        if (n == 1) {
            JFileChooser filechooser = new JFileChooser();
            updateFonts(filechooser, GUI_FONT, (float) (_main_panel.getZoom_factor() * 1.25));
            filechooser.setCurrentDirectory(new File(_download_path));
            filechooser.setDialogTitle("Save as");

            if (filechooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {

                File file = filechooser.getSelectedFile();

                try {

                    if (file.exists()) {
                        file.delete();
                    }

                    file.createNewFile();

                    try (BufferedOutputStream fos = new BufferedOutputStream(new FileOutputStream(file)); ObjectOutputStream oos = new ObjectOutputStream(fos)) {

                        HashMap<String, Object> settings = new HashMap<>();

                        settings.put("settings", selectSettingsValues());

                        settings.put("mega_accounts", selectMegaAccounts());

                        settings.put("elc_accounts", selectELCAccounts());

                        oos.writeObject(settings);

                        JOptionPane.showMessageDialog(this, LabelTranslatorSingleton.getInstance().translate("Settings successfully exported!"), LabelTranslatorSingleton.getInstance().translate("Settings exported"), JOptionPane.INFORMATION_MESSAGE);

                        setVisible(false);

                    } catch (SQLException ex) {
                        LOG.log(Level.SEVERE, ex.getMessage());
                    }

                } catch (IOException ex) {
                    LOG.log(Level.SEVERE, ex.getMessage());
                }
            }
        }

    }//GEN-LAST:event_export_settings_buttonActionPerformed

    private void add_elc_account_buttonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_add_elc_account_buttonActionPerformed

        DefaultTableModel model = (DefaultTableModel) elc_accounts_table.getModel();

        model.addRow(new Object[]{"", "", ""});

        elc_accounts_table.clearSelection();

        remove_elc_account_button.setEnabled(true);
    }//GEN-LAST:event_add_elc_account_buttonActionPerformed

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

    private void unlock_accounts_buttonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_unlock_accounts_buttonActionPerformed

        unlock_accounts_button.setEnabled(false);

        final Dialog tthis = this;

        MiscTools.GUIRun(() -> {
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

                _main_panel.getMega_accounts().entrySet().stream().map((pair) -> {
                    HashMap<String, Object> data = (HashMap) pair.getValue();
                    String pass = null;
                    try {

                        pass = new String(CryptTools.aes_cbc_decrypt_pkcs7(BASE642Bin((String) data.get("password")), _main_panel.getMaster_pass(), CryptTools.AES_ZERO_IV), "UTF-8");

                    } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException ex) {
                        LOG.log(Level.SEVERE, ex.getMessage());
                    } catch (Exception ex) {
                        LOG.log(Level.SEVERE, ex.getMessage());
                    }
                    String[] new_row_data = {(String) pair.getKey(), pass};
                    return new_row_data;
                }).forEachOrdered((new_row_data) -> {
                    mega_model.addRow(new_row_data);
                });
                _main_panel.getElc_accounts().entrySet().stream().map((pair) -> {
                    HashMap<String, Object> data = (HashMap) pair.getValue();
                    String user = null, apikey = null;
                    try {

                        user = new String(CryptTools.aes_cbc_decrypt_pkcs7(BASE642Bin((String) data.get("user")), _main_panel.getMaster_pass(), CryptTools.AES_ZERO_IV), "UTF-8");

                        apikey = new String(CryptTools.aes_cbc_decrypt_pkcs7(BASE642Bin((String) data.get("apikey")), _main_panel.getMaster_pass(), CryptTools.AES_ZERO_IV), "UTF-8");

                    } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException ex) {
                        LOG.log(Level.SEVERE, ex.getMessage());
                    } catch (Exception ex) {
                        LOG.log(Level.SEVERE, ex.getMessage());
                    }
                    String[] new_row_data = {(String) pair.getKey(), user, apikey};
                    return new_row_data;
                }).forEachOrdered((new_row_data) -> {
                    elc_model.addRow(new_row_data);
                });

                mega_accounts_table.setAutoCreateRowSorter(true);
                DefaultRowSorter sorter_mega = ((DefaultRowSorter) mega_accounts_table.getRowSorter());
                ArrayList list_mega = new ArrayList();
                list_mega.add(new RowSorter.SortKey(0, SortOrder.ASCENDING));
                sorter_mega.setSortKeys(list_mega);
                sorter_mega.sort();

                elc_accounts_table.setAutoCreateRowSorter(true);
                DefaultRowSorter sorter_elc = ((DefaultRowSorter) elc_accounts_table.getRowSorter());
                ArrayList list_elc = new ArrayList();
                list_elc.add(new RowSorter.SortKey(0, SortOrder.ASCENDING));
                sorter_elc.setSortKeys(list_elc);
                sorter_elc.sort();

            }

            _remember_master_pass = dialog.getRemember_checkbox().isSelected();

            dialog.dispose();

            unlock_accounts_button.setEnabled(true);
        });
    }//GEN-LAST:event_unlock_accounts_buttonActionPerformed

    private void delete_all_accounts_buttonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_delete_all_accounts_buttonActionPerformed

        Object[] options = {"No",
            LabelTranslatorSingleton.getInstance().translate("Yes")};

        int n = showOptionDialog(this,
                LabelTranslatorSingleton.getInstance().translate("Master password will be reset and all your accounts will be removed. (THIS CAN'T BE UNDONE)\n\nDo you want to continue?"),
                LabelTranslatorSingleton.getInstance().translate("RESET ACCOUNTS"), YES_NO_CANCEL_OPTION, javax.swing.JOptionPane.WARNING_MESSAGE,
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

                DBTools.truncateMegaAccounts();

                DBTools.truncateELCAccounts();

                DBTools.truncateMegaSessions();

                _main_panel.setMaster_pass_hash(null);

                _main_panel.setMaster_pass(null);

                insertSettingValue("master_pass_hash", null);

                encrypt_pass_checkbox.setSelected(false);

                _main_panel.getMega_accounts().clear();

                _main_panel.getMega_active_accounts().clear();

                _main_panel.getElc_accounts().clear();

                JOptionPane.showMessageDialog(this, LabelTranslatorSingleton.getInstance().translate("Accounts successfully reset!"), LabelTranslatorSingleton.getInstance().translate("Accounts reset"), JOptionPane.INFORMATION_MESSAGE);

                setVisible(false);

            } catch (SQLException ex) {
                LOG.log(Level.SEVERE, ex.getMessage());
            }
        }
    }//GEN-LAST:event_delete_all_accounts_buttonActionPerformed

    private void encrypt_pass_checkboxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_encrypt_pass_checkboxActionPerformed

        encrypt_pass_checkbox.setEnabled(false);

        final Dialog tthis = this;

        MiscTools.GUIRun(() -> {
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

                    DBTools.truncateMegaSessions();

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

                            password = new String(CryptTools.aes_cbc_decrypt_pkcs7(BASE642Bin((String) data.get("password")), old_master_pass, CryptTools.AES_ZERO_IV), "UTF-8");

                            password_aes = Bin2BASE64(CryptTools.aes_cbc_decrypt_pkcs7(BASE642Bin((String) data.get("password_aes")), old_master_pass, CryptTools.AES_ZERO_IV));

                            user_hash = Bin2BASE64(CryptTools.aes_cbc_decrypt_pkcs7(BASE642Bin((String) data.get("user_hash")), old_master_pass, CryptTools.AES_ZERO_IV));

                        } else {

                            password = (String) data.get("password");

                            password_aes = (String) data.get("password_aes");

                            user_hash = (String) data.get("user_hash");
                        }

                        if (_main_panel.getMaster_pass() != null) {

                            password = Bin2BASE64(CryptTools.aes_cbc_encrypt_pkcs7(password.getBytes("UTF-8"), _main_panel.getMaster_pass(), CryptTools.AES_ZERO_IV));

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

                            user = new String(CryptTools.aes_cbc_decrypt_pkcs7(BASE642Bin((String) data.get("user")), old_master_pass, CryptTools.AES_ZERO_IV), "UTF-8");

                            apikey = new String(CryptTools.aes_cbc_decrypt_pkcs7(BASE642Bin((String) data.get("apikey")), old_master_pass, CryptTools.AES_ZERO_IV), "UTF-8");

                        } else {

                            user = (String) data.get("user");

                            apikey = (String) data.get("apikey");

                        }

                        if (_main_panel.getMaster_pass() != null) {

                            user = Bin2BASE64(CryptTools.aes_cbc_encrypt_pkcs7(user.getBytes("UTF-8"), _main_panel.getMaster_pass(), CryptTools.AES_ZERO_IV));

                            apikey = Bin2BASE64(CryptTools.aes_cbc_encrypt_pkcs7(apikey.getBytes("UTF-8"), _main_panel.getMaster_pass(), CryptTools.AES_ZERO_IV));
                        }

                        data.put("user", user);

                        data.put("apikey", apikey);

                        DBTools.insertELCAccount(host, user, apikey);
                    }

                } catch (Exception ex) {
                    LOG.log(Level.SEVERE, ex.getMessage());
                }

            }

            encrypt_pass_checkbox.setSelected((_main_panel.getMaster_pass_hash() != null));

            dialog.dispose();

            encrypt_pass_checkbox.setEnabled(true);
        });
    }//GEN-LAST:event_encrypt_pass_checkboxActionPerformed

    private void add_mega_account_buttonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_add_mega_account_buttonActionPerformed

        DefaultTableModel model = (DefaultTableModel) mega_accounts_table.getModel();

        model.addRow(new Object[]{"", ""});

        mega_accounts_table.clearSelection();
    }//GEN-LAST:event_add_mega_account_buttonActionPerformed

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
        updateFonts(filechooser, GUI_FONT, (float) (_main_panel.getZoom_factor() * 1.25));

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

    private void use_mega_account_down_checkboxStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_use_mega_account_down_checkboxStateChanged

        if (!use_mega_account_down_checkbox.isSelected()) {

            use_mega_account_down_combobox.setEnabled(false);

            use_mega_label.setEnabled(false);

        } else {

            use_mega_account_down_combobox.setEnabled(true);

            use_mega_label.setEnabled(true);

            use_mega_account_down_combobox.removeAllItems();

            if (_main_panel.getMega_accounts().size() > 0) {

                _main_panel.getMega_accounts().keySet().forEach((o) -> {
                    use_mega_account_down_combobox.addItem(o);
                });

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

            rec_smart_proxy_label.setEnabled(true);
            rec_smart_proxy_label1.setEnabled(true);
            multi_slot_down_checkbox.setSelected(true);
            use_proxy_checkbox.setSelected(false);
            custom_proxy_list_label.setEnabled(true);
            custom_proxy_textarea.setEnabled(true);

        } else {

            rec_smart_proxy_label.setEnabled(false);
            rec_smart_proxy_label1.setEnabled(false);
            custom_proxy_list_label.setEnabled(false);
            custom_proxy_textarea.setEnabled(false);
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

    private void limit_upload_speed_checkboxStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_limit_upload_speed_checkboxStateChanged

        max_up_speed_label.setEnabled(limit_upload_speed_checkbox.isSelected());
        max_up_speed_spinner.setEnabled(limit_upload_speed_checkbox.isSelected());
    }//GEN-LAST:event_limit_upload_speed_checkboxStateChanged

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        // TODO add your handling code here:

        Object[] options = {"No",
            LabelTranslatorSingleton.getInstance().translate("Yes")};

        int n = showOptionDialog(this,
                LabelTranslatorSingleton.getInstance().translate("ALL YOUR SETTINGS, ACCOUNTS AND TRANSFERENCES WILL BE REMOVED. (THIS CAN'T BE UNDONE)\n\nDo you want to continue?"),
                LabelTranslatorSingleton.getInstance().translate("RESET MEGABASTERD"), YES_NO_CANCEL_OPTION, javax.swing.JOptionPane.WARNING_MESSAGE,
                null,
                options,
                options[0]);

        if (n == 1) {

            setVisible(false);
            _main_panel.byebyenow(true, true);

        }
    }//GEN-LAST:event_jButton1ActionPerformed

    private void custom_chunks_dir_checkboxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_custom_chunks_dir_checkboxActionPerformed

        if (custom_chunks_dir_checkbox.isSelected()) {

            custom_chunks_dir_button.setEnabled(true);
            custom_chunks_dir_current_label.setEnabled(true);

        } else {

            custom_chunks_dir_button.setEnabled(false);
            custom_chunks_dir_current_label.setEnabled(false);

        }
    }//GEN-LAST:event_custom_chunks_dir_checkboxActionPerformed

    private void custom_chunks_dir_buttonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_custom_chunks_dir_buttonActionPerformed
        javax.swing.JFileChooser filechooser = new javax.swing.JFileChooser();
        updateFonts(filechooser, GUI_FONT, (float) (_main_panel.getZoom_factor() * 1.25));

        filechooser.setCurrentDirectory(new java.io.File(_download_path));
        filechooser.setDialogTitle("Temporary chunks directory");
        filechooser.setFileSelectionMode(javax.swing.JFileChooser.DIRECTORIES_ONLY);
        filechooser.setAcceptAllFileFilterUsed(false);

        if (filechooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {

            File file = filechooser.getSelectedFile();

            _custom_chunks_dir = file.getAbsolutePath();

            this.custom_chunks_dir_current_label.setText(truncateText(_custom_chunks_dir, 80));
        }
    }//GEN-LAST:event_custom_chunks_dir_buttonActionPerformed

    private void run_command_checkboxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_run_command_checkboxActionPerformed
        // TODO add your handling code here:

        run_command_textbox.setEnabled(run_command_checkbox.isSelected());

    }//GEN-LAST:event_run_command_checkboxActionPerformed

    private void run_command_test_buttonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_run_command_test_buttonActionPerformed
        // TODO add your handling code here:

        if (run_command_textbox.getText() != null && !"".equals(run_command_textbox.getText().trim())) {

            try {
                Runtime.getRuntime().exec(run_command_textbox.getText().trim());
            } catch (IOException ex) {
                Logger.getLogger(MiscTools.class.getName()).log(Level.SEVERE, ex.getMessage());
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }//GEN-LAST:event_run_command_test_buttonActionPerformed

    private void smart_proxy_checkboxMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_smart_proxy_checkboxMouseClicked
        // TODO add your handling code here:
        if (this.smart_proxy_checkbox.isSelected()) {
            JOptionPane.showMessageDialog(this, LabelTranslatorSingleton.getInstance().translate("Using proxies or VPN to bypass MEGA's daily download limitation may violate its Terms of Use.\n\nUSE THIS OPTION AT YOUR OWN RISK."), LabelTranslatorSingleton.getInstance().translate("WARNING"), JOptionPane.WARNING_MESSAGE);
        }

    }//GEN-LAST:event_smart_proxy_checkboxMouseClicked

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel accounts_panel;
    private javax.swing.JButton add_elc_account_button;
    private javax.swing.JButton add_mega_account_button;
    private javax.swing.JPanel advanced_panel;
    private javax.swing.JScrollPane advanced_scrollpane;
    private javax.swing.JButton cancel_button;
    private javax.swing.JButton change_download_dir_button;
    private javax.swing.JButton custom_chunks_dir_button;
    private javax.swing.JCheckBox custom_chunks_dir_checkbox;
    private javax.swing.JLabel custom_chunks_dir_current_label;
    private javax.swing.JLabel custom_proxy_list_label;
    private javax.swing.JTextArea custom_proxy_textarea;
    private javax.swing.JCheckBox debug_file_checkbox;
    private javax.swing.JLabel default_dir_label;
    private javax.swing.JLabel default_slots_down_label;
    private javax.swing.JSpinner default_slots_down_spinner;
    private javax.swing.JLabel default_slots_up_label;
    private javax.swing.JSpinner default_slots_up_spinner;
    private javax.swing.JButton delete_all_accounts_button;
    private javax.swing.JLabel down_dir_label;
    private javax.swing.JPanel downloads_panel;
    private javax.swing.JScrollPane downloads_scrollpane;
    private javax.swing.JLabel elc_accounts_label;
    private javax.swing.JScrollPane elc_accounts_scrollpane;
    private javax.swing.JTable elc_accounts_table;
    private javax.swing.JCheckBox encrypt_pass_checkbox;
    private javax.swing.JButton export_settings_button;
    private javax.swing.JComboBox<String> font_combo;
    private javax.swing.JLabel font_label;
    private javax.swing.JButton import_settings_button;
    private javax.swing.JButton jButton1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JProgressBar jProgressBar1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator10;
    private javax.swing.JSeparator jSeparator11;
    private javax.swing.JSeparator jSeparator12;
    private javax.swing.JSeparator jSeparator15;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JSeparator jSeparator3;
    private javax.swing.JSeparator jSeparator4;
    private javax.swing.JSeparator jSeparator5;
    private javax.swing.JSeparator jSeparator6;
    private javax.swing.JSeparator jSeparator7;
    private javax.swing.JSeparator jSeparator8;
    private javax.swing.JSeparator jSeparator9;
    private javax.swing.JComboBox<String> language_combo;
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
    private javax.swing.JTabbedPane panel_tabs;
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
    private javax.swing.JLabel rec_smart_proxy_label1;
    private javax.swing.JLabel rec_upload_slots_label;
    private javax.swing.JLabel rec_zoom_label;
    private javax.swing.JButton remove_elc_account_button;
    private javax.swing.JButton remove_mega_account_button;
    private javax.swing.JCheckBox run_command_checkbox;
    private javax.swing.JButton run_command_test_button;
    private javax.swing.JTextField run_command_textbox;
    private javax.swing.JButton save_button;
    private javax.swing.JCheckBox smart_proxy_checkbox;
    private javax.swing.JCheckBox start_frozen_checkbox;
    private javax.swing.JLabel status;
    private javax.swing.JButton unlock_accounts_button;
    private javax.swing.JPanel uploads_panel;
    private javax.swing.JScrollPane uploads_scrollpane;
    private javax.swing.JCheckBox use_mega_account_down_checkbox;
    private javax.swing.JComboBox<String> use_mega_account_down_combobox;
    private javax.swing.JLabel use_mega_label;
    private javax.swing.JCheckBox use_proxy_checkbox;
    private javax.swing.JCheckBox verify_file_down_checkbox;
    private javax.swing.JLabel zoom_label;
    private javax.swing.JSpinner zoom_spinner;
    // End of variables declaration//GEN-END:variables
    private static final Logger LOG = Logger.getLogger(SettingsDialog.class.getName());
}
