/*package de.wladimircomputin.cryptogarage;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.wladimircomputin.cryptogarage.util.Updater;
import de.wladimircomputin.libcryptogarage.net.ConReceiver;
import de.wladimircomputin.libcryptogarage.protocol.Content;
import de.wladimircomputin.libcryptogarage.protocol.CryptCon;
import de.wladimircomputin.libcryptogarage.protocol.CryptConReceiver;

public class SettingsActivity_backup extends AppCompatActivity {

    EditText wifimode_ip_text;
    View wifimode_ip_layout;
    EditText devpass_text;
    Spinner  wifimode_spinner;
    EditText wlanssid_text;
    EditText wlanpass_text;
    EditText autotrigger_timeout_text;
    ProgressBar devpass_progress;
    ProgressBar wifimode_progress;
    ProgressBar wlanssid_progress;
    ProgressBar wlanpass_progress;
    ProgressBar autotrigger_timeout_progress;
    Switch lr_switch;

    ArrayAdapter<String> wifimode_spinner_adapter;

    CryptCon cc;
    SharedPreferences sharedPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        wifimode_ip_text = findViewById(R.id.settings_wifimode_ip_text);
        wifimode_ip_layout = findViewById(R.id.settings_wifimode_ip_layout);
        devpass_text = findViewById(R.id.settings_devpass_text);
        wifimode_spinner = findViewById(R.id.settings_wifimode_spinner);
        wlanssid_text = findViewById(R.id.settings_wlanssid_text);
        wlanpass_text = findViewById(R.id.settings_wlanpass_text);
        autotrigger_timeout_text = findViewById(R.id.settings_autotrigger_timeout_text);
        devpass_progress = findViewById(R.id.settings_devpass_progress);
        wifimode_progress = findViewById(R.id.settings_wifimode_progress);
        wlanssid_progress = findViewById(R.id.settings_wlanssid_progress);
        wlanpass_progress = findViewById(R.id.settings_wlanpass_progress);
        autotrigger_timeout_progress = findViewById(R.id.settings_autotrigger_timeout_progress);

        lr_switch = findViewById(R.id.settings_lr_switch);

        List<String> list = new ArrayList<>();
        list.addAll(Arrays.asList(getResources().getStringArray(R.array.wifimode_array)));
        wifimode_spinner_adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, list);
        wifimode_spinner_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        wifimode_spinner.setAdapter(wifimode_spinner_adapter);

        wifimode_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                if (list.get(position).equals("AP")){
                    wifimode_ip_text.setText(getString(R.string.ap_ip_default));
                }
                wifimode_ip_layout.setVisibility(list.get(position).equals("AP") ? View.GONE : View.VISIBLE);

            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {}

        });

        sharedPref = this.getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);

        cc = new CryptCon(sharedPref.getString(getString(R.string.preference_devpass_key), getString(R.string.preference_devpass_default)), sharedPref.getString(getString(R.string.preference_ip_key), getString(R.string.preference_ip_default)),this);

        wifimode_ip_text.setText(sharedPref.getString(getString(R.string.preference_ip_key), getString(R.string.preference_ip_default)));
        devpass_text.setText(sharedPref.getString(getString(R.string.preference_devpass_key), getString(R.string.preference_devpass_default)));
        wifimode_spinner.setSelection(wifimode_spinner_adapter.getPosition(sharedPref.getString(getString(R.string.preference_wifimode_key), getString(R.string.preference_wifimode_default))));
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
            public void onSuccess(Content response) {}

            @Override
            public void onFail() {}

            @Override
            public void onFinished() {
                runOnUiThread(() -> {
                    Toast.makeText(SettingsActivity_backup.this, "Rebooting Device...", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onProgress(String sprogress, int iprogress) {}
        });
    }

    public void update_mode(){
        cc.sendMessageEncrypted(getString(R.string.command_update), new CryptConReceiver() {
            @Override
            public void onSuccess(Content response) {
                runOnUiThread(() -> {
                    Toast.makeText(SettingsActivity_backup.this, "Update server enabled on\n" + response.data, Toast.LENGTH_LONG).show();
                });

                File update = new File(getExternalFilesDir(null), "CryptoGarage.bin");

                if(update.exists()) {

                    AlertDialog.Builder builder = new AlertDialog.Builder(SettingsActivity_backup.this);

                    builder.setTitle("Firmware Update");
                    builder.setMessage("Found binary firmware file \"CryptoGarage.bin\". \n Upload?");

                    builder.setPositiveButton("YES", new DialogInterface.OnClickListener() {

                        public void onClick(DialogInterface dialog, int which) {
                            runOnUiThread(() -> {
                                Toast.makeText(SettingsActivity_backup.this, "Uploading new firmware...", Toast.LENGTH_LONG).show();
                            });
                            Updater updater = new Updater(response.data, update);

                            updater.startUpdate(new ConReceiver() {
                                @Override
                                public void onResponseReceived(String responseData) {
                                    runOnUiThread(() -> {
                                        Toast.makeText(SettingsActivity_backup.this, responseData, Toast.LENGTH_LONG).show();
                                    });
                                }

                                @Override
                                public void onError(String reason) {
                                    runOnUiThread(() -> {
                                        Toast.makeText(SettingsActivity_backup.this, "Error: " + reason, Toast.LENGTH_LONG).show();
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
                    Toast.makeText(SettingsActivity_backup.this, "Error: " + "Failed to enable update server!", Toast.LENGTH_LONG).show();
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
            public void onSuccess(Content response) {
                runOnUiThread(() -> {
                    Toast.makeText(SettingsActivity_backup.this, "All Settings cleared!", Toast.LENGTH_SHORT).show();
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
            public void onSuccess(Content response) {
                runOnUiThread(() -> {
                    AlertDialog.Builder builder1 = new AlertDialog.Builder(SettingsActivity_backup.this);
                    builder1.setMessage(response.data);
                    builder1.setCancelable(true);
                    builder1.setTitle("Status");
                    builder1.show();
                });
            }

            @Override
            public void onFail() {
                    runOnUiThread(() -> {
                        Toast.makeText(SettingsActivity_backup.this, "Failed!", Toast.LENGTH_SHORT).show();
                    });
            }

            @Override
            public void onFinished() {}

            @Override
            public void onProgress(String sprogress, int iprogress) {}
        });
    }

    public void scan_click(View view){
        wifimode_progress.setVisibility(View.VISIBLE);
        cc.discoverDevices(new CryptConReceiver() {
            @Override
            public void onSuccess(Content response) {
                String[] device = response.data.split(":");
                runOnUiThread(() -> {
                    Toast.makeText(SettingsActivity_backup.this, "Found device " + device[0] + " with IP " + device[1], Toast.LENGTH_SHORT).show();
                    wifimode_ip_text.setText(device[1]);
                });
            }

            @Override
            public void onFail() {
                runOnUiThread(() -> {
                    Toast.makeText(SettingsActivity_backup.this, "Device discovery failed!", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onFinished() {
                runOnUiThread(() -> {
                    wifimode_progress.setVisibility(View.GONE);
                });
            }

            @Override
            public void onProgress(String sprogress, int iprogress) { }
        });
    }

    public void save_click(View view){

        SettingsPack save = new SettingsPack(null, null, 0, 0, R.string.command_save, null);
        SettingsPack autotriggerTimeout = new SettingsPack(autotrigger_timeout_text, autotrigger_timeout_progress, R.string.preference_autotrigger_timeout_key, R.string.preference_autotrigger_timeout_default, R.string.command_setAutotriggerTimeout, save);
        SettingsPack devPass = new SettingsPack(devpass_text, devpass_progress, R.string.preference_devpass_key, R.string.preference_devpass_default, R.string.command_setDevicePass, autotriggerTimeout);
        SettingsPack wlanPass = new SettingsPack(wlanpass_text, wlanpass_progress, R.string.preference_wlanpass_key, R.string.preference_wlanpass_default, R.string.command_setWifiPass, devPass);
        SettingsPack wlanSSID = new SettingsPack(wlanssid_text, wlanssid_progress, R.string.preference_wlanssid_key, R.string.preference_wlanssid_default, R.string.command_setSSID, wlanPass);
        SettingsPack wifimode_ip = new SettingsPack(wifimode_ip_text, wifimode_progress, R.string.preference_ip_key, R.string.preference_ip_default, 0, null);
        SettingsPack wifimode = new SettingsPack(wifimode_spinner, wifimode_progress, R.string.preference_wifimode_key, R.string.preference_wifimode_default, R.string.command_setWifiMode, wlanSSID);

        if(lr_switch.isChecked()) {
            wifimode.saveRemote();
            wifimode_ip.saveLocale();
        } else {
            wifimode.saveLocale();
            wifimode_ip.saveLocale();
            wlanSSID.saveLocale();
            wlanPass.saveLocale();
            devPass.saveLocale();
            autotriggerTimeout.saveLocale();
            Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show();
        }
    }

    private class SettingsPack{
        private View view;
        private ProgressBar progressBar;
        private int prefKey;
        private int def;
        private int command;
        private SettingsPack next;

        public SettingsPack(View view, ProgressBar progressBar, int prefKey, int def, int command, SettingsPack next){
            this.view = view;
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
                    public void onSuccess(Content response) {
                        runOnUiThread(() -> {
                            Toast.makeText(SettingsActivity_backup.this, "Settings saved", Toast.LENGTH_SHORT).show();
                        });

                    }

                    @Override
                    public void onFail() {
                        runOnUiThread(() -> {
                            Toast.makeText(SettingsActivity_backup.this, "ERROR: Settings not saved", Toast.LENGTH_SHORT).show();
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
                final String new_;
                if(view instanceof EditText) {
                    new_ = ((EditText)view).getText().toString();
                } else if(view instanceof Spinner){
                    new_ = (String)((Spinner)view).getSelectedItem();
                } else {
                    new_ = "";
                }
                if (!new_.equals(saved) || true) {
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.VISIBLE);
                    });
                    cc.sendMessageEncrypted(getString(command) + new_, new CryptConReceiver() {
                        @Override
                        public void onSuccess(Content response) {
                            runOnUiThread(() -> {
                                saveLocale();
                                progressBar.setVisibility(View.GONE);
                            });

                        }

                        @Override
                        public void onFail() {
                            runOnUiThread(() -> {
                                if(view instanceof EditText) {
                                    ((EditText)view).setText(saved);
                                } else if(view instanceof Spinner){
                                    ((Spinner)view).setSelection(wifimode_spinner_adapter.getPosition(saved));
                                }
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
            final String new_;
            if(view instanceof EditText) {
                new_ = ((EditText)view).getText().toString();
            } else if(view instanceof Spinner){
                new_ = ((Spinner)view).getSelectedItem().toString();
            } else {
                new_ = "";
            }
            sharedPref.edit().putString(getString(prefKey), new_).apply();
        }

    }
}
*/