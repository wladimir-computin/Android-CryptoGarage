package de.wladimircomputin.libcryptoiot.v1.protocol;

/**
 * Created by spamd on 11.03.2017.
 */

public interface CryptConBulkReceiver {
    void onSuccess(de.wladimircomputin.libcryptoiot.v1.protocol.Content response, int i);
    void onFail(int i);
    void onFinished();
    void onProgress(String sprogress, int iprogress);
}
