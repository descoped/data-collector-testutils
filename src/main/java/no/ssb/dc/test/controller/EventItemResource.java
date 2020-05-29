package no.ssb.dc.test.controller;

import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringWriter;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

class EventItemResource extends AbstractResource {

    private static final Logger LOG = LoggerFactory.getLogger(EventItemResource.class);

    EventItemResource() {
    }

    @Override
    void handle(HttpServerExchange exchange) {
        int position = getPathParam(exchange.getRequestPath(), 1, 1);

        Optional<String> contentTypeHeader = getContentTypeHeader(exchange);
        String contentType = contentTypeHeader.orElse("application/json");

        boolean generateRandomEventItemData = getQueryParam(exchange.getQueryParameters(), "generateRandomEventItemData", false);
        int generateEventItemRecordSize = getQueryParam(exchange.getQueryParameters(), "generateEventItemRecordSize", 0);
        int generateEventItemRecordKeySize = getQueryParam(exchange.getQueryParameters(), "generateEventItemRecordKeySize", 0);
        int generateEventItemRecordElementSize = getQueryParam(exchange.getQueryParameters(), "generateEventItemRecordElementSize", 0);

        if (checkFailForStatusCodeAndFailAt(exchange, contentType, position)) {
            return;
        }

        if ("application/json".equals(contentType)) {
            if (checkHttpError404WithExplanation(exchange, contentType)) {
                return;
            }
            String payload = renderEventItemAsJson(position, generateRandomEventItemData, generateEventItemRecordSize, generateEventItemRecordKeySize, generateEventItemRecordElementSize);
            exchange.setStatusCode(200);
            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
            exchange.getResponseSender().send(payload);
            return;

        } else if ("application/xml".equals(contentType)) {
            if (checkHttpError404WithExplanation(exchange, contentType)) {
                return;
            }
            String payload = renderEventItemAsXml(position, generateRandomEventItemData, generateEventItemRecordSize, generateEventItemRecordKeySize, generateEventItemRecordElementSize);
            exchange.setStatusCode(200);
            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/xml");
            exchange.getResponseSender().send(payload);
            return;
        }

        exchange.setStatusCode(404);
        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
        exchange.getResponseSender().send("Not found: " + exchange.getRequestPath());
    }

    Map<String, Object> getItemDataModel(int position, boolean generateRandomEventItemData, int generateEventItemRecordSize, int generateEventItemRecordKeySize, int generateEventItemRecordElementSize) {
        Map<String, Object> dataModel = new HashMap<>();
        dataModel.put("item", new EventListItem(position, position + EventItem.EVENT_ID_OFFSET));

        if (generateRandomEventItemData) {
            Map<String, String> recordsMap = new HashMap<>();
            RandomCharacters randomCharacters = new RandomCharacters();
            for (int i = 0; i < generateEventItemRecordSize; i++) {
                recordsMap.put(randomCharacters.generateAlphaNumChars(generateEventItemRecordKeySize), randomCharacters.generateAlphaNumSpaceChars(generateEventItemRecordElementSize));
            }
            dataModel.put("records", recordsMap);
        }

        return dataModel;
    }

    String renderEventItemAsXml(int position, boolean generateRandomEventItemData, int generateEventItemRecordSize, int generateEventItemRecordKeySize, int generateEventItemRecordElementSize) {
        StringWriter output = renderTemplate("event-list-item-xml.ftl", getItemDataModel(position, generateRandomEventItemData, generateEventItemRecordSize, generateEventItemRecordKeySize, generateEventItemRecordElementSize));
        return compactXml(output.toString());
    }

    String renderEventItemAsJson(int position, boolean generateRandomEventItemData, int generateEventItemRecordSize, int generateEventItemRecordKeySize, int generateEventItemRecordElementSize) {
        StringWriter output = renderTemplate("event-list-item-json.ftl", getItemDataModel(position, generateRandomEventItemData, generateEventItemRecordSize, generateEventItemRecordKeySize, generateEventItemRecordElementSize));
        return compactJson(output.toString());
    }

    // SKE returns http error 404 with bespoke payload that contains service error codes
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
            exchange.getResponseSender().send("");
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

    static class RandomCharacters {
        static final char[] ALPHA_NUM_SPACE_CHARS = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ ".toCharArray();
        static final char[] ALPHA_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();

        private String randomString(char[] charTable, int size) {
            SecureRandom random = new SecureRandom();
            byte[] key = new byte[size];
            random.nextBytes(key);
            StringBuilder sb = new StringBuilder(key.length * 2);
            for (byte b : key) {
                sb.append(charTable[(b >> 4) & charTable.length - 1]);
                sb.append(charTable[b & charTable.length - 1]);
            }
            return sb.toString();
        }

        String generateAlphaNumSpaceChars(int size) {
            return randomString(ALPHA_NUM_SPACE_CHARS, size);
        }

        String generateAlphaNumChars(int size) {
            return randomString(ALPHA_CHARS, size);
        }

    }

}
