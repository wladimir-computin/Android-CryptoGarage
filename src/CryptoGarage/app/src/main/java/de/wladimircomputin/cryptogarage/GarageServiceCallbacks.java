package de.wladimircomputin.cryptogarage;

/**
 * Created by spamd on 23.03.2017.
 */

public interface GarageServiceCallbacks {
    void triggerStart();
    void logMessage(String message);
    void setProgress(String progressbar, int progress);
    void wifiAlreadyConnected();
    void wifiConnected();
    void wifiDisconnected();
}
