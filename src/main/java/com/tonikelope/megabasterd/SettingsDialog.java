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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tonikelope.megabasterd.DBTools.getSettingsCache;
import static com.tonikelope.megabasterd.DBTools.insertELCAccounts;
import static com.tonikelope.megabasterd.DBTools.insertMegaAccounts;
import static com.tonikelope.megabasterd.DBTools.insertSettingValue;
import static com.tonikelope.megabasterd.DBTools.insertSettingsValues;
import static com.tonikelope.megabasterd.DBTools.selectELCAccounts;
import static com.tonikelope.megabasterd.DBTools.selectMegaAccounts;
import static com.tonikelope.megabasterd.MainPanel.GUI_FONT;
import static com.tonikelope.megabasterd.MainPanel.THREAD_POOL;
import static com.tonikelope.megabasterd.MiscTools.BASE642Bin;
import static com.tonikelope.megabasterd.MiscTools.Bin2BASE64;
import static com.tonikelope.megabasterd.MiscTools.UrlBASE642Bin;
import static com.tonikelope.megabasterd.MiscTools.createUploadLogDir;
import static com.tonikelope.megabasterd.MiscTools.i32a2bin;
import static com.tonikelope.megabasterd.MiscTools.translateLabels;
import static com.tonikelope.megabasterd.MiscTools.truncateText;
import static com.tonikelope.megabasterd.MiscTools.updateFonts;
import static com.tonikelope.megabasterd.MiscTools.updateTitledBorderFont;
import static com.tonikelope.megabasterd.SmartMegaProxyManager.PROXY_AUTO_REFRESH_TIME;
import static com.tonikelope.megabasterd.SmartMegaProxyManager.PROXY_BLOCK_TIME;
import static javax.swing.JOptionPane.YES_NO_CANCEL_OPTION;
import static javax.swing.JOptionPane.showOptionDialog;

/**
 *
 * @author tonikelope
 */
public class SettingsDialog extends javax.swing.JDialog {

    private static final Logger LOG = LogManager.getLogger(SettingsDialog.class);
    private String _download_path;
    private String _custom_chunks_dir;
    private boolean _settings_ok;
    private final Set<String> _deleted_mega_accounts;
    private final Set<String> _deleted_elc_accounts;
    private final MainPanel _main_panel;
    private boolean _remember_master_pass;
    private volatile boolean _exit = false;

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

        _deleted_mega_accounts = new HashSet<>();

        _deleted_elc_accounts = new HashSet<>();

        _settings_ok = false;

        MiscTools.GUIRunAndWait(() -> {

            initComponents();

            updateFonts(this, GUI_FONT, _main_panel.getZoom_factor());

            updateTitledBorderFont(((javax.swing.border.TitledBorder) proxy_panel.getBorder()), GUI_FONT, _main_panel.getZoom_factor());

            updateTitledBorderFont(((javax.swing.border.TitledBorder) proxy_auth_panel.getBorder()), GUI_FONT, _main_panel.getZoom_factor());

            translateLabels(this);

            Border original_regex_textfield_border = file_regex_textfield.getBorder();
            
            file_regex101_label.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseClicked(java.awt.event.MouseEvent evt) {
                    String currentRegex = file_regex_textfield.getText();
                    if (currentRegex.isEmpty()) return;
                    
                    String url = (String) file_regex101_label.getClientProperty("regexUrl");
                    if (url != null) {
                        MiscTools.openBrowserURL(url);
                    }
                }
            });
            
            file_regex_textfield.setInputVerifier(new InputVerifier() {
                @Override
                public boolean verify(JComponent input) {
                    String currentRegex = ((JTextField) input).getText();
                    try {
                        Pattern.compile(currentRegex);
                        input.setBorder(original_regex_textfield_border);
                        String encodedRegex = URLEncoder.encode(currentRegex, StandardCharsets.UTF_8);
                        String regex101Url = String.format("https://regex101.com/?regex=%s&flags=gm", encodedRegex);
                        String localizedRegex101Display = LabelTranslatorSingleton.getInstance().translate("Test on Regex101");
                        String formattedHtml = String.format("<HTML><a target=\"_blank\" href=\"%s\">%s</a></HTML>", regex101Url, localizedRegex101Display);
                        file_regex101_label.setEnabled(true);
                        file_regex101_label.setText(formattedHtml);
                        file_regex101_label.putClientProperty("regexUrl", regex101Url);
                        return true;
                    } catch (PatternSyntaxException ex) {
                        input.setBorder(BorderFactory.createLineBorder(Color.RED));
                        file_regex101_label.setEnabled(false);
                        file_regex101_label.putClientProperty("regexUrl", "");
                        return false;
                    } catch (Exception ex) {
                        LOG.fatal("Generic exception in Regex parsing! {}", ex.getMessage());
                        file_regex101_label.setEnabled(false);
                        file_regex101_label.putClientProperty("regexUrl", "");
                        return false;
                    }
                }
            });
            
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

            boolean monitor_clipboard = Download.DEFAULT_CLIPBOARD_LINK_MONITOR;

            String monitor_clipboard_string = DBTools.selectSettingValue("clipboardspy");

            if (monitor_clipboard_string != null) {
                monitor_clipboard = monitor_clipboard_string.equals("yes");
            }

            boolean thumbnails = Upload.DEFAULT_THUMBNAILS;

            String thumbnails_string = DBTools.selectSettingValue("thumbnails");

            if (thumbnails_string != null) {
                thumbnails = thumbnails_string.equals("yes");
            }

            thumbnail_checkbox.setSelected(thumbnails);

            boolean upload_log = Upload.UPLOAD_LOG;

            String upload_log_string = DBTools.selectSettingValue("upload_log");

            if (upload_log_string != null) {
                upload_log = upload_log_string.equals("yes");
            }

            upload_log_checkbox.setSelected(upload_log);

            boolean upload_public_folder = Upload.UPLOAD_PUBLIC_FOLDER;

            String upload_public_folder_string = DBTools.selectSettingValue("upload_public_folder");

            if (upload_public_folder_string != null) {
                upload_public_folder = upload_public_folder_string.equals("yes");
            }

            upload_public_folder_checkbox.setSelected(upload_public_folder);

            upload_public_folder_checkbox.setBackground(upload_public_folder_checkbox.isSelected() ? java.awt.Color.RED : null);

            this.public_folder_panel.setVisible(this.upload_public_folder_checkbox.isSelected());

            clipboardspy_checkbox.setSelected(monitor_clipboard);

            String default_download_dir = DBTools.selectSettingValue("default_down_dir");

            default_download_dir = Paths.get(default_download_dir == null ? MainPanel.MEGABASTERD_HOME_DIR : default_download_dir).toAbsolutePath().normalize().toString();

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

            String smartproxy_auto_refresh = DBTools.selectSettingValue("smartproxy_autorefresh_time");

            int smartproxy_auto_refresh_int = PROXY_AUTO_REFRESH_TIME;

            if (smartproxy_auto_refresh != null) {
                smartproxy_auto_refresh_int = Integer.parseInt(smartproxy_auto_refresh);
            }

            auto_refresh_proxy_time_spinner.setModel(new SpinnerNumberModel(smartproxy_auto_refresh_int, 1, Integer.MAX_VALUE, 1));

            ((JSpinner.DefaultEditor) auto_refresh_proxy_time_spinner.getEditor()).getTextField().setEditable(true);

            String smartproxy_ban_time = DBTools.selectSettingValue("smartproxy_ban_time");

            int smartproxy_ban_time_int = PROXY_BLOCK_TIME;

            if (smartproxy_ban_time != null) {
                smartproxy_ban_time_int = Integer.parseInt(smartproxy_ban_time);
            }

            bad_proxy_time_spinner.setModel(new SpinnerNumberModel(smartproxy_ban_time_int, 0, Integer.MAX_VALUE, 1));

            ((JSpinner.DefaultEditor) bad_proxy_time_spinner.getEditor()).getTextField().setEditable(true);

            String smartproxy_timeout = DBTools.selectSettingValue("smartproxy_timeout");

            int smartproxy_timeout_int = (int) ((float) Transference.HTTP_PROXY_TIMEOUT / 1000);

            if (smartproxy_timeout != null) {
                smartproxy_timeout_int = Integer.parseInt(smartproxy_timeout);
            }

            proxy_timeout_spinner.setModel(new SpinnerNumberModel(smartproxy_timeout_int, 1, Integer.MAX_VALUE, 1));

            ((JSpinner.DefaultEditor) proxy_timeout_spinner.getEditor()).getTextField().setEditable(true);

            boolean reset_slot_proxy = SmartMegaProxyManager.RESET_SLOT_PROXY;

            String sreset_slot_proxy = DBTools.selectSettingValue("reset_slot_proxy");

            if (sreset_slot_proxy != null) {

                reset_slot_proxy = sreset_slot_proxy.equals("yes");
            }

            proxy_reset_slot_checkbox.setSelected(reset_slot_proxy);

            boolean random_select = SmartMegaProxyManager.RANDOM_SELECT;

            String srandom_select = DBTools.selectSettingValue("random_proxy");

            if (srandom_select != null) {

                random_select = srandom_select.equals("yes");
            }

            if (random_select) {
                proxy_random_radio.setSelected(true);
            } else {
                proxy_sequential_radio.setSelected(true);
            }

            boolean dark_mode = false;

            String dark_mode_select = DBTools.selectSettingValue("dark_mode");

            if (dark_mode_select != null) {

                dark_mode = dark_mode_select.equals("yes");
            }

            dark_mode_checkbox.setSelected(dark_mode);

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
            auto_restart_damaged_checkbox.setEnabled(cbc_mac);
            
