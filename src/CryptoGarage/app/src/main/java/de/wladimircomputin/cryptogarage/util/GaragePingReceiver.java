package de.wladimircomputin.cryptogarage.util;

/**
 * Created by spamd on 26.03.2017.
 */

public interface GaragePingReceiver {
    void onPingSuccess();
    void onPingFail(int count);
    void onPingLost();
}
