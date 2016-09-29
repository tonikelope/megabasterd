package megabasterd;

import java.awt.Component;
import java.io.File;
import java.sql.SQLException;
import static java.util.logging.Level.SEVERE;
import static java.util.logging.Logger.getLogger;
import static megabasterd.DBTools.deleteUpload;
import static megabasterd.MiscTools.HashString;
import static megabasterd.MiscTools.swingReflectionInvoke;



/**
 *
 * @author tonikelope
 */
public final class UploadManager extends TransferenceManager {
    
    public UploadManager(MainPanel main_panel) {
        
        super(main_panel, main_panel.getView().jPanel_scroll_up);
    }
    
    public void provision(Upload upload) 
    {      
        getScroll_panel().add(upload.getView());

        upload.provisionIt();
      
        if(upload.isProvision_ok()) {

            getTransference_start_queue().add(upload);

            if(getTransference_provision_queue().isEmpty()) {

                sortTransferenceStartQueue();

                for(Transference up:getTransference_start_queue()) {

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
        
        if(getTransference_provision_queue().isEmpty()) {
        
            swingReflectionInvoke("setText", getMain_panel().getView().getStatus_up_label(), "");

        } else {

            swingReflectionInvoke("setText", getMain_panel().getView().getStatus_up_label(), getTransference_provision_queue().size() + " uploads waiting for provision...");
        }
        
    }
    
    
    public void remove(Upload upload) {
        
        getScroll_panel().remove(upload.getView());
        
        getTransference_start_queue().remove(upload);

        getTransference_running_list().remove(upload);

        getTransference_finished_queue().remove(upload);

        if(upload.isProvision_ok()) {

            try {
                deleteUpload(upload.getFile_name(), upload.getMa().getEmail());
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

        if(!getTransference_remove_queue().isEmpty()) {

            swingReflectionInvoke("setText", upload.getMain_panel().getView().getStatus_up_label(), "Removing "+getTransference_remove_queue().size()+" uploads, please wait...");

        } else {

            swingReflectionInvoke("setText", upload.getMain_panel().getView().getStatus_up_label(), "");
        }
    }
    
    
    @Override
    public void run() {
        
        while(true)
        {
            if(!getTransference_provision_queue().isEmpty())
            {
                swingReflectionInvoke("setEnabled", getMain_panel().getView().getNew_upload_menu(), false);
                
                swingReflectionInvoke("setText", getMain_panel().getView().getStatus_up_label(), getTransference_provision_queue().size() + " uploads waiting for provision...");
                
                while(!getTransference_provision_queue().isEmpty())
                {
                    Upload upload = (Upload)getTransference_provision_queue().poll();
                    
                    if(upload != null) {
                        
                        provision(upload);
                    }
                }
            }
            
            if(!getTransference_remove_queue().isEmpty()){
                
                swingReflectionInvoke("setEnabled", getMain_panel().getView().getNew_upload_menu(), false);
                
                swingReflectionInvoke("setText", getMain_panel().getView().getStatus_up_label(), "Removing "+getTransference_remove_queue().size()+" uploads, please wait...");
                
                while(!getTransference_remove_queue().isEmpty()) {
                    
                    Upload upload = (Upload)getTransference_remove_queue().poll();
                    
                    if(upload != null) {
                        
                        remove(upload);
                    }
                }
            }
            
            while(!getTransference_start_queue().isEmpty() && getTransference_running_list().size() < getMain_panel().getMax_ul()) {
                
                Upload upload = (Upload)getTransference_start_queue().poll();
                
                if(upload != null) {
                    
                    start(upload);
                }
            }
            
            checkButtonsAndMenus(getMain_panel().getView().getClose_all_finished_up(), getMain_panel().getView().getPause_all_up(), getMain_panel().getView().getNew_upload_menu(), getMain_panel().getView().getClean_all_up_menu());

            secureWait();
        }
        
        }
    
    
}