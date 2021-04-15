package de.wladimircomputin.libcryptogarage.net;

import android.content.Context;

import java.util.List;

/**
 * Created by spamd on 07.01.2018.
 */

public class UDPCon_async implements NetCon_async {
    public UDPCon UDPCon_sync;

    public UDPCon_async(String url, int port, Context context){
        UDPCon_sync = new UDPCon(url, port, context);
    }

    @Override
    public void sendMessage(final String message, int retries , final ConReceiver callback){
        sendMessage(message, false, retries, callback);
    }

    public void sendMessage(final String message, boolean broadcast, int retries, final ConReceiver callback){
        new Thread(() -> {
            final String out = UDPCon_sync.sendMessage(message, broadcast, retries);
            if (!out.contains(NetCon.ERROR_HEADER)) {
                callback.onResponseReceived(out);
            } else {
                callback.onError(out.substring(out.indexOf(":") + 1));
            }
        }).start();
    }

    public void sendMessageBroadcast(final String message, final ConBroadcastReceiver callback){
        new Thread(() -> {
            final List<String> out = UDPCon_sync.sendMessageBroadcast(message);
            if (!out.contains(NetCon.ERROR_HEADER)) {
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
