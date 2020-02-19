package no.ssb.dc.test.controller;

import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;

import java.io.StringWriter;
import java.util.Map;
import java.util.Optional;

class EventListResource extends AbstractFeedResource {

    EventListResource() {
    }

    void handle(HttpServerExchange exchange) {
        int position = getQueryParam(exchange.getQueryParameters(), "position", 1);
        int pageSize = getQueryParam(exchange.getQueryParameters(), "pageSize", 50);
        int stopAt = getQueryParam(exchange.getQueryParameters(), "stopAt", 25);

        Optional<String> contentTypeHeader = getContentTypeHeader(exchange);

        if (contentTypeHeader.isEmpty() || contentTypeHeader.orElseThrow().equals("application/json")) {
            String payload = renderEventListAsJson(position, pageSize, stopAt);
            exchange.setStatusCode(200);
            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
            exchange.getResponseSender().send(payload);
            return;

        } else if (contentTypeHeader.orElseThrow().equals("application/xml")) {
            String linkNextURL = String.format("http://%s:%s%s?position=%s&pageSize=%s%s", exchange.getHostName(), exchange.getHostPort(),
                    exchange.getRequestPath(), position + pageSize, pageSize, stopAt == -1 ? "" : "&stopAt=" + stopAt
            );

            String payload = renderEventListAsXml(position, pageSize, stopAt, linkNextURL);
            exchange.setStatusCode(200);
            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/xml");
            exchange.getResponseSender().send(payload);
            return;
        }

        exchange.setStatusCode(404);
        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
        exchange.getResponseSender().send("Not found: " + exchange.getRequestPath());
    }

    String renderEventListAsXml(int fromPosition, int pageSize, int stopAt, String linkNextURL) {
        Map<String, Object> listDataModel = getListDataModel(fromPosition, pageSize, stopAt);
        listDataModel.put("linkNextURL", linkNextURL);
        StringWriter output = renderTemplate("event-list-xml.ftl", listDataModel);
        return compactXml(output.toString());
    }

    String renderEventListAsJson(int fromPosition, int pageSize, int stopAt) {
        StringWriter output = renderTemplate("event-list-json.ftl", getListDataModel(fromPosition, pageSize, stopAt));
        String json = output.toString();
        return compactJson(json);
    }

}
