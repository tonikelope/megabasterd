package com.tonikelope.megabasterd;

import static com.tonikelope.megabasterd.CryptTools.*;
import static com.tonikelope.megabasterd.DBTools.*;
import static com.tonikelope.megabasterd.MainPanel.*;
import static com.tonikelope.megabasterd.MiscTools.*;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.event.WindowEvent;
import static java.awt.event.WindowEvent.WINDOW_CLOSING;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import static java.util.logging.Level.SEVERE;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import static javax.swing.JOptionPane.DEFAULT_OPTION;
import static javax.swing.JOptionPane.INFORMATION_MESSAGE;
import static javax.swing.JOptionPane.QUESTION_MESSAGE;
import static javax.swing.JOptionPane.YES_NO_CANCEL_OPTION;
import static javax.swing.JOptionPane.showOptionDialog;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

/**
 *
 * @author tonikelope
 */
public final class MainPanelView extends javax.swing.JFrame {

    private final MainPanel _main_panel;

    public JLabel getKiss_server_status() {
        return kiss_server_status;
    }

    public JMenuItem getClean_all_down_menu() {
        return clean_all_down_menu;
    }

    public JMenuItem getClean_all_up_menu() {
        return clean_all_up_menu;
    }

    public JButton getClose_all_finished_down_button() {
        return close_all_finished_down_button;
    }

    public JButton getClose_all_finished_up_button() {
        return close_all_finished_up_button;
    }

    public JLabel getMemory_status() {
        return memory_status;
    }

    public JLabel getGlobal_speed_down_label() {
        return global_speed_down_label;
    }

    public JLabel getDown_remtime_label() {
        return down_remtime_label;
    }

    public JLabel getUp_remtime_label() {
        return up_remtime_label;
    }

    public JLabel getGlobal_speed_up_label() {
        return global_speed_up_label;
    }

    public JPanel getjPanel_scroll_down() {
        return jPanel_scroll_down;
    }

    public JPanel getjPanel_scroll_up() {
        return jPanel_scroll_up;
    }

    public JMenuItem getNew_download_menu() {
        return new_download_menu;
    }

    public JMenuItem getNew_upload_menu() {
        return new_upload_menu;
    }

    public JButton getPause_all_down_button() {
        return pause_all_down_button;
    }

    public JButton getPause_all_up_button() {
        return pause_all_up_button;
    }

    public JLabel getStatus_down_label() {
        return status_down_label;
    }

    public JLabel getStatus_up_label() {
        return status_up_label;
    }

    public JButton getUnfreeze_transferences_button() {
        return unfreeze_transferences_button;
    }

    public MainPanel getMain_panel() {
        return _main_panel;
    }

    public JTabbedPane getjTabbedPane1() {
        return jTabbedPane1;
    }

    public JLabel getSmart_proxy_status() {
        return smart_proxy_status;
    }

    public JLabel getMc_reverse_status() {
        return mc_reverse_status;
    }

    public JCheckBoxMenuItem getAuto_close_menu() {
        return auto_close_menu;
    }

    public void updateKissStreamServerStatus(final String status) {

        MiscTools.GUIRun(() -> {
            String old_status = getKiss_server_status().getText();

            if (!old_status.equals(status + " ")) {
                Dimension frame_size = this.getSize();

                getKiss_server_status().setText(status + " ");

                pack();
                setSize(frame_size);

            }
        });
    }

    public void updateSmartProxyStatus(final String status) {

        MiscTools.GUIRun(() -> {
            String old_status = getSmart_proxy_status().getText();

            if (!old_status.equals(status + " ")) {
                Dimension frame_size = this.getSize();

                getSmart_proxy_status().setText(status + " ");

                pack();
                setSize(frame_size);

            }
        });
    }

    public void updateMCReverseStatus(final String status) {

        MiscTools.GUIRun(() -> {

            String old_status = getMc_reverse_status().getText();

            if (!old_status.equals(status + " ")) {
                Dimension frame_size = this.getSize();

                getMc_reverse_status().setText(status + " ");

                pack();
                setSize(frame_size);

            }
        });
    }

