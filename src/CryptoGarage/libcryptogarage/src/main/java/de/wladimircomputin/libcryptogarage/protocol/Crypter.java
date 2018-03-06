package de.wladimircomputin.libcryptogarage.protocol;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Created by spamd on 08.03.2017.
 */

public class Crypter {
    private SecretKeySpec secretKeySpec;
    private Cipher aes;
    private SecureRandom random;

    public Crypter(String devicePass){
        byte[] key = devicePass.getBytes(StandardCharsets.UTF_8);
        this.random = new SecureRandom();
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            key = sha.digest(key);
            this.secretKeySpec = new SecretKeySpec(key, "AES");
            this.aes = Cipher.getInstance("AES/GCM/NoPadding");
        } catch (Exception x){
            x.printStackTrace();
        }
    }

    public byte[] encrypt(byte[] message, byte[] iv){
        if (iv.length == 12 && message != null) {
            try {
                final GCMParameterSpec spec = new GCMParameterSpec(16 * 8, iv);
                aes.init(Cipher.ENCRYPT_MODE, secretKeySpec, spec);
                return aes.doFinal(message);
            } catch (Exception x) {
                x.printStackTrace();
            }
        }
        return null;
    }

    public byte[] decrypt(byte[] message, byte[] iv, byte[] tag){
        if (iv.length == 12 && tag.length == 16 &&  message != null) {
            try {
                byte[] messageWithTag = new byte[message.length + tag.length];
                System.arraycopy(message, 0, messageWithTag, 0, message.length);
                System.arraycopy(tag, 0, messageWithTag, message.length, tag.length);
                final GCMParameterSpec spec = new GCMParameterSpec(16 * 8, iv);
                aes.init(Cipher.DECRYPT_MODE, secretKeySpec, spec);
                return aes.doFinal(messageWithTag);
            } catch (Exception x) {
                x.printStackTrace();
            }
        }
        return null;
    }

    public byte[] getRandomIV(){
        byte[] iv = new byte[12];
        random.nextBytes(iv);
        return iv;
    }
}
