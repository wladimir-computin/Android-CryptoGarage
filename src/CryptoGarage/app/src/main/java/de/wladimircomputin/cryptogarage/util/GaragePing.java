package de.wladimircomputin.cryptogarage.util;

import android.content.Context;
import android.os.Handler;

import de.wladimircomputin.libcryptoiot.v2.Constants;
import de.wladimircomputin.libcryptoiot.v2.protocol.Content;
import de.wladimircomputin.libcryptoiot.v2.protocol.CryptCon;
import de.wladimircomputin.libcryptoiot.v2.protocol.CryptConReceiver;

/**
 * Created by spamd on 26.03.2017.
 */

public class GaragePing {
    private int pingFails;
    private int maxFails;
    private CryptCon cc;
    private Handler pingHandler;
    private Context context;
    private Awake awake;
    private GaragePingReceiver callback;
    private boolean running = false;

    public GaragePing(int maxFails, CryptCon cc, Context context){
        this.maxFails = maxFails;
        this.context = context;
        this.cc = cc;
        pingHandler = new Handler();
        awake = new Awake(context);
    }

    public void enableAutotriggerPing(GaragePingReceiver callback){
        awake.setAwake(true);
        if(!isRunning()) {
            this.callback = callback;
            pingFails = 0;
            running = true;
            pingHandler.postDelayed(ping, 500);
        }
    }

    public void disableAutotriggerPing(boolean onPingLost_event){
        pingHandler.removeCallbacks(ping);
        if(running && onPingLost_event){
            callback.onPingLost();
        }
        running = false;
        awake.setAwake(false);
    }

    public boolean isRunning(){
        return running;
    }

    private Runnable ping = new Runnable() {
        @Override
        public void run() {
            final Runnable me = this;
            cc.sendMessageEncrypted(Constants.command_ping, CryptCon.Mode.UDP, new CryptConReceiver() {
                @Override
                public void onSuccess(Content response) {
                    pingFails = 0;
                    callback.onPingSuccess();
                }

                @Override
                public void onFail() {
                    pingFails++;
                    if(pingFails > maxFails){
                        disableAutotriggerPing(false);
                        callback.onPingLost();
                    }
                    callback.onPingFail(pingFails);
                }

                @Override
                public void onFinished() {
                    if (running) {
                        pingHandler.postDelayed(me, 2000);
                    }
                }

                @Override
                public void onProgress(String sprogress, int iprogress) {}
            });

        }
    };
}
