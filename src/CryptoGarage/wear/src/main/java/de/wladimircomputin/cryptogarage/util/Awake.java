package de.wladimircomputin.cryptogarage.util;

import android.content.Context;
import android.os.PowerManager;

/**
 * Created by spamd on 03.05.2017.
 */

public class Awake {
    static private Awake awake = null;
    private PowerManager pm;
    private PowerManager.WakeLock wl;

    public Awake(Context context){
        pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        wl = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK, "Garage Wakelock");
    }

    public void setAwake(boolean keep){
        if(keep){
            wl.acquire();
        } else {
            if(wl.isHeld()) {
                wl.release();
            }
        }
    }


}
