package megabasterd;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import static java.lang.Integer.MAX_VALUE;
import static java.lang.Integer.parseInt;
import static java.lang.Long.valueOf;
import static java.lang.Math.ceil;
import static java.lang.System.out;
import static java.lang.Thread.sleep;
import java.nio.channels.FileChannel;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import static java.util.concurrent.Executors.newCachedThreadPool;
import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.logging.Level.SEVERE;
import static java.util.logging.Logger.getLogger;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import static megabasterd.CryptTools.genCrypter;
import static megabasterd.DBTools.deleteDownload;
import static megabasterd.DBTools.insertDownload;
import static megabasterd.DBTools.selectSettingValueFromDB;
import static megabasterd.MainPanel.THREAD_POOL;
import static megabasterd.MiscTools.UrlBASE642Bin;
import static megabasterd.MiscTools.bin2i32a;
import static megabasterd.MiscTools.checkMegaDownloadUrl;
import static megabasterd.MiscTools.findFirstRegex;
import static megabasterd.MiscTools.formatBytes;
import static megabasterd.MiscTools.getWaitTimeExpBackOff;
import static megabasterd.MiscTools.i32a2bin;
import static megabasterd.MiscTools.swingReflectionInvoke;
import static megabasterd.MiscTools.swingReflectionInvokeAndWait;
import static megabasterd.MiscTools.swingReflectionInvokeAndWaitForReturn;
import static megabasterd.MiscTools.truncateText;

/**
 *
 * @author tonikelope
 */
public final class Download implements Transference, Runnable, SecureNotifiable {
        
    public static final boolean VERIFY_CBC_MAC_DEFAULT=false;
    public static final boolean USE_SLOTS_DEFAULT=false;
    public static final Object CBC_LOCK=new Object();
    
    private final MainPanel _main_panel;
    private DownloadView _view=null; //lazy init
    private ProgressMeter _progress_meter=null; //lazy init
    private SpeedMeter _speed_meter=null; //lazy init
    private final Object _secure_notify_lock;
    private boolean _notified;
    private final String _url;
    private final String _download_path;
    private String _file_name;
    private String _file_key;
    private Long _file_size;
    private String _file_pass;
    private String _file_noexpire;
    private final boolean _use_slots;
    private final int _slots;
    private final boolean _restart;
    private final ArrayList<ChunkDownloader> _chunkworkers;
    private final ExecutorService _thread_pool;
    private volatile boolean _exit;
    private volatile boolean _pause;
    private final ConcurrentLinkedQueue<Integer> _partialProgressQueue;
    private volatile long _progress;
    private ChunkWriter _chunkwriter;
    private String _last_download_url;
    private boolean _provision_ok;
    private boolean _finishing_download;
    private int _paused_workers;
    private File _file;
    private boolean _checking_cbc;
    private boolean _retrying_request;
    private Double _progress_bar_rate;
    private OutputStream _output_stream;
    private String _fatal_error;
    private boolean _status_error;
    private final ConcurrentLinkedQueue<Long> _rejectedChunkIds;
    private long _last_chunk_id_dispatched;
    
    public Download(MainPanel main_panel, String url, String download_path, String file_name, String file_key, Long file_size, String file_pass, String file_noexpire, boolean use_slots, int slots, boolean restart) {
        
        _paused_workers = 0;
        _last_chunk_id_dispatched = 0L;
        _status_error = false;
        _fatal_error = null;
        _retrying_request = false;
        _checking_cbc = false;
        _finishing_download = false;
        _pause = false;
        _exit = false;
        _last_download_url = null;
        _provision_ok = true;
        _progress = 0L;
        _notified = false;
        _main_panel = main_panel;
        _url = url;
        _download_path = download_path;
        _file_name = file_name;
        _file_key = file_key;
        _file_size = file_size;
        _file_pass = file_pass;
        _file_noexpire = file_noexpire;
        _use_slots = use_slots;
        _slots = slots;
        _restart= restart;
        _secure_notify_lock = new Object();
        _chunkworkers = new ArrayList();
        _partialProgressQueue = new ConcurrentLinkedQueue();
        _rejectedChunkIds = new ConcurrentLinkedQueue();
        _thread_pool = newCachedThreadPool();
    }

    
    public boolean isChecking_cbc() {
        return _checking_cbc;
    }

    public boolean isRetrying_request() {
        return _retrying_request;
    }
    
    public boolean isExit() {
        return _exit;
    }

    public boolean isPause() {
        return _pause;
    }

