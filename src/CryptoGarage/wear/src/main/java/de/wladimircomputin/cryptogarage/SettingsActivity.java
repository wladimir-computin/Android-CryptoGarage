package de.wladimircomputin.cryptogarage;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;


public class SettingsActivity extends WearableActivity {

    EditText devpass_text;
    EditText wlanssid_text;
    EditText wlanpass_text;

    SharedPreferences sharedPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        devpass_text = findViewById(R.id.settings_devpass_text);
        wlanssid_text = findViewById(R.id.settings_wlanssid_text);
        wlanpass_text = findViewById(R.id.settings_wlanpass_text);

        sharedPref = this.getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);


        devpass_text.setText(sharedPref.getString(getString(R.string.preference_devpass_key), getString(R.string.preference_devpass_default)));
        wlanssid_text.setText(sharedPref.getString(getString(R.string.preference_wlanssid_key), getString(R.string.preference_wlanssid_default)));
        wlanpass_text.setText(sharedPref.getString(getString(R.string.preference_wlanpass_key), getString(R.string.preference_wlanpass_default)));
    }

    protected void onResume(){
        super.onResume();
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_right);
    }

    public void save_click(View view){

        SettingsPack save = new SettingsPack(null, 0, 0, R.string.command_save, null);
        SettingsPack wlanPass = new SettingsPack(wlanpass_text, R.string.preference_wlanpass_key, R.string.preference_wlanpass_default, R.string.command_setWifiPass, save);
        SettingsPack wlanSSID = new SettingsPack(wlanssid_text, R.string.preference_wlanssid_key, R.string.preference_wlanssid_default, R.string.command_setSSID, wlanPass);
        SettingsPack devPass = new SettingsPack(devpass_text, R.string.preference_devpass_key, R.string.preference_devpass_default, R.string.command_setDevicePass, wlanSSID);

        devPass.saveLocale();
        wlanSSID.saveLocale();
        wlanPass.saveLocale();
        Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show();
    }

    private class SettingsPack{
        private EditText editText;
        private ProgressBar progressBar;
        private int prefKey;
        private int def;
        private int command;
        private SettingsPack next;

        public SettingsPack(EditText editText, int prefKey, int def, int command, SettingsPack next){
            this.editText = editText;
            this.prefKey = prefKey;
            this.def = def;
            this.command = command;
            this.next = next;
        }

        public void saveLocale(){
            final String new_ = editText.getText().toString();
            sharedPref.edit().putString(getString(prefKey), new_).apply();
        }

    }
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event){
        if (event.getRepeatCount() == 0) {
            if (keyCode == KeyEvent.KEYCODE_STEM_2) {
                save_click(null);
                finish();
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_left);
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }
}
