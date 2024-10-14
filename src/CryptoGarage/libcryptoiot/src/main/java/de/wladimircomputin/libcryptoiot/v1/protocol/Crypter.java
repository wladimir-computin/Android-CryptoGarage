package de.wladimircomputin.libcryptoiot.v1.protocol;

import static de.wladimircomputin.libcryptoiot.v1.Constants.key_salt;
import static de.wladimircomputin.libcryptoiot.v1.Constants.sha_rounds;
import static de.wladimircomputin.libcryptoiot.v1.protocol.Message.CHALLENGE_LEN;

import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;

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
    private String aeskey;
    private String aeskey_probe;

    public final static int AES_GCM_IV_LEN = 12;
    public final static int AES_GCM_TAG_LEN = 16;


    public Crypter(String devicePass){
        this(devicePass, "", "");
    }
    public Crypter(String devicePass, String key1, String key1_probe){

        this.random = new SecureRandom();
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-512");
            int count = sha_rounds + 1;
            byte[] key = (devicePass + key_salt).getBytes(StandardCharsets.UTF_8);
            aeskey_probe = Base64.encodeToString(sha.digest(key), Base64.NO_WRAP);
            if(!key1_probe.isEmpty() && aeskey_probe.equals(key1_probe)){
                key = Base64.decode(key1, Base64.NO_WRAP);
                aeskey = key1;
            } else {
                for (int i = 0; i < count; i++) {
                    key = sha.digest(key);
                }
                aeskey = Base64.encodeToString(key, Base64.NO_WRAP);
            }

            this.secretKeySpec = new SecretKeySpec(Arrays.copyOf(key, 32), "AES");
            this.aes = Cipher.getInstance("AES/GCM/NoPadding");
        } catch (Exception x){
            x.printStackTrace();
        }
    }

    public byte[] encrypt(byte[] message, byte[] iv){
        if (iv.length == AES_GCM_IV_LEN && message != null) {
            try {
                GCMParameterSpec spec = new GCMParameterSpec(AES_GCM_TAG_LEN * Byte.SIZE, iv);
                aes.init(Cipher.ENCRYPT_MODE, secretKeySpec, spec);
                return aes.doFinal(message);
            } catch (Exception x) {
                x.printStackTrace();
            }
        }
        return null;
    }

    public byte[] decrypt(byte[] message, byte[] iv, byte[] tag){
        if (iv.length == AES_GCM_IV_LEN && tag.length == AES_GCM_TAG_LEN && message != null) {
            try {
                byte[] messageWithTag = new byte[message.length + tag.length];
                System.arraycopy(message, 0, messageWithTag, 0, message.length);
                System.arraycopy(tag, 0, messageWithTag, message.length, tag.length);
                GCMParameterSpec spec = new GCMParameterSpec(AES_GCM_TAG_LEN * Byte.SIZE, iv);
                aes.init(Cipher.DECRYPT_MODE, secretKeySpec, spec);
                return aes.doFinal(messageWithTag);
            } catch (Exception x) {
                x.printStackTrace();
            }
        }
        return null;
    }

    public byte[] getRandom(int len) {
        byte[] out = new byte[len];
        random.nextBytes(out);
        return out;
    }

    public byte[] getRandomIV(){
        return getRandom(AES_GCM_IV_LEN);
    }

    public byte[] getRandomChallenge(){
        return getRandom(CHALLENGE_LEN);
    }

    public String getAesKey(){
        return aeskey;
    }

    public String getAesKeyProbe(){
        return aeskey_probe;
    }
}
