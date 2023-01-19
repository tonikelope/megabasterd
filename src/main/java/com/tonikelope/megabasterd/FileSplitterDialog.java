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
import static javax.swing.JOptionPane.YES_NO_CANCEL_OPTION;
import static javax.swing.JOptionPane.showOptionDialog;

/**
 *
 * @author tonikelope
 */
public class FileSplitterDialog extends javax.swing.JDialog {

    private final MainPanel _main_panel;
    private File[] _files = null;
    private File _output_dir = null;
    private volatile String _sha1 = null;
    private volatile long _progress = 0L;
    private volatile Path _current_part = null;
    private volatile int _current_file = 0;
    private volatile boolean _exit = false;

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

    private boolean _splitFile(int i) throws IOException {

        _sha1 = "";

        THREAD_POOL.execute(() -> {

            try {
                _sha1 = MiscTools.computeFileSHA1(new File(_files[i].getAbsolutePath()));
            } catch (IOException ex) {
                Logger.getLogger(FileSplitterDialog.class.getName()).log(Level.SEVERE, null, ex);
            }
        });

        this._progress = 0L;

        int mBperSplit = Integer.parseInt(this.split_size_text.getText());

        if (mBperSplit <= 0) {
            throw new IllegalArgumentException("mBperSplit must be more than zero");
        }

        final long sourceSize = Files.size(Paths.get(this._files[i].getAbsolutePath()));
        final long bytesPerSplit = 1024L * 1024L * mBperSplit;
        final long numSplits = sourceSize / bytesPerSplit;
        final long remainingBytes = sourceSize % bytesPerSplit;
        int position = 0;
        int conta_split = 1;

        MiscTools.GUIRunAndWait(() -> {
            jProgressBar2.setMinimum(0);
            jProgressBar2.setMaximum(MAX_VALUE);
            jProgressBar2.setStringPainted(true);
            jProgressBar2.setValue(0);
            file_name_label.setText(truncateText(_files[i].getName(), 150));
            file_name_label.setToolTipText(_files[i].getAbsolutePath());
            file_size_label.setText(MiscTools.formatBytes(_files[i].length()));
            pack();

        });

        try ( RandomAccessFile sourceFile = new RandomAccessFile(this._files[i].getAbsolutePath(), "r");  FileChannel sourceChannel = sourceFile.getChannel()) {

            for (; position < numSplits && !_exit; position++, conta_split++) {
                _writePartToFile(i, bytesPerSplit, position * bytesPerSplit, sourceChannel, conta_split, numSplits + (remainingBytes > 0 ? 1 : 0));
            }

            if (remainingBytes > 0 && !_exit) {
                _writePartToFile(i, remainingBytes, position * bytesPerSplit, sourceChannel, conta_split, numSplits + (remainingBytes > 0 ? 1 : 0));
            }
        }

        while ("".equals(_sha1)) {
            MiscTools.GUIRunAndWait(() -> {

                split_button.setText(LabelTranslatorSingleton.getInstance().translate("GENERATING SHA1, please wait..."));

            });

            MiscTools.pausar(1000);
        }

        if (_sha1 != null) {
            Files.writeString(Paths.get(this._files[i].getAbsolutePath() + ".sha1"), _sha1);
        }

        return true;
    }

