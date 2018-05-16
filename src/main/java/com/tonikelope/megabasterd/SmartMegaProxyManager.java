package com.tonikelope.megabasterd;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import static com.tonikelope.megabasterd.MainPanel.THREAD_POOL;
import static com.tonikelope.megabasterd.MiscTools.getApacheKissHttpClient;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;

/**
 *
 * @author tonikelope
 */
public class SmartMegaProxyManager implements Runnable {

    public static final int PROXY_TIMEOUT = 30;
    public static final int PROXY_MAX_EXCLUDE_COUNTER = 5;
    public static final int PROXY_EXCLUDE_SECS = 10;
    private volatile String _proxy_list_url;
    private final ConcurrentLinkedQueue<String> _proxy_list;
    private final ConcurrentHashMap<String, HashMap> _proxy_info;
    private final MainPanel _main_panel;
    private volatile boolean _exit;

    public SmartMegaProxyManager(MainPanel main_panel, String proxy_list_url) {
        _main_panel = main_panel;
        _proxy_list_url = proxy_list_url;
        _proxy_list = new ConcurrentLinkedQueue<>();
        _proxy_info = new ConcurrentHashMap<>();
        _exit = false;
    }

    public String getProxy_list_url() {
        return _proxy_list_url;
    }

    public void setExit(boolean exit) {
        _exit = exit;
    }

    public void setProxy_list_url(String proxy_list_url) {

        _proxy_list_url = proxy_list_url;

        THREAD_POOL.execute(new Runnable() {
            @Override
            public void run() {

                _refreshProxyList();
            }
        });
    }

    public String getFastestProxy() {

        for (String proxy : _proxy_list) {

            HashMap<String, Object> proxy_info = (HashMap<String, Object>) _proxy_info.get(proxy);

            Long extimestamp = (Long) proxy_info.get("extimestamp");

            if (extimestamp == null || extimestamp + PROXY_EXCLUDE_SECS * 1000 < System.currentTimeMillis()) {

                return proxy;

            } else {
                Logger.getLogger(getClass().getName()).log(Level.INFO, "{0} Smart Proxy Manager: proxy is temporary excluded -> {1}", new Object[]{Thread.currentThread().getName(), proxy});
            }
        }

        return null;
    }

    public void excludeProxy(String proxy) {

        if (_proxy_info.containsKey(proxy)) {

            HashMap<String, Object> proxy_info = (HashMap<String, Object>) _proxy_info.get(proxy);

            int excount = (int) proxy_info.get("excount") + 1;

            if (excount < PROXY_MAX_EXCLUDE_COUNTER) {

                proxy_info.put("excount", excount);

                proxy_info.put("extimestamp", System.currentTimeMillis());

            } else {

                Logger.getLogger(getClass().getName()).log(Level.INFO, "{0} Smart Proxy Manager: proxy removed -> {1}", new Object[]{Thread.currentThread().getName(), proxy});

                _proxy_list.remove(proxy);
                _proxy_info.remove(proxy);

                _main_panel.getView().updateSmartProxyStatus("SmartProxy: " + _proxy_list.size());

                if (_proxy_list.isEmpty()) {

                    THREAD_POOL.execute(new Runnable() {
                        @Override
                        public void run() {

                            _refreshProxyList();
                        }
                    });
                }
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
                    _proxy_info.clear();

                    for (String proxy : proxy_list) {

                        if (proxy.trim().matches(".+?:[0-9]{1,5}")) {
                            _proxy_list.add(proxy);
                            HashMap<String, Object> proxy_info = new HashMap<>();
                            proxy_info.put("extimestamp", null);
                            proxy_info.put("excount", 0);
                            _proxy_info.put(proxy.trim(), proxy_info);
                        }
                    }
                }

                Logger.getLogger(getClass().getName()).log(Level.INFO, "{0} Smart Proxy Manager: proxy list refreshed ({1})", new Object[]{Thread.currentThread().getName(), _proxy_list.size()});

                _main_panel.getView().updateSmartProxyStatus("SmartProxy: " + _proxy_list.size());
            }

        } catch (MalformedURLException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
        } catch (IOException | URISyntaxException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void run() {

        Logger.getLogger(getClass().getName()).log(Level.INFO, "{0} Smart Proxy Manager: hello!", new Object[]{Thread.currentThread().getName()});

        _main_panel.getView().updateSmartProxyStatus("");

        this._refreshProxyList();

        while (!_exit) {

            this._refreshProxyList();

            try {
                synchronized (this) {
                    wait();
                }
            } catch (InterruptedException ex) {
                Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
            }
        }

        _main_panel.getView().updateSmartProxyStatus("");

        Logger.getLogger(getClass().getName()).log(Level.INFO, "{0} Smart Proxy Manager: bye bye", new Object[]{Thread.currentThread().getName()});

    }

}
