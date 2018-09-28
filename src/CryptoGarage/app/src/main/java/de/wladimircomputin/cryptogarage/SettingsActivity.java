package de.wladimircomputin.cryptogarage;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.Toast;

import java.io.File;

import de.wladimircomputin.cryptogarage.util.Updater;
import de.wladimircomputin.libcryptogarage.net.TCPConReceiver;
import de.wladimircomputin.libcryptogarage.protocol.CryptCon;
import de.wladimircomputin.libcryptogarage.protocol.CryptConReceiver;

public class SettingsActivity extends AppCompatActivity {

    EditText devpass_text;
    EditText wlanssid_text;
    EditText wlanpass_text;
    EditText autotrigger_timeout_text;
    ProgressBar devpass_progress;
    ProgressBar wlanssid_progress;
    ProgressBar wlanpass_progress;
    ProgressBar autotrigger_timeout_progress;
    Switch lr_switch;

    CryptCon cc;
    SharedPreferences sharedPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        devpass_text = findViewById(R.id.settings_devpass_text);
        wlanssid_text = findViewById(R.id.settings_wlanssid_text);
        wlanpass_text = findViewById(R.id.settings_wlanpass_text);
        autotrigger_timeout_text = findViewById(R.id.settings_autotrigger_timeout_text);
        devpass_progress = findViewById(R.id.settings_devpass_progress);
        wlanssid_progress = findViewById(R.id.settings_wlanssid_progress);
        wlanpass_progress = findViewById(R.id.settings_wlanpass_progress);
        autotrigger_timeout_progress = findViewById(R.id.settings_autotrigger_timeout_progress);

        lr_switch = findViewById(R.id.settings_lr_switch);

