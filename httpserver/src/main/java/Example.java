import com.networknt.handler.HandlerProvider;
import io.undertow.Handlers;
import io.undertow.server.HttpHandler;
import io.undertow.util.Methods;

public class Example implements HandlerProvider {
    public static final String MESSAGE = "Hello ";
    public HttpHandler getHandler() {
        return Handlers.routing()
            .add(Methods.POST, "/post", new PostHandler())
            .add(Methods.GET, "/get", exchange -> {
                String name = exchange.getQueryParameters().get("name").getFirst();
                exchange.getResponseSender().send(MESSAGE + name);
            });
    }
}
