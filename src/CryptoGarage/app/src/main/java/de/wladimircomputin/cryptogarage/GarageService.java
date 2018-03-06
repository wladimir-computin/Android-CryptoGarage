package de.wladimircomputin.cryptogarage;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Vibrator;

import de.wladimircomputin.cryptogarage.util.Awake;
import de.wladimircomputin.cryptogarage.util.GaragePing;
import de.wladimircomputin.cryptogarage.util.GaragePingReceiver;
import de.wladimircomputin.cryptogarage.util.WiFi;
import de.wladimircomputin.libcryptogarage.protocol.CryptCon;
import de.wladimircomputin.libcryptogarage.protocol.CryptConReceiver;

public class GarageService extends Service {
    private final IBinder binder = new LocalBinder();
    private CryptCon cc;
    private WiFi wifi;
    private String devPass;
    private String ssid;
    private String pass;
    private int autotrigger_timeout;

    private GarageServiceCallbacks callbacks;

    private boolean autotrigger_active = false;
    private Handler autotrigger_handler = new Handler();
    private GaragePing garagePing;
    private Awake awake;

    Vibrator vib;


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
    public void init(String devPass, String ssid, String pass, int autotrigger_timeout, GarageServiceCallbacks callbacks) {
        this.devPass = devPass;
        this.ssid = ssid;
        this.pass = pass;
        this.autotrigger_timeout = autotrigger_timeout * 1000;
        this.callbacks = callbacks;
        wifi = new WiFi(this.getApplicationContext());
        cc = new CryptCon(devPass, this);
        vib = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        garagePing = new GaragePing(2, devPass, this.getApplicationContext());
    }

    public void setCallbacks(GarageServiceCallbacks callbacks) {
        this.callbacks = callbacks;
    }

    public void init_wifi(BroadcastReceiver receiver, boolean aggressiveConnect){
        wifi.setWifiEnabled(true);
        try {
            this.registerReceiver(receiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
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
                //wifi.bindOnNetwork();
                callbacks.wifiConnected();
            } else {
                callbacks.wifiDisconnected();
            }
        }
    };

    public BroadcastReceiver autotrigger_receiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context arg0, Intent arg1) {
            if(wifi.isConnectedTo(ssid) && !autotrigger_active) {
                callbacks.autotriggerStart();
                trigger(new CryptConReceiver() {
                    @Override
                    public void onSuccess(String response) {
                        vib.vibrate(new long[] {0, 200}, -1);
                    }
                    @Override
                    public void onFail() {
                        //wait and again
                    }
                    @Override
                    public void onFinished() {
                        callbacks.setProgress("autotrigger", -1);
                    }
                    @Override
                    public void onProgress(String sprogress, int iprogress) {
                        callbacks.logMessage(sprogress);
                        callbacks.setProgress("autotrigger", iprogress);
                    }
                });
            } else if (!wifi.isConnectedTo(ssid) && autotrigger_active) {
                garagePing.disableAutotriggerPing(true);
            } else if (wifi.isConnectedTo(ssid) && autotrigger_active) {
                vib.vibrate(new long[] {0, 200}, -1);
                callbacks.autotriggerCountdown(true);
                autotrigger_handler.removeCallbacks(autotrigger_finish);
                garagePing.enableAutotriggerPing(new GaragePingReceiver() {
                    @Override
                    public void onPingSuccess() {}

                    @Override
                    public void onPingFail(int count) {
                        callbacks.logMessage("Failed " + count + " pings");
                    }

                    @Override
                    public void onPingLost() {
                        prepareAutotriggerEnd();
                    }
                });
            } else if (!wifi.isConnectedTo(ssid) && !autotrigger_active){
            }
        }
    };

    public void autotriggerStopSearch(){
        wifi.stopAgressiveConnect();
        garagePing.disableAutotriggerPing(false);
        try{
            GarageService.this.unregisterReceiver(autotrigger_receiver);
        } catch (Exception x){}
    }

    private void prepareAutotriggerEnd() {
        if (autotrigger_active) {
            vib.vibrate(new long[]{0, 200, 150, 200, 150, 200}, -1);
            int temp = autotrigger_timeout > 500 ? autotrigger_timeout - 500 : autotrigger_timeout;
            autotrigger_handler.postDelayed(autotrigger_finish, temp);
            wifi.setWifiEnabled(false);
            callbacks.autotriggerCountdown(false);
        }
    }

    public Runnable autotrigger_finish = new Runnable() {
        @Override
        public void run() {
            autotrigger_active = false;
            garagePing.disableAutotriggerPing(false);
            vib.vibrate(new long[] {0, 300, 200, 300}, -1);
            try{
                GarageService.this.unregisterReceiver(autotrigger_receiver);
            } catch (Exception x){}
            callbacks.autotriggerCycleEnd();
        }
    };

    public boolean isAutotrigger_active(){
        return autotrigger_active;
    }
    public boolean isAutotrigger_searching(){
        return wifi.isAggressiveConnectRunning();
    }

    public void trigger(final CryptConReceiver c){
        callbacks.triggerStart();
        cc.sendMessageEncrypted(getString(R.string.command_trigger), new CryptConReceiver() {
            @Override
            public void onSuccess(String response) {
                autotriggerStopSearch();
                autotrigger_active = false;
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

    public void autotrigger(final CryptConReceiver c){
        callbacks.autotriggerStart();
        garagePing.disableAutotriggerPing(false);
        cc.sendMessageEncrypted(getString(R.string.command_autotrigger), new CryptConReceiver() {
            @Override
            public void onSuccess(String response) {
                autotrigger_active = !autotrigger_active;
                if(autotrigger_active) {
                    garagePing.enableAutotriggerPing(new GaragePingReceiver() {
                        @Override
                        public void onPingSuccess() {
                        }

                        @Override
                        public void onPingFail(int count) {
                            callbacks.logMessage("Failed " + count + " pings");
                        }

                        @Override
                        public void onPingLost() {
                            prepareAutotriggerEnd();
                        }
                    });
                    init_wifi(autotrigger_receiver, false);
                } else {
                    autotriggerStopSearch();
                }
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

    public void reboot(final CryptConReceiver c){
        cc.sendMessageEncrypted(getString(R.string.command_reboot), new CryptConReceiver() {
            @Override
            public void onSuccess(String response) {
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