        sharedPref = this.getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);

        cc = new CryptCon(sharedPref.getString(getString(R.string.preference_devpass_key), getString(R.string.preference_devpass_default)), this);

        devpass_text.setText(sharedPref.getString(getString(R.string.preference_devpass_key), getString(R.string.preference_devpass_default)));
        wlanssid_text.setText(sharedPref.getString(getString(R.string.preference_wlanssid_key), getString(R.string.preference_wlanssid_default)));
        wlanpass_text.setText(sharedPref.getString(getString(R.string.preference_wlanpass_key), getString(R.string.preference_wlanpass_default)));
        autotrigger_timeout_text.setText(sharedPref.getString(getString(R.string.preference_autotrigger_timeout_key), getString(R.string.preference_autotrigger_timeout_default)));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_settings, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // action with ID action_refresh was selected
            case R.id.menu_reboot:
                reboot();
                break;

            case R.id.menu_update:
                update_mode();
                break;

            case R.id.menu_reset:
                reset();
                break;

            case R.id.menu_status:
                getStatus();
                break;

            default:
                break;
        }
        return true;
    }

    public void reboot(){
        cc.sendMessageEncrypted(getString(R.string.command_reboot), new CryptConReceiver() {
            @Override
            public void onSuccess(String response) {}

            @Override
            public void onFail() {}

            @Override
            public void onFinished() {
                runOnUiThread(() -> {
                    Toast.makeText(SettingsActivity.this, "Rebooting Device...", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onProgress(String sprogress, int iprogress) {}
        });
    }

    public void update_mode(){
        cc.sendMessageEncrypted(getString(R.string.command_update), new CryptConReceiver() {
            @Override
            public void onSuccess(String response) {
                runOnUiThread(() -> {
                    Toast.makeText(SettingsActivity.this, "Update server enabled on\n" + response, Toast.LENGTH_LONG).show();
                });

                File update = new File(getExternalFilesDir(null), "CryptoGarage.bin");

                if(update.exists()) {

                    AlertDialog.Builder builder = new AlertDialog.Builder(SettingsActivity.this);

                    builder.setTitle("Firmware Update");
                    builder.setMessage("Found binary firmware file \"CryptoGarage.bin\". \n Upload?");

                    builder.setPositiveButton("YES", new DialogInterface.OnClickListener() {

                        public void onClick(DialogInterface dialog, int which) {
                            runOnUiThread(() -> {
                                Toast.makeText(SettingsActivity.this, "Uploading new firmware...", Toast.LENGTH_LONG).show();
                            });
                            Updater updater = new Updater(response, update);

                            updater.startUpdate(new TCPConReceiver() {
                                @Override
                                public void onResponseReceived(String responseData) {
                                    runOnUiThread(() -> {
                                        Toast.makeText(SettingsActivity.this, responseData, Toast.LENGTH_LONG).show();
                                    });
                                }

                                @Override
                                public void onError(String reason) {
                                    runOnUiThread(() -> {
                                        Toast.makeText(SettingsActivity.this, "Error: " + reason, Toast.LENGTH_LONG).show();
                                    });
                                }
                            });

                            dialog.dismiss();
                        }
                    });

                    builder.setNegativeButton("NO", new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                            // Do nothing
                            dialog.dismiss();
                        }
                    });

                    runOnUiThread(() -> {
                        AlertDialog alert = builder.create();
                        alert.show();
                    });
                }
            }

            @Override
            public void onFail() {
                runOnUiThread(() -> {
                    Toast.makeText(SettingsActivity.this, "Error: " + "Failed to enable update server!", Toast.LENGTH_LONG).show();
                });
            }

            @Override
            public void onFinished() {}

            @Override
            public void onProgress(String sprogress, int iprogress) {}
        });
    }

    public void reset(){
        cc.sendMessageEncrypted(getString(R.string.command_reset), new CryptConReceiver() {
            @Override
            public void onSuccess(String response) {
                runOnUiThread(() -> {
                    Toast.makeText(SettingsActivity.this, "All Settings cleared!", Toast.LENGTH_SHORT).show();
                    sharedPref.edit().clear().apply();});
            }

            @Override
            public void onFail() {}

            @Override
            public void onFinished() {}

            @Override
            public void onProgress(String sprogress, int iprogress) {}
        });
    }

    public void getStatus(){
        cc.sendMessageEncrypted(getString(R.string.command_status), new CryptConReceiver() {
            @Override
            public void onSuccess(String response) {
                runOnUiThread(() -> {
                    AlertDialog.Builder builder1 = new AlertDialog.Builder(SettingsActivity.this);
                    builder1.setMessage(response);
                    builder1.setCancelable(true);
                    builder1.setTitle("Status");
                    builder1.show();
                });
            }

            @Override
            public void onFail() {
                    runOnUiThread(() -> {
                        Toast.makeText(SettingsActivity.this, "Failed!", Toast.LENGTH_SHORT).show();
                    });
            }

            @Override
            public void onFinished() {}

            @Override
            public void onProgress(String sprogress, int iprogress) {}
        });
    }

    public void save_click(View view){

        SettingsPack save = new SettingsPack(null, null, 0, 0, R.string.command_save, null);
        SettingsPack autotriggerTimeout = new SettingsPack(autotrigger_timeout_text, autotrigger_timeout_progress, R.string.preference_autotrigger_timeout_key, R.string.preference_autotrigger_timeout_default, R.string.command_setAutotriggerTimeout, save);
        SettingsPack wlanPass = new SettingsPack(wlanpass_text, wlanpass_progress, R.string.preference_wlanpass_key, R.string.preference_wlanpass_default, R.string.command_setWifiPass, autotriggerTimeout);
        SettingsPack wlanSSID = new SettingsPack(wlanssid_text, wlanssid_progress, R.string.preference_wlanssid_key, R.string.preference_wlanssid_default, R.string.command_setSSID, wlanPass);
        SettingsPack devPass = new SettingsPack(devpass_text, devpass_progress, R.string.preference_devpass_key, R.string.preference_devpass_default, R.string.command_setDevicePass, wlanSSID);

        if(lr_switch.isChecked()) {
            devPass.saveRemote();
        } else {
            devPass.saveLocale();
            wlanSSID.saveLocale();
            wlanPass.saveLocale();
            autotriggerTimeout.saveLocale();
            Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show();
        }
    }

    private class SettingsPack{
        private EditText editText;
        private ProgressBar progressBar;
        private int prefKey;
        private int def;
        private int command;
        private SettingsPack next;

        public SettingsPack(EditText editText, ProgressBar progressBar, int prefKey, int def, int command, SettingsPack next){
            this.editText = editText;
            this.progressBar = progressBar;
            this.prefKey = prefKey;
            this.def = def;
            this.command = command;
            this.next = next;
        }

        public void saveRemote(){
            if (getString(command).equals(getString(R.string.command_save))){
                cc.sendMessageEncrypted(getString(command), new CryptConReceiver() {
                    @Override
                    public void onSuccess(String response) {
                        runOnUiThread(() -> {
                            Toast.makeText(SettingsActivity.this, "Settings saved", Toast.LENGTH_SHORT).show();
                        });

                    }

                    @Override
                    public void onFail() {
                        runOnUiThread(() -> {
                            Toast.makeText(SettingsActivity.this, "ERROR: Settings not saved", Toast.LENGTH_SHORT).show();
                        });
                    }

                    @Override
                    public void onFinished() {

                    }

                    @Override
                    public void onProgress(String sprogress, int iprogress) {
                    }
                });
            } else {
                final String saved = sharedPref.getString(getString(prefKey), getString(def));
                final String new_ = editText.getText().toString();
                if (!new_.equals(saved)) {
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.VISIBLE);
                    });
                    cc.sendMessageEncrypted(getString(command) + new_, new CryptConReceiver() {
                        @Override
                        public void onSuccess(String response) {
                            runOnUiThread(() -> {
                                saveLocale();
                                progressBar.setVisibility(View.GONE);
                            });

                        }

                        @Override
                        public void onFail() {
                            runOnUiThread(() -> {
                                editText.setText(saved);
                                progressBar.setVisibility(View.GONE);
                            });

                        }

                        @Override
                        public void onFinished() {
                            if (next != null) {
                                next.saveRemote();
                            }
                        }

                        @Override
                        public void onProgress(String sprogress, int iprogress) {}
                    });
                } else {
                    if (next != null) {
                        next.saveRemote();
                    }
                }
            }
        }

        public void saveLocale(){
            final String new_ = editText.getText().toString();
            sharedPref.edit().putString(getString(prefKey), new_).apply();
        }

    }
}
