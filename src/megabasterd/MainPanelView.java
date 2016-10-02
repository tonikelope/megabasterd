package megabasterd;


import java.awt.Color;
import static java.awt.Font.BOLD;
import static java.awt.Font.PLAIN;
import java.awt.event.WindowEvent;
import static java.awt.event.WindowEvent.WINDOW_CLOSING;
import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import static java.util.logging.Level.SEVERE;
import static java.util.logging.Logger.getLogger;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import static javax.swing.JOptionPane.QUESTION_MESSAGE;
import static javax.swing.JOptionPane.YES_NO_CANCEL_OPTION;
import static javax.swing.JOptionPane.showOptionDialog;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import static megabasterd.CryptTools.decryptMegaDownloaderLink;
import static megabasterd.DBTools.deleteMegaAccount;
import static megabasterd.MainPanel.FONT_DEFAULT;
import static megabasterd.MainPanel.ICON_FILE;
import static megabasterd.MainPanel.THREAD_POOL;
import static megabasterd.MainPanel.VERSION;
import static megabasterd.MiscTools.BASE642Bin;
import static megabasterd.MiscTools.bin2i32a;
import static megabasterd.MiscTools.findAllRegex;
import static megabasterd.MiscTools.findFirstRegex;
import static megabasterd.MiscTools.genID;
import static megabasterd.MiscTools.i32a2bin;
import static megabasterd.MiscTools.swingReflectionInvoke;
import static megabasterd.MiscTools.swingReflectionInvokeAndWait;
import static megabasterd.MiscTools.updateFont;


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

    public JLabel getGlobal_speed_down_label() {
        return global_speed_down_label;
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

    public MainPanel getMain_panel() {
        return _main_panel;
    }

    public JTabbedPane getjTabbedPane1() {
        return jTabbedPane1;
    }
    
    

    public MainPanelView(MainPanel main_panel) {

        _main_panel = main_panel;
         
        initComponents();
 
        setTitle("MegaBasterd " + VERSION);
        
        setIconImage(new ImageIcon(getClass().getResource(ICON_FILE)).getImage());

        updateFont(file_menu, FONT_DEFAULT, PLAIN);
        updateFont(edit_menu, FONT_DEFAULT, PLAIN);
        updateFont(help_menu, FONT_DEFAULT, PLAIN);
        updateFont(new_download_menu, FONT_DEFAULT, PLAIN);
        updateFont(exit_menu, FONT_DEFAULT, PLAIN);
        updateFont(settings_menu, FONT_DEFAULT, PLAIN);
        updateFont(hide_tray_menu, FONT_DEFAULT, PLAIN);
        updateFont(about_menu, FONT_DEFAULT, PLAIN);
        updateFont(new_stream_menu, FONT_DEFAULT, PLAIN);
        updateFont(new_upload_menu, FONT_DEFAULT, PLAIN);
        updateFont(clean_all_up_menu, FONT_DEFAULT, PLAIN);
        updateFont(clean_all_down_menu, FONT_DEFAULT, PLAIN);
        updateFont(global_speed_down_label, FONT_DEFAULT, BOLD);
        updateFont(global_speed_up_label, FONT_DEFAULT, BOLD);
        updateFont(kiss_server_status, FONT_DEFAULT, BOLD);
        updateFont(status_down_label, FONT_DEFAULT, BOLD);
        updateFont(status_up_label, FONT_DEFAULT, BOLD);
        updateFont(close_all_finished_down_button, FONT_DEFAULT, BOLD);
        updateFont(close_all_finished_up_button, FONT_DEFAULT, BOLD);
        updateFont(pause_all_down_button, FONT_DEFAULT, BOLD);
        updateFont(pause_all_up_button, FONT_DEFAULT, BOLD);
        updateFont(jTabbedPane1, FONT_DEFAULT, PLAIN);
        
        swingReflectionInvoke("setVisible", global_speed_down_label, false);
        swingReflectionInvoke("setVisible", global_speed_up_label, false);
        swingReflectionInvoke("setForeground", global_speed_down_label, getMain_panel().isLimit_download_speed()?new Color(255,0,0):new Color(0,128,255));
        swingReflectionInvoke("setForeground", global_speed_up_label, getMain_panel().isLimit_upload_speed()?new Color(255,0,0):new Color(0,128,255));
        swingReflectionInvoke("setVisible", close_all_finished_down_button, false);
        swingReflectionInvoke("setVisible", close_all_finished_up_button, false);
        swingReflectionInvoke("setVisible", pause_all_down_button, false);
        swingReflectionInvoke("setVisible", pause_all_up_button, false);
        swingReflectionInvoke("setEnabled", clean_all_down_menu, false);
        swingReflectionInvoke("setEnabled", clean_all_up_menu, false);
        
        jScrollPane_down.getVerticalScrollBar().setUnitIncrement(20);
        jScrollPane_up.getVerticalScrollBar().setUnitIncrement(20);

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
        jTabbedPane1 = new javax.swing.JTabbedPane();
        downloads_panel = new javax.swing.JPanel();
        global_speed_down_label = new javax.swing.JLabel();
        status_down_label = new javax.swing.JLabel();
        close_all_finished_down_button = new javax.swing.JButton();
        jScrollPane_down = new javax.swing.JScrollPane();
        jPanel_scroll_down = new javax.swing.JPanel();
        pause_all_down_button = new javax.swing.JButton();
        uploads_panel = new javax.swing.JPanel();
        global_speed_up_label = new javax.swing.JLabel();
        status_up_label = new javax.swing.JLabel();
        close_all_finished_up_button = new javax.swing.JButton();
        jScrollPane_up = new javax.swing.JScrollPane();
        jPanel_scroll_up = new javax.swing.JPanel();
        pause_all_up_button = new javax.swing.JButton();
        main_menubar = new javax.swing.JMenuBar();
        file_menu = new javax.swing.JMenu();
        new_download_menu = new javax.swing.JMenuItem();
        jSeparator6 = new javax.swing.JPopupMenu.Separator();
        new_upload_menu = new javax.swing.JMenuItem();
        jSeparator1 = new javax.swing.JPopupMenu.Separator();
        new_stream_menu = new javax.swing.JMenuItem();
        jSeparator4 = new javax.swing.JPopupMenu.Separator();
        hide_tray_menu = new javax.swing.JMenuItem();
        jSeparator2 = new javax.swing.JPopupMenu.Separator();
        clean_all_down_menu = new javax.swing.JMenuItem();
        jSeparator5 = new javax.swing.JPopupMenu.Separator();
        clean_all_up_menu = new javax.swing.JMenuItem();
        jSeparator3 = new javax.swing.JPopupMenu.Separator();
        exit_menu = new javax.swing.JMenuItem();
        edit_menu = new javax.swing.JMenu();
        settings_menu = new javax.swing.JMenuItem();
        help_menu = new javax.swing.JMenu();
        about_menu = new javax.swing.JMenuItem();

        setTitle("MegaBasterd");

        logo_label.setIcon(new javax.swing.ImageIcon(getClass().getResource("/megabasterd/mbasterd_logo_nuevo.png"))); // NOI18N
        logo_label.setDoubleBuffered(true);

        kiss_server_status.setFont(new java.awt.Font("Dialog", 1, 14)); // NOI18N
        kiss_server_status.setDoubleBuffered(true);

        jTabbedPane1.setDoubleBuffered(true);
        jTabbedPane1.setFont(new java.awt.Font("Dialog", 1, 22)); // NOI18N

        global_speed_down_label.setFont(new java.awt.Font("Dialog", 1, 54)); // NOI18N
        global_speed_down_label.setText("Speed");
        global_speed_down_label.setDoubleBuffered(true);

        status_down_label.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        status_down_label.setDoubleBuffered(true);

        close_all_finished_down_button.setFont(new java.awt.Font("Dialog", 1, 16)); // NOI18N
        close_all_finished_down_button.setText("Close all finished");
        close_all_finished_down_button.setDoubleBuffered(true);
        close_all_finished_down_button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                close_all_finished_down_buttonActionPerformed(evt);
            }
        });

        jPanel_scroll_down.setLayout(new javax.swing.BoxLayout(jPanel_scroll_down, javax.swing.BoxLayout.Y_AXIS));
        jScrollPane_down.setViewportView(jPanel_scroll_down);

        pause_all_down_button.setBackground(new java.awt.Color(255, 153, 0));
        pause_all_down_button.setFont(new java.awt.Font("Dialog", 1, 16)); // NOI18N
        pause_all_down_button.setForeground(new java.awt.Color(255, 255, 255));
        pause_all_down_button.setText("PAUSE ALL");
        pause_all_down_button.setDoubleBuffered(true);
        pause_all_down_button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                pause_all_down_buttonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout downloads_panelLayout = new javax.swing.GroupLayout(downloads_panel);
        downloads_panel.setLayout(downloads_panelLayout);
        downloads_panelLayout.setHorizontalGroup(
            downloads_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(downloads_panelLayout.createSequentialGroup()
                .addComponent(global_speed_down_label, javax.swing.GroupLayout.PREFERRED_SIZE, 404, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 258, Short.MAX_VALUE)
                .addComponent(pause_all_down_button))
            .addGroup(downloads_panelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(status_down_label, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(close_all_finished_down_button))
            .addComponent(jScrollPane_down)
        );
        downloads_panelLayout.setVerticalGroup(
            downloads_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, downloads_panelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(downloads_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(close_all_finished_down_button, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(status_down_label, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane_down, javax.swing.GroupLayout.DEFAULT_SIZE, 277, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(downloads_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(global_speed_down_label)
                    .addComponent(pause_all_down_button)))
        );

        jTabbedPane1.addTab("Downloads", downloads_panel);

        global_speed_up_label.setFont(new java.awt.Font("Dialog", 1, 54)); // NOI18N
        global_speed_up_label.setText("Speed");
        global_speed_up_label.setDoubleBuffered(true);

        status_up_label.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N

        close_all_finished_up_button.setFont(new java.awt.Font("Dialog", 1, 16)); // NOI18N
        close_all_finished_up_button.setText("Close all finished");
        close_all_finished_up_button.setDoubleBuffered(true);
        close_all_finished_up_button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                close_all_finished_up_buttonActionPerformed(evt);
            }
        });

        jPanel_scroll_up.setLayout(new javax.swing.BoxLayout(jPanel_scroll_up, javax.swing.BoxLayout.Y_AXIS));
        jScrollPane_up.setViewportView(jPanel_scroll_up);

        pause_all_up_button.setBackground(new java.awt.Color(255, 153, 0));
        pause_all_up_button.setFont(new java.awt.Font("Dialog", 1, 16)); // NOI18N
        pause_all_up_button.setForeground(new java.awt.Color(255, 255, 255));
        pause_all_up_button.setText("PAUSE ALL");
        pause_all_up_button.setDoubleBuffered(true);
        pause_all_up_button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                pause_all_up_buttonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout uploads_panelLayout = new javax.swing.GroupLayout(uploads_panel);
        uploads_panel.setLayout(uploads_panelLayout);
        uploads_panelLayout.setHorizontalGroup(
            uploads_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(uploads_panelLayout.createSequentialGroup()
                .addComponent(global_speed_up_label, javax.swing.GroupLayout.PREFERRED_SIZE, 404, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 258, Short.MAX_VALUE)
                .addComponent(pause_all_up_button))
            .addGroup(uploads_panelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(status_up_label, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(close_all_finished_up_button))
            .addComponent(jScrollPane_up)
        );
        uploads_panelLayout.setVerticalGroup(
            uploads_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, uploads_panelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(uploads_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(close_all_finished_up_button, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(status_up_label, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane_up, javax.swing.GroupLayout.DEFAULT_SIZE, 277, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(uploads_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(global_speed_up_label)
                    .addComponent(pause_all_up_button)))
        );

        jTabbedPane1.addTab("Uploads", uploads_panel);

        file_menu.setText("File");
        file_menu.setDoubleBuffered(true);
        file_menu.setFont(new java.awt.Font("Dialog", 0, 20)); // NOI18N

        new_download_menu.setFont(new java.awt.Font("Dialog", 0, 20)); // NOI18N
        new_download_menu.setText("New download");
        new_download_menu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                new_download_menuActionPerformed(evt);
            }
        });
        file_menu.add(new_download_menu);
        file_menu.add(jSeparator6);

        new_upload_menu.setFont(new java.awt.Font("Dialog", 0, 20)); // NOI18N
        new_upload_menu.setText("New upload");
        new_upload_menu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                new_upload_menuActionPerformed(evt);
            }
        });
        file_menu.add(new_upload_menu);
        file_menu.add(jSeparator1);

        new_stream_menu.setFont(new java.awt.Font("Dialog", 0, 20)); // NOI18N
        new_stream_menu.setText("New stream");
        new_stream_menu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                new_stream_menuActionPerformed(evt);
            }
        });
        file_menu.add(new_stream_menu);
        file_menu.add(jSeparator4);

        hide_tray_menu.setFont(new java.awt.Font("Dialog", 0, 20)); // NOI18N
        hide_tray_menu.setText("Hide to tray");
        hide_tray_menu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                hide_tray_menuActionPerformed(evt);
            }
        });
        file_menu.add(hide_tray_menu);
        file_menu.add(jSeparator2);

        clean_all_down_menu.setFont(new java.awt.Font("Dialog", 0, 20)); // NOI18N
        clean_all_down_menu.setText("Remove all waiting downloads");
        clean_all_down_menu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clean_all_down_menuActionPerformed(evt);
            }
        });
        file_menu.add(clean_all_down_menu);
        file_menu.add(jSeparator5);

        clean_all_up_menu.setFont(new java.awt.Font("Dialog", 0, 20)); // NOI18N
        clean_all_up_menu.setText("Remove all waiting uploads");
        clean_all_up_menu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clean_all_up_menuActionPerformed(evt);
            }
        });
        file_menu.add(clean_all_up_menu);
        file_menu.add(jSeparator3);

        exit_menu.setFont(new java.awt.Font("Dialog", 0, 20)); // NOI18N
        exit_menu.setText("EXIT");
        exit_menu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exit_menuActionPerformed(evt);
            }
        });
        file_menu.add(exit_menu);

        main_menubar.add(file_menu);

        edit_menu.setText("Edit");
        edit_menu.setDoubleBuffered(true);
        edit_menu.setFont(new java.awt.Font("Dialog", 0, 20)); // NOI18N

        settings_menu.setFont(new java.awt.Font("Dialog", 0, 20)); // NOI18N
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
        help_menu.setFont(new java.awt.Font("Dialog", 0, 20)); // NOI18N

        about_menu.setFont(new java.awt.Font("Dialog", 0, 20)); // NOI18N
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
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jTabbedPane1)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(kiss_server_status, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(18, 18, 18)
                        .addComponent(logo_label)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jTabbedPane1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(kiss_server_status, javax.swing.GroupLayout.PREFERRED_SIZE, 11, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(logo_label))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void new_download_menuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_new_download_menuActionPerformed

        swingReflectionInvoke("setEnabled", new_download_menu, false);
        
        final LinkGrabberDialog dialog = new LinkGrabberDialog(this, true, _main_panel.getDefault_download_path(), _main_panel.getClipboardspy());
        
        _main_panel.getClipboardspy().attachObserver(dialog);

        swingReflectionInvokeAndWait("setLocationRelativeTo", dialog, this);

        swingReflectionInvokeAndWait("setVisible", dialog, true);

        _main_panel.getClipboardspy().detachObserver(dialog);
        
        final String dl_path = dialog.getDownload_path();
        
        jTabbedPane1.setSelectedIndex(0);
        
        if(dialog.isDownload()) { 
            
            getMain_panel().getDownload_manager().setPreprocessing_transferences(true);
            
            final MainPanelView tthis = this;
            
            THREAD_POOL.execute(new Runnable(){
                    @Override
                    public void run() {

                    swingReflectionInvoke("setText", status_down_label, "Pre-processing downloads, please wait...");

                    Set<String> urls = new HashSet(findAllRegex("(?:https?|mega)://[^/]*/(#.*?)?!.+![^\r\n]+", dialog.getLinks_textarea().getText(), 0));
            
                    Set<String> megadownloader = new HashSet(findAllRegex("mega://enc.*?[^\r\n]+", dialog.getLinks_textarea().getText(), 0));

                    for (String link : megadownloader ) {

                        try {

                            urls.add(decryptMegaDownloaderLink(link));

                        } catch (Exception ex) {
                            getLogger(MainPanelView.class.getName()).log(SEVERE, null, ex);
                        }
                    }

                    if(!urls.isEmpty()) {

                        int conta_downloads = 0;

                        for (String url : urls ) {

                            url = url.replaceAll("^mega://", "https://mega.nz");

                            Download download;

                            if(findFirstRegex("#F!", url, 0) != null) {

                                FolderLinkDialog fdialog = new FolderLinkDialog(tthis, true, url);
  
                                if(!fdialog.isMega_error()) {
                                    
                                    swingReflectionInvokeAndWait("setLocationRelativeTo", fdialog, tthis);
                                
                                    swingReflectionInvokeAndWait("setVisible", fdialog, true);
                                    
                                    if(fdialog.isDownload()) {

                                        List<HashMap> folder_links = fdialog.getDownload_links();

                                        fdialog.dispose();

                                        for(HashMap folder_link:folder_links) {

                                            download = new Download(getMain_panel(), (String)folder_link.get("url"), dl_path, (String)folder_link.get("filename"), (String)folder_link.get("filekey"), (long)folder_link.get("filesize"), null, null, getMain_panel().isUse_slots_down(), getMain_panel().getDefault_slots_down(), true);

                                            getMain_panel().getDownload_manager().getTransference_provision_queue().add(download);

                                            conta_downloads++;
                                        }
                                    }
                                    
                                }
                                    
                                fdialog.dispose();
                                

                            } else {
                                
                                download = new Download(getMain_panel(), url, dl_path, null, null, null, null, null, getMain_panel().isUse_slots_down(), getMain_panel().getDefault_slots_down(), false);

                                getMain_panel().getDownload_manager().getTransference_provision_queue().add(download);

                                conta_downloads++;
                            }
                        }

                        if(conta_downloads > 0) {
                            
                            swingReflectionInvoke("setText", status_down_label, "Starting downloads provisioning, please wait...");

                            getMain_panel().getDownload_manager().secureNotify();

                        }
                } 

                swingReflectionInvoke("setText", status_down_label, "");
                
                swingReflectionInvoke("setEnabled", new_download_menu, true);
                
                getMain_panel().getDownload_manager().setPreprocessing_transferences(false);

                    }});
        } else {
            swingReflectionInvoke("setEnabled", new_download_menu, true);
        }
            
        
       
        dialog.dispose();

    }//GEN-LAST:event_new_download_menuActionPerformed

    private void settings_menuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_settings_menuActionPerformed
        
        
        SettingsDialog dialog = new SettingsDialog(this, true);
       
        swingReflectionInvokeAndWait("setLocationRelativeTo", dialog, this);
        
        swingReflectionInvokeAndWait("setVisible", dialog, true);
        
        if(dialog.isSettings_ok()) {

            for(String email:dialog.getDeleted_accounts()) {
                
                try {
                    deleteMegaAccount(email);
                } catch (SQLException ex) {
                    getLogger(MainPanelView.class.getName()).log(SEVERE, null, ex);
                }

                _main_panel.getMega_accounts().remove(email);

                _main_panel.getMega_active_accounts().remove(email);
            }
            
            _main_panel.loadUserSettings();
            
            _main_panel.getStream_supervisor().setMaxBytesPerSecInput(_main_panel.isLimit_download_speed()?_main_panel.getMax_dl_speed()*1024:0);
            
            _main_panel.getStream_supervisor().setMaxBytesPerSecOutput( _main_panel.isLimit_upload_speed()?_main_panel.getMax_up_speed()*1024:0);
            
            swingReflectionInvoke("setForeground", global_speed_down_label, _main_panel.isLimit_download_speed()?new Color(255,0,0):new Color(0,128,255));
            
            swingReflectionInvoke("setForeground", global_speed_up_label, _main_panel.isLimit_upload_speed()?new Color(255,0,0):new Color(0,128,255));
            
            _main_panel.getDownload_manager().secureNotify();

            _main_panel.getUpload_manager().secureNotify();
        }
       
        dialog.dispose();
    }//GEN-LAST:event_settings_menuActionPerformed

    private void hide_tray_menuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_hide_tray_menuActionPerformed
  
        dispatchEvent(new WindowEvent(this, WINDOW_CLOSING));
    }//GEN-LAST:event_hide_tray_menuActionPerformed

    private void about_menuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_about_menuActionPerformed
        
        AboutDialog dialog = new AboutDialog(this, true);
       
        swingReflectionInvokeAndWait("setLocationRelativeTo", dialog, this);
        
        swingReflectionInvokeAndWait("setVisible", dialog, true);
    }//GEN-LAST:event_about_menuActionPerformed

    private void exit_menuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exit_menuActionPerformed
        
        
        _main_panel._byebye();
    }//GEN-LAST:event_exit_menuActionPerformed

    private void close_all_finished_down_buttonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_close_all_finished_down_buttonActionPerformed

        _main_panel.getDownload_manager().closeAllFinished();
    }//GEN-LAST:event_close_all_finished_down_buttonActionPerformed

    private void clean_all_down_menuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clean_all_down_menuActionPerformed
        
        
        Object[] options = {"No",
                            "Yes"};
        
            int n = showOptionDialog(this,
            "Remove all waiting downloads?",
            "Warning!", YES_NO_CANCEL_OPTION, QUESTION_MESSAGE,
            null,
            options,
            options[0]);
        
            if(n==1) {
                _main_panel.getDownload_manager().closeAllWaiting();
            }
    }//GEN-LAST:event_clean_all_down_menuActionPerformed

    private void pause_all_down_buttonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pause_all_down_buttonActionPerformed
        
        
        _main_panel.getDownload_manager().pauseAll();
    }//GEN-LAST:event_pause_all_down_buttonActionPerformed

    private void new_stream_menuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_new_stream_menuActionPerformed
        
        StreamerDialog dialog = new StreamerDialog(this, true, _main_panel.getClipboardspy());
        
        _main_panel.getClipboardspy().attachObserver(dialog);

        swingReflectionInvokeAndWait("setLocationRelativeTo", dialog, this);

        swingReflectionInvokeAndWait("setVisible", dialog, true);
        
        _main_panel.getClipboardspy().detachObserver(dialog);
    }//GEN-LAST:event_new_stream_menuActionPerformed

    private void new_upload_menuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_new_upload_menuActionPerformed
        
        final FileGrabberDialog dialog = new FileGrabberDialog(this,true);
        
        try{
    
        swingReflectionInvoke("setEnabled", new_upload_menu, false);

        swingReflectionInvokeAndWait("setLocationRelativeTo", dialog, this);

        swingReflectionInvokeAndWait("setVisible", dialog, true);

        if(dialog.isUpload() && dialog.getFiles().size() > 0) {
        
                getMain_panel().getUpload_manager().setPreprocessing_transferences(true);
                
                swingReflectionInvoke("setText", status_up_label, "Pre-processing uploads, please wait...");
                
                final String mega_account = (String)dialog.getAccount_combobox().getSelectedItem();
                
                HashMap<String,Object> data_account = (HashMap)_main_panel.getMega_accounts().get(mega_account);
                
                final String base_path = dialog.getBase_path();
                
                final String dir_name=dialog.getDir_name_textfield().getText();

                
                    final int[] mega_aes_pass = bin2i32a(BASE642Bin((String)data_account.get("password_aes")));
                
        
                final String mega_user_hash = (String)data_account.get("user_hash");
                
                final ArrayList<File> files = dialog.getFiles();
                
                jTabbedPane1.setSelectedIndex(1);
                
                THREAD_POOL.execute(new Runnable(){
                    @Override
                    public void run() {
                        
                        MegaAPI ma=getMain_panel().getMega_active_accounts().get(mega_account)!=null?getMain_panel().getMega_active_accounts().get(mega_account):new MegaAPI();
              
                            try {
                                
                                ma.login(mega_account, mega_aes_pass, mega_user_hash);
                                
                                getMain_panel().getMega_active_accounts().put(mega_account, ma);
                                
                                byte[] parent_key = ma.genFolderKey();
                                
                                byte[] share_key = ma.genShareKey();
                        
                                HashMap<String,Object> res = ma.createDir(dir_name!=null?dir_name:files.get(0).getName()+"_"+genID(10), ma.getRoot_id(), parent_key, i32a2bin(ma.getMaster_key()));

                                String parent_node = (String)((Map)((List)res.get("f")).get(0)).get("h");

                                System.out.println("Dir "+ parent_node+" created");
                                
                                ma.shareFolder(parent_node, parent_key, share_key);
                                
                                String folder_link = ma.getPublicFolderLink(parent_node, share_key);
                                
                                MegaDirNode file_paths = new MegaDirNode(parent_node);

                                int conta = 1;

                                for(File f:files) {

                                        swingReflectionInvoke("setText", status_up_label, "Pre-processing ("+(conta++)+"/"+files.size()+") uploads, please wait...");

                                        String file_path = f.getParentFile().getAbsolutePath().replace(base_path, "");

                                        String[] dirs = file_path.split("/");

                                        System.out.println(file_path);

                                        MegaDirNode current_node = file_paths;

                                        String file_parent = current_node.getNode_id();

                                        for(String d:dirs) {

                                            if(!d.isEmpty()) {

                                                if(current_node.getChildren().get(d) != null) {

                                                    current_node = current_node.getChildren().get(d);

                                                    file_parent = current_node.getNode_id();

                                                } else {

                                                    res = ma.createDirInsideAnotherSharedDir(d, current_node.getNode_id(), ma.genFolderKey(), i32a2bin(ma.getMaster_key()), parent_node, share_key);

                                                    file_parent = (String)((Map)((List)res.get("f")).get(0)).get("h");

                                                    current_node.getChildren().put(d, new MegaDirNode(file_parent));

                                                    current_node = current_node.getChildren().get(d);
                                                }
                                            }
                                        }

                                        Upload upload = new Upload(getMain_panel(), ma, f.getAbsolutePath(), file_parent, null, null, parent_node, share_key, folder_link, getMain_panel().isUse_slots_up(), getMain_panel().getDefault_slots_up(), false);

                                        getMain_panel().getUpload_manager().getTransference_provision_queue().add(upload);
                            
                                    }
                        
                                    swingReflectionInvoke("setText", status_up_label, "Starting uploads provisioning, please wait...");

                                    getMain_panel().getUpload_manager().secureNotify();
                          
                            } catch (Exception ex) {
                                
                                getLogger(MainPanelView.class.getName()).log(SEVERE, null, ex);
                            }
                            
                            swingReflectionInvoke("setEnabled", new_upload_menu, true);
                            
                            getMain_panel().getUpload_manager().setPreprocessing_transferences(false);
                    }
                    
                });
            
        } else {
            swingReflectionInvoke("setEnabled", new_upload_menu, true);
        } 
        
        }catch(Exception ex) {}

        dialog.dispose();

    }//GEN-LAST:event_new_upload_menuActionPerformed

    private void close_all_finished_up_buttonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_close_all_finished_up_buttonActionPerformed
        
        
        _main_panel.getUpload_manager().closeAllFinished();
    }//GEN-LAST:event_close_all_finished_up_buttonActionPerformed

    private void pause_all_up_buttonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pause_all_up_buttonActionPerformed
        
        
        _main_panel.getUpload_manager().pauseAll();
    }//GEN-LAST:event_pause_all_up_buttonActionPerformed

    private void clean_all_up_menuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clean_all_up_menuActionPerformed
        
         Object[] options = {"No",
                            "Yes"};
        
            int n = showOptionDialog(this,
            "Remove all waiting uploads?",
            "Warning!", YES_NO_CANCEL_OPTION, QUESTION_MESSAGE,
            null,
            options,
            options[0]);
        
            if(n==1) {
                _main_panel.getUpload_manager().closeAllWaiting();
            }
    }//GEN-LAST:event_clean_all_up_menuActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JMenuItem about_menu;
    private javax.swing.JMenuItem clean_all_down_menu;
    private javax.swing.JMenuItem clean_all_up_menu;
    private javax.swing.JButton close_all_finished_down_button;
    private javax.swing.JButton close_all_finished_up_button;
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
    private javax.swing.JPopupMenu.Separator jSeparator1;
    private javax.swing.JPopupMenu.Separator jSeparator2;
    private javax.swing.JPopupMenu.Separator jSeparator3;
    private javax.swing.JPopupMenu.Separator jSeparator4;
    private javax.swing.JPopupMenu.Separator jSeparator5;
    private javax.swing.JPopupMenu.Separator jSeparator6;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JLabel kiss_server_status;
    private javax.swing.JLabel logo_label;
    private javax.swing.JMenuBar main_menubar;
    private javax.swing.JMenuItem new_download_menu;
    private javax.swing.JMenuItem new_stream_menu;
    private javax.swing.JMenuItem new_upload_menu;
    private javax.swing.JButton pause_all_down_button;
    private javax.swing.JButton pause_all_up_button;
    private javax.swing.JMenuItem settings_menu;
    private javax.swing.JLabel status_down_label;
    private javax.swing.JLabel status_up_label;
    private javax.swing.JPanel uploads_panel;
    // End of variables declaration//GEN-END:variables

    
    
  
    
}