            boolean removeNoRestart = Download.REMOVE_NO_RESTART_DEFAULT;
            String settingValueRemoveNoRestart = DBTools.selectSettingValue("remove_no_restart");
            if (settingValueRemoveNoRestart != null) {
                removeNoRestart = (settingValueRemoveNoRestart.equals("yes"));
            }
            remove_no_restart_checkbox.setSelected(removeNoRestart);

            boolean auto_restart_damaged = Download.AUTO_RESTART_DAMAGED_DEFAULT;
            String settingValueAutoRestartDamaged = DBTools.selectSettingValue("auto_restart_damaged");
            if (settingValueAutoRestartDamaged != null) {
                auto_restart_damaged = (settingValueAutoRestartDamaged.equals("yes"));
            }
            auto_restart_damaged_checkbox.setSelected(auto_restart_damaged);

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

                    for (String k : _main_panel.getMega_accounts().keySet()) {

                        String[] new_row_data = {k, "**************************"};

                        mega_model.addRow(new_row_data);
                    }

                    for (String k : _main_panel.getElc_accounts().keySet()) {

                        String[] new_row_data = {k, "**************************", "**************************"};

                        elc_model.addRow(new_row_data);
                    }

                    mega_accounts_table.setEnabled(false);

                    elc_accounts_table.setEnabled(false);

                } else {

                    unlock_accounts_button.setVisible(false);

                    for (Map.Entry<String, Object> pair : _main_panel.getMega_accounts().entrySet()) {

                        HashMap<String, Object> data = (HashMap<String, Object>) pair.getValue();
                        String pass = null;

                        try {
                            pass = new String(CryptTools.aes_cbc_decrypt_pkcs7(BASE642Bin((String) data.get("password")), _main_panel.getMaster_pass(), CryptTools.AES_ZERO_IV), StandardCharsets.UTF_8);
                        } catch (Exception ex) {
                            LOG.fatal("Exception trying to setup auth! {}", ex.getMessage());
                        }

                        String[] new_row_data = {pair.getKey(), pass};

                        mega_model.addRow(new_row_data);
                    }

                    for (Map.Entry<String, Object> pair : _main_panel.getElc_accounts().entrySet()) {

                        HashMap<String, String> data = (HashMap<String, String>) pair.getValue();
                        String user = null, apikey = null;

                        try {
                            user = new String(CryptTools.aes_cbc_decrypt_pkcs7(BASE642Bin(data.get("user")), _main_panel.getMaster_pass(), CryptTools.AES_ZERO_IV), StandardCharsets.UTF_8);
                            apikey = new String(CryptTools.aes_cbc_decrypt_pkcs7(BASE642Bin(data.get("apikey")), _main_panel.getMaster_pass(), CryptTools.AES_ZERO_IV), StandardCharsets.UTF_8);
                        } catch (Exception ex) {
                            LOG.fatal("Exception setting up apikey auth! {}", ex.getMessage());
                        }

                        String[] new_row_data = { pair.getKey(), user, apikey };

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

                megacrypter_reverse_p = DBTools.selectSettingValue("megacrypter_reverse_port");
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

            MiscTools.containerSetEnabled(smart_proxy_settings, use_smart_proxy);

            boolean force_smart_proxy = MainPanel.FORCE_SMART_PROXY;

            String force_smart_proxy_string = DBTools.selectSettingValue("force_smart_proxy");

            if (force_smart_proxy_string != null) {

                force_smart_proxy = force_smart_proxy_string.equals("yes");
            }

            force_smart_proxy_checkbox.setSelected(force_smart_proxy);

            boolean run_command = false;

            String run_command_string = DBTools.selectSettingValue("run_command");

            if (run_command_string != null) {

                run_command = run_command_string.equals("yes");
            }

            run_command_checkbox.setSelected(run_command);

            run_command_textbox.setEnabled(run_command);

            run_command_textbox.setText(DBTools.selectSettingValue("run_command_path"));

            String use_file_regex_string = DBTools.selectSettingValue("use_file_regex");
            
            boolean use_file_regex = false;
            
            if (use_file_regex_string != null) {
                
                use_file_regex = use_file_regex_string.equals("yes");
            }

            file_regex_checkbox.setSelected(use_file_regex);
            
            file_regex_textfield.setEnabled(use_file_regex);

            file_regex_textfield.setText(DBTools.selectSettingValue("file_regex_pattern"));
            
            if (use_file_regex) {

                InputVerifier verifier = file_regex_textfield.getInputVerifier();

                if (verifier != null) {
                    verifier.verify(file_regex_textfield);
                }

            }

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

            this.font_combo.setSelectedItem(LabelTranslatorSingleton.getInstance().translate(Objects.requireNonNullElse(font, "DEFAULT")));

            String language = DBTools.selectSettingValue("language");

            this.language_combo.addItem(LabelTranslatorSingleton.getInstance().translate("English"));

            this.language_combo.addItem(LabelTranslatorSingleton.getInstance().translate("Spanish"));

            this.language_combo.addItem(LabelTranslatorSingleton.getInstance().translate("Italian"));

            this.language_combo.addItem(LabelTranslatorSingleton.getInstance().translate("Turkish"));

            this.language_combo.addItem(LabelTranslatorSingleton.getInstance().translate("Chinese"));

            this.language_combo.addItem(LabelTranslatorSingleton.getInstance().translate("Vietnamese"));

            this.language_combo.addItem(LabelTranslatorSingleton.getInstance().translate("German"));

            this.language_combo.addItem(LabelTranslatorSingleton.getInstance().translate("Hungarian"));

            if (language == null) {
                language = MainPanel.DEFAULT_LANGUAGE;
            }

            String newLanguage = switch (language) {
                case "EN" -> "English";
                case "ES" -> "Spanish";
                case "IT" -> "Italian";
                case "TU" -> "Turkish";
                case "CH" -> "Chinese";
                case "VI" -> "Vietnamese";
                case "GE" -> "German";
                case "HU" -> "Hungarian";
                default -> "English";
            };

            this.language_combo.setSelectedItem(LabelTranslatorSingleton.getInstance().translate(newLanguage));

            String custom_proxy_list = DBTools.selectSettingValue("custom_proxy_list");

            if (custom_proxy_list != null) {
                custom_proxy_textarea.setText(custom_proxy_list);
            }

            revalidate();

            repaint();

            setPreferredSize(parent.getSize());

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
        clipboardspy_checkbox = new javax.swing.JCheckBox();
        smart_proxy_settings = new javax.swing.JPanel();
        jLabel5 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        bad_proxy_time_spinner = new javax.swing.JSpinner();
        jLabel6 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        custom_proxy_textarea = new javax.swing.JTextArea();
        rec_smart_proxy_label1 = new javax.swing.JLabel();
        custom_proxy_list_label = new javax.swing.JLabel();
        rec_smart_proxy_label = new javax.swing.JLabel();
        proxy_timeout_spinner = new javax.swing.JSpinner();
        force_smart_proxy_checkbox = new javax.swing.JCheckBox();
        jLabel7 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        auto_refresh_proxy_time_spinner = new javax.swing.JSpinner();
        proxy_random_radio = new javax.swing.JRadioButton();
        proxy_sequential_radio = new javax.swing.JRadioButton();
        jLabel9 = new javax.swing.JLabel();
        proxy_reset_slot_checkbox = new javax.swing.JCheckBox();
        jLabel10 = new javax.swing.JLabel();
        jLabel11 = new javax.swing.JLabel();
        remove_no_restart_checkbox = new javax.swing.JCheckBox();
        remove_no_restart_checkbox1 = new javax.swing.JCheckBox();
        auto_restart_damaged_checkbox = new javax.swing.JCheckBox();
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
        thumbnail_checkbox = new javax.swing.JCheckBox();
        upload_log_checkbox = new javax.swing.JCheckBox();
        upload_public_folder_checkbox = new javax.swing.JCheckBox();
        public_folder_panel = new javax.swing.JScrollPane();
        public_folder_warning = new javax.swing.JTextArea();
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
        import_mega_button = new javax.swing.JButton();
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
        rec_zoom_label = new javax.swing.JLabel();
        custom_chunks_dir_button = new javax.swing.JButton();
        custom_chunks_dir_current_label = new javax.swing.JLabel();
        custom_chunks_dir_checkbox = new javax.swing.JCheckBox();
        start_frozen_checkbox = new javax.swing.JCheckBox();
        run_command_checkbox = new javax.swing.JCheckBox();
        run_command_textbox = new javax.swing.JTextField();
        run_command_textbox.addMouseListener(new ContextMenuMouseListener());
        run_command_test_button = new javax.swing.JButton();
        debug_file_checkbox = new javax.swing.JCheckBox();
        jPanel1 = new javax.swing.JPanel();
        jButton1 = new javax.swing.JButton();
        import_settings_button = new javax.swing.JButton();
        export_settings_button = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        font_label = new javax.swing.JLabel();
        language_combo = new javax.swing.JComboBox<>();
        font_combo = new javax.swing.JComboBox<>();
        zoom_label = new javax.swing.JLabel();
        zoom_spinner = new javax.swing.JSpinner();
        dark_mode_checkbox = new javax.swing.JCheckBox();
        debug_file_path = new javax.swing.JLabel();
        file_regex_checkbox = new javax.swing.JCheckBox();
        file_regex_textfield = new javax.swing.JTextField();
        run_command_textbox.addMouseListener(new ContextMenuMouseListener());
        file_regex101_label = new javax.swing.JLabel();
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
        megacrypter_reverse_warning_label.setEnabled(false);

        rec_download_slots_label.setFont(new java.awt.Font("Dialog", 2, 16)); // NOI18N
        rec_download_slots_label.setText("Note: slots consume resources, so use them moderately.");
        rec_download_slots_label.setEnabled(false);

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
        max_down_speed_spinner.setEnabled(false);

        verify_file_down_checkbox.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        verify_file_down_checkbox.setText("Verify file integrity (when download is finished)");
        verify_file_down_checkbox.setDoubleBuffered(true);
        verify_file_down_checkbox.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                verify_file_down_checkboxStateChanged(evt);
            }
        });

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
        use_mega_account_down_combobox.setEnabled(false);

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
        max_down_speed_label.setEnabled(false);

        megacrypter_reverse_port_label.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        megacrypter_reverse_port_label.setText("TCP Port:");
        megacrypter_reverse_port_label.setEnabled(false);

        default_dir_label.setBackground(new java.awt.Color(153, 255, 153));
        default_dir_label.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        default_dir_label.setForeground(new java.awt.Color(51, 0, 255));
        default_dir_label.setText("default dir");
        default_dir_label.setOpaque(true);

        default_slots_down_label.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        default_slots_down_label.setText("Default slots per file:");
        default_slots_down_label.setDoubleBuffered(true);
        default_slots_down_label.setEnabled(false);

        use_mega_label.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        use_mega_label.setText("Default account:");
        use_mega_label.setEnabled(false);

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
        default_slots_down_spinner.setEnabled(false);
        default_slots_down_spinner.setValue(2);

        megacrypter_reverse_port_spinner.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        megacrypter_reverse_port_spinner.setEnabled(false);

        down_dir_label.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        down_dir_label.setText("Download folder:");
        down_dir_label.setDoubleBuffered(true);

        clipboardspy_checkbox.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        clipboardspy_checkbox.setText("Monitor clipboard looking for new links");

        smart_proxy_settings.setEnabled(false);

        jLabel5.setFont(new java.awt.Font("Noto Sans", 1, 16)); // NOI18N
        jLabel5.setText("Proxy timeout (seconds):");

        jLabel3.setFont(new java.awt.Font("Noto Sans", 1, 16)); // NOI18N
        jLabel3.setText("Proxy error ban time (seconds):");

        jLabel4.setFont(new java.awt.Font("Noto Sans", 2, 16)); // NOI18N
        jLabel4.setText("(0 for permanent ban)");

        bad_proxy_time_spinner.setFont(new java.awt.Font("Noto Sans", 0, 16)); // NOI18N
        bad_proxy_time_spinner.setModel(new javax.swing.SpinnerNumberModel(300, 0, null, 1));

        jLabel6.setFont(new java.awt.Font("Noto Sans", 2, 16)); // NOI18N
        jLabel6.setText("(Lower values can speed up finding working proxies but it could ban slow proxies)");

        custom_proxy_textarea.setColumns(20);
        custom_proxy_textarea.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        custom_proxy_textarea.setRows(5);
        custom_proxy_textarea.setDoubleBuffered(true);
        jScrollPane1.setViewportView(custom_proxy_textarea);
        custom_proxy_textarea.addMouseListener(new ContextMenuMouseListener());

        rec_smart_proxy_label1.setFont(new java.awt.Font("Dialog", 1, 16)); // NOI18N
        rec_smart_proxy_label1.setForeground(new java.awt.Color(255, 51, 0));
        rec_smart_proxy_label1.setText("WARNING: Using proxies or VPN to bypass MEGA's daily download limitation may violate its Terms of Use. USE THIS OPTION AT YOUR OWN RISK.");

        custom_proxy_list_label.setBackground(new java.awt.Color(0, 0, 0));
        custom_proxy_list_label.setFont(new java.awt.Font("Dialog", 1, 16)); // NOI18N
        custom_proxy_list_label.setForeground(new java.awt.Color(255, 255, 255));
        custom_proxy_list_label.setText("[*]IP:PORT[@user_b64:password_b64] OR #PROXY_LIST_URL");
        custom_proxy_list_label.setOpaque(true);

        rec_smart_proxy_label.setFont(new java.awt.Font("Dialog", 2, 16)); // NOI18N
        rec_smart_proxy_label.setText("Note1: enable it in order to mitigate bandwidth limit. (Multislot is required) ");

        proxy_timeout_spinner.setFont(new java.awt.Font("Noto Sans", 0, 16)); // NOI18N
        proxy_timeout_spinner.setModel(new javax.swing.SpinnerNumberModel(10, 1, null, 1));

        force_smart_proxy_checkbox.setFont(new java.awt.Font("Noto Sans", 1, 16)); // NOI18N
        force_smart_proxy_checkbox.setText("FORCE SMART PROXY");

        jLabel7.setFont(new java.awt.Font("Noto Sans", 2, 16)); // NOI18N
        jLabel7.setText("Forces the use of smart proxy even if we still have direct bandwidth available (useful to test proxies)");

        jLabel8.setFont(new java.awt.Font("Noto Sans", 1, 16)); // NOI18N
        jLabel8.setText("Proxy list refresh (minutes):");

        auto_refresh_proxy_time_spinner.setFont(new java.awt.Font("Noto Sans", 0, 16)); // NOI18N
        auto_refresh_proxy_time_spinner.setModel(new javax.swing.SpinnerNumberModel(60, 1, null, 1));

        proxy_random_radio.setFont(new java.awt.Font("Noto Sans", 0, 16)); // NOI18N
        proxy_random_radio.setText("RANDOM");
        proxy_random_radio.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                proxy_random_radioActionPerformed(evt);
            }
        });

        proxy_sequential_radio.setFont(new java.awt.Font("Noto Sans", 0, 16)); // NOI18N
        proxy_sequential_radio.setText("SEQUENTIAL");
        proxy_sequential_radio.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                proxy_sequential_radioActionPerformed(evt);
            }
        });

        jLabel9.setFont(new java.awt.Font("Noto Sans", 1, 16)); // NOI18N
        jLabel9.setText("Proxy selection order:");

        proxy_reset_slot_checkbox.setFont(new java.awt.Font("Noto Sans", 1, 16)); // NOI18N
        proxy_reset_slot_checkbox.setText("Reset slot proxy after successfully downloading a chunk");

        jLabel10.setFont(new java.awt.Font("Noto Sans", 2, 16)); // NOI18N
        jLabel10.setText("(Useful to avoid getting trapped in slow proxies)");

        jLabel11.setFont(new java.awt.Font("Noto Sans", 2, 16)); // NOI18N
        jLabel11.setText("(If you have a list of proxies sorted from best to worst, check sequential)");

        javax.swing.GroupLayout smart_proxy_settingsLayout = new javax.swing.GroupLayout(smart_proxy_settings);
        smart_proxy_settings.setLayout(smart_proxy_settingsLayout);
        smart_proxy_settingsLayout.setHorizontalGroup(
            smart_proxy_settingsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(smart_proxy_settingsLayout.createSequentialGroup()
                .addComponent(force_smart_proxy_checkbox)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel7, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addComponent(custom_proxy_list_label, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jScrollPane1)
            .addGroup(smart_proxy_settingsLayout.createSequentialGroup()
                .addGroup(smart_proxy_settingsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(smart_proxy_settingsLayout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(smart_proxy_settingsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(rec_smart_proxy_label1)
                            .addComponent(rec_smart_proxy_label)
                            .addGroup(smart_proxy_settingsLayout.createSequentialGroup()
                                .addGroup(smart_proxy_settingsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                    .addGroup(smart_proxy_settingsLayout.createSequentialGroup()
                                        .addComponent(jLabel9)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 49, Short.MAX_VALUE)
                                        .addComponent(proxy_random_radio)
                                        .addGap(18, 18, 18)
                                        .addComponent(proxy_sequential_radio))
                                    .addGroup(smart_proxy_settingsLayout.createSequentialGroup()
                                        .addGroup(smart_proxy_settingsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                            .addComponent(jLabel5, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, 248, javax.swing.GroupLayout.PREFERRED_SIZE)
                                            .addComponent(jLabel8, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, 248, javax.swing.GroupLayout.PREFERRED_SIZE)
                                            .addComponent(jLabel3, javax.swing.GroupLayout.Alignment.LEADING))
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addGroup(smart_proxy_settingsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                            .addComponent(bad_proxy_time_spinner, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 150, Short.MAX_VALUE)
                                            .addComponent(auto_refresh_proxy_time_spinner, javax.swing.GroupLayout.Alignment.LEADING)
                                            .addComponent(proxy_timeout_spinner))))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(smart_proxy_settingsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabel4)
                                    .addComponent(jLabel6)
                                    .addComponent(jLabel11)))))
                    .addGroup(smart_proxy_settingsLayout.createSequentialGroup()
                        .addComponent(proxy_reset_slot_checkbox)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel10)))
                .addGap(0, 0, Short.MAX_VALUE))
        );
        smart_proxy_settingsLayout.setVerticalGroup(
            smart_proxy_settingsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(smart_proxy_settingsLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(rec_smart_proxy_label)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(rec_smart_proxy_label1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(smart_proxy_settingsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(proxy_random_radio)
                    .addComponent(proxy_sequential_radio)
                    .addComponent(jLabel9)
                    .addComponent(jLabel11))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(smart_proxy_settingsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel8)
                    .addComponent(auto_refresh_proxy_time_spinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(smart_proxy_settingsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(bad_proxy_time_spinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel4))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(smart_proxy_settingsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel5)
                    .addComponent(proxy_timeout_spinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel6))
                .addGap(7, 7, 7)
                .addGroup(smart_proxy_settingsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(proxy_reset_slot_checkbox)
                    .addComponent(jLabel10))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(smart_proxy_settingsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(force_smart_proxy_checkbox)
                    .addComponent(jLabel7))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(custom_proxy_list_label)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 344, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        remove_no_restart_checkbox.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        remove_no_restart_checkbox.setText("Auto-remove failed no-restart transfers");
        remove_no_restart_checkbox.setDoubleBuffered(true);
        remove_no_restart_checkbox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                remove_no_restart_checkboxActionPerformed(evt);
            }
        });

        remove_no_restart_checkbox1.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        remove_no_restart_checkbox1.setText("Auto-remove no-restart transfers");
        remove_no_restart_checkbox1.setDoubleBuffered(true);

        auto_restart_damaged_checkbox.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        auto_restart_damaged_checkbox.setText("Auto-restart damaged transfers");
        auto_restart_damaged_checkbox.setDoubleBuffered(true);

        javax.swing.GroupLayout downloads_panelLayout = new javax.swing.GroupLayout(downloads_panel);
        downloads_panel.setLayout(downloads_panelLayout);
        downloads_panelLayout.setHorizontalGroup(
            downloads_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(downloads_panelLayout.createSequentialGroup()
                .addGroup(downloads_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(downloads_panelLayout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(downloads_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(smart_proxy_checkbox)
                            .addGroup(downloads_panelLayout.createSequentialGroup()
                                .addComponent(max_downloads_label)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(max_downloads_spinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(multi_slot_down_checkbox)
                            .addGroup(downloads_panelLayout.createSequentialGroup()
                                .addComponent(change_download_dir_button)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(down_dir_label)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(default_dir_label))
                            .addComponent(megacrypter_reverse_checkbox)
                            .addComponent(use_mega_account_down_checkbox)
                            .addComponent(verify_file_down_checkbox)
                            .addComponent(limit_download_speed_checkbox)
                            .addComponent(clipboardspy_checkbox)
                            .addGroup(downloads_panelLayout.createSequentialGroup()
                                .addGap(21, 21, 21)
                                .addGroup(downloads_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(rec_download_slots_label)
                                    .addGroup(downloads_panelLayout.createSequentialGroup()
                                        .addComponent(default_slots_down_label)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(default_slots_down_spinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addGroup(downloads_panelLayout.createSequentialGroup()
                                        .addComponent(use_mega_label)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(use_mega_account_down_combobox, javax.swing.GroupLayout.PREFERRED_SIZE, 700, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addComponent(megacrypter_reverse_warning_label)
                                    .addGroup(downloads_panelLayout.createSequentialGroup()
                                        .addComponent(megacrypter_reverse_port_label)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(megacrypter_reverse_port_spinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addGroup(downloads_panelLayout.createSequentialGroup()
                                        .addComponent(max_down_speed_label)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(max_down_speed_spinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))))
                    .addGroup(downloads_panelLayout.createSequentialGroup()
                        .addGap(12, 12, 12)
                        .addComponent(smart_proxy_settings, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(downloads_panelLayout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(remove_no_restart_checkbox))
                    .addGroup(downloads_panelLayout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(auto_restart_damaged_checkbox)))
                .addContainerGap())
            .addGroup(downloads_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(downloads_panelLayout.createSequentialGroup()
                    .addGap(0, 435, Short.MAX_VALUE)
                    .addComponent(remove_no_restart_checkbox1)
                    .addGap(0, 436, Short.MAX_VALUE)))
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
                .addGroup(downloads_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(max_downloads_label)
                    .addComponent(max_downloads_spinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addComponent(multi_slot_down_checkbox)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(downloads_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(default_slots_down_label)
                    .addComponent(default_slots_down_spinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(rec_download_slots_label)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(clipboardspy_checkbox)
                .addGap(10, 10, 10)
                .addComponent(limit_download_speed_checkbox)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(downloads_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(max_down_speed_spinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(max_down_speed_label))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(verify_file_down_checkbox)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(auto_restart_damaged_checkbox)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(remove_no_restart_checkbox)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(use_mega_account_down_checkbox)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(downloads_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(use_mega_account_down_combobox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(use_mega_label))
                .addGap(18, 18, 18)
                .addComponent(megacrypter_reverse_checkbox)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(downloads_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(megacrypter_reverse_port_spinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(megacrypter_reverse_port_label))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(megacrypter_reverse_warning_label)
                .addGap(18, 18, 18)
                .addComponent(smart_proxy_checkbox)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(smart_proxy_settings, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
            .addGroup(downloads_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(downloads_panelLayout.createSequentialGroup()
                    .addGap(0, 1037, Short.MAX_VALUE)
                    .addComponent(remove_no_restart_checkbox1)
                    .addGap(0, 217, Short.MAX_VALUE)))
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
        max_up_speed_label.setEnabled(false);

        max_up_speed_spinner.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        max_up_speed_spinner.setEnabled(false);

        limit_upload_speed_checkbox.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        limit_upload_speed_checkbox.setText("Limit upload speed");
        limit_upload_speed_checkbox.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                limit_upload_speed_checkboxStateChanged(evt);
            }
        });

        rec_upload_slots_label.setFont(new java.awt.Font("Dialog", 2, 16)); // NOI18N
        rec_upload_slots_label.setText("Note: slots consume resources, so use them moderately.");

        thumbnail_checkbox.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        thumbnail_checkbox.setText("Create and upload image/video thumbnails");
        thumbnail_checkbox.setDoubleBuffered(true);

        upload_log_checkbox.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        upload_log_checkbox.setText("Create upload logs");
        upload_log_checkbox.setDoubleBuffered(true);

        upload_public_folder_checkbox.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        upload_public_folder_checkbox.setText("CREATE UPLOAD FOLDER PUBLIC LINK");
        upload_public_folder_checkbox.setDoubleBuffered(true);
        upload_public_folder_checkbox.setOpaque(true);
        upload_public_folder_checkbox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                upload_public_folder_checkboxActionPerformed(evt);
            }
        });

        public_folder_warning.setEditable(false);
        public_folder_warning.setBackground(new java.awt.Color(255, 255, 51));
        public_folder_warning.setColumns(20);
        public_folder_warning.setFont(new java.awt.Font("Noto Sans", 1, 18)); // NOI18N
        public_folder_warning.setForeground(new java.awt.Color(0, 51, 255));
        public_folder_warning.setLineWrap(true);
        public_folder_warning.setRows(5);
        public_folder_warning.setText("THIS OPTION IS NOT RECOMMENDED. Using this will cause MegaBasterd uploaded folder to appear in your account as NOT DECRYPTABLE. \n\nAt the time of writing this text, there is a method to FIX IT:\n\n1) Move first upload subfolder to the ROOT (CLOUD) folder of your account. \n\n2) Go to account settings and click RELOAD ACCOUNT. \n\nI don't know how long this method will last. USE THIS OPTION AT YOUR OWN RISK.");
        public_folder_warning.setWrapStyleWord(true);
        public_folder_panel.setViewportView(public_folder_warning);

        javax.swing.GroupLayout uploads_panelLayout = new javax.swing.GroupLayout(uploads_panel);
        uploads_panel.setLayout(uploads_panelLayout);
        uploads_panelLayout.setHorizontalGroup(
            uploads_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(uploads_panelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(uploads_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
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
                            .addComponent(max_uploads_spinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addComponent(thumbnail_checkbox)
                    .addComponent(upload_log_checkbox)
                    .addComponent(upload_public_folder_checkbox)
                    .addComponent(public_folder_panel, javax.swing.GroupLayout.PREFERRED_SIZE, 1003, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(rec_upload_slots_label))
                .addGap(0, 0, 0))
        );
        uploads_panelLayout.setVerticalGroup(
            uploads_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(uploads_panelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(uploads_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(max_uploads_label)
                    .addComponent(max_uploads_spinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(uploads_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(default_slots_up_label)
                    .addComponent(default_slots_up_spinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(rec_upload_slots_label)
                .addGap(18, 18, 18)
                .addComponent(limit_upload_speed_checkbox)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(uploads_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(max_up_speed_label)
                    .addComponent(max_up_speed_spinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addComponent(thumbnail_checkbox)
                .addGap(18, 18, 18)
                .addComponent(upload_log_checkbox)
                .addGap(18, 18, 18)
                .addComponent(upload_public_folder_checkbox)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(public_folder_panel, javax.swing.GroupLayout.PREFERRED_SIZE, 324, javax.swing.GroupLayout.PREFERRED_SIZE)
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

        jLabel1.setFont(new java.awt.Font("Dialog", 2, 14)); // NOI18N
        jLabel1.setText("Note: you can use a (optional) alias for your email addresses -> bob@supermail.com#bob_mail (don't forget to save after entering your accounts).");
        jLabel1.setDoubleBuffered(true);

        import_mega_button.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        import_mega_button.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/icons8-import-30.png"))); // NOI18N
        import_mega_button.setText("IMPORT ACCOUNTS (FILE)");
        import_mega_button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                import_mega_buttonActionPerformed(evt);
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
                    .addGroup(accounts_panelLayout.createSequentialGroup()
                        .addComponent(delete_all_accounts_button)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(unlock_accounts_button)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(encrypt_pass_checkbox))
                    .addGroup(accounts_panelLayout.createSequentialGroup()
                        .addComponent(remove_mega_account_button)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(import_mega_button)
                        .addGap(18, 18, 18)
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
                .addComponent(mega_accounts_scrollpane, javax.swing.GroupLayout.DEFAULT_SIZE, 395, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(accounts_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(remove_mega_account_button)
                    .addComponent(add_mega_account_button)
                    .addComponent(import_mega_button))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(elc_accounts_label)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(elc_accounts_scrollpane, javax.swing.GroupLayout.DEFAULT_SIZE, 395, Short.MAX_VALUE)
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

        proxy_warning_label.setFont(new java.awt.Font("Dialog", 2, 14)); // NOI18N
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
                .addComponent(proxy_user_textfield, javax.swing.GroupLayout.DEFAULT_SIZE, 443, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(proxy_pass_label)
                .addGap(6, 6, 6)
                .addComponent(proxy_pass_textfield, javax.swing.GroupLayout.DEFAULT_SIZE, 499, Short.MAX_VALUE)
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

        rec_zoom_label.setFont(new java.awt.Font("Dialog", 2, 16)); // NOI18N
        rec_zoom_label.setText("Note: restart might be required.");
        rec_zoom_label.setDoubleBuffered(true);

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
        debug_file_checkbox.setText("Save debug info to file -> ");

        jButton1.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        jButton1.setForeground(new java.awt.Color(255, 0, 0));
        jButton1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/icons8-cancel-30.png"))); // NOI18N
        jButton1.setText("RESET MEGABASTERD");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

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

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jButton1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(import_settings_button)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(export_settings_button, javax.swing.GroupLayout.PREFERRED_SIZE, 232, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(import_settings_button)
                    .addComponent(export_settings_button))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButton1)
                .addContainerGap())
        );

        jLabel2.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        jLabel2.setText("Language:");

        font_label.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        font_label.setText("Font:");

        language_combo.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N

        font_combo.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N

        zoom_label.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        zoom_label.setText("Font ZOOM (%):");
        zoom_label.setDoubleBuffered(true);

        zoom_spinner.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        zoom_spinner.setDoubleBuffered(true);

        dark_mode_checkbox.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        dark_mode_checkbox.setText("DARK MODE");

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                        .addGroup(jPanel2Layout.createSequentialGroup()
                            .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(zoom_label)
                                .addComponent(font_label))
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                .addComponent(font_combo, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(zoom_spinner, javax.swing.GroupLayout.PREFERRED_SIZE, 351, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addGroup(jPanel2Layout.createSequentialGroup()
                            .addComponent(jLabel2)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(language_combo, javax.swing.GroupLayout.PREFERRED_SIZE, 351, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addComponent(dark_mode_checkbox))
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(font_label)
                    .addComponent(font_combo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(zoom_label)
                    .addComponent(zoom_spinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(language_combo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(dark_mode_checkbox)
                .addContainerGap())
        );

        debug_file_path.setFont(new java.awt.Font("Noto Sans", 0, 18)); // NOI18N
        debug_file_path.setText(MainPanel.MEGABASTERD_HOME_DIR + "/MEGABASTERD_DEBUG.log");

        file_regex_checkbox.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        file_regex_checkbox.setText("Automatically remove files that match this REGEX:");
        file_regex_checkbox.setToolTipText("");
        file_regex_checkbox.setDoubleBuffered(true);
        file_regex_checkbox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                file_regex_checkboxActionPerformed(evt);
            }
        });

        file_regex_textfield.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        file_regex_textfield.setDoubleBuffered(true);
        file_regex_textfield.setEnabled(false);

        file_regex101_label.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        file_regex101_label.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        file_regex101_label.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        file_regex101_label.setEnabled(false);

        javax.swing.GroupLayout advanced_panelLayout = new javax.swing.GroupLayout(advanced_panel);
        advanced_panel.setLayout(advanced_panelLayout);
        advanced_panelLayout.setHorizontalGroup(
            advanced_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(advanced_panelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(advanced_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(proxy_panel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(advanced_panelLayout.createSequentialGroup()
                        .addComponent(run_command_test_button)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(run_command_textbox))
                    .addGroup(advanced_panelLayout.createSequentialGroup()
                        .addGroup(advanced_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(start_frozen_checkbox)
                            .addGroup(advanced_panelLayout.createSequentialGroup()
                                .addComponent(debug_file_checkbox)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(debug_file_path))
                            .addGroup(advanced_panelLayout.createSequentialGroup()
                                .addGap(165, 165, 165)
                                .addComponent(custom_chunks_dir_current_label))
                            .addComponent(rec_zoom_label)
                            .addComponent(run_command_checkbox)
                            .addGroup(advanced_panelLayout.createSequentialGroup()
                                .addComponent(custom_chunks_dir_checkbox)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(custom_chunks_dir_button))
                            .addGroup(advanced_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                .addComponent(jPanel1, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(jPanel2, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addComponent(file_regex_textfield)
                    .addGroup(advanced_panelLayout.createSequentialGroup()
                        .addComponent(file_regex_checkbox)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(file_regex101_label)))
                .addContainerGap())
        );
        advanced_panelLayout.setVerticalGroup(
            advanced_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(advanced_panelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(advanced_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(custom_chunks_dir_checkbox)
                    .addComponent(custom_chunks_dir_button))
                .addGap(0, 0, 0)
                .addComponent(custom_chunks_dir_current_label)
                .addGap(18, 18, 18)
                .addComponent(start_frozen_checkbox)
                .addGap(18, 18, 18)
                .addGroup(advanced_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(debug_file_checkbox)
                    .addComponent(debug_file_path))
                .addGap(18, 18, 18)
                .addComponent(run_command_checkbox)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(advanced_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(run_command_textbox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(run_command_test_button))
                .addGap(7, 7, 7)
                .addGroup(advanced_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(file_regex_checkbox)
                    .addComponent(file_regex101_label))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(file_regex_textfield, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(2, 2, 2)
                .addComponent(proxy_panel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(rec_zoom_label)
                .addContainerGap(278, Short.MAX_VALUE))
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
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(status, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(save_button)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(cancel_button))
                    .addComponent(panel_tabs))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(panel_tabs, javax.swing.GroupLayout.DEFAULT_SIZE, 1082, Short.MAX_VALUE)
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

        if (!this.save_button.isEnabled()) {

            Object[] options = {"No",
                LabelTranslatorSingleton.getInstance().translate("Yes")};

            int n = 1;
            n = showOptionDialog(this,
                    LabelTranslatorSingleton.getInstance().translate("SURE?"),
                    LabelTranslatorSingleton.getInstance().translate("EXIT"), YES_NO_CANCEL_OPTION, javax.swing.JOptionPane.WARNING_MESSAGE,
                    null,
                    options,
                    options[0]);

            if (n == 1) {
                _exit = true;
                dispose();
            }

        } else {
            _exit = true;
            dispose();
        }

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
            settings.put("auto_restart_damaged", auto_restart_damaged_checkbox.isSelected() ? "yes" : "no");
            settings.put("remove_no_restart", remove_no_restart_checkbox.isSelected() ? "yes" : "no");
            settings.put("limit_download_speed", limit_download_speed_checkbox.isSelected() ? "yes" : "no");
            settings.put("max_download_speed", String.valueOf(max_down_speed_spinner.getValue()));
            settings.put("limit_upload_speed", limit_upload_speed_checkbox.isSelected() ? "yes" : "no");
            settings.put("max_upload_speed", String.valueOf(max_up_speed_spinner.getValue()));
            settings.put("use_mega_account_down", use_mega_account_down_checkbox.isSelected() ? "yes" : "no");
            settings.put("mega_account_down", use_mega_account_down_combobox.getSelectedItem());
            settings.put("megacrypter_reverse", megacrypter_reverse_checkbox.isSelected() ? "yes" : "no");
            settings.put("megacrypter_reverse_port", String.valueOf(megacrypter_reverse_port_spinner.getValue()));
            settings.put("start_frozen", start_frozen_checkbox.isSelected() ? "yes" : "no");
            settings.put("use_custom_chunks_dir", custom_chunks_dir_checkbox.isSelected() ? "yes" : "no");
            settings.put("custom_chunks_dir", _custom_chunks_dir);
            settings.put("run_command", run_command_checkbox.isSelected() ? "yes" : "no");
            settings.put("run_command_path", run_command_textbox.getText());
            settings.put("use_file_regex", file_regex_checkbox.isSelected() ? "yes" : "no");
            settings.put("file_regex_pattern", file_regex_textfield.getText());
            settings.put("clipboardspy", clipboardspy_checkbox.isSelected() ? "yes" : "no");
            settings.put("thumbnails", thumbnail_checkbox.isSelected() ? "yes" : "no");
            settings.put("upload_log", upload_log_checkbox.isSelected() ? "yes" : "no");
            settings.put("force_smart_proxy", force_smart_proxy_checkbox.isSelected() ? "yes" : "no");
            settings.put("reset_slot_proxy", proxy_reset_slot_checkbox.isSelected() ? "yes" : "no");
            settings.put("random_proxy", proxy_random_radio.isSelected() ? "yes" : "no");
            settings.put("dark_mode", dark_mode_checkbox.isSelected() ? "yes" : "no");
            settings.put("upload_public_folder", upload_public_folder_checkbox.isSelected() ? "yes" : "no");
            settings.put("smartproxy_ban_time", String.valueOf(bad_proxy_time_spinner.getValue()));
            settings.put("smartproxy_timeout", String.valueOf(proxy_timeout_spinner.getValue()));
            settings.put("smartproxy_autorefresh_time", String.valueOf(auto_refresh_proxy_time_spinner.getValue()));

            if (upload_log_checkbox.isSelected()) {
                createUploadLogDir();
            }

            if (custom_proxy_textarea.getText().trim().isEmpty()) {
                smart_proxy_checkbox.setSelected(false);
            }

            settings.put("smart_proxy", smart_proxy_checkbox.isSelected() ? "yes" : "no");
            settings.put("custom_proxy_list", custom_proxy_textarea.getText());

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

            String old_language = DBTools.selectSettingValue("language");

            if (old_language == null) {
                old_language = MainPanel.DEFAULT_LANGUAGE;
            }

            String language = (String) language_combo.getSelectedItem();

            if (language.equals(LabelTranslatorSingleton.getInstance().translate("English"))) {
                language = "EN";
            } else if (language.equals(LabelTranslatorSingleton.getInstance().translate("Spanish"))) {
                language = "ES";
            } else if (language.equals(LabelTranslatorSingleton.getInstance().translate("Italian"))) {
                language = "IT";
            } else if (language.equals(LabelTranslatorSingleton.getInstance().translate("German"))) {
                language = "GE";
            } else if (language.equals(LabelTranslatorSingleton.getInstance().translate("Hungarian"))) {
                language = "HU";
            } else if (language.equals(LabelTranslatorSingleton.getInstance().translate("Turkish"))) {
                language = "TU";
                font = "DEFAULT";
            } else if (language.equals(LabelTranslatorSingleton.getInstance().translate("Chinese"))) {
                language = "CH";
                font = "ALTERNATIVE";
            } else if (language.equals(LabelTranslatorSingleton.getInstance().translate("Vietnamese"))) {
                language = "VI";
            }

            settings.put("font", font);

            settings.put("language", language);

            String old_zoom = DBTools.selectSettingValue("font_zoom");

            if (old_zoom == null) {

                old_zoom = String.valueOf(Math.round(100 * MainPanel.ZOOM_FACTOR));
            }

            String zoom = String.valueOf(zoom_spinner.getValue());

            boolean old_dark_mode = false;

            String dark_mode_val = DBTools.selectSettingValue("dark_mode");

            if (dark_mode_val != null) {
                old_dark_mode = (dark_mode_val.equals("yes"));
            }

            boolean dark_mode = dark_mode_checkbox.isSelected();

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

            String old_debug_file = DBTools.selectSettingValue("debug_file");

            if (old_debug_file == null) {

                old_debug_file = "no";
            }

            String debug_file = debug_file_checkbox.isSelected() ? "yes" : "no";

            settings.put("debug_file", debug_file);
            settings.put("use_proxy", use_proxy ? "yes" : "no");
            settings.put("proxy_host", proxy_host);
            settings.put("proxy_port", proxy_port);
            settings.put("proxy_user", proxy_user);
            settings.put("proxy_pass", proxy_pass);
            settings.put("font_zoom", zoom);

            insertSettingsValues(settings);

            if (!debug_file.equals(old_debug_file)
                    || !font.equals(old_font)
                    || !language.equals(old_language)
                    || !zoom.equals(old_zoom)
                    || use_proxy != old_use_proxy
                    || !proxy_host.equals(old_proxy_host)
                    || !proxy_port.equals(old_proxy_port)
                    || !proxy_user.equals(old_proxy_user)
                    || !proxy_pass.equals(old_proxy_pass)
                    || dark_mode != old_dark_mode) {

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

                                user_table = Bin2BASE64(CryptTools.aes_cbc_encrypt_pkcs7(user_table.getBytes(StandardCharsets.UTF_8), _main_panel.getMaster_pass(), CryptTools.AES_ZERO_IV));

                                apikey_table = Bin2BASE64(CryptTools.aes_cbc_encrypt_pkcs7(apikey_table.getBytes(StandardCharsets.UTF_8), _main_panel.getMaster_pass(), CryptTools.AES_ZERO_IV));
                            }

                            DBTools.insertELCAccount(host_table, user_table, apikey_table);

                        } else {

                            HashMap<String, Object> elc_account_data = (HashMap) _main_panel.getElc_accounts().get(host_table);

                            String user = (String) elc_account_data.get("user");

                            String apikey = (String) elc_account_data.get("apikey");

                            if (_main_panel.getMaster_pass() != null) {

                                try {
                                    user = new String(CryptTools.aes_cbc_decrypt_pkcs7(BASE642Bin(user), _main_panel.getMaster_pass(), CryptTools.AES_ZERO_IV), StandardCharsets.UTF_8);
                                    apikey = new String(CryptTools.aes_cbc_decrypt_pkcs7(BASE642Bin(apikey), _main_panel.getMaster_pass(), CryptTools.AES_ZERO_IV), StandardCharsets.UTF_8);
                                } catch (Exception ex) {
                                    LOG.fatal("Error setting up api-key auth! {}", ex.getMessage());
                                }
                            }

                            if (!user.equals(user_table) || !apikey.equals(apikey_table)) {

                                user = user_table;

                                apikey = apikey_table;

                                if (_main_panel.getMaster_pass() != null) {

                                    user = Bin2BASE64(CryptTools.aes_cbc_encrypt_pkcs7(user_table.getBytes(StandardCharsets.UTF_8), _main_panel.getMaster_pass(), CryptTools.AES_ZERO_IV));

                                    apikey = Bin2BASE64(CryptTools.aes_cbc_encrypt_pkcs7(apikey_table.getBytes(StandardCharsets.UTF_8), _main_panel.getMaster_pass(), CryptTools.AES_ZERO_IV));

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

                cancel_button.setEnabled(true);

                import_mega_button.setEnabled(false);

                remove_mega_account_button.setEnabled(false);

                remove_elc_account_button.setEnabled(false);

                add_mega_account_button.setEnabled(false);

                add_elc_account_button.setEnabled(false);

                delete_all_accounts_button.setEnabled(false);

                mega_accounts_table.setEnabled(false);

                elc_accounts_table.setEnabled(false);

                encrypt_pass_checkbox.setEnabled(false);

                final Dialog self = this;

                THREAD_POOL.execute(() -> {
                    ArrayList<String> email_error = new ArrayList<>();
                    ArrayList<String> new_valid_mega_accounts = new ArrayList<>();
                    for (int i = 0; i < model_row_count && !_exit; i++) {

                        String email = (String) model.getValueAt(i, 0);

                        String pass = (String) model.getValueAt(i, 1);

                        int j = i;

                        MiscTools.GUIRun(() -> {

                            status.setText(LabelTranslatorSingleton.getInstance().translate("Checking your MEGA accounts, please wait... ") + email + " (" + String.valueOf(j + 1) + "/" + String.valueOf(model_row_count) + ")");

                        });

                        if (!email.isEmpty() && !pass.isEmpty()) {

                            new_valid_mega_accounts.add(email);

                            MegaAPI ma;

                            if (_main_panel.getMega_accounts().get(email) == null) {

                                ma = new MegaAPI();

                                try {

                                    String pincode = null;

                                    boolean error_2FA = false;

                                    if (!_main_panel.getMega_active_accounts().containsKey(email) && ma.check2FA(email)) {

                                        Get2FACode dialog = new Get2FACode((Frame) getParent(), true, email, _main_panel);

                                        dialog.setLocationRelativeTo(self);

                                        dialog.setVisible(true);

                                        if (dialog.isCode_ok()) {
                                            pincode = dialog.getPin_code();
                                        } else {
                                            error_2FA = true;
                                        }
                                    }

                                    if (!error_2FA) {
                                        if (!_main_panel.getMega_active_accounts().containsKey(email)) {
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
                                                password = Bin2BASE64(CryptTools.aes_cbc_encrypt_pkcs7(pass.getBytes(StandardCharsets.UTF_8), _main_panel.getMaster_pass(), CryptTools.AES_ZERO_IV));
                                                password_aes = Bin2BASE64(CryptTools.aes_cbc_encrypt_pkcs7(i32a2bin(ma.getPassword_aes()), _main_panel.getMaster_pass(), CryptTools.AES_ZERO_IV));
                                                user_hash = Bin2BASE64(CryptTools.aes_cbc_encrypt_pkcs7(UrlBASE642Bin(ma.getUser_hash()), _main_panel.getMaster_pass(), CryptTools.AES_ZERO_IV));
                                            }

                                            DBTools.insertMegaAccount(email, password, password_aes, user_hash);
                                        }
                                    } else email_error.add(email);
                                } catch (Exception ex) {
                                    email_error.add(email);
                                    LOG.fatal("Failed to save settings! {}", ex.getMessage());
                                }

                            } else {

                                HashMap<String, Object> mega_account_data = (HashMap) _main_panel.getMega_accounts().get(email);

                                String password = (String) mega_account_data.get("password");

                                if (_main_panel.getMaster_pass() != null) {

                                    try {
                                        password = new String(CryptTools.aes_cbc_decrypt_pkcs7(BASE642Bin(password), _main_panel.getMaster_pass(), CryptTools.AES_ZERO_IV), StandardCharsets.UTF_8);
                                    } catch (Exception ex) {
                                        LOG.fatal("Failed to generate password! {}", ex.getMessage());
                                    }
                                }

                                if (!password.equals(pass)) {

                                    ma = new MegaAPI();

                                    try {

                                        String pincode = null;

                                        boolean error_2FA = false;

                                        if (!_main_panel.getMega_active_accounts().containsKey(email) && ma.check2FA(email)) {

                                            Get2FACode dialog = new Get2FACode((Frame) getParent(), true, email, _main_panel);

                                            dialog.setLocationRelativeTo(self);

                                            dialog.setVisible(true);

                                            if (dialog.isCode_ok()) {
                                                pincode = dialog.getPin_code();
                                            } else {
                                                error_2FA = true;
                                            }
                                        }

                                        if (!error_2FA) {
                                            if (!_main_panel.getMega_active_accounts().containsKey(email)) {
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

                                                    password = Bin2BASE64(CryptTools.aes_cbc_encrypt_pkcs7(pass.getBytes(StandardCharsets.UTF_8), _main_panel.getMaster_pass(), CryptTools.AES_ZERO_IV));

                                                    password_aes = Bin2BASE64(CryptTools.aes_cbc_encrypt_pkcs7(i32a2bin(ma.getPassword_aes()), _main_panel.getMaster_pass(), CryptTools.AES_ZERO_IV));

                                                    user_hash = Bin2BASE64(CryptTools.aes_cbc_encrypt_pkcs7(UrlBASE642Bin(ma.getUser_hash()), _main_panel.getMaster_pass(), CryptTools.AES_ZERO_IV));
                                                }

                                                DBTools.insertMegaAccount(email, password, password_aes, user_hash);
                                            }
                                        } else {
                                            email_error.add(email);
                                        }

                                    } catch (Exception ex) {
                                        email_error.add(email);
                                        LOG.fatal("Failed to authenticate! {}", ex.getMessage());
                                    }
                                }
                            }
                        }
                    }
                    if (!_exit) {
                        if (!email_error.isEmpty()) {
                            String email_error_s = "";
                            email_error_s = email_error.stream().map((s) -> s + "\n").reduce(email_error_s, String::concat);
                            final String final_email_error = email_error_s;
                            MiscTools.GUIRun(() -> {
                                status.setText("");

                                JOptionPane.showMessageDialog(self, LabelTranslatorSingleton.getInstance().translate("There were errors with some accounts (email and/or password are/is wrong). Please, check them:\n\n") + final_email_error, "Mega Account Check Error", JOptionPane.ERROR_MESSAGE);

                                save_button.setEnabled(true);

                                cancel_button.setEnabled(true);

                                panel_tabs.setEnabled(true);

                                import_mega_button.setEnabled(true);

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
                            _main_panel.getMega_accounts().keySet().stream().filter((email) -> (!new_valid_mega_accounts.contains(email))).forEachOrdered(_deleted_mega_accounts::add);
                            MiscTools.GUIRun(() -> {
                                status.setText("");
                                JOptionPane.showMessageDialog(self, LabelTranslatorSingleton.getInstance().translate("Settings successfully saved!"), LabelTranslatorSingleton.getInstance().translate("Settings saved"), JOptionPane.INFORMATION_MESSAGE);
                                _settings_ok = true;
                                setDefaultCloseOperation(DISPOSE_ON_CLOSE);
                                setVisible(false);
                            });
                        }
                    }
                });

            } else {

                JOptionPane.showMessageDialog(this, LabelTranslatorSingleton.getInstance().translate("Settings successfully saved!"), LabelTranslatorSingleton.getInstance().translate("Settings saved"), JOptionPane.INFORMATION_MESSAGE);
                _settings_ok = true;
                setDefaultCloseOperation(DISPOSE_ON_CLOSE);
                setVisible(false);
            }

        } catch (Exception ex) {
            LOG.fatal("Exception captured trying to save Settings! {}", ex.getMessage());
        }
    }//GEN-LAST:event_save_buttonActionPerformed

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

        final Dialog self = this;

        MiscTools.GUIRun(() -> {
            GetMasterPasswordDialog dialog = new GetMasterPasswordDialog((Frame) getParent(), true, _main_panel.getMaster_pass_hash(), _main_panel.getMaster_pass_salt(), _main_panel);

            dialog.setLocationRelativeTo(self);

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
                        pass = new String(CryptTools.aes_cbc_decrypt_pkcs7(BASE642Bin((String) data.get("password")), _main_panel.getMaster_pass(), CryptTools.AES_ZERO_IV), StandardCharsets.UTF_8);
                    } catch (Exception ex) {
                        LOG.fatal("Accounts: Exception captured! {}", ex.getMessage());
                    }
                    return new String[]{pair.getKey(), pass};
                }).forEachOrdered(mega_model::addRow);
                _main_panel.getElc_accounts().entrySet().stream().map((pair) -> {
                    HashMap<String, Object> data = (HashMap) pair.getValue();
                    String user = null, apikey = null;
                    try {
                        user = new String(CryptTools.aes_cbc_decrypt_pkcs7(BASE642Bin((String) data.get("user")), _main_panel.getMaster_pass(), CryptTools.AES_ZERO_IV), StandardCharsets.UTF_8);
                        apikey = new String(CryptTools.aes_cbc_decrypt_pkcs7(BASE642Bin((String) data.get("apikey")), _main_panel.getMaster_pass(), CryptTools.AES_ZERO_IV), StandardCharsets.UTF_8);
                    } catch (Exception ex) {
                        LOG.fatal("ELC Accounts: Exception captured! {}", ex.getMessage());
                    }
                    return new String[]{pair.getKey(), user, apikey};
                }).forEachOrdered(elc_model::addRow);

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
                LOG.fatal("Failed to delete from DB! {}", ex.getMessage());
            }
        }
    }//GEN-LAST:event_delete_all_accounts_buttonActionPerformed

    private void encrypt_pass_checkboxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_encrypt_pass_checkboxActionPerformed

        encrypt_pass_checkbox.setEnabled(false);

        final Dialog self = this;

        MiscTools.GUIRun(() -> {
            SetMasterPasswordDialog dialog = new SetMasterPasswordDialog((Frame) getParent(), true, _main_panel.getMaster_pass_salt(), _main_panel);

            dialog.setLocationRelativeTo(self);

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

                            password = new String(CryptTools.aes_cbc_decrypt_pkcs7(BASE642Bin((String) data.get("password")), old_master_pass, CryptTools.AES_ZERO_IV), StandardCharsets.UTF_8);

                            password_aes = Bin2BASE64(CryptTools.aes_cbc_decrypt_pkcs7(BASE642Bin((String) data.get("password_aes")), old_master_pass, CryptTools.AES_ZERO_IV));

                            user_hash = Bin2BASE64(CryptTools.aes_cbc_decrypt_pkcs7(BASE642Bin((String) data.get("user_hash")), old_master_pass, CryptTools.AES_ZERO_IV));

                        } else {

                            password = (String) data.get("password");

                            password_aes = (String) data.get("password_aes");

                            user_hash = (String) data.get("user_hash");
                        }

                        if (_main_panel.getMaster_pass() != null) {

                            password = Bin2BASE64(CryptTools.aes_cbc_encrypt_pkcs7(password.getBytes(StandardCharsets.UTF_8), _main_panel.getMaster_pass(), CryptTools.AES_ZERO_IV));

                            password_aes = Bin2BASE64(CryptTools.aes_cbc_encrypt_pkcs7(BASE642Bin(password_aes), _main_panel.getMaster_pass(), CryptTools.AES_ZERO_IV));

                            user_hash = Bin2BASE64(CryptTools.aes_cbc_encrypt_pkcs7(BASE642Bin(user_hash.replace('-', '+').replace('_', '/')), _main_panel.getMaster_pass(), CryptTools.AES_ZERO_IV));
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

                            user = new String(CryptTools.aes_cbc_decrypt_pkcs7(BASE642Bin((String) data.get("user")), old_master_pass, CryptTools.AES_ZERO_IV), StandardCharsets.UTF_8);

                            apikey = new String(CryptTools.aes_cbc_decrypt_pkcs7(BASE642Bin((String) data.get("apikey")), old_master_pass, CryptTools.AES_ZERO_IV), StandardCharsets.UTF_8);

                        } else {

                            user = (String) data.get("user");

                            apikey = (String) data.get("apikey");

                        }

                        if (_main_panel.getMaster_pass() != null) {

                            user = Bin2BASE64(CryptTools.aes_cbc_encrypt_pkcs7(user.getBytes(StandardCharsets.UTF_8), _main_panel.getMaster_pass(), CryptTools.AES_ZERO_IV));

                            apikey = Bin2BASE64(CryptTools.aes_cbc_encrypt_pkcs7(apikey.getBytes(StandardCharsets.UTF_8), _main_panel.getMaster_pass(), CryptTools.AES_ZERO_IV));
                        }

                        data.put("user", user);
                        data.put("apikey", apikey);
                        DBTools.insertELCAccount(host, user, apikey);
                    }

                } catch (Exception ex) {
                    LOG.fatal("Error setting up auth! {}", ex.getMessage());
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

    private void limit_upload_speed_checkboxStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_limit_upload_speed_checkboxStateChanged

        max_up_speed_label.setEnabled(limit_upload_speed_checkbox.isSelected());
        max_up_speed_spinner.setEnabled(limit_upload_speed_checkbox.isSelected());
    }//GEN-LAST:event_limit_upload_speed_checkboxStateChanged

    private void run_command_test_buttonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_run_command_test_buttonActionPerformed
        // TODO add your handling code here:

        if (run_command_textbox.getText() != null && !run_command_textbox.getText().trim().isEmpty()) {

            try {
                Runtime.getRuntime().exec(run_command_textbox.getText().trim());
            } catch (IOException ex) {
                LOG.fatal("Could not run command! {}", ex.getMessage());
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }//GEN-LAST:event_run_command_test_buttonActionPerformed

    private void run_command_checkboxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_run_command_checkboxActionPerformed
        // TODO add your handling code here:

        run_command_textbox.setEnabled(run_command_checkbox.isSelected());
    }//GEN-LAST:event_run_command_checkboxActionPerformed

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
            _main_panel.byeByeNow(true, true);

        }
    }//GEN-LAST:event_jButton1ActionPerformed

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

                        settings.put("settings", getSettingsCache());

                        settings.put("mega_accounts", selectMegaAccounts());

                        settings.put("elc_accounts", selectELCAccounts());

                        oos.writeObject(settings);

                        JOptionPane.showMessageDialog(this, LabelTranslatorSingleton.getInstance().translate("Settings successfully exported!"), LabelTranslatorSingleton.getInstance().translate("Settings exported"), JOptionPane.INFORMATION_MESSAGE);

                        setVisible(false);

                    } catch (SQLException ex) {
                        LOG.fatal("Settings export failed! {}", ex.getMessage());
                    }
                } catch (IOException ex) {
                    LOG.fatal("IO Exception in settings export! {}", ex.getMessage());
                }
            }
        }
    }//GEN-LAST:event_export_settings_buttonActionPerformed

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
            JFileChooser fileChooser = new JFileChooser();
            updateFonts(fileChooser, GUI_FONT, (float) (_main_panel.getZoom_factor() * 1.25));
            fileChooser.setCurrentDirectory(new File(_download_path));
            fileChooser.setDialogTitle("Select settings file");

            if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {

                File file = fileChooser.getSelectedFile();

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
                        LOG.fatal("Could not import settings! {}", ex.getMessage());
                    }
                } catch (IOException ex) {
                    LOG.fatal("IO Exception reading settings! {}", ex.getMessage());
                }
            }
        }
    }//GEN-LAST:event_import_settings_buttonActionPerformed

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

    private void import_mega_buttonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_import_mega_buttonActionPerformed
        // TODO add your handling code here:

        if (!unlock_accounts_button.isVisible() || !unlock_accounts_button.isEnabled()) {

            JOptionPane.showMessageDialog(this, LabelTranslatorSingleton.getInstance().translate("EMAIL1#PASS1\nEMAIL2#PASS2"), "TXT FILE FORMAT", JOptionPane.INFORMATION_MESSAGE);

            javax.swing.JFileChooser filechooser = new javax.swing.JFileChooser();

            updateFonts(filechooser, GUI_FONT, (float) (_main_panel.getZoom_factor() * 1.25));

            filechooser.setDialogTitle("Select MEGA ACCOUNTS FILE");

            filechooser.setFileSelectionMode(javax.swing.JFileChooser.FILES_ONLY);

            filechooser.addChoosableFileFilter(new FileNameExtensionFilter("TXT", "txt"));

            filechooser.setAcceptAllFileFilterUsed(false);

            if (filechooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {

                try {
                    final File file = filechooser.getSelectedFile();

                    Stream<String> filter = Files.lines(file.toPath()).map(s -> s.trim()).filter(s -> !s.isEmpty());

                    List<String> result = filter.collect(Collectors.toList());

                    DefaultTableModel model = (DefaultTableModel) mega_accounts_table.getModel();

                    for (String line : result) {

                        String email = MiscTools.findFirstRegex("^[^#]+", line, 0).trim();
                        String pass = MiscTools.findFirstRegex("^[^#]+#(.+)$", line, 1);
                        model.addRow(new Object[]{email, pass});
                    }

                    mega_accounts_table.setModel(model);

                } catch (IOException ex) {
                    LOG.fatal("IOException in showing dialog!", ex);
                }

            }
        } else {
            JOptionPane.showMessageDialog(this, LabelTranslatorSingleton.getInstance().translate("MEGA ACCOUNTS ARE LOCKED"), "ERROR", JOptionPane.ERROR_MESSAGE);

        }
    }//GEN-LAST:event_import_mega_buttonActionPerformed

    private void upload_public_folder_checkboxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_upload_public_folder_checkboxActionPerformed
        // TODO add your handling code here:
        if (this.upload_public_folder_checkbox.isSelected()) {
            JOptionPane.showMessageDialog(this, LabelTranslatorSingleton.getInstance().translate("Using this option may irreversibly corrupt your uploads.\n\nUSE IT AT YOUR OWN RISK"), LabelTranslatorSingleton.getInstance().translate("WARNING"), JOptionPane.WARNING_MESSAGE);

        }

        this.upload_public_folder_checkbox.setBackground(this.upload_public_folder_checkbox.isSelected() ? java.awt.Color.RED : null);

        this.public_folder_panel.setVisible(this.upload_public_folder_checkbox.isSelected());

        revalidate();

        repaint();

    }//GEN-LAST:event_upload_public_folder_checkboxActionPerformed

    private void file_regex_checkboxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_file_regex_checkboxActionPerformed
        // TODO add your handling code here:
        file_regex_textfield.setEnabled(file_regex_checkbox.isSelected());
        file_regex101_label.setEnabled(file_regex_checkbox.isSelected());
        InputVerifier regexVerifier = file_regex_textfield.getInputVerifier();
        if (regexVerifier != null && file_regex_checkbox.isSelected())
            regexVerifier.verify(file_regex_textfield);
        else
            file_regex101_label.setText("");
    }//GEN-LAST:event_file_regex_checkboxActionPerformed

    private void proxy_sequential_radioActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_proxy_sequential_radioActionPerformed
        // TODO add your handling code here:
        proxy_sequential_radio.setSelected(true);
        proxy_random_radio.setSelected(false);
    }//GEN-LAST:event_proxy_sequential_radioActionPerformed

    private void proxy_random_radioActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_proxy_random_radioActionPerformed
        // TODO add your handling code here:
        proxy_random_radio.setSelected(true);
        proxy_sequential_radio.setSelected(false);
    }//GEN-LAST:event_proxy_random_radioActionPerformed

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

    private void smart_proxy_checkboxMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_smart_proxy_checkboxMouseClicked
        // TODO add your handling code here:
        if (this.smart_proxy_checkbox.isSelected()) {
            JOptionPane.showMessageDialog(this, LabelTranslatorSingleton.getInstance().translate("Using proxies or VPN to bypass MEGA's daily download limitation may violate its Terms of Use.\n\nUSE THIS OPTION AT YOUR OWN RISK."), LabelTranslatorSingleton.getInstance().translate("WARNING"), JOptionPane.WARNING_MESSAGE);
        }
    }//GEN-LAST:event_smart_proxy_checkboxMouseClicked

    private void smart_proxy_checkboxStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_smart_proxy_checkboxStateChanged

        MiscTools.containerSetEnabled(smart_proxy_settings, smart_proxy_checkbox.isSelected());
        revalidate();
        repaint();
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

    private void verify_file_down_checkboxStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_verify_file_down_checkboxStateChanged
        // TODO add your handling code here:
        auto_restart_damaged_checkbox.setEnabled(verify_file_down_checkbox.isSelected());

    }//GEN-LAST:event_verify_file_down_checkboxStateChanged

    private void remove_no_restart_checkboxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_remove_no_restart_checkboxActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_remove_no_restart_checkboxActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel accounts_panel;
    private javax.swing.JButton add_elc_account_button;
    private javax.swing.JButton add_mega_account_button;
    private javax.swing.JPanel advanced_panel;
    private javax.swing.JScrollPane advanced_scrollpane;
    private javax.swing.JSpinner auto_refresh_proxy_time_spinner;
    private javax.swing.JCheckBox auto_restart_damaged_checkbox;
    private javax.swing.JSpinner bad_proxy_time_spinner;
    private javax.swing.JButton cancel_button;
    private javax.swing.JButton change_download_dir_button;
    private javax.swing.JCheckBox clipboardspy_checkbox;
    private javax.swing.JButton custom_chunks_dir_button;
    private javax.swing.JCheckBox custom_chunks_dir_checkbox;
    private javax.swing.JLabel custom_chunks_dir_current_label;
    private javax.swing.JLabel custom_proxy_list_label;
    private javax.swing.JTextArea custom_proxy_textarea;
    private javax.swing.JCheckBox dark_mode_checkbox;
    private javax.swing.JCheckBox debug_file_checkbox;
    private javax.swing.JLabel debug_file_path;
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
    private javax.swing.JLabel file_regex101_label;
    private javax.swing.JCheckBox file_regex_checkbox;
    private javax.swing.JTextField file_regex_textfield;
    private javax.swing.JComboBox<String> font_combo;
    private javax.swing.JLabel font_label;
    private javax.swing.JCheckBox force_smart_proxy_checkbox;
    private javax.swing.JButton import_mega_button;
    private javax.swing.JButton import_settings_button;
    private javax.swing.JButton jButton1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JProgressBar jProgressBar1;
    private javax.swing.JScrollPane jScrollPane1;
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
    private javax.swing.JRadioButton proxy_random_radio;
    private javax.swing.JCheckBox proxy_reset_slot_checkbox;
    private javax.swing.JRadioButton proxy_sequential_radio;
    private javax.swing.JSpinner proxy_timeout_spinner;
    private javax.swing.JLabel proxy_user_label;
    private javax.swing.JTextField proxy_user_textfield;
    private javax.swing.JLabel proxy_warning_label;
    private javax.swing.JScrollPane public_folder_panel;
    private javax.swing.JTextArea public_folder_warning;
    private javax.swing.JLabel rec_download_slots_label;
    private javax.swing.JLabel rec_smart_proxy_label;
    private javax.swing.JLabel rec_smart_proxy_label1;
    private javax.swing.JLabel rec_upload_slots_label;
    private javax.swing.JLabel rec_zoom_label;
    private javax.swing.JButton remove_elc_account_button;
    private javax.swing.JButton remove_mega_account_button;
    private javax.swing.JCheckBox remove_no_restart_checkbox;
    private javax.swing.JCheckBox remove_no_restart_checkbox1;
    private javax.swing.JCheckBox run_command_checkbox;
    private javax.swing.JButton run_command_test_button;
    private javax.swing.JTextField run_command_textbox;
    private javax.swing.JButton save_button;
    private javax.swing.JCheckBox smart_proxy_checkbox;
    private javax.swing.JPanel smart_proxy_settings;
    private javax.swing.JCheckBox start_frozen_checkbox;
    private javax.swing.JLabel status;
    private javax.swing.JCheckBox thumbnail_checkbox;
    private javax.swing.JButton unlock_accounts_button;
    private javax.swing.JCheckBox upload_log_checkbox;
    private javax.swing.JCheckBox upload_public_folder_checkbox;
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
}
