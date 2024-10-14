package de.wladimircomputin.libcryptoiot.v1.net;

import java.util.List;

/**
 * Created by spamd on 07.01.2018.
 */

public class UDPCon_async implements de.wladimircomputin.libcryptoiot.v1.net.NetCon_async {
    public de.wladimircomputin.libcryptoiot.v1.net.UDPCon UDPCon_sync;

    public UDPCon_async(String url, int port){
        UDPCon_sync = new de.wladimircomputin.libcryptoiot.v1.net.UDPCon(url, port);
    }

    @Override
    public void sendMessage(final String message, int retries , final de.wladimircomputin.libcryptoiot.v1.net.ConReceiver callback){
        sendMessage(message, false, retries, callback);
    }

    public void sendMessage(final String message, boolean broadcast, int retries, final de.wladimircomputin.libcryptoiot.v1.net.ConReceiver callback){
        new Thread(() -> {
            final String out = UDPCon_sync.sendMessage(message, broadcast, retries);
            if (!out.contains(de.wladimircomputin.libcryptoiot.v1.net.NetCon.ERROR_HEADER)) {
                callback.onResponseReceived(out);
            } else {
                callback.onError(out.substring(out.indexOf(":") + 1));
            }
        }).start();
    }

    public void sendMessageBroadcast(final String message, final de.wladimircomputin.libcryptoiot.v1.net.ConBroadcastReceiver callback){
        new Thread(() -> {
            final List<String> out = UDPCon_sync.sendMessageBroadcast(message);
            if (!out.contains(de.wladimircomputin.libcryptoiot.v1.net.NetCon.ERROR_HEADER)) {
                callback.onResponseReceived(out);
            } else {
                callback.onError("");
            }
        }).start();
    }

    @Override
    public void close() {

    }
}
