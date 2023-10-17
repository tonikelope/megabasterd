/*
 __  __                  _               _               _ 
|  \/  | ___  __ _  __ _| |__   __ _ ___| |_ ___ _ __ __| |
| |\/| |/ _ \/ _` |/ _` | '_ \ / _` / __| __/ _ \ '__/ _` |
| |  | |  __/ (_| | (_| | |_) | (_| \__ \ ||  __/ | | (_| |
|_|  |_|\___|\__, |\__,_|_.__/ \__,_|___/\__\___|_|  \__,_|
             |___/                                         
© Perpetrated by tonikelope since 2016
 */
package com.tonikelope.megabasterd;

import static com.tonikelope.megabasterd.MainPanel.THREAD_POOL;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author tonikelope
 */
public final class SmartMegaProxyManager {

    public static String DEFAULT_SMART_PROXY_URL = null;
    public static final int PROXY_BLOCK_TIME = 300;
    public static final int PROXY_AUTO_REFRESH_SLEEP_TIME = 30;
    private static final Logger LOG = Logger.getLogger(SmartMegaProxyManager.class.getName());
    private volatile String _proxy_list_url;
    private final LinkedHashMap<String, Long[]> _proxy_list;
    private static final HashMap<String, String> PROXY_LIST_AUTH = new HashMap<>();
    private final MainPanel _main_panel;
    private volatile int _ban_time;
    private volatile int _proxy_timeout;
    private volatile boolean _force_smart_proxy;

    public int getBan_time() {
        return _ban_time;
    }

    public int getProxy_timeout() {
        return _proxy_timeout;
    }

    public boolean isForce_smart_proxy() {
        return _force_smart_proxy;
    }

    public SmartMegaProxyManager(String proxy_list_url, MainPanel main_panel) {
        _proxy_list_url = (proxy_list_url != null && !"".equals(proxy_list_url)) ? proxy_list_url : DEFAULT_SMART_PROXY_URL;
        _proxy_list = new LinkedHashMap<>();
        _main_panel = main_panel;

        refreshSmartProxySettings();

        refreshProxyList();
    }

    public synchronized void refreshSmartProxySettings() {
        String smartproxy_ban_time = DBTools.selectSettingValue("smartproxy_ban_time");

        if (smartproxy_ban_time != null) {
            _ban_time = Integer.parseInt(smartproxy_ban_time);
        } else {
            _ban_time = PROXY_BLOCK_TIME;
        }

        String smartproxy_timeout = DBTools.selectSettingValue("smartproxy_timeout");

        if (smartproxy_timeout != null) {
            _proxy_timeout = Integer.parseInt(smartproxy_timeout) * 1000;
        } else {
            _proxy_timeout = Transference.HTTP_PROXY_TIMEOUT;
        }

        String force_smart_proxy_string = DBTools.selectSettingValue("force_smart_proxy");

        if (force_smart_proxy_string != null) {

            _force_smart_proxy = force_smart_proxy_string.equals("yes");
        } else {
            _force_smart_proxy = MainPanel.FORCE_SMART_PROXY;
        }

        LOG.log(Level.INFO, "SmartProxy BAN_TIME: " + String.valueOf(_ban_time) + " TIMEOUT: " + String.valueOf(_proxy_timeout / 1000) + " FORCE: " + String.valueOf(_force_smart_proxy));
    }

    public synchronized int getProxyCount() {

        return _proxy_list.size();
    }

    public synchronized String[] getProxy(ArrayList<String> excluded) {

        if (_proxy_list.size() > 0) {

            Set<String> keys = _proxy_list.keySet();

            List<String> keysList = new ArrayList<>(keys);

            Collections.shuffle(keysList);

            Long current_time = System.currentTimeMillis();

            for (String k : keysList) {

                if (_proxy_list.get(k)[0] < current_time && (excluded == null || !excluded.contains(k))) {

                    return new String[]{k, _proxy_list.get(k)[1] == -1L ? "http" : "socks"};
                }
            }
        }

        LOG.log(Level.WARNING, "{0} Smart Proxy Manager: NO PROXYS AVAILABLE!! (Refreshing in " + String.valueOf(PROXY_AUTO_REFRESH_SLEEP_TIME) + " secs...)", new Object[]{Thread.currentThread().getName()});

        try {
            Thread.sleep(PROXY_AUTO_REFRESH_SLEEP_TIME * 1000);
        } catch (InterruptedException ex) {
            Logger.getLogger(SmartMegaProxyManager.class.getName()).log(Level.SEVERE, null, ex);
        }

        refreshProxyList();

        return getProxyCount() > 0 ? getProxy(excluded) : null;
    }

    public synchronized void blockProxy(String proxy, String cause) {

        if (_proxy_list.containsKey(proxy)) {

            if (this._ban_time == 0) {

                _proxy_list.remove(proxy);

                LOG.log(Level.WARNING, "[Smart Proxy] REMOVING PROXY {0} ({1})", new Object[]{proxy, cause});

            } else {

                Long[] proxy_data = _proxy_list.get(proxy);

                proxy_data[0] = System.currentTimeMillis() + this._ban_time * 1000;

                _proxy_list.put(proxy, proxy_data);

                LOG.log(Level.WARNING, "[Smart Proxy] BLOCKING PROXY {0} ({1} secs) ({2})", new Object[]{proxy, _ban_time, cause});

            }
        }
    }

    public synchronized void refreshProxyList(String url_list) {
        if (url_list != null) {
            _proxy_list_url = url_list;
        } else {
            _proxy_list_url = null;
        }

        refreshProxyList();
    }

