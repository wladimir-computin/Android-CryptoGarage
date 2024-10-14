package de.wladimircomputin.libcryptoiot.v2.net;

import static de.wladimircomputin.libcryptoiot.v2.Constants.broadcast_ip_default;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

public class UDPCon extends NetCon {

    public static final String ERROR_HEADER = "CONFAIL:";

    private String broadcast_ip;

    public UDPCon(String ip, int port) {
        super(ip, port);
        this.broadcast_ip = broadcast_ip_default;
    }

    public String sendMessage(String message, int retries) {
        return sendMessage(message, false, retries);
    }

    public String sendMessage(String message, boolean broadcast, int retries) {
        for (int failcount = 0; failcount < retries; failcount++) {
            String out = "";
            try (DatagramSocket udpSocket = new DatagramSocket()) {
                udpSocket.setSoTimeout(4000);
                InetAddress ipAddress;
                if (broadcast) {
                    udpSocket.setBroadcast(true);
                    ipAddress = InetAddress.getByName(broadcast_ip);
                } else {
                    ipAddress = getIPByName(ip);
                }
                byte[] sendData = (packMessage(message) + "\n").getBytes("UTF8");
                DatagramPacket udpPacket = new DatagramPacket(sendData, sendData.length, ipAddress, port);
                udpSocket.send(udpPacket);

                byte[] receiveData = new byte[2048];
                udpPacket = new DatagramPacket(receiveData, receiveData.length);
                udpSocket.receive(udpPacket);
                out = unpackMessage(new String(receiveData, 0, udpPacket.getLength()));
                if (broadcast) {
                    String ip = udpPacket.getAddress().getHostAddress();
                    out += ":" + ip;
                }
                return out;
            } catch (Exception x) {
                x.printStackTrace();
            }
            try {
                Thread.sleep(250);
            } catch (Exception x) {
            }
        }


        return ERROR_HEADER + "No Connection";
    }

    public List<String> sendMessageBroadcast(String message) {
        List<String> list = new ArrayList<>();
        try (DatagramSocket udpSocket = new DatagramSocket()) {
            udpSocket.setSoTimeout(1000);
            InetAddress ipAddress;
            udpSocket.setBroadcast(true);
            ipAddress = InetAddress.getByName(broadcast_ip);
            byte[] sendData = (packMessage(message) + "\n").getBytes("UTF8");
            DatagramPacket udpPacket = new DatagramPacket(sendData, sendData.length, ipAddress, port);
            udpSocket.send(udpPacket);

            byte[] receiveData = new byte[1024];
            udpPacket = new DatagramPacket(receiveData, receiveData.length);

            while (true) {
                udpSocket.receive(udpPacket);
                String out = unpackMessage(new String(receiveData, 0, udpPacket.getLength()));
                String ip = udpPacket.getAddress().getHostAddress();
                out += ":" + ip;
                list.add(out);
            }
        } catch (Exception x) {
            //x.printStackTrace();
        }
        return list;
    }
}
