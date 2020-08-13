package de.wladimircomputin.libcryptogarage.net;

import android.content.Context;

import de.wladimircomputin.libcryptogarage.R;

/**
 * Created by spamd on 07.01.2018.
 */

public class UDPCon_async implements NetCon_async{
    public UDPCon UDPCon_sync;

    public UDPCon_async(String url, int port, Context context){
        UDPCon_sync = UDPCon.instance(url, port, context);
    }

    public UDPCon_async(String url, int port, Context context, boolean newinstance){
        if(!newinstance) {
            new UDPCon_async(url, port, context);
        } else {
            UDPCon_sync = new UDPCon(url, port, context);
        }
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

    @Override
    public void close() {

    }
}
