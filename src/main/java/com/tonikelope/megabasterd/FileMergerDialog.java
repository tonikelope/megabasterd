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

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;

import static com.tonikelope.megabasterd.MainPanel.GUI_FONT;
import static com.tonikelope.megabasterd.MainPanel.THREAD_POOL;
import static com.tonikelope.megabasterd.MiscTools.translateLabels;
import static com.tonikelope.megabasterd.MiscTools.truncateText;
import static com.tonikelope.megabasterd.MiscTools.updateFonts;
import static java.lang.Integer.MAX_VALUE;
import static javax.swing.JOptionPane.YES_NO_CANCEL_OPTION;
import static javax.swing.JOptionPane.showOptionDialog;

/**
 *
 * @author tonikelope
 */
public class FileMergerDialog extends javax.swing.JDialog {

    private final MainPanel _main_panel;
    private File _output_dir = null;
    private volatile long _progress = 0L;
    private final ArrayList<String> _file_parts = new ArrayList<>();
    private String _file_name = null;
    private long _file_size = 0L;
    private volatile boolean _exit = false;
    private volatile String _file_name_full;

    /**
     * Creates new form FileSplitterDialog
     */
    public FileMergerDialog(MainPanelView parent, boolean modal) {
        super(parent, modal);
        _main_panel = parent.getMain_panel();

        MiscTools.GUIRunAndWait(() -> {
            initComponents();
            updateFonts(this, GUI_FONT, _main_panel.getZoom_factor());
            translateLabels(this);
            jProgressBar2.setMinimum(0);
            jProgressBar2.setMaximum(MAX_VALUE);
            jProgressBar2.setStringPainted(true);
            jProgressBar2.setValue(0);
            jProgressBar2.setVisible(false);

            pack();
        });
    }

    private void monitorProgress(Path file) {

        THREAD_POOL.execute(() -> {

            long p = 0;

            while (!_exit && p < _file_size) {

                try {

                    if (Files.exists(file)) {

                        p = Files.size(file);

                        long fp = p;

                        MiscTools.GUIRunAndWait(() -> {
                            if (jProgressBar2.getValue() < jProgressBar2.getMaximum()) {
                                jProgressBar2.setValue((int) Math.floor((MAX_VALUE / (double) _file_size) * fp));
                            }
                        });
                    }

                    MiscTools.pause(2000);

                } catch (IOException ex) {
                    LOG.log(Level.FATAL, "IOException monitoring progress!", ex);
                }
            }

        });

    }

    private boolean _mergeFile() throws IOException {

        try (RandomAccessFile targetFile = new RandomAccessFile(_file_name_full, "rw")) {

            FileChannel targetChannel = targetFile.getChannel();

            monitorProgress(Paths.get(_file_name));

            for (String file_path : this._file_parts) {

                if (_exit) {
                    break;
                }

                RandomAccessFile rfile = new RandomAccessFile(file_path, "r");

                targetChannel.transferFrom(rfile.getChannel(), this._progress, rfile.length());

                _progress += rfile.length();

                MiscTools.GUIRun(() -> {
                    jProgressBar2.setValue((int) Math.floor((MAX_VALUE / (double) _file_size) * _progress));
                });
            }
        }

        if (Files.exists(Paths.get(_file_name_full + ".sha1"))) {

            String sha1 = Files.readString(Paths.get(_file_name_full + ".sha1")).toLowerCase().trim();

            MiscTools.GUIRunAndWait(() -> {
                merge_button.setText(LabelTranslatorSingleton.getInstance().translate("CHECKING FILE INTEGRITY, please wait..."));
            });

            if (sha1.equals(MiscTools.computeFileSHA1(new File(_file_name_full)))) {
                JOptionPane.showMessageDialog(this, LabelTranslatorSingleton.getInstance().translate("FILE INTEGRITY IS OK"));
                return true;
            } else {
                JOptionPane.showMessageDialog(this, LabelTranslatorSingleton.getInstance().translate("FILE INTEGRITY CHECK FAILED"), "ERROR", JOptionPane.ERROR_MESSAGE);
                return false;
            }
        }

        return true;
    }

