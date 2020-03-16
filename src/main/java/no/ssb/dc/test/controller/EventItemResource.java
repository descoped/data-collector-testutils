package no.ssb.dc.test.controller;

import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

class EventItemResource extends AbstractResource {

    EventItemResource() {
    }

    @Override
    void handle(HttpServerExchange exchange) {
        int position = getPathParam(exchange.getRequestPath(), 1, 1);

        Optional<String> contentTypeHeader = getContentTypeHeader(exchange);
        String contentType = contentTypeHeader.orElse("application/json");

        if (checkFailForStatusCodeAndFailAt(exchange, contentType, position)) {
            return;
        }

        if ("application/json".equals(contentType)) {
            if (checkHttpError404WithExplanation(exchange, contentType)) {
                return;
            }
            String payload = renderEventItemAsJson(position);
            exchange.setStatusCode(200);
            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
            exchange.getResponseSender().send(payload);
            return;

        } else if ("application/xml".equals(contentType)) {
            if (checkHttpError404WithExplanation(exchange, contentType)) {
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

    Map<String, Object> getItemDataModel(int position) {
        Map<String, Object> dataModel = new HashMap<>();
        dataModel.put("item", new EventListItem(position, String.valueOf(position + 1000)));
        return dataModel;
    }

    String renderEventItemAsXml(int position) {
        StringWriter output = renderTemplate("event-list-item-xml.ftl", getItemDataModel(position));
        return compactXml(output.toString());
    }

    String renderEventItemAsJson(int position) {
        StringWriter output = renderTemplate("event-list-item-json.ftl", getItemDataModel(position));
        return compactJson(output.toString());
    }

    boolean checkHttpError404WithExplanation(HttpServerExchange exchange, String contentType) {
        if (exchange.getQueryParameters().containsKey("404withResponseError")) {
            String payload = null;
            if ("application/json".equals(contentType)) {
                payload = renderHttpError404WithExplanationAsJson();
            } else if ("application/xml".equals(contentType)) {
                payload = renderHttpError404WithExplanationAsXml();
            }
            exchange.setStatusCode(404);
            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, contentType);
            exchange.getResponseSender().send(payload);
            return true;
        }
        return false;
    }

    boolean checkFailForStatusCodeAndFailAt(HttpServerExchange exchange, String contentType, int position) {
        int failWithStatusCode = getQueryParam(exchange.getQueryParameters(), "failWithStatusCode", -1);
        int failAt = getQueryParam(exchange.getQueryParameters(), "failAt", -1);

        if (failWithStatusCode > -1 && failAt <= position) {
            exchange.setStatusCode(failWithStatusCode);
            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
            return true;
        }

        return false;
    }

    String renderHttpError404WithExplanationAsXml() {
        StringWriter output = renderTemplate("event-item-404-error-response-xml.ftl", new HashMap<>());
        return compactXml(output.toString());
    }

    String renderHttpError404WithExplanationAsJson() {
        StringWriter output = renderTemplate("event-item-404-error-response-json.ftl", new HashMap<>());
        return compactJson(output.toString());
    }
}
