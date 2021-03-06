package de.wladimircomputin.cryptogarage.util;

import android.util.Log;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import de.wladimircomputin.libcryptogarage.net.ConReceiver;


public class Updater {
    private File file;
    private String url;

    public Updater(String url, File file){
        this.file = file;
        this.url = url;
    }

    public void startUpdate(final ConReceiver callback){
        new Thread(() -> {
            final String out = startUpdate();
            if (out.contains("Error") || out.isEmpty()) {
                callback.onError(out);
            } else {
                callback.onResponseReceived(out);
            }
        }).start();
    }

    private String startUpdate(){
        HttpUploader uploader = new HttpUploader();
        return uploader.send(url, file);
    }

    private class HttpUploader {

        String crlf = "\r\n";
        String twoHyphens = "--";
        String boundary = "*****";

        String send(String path, File file) {
            String out = "";
            try {
                if(!path.startsWith("http://")){
                    path = "http://" + path;
                }
                URL url = new URL(path);
                HttpURLConnection httpUrlConnection = (HttpURLConnection) url.openConnection();
                httpUrlConnection.setUseCaches(false);
                httpUrlConnection.setDoOutput(true);

                httpUrlConnection.setRequestMethod("POST");
                httpUrlConnection.setRequestProperty("Cache-Control", "no-cache");
                httpUrlConnection.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + this.boundary);

                DataOutputStream request = new DataOutputStream(httpUrlConnection.getOutputStream());

                request.writeBytes(this.twoHyphens + this.boundary + this.crlf);
                request.writeBytes("Content-Disposition: form-data; name=\"firmware\";" + " filename=\"" + file.getName() + "\"" + this.crlf);
                request.writeBytes(this.crlf);

                byte[] bytesArray = new byte[(int) file.length()];

                FileInputStream fis = new FileInputStream(file);
                fis.read(bytesArray); //read file into bytes[]
                fis.close();

                request.write(bytesArray);

                request.writeBytes(this.crlf);
                request.writeBytes(this.twoHyphens + this.boundary + this.twoHyphens + this.crlf);

                request.flush();
                request.close();

                java.util.Scanner s = new java.util.Scanner(httpUrlConnection.getInputStream()).useDelimiter("\\A");
                out = s.hasNext() ? s.next() : "";

            } catch (Exception x) {
                out = "Update failed.";
                Log.e("WTF", "send: ", x);
            }
            return out;
        }
    }

}
