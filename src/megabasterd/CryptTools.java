package megabasterd;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.RSAPrivateKeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.JOptionPane;
import static megabasterd.MiscTools.BASE642Bin;
import static megabasterd.MiscTools.Bin2UrlBASE64;
import static megabasterd.MiscTools.UrlBASE642Bin;
import static megabasterd.MiscTools.bin2i32a;
import static megabasterd.MiscTools.findFirstRegex;
import static megabasterd.MiscTools.getApacheKissHttpClient;
import static megabasterd.MiscTools.hex2bin;
import static megabasterd.MiscTools.i32a2bin;
import static megabasterd.MiscTools.long2bytearray;
import static megabasterd.MiscTools.recReverseArray;
import static megabasterd.MiscTools.swingReflectionInvokeAndWait;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.codehaus.jackson.map.ObjectMapper;

public final class CryptTools {

    public static final int[] AES_ZERO_IV_I32A = {0, 0, 0, 0};

    public static final byte[] AES_ZERO_IV = i32a2bin(AES_ZERO_IV_I32A);

    public static final int PBKDF2_SALT_BYTE_LENGTH = 16;

    public static final int PBKDF2_ITERATIONS = 0x10000;

    public static Cipher genDecrypter(String algo, String mode, byte[] key, byte[] iv) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException {
        SecretKeySpec skeySpec = new SecretKeySpec(key, algo);

        Cipher decryptor = Cipher.getInstance(mode);

        if (iv != null) {

            IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);

            decryptor.init(Cipher.DECRYPT_MODE, skeySpec, ivParameterSpec);

        } else {

            decryptor.init(Cipher.DECRYPT_MODE, skeySpec);
        }

