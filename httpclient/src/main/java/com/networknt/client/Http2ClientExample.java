package com.networknt.client;

import com.networknt.client.ssl.ClientX509ExtendedTrustManager;
import com.networknt.client.ssl.TLSConfig;
import com.networknt.utility.TlsUtil;
import io.undertow.UndertowOptions;
import io.undertow.client.ClientConnection;
import io.undertow.client.ClientRequest;
import io.undertow.client.ClientResponse;
import io.undertow.util.Headers;
import io.undertow.util.Methods;
import okhttp3.*;
import org.owasp.encoder.Encode;
import org.xnio.IoUtils;
import org.xnio.OptionMap;

import javax.net.ssl.*;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static com.networknt.client.Http2Client.*;

/**
 * Created by stevehu on 2017-03-19.
 */
public class Http2ClientExample {
    static final MediaType TEXT
            = MediaType.get("text/plain; charset=utf-8");

    public static void main(String[] args) throws Exception {
        String cl = args[0];
        if(cl == null) System.out.println("java -jar httpclient-1.0-SNAPSHOT.jar jdk11 | okhttp | light");

        Http2ClientExample e = new Http2ClientExample();
        for (int i = 0; i <= 2; i++) {
            if(cl.equals("jdk11")) {
                long startTime = System.nanoTime();
                e.testMultipleJdk11ClientGet(10000);
                long duration = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
                if(i > 1) System.out.println("duration jdk11 client with 10K get requests = " + duration + " milliseconds");

                startTime = System.nanoTime();
                e.testMultipleJdk11ClientPost(10000);
                duration = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
                if(i > 1) System.out.println("duration jdk11 client with 10K post requests = " + duration + "milliseconds");
            } else if (cl.equals("okhttp")) {
                long startTime = System.nanoTime();
                e.testMultipleOkhttpClientGet(10000);
                long duration = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
                if(i > 1) System.out.println("duration okhttp client with 10K get requests = " + duration + " milliseconds");

                startTime = System.nanoTime();
                e.testMultipleOkhttpClientPost(10000);
                duration = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
                if(i > 1) System.out.println("duration okhttp client with 10K post requests = " + duration + " milliseconds");
            } else if (cl.equals("light")) {
                long startTime = System.nanoTime();
                e.testMultipleLightClientGet(10000);
                long duration = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
                if(i > 1) System.out.println("duration light client with 10K get requests = " + duration + " milliseconds");

                startTime = System.nanoTime();
                e.testMultipleLightClientPost(10000);
                duration = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
                if(i > 1) System.out.println("duration light client with 10K post requests = " + duration + " milliseconds");
            } else {
                System.out.println("invalid client selected " + cl);
            }
        }
        System.exit(0);
    }

    public void testMultipleLightClientGet(int round) throws Exception {
        final Http2Client client = Http2Client.getInstance();
        final List<AtomicReference<ClientResponse>> references = new CopyOnWriteArrayList<>();
        final CountDownLatch latch = new CountDownLatch(round);
        final ClientConnection connection = client.connect(new URI("https://localhost:8443"), Http2Client.WORKER, Http2Client.SSL, Http2Client.BUFFER_POOL, OptionMap.create(UndertowOptions.ENABLE_HTTP2, true)).get();
        try {
            connection.getIoThread().execute(new Runnable() {
                @Override
                public void run() {
                    for (int i = 0; i < round; i++) {
                        AtomicReference<ClientResponse> reference = new AtomicReference<>();
                        references.add(i, reference);
                        final ClientRequest request = new ClientRequest().setMethod(Methods.GET).setPath("/get?name=Steve");
                        request.getRequestHeaders().put(Headers.HOST, "localhost");
                        connection.sendRequest(request, client.createClientCallback(reference, latch));
                    }
                }
            });
            latch.await(10, TimeUnit.SECONDS);
            for (final AtomicReference<ClientResponse> reference : references) {
                String body = reference.get().getAttachment(Http2Client.RESPONSE_BODY);
            }
        } finally {
            IoUtils.safeClose(connection);
        }
    }

