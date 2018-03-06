package de.wladimircomputin.cryptogarage;

/**
 * Created by spamd on 23.03.2017.
 */

public interface GarageServiceCallbacks {
    void triggerStart();
    void logMessage(String message);
    void setProgr(int progress);
    void wifiConnected();
    void wifiDisconnected();
}
