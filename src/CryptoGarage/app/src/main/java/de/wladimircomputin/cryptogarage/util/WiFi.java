package de.wladimircomputin.cryptogarage.util;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;

import androidx.core.app.ActivityCompat;

import static android.content.Context.CONNECTIVITY_SERVICE;
import static android.content.Context.WIFI_SERVICE;

/**
 * Created by spamd on 09.03.2017.
 */

public class WiFi {
    private static WiFi instance;

    private WifiManager wifiManager;
    private ConnectivityManager connMgr;
    private Handler connectHandler;
    private Awake awake;
    private boolean isAggressiveConnect;
    private int netId = -1;
    private Context context;

    private WiFi(Context context) {
        this.wifiManager = (WifiManager) context.getApplicationContext().getSystemService(WIFI_SERVICE);
        this.connMgr = (ConnectivityManager) context.getApplicationContext().getSystemService(CONNECTIVITY_SERVICE);
        ;
        connectHandler = new Handler();
        awake = new Awake(context);
        this.context = context;
        bindOnNetwork();
    }

    public static WiFi instance(Context context) {
        if (instance == null) {
            instance = new WiFi(context);
        }
        return instance;
    }

    public boolean isWifiEnabled() {
        return wifiManager.isWifiEnabled();
    }

    public void setWifiEnabled(boolean set) {
        wifiManager.setWifiEnabled(set);
    }

    public void connectToSSID(String ssid, String pass) {
        final String s = ssid;
        final String p = pass;
        (new Thread() {
            public void run() {
                int netId = getNetId(s);
                if (netId != -1) {
                    connectNetwork(netId);
                } else {
                    connectNetwork(addNetwork(s, p));
                }
            }
        }).start();
    }

    public boolean isConnectedTo(String ssid) {
        if (isWifiEnabled()) {
            WifiInfo inf = wifiManager.getConnectionInfo();
            if (inf.getSupplicantState() == SupplicantState.COMPLETED) {
                if (inf.getSSID().contains(ssid)) {
                    return true;
                } else if (inf.getSSID().equals("<unknown ssid>")) {
                    return true;
                }
            }
        }
        return false;
    }

    private int getNetId(String ssid) {
        try {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                while (wifiManager.getConfiguredNetworks() == null) {
                    try {
                        this.wait(100);
                    } catch (Exception x) {
                        break;
                    }
                }
                for (WifiConfiguration wifiConfiguration : wifiManager.getConfiguredNetworks()) {
                    if (wifiConfiguration.SSID.equals("\"" + ssid + "\"")) {
                        return wifiConfiguration.networkId;
                    }
                }
            }
        } catch (Exception x){}
        return -1;
    }

    private int addNetwork(String ssid, String pass){
        WifiConfiguration wifiConfig = new WifiConfiguration();
        wifiConfig.SSID = String.format("\"%s\"", ssid);
        wifiConfig.preSharedKey = String.format("\"%s\"", pass);

        return wifiManager.addNetwork(wifiConfig);
    }

    private void connectNetwork(int netId){
        wifiManager.disconnect();
        wifiManager.enableNetwork(netId, true);
        wifiManager.reconnect();
    }

    public void aggressiveConnect(String ssid){
        new Thread() {
            public void run() {
                isAggressiveConnect = true;
                netId = getNetId(ssid);
                if (netId != -1) {
                    awake.setAwake(true);
                    connectHandler.postDelayed(aggressiveConnector, 1000);
                }
            }
        }.start();
    }

    public void stopAgressiveConnect(){
        connectHandler.removeCallbacks(aggressiveConnector);
        isAggressiveConnect = false;
        awake.setAwake(false);
    }

    public boolean isAggressiveConnectRunning(){
        return isAggressiveConnect;
    }

    private Runnable aggressiveConnector = new Runnable() {
        @Override
        public void run() {
            if(!(wifiManager.getConnectionInfo().getSupplicantState() == SupplicantState.COMPLETED) && isWifiEnabled()) {
                connectNetwork(netId);
                connectHandler.postDelayed(this, 1000);
            } else {
                stopAgressiveConnect();
            }
        }
    };

    public void bindOnNetwork() {
        ConnectivityManager.NetworkCallback mNetworkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                connMgr.bindProcessToNetwork(network);
            }
        };
        NetworkRequest request = new NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
                .build();

        connMgr.requestNetwork(request, mNetworkCallback);
    }
}
