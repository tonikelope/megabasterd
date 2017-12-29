package megabasterd;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import static megabasterd.MainPanel.THREAD_POOL;
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
    public static final int REFRESH_PROXY_LIST_TIMEOUT = 1800;
    private volatile String _proxy_list_url;
    private final ConcurrentLinkedQueue<String> _proxy_list;
    private final MainPanel _main_panel;
    private volatile boolean _exit;
    private volatile boolean _use_smart_proxy;
    private final Object _refresh_lock;

    public SmartMegaProxyManager(MainPanel main_panel, String proxy_list_url) {
        _main_panel = main_panel;
        _proxy_list_url = proxy_list_url;
        _proxy_list = new ConcurrentLinkedQueue<>();
        _exit = false;
        _use_smart_proxy = false;
        _refresh_lock = new Object();
    }

    public boolean isUse_smart_proxy() {
        return _use_smart_proxy;
    }

    public void setUse_smart_proxy(boolean use_smart_proxy) {

        if (!_main_panel.isLimit_download_speed()) {

            if (!_use_smart_proxy && use_smart_proxy) {
                _main_panel.getView().getGlobal_speed_down_label().setForeground(Color.BLACK);
            } else if (_use_smart_proxy && !use_smart_proxy) {
                _main_panel.getView().getGlobal_speed_down_label().setForeground(new Color(0, 128, 255));
            }
        }

        _use_smart_proxy = use_smart_proxy;
    }

    public void setExit(boolean exit) {
        _exit = exit;
    }

    public void setProxy_list_url(String proxy_list_url) {

        _proxy_list_url = proxy_list_url;

        THREAD_POOL.execute(new Runnable() {
            @Override
            public void run() {

                synchronized (_refresh_lock) {

                    _refreshProxyList();
                }
            }
        });
    }

    public Object getRefresh_lock() {
        return _refresh_lock;
    }

    public String getFastestProxy() {

        return _proxy_list.peek();
    }

    public String getRandomProxy() {

        synchronized (_refresh_lock) {
            return _proxy_list.toArray(new String[_proxy_list.size()])[(new Random()).nextInt(_proxy_list.size())];
        }
    }

    public void excludeProxy(String proxy) {

        if (_proxy_list.contains(proxy)) {

            synchronized (_refresh_lock) {

                _proxy_list.remove(proxy);
            }

            _main_panel.getView().getSmart_proxy_status().setText("SmartProxy: " + _proxy_list.size() + " ");

            if (_proxy_list.isEmpty()) {

                THREAD_POOL.execute(new Runnable() {
                    @Override
                    public void run() {

                        synchronized (_refresh_lock) {

                            _refreshProxyList();
                        }
                    }
                });
            }
        }
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

                    for (String proxy : proxy_list) {

                        if (proxy.trim().matches(".+?:[0-9]{1,5}")) {
                            this._proxy_list.add(proxy);
                        }
                    }
                }

                Logger.getLogger(SmartMegaProxyManager.class.getName()).log(Level.INFO, "{0} Smart Proxy Manager: proxy list refreshed ({1})", new Object[]{Thread.currentThread().getName(), _proxy_list.size()});

                if (_use_smart_proxy) {

                    _use_smart_proxy = false;
                }

                _main_panel.getView().getSmart_proxy_status().setText("SmartProxy: " + _proxy_list.size() + " ");
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

        _main_panel.getView().getSmart_proxy_status().setVisible(true);

        while (!_exit) {

            synchronized (_refresh_lock) {

                this._refreshProxyList();

                try {
                    _refresh_lock.wait(1000 * REFRESH_PROXY_LIST_TIMEOUT);
                } catch (InterruptedException ex) {
                    Logger.getLogger(SmartMegaProxyManager.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }

        _main_panel.getView().getSmart_proxy_status().setText("");

        _main_panel.getView().getSmart_proxy_status().setVisible(false);

        Logger.getLogger(SmartMegaProxyManager.class.getName()).log(Level.INFO, "{0} Smart Proxy Manager: bye bye", new Object[]{Thread.currentThread().getName()});

    }

}
