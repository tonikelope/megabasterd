package megabasterd;

import java.awt.Component;
import java.io.File;
import java.sql.SQLException;
import static java.util.logging.Level.SEVERE;
import static java.util.logging.Logger.getLogger;
import static megabasterd.DBTools.deleteUpload;
import static megabasterd.MiscTools.HashString;



/**
 *
 * @author tonikelope
 */
public final class UploadManager extends TransferenceManager {
    
    public UploadManager(MainPanel main_panel) {
        
        super(main_panel, main_panel.getMax_ul(), main_panel.getView().getStatus_up_label(), main_panel.getView().getjPanel_scroll_up(), main_panel.getView().getClose_all_finished_up_button(), main_panel.getView().getPause_all_up_button(), main_panel.getView().getClean_all_up_menu());

    }
    
    @Override
    public void provision(Transference upload) 
    {      
        getScroll_panel().add(((Upload)upload).getView());

        ((Upload)upload).provisionIt();
      
        if(((Upload)upload).isProvision_ok()) {

            getTransference_waitstart_queue().add(upload);

            if(getTransference_provision_queue().isEmpty()) {

                sortTransferenceStartQueue();

                for(Transference up:getTransference_waitstart_queue()) {

                    getScroll_panel().remove((Component)up.getView());
                    getScroll_panel().add((Component)up.getView());
                }

                for(Transference up:getTransference_finished_queue()) {

                    getScroll_panel().remove((Component)up.getView());
                    getScroll_panel().add((Component)up.getView());
                }
            } 
        } else {
            
            getTransference_finished_queue().add(upload);
        }

        secureNotify();
    }
    
    
    @Override
    public void remove(Transference upload) {
        
        getScroll_panel().remove(((Upload)upload).getView());
        
        getTransference_waitstart_queue().remove(upload);

        getTransference_running_list().remove(upload);

        getTransference_finished_queue().remove(upload);

        if(((Upload)upload).isProvision_ok()) {

            try {
                deleteUpload(upload.getFile_name(), ((Upload)upload).getMa().getEmail());
            } catch (SQLException ex) {
                getLogger(UploadManager.class.getName()).log(SEVERE, null, ex);
            }

            try {

                File temp_file = new File("."+HashString("SHA-1", upload.getFile_name()));

                if(temp_file.exists()) {

                    temp_file.delete();
                }

            } catch (Exception ex) {
                getLogger(UploadManager.class.getName()).log(SEVERE, null, ex);
            }
        }

        secureNotify();
    }
    
}