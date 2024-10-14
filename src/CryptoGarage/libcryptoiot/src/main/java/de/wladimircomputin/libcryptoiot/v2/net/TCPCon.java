package de.wladimircomputin.libcryptoiot.v2.net;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Created by spamd on 07.01.2018.
 */

public class TCPCon extends NetCon{

    private static TCPCon instance;

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    public TCPCon(String ip, int port) {
        super(ip, port);
    }

    public String sendMessage(String message, int retries) {
        for (int failcount = 0; failcount < retries; failcount++){
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
