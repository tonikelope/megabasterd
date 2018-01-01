package megabasterd;

import java.awt.Component;
import java.awt.Container;
import java.awt.Desktop;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.management.ManagementFactory;
import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.xml.bind.DatatypeConverter;
import static megabasterd.MainPanel.VERSION;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthScope;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.RequestAddCookies;
import org.apache.http.client.protocol.RequestAuthCache;
import org.apache.http.client.protocol.RequestClientConnControl;
import org.apache.http.client.protocol.RequestDefaultHeaders;
import org.apache.http.client.protocol.ResponseProcessCookies;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.RequestContent;
import org.apache.http.protocol.RequestTargetHost;

/**
 *
 * @author tonikelope
 */
public final class MiscTools {

    public static final int EXP_BACKOFF_BASE = 2;
    public static final int EXP_BACKOFF_SECS_RETRY = 1;
    public static final int EXP_BACKOFF_MAX_WAIT_TIME = 16;
    public static final Object PASS_LOCK = new Object();
    public static final int HTTP_TIMEOUT = 30;
    private static final Comparator<DefaultMutableTreeNode> TREE_NODE_COMPARATOR = new Comparator< DefaultMutableTreeNode>() {

        @Override
        public int compare(DefaultMutableTreeNode a, DefaultMutableTreeNode b) {

            if (a.isLeaf() && !b.isLeaf()) {
                return 1;
            } else if (!a.isLeaf() && b.isLeaf()) {
                return -1;
            } else {
                String sa = a.getUserObject().toString();
                String sb = b.getUserObject().toString();
                return sa.compareToIgnoreCase(sb);
            }
        }
    };

    public static Font createAndRegisterFont(String name) {

        Font font = null;

        try {

            font = Font.createFont(Font.TRUETYPE_FONT, MiscTools.class.getResourceAsStream(name));

            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();

            ge.registerFont(font);

        } catch (FontFormatException | IOException ex) {
            Logger.getLogger(MiscTools.class.getName()).log(Level.SEVERE, null, ex);
        }

        return font;
    }

    public static void setNimbusLookAndFeel() {

        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(MiscTools.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
    }

    public static int[] bin2i32a(byte[] bin) {
        int l = (int) (4 * Math.ceil((double) bin.length / 4));

        byte[] new_bin = Arrays.copyOfRange(bin, 0, l);

        bin = new_bin;

        ByteBuffer bin_buffer = ByteBuffer.wrap(bin);
        IntBuffer int_buffer = bin_buffer.asIntBuffer();

        if (int_buffer.hasArray()) {
            return int_buffer.array();
        } else {
            ArrayList<Integer> list = new ArrayList<>();

            while (int_buffer.hasRemaining()) {
                list.add(int_buffer.get());
            }

            int[] aux = new int[list.size()];

            for (int i = 0; i < aux.length; i++) {
                aux[i] = list.get(i);
            }

            return aux;
        }
    }

    public static byte[] i32a2bin(int[] i32a) {
        ByteBuffer bin_buffer = ByteBuffer.allocate(i32a.length * 4);
        IntBuffer int_buffer = bin_buffer.asIntBuffer();
        int_buffer.put(i32a);

        if (bin_buffer.hasArray()) {
            return bin_buffer.array();
        } else {
            ArrayList<Byte> list = new ArrayList<>();

            while (int_buffer.hasRemaining()) {
                list.add(bin_buffer.get());
            }

            byte[] aux = new byte[list.size()];

            for (int i = 0; i < aux.length; i++) {
                aux[i] = list.get(i);
            }

            return aux;
        }
    }

    public static BigInteger mpi2big(byte[] s) {

        byte[] ns = Arrays.copyOfRange(s, 2, s.length);

        BigInteger bigi = new BigInteger(1, ns);

        return bigi;
    }

    public static String genID(int length) {

        String pos = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

        String res = "";

        Random randomno = new Random();

        for (int i = 0; i < length; i++) {

            res += pos.charAt(randomno.nextInt(pos.length()));
        }

        return res;
    }

    public static byte[] long2bytearray(long val) {

        byte[] b = new byte[8];

        for (int i = 7; i >= 0; i--) {
            b[i] = (byte) val;
            val >>>= 8;
        }

        return b;
    }

    public static long bytearray2long(byte[] val) {

        long l = 0;

        for (int i = 0; i <= 7; i++) {
            l += val[i];
            l <<= 8;
        }

        return l;
    }

    public static String findFirstRegex(String regex, String data, int group) {
        Pattern pattern = Pattern.compile(regex, Pattern.DOTALL);

        Matcher matcher = pattern.matcher(data);

        return matcher.find() ? matcher.group(group) : null;
    }

    public static ArrayList<String> findAllRegex(String regex, String data, int group) {
        Pattern pattern = Pattern.compile(regex, Pattern.DOTALL);

        Matcher matcher = pattern.matcher(data);

        ArrayList<String> matches = new ArrayList<>();

        while (matcher.find()) {
            matches.add(matcher.group(group));
        }

        return matches;
    }

    public static void updateFonts(final Component component, final Font font, final float zoom_factor) {

        if (component != null) {

            if (component instanceof javax.swing.JMenu) {

                for (Component child : ((javax.swing.JMenu) component).getMenuComponents()) {
                    if (child instanceof Container) {

                        updateFonts(child, font, zoom_factor);
                    }
                }

            } else if (component instanceof Container) {

                for (Component child : ((Container) component).getComponents()) {
                    if (child instanceof Container) {

                        updateFonts(child, font, zoom_factor);
                    }
                }
            }

            Font old_font = component.getFont();

            Font new_font = font.deriveFont(old_font.getStyle(), (float) Math.floor(old_font.getSize() * zoom_factor));

            component.setFont(new_font);
        }
    }

    public static String HashString(String algo, String data) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        MessageDigest md = MessageDigest.getInstance(algo);

        byte[] thedigest = md.digest(data.getBytes("UTF-8"));

        return bin2hex(thedigest);
    }

