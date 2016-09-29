package megabasterd;

import java.awt.Color;
import java.awt.Font;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import static megabasterd.MainPanel.FONT_DEFAULT;
import static megabasterd.MainPanel.THREAD_POOL;
import static megabasterd.MiscTools.copyTextToClipboard;
import static megabasterd.MiscTools.swingReflectionInvoke;
import static megabasterd.MiscTools.swingReflectionInvokeAndWait;
import static megabasterd.MiscTools.swingReflectionInvokeAndWaitForReturn;
import static megabasterd.MiscTools.updateFont;


public final class DownloadView extends javax.swing.JPanel implements TransferenceView {

    private final Download _download;

    public JButton getClose_button() {
        return close_button;
    }

    public JButton getCopy_link_button() {
        return copy_link_button;
    }

    public JLabel getFile_name_label() {
        return file_name_label;
    }

    public JLabel getFile_size_label() {
        return file_size_label;
    }

    public JCheckBox getKeep_temp_checkbox() {
        return keep_temp_checkbox;
    }

    public JButton getPause_button() {
        return pause_button;
    }

    public JProgressBar getProgress_pbar() {
        return progress_pbar;
    }

    public JLabel getRemtime_label() {
        return remtime_label;
    }

    public JButton getRestart_button() {
        return restart_button;
    }

    public JLabel getSlot_status_label() {
        return slot_status_label;
    }

    public JLabel getSlots_label() {
        return slots_label;
    }

    public JSpinner getSlots_spinner() {
        return slots_spinner;
    }

    public JLabel getSpeed_label() {
        return speed_label;
    }

    public JLabel getStatus_label() {
        return status_label;
    }

    public JButton getStop_button() {
        return stop_button;
    }

    public DownloadView(Download download) {
        
        initComponents();
        
        _download = download;
        
        updateFont(status_label, FONT_DEFAULT, Font.BOLD);
        updateFont(remtime_label, FONT_DEFAULT, Font.PLAIN);
        updateFont(speed_label, FONT_DEFAULT, Font.BOLD);
        updateFont(progress_pbar, FONT_DEFAULT, Font.PLAIN);
        updateFont(slots_label, FONT_DEFAULT, Font.BOLD);
        updateFont(slots_spinner, FONT_DEFAULT, Font.PLAIN);
        updateFont(pause_button, FONT_DEFAULT, Font.BOLD);
        updateFont(stop_button, FONT_DEFAULT, Font.BOLD);
        updateFont(keep_temp_checkbox, FONT_DEFAULT, Font.PLAIN);
        updateFont(file_name_label, FONT_DEFAULT, Font.PLAIN);
        updateFont(file_size_label, FONT_DEFAULT, Font.BOLD);
        updateFont(close_button, FONT_DEFAULT, Font.PLAIN);
        updateFont(copy_link_button, FONT_DEFAULT, Font.PLAIN);
        updateFont(restart_button, FONT_DEFAULT, Font.PLAIN);
        updateFont(slot_status_label, FONT_DEFAULT, Font.PLAIN);
        
        swingReflectionInvokeAndWait("setModel", slots_spinner, new SpinnerNumberModel(_download.getMain_panel().getDefault_slots_down(), Download.MIN_WORKERS, Download.MAX_WORKERS, 1));
        swingReflectionInvoke("setEditable", swingReflectionInvokeAndWaitForReturn("getTextField", swingReflectionInvokeAndWaitForReturn("getEditor", slots_spinner)), false);
        swingReflectionInvoke("setVisible", slots_spinner, false);
        swingReflectionInvoke("setVisible", slots_label, false);
        swingReflectionInvoke("setVisible", pause_button, false);
        swingReflectionInvoke("setVisible", stop_button, false);
        swingReflectionInvoke("setForeground", speed_label, new Color(0,128,255));
        swingReflectionInvoke("setVisible", speed_label, false);
        swingReflectionInvoke("setVisible", remtime_label, false);
        swingReflectionInvoke("setVisible", progress_pbar, false);
        swingReflectionInvoke("setVisible", keep_temp_checkbox, false);
        swingReflectionInvoke("setVisible", file_name_label, false);
        swingReflectionInvoke("setVisible", close_button, false);
        swingReflectionInvoke("setVisible", copy_link_button, false);
        swingReflectionInvoke("setVisible", restart_button, false);
        swingReflectionInvoke("setVisible", file_size_label, false);
        
    }
    
