package com.bzi.taskcloud.security.data;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.security.auth.DestroyFailedException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

@Component
public class PassportCrypto {
    private static String aesKey;
    private static String iv;

    @Value("${data-security.passport.aesKey}")
    public void setAesKey(String aesKey) {
        if (!StringUtils.isNotBlank(aesKey))
            throw new Error("请设置通行证加解密使用的AES密钥");

        PassportCrypto.aesKey = aesKey;
    }

    @Value("${data-security.passport.aesIv}")
    public void setIv(String iv) {
        if (!StringUtils.isNotBlank(iv))
            throw new Error("请设置通行证加解密使用的AESIV");

        PassportCrypto.iv = iv;
    }

    public static String encrypt(String data) throws DestroyFailedException, InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        if (StringUtils.isBlank(data))
            return data;

        return DataCrypto.stringToHex(
                DataCrypto.aesEncrypt(
                        data,
                        aesKey,
                        iv
                )
        );
    }

    public static String decrypt(String data) throws DestroyFailedException, InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException, NoSuchProviderException {
        if (StringUtils.isBlank(data))
            return data;

        return DataCrypto.aesDecrypt(DataCrypto.hexToString(data), aesKey, iv);
    }
}