    public static String HashString(String algo, byte[] data) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance(algo);

        byte[] thedigest = md.digest(data);

        return bin2hex(thedigest);
    }

    public static byte[] HashBin(String algo, String data) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        MessageDigest md = MessageDigest.getInstance(algo);

        return md.digest(data.getBytes("UTF-8"));
    }

    public static byte[] HashBin(String algo, byte[] data) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance(algo);

        return md.digest(data);
    }

    public static byte[] BASE642Bin(String data) {
        return Base64.getDecoder().decode(data);
    }

    public static String Bin2BASE64(byte[] data) {
        return Base64.getEncoder().encodeToString(data);
    }

    public static byte[] UrlBASE642Bin(String data) {
        return Base64.getUrlDecoder().decode(data);
    }

    public static String Bin2UrlBASE64(byte[] data) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
    }

    public static long getWaitTimeExpBackOff(int retryCount) {

        long waitTime = ((long) Math.pow(EXP_BACKOFF_BASE, retryCount) * EXP_BACKOFF_SECS_RETRY);

        return Math.min(waitTime, EXP_BACKOFF_MAX_WAIT_TIME);
    }

    public static void swingInvoke(Runnable r) {

        _swingInvokeIt(r, false);
    }

    public static void swingInvokeAndWait(Runnable r) {

        _swingInvokeIt(r, true);
    }

    public static Object swingInvokeAndWaitForReturn(Callable c) {

        return _swingInvokeItAndWaitForReturn(c);
    }

    private static void _swingInvokeIt(Runnable r, boolean wait) {

        if (wait) {

            if (SwingUtilities.isEventDispatchThread()) {

                r.run();

            } else {

                try {
                    /* OJO!!! El thread que lanza esto NO PUEDE poseer locks que necesite el EDT o se producirá un DEADLOCK */
                    SwingUtilities.invokeAndWait(r);

                } catch (InterruptedException | InvocationTargetException ex) {
                    Logger.getLogger(MiscTools.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

        } else {

            SwingUtilities.invokeLater(r);
        }
    }

    private static Object _swingInvokeItAndWaitForReturn(Callable c) {
        Object ret = null;

        if (SwingUtilities.isEventDispatchThread()) {

            try {
                ret = c.call();
            } catch (Exception ex) {
                Logger.getLogger(MiscTools.class.getName()).log(Level.SEVERE, null, ex);
            }

        } else {

            FutureTask<Object> futureTask = new FutureTask<>(c);

            SwingUtilities.invokeLater(futureTask);

            try {
                ret = futureTask.get();
            } catch (InterruptedException | ExecutionException ex) {
                Logger.getLogger(MiscTools.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        return ret;
    }

    public static String bin2hex(byte[] b) {

        BigInteger bi = new BigInteger(1, b);

        return String.format("%0" + (b.length << 1) + "x", bi);
    }

    public static byte[] hex2bin(String s) {
        return DatatypeConverter.parseHexBinary(s);
    }

    public static void copyTextToClipboard(String text) {

        StringSelection stringSelection = new StringSelection(text);
        Clipboard clpbrd = Toolkit.getDefaultToolkit().getSystemClipboard();
        clpbrd.setContents(stringSelection, null);

    }

    public static String deflateURL(String link) throws MalformedURLException, IOException {

        String response = null;

        try (CloseableHttpClient httpclient = getApacheKissHttpClient()) {

            HttpGet httpget = new HttpGet(new URI("http://tinyurl.com/api-create.php?url=" + URLEncoder.encode(link.trim(), "UTF-8")));

            httpget.addHeader("Custom-User-Agent", MainPanel.DEFAULT_USER_AGENT);

            try (CloseableHttpResponse httpresponse = httpclient.execute(httpget)) {

                InputStream is = httpresponse.getEntity().getContent();

                try (ByteArrayOutputStream byte_res = new ByteArrayOutputStream()) {

                    byte[] buffer = new byte[MainPanel.DEFAULT_BYTE_BUFFER_SIZE];

                    int reads;

                    while ((reads = is.read(buffer)) != -1) {

                        byte_res.write(buffer, 0, reads);
                    }

                    response = new String(byte_res.toByteArray()).trim();

                }
            }

        } catch (MalformedURLException ex) {
            Logger.getLogger(MiscTools.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException | URISyntaxException ex) {
            Logger.getLogger(MiscTools.class.getName()).log(Level.SEVERE, null, ex);
        }

        return findFirstRegex("http", response, 0) != null ? response : link;
    }

    public static String formatBytes(Long bytes) {

        String[] units = {"B", "KB", "MB", "GB", "TB"};

        bytes = Math.max(bytes, 0L);

        int pow = Math.min((int) ((bytes > 0L ? Math.log(bytes) : 0) / Math.log(1024)), units.length - 1);

        Double bytes_double = (double) bytes / (1 << (10 * pow));

        DecimalFormat df = new DecimalFormat("#.##");

        return df.format(bytes_double) + ' ' + units[pow];
    }

    public static DefaultMutableTreeNode sortTree(DefaultMutableTreeNode root) {

        Enumeration e = root.depthFirstEnumeration();

        while (e.hasMoreElements()) {

            DefaultMutableTreeNode node = (DefaultMutableTreeNode) e.nextElement();

            if (!node.isLeaf()) {

                _sortTreeNode(node);

            }
        }

        return root;

    }

    private static void _sortTreeNode(DefaultMutableTreeNode parent) {

        int n = parent.getChildCount();

        List<DefaultMutableTreeNode> children = new ArrayList<>(n);

        for (int i = 0; i < n; i++) {

            children.add((DefaultMutableTreeNode) parent.getChildAt(i));
        }

        Collections.sort(children, TREE_NODE_COMPARATOR);

        parent.removeAllChildren();

        for (DefaultMutableTreeNode node : children) {

            parent.add(node);

        }
    }

    public static boolean deleteSelectedTreeItems(JTree tree) {

        TreePath[] paths = tree.getSelectionPaths();

        if (paths != null) {

            DefaultTreeModel tree_model = (DefaultTreeModel) tree.getModel();

            MutableTreeNode node;

            for (TreePath path : paths) {

                node = (MutableTreeNode) path.getLastPathComponent();

                if (node != null) {

                    if (node != tree_model.getRoot()) {

                        MutableTreeNode parent = (MutableTreeNode) node.getParent();

                        tree_model.removeNodeFromParent(node);

                        while (parent != null && parent.isLeaf()) {

                            if (parent != tree_model.getRoot()) {

                                MutableTreeNode parent_aux = (MutableTreeNode) parent.getParent();

                                tree_model.removeNodeFromParent(parent);

                                parent = parent_aux;

                            } else {

                                parent = null;
                            }
                        }

                    } else {

                        MutableTreeNode new_root;

                        try {

                            new_root = (MutableTreeNode) tree_model.getRoot().getClass().newInstance();

                            tree.setModel(new DefaultTreeModel(new_root));

                            tree.setRootVisible(new_root.getChildCount() > 0);

                            tree.setEnabled(true);

                        } catch (InstantiationException | IllegalAccessException ex) {
                            Logger.getLogger(MiscTools.class.getName()).log(Level.SEVERE, null, ex);
                        }

                        return true;
                    }
                }
            }

            tree.setRootVisible(((TreeNode) tree_model.getRoot()).getChildCount() > 0);
            tree.setEnabled(true);

            return true;
        }

        return false;
    }

    public static boolean deleteAllExceptSelectedTreeItems(JTree tree) {

        TreePath[] paths = tree.getSelectionPaths();

        HashMap<MutableTreeNode, MutableTreeNode> hashmap_old = new HashMap<>();

        DefaultTreeModel tree_model = (DefaultTreeModel) tree.getModel();

        if (paths != null) {

            Class node_class = tree_model.getRoot().getClass();

            Object new_root = null;

            try {

                new_root = node_class.newInstance();

                ((MutableTreeNode) new_root).setUserObject(((DefaultMutableTreeNode) tree_model.getRoot()).getUserObject());

            } catch (InstantiationException | IllegalAccessException ex) {
                Logger.getLogger(MiscTools.class.getName()).log(Level.SEVERE, null, ex);
            }

            for (TreePath path : paths) {

                if ((MutableTreeNode) path.getLastPathComponent() != (MutableTreeNode) tree_model.getRoot()) {
                    Object parent = new_root;

                    for (Object path_element : path.getPath()) {

                        if ((MutableTreeNode) path_element != (MutableTreeNode) tree_model.getRoot()) {

                            if (hashmap_old.get(path_element) == null) {

                                Object node = null;

                                if ((MutableTreeNode) path_element == (MutableTreeNode) path.getLastPathComponent()) {

                                    node = path_element;

                                } else {

                                    try {

                                        node = node_class.newInstance();

                                        ((MutableTreeNode) node).setUserObject(((DefaultMutableTreeNode) path_element).getUserObject());

                                    } catch (InstantiationException | IllegalAccessException ex) {
                                        Logger.getLogger(MiscTools.class.getName()).log(Level.SEVERE, null, ex);
                                    }
                                }

                                if (parent != null) {

                                    ((DefaultMutableTreeNode) parent).add((MutableTreeNode) node);

                                    if (!((TreeNode) path_element).isLeaf()) {

                                        hashmap_old.put((MutableTreeNode) path_element, (MutableTreeNode) node);

                                        parent = node;
                                    }
                                }

                            } else {

                                parent = hashmap_old.get(path_element);
                            }
                        }
                    }

                } else {

                    return false;
                }
            }

            tree.setModel(new DefaultTreeModel(sortTree((DefaultMutableTreeNode) new_root)));

            tree.setRootVisible(new_root != null ? ((TreeNode) new_root).getChildCount() > 0 : false);

            tree.setEnabled(true);

            return true;
        }

        return false;
    }

    public static String truncateText(String text, int max_length) {

        String separator = " ... ";

        max_length -= separator.length();

        if (max_length % 2 != 0) {

            max_length--;
        }

        return (text.length() > max_length) ? text.replaceAll("^(.{1," + (max_length / 2) + "}).*?(.{1," + (max_length / 2) + "})$", "$1" + separator + "$2") : text;
    }

    public static String cleanFilename(String filename) {

        return (System.getProperty("os.name").toLowerCase().contains("win") ? filename.replaceAll("[<>:\"/\\\\\\|\\?\\*]+", "").replaceAll("[ \\.]{1,}/{1,}", "/") : filename).replaceAll("[\\.]{1,}$", "").trim();
    }

    public static String cleanFilePath(String path) {

        return !path.equals(".") ? ((System.getProperty("os.name").toLowerCase().contains("win") ? path.replaceAll("[<>:\"\\|\\?\\*]+", "").replaceAll("[ \\.]{1,}/{1,}", "/") : path).replaceAll("[\\.]{1,}$", "").trim()) : path;
    }

    public static byte[] genRandomByteArray(int length) {

        byte[] the_array = new byte[length];

        Random randomno = new Random();

        randomno.nextBytes(the_array);

        return the_array;
    }

    public static String extractStringFromClipboardContents(Transferable contents) {

        String ret = null;

        if (contents != null) {

            try {

                Object o = contents.getTransferData(DataFlavor.stringFlavor);

                if (o instanceof String) {

                    ret = (String) o;
                }

            } catch (Exception ex) {
            }
        }

        return ret;

    }

    public static String extractMegaLinksFromString(String data) {

        String res = "";

        if (data != null) {

            try {
                ArrayList<String> links = findAllRegex("(?:https?|mega)://[^/\r\n]+/(#[^\r\n!]*?)?![^\r\n!]+![^\r\n]+", URLDecoder.decode(data, "UTF-8"), 0);

                links.addAll(findAllRegex("mega://e(n|l)c[^\r\n]+", URLDecoder.decode(data, "UTF-8"), 0));

                for (String s : links) {

                    res += s + "\n";
                }
            } catch (UnsupportedEncodingException ex) {
                Logger.getLogger(MiscTools.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        return res.trim();
    }

    public static String extractFirstMegaLinkFromString(String data) {

        String res = "";

        if (data != null) {

            try {
                ArrayList<String> links = findAllRegex("(?:https?|mega)://[^/\r\n]+/(#[^\r\n!]*?)?![^\r\n!]+![^\r\n]+", URLDecoder.decode(data, "UTF-8"), 0);

                links.addAll(findAllRegex("mega://e(n|l)c[^\r\n]+", URLDecoder.decode(data, "UTF-8"), 0));

                if (links.size() > 0) {

                    res = links.get(0);
                }
            } catch (UnsupportedEncodingException ex) {
                Logger.getLogger(MiscTools.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        return res;
    }

    public static boolean checkMegaDownloadUrl(String string_url) {

        boolean url_ok = false;

        try (CloseableHttpClient httpclient = getApacheKissHttpClient()) {

            HttpGet httpget = new HttpGet(new URI(string_url + "/0-0"));

            try (CloseableHttpResponse httpresponse = httpclient.execute(httpget)) {

                url_ok = (httpresponse.getStatusLine().getStatusCode() == HttpStatus.SC_OK);
            }

        } catch (MalformedURLException ex) {
            Logger.getLogger(MiscTools.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException | URISyntaxException ex) {

            Logger.getLogger(MiscTools.class.getName()).log(Level.SEVERE, null, ex);
        }

        return url_ok;
    }

    public static String getMyPublicIP() {

        String public_ip = null;

        try (CloseableHttpClient httpclient = getApacheKissHttpClientNOProxy()) {

            HttpGet httpget = new HttpGet(new URI("http://whatismyip.akamai.com/"));

            try (CloseableHttpResponse httpresponse = httpclient.execute(httpget)) {

                InputStream is = httpresponse.getEntity().getContent();

                try (ByteArrayOutputStream byte_res = new ByteArrayOutputStream()) {

                    byte[] buffer = new byte[MainPanel.DEFAULT_BYTE_BUFFER_SIZE];

                    int reads;

                    while ((reads = is.read(buffer)) != -1) {

                        byte_res.write(buffer, 0, reads);
                    }

                    public_ip = new String(byte_res.toByteArray());
                }
            }

        } catch (MalformedURLException ex) {
            Logger.getLogger(MiscTools.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException | URISyntaxException ex) {
            Logger.getLogger(MiscTools.class.getName()).log(Level.SEVERE, null, ex);
        }

        return public_ip;
    }

    public static String checkNewVersion(String folder_node, String folder_key) {

        String new_version = null;

        try {
            MegaAPI ma = new MegaAPI();

            HashMap<String, Object> folder_nodes = ma.getFolderNodes(folder_node, folder_key);

            if (folder_nodes != null && !folder_nodes.isEmpty()) {

                for (Object o : folder_nodes.values()) {

                    HashMap<String, Object> current_node = (HashMap<String, Object>) o;

                    new_version = findFirstRegex("([0-9\\.]+)\\.run", (String) current_node.get("name"), 1);

                    if (new_version != null && Double.parseDouble(new_version) > Double.parseDouble(VERSION)) {

                        break;

                    } else {

                        new_version = null;
                    }
                }
            }

        } catch (Exception ex) {
            Logger.getLogger(MiscTools.class.getName()).log(Level.SEVERE, null, ex);
        }

        return new_version;
    }

    public static void openBrowserURL(final String url) {

        try {
            Desktop.getDesktop().browse(new URI(url));
        } catch (URISyntaxException | IOException ex) {
            Logger.getLogger(MiscTools.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private static HttpClientBuilder _getApacheKissHttpClientBuilder() {

        return HttpClients.custom()
                .addInterceptorFirst(new RequestDefaultHeaders())
                .addInterceptorFirst(new RequestContent())
                .addInterceptorFirst(new RequestTargetHost())
                .addInterceptorFirst(new RequestClientConnControl())
                .addInterceptorFirst(new RequestAddCookies())
                .addInterceptorFirst(new ResponseProcessCookies())
                .addInterceptorFirst(new RequestAuthCache())
                .addInterceptorLast(new HttpRequestInterceptor() {

                    @Override
                    public void process(final HttpRequest request, final HttpContext context) throws HttpException, IOException {

                        if (request.containsHeader("User-Agent")) {

                            request.removeHeaders("User-Agent");

                        }

                        if (request.containsHeader("Custom-User-Agent")) {

                            request.addHeader("User-Agent", request.getFirstHeader("Custom-User-Agent").getValue());

                            request.removeHeaders("Custom-User-Agent");
                        }
                    }

                });
    }

    public static CloseableHttpClient getApacheKissHttpClient() {

        HttpClientBuilder builder = _getApacheKissHttpClientBuilder();

        if (MainPanel.isUse_proxy() && MainPanel.getProxy_host() != null) {

            HttpHost proxy = new HttpHost(MainPanel.getProxy_host(), MainPanel.getProxy_port());

            builder = builder.setProxy(proxy);

            if (MainPanel.getProxy_credentials() != null) {

                CredentialsProvider credsProvider = new BasicCredentialsProvider();

                AuthScope authScope = new AuthScope(MainPanel.getProxy_host(), MainPanel.getProxy_port());

                credsProvider.setCredentials(authScope, MainPanel.getProxy_credentials());

                builder = builder.setDefaultCredentialsProvider(credsProvider);
            }
        }

        RequestConfig requestConfig = RequestConfig.custom()
                .setSocketTimeout(HTTP_TIMEOUT * 1000)
                .setConnectTimeout(HTTP_TIMEOUT * 1000)
                .build();

        return builder.setDefaultRequestConfig(requestConfig).build();
    }

    public static CloseableHttpClient getApacheKissHttpClientSmartProxy(String current_proxy) throws Exception {

        HttpClientBuilder builder = _getApacheKissHttpClientBuilder();

        String[] proxy_parts = current_proxy.split(":");

        HttpHost proxy = new HttpHost(proxy_parts[0], Integer.valueOf(proxy_parts[1]));

        builder = builder.setProxy(proxy);

        RequestConfig requestConfig = RequestConfig.custom()
                .setSocketTimeout(SmartMegaProxyManager.PROXY_TIMEOUT * 1000)
                .setConnectTimeout(SmartMegaProxyManager.PROXY_TIMEOUT * 1000)
                .build();

        return builder.setDefaultRequestConfig(requestConfig).build();
    }

    public static CloseableHttpClient getApacheKissHttpClientNOProxy() {

        HttpClientBuilder builder = _getApacheKissHttpClientBuilder();

        RequestConfig requestConfig = RequestConfig.custom()
                .setSocketTimeout(HTTP_TIMEOUT * 1000)
                .setConnectTimeout(HTTP_TIMEOUT * 1000)
                .build();

        return builder.setDefaultRequestConfig(requestConfig).build();
    }

    public static byte[] recReverseArray(byte[] arr, int start, int end) {

        byte temp;

        if (start < end) {
            temp = arr[start];

            arr[start] = arr[end];

            arr[end] = temp;

            return recReverseArray(arr, start + 1, end - 1);

        } else {
            return arr;
        }
    }

    public static void restartApplication(int delay) {
        StringBuilder cmd = new StringBuilder();

        cmd.append(System.getProperty("java.home")).append(File.separator).append("bin").append(File.separator).append("java ");

        for (String jvmArg : ManagementFactory.getRuntimeMXBean().getInputArguments()) {
            cmd.append(jvmArg).append(" ");
        }

        cmd.append("-cp ").append(ManagementFactory.getRuntimeMXBean().getClassPath()).append(" ");

        cmd.append(MainPanel.class.getName()).append(" ");

        cmd.append(String.valueOf(delay));

        try {
            Runtime.getRuntime().exec(cmd.toString());
        } catch (IOException ex) {
            Logger.getLogger(MiscTools.class.getName()).log(Level.SEVERE, null, ex);
        }

        System.exit(0);
    }

    public static MegaAPI checkMegaAccountLoginAndShowMasterPassDialog(MainPanel main_panel, Container container, String email) throws Exception {

        boolean remember_master_pass = true;

        HashMap<String, Object> account_info = (HashMap) main_panel.getMega_accounts().get(email);

        MegaAPI ma = main_panel.getMega_active_accounts().get(email);

        if (ma == null) {

            ma = new MegaAPI();

            String password_aes, user_hash;
            synchronized (PASS_LOCK) {

                if (main_panel.getMaster_pass_hash() != null) {

                    if (main_panel.getMaster_pass() == null) {

                        GetMasterPasswordDialog pdialog = new GetMasterPasswordDialog((Frame) container.getParent(), true, main_panel.getMaster_pass_hash(), main_panel.getMaster_pass_salt());

                        pdialog.setLocationRelativeTo(container);

                        pdialog.setVisible(true);

                        if (pdialog.isPass_ok()) {

                            main_panel.setMaster_pass(pdialog.getPass());

                            pdialog.deletePass();

                            remember_master_pass = pdialog.getRemember_checkbox().isSelected();

                            pdialog.dispose();

                            password_aes = Bin2BASE64(CryptTools.aes_cbc_decrypt_pkcs7(BASE642Bin((String) account_info.get("password_aes")), main_panel.getMaster_pass(), CryptTools.AES_ZERO_IV));

                            user_hash = Bin2BASE64(CryptTools.aes_cbc_decrypt_pkcs7(BASE642Bin((String) account_info.get("user_hash")), main_panel.getMaster_pass(), CryptTools.AES_ZERO_IV));

                        } else {

                            pdialog.dispose();

                            throw new Exception();
                        }

                    } else {

                        password_aes = Bin2BASE64(CryptTools.aes_cbc_decrypt_pkcs7(BASE642Bin((String) account_info.get("password_aes")), main_panel.getMaster_pass(), CryptTools.AES_ZERO_IV));

                        user_hash = Bin2BASE64(CryptTools.aes_cbc_decrypt_pkcs7(BASE642Bin((String) account_info.get("user_hash")), main_panel.getMaster_pass(), CryptTools.AES_ZERO_IV));

                    }

                } else {

                    password_aes = (String) account_info.get("password_aes");

                    user_hash = (String) account_info.get("user_hash");
                }

                ma.fastLogin(email, bin2i32a(BASE642Bin(password_aes)), user_hash);

                main_panel.getMega_active_accounts().put(email, ma);

            }
        }

        if (!remember_master_pass) {

            main_panel.setMaster_pass(null);
        }

        return ma;

    }

    private MiscTools() {
    }

}