    public void setExit(boolean exit) {
        _exit = exit;
    }

    public void setPause(boolean pause) {
        _pause = pause;
    }
   
    public ChunkWriter getChunkwriter() {
        return _chunkwriter;
    }

    public ConcurrentLinkedQueue<Integer> getPartialProgressQueue() {
        return _partialProgressQueue;
    }

    public void setFinishing_download(boolean finishing_download) {
        _finishing_download = finishing_download;
    }

    public boolean isFinishing_download() {
        return _finishing_download;
    }

    public String getFile_key() {
        return _file_key;
    }

    @Override
    public long getProgress() {
        return _progress;
    }

    public OutputStream getOutput_stream() {
        return _output_stream;
    }

    public File getFile() {
        return _file;
    }

    public ArrayList<ChunkDownloader> getChunkworkers() {
        return _chunkworkers;
    }

    public void setPaused_workers(int paused_workers) {
        _paused_workers = paused_workers;
    }

    public String getUrl() {
        return _url;
    }

    public String getDownload_path() {
        return _download_path;
    }

    @Override
    public String getFile_name() {
        return _file_name;
    }

    public String getFile_pass() {
        return _file_pass;
    }

    public String getFile_noexpire() {
        return _file_noexpire;
    }

    public boolean isUse_slots() {
        return _use_slots;
    }

    public int getSlots() {
        return _slots;
    }

    public void setLast_chunk_id_dispatched(long last_chunk_id_dispatched) {
        _last_chunk_id_dispatched = last_chunk_id_dispatched;
    }

    public boolean isProvision_ok() {
        return _provision_ok;
    }

    @Override
    public ProgressMeter getProgress_meter() {
        return _progress_meter == null?(_progress_meter = new ProgressMeter(this)):_progress_meter;
    }
    
    @Override
    public SpeedMeter getSpeed_meter() {
        return _speed_meter == null?(_speed_meter = new SpeedMeter(this, getMain_panel().getGlobal_dl_speed())):_speed_meter;
    }
    
    @Override
    public DownloadView getView() {
        return _view == null?(_view = new DownloadView(this)):_view;
    }

    @Override
    public MainPanel getMain_panel() {
        return _main_panel;
    }

    @Override
    public void start() {
        
        THREAD_POOL.execute(this);
    }

    @Override
    public void stop() {
        
        if(!isExit()) {
            stopDownloader();
        }
    }

    @Override
    public void pause() {
        
        if(isPause()) {

            setPause(false);
            
            setPaused_workers(0);
            
            getSpeed_meter().secureNotify();

            for(ChunkDownloader downloader:getChunkworkers()) {
                
                downloader.secureNotify();
            }
            
            getView().resume();

        } else {
            
            setPause(true);
            
            getView().pause();
        }
        
        _main_panel.getDownload_manager().secureNotify();
    }

    @Override
    public void restart() {
        
        Download new_download = new Download(getMain_panel(), getUrl(), getDownload_path(), getFile_name(), getFile_key(), getFile_size(), getFile_pass(), getFile_noexpire(), getMain_panel().isUse_slots_down(), getMain_panel().getDefault_slots_down(), true);
     
        getMain_panel().getDownload_manager().getTransference_remove_queue().add(this);
        
        getMain_panel().getDownload_manager().getTransference_provision_queue().add(new_download);
        
        getMain_panel().getDownload_manager().secureNotify();
    }

    @Override
    public boolean isPaused() {
        return isPause();
    }

    @Override
    public boolean isStopped() {
        return isExit();
    }

    @Override
    public synchronized void checkSlotsAndWorkers() {
        
        if(!isExit()) {

            int sl = (int)swingReflectionInvokeAndWaitForReturn("getValue", getView().getSlots_spinner());

            int cworkers = getChunkworkers().size();

            if(sl != cworkers) {

                if(sl > cworkers) {

                    startSlot();

                } else {

                    swingReflectionInvoke("setEnabled", getView().getSlots_spinner(), false);

                    swingReflectionInvoke("setText", getView().getSlot_status_label(), "Removing slot...");

                    stopLastStartedSlot();
                }
            }
        }
    }

    @Override
    public void close() {
        
        _main_panel.getDownload_manager().getTransference_remove_queue().add(this);
       
        _main_panel.getDownload_manager().secureNotify();
    }

    @Override
    public ConcurrentLinkedQueue<Integer> getPartialProgress() {
        return _partialProgressQueue;
    }

 
    @Override
    public long getFile_size() {
        return _file_size;
    }