    private void monitorProgress(int f, long part_size) {

        THREAD_POOL.execute(() -> {

            long p = 0;

            Path file = _current_part;

            while (!_exit && f == _current_file && file == _current_part && p < part_size) {
                try {
                    if (Files.exists(_current_part)) {

                        p = Files.size(file);

                        long fp = _progress + p;

                        MiscTools.GUIRunAndWait(() -> {
                            if (jProgressBar2.getValue() < jProgressBar2.getMaximum()) {
                                jProgressBar2.setValue((int) Math.floor((MAX_VALUE / (double) _files[f].length()) * fp));
                            }
                        });
                    }
                    MiscTools.pausar(2000);
                } catch (IOException ex) {
                    Logger.getLogger(FileSplitterDialog.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

        });

    }

    private void _writePartToFile(int f, long byteSize, long position, FileChannel sourceChannel, int conta_split, long num_splits) throws IOException {

        Path fileName = Paths.get(this._output_dir.getAbsolutePath() + "/" + this._files[f].getName() + ".part" + String.valueOf(conta_split) + "-" + String.valueOf(num_splits));

        _current_part = fileName;

        _current_file = f;

        monitorProgress(f, byteSize);

        if (!_exit) {
            try ( RandomAccessFile toFile = new RandomAccessFile(fileName.toFile(), "rw");  FileChannel toChannel = toFile.getChannel()) {
                sourceChannel.position(position);
                toChannel.transferFrom(sourceChannel, 0, byteSize);
            }
        }

        _progress += byteSize;
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

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("File Splitter");
        setResizable(false);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        file_button.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        file_button.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/icons8-add-file-30.png"))); // NOI18N
        file_button.setText("Select file/s");
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
        split_button.setText("SPLIT FILE/s");
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

        filechooser.setMultiSelectionEnabled(true);

        updateFonts(filechooser, GUI_FONT, (float) (_main_panel.getZoom_factor() * 1.25));

        filechooser.setDialogTitle("Select file/s");

        filechooser.setAcceptAllFileFilterUsed(false);

        if (filechooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION && filechooser.getSelectedFile().canRead()) {

            this._files = filechooser.getSelectedFiles();
            this.file_name_label.setText(truncateText(this._files[0].getName(), 150));
            this.file_name_label.setToolTipText(this._files[0].getAbsolutePath());
            this.file_size_label.setText(MiscTools.formatBytes(this._files[0].length()));
            this.output_folder_label.setText(truncateText(this._files[0].getParentFile().getAbsolutePath(), 150));
            this.output_folder_label.setToolTipText(this._files[0].getParentFile().getAbsolutePath());
            this._output_dir = new File(this._files[0].getParentFile().getAbsolutePath());
            this.jProgressBar2.setMinimum(0);
            this.jProgressBar2.setMaximum(MAX_VALUE);
            this.jProgressBar2.setStringPainted(true);
            this.jProgressBar2.setValue(0);

            this.output_button.setEnabled(true);
            this.split_size_label.setEnabled(true);
            this.split_size_text.setEnabled(true);
            this.split_button.setEnabled(true);
        }

        this.file_button.setText(LabelTranslatorSingleton.getInstance().translate("Select file/s"));

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
                    for (int i = 0; i < this._files.length && !_exit; i++) {

                        if (_splitFile(i)) {

                            if (i == this._files.length - 1 && !_exit) {

                                MiscTools.GUIRun(() -> {

                                    JOptionPane.showMessageDialog(tthis, LabelTranslatorSingleton.getInstance().translate("File/s successfully splitted!"));

                                    if (Desktop.isDesktopSupported()) {
                                        try {
                                            Desktop.getDesktop().open(_output_dir);
                                        } catch (Exception ex) {
                                            Logger.getLogger(FileSplitterDialog.class.getName()).log(Level.SEVERE, ex.getMessage());
                                        }
                                    }

                                    _exit = true;

                                    dispose();
                                });
                            }

                        } else {
                            _files = null;
                            _output_dir = null;
                            MiscTools.GUIRun(() -> {
                                file_name_label.setText("");

                                output_folder_label.setText("");

                                split_size_text.setText("");

                                file_size_label.setText("");

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

                                pack();
                            });
                        }

                    }
                } catch (Exception ex) {
                    Logger.getLogger(FileSplitterDialog.class.getName()).log(Level.SEVERE, ex.getMessage());
                }
            });

        }
    }//GEN-LAST:event_split_buttonActionPerformed

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

            _main_panel.getView().getSplit_file_menu().setEnabled(this.file_button.isEnabled());

            dispose();
        }
    }//GEN-LAST:event_formWindowClosing

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