    public void hideAllExceptStatus()
    {
        swingReflectionInvoke("setVisible", speed_label, false);
        swingReflectionInvoke("setVisible", remtime_label, false);
        swingReflectionInvoke("setVisible", slots_spinner, false);
        swingReflectionInvoke("setVisible", slots_label, false);
        swingReflectionInvoke("setVisible", slot_status_label, false);
        swingReflectionInvoke("setVisible", pause_button, false);
        swingReflectionInvoke("setVisible", stop_button, false);
        swingReflectionInvoke("setVisible", progress_pbar, false);
        swingReflectionInvoke("setVisible", keep_temp_checkbox, false);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        status_label = new javax.swing.JLabel();
        slots_label = new javax.swing.JLabel();
        slots_spinner = new javax.swing.JSpinner();
        remtime_label = new javax.swing.JLabel();
        speed_label = new javax.swing.JLabel();
        progress_pbar = new javax.swing.JProgressBar();
        pause_button = new javax.swing.JButton();
        stop_button = new javax.swing.JButton();
        keep_temp_checkbox = new javax.swing.JCheckBox();
        file_name_label = new javax.swing.JLabel();
        close_button = new javax.swing.JButton();
        copy_link_button = new javax.swing.JButton();
        restart_button = new javax.swing.JButton();
        file_size_label = new javax.swing.JLabel();
        slot_status_label = new javax.swing.JLabel();

        setBorder(new javax.swing.border.LineBorder(new java.awt.Color(153, 204, 255), 4, true));

        status_label.setFont(new java.awt.Font("Dialog", 1, 20)); // NOI18N
        status_label.setText("status");
        status_label.setDoubleBuffered(true);

        slots_label.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        slots_label.setText("Slots");
        slots_label.setDoubleBuffered(true);

        slots_spinner.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        slots_spinner.setToolTipText("Slots");
        slots_spinner.setDoubleBuffered(true);
        slots_spinner.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                slots_spinnerStateChanged(evt);
            }
        });

        remtime_label.setFont(new java.awt.Font("Verdana", 1, 18)); // NOI18N
        remtime_label.setText("remaining_time");
        remtime_label.setDoubleBuffered(true);

        speed_label.setFont(new java.awt.Font("Verdana", 3, 26)); // NOI18N
        speed_label.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        speed_label.setText("speed");
        speed_label.setDoubleBuffered(true);

        progress_pbar.setFont(new java.awt.Font("Verdana", 1, 18)); // NOI18N
        progress_pbar.setDoubleBuffered(true);

        pause_button.setBackground(new java.awt.Color(255, 153, 0));
        pause_button.setFont(new java.awt.Font("Verdana", 1, 16)); // NOI18N
        pause_button.setForeground(java.awt.Color.white);
        pause_button.setText("PAUSE DOWNLOAD");
        pause_button.setDoubleBuffered(true);
        pause_button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                pause_buttonActionPerformed(evt);
            }
        });

        stop_button.setBackground(new java.awt.Color(255, 0, 0));
        stop_button.setFont(new java.awt.Font("Dialog", 1, 16)); // NOI18N
        stop_button.setForeground(java.awt.Color.white);
        stop_button.setText("CANCEL DOWNLOAD");
        stop_button.setDoubleBuffered(true);
        stop_button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                stop_buttonActionPerformed(evt);
            }
        });

        keep_temp_checkbox.setFont(new java.awt.Font("Dialog", 1, 16)); // NOI18N
        keep_temp_checkbox.setSelected(true);
        keep_temp_checkbox.setText("Keep temp file");
        keep_temp_checkbox.setDoubleBuffered(true);

        file_name_label.setFont(new java.awt.Font("Dialog", 1, 20)); // NOI18N
        file_name_label.setForeground(new java.awt.Color(51, 51, 255));
        file_name_label.setText("file_name");
        file_name_label.setDoubleBuffered(true);

        close_button.setFont(new java.awt.Font("Dialog", 1, 16)); // NOI18N
        close_button.setText("Close");
        close_button.setDoubleBuffered(true);
        close_button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                close_buttonActionPerformed(evt);
            }
        });

        copy_link_button.setFont(new java.awt.Font("Dialog", 1, 16)); // NOI18N
        copy_link_button.setText("Copy link");
        copy_link_button.setDoubleBuffered(true);
        copy_link_button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                copy_link_buttonActionPerformed(evt);
            }
        });

        restart_button.setBackground(new java.awt.Color(51, 51, 255));
        restart_button.setFont(new java.awt.Font("Dialog", 1, 16)); // NOI18N
        restart_button.setForeground(new java.awt.Color(255, 255, 255));
        restart_button.setText("Restart");
        restart_button.setDoubleBuffered(true);
        restart_button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                restart_buttonActionPerformed(evt);
            }
        });

        file_size_label.setFont(new java.awt.Font("Dialog", 1, 20)); // NOI18N
        file_size_label.setForeground(new java.awt.Color(51, 51, 255));
        file_size_label.setText("file_size");
        file_size_label.setDoubleBuffered(true);

        slot_status_label.setFont(new java.awt.Font("Dialog", 1, 14)); // NOI18N
        slot_status_label.setDoubleBuffered(true);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(close_button)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(restart_button)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(keep_temp_checkbox)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(stop_button))
                    .addComponent(progress_pbar, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(speed_label, javax.swing.GroupLayout.PREFERRED_SIZE, 340, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 73, Short.MAX_VALUE)
                        .addComponent(pause_button))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(status_label, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(slots_label)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(slots_spinner, javax.swing.GroupLayout.PREFERRED_SIZE, 49, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(file_name_label)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(copy_link_button, javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(slot_status_label, javax.swing.GroupLayout.Alignment.TRAILING)))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(remtime_label)
                            .addComponent(file_size_label))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(slots_spinner, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(slots_label)
                    .addComponent(status_label))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(file_name_label)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(file_size_label))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(slot_status_label)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(copy_link_button)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(remtime_label)
                .addGap(6, 6, 6)
                .addComponent(progress_pbar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(speed_label)
                    .addComponent(pause_button))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(keep_temp_checkbox)
                    .addComponent(stop_button)
                    .addComponent(close_button)
                    .addComponent(restart_button))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void slots_spinnerStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_slots_spinnerStateChanged

        THREAD_POOL.execute(new Runnable(){
            
            @Override
            public void run() {
                
                _download.checkSlotsAndWorkers();
            }
        });
    }//GEN-LAST:event_slots_spinnerStateChanged

   
    private void close_buttonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_close_buttonActionPerformed
      
        _download.close();
    }//GEN-LAST:event_close_buttonActionPerformed

    private void copy_link_buttonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_copy_link_buttonActionPerformed
        
        copyTextToClipboard(_download.getUrl());
        
        JOptionPane.showMessageDialog(_download.getMain_panel().getView(), "Link was copied to clipboard!");
    }//GEN-LAST:event_copy_link_buttonActionPerformed

    private void restart_buttonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_restart_buttonActionPerformed
        
        _download.restart();
    }//GEN-LAST:event_restart_buttonActionPerformed

    private void stop_buttonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_stop_buttonActionPerformed
        
        _download.stop();
    }//GEN-LAST:event_stop_buttonActionPerformed

    private void pause_buttonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pause_buttonActionPerformed
        
        _download.pause();
    }//GEN-LAST:event_pause_buttonActionPerformed

    @Override
    public void pause() {

        printStatusNormal("Pausing download ...");
        
        swingReflectionInvoke("setEnabled", pause_button, false);
        swingReflectionInvoke("setEnabled", speed_label, false);
        swingReflectionInvoke("setEnabled", slots_label, false);
        swingReflectionInvoke("setEnabled", slots_spinner, false);
        swingReflectionInvoke("setVisible", stop_button, true);
        swingReflectionInvoke("setVisible", keep_temp_checkbox, true);
    }
    
    @Override
    public void resume() {
     
        printStatusNormal("Downloading file from mega ...");
        
        swingReflectionInvoke("setEnabled", pause_button, false);
        swingReflectionInvoke("setEnabled", speed_label, true);
        swingReflectionInvoke("setEnabled", slots_label, true);
        swingReflectionInvoke("setEnabled", slots_spinner, true);
        swingReflectionInvoke("setVisible", stop_button, false);
        swingReflectionInvoke("setVisible", keep_temp_checkbox, false);
        swingReflectionInvoke("setEnabled", pause_button, true);
        swingReflectionInvoke("setText", pause_button, "PAUSE DOWNLOAD");
        swingReflectionInvoke("setVisible", _download.getMain_panel().getView().getPause_all_down_button(), true);
        
    }
    
    @Override
    public void stop() {
        
        printStatusNormal("Stopping download safely, please wait...");
                
        swingReflectionInvoke("setEnabled", speed_label, false);
        swingReflectionInvoke("setEnabled", pause_button, false);
        swingReflectionInvoke("setEnabled", stop_button, false);
        swingReflectionInvoke("setEnabled", keep_temp_checkbox, false);
        swingReflectionInvoke("setEnabled", slots_label, false);
        swingReflectionInvoke("setEnabled", slots_spinner, false);
    }

    
    @Override
    public void updateSpeed(String speed, Boolean visible) {
        
        if(speed != null) {
            
            swingReflectionInvoke("setText", speed_label, speed);
        }
        
        if(visible != null) {
            
            swingReflectionInvoke("setVisible", speed_label, visible);
        }
    }

    @Override
    public void updateRemainingTime(String rem_time, Boolean visible) {
        
        if(speed_label != null) {
            
            swingReflectionInvoke("setText", remtime_label, rem_time);
        }
        
        if(visible != null) {
            
            swingReflectionInvoke("setVisible", remtime_label, visible);
        }
    }

    @Override
    public void updateProgressBar(long progress, double bar_rate) {
        
        swingReflectionInvoke("setValue", progress_pbar, (int)Math.ceil(bar_rate*progress));
        
    }
    
    @Override
    public void printStatusError(String message)
    {
        swingReflectionInvoke("setForeground", status_label, Color.red);
        swingReflectionInvoke("setText", status_label, message);
    }
    
    @Override
    public void printStatusOK(String message)
    {        
        swingReflectionInvoke("setForeground", status_label, new Color(0,128,0));
        swingReflectionInvoke("setText", status_label, message);
    }
    
    @Override
    public void printStatusNormal(String message)
    {
        swingReflectionInvoke("setForeground", status_label, Color.BLACK);
        swingReflectionInvoke("setText", status_label, message);
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton close_button;
    private javax.swing.JButton copy_link_button;
    private javax.swing.JLabel file_name_label;
    private javax.swing.JLabel file_size_label;
    private javax.swing.JCheckBox keep_temp_checkbox;
    private javax.swing.JButton pause_button;
    private javax.swing.JProgressBar progress_pbar;
    private javax.swing.JLabel remtime_label;
    private javax.swing.JButton restart_button;
    private javax.swing.JLabel slot_status_label;
    private javax.swing.JLabel slots_label;
    private javax.swing.JSpinner slots_spinner;
    private javax.swing.JLabel speed_label;
    private javax.swing.JLabel status_label;
    private javax.swing.JButton stop_button;
    // End of variables declaration//GEN-END:variables

}
