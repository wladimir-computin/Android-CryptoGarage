package de.wladimircomputin.cryptogarage;

import android.animation.ObjectAnimator;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Vibrator;
import android.support.wearable.activity.WearableActivity ;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import de.wladimircomputin.cryptogarage.util.Awake;
import de.wladimircomputin.cryptogarage.util.WiFi;
import de.wladimircomputin.libcryptoiot.v2.protocol.Content;
import de.wladimircomputin.libcryptoiot.v2.protocol.CryptConReceiver;

public class Garage extends WearableActivity implements GarageServiceCallbacks{

    GarageService garage;
    boolean garageBound;

    Button triggerButton;
    TextView logTextView;
    ProgressBar triggerProgress;

    Awake awake;
    WiFi wifi;
    SharedPreferences sharedPref;
    Vibrator vib;
    ConnectivityManager connMgr;


    String ip;
    String devPass;
    String ssid;
    String pass;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_garage_wear);


        triggerButton = findViewById(R.id.triggerButton);
        triggerProgress = findViewById(R.id.triggerButton_Progress);

        sharedPref = this.getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        vib = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        awake = new Awake(getApplicationContext());

        //cc = new CryptCon(sharedPref.getString(getString(R.string.preference_devpass_key), getString(R.string.preference_devpass_default)), this);

        ip = sharedPref.getString(getString(R.string.preference_ip_key), getString(R.string.preference_ip_default));
        devPass = sharedPref.getString(getString(R.string.preference_devpass_key), getString(R.string.preference_devpass_default));
        ssid = sharedPref.getString(getString(R.string.preference_wlanssid_key), getString(R.string.preference_wlanssid_default));
        pass = sharedPref.getString(getString(R.string.preference_wlanpass_key), getString(R.string.preference_wlanpass_default));

        wifi = new WiFi(this.getApplicationContext());
        connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);


        if (savedInstanceState != null) {
            ip = savedInstanceState.getString("ip");
            devPass = savedInstanceState.getString("devPass");
            ssid = savedInstanceState.getString("ssid");
            pass = savedInstanceState.getString("pass");
        }

        awake.setAwake(true);

        Intent intent = new Intent(this, GarageService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onResume(){
        super.onResume();
        Intent intent = new Intent(this, GarageService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop(){
        super.onStop();
        //if (garageBound) {
        //    unbindService(mConnection);
        //    garageBound = false;
        //}
        awake.setAwake(false);
        stopService(new Intent(this, GarageService.class));
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString("ip", ip);
        outState.putString("devPass", devPass);
        outState.putString("ssid", ssid);
        outState.putString("pass", pass);
        super.onSaveInstanceState(outState);
    }

    public void trigger_click(View view) {
        vib.vibrate(new long[] {0, 50}, -1);
        garage.init_wifi(garage.wifi_init_receiver,false);
        trigger();
    }

    public void menu_reboot_click(){
        garage.init_wifi(garage.wifi_init_receiver,false);
        reboot();
    }

    public void trigger(){
        garage.trigger(new CryptConReceiver() {
            @Override
            public void onSuccess(Content response) {

            }

            @Override
            public void onFail() {
                runOnUiThread(() -> {
                    final int old = triggerButton.getCurrentTextColor();
                    triggerButton.setTextColor(Color.RED);
                    (new Handler()).postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            triggerButton.setTextColor(old);
                        }
                    }, 1500);
                });
            }

            @Override
            public void onFinished() {
                setProgr(-1);
            }

            @Override
            public void onProgress(String sprogress, int iprogress) {
                //logTextView.append(sprogress);
                runOnUiThread(() -> {
                    setProgressAnimate(triggerProgress, iprogress);
                });

            }
        });
    }

    public void reboot(){
        garage.reboot(new CryptConReceiver() {

            @Override
            public void onSuccess(Content response) {}

            @Override
            public void onFail() {}

            @Override
            public void onFinished() {}

            @Override
            public void onProgress(String sprogress, int iprogress) {
                logTextView.append(sprogress);
            }
        });
    }

    public void setProgr(int progress){
        if(progress == -1){
            runOnUiThread(() -> {
                new Handler().postDelayed(() -> {
                    setProgr(0);
                }, 1000);
            });
        } else {
            runOnUiThread(() -> {
                setProgressAnimate(triggerProgress, progress);
            });
        }
    }

    private void setProgressAnimate(ProgressBar pb, int progressTo) {
        ObjectAnimator animation = ObjectAnimator.ofInt(pb, "progress", pb.getProgress(), progressTo * 10);
        animation.setDuration(300);
        animation.setInterpolator(new DecelerateInterpolator());
        animation.start();
    }

    @Override
    public void triggerStart() {

    }

    @Override
    public void logMessage(String message) {
        if(logTextView != null)
            logTextView.append(message + "\n");
    }

    @Override
    public void wifiConnected() {
        triggerButton.setTextColor(getResources().getColor(R.color.colorAccent));
    }

    @Override
    public void wifiDisconnected() {
        triggerButton.setTextColor(Color.GRAY);
    }

    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            GarageService.LocalBinder binder = (GarageService.LocalBinder) service;
            garage = binder.getService();
            garageBound = true;
            garage.init(ip, devPass, ssid, pass, Garage.this);
            garage.init_wifi(garage.wifi_init_receiver,false);
            //("wtf", "test");
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            garageBound = false;
        }
    };

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event){
        if (event.getRepeatCount() == 0) {
            if (keyCode == KeyEvent.KEYCODE_STEM_1) {
                triggerButton.setPressed(true);
                triggerButton.setPressed(false);
                trigger_click(null);
                return true;
            }
            if (keyCode == KeyEvent.KEYCODE_STEM_2) {
                Intent intent = new Intent(getApplicationContext(), SettingsActivity.class);
                startActivity(intent);
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }
}
