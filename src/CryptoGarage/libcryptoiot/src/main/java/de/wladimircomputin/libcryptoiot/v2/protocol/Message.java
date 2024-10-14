package de.wladimircomputin.libcryptoiot.v2.protocol;


import static de.wladimircomputin.libcryptoiot.v2.Constants.ciot_v2_message_header;
import static de.wladimircomputin.libcryptoiot.v2.protocol.ChallengeManager.CHALLENGE_LEN;
import static de.wladimircomputin.libcryptoiot.v2.protocol.Crypter.AES_GCM_IV_LEN;
import static de.wladimircomputin.libcryptoiot.v2.protocol.Crypter.AES_GCM_TAG_LEN;

import android.util.Base64;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;


public class Message {
    public static final int FLAGS_LEN = 5;

    public MessageType type = MessageType.NOPE;
    public byte[] challenge_request = new byte[CHALLENGE_LEN];
    public byte[] challenge_response  = new byte[CHALLENGE_LEN];
    public byte[] data;
    public String flags = "";

    public byte[] iv = new byte[AES_GCM_IV_LEN];
    public byte[] tag = new byte[AES_GCM_TAG_LEN];
    public byte[] encrypted_message;

    public Message(){}

    public Message(MessageType type, byte[] challenge_response, byte[] challenge_request, String commnand){
        this.type = type;
        this.challenge_response = challenge_response;
        this.challenge_request = challenge_request;
        this.data = commnand.getBytes(StandardCharsets.UTF_8);
    }

    public Message parseEncrypted(String raw){
        if(raw.startsWith(ciot_v2_message_header)) {
            raw = raw.replace(ciot_v2_message_header, "");
            byte[] packet = Base64.decode(raw, Base64.NO_WRAP);
            if (packet != null && packet.length >= AES_GCM_IV_LEN + AES_GCM_TAG_LEN + 1 + 1 + FLAGS_LEN + 1 + CHALLENGE_LEN + CHALLENGE_LEN) {
                ByteBuffer buffer = ByteBuffer.wrap(packet);
                buffer.get(this.iv, 0, AES_GCM_IV_LEN);
                buffer.get(this.tag, 0, AES_GCM_TAG_LEN);
                encrypted_message = new byte[buffer.remaining()];
                buffer.get(this.encrypted_message, 0, buffer.remaining());
            } else {
                discardMessage();
            }
        }
        return this;
    }

    public Message encrypt(de.wladimircomputin.libcryptoiot.v2.protocol.Crypter crypter){
        int cleartext_len = 1 + 1 + FLAGS_LEN + 1 + CHALLENGE_LEN + CHALLENGE_LEN + data.length;

        byte[] buffer = new byte[cleartext_len];

        ByteBuffer message = ByteBuffer.wrap(buffer);
        message.put(type.toString().getBytes(StandardCharsets.UTF_8));
        message.put((byte)':');
        message.put(flagsToBytes(flags), 0, FLAGS_LEN);
        message.put((byte)':');
        if(challenge_response == null){
            challenge_response = new byte[CHALLENGE_LEN];
        }
        message.put(challenge_response, 0, CHALLENGE_LEN);
        message.put(challenge_request, 0, CHALLENGE_LEN);
        message.put(data, 0, data.length);

        this.iv = crypter.getRandom(AES_GCM_IV_LEN);
        byte[] encryptedMessageWithTag = crypter.encrypt(message.array(), iv);

        this.encrypted_message = new byte[encryptedMessageWithTag.length - AES_GCM_TAG_LEN];
        this.tag = new byte[AES_GCM_TAG_LEN];

        System.arraycopy(encryptedMessageWithTag, 0, this.encrypted_message, 0, this.encrypted_message.length);
        System.arraycopy(encryptedMessageWithTag, this.encrypted_message.length, this.tag, 0, AES_GCM_TAG_LEN);

        return this;
    }

    public Message decrypt_verify(Crypter crypter, ChallengeManager challengeManager){
        try {
            if(encrypted_message.length >= 1 + 1 + FLAGS_LEN + 1 + CHALLENGE_LEN + CHALLENGE_LEN) {
                byte[] raw_message = crypter.decrypt(encrypted_message, iv, tag);
                if (raw_message != null) {
                    ByteBuffer message = ByteBuffer.wrap(raw_message);
                    this.type = MessageType.fromString(String.valueOf((char)message.get()));
                    if (type != MessageType.ERR && type != MessageType.NOPE) {
                        if (message.get() == (byte)':') {
                            this.flags = "";
                            for (int i=0; i<FLAGS_LEN; i++){
                                byte c = message.get();
                                if(c != 0x00) {
                                    this.flags += String.valueOf((char) c);
                                }
                            }
                            if (message.get() == (byte)':') {
                                message.get(this.challenge_response, 0, CHALLENGE_LEN);
                                if (challengeManager.verifyChallenge(this.challenge_response)) {
                                    message.get(this.challenge_request, 0, CHALLENGE_LEN);
                                    this.data = new byte[message.remaining()];
                                    message.get(this.data, 0, message.remaining());
                                } else {
                                    discardMessage();
                                }
                            } else {
                                discardMessage();
                            }
                        } else {
                            discardMessage();
                        }
                    }
                }
            }
        } catch (Exception x) {
            x.printStackTrace();
        }
        return this;
    }

    public String toEncryptedString(){
        int packet_len = AES_GCM_IV_LEN + AES_GCM_TAG_LEN + encrypted_message.length;
        byte[] buffer = new byte[packet_len];
        ByteBuffer packet = ByteBuffer.wrap(buffer);
        packet.put(this.iv, 0, AES_GCM_IV_LEN);
        packet.put(this.tag, 0, AES_GCM_TAG_LEN);
        packet.put(this.encrypted_message, 0, this.encrypted_message.length);
        return ciot_v2_message_header + Base64.encodeToString(packet.array(), Base64.NO_WRAP);
    }

    public String getDataString(){
        try {
            return new String(data);
        } catch (Exception x){}
        return "";
    }

    public byte[] getDataBinary(){
        return data;
    }

    private void discardMessage(){
        this.type = MessageType.NOPE;
    }

    private byte[] flagsToBytes(String flags){
        byte[] out = new byte[FLAGS_LEN];
        for(int i=0; i < Math.min(flags.length(), FLAGS_LEN); i++){
            out[i] = (byte)flags.getBytes(StandardCharsets.US_ASCII)[i];
        }
        return out;
    }
}
