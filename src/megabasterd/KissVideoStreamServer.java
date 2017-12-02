package megabasterd;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import static java.util.logging.Level.SEVERE;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.crypto.CipherInputStream;
import static megabasterd.Download.WATCHDOG_SMART_PROXY_TIMEOUT;
import static megabasterd.MainPanel.*;
import static megabasterd.MiscTools.*;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;

public final class KissVideoStreamServer implements HttpHandler, SecureSingleThreadNotifiable {

    public static final int WORKER_STATUS_FILE_INFO = 0x01;
    public static final int WORKER_STATUS_STREAM = 0x02;
    public static final int WORKER_STATUS_RETRY = 0x03;
    public static final int WORKER_STATUS_EXIT = 0x04;
    public static final int WORKERS = 8;

    private final MainPanel _main_panel;
    private final ConcurrentHashMap<String, HashMap<String, Object>> _link_cache;
    private final ConcurrentHashMap<Thread, Integer> _working_threads;
    private final ContentType _ctype;
    private boolean _notified;
    private final Object _secure_notify_lock;
    private volatile boolean _use_smart_proxy;
    private volatile int _last_proxy_list_hashcode;
    private final ConcurrentLinkedQueue<String> _excluded_proxies;
    private final Object _watchdog_lock;

    public KissVideoStreamServer(MainPanel panel) {
        _main_panel = panel;
        _link_cache = new ConcurrentHashMap();
        _working_threads = new ConcurrentHashMap();
        _ctype = new ContentType();
        _notified = false;
        _secure_notify_lock = new Object();
        _use_smart_proxy = false;
        _watchdog_lock = new Object();
        _excluded_proxies = new ConcurrentLinkedQueue<>();
        _last_proxy_list_hashcode = -1;
    }

