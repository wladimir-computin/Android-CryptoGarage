package de.wladimircomputin.libcryptoiot.v2.protocol;

import static de.wladimircomputin.libcryptoiot.v2.Constants.ciot_v2_message_header;
import static de.wladimircomputin.libcryptoiot.v2.Constants.command_discover;

import android.os.Looper;
import android.util.Base64;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import de.wladimircomputin.libcryptoiot.v2.Constants;
import de.wladimircomputin.libcryptoiot.v2.net.ConBroadcastReceiver;
import de.wladimircomputin.libcryptoiot.v2.net.ConReceiver;
import de.wladimircomputin.libcryptoiot.v2.net.NetCon_async;
import de.wladimircomputin.libcryptoiot.v2.net.TCPCon_async;
import de.wladimircomputin.libcryptoiot.v2.net.UDPCon_async;

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
            public void onSuccess(de.wladimircomputin.libcryptoiot.v2.protocol.Content response) {
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
            public void onSuccess(de.wladimircomputin.libcryptoiot.v2.protocol.Content response) {}
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

    public void sendMessageEncrypted(String command, Mode mode, int retries, CryptConReceiver receiver){
        CryptConReceiver receiver2 = new CryptConReceiver() {
            @Override
            public void onSuccess(de.wladimircomputin.libcryptoiot.v2.protocol.Content response) {
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
            if(!challengeManager.hasNextChallengeResponse()) {
                phase1(command, mode, retries, receiver2);
            } else {
                Message fake_respone = new Message(MessageType.HELLO, challengeManager.getCurrentChallengeRequest(), challengeManager.getNextChallengeResponse(), "");
                phase2(fake_respone, command, mode, retries, new CryptConReceiver() {
                    @Override
                    public void onSuccess(de.wladimircomputin.libcryptoiot.v2.protocol.Content response) {
                        receiver2.onSuccess(response);
                    }

                    @Override
                    public void onFail() {
                        Looper.prepare();
                        phase1(command, mode, retries, receiver2);
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
                challengeManager.resetChallengeResponse();
            }
        } else {
            receiver2.onFail();
            receiver2.onFinished();
        }
    }

    public static void discoverDevices(CryptConDiscoverReceiver receiver){
        UDPCon_async udp= new UDPCon_async("255.255.255.255", UDP_PORT);
        udp.sendMessageBroadcast(ciot_v2_message_header + command_discover, new ConBroadcastReceiver() {

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

        Message hello = new Message(MessageType.HELLO, null, challengeManager.generateRandomChallengeRequest(), "");
        hello.encrypt(crypt);

        if (hello != null) {

            netCon_async.sendMessage(hello.toEncryptedString(), retries, new ConReceiver() {
                @Override
                public void onResponseReceived(String responseData) {
                    receiver.onProgress("Got encrypted challenge!\n", 50);
                    Message response = new Message();
                    response.parseEncrypted(responseData);
                    if(response.decrypt_verify(crypt, challengeManager).type == MessageType.HELLO){
                        phase2(response, command, mode, retries, receiver);
                    }
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
            challengeManager.setNextChallengeResponse(response.challenge_request);
            Message data = new Message(MessageType.DATA, challengeManager.getNextChallengeResponse(), challengeManager.generateRandomChallengeRequest(), command);
            data.encrypt(crypt);

            receiver.onProgress("Sending encrypted command...\n", 75);

            netCon_async.sendMessage(data.toEncryptedString(), retries, new ConReceiver() {
                @Override
                public void onResponseReceived(String responseData) {
                    //decrypt
                    Message response = new Message();
                    response.parseEncrypted(responseData);
                    response.decrypt_verify(crypt, challengeManager);
                    Content out = new Content(response.type);

                    if (response.type == MessageType.DATA || response.type == MessageType.ACK) {
                        if(!response.flags.contains("B")){
                            out.data = response.getDataString();
                        } else {
                            out.data = Base64.encodeToString(response.getDataBinary(), Base64.NO_WRAP);
                        }

                        if(!response.flags.contains("F")){
                            netCon_async.close();
                        }
                        challengeManager.setNextChallengeResponse(response.challenge_request);
                        receiver.onProgress("Success :)\n\n", 100);
                        receiver.onSuccess(out);
                        receiver.onFinished();

                    } else if (response.type == MessageType.HELLO) {
                        phase2(response, command, mode, retries, receiver);

                    } else if (response.type == MessageType.ERR) {
                        netCon_async.close();
                        if (!response.getDataString().isEmpty()) {
                            receiver.onProgress("Error: " + response.getDataString() + "\n\n", 75);
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