    private void _new_upload_dialog(FileGrabberDialog dialog) {

        try {

            dialog.setLocationRelativeTo(this);

            dialog.setVisible(true);

            if (dialog.isUpload() && dialog.getFiles().size() > 0) {

                getMain_panel().resumeUploads();

                getMain_panel().getUpload_manager().getTransference_preprocess_global_queue().addAll(dialog.getFiles());

                getMain_panel().getUpload_manager().secureNotify();

                final String mega_account = (String) dialog.getAccount_combobox().getSelectedItem();

                final String base_path = dialog.getBase_path();

                final String dir_name = dialog.getDir_name_textfield().getText();

                jTabbedPane1.setSelectedIndex(1);

                Runnable run = () -> {

                    Thread.currentThread().setPriority(Thread.MIN_PRIORITY);

                    MegaAPI ma = getMain_panel().getMega_active_accounts().get(mega_account);

                    try {

                        byte[] parent_key = ma.genFolderKey();

                        byte[] share_key = ma.genShareKey();

                        HashMap<String, Object> res = ma.createDir(dir_name != null ? dir_name : dialog.getFiles().get(0).getName() + "_" + genID(10), ma.getRoot_id(), parent_key, i32a2bin(ma.getMaster_key()));

                        String parent_node = (String) ((Map) ((List) res.get("f")).get(0)).get("h");

                        LOG.log(Level.INFO, "{0} Dir {1} created", new Object[]{Thread.currentThread().getName(), parent_node});

                        ma.shareFolder(parent_node, parent_key, share_key);

                        String folder_link = ma.getPublicFolderLink(parent_node, share_key);

                        if (dialog.getUpload_log_checkbox().isSelected()) {

                            File upload_log = new File(MainPanel.MEGABASTERD_HOME_DIR + "/megabasterd_upload_" + parent_node + ".log");
                            upload_log.createNewFile();

                            FileWriter fr;
                            try {
                                fr = new FileWriter(upload_log, true);
                                fr.write("***** MegaBasterd UPLOAD LOG FILE *****\n\n");
                                fr.write(dir_name + "   " + folder_link + "\n\n");
                                fr.close();
                            } catch (IOException ex) {
                                Logger.getLogger(Upload.class.getName()).log(Level.SEVERE, ex.getMessage());
                            }
                        }

                        MegaDirNode file_paths = new MegaDirNode(parent_node);

                        for (File f : dialog.getFiles()) {

                            String file_path = f.getParentFile().getAbsolutePath().replace(base_path, "");

                            LOG.log(Level.INFO, "{0} FILE_PATH -> {1}", new Object[]{Thread.currentThread().getName(), file_path});

                            String[] dirs = file_path.split("\\" + File.separator);

                            MegaDirNode current_node = file_paths;

                            String file_parent = current_node.getNode_id();

                            for (String d : dirs) {

                                LOG.log(Level.INFO, "{0} DIR -> {1}", new Object[]{Thread.currentThread().getName(), d});

                                if (!d.isEmpty()) {

                                    if (current_node.getChildren().get(d) != null) {

                                        current_node = current_node.getChildren().get(d);

                                        file_parent = current_node.getNode_id();

                                    } else {

                                        res = ma.createDirInsideAnotherSharedDir(d, current_node.getNode_id(), ma.genFolderKey(), i32a2bin(ma.getMaster_key()), parent_node, share_key);

                                        file_parent = (String) ((Map) ((List) res.get("f")).get(0)).get("h");

                                        current_node.getChildren().put(d, new MegaDirNode(file_parent));

                                        current_node = current_node.getChildren().get(d);
                                    }
                                }
                            }

                            while (getMain_panel().getUpload_manager().getTransference_waitstart_queue().size() >= TransferenceManager.MAX_WAIT_QUEUE || getMain_panel().getUpload_manager().getTransference_waitstart_aux_queue().size() >= TransferenceManager.MAX_WAIT_QUEUE) {

                                synchronized (getMain_panel().getUpload_manager().getWait_queue_lock()) {
                                    getMain_panel().getUpload_manager().getWait_queue_lock().wait(1000);
                                }
                            }

                            if (!getMain_panel().getUpload_manager().getTransference_preprocess_global_queue().isEmpty()) {

                                Upload upload = new Upload(getMain_panel(), ma, f.getAbsolutePath(), file_parent, null, null, parent_node, share_key, folder_link, dialog.getPriority_checkbox().isSelected());

                                getMain_panel().getUpload_manager().getTransference_provision_queue().add(upload);

                                getMain_panel().getUpload_manager().getTransference_preprocess_global_queue().remove(f);

                                getMain_panel().getUpload_manager().secureNotify();

                            }

                        }

                    } catch (Exception ex) {

                        LOG.log(SEVERE, null, ex);
                    }

                };

                getMain_panel().getUpload_manager().getTransference_preprocess_queue().add(run);

                getMain_panel().getUpload_manager().secureNotify();

            }

        } catch (Exception ex) {
        }

        if (!dialog.isRemember_master_pass()) {

            _main_panel.setMaster_pass(null);
        }

        dialog.dispose();

    }

    private void _file_drop_notify(List<File> files) {

        final MainPanelView tthis = this;

        THREAD_POOL.execute(() -> {
            int n;

            if (files.size() > 1) {

                Object[] options = {LabelTranslatorSingleton.getInstance().translate("Split content in different uploads"), LabelTranslatorSingleton.getInstance().translate("Merge content in the same upload")};

                n = showOptionDialog(_main_panel.getView(),
                        LabelTranslatorSingleton.getInstance().translate("How do you want to proceed?"),
                        LabelTranslatorSingleton.getInstance().translate("File Grabber"), DEFAULT_OPTION, INFORMATION_MESSAGE,
                        null,
                        options,
                        null);

            } else {

                n = 1;

            }

            if (n == 0) {

                files.stream().map((file) -> {
                    List<File> aux = new ArrayList<>();
                    aux.add(file);
                    return aux;
                }).map((aux) -> new FileGrabberDialog(tthis, true, aux)).forEachOrdered((dialog) -> {
                    _new_upload_dialog(dialog);
                });

            } else if (n == 1) {

                final FileGrabberDialog dialog = new FileGrabberDialog(tthis, true, files);

                _new_upload_dialog(dialog);

            }
        });
    }

