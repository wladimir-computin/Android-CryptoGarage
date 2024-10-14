package de.wladimircomputin.libcryptoiot.v1.protocol;

import static de.wladimircomputin.libcryptoiot.v1.Constants.command_discover;

import android.os.Looper;
import android.util.Base64;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import de.wladimircomputin.libcryptoiot.v1.net.ConBroadcastReceiver;
import de.wladimircomputin.libcryptoiot.v1.net.ConReceiver;
import de.wladimircomputin.libcryptoiot.v1.net.NetCon_async;
import de.wladimircomputin.libcryptoiot.v1.net.TCPCon_async;
import de.wladimircomputin.libcryptoiot.v1.net.UDPCon_async;

/**
 * Created by spamd on 11.03.2017.
 */

public class CryptCon {
    private final Crypter crypt;
    private final TCPCon_async tcpCon_async;
    private final UDPCon_async udpCon_async;
    private final ChallengeManager challengeManager;

    final static int TCP_PORT = 4646;
    final static int UDP_PORT = 4647;

    private boolean SEMAPHORE = true;

    public enum Mode {UDP, TCP}

    public CryptCon(String pass, String host){
        this(pass, host, "", "");
    }

    public CryptCon(String pass, String host, String key, String key_probe){
        this.crypt = new Crypter(pass, key, key_probe);
        this.challengeManager = new ChallengeManager();
        int port = 0;
        try {
            if (host.contains(":")) {
                int index = host.lastIndexOf(":");
                port = Integer.parseInt(host.substring(index + 1));
                host = host.substring(0, index);
            }
        } catch (Exception x){}
        if(port != 0) {
            tcpCon_async = new TCPCon_async(host, port);
            udpCon_async = new UDPCon_async(host, port);
        } else {
            tcpCon_async = new TCPCon_async(host, TCP_PORT);
            udpCon_async = new UDPCon_async(host, UDP_PORT);
        }
    }

    public void sendMessageEncryptedBulk(String[] message){
        sendMessageEncryptedBulk(message, new CryptConBulkReceiver() {
            @Override
            public void onSuccess(Content response, int i) {}
            @Override
            public void onFail(int i) {}
            @Override
            public void onFinished() {}
            @Override
            public void onProgress(String sprogress, int iprogress) {}
        });
    }

    public void sendMessageEncryptedBulk(String[] message, CryptConBulkReceiver receiver){
        sendMessageEncryptedBulk(message, Mode.UDP, receiver);
    }

    public void sendMessageEncryptedBulk(String[] message, Mode mode, CryptConBulkReceiver receiver){
        sendMessageEncryptedBulk(message, mode, 4, receiver);
    }

    public void sendMessageEncryptedBulk(String[] message, Mode mode, int retries, CryptConBulkReceiver receiver){
        ArrayList<String> commands = new ArrayList<>(Arrays.asList(message));
        Iterator<String> iter = commands.iterator();
        final int[] i = {0};
        CryptConReceiver r = new CryptConReceiver() {
            @Override
            public void onSuccess(de.wladimircomputin.libcryptoiot.v1.protocol.Content response) {
                if(iter.hasNext()) {
                    receiver.onSuccess(response, i[0]++);
                    sendMessageEncrypted(iter.next(), mode, retries, this);
                } else {
                    receiver.onSuccess(response, i[0]++);
                    receiver.onFinished();
                }
            }

            @Override
            public void onFail() {
                receiver.onFail(i[0]++);
            }

            @Override
            public void onFinished() {

            }

            @Override
            public void onProgress(String sprogress, int iprogress) {
            }
        };
        sendMessageEncrypted(iter.next(), mode, retries, r);
    }

    public void sendMessageEncrypted(String message){
        sendMessageEncrypted(message, new CryptConReceiver() {
            @Override
            public void onSuccess(de.wladimircomputin.libcryptoiot.v1.protocol.Content response) {}
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
            public void onSuccess(de.wladimircomputin.libcryptoiot.v1.protocol.Content response) {
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
                    public void onSuccess(de.wladimircomputin.libcryptoiot.v1.protocol.Content response) {
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

    public static void discoverDevices(CryptConDiscoverReceiver receiver){
        UDPCon_async udp= new UDPCon_async("255.255.255.255", UDP_PORT);
        udp.sendMessageBroadcast(MessageType.HELLO + ":::" + command_discover, new ConBroadcastReceiver() {

            @Override
            public void onResponseReceived(List<String> responseData) {
                List<DiscoveryDevice> discoveryDevices = new ArrayList<>();
                for(String s : responseData){
                    DiscoveryDevice discoveryDevice = new DiscoveryDevice(s);
                    if(!discoveryDevice.isEmpty()){
                        discoveryDevices.add(discoveryDevice);
                    }
                }

                discoveryDevices.sort((lhs, rhs) -> {
                    return lhs.ip.compareTo(rhs.ip);
                });

                udp.close();
                receiver.onSuccess(discoveryDevices);
                receiver.onFinished();
            }

            @Override
            public void onError(String reason) {
                udp.close();
                receiver.onFail();
                receiver.onFinished();
            }
        });
    }

    public Crypter getCrypter(){
        return crypt;
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
                        de.wladimircomputin.libcryptoiot.v1.protocol.Content out = new de.wladimircomputin.libcryptoiot.v1.protocol.Content(response.type);

                        if (response.type == MessageType.DATA || response.type == MessageType.ACK) {
                            if(!response.flags.contains("B")){
                                out.data = response.getDataString();
                            } else {
                                out.data = response.data_b64;
                            }

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

    private NetCon_async getNetCon(Mode mode) {
        switch (mode) {
            case TCP:
                return tcpCon_async;
            case UDP:
                return udpCon_async;
        }
        return tcpCon_async;
    }
}
