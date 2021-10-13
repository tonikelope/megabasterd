package com.tonikelope.megabasterd;

import static com.tonikelope.megabasterd.MainPanel.VERSION;
import java.awt.Component;
import java.awt.Container;
import java.awt.Desktop;
import java.awt.Dialog;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.management.ManagementFactory;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.security.CodeSource;
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
import java.util.concurrent.FutureTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.xml.bind.DatatypeConverter;

/**
 *
 * @author tonikelope
 */
public class MiscTools {

    public static final int EXP_BACKOFF_BASE = 2;
    public static final int EXP_BACKOFF_SECS_RETRY = 1;
    public static final int EXP_BACKOFF_MAX_WAIT_TIME = 8;
    public static final Object PASS_LOCK = new Object();
    public static final int HTTP_TIMEOUT = 30;
    private static final Comparator<DefaultMutableTreeNode> TREE_NODE_COMPARATOR = (DefaultMutableTreeNode a, DefaultMutableTreeNode b) -> {
        if (a.isLeaf() && !b.isLeaf()) {
            return 1;
        } else if (!a.isLeaf() && b.isLeaf()) {
            return -1;
        } else {
            String sa = a.getUserObject().toString();
            String sb = b.getUserObject().toString();

            return MiscTools.naturalCompare(sa, sb, true);
        }
    };
    private static final Logger LOG = Logger.getLogger(MiscTools.class.getName());

