package no.ssb.dc.test.controller;

import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;


/**
 * The mock feed simulates an atom feed and only support forward direction of data.
 */
class AtomFeedResource extends AbstractResource {

    AtomFeedResource() {
    }

    Map<String, Object> getListDataModel(String fromMarker, int pageSize, String stopAt) {
        Map<String, Object> dataModel = new HashMap<>();
        List<EventListItem> entries = new ArrayList<>();

        if (Integer.parseInt(stopAt) == -1 || Integer.parseInt(fromMarker) < Integer.parseInt(stopAt)) {
            for (int marker = Integer.parseInt(fromMarker); marker < Integer.parseInt(fromMarker) + pageSize; marker++) {
                EventListItem entry = new EventListItem(marker, marker);
                entries.add(entry);
            }
        }

        dataModel.put("entries", entries);
        return dataModel;
    }

    @Override
    void handle(HttpServerExchange exchange) {
        String fromMarker = getQueryParam(exchange.getQueryParameters(), "marker", "1000", "first");
        int pageSize = getQueryParam(exchange.getQueryParameters(), "pageSize", 50);
        String stopAt = getQueryParam(exchange.getQueryParameters(), "stopAt", "1025");

        Optional<String> contentTypeHeader = getContentTypeHeader(exchange);
        String contentType = contentTypeHeader.orElse("application/xml");

        if ("application/xml".equals(contentType)) {
            String payload = renderAtomFeedAsXml(fromMarker, pageSize, stopAt);
            exchange.setStatusCode(200);
            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/xml");
            exchange.getResponseSender().send(payload);
            return;

        } else if ("application/json".equals(contentType)) {
            throw new UnsupportedOperationException();
        }

        exchange.setStatusCode(404);
        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
        exchange.getResponseSender().send("Not found: " + exchange.getRequestPath());
    }

    String renderAtomFeedAsXml(String fromMarker, int pageSize, String stopAt) {
        Map<String, Object> listDataModel = getListDataModel(fromMarker, pageSize, stopAt);

        String linkPreviousURL = ""; // TODO fix prev
        String stopAtMarker = Integer.parseInt(stopAt) == -1 ? "" : "&stopAt=" + stopAt;
        String linkSelfURL = String.format("http://%s:%s/api%s?position=%s&pageSize=%s%s", "0.0.0.0", "9999", "/feed", fromMarker, pageSize, stopAtMarker);
        String linkNextURL = String.format("http://%s:%s/api%s?position=%s&pageSize=%s%s", "0.0.0.0", "9999", "/feed", Integer.parseInt(fromMarker) + pageSize, pageSize, stopAtMarker);
        listDataModel.put("linkPreviousURL", linkPreviousURL);
        listDataModel.put("linkSelfURL", linkSelfURL);
        listDataModel.put("linkNextURL", linkNextURL);

        listDataModel.put("fromMarker", fromMarker);

        StringWriter output = renderTemplate("atom-feed-xml.ftl", listDataModel);
        return compactXml(output.toString());
    }
}