    private void _deleteParts() {

        try {
            this._file_parts.stream().map(File::new).forEachOrdered(File::delete);

            Files.deleteIfExists(Paths.get(_file_name_full + ".sha1"));
        } catch (IOException ex) {
            LOG.log(Level.FATAL, "Error deleting file parts!", ex);
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        file_button = new javax.swing.JButton();
        file_name_label = new javax.swing.JLabel();
        output_button = new javax.swing.JButton();
        file_size_label = new javax.swing.JLabel();
        output_folder_label = new javax.swing.JLabel();
        jProgressBar2 = new javax.swing.JProgressBar();
        merge_button = new javax.swing.JButton();
        delete_parts_checkbox = new javax.swing.JCheckBox();

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("File Merger");
        setResizable(false);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        file_button.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        file_button.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/icons8-add-file-30.png"))); // NOI18N
        file_button.setText("Select (any) file part");
        file_button.setDoubleBuffered(true);
        file_button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                file_buttonActionPerformed(evt);
            }
        });

        file_name_label.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        file_name_label.setDoubleBuffered(true);

        output_button.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        output_button.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/icons8-add-folder-30.png"))); // NOI18N
        output_button.setText("Change output folder");
        output_button.setDoubleBuffered(true);
        output_button.setEnabled(false);
        output_button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                output_buttonActionPerformed(evt);
            }
        });

        file_size_label.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        file_size_label.setDoubleBuffered(true);

        output_folder_label.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        output_folder_label.setDoubleBuffered(true);

        jProgressBar2.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        jProgressBar2.setDoubleBuffered(true);

        merge_button.setBackground(new java.awt.Color(102, 204, 255));
        merge_button.setFont(new java.awt.Font("Dialog", 1, 24)); // NOI18N
        merge_button.setForeground(new java.awt.Color(255, 255, 255));
        merge_button.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/icons8-glue-30.png"))); // NOI18N
        merge_button.setText("MERGE FILE");
        merge_button.setDoubleBuffered(true);
        merge_button.setEnabled(false);
        merge_button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                merge_buttonActionPerformed(evt);
            }
        });

        delete_parts_checkbox.setFont(new java.awt.Font("Dialog", 0, 14)); // NOI18N
        delete_parts_checkbox.setSelected(true);
        delete_parts_checkbox.setText("Delete parts after merge");
        delete_parts_checkbox.setDoubleBuffered(true);
        delete_parts_checkbox.setEnabled(false);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(delete_parts_checkbox)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addComponent(file_button, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(file_name_label, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(output_button, javax.swing.GroupLayout.DEFAULT_SIZE, 576, Short.MAX_VALUE)
                    .addComponent(file_size_label, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(output_folder_label, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jProgressBar2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(merge_button, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(file_button)
                .addGap(9, 9, 9)
                .addComponent(file_name_label)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(file_size_label)
                .addGap(18, 18, 18)
                .addComponent(output_button)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(output_folder_label)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jProgressBar2, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, Short.MAX_VALUE)
                .addComponent(delete_parts_checkbox)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(merge_button)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void file_buttonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_file_buttonActionPerformed
        // TODO add your handling code here:

        this.file_button.setText(LabelTranslatorSingleton.getInstance().translate("Selecting file..."));

        this.file_button.setEnabled(false);

        JFileChooser filechooser = new javax.swing.JFileChooser();

        updateFonts(filechooser, GUI_FONT, (float) (_main_panel.getZoom_factor() * 1.25));

        filechooser.setDialogTitle("Select any part of the original file");

        filechooser.setAcceptAllFileFilterUsed(false);

        if (filechooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION && filechooser.getSelectedFile().canRead()) {

            this._file_name = MiscTools.findFirstRegex("^(.+)\\.part[0-9]+\\-[0-9]+$", filechooser.getSelectedFile().getName(), 1);

            this._file_name_full = MiscTools.findFirstRegex("^(.+)\\.part[0-9]+\\-[0-9]+$", filechooser.getSelectedFile().getAbsolutePath(), 1);

            if (this._file_name != null) {

                this.file_name_label.setText(truncateText(this._file_name, 150));

                this.file_name_label.setToolTipText(filechooser.getSelectedFile().getParentFile().getAbsolutePath() + "/" + this._file_name);

                this.output_folder_label.setText(truncateText(filechooser.getSelectedFile().getParentFile().getAbsolutePath(), 150));

                this.output_folder_label.setToolTipText(filechooser.getSelectedFile().getParentFile().getAbsolutePath());

                this._output_dir = new File(filechooser.getSelectedFile().getParentFile().getAbsolutePath());

                File directory = filechooser.getSelectedFile().getParentFile();

                File[] fList = directory.listFiles();

                _file_size = 0L;

                for (File file : fList) {

                    if (file.isFile() && file.canRead() && file.getName().startsWith(this._file_name + ".part")) {

                        this._file_parts.add(file.getAbsolutePath());

                        _file_size += file.length();

                    }
                }

                Collections.sort(this._file_parts);

                this.file_size_label.setText(MiscTools.formatBytes(_file_size));

                this.output_button.setEnabled(true);

                this.delete_parts_checkbox.setEnabled(true);

                this.merge_button.setEnabled(true);
            }

        }

        this.file_button.setText(LabelTranslatorSingleton.getInstance().translate("Select (any) file part"));

        this.file_button.setEnabled(true);

        pack();

    }//GEN-LAST:event_file_buttonActionPerformed

    private void output_buttonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_output_buttonActionPerformed
        // TODO add your handling code here:

        this.output_button.setText(LabelTranslatorSingleton.getInstance().translate("Changing output folder..."));

        this.file_button.setEnabled(false);

        this.output_button.setEnabled(false);

        this.merge_button.setEnabled(false);

        this.delete_parts_checkbox.setEnabled(false);

        JFileChooser filechooser = new javax.swing.JFileChooser();

        updateFonts(filechooser, GUI_FONT, (float) (_main_panel.getZoom_factor() * 1.25));

        filechooser.setDialogTitle("Add directory");

        filechooser.setFileSelectionMode(javax.swing.JFileChooser.DIRECTORIES_ONLY);

        filechooser.setAcceptAllFileFilterUsed(false);

        if (filechooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION && filechooser.getSelectedFile().canRead()) {

            this._output_dir = filechooser.getSelectedFile();

            this.output_folder_label.setText(truncateText(this._output_dir.getAbsolutePath(), 100));

            this.output_folder_label.setToolTipText(this._output_dir.getAbsolutePath());
        }

        this.output_button.setText(LabelTranslatorSingleton.getInstance().translate("Change output folder"));

        this.file_button.setEnabled(true);

        this.output_button.setEnabled(true);

        this.merge_button.setEnabled(true);

        this.delete_parts_checkbox.setEnabled(true);

        pack();
    }//GEN-LAST:event_output_buttonActionPerformed

    private void merge_buttonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_merge_buttonActionPerformed
        // TODO add your handling code here:

        if (this._output_dir != null) {

            setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

            this.merge_button.setText(LabelTranslatorSingleton.getInstance().translate("MERGING FILE..."));

            this.file_button.setEnabled(false);

            this.output_button.setEnabled(false);

            this.merge_button.setEnabled(false);

            this.delete_parts_checkbox.setEnabled(false);

            this.jProgressBar2.setVisible(true);

            pack();

            Dialog tthis = this;

            THREAD_POOL.execute(() -> {
                try {
                    if (_mergeFile()) {
                        if (delete_parts_checkbox.isSelected()) {
                            _deleteParts();
                        }

                        if (!_exit) {
                            MiscTools.GUIRun(() -> {
                                jProgressBar2.setValue(jProgressBar2.getMaximum());

                                JOptionPane.showMessageDialog(tthis, LabelTranslatorSingleton.getInstance().translate("File successfully merged!"));

                                if (Desktop.isDesktopSupported()) {
                                    try {
                                        Desktop.getDesktop().open(_output_dir);
                                    } catch (Exception ex) {
                                        LOG.log(Level.FATAL, ex.getMessage());
                                    }
                                }

                                _exit = true;
                                dispose();
                            });
                        }
                    } else {
                        _file_parts.clear();
                        MiscTools.GUIRun(() -> {
                            file_name_label.setText("");

                            file_size_label.setText("");

                            output_folder_label.setText("");

                            _output_dir = null;

                            _file_name = null;

                            _file_size = 0L;

                            _progress = 0L;

                            jProgressBar2.setMinimum(0);
                            jProgressBar2.setMaximum(MAX_VALUE);
                            jProgressBar2.setStringPainted(true);
                            jProgressBar2.setValue(0);
                            jProgressBar2.setVisible(false);

                            merge_button.setText(LabelTranslatorSingleton.getInstance().translate("MERGE FILE"));

                            file_button.setEnabled(true);

                            output_button.setEnabled(true);

                            merge_button.setEnabled(true);

                            delete_parts_checkbox.setEnabled(true);

                            setDefaultCloseOperation(DISPOSE_ON_CLOSE);

                            pack();
                        });
                    }
                } catch (Exception ex) {
                    LOG.log(Level.FATAL, ex.getMessage());
                }
            });

        }
    }//GEN-LAST:event_merge_buttonActionPerformed

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        // TODO add your handling code here:
        Object[] options = {"No",
            LabelTranslatorSingleton.getInstance().translate("Yes")};

        int n = 1;

        if (!this.file_button.isEnabled()) {
            n = showOptionDialog(this,
                    LabelTranslatorSingleton.getInstance().translate("SURE?"),
                    LabelTranslatorSingleton.getInstance().translate("EXIT"), YES_NO_CANCEL_OPTION, javax.swing.JOptionPane.WARNING_MESSAGE,
                    null,
                    options,
                    options[0]);
        }

        if (n == 1) {
            _exit = true;

            _main_panel.getView().getMerge_file_menu().setEnabled(this.file_button.isEnabled());

            dispose();
        }
    }//GEN-LAST:event_formWindowClosing

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JCheckBox delete_parts_checkbox;
    private javax.swing.JButton file_button;
    private javax.swing.JLabel file_name_label;
    private javax.swing.JLabel file_size_label;
    private javax.swing.JProgressBar jProgressBar2;
    private javax.swing.JButton merge_button;
    private javax.swing.JButton output_button;
    private javax.swing.JLabel output_folder_label;
    // End of variables declaration//GEN-END:variables
    private static final Logger LOG = LogManager.getLogger();
}