    public static void deleteDirectoryRecursion(Path path) throws IOException {
        if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
            try (DirectoryStream<Path> entries = Files.newDirectoryStream(path)) {
                for (Path entry : entries) {
                    deleteDirectoryRecursion(entry);
                }
            }
        }
        Files.delete(path);
    }

    public static Font createAndRegisterFont(String name) {

        Font font = null;

        try {

            font = Font.createFont(Font.TRUETYPE_FONT, MiscTools.class.getResourceAsStream(name));

            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();

            ge.registerFont(font);

        } catch (FontFormatException | IOException ex) {
            Logger.getLogger(MiscTools.class.getName()).log(Level.SEVERE, ex.getMessage());
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
            java.util.logging.Logger.getLogger(MiscTools.class.getName()).log(java.util.logging.Level.SEVERE, ex.getMessage());
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
                    if (child instanceof JMenuItem) {

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

            Font new_font = font.deriveFont(old_font.getStyle(), Math.round(old_font.getSize() * zoom_factor));

            boolean error;

            do {
                try {
                    component.setFont(new_font);
                    error = false;
                } catch (Exception ex) {
                    error = true;
                }
            } while (error);

        }
    }

    public static void translateLabels(final Component component) {

        if (component != null) {

            if (component instanceof javax.swing.JLabel) {

                ((JLabel) component).setText(LabelTranslatorSingleton.getInstance().translate(((JLabel) component).getText()));

            } else if (component instanceof javax.swing.JButton) {

                ((AbstractButton) component).setText(LabelTranslatorSingleton.getInstance().translate(((AbstractButton) component).getText()));

            } else if (component instanceof javax.swing.JCheckBox) {

                ((AbstractButton) component).setText(LabelTranslatorSingleton.getInstance().translate(((AbstractButton) component).getText()));

            } else if ((component instanceof JMenuItem) && !(component instanceof JMenu)) {

                ((AbstractButton) component).setText(LabelTranslatorSingleton.getInstance().translate(((AbstractButton) component).getText()));

            } else if (component instanceof JMenu) {

                for (Component child : ((JMenu) component).getMenuComponents()) {
                    if (child instanceof JMenuItem) {
                        translateLabels(child);
                    }
                }

                ((AbstractButton) component).setText(LabelTranslatorSingleton.getInstance().translate(((AbstractButton) component).getText()));

            } else if (component instanceof Container) {

                for (Component child : ((Container) component).getComponents()) {
                    if (child instanceof Container) {

                        translateLabels(child);
                    }
                }

                if ((component instanceof JPanel) && (((JComponent) component).getBorder() instanceof TitledBorder)) {
                    ((TitledBorder) ((JComponent) component).getBorder()).setTitle(LabelTranslatorSingleton.getInstance().translate(((TitledBorder) ((JComponent) component).getBorder()).getTitle()));
                }

                if (component instanceof JDialog) {
                    ((Dialog) component).setTitle(LabelTranslatorSingleton.getInstance().translate(((Dialog) component).getTitle()));
                }
            }
        }
    }

    public static void updateTitledBorderFont(final TitledBorder border, final Font font, final float zoom_factor) {

        Font old_title_font = border.getTitleFont();

        Font new_title_font = font.deriveFont(old_title_font.getStyle(), Math.round(old_title_font.getSize() * zoom_factor));

        border.setTitleFont(new_title_font);
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

    public static void pausar(long pause) {
        try {
            Thread.sleep(pause);
        } catch (InterruptedException ex) {
            Logger.getLogger(MiscTools.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public static void GUIRun(Runnable r) {

        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(r);
        } else {
            r.run();
        }

    }

    public static void GUIRunAndWait(Runnable r) {

        try {
            if (!SwingUtilities.isEventDispatchThread()) {
                SwingUtilities.invokeAndWait(r);
            } else {
                r.run();
            }
        } catch (Exception ex) {

            Logger.getLogger(MiscTools.class.getName()).log(Level.SEVERE, null, ex);

        }

    }

    public static FutureTask futureRun(Callable c) {

        FutureTask f = new FutureTask(c);

        Thread hilo = new Thread(f);

        hilo.start();

        return f;
    }

    public static long getWaitTimeExpBackOff(int retryCount) {

        long waitTime = ((long) Math.pow(EXP_BACKOFF_BASE, retryCount) * EXP_BACKOFF_SECS_RETRY);

        return Math.min(waitTime, EXP_BACKOFF_MAX_WAIT_TIME);
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

    public static String formatBytes(Long bytes) {

        String[] units = {"B", "KB", "MB", "GB", "TB"};

        bytes = Math.max(bytes, 0L);

        int pow = Math.min((int) ((bytes > 0L ? Math.log(bytes) : 0) / Math.log(1024)), units.length - 1);

        Double bytes_double = (double) bytes / (1L << (10 * pow));

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

        children.forEach((node) -> {
            parent.add(node);
        });
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
                            Logger.getLogger(MiscTools.class.getName()).log(Level.SEVERE, ex.getMessage());
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
                Logger.getLogger(MiscTools.class.getName()).log(Level.SEVERE, ex.getMessage());
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
                                        Logger.getLogger(MiscTools.class.getName()).log(Level.SEVERE, ex.getMessage());
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

        return (System.getProperty("os.name").toLowerCase().contains("win") ? filename.replaceAll("[<>:\"/\\\\\\|\\?\\*\t]+", "") : filename).replaceAll("\\" + File.separator, "").replaceAll("[\\.]{1,}$", "").replaceAll("[\\x00-\\x1F]", "").trim();
    }

    public static String cleanFilePath(String path) {

        return !path.equals(".") ? ((System.getProperty("os.name").toLowerCase().contains("win") ? path.replaceAll("[<>:\"\\|\\?\\*\t]+", "") : path).replaceAll(" +\\" + File.separator, "\\" + File.separator).replaceAll("[\\.]{1,}$", "").replaceAll("[\\x00-\\x1F]", "").trim()) : path;
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

            if (data.startsWith("moz-extension") || data.startsWith("chrome-extension")) {
                data = extensionURL2NormalLink(data);
            }

            ArrayList<String> links = new ArrayList<>();
            String url_decoded;
            try {
                url_decoded = URLDecoder.decode(data, "UTF-8");
            } catch (Exception ex) {
                url_decoded = data;
            }
            ArrayList<String> base64_chunks = findAllRegex("[A-Za-z0-9+/_-]+=*", url_decoded, 0);
            if (!base64_chunks.isEmpty()) {

                for (String chunk : base64_chunks) {

                    try {

                        String clean_data = MiscTools.newMegaLinks2Legacy(new String(Base64.getDecoder().decode(chunk)));

                        String decoded = MiscTools.findFirstRegex("(?:https?|mega)://[^\r\n]+(#[^\r\n!]*?)?![^\r\n!]+![^\\?\r\n]+", clean_data, 0);

                        if (decoded != null) {
                            links.add(decoded);
                        }

                    } catch (Exception e) {
                    };
                }
            }
            try {
                url_decoded = URLDecoder.decode(data, "UTF-8");
            } catch (Exception ex) {
                url_decoded = data;
            }
            String clean_data = MiscTools.newMegaLinks2Legacy(url_decoded);
            links.addAll(findAllRegex("(?:https?|mega)://[^\r\n]+(#[^\r\n!]*?)?![^\r\n!]+![^\\?\r\n]+", clean_data, 0));
            links.addAll(findAllRegex("mega://e(n|l)c[^\r\n]+", clean_data, 0));
            res = links.stream().map((s) -> s + "\n").reduce(res, String::concat);
        }

        return res.trim();
    }

    public static String extractFirstMegaLinkFromString(String data) {

        String res = "";

        if (data != null) {

            try {
                String clean_data = MiscTools.newMegaLinks2Legacy(URLDecoder.decode(data, "UTF-8"));

                ArrayList<String> links = findAllRegex("(?:https?|mega)://[^\r\n]+(#[^\r\n!]*?)?![^\r\n!]+![^\\?\r\n]+", clean_data, 0);

                links.addAll(findAllRegex("mega://e(n|l)c[^\r\n]+", clean_data, 0));

                if (links.size() > 0) {

                    res = links.get(0);
                }
            } catch (UnsupportedEncodingException ex) {
                Logger.getLogger(MiscTools.class.getName()).log(Level.SEVERE, ex.getMessage());
            }
        }

        return res;
    }

    public static boolean checkMegaDownloadUrl(String string_url) {

        if (string_url == null || "".equals(string_url)) {
            return false;
        }

        HttpURLConnection con = null;

        int http_status = 0, http_error = 0;

        SmartMegaProxyManager proxy_manager = MainPanel.getProxy_manager();

        String current_smart_proxy = null;

        boolean smart_proxy_socks = false;

        ArrayList<String> excluded_proxy_list = new ArrayList<>();

        if (MainPanel.FORCE_SMART_PROXY) {

            String[] smart_proxy = proxy_manager.getProxy(excluded_proxy_list);

            current_smart_proxy = smart_proxy[0];

            smart_proxy_socks = smart_proxy[1].equals("socks");

        }

        do {

            try {

                URL url = new URL(string_url + "/0-0");

                if ((current_smart_proxy != null || http_error == 509) && MainPanel.isUse_smart_proxy() && !MainPanel.isUse_proxy()) {

                    if (current_smart_proxy != null && http_error != 0) {

                        if (http_error == 509) {
                            proxy_manager.blockProxy(current_smart_proxy);
                        }

                        excluded_proxy_list.add(current_smart_proxy);

                        String[] smart_proxy = proxy_manager.getProxy(excluded_proxy_list);

                        current_smart_proxy = smart_proxy[0];

                        smart_proxy_socks = smart_proxy[1].equals("socks");

                    } else if (current_smart_proxy == null) {

                        String[] smart_proxy = proxy_manager.getProxy(excluded_proxy_list);

                        current_smart_proxy = smart_proxy[0];

                        smart_proxy_socks = smart_proxy[1].equals("socks");
                    }

                    if (current_smart_proxy != null) {

                        String[] proxy_info = current_smart_proxy.split(":");

                        Proxy proxy = new Proxy(smart_proxy_socks ? Proxy.Type.SOCKS : Proxy.Type.HTTP, new InetSocketAddress(proxy_info[0], Integer.parseInt(proxy_info[1])));

                        con = (HttpURLConnection) url.openConnection(proxy);

                    } else {

                        if (MainPanel.isUse_proxy()) {

                            con = (HttpURLConnection) url.openConnection(new Proxy(smart_proxy_socks ? Proxy.Type.SOCKS : Proxy.Type.HTTP, new InetSocketAddress(MainPanel.getProxy_host(), MainPanel.getProxy_port())));

                            if (MainPanel.getProxy_user() != null && !"".equals(MainPanel.getProxy_user())) {

                                con.setRequestProperty("Proxy-Authorization", "Basic " + MiscTools.Bin2BASE64((MainPanel.getProxy_user() + ":" + MainPanel.getProxy_pass()).getBytes("UTF-8")));
                            }
                        } else {

                            con = (HttpURLConnection) url.openConnection();
                        }
                    }

                } else {

                    if (MainPanel.isUse_proxy()) {

                        con = (HttpURLConnection) url.openConnection(new Proxy(smart_proxy_socks ? Proxy.Type.SOCKS : Proxy.Type.HTTP, new InetSocketAddress(MainPanel.getProxy_host(), MainPanel.getProxy_port())));

                        if (MainPanel.getProxy_user() != null && !"".equals(MainPanel.getProxy_user())) {

                            con.setRequestProperty("Proxy-Authorization", "Basic " + MiscTools.Bin2BASE64((MainPanel.getProxy_user() + ":" + MainPanel.getProxy_pass()).getBytes("UTF-8")));
                        }
                    } else {

                        con = (HttpURLConnection) url.openConnection();
                    }
                }

                if (current_smart_proxy != null) {
                    con.setConnectTimeout(Transference.HTTP_PROXY_CONNECT_TIMEOUT);
                    con.setReadTimeout(Transference.HTTP_PROXY_READ_TIMEOUT);
                }

                con.setUseCaches(false);

                con.setRequestProperty("User-Agent", MainPanel.DEFAULT_USER_AGENT);

                http_status = con.getResponseCode();

                if (http_status != 200) {
                    http_error = http_status;
                } else {
                    http_error = 0;
                }

            } catch (Exception ex) {
                Logger.getLogger(MiscTools.class.getName()).log(Level.SEVERE, ex.getMessage());
            } finally {

                if (con != null) {
                    con.disconnect();
                }

            }

        } while (http_error == 509);

        return http_status != 403;
    }

    public static String getMyPublicIP() {

        String public_ip = null;
        HttpURLConnection con = null;

        try {

            URL url_api = new URL("http://whatismyip.akamai.com/");

            if (MainPanel.isUse_proxy()) {

                con = (HttpURLConnection) url_api.openConnection(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(MainPanel.getProxy_host(), MainPanel.getProxy_port())));

                if (MainPanel.getProxy_user() != null && !"".equals(MainPanel.getProxy_user())) {

                    con.setRequestProperty("Proxy-Authorization", "Basic " + MiscTools.Bin2BASE64((MainPanel.getProxy_user() + ":" + MainPanel.getProxy_pass()).getBytes("UTF-8")));
                }
            } else {

                con = (HttpURLConnection) url_api.openConnection();
            }

            con.setUseCaches(false);

            try (InputStream is = con.getInputStream(); ByteArrayOutputStream byte_res = new ByteArrayOutputStream()) {

                byte[] buffer = new byte[MainPanel.DEFAULT_BYTE_BUFFER_SIZE];

                int reads;

                while ((reads = is.read(buffer)) != -1) {

                    byte_res.write(buffer, 0, reads);
                }

                public_ip = new String(byte_res.toByteArray(), "UTF-8");
            }

        } catch (MalformedURLException ex) {
            Logger.getLogger(MiscTools.class.getName()).log(Level.SEVERE, ex.getMessage());
        } catch (IOException ex) {
            Logger.getLogger(MiscTools.class.getName()).log(Level.SEVERE, ex.getMessage());
        } finally {
            if (con != null) {
                con.disconnect();
            }
        }

        return public_ip;
    }

    public static String checkNewVersion(String url) {

        String new_version_major = null, new_version_minor = null, current_version_major = null, current_version_minor = null;

        URL mb_url;

        HttpURLConnection con = null;

        try {

            mb_url = new URL(url);

            if (MainPanel.isUse_proxy()) {

                con = (HttpURLConnection) mb_url.openConnection(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(MainPanel.getProxy_host(), MainPanel.getProxy_port())));

                if (MainPanel.getProxy_user() != null && !"".equals(MainPanel.getProxy_user())) {

                    con.setRequestProperty("Proxy-Authorization", "Basic " + MiscTools.Bin2BASE64((MainPanel.getProxy_user() + ":" + MainPanel.getProxy_pass()).getBytes("UTF-8")));
                }
            } else {

                con = (HttpURLConnection) mb_url.openConnection();
            }

            con.setUseCaches(false);

            try (InputStream is = con.getInputStream(); ByteArrayOutputStream byte_res = new ByteArrayOutputStream()) {

                byte[] buffer = new byte[MainPanel.DEFAULT_BYTE_BUFFER_SIZE];

                int reads;

                while ((reads = is.read(buffer)) != -1) {

                    byte_res.write(buffer, 0, reads);
                }

                String latest_version_res = new String(byte_res.toByteArray(), "UTF-8");

                String latest_version = findFirstRegex("releases\\/tag\\/v?([0-9]+\\.[0-9]+)", latest_version_res, 1);

                new_version_major = findFirstRegex("([0-9]+)\\.[0-9]+", latest_version, 1);

                new_version_minor = findFirstRegex("[0-9]+\\.([0-9]+)", latest_version, 1);

                current_version_major = findFirstRegex("([0-9]+)\\.[0-9]+$", VERSION, 1);

                current_version_minor = findFirstRegex("[0-9]+\\.([0-9]+)$", VERSION, 1);

                if (new_version_major != null && (Integer.parseInt(current_version_major) < Integer.parseInt(new_version_major) || (Integer.parseInt(current_version_major) == Integer.parseInt(new_version_major) && Integer.parseInt(current_version_minor) < Integer.parseInt(new_version_minor)))) {

                    return new_version_major + "." + new_version_minor;

                }
            }

        } catch (MalformedURLException ex) {
            Logger.getLogger(MiscTools.class.getName()).log(Level.SEVERE, ex.getMessage());
        } catch (IOException ex) {
            Logger.getLogger(MiscTools.class.getName()).log(Level.SEVERE, ex.getMessage());
        } finally {
            if (con != null) {
                con.disconnect();
            }
        }

        return null;
    }

    public static void openBrowserURL(final String url) {

        try {
            Logger.getLogger(MiscTools.class.getName()).log(Level.INFO, "Trying to open URL in external browser: {0}", url);

            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(url));
                return;
            }
            if (System.getProperty("os.name").toLowerCase().contains("nux")) {
                Process p = Runtime.getRuntime().exec(new String[]{"xdg-open", url});
                p.waitFor();
                p.destroy();
                return;
            }
            Logger.getLogger(MiscTools.class.getName()).log(Level.WARNING, "Unable to open URL: Unsupported platform.", url);
        } catch (Exception ex) {
            Logger.getLogger(MiscTools.class.getName()).log(Level.SEVERE, ex.getMessage());
        }
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

    public static String getCurrentJarParentPath() {
        try {
            CodeSource codeSource = MainPanel.class.getProtectionDomain().getCodeSource();

            File jarFile = new File(codeSource.getLocation().toURI().getPath());

            return jarFile.getParentFile().getAbsolutePath();

        } catch (URISyntaxException ex) {
            Logger.getLogger(MiscTools.class.getName()).log(Level.SEVERE, null, ex);
        }

        return null;
    }

    public static void restartApplication() {

        StringBuilder cmd = new StringBuilder();

        cmd.append(System.getProperty("java.home")).append(File.separator).append("bin").append(File.separator).append("java ");

        ManagementFactory.getRuntimeMXBean().getInputArguments().forEach((jvmArg) -> {
            cmd.append(jvmArg).append(" ");
        });

        cmd.append("-cp ").append(ManagementFactory.getRuntimeMXBean().getClassPath()).append(" ");

        cmd.append(MainPanel.class.getName()).append(" native 1");

        try {
            Runtime.getRuntime().exec(cmd.toString());
        } catch (IOException ex) {
            Logger.getLogger(MiscTools.class.getName()).log(Level.SEVERE, ex.getMessage());
        }

        System.exit(2);
    }

    /*
        Thanks -> https://stackoverflow.com/a/26884326
     */
    public static int naturalCompare(String a, String b, boolean ignoreCase) {

        if (a == null) {
            a = "";
        }

        if (b == null) {
            b = "";
        }

        if (ignoreCase) {
            a = a.toLowerCase();
            b = b.toLowerCase();
        }
        int aLength = a.length();
        int bLength = b.length();
        int minSize = Math.min(aLength, bLength);
        char aChar, bChar;
        boolean aNumber, bNumber;
        boolean asNumeric = false;
        int lastNumericCompare = 0;
        for (int i = 0; i < minSize; i++) {
            aChar = a.charAt(i);
            bChar = b.charAt(i);
            aNumber = aChar >= '0' && aChar <= '9';
            bNumber = bChar >= '0' && bChar <= '9';
            if (asNumeric) {
                if (aNumber && bNumber) {
                    if (lastNumericCompare == 0) {
                        lastNumericCompare = aChar - bChar;
                    }
                } else if (aNumber) {
                    return 1;
                } else if (bNumber) {
                    return -1;
                } else if (lastNumericCompare == 0) {
                    if (aChar != bChar) {
                        return aChar - bChar;
                    }
                    asNumeric = false;
                } else {
                    return lastNumericCompare;
                }
            } else if (aNumber && bNumber) {
                asNumeric = true;
                if (lastNumericCompare == 0) {
                    lastNumericCompare = aChar - bChar;
                }
            } else if (aChar != bChar) {
                return aChar - bChar;
            }
        }
        if (asNumeric) {
            if (aLength > bLength && a.charAt(bLength) >= '0' && a.charAt(bLength) <= '9') // as number
            {
                return 1;  // a has bigger size, thus b is smaller
            } else if (bLength > aLength && b.charAt(aLength) >= '0' && b.charAt(aLength) <= '9') // as number
            {
                return -1;  // b has bigger size, thus a is smaller
            } else if (lastNumericCompare == 0) {
                return aLength - bLength;
            } else {
                return lastNumericCompare;
            }
        } else {
            return aLength - bLength;
        }
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

                        GetMasterPasswordDialog pdialog = new GetMasterPasswordDialog((Frame) container.getParent(), true, main_panel.getMaster_pass_hash(), main_panel.getMaster_pass_salt(), main_panel);

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

                            return null;
                        }

                    } else {

                        password_aes = Bin2BASE64(CryptTools.aes_cbc_decrypt_pkcs7(BASE642Bin((String) account_info.get("password_aes")), main_panel.getMaster_pass(), CryptTools.AES_ZERO_IV));

                        user_hash = Bin2BASE64(CryptTools.aes_cbc_decrypt_pkcs7(BASE642Bin((String) account_info.get("user_hash")), main_panel.getMaster_pass(), CryptTools.AES_ZERO_IV));

                    }

                } else {

                    password_aes = (String) account_info.get("password_aes");

                    user_hash = (String) account_info.get("user_hash");
                }

                try {

                    HashMap<String, Object> old_session_data = DBTools.selectMegaSession(email);

                    boolean unserialization_error = false;

                    if (old_session_data != null) {

                        Logger.getLogger(MiscTools.class.getName()).log(Level.INFO, "Reutilizando sesión de MEGA guardada para {0}", email);

                        MegaAPI old_ma = new MegaAPI();

                        if ((boolean) old_session_data.get("crypt")) {

                            ByteArrayInputStream bs = new ByteArrayInputStream(CryptTools.aes_cbc_decrypt_pkcs7((byte[]) old_session_data.get("ma"), main_panel.getMaster_pass(), CryptTools.AES_ZERO_IV));

                            try (ObjectInputStream is = new ObjectInputStream(bs)) {

                                old_ma = (MegaAPI) is.readObject();

                            } catch (Exception ex) {
                                unserialization_error = true;
                            }

                        } else {

                            ByteArrayInputStream bs = new ByteArrayInputStream((byte[]) old_session_data.get("ma"));

                            try (ObjectInputStream is = new ObjectInputStream(bs)) {
                                old_ma = (MegaAPI) is.readObject();
                            } catch (Exception ex) {
                                unserialization_error = true;
                            }

                        }

                        if (old_ma.getQuota() == null) {

                            unserialization_error = true;

                        } else {

                            ma = old_ma;
                        }
                    }

                    if (old_session_data == null || unserialization_error) {

                        String pincode = null;

                        if (ma.check2FA(email)) {

                            Get2FACode dialog = new Get2FACode((Frame) container.getParent(), true, email, main_panel);

                            dialog.setLocationRelativeTo(container);

                            dialog.setVisible(true);

                            if (dialog.isCode_ok()) {
                                pincode = dialog.getPin_code();
                            } else {
                                throw new MegaAPIException(-26);
                            }
                        }

                        ma.fastLogin(email, bin2i32a(BASE642Bin(password_aes)), user_hash, pincode);

                        ByteArrayOutputStream bs = new ByteArrayOutputStream();

                        try (ObjectOutputStream os = new ObjectOutputStream(bs)) {
                            os.writeObject(ma);
                        }

                        if (main_panel.getMaster_pass() != null) {

                            DBTools.insertMegaSession(email, CryptTools.aes_cbc_encrypt_pkcs7(bs.toByteArray(), main_panel.getMaster_pass(), CryptTools.AES_ZERO_IV), true);

                        } else {

                            DBTools.insertMegaSession(email, bs.toByteArray(), false);
                        }
                    }

                    main_panel.getMega_active_accounts().put(email, ma);

                } catch (MegaAPIException exception) {

                    if (exception.getCode() == -6) {
                        JOptionPane.showMessageDialog(container.getParent(), LabelTranslatorSingleton.getInstance().translate("You've tried to login too many times. Wait an hour."), "Error", JOptionPane.ERROR_MESSAGE);
                    }

                    throw exception;
                }

            }
        }

        if (!remember_master_pass) {

            main_panel.setMaster_pass(null);
        }

        return ma;

    }

    public static String newMegaLinks2Legacy(String data) {

        data = MiscTools.addHTTPSToMegaLinks(data);

        String replace1 = data.replaceAll("(?:https://)?mega(?:\\.co)?\\.nz/folder/([^#]+)#(.+)", "https://mega.nz/#F!$1!$2");

        return replace1.replaceAll("(?:https://)?mega(?:\\.co)?\\.nz/file/([^#]+)#(.+)", "https://mega.nz/#!$1!$2");
    }

    public static String addHTTPSToMegaLinks(String data) {

        return data.replaceAll("(?<!http://|https://)mega(?:\\.co)?\\.nz", "https://mega.nz");
    }

    /* This method changes the MEGA extension URL to a ordinary MEGA URL,
    so copying the extension URL from Firefox or Chrome also works as a normal URL */
    public static String extensionURL2NormalLink(String data) {

        String toReplace = data.substring(0, data.indexOf('#') + 1);

        return data.replace(toReplace, "https://mega.nz");
    }

    private MiscTools() {
    }

}
