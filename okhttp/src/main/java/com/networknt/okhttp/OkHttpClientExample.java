package com.networknt.okhttp;


import okhttp3.*;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.eclipse.jetty.util.ssl.SslContextFactory.TRUST_ALL_CERTS;


public class OkHttpClientExample {

    // In order to run this, you need the alpn-boot-XXX.jar in the bootstrap classpath.
    public static void main(String[] args) throws Exception {
        OkHttpClient client = getUnsafeOkHttpClient();
        Request request = new Request.Builder()
                .url("https://localhost:8443") // The Http2Server should be running here.
                .build();
        long startTime = System.nanoTime();
        for (int i=0; i<3; i++) {
            client.newCall(request).enqueue(new Callback() {
                public void onFailure(Call call, IOException e) {
                    e.printStackTrace();
                }
                public void onResponse(Call call, Response response) throws IOException {
                    long duration = TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - startTime);
                    System.out.println("Protocol: "+response.protocol().toString());
                    System.out.println("After " + duration + " seconds: " + response.body().string());
                }
            });
        }
    }

    // http://stackoverflow.com/questions/25509296/trusting-all-certificates-with-okhttp
    private static OkHttpClient getUnsafeOkHttpClient() {
        try {
            // Install the all-trusting trust manager
            final SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, TRUST_ALL_CERTS, new java.security.SecureRandom());
            // Create an ssl socket factory with our all-trusting manager
            final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
            OkHttpClient okHttpClient = new OkHttpClient();
            okHttpClient.newBuilder().sslSocketFactory(sslSocketFactory).hostnameVerifier((hostname, session) -> true);
            return okHttpClient;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}