        return decryptor;
    }

    public static Cipher genCrypter(String algo, String mode, byte[] key, byte[] iv) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException {
        SecretKeySpec skeySpec = new SecretKeySpec(key, algo);

        Cipher cryptor = Cipher.getInstance(mode);

        if (iv != null) {

            IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);

            cryptor.init(Cipher.ENCRYPT_MODE, skeySpec, ivParameterSpec);

        } else {

            cryptor.init(Cipher.ENCRYPT_MODE, skeySpec);
        }

        return cryptor;
    }

    public static byte[] aes_cbc_encrypt(byte[] data, byte[] key, byte[] iv) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {

        Cipher cryptor = CryptTools.genCrypter("AES", "AES/CBC/NoPadding", key, iv);

        return cryptor.doFinal(data);
    }

    public static byte[] aes_cbc_encrypt_pkcs7(byte[] data, byte[] key, byte[] iv) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {

        Cipher cryptor = CryptTools.genCrypter("AES", "AES/CBC/PKCS5Padding", key, iv);

        return cryptor.doFinal(data);
    }

    public static byte[] aes_cbc_decrypt(byte[] data, byte[] key, byte[] iv) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {

        Cipher decryptor = CryptTools.genDecrypter("AES", "AES/CBC/NoPadding", key, iv);

        return decryptor.doFinal(data);
    }

    public static byte[] aes_cbc_decrypt_pkcs7(byte[] data, byte[] key, byte[] iv) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {

        Cipher decryptor = CryptTools.genDecrypter("AES", "AES/CBC/PKCS5Padding", key, iv);

        return decryptor.doFinal(data);
    }

    public static byte[] aes_ecb_encrypt(byte[] data, byte[] key) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {

        Cipher cryptor = CryptTools.genCrypter("AES", "AES/ECB/NoPadding", key, null);

        return cryptor.doFinal(data);
    }

    public static byte[] aes_ecb_decrypt(byte[] data, byte[] key) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {

        Cipher decryptor = CryptTools.genDecrypter("AES", "AES/ECB/NoPadding", key, null);

        return decryptor.doFinal(data);
    }

    public static byte[] aes_ctr_encrypt(byte[] data, byte[] key, byte[] iv) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {

        Cipher cryptor = CryptTools.genCrypter("AES", "AES/CTR/NoPadding", key, iv);

        return cryptor.doFinal(data);
    }

    public static byte[] aes_ctr_decrypt(byte[] data, byte[] key, byte[] iv) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {

        Cipher decryptor = CryptTools.genDecrypter("AES", "AES/CTR/NoPadding", key, iv);

        return decryptor.doFinal(data);
    }

    public static int[] aes_cbc_encrypt_ia32(int[] data, int[] key, int[] iv) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {

        Cipher cryptor = CryptTools.genCrypter("AES", "AES/CBC/NoPadding", i32a2bin(key), i32a2bin(iv));

        return bin2i32a(cryptor.doFinal(i32a2bin(data)));
    }

    public static int[] aes_cbc_decrypt_ia32(int[] data, int[] key, int[] iv) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {

        Cipher decryptor = CryptTools.genDecrypter("AES", "AES/CBC/NoPadding", i32a2bin(key), i32a2bin(iv));

        return bin2i32a(decryptor.doFinal(i32a2bin(data)));
    }

    public static int[] aes_ecb_encrypt_ia32(int[] data, int[] key, int[] iv) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {

        Cipher cryptor = CryptTools.genCrypter("AES", "AES/ECB/NoPadding", i32a2bin(key), i32a2bin(iv));

        return bin2i32a(cryptor.doFinal(i32a2bin(data)));
    }

    public static int[] aes_ecb_decrypt_ia32(int[] data, int[] key, int[] iv) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {

        Cipher decryptor = CryptTools.genDecrypter("AES", "AES/ECB/NoPadding", i32a2bin(key), i32a2bin(iv));

        return bin2i32a(decryptor.doFinal(i32a2bin(data)));
    }

    public static int[] aes_ctr_encrypt_ia32(int[] data, int[] key, int[] iv) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {

        Cipher cryptor = CryptTools.genCrypter("AES", "AES/CTR/NoPadding", i32a2bin(key), i32a2bin(iv));

        return bin2i32a(cryptor.doFinal(i32a2bin(data)));
    }

    public static int[] aes_ctr_decrypt_ia32(int[] data, int[] key, int[] iv) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {

        Cipher decryptor = CryptTools.genDecrypter("AES", "AES/CTR/NoPadding", i32a2bin(key), i32a2bin(iv));

        return bin2i32a(decryptor.doFinal(i32a2bin(data)));
    }

    public static byte[] rsaDecrypt(BigInteger enc_data, BigInteger p, BigInteger q, BigInteger d) throws NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {

        RSAPrivateKeySpec privateSpec = new RSAPrivateKeySpec(p.multiply(q), d);

        KeyFactory factory = KeyFactory.getInstance("RSA");

        Cipher cipher = Cipher.getInstance("RSA/ECB/NoPadding");

        RSAPrivateKey privKey = (RSAPrivateKey) factory.generatePrivate(privateSpec);

        cipher.init(Cipher.DECRYPT_MODE, privKey);

        byte[] enc_data_byte = enc_data.toByteArray();

        if (enc_data_byte[0] == 0) {

            enc_data_byte = Arrays.copyOfRange(enc_data_byte, 1, enc_data_byte.length);
        }

        byte[] plainText = cipher.doFinal(enc_data_byte);

        if (plainText[0] == 0) {

            plainText = Arrays.copyOfRange(plainText, 1, plainText.length);
        }

        return plainText;
    }

    public static byte[] initMEGALinkKey(String key_string) throws Exception {
        int[] int_key = bin2i32a(UrlBASE642Bin(key_string));
        int[] k = new int[4];

        k[0] = int_key[0] ^ int_key[4];
        k[1] = int_key[1] ^ int_key[5];
        k[2] = int_key[2] ^ int_key[6];
        k[3] = int_key[3] ^ int_key[7];

        return i32a2bin(k);
    }

    public static byte[] initMEGALinkKeyIV(String key_string) throws Exception {
        int[] int_key = bin2i32a(UrlBASE642Bin(key_string));
        int[] iv = new int[4];

        iv[0] = int_key[4];
        iv[1] = int_key[5];
        iv[2] = 0;
        iv[3] = 0;

        return i32a2bin(iv);
    }

    public static byte[] forwardMEGALinkKeyIV(byte[] iv, long forward_bytes) {
        byte[] new_iv = new byte[iv.length];

        System.arraycopy(iv, 0, new_iv, 0, iv.length / 2);

        byte[] ctr = long2bytearray(forward_bytes / iv.length);

        System.arraycopy(ctr, 0, new_iv, iv.length / 2, ctr.length);

        return new_iv;
    }

    public static String decryptMegaDownloaderLink(String link) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, Exception, IllegalBlockSizeException, BadPaddingException {
        String[] keys = {"6B316F36416C2D316B7A3F217A30357958585858585858585858585858585858", "ED1F4C200B35139806B260563B3D3876F011B4750F3A1A4A5EFD0BBE67554B44"};
        String iv = "79F10A01844A0B27FF5B2D4E0ED3163E";

        String enc_type, folder, dec_link;

        if ((enc_type = findFirstRegex("mega://f?(enc[0-9]*)\\?", link, 1)) != null) {
            Cipher decrypter;

            String the_key = null;

            switch (enc_type.toLowerCase()) {
                case "enc":
                    the_key = keys[0];
                    break;
                case "enc2":
                    the_key = keys[1];
                    break;
            }

            folder = findFirstRegex("mega://(f)?enc[0-9]*\\?", link, 1);

            decrypter = CryptTools.genDecrypter("AES", "AES/CBC/NoPadding", hex2bin(the_key), hex2bin(iv));

            byte[] decrypted_data = decrypter.doFinal(UrlBASE642Bin(findFirstRegex("mega://f?enc[0-9]*\\?([\\da-zA-Z_,-]*)", link, 1)));

            dec_link = new String(decrypted_data).trim();

            return "https://mega.nz/#" + (folder != null ? "f" : "") + dec_link;

        } else {
            return link;
        }
    }

    public static HashSet<String> decryptELC(String link, MainPanel main_panel) {

        String elc;

        HashSet<String> links = new HashSet<>();

        if ((elc = findFirstRegex("mega://elc\\?([0-9a-zA-Z,_-]+)", link, 1)) != null) {

            try {

                byte[] elc_byte = UrlBASE642Bin(elc);

                boolean compression;

                if (((int) elc_byte[0] & 0xFF) != 112 && ((int) elc_byte[0] & 0xFF) != 185) {

                    throw new Exception("BAD ELC!");

                } else {

                    compression = (((int) elc_byte[0] & 0xFF) == 112);
                }

                elc_byte = Arrays.copyOfRange(elc_byte, 1, elc_byte.length);

                if (compression) {

                    InputStream is = new GZIPInputStream(new ByteArrayInputStream(elc_byte));

                    ByteArrayOutputStream out = new ByteArrayOutputStream();

                    byte[] buffer = new byte[16 * 1024];

                    int reads;

                    while ((reads = is.read(buffer)) != -1) {

                        out.write(buffer, 0, reads);
                    }

                    elc_byte = out.toByteArray();
                }

                int bin_links_length = ByteBuffer.wrap(recReverseArray(Arrays.copyOfRange(elc_byte, 0, 4), 0, 3)).getInt();

                byte[] bin_links = Arrays.copyOfRange(elc_byte, 4, 4 + bin_links_length);

                short url_bin_length = ByteBuffer.wrap(recReverseArray(Arrays.copyOfRange(elc_byte, 4 + bin_links_length, 4 + bin_links_length + 2), 0, 1)).getShort();

                byte[] url_bin = Arrays.copyOfRange(elc_byte, 4 + bin_links_length + 2, 4 + bin_links_length + 2 + url_bin_length);

                if (!new String(url_bin).contains("http")) {

                    throw new Exception("BAD ELC HOST URL!");
                }

                short pass_bin_length = ByteBuffer.wrap(recReverseArray(Arrays.copyOfRange(elc_byte, 4 + bin_links_length + 2 + url_bin_length, 4 + bin_links_length + 2 + url_bin_length + 2), 0, 1)).getShort();

                byte[] pass_bin = Arrays.copyOfRange(elc_byte, 4 + bin_links_length + 2 + url_bin_length + 2, 4 + bin_links_length + 2 + url_bin_length + 2 + pass_bin_length);

                try (CloseableHttpClient httpclient = getApacheKissHttpClient()) {

                    HttpPost httppost = new HttpPost(new String(url_bin));

                    httppost.setHeader("Custom-User-Agent", MainPanel.DEFAULT_USER_AGENT);

                    ArrayList<NameValuePair> nameValuePairs = new ArrayList<>();

                    nameValuePairs.add(new BasicNameValuePair("OPERATION_TYPE", "D"));

                    nameValuePairs.add(new BasicNameValuePair("DATA", new String(pass_bin)));

                    HashMap<String, String> elc_account_data;

                    String user, api_key;

                    boolean remember_master_pass;

                    if (main_panel.getElc_accounts().get(httppost.getURI().getHost()) != null) {

                        elc_account_data = (HashMap) main_panel.getElc_accounts().get(httppost.getURI().getHost());

                        if (main_panel.getMaster_pass_hash() != null) {

                            if (main_panel.getMaster_pass() == null) {

                                GetMasterPasswordDialog dialog = new GetMasterPasswordDialog(main_panel.getView(), true, main_panel.getMaster_pass_hash(), main_panel.getMaster_pass_salt());

                                swingReflectionInvokeAndWait("setLocationRelativeTo", dialog, main_panel.getView());

                                swingReflectionInvokeAndWait("setVisible", dialog, true);

                                if (dialog.isPass_ok()) {

                                    main_panel.setMaster_pass(dialog.getPass());

                                    dialog.deletePass();

                                    remember_master_pass = dialog.getRemember_checkbox().isSelected();

                                    dialog.dispose();

                                    user = new String(CryptTools.aes_cbc_decrypt_pkcs7(BASE642Bin((String) elc_account_data.get("user")), main_panel.getMaster_pass(), CryptTools.AES_ZERO_IV));

                                    api_key = new String(CryptTools.aes_cbc_decrypt_pkcs7(BASE642Bin((String) elc_account_data.get("apikey")), main_panel.getMaster_pass(), CryptTools.AES_ZERO_IV));

                                    if (!remember_master_pass) {

                                        main_panel.setMaster_pass(null);
                                    }

                                } else {

                                    dialog.dispose();

                                    throw new Exception("NO valid ELC account available!");
                                }

                            } else {

                                user = new String(CryptTools.aes_cbc_decrypt_pkcs7(BASE642Bin((String) elc_account_data.get("user")), main_panel.getMaster_pass(), CryptTools.AES_ZERO_IV));

                                api_key = new String(CryptTools.aes_cbc_decrypt_pkcs7(BASE642Bin((String) elc_account_data.get("apikey")), main_panel.getMaster_pass(), CryptTools.AES_ZERO_IV));

                            }

                        } else {

                            user = (String) elc_account_data.get("user");

                            api_key = (String) elc_account_data.get("apikey");
                        }

                    } else {

                        throw new Exception("NO valid ELC account available!");
                    }

                    nameValuePairs.add(new BasicNameValuePair("USER", user));

                    nameValuePairs.add(new BasicNameValuePair("APIKEY", api_key));

                    httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

                    try (CloseableHttpResponse httpresponse = httpclient.execute(httppost)) {

                        InputStream is = httpresponse.getEntity().getContent();

                        ByteArrayOutputStream out = new ByteArrayOutputStream();

                        byte[] buffer = new byte[16 * 1024];

                        int reads;

                        while ((reads = is.read(buffer)) != -1) {

                            out.write(buffer, 0, reads);
                        }

                        ObjectMapper objectMapper = new ObjectMapper();

                        HashMap res_map = objectMapper.readValue(new String(out.toByteArray()), HashMap.class);

                        String dec_pass = (String) res_map.get("d");

                        if (dec_pass != null && dec_pass.length() > 0) {

                            dec_pass = (String) res_map.get("d");

                            byte[] pass_dec_byte = MiscTools.BASE642Bin(dec_pass);

                            byte[] key = Arrays.copyOfRange(pass_dec_byte, 0, 16);

                            byte[] iv = new byte[16];

                            Arrays.fill(iv, (byte) 0);

                            System.arraycopy(pass_dec_byte, 16, iv, 0, 8);

                            byte[] bin_links_dec = CryptTools.aes_cbc_decrypt(bin_links, key, iv);

                            String[] links_string = (new String(bin_links_dec).trim()).split("\\|");

                            for (String s : links_string) {

                                links.add("https://mega.nz/" + s);
                            }

                        } else {
                            throw new Exception(httppost.getURI().getAuthority() + " ELC server ERROR -> " + new String(out.toByteArray()));
                        }
                    }
                }

            } catch (Exception ex) {
                Logger.getLogger(MiscTools.class.getName()).log(Level.SEVERE, null, ex);
                JOptionPane.showMessageDialog(main_panel.getView(), ex.getMessage(), "ELC decryption Error", JOptionPane.ERROR_MESSAGE);
            }
        }

        return links;
    }

    public static String MEGAUserHash(byte[] str, int[] aeskey) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException, Exception {

        int[] s32 = bin2i32a(str);

        int[] h32 = {0, 0, 0, 0};

        int[] iv = {0, 0, 0, 0};

        for (int i = 0; i < s32.length; i++) {

            h32[i % 4] ^= s32[i];
        }

        for (int i = 0; i < 0x4000; i++) {

            h32 = CryptTools.aes_cbc_encrypt_ia32(h32, aeskey, iv);
        }

        int[] res = {h32[0], h32[2]};

        return Bin2UrlBASE64(i32a2bin(res));
    }

    public static int[] MEGAPrepareMasterKey(int[] key) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {

        int[] pkey = {0x93C467E3, 0x7DB0_C7A4, 0xD1BE_3F81, 0x0152_CB56};

        int[] iv = {0, 0, 0, 0};

        for (int r = 0; r < 0x10000; r++) {

            for (int j = 0; j < key.length; j += 4) {

                int[] k = {0, 0, 0, 0};

                for (int i = 0; i < 4; i++) {

                    if (i + j < key.length) {

                        k[i] = key[i + j];
                    }
                }

                pkey = CryptTools.aes_cbc_encrypt_ia32(pkey, k, iv);
            }
        }

        return pkey;
    }

    public static byte[] PBKDF2HMACSHA256(String password, byte[] salt, int iterations) throws NoSuchAlgorithmException, InvalidKeySpecException {

        SecretKeyFactory f = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");

        KeySpec ks = new PBEKeySpec(password.toCharArray(), salt, iterations, 256);

        return f.generateSecret(ks).getEncoded();
    }

    private CryptTools() {
    }
}
