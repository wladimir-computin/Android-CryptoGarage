package de.wladimircomputin.libcryptogarage.net;

import java.util.List;

public interface ConBroadcastReceiver {
    void onResponseReceived(List<String> responseData);

    void onError(String reason);
}
