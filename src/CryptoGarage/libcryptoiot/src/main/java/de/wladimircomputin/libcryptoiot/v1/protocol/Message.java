package de.wladimircomputin.libcryptoiot.v1.protocol;


import android.util.Base64;

import java.nio.charset.StandardCharsets;


public class Message {
    public static final int IV_LEN = 12;
    public static final int TAG_LEN = 16;
    public static final int CHALLENGE_LEN = 12;
    private static final Message ERR = new Message();
    public MessageType type = MessageType.NOPE;
    public String challenge_request_b64;
    public String challenge_response_b64;
    public String data_b64;
    public String flags = "";

    public byte[] iv;
    public byte[] tag;
    public byte[] encrypted_message;

    public Message(){}

    public Message parseEncrypted(String raw){
        String[] parts = raw.split(":", -1); //keep empty Strings

        if (parts.length == 4){

            String iv_b64 = parts[1];
            String tag_b64 = parts[2];
            String encrypted_message_b64 = parts[3];

            this.type = MessageType.fromString(parts[0]);
            if (this.type == MessageType.NOPE){
                discardMessage();
                return this;
            }

            if(type == MessageType.ERR){
                this.encrypted_message = encrypted_message_b64.getBytes();
                return this;
            }

            this.iv = Base64.decode(iv_b64, Base64.NO_WRAP);
            if(iv.length != IV_LEN){
                discardMessage();
                return this;
            }

            this.tag = Base64.decode(tag_b64, Base64.NO_WRAP);
            if(tag.length != TAG_LEN){
                discardMessage();
                return this;
            }

            this.encrypted_message = Base64.decode(encrypted_message_b64, Base64.NO_WRAP);
        }
        return this;
    }

    public String toEncryptedString(){
        String iv_b64 = Base64.encodeToString(iv, Base64.NO_WRAP);
        String tag_b64 = Base64.encodeToString(tag, Base64.NO_WRAP);
        String encryptedMessage_b64 = Base64.encodeToString(encrypted_message, Base64.NO_WRAP);

        return type.toString() + ":" + iv_b64 + ":" + tag_b64 + ":" + encryptedMessage_b64;
    }

    public Message encrypt(de.wladimircomputin.libcryptoiot.v1.protocol.Crypter crypter){
        this.iv = crypter.getRandomIV();

        String message_b64 = flags + ":" + challenge_response_b64 + ":" + challenge_request_b64 + ":" + data_b64;

        byte[] encryptedMessageWithTag = crypter.encrypt(message_b64.getBytes(StandardCharsets.UTF_8), iv);

        encrypted_message = new byte[encryptedMessageWithTag.length - TAG_LEN];
        tag = new byte[TAG_LEN];

        System.arraycopy(encryptedMessageWithTag, 0, encrypted_message, 0, encrypted_message.length);
        System.arraycopy(encryptedMessageWithTag, encrypted_message.length, tag, 0, tag.length);

        return this;
    }

    public Message decrypt_verify(de.wladimircomputin.libcryptoiot.v1.protocol.Crypter crypter, String challenge_request_b64){
        try {
            byte[] raw_message = crypter.decrypt(encrypted_message, iv, tag);
            if(raw_message != null){
                String raw_message2 = new String(raw_message);
                String[] parts = raw_message2.split(":", -1);//keep empty Strings
                if (parts.length == 4){
                    this.flags = parts[0];
                    String challenge_response_b64 = parts[1];
                    this.challenge_request_b64 = parts[2];

                    if(type == MessageType.ERR){
                        this.data_b64 = parts[3];
                        return this;
                    }

                    if(verifyChallenge(challenge_response_b64, challenge_request_b64)){
                        this.data_b64 = parts[3];
                        return this;
                    }
                }
            }
            } catch (Exception x){}
        discardMessage();
        return this;
    }

    public String getDataString(){
        try {
            return new String(Base64.decode(data_b64, Base64.NO_WRAP));
        } catch (Exception x){}
        return "";
    }

    public byte[] getDataBinary(){
        try {
            return Base64.decode(data_b64, Base64.NO_WRAP);
        } catch (Exception x){}
        return null;
    }

    private boolean verifyChallenge(String challenge_request_b64, String challenge_response_b64){
        return challenge_request_b64.equals(challenge_response_b64) && !challenge_request_b64.isEmpty();
    }

    private void discardMessage(){
        this.type = MessageType.NOPE;
    }
}