    @Override
    public void run()
    {    
        swingReflectionInvoke("setVisible", getView().getClose_button(), false);

        String exit_message;
        
        getView().printStatusNormal("Starting download, please wait...");
     
        try {       
            
            if(!_exit)
            {
                String filename = _download_path+"/"+_file_name;
     
                _file = new File(filename);
                
                if(_file.getParent()!=null)
                {
                    File path = new File(_file.getParent());
                
                    path.mkdirs();
                }
                
                if(!_file.exists()) 
                {
                    getView().printStatusNormal("Starting download (retrieving MEGA temp link), please wait...");
                    
                    _last_download_url = getMegaFileDownloadUrl(_url);
                    
                    if(!_exit)
                    {
                        _retrying_request = false;
                        
                        swingReflectionInvoke("setMinimum", getView().getProgress_pbar(), 0);
                        swingReflectionInvoke("setMaximum", getView().getProgress_pbar(), MAX_VALUE);
                        swingReflectionInvoke("setStringPainted", getView().getProgress_pbar(), true);

                        _progress_bar_rate = MAX_VALUE/(double)_file_size;

                        filename = _download_path+"/"+_file_name;

                        _file = new File(filename+".mctemp");

                        if(_file.exists())
                        {
                            getView().printStatusNormal("File exists, resuming download...");

                            long max_size = calculateMaxTempFileSize(_file.length());

                            if(max_size != _file.length())
                            {                            
                                getView().printStatusNormal("Truncating temp file...");

                                try (FileChannel out_truncate = new FileOutputStream(filename+".mctemp", true).getChannel())
                                {
                                    out_truncate.truncate(max_size);
                                }
                            }

                            _progress = _file.length();
                            swingReflectionInvoke("setValue", getView().getProgress_pbar(), (int)ceil(_progress_bar_rate*_progress));
                        }
                        else
                        {
                            _progress = 0;
                            swingReflectionInvoke("setValue", getView().getProgress_pbar(), 0);
                        }

                        _output_stream = new BufferedOutputStream(new FileOutputStream(_file, (_progress > 0)));

                        _chunkwriter = new ChunkWriter(this);

                        _thread_pool.execute(_chunkwriter);
                        
                        _thread_pool.execute(getProgress_meter());
                       
                        _thread_pool.execute(getSpeed_meter());

                        getMain_panel().getGlobal_dl_speed().attachSpeedMeter(getSpeed_meter());
                        
                        getMain_panel().getGlobal_dl_speed().secureNotify();
                        
                        if(_use_slots) {
                            
                            for(int t=1; t <= _slots; t++)
                            {
                                ChunkDownloader c = new ChunkDownloader(t, this);

                                _chunkworkers.add(c);

                                _thread_pool.execute(c);
                            }
                            
                            swingReflectionInvoke("setVisible", getView().getSlots_label(), true);
                            
                            swingReflectionInvoke("setVisible", getView().getSlots_spinner(), true);
                            
                        } else {
                            
                            ChunkDownloaderMono c = new ChunkDownloaderMono(1, this);
                        
                            _chunkworkers.add(c);

                            _thread_pool.execute(c);
                        }
 
                        getView().printStatusNormal("Downloading file from mega ...");
                      
                        getMain_panel().getDownload_manager().secureNotify();
                        
                        swingReflectionInvoke("setVisible", getView().getPause_button(), true);
                        swingReflectionInvoke("setVisible", getView().getProgress_pbar(), true);

                        secureWait();
                        
                        out.println("Chunkdownloaders finished!");
                
                        getSpeed_meter().setExit(true);

                        getSpeed_meter().secureNotify();

                        getProgress_meter().setExit(true);

                        getProgress_meter().secureNotify();

                        _thread_pool.shutdown();
                        
                        while(!_thread_pool.isTerminated())
                        {
                            try {
                                
                                _thread_pool.awaitTermination(MAX_VALUE, DAYS);

                            } catch (InterruptedException ex) {
                                getLogger(Download.class.getName()).log(SEVERE, null, ex);
                            }
                        }
                        
                        out.println("Downloader thread pool finished!");
                        
                        getMain_panel().getGlobal_dl_speed().detachSpeedMeter(getSpeed_meter());
                        
                        getMain_panel().getGlobal_dl_speed().secureNotify();
                        
                        _output_stream.close();
                        
                        swingReflectionInvoke("setVisible", getView().getSpeed_label(), false);
                        swingReflectionInvoke("setVisible", getView().getRemtime_label(), false);
                        swingReflectionInvoke("setVisible", getView().getPause_button(), false);
                        swingReflectionInvoke("setVisible", getView().getStop_button(), false);
                        swingReflectionInvoke("setVisible", getView().getSlots_label(), false);
                        swingReflectionInvoke("setVisible", getView().getSlots_spinner(), false);
                        
                        getMain_panel().getDownload_manager().secureNotify();

                        if(_progress == _file_size)
                        {
                            if(_file.length() != _file_size) {
                                
                                throw new IOException("El tamaño del fichero es incorrecto!");
                            }
                            
                            swingReflectionInvoke("setValue", getView().getProgress_pbar(), MAX_VALUE);

                            _file.renameTo(new File(filename));
                            
                            String verify_file = selectSettingValueFromDB("verify_down_file");

                            if(verify_file!=null && verify_file.equals("yes"))
                            {
                                _checking_cbc = true;
                   
                                getView().printStatusNormal("Waiting to check file integrity...");
                                
                                _progress = 0;
                                
                                swingReflectionInvoke("setValue", getView().getProgress_pbar(), 0);
                                
                                synchronized(CBC_LOCK) {
                                    
                                    getView().printStatusNormal("Checking file integrity, please wait...");
                                   
                                    swingReflectionInvoke("setVisible", getView().getStop_button(), true);
                                    
                                    swingReflectionInvoke("setText", getView().getStop_button(), "CANCEL CHECK");

                                    getMain_panel().getDownload_manager().getTransference_running_list().remove(this);

                                    getMain_panel().getDownload_manager().secureNotify();

                                    if(verifyFileCBCMAC(filename))
                                    {
                                        exit_message = "File successfully downloaded! (Integrity check PASSED)";

                                        getView().printStatusOK(exit_message);
                                    }
                                    else if(!_exit)
                                    {
                                        exit_message = "BAD NEWS :( File is DAMAGED!";

                                        getView().printStatusError(exit_message);

                                        _status_error = true;
                                    }
                                    else
                                    {                                
                                        exit_message = "File successfully downloaded! (but integrity check CANCELED)";

                                        getView().printStatusOK(exit_message);

                                        _status_error = true;

                                    }

                                    swingReflectionInvoke("setVisible", getView().getStop_button(), false);

                                    swingReflectionInvoke("setValue", getView().getProgress_pbar(), MAX_VALUE);
                                
                                }
                            }
                            else
                            {
                                exit_message = "File successfully downloaded!";
                                
                                getView().printStatusOK(exit_message);
                                                                
                            }
                        }
                        else if(_exit && _fatal_error == null)
                        {
                            getView().hideAllExceptStatus();
                            
                            exit_message = "Download CANCELED!";
                            
                            getView().printStatusError(exit_message);
                            
                            _status_error = true;
                            
                            if(_file!=null && !(boolean)swingReflectionInvokeAndWaitForReturn("isSelected", getView().getKeep_temp_checkbox())){
                                _file.delete();
                            }
                            
                        }
                        else if(_fatal_error != null)
                        {
                            getView().hideAllExceptStatus();
                            
                            getView().printStatusError(_fatal_error);
                            
                            _status_error = true;
                            
                           
                        }
                        else
                        {
                            getView().hideAllExceptStatus();
                            
                            exit_message = "OOOPS!! Something (bad) happened but... what?";
                            
                            getView().printStatusError(exit_message);
                            
                            _status_error = true;
                           
                        }     
                        

                      
                    }
                    else if(_fatal_error != null)
                    {
                        getView().hideAllExceptStatus();
                        
                        getView().printStatusError(_fatal_error);
                            
                        _status_error = true;
                    }
                    else
                    {
                        getView().hideAllExceptStatus();
                        
                        exit_message = "Download CANCELED!";
                            
                        getView().printStatusError(exit_message);
                            
                        _status_error = true;
                        
                        if(_file!=null && !(boolean)swingReflectionInvokeAndWaitForReturn("isSelected", getView().getKeep_temp_checkbox())){
                            _file.delete();
                        }
                    }
                    
            } else {
            
                    getView().hideAllExceptStatus();
                    
                    swingReflectionInvoke("setVisible", getView().getFile_name_label(), false);

                    exit_message = filename+" already exists!";

                    getView().printStatusError(exit_message);
                    
                    _status_error = true;
            }
                
            }
            else if(_fatal_error != null)
            {
                getView().hideAllExceptStatus();
                                          
                getView().printStatusError(_fatal_error);
                            
                _status_error = true;
            }
            else
            {
                getView().hideAllExceptStatus();
                
                exit_message = "Download CANCELED!";
                            
                getView().printStatusError(exit_message);
                
                _status_error = true;

                if(_file!=null && !(boolean)swingReflectionInvokeAndWaitForReturn("isSelected", getView().getKeep_temp_checkbox())){
                    _file.delete();
                }
            }
        }
        catch (IOException ex) {
            exit_message = "I/O ERROR "+ex.getMessage();
                            
            getView().printStatusError(exit_message);
            
            _status_error = true;
       
            out.println(ex.getMessage());
            
        } catch (Exception ex) {
            out.println(ex.getMessage());
        }    
        
        if(!_exit) {
            
            try {
                deleteDownload(_url);
            } catch (SQLException ex) {
                getLogger(Download.class.getName()).log(SEVERE, null, ex);
            }
            
            getMain_panel().getDownload_manager().getTransference_running_list().remove(this);
        
            getMain_panel().getDownload_manager().getTransference_finished_queue().add(this);

            getMain_panel().getDownload_manager().getScroll_panel().remove(getView());

            getMain_panel().getDownload_manager().getScroll_panel().add(getView());

            getMain_panel().getDownload_manager().secureNotify();
        }

        swingReflectionInvoke("setVisible", getView().getClose_button(), true);
        
        if(_status_error) {
            swingReflectionInvoke("setVisible", getView().getRestart_button(), true);
        }
        
        out.println(_file_name+" Downloader: bye bye");
    }
    
