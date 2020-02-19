package no.ssb.dc.test.controller;

import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import no.ssb.dc.api.http.Request;
import no.ssb.dc.application.spi.Controller;
import no.ssb.dc.test.server.TestServerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.NavigableSet;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

public class MockDataController implements Controller {

    private static final Logger LOG = LoggerFactory.getLogger(MockDataController.class);

    @Override
    public String contextPath() {
        return "/events";
    }

    @Override
    public Set<Request.Method> allowedMethods() {
        return Set.of(Request.Method.GET);
    }

    boolean isListResourceWithContext(HttpServerExchange exchange, String endsWithContext) {
        String requestPath = exchange.getRequestPath();
        NavigableSet<String> pathElements = new TreeSet<>(Arrays.asList(requestPath.split("/")));
        return pathElements.size() - 1 == 1 && requestPath.endsWith(endsWithContext);
    }

    boolean isItemResourceWithContext(HttpServerExchange exchange, String endsWithContext, int pathElementCount) {
        String requestPath = exchange.getRequestPath();
        NavigableSet<String> pathElements = new TreeSet<>(Arrays.asList(requestPath.split("/")));
        return pathElements.size() - 1 == pathElementCount && endsWithContext.substring(1).equalsIgnoreCase(pathElements.pollLast());
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        if (exchange.isInIoThread()) {
            exchange.dispatch(this);
            return;
        }

        if (isListResourceWithContext(exchange, contextPath())) {
            new EventListResource().handle(exchange);
            return;

        } else if (isItemResourceWithContext(exchange, contextPath(), 2)) {
            new EventItemResource().handle(exchange);
            return;
        }

        exchange.setStatusCode(404);
        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
        exchange.getResponseSender().send("Not found: " + exchange.getRequestPath());
    }

    private void randomNap(int maxTimeInMillis) {
        try {
            int randomInt = 50 + new Random().nextInt(maxTimeInMillis - 50 + 1);
            Thread.sleep(randomInt);
        } catch (InterruptedException e) {
            throw new TestServerException(e);
        }
    }

}
