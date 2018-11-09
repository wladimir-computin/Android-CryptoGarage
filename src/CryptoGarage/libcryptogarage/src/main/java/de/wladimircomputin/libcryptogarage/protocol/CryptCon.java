package de.wladimircomputin.libcryptogarage.protocol;

import android.content.Context;
import android.util.Base64;
import android.widget.Toast;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import de.wladimircomputin.libcryptogarage.R;
import de.wladimircomputin.libcryptogarage.net.*;

/**
 * Created by spamd on 11.03.2017.
 */

public class CryptCon {
    private Crypter crypt;
    private Context context;
    private TCPCon_async tcpCon_async;
    private UDPCon_async udpCon_async;
    private ChallengeManager challengeManager;

    public enum Mode {UDP, TCP}


    public CryptCon(String pass, String ip, Context context){
        new CryptCon(pass, ip, context, false);
    }

    public CryptCon(String pass, String ip, Context context, boolean newinstance){
        this.crypt = Crypter.init(pass, context);
        this.context = context;
        challengeManager = ChallengeManager.instance(context);
        tcpCon_async = new TCPCon_async(ip, 4646, context, newinstance);
        udpCon_async = new UDPCon_async(ip, 4647, context, newinstance);
    }

    public void sendMessageEncrypted(String message){
        sendMessageEncrypted(message, new CryptConReceiver() {
            @Override
            public void onSuccess(Content response) {}
            @Override
            public void onFail() {}
            @Override
            public void onFinished() {}
            @Override
            public void onProgress(String sprogress, int iprogress) {}
        });
    }

    public void sendMessageEncrypted(String message, CryptConReceiver receiver){
        sendMessageEncrypted(message, Mode.TCP, receiver);
    }

    public void sendMessageEncrypted(String message, Mode mode, CryptConReceiver receiver){
        receiver.onProgress("Connecting...\n", 25);
        String last_challenge_request = challengeManager.getCurrentChallenge();
        if(last_challenge_request.isEmpty()) {
            phase1(message, mode, receiver);
        } else {
            Message m = new Message();
            m.challenge_request_b64 = last_challenge_request;
            m.type = MessageType.HELLO;
            phase2(m, message, mode, receiver);
            challengeManager.resetChallenge();
        }
    }

    public void discoverDevices(CryptConReceiver receiver){
        udpCon_async.sendMessage(MessageType.HELLO + ":::" + context.getString(R.string.command_discover), true, new ConReceiver() {

            @Override
            public void onResponseReceived(String responseData) {
                if(responseData.startsWith(MessageType.DATA.toString())){
                    Content out = new Content(MessageType.DATA, responseData.replace(MessageType.DATA.toString() + ":::", ""));
                   receiver.onSuccess(out);
                   receiver.onFinished();
                }
            }

            @Override
            public void onError(String reason) {
                receiver.onFail();
                receiver.onFinished();
            }
        });
    }

    private void phase1(final String command, Mode mode, final CryptConReceiver receiver) {
        NetCon_async netCon_async = getNetCon(mode);

        Message hello = encryptToMessage(MessageType.HELLO, "", "");

        if (hello != null) {

            netCon_async.sendMessage(hello.toEncryptedString(), new ConReceiver() {
                @Override
                public void onResponseReceived(String responseData) {
                    receiver.onProgress("Got encrypted challenge!\n", 50);
                    Message response = decryptRawMessage(responseData, hello.challenge_request_b64);
                    phase2(response, command, mode, receiver);
                }

                @Override
                public void onError(String reason) {
                    if (!reason.isEmpty()) {
                        receiver.onProgress("Error: " + reason + "\n\n", 50);
                    } else {
                        receiver.onProgress("Connection failed!\n\n", 25);
                    }
                    netCon_async.close();
                    receiver.onFail();
                    receiver.onFinished();
                }
            });
        } else {
            netCon_async.close();
            receiver.onProgress("Encryption of Hello failed!\n\n", 25);
            receiver.onFail();
            receiver.onFinished();
        }
    }

    private void phase2(Message response, String command, Mode mode, final CryptConReceiver receiver) {
        NetCon_async netCon_async = getNetCon(mode);

        //do some verification here
        if (response.type != MessageType.NOPE) {

            Message data = encryptToMessage(MessageType.DATA, command, response.challenge_request_b64);
            if(data != null) {
                receiver.onProgress("Sending encrypted command...\n", 75);

                netCon_async.sendMessage(data.toEncryptedString(), new ConReceiver() {
                    @Override
                    public void onResponseReceived(String responseData) {
                        //decrypt
                        Message response = decryptRawMessage(responseData, data.challenge_request_b64);
                        Content out = new Content(response.type);

                        if (response.type == MessageType.DATA || response.type == MessageType.ACK) {
                            out.data = response.getDataString();
                            if(!response.flags.contains("F")){
                                netCon_async.close();
                            }
                            challengeManager.setChallenge(response.challenge_request_b64);
                            receiver.onProgress("Success :)\n\n", 100);
                            receiver.onSuccess(out);
                            receiver.onFinished();

                        } else if (response.type == MessageType.ERR) {
                            netCon_async.close();
                            if (!response.data_b64.isEmpty()) {
                                receiver.onProgress("Error: " + response.data_b64 + "\n\n", 75);
                            } else {
                                receiver.onProgress("Command not accepted!\n\n", 75);
                            }
                            receiver.onFail();
                            receiver.onFinished();
                        } else {
                            netCon_async.close();
                            receiver.onProgress("Decryption error!\n\n", 75);
                            receiver.onFail();
                            receiver.onFinished();
                        }

                    }

                    @Override
                    public void onError(String reason) {
                        if (!reason.isEmpty()) {
                            receiver.onProgress("Error: " + reason + "\n\n", 75);
                        } else {
                            receiver.onProgress("Command not accepted!\n\n", 75);
                        }
                        netCon_async.close();
                        receiver.onFail();
                        receiver.onFinished();
                    }
                });
            }
        } else {
            netCon_async.close();
            receiver.onProgress("Error: Got no valid response for HELLO\n", 50);
            receiver.onFail();
            receiver.onFinished();
        }
    }

    private Message encryptToMessage(MessageType type, String data, String challenge_response_b64) {
        if(type == MessageType.HELLO){
            challenge_response_b64 = "";
        }
        try {
            Message m = new Message();
            m.type = type;
            m.data_b64 = Base64.encodeToString(data.getBytes("UTF8"), Base64.NO_WRAP);
            m.challenge_request_b64 = Base64.encodeToString(crypt.getRandomChallenge(), Base64.NO_WRAP);
            m.challenge_response_b64 = challenge_response_b64;
            return m.encrypt(crypt);
        } catch (Exception x){}
        return null;
    }

    private Message decryptRawMessage(String raw, String challenge_request_b64){
        Message m = new Message();
        m.parseEncrypted(raw);
        if(m.type != MessageType.NOPE) { //check parsing
            m.decrypt_verify(crypt, challenge_request_b64);
            if (m.type != MessageType.NOPE) { //check decryption
                return m;
            }
        }
        return m;
    }

    private NetCon_async getNetCon(Mode mode){
        switch (mode){
            case TCP:
                return tcpCon_async;
            case UDP:
                return udpCon_async;
        }
        return tcpCon_async;
    }
}
