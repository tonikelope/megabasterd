package com.tonikelope.megabasterd;

import static com.tonikelope.megabasterd.MiscTools.swingInvoke;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author tonikelope
 */
public final class SmartMegaProxyManager {

    public static String DEFAULT_SMART_PROXY_URL = "https://raw.githubusercontent.com/tonikelope/megabasterd/proxy_list/proxy_list.txt";
    public static final int BLOCK_TIME = 60;
    private volatile String _proxy_list_url;
    private final LinkedHashMap<String, Long> _proxy_list;
    private final MainPanel _main_panel;

    public SmartMegaProxyManager(String proxy_list_url, MainPanel main_panel) {
        _proxy_list_url = (proxy_list_url != null && !"".equals(proxy_list_url)) ? proxy_list_url : DEFAULT_SMART_PROXY_URL;
        _proxy_list = new LinkedHashMap<>();
        _main_panel = main_panel;
        refreshProxyList();
    }

    public synchronized int getProxyCount() {

        return _proxy_list.size();
    }

    public synchronized String getFastestProxy() {

        Set<String> keys = _proxy_list.keySet();

        Long current_time = System.currentTimeMillis();

        for (String k : keys) {

            if (_proxy_list.get(k) < current_time) {

                return k;
            }
        }

        LOG.log(Level.WARNING, "{0} Smart Proxy Manager: NO PROXYS AVAILABLE!!", new Object[]{Thread.currentThread().getName()});

        refreshProxyList();

        return getProxyCount() > 0 ? getFastestProxy() : null;
    }

    public synchronized void blockProxy(String proxy) {

        if (_proxy_list.containsKey(proxy)) {

            _proxy_list.put(proxy, System.currentTimeMillis() + BLOCK_TIME * 1000);
        }
    }

    public synchronized void refreshProxyList() {

        String data;

        HttpURLConnection con = null;

        try {

            if (this._proxy_list_url != null && this._proxy_list_url.length() > 0) {

                URL url = new URL(this._proxy_list_url);

                con = (HttpURLConnection) url.openConnection();

                con.setConnectTimeout(Transference.HTTP_TIMEOUT);

                con.setReadTimeout(Transference.HTTP_TIMEOUT);

                con.setUseCaches(false);

                con.setRequestProperty("User-Agent", MainPanel.DEFAULT_USER_AGENT);

                try (InputStream is = con.getInputStream(); ByteArrayOutputStream byte_res = new ByteArrayOutputStream()) {

                    byte[] buffer = new byte[MainPanel.DEFAULT_BYTE_BUFFER_SIZE];

                    int reads;

                    while ((reads = is.read(buffer)) != -1) {

                        byte_res.write(buffer, 0, reads);
                    }

                    data = new String(byte_res.toByteArray(), "UTF-8");
                }

                String[] proxy_list = data.split("\n");

                if (proxy_list.length > 0) {

                    _proxy_list.clear();

                    Long current_time = System.currentTimeMillis();

                    for (String proxy : proxy_list) {

                        if (proxy.trim().matches(".+?:[0-9]{1,5}")) {
                            _proxy_list.put(proxy, current_time);
                        }
                    }
                }

                swingInvoke(
                        new Runnable() {
                    @Override
                    public void run() {
                        _main_panel.getView().updateSmartProxyStatus("SmartProxy: ON (" + String.valueOf(getProxyCount()) + ")");
                    }
                });

                LOG.log(Level.INFO, "{0} Smart Proxy Manager: proxy list refreshed ({1})", new Object[]{Thread.currentThread().getName(), _proxy_list.size()});
            }

        } catch (MalformedURLException ex) {
            LOG.log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, null, ex);
        } finally {
            if (con != null) {
                con.disconnect();
            }

        }
    }
    private static final Logger LOG = Logger.getLogger(SmartMegaProxyManager.class.getName());

}
