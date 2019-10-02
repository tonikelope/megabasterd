package com.tonikelope.megabasterd;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import static com.tonikelope.megabasterd.MainPanel.*;
import static com.tonikelope.megabasterd.MiscTools.*;
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

/**
 *
 * @author tonikelope
 */
public final class KissVideoStreamServer implements HttpHandler, SecureSingleThreadNotifiable {

    public static final int THREAD_START = 0x01;
    public static final int THREAD_STOP = 0x02;
    public static final int DEFAULT_WORKERS = 10;

    private final MainPanel _main_panel;
    private final ConcurrentHashMap<String, HashMap<String, Object>> _link_cache;
    private final ConcurrentLinkedQueue<Thread> _working_threads;
    private final ContentType _ctype;
    private boolean _notified;
    private final Object _secure_notify_lock;

    public KissVideoStreamServer(MainPanel panel) {
        _main_panel = panel;
        _link_cache = new ConcurrentHashMap();
        _working_threads = new ConcurrentLinkedQueue<>();
        _ctype = new ContentType();
        _notified = false;
        _secure_notify_lock = new Object();

    }

    public MainPanel getMain_panel() {
        return _main_panel;
    }

    public ConcurrentHashMap<String, HashMap<String, Object>> getLink_cache() {
        return _link_cache;
    }

