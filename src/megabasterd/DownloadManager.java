package megabasterd;

import java.awt.Component;
import java.sql.SQLException;
import static java.util.logging.Level.SEVERE;
import static java.util.logging.Logger.getLogger;
import static megabasterd.DBTools.deleteDownload;
import static megabasterd.MainPanel.THREAD_POOL;
import static megabasterd.MiscTools.swingReflectionInvoke;


public final class DownloadManager extends TransferenceManager {

    public DownloadManager(MainPanel main_panel) {
        
        super(main_panel, main_panel.getView().getjPanel_scroll_down());
    }

    public void remove(Download download) {
        
        getScroll_panel().remove(download.getView());
        
        getTransference_start_queue().remove(download);

        getTransference_running_list().remove(download);

        getTransference_finished_queue().remove(download);

        if(download.isProvision_ok()) {

            try {
                deleteDownload(download.getUrl());
            } catch (SQLException ex) {
                getLogger(DownloadManager.class.getName()).log(SEVERE, null, ex);
            }
        }
   
        secureNotify();
    }
    
    public void provision(Download download, boolean retry) throws MegaAPIException, MegaCrypterAPIException 
    {            
        getScroll_panel().add(download.getView());

        download.provisionIt(retry);

        if(download.isProvision_ok()) {

            getTransference_start_queue().add(download);

            if(getTransference_provision_queue().isEmpty()) {

                sortTransferenceStartQueue();

                for(Transference down:getTransference_start_queue()) {

                    getScroll_panel().remove((Component)down.getView());
                    getScroll_panel().add((Component)down.getView());
                }

                for(Transference down:getTransference_finished_queue()) {

                    getScroll_panel().remove((Component)down.getView());
                    getScroll_panel().add((Component)down.getView());
                }
            } 
        } else {

            getTransference_finished_queue().add(download);
        }
        
        
        secureNotify();
        
    }
    
    
    @Override
    public void run() {
        
        final DownloadManager tthis = this;

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
                                        final Download download = (Download)getTransference_provision_queue().poll();

                                        if(download != null) {

                                            try{

                                                provision(download, false);

                                            }catch (MegaAPIException | MegaCrypterAPIException ex) {

                                                System.out.println("Provision failed! Retrying in separated thread...");

                                                getScroll_panel().remove(download.getView());

                                                THREAD_POOL.execute(new Runnable(){
                                                    @Override
                                                    public void run(){

                                                        try {

                                                            tthis.provision(download, true);

                                                        } catch (MegaAPIException | MegaCrypterAPIException ex1) {

                                                            getLogger(DownloadManager.class.getName()).log(SEVERE, null, ex1);
                                                        }

                                                    }});
                                                }
                                            }
                                        }
                                    
                                    tthis.setProvisioning_transferences(false);
                                    
                                    tthis.secureNotify();
             
                                }});

            }
            
            if(!isRemoving_transferences() && !getTransference_remove_queue().isEmpty()) {
                
                setRemoving_transferences(true);
                
                THREAD_POOL.execute(new Runnable(){
                                @Override
                                public void run(){
                                
                                   
                                    while(!getTransference_remove_queue().isEmpty()) {

                                        Download download = (Download)getTransference_remove_queue().poll();

                                        if(download != null) {
                                            remove(download);
                                        }
                                    }
                                    
                                    tthis.setRemoving_transferences(false);
                                    
                                    tthis.secureNotify();
                                    
                                }});
            }
            
            if(!isStarting_transferences() && !getTransference_start_queue().isEmpty() && getTransference_running_list().size() < getMain_panel().getMax_dl())
            {
                setStarting_transferences(true);
                
                THREAD_POOL.execute(new Runnable(){
                                @Override
                                public void run(){
                                
                                while(!getTransference_start_queue().isEmpty() && getTransference_running_list().size() < getMain_panel().getMax_dl()) {
                
                                    Download download = (Download)getTransference_start_queue().poll();

                                    if(download != null) {

                                        start(download);
                                    }
                                }
                                
                                tthis.setStarting_transferences(false);
                                    
                                tthis.secureNotify();
                                
                           }});
            }

            secureWait();
            
            checkButtonsAndMenus(getMain_panel().getView().getClose_all_finished_down_button(), getMain_panel().getView().getPause_all_down_button(), getMain_panel().getView().getClean_all_down_menu());
            
            if(!this.getMain_panel().getView().isPre_processing_downloads()) {
                swingReflectionInvoke("setText", getMain_panel().getView().getStatus_down_label(), getStatus());
            }
        }
        
        }
    
}