    public void refreshProxyList() {

        THREAD_POOL.execute(() -> {

            synchronized (this) {

                String data;

                HttpURLConnection con = null;

                try {

                    String custom_proxy_list = (_proxy_list_url == null ? DBTools.selectSettingValue("custom_proxy_list") : null);

                    LinkedHashMap<String, Long[]> custom_clean_list = new LinkedHashMap<>();

                    HashMap<String, String> custom_clean_list_auth = new HashMap<>();

                    if (custom_proxy_list != null) {

                        ArrayList<String> custom_list = new ArrayList<>(Arrays.asList(custom_proxy_list.split("\\r?\\n")));

                        if (!custom_list.isEmpty()) {

                            Long current_time = System.currentTimeMillis();

                            for (String proxy : custom_list) {

                                boolean socks = false;

                                if (proxy.trim().startsWith("*")) {
                                    socks = true;

                                    proxy = proxy.trim().substring(1);
                                }

                                if (proxy.trim().contains("@")) {

                                    String[] proxy_parts = proxy.trim().split("@");

                                    custom_clean_list_auth.put(proxy_parts[0], proxy_parts[1]);

                                    Long[] proxy_data = new Long[]{current_time, socks ? 1L : -1L};

                                    custom_clean_list.put(proxy_parts[0], proxy_data);

                                } else if (proxy.trim().matches(".+?:[0-9]{1,5}")) {

                                    Long[] proxy_data = new Long[]{current_time, socks ? 1L : -1L};

                                    custom_clean_list.put(proxy, proxy_data);
                                }
                            }
                        }

                        if (!custom_clean_list.isEmpty()) {

                            _proxy_list.clear();

                            _proxy_list.putAll(custom_clean_list);
                        }

                        if (!custom_clean_list_auth.isEmpty()) {

                            PROXY_LIST_AUTH.clear();

                            PROXY_LIST_AUTH.putAll(custom_clean_list_auth);
                        }

                    }

                    if (custom_clean_list.isEmpty() && _proxy_list_url != null && !"".equals(_proxy_list_url)) {

                        URL url = new URL(this._proxy_list_url);

                        con = (HttpURLConnection) url.openConnection();

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

                            PROXY_LIST_AUTH.clear();

                            Long current_time = System.currentTimeMillis();

                            for (String proxy : proxy_list) {

                                boolean socks = false;

                                if (proxy.trim().startsWith("*")) {
                                    socks = true;

                                    proxy = proxy.trim().substring(1);
                                }

                                if (proxy.trim().contains("@")) {

                                    String[] proxy_parts = proxy.trim().split("@");

                                    PROXY_LIST_AUTH.put(proxy_parts[0], proxy_parts[1]);

                                    Long[] proxy_data = new Long[]{current_time, socks ? 1L : -1L};

                                    _proxy_list.put(proxy_parts[0], proxy_data);

                                } else if (proxy.trim().matches(".+?:[0-9]{1,5}")) {
                                    Long[] proxy_data = new Long[]{current_time, socks ? 1L : -1L};
                                    _proxy_list.put(proxy, proxy_data);
                                }

                            }
                        }

                        _main_panel.getView().updateSmartProxyStatus("SmartProxy: ON (" + String.valueOf(getProxyCount()) + ")" + (this.isForce_smart_proxy() ? " F!" : ""));

                        LOG.log(Level.INFO, "{0} Smart Proxy Manager: proxy list refreshed ({1})", new Object[]{Thread.currentThread().getName(), _proxy_list.size()});

                    } else if (!custom_clean_list.isEmpty()) {

                        _main_panel.getView().updateSmartProxyStatus("SmartProxy: ON (" + String.valueOf(getProxyCount()) + ")" + (this.isForce_smart_proxy() ? " F!" : ""));

                        LOG.log(Level.INFO, "{0} Smart Proxy Manager: proxy list refreshed ({1})", new Object[]{Thread.currentThread().getName(), _proxy_list.size()});
                    } else {
                        _main_panel.getView().updateSmartProxyStatus("SmartProxy: ON (0 proxies!)" + (this.isForce_smart_proxy() ? " F!" : ""));
                        LOG.log(Level.INFO, "{0} Smart Proxy Manager: NO PROXYS");
                    }

                } catch (MalformedURLException ex) {
                    LOG.log(Level.SEVERE, ex.getMessage());
                } catch (IOException ex) {
                    LOG.log(Level.SEVERE, ex.getMessage());
                } finally {
                    if (con != null) {
                        con.disconnect();
                    }

                }
            }
        });

    }

    public static class SmartProxyAuthenticator extends Authenticator {

        @Override
        protected PasswordAuthentication getPasswordAuthentication() {

            InetAddress ipaddr = getRequestingSite();
            int port = getRequestingPort();

            String auth_data;

            if ((auth_data = PROXY_LIST_AUTH.get(ipaddr.getHostAddress() + ":" + String.valueOf(port))) != null) {

                try {
                    String[] auth_data_parts = auth_data.split(":");

                    String user = new String(MiscTools.BASE642Bin(auth_data_parts[0]), "UTF-8");

                    String password = new String(MiscTools.BASE642Bin(auth_data_parts[1]), "UTF-8");

                    return new PasswordAuthentication(user, password.toCharArray());

                } catch (UnsupportedEncodingException ex) {
                    Logger.getLogger(SmartMegaProxyManager.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

            return null;
        }
    }

}
