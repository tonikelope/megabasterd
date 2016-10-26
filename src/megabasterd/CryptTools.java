package megabasterd;

import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.RSAPrivateKeySpec;
import java.util.Arrays;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import static megabasterd.MiscTools.Bin2UrlBASE64;
import static megabasterd.MiscTools.UrlBASE642Bin;
import static megabasterd.MiscTools.bin2i32a;
import static megabasterd.MiscTools.findFirstRegex;
import static megabasterd.MiscTools.hex2bin;
import static megabasterd.MiscTools.i32a2bin;
import static megabasterd.MiscTools.long2bytearray;

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