    public MainPanelView(MainPanel main_panel) {

        _main_panel = main_panel;

        MiscTools.GUIRunAndWait(() -> {

            initComponents();

            updateFonts(this, GUI_FONT, _main_panel.getZoom_factor());

            translateLabels(this);

            for (JComponent c : new JComponent[]{unfreeze_transferences_button, global_speed_down_label, global_speed_up_label, down_remtime_label, up_remtime_label, close_all_finished_down_button, close_all_finished_up_button, pause_all_down_button, pause_all_up_button}) {

                c.setVisible(false);
            }

            clean_all_down_menu.setEnabled(false);
            clean_all_up_menu.setEnabled(false);

            jScrollPane_down.getVerticalScrollBar().setUnitIncrement(20);
            jScrollPane_up.getVerticalScrollBar().setUnitIncrement(20);

            jTabbedPane1.setTitleAt(0, LabelTranslatorSingleton.getInstance().translate("Downloads"));
            jTabbedPane1.setTitleAt(1, LabelTranslatorSingleton.getInstance().translate("Uploads"));
            jTabbedPane1.setDropTarget(new DropTarget() {

                public boolean canImport(DataFlavor[] flavors) {
                    for (DataFlavor flavor : flavors) {
                        if (flavor.isFlavorJavaFileListType()) {
                            return true;
                        }
                    }
                    return false;
                }

                @Override
                public synchronized void drop(DropTargetDropEvent dtde) {
                    changeToNormal();
                    dtde.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);

                    List<File> files;

                    try {

                        if (canImport(dtde.getTransferable().getTransferDataFlavors())) {
                            files = (List<File>) dtde.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);

                            THREAD_POOL.execute(() -> {
                                _file_drop_notify(files);
                            });
                        }

                    } catch (UnsupportedFlavorException | IOException ex) {

                    }
                }

                @Override
                public synchronized void dragEnter(DropTargetDragEvent dtde) {
                    changeToDrop();
                }

                @Override
                public synchronized void dragExit(DropTargetEvent dtde) {
                    changeToNormal();
                }

                private void changeToDrop() {
                    jTabbedPane1.setBorder(BorderFactory.createLineBorder(Color.green, 5));

                }

                private void changeToNormal() {
                    jTabbedPane1.setBorder(null);
                }
            }
            );

            String auto_close = selectSettingValue("auto_close");

            if (auto_close != null) {
                getAuto_close_menu().setSelected(auto_close.equals("yes"));
            } else {
                getAuto_close_menu().setSelected(false);
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

        logo_label = new javax.swing.JLabel();
        kiss_server_status = new javax.swing.JLabel();
        mc_reverse_status = new javax.swing.JLabel();
        smart_proxy_status = new javax.swing.JLabel();
        memory_status = new javax.swing.JLabel();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        downloads_panel = new javax.swing.JPanel();
        global_speed_down_label = new javax.swing.JLabel();
        status_down_label = new javax.swing.JLabel();
        close_all_finished_down_button = new javax.swing.JButton();
        jScrollPane_down = new javax.swing.JScrollPane();
        jPanel_scroll_down = new javax.swing.JPanel();
        pause_all_down_button = new javax.swing.JButton();
        down_remtime_label = new javax.swing.JLabel();
        uploads_panel = new javax.swing.JPanel();
        global_speed_up_label = new javax.swing.JLabel();
        status_up_label = new javax.swing.JLabel();
        close_all_finished_up_button = new javax.swing.JButton();
        jScrollPane_up = new javax.swing.JScrollPane();
        jPanel_scroll_up = new javax.swing.JPanel();
        pause_all_up_button = new javax.swing.JButton();
        up_remtime_label = new javax.swing.JLabel();
        unfreeze_transferences_button = new javax.swing.JButton();
        main_menubar = new javax.swing.JMenuBar();
        file_menu = new javax.swing.JMenu();
        new_download_menu = new javax.swing.JMenuItem();
        new_upload_menu = new javax.swing.JMenuItem();
        new_stream_menu = new javax.swing.JMenuItem();
        jSeparator5 = new javax.swing.JPopupMenu.Separator();
        split_file_menu = new javax.swing.JMenuItem();
        merge_file_menu = new javax.swing.JMenuItem();
        jSeparator4 = new javax.swing.JPopupMenu.Separator();
        clean_all_down_menu = new javax.swing.JMenuItem();
        clean_all_up_menu = new javax.swing.JMenuItem();
        jSeparator2 = new javax.swing.JPopupMenu.Separator();
        hide_tray_menu = new javax.swing.JMenuItem();
        auto_close_menu = new javax.swing.JCheckBoxMenuItem();
        exit_menu = new javax.swing.JMenuItem();
        edit_menu = new javax.swing.JMenu();
        settings_menu = new javax.swing.JMenuItem();
        help_menu = new javax.swing.JMenu();
        about_menu = new javax.swing.JMenuItem();

        setTitle("MegaBasterd " + VERSION);
        setIconImage(new ImageIcon(getClass().getResource(ICON_FILE)).getImage());

        logo_label.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/mbasterd_logo_nuevo-picsay.png"))); // NOI18N
        logo_label.setDoubleBuffered(true);

        kiss_server_status.setFont(new java.awt.Font("Dialog", 0, 14)); // NOI18N
        kiss_server_status.setForeground(new java.awt.Color(102, 102, 102));
        kiss_server_status.setDoubleBuffered(true);

        mc_reverse_status.setFont(new java.awt.Font("Dialog", 0, 14)); // NOI18N
        mc_reverse_status.setForeground(new java.awt.Color(102, 102, 102));

        smart_proxy_status.setFont(new java.awt.Font("Dialog", 0, 14)); // NOI18N
        smart_proxy_status.setForeground(new java.awt.Color(102, 102, 102));
        smart_proxy_status.setDoubleBuffered(true);

        memory_status.setFont(new java.awt.Font("Dialog", 0, 14)); // NOI18N
        memory_status.setForeground(new java.awt.Color(102, 102, 102));
        memory_status.setDoubleBuffered(true);

        jTabbedPane1.setDoubleBuffered(true);
        jTabbedPane1.setFont(new java.awt.Font("Dialog", 1, 24)); // NOI18N

        global_speed_down_label.setFont(new java.awt.Font("Dialog", 1, 54)); // NOI18N
        global_speed_down_label.setText("Speed");
        global_speed_down_label.setDoubleBuffered(true);

        status_down_label.setFont(new java.awt.Font("Dialog", 0, 16)); // NOI18N
        status_down_label.setForeground(new java.awt.Color(102, 102, 102));
        status_down_label.setDoubleBuffered(true);

        close_all_finished_down_button.setFont(new java.awt.Font("Dialog", 1, 16)); // NOI18N
        close_all_finished_down_button.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/icons8-ok-30.png"))); // NOI18N
        close_all_finished_down_button.setText("Clear finished");
        close_all_finished_down_button.setDoubleBuffered(true);
        close_all_finished_down_button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                close_all_finished_down_buttonActionPerformed(evt);
            }
        });

        jScrollPane_down.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153)));

        jPanel_scroll_down.setLayout(new javax.swing.BoxLayout(jPanel_scroll_down, javax.swing.BoxLayout.Y_AXIS));
        jScrollPane_down.setViewportView(jPanel_scroll_down);

        pause_all_down_button.setBackground(new java.awt.Color(255, 153, 0));
        pause_all_down_button.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        pause_all_down_button.setForeground(new java.awt.Color(255, 255, 255));
        pause_all_down_button.setText("PAUSE ALL");
        pause_all_down_button.setDoubleBuffered(true);
        pause_all_down_button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                pause_all_down_buttonActionPerformed(evt);
            }
        });

        down_remtime_label.setFont(new java.awt.Font("Dialog", 1, 20)); // NOI18N

        javax.swing.GroupLayout downloads_panelLayout = new javax.swing.GroupLayout(downloads_panel);
        downloads_panel.setLayout(downloads_panelLayout);
        downloads_panelLayout.setHorizontalGroup(
            downloads_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(downloads_panelLayout.createSequentialGroup()
                .addComponent(global_speed_down_label, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(pause_all_down_button))
            .addGroup(downloads_panelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(status_down_label, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(close_all_finished_down_button))
            .addComponent(jScrollPane_down)
            .addComponent(down_remtime_label, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        downloads_panelLayout.setVerticalGroup(
            downloads_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, downloads_panelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(downloads_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(close_all_finished_down_button, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(status_down_label, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane_down, javax.swing.GroupLayout.DEFAULT_SIZE, 288, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(down_remtime_label)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(downloads_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(global_speed_down_label)
                    .addComponent(pause_all_down_button)))
        );

        jTabbedPane1.addTab("Downloads", new javax.swing.ImageIcon(getClass().getResource("/images/icons8-download-from-ftp-30.png")), downloads_panel); // NOI18N

        global_speed_up_label.setFont(new java.awt.Font("Dialog", 1, 54)); // NOI18N
        global_speed_up_label.setText("Speed");
        global_speed_up_label.setDoubleBuffered(true);

        status_up_label.setFont(new java.awt.Font("Dialog", 0, 16)); // NOI18N
        status_up_label.setForeground(new java.awt.Color(102, 102, 102));

        close_all_finished_up_button.setFont(new java.awt.Font("Dialog", 1, 16)); // NOI18N
        close_all_finished_up_button.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/icons8-ok-30.png"))); // NOI18N
        close_all_finished_up_button.setText("Clear finished");
        close_all_finished_up_button.setDoubleBuffered(true);
        close_all_finished_up_button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                close_all_finished_up_buttonActionPerformed(evt);
            }
        });

        jScrollPane_up.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153)));

        jPanel_scroll_up.setLayout(new javax.swing.BoxLayout(jPanel_scroll_up, javax.swing.BoxLayout.Y_AXIS));
        jScrollPane_up.setViewportView(jPanel_scroll_up);

        pause_all_up_button.setBackground(new java.awt.Color(255, 153, 0));
        pause_all_up_button.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        pause_all_up_button.setForeground(new java.awt.Color(255, 255, 255));
        pause_all_up_button.setText("PAUSE ALL");
        pause_all_up_button.setDoubleBuffered(true);
        pause_all_up_button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                pause_all_up_buttonActionPerformed(evt);
            }
        });

        up_remtime_label.setFont(new java.awt.Font("Dialog", 1, 20)); // NOI18N

        javax.swing.GroupLayout uploads_panelLayout = new javax.swing.GroupLayout(uploads_panel);
        uploads_panel.setLayout(uploads_panelLayout);
        uploads_panelLayout.setHorizontalGroup(
            uploads_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(uploads_panelLayout.createSequentialGroup()
                .addComponent(global_speed_up_label, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(pause_all_up_button))
            .addGroup(uploads_panelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(status_up_label, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(close_all_finished_up_button))
            .addComponent(jScrollPane_up)
            .addComponent(up_remtime_label, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        uploads_panelLayout.setVerticalGroup(
            uploads_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, uploads_panelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(uploads_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(close_all_finished_up_button, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(status_up_label, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane_up, javax.swing.GroupLayout.DEFAULT_SIZE, 288, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(up_remtime_label)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(uploads_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(global_speed_up_label)
                    .addComponent(pause_all_up_button)))
        );

        jTabbedPane1.addTab("Uploads", new javax.swing.ImageIcon(getClass().getResource("/images/icons8-upload-to-ftp-30.png")), uploads_panel); // NOI18N

        unfreeze_transferences_button.setBackground(new java.awt.Color(255, 255, 255));
        unfreeze_transferences_button.setFont(new java.awt.Font("Dialog", 1, 24)); // NOI18N
        unfreeze_transferences_button.setForeground(new java.awt.Color(0, 153, 255));
        unfreeze_transferences_button.setText("UNFREEZE WAITING TRANSFERENCES");
        unfreeze_transferences_button.setDoubleBuffered(true);
        unfreeze_transferences_button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                unfreeze_transferences_buttonActionPerformed(evt);
            }
        });

        file_menu.setText("File");
        file_menu.setDoubleBuffered(true);
        file_menu.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N

        new_download_menu.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        new_download_menu.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/icons8-download-from-ftp-30.png"))); // NOI18N
        new_download_menu.setText("New download");
        new_download_menu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                new_download_menuActionPerformed(evt);
            }
        });
        file_menu.add(new_download_menu);

        new_upload_menu.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        new_upload_menu.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/icons8-upload-to-ftp-30.png"))); // NOI18N
        new_upload_menu.setText("New upload");
        new_upload_menu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                new_upload_menuActionPerformed(evt);
            }
        });
        file_menu.add(new_upload_menu);

        new_stream_menu.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        new_stream_menu.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/icons8-video-playlist-30.png"))); // NOI18N
        new_stream_menu.setText("New streaming");
        new_stream_menu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                new_stream_menuActionPerformed(evt);
            }
        });
        file_menu.add(new_stream_menu);
        file_menu.add(jSeparator5);

        split_file_menu.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        split_file_menu.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/icons8-cut-30.png"))); // NOI18N
        split_file_menu.setText("Split file");
        split_file_menu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                split_file_menuActionPerformed(evt);
            }
        });
        file_menu.add(split_file_menu);

        merge_file_menu.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        merge_file_menu.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/icons8-glue-30.png"))); // NOI18N
        merge_file_menu.setText("Merge file");
        merge_file_menu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                merge_file_menuActionPerformed(evt);
            }
        });
        file_menu.add(merge_file_menu);
        file_menu.add(jSeparator4);

        clean_all_down_menu.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        clean_all_down_menu.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/icons8-trash-can-30.png"))); // NOI18N
        clean_all_down_menu.setText("Remove all no running downloads");
        clean_all_down_menu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clean_all_down_menuActionPerformed(evt);
            }
        });
        file_menu.add(clean_all_down_menu);

        clean_all_up_menu.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        clean_all_up_menu.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/icons8-trash-can-30.png"))); // NOI18N
        clean_all_up_menu.setText("Remove all no running uploads");
        clean_all_up_menu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clean_all_up_menuActionPerformed(evt);
            }
        });
        file_menu.add(clean_all_up_menu);
        file_menu.add(jSeparator2);

        hide_tray_menu.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        hide_tray_menu.setText("Hide to tray");
        hide_tray_menu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                hide_tray_menuActionPerformed(evt);
            }
        });
        file_menu.add(hide_tray_menu);

        auto_close_menu.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        auto_close_menu.setText("Close MegaBasterd when all transfers finish");
        auto_close_menu.setDoubleBuffered(true);
        auto_close_menu.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/icons8-cancel-30.png"))); // NOI18N
        auto_close_menu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                auto_close_menuActionPerformed(evt);
            }
        });
        file_menu.add(auto_close_menu);

        exit_menu.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        exit_menu.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/icons8-shutdown-30.png"))); // NOI18N
        exit_menu.setText("Exit");
        exit_menu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exit_menuActionPerformed(evt);
            }
        });
        file_menu.add(exit_menu);

        main_menubar.add(file_menu);

        edit_menu.setText("Edit");
        edit_menu.setDoubleBuffered(true);
        edit_menu.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N

        settings_menu.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        settings_menu.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/icons8-services-30.png"))); // NOI18N
        settings_menu.setText("Settings");
        settings_menu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                settings_menuActionPerformed(evt);
            }
        });
        edit_menu.add(settings_menu);

        main_menubar.add(edit_menu);

        help_menu.setText("Help");
        help_menu.setDoubleBuffered(true);
        help_menu.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N

        about_menu.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        about_menu.setText("About");
        about_menu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                about_menuActionPerformed(evt);
            }
        });
        help_menu.add(about_menu);

        main_menubar.add(help_menu);

        setJMenuBar(main_menubar);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jTabbedPane1, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGap(0, 283, Short.MAX_VALUE)
                        .addComponent(unfreeze_transferences_button)
                        .addGap(0, 282, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(kiss_server_status, javax.swing.GroupLayout.DEFAULT_SIZE, 191, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(mc_reverse_status, javax.swing.GroupLayout.DEFAULT_SIZE, 191, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(smart_proxy_status, javax.swing.GroupLayout.DEFAULT_SIZE, 191, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(memory_status, javax.swing.GroupLayout.DEFAULT_SIZE, 198, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(logo_label)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(unfreeze_transferences_button)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jTabbedPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 469, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(logo_label)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(mc_reverse_status)
                            .addComponent(smart_proxy_status)
                            .addComponent(memory_status))
                        .addComponent(kiss_server_status)))
                .addGap(2, 2, 2))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void new_download_menuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_new_download_menuActionPerformed

        new_download_menu.setEnabled(false);

        final LinkGrabberDialog dialog = new LinkGrabberDialog(this, true, _main_panel.getDefault_download_path(), _main_panel.getClipboardspy());

        _main_panel.getClipboardspy().attachObserver(dialog);

        dialog.setLocationRelativeTo(this);

        dialog.setVisible(true);

        _main_panel.getClipboardspy().detachObserver(dialog);

        final String dl_path = dialog.getDownload_path();

        final MegaAPI ma;

        if (getMain_panel().isUse_mega_account_down()) {
            final String mega_account = (String) dialog.getUse_mega_account_down_combobox().getSelectedItem();

            if ("".equals(mega_account)) {

                ma = new MegaAPI();

            } else {

                ma = getMain_panel().getMega_active_accounts().get(mega_account);
            }

        } else {

            ma = new MegaAPI();
        }

        jTabbedPane1.setSelectedIndex(0);

        if (dialog.isDownload()) {

            getMain_panel().resumeDownloads();

            final MainPanelView tthis = this;

            Runnable run = () -> {

                Thread.currentThread().setPriority(Thread.MIN_PRIORITY);

                //Convert to legacy link format
                String link_data = MiscTools.newMegaLinks2Legacy(dialog.getLinks_textarea().getText());

                Set<String> urls = new HashSet(findAllRegex("(?:https?|mega)://[^\r\n]+(#[^\r\n!]*?)?![^\r\n!]+![^\\?\r\n]+", link_data, 0));

                Set<String> megadownloader = new HashSet(findAllRegex("mega://enc[^\r\n]+", link_data, 0));

                megadownloader.forEach((link) -> {
                    try {

                        urls.add(decryptMegaDownloaderLink(link));

                    } catch (Exception ex) {
                        LOG.log(SEVERE, null, ex);
                    }
                });

                Set<String> elc = new HashSet(findAllRegex("mega://elc[^\r\n]+", link_data, 0));

                elc.forEach((link) -> {
                    try {

                        urls.addAll(CryptTools.decryptELC(link, getMain_panel()));

                    } catch (Exception ex) {
                        LOG.log(SEVERE, null, ex);
                    }
                });

                Set<String> dlc = new HashSet(findAllRegex("dlc://([^\r\n]+)", link_data, 1));

                dlc.stream().map((d) -> CryptTools.decryptDLC(d, _main_panel)).forEachOrdered((links) -> {
                    links.stream().filter((link) -> (findFirstRegex("(?:https?|mega)://[^\r\n](#[^\r\n!]*?)?![^\r\n!]+![^\\?\r\n]+", link, 0) != null)).forEachOrdered((link) -> {
                        urls.add(link);
                    });
                });

                if (!urls.isEmpty()) {

                    getMain_panel().getDownload_manager().getTransference_preprocess_global_queue().addAll(urls);

                    getMain_panel().getDownload_manager().secureNotify();

                    boolean link_warning;

                    for (String url : urls) {

                        try {

                            link_warning = false;

                            url = URLDecoder.decode(url, "UTF-8").replaceAll("^mega://", "https://mega.nz").trim();

                            Download download;

                            if (findFirstRegex("#F!", url, 0) != null) {

                                FolderLinkDialog fdialog = new FolderLinkDialog(_main_panel.getView(), true, url);

                                if (fdialog.isMega_error() == 0) {

                                    fdialog.setLocationRelativeTo(_main_panel.getView());

                                    fdialog.setVisible(true);

                                    if (fdialog.isDownload()) {

                                        List<HashMap> folder_links = fdialog.getDownload_links();

                                        fdialog.dispose();

                                        for (HashMap folder_link : folder_links) {

                                            while (getMain_panel().getDownload_manager().getTransference_waitstart_queue().size() >= TransferenceManager.MAX_WAIT_QUEUE || getMain_panel().getDownload_manager().getTransference_waitstart_aux_queue().size() >= TransferenceManager.MAX_WAIT_QUEUE) {

                                                if (!link_warning) {
                                                    link_warning = true;

                                                    JOptionPane.showMessageDialog(tthis, LabelTranslatorSingleton.getInstance().translate("There are a lot of files in this folder.\nNot all links will be provisioned at once to avoid saturating MegaBasterd"), "Warning", JOptionPane.WARNING_MESSAGE);
                                                }

                                                synchronized (getMain_panel().getDownload_manager().getWait_queue_lock()) {
                                                    getMain_panel().getDownload_manager().getWait_queue_lock().wait(1000);
                                                }
                                            }

                                            if (!getMain_panel().getDownload_manager().getTransference_preprocess_global_queue().isEmpty()) {

                                                if (!((String) folder_link.get("url")).equals("*")) {

                                                    download = new Download(getMain_panel(), ma, (String) folder_link.get("url"), dl_path, (String) folder_link.get("filename"), (String) folder_link.get("filekey"), (long) folder_link.get("filesize"), null, null, getMain_panel().isUse_slots_down(), false, getMain_panel().isUse_custom_chunks_dir() ? getMain_panel().getCustom_chunks_dir() : null, dialog.getPriority_checkbox().isSelected());

                                                    getMain_panel().getDownload_manager().getTransference_provision_queue().add(download);

                                                    getMain_panel().getDownload_manager().secureNotify();

                                                } else {
                                                    //Directorio vacío
                                                    String filename = dl_path + "/" + (String) folder_link.get("filename");

                                                    File file = new File(filename);

                                                    if (file.getParent() != null) {
                                                        File path = new File(file.getParent());

                                                        path.mkdirs();
                                                    }

                                                    if (((int) folder_link.get("type")) == 1) {

                                                        file.mkdir();

                                                    } else {
                                                        try {
                                                            file.createNewFile();
                                                        } catch (IOException ex) {
                                                            Logger.getLogger(MainPanelView.class.getName()).log(Level.SEVERE, null, ex);
                                                        }
                                                    }
                                                }
                                            } else {
                                                break;
                                            }
                                        }
                                    }

                                }

                                fdialog.dispose();

                            } else {

                                while (getMain_panel().getDownload_manager().getTransference_waitstart_queue().size() >= TransferenceManager.MAX_WAIT_QUEUE || getMain_panel().getDownload_manager().getTransference_waitstart_aux_queue().size() >= TransferenceManager.MAX_WAIT_QUEUE) {

                                    synchronized (getMain_panel().getDownload_manager().getWait_queue_lock()) {
                                        getMain_panel().getDownload_manager().getWait_queue_lock().wait(1000);
                                    }
                                }

                                download = new Download(getMain_panel(), ma, url, dl_path, null, null, null, null, null, getMain_panel().isUse_slots_down(), false, getMain_panel().isUse_custom_chunks_dir() ? getMain_panel().getCustom_chunks_dir() : null, dialog.getPriority_checkbox().isSelected());

                                getMain_panel().getDownload_manager().getTransference_provision_queue().add(download);

                                getMain_panel().getDownload_manager().secureNotify();

                            }

                            getMain_panel().getDownload_manager().getTransference_preprocess_global_queue().remove(url);

                            getMain_panel().getDownload_manager().secureNotify();

                        } catch (UnsupportedEncodingException ex) {
                            LOG.log(Level.SEVERE, ex.getMessage());
                        } catch (InterruptedException ex) {
                            Logger.getLogger(MainPanelView.class.getName()).log(Level.SEVERE, ex.getMessage());
                        }

                    }
                }
            };

            getMain_panel().getDownload_manager().getTransference_preprocess_queue().add(run);

            getMain_panel().getDownload_manager().secureNotify();

            new_download_menu.setEnabled(true);

        } else {

            new_download_menu.setEnabled(true);
        }

        dialog.dispose();

    }//GEN-LAST:event_new_download_menuActionPerformed

    private void settings_menuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_settings_menuActionPerformed

        SettingsDialog dialog = new SettingsDialog(this, true);

        dialog.setLocationRelativeTo(this);

        dialog.setVisible(true);

        if (dialog.isSettings_ok()) {

            dialog.getDeleted_mega_accounts().stream().map((email) -> {
                try {
                    deleteMegaAccount(email);
                } catch (SQLException ex) {
                    LOG.log(SEVERE, null, ex);
                }
                return email;
            }).map((email) -> {
                _main_panel.getMega_accounts().remove(email);
                return email;
            }).forEachOrdered((email) -> {
                _main_panel.getMega_active_accounts().remove(email);
            });
            dialog.getDeleted_elc_accounts().stream().map((host) -> {
                try {
                    deleteELCAccount(host);
                } catch (SQLException ex) {
                    LOG.log(SEVERE, null, ex);
                }
                return host;
            }).forEachOrdered((host) -> {
                _main_panel.getElc_accounts().remove(host);
            });

            if (_main_panel.isRestart()) {

                _main_panel.byebye(true);
            } else {
                _main_panel.loadUserSettings();

                if (_main_panel.isLimit_download_speed()) {

                    _main_panel.getStream_supervisor().setMaxBytesPerSecInput(_main_panel.getMax_dl_speed() * 1024);

                    global_speed_down_label.setForeground(new Color(255, 0, 0));

                } else {

                    _main_panel.getStream_supervisor().setMaxBytesPerSecInput(0);

                    global_speed_down_label.setForeground(new Color(0, 128, 255));

                }

                if (_main_panel.isLimit_upload_speed()) {

                    _main_panel.getStream_supervisor().setMaxBytesPerSecOutput(_main_panel.getMax_up_speed() * 1024);

                    global_speed_up_label.setForeground(new Color(255, 0, 0));

                } else {

                    _main_panel.getStream_supervisor().setMaxBytesPerSecOutput(0);

                    global_speed_up_label.setForeground(new Color(0, 128, 255));

                }

                _main_panel.getDownload_manager().setMax_running_trans(_main_panel.getMax_dl());

                _main_panel.getUpload_manager().setMax_running_trans(_main_panel.getMax_ul());

                _main_panel.getDownload_manager().secureNotify();

                _main_panel.getUpload_manager().secureNotify();

                if (_main_panel.isMegacrypter_reverse()) {

                    if (_main_panel.getMega_proxy_server() == null) {

                        _main_panel.setMega_proxy_server(new MegaProxyServer(_main_panel, UUID.randomUUID().toString(), _main_panel.getMegacrypter_reverse_port()));

                        THREAD_POOL.execute(_main_panel.getMega_proxy_server());

                    } else if (_main_panel.getMega_proxy_server().getPort() != _main_panel.getMegacrypter_reverse_port()) {

                        try {

                            _main_panel.getMega_proxy_server().stopServer();
                            _main_panel.setMega_proxy_server(new MegaProxyServer(_main_panel, UUID.randomUUID().toString(), _main_panel.getMegacrypter_reverse_port()));
                            THREAD_POOL.execute(_main_panel.getMega_proxy_server());

                        } catch (IOException ex) {
                            LOG.log(Level.SEVERE, ex.getMessage());
                        }
                    }

                } else {

                    if (_main_panel.getMega_proxy_server() != null) {

                        try {
                            _main_panel.getMega_proxy_server().stopServer();
                        } catch (IOException ex) {
                            LOG.log(Level.SEVERE, ex.getMessage());
                        }
                    }

                    _main_panel.setMega_proxy_server(null);
                }

                if (MainPanel.isUse_smart_proxy()) {

                    if (MainPanel.getProxy_manager() == null) {
                        MainPanel.setProxy_manager(new SmartMegaProxyManager(null, _main_panel));
                    }

                    MainPanel.getProxy_manager().refreshProxyList();

                } else {

                    updateSmartProxyStatus("SmartProxy: OFF");
                }
            }

            if (!dialog.isRemember_master_pass()) {

                _main_panel.setMaster_pass(null);
            }

            dialog.dispose();

        }
    }//GEN-LAST:event_settings_menuActionPerformed

    private void hide_tray_menuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_hide_tray_menuActionPerformed

        dispatchEvent(new WindowEvent(this, WINDOW_CLOSING));
    }//GEN-LAST:event_hide_tray_menuActionPerformed

    private void about_menuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_about_menuActionPerformed

        AboutDialog dialog = new AboutDialog(this, true);

        dialog.setLocationRelativeTo(this);

        dialog.setVisible(true);
    }//GEN-LAST:event_about_menuActionPerformed

    private void exit_menuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exit_menuActionPerformed

        _main_panel.byebye(false);
    }//GEN-LAST:event_exit_menuActionPerformed

    private void close_all_finished_down_buttonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_close_all_finished_down_buttonActionPerformed

        _main_panel.getDownload_manager().closeAllFinished();
    }//GEN-LAST:event_close_all_finished_down_buttonActionPerformed

    private void clean_all_down_menuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clean_all_down_menuActionPerformed

        Object[] options = {"No",
            LabelTranslatorSingleton.getInstance().translate("Yes")};

        int n = showOptionDialog(_main_panel.getView(),
                LabelTranslatorSingleton.getInstance().translate("Remove all no running downloads?"),
                LabelTranslatorSingleton.getInstance().translate("Warning!"), YES_NO_CANCEL_OPTION, QUESTION_MESSAGE,
                null,
                options,
                options[0]);

        if (n == 1) {
            _main_panel.getDownload_manager().closeAllPreProWaiting();
        }
    }//GEN-LAST:event_clean_all_down_menuActionPerformed

    private void pause_all_down_buttonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pause_all_down_buttonActionPerformed

        pause_all_down_button.setEnabled(false);

        if (!_main_panel.getDownload_manager().isPaused_all()) {

            _main_panel.getDownload_manager().pauseAll();

        } else {

            _main_panel.getDownload_manager().resumeAll();
        }

    }//GEN-LAST:event_pause_all_down_buttonActionPerformed

    private void new_stream_menuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_new_stream_menuActionPerformed

        StreamerDialog dialog = new StreamerDialog(this, true, _main_panel.getClipboardspy());

        _main_panel.getClipboardspy().attachObserver(dialog);

        dialog.setLocationRelativeTo(this);

        dialog.setVisible(true);

        _main_panel.getClipboardspy().detachObserver(dialog);
    }//GEN-LAST:event_new_stream_menuActionPerformed

    private void new_upload_menuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_new_upload_menuActionPerformed

        final FileGrabberDialog dialog = new FileGrabberDialog(this, true, null);

        _new_upload_dialog(dialog);
    }//GEN-LAST:event_new_upload_menuActionPerformed

    private void close_all_finished_up_buttonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_close_all_finished_up_buttonActionPerformed

        _main_panel.getUpload_manager().closeAllFinished();
    }//GEN-LAST:event_close_all_finished_up_buttonActionPerformed

    private void pause_all_up_buttonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pause_all_up_buttonActionPerformed

        pause_all_up_button.setEnabled(false);

        if (!_main_panel.getUpload_manager().isPaused_all()) {

            _main_panel.getUpload_manager().pauseAll();

        } else {

            _main_panel.getUpload_manager().resumeAll();
        }
    }//GEN-LAST:event_pause_all_up_buttonActionPerformed

    private void clean_all_up_menuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clean_all_up_menuActionPerformed

        Object[] options = {"No",
            LabelTranslatorSingleton.getInstance().translate("Yes")};

        int n = showOptionDialog(_main_panel.getView(),
                LabelTranslatorSingleton.getInstance().translate("Remove all no running uploads?"),
                LabelTranslatorSingleton.getInstance().translate("Warning!"), YES_NO_CANCEL_OPTION, QUESTION_MESSAGE,
                null,
                options,
                options[0]);

        if (n == 1) {
            _main_panel.getUpload_manager().closeAllPreProWaiting();
        }
    }//GEN-LAST:event_clean_all_up_menuActionPerformed

    private void split_file_menuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_split_file_menuActionPerformed
        // TODO add your handling code here:
        FileSplitterDialog dialog = new FileSplitterDialog(this, true);

        dialog.setLocationRelativeTo(this);

        dialog.setVisible(true);
    }//GEN-LAST:event_split_file_menuActionPerformed

    private void merge_file_menuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_merge_file_menuActionPerformed
        // TODO add your handling code here:

        FileMergerDialog dialog = new FileMergerDialog(this, true);

        dialog.setLocationRelativeTo(this);

        dialog.setVisible(true);
    }//GEN-LAST:event_merge_file_menuActionPerformed

    private void auto_close_menuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_auto_close_menuActionPerformed
        try {
            DBTools.insertSettingValue("auto_close", getAuto_close_menu().isSelected() ? "yes" : "no");
        } catch (SQLException ex) {
            Logger.getLogger(MainPanelView.class.getName()).log(Level.SEVERE, ex.getMessage());
        }
    }//GEN-LAST:event_auto_close_menuActionPerformed

    private void unfreeze_transferences_buttonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_unfreeze_transferences_buttonActionPerformed
        // TODO add your handling code here:

        unfreeze_transferences_button.setVisible(false);

        THREAD_POOL.execute(_main_panel.getDownload_manager()::unfreezeTransferenceWaitStartQueue);

        THREAD_POOL.execute(_main_panel.getUpload_manager()::unfreezeTransferenceWaitStartQueue);
    }//GEN-LAST:event_unfreeze_transferences_buttonActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JMenuItem about_menu;
    private javax.swing.JCheckBoxMenuItem auto_close_menu;
    private javax.swing.JMenuItem clean_all_down_menu;
    private javax.swing.JMenuItem clean_all_up_menu;
    private javax.swing.JButton close_all_finished_down_button;
    private javax.swing.JButton close_all_finished_up_button;
    private javax.swing.JLabel down_remtime_label;
    private javax.swing.JPanel downloads_panel;
    private javax.swing.JMenu edit_menu;
    private javax.swing.JMenuItem exit_menu;
    private javax.swing.JMenu file_menu;
    private javax.swing.JLabel global_speed_down_label;
    private javax.swing.JLabel global_speed_up_label;
    private javax.swing.JMenu help_menu;
    private javax.swing.JMenuItem hide_tray_menu;
    private javax.swing.JPanel jPanel_scroll_down;
    private javax.swing.JPanel jPanel_scroll_up;
    private javax.swing.JScrollPane jScrollPane_down;
    private javax.swing.JScrollPane jScrollPane_up;
    private javax.swing.JPopupMenu.Separator jSeparator2;
    private javax.swing.JPopupMenu.Separator jSeparator4;
    private javax.swing.JPopupMenu.Separator jSeparator5;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JLabel kiss_server_status;
    private javax.swing.JLabel logo_label;
    private javax.swing.JMenuBar main_menubar;
    private javax.swing.JLabel mc_reverse_status;
    private javax.swing.JLabel memory_status;
    private javax.swing.JMenuItem merge_file_menu;
    private javax.swing.JMenuItem new_download_menu;
    private javax.swing.JMenuItem new_stream_menu;
    private javax.swing.JMenuItem new_upload_menu;
    private javax.swing.JButton pause_all_down_button;
    private javax.swing.JButton pause_all_up_button;
    private javax.swing.JMenuItem settings_menu;
    private javax.swing.JLabel smart_proxy_status;
    private javax.swing.JMenuItem split_file_menu;
    private javax.swing.JLabel status_down_label;
    private javax.swing.JLabel status_up_label;
    private javax.swing.JButton unfreeze_transferences_button;
    private javax.swing.JLabel up_remtime_label;
    private javax.swing.JPanel uploads_panel;
    // End of variables declaration//GEN-END:variables
    private static final Logger LOG = Logger.getLogger(MainPanelView.class.getName());

}