    public void provisionIt(boolean retry) throws MegaAPIException, MegaCrypterAPIException {
        
        getView().printStatusNormal("Provisioning download, please wait...");
        
        swingReflectionInvoke("setVisible", getView().getCopy_link_button(), true);
        
        String[] file_info;
        
        String exit_message=null;
        
        try {
                if(_file_name == null)
                {
                    file_info = getMegaFileMetadata(_url, getMain_panel().getView(), retry);
                    
                    if(file_info==null) {
                        
                       _provision_ok=false;

                    } else {

                        _file_name = file_info[0];

                        _file_size = valueOf(file_info[1]);

                        _file_key=file_info[2];
                        
                        if(file_info.length == 5)
                        {
                            _file_pass = file_info[3];
                        
                            _file_noexpire = file_info[4];
                        }
                        
                        try {

                            insertDownload(_url, _download_path, _file_name, _file_key, _file_size, _file_pass, _file_noexpire);

                        } catch (SQLException ex) {

                            _provision_ok=false;

                            exit_message = "Error registering download (file "+ _download_path+"/"+_file_name +" already downloading)";
                        }
                    }
                } else if(_restart) {
                    
                    try {

                            insertDownload(_url, _download_path, _file_name, _file_key, _file_size, _file_pass, _file_noexpire);

                        } catch (SQLException ex) {

                            _provision_ok=false;

                            exit_message = "Error registering download (file "+ _download_path+"/"+_file_name +" already downloading)";
                        }
                } 
        }catch(MegaAPIException | MegaCrypterAPIException ex){
                    
            throw ex;
            
        } catch (Exception ex) {
                
                _provision_ok=false;
                
                exit_message = ex.getMessage();
            }

        if(!_provision_ok) {
            
            getView().hideAllExceptStatus();
            
            if(_fatal_error != null) {
                
                getView().printStatusError(_fatal_error);
                
            }else if(exit_message!=null) {
                
                getView().printStatusError(exit_message);
            }
            
            swingReflectionInvoke("setVisible", getView().getRestart_button(), true);

        } else {

            getView().printStatusNormal("Waiting to start...");
            
            swingReflectionInvoke("setVisible", getView().getFile_name_label(), true);
            
            swingReflectionInvoke("setText", getView().getFile_name_label(),  truncateText(_download_path+"/"+_file_name, 100));
            
            swingReflectionInvoke("setToolTipText", getView().getFile_name_label(), _download_path+"/"+_file_name);
            
            swingReflectionInvoke("setVisible", getView().getFile_size_label(), true);
            
            swingReflectionInvoke("setText", getView().getFile_size_label(),  formatBytes(_file_size));
        }
        
        swingReflectionInvoke("setVisible", getView().getClose_button(), true);
        
    }
    