    private void _start_smart_proxy_watchdog() {

        THREAD_POOL.execute(new Runnable() {

            @Override
            public void run() {

                while (true) {

                    try {

                        if (_use_smart_proxy) {

                            _use_smart_proxy = false;

                            int proxy_list_hashcode = getMain_panel().getProxy_manager().getProxy_list().hashCode();

                            if (_last_proxy_list_hashcode != proxy_list_hashcode) {

                                _last_proxy_list_hashcode = proxy_list_hashcode;
                                _excluded_proxies.clear();
                                Logger.getLogger(KissVideoStreamServer.class.getName()).log(Level.INFO, "{0} SmartProxy excluded list cleared!", new Object[]{Thread.currentThread().getName()});

                            }
                        }

                        synchronized (_watchdog_lock) {
                            _watchdog_lock.wait(WATCHDOG_SMART_PROXY_TIMEOUT * 1000);
                        }

                    } catch (InterruptedException ex) {
                        Logger.getLogger(KissVideoStreamServer.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        });
    }

    public ConcurrentLinkedQueue<String> getExcluded_proxies() {
        return _excluded_proxies;
    }

    public boolean isUse_smart_proxy() {
        return _use_smart_proxy;
    }

    public void setUse_smart_proxy(boolean _use_smart_proxy) {
        this._use_smart_proxy = _use_smart_proxy;
    }

    public MainPanel getMain_panel() {
        return _main_panel;
    }

    public ConcurrentHashMap<String, HashMap<String, Object>> getLink_cache() {
        return _link_cache;
    }

    public ConcurrentHashMap<Thread, Integer> getWorking_threads() {
        return _working_threads;
    }

    public ContentType getCtype() {
        return _ctype;
    }

    @Override
    public void secureNotify() {
        synchronized (_secure_notify_lock) {

            _notified = true;

            _secure_notify_lock.notify();
        }
    }

    @Override
    public void secureWait() {

        synchronized (_secure_notify_lock) {
            while (!_notified) {

                try {
                    _secure_notify_lock.wait();
                } catch (InterruptedException ex) {
                    Logger.getLogger(getClass().getName()).log(SEVERE, null, ex);
                }
            }

            _notified = false;
        }
    }

    public void start(int port, String context) throws IOException {

        _start_smart_proxy_watchdog();

        swingReflectionInvoke("setForeground", _main_panel.getView().getKiss_server_status(), new Color(0, 128, 0));

        swingReflectionInvoke("setText", _main_panel.getView().getKiss_server_status(), "Stream server running on localhost:" + STREAMER_PORT + " (Waiting for request...)");

        HttpServer httpserver = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), port), 0);

        httpserver.createContext(context, this);

        httpserver.setExecutor(THREAD_POOL);

        httpserver.start();
    }

    private void updateStatus(Integer new_status) {

        if (new_status != WORKER_STATUS_EXIT) {

            getWorking_threads().put(Thread.currentThread(), new_status);

        } else {

            getWorking_threads().remove(Thread.currentThread());
        }

        int conta_info = 0, conta_connect = 0, conta_stream = 0, conta_retry = 0;

        for (Integer thread_status : getWorking_threads().values()) {

            switch (thread_status) {

                case WORKER_STATUS_FILE_INFO:
                    conta_info++;
                    break;

                case WORKER_STATUS_STREAM:
                    conta_stream++;
                    break;

                case WORKER_STATUS_RETRY:
                    conta_retry++;
                    break;
            }
        }

        String status;

        if (conta_info > 0 || conta_connect > 0 || conta_stream > 0 || conta_retry > 0) {

            status = "Stream server running on localhost:" + STREAMER_PORT + "  Info: " + conta_info + " / Stream: " + conta_stream + " / Retry: " + conta_retry;

        } else {

            status = "Stream server running on localhost:" + STREAMER_PORT + " (Waiting for request...)";
        }

        swingReflectionInvoke("setText", _main_panel.getView().getKiss_server_status(), status);
    }

    private String[] getMegaFileMetadata(String link, MainPanelView panel) throws IOException, InterruptedException {
        String[] file_info = null;
        int retry = 0;
        boolean error;

        do {
            updateStatus(WORKER_STATUS_FILE_INFO);

            error = false;

            try {
                if (findFirstRegex("://mega(\\.co)?\\.nz/", link, 0) != null) {

                    MegaAPI ma = new MegaAPI();

                    file_info = ma.getMegaFileMetadata(link);

                } else {

                    file_info = MegaCrypterAPI.getMegaFileMetadata(link, panel, getMain_panel().getMega_proxy_server() != null ? (getMain_panel().getMega_proxy_server().getPort() + ":" + Bin2BASE64(("megacrypter:" + getMain_panel().getMega_proxy_server().getPassword()).getBytes())) : null);

                }

            } catch (MegaAPIException | MegaCrypterAPIException e) {
                error = true;

                switch (Integer.parseInt(e.getMessage())) {
                    case -2:
                        throw new IOException("Mega link is not valid!");

                    case -14:
                        throw new IOException("Mega link is not valid!");

                    case 22:
                        throw new IOException("MegaCrypter link is not valid!");

                    case 23:
                        throw new IOException("MegaCrypter link is blocked!");

                    case 24:
                        throw new IOException("MegaCrypter link has expired!");

                    case 25:
                        throw new IOException("MegaCrypter link pass error!");

                    default:

                        updateStatus(WORKER_STATUS_RETRY);

                        for (long i = getWaitTimeExpBackOff(retry++); i > 0; i--) {
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException ex) {
                            }
                        }
                }

            }

        } while (error);

        return file_info;
    }

    public String getMegaFileDownloadUrl(String link, String pass_hash, String noexpire_token, String mega_account) throws IOException, InterruptedException {
        String dl_url = null;
        int retry = 0;
        boolean error;

        do {
            updateStatus(WORKER_STATUS_FILE_INFO);

            error = false;

            try {

                MegaAPI ma = null;

                if (mega_account == null || (ma = checkMegaAccountLoginAndShowMasterPassDialog(_main_panel, _main_panel.getView(), mega_account)) == null) {

                    ma = new MegaAPI();
                }

                if (findFirstRegex("://mega(\\.co)?\\.nz/", link, 0) != null) {
                    dl_url = ma.getMegaFileDownloadUrl(link);

                } else {
                    dl_url = MegaCrypterAPI.getMegaFileDownloadUrl(link, pass_hash, noexpire_token, ma.getSid(), getMain_panel().getMega_proxy_server() != null ? (getMain_panel().getMega_proxy_server().getPort() + ":" + Bin2BASE64(("megacrypter:" + getMain_panel().getMega_proxy_server().getPassword()).getBytes()) + ":" + MiscTools.getMyPublicIP()) : null);
                }
            } catch (MegaAPIException | MegaCrypterAPIException e) {
                error = true;

                switch (Integer.parseInt(e.getMessage())) {
                    case 22:
                        throw new IOException("MegaCrypter link is not valid!");

                    case 23:
                        throw new IOException("MegaCrypter link is blocked!");

                    case 24:
                        throw new IOException("MegaCrypter link has expired!");

                    case 25:
                        throw new IOException("MegaCrypter link pass error!");

                    default:

                        updateStatus(WORKER_STATUS_RETRY);

                        for (long i = getWaitTimeExpBackOff(retry++); i > 0; i--) {
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException ex) {
                            }
                        }
                }
            } catch (Exception ex) {
                Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
            }

        } while (error);

