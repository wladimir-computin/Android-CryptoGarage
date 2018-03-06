package de.wladimircomputin.libcryptogarage.net;

/**
 * Created by spamd on 07.01.2018.
 */

public interface TCPConReceiver {
    void onResponseReceived(String responseData);

    void onError(String reason);
}
