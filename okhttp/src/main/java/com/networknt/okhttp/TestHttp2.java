package com.networknt.okhttp;

import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by stevehu on 2017-03-19.
 */
public class TestHttp2 {


    public static void main(String[] args) throws Exception {
        String url = "https://www.cloudflare.com/";

        List<Protocol> protocols = new ArrayList<>();
        protocols.add(Protocol.HTTP_2);
        protocols.add(Protocol.HTTP_1_1);

        OkHttpClient client = new OkHttpClient.Builder()
                //.protocols(protocols)
                .build();
        Request request = new Request.Builder()
                .url(url)
                .build();

        try {
            Response response = client.newCall(request).execute();
            System.out.println("Protocol: "+response.protocol().toString());
        } catch (IOException ex) {
            Logger.getLogger("Test").log(Level.SEVERE, null, ex);
        }
    }
}
