package de.wladimircomputin.libcryptoiot.v2.util;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Objects;

public class WifiScanItem {
    public String ssid;
    public String bssid;
    public int enc;
    public int channel;
    public int rssi;
    public boolean hidden;

    public WifiScanItem(String ssid, String bssid, int enc, int channel, int rssi, boolean hidden) {
        this.ssid = ssid;
        this.bssid = bssid;
        this.enc = enc;
        this.channel = channel;
        this.rssi = rssi;
        this.hidden = hidden;
    }

    public WifiScanItem(JSONObject jsonobj){
        try {
            this.ssid = jsonobj.getString("ssid");
            this.bssid = jsonobj.getString("bssid");
            this.enc = jsonobj.getInt("enc");
            this.channel = jsonobj.getInt("ch");
            this.rssi = jsonobj.getInt("rssi");
            this.hidden = jsonobj.getBoolean("hidden");
        } catch (Exception x){}
    }

    public WifiScanItem(String json){
        try {
            JSONObject jsonobj = new JSONObject(json);
            this.ssid = jsonobj.getString("ssid");
            this.bssid = jsonobj.getString("bssid");
            this.enc = jsonobj.getInt("enc");
            this.channel = jsonobj.getInt("ch");
            this.rssi = jsonobj.getInt("rssi");
            this.hidden = jsonobj.getBoolean("hidden");

        } catch (Exception x){}
    }

    public String toJSON() throws JSONException {
        JSONObject out = new JSONObject();
        out.put("ssid", ssid);
        out.put("bssid", bssid);
        out.put("enc", enc);
        out.put("ch", channel);
        out.put("rssi", rssi);
        out.put("hidden", hidden);
        return out.toString();
    }

    @Override
    public WifiScanItem clone(){
        return new WifiScanItem(ssid, bssid, enc, channel, rssi, hidden);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WifiScanItem that = (WifiScanItem) o;
        return enc == that.enc &&
                channel == that.channel &&
                rssi == that.rssi &&
                hidden == that.hidden &&
                Objects.equals(ssid, that.ssid) &&
                Objects.equals(bssid, that.bssid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ssid, bssid, enc, channel, rssi, hidden);
    }
}