    public void testMultipleLightClientPost(int round) throws Exception {
        final Http2Client client = Http2Client.getInstance();
        final List<AtomicReference<ClientResponse>> references = new CopyOnWriteArrayList<>();
        final CountDownLatch latch = new CountDownLatch(round);
        final ClientConnection connection = client.connect(new URI("https://localhost:8443"), Http2Client.WORKER, Http2Client.SSL, Http2Client.BUFFER_POOL, OptionMap.create(UndertowOptions.ENABLE_HTTP2, true)).get();
        try {
            connection.getIoThread().execute(new Runnable() {
                @Override
                public void run() {
                    for (int i = 0; i < round; i++) {
                        AtomicReference<ClientResponse> reference = new AtomicReference<>();
                        references.add(i, reference);
                        final ClientRequest request = new ClientRequest().setMethod(Methods.POST).setPath("/post");
                        request.getRequestHeaders().put(Headers.TRANSFER_ENCODING, "chunked");
                        request.getRequestHeaders().put(Headers.HOST, "localhost");
                        connection.sendRequest(request, client.createClientCallback(reference, latch, "Steve"));
                    }
                }
            });
            latch.await(10, TimeUnit.SECONDS);
            for (final AtomicReference<ClientResponse> reference : references) {
                String body = reference.get().getAttachment(Http2Client.RESPONSE_BODY);
            }
        } finally {
            IoUtils.safeClose(connection);
        }
    }

    public void testMultipleJdk11ClientGet(int round) throws Exception {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .sslContext(Http2Client.createSSLContext())
                .build();
        for(int i = 0; i < round; i++) {
            HttpRequest request = HttpRequest.newBuilder()
                    .GET()
                    .uri(URI.create("https://localhost:8443/get?name=Steve"))
                    .build();
            CompletableFuture<HttpResponse<String>> response = client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
            String body = response.thenApply(HttpResponse::body).get();
            //System.out.println(body);
        }
    }

    public void testMultipleJdk11ClientPost(int round) throws Exception {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .sslContext(Http2Client.createSSLContext())
                .build();
        for(int i = 0; i < round; i++) {

            HttpRequest request = HttpRequest.newBuilder()
                    .POST(HttpRequest.BodyPublishers.ofString("Steve"))
                    .uri(URI.create("https://localhost:8443/post"))
                    .build();
            CompletableFuture<HttpResponse<String>> response = client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
            String body = response.thenApply(HttpResponse::body).get();
        }
    }

    String run(OkHttpClient client, String url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .build();

        try (Response response = client.newCall(request).execute()) {
            return response.body().string();
        }
    }

    public OkHttpClient getOkHttpClient() {
        SSLSocketFactory sslSocketFactory;
        X509TrustManager trustManager;
        try {
            trustManager = (X509TrustManager)getTrustManager();
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[] { trustManager }, null);
            sslSocketFactory = sslContext.getSocketFactory();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return new OkHttpClient.Builder()
                .sslSocketFactory(sslSocketFactory, trustManager)
                .hostnameVerifier(new HostnameVerifier() {
                    @Override
                    public boolean verify(String hostname, SSLSession session) {
                        return true;
                    }
                })
                .build();
    }

    public void testMultipleOkhttpClientGet(int round) throws Exception {
        OkHttpClient client = getOkHttpClient();
        for(int i = 0; i < round; i++) {
            String body = run(client, "https://localhost:8443/get?name=Steve");
        }
    }

    public void testMultipleOkhttpClientPost(int round) throws Exception {
        OkHttpClient client = getOkHttpClient();
        for(int i = 0; i < round; i++) {
            RequestBody body = RequestBody.create("Steve", TEXT);
            Request request = new Request.Builder()
                    .url("https://localhost:8443/post")
                    .post(body)
                    .build();
            try (Response response = client.newCall(request).execute()) {
                String res = response.body().string();
            }
        }
    }

    private TrustManager getTrustManager() {
        TrustManager[] trustManagers = null;
        try {
            Map<String, Object> tlsMap = (Map<String, Object>)ClientConfig.get().getMappedConfig().get(TLS);
            Boolean loadTrustStore = (Boolean) tlsMap.get(LOAD_TRUST_STORE);
            if (loadTrustStore != null && loadTrustStore) {
                String trustStoreName = System.getProperty(TRUST_STORE_PROPERTY);
                String trustStorePass = System.getProperty(TRUST_STORE_PASSWORD_PROPERTY);
                if (trustStoreName != null && trustStorePass != null) {
                } else {
                    trustStoreName = (String) tlsMap.get(TRUST_STORE);
                    trustStorePass = (String) tlsMap.get(TRUST_STORE_PASS);
                }
                if (trustStoreName != null && trustStorePass != null) {
                    KeyStore trustStore = TlsUtil.loadTrustStore(trustStoreName, trustStorePass.toCharArray());
                    TLSConfig tlsConfig = TLSConfig.create(tlsMap, (String)tlsMap.get(TLSConfig.DEFAULT_GROUP_KEY));

                    TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
                    trustManagerFactory.init(trustStore);
                    trustManagers = ClientX509ExtendedTrustManager.decorate(trustManagerFactory.getTrustManagers(), tlsConfig);
                }
            }
        } catch (NoSuchAlgorithmException | KeyStoreException e) {
            e.printStackTrace();
        }
        return trustManagers[0];
    }
}