    public synchronized void pause_worker() {
        
        if(++_paused_workers == _chunkworkers.size() && !_exit) {
            
            getView().printStatusNormal("Download paused!");
            swingReflectionInvoke("setText", getView().getPause_button(), "RESUME DOWNLOAD");
            swingReflectionInvoke("setEnabled", getView().getPause_button(), true);
        }
    }
    
    public void pause_worker_mono() {
        
        getView().printStatusNormal("Download paused!");
        swingReflectionInvoke("setText", getView().getPause_button(), "RESUME DOWNLOAD");
        swingReflectionInvoke("setEnabled", getView().getPause_button(), true);
        
    }
    

    /* OJO!! -> ESTO ESTÁ CAMBIADO Y NO COMPROBADO!! */
    public synchronized String getDownloadUrlForWorker() throws IOException
    {
        if(_last_download_url != null && checkMegaDownloadUrl(_last_download_url)) {
            
            return _last_download_url;
        }
        
        boolean error;
        
        int api_error_retry=0;
        
        String download_url;
        
        do{
            
            error = false;
            
            try {
                    if( findFirstRegex("://mega(\\.co)?\\.nz/", _url, 0) != null )
                    {
                        MegaAPI ma = new MegaAPI();

                        download_url = ma.getMegaFileDownloadUrl(_url);
                    }    
                    else
                    {
                        download_url = MegaCrypterAPI.getMegaFileDownloadUrl(_url, _file_pass, _file_noexpire);
                    }
                    
                    if(checkMegaDownloadUrl(download_url)) {
                        
                        _last_download_url = download_url;
                        
                    } else {
                        
                        error = true;
                    }
 
            } catch(MegaCrypterAPIException | MegaAPIException e) {
                
                error = true;
                
                for(long i=getWaitTimeExpBackOff(api_error_retry++); i>0 && !_exit; i--)
                {
                    try {
                        sleep(1000);
                    } catch (InterruptedException ex) {}
                }
            }
            
        }while(error);
        
        return _last_download_url;
    }
    
