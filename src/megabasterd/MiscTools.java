package megabasterd;

import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import static java.util.logging.Logger.getLogger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreePath;
import javax.xml.bind.DatatypeConverter;
import static megabasterd.MainPanel.CONNECTION_TIMEOUT;
import static megabasterd.MainPanel.VERSION;

public final class MiscTools {
    
    public static final int EXP_BACKOFF_BASE=2;
    public static final int EXP_BACKOFF_SECS_RETRY=1;
    public static final int EXP_BACKOFF_MAX_WAIT_TIME=128;
    private static final ConcurrentHashMap<String, Method> REFLECTION_METHOD_CACHE = new ConcurrentHashMap<>();
    
    private static final Comparator<DefaultMutableTreeNode> TREE_NODE_COMPARATOR = new Comparator< DefaultMutableTreeNode>() {
        
        @Override public int compare(DefaultMutableTreeNode a, DefaultMutableTreeNode b) {
            
            
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
            getLogger(MiscTools.class.getName()).log(Level.SEVERE, null, ex);
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
    
    public static int[] bin2i32a(byte[] bin)
    {
        int l = (int)(4 * Math.ceil((double)bin.length/4));

        byte[] new_bin = Arrays.copyOfRange(bin, 0, l);
       
        bin = new_bin;

        ByteBuffer bin_buffer = ByteBuffer.wrap(bin);
        IntBuffer int_buffer = bin_buffer.asIntBuffer();
        
        if(int_buffer.hasArray()) {
            return int_buffer.array();
        }
        else
        {
            ArrayList<Integer> list = new ArrayList<>();
        
            while(int_buffer.hasRemaining()) {
                list.add(int_buffer.get());
            }

            int[] aux = new int[list.size()];

            for(int i=0; i<aux.length; i++) {
                aux[i] = list.get(i);
            }

            return aux; 
        }
    }
    
    public static byte[] i32a2bin(int[] i32a)
    {
        ByteBuffer bin_buffer  = ByteBuffer.allocate(i32a.length * 4);        
        IntBuffer int_buffer = bin_buffer.asIntBuffer();
        int_buffer.put(i32a);
        
        if(bin_buffer.hasArray()) {
            return bin_buffer.array();
        }
        else
        {
            ArrayList<Byte> list = new ArrayList<>();
        
            while(int_buffer.hasRemaining()) {
                list.add(bin_buffer.get());
            }

            byte[] aux = new byte[list.size()];

            for(int i=0; i<aux.length; i++) {
                aux[i] = list.get(i);
            }

            return aux;
        }
    }
    
    
    public static BigInteger mpi2big(byte[] s) {
        
        byte[] ns = Arrays.copyOfRange(s, 2, s.length);
        
        BigInteger bigi = new BigInteger(1,ns);
        
        return bigi;
    }
    
    public static String genID(int length) {
        
        String pos = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        
        String res="";
        
        Random randomno = new Random();
        
        for(int i=0; i<length; i++) {

            res+=pos.charAt(randomno.nextInt(pos.length()));
        }
        
        return res;
    }
    
    public static byte[] long2bytearray(long val) {
    
        byte [] b = new byte[8];
        
        for (int i = 7; i >= 0; i--) {
          b[i] = (byte) val;
          val >>>= 8;
        }
        
        return b;
    }
    
    public static long bytearray2long(byte[] val) {
        
        long l=0;
        
        for (int i = 0; i <=7; i++) {
            l+=val[i];
            l<<=8;
        }
        
        return l;
    }
    
    public static String findFirstRegex(String regex, String data, int group)
    {
        Pattern pattern = Pattern.compile(regex);
        
        Matcher matcher = pattern.matcher(data);
        
        return matcher.find()?matcher.group(group):null;   
    }
    
    public static ArrayList<String> findAllRegex(String regex, String data, int group)
    {
        Pattern pattern = Pattern.compile(regex);
        
        Matcher matcher = pattern.matcher(data);
        
        ArrayList<String> matches = new ArrayList<>();
        
        while(matcher.find()) {
            matches.add(matcher.group(group));
        }
        
        return matches;
    }
    
    public static void updateFont(javax.swing.JComponent label, Font font, int layout)
    {
        label.setFont(font.deriveFont(layout, label.getFont().getSize()));
    }
    
    
    public static String HashString(String algo, String data) throws NoSuchAlgorithmException, UnsupportedEncodingException
    {
        MessageDigest md = MessageDigest.getInstance(algo);
        
        byte[] thedigest = md.digest(data.getBytes("UTF-8"));
        
        return bin2hex(thedigest); 
    }
    
    public static String HashString(String algo, byte[] data) throws NoSuchAlgorithmException
    {
        MessageDigest md = MessageDigest.getInstance(algo);
        
        byte[] thedigest = md.digest(data);
        
        return bin2hex(thedigest); 
    }
    
    public static byte[] HashBin(String algo, String data) throws NoSuchAlgorithmException, UnsupportedEncodingException
    {
        MessageDigest md = MessageDigest.getInstance(algo);
        
        return md.digest(data.getBytes("UTF-8"));
    }
    
    public static byte[] HashBin(String algo, byte[] data) throws NoSuchAlgorithmException
    {
        MessageDigest md = MessageDigest.getInstance(algo);
        
        return md.digest(data);
    }
    
    public static byte[] BASE642Bin(String data) throws Exception
    {
        return Base64.getDecoder().decode(data);
    }
    
    public static String Bin2BASE64(byte[] data) throws Exception
    {
        return Base64.getEncoder().encodeToString(data);
    }
    
    public static byte[] UrlBASE642Bin(String data) throws Exception
    {
        return Base64.getUrlDecoder().decode(data);
    }
    
    public static String Bin2UrlBASE64(byte[] data) throws Exception
    {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
    }
    
    
    public static long getWaitTimeExpBackOff(int retryCount) {

        long waitTime = ((long) Math.pow(EXP_BACKOFF_BASE, retryCount) * EXP_BACKOFF_SECS_RETRY);

        return Math.min(waitTime, EXP_BACKOFF_MAX_WAIT_TIME);
    }
    
    public static void swingReflectionInvoke(final String method_name, final Object obj, final Object... params) {
        
        _swingReflectionInvoke(method_name, obj, false, params);
    }
    
    public static void swingReflectionInvokeAndWait(final String method_name, final Object obj, final Object... params) {
        
        _swingReflectionInvoke(method_name, obj, true, params);
    }
 
    private static void _swingReflectionInvoke(final String method_name, final Object obj, final boolean wait, final Object... params) {

            Runnable r = new Runnable(){

            @Override
            public void run() {

                if(obj != null) {
                    
                    Method method;

                    try {

                        if( (method=REFLECTION_METHOD_CACHE.get( method_name+"#"+obj.getClass().toString()+"#"+String.valueOf(params.length))) != null) {

                            try {

                                method.invoke(obj, params);

                            } catch(SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {

                                method = null;
                            }
                        }

                        if(method == null) {

                            for(Method m:obj.getClass().getMethods()) {

                                if(m.getName().equals(method_name) && m.getParameterCount() == params.length) {

                                    try {

                                        m.invoke(obj, params);

                                        REFLECTION_METHOD_CACHE.put(method_name+"#"+obj.getClass().toString()+"#"+String.valueOf(params.length), m);

                                        method = m;

                                        break;

                                    }catch(SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex2)  {

                                    }
                                }
                            }

                            if(method == null) {

                                throw new NoSuchMethodException();

                            }
                        }       

                    } catch (SecurityException | IllegalArgumentException | NoSuchMethodException ex) {

                        getLogger(MiscTools.class.getName()).log(Level.SEVERE, null, ex);

                        System.out.println("REFLECTION METHOD NOT FOUND -> "+method_name+"#"+obj.getClass().toString()+"#"+String.valueOf(params.length));
                    }

                }
            }
        };

        swingInvokeIt(r, wait);
    }
    

    public static Object swingReflectionInvokeAndWaitForReturn(final String method_name, final Object obj, final Object... params) {
        
            Callable c = new Callable(){

            @Override
            public Object call() {
                
                Object ret = null;
                
                if(obj != null) {

                    Method method;

                    try {

                        if( (method=REFLECTION_METHOD_CACHE.get( method_name+"#"+obj.getClass().toString()+"#"+String.valueOf(params.length))) != null) {

                            try {

                                ret = method.invoke(obj, params);

                            } catch(SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {

                                method = null;
                            }
                        }

                        if(method == null) {

                            for(Method m:obj.getClass().getMethods()) {

                                if(m.getName().equals(method_name) && m.getParameterCount() == params.length) {

                                    try {

                                        ret = m.invoke(obj, params);

                                        REFLECTION_METHOD_CACHE.put(method_name+"#"+obj.getClass().toString()+"#"+String.valueOf(params.length), m);

                                        method = m;

                                        break;

                                    }catch(SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex2)  {

                                    }
                                }
                            }

                            if(method == null) {

                                throw new NoSuchMethodException();
                            }
                        }       

                    } catch (SecurityException | IllegalArgumentException | NoSuchMethodException ex) {
                        getLogger(MiscTools.class.getName()).log(Level.SEVERE, null, ex);

                        System.out.println("REFLECTION METHOD NOT FOUND -> "+method_name+"#"+obj.getClass().toString()+"#"+String.valueOf(params.length));
                    }

                }
                
                return ret;
            }
        };

        return swingInvokeItAndWaitForReturn(c);
    }
    
    
    private static void swingInvokeIt(Runnable r, boolean wait) {
        
        if(wait) {

            if(SwingUtilities.isEventDispatchThread()) {
                
                r.run();
                
            } else {
                
                try {
                    /* OJO!!! El thread que lanza esto NO PUEDE poseer locks que necesite el EDT o se producirá un DEADLOCK */
                    SwingUtilities.invokeAndWait(r);
            
                } catch (InterruptedException | InvocationTargetException ex) {
                    getLogger(MiscTools.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
   
        }else{
            
            SwingUtilities.invokeLater(r);
        }
    }
    
    private static Object swingInvokeItAndWaitForReturn(Callable c)
    {
        Object ret=null;
    
        if(SwingUtilities.isEventDispatchThread()) {
            
            try {
                ret = c.call();
            } catch (Exception ex) {
                getLogger(MiscTools.class.getName()).log(Level.SEVERE, null, ex);
            }
            
        } else {
            
            FutureTask<Object> futureTask = new FutureTask<>(c);

            SwingUtilities.invokeLater(futureTask);

            try {
                ret = futureTask.get();
            } catch (InterruptedException | ExecutionException ex) {
                getLogger(MiscTools.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        return ret;
    }
 
    public static String bin2hex(byte[] b){
        
        BigInteger bi = new BigInteger(1, b);

        return String.format("%0" + (b.length << 1) + "x", bi);
    }
    
     public static byte[] hex2bin(String s) {
        return DatatypeConverter.parseHexBinary(s);
    }
    
    public static void copyTextToClipboard(String text) {

        StringSelection stringSelection = new StringSelection (text);
        Clipboard clpbrd = Toolkit.getDefaultToolkit ().getSystemClipboard ();
        clpbrd.setContents (stringSelection, null);
        
    }
    
    public static String deflateURL(String link) throws MalformedURLException, IOException {
        
        String urlString = "http://tinyurl.com/api-create.php?url="+URLEncoder.encode(link.trim(), "UTF-8");
        
        URL url = new URL(urlString);
        
        URLConnection conn = url.openConnection();
        
        conn.setRequestProperty("User-Agent", MegaCrypterAPI.USER_AGENT);

        String content_encoding = conn.getContentEncoding();
            
        InputStream is=(content_encoding!=null && content_encoding.equals("gzip"))?new GZIPInputStream(conn.getInputStream()):conn.getInputStream();

        ByteArrayOutputStream byte_res = new ByteArrayOutputStream();

        byte[] buffer = new byte[16*1024];

        int reads;

        while( (reads=is.read(buffer)) != -1 ) {

            byte_res.write(buffer, 0, reads);
        }
            
        String response = new String(byte_res.toByteArray()).trim();
        
        return findFirstRegex("http", response, 0)!=null?response:link;
    }
    
    public static String formatBytes(Long bytes) {
        
        String[] units = {"B", "KB", "MB", "GB", "TB"};
        
        bytes = Math.max(bytes, 0L);
        
        int pow = Math.min((int)((bytes>0L?Math.log(bytes):0) / Math.log(1024)), units.length - 1);
      
        Double bytes_double = (double)bytes/(1 << (10 * pow));
        
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
        
        for (DefaultMutableTreeNode node: children) {
            
            parent.add(node);
          
        }
    }
    
    public static boolean deleteSelectedTreeItems(JTree tree) {
        
        TreePath[] paths = tree.getSelectionPaths();
        
        if(paths != null) {
            
            DefaultTreeModel model = (DefaultTreeModel) (tree.getModel());
            
            MutableTreeNode node;
            
            for (TreePath path : paths) {

                node = (MutableTreeNode)path.getLastPathComponent();
                    
                if(node != null) {
                    
                    if(node != model.getRoot()) {

                        MutableTreeNode parent = (MutableTreeNode)node.getParent();

                        model.removeNodeFromParent(node);

                        while(parent != null && parent.isLeaf()) {

                            if(parent != model.getRoot()) {

                                MutableTreeNode parent_aux = (MutableTreeNode)parent.getParent();

                                model.removeNodeFromParent(parent);

                                parent = parent_aux;

                            } else {

                                parent = null;
                            }
                        }
                    
                    } else {
                        
                        swingReflectionInvokeAndWait("setEnabled", tree, true);
                        
                        try {
                            tree.setModel(new DefaultTreeModel((MutableTreeNode)tree.getModel().getRoot().getClass().newInstance()));
                            
                            tree.revalidate();
            
                            tree.repaint();
                        } catch (InstantiationException | IllegalAccessException ex) {
                            Logger.getLogger(MiscTools.class.getName()).log(Level.SEVERE, null, ex);
                        }
                        
                        return true;
                    }
                }
            }
            
            return true;
        }
        
        return false;
    }
    
    public static boolean deleteAllExceptSelectedTreeItems(JTree tree) {
        
        TreePath[] paths = tree.getSelectionPaths();
        
        HashMap<MutableTreeNode,MutableTreeNode> hashmap_old = new HashMap<>();
        
        if(paths != null) {
            
            Class node_class = tree.getModel().getRoot().getClass();

            Object new_root = null;
            
            try {
                
                new_root = node_class.newInstance();
                
                ((MutableTreeNode)new_root).setUserObject( ((DefaultMutableTreeNode)tree.getModel().getRoot()).getUserObject() );
                
            } catch (InstantiationException | IllegalAccessException ex) {
                getLogger(MiscTools.class.getName()).log(Level.SEVERE, null, ex);
            }

            for (TreePath path : paths) {
                
                if((MutableTreeNode)path.getLastPathComponent() != (MutableTreeNode)tree.getModel().getRoot())
                {
                    Object parent = new_root;

                    for(Object path_element:path.getPath()) {

                        if((MutableTreeNode)path_element != (MutableTreeNode)tree.getModel().getRoot()) {

                            if(hashmap_old.get((MutableTreeNode)path_element) == null) {

                                Object node=null;

                                if((MutableTreeNode)path_element == (MutableTreeNode)path.getLastPathComponent()) {

                                    node = path_element;

                                } else {

                                    try {

                                        node = node_class.newInstance();

                                        ((MutableTreeNode)node).setUserObject( ((DefaultMutableTreeNode)path_element).getUserObject() );

                                    } catch (InstantiationException | IllegalAccessException ex) {
                                        getLogger(MiscTools.class.getName()).log(Level.SEVERE, null, ex);
                                    }
                                }
                                
                                if(parent != null) {
                                    
                                    ((DefaultMutableTreeNode)parent).add((MutableTreeNode)node);
                                    
                                    if(!((MutableTreeNode)path_element).isLeaf()) {

                                        hashmap_old.put((MutableTreeNode)path_element, (MutableTreeNode)node);

                                        parent = node;
                                    }
                                }
                                
                            } else {

                                parent = hashmap_old.get((MutableTreeNode)path_element);
                            }
                        }
                    }
                    
                } else {
                    
                    return false;
                }
            }
            
            swingReflectionInvokeAndWait("setEnabled", tree, true);
            
            tree.setModel(new DefaultTreeModel(sortTree((DefaultMutableTreeNode)new_root)));
            
            tree.revalidate();
            
            tree.repaint();
            
            return true;
        }
        
        return false;
    }
    
    public static String truncateText(String text, int max_length) {
        
        String separator = " ... ";
        
        max_length-=separator.length();
        
        if(max_length %2 != 0) {
            
            max_length--;
        }

        return (text.length() > max_length)?text.replaceAll("^(.{1,"+(max_length/2)+"}).*?(.{1,"+(max_length/2)+"})$", "$1"+separator+"$2"):text;
    }
    
    public static String cleanFilename(String filename) {
        
        return (System.getProperty("os.name").toLowerCase().contains("win")?filename.replaceAll("[<>:\"/\\\\\\|\\?\\*]+", "").replaceAll("[ \\.]{1,}/{1,}", "/"):filename).replaceAll("[\\.]{1,}$", "").trim();
    }
    
    public static String cleanFilePath(String path) {
        
        return path.equals(".")?((System.getProperty("os.name").toLowerCase().contains("win")?path.replaceAll("[<>:\"\\|\\?\\*]+", "").replaceAll("[ \\.]{1,}/{1,}", "/"):path).replaceAll("[\\.]{1,}$", "").trim()):path;
    }

    public static byte[] genRandomByteArray(int length) {
        
        byte[] the_array = new byte[length];

        Random randomno = new Random();
        
        randomno.nextBytes(the_array);
        
        return the_array;
    }
    
    public static String extractStringFromClipboardContents(Transferable contents) {
    
        String ret = null;
        
        if(contents != null) {
            
            try {

                Object o = contents.getTransferData(DataFlavor.stringFlavor);

                if(o instanceof String) {

                    ret = (String)o;
                }

            } catch (Exception ex) {}
        }
 
        return ret;
        
    }
    
    public static String extractMegaLinksFromString(String data) {
        
        String res = "";
        
        if(data != null) {
            
            ArrayList<String> links = findAllRegex("(?:https?|mega)://[^/]*/(#.*?)?!.+![^\r\n]+", data, 0);
            
            links.addAll(findAllRegex("mega://enc.*?[^\r\n]+", data, 0));

            for(String s:links) {

                res+=s+"\n";
            }
        }

        return res.trim();
    }
    
    public static String extractFirstMegaLinkFromString(String data) {
        
        String res = "";
        
        if(data != null) {
            
            ArrayList<String> links = findAllRegex("(?:https?|mega)://[^/]*/(#.*?)?!.+![^\r\n]+", data, 0);
            
            links.addAll(findAllRegex("mega://enc.*?[^\r\n]+", data, 0));

            if(links.size()>0) {
                
                res = links.get(0);
            }
        }

        return res;
    }
    
    public static boolean checkMegaDownloadUrl(String string_url) {
        
        boolean url_ok=false;
               
        try {
             URL url = new URL(string_url+"/0-0");
             URLConnection connection = url.openConnection();
             connection.setConnectTimeout(CONNECTION_TIMEOUT);
             connection.setRequestProperty("User-Agent", MegaAPI.USER_AGENT);

            try (InputStream is = connection.getInputStream()) {
                while(is.read()!=-1);
            }

            url_ok=true;

         }catch (Exception ex) {}        

        return url_ok;
    }
    
    
    private MiscTools() {
    }
    
    public static boolean checkNewVersion(String folder_node, String folder_key) {
        
        boolean new_version = true;
        
        boolean invalid_repo = true;
        
        try {
                MegaAPI ma = new MegaAPI();

                HashMap<String, Object> folder_nodes = ma.getFolderNodes(folder_node, folder_key);
                
                if(folder_nodes == null || folder_nodes.isEmpty()) {
                    
                    new_version = false;
                    
                } else {
                    
                    for(Object o:folder_nodes.values()) {

                        HashMap<String,Object> current_node = (HashMap<String,Object>)o;

                        if(((String)current_node.get("name")).contains("_"+VERSION.replaceAll(" *beta *", "")+".run")) {

                            invalid_repo = false;
                            
                            new_version = false;

                            break;
                            
                        } else if(((String)current_node.get("name")).contains(".run")) {
                            
                            invalid_repo = false;
                        }
                    }
                    
                    if(invalid_repo) {
                        
                        new_version = false;
                    }
                }

            } catch (Exception ex) {
                getLogger(AboutDialog.class.getName()).log(Level.SEVERE, null, ex);
            }
        
        return new_version;
    }
    
}
