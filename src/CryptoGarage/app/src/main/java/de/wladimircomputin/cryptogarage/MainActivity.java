package de.wladimircomputin.cryptogarage;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.SpannableString;
import android.text.TextWatcher;
import android.text.util.Linkify;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.security.acl.Permission;

import de.wladimircomputin.cryptogarage.util.GateState;
import de.wladimircomputin.cryptogarage.util.WiFi;
import de.wladimircomputin.libcryptogarage.protocol.Content;
import de.wladimircomputin.libcryptogarage.protocol.CryptConReceiver;
import de.wladimircomputin.libcryptogarage.protocol.Crypter;

public class MainActivity extends AppCompatActivity implements GarageServiceCallbacks{

    GarageService garage;
    boolean garageBound;

    Button triggerButton;
    ImageView statusImageView;
    Button autotriggerButton;
    TextView logTextView;
    ProgressBar triggerProgress;
    ProgressBar autotriggerProgress;
    ProgressBar autotriggerProgress_indeterminate;

    WiFi wifi;
    SharedPreferences sharedPref;
    Handler gateStateHandler = new Handler();

    String ip;
    String devPass;
    String ssid;
    String pass;
    int autotrigger_timeout;

    GateState currentGateState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        logTextView = findViewById(R.id.logTextView);
        triggerButton = findViewById(R.id.triggerButton);
        statusImageView = findViewById(R.id.statusImageView);
        autotriggerButton = findViewById(R.id.autotriggerButton);
        triggerProgress = findViewById(R.id.triggerButton_progress);
        autotriggerProgress = findViewById(R.id.autotriggerButton_progress);
        autotriggerProgress_indeterminate = findViewById(R.id.autotriggerButton_progress_indeterminate);
        final ScrollView sc = findViewById(R.id.sc);
        logTextView.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {sc.fullScroll(ScrollView.FOCUS_DOWN);}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        });

        sharedPref = this.getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        //cc = new CryptCon(sharedPref.getString(getString(R.string.preference_devpass_key), getString(R.string.preference_devpass_default)), this);

        ip = sharedPref.getString(getString(R.string.preference_ip_key), getString(R.string.preference_ip_default));
        devPass = sharedPref.getString(getString(R.string.preference_devpass_key), getString(R.string.preference_devpass_default));
        ssid = sharedPref.getString(getString(R.string.preference_wlanssid_key), getString(R.string.preference_wlanssid_default));
        pass = sharedPref.getString(getString(R.string.preference_wlanpass_key), getString(R.string.preference_wlanpass_default));
        autotrigger_timeout = Integer.valueOf(sharedPref.getString(getString(R.string.preference_autotrigger_timeout_key), getString(R.string.preference_autotrigger_timeout_default)));

        wifi = WiFi.instance(this.getApplicationContext());

        if (savedInstanceState != null) {
            ip = savedInstanceState.getString("ip");
            devPass = savedInstanceState.getString("devPass");
            ssid = savedInstanceState.getString("ssid");
            pass = savedInstanceState.getString("pass");
            autotrigger_timeout = savedInstanceState.getInt("autotrigger_timeout");
        }

        Intent intent = new Intent(this, GarageService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 2);
        }
    }

    @Override
    protected void onResume(){
        super.onResume();
        attachUpdateGatestate(500);
    }

    @Override
    protected void onStop(){
        super.onStop();
    }

    @Override
    protected void onPause(){
        super.onPause();
        detachUpdateGatestate();
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        if (garageBound) {
            unbindService(mConnection);
            garageBound = false;
        }
        stopService(new Intent(this, GarageService.class));
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString("ip", ip);
        outState.putString("devPass", devPass);
        outState.putString("ssid", ssid);
        outState.putString("pass", pass);
        outState.putInt("autotrigger_timeout", autotrigger_timeout);
        super.onSaveInstanceState(outState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // action with ID action_refresh was selected
            case R.id.menu_reboot:
                    menu_reboot_click();
                break;

            case R.id.menu_app_restart:
                    Intent i = getBaseContext().getPackageManager().getLaunchIntentForPackage(getBaseContext().getPackageName() );
                    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    stopService(new Intent(this, GarageService.class));
                    finish();
                    startActivity(i);
                break;

            case R.id.menu_settings:
                    Intent intent = new Intent(getApplicationContext(), SettingsActivity.class);
                    startActivity(intent);
                break;

            case R.id.menu_status:
                getStatus();
                break;

            case R.id.menu_about:
                    String version = BuildConfig.VERSION_NAME;
                    String about =  getString(R.string.about_author) + "\n\n" +
                                    getString(R.string.about_source);
                    AlertDialog.Builder builder1 = new AlertDialog.Builder(this);
                    SpannableString message = new SpannableString(about);
                    Linkify.addLinks(message, Linkify.ALL);
                    builder1.setMessage(message);
                    builder1.setCancelable(true);
                    builder1.setTitle("CryptoGarage " + version);
                    builder1.show();
                break;

            default:
                break;
        }
        return true;
    }

    public void trigger_click(View view) {
        garage.autotriggerStopSearch();
        garage.init_wifi(garage.wifi_init_receiver,false);
        trigger();
    }

    public void autotrigger_click(View view) {
        if(wifi.isConnectedTo(ssid)){
            autotrigger();
        } else if(garage.isAutotrigger_searching()){
            garage.autotriggerStopSearch();
            setProgressIndeterminate(false);
        } else if (!garage.isAutotrigger_active()) {
            setProgressIndeterminate(true, 1);
            garage.init_wifi(garage.autotrigger_receiver, true);
        } else if (garage.isAutotrigger_active()) {
            garage.autotrigger_finish.run();
            setProgressIndeterminate(false);
        }
    }

    public void menu_reboot_click(){
        reboot();
    }

    private void attachUpdateGatestate(int after){
        detachUpdateGatestate();
        gateStateHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                updateGateState(null);
                gateStateHandler.postDelayed(this, 5000);
            }
        }, after);
    }

    private void detachUpdateGatestate(){
        gateStateHandler.removeCallbacksAndMessages(null);
    }

    public void trigger(){
        garage.failsafe_trigger(new CryptConReceiver() {
            @Override
            public void onSuccess(Content response) {
                setProgressIndeterminate(false);
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
                setProgress("trigger", -1);
                attachUpdateGatestate(300);
            }

            @Override
            public void onProgress(String sprogress, int iprogress) {
                runOnUiThread(() -> {
                    logTextView.append(sprogress);
                    setProgressAnimate(triggerProgress, iprogress);
                });
            }
        }, 0);
    }

    public void autotrigger(){
        garage.autotrigger( new CryptConReceiver() {
            @Override
            public void onSuccess(Content response) {
            }

            @Override
            public void onFail() {
                runOnUiThread(() -> {
                    final int old = autotriggerButton.getCurrentTextColor();
                    autotriggerButton.setTextColor(Color.RED);
                    (new Handler()).postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            autotriggerButton.setTextColor(old);
                        }
                    }, 1500);
                });
            }

            @Override
            public void onFinished() {
                setProgress("autotrigger", -1);
                setProgressIndeterminate(garage.isAutotrigger_active());
                attachUpdateGatestate(300);
            }

            @Override
            public void onProgress(String sprogress, int iprogress) {
                runOnUiThread(() -> {
                    logTextView.append(sprogress);
                    setProgressAnimate(autotriggerProgress, iprogress);
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
                runOnUiThread(() -> {
                    logTextView.append(sprogress);
                });
            }
        });
    }

    public void updateGateState(View view){
        Log.d("WTF", "updateGateState: ");
        garage.getGateState(new CryptConReceiver() {
            @Override
            public void onSuccess(Content response) {
                runOnUiThread(() -> {
                    GateState gateState = GateState.valueOf(response.data);
                    if(!gateState.equals(currentGateState)) {
                        ObjectAnimator scaleXAnimation = ObjectAnimator.ofFloat(statusImageView, "scaleX", 1.0f, 20.0f);
                        scaleXAnimation.setInterpolator(new AccelerateInterpolator());
                        scaleXAnimation.setDuration(500);
                        ObjectAnimator scaleYAnimation = ObjectAnimator.ofFloat(statusImageView, "scaleY", 1.0f, 20.0f);
                        scaleYAnimation.setInterpolator(new AccelerateInterpolator());
                        scaleYAnimation.setDuration(500);
                        ObjectAnimator alphaAnimation = ObjectAnimator.ofFloat(statusImageView, "alpha", 1.0F, 0F);
                        alphaAnimation.setInterpolator(new AccelerateInterpolator());
                        alphaAnimation.setDuration(200);

                        AnimatorSet animatorSet = new AnimatorSet();
                        animatorSet.play(scaleXAnimation).with(scaleYAnimation).with(alphaAnimation);
                        animatorSet.start();
                        animatorSet.addListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation, boolean isReverse) {
                                statusImageView.setImageDrawable(getDrawable(gateState.getIcon()));
                                ObjectAnimator scaleXAnimation = ObjectAnimator.ofFloat(statusImageView, "scaleX", 0.0f, 1.0f);
                                scaleXAnimation.setInterpolator(new AccelerateDecelerateInterpolator());
                                scaleXAnimation.setDuration(500);
                                ObjectAnimator scaleYAnimation = ObjectAnimator.ofFloat(statusImageView, "scaleY", 0.0f, 1.0f);
                                scaleYAnimation.setInterpolator(new AccelerateDecelerateInterpolator());
                                scaleYAnimation.setDuration(500);
                                ObjectAnimator alphaAnimation = ObjectAnimator.ofFloat(statusImageView, "alpha", 0.0F, 1.0f);
                                alphaAnimation.setInterpolator(new AccelerateDecelerateInterpolator());
                                alphaAnimation.setDuration(500);
                                AnimatorSet animatorSet = new AnimatorSet();
                                animatorSet.play(scaleXAnimation).with(scaleYAnimation).with(alphaAnimation);
                                animatorSet.start();
                            }
                        });
                    }
                    currentGateState = gateState;
                });
            }

            @Override
            public void onFail() {
                runOnUiThread(() -> {
                    statusImageView.setImageDrawable(getDrawable(GateState.GATE_NONE.getIcon()));
                });
            }

            @Override
            public void onFinished() {

            }

            @Override
            public void onProgress(String sprogress, int iprogress) {

            }
        });
    }

    public void getStatus(){
        garage.getStatus(new CryptConReceiver() {
            @Override
            public void onSuccess(Content response) {
                runOnUiThread(() -> {
                    AlertDialog.Builder builder1 = new AlertDialog.Builder(MainActivity.this);
                    builder1.setMessage(response.data);
                    builder1.setCancelable(true);
                    builder1.setTitle("Status");
                    builder1.show();
                });
            }

            @Override
            public void onFail() {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Failed!", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onFinished() {}

            @Override
            public void onProgress(String sprogress, int iprogress) {
                runOnUiThread(() -> {
                    logTextView.append(sprogress);
                });
            }
        });
    }

    public void setProgress(String progressbar, int progress){
        if(progress == -1){
            runOnUiThread(() -> {
                new Handler().postDelayed(() -> {
                    setProgress(progressbar, 0);
                }, 1000);
            });
        } else {
            runOnUiThread(() -> {
                if(progressbar.equals("autotrigger")){
                    setProgressAnimate(autotriggerProgress, progress);
                } else {
                    setProgressAnimate(triggerProgress, progress);
                }
            });
        }
    }

    private void setProgressAnimate(ProgressBar pb, int progressTo) {
        ObjectAnimator animation = ObjectAnimator.ofInt(pb, "progress", pb.getProgress(), progressTo * 10);
        animation.setDuration(300);
        animation.setInterpolator(new DecelerateInterpolator());
        animation.start();
    }

    private void setProgressIndeterminate(boolean indeterminate) {
        setProgressIndeterminate(indeterminate, 0);
    }

    private void setProgressIndeterminate(boolean indeterminate, int mode) {
        runOnUiThread(() -> {
            if (indeterminate){
                //setProgressAnimate(autotriggerProgress, 0, 0);
                Interpolator i;
                switch (mode){
                    case 0:
                        i = new DecelerateInterpolator();
                        if(!autotriggerProgress_indeterminate.getInterpolator().equals(i))
                            autotriggerProgress_indeterminate.setInterpolator(i);
                        break;

                    case 1:
                        i = new AccelerateInterpolator();
                        if(!autotriggerProgress_indeterminate.getInterpolator().equals(i))
                            autotriggerProgress_indeterminate.setInterpolator(i);
                        break;
                }
                autotriggerProgress_indeterminate.setVisibility(View.VISIBLE);
            } else {
                autotriggerProgress_indeterminate.setVisibility(View.GONE);
            }
        });
    }

    @Override
    public void triggerStart() {

    }

    @Override
    public void autotriggerStart() {
        runOnUiThread(() -> {
            autotriggerProgress.setProgress(0);
            setProgressIndeterminate(false);
        });
    }

    @Override
    public void autotriggerCountdown(boolean stop) {
        runOnUiThread(() -> {
            if(!stop) {
                setProgressIndeterminate(false);
                if (autotrigger_timeout > 1) {
                    ObjectAnimator animation = ObjectAnimator.ofInt(autotriggerProgress, "progress", 100 * 10, 0);
                    animation.setDuration((autotrigger_timeout * 1000) - 1000);
                    animation.setInterpolator(new LinearInterpolator());
                    animation.setAutoCancel(true);
                    animation.start();
                }
            } else {
                setProgressIndeterminate(true);
            }
        });
    }

    @Override
    public void autotriggerCycleEnd() {
        //finish();
    }

    @Override
    public void logMessage(String message) {
        runOnUiThread(() -> {
            logTextView.append(message);
        });
    }

    @Override
    public void wifiConnected() {
        runOnUiThread(() -> {
            triggerButton.setTextColor(getResources().getColor(R.color.colorAccent));
            autotriggerButton.setTextColor(getResources().getColor(R.color.colorAccent));
        });
    }

    @Override
    public void wifiAlreadyConnected() {
        runOnUiThread(() -> {
            triggerButton.setTextColor(getResources().getColor(R.color.colorAccent));
            autotriggerButton.setTextColor(getResources().getColor(R.color.colorAccent));
        });
    }

    @Override
    public void wifiDisconnected() {
        runOnUiThread(() -> {
            triggerButton.setTextColor(Color.GRAY);
            autotriggerButton.setTextColor(Color.GRAY);
        });

    }

    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            GarageService.LocalBinder binder = (GarageService.LocalBinder) service;
            garage = binder.getService();
            garageBound = true;
            garage.init(sharedPref, MainActivity.this);
            garage.init_wifi(garage.wifi_init_receiver,false);
            setProgressIndeterminate(garage.isAutotrigger_active() || garage.isAutotrigger_searching());
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            garageBound = false;
        }
    };

}
