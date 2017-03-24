package com.networknt.undertow;

import io.undertow.client.*;
import io.undertow.util.AttachmentKey;
import io.undertow.util.Headers;
import io.undertow.util.Methods;
import io.undertow.util.StringReadChannelListener;
import org.xnio.ChannelListeners;
import org.xnio.IoUtils;
import org.xnio.OptionMap;
import org.xnio.Options;
import org.xnio.Xnio;
import org.xnio.channels.StreamSinkChannel;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;

/**
 * Created by stevehu on 2017-03-19.
 */
public class Http2ClientExample {
    private static final AttachmentKey<String> RESPONSE_BODY = AttachmentKey.create(String.class);
    private static final char[] STORE_PASSWORD = "password".toCharArray();


    public static void main(String[] args) throws Exception {

        final UndertowHttp2Client client = UndertowHttp2Client.getInstance();

        final CountDownLatch latch = new CountDownLatch(1);

        final ClientConnection connection = client.connect(new URI("https://localhost:8443"), Xnio.getInstance().createWorker(null, DEFAULT_OPTIONS), DefaultServer.getBufferPool(), OptionMap.EMPTY).get();
        try {
            ClientRequest request = new ClientRequest().setPath("/1324").setMethod(Methods.GET);
            request.getRequestHeaders().put(Headers.HOST, DefaultServer.getHostAddress());
            final List<ClientResponse> responses = new CopyOnWriteArrayList<>();
            request.getRequestHeaders().add(Headers.CONNECTION, Headers.CLOSE.toString());
            connection.sendRequest(request, createClientCallback(responses, latch));
            latch.await();
            final ClientResponse response = responses.iterator().next();
            System.out.println(response.getProtocol());
            System.out.println(response.getAttachment(RESPONSE_BODY));
        } finally {
            IoUtils.safeClose(connection);
        }

    }

    private ClientCallback<ClientExchange> createClientCallback(final List<ClientResponse> responses, final CountDownLatch latch) {
        return new ClientCallback<ClientExchange>() {
            @Override
            public void completed(ClientExchange result) {
                result.setResponseListener(new ClientCallback<ClientExchange>() {
                    @Override
                    public void completed(final ClientExchange result) {
                        responses.add(result.getResponse());
                        new StringReadChannelListener(result.getConnection().getBufferPool()) {

                            @Override
                            protected void stringDone(String string) {
                                result.getResponse().putAttachment(RESPONSE_BODY, string);
                                latch.countDown();
                            }

                            @Override
                            protected void error(IOException e) {
                                e.printStackTrace();

                                latch.countDown();
                            }
                        }.setup(result.getResponseChannel());
                    }

                    @Override
                    public void failed(IOException e) {
                        e.printStackTrace();

                        latch.countDown();
                    }
                });
                try {
                    result.getRequestChannel().shutdownWrites();
                    if(!result.getRequestChannel().flush()) {
                        result.getRequestChannel().getWriteSetter().set(ChannelListeners.<StreamSinkChannel>flushingChannelListener(null, null));
                        result.getRequestChannel().resumeWrites();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    latch.countDown();
                }
            }

            @Override
            public void failed(IOException e) {
                e.printStackTrace();
                latch.countDown();
            }
        };
    }

    private static KeyStore loadKeyStore(String name) throws Exception {
        String storeLoc = System.getProperty(name);
        final InputStream stream;
        if(storeLoc == null) {
            stream = Http2ClientExample.class.getClassLoader().getResourceAsStream(name);
        } else {
            stream = Files.newInputStream(Paths.get(storeLoc));
        }

        if(stream == null) {
            throw new RuntimeException("Could not load keystore");
        }
        try(InputStream is = stream) {
            KeyStore loadedKeystore = KeyStore.getInstance("JKS");
            loadedKeystore.load(is, password(name));
            return loadedKeystore;
        }
    }

    static char[] password(String name) {
        String pw = System.getProperty(name + ".password");
        return pw != null ? pw.toCharArray() : STORE_PASSWORD;
    }

    private static SSLContext createSSLContext(final KeyStore keyStore, final KeyStore trustStore) throws Exception {
        KeyManager[] keyManagers;
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, password("key"));
        keyManagers = keyManagerFactory.getKeyManagers();

        TrustManager[] trustManagers;
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(trustStore);
        trustManagers = trustManagerFactory.getTrustManagers();

        SSLContext sslContext;
        sslContext = SSLContext.getInstance("TLS");
        sslContext.init(keyManagers, trustManagers, null);

        return sslContext;
    }

}