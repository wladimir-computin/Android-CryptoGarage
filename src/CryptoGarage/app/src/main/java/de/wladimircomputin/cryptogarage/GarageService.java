package de.wladimircomputin.cryptogarage;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Vibrator;

import de.wladimircomputin.cryptogarage.util.WiFi;
import de.wladimircomputin.libcryptoiot.v2.Constants;
import de.wladimircomputin.libcryptoiot.v2.protocol.Content;
import de.wladimircomputin.libcryptoiot.v2.protocol.CryptCon;
import de.wladimircomputin.libcryptoiot.v2.protocol.CryptConReceiver;

public class GarageService extends Service {
    private final IBinder binder = new LocalBinder();
    private CryptCon cc;
    private WiFi wifi;

    private String ip;
    private String devPass;
    private String ssid;
    private String pass;
    private String wifimode;
    private String remote_url;

    private GarageServiceCallbacks callbacks;

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

    /**
     * method for clients
     */
    public void init(SharedPreferences sharedPref, GarageServiceCallbacks callbacks) {
        this.ip = sharedPref.getString(getString(R.string.preference_ip_key), getString(R.string.preference_ip_default));
        this.devPass = sharedPref.getString(getString(R.string.preference_devpass_key), getString(R.string.preference_devpass_default));
        this.ssid = sharedPref.getString(getString(R.string.preference_wlanssid_key), getString(R.string.preference_wlanssid_default));
        this.pass = sharedPref.getString(getString(R.string.preference_wlanpass_key), getString(R.string.preference_wlanpass_default));
        this.wifimode = sharedPref.getString(getString(R.string.preference_wifimode_key), getString(R.string.preference_wifimode_default));
        this.remote_url = sharedPref.getString(getString(R.string.preference_remote_key), getString(R.string.preference_remote_default));
        this.callbacks = callbacks;

        if (!wifimode.equals("Remote")) {
            wifi = WiFi.instance(this.getApplicationContext());
            cc = new CryptCon(devPass, ip);
        } else {
            if (remote_url.contains(":")) {
                String server = remote_url.split(":")[0];
                int port = Integer.parseInt(remote_url.split(":")[1]);
                cc = new CryptCon(devPass, server + ":" + port);
            }
        }
        vib = (Vibrator) getSystemService(VIBRATOR_SERVICE);
    }

    public void setCallbacks(GarageServiceCallbacks callbacks) {
        this.callbacks = callbacks;
    }

    public void init_wifi(BroadcastReceiver receiver, boolean aggressiveConnect) {
        if (!wifimode.equals("Remote")) {
            if (!wifi.isWifiEnabled()) {
                wifi.setWifiEnabled(true);
            }

            try {
                this.registerReceiver(receiver, new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION));
            } catch (Exception x) {
            }
            if (!wifi.isConnectedTo(ssid)) {
                if (wifimode.equals("Hybrid")) {
                    ssid = getString(R.string.ap_hybrid_default);
                }
                wifi.connectToSSID(ssid, pass);
                if (aggressiveConnect) {
                    wifi.aggressiveConnect(ssid);
                }
                callbacks.wifiDisconnected();
            } else {
                callbacks.wifiAlreadyConnected();
            }
        }
    }

    @Override
    public void onDestroy() {
        try {
            unregisterReceiver(wifi_init_receiver);
        } catch (Exception x) {
        }
    }

    public BroadcastReceiver wifi_init_receiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context arg0, Intent arg1) {
            if (wifi.isConnectedTo(ssid)) {
                wifi.bindOnNetwork();
                callbacks.wifiConnected();
            } else {
                callbacks.wifiDisconnected();
            }
        }
    };



    private int failcount = 0;
    private final int failcount_limit = 3;
    private Handler later = new Handler();

    public void failsafe_trigger(CryptConReceiver c, long delay) {
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
                    if (failcount < failcount_limit) {
                        callbacks.logMessage("Trying again...");
                        if (Looper.myLooper() == null)
                            Looper.prepare();
                        failcount++;
                        failsafe_trigger(this, 250);
                    } else {
                        later.postDelayed(() -> failcount = 0, delay * 2);
                        c.onFail();
                        c.onFinished();
                    }
                }

                @Override
                public void onFinished() {
                }

                @Override
                public void onProgress(String sprogress, int iprogress) {
                    c.onProgress(sprogress, iprogress);
                }
            });
        }, delay);
    }

    public void trigger(final CryptConReceiver c) {
        callbacks.triggerStart();
        cc.sendMessageEncrypted("Garage:" + "trigger", CryptCon.Mode.UDP, new CryptConReceiver() {
            @Override
            public void onSuccess(Content response) {
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

    public void getGateState(final CryptConReceiver c) {
        cc.sendMessageEncrypted("Garage:" + "gatestate", CryptCon.Mode.UDP, 1, c);
    }

    public void reboot(final CryptConReceiver c) {
        cc.sendMessageEncrypted(Constants.command_reboot, CryptCon.Mode.UDP, new CryptConReceiver() {
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

    public void getStatus(final CryptConReceiver c) {
        cc.sendMessageEncrypted(Constants.command_status, CryptCon.Mode.UDP, new CryptConReceiver() {
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
