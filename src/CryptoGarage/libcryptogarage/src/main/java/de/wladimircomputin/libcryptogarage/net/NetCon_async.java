package de.wladimircomputin.libcryptogarage.net;

public interface NetCon_async {
    void sendMessage(final String message, int retries, final ConReceiver callback);
    void close();
}
