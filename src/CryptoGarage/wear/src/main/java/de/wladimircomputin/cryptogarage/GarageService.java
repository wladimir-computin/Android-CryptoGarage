package de.wladimircomputin.cryptogarage;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import de.wladimircomputin.cryptogarage.util.WiFi;
import de.wladimircomputin.libcryptogarage.protocol.Content;
import de.wladimircomputin.libcryptogarage.protocol.CryptCon;
import de.wladimircomputin.libcryptogarage.protocol.CryptConReceiver;

public class GarageService extends Service {
    private final IBinder binder = new LocalBinder();
    private CryptCon cc;
    private WiFi wifi;
    private String ip;
    private String devPass;
    private String ssid;
    private String pass;

    private GarageServiceCallbacks callbacks;

    public class LocalBinder extends Binder {
        GarageService getService() {// Return this instance of LocalService so clients can call public methods
            return GarageService.this;
        }
    }


    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    /** method for clients */
    public void init(String ip, String devPass, String ssid, String pass, GarageServiceCallbacks callbacks) {
        this.ip = ip;
        this.devPass = devPass;
        this.ssid = ssid;
        this.pass = pass;
        this.callbacks = callbacks;
        wifi = new WiFi(this.getApplicationContext());
        cc = new CryptCon(ip, devPass, this);
    }

    public void setCallbacks(GarageServiceCallbacks callbacks) {
        this.callbacks = callbacks;
    }

    public void init_wifi(BroadcastReceiver receiver, boolean aggressiveConnect){
        wifi.setWifiEnabled(true);
        try {
            this.registerReceiver(receiver, new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION));
        } catch (Exception x){}
        if(!wifi.isConnectedTo(ssid)) {
            wifi.connectToSSID(ssid, pass);
            if(aggressiveConnect){
                wifi.aggressiveConnect(ssid);
            }
            callbacks.wifiDisconnected();
        } else {
            callbacks.wifiConnected();
        }
    }

    public BroadcastReceiver wifi_init_receiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context arg0, Intent arg1) {
            if (wifi.isConnectedTo(ssid)) {
                callbacks.wifiConnected();
            } else {
                callbacks.wifiDisconnected();
            }
        }
    };

    public void trigger(final CryptConReceiver c){
        callbacks.triggerStart();
        if(wifi.isConnectedTo(ssid)){
            cc.sendMessageEncrypted(getString(R.string.command_trigger), new CryptConReceiver() {
                @Override
                public void onSuccess(Content response) {
                    c.onSuccess(response);
                }

                @Override
                public void onFail() {
                    c.onFail();
                }

                @Override
                public void onFinished() {
                    c.onFinished();
                }

                @Override
                public void onProgress(String sprogress, int iprogress) {
                    c.onProgress(sprogress, iprogress);
                }
            });
        } else {
            wifi.aggressiveConnect(ssid);
            c.onFail();
        }
    }

    private int failcount = 0;
    private final int failcount_limit = 3;
    private Handler later = new Handler();

    public void failsafe_trigger(CryptConReceiver c, long delay){
        later.postDelayed(() -> {
            trigger(new CryptConReceiver() {
                @Override
                public void onSuccess(Content response) {
                    failcount = 0;
                    c.onSuccess(response);
                    c.onFinished();
                }

                @Override
                public void onFail() {
                    if(failcount < failcount_limit){
                        callbacks.logMessage("Trying again...");
                        Looper.prepare();
                        failcount++;
                        failsafe_trigger(this, 250);
                    } else {
                        later.postDelayed(()->failcount = 0, delay * 2);
                        c.onFail();
                        c.onFinished();
                    }
                }

                @Override
                public void onFinished() {}

                @Override
                public void onProgress(String sprogress, int iprogress) {
                    c.onProgress(sprogress, iprogress);
                }
            });
        }, delay);
    }

    public void reboot(final CryptConReceiver c){
        cc.sendMessageEncrypted(getString(R.string.command_reboot), new CryptConReceiver() {
            @Override
            public void onSuccess(Content response) {
                c.onSuccess(response);
            }

            @Override
            public void onFail() {
                c.onFail();
            }

            @Override
            public void onFinished() {
                c.onFinished();
            }

            @Override
            public void onProgress(String sprogress, int iprogress) {
                c.onProgress(sprogress, iprogress);
            }
        });
    }
}
