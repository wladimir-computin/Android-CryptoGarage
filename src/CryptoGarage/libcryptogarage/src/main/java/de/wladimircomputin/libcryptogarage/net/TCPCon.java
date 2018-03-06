package de.wladimircomputin.libcryptogarage.net;

import android.content.Context;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;

import de.wladimircomputin.libcryptogarage.R;

/**
 * Created by spamd on 07.01.2018.
 */

public class TCPCon {

    private static TCPCon instance;

    private String ip = "";
    private int port;
    private Context context;

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    public static TCPCon instance(String url, int port, Context context){
        if(instance == null){
            instance = new TCPCon(url, port, context);
        }
        return instance;
    }

    private TCPCon(String ip, int port, Context context) {
        this.ip = ip;
        this.port = port;
        this.context = context;
    }

    public String sendMessage(String message){
        try {
            if(connect(ip,port)) {
                out.println(packMessage(message));
                out.flush();
                StringBuilder sb = new StringBuilder();
                String line;
                String end = context.getString(R.string.message_end);
                while ((line = in.readLine()) != null) {
                    sb.append(line).append("\n");
                    if(line.contains(end))
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
            socket = new Socket(ip, port);
            //socket.setTcpNoDelay(true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
            socket.setSoTimeout(500);
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
        return context.getString(R.string.message_begin) + in + context.getString(R.string.message_end);
    }

    private String unpackMessage(String in){
        String msg = "";
        int startIndex = in.indexOf(context.getString(R.string.message_begin));
        int endIndex = in.indexOf(context.getString(R.string.message_end), startIndex);

        if ((startIndex != -1) && (endIndex != -1) && (endIndex - startIndex <= 200)){
            msg = in.substring(startIndex + context.getString(R.string.message_begin).length(), endIndex);
        }
        return msg;
    }
}
