package de.wladimircomputin.cryptogarage;

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

public class SettingsActivity extends AppCompatActivity {

    EditText wifimode_ip_text;
    EditText wifimode_remote_text;
    View wifimode_ip_layout;
    View wifimode_remote_layout;
    EditText devpass_text;
    Spinner wifimode_spinner;
    EditText wlanssid_text;
    EditText wlanpass_text;
    ProgressBar wifimode_progress;

    ArrayAdapter<String> wifimode_spinner_adapter;

    CryptCon cc;
    SharedPreferences sharedPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        wifimode_ip_text = findViewById(R.id.settings_wifimode_ip_text);
        wifimode_ip_layout = findViewById(R.id.settings_wifimode_ip_layout);
        wifimode_remote_text = findViewById(R.id.settings_wifimode_remote_text);
        wifimode_remote_layout = findViewById(R.id.settings_wifimode_remote_layout);
        devpass_text = findViewById(R.id.settings_devpass_text);
        wifimode_spinner = findViewById(R.id.settings_wifimode_spinner);
        wlanssid_text = findViewById(R.id.settings_wlanssid_text);
        wlanpass_text = findViewById(R.id.settings_wlanpass_text);
        wifimode_progress = findViewById(R.id.settings_wifimode_progress);


        List<String> list = new ArrayList<>();
        list.addAll(Arrays.asList(getResources().getStringArray(R.array.wifimode_array)));
        wifimode_spinner_adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, list);
        wifimode_spinner_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        wifimode_spinner.setAdapter(wifimode_spinner_adapter);

        wifimode_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                wifimode_ip_layout.setVisibility(list.get(position).equals("AP") ?  View.VISIBLE : View.GONE);
                wifimode_remote_layout.setVisibility(list.get(position).equals("Remote") ? View.VISIBLE : View.GONE);
                if (list.get(position).equals("AP")) {
                    wifimode_ip_text.setText(getString(R.string.ap_ip_default));
                }

            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
            }

        });

        sharedPref = this.getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);


        String wifimode = sharedPref.getString(getString(R.string.preference_wifimode_key), getString(R.string.preference_wifimode_default));
        String ssid = sharedPref.getString(getString(R.string.preference_wlanssid_key), getString(R.string.preference_wlanssid_default));
        String pass = sharedPref.getString(getString(R.string.preference_wlanpass_key), getString(R.string.preference_wlanpass_default));
        String devPass = sharedPref.getString(getString(R.string.preference_devpass_key), getString(R.string.preference_devpass_default));
        String ip = sharedPref.getString(getString(R.string.preference_ip_key), getString(R.string.preference_ip_default));
        String remote_url = sharedPref.getString(getString(R.string.preference_remote_key), getString(R.string.preference_remote_default));;


        if(!wifimode.equals("Remote")){
            cc = new CryptCon(devPass, ip, this);
        } else {
            if(remote_url.contains(":")){
                String server = remote_url.split(":")[0];
                int port = Integer.parseInt(remote_url.split(":")[1]);
                cc = new CryptCon(devPass, server, port, port, this);
            }

        }

        wifimode_ip_text.setText(ip);
        wifimode_remote_text.setText(remote_url);
        devpass_text.setText(devPass);
        wifimode_spinner.setSelection(wifimode_spinner_adapter.getPosition(wifimode));
        wlanssid_text.setText(ssid);
        wlanpass_text.setText(pass);
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
            case android.R.id.home:
                finish();
                break;

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

    public void reboot() {
        cc.sendMessageEncrypted(getString(R.string.command_reboot), new CryptConReceiver() {
            @Override
            public void onSuccess(Content response) {
            }

            @Override
            public void onFail() {
            }

            @Override
            public void onFinished() {
                runOnUiThread(() -> {
                    Toast.makeText(SettingsActivity.this, "Rebooting Device...", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onProgress(String sprogress, int iprogress) {
            }
        });
    }

    public void update_mode() {
        cc.sendMessageEncrypted(getString(R.string.command_update), new CryptConReceiver() {
            @Override
            public void onSuccess(Content response) {
                runOnUiThread(() -> {
                    Toast.makeText(SettingsActivity.this, "Update server enabled on\n" + response.data, Toast.LENGTH_LONG).show();
                });

                File update = new File(getExternalFilesDir(null), "CryptoGarage.bin");

                if (update.exists()) {

                    AlertDialog.Builder builder = new AlertDialog.Builder(SettingsActivity.this);

                    builder.setTitle("Firmware Update");
                    builder.setMessage("Found binary firmware file \"CryptoGarage.bin\". \n Upload?");

                    builder.setPositiveButton("YES", new DialogInterface.OnClickListener() {

                        public void onClick(DialogInterface dialog, int which) {
                            runOnUiThread(() -> {
                                Toast.makeText(SettingsActivity.this, "Uploading new firmware...", Toast.LENGTH_LONG).show();
                            });
                            Updater updater = new Updater(response.data, update);

                            updater.startUpdate(new ConReceiver() {
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
            public void onFinished() {
            }

            @Override
            public void onProgress(String sprogress, int iprogress) {
            }
        });
    }

    public void reset() {
        cc.sendMessageEncrypted(getString(R.string.command_reset), new CryptConReceiver() {
            @Override
            public void onSuccess(Content response) {
                runOnUiThread(() -> {
                    Toast.makeText(SettingsActivity.this, "All Settings cleared!", Toast.LENGTH_SHORT).show();
                    sharedPref.edit().clear().apply();
                });
            }

            @Override
            public void onFail() {
            }

            @Override
            public void onFinished() {
            }

            @Override
            public void onProgress(String sprogress, int iprogress) {
            }
        });
    }

    public void getStatus() {
        cc.sendMessageEncrypted(getString(R.string.command_status), new CryptConReceiver() {
            @Override
            public void onSuccess(Content response) {
                runOnUiThread(() -> {
                    AlertDialog.Builder builder1 = new AlertDialog.Builder(SettingsActivity.this);
                    builder1.setMessage(response.data);
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
            public void onFinished() {
            }

            @Override
            public void onProgress(String sprogress, int iprogress) {
            }
        });
    }

    public void scan_click(View view) {
        wifimode_progress.setVisibility(View.VISIBLE);
        cc.discoverDevices(new CryptConReceiver() {
            @Override
            public void onSuccess(Content response) {
                String[] device = response.data.split(":");
                runOnUiThread(() -> {
                    Toast.makeText(SettingsActivity.this, "Found device " + device[0] + " with IP " + device[1], Toast.LENGTH_SHORT).show();
                    wifimode_ip_text.setText(device[1]);
                });
            }

            @Override
            public void onFail() {
                runOnUiThread(() -> {
                    Toast.makeText(SettingsActivity.this, "Device discovery failed!", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onFinished() {
                runOnUiThread(() -> {
                    wifimode_progress.setVisibility(View.GONE);
                });
            }

            @Override
            public void onProgress(String sprogress, int iprogress) {
            }
        });
    }

    public void save_click(View view) {

        SettingsPack devPass = new SettingsPack(devpass_text, R.string.preference_devpass_key, R.string.preference_devpass_default);
        SettingsPack wlanPass = new SettingsPack(wlanpass_text, R.string.preference_wlanpass_key, R.string.preference_wlanpass_default);
        SettingsPack wlanSSID = new SettingsPack(wlanssid_text, R.string.preference_wlanssid_key, R.string.preference_wlanssid_default);
        SettingsPack wifimode_ip = new SettingsPack(wifimode_ip_text, R.string.preference_ip_key, R.string.preference_ip_default);
        SettingsPack wifimode_remote = new SettingsPack(wifimode_remote_text, R.string.preference_remote_key, R.string.preference_remote_default);
        SettingsPack wifimode = new SettingsPack(wifimode_spinner, R.string.preference_wifimode_key, R.string.preference_wifimode_default);

        wifimode.saveLocale();
        wifimode_ip.saveLocale();
        wifimode_remote.saveLocale();
        wlanSSID.saveLocale();
        wlanPass.saveLocale();
        devPass.saveLocale();
        Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show();
    }

    private class SettingsPack {
        private final View view;
        private final int prefKey;
        private final int def;

        public SettingsPack(View view, int prefKey, int def) {
            this.view = view;
            this.prefKey = prefKey;
            this.def = def;
        }

        public void saveLocale() {
            final String new_;
            if (view instanceof EditText) {
                new_ = ((EditText) view).getText().toString();
            } else if (view instanceof Spinner) {
                new_ = ((Spinner) view).getSelectedItem().toString();
            } else {
                new_ = "";
            }
            sharedPref.edit().putString(getString(prefKey), new_).apply();
        }

    }
}
