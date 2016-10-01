package megabasterd;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import static java.util.logging.Logger.getLogger;

/**
 *
 * @author tonikelope
 */
public final class StreamThrottlerSupervisor implements Runnable, SecureNotifiable {

    private final ConcurrentLinkedQueue<Integer> _input_slice_queue;
    
    private final ConcurrentLinkedQueue<Integer> _output_slice_queue;
    
    private final int _slice_size;
    
    private volatile int _maxBytesPerSecInput;
    
    private volatile int _maxBytesPerSecOutput;
    
    private final Object _secure_notify_lock;
    
    private boolean _notified;
    
    public StreamThrottlerSupervisor(int maxBytesPerSecInput, int maxBytesPerSecOutput, int slice_size) {
        _notified = false;
        
        _secure_notify_lock = new Object();
        
        _maxBytesPerSecInput = maxBytesPerSecInput;
        
        _maxBytesPerSecOutput = maxBytesPerSecOutput;
        
        _slice_size = slice_size;
        
        _input_slice_queue = new ConcurrentLinkedQueue<>();
        
        _output_slice_queue = new ConcurrentLinkedQueue<>();
    }

    public int getMaxBytesPerSecInput() {
        return _maxBytesPerSecInput;
    }

    public void setMaxBytesPerSecInput(int maxBytesPerSecInput) {
        _maxBytesPerSecInput = maxBytesPerSecInput;
    }

    public int getMaxBytesPerSecOutput() {
        return _maxBytesPerSecOutput;
    }

    public void setMaxBytesPerSecOutput(int maxBytesPerSecOutput) {
        _maxBytesPerSecOutput = maxBytesPerSecOutput;
    }

    public ConcurrentLinkedQueue<Integer> getInput_slice_queue() {
        return _input_slice_queue;
    }

    public ConcurrentLinkedQueue<Integer> getOutput_slice_queue() {
        return _output_slice_queue;
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
                    getLogger(StreamThrottlerSupervisor.class.getName()).log(Level.SEVERE, null, ex);
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

    @Override
    public void run() {
        
        while(true) {

            if(_maxBytesPerSecInput > 0) {
                
                _input_slice_queue.clear();
                
                int slice_num = (int)Math.floor((double)_maxBytesPerSecInput / _slice_size);
                
                for(int i=0; i<slice_num; i++)
                {
                    _input_slice_queue.add(_slice_size);
                }
                
                if(_maxBytesPerSecInput % _slice_size != 0) {
                    
                    _input_slice_queue.add(_maxBytesPerSecInput % _slice_size);
                }
            } 
        
            if(_maxBytesPerSecOutput > 0) {
                
                _output_slice_queue.clear();
                
                int slice_num = (int)Math.floor((double)_maxBytesPerSecOutput / _slice_size);
                
                for(int i=0; i<slice_num; i++)
                {
                    _output_slice_queue.add(_slice_size);
                }
                
                if(_maxBytesPerSecOutput % _slice_size != 0) {
                    
                    _output_slice_queue.add(_maxBytesPerSecOutput % _slice_size);
                }
            } 
            
            secureNotifyAll();
            
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                getLogger(StreamThrottlerSupervisor.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}
