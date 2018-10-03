package com.networknt.client;

import io.undertow.UndertowOptions;
import io.undertow.client.ClientConnection;
import io.undertow.client.ClientRequest;
import io.undertow.client.ClientResponse;
import io.undertow.util.Headers;
import io.undertow.util.Methods;
import org.xnio.IoUtils;
import org.xnio.OptionMap;

import java.net.URI;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by stevehu on 2017-03-19.
 */
public class Http2ClientExample {
    public static void main(String[] args) throws Exception {
        long startTime = System.nanoTime();
        Http2ClientExample e = new Http2ClientExample();
        e.testMultipleHttp2Get(10000);
        e.testMultipleHttp2Post(10000);
        long duration = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
        System.out.println("duration = " + duration);
        System.exit(0);
    }

    public void testMultipleHttp2Get(int round) throws Exception {
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
                        final ClientRequest request = new ClientRequest().setMethod(Methods.GET).setPath("/get");
                        request.getRequestHeaders().put(Headers.HOST, "localhost");
                        connection.sendRequest(request, client.createClientCallback(reference, latch));
                    }
                }
            });
            latch.await(10, TimeUnit.SECONDS);
            /*
            for (final AtomicReference<ClientResponse> reference : references) {
                System.out.println(reference.get().getAttachment(Http2Client.RESPONSE_BODY));
                System.out.println(reference.get().getProtocol().toString());
            }
            */
        } finally {
            IoUtils.safeClose(connection);
        }
    }

    public void testMultipleHttp2Post(int round) throws Exception {
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
                        connection.sendRequest(request, client.createClientCallback(reference, latch, "post"));
                    }
                }
            });
            latch.await(10, TimeUnit.SECONDS);
            /*
            for (final AtomicReference<ClientResponse> reference : references) {
                System.out.println(reference.get().getAttachment(Http2Client.RESPONSE_BODY));
                System.out.println(reference.get().getProtocol().toString());
            }
            */
        } finally {
            IoUtils.safeClose(connection);
        }
    }
}
