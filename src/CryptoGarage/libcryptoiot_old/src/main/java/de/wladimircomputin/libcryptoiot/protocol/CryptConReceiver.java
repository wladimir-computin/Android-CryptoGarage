package de.wladimircomputin.libcryptoiot.protocol;

/**
 * Created by spamd on 11.03.2017.
 */

public interface CryptConReceiver {
    void onSuccess(Content response);
    void onFail();
    void onFinished();
    void onProgress(String sprogress, int iprogress);
}
