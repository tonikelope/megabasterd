package megabasterd;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import static megabasterd.MiscTools.getApacheKissHttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;

/**
 *
 * @author tonikelope
 */
public class SmartMegaProxyManager implements Runnable {

    public static final int PROXY_TIMEOUT = 30;
    public static final int REFRESH_PROXY_LIST_TIMEOUT = 300;
    private String _proxy_list_url;
    private final ConcurrentLinkedQueue<String> _proxy_list;
    private volatile boolean _exit;
    private final Object _refresh_lock;

    public SmartMegaProxyManager(String proxy_list_url) {
        _proxy_list_url = proxy_list_url;
        _proxy_list = new ConcurrentLinkedQueue<>();
        _exit = false;
        _refresh_lock = new Object();
    }

    public void setExit(boolean exit) {
        _exit = exit;
    }

    public Object getRefresh_lock() {
        return _refresh_lock;
    }

    public synchronized String getRandomProxy() {

        synchronized (_refresh_lock) {

            if (_proxy_list.size() > 0) {

                Random random = new Random();

                return (String) _proxy_list.toArray()[random.nextInt(_proxy_list.size())];

            } else {

                return null;
            }
        }
    }

    public synchronized void setProxy_list_url(String proxy_list_url) {
        _proxy_list_url = proxy_list_url;
    }

    public String getRandomProxy(ConcurrentLinkedQueue<String> excluded) {

        synchronized (_refresh_lock) {

            if (_proxy_list.size() > 0) {

                if (excluded.size() > 0) {

                    ArrayList<String> available_proxys = new ArrayList<>();

                    for (String proxy : _proxy_list) {

                        if (!excluded.contains(proxy)) {

                            available_proxys.add(proxy);
                        }
                    }

                    if (available_proxys.size() > 0) {

                        Random random = new Random();

                        return (String) available_proxys.toArray()[random.nextInt(available_proxys.size())];

                    } else {

                        return null;
                    }

                } else {

                    Random random = new Random();

                    return (String) _proxy_list.toArray()[random.nextInt(_proxy_list.size())];
                }

            } else {

                return null;
            }
        }
    }

    public ConcurrentLinkedQueue<String> getProxy_list() {
        return _proxy_list;
    }

    private void _refreshProxyList() {

        String data;

        try (CloseableHttpClient httpclient = getApacheKissHttpClient()) {

            if (this._proxy_list_url != null && this._proxy_list_url.length() > 0) {
                HttpGet httpget = new HttpGet(new URI(this._proxy_list_url));

                try (CloseableHttpResponse httpresponse = httpclient.execute(httpget)) {

                    InputStream is = httpresponse.getEntity().getContent();

                    try (ByteArrayOutputStream byte_res = new ByteArrayOutputStream()) {

                        byte[] buffer = new byte[MainPanel.DEFAULT_BYTE_BUFFER_SIZE];

                        int reads;

                        while ((reads = is.read(buffer)) != -1) {

                            byte_res.write(buffer, 0, reads);
                        }

                        data = new String(byte_res.toByteArray());
                    }
                }

                String[] proxy_list = data.split("\n");

                if (proxy_list.length > 0) {

                    _proxy_list.clear();

                    this._proxy_list.addAll(Arrays.asList(proxy_list));
                }
            }

        } catch (MalformedURLException ex) {
            Logger.getLogger(SmartMegaProxyManager.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException | URISyntaxException ex) {
            Logger.getLogger(SmartMegaProxyManager.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void run() {

        Logger.getLogger(SmartMegaProxyManager.class.getName()).log(Level.INFO, "{0} Smart Proxy Manager: hello!", new Object[]{Thread.currentThread().getName()});

        while (!_exit) {

            synchronized (_refresh_lock) {

                this._refreshProxyList();

                Logger.getLogger(SmartMegaProxyManager.class.getName()).log(Level.INFO, "{0} Smart Proxy Manager: proxy list refreshed ({1})", new Object[]{Thread.currentThread().getName(), _proxy_list.size()});

                try {
                    _refresh_lock.wait(1000 * REFRESH_PROXY_LIST_TIMEOUT);
                } catch (InterruptedException ex) {
                    Logger.getLogger(SmartMegaProxyManager.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }

        Logger.getLogger(SmartMegaProxyManager.class.getName()).log(Level.INFO, "{0} Smart Proxy Manager: bye bye", new Object[]{Thread.currentThread().getName()});
    }

}
