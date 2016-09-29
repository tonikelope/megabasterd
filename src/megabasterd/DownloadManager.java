package megabasterd;

import java.awt.Component;
import static java.lang.System.out;
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
        
        if(!getTransference_remove_queue().isEmpty()) {

            swingReflectionInvoke("setText", getMain_panel().getView().getStatus_down_label(), "Removing "+getTransference_remove_queue().size()+" downloads, please wait...");

        } else {

            swingReflectionInvoke("setText", getMain_panel().getView().getStatus_down_label(), "");
        }
    }
    
    public void provision(Download download, boolean retry) throws MegaAPIException, MegaCrypterAPIException 
    {            
        getScroll_panel().add(download.getView());

        download.provisionIt(retry);

        if(download.isProvision_ok()) {
            
            out.println("Provision OK!");

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
            
            out.println("Provision error!");
            
            getTransference_finished_queue().add(download);
        }
        
        if(getTransference_provision_queue().isEmpty()) {
        
            swingReflectionInvoke("setText", getMain_panel().getView().getStatus_down_label(), "");

        } else {

            swingReflectionInvoke("setText", getMain_panel().getView().getStatus_down_label(), getTransference_provision_queue().size() + " downloads waiting for provision...");
        }
        
        if(retry) {
            
            secureNotify();
        }
    }
    
    
    @Override
    public void run() {
        
        out.println("Download manager hello!");
        
        while(true)
        {
            if(!getTransference_provision_queue().isEmpty())
            {
                swingReflectionInvoke("setEnabled", getMain_panel().getView().getNew_download_menu(), false);
                
                swingReflectionInvoke("setText", getMain_panel().getView().getStatus_down_label(), getTransference_provision_queue().size() + " downloads waiting for provision...");
                
                while(!getTransference_provision_queue().isEmpty())
                {
                    final Download download = (Download)getTransference_provision_queue().poll();
                    
                    if(download != null) {
                        
                        try{
                            
                            provision(download, false);
                            
                        }catch (MegaAPIException | MegaCrypterAPIException ex) {
                            
                            out.println("Provision failed! Retrying in separated thread...");
                            
                            getScroll_panel().remove(download.getView());
                            
                            final DownloadManager main = this;
                            
                            THREAD_POOL.execute(new Runnable(){
                                @Override
                                public void run(){
                                    
                                    try {
                                        
                                        main.provision(download, true);
                                        
                                    } catch (MegaAPIException | MegaCrypterAPIException ex1) {
                                        
                                        getLogger(DownloadManager.class.getName()).log(SEVERE, null, ex1);
                                    }
                                    
                                }});
                        }
                    }
                }
            }
            
            if(!getTransference_remove_queue().isEmpty()) {
                
                swingReflectionInvoke("setEnabled", getMain_panel().getView().getNew_download_menu(), false);
                
                swingReflectionInvoke("setText", getMain_panel().getView().getStatus_down_label(), "Removing "+getTransference_remove_queue().size()+" downloads, please wait...");
                
                while(!getTransference_remove_queue().isEmpty()) {
                    
                    Download download = (Download)getTransference_remove_queue().poll();
                    
                    if(download != null) {
                        
                        remove(download);
                        
                    }
                }
            }
            
            while(!getTransference_start_queue().isEmpty() && getTransference_running_list().size() < getMain_panel().getMax_dl()) {
                
                Download download = (Download)getTransference_start_queue().poll();
                
                if(download != null) {
   
                    start(download);
                }
            }
            
            checkButtonsAndMenus(getMain_panel().getView().getClose_all_finished_down_button(), getMain_panel().getView().getPause_all_down_button(), getMain_panel().getView().getNew_download_menu(), getMain_panel().getView().getClean_all_down_menu());
            
            out.println("Download manager wait");
            
            secureWait();
            
            out.println("Download manager let's go");
        }
        
        }
    
}