        return dl_url;
    }

    private long[] parseRangeHeader(String header) {

        Pattern pattern = Pattern.compile("bytes *\\= *([0-9]+) *\\- *([0-9]+)?");

        Matcher matcher = pattern.matcher(header);

        long[] ranges = new long[2];

        if (matcher.find()) {
            ranges[0] = Long.valueOf(matcher.group(1));

            if (matcher.group(2) != null) {
                ranges[1] = Long.valueOf(matcher.group(2));
            } else {
                ranges[1] = -1;
            }
        }

        return ranges;
    }

    @Override
    public void handle(HttpExchange xchg) throws IOException {

        StreamChunkWriter chunkwriter = null;
        ArrayList<StreamChunkDownloader> chunkworkers = new ArrayList<>();
        final PipedInputStream pipein = new PipedInputStream();
        final PipedOutputStream pipeout = new PipedOutputStream(pipein);

        long clength;

        OutputStream os;

        CipherInputStream cis;

        String httpmethod = xchg.getRequestMethod();

        HttpGet httpget;

        try (CloseableHttpClient httpclient = getApacheKissHttpClient()) {

            Headers reqheaders = xchg.getRequestHeaders();

            Headers resheaders = xchg.getResponseHeaders();

            String url_path = xchg.getRequestURI().getPath();

            String mega_account;

            String link;

            String[] url_parts = new String(UrlBASE642Bin(url_path.substring(url_path.indexOf("/video/") + 7))).split("\\|");

            mega_account = url_parts[0];

            if (mega_account.isEmpty()) {
                mega_account = null;
            }

            link = url_parts[1];

            Logger.getLogger(getClass().getName()).log(Level.INFO, "{0} {1} {2}", new Object[]{Thread.currentThread().getName(), link, mega_account});

            HashMap cache_info, file_info;

            cache_info = getLink_cache().get(link);

            if (cache_info != null) {

                file_info = cache_info;

            } else {

                String[] finfo = getMegaFileMetadata(link, _main_panel.getView());

                file_info = new HashMap<>();

                file_info.put("file_name", finfo[0]);

                file_info.put("file_size", Long.parseLong(finfo[1]));

                file_info.put("file_key", finfo[2]);

                file_info.put("pass_hash", finfo.length >= 5 ? finfo[3] : null);

                file_info.put("noexpiretoken", finfo.length >= 5 ? finfo[4] : null);

                file_info.put("url", null);
            }

            String file_name = (String) file_info.get("file_name");

            long file_size = (long) file_info.get("file_size");

            String file_key = (String) file_info.get("file_key");

            String pass_hash = (String) file_info.get("pass_hash");

            String noexpire_token = (String) file_info.get("noexpiretoken");

            String file_ext = file_name.substring(file_name.lastIndexOf('.') + 1).toLowerCase();

            if (httpmethod.equals("HEAD")) {

                resheaders.add("Accept-Ranges", "bytes");

                resheaders.add("transferMode.dlna.org", "Streaming");

                resheaders.add("contentFeatures.dlna.org", "DLNA.ORG_OP=01;DLNA.ORG_CI=0;DLNA.ORG_FLAGS=01700000000000000000000000000000");

                resheaders.add("Content-Type", getCtype().getMIME(file_ext));

                resheaders.add("Content-Length", String.valueOf(file_size));

                resheaders.add("Connection", "close");

                xchg.sendResponseHeaders(HttpStatus.SC_OK, 0);

            } else if (httpmethod.equals("GET")) {

                resheaders.add("Accept-Ranges", "bytes");

                resheaders.add("transferMode.dlna.org", "Streaming");

                resheaders.add("contentFeatures.dlna.org", "DLNA.ORG_OP=01;DLNA.ORG_CI=0;DLNA.ORG_FLAGS=01700000000000000000000000000000");

                resheaders.add("Content-Type", getCtype().getMIME(file_ext));

                resheaders.add("Connection", "close");

                byte[] buffer = new byte[MainPanel.DEFAULT_BYTE_BUFFER_SIZE];

                int reads;

                String temp_url;

                if (file_info.get("url") != null) {

                    temp_url = (String) file_info.get("url");

                    if (!checkMegaDownloadUrl(temp_url)) {

                        temp_url = getMegaFileDownloadUrl(link, pass_hash, noexpire_token, mega_account);

                        file_info.put("url", temp_url);
                    }

                } else {

                    temp_url = getMegaFileDownloadUrl(link, pass_hash, noexpire_token, mega_account);

                    file_info.put("url", temp_url);
                }

                getLink_cache().put(link, file_info);

                long[] ranges = new long[2];

                int sync_bytes = 0;

                String header_range = null;

                InputStream is;

                URL url;

                if (reqheaders.containsKey("Range")) {
                    header_range = "Range";

                } else if (reqheaders.containsKey("range")) {

                    header_range = "range";
                }

                if (header_range != null) {
                    List<String> ranges_raw = reqheaders.get(header_range);

                    String range_header = ranges_raw.get(0);

                    ranges = parseRangeHeader(range_header);

                    sync_bytes = (int) ranges[0] % 16;

                    if (ranges[1] >= 0 && ranges[1] >= ranges[0]) {

                        clength = ranges[1] - ranges[0] + 1;

                    } else {

                        clength = file_size - ranges[0];
                    }

                    resheaders.add("Content-Range", "bytes " + ranges[0] + "-" + (ranges[1] >= 0 ? ranges[1] : (file_size - 1)) + "/" + file_size);

                    xchg.sendResponseHeaders(HttpStatus.SC_PARTIAL_CONTENT, clength);

                    chunkwriter = new StreamChunkWriter(this, link, file_info, mega_account, pipeout, temp_url, ranges[0] - sync_bytes, ranges[1] >= 0 ? ranges[1] : file_size - 1);

                } else {

                    xchg.sendResponseHeaders(HttpStatus.SC_OK, file_size);

                    chunkwriter = new StreamChunkWriter(this, link, file_info, mega_account, pipeout, temp_url, 0, file_size - 1);
                }

                THREAD_POOL.execute(chunkwriter);

                for (int i = 0; i < WORKERS; i++) {

                    StreamChunkDownloader worker = new StreamChunkDownloader(i + 1, chunkwriter);

                    chunkworkers.add(worker);

                    THREAD_POOL.execute(worker);
                }

                is = pipein;

                byte[] iv = CryptTools.initMEGALinkKeyIV(file_key);

                cis = new CipherInputStream(is, CryptTools.genDecrypter("AES", "AES/CTR/NoPadding", CryptTools.initMEGALinkKey(file_key), (header_range != null && (ranges[0] - sync_bytes) > 0) ? CryptTools.forwardMEGALinkKeyIV(iv, ranges[0] - sync_bytes) : iv));

                os = xchg.getResponseBody();

                cis.skip(sync_bytes);

                updateStatus(WORKER_STATUS_STREAM);

                while ((reads = cis.read(buffer)) != -1) {

                    os.write(buffer, 0, reads);
                }
            }
        } catch (Exception ex) {

            if (!(ex instanceof IOException)) {
                Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
            }

        } finally {
            Logger.getLogger(getClass().getName()).log(Level.INFO, "{0} KissVideoStreamerHandle: bye bye", Thread.currentThread().getName());

            xchg.close();

            if (chunkwriter != null) {

                pipeout.close();

                for (StreamChunkDownloader d : chunkworkers) {

                    d.setExit(true);
                }

                chunkwriter.setExit(true);

                chunkwriter.secureNotifyAll();
            }

            updateStatus(WORKER_STATUS_EXIT);
        }
    }

}
