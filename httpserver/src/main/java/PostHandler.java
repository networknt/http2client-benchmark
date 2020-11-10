import com.networknt.utility.StringUtils;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class PostHandler implements HttpHandler {

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        if (exchange.isInIoThread()) {
            exchange.dispatch(this);
            return;
        }
        exchange.startBlocking();
        InputStream inputStream = exchange.getInputStream();
        String name = StringUtils.inputStreamToString(inputStream, StandardCharsets.UTF_8);
        exchange.getResponseSender().send(Example.MESSAGE + name);
    }

}
