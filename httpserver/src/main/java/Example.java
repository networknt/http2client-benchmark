import com.networknt.handler.HandlerProvider;
import io.undertow.Handlers;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Methods;


public class Example implements HandlerProvider {
    private static final String MESSAGE = "Hello World!";
    public HttpHandler getHandler() {
        return Handlers.routing()
            .add(Methods.POST, "/post", exchange ->  exchange.getResponseSender().send(MESSAGE))
            .add(Methods.GET, "/get", exchange -> exchange.getResponseSender().send(MESSAGE));
    }
}
