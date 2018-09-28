package de.wladimircomputin.libcryptogarage.net;

import android.content.Context;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;

import de.wladimircomputin.libcryptogarage.R;

/**
 * Created by spamd on 07.01.2018.
 */

public class TCPCon {

    private static TCPCon instance;

    private String ip = "";
    private int port;

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    private final String BEGIN;
    private final String END;

    public static TCPCon instance(String url, int port, Context context){
        if(instance == null){
            instance = new TCPCon(url, port, context);
        }
        return instance;
    }

    private TCPCon(String ip, int port, Context context) {
        this.ip = ip;
        this.port = port;
        this.BEGIN = context.getString(R.string.message_begin);
        this.END = context.getString(R.string.message_end);
    }

    public String sendMessage(String message){
        try {
            if(connect(ip,port)) {
                out.println(packMessage(message));
                out.flush();
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) {
                    sb.append(line).append("\n");
                    if(line.contains(END))
                        break;
                }
                return unpackMessage(sb.toString());
            }
        } catch (Exception x){
            x.printStackTrace();
        }
        return "FAIL: No Connection";
    }

    public boolean connect(String ip, int port) throws IOException{
        if(socket == null || !socket.isConnected()) {
            socket = new Socket();
            socket.connect(new InetSocketAddress(ip, port), 500);
            //socket.setTcpNoDelay(true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
        }
        return socket.isConnected();
    }

    public void close(){
        try {
            in.close();
            out.close();
            socket.close();
        } catch (Exception x){
            //ignore
        }
        socket = null;
    }

    private String packMessage(String in){
        return BEGIN + in + END;
    }

    private String unpackMessage(String in){
        String msg = "";
        int startIndex = in.indexOf(BEGIN);
        int endIndex = in.indexOf(END, startIndex);

        if ((startIndex != -1) && (endIndex != -1) && (endIndex - startIndex <= 300)){
            msg = in.substring(startIndex + BEGIN.length(), endIndex);
        }
        return msg;
    }
}
