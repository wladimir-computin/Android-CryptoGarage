package de.wladimircomputin.libcryptogarage.protocol;

import android.content.Context;
import android.os.Looper;
import android.util.Base64;

import de.wladimircomputin.libcryptogarage.R;
import de.wladimircomputin.libcryptogarage.net.ConReceiver;
import de.wladimircomputin.libcryptogarage.net.NetCon_async;
import de.wladimircomputin.libcryptogarage.net.TCPCon_async;
import de.wladimircomputin.libcryptogarage.net.UDPCon_async;

/**
 * Created by spamd on 11.03.2017.
 */

public class CryptCon {
    private final Crypter crypt;
    private final Context context;
    private final TCPCon_async tcpCon_async;
    private final UDPCon_async udpCon_async;
    private final ChallengeManager challengeManager;

    private boolean SEMAPHORE = true;

    public enum Mode {UDP, TCP}

    public CryptCon(String pass, String ip, int port_tcp, int port_udp, Context context){
        this.crypt = new Crypter(pass, context);
        this.context = context;
        this.challengeManager = new ChallengeManager(context);
        tcpCon_async = new TCPCon_async(ip, port_tcp, context);
        udpCon_async = new UDPCon_async(ip, port_udp, context);
    }

    public CryptCon(String pass, String ip, Context context){
        this(pass, ip, 4646, 4647, context);
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
        sendMessageEncrypted(message, Mode.UDP, receiver);
    }

    public void sendMessageEncrypted(String message, Mode mode, CryptConReceiver receiver){
        sendMessageEncrypted(message, mode, 4, receiver);
    }

    public void sendMessageEncrypted(String message, Mode mode, int retries, CryptConReceiver receiver){
        CryptConReceiver receiver2 = new CryptConReceiver() {
            @Override
            public void onSuccess(Content response) {
                SEMAPHORE = true;
                receiver.onSuccess(response);
            }

            @Override
            public void onFail() {
                SEMAPHORE = true;
                receiver.onFail();
            }

            @Override
            public void onFinished() {
                receiver.onFinished();
            }

            @Override
            public void onProgress(String sprogress, int iprogress) {
                receiver.onProgress(sprogress, iprogress);
            }
        };
        if(SEMAPHORE) {
            SEMAPHORE = false;
            receiver2.onProgress("Connecting...\n", 25);
            String last_challenge_request = challengeManager.getCurrentChallenge();
            if(last_challenge_request.isEmpty()) {
                phase1(message, mode, retries, receiver2);
            } else {
                Message m = new Message();
                m.challenge_request_b64 = last_challenge_request;
                m.type = MessageType.HELLO;
                phase2(m, message, mode, retries, new CryptConReceiver() {
                    @Override
                    public void onSuccess(Content response) {
                        receiver2.onSuccess(response);
                    }

                    @Override
                    public void onFail() {
                        Looper.prepare();
                        phase1(message, mode, retries, receiver2);
                    }

                    @Override
                    public void onFinished() {
                        receiver2.onFinished();
                    }

                    @Override
                    public void onProgress(String sprogress, int iprogress) {
                        receiver2.onProgress(sprogress, iprogress);
                    }
                });
                challengeManager.resetChallenge();
            }
        } else {
            receiver2.onFail();
            receiver2.onFinished();
        }
    }

    public void discoverDevices(CryptConReceiver receiver){
        udpCon_async.sendMessage(MessageType.HELLO + ":::" + context.getString(R.string.command_discover), true, 1,  new ConReceiver() {

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

    private void phase1(final String command, Mode mode, int retries, final CryptConReceiver receiver) {
        NetCon_async netCon_async = getNetCon(mode);

        Message hello = encryptToMessage(MessageType.HELLO, "", "");

        if (hello != null) {

            netCon_async.sendMessage(hello.toEncryptedString(), retries, new ConReceiver() {
                @Override
                public void onResponseReceived(String responseData) {
                    receiver.onProgress("Got encrypted challenge!\n", 50);
                    Message response = decryptRawMessage(responseData, hello.challenge_request_b64);
                    phase2(response, command, mode, retries, receiver);
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

    private void phase2(Message response, String command, Mode mode, int retries, final CryptConReceiver receiver) {
        NetCon_async netCon_async = getNetCon(mode);

        //do some verification here
        if (response.type != MessageType.NOPE) {

            Message data = encryptToMessage(MessageType.DATA, command, response.challenge_request_b64);
            if(data != null) {
                receiver.onProgress("Sending encrypted command...\n", 75);

                netCon_async.sendMessage(data.toEncryptedString(), retries, new ConReceiver() {
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
            } else {
                netCon_async.close();
                receiver.onProgress("Error: Decryption failed\n", 50);
                receiver.onFail();
                receiver.onFinished();
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
