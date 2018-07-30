package com.android.server.secrecy;

import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;
import javax.crypto.Cipher;

public class RSA {
    private static final String ALGORITHM = "RSA";

    static void initRsaKey() {
        try {
            BigInteger modulus;
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(ALGORITHM);
            keyPairGenerator.initialize(2048);
            KeyPair keyPair = keyPairGenerator.generateKeyPair();
            PublicKey publicKey = keyPair.getPublic();
            PrivateKey privateKey = keyPair.getPrivate();
            try {
                RSAPublicKeySpec rsaPublicKeySpec = (RSAPublicKeySpec) KeyFactory.getInstance(ALGORITHM).getKeySpec(publicKey, RSAPublicKeySpec.class);
                modulus = rsaPublicKeySpec.getModulus();
                BigInteger publicExponent = rsaPublicKeySpec.getPublicExponent();
            } catch (GeneralSecurityException e) {
                e.printStackTrace();
            }
            try {
                RSAPrivateKeySpec rsaPrivateKeySpec = (RSAPrivateKeySpec) KeyFactory.getInstance(ALGORITHM).getKeySpec(privateKey, RSAPrivateKeySpec.class);
                modulus = rsaPrivateKeySpec.getModulus();
                BigInteger privateExponent = rsaPrivateKeySpec.getPrivateExponent();
            } catch (GeneralSecurityException e2) {
                e2.printStackTrace();
            }
        } catch (NoSuchAlgorithmException e3) {
            e3.printStackTrace();
        }
    }

    public static byte[] shaDigest(String imei) {
        byte[] digest = null;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(imei.getBytes("utf-8"));
            return md.digest();
        } catch (Exception e) {
            e.printStackTrace();
            return digest;
        }
    }

    static byte[] shrink(byte[] data, int length) {
        int pieces = data.length / length;
        byte[] result = new byte[pieces];
        for (int p = 0; p < pieces; p++) {
            result[p] = data[p * length];
            for (int i = 1; i < length; i++) {
                result[p] = (byte) (result[p] ^ data[(p * length) + i]);
            }
        }
        return result;
    }

    public static PublicKey initRsaPublicKey(BigInteger modulus, BigInteger exponent) {
        PublicKey publicKey = null;
        try {
            return KeyFactory.getInstance(ALGORITHM).generatePublic(new RSAPublicKeySpec(modulus, exponent));
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
            return publicKey;
        }
    }

    static PrivateKey initRsaPrivateKey(BigInteger modulus, BigInteger privateExponent) {
        PrivateKey privateKey = null;
        try {
            return KeyFactory.getInstance(ALGORITHM).generatePrivate(new RSAPrivateKeySpec(modulus, privateExponent));
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
            return privateKey;
        }
    }

    public static byte[] decrypt(PublicKey publicKey, byte[] cipherText) {
        byte[] deciphered = null;
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(2, publicKey);
            return cipher.doFinal(cipherText);
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
            return deciphered;
        }
    }

    static byte[] encrypt(PrivateKey privateKey, byte[] text) {
        byte[] ciphered = null;
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(1, privateKey);
            return cipher.doFinal(text);
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
            return ciphered;
        }
    }
}
