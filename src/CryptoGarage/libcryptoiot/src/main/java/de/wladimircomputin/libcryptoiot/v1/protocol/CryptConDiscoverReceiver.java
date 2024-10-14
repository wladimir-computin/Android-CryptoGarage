package de.wladimircomputin.libcryptoiot.v1.protocol;

import java.util.List;


public interface CryptConDiscoverReceiver {
    void onSuccess(List<DiscoveryDevice> results);
    void onFail();
    void onFinished();
}
