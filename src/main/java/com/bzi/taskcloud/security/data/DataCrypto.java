package com.bzi.taskcloud.security.data;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.bzi.taskcloud.common.utils.LoggerUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.security.auth.DestroyFailedException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

@Component
public class DataCrypto {
    private static PrivateKey privateKey;
    private static PublicKey publicKey;
    private static String aesKey;
    private static String iv;
    private static final BouncyCastleProvider aesCryptoProvider = new BouncyCastleProvider();

    @Value("${data-security.data.privateKey}")
    public void setPrivateKey(String privateKey) {
        if (!StringUtils.isNotBlank(privateKey))
            throw new Error("请设置数据解密使用的私钥");

        try {
            DataCrypto.privateKey = KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(Base64.decode(privateKey)));
        } catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
            throw new Error("私钥加载失败");
        }
    }

    @Value("${data-security.data.publicKey}")
    public void setPublicKey(String publicKey) {
        if (!StringUtils.isNotBlank(publicKey))
            throw new Error("请设置数据加密使用的公钥");

        try {
            DataCrypto.publicKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(Base64.decode(publicKey)));
        } catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
            throw new Error("公钥加载失败");
        }
    }

    @Value("${data-security.data.aesKey}")
    public void setAesKey(String aesKey) {
        if (!StringUtils.isNotBlank(aesKey))
            throw new Error("请设置加数据解密使用的AES密钥");

        DataCrypto.aesKey = aesKey;
    }

    @Value("${data-security.data.aesIv}")
    public void setIv(String iv) {
        if (!StringUtils.isNotBlank(iv))
            throw new Error("请设置加数据解密使用的AESIV");

        DataCrypto.iv = iv;
    }

    public static String stringToHex(String text) {
        final char[] charCodes = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
        byte[] sequence = text.getBytes(StandardCharsets.UTF_8);
        StringBuilder result = new StringBuilder();

        for (int i = 0, c = 0; i < sequence.length; i++) {
            c = (sequence[i] & 0x0f0) >> 4;
            result.append(charCodes[c]);

            c = sequence[i] & 0x0f;
            result.append(charCodes[c]);
        }

        return result.toString().trim();
    }

    public static String hexToString(String text) {
        final String hexCodes = "0123456789abcdef";
        char[] sequence = text.toCharArray();
        byte[] result = new byte[text.length() / 2];

        for (int i = 0, c = 0; i < result.length; i++) {
            c = hexCodes.indexOf(sequence[2 * i]) * 16;
            c += hexCodes.indexOf(sequence[2 * i + 1]);

            result[i] = (byte) (c & 0xff);
        }

        return new String(result);
    }

    public static String rsaEncrypt(String data) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException, IOException {
        Cipher cipher = Cipher.getInstance("RSA");

        cipher.init(Cipher.ENCRYPT_MODE, publicKey);

        return new String(Base64.encode(handleCrypto(cipher, data.getBytes(StandardCharsets.UTF_8), 117)));
    }

    public static String rsaDecrypt(String data) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException, IOException {
        Cipher cipher = Cipher.getInstance("RSA");

        cipher.init(Cipher.DECRYPT_MODE, privateKey);

        return new String(handleCrypto(cipher, Base64.decode(data.getBytes(StandardCharsets.UTF_8)), 128), StandardCharsets.UTF_8);
    }

    public static String aesEncrypt(String data, String key, String iv) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException, DestroyFailedException {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7Padding", aesCryptoProvider);
        SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "AES");
        IvParameterSpec ivParameterSpec = new IvParameterSpec(iv.getBytes(StandardCharsets.UTF_8));
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivParameterSpec);

        return new String(Base64.encode(cipher.doFinal(data.getBytes(StandardCharsets.UTF_8))));
    }

    public static String aesDecrypt(String data, String key, String iv) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException, NoSuchProviderException, DestroyFailedException {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7Padding", aesCryptoProvider);
        SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "AES");
        IvParameterSpec ivParameterSpec = new IvParameterSpec(iv.getBytes(StandardCharsets.UTF_8));
        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec);

        return  new String(cipher.doFinal(Base64.decode(data)));
    }

    public static String encrypt(String data) {
        String result = null;

        try {
            result = stringToHex(aesEncrypt(data, aesKey, iv));
        } catch (Exception e) {
            LoggerUtil.failed("内容加密失败", e);
        }

        return result;
    }

    public static String decrypt(String data) {
        String result = null;

        try {
            var root = new ObjectMapper().readTree(aesDecrypt(hexToString(data), aesKey, iv));
            if (!root.has("data") || !root.has("hash"))
                return null;

            String key = rsaDecrypt(root.get("hash").asText());
            result = aesDecrypt(root.get("data").asText(), key, iv);
            if (!StringUtils.isNotBlank(result))
                return null;
        } catch (Exception e) {
            LoggerUtil.failed("内容解密失败", e);
        }

        return result;
    }

    private static byte[] handleCrypto(Cipher cipher, byte[] data, int blockSize) throws BadPaddingException, IllegalBlockSizeException, IOException {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        int offset = 0, remain = 0;

        remain = data.length - offset;
        while(remain > 0) {
            result.write(cipher.doFinal(data, offset, Math.min(blockSize, remain)));

            offset += blockSize;
            remain = data.length - offset;
        }

        return result.toByteArray();
    }
}
