package de.wladimircomputin.libcryptogarage.protocol;

/**
 * Created by spamd on 11.03.2017.
 */

public interface CryptConReceiver {
    void onSuccess(String response);
    void onFail();
    void onFinished();
    void onProgress(String sprogress, int iprogress);
}
