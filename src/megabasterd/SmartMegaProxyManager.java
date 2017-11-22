package megabasterd;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author tonikelope
 */
public class SmartMegaProxyManager implements Runnable {

    public static final int TIMEOUT = 15;
    private final String _proxy_list_url;
    private final ConcurrentHashMap<String, HashMap> _proxy_list;

    public SmartMegaProxyManager(String proxy_list_url) {
        _proxy_list_url = proxy_list_url;
        _proxy_list = new ConcurrentHashMap<>();
    }

    public HashMap getRandomProxy() {

        return null;
    }

    @Override
    public void run() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
