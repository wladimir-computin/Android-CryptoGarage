package de.wladimircomputin.libcryptoiot.v2.protocol;

/**
 * Created by spamd on 11.03.2017.
 */

public interface CryptConReceiver {
    void onSuccess(de.wladimircomputin.libcryptoiot.v2.protocol.Content response);
    void onFail();
    void onFinished();
    void onProgress(String sprogress, int iprogress);
}
