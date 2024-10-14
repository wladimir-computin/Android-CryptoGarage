package de.wladimircomputin.libcryptoiot.v1.net;

import static de.wladimircomputin.libcryptoiot.v1.Constants.max_message_len;
import static de.wladimircomputin.libcryptoiot.v1.Constants.message_begin;
import static de.wladimircomputin.libcryptoiot.v1.Constants.message_end;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

public abstract class NetCon {

    public static final String ERROR_HEADER = "CONFAIL:";

    private final String BEGIN;
    private final String END;
    private final int MAX_MESSAGE_LEN;

    protected String ip = "";
    protected int port;

    public abstract String sendMessage(String message, int retries);

    public NetCon(String ip, int port) {
        this.ip = ip;
        this.port = port;
        this.BEGIN = message_begin;
        this.END = message_end;
        this.MAX_MESSAGE_LEN = max_message_len;
    }

    protected String packMessage(String in){
        return BEGIN + in + END;
    }

    protected String unpackMessage(String in){
        String msg = "";
        int startIndex = in.indexOf(BEGIN);
        int endIndex = in.indexOf(END, startIndex);

        if ((startIndex != -1) && (endIndex != -1) && (endIndex - startIndex <= MAX_MESSAGE_LEN)){
            msg = in.substring(startIndex + BEGIN.length(), endIndex);
        }
        return msg;
    }

    protected InetAddress getIPByName(String host) throws UnknownHostException {
        InetAddress[] all = InetAddress.getAllByName(host);
        boolean found_ipv4 = false;
        for (InetAddress ip : all) {
            if (ip instanceof Inet4Address) {
                return ip;
            }
        }
        return InetAddress.getByName(host);
    }
}