    public synchronized void startSlot()
    {

        int chunk_id = _chunkworkers.size()+1;

        ChunkDownloader c = new ChunkDownloader(chunk_id, this);

        _chunkworkers.add(c);

        try {
            
            _thread_pool.execute(c);
            
        }catch(java.util.concurrent.RejectedExecutionException e){out.println(e.getMessage());}
    }
    
    public synchronized void stopLastStartedSlot()
    {
        if(!_chunkworkers.isEmpty()) {
            
            ChunkDownloader chunkdownloader = _chunkworkers.remove(_chunkworkers.size()-1);
            chunkdownloader.setExit(true);
        }
    }
    
    public synchronized void stopThisSlot(ChunkDownloader chunkdownloader)
    {
        if(_chunkworkers.remove(chunkdownloader))
        {
            swingReflectionInvokeAndWait("setValue", getView().getSlots_spinner(), (int)swingReflectionInvokeAndWaitForReturn("getValue", getView().getSlots_spinner())-1 );
            
            if(!_exit && isPause() && _paused_workers == _chunkworkers.size()) {
                
                getView().printStatusNormal("Download paused!");
                
                swingReflectionInvoke("setText", getView().getPause_button(), "RESUME DOWNLOAD");
                
                swingReflectionInvoke("setEnabled", getView().getPause_button(), true);
            } 
        }
    }
   
    public synchronized boolean chunkDownloadersRunning()
    {
        return !getChunkworkers().isEmpty();
    }
    
    
    @Override
    public void updateProgress(int reads)
    {
        _progress+=reads;
        
        getView().updateProgressBar(_progress, _progress_bar_rate);
    }
    
    
    
