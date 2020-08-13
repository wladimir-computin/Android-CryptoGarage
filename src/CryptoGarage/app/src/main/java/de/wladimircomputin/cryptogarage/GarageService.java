package de.wladimircomputin.cryptogarage;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Vibrator;
import android.util.Log;
import android.widget.Toast;

import de.wladimircomputin.cryptogarage.util.Awake;
import de.wladimircomputin.cryptogarage.util.GaragePing;
import de.wladimircomputin.cryptogarage.util.GaragePingReceiver;
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
    private String wifimode;
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
    public void init(SharedPreferences sharedPref, GarageServiceCallbacks callbacks) {
        this.ip = sharedPref.getString(getString(R.string.preference_ip_key), getString(R.string.preference_ip_default));
        this.devPass = sharedPref.getString(getString(R.string.preference_devpass_key), getString(R.string.preference_devpass_default));
        this.ssid = sharedPref.getString(getString(R.string.preference_wlanssid_key), getString(R.string.preference_wlanssid_default));
        this.pass = sharedPref.getString(getString(R.string.preference_wlanpass_key), getString(R.string.preference_wlanpass_default));
        this.autotrigger_timeout = Integer.valueOf(sharedPref.getString(getString(R.string.preference_autotrigger_timeout_key), getString(R.string.preference_autotrigger_timeout_default))) * 1000;
        this.wifimode = sharedPref.getString(getString(R.string.preference_wifimode_key), getString(R.string.preference_wifimode_default));
        this.callbacks = callbacks;

        wifi = WiFi.instance(this.getApplicationContext());
        cc = new CryptCon(devPass, ip, this, true);
        vib = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        garagePing = new GaragePing(2, cc, this.getApplicationContext());
    }

    public void setCallbacks(GarageServiceCallbacks callbacks) {
        this.callbacks = callbacks;
    }

    public void init_wifi(BroadcastReceiver receiver, boolean aggressiveConnect){
        if(!wifi.isWifiEnabled()) {
            wifi.setWifiEnabled(true);
        }
        try {
            this.registerReceiver(receiver, new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION));
        } catch (Exception x){}
        if(!wifi.isConnectedTo(ssid)) {
            if(wifimode.equals("Hybrid")){
                ssid = getString(R.string.ap_hybrid_default);
            }
            wifi.connectToSSID(ssid, pass);
            if(aggressiveConnect){
                wifi.aggressiveConnect(ssid);
            }
            callbacks.wifiDisconnected();
        } else {
            callbacks.wifiAlreadyConnected();
        }
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(wifi_init_receiver);
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
                NetworkInfo info = arg1.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                if(info != null && info.isConnected()){
                    autotriggerStopSearch();
                    callbacks.autotriggerStart();
                    failsafe_trigger(new CryptConReceiver() {
                        @Override
                        public void onSuccess(Content response) {}

                        @Override
                        public void onFail() {}

                        @Override
                        public void onFinished() {
                            callbacks.setProgress("autotrigger", -1);
                        }

                        @Override
                        public void onProgress(String sprogress, int iprogress) {
                            callbacks.logMessage(sprogress);
                            callbacks.setProgress("autotrigger", iprogress);
                        }
                    }, 250);
                }
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
                        callbacks.logMessage("Failed " + count + " pings" + "\n");
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
                        if (Looper.myLooper()==null)
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

    public void trigger(final CryptConReceiver c){
        callbacks.triggerStart();
        cc.sendMessageEncrypted(getString(R.string.command_trigger), new CryptConReceiver() {
            @Override
            public void onSuccess(Content response) {
                autotriggerStopSearch();
                autotrigger_active = false;
                vib.vibrate(new long[]{0, 200}, -1);
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
            public void onSuccess(Content response) {
                autotrigger_active = !autotrigger_active;
                if(autotrigger_active) {
                    garagePing.enableAutotriggerPing(new GaragePingReceiver() {
                        @Override
                        public void onPingSuccess() {
                        }

                        @Override
                        public void onPingFail(int count) {
                            callbacks.logMessage("Failed " + count + " pings" + "\n");
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

    public void getGateState(final CryptConReceiver c){
        cc.sendMessageEncrypted(getString(R.string.command_gatestate), CryptCon.Mode.UDP, 1, c);
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

    public void getStatus(final CryptConReceiver c){
        cc.sendMessageEncrypted(getString(R.string.command_status), new CryptConReceiver() {
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
