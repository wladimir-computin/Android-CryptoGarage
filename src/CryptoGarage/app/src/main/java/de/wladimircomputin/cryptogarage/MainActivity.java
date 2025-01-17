package de.wladimircomputin.cryptogarage;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.text.Editable;
import android.text.SpannableString;
import android.text.TextWatcher;
import android.text.util.Linkify;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.app.ActivityCompat;

import de.wladimircomputin.cryptogarage.util.GateState;
import de.wladimircomputin.cryptogarage.util.WiFi;
import de.wladimircomputin.libcryptoiot.v2.protocol.Content;
import de.wladimircomputin.libcryptoiot.v2.protocol.CryptConReceiver;

public class MainActivity extends AppCompatActivity implements GarageServiceCallbacks{

    GarageService garage;
    boolean garageBound;

    Button triggerButton;
    ImageView statusImageView;
    ImageView statusImageViewAnim;
    TextView logTextView;
    ProgressBar triggerProgress;

    WiFi wifi;
    SharedPreferences sharedPref;
    Handler gateStateHandler = new Handler();

    String ip;
    String devPass;
    String ssid;
    String pass;
    String wifimode;

    GateState currentGateState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        logTextView = findViewById(R.id.logTextView);
        triggerButton = findViewById(R.id.triggerButton);
        statusImageView = findViewById(R.id.statusImageView);
        statusImageViewAnim = findViewById(R.id.statusImageViewAnim);
        triggerProgress = findViewById(R.id.triggerButton_progress);
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

        wifimode = sharedPref.getString(getString(R.string.preference_wifimode_key), getString(R.string.preference_wifimode_default));

        if(!wifimode.equals("Remote")) {
            wifi = WiFi.instance(this.getApplicationContext());
            this.ssid = sharedPref.getString(getString(R.string.preference_wlanssid_key), getString(R.string.preference_wlanssid_default));
        }

        if (savedInstanceState != null) {
            wifimode = savedInstanceState.getString("wifimode");
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
        triggerProgress.setProgress(0);
        attachUpdateGatestate(100);
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
    public void onBackPressed() {
        if (garageBound) {
            unbindService(mConnection);
            garageBound = false;
        }
        stopService(new Intent(this, GarageService.class));
        finishAndRemoveTask();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString("wifimode", wifimode);
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
        if(!wifimode.equals("Remote")) {
            garage.init_wifi(garage.wifi_init_receiver, false);
        }
        trigger();
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
                gateStateHandler.postDelayed(this, 1000);
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
        if(garage != null) {
            garage.getGateState(new CryptConReceiver() {
                @Override
                public void onSuccess(Content response) {
                    runOnUiThread(() -> {
                        triggerButton.setTextColor(getResources().getColor(R.color.colorAccent));
                        GateState gateState = GateState.valueOf(response.data);
                        if (!gateState.equals(currentGateState)) {
                            animateGateStateChange(gateState);
                            currentGateState = gateState;
                        }
                    });
                }

                @Override
                public void onFail() {
                    runOnUiThread(() -> {
                        triggerButton.setTextColor(Color.GRAY);
                        GateState gateState = GateState.GATE_NONE;
                        if (!gateState.equals(currentGateState)) {
                            animateGateStateChange(gateState);
                            currentGateState = gateState;
                        }
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
                } else {
                    setProgressAnimate(triggerProgress, progress);
                }
            });
        }
    }

    private void animateGateStateChange(GateState newGateState){
        AnimatorSet animatorSet = (AnimatorSet) AnimatorInflater.loadAnimator(MainActivity.this, R.animator.gate_state_transition_1);
        AnimatorSet animatorSet2 = (AnimatorSet) AnimatorInflater.loadAnimator(MainActivity.this, R.animator.gate_state_transition_2);
        animatorSet.setTarget(statusImageViewAnim);
        animatorSet2.setTarget(statusImageView);
        animatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation, boolean isReverse) {
                super.onAnimationStart(animation, isReverse);
                statusImageViewAnim.setImageDrawable(statusImageView.getDrawable());
                statusImageViewAnim.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animator animation, boolean isReverse) {
                statusImageViewAnim.setVisibility(View.INVISIBLE);
            }
        });

        animatorSet2.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation, boolean isReverse) {
                super.onAnimationStart(animation, isReverse);
                statusImageView.setImageDrawable(AppCompatResources.getDrawable(MainActivity.this, newGateState.getIcon()));
            }

            @Override
            public void onAnimationEnd(Animator animation, boolean isReverse) {
            }
        });
        animatorSet.start();
        animatorSet2.start();
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
        runOnUiThread(() -> {
            logTextView.append(message);
        });
    }

    @Override
    public void wifiConnected() {
        runOnUiThread(() -> {
            triggerButton.setTextColor(getResources().getColor(R.color.colorAccent));
        });
    }

    @Override
    public void wifiAlreadyConnected() {
        runOnUiThread(() -> {
            triggerButton.setTextColor(getResources().getColor(R.color.colorAccent));
        });
    }

    @Override
    public void wifiDisconnected() {
        runOnUiThread(() -> {
            triggerButton.setTextColor(Color.GRAY);
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
            if(!wifimode.equals("Remote")) {
                garage.init_wifi(garage.wifi_init_receiver, false);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            garageBound = false;
        }
    };

}
