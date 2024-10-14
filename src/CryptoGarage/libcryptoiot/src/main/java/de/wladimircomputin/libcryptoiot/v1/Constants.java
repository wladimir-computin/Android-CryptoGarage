package de.wladimircomputin.libcryptoiot.v1;

public class Constants {
    public static final int sha_rounds = 5000;
    public static final int max_message_len = 2048;
    public static final int challenge_validity_timeout = 10000;
    public static final String message_begin = "[BEGIN]";
    public static final String message_end = "[END]";
    public static final String header_hello = "HELLO";
    public static final String header_ok = "OK";
    public static final String header_data ="DATA";
    public static final String header_error = "FAIL";
    public static final String key_salt = "FTh.!%B$";
    public static final String command_discover = "discover";
    public static final String broadcast_ip_default = "255.255.255.255";

    public static final String command_readSettings = "reads";
    public static final String command_writeSettings = "writes";
    public static final String command_ping = "ping";
    public static final String command_save = "save";
    public static final String command_reboot = "reboot";
    public static final String command_reset = "reset";
    public static final String command_status = "status";
    public static final String command_wifiscan = "wifiscan";
    public static final String command_wifiresults = "wifiresults";
    public static final String command_update = "update";
    public static final String wifissid_factory_default = "CryptoIoT-Setup";
    public static final String wifipass_factory_default = "12345670";
    public static final String devicepass_factory_default = "TestTest1";
    public static final String ap_ip_default = "192.168.4.1";
}
