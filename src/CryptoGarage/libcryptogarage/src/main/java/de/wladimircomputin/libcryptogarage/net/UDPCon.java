package de.wladimircomputin.libcryptogarage.net;

import android.content.Context;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import de.wladimircomputin.libcryptogarage.R;

public class UDPCon extends NetCon{

    public static final String ERROR_HEADER = "CONFAIL:";

    private static UDPCon instance;

    private String broadcast_ip;

    public static UDPCon instance(String url, int port, Context context){
        if(instance == null){
            instance = new UDPCon(url, port, context);
        }
        return instance;
    }

    public UDPCon(String ip, int port, Context context) {
        super(ip, port, context);
        this.broadcast_ip = context.getString(R.string.broadcast_ip_default);
    }

    public String sendMessage(String message, boolean broadcast){
        for (int failcount = 0; failcount < 4; failcount++) {
            String out = "";
            try {
                DatagramSocket udpSocket = new DatagramSocket();
                udpSocket.setSoTimeout(2000);
                InetAddress ipAddress;
                if (broadcast) {
                    udpSocket.setBroadcast(true);
                    ipAddress = InetAddress.getByName(broadcast_ip);
                } else {
                    ipAddress = InetAddress.getByName(ip);
                }
                byte[] sendData = (packMessage(message) + "\n").getBytes("UTF8");
                DatagramPacket udpPacket = new DatagramPacket(sendData, sendData.length, ipAddress, port);
                udpSocket.send(udpPacket);

                byte[] receiveData = new byte[1024];
                udpPacket = new DatagramPacket(receiveData, receiveData.length);
                udpSocket.receive(udpPacket);
                out = unpackMessage(new String(receiveData, 0, udpPacket.getLength()));
                if (broadcast) {
                    String ip = udpPacket.getAddress().getHostAddress();
                    out += ":" + ip;
                }
                return out;
            }
            catch (Exception x) {
                x.printStackTrace();
            }
            try {
                Thread.sleep(250);
            } catch (Exception x) {}
        }

        return ERROR_HEADER + "No Connection";
    }

    public String sendMessage(String message){
        return sendMessage(message, false);
    }

}