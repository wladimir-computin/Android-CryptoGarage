package de.wladimircomputin.libcryptogarage.net;

import android.content.Context;
import android.util.Log;

import de.wladimircomputin.libcryptogarage.R;

/**
 * Created by spamd on 07.01.2018.
 */

public class TCPCon_async {
    public TCPCon tcpCon_sync;
    private Context context;

    public TCPCon_async(String url, int port, Context context){
        this.context = context;
        tcpCon_sync = TCPCon.instance(url, port, context);
    }

    public void sendMessage(final String message, final TCPConReceiver callback){
        new Thread(() -> {
            final String out = tcpCon_sync.sendMessage(message);
            if (out.startsWith(context.getString(R.string.response_ok)) || out.startsWith(context.getString(R.string.response_data))) {
                callback.onResponseReceived(out);
            } else {
                callback.onError(out.substring(out.indexOf(":") + 1));
            }
        }).start();
    }

    public void close(){
        new Thread(() -> {
            tcpCon_sync.close();
        }).start();
    }
}
