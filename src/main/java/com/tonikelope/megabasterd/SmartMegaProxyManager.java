package com.tonikelope.megabasterd;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import static com.tonikelope.megabasterd.MainPanel.THREAD_POOL;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 *
 * @author tonikelope
 */
public class SmartMegaProxyManager implements Runnable {

    public static final int PROXY_TIMEOUT = 15;
    private volatile String _proxy_list_url;
    private final ConcurrentLinkedQueue<String> _proxy_list;
    private final MainPanel _main_panel;
    private volatile boolean _exit;

    public SmartMegaProxyManager(MainPanel main_panel, String proxy_list_url) {
        _main_panel = main_panel;
        _proxy_list_url = proxy_list_url;
        _proxy_list = new ConcurrentLinkedQueue<>();
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

        return _proxy_list.peek();
    }

    public void removeProxy(String proxy) {

        if (_proxy_list.contains(proxy)) {

            _proxy_list.remove(proxy);

            _main_panel.getView().updateSmartProxyStatus("SmartProxy: " + _proxy_list.size());

            Logger.getLogger(getClass().getName()).log(Level.INFO, "{0} Smart Proxy Manager: proxy removed -> {1}", new Object[]{Thread.currentThread().getName(), proxy});

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

    private void _refreshProxyList() {

        String data;

        try {

            if (this._proxy_list_url != null && this._proxy_list_url.length() > 0) {

                URL url = new URL(this._proxy_list_url);

                HttpURLConnection con = (HttpURLConnection) url.openConnection();

                con.setConnectTimeout(Transference.HTTP_TIMEOUT);

                con.setReadTimeout(Transference.HTTP_TIMEOUT);

                con.setRequestProperty("User-Agent", MainPanel.DEFAULT_USER_AGENT);

                InputStream is = con.getInputStream();

                try (ByteArrayOutputStream byte_res = new ByteArrayOutputStream()) {

                    byte[] buffer = new byte[MainPanel.DEFAULT_BYTE_BUFFER_SIZE];

                    int reads;

                    while ((reads = is.read(buffer)) != -1) {

                        byte_res.write(buffer, 0, reads);
                    }

                    data = new String(byte_res.toByteArray());
                }

                String[] proxy_list = data.split("\n");

                if (proxy_list.length > 0) {

                    _proxy_list.clear();

                    for (String proxy : proxy_list) {

                        if (proxy.trim().matches(".+?:[0-9]{1,5}")) {
                            _proxy_list.add(proxy);
                        }
                    }
                }

                _main_panel.getView().updateSmartProxyStatus("SmartProxy: " + _proxy_list.size());

                Logger.getLogger(getClass().getName()).log(Level.INFO, "{0} Smart Proxy Manager: proxy list refreshed ({1})", new Object[]{Thread.currentThread().getName(), _proxy_list.size()});
            }

        } catch (MalformedURLException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
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
