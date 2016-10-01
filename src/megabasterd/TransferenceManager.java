package megabasterd;

import java.awt.Component;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import static java.util.logging.Logger.getLogger;
import javax.swing.JPanel;
import static megabasterd.MiscTools.swingReflectionInvoke;

/**
 *
 * @author tonikelope
 */
abstract public class TransferenceManager implements Runnable, SecureNotifiable {
    
    private final ConcurrentLinkedQueue<Transference> _transference_provision_queue;
    private final ConcurrentLinkedQueue<Transference> _transference_waitstart_queue;
    private final ConcurrentLinkedQueue<Transference> _transference_remove_queue;
    private final ConcurrentLinkedQueue<Transference> _transference_finished_queue;
    private final ConcurrentLinkedQueue<Transference> _transference_running_list;
    private final javax.swing.JPanel _scroll_panel;
    private final MainPanel _main_panel;
    private final Object _secure_notify_lock;
    private boolean _notified;
    private volatile boolean _removing_transferences;
    private volatile boolean _provisioning_transferences;
    private volatile boolean _starting_transferences;

    public boolean isRemoving_transferences() {
        return _removing_transferences;
    }

    public void setRemoving_transferences(boolean removing) {
        _removing_transferences = removing;
    }

    public boolean isProvisioning_transferences() {
        return _provisioning_transferences;
    }

    public void setProvisioning_transferences(boolean provisioning) {
        _provisioning_transferences = provisioning;
    }

    public boolean isStarting_transferences() {
        return _starting_transferences;
    }

    public void setStarting_transferences(boolean starting) {
        _starting_transferences = starting;
    }
 
    public TransferenceManager(MainPanel main_panel, javax.swing.JPanel scroll_panel) {
        _notified = false;
        _removing_transferences = false;
        _provisioning_transferences = false;
        _starting_transferences=false;
        _main_panel = main_panel;
        _scroll_panel = scroll_panel;
        _secure_notify_lock = new Object();
        _transference_waitstart_queue = new ConcurrentLinkedQueue();
        _transference_provision_queue = new ConcurrentLinkedQueue();
        _transference_remove_queue = new ConcurrentLinkedQueue();
        _transference_finished_queue = new ConcurrentLinkedQueue();
        _transference_running_list = new ConcurrentLinkedQueue();
    }
    
    @Override
    public void secureNotify()
    {
        synchronized(_secure_notify_lock) {
      
            _notified = true;

            _secure_notify_lock.notify();
        }
    }
    
    @Override
    public void secureWait() {
        
        synchronized(_secure_notify_lock)
        {
            while(!_notified) {

                try {
                    _secure_notify_lock.wait();
                } catch (InterruptedException ex) {
                    getLogger(TransferenceManager.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

            _notified = false;
        }
    }
    
    @Override
    public void secureNotifyAll() {
        
        synchronized(_secure_notify_lock) {
            
            _notified = true;
      
            _secure_notify_lock.notifyAll();
        }
    }

    public MainPanel getMain_panel() {
        return _main_panel;
    }
    
    public ConcurrentLinkedQueue<Transference> getTransference_provision_queue() {
        return _transference_provision_queue;
    }

    public ConcurrentLinkedQueue<Transference> getTransference_start_queue() {
        return _transference_waitstart_queue;
    }

    public ConcurrentLinkedQueue<Transference> getTransference_remove_queue() {
        return _transference_remove_queue;
    }

    public ConcurrentLinkedQueue<Transference> getTransference_finished_queue() {
        return _transference_finished_queue;
    }

    public ConcurrentLinkedQueue<Transference> getTransference_running_list() {
        return _transference_running_list;
    }

    public JPanel getScroll_panel() {
        return _scroll_panel;
    }
    
    public void closeAllFinished() 
    {
        _transference_remove_queue.addAll(new ArrayList(_transference_finished_queue));
        
        _transference_finished_queue.clear();
        
        secureNotify();
    }
    
    public void closeAllWaiting() 
    {   
        _transference_remove_queue.addAll(new ArrayList(_transference_waitstart_queue));
        
        _transference_waitstart_queue.clear();
        
        secureNotify();
    }
    
    public void start(Transference transference) {
        
        _transference_running_list.add(transference);

        _scroll_panel.add((Component)transference.getView(), 0);
        
        transference.start();
    }
    
    public void pauseAll()
    {
        for(Transference transference:_transference_running_list) {
            
            if(!transference.isPaused()) {
                
                transference.pause();
            }
        }
      
        secureNotify();
    }
    
    public void sortTransferenceStartQueue() 
    {
        ArrayList<Transference> trans_list = new ArrayList(_transference_waitstart_queue);

        trans_list.sort(new Comparator<Transference> () {

        @Override
        public int compare(Transference o1, Transference o2) {
      
            return o1.getFile_name().compareToIgnoreCase(o2.getFile_name());
        }
        });
        
        _transference_waitstart_queue.clear();
        
        _transference_waitstart_queue.addAll(trans_list);
    }
    
    public void checkButtonsAndMenus(javax.swing.JButton close_all_finished_button, javax.swing.JButton pause_all_button, 
            javax.swing.MenuElement clean_all_waiting_trans_menu) {
        
        if(!_transference_running_list.isEmpty()) {
            
            boolean show_pause_all = false;
            
            for(Transference trans:_transference_running_list) { 
                
                if((show_pause_all = !trans.isPaused())) {
                    
                    break;
                }
            }
            
            swingReflectionInvoke("setVisible", pause_all_button, show_pause_all);
            
        } else {
            
            swingReflectionInvoke("setVisible", pause_all_button, false);
        }
        
          
        swingReflectionInvoke("setEnabled", clean_all_waiting_trans_menu, !_transference_waitstart_queue.isEmpty());
 
        if(!_transference_finished_queue.isEmpty()) {

            swingReflectionInvoke("setText", close_all_finished_button, "Close all finished ("+_transference_finished_queue.size()+")" );
            
            swingReflectionInvoke("setVisible", close_all_finished_button, true);
            
        } else {
            
            swingReflectionInvoke("setVisible", close_all_finished_button, false);
        }
    }
    
    public String getStatus() {
        
        int prov = _transference_provision_queue.size();
        
        int rem = _transference_remove_queue.size();
        
        int wait = _transference_waitstart_queue.size();
        
        int run = _transference_running_list.size();
        
        int finish = _transference_finished_queue.size();

        return (prov+rem+wait+run+finish > 0)?"Prov("+prov+") / Rem("+rem+") / Wait("+wait+") / Run("+run+") / Finish("+finish+")":"";
    }
    
}
