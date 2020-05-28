package no.ssb.dc.test.controller;

import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

class EventListResource extends AbstractResource {

    private static final Logger LOG = LoggerFactory.getLogger(EventListResource.class);

    EventListResource() {
    }

    @Override
    void handle(HttpServerExchange exchange) {
        int position = getQueryParam(exchange.getQueryParameters(), "position", 1);
        int pageSize = getQueryParam(exchange.getQueryParameters(), "pageSize", 50);
        int stopAt = getQueryParam(exchange.getQueryParameters(), "stopAt", 25);

        Optional<String> contentTypeHeader = getContentTypeHeader(exchange);
        String contentType = contentTypeHeader.orElse("application/json");

        if ("application/json".equals(contentType)) {
            String payload = renderEventListAsJson(position, pageSize, stopAt);
            exchange.setStatusCode(200);
            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
            exchange.getResponseSender().send(payload);
            return;

        } else if ("application/xml".equals(contentType)) {
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

    Map<String, Object> getListDataModel(int fromPosition, int pageSize, int stopAt) {
        Map<String, Object> dataModel = new HashMap<>();
        List<EventListItem> list = new ArrayList<>();
        if (stopAt == -1 || fromPosition < stopAt) {
            for (int n = fromPosition; n < fromPosition + pageSize; n++) {
                list.add(new EventListItem(n, n + EventItem.EVENT_ID_OFFSET));
            }
        }
        dataModel.put("list", list);
        return dataModel;
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
