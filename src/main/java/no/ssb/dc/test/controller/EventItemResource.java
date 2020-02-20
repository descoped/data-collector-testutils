package no.ssb.dc.test.controller;

import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.Optional;

class EventItemResource extends AbstractResource {

    EventItemResource() {
    }

    @Override
    void handle(HttpServerExchange exchange) {
        int position = getPathParam(exchange.getRequestPath(), 1, 1);

        Optional<String> contentTypeHeader = getContentTypeHeader(exchange);

        if (contentTypeHeader.isEmpty() || contentTypeHeader.orElseThrow().equals("application/json")) {
            if (checkHttpError404WithExplanation(exchange)) {
                return;
            }
            String payload = renderEventItemAsJson(position);
            exchange.setStatusCode(200);
            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
            exchange.getResponseSender().send(payload);
            return;

        } else if (contentTypeHeader.orElseThrow().equals("application/xml")) {
            if (checkHttpError404WithExplanation(exchange)) {
                return;
            }
            String payload = renderEventItemAsXml(position);
            exchange.setStatusCode(200);
            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/xml");
            exchange.getResponseSender().send(payload);
            return;
        }

        exchange.setStatusCode(404);
        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
        exchange.getResponseSender().send("Not found: " + exchange.getRequestPath());
    }

    String renderEventItemAsXml(int position) {
        StringWriter output = renderTemplate("event-list-item-xml.ftl", getItemDataModel(position));
        return compactXml(output.toString());
    }

    String renderEventItemAsJson(int position) {
        StringWriter output = renderTemplate("event-list-item-json.ftl", getItemDataModel(position));
        return compactJson(output.toString());
    }

    boolean checkHttpError404WithExplanation(HttpServerExchange exchange) {
        if (exchange.getQueryParameters().containsKey("404withResponseError")) {
            String payload = renderHttpError404WithExplaination();
            exchange.setStatusCode(404);
            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
            exchange.getResponseSender().send(payload);
            return true;
        }
        return false;
    }

    String renderHttpError404WithExplaination() {
        StringWriter output = renderTemplate("event-item-404-error-response-json.ftl", new HashMap<>());
        return compactJson(output.toString());
    }
}
