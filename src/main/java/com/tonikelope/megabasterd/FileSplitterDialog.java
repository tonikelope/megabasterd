/*
 * Copyright (C) 2018 tonikelope
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.tonikelope.megabasterd;

import static com.tonikelope.megabasterd.MainPanel.GUI_FONT;
import static com.tonikelope.megabasterd.MainPanel.THREAD_POOL;
import static com.tonikelope.megabasterd.MiscTools.translateLabels;
import static com.tonikelope.megabasterd.MiscTools.truncateText;
import static com.tonikelope.megabasterd.MiscTools.updateFonts;
import java.awt.Desktop;
import java.awt.Dialog;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import static java.lang.Integer.MAX_VALUE;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import static javax.swing.WindowConstants.DISPOSE_ON_CLOSE;
import static javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE;

/**
 *
 * @author tonikelope
 */
public class FileSplitterDialog extends javax.swing.JDialog {

    private final MainPanel _main_panel;
    private File _file = null;
    private File _output_dir = null;
    private long _progress = 0L;

    /**
     * Creates new form FileSplitterDialog
     */
    public FileSplitterDialog(MainPanelView parent, boolean modal) {
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

            split_size_text.addKeyListener(new java.awt.event.KeyAdapter() {

                public void keyReleased(java.awt.event.KeyEvent evt) {
                    try {
                        Integer.parseInt(split_size_text.getText());
                    } catch (Exception e) {
                        split_size_text.setText(split_size_text.getText().substring(0, Math.max(0, split_size_text.getText().length() - 1)));
                    }
                }
            });

            split_size_text.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

            pack();
        });
    }

    private boolean _splitFile() throws IOException {

        int mBperSplit = Integer.parseInt(this.split_size_text.getText());

        if (mBperSplit <= 0) {
            throw new IllegalArgumentException("mBperSplit must be more than zero");
        }

        final long sourceSize = Files.size(Paths.get(this._file.getAbsolutePath()));
        final long bytesPerSplit = 1024L * 1024L * mBperSplit;
        final long numSplits = sourceSize / bytesPerSplit;
        final long remainingBytes = sourceSize % bytesPerSplit;
        int position = 0;
        int conta_split = 1;

        try (RandomAccessFile sourceFile = new RandomAccessFile(this._file.getAbsolutePath(), "r"); FileChannel sourceChannel = sourceFile.getChannel()) {

            for (; position < numSplits; position++, conta_split++) {
                _writePartToFile(bytesPerSplit, position * bytesPerSplit, sourceChannel, conta_split, numSplits + (remainingBytes > 0 ? 1 : 0));
            }

            if (remainingBytes > 0) {
                _writePartToFile(remainingBytes, position * bytesPerSplit, sourceChannel, conta_split, numSplits + (remainingBytes > 0 ? 1 : 0));
            }
        }

        return true;
    }

    private void _writePartToFile(long byteSize, long position, FileChannel sourceChannel, int conta_split, long num_splits) throws IOException {

        Path fileName = Paths.get(this._output_dir.getAbsolutePath() + "/" + this._file.getName() + ".part" + String.valueOf(conta_split) + "-" + String.valueOf(num_splits));
        try (RandomAccessFile toFile = new RandomAccessFile(fileName.toFile(), "rw"); FileChannel toChannel = toFile.getChannel()) {
            sourceChannel.position(position);
            toChannel.transferFrom(sourceChannel, 0, byteSize);
        }

        _progress += byteSize;

        MiscTools.GUIRun(() -> {
            jProgressBar2.setValue((int) Math.floor((MAX_VALUE / (double) _file.length()) * _progress));
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

        file_button = new javax.swing.JButton();
        file_name_label = new javax.swing.JLabel();
        output_button = new javax.swing.JButton();
        file_size_label = new javax.swing.JLabel();
        output_folder_label = new javax.swing.JLabel();
        split_size_label = new javax.swing.JLabel();
        split_size_text = new javax.swing.JTextField();
        jProgressBar2 = new javax.swing.JProgressBar();
        split_button = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("File Splitter");
        setResizable(false);

        file_button.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        file_button.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/icons8-add-file-30.png"))); // NOI18N
        file_button.setText("Select file");
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

        split_size_label.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        split_size_label.setText("Split size (MBs):");
        split_size_label.setDoubleBuffered(true);
        split_size_label.setEnabled(false);

        split_size_text.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        split_size_text.setBorder(null);
        split_size_text.setDoubleBuffered(true);
        split_size_text.setEnabled(false);

        jProgressBar2.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        jProgressBar2.setDoubleBuffered(true);

        split_button.setBackground(new java.awt.Color(102, 204, 255));
        split_button.setFont(new java.awt.Font("Dialog", 1, 24)); // NOI18N
        split_button.setForeground(new java.awt.Color(255, 255, 255));
        split_button.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/icons8-cut-30.png"))); // NOI18N
        split_button.setText("SPLIT FILE");
        split_button.setDoubleBuffered(true);
        split_button.setEnabled(false);
        split_button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                split_buttonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(file_button, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(file_name_label, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(output_button, javax.swing.GroupLayout.DEFAULT_SIZE, 576, Short.MAX_VALUE)
                    .addComponent(file_size_label, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(output_folder_label, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jProgressBar2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(split_size_label)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(split_size_text))
                    .addComponent(split_button, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
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
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(split_size_label)
                    .addComponent(split_size_text, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addComponent(jProgressBar2, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(split_button)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void file_buttonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_file_buttonActionPerformed
        // TODO add your handling code here:

        this.file_button.setText(LabelTranslatorSingleton.getInstance().translate("Opening file..."));

        this.file_button.setEnabled(false);

        JFileChooser filechooser = new javax.swing.JFileChooser();

        updateFonts(filechooser, GUI_FONT, (float) (_main_panel.getZoom_factor() * 1.25));

        filechooser.setDialogTitle("Select file");

        filechooser.setAcceptAllFileFilterUsed(false);

        if (filechooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION && filechooser.getSelectedFile().canRead()) {

            this._file = filechooser.getSelectedFile();
            this.file_name_label.setText(truncateText(this._file.getAbsolutePath(), 100));
            this.file_name_label.setToolTipText(this._file.getAbsolutePath());
            this.file_size_label.setText(MiscTools.formatBytes(this._file.length()));
            this.output_folder_label.setText(truncateText(this._file.getParentFile().getAbsolutePath(), 100));
            this.output_folder_label.setToolTipText(this._file.getParentFile().getAbsolutePath());
            this._output_dir = new File(this._file.getParentFile().getAbsolutePath());
            this.jProgressBar2.setMinimum(0);
            this.jProgressBar2.setMaximum(MAX_VALUE);
            this.jProgressBar2.setStringPainted(true);
            this.jProgressBar2.setValue(0);
            this._progress = 0L;

            this.output_button.setEnabled(true);
            this.split_size_label.setEnabled(true);
            this.split_size_text.setEnabled(true);
            this.split_button.setEnabled(true);
        }

        this.file_button.setText(LabelTranslatorSingleton.getInstance().translate("Select file"));

        this.file_button.setEnabled(true);

        pack();

    }//GEN-LAST:event_file_buttonActionPerformed

    private void output_buttonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_output_buttonActionPerformed
        // TODO add your handling code here:

        this.output_button.setText(LabelTranslatorSingleton.getInstance().translate("Changing output folder..."));

        this.file_button.setEnabled(false);

        this.output_button.setEnabled(false);

        this.split_button.setEnabled(false);

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

        this.split_button.setEnabled(true);

        pack();
    }//GEN-LAST:event_output_buttonActionPerformed

    private void split_buttonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_split_buttonActionPerformed
        // TODO add your handling code here:

        if (this._output_dir != null && !"".equals(this.split_size_text.getText())) {

            this.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

            this.split_button.setText(LabelTranslatorSingleton.getInstance().translate("SPLITTING FILE..."));

            this.file_button.setEnabled(false);

            this.output_button.setEnabled(false);

            this.split_button.setEnabled(false);

            this.split_size_text.setEnabled(false);

            this.jProgressBar2.setVisible(true);

            pack();

            Dialog tthis = this;

            THREAD_POOL.execute(() -> {
                try {
                    if (_splitFile()) {
                        MiscTools.GUIRun(() -> {
                            JOptionPane.showMessageDialog(tthis, LabelTranslatorSingleton.getInstance().translate("File successfully splitted!"));

                            if (Desktop.isDesktopSupported()) {
                                try {
                                    Desktop.getDesktop().open(_output_dir);
                                } catch (IOException ex) {

                                }
                            }

                            setDefaultCloseOperation(DISPOSE_ON_CLOSE);

                            setVisible(false);
                        });
                    } else {
                        _file = null;
                        _output_dir = null;
                        MiscTools.GUIRun(() -> {
                            file_name_label.setText("");

                            output_folder_label.setText("");

                            split_size_text.setText("");

                            file_size_label.setText("");

                            _progress = 0L;

                            jProgressBar2.setMinimum(0);
                            jProgressBar2.setMaximum(MAX_VALUE);
                            jProgressBar2.setStringPainted(true);
                            jProgressBar2.setValue(0);
                            jProgressBar2.setVisible(false);

                            split_button.setText(LabelTranslatorSingleton.getInstance().translate("SPLIT FILE"));

                            file_button.setEnabled(true);

                            output_button.setEnabled(true);

                            split_button.setEnabled(true);

                            split_size_text.setEnabled(true);

                            setDefaultCloseOperation(DISPOSE_ON_CLOSE);

                            pack();
                        });
                    }
                } catch (IOException ex) {
                    Logger.getLogger(FileSplitterDialog.class.getName()).log(Level.SEVERE, ex.getMessage());
                }
            });

        }
    }//GEN-LAST:event_split_buttonActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton file_button;
    private javax.swing.JLabel file_name_label;
    private javax.swing.JLabel file_size_label;
    private javax.swing.JProgressBar jProgressBar2;
    private javax.swing.JButton output_button;
    private javax.swing.JLabel output_folder_label;
    private javax.swing.JButton split_button;
    private javax.swing.JLabel split_size_label;
    private javax.swing.JTextField split_size_text;
    // End of variables declaration//GEN-END:variables
    private static final Logger LOG = Logger.getLogger(FileSplitterDialog.class.getName());
}
