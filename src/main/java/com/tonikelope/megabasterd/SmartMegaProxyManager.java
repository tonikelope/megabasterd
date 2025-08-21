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

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.tonikelope.megabasterd.MainPanel.THREAD_POOL; 

/**
 *
 * @author tonikelope
 */
public final class SmartMegaProxyManager {

    public static String DEFAULT_SMART_PROXY_URL = null;
    public static final int PROXY_BLOCK_TIME = 300;
    public static final int PROXY_AUTO_REFRESH_TIME = 60;
    public static final int PROXY_AUTO_REFRESH_SLEEP_TIME = 30;
    public static final boolean RESET_SLOT_PROXY = true;
    public static final boolean RANDOM_SELECT = true;

    private static final Logger LOG = LogManager.getLogger(SmartMegaProxyManager.class);
    private volatile String _proxy_list_url;
    private final ConcurrentHashMap<String, Long[]> _proxy_list;
    private static final HashMap<String, String> PROXY_LIST_AUTH = new HashMap<>();
    private final MainPanel _main_panel;
    private volatile int _ban_time;
    private volatile int _proxy_timeout;
    private volatile boolean _force_smart_proxy;
    private volatile int _autorefresh_time;
    private volatile long _last_refresh_timestamp;
    private volatile boolean _random_select;
    private volatile boolean _reset_slot_proxy;

    public boolean isRandom_select() {
        return _random_select;
    }

    public boolean isReset_slot_proxy() {
        return _reset_slot_proxy;
    }

    public int getProxy_timeout() {
        return _proxy_timeout;
    }

    public boolean isForce_smart_proxy() {
        return _force_smart_proxy;
    }

