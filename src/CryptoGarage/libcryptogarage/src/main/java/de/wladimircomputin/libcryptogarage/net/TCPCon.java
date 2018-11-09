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

public class TCPCon extends NetCon{

    private static TCPCon instance;

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    public static TCPCon instance(String url, int port, Context context){
        if(instance == null){
            instance = new TCPCon(url, port, context);
        }
        return instance;
    }

    public TCPCon(String ip, int port, Context context) {
        super(ip, port, context);
    }

    public String sendMessage(String message) {
        for (int failcount = 0; failcount < 4; failcount++){
            try {
                if (connect(ip, port)) {
                    out.println(packMessage(message));
                    out.flush();
                    /*StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = in.readLine()) != null) {
                        sb.append(line).append("\n");
                        if (line.contains(END))
                            break;
                    }
                    */
                    String sb = in.readLine();
                    if(sb == null)
                        sb = "";
                    return unpackMessage(sb.toString());
                }
            } catch (Exception x) {
                x.printStackTrace();
            }
            try {
                Thread.sleep(250);
            } catch (Exception x){}
        }
        close();
        return ERROR_HEADER + "No Connection";
    }

    public boolean connect(String ip, int port) throws IOException{
        if(socket == null || !socket.isConnected()) {
            socket = new Socket();
            socket.connect(new InetSocketAddress(ip, port), 500);
            socket.setTcpNoDelay(true);
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


}
