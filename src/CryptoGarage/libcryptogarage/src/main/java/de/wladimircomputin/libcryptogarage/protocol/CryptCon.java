package de.wladimircomputin.libcryptogarage.protocol;

import android.content.Context;
import android.util.Base64;

import java.nio.charset.StandardCharsets;

import de.wladimircomputin.libcryptogarage.R;
import de.wladimircomputin.libcryptogarage.net.TCPConReceiver;
import de.wladimircomputin.libcryptogarage.net.TCPCon_async;

/**
 * Created by spamd on 11.03.2017.
 */

public class CryptCon {
    private Crypter crypt;
    private Context context;
    private TCPCon_async tcpCon_async;

    public CryptCon(String pass, Context context){
        this.crypt = new Crypter(pass);
        this.context = context;
        tcpCon_async = new TCPCon_async(context.getString(R.string.static_ip), 4646, context);
    }

    public void sendMessageEncrypted(String message){
        sendMessageEncrypted(message, new CryptConReceiver() {
            @Override
            public void onSuccess(String response) {}
            @Override
            public void onFail() {}
            @Override
            public void onFinished() {}
            @Override
            public void onProgress(String sprogress, int iprogress) {}
        });
    }

    public void sendMessageEncrypted(String message, CryptConReceiver receiver){
        receiver.onProgress("Connecting...\n", 25);
        phase1(message, receiver);
    }

    private void phase1(final String message, final CryptConReceiver receiver){
        String encryptedMessage_b64 = encryptToMessage(context.getString(R.string.response_data), context.getString(R.string.command_hello), null);

        tcpCon_async.sendMessage(encryptedMessage_b64, new TCPConReceiver() {
            @Override
            public void onResponseReceived(String responseData) {
                if(responseData.startsWith(context.getString(R.string.response_data))) {
                    receiver.onProgress("Got encrypted challenge!\n", 50);
                    phase2(responseData, message, receiver);
                }

            }

            @Override
            public void onError(String reason) {
                if(reason != "") {
                    receiver.onProgress("Error: " + reason + "\n\n", 75);
                } else {
                    receiver.onProgress("Connection failed!\n\n", 75);
                }
                tcpCon_async.close();
                receiver.onFail();
                receiver.onFinished();
            }
        });
    }

    private void phase2(String raw, String message, final CryptConReceiver receiver){
        //do some verification here
        byte[] challengeIV = decryptRawMessage(raw);
        if (challengeIV == null){
            receiver.onFail();
            receiver.onFinished();
            return;
        }

        String encryptedMessage_b64 = encryptToMessage(context.getString(R.string.response_data), message, challengeIV);

        receiver.onProgress("Sending encrypted command...\n", 75);

        tcpCon_async.sendMessage(encryptedMessage_b64, new TCPConReceiver() {
            @Override
            public void onResponseReceived(String responseData) {
                //decrypt
                String data = "";
                if(responseData.startsWith(context.getString(R.string.response_data))){
                    byte[] temp = decryptRawMessage(responseData);
                    if (temp != null) {
                        data = new String(temp);
                    } else {
                        receiver.onProgress("Decryption error!\n\n", 75);
                        receiver.onFail();
                        receiver.onFinished();
                        return;
                    }
                }
                tcpCon_async.close();
                receiver.onProgress("Success :)\n\n", 100);
                receiver.onSuccess(data);
                receiver.onFinished();
            }

            @Override
            public void onError(String reason) {
                if(reason != "") {
                    receiver.onProgress("Error: " + reason + "\n\n", 75);
                } else {
                    receiver.onProgress("Command not accepted!\n\n", 75);
                }
                tcpCon_async.close();
                receiver.onFail();
                receiver.onFinished();
            }
        });
     }

    private String encryptToMessage(String type, String data, byte[] challengeIV) {
        byte[] iv;
        if (challengeIV != null){
            iv = challengeIV;
        } else {
            iv = crypt.getRandomIV();
        }

        byte[] encryptedMessageWithTag = crypt.encrypt(data.getBytes(StandardCharsets.UTF_8), iv);
        byte[] encryptedMessage = new byte[encryptedMessageWithTag.length - 16];
        byte[] tag = new byte[16];
        System.arraycopy(encryptedMessageWithTag, 0, encryptedMessage, 0, encryptedMessage.length);
        System.arraycopy(encryptedMessageWithTag, encryptedMessage.length, tag, 0, tag.length);
        String tag_b64 = Base64.encodeToString(tag, Base64.NO_WRAP);
        String encryptedMessage_b64 = Base64.encodeToString(encryptedMessage, Base64.NO_WRAP);
        String iv_b64;
        if(challengeIV == null){
            iv_b64 = Base64.encodeToString(iv, Base64.NO_WRAP);
        } else {
            iv_b64 = "";
        }

        String out = context.getString(R.string.response_data) + ":" + iv_b64 + ":" + tag_b64 + ":" + encryptedMessage_b64;
        return out;
    }

    private byte[] decryptRawMessage(String raw){
        String[] parts = raw.split(":");
        if(parts.length != 4 || parts[3].isEmpty())
            return null;
        String iv_b64 = parts[1];
        String tag_b64 = parts[2];
        String encryptedMessage_b64 = parts[3];
        byte[] iv = Base64.decode(iv_b64, Base64.NO_WRAP);
        byte[] encryptedMessage = Base64.decode(encryptedMessage_b64, Base64.NO_WRAP);
        byte[] tag = Base64.decode(tag_b64, Base64.NO_WRAP);
        return crypt.decrypt(encryptedMessage, iv, tag);
    }
}