    public SmartMegaProxyManager(String proxy_list_url, MainPanel main_panel) {
        _proxy_list_url = (proxy_list_url != null && !"".equals(proxy_list_url)) ? proxy_list_url : DEFAULT_SMART_PROXY_URL;
        _proxy_list = new ConcurrentHashMap<>();
        _main_panel = main_panel;

        refreshSmartProxySettings();

        THREAD_POOL.execute(() -> {
            refreshProxyList();

            while (true) {

                while (System.currentTimeMillis() < _last_refresh_timestamp + _autorefresh_time * 60 * 1000) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ex) {
                        LOG.fatal("SmartProxyManager interrupted!", ex);
                    }
                }

                if (MainPanel.isUse_smart_proxy()) {

                    refreshProxyList();
                }
            }
        });
    }

    private synchronized int countBlockedProxies() {

        int i = 0;

        Long current_time = System.currentTimeMillis();

        for (String k : _proxy_list.keySet()) {

            if (_proxy_list.get(k)[0] != -1 && _proxy_list.get(k)[0] > current_time - _ban_time * 1000) {

                i++;
            }
        }

        return i;

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

        String autorefresh_smart_proxy_string = DBTools.selectSettingValue("smartproxy_autorefresh_time");

        if (autorefresh_smart_proxy_string != null) {
            _autorefresh_time = Integer.parseInt(autorefresh_smart_proxy_string);
        } else {
            _autorefresh_time = PROXY_AUTO_REFRESH_TIME;
        }

        String reset_slot_proxy = DBTools.selectSettingValue("reset_slot_proxy");

        if (reset_slot_proxy != null) {

            _reset_slot_proxy = reset_slot_proxy.equals("yes");
        } else {
            _reset_slot_proxy = RESET_SLOT_PROXY;
        }

        String random_select = DBTools.selectSettingValue("random_proxy");

        if (random_select != null) {

            _random_select = random_select.equals("yes");
        } else {
            _random_select = RANDOM_SELECT;
        }

        LOG.info("SmartProxy BAN_TIME: " + _ban_time + "   TIMEOUT: " + _proxy_timeout / 1000 + "   REFRESH: " + _autorefresh_time + "   FORCE: " + _force_smart_proxy + "   RANDOM: " + _random_select + "   RESET-SLOT-PROXY: " + _reset_slot_proxy);
    }

    public synchronized int getProxyCount() {

        return _proxy_list.size();
    }

    public synchronized String[] getProxy(ArrayList<String> excluded) {

        if (!_proxy_list.isEmpty()) {

            Set<String> keys = _proxy_list.keySet();

            List<String> keysList = new ArrayList<>(keys);

            if (isRandom_select()) {
                Collections.shuffle(keysList);
            }

            long current_time = System.currentTimeMillis();

            for (String k : keysList) {

                if ((_proxy_list.get(k)[0] == -1 || _proxy_list.get(k)[0] < current_time - _ban_time * 1000L) && (excluded == null || !excluded.contains(k))) {

                    return new String[]{k, _proxy_list.get(k)[1] == -1L ? "http" : "socks"};
                }
            }
        }

        LOG.warn("Smart Proxy Manager: NO PROXIES AVAILABLE!! (Refreshing in " + PROXY_AUTO_REFRESH_SLEEP_TIME + " secs...)");

        try {
            Thread.sleep(PROXY_AUTO_REFRESH_SLEEP_TIME * 1000);
        } catch (InterruptedException ex) {
            LOG.fatal("Auto-refresh sleep interrupted!", ex);
        }

        refreshProxyList();

        return getProxyCount() > 0 ? getProxy(excluded) : null;
    }

    public synchronized void blockProxy(String proxy, String cause) {

        if (_proxy_list.containsKey(proxy)) {

            if (this._ban_time == 0) {

                _proxy_list.remove(proxy);

                LOG.warn("[Smart Proxy] REMOVING PROXY {} ({})", proxy, cause);

            } else {

                Long[] proxy_data = _proxy_list.get(proxy);

                proxy_data[0] = System.currentTimeMillis();

                _proxy_list.put(proxy, proxy_data);

                LOG.warn("[Smart Proxy] BLOCKING PROXY {} ({} secs) ({})", proxy, _ban_time, cause);

            }

            _main_panel.getView().updateSmartProxyStatus("SmartProxy: ON (" + (getProxyCount() - countBlockedProxies()) + ")" + (this.isForce_smart_proxy() ? " F!" : ""));

        }
    }

    public synchronized void refreshProxyList(String url_list) {
        _proxy_list_url = url_list;
        THREAD_POOL.execute(this::refreshProxyList);
    }

    public synchronized void refreshProxyList() {

        String data;

        HttpURLConnection con = null;

        try {

            String custom_proxy_list = (_proxy_list_url == null ? DBTools.selectSettingValue("custom_proxy_list") : null);

            LinkedHashMap<String, Long[]> custom_clean_list = new LinkedHashMap<>();

            HashMap<String, String> custom_clean_list_auth = new HashMap<>();

            if (custom_proxy_list != null) {

                ArrayList<String> custom_list = new ArrayList<>(Arrays.asList(custom_proxy_list.split("\\r?\\n")));

                if (!custom_list.isEmpty()) {

                    for (String proxy : custom_list) {

                        boolean socks = false;

                        if (proxy.trim().startsWith("*")) {
                            socks = true;

                            proxy = proxy.trim().substring(1);
                        }

                        if (proxy.trim().contains("@")) {

                            String[] proxy_parts = proxy.trim().split("@");

                            custom_clean_list_auth.put(proxy_parts[0], proxy_parts[1]);

                            Long[] proxy_data = new Long[]{-1L, socks ? 1L : -1L};

                            custom_clean_list.put(proxy_parts[0], proxy_data);

                        } else if (proxy.trim().matches(".+?:[0-9]{1,5}")) {

                            Long[] proxy_data = new Long[]{-1L, socks ? 1L : -1L};

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

                    for (String proxy : proxy_list) {

                        boolean socks = false;

                        if (proxy.trim().startsWith("*")) {
                            socks = true;

                            proxy = proxy.trim().substring(1);
                        }

                        if (proxy.trim().contains("@")) {

                            String[] proxy_parts = proxy.trim().split("@");

                            PROXY_LIST_AUTH.put(proxy_parts[0], proxy_parts[1]);

                            Long[] proxy_data = new Long[]{-1L, socks ? 1L : -1L};

                            _proxy_list.put(proxy_parts[0], proxy_data);

                        } else if (proxy.trim().matches(".+?:[0-9]{1,5}")) {
                            Long[] proxy_data = new Long[]{-1L, socks ? 1L : -1L};
                            _proxy_list.put(proxy, proxy_data);
                        }

                    }
                }

                _main_panel.getView().updateSmartProxyStatus("SmartProxy: ON (" + getProxyCount() + ")" + (this.isForce_smart_proxy() ? " F!" : ""));
                LOG.info("Smart Proxy Manager: proxy list refreshed ({})", _proxy_list.size());
            } else if (!custom_clean_list.isEmpty()) {
                _main_panel.getView().updateSmartProxyStatus("SmartProxy: ON (" + getProxyCount() + ")" + (this.isForce_smart_proxy() ? " F!" : ""));
                LOG.info("Smart Proxy Manager: proxy list refreshed ({})", _proxy_list.size());
            } else {
                _main_panel.getView().updateSmartProxyStatus("SmartProxy: ON (0 proxies!)" + (this.isForce_smart_proxy() ? " F!" : ""));
                LOG.info("Smart Proxy Manager: NO PROXIES");
            }

        } catch (IOException ex) {
            LOG.fatal("IO Exception refreshing proxy list! {}", ex.getMessage());
        } finally {
            if (con != null) con.disconnect();
        }

        _last_refresh_timestamp = System.currentTimeMillis();

    }

    public static class SmartProxyAuthenticator extends Authenticator {

        @Override
        protected PasswordAuthentication getPasswordAuthentication() {

            InetAddress ipaddr = getRequestingSite();
            int port = getRequestingPort();

            String auth_data;

            if ((auth_data = PROXY_LIST_AUTH.get(ipaddr.getHostAddress() + ":" + port)) != null) {
                String[] auth_data_parts = auth_data.split(":");
                String user = new String(MiscTools.BASE642Bin(auth_data_parts[0]), StandardCharsets.UTF_8);
                String password = new String(MiscTools.BASE642Bin(auth_data_parts[1]), StandardCharsets.UTF_8);
                return new PasswordAuthentication(user, password.toCharArray());
            }

            return null;
        }
    }

}