    private boolean verifyFileCBCMAC(String filename) throws FileNotFoundException, Exception, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException
    {
        int[] int_key = bin2i32a( UrlBASE642Bin(_file_key));
        
        int[] iv = new int[2];

        iv[0] = int_key[4];
        iv[1] = int_key[5];
        
        int[] meta_mac = new int[2];
        
        meta_mac[0] = int_key[6];
        meta_mac[1] = int_key[7];
        
        int[] file_mac = {0,0,0,0};
        
        int[] cbc_iv = {0,0,0,0};
     
        Cipher cryptor = genCrypter("AES", "AES/CBC/NoPadding", _chunkwriter.getByte_file_key(), i32a2bin(cbc_iv));
        
        try(FileInputStream is = new FileInputStream(new File(filename))) {
            
            long chunk_id=1;
            long tot=0L;
            byte[] chunk_buffer = new byte[16*1024];
            byte[] byte_block = new byte[16];
            int[] int_block;
            int re, reads, to_read;
            try
            {
                while(!_exit)
                {
                    Chunk chunk = new Chunk(chunk_id++, _file_size, null);
                    
                    tot+=chunk.getSize();
                    
                    int[] chunk_mac = {iv[0], iv[1], iv[0], iv[1]};
                    
                    do
                    {
                        to_read = chunk.getSize() - chunk.getOutputStream().size() >= chunk_buffer.length?chunk_buffer.length:(int)(chunk.getSize() - chunk.getOutputStream().size());
                        
                        re=is.read(chunk_buffer, 0, to_read);
                        
                        chunk.getOutputStream().write(chunk_buffer, 0, re);
                        
                    }while(!_exit && chunk.getOutputStream().size()<chunk.getSize());
                    
                    InputStream chunk_is = chunk.getInputStream();
                    
                    while(!_exit && (reads=chunk_is.read(byte_block))!=-1)
                    {
                        if(reads<byte_block.length)
                        {
                            for(int i=reads; i<byte_block.length; i++)
                                byte_block[i]=0;
                        }
                        
                        int_block = bin2i32a(byte_block);
                        
                        for(int i=0; i<chunk_mac.length; i++)
                        {
                            chunk_mac[i]^=int_block[i];
                        }
                        
                        chunk_mac =  bin2i32a(cryptor.doFinal( i32a2bin(chunk_mac)));
                    }
                    
                    for(int i=0; i<file_mac.length; i++)
                    {
                        file_mac[i]^=chunk_mac[i];
                    }
                    
                    file_mac = bin2i32a(cryptor.doFinal( i32a2bin(file_mac)));
                    
                    updateProgress((int)chunk.getSize());
                    
                }
                
            } catch (ChunkInvalidIdException e){}
        
        int[] cbc={file_mac[0]^file_mac[1], file_mac[2]^file_mac[3]};

        return (cbc[0] == meta_mac[0] && cbc[1]==meta_mac[1]);
    }
        
    }
    
    public synchronized void stopDownloader()
    {
        if(!_exit)
        {
            setExit(true);
            
            try {
                deleteDownload(_url);
            } catch (SQLException ex) {
                getLogger(Download.class.getName()).log(SEVERE, null, ex);
            }
            
            getMain_panel().getDownload_manager().getTransference_running_list().remove(this);
        
            getMain_panel().getDownload_manager().getTransference_finished_queue().add(this);

            getMain_panel().getDownload_manager().getScroll_panel().remove(getView());

            getMain_panel().getDownload_manager().getScroll_panel().add(getView());

            getMain_panel().getDownload_manager().secureNotify();

            if(isRetrying_request())
            {
                getView().printStatusNormal("Retrying cancelled!");
                
                swingReflectionInvoke("setEnabled", getView().getStop_button(), false);
            }
            else if(isChecking_cbc())
            {
                getView().printStatusNormal("Verification cancelled!");
                
                swingReflectionInvoke("setEnabled", getView().getStop_button(), false);
            }
            else
            {
                getView().stop();
  
                for(ChunkDownloader downloader:_chunkworkers) {
                
                    downloader.secureNotify();
                }
            }
        }
    }
    
    public synchronized void emergencyStopDownloader(String reason)
    {
        if(_fatal_error == null)
        {
            _fatal_error = reason!=null?reason:"FATAL ERROR!";
            
            stopDownloader();
        }
    }
    
    public long calculateMaxTempFileSize(long size)
    {
        if(size > 3584*1024)
        {
            long reminder = (size - 3584*1024)%(1024*1024);
            
            return reminder==0?size:(size - reminder);
        }
        else
        {
            int i=0, tot=0;
            
            while(tot < size)
            {
                i++;
                tot+=i*128*1024;
            }
            
            return tot==size?size:(tot-i*128*1024);
        }
    }
    
    
    
