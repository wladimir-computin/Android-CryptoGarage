package de.wladimircomputin.libcryptoiot.v1.net;

/**
 * Created by spamd on 07.01.2018.
 */

public class TCPCon_async implements NetCon_async {
    public TCPCon tcpCon_sync;

    public TCPCon_async(String url, int port){
        tcpCon_sync = new TCPCon(url, port);
    }

    @Override
    public void sendMessage(final String message, int retries, final ConReceiver callback){
        new Thread(() -> {
            final String out = tcpCon_sync.sendMessage(message, retries);
            if (!out.contains(NetCon.ERROR_HEADER)) {
                callback.onResponseReceived(out);
            } else {
                callback.onError(out.substring(out.indexOf(":") + 1));
            }
        }).start();
    }

    @Override
    public void close(){
        new Thread(() -> {
            tcpCon_sync.close();
        }).start();
    }
}
