package megabasterd;

import java.awt.Component;
import java.io.File;
import java.sql.SQLException;
import static java.util.logging.Level.SEVERE;
import static java.util.logging.Logger.getLogger;
import static megabasterd.DBTools.deleteUpload;
import static megabasterd.MainPanel.THREAD_POOL;
import static megabasterd.MiscTools.HashString;
import static megabasterd.MiscTools.swingReflectionInvoke;



/**
 *
 * @author tonikelope
 */
public final class UploadManager extends TransferenceManager {
    
    public UploadManager(MainPanel main_panel) {
        
        super(main_panel, main_panel.getView().getjPanel_scroll_up());
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

        secureNotify();
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

        secureNotify();
    }
    
    
    @Override
    public void run() {
        
        final UploadManager tthis = this;
        
        while(true)
        {
            if(!isProvisioning_transferences() && !getTransference_provision_queue().isEmpty())
            {
                setProvisioning_transferences(true);
                
                THREAD_POOL.execute(new Runnable(){
                                @Override
                                public void run(){
        
                                while(!getTransference_provision_queue().isEmpty())
                                {
                                    Upload upload = (Upload)getTransference_provision_queue().poll();

                                    if(upload != null) {

                                        provision(upload);
                                    }
                                }
                                
                                tthis.setProvisioning_transferences(false);
                                    
                                tthis.secureNotify();
                                
                                
                                }});
                
                
                
            }
            
            if(!isRemoving_transferences() && !getTransference_remove_queue().isEmpty()){
                
                setRemoving_transferences(true);
                
                THREAD_POOL.execute(new Runnable(){
                                @Override
                                public void run(){

                            while(!getTransference_remove_queue().isEmpty()) {

                                Upload upload = (Upload)getTransference_remove_queue().poll();

                                if(upload != null) {

                                    remove(upload);
                                }
                            }
                            
                            tthis.setRemoving_transferences(false);
                                    
                            tthis.secureNotify();
                
                 }});
            }
            
            
            if(!isStarting_transferences() && !getTransference_start_queue().isEmpty() && getTransference_running_list().size() < getMain_panel().getMax_ul())
            {
                setStarting_transferences(true);
                
                THREAD_POOL.execute(new Runnable(){
                                @Override
                                public void run(){
                                
                                    while(!getTransference_start_queue().isEmpty() && getTransference_running_list().size() < getMain_panel().getMax_ul()) {
                                    
                                        Upload upload = (Upload)getTransference_start_queue().poll();
                
                                        if(upload != null) {

                                            start(upload);
                                        }
                                        
                                    }
                                    
                                    tthis.setStarting_transferences(false);
                                    
                                    tthis.secureNotify();
                                }
                                
                             });
                
                
            }
  
            secureWait();
            
            checkButtonsAndMenus(getMain_panel().getView().getClose_all_finished_up_button(), getMain_panel().getView().getPause_all_up(), getMain_panel().getView().getClean_all_up_menu());
            
            if(!this.getMain_panel().getView().isPre_processing_uploads()) {
                swingReflectionInvoke("setText", getMain_panel().getView().getStatus_up_label(), getStatus());
            }
        }
        
        }
    
    
}