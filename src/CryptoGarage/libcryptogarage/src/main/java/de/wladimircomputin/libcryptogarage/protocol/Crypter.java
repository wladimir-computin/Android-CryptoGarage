package de.wladimircomputin.libcryptogarage.protocol;

import android.content.Context;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import de.wladimircomputin.libcryptogarage.R;

/**
 * Created by spamd on 08.03.2017.
 */

public class Crypter {
    private SecretKeySpec secretKeySpec;
    private Cipher aes;
    private SecureRandom random;

    private final static int GCM_IV_LENGTH = 12;
    private final static int GCM_TAG_LENGTH = 16;


    public Crypter(String devicePass, Context context){
        byte[] key = (devicePass + context.getResources().getString(R.string.key_salt)).getBytes(StandardCharsets.UTF_8);
        this.random = new SecureRandom();
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-512");
            int count = context.getResources().getInteger(R.integer.sha_rounds) + 1;
            for(int i = 0; i < count; i++){
                key = sha.digest(key);
            }
            this.secretKeySpec = new SecretKeySpec(Arrays.copyOf(key, 32), "AES");
            this.aes = Cipher.getInstance("AES/GCM/NoPadding");
        } catch (Exception x){
            x.printStackTrace();
        }
    }

    public byte[] encrypt(byte[] message, byte[] iv){
        if (iv.length == GCM_IV_LENGTH && message != null) {
            try {
                GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH * Byte.SIZE, iv);
                aes.init(Cipher.ENCRYPT_MODE, secretKeySpec, spec);
                return aes.doFinal(message);
            } catch (Exception x) {
                x.printStackTrace();
            }
        }
        return null;
    }

    public byte[] decrypt(byte[] message, byte[] iv, byte[] tag){
        if (iv.length == GCM_IV_LENGTH && tag.length == GCM_TAG_LENGTH && message != null) {
            try {
                byte[] messageWithTag = new byte[message.length + tag.length];
                System.arraycopy(message, 0, messageWithTag, 0, message.length);
                System.arraycopy(tag, 0, messageWithTag, message.length, tag.length);
                GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH * Byte.SIZE, iv);
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

    public byte[] getRandomChallenge(){
        byte[] challenge = new byte[12];
        random.nextBytes(challenge);
        return challenge;
    }
}