    public String[] getMegaFileMetadata(String link, MainPanelView panel, boolean retry_request) throws IOException, InterruptedException, MegaAPIException,MegaCrypterAPIException
    {
 
        String[] file_info=null;
        int retry=0, error_code;
        boolean error;

        do
        {
            error=false;
            
            try
            {
 
                if( findFirstRegex("://mega(\\.co)?\\.nz/", link, 0) != null)
                {
                    MegaAPI ma = new MegaAPI();

                    file_info = ma.getMegaFileMetadata(link);
                }    
                else
                {
                    file_info = MegaCrypterAPI.getMegaFileMetadata(link, panel);    
                }

            }
            catch(MegaAPIException | MegaCrypterAPIException ex)
            {
                error=true;

                error_code = parseInt(ex.getMessage());

                switch(error_code)
                { 
                    case -2:
                        emergencyStopDownloader("Mega link is not valid!");
                        break;
                        
                    case -14:
                        emergencyStopDownloader("Mega link is not valid!");
                        break;
                        
                    case 22:
                        emergencyStopDownloader("MegaCrypter link is not valid!");
                        break;

                    case 23:
                        emergencyStopDownloader("MegaCrypter link is blocked!");
                        break;

                    case 24:
                        emergencyStopDownloader("MegaCrypter link has expired!");
                        break;

                    default:
                        
                        if(!retry_request) {
                    
                            throw ex;
                        }

                        _retrying_request = true;
                        
                        swingReflectionInvoke("setEnabled", getMain_panel().getView().getNew_download_menu(), true);

                        swingReflectionInvoke("setVisible", getView().getStop_button(), true);

                        swingReflectionInvoke("setText", getView().getStop_button(), "CANCEL RETRY");

                        for(long i=getWaitTimeExpBackOff(retry++); i>0 && !_exit; i--)
                        {
                            if(error_code == -18)
                            {
                                getView().printStatusError("File temporarily unavailable! (Retrying in "+i+" secs...)");
                            }
                            else
                            {
                                getView().printStatusError("Mega/MC APIException error "+ex.getMessage()+" (Retrying in "+i+" secs...)");
                            }

                            try {
                                sleep(1000);
                            } catch (InterruptedException ex2) {}
                        }
                }
            } catch(Exception ex) {
                emergencyStopDownloader("Mega link is not valid!");
            }
            
        }while(!_exit && error);
        
        if(!error) {
            swingReflectionInvoke("setText", getView().getStop_button(), "CANCEL DOWNLOAD");
            swingReflectionInvoke("setVisible", getView().getStop_button(), false);
        }
        
        return file_info;

        
    }
    
    public String getMegaFileDownloadUrl(String link) throws IOException, InterruptedException
    {

        String dl_url=null;
        int retry=0, error_code;
        boolean error;

        do
        {
            error=false;

            try
            {
                if( findFirstRegex("://mega(\\.co)?\\.nz/", _url, 0) != null)
                {
                    MegaAPI ma = new MegaAPI();

                    dl_url = ma.getMegaFileDownloadUrl(link);


                }    
                else
                {
                    dl_url = MegaCrypterAPI.getMegaFileDownloadUrl(link, _file_pass, _file_noexpire);
                }
                
            }
            catch(MegaAPIException | MegaCrypterAPIException ex)
            {
                error=true;

                error_code = parseInt(ex.getMessage());

                switch(error_code)
                { 
                    case 22:
                        emergencyStopDownloader("MegaCrypter link is not valid!");
                        break;

                    case 23:
                        emergencyStopDownloader("MegaCrypter link is blocked!");
                        break;

                    case 24:
                        emergencyStopDownloader("MegaCrypter link has expired!");
                        break;

                    default:

                        _retrying_request = true;

                        swingReflectionInvoke("setVisible", getView().getStop_button(), true);

                        swingReflectionInvoke("setText", getView().getStop_button(), "CANCEL RETRY");

                        for(long i=getWaitTimeExpBackOff(retry++); i>0 && !_exit; i--)
                        {
                            if(error_code == -18)
                            {
                                getView().printStatusError("File temporarily unavailable! (Retrying in "+i+" secs...)");
                            }
                            else
                            {
                                getView().printStatusError("Mega/MC APIException error "+ex.getMessage()+" (Retrying in "+i+" secs...)");
                            }

                            try {
                                sleep(1000);
                            } catch (InterruptedException ex2) {}
                        }
                }
            }

        }while(!_exit && error);
        
        if(!error) {
            swingReflectionInvoke("setText", getView().getStop_button(), "CANCEL DOWNLOAD");
            swingReflectionInvoke("setVisible", getView().getStop_button(), false);
        }
        
        return dl_url;
        }
    
    
    public synchronized long nextChunkId()
    {
        Long next_id;
        
        if((next_id=_rejectedChunkIds.poll()) != null) {
            return next_id;
        }
        else {
            return ++_last_chunk_id_dispatched;
        }
    }
    
    public void rejectChunkId(long chunk_id)
    {
        _rejectedChunkIds.add(chunk_id);
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
                    getLogger(Download.class.getName()).log(SEVERE, null, ex);
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
    
}