    public ConcurrentLinkedQueue<Thread> getWorking_threads() {
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
                    LOG.log(SEVERE, null, ex);
                }
            }

            _notified = false;
        }
    }

    public void start(int port, String context) throws IOException {

        _main_panel.getView().updateKissStreamServerStatus(LabelTranslatorSingleton.getInstance().translate("Streaming server: ON (port ") + STREAMER_PORT + ")");

        HttpServer httpserver = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), port), 0);

        httpserver.createContext(context, this);

        httpserver.setExecutor(THREAD_POOL);

        httpserver.start();
    }

    private void _updateStatus(Integer status) {

        if (status == THREAD_START && !getWorking_threads().contains(Thread.currentThread())) {
            getWorking_threads().add(Thread.currentThread());
        } else {
            getWorking_threads().remove(Thread.currentThread());
        }

        _updateStatusView();
    }

    private void _updateStatusView() {

        String status;

        if (getWorking_threads().size() > 0) {

            status = LabelTranslatorSingleton.getInstance().translate("Streaming server: ON (port ") + STREAMER_PORT + " [" + getWorking_threads().size() + "])";

        } else {

            status = LabelTranslatorSingleton.getInstance().translate("Streaming server: ON (port ") + STREAMER_PORT + ")";
        }

        _main_panel.getView().updateKissStreamServerStatus(status);

    }

    private String[] _getMegaFileMetadata(String link, MainPanelView panel) throws IOException {

        String[] file_info = null;
        int conta_error = 0;
        boolean error;

        do {

            error = false;

            try {

                if (findFirstRegex("://mega(\\.co)?\\.nz/", link, 0) != null) {

                    MegaAPI ma = new MegaAPI();

                    file_info = ma.getMegaFileMetadata(link);

                } else {

                    file_info = MegaCrypterAPI.getMegaFileMetadata(link, panel, getMain_panel().getMega_proxy_server() != null ? (getMain_panel().getMega_proxy_server().getPort() + ":" + Bin2BASE64(("megacrypter:" + getMain_panel().getMega_proxy_server().getPassword()).getBytes("UTF-8"))) : null);

                }

            } catch (APIException ex) {

                error = true;

                LOG.log(Level.SEVERE, ex.getMessage());

                try {
                    Thread.sleep(getWaitTimeExpBackOff(conta_error++) * 1000);
                } catch (InterruptedException ex2) {
                    LOG.log(Level.SEVERE, ex2.getMessage());

                }

            }

        } while (error);

        return file_info;
    }

    public String getMegaFileDownloadUrl(String link, String pass_hash, String noexpire_token, String mega_account) throws Exception {

        String dl_url = null;
        int conta_error = 0;
        boolean error;

        do {

            error = false;

            try {

                MegaAPI ma = new MegaAPI();

                if (mega_account != null) {

                    ma = checkMegaAccountLoginAndShowMasterPassDialog(_main_panel, _main_panel.getView(), mega_account);
                }

                if (findFirstRegex("://mega(\\.co)?\\.nz/", link, 0) != null) {
                    dl_url = ma.getMegaFileDownloadUrl(link);

                } else {
                    dl_url = MegaCrypterAPI.getMegaFileDownloadUrl(link, pass_hash, noexpire_token, ma.getSid(), getMain_panel().getMega_proxy_server() != null ? (getMain_panel().getMega_proxy_server().getPort() + ":" + Bin2BASE64(("megacrypter:" + getMain_panel().getMega_proxy_server().getPassword()).getBytes("UTF-8")) + ":" + MiscTools.getMyPublicIP()) : null);
                }

            } catch (APIException ex) {

                error = true;

                LOG.log(Level.SEVERE, ex.getMessage());

                try {
                    Thread.sleep(getWaitTimeExpBackOff(conta_error++) * 1000);
                } catch (InterruptedException ex2) {
                    LOG.log(Level.SEVERE, ex2.getMessage());
                }

            }

        } while (error);

        return dl_url;
    }

    private long[] _parseRangeHeader(String header) {

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

        _updateStatus(THREAD_START);

        StreamChunkManager chunkwriter = null;
        ArrayList<StreamChunkDownloader> chunkworkers = new ArrayList<>();
        final PipedOutputStream pipeout = new PipedOutputStream();
        final PipedInputStream pipein = new PipedInputStream(pipeout);

        long clength;

        OutputStream os;

        CipherInputStream cis;

        String httpmethod = xchg.getRequestMethod();

        try {

            Headers reqheaders = xchg.getRequestHeaders();

            Headers resheaders = xchg.getResponseHeaders();

            String url_path = xchg.getRequestURI().getPath();

            String mega_account;

            String link;

            String[] url_parts = new String(UrlBASE642Bin(url_path.substring(url_path.indexOf("/video/") + 7)), "UTF-8").split("\\|");

            mega_account = url_parts[0];

            if (mega_account.isEmpty()) {
                mega_account = null;
            }

            link = url_parts[1];

            LOG.log(Level.INFO, "{0} {1} {2}", new Object[]{Thread.currentThread().getName(), link, mega_account});

            HashMap cache_info, file_info;

            cache_info = getLink_cache().get(link);

            if (cache_info != null) {

                file_info = cache_info;

            } else {

                String[] finfo = _getMegaFileMetadata(link, _main_panel.getView());

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

                xchg.sendResponseHeaders(200, 0);

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

                    ranges = _parseRangeHeader(range_header);

                    sync_bytes = (int) ranges[0] % 16;

                    if (ranges[1] >= 0 && ranges[1] >= ranges[0]) {

                        clength = ranges[1] - ranges[0] + 1;

                    } else {

                        clength = file_size - ranges[0];
                    }

                    resheaders.add("Content-Range", "bytes " + ranges[0] + "-" + (ranges[1] >= 0 ? ranges[1] : (file_size - 1)) + "/" + file_size);

                    xchg.sendResponseHeaders(206, clength);

                    chunkwriter = new StreamChunkManager(this, link, file_info, mega_account, pipeout, temp_url, ranges[0] - sync_bytes, ranges[1] >= 0 ? ranges[1] : file_size - 1);

                } else {

                    xchg.sendResponseHeaders(200, file_size);

                    chunkwriter = new StreamChunkManager(this, link, file_info, mega_account, pipeout, temp_url, 0, file_size - 1);
                }

                THREAD_POOL.execute(chunkwriter);

                for (int i = 0; i < DEFAULT_WORKERS; i++) {

                    StreamChunkDownloader worker = new StreamChunkDownloader(i + 1, chunkwriter);

                    chunkworkers.add(worker);

                    THREAD_POOL.execute(worker);
                }

                is = pipein;

                byte[] iv = CryptTools.initMEGALinkKeyIV(file_key);

                cis = new CipherInputStream(is, CryptTools.genDecrypter("AES", "AES/CTR/NoPadding", CryptTools.initMEGALinkKey(file_key), (header_range != null && (ranges[0] - sync_bytes) > 0) ? CryptTools.forwardMEGALinkKeyIV(iv, ranges[0] - sync_bytes) : iv));

                os = xchg.getResponseBody();

                cis.skip(sync_bytes);

                while ((reads = cis.read(buffer)) != -1) {

                    os.write(buffer, 0, reads);
                }
            }
        } catch (Exception ex) {

            if (!(ex instanceof IOException)) {
                LOG.log(Level.SEVERE, ex.getMessage());
            }

        } finally {
            LOG.log(Level.INFO, "{0} KissVideoStreamerHandle: bye bye", Thread.currentThread().getName());

            if (chunkwriter != null) {

                pipeout.close();

                for (StreamChunkDownloader d : chunkworkers) {

                    d.setExit(true);
                }

                chunkwriter.setExit(true);

                chunkwriter.secureNotifyAll();
            }

            xchg.close();
        }

        _updateStatus(THREAD_STOP);
    }
    private static final Logger LOG = Logger.getLogger(KissVideoStreamServer.class.getName());
}
