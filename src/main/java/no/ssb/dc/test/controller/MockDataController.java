package no.ssb.dc.test.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import no.ssb.dc.api.util.JsonParser;
import no.ssb.dc.application.Controller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.NavigableSet;
import java.util.Random;
import java.util.TreeSet;
import java.util.UUID;

public class MockDataController implements Controller {

    private static final Logger LOG = LoggerFactory.getLogger(MockDataController.class);

    @Override
    public String contextPath() {
        return "/mock";
    }

    String getResource(String filename) throws IOException {
        try (InputStream in = ClassLoader.getSystemResourceAsStream("testdata/" + filename)) {
            if (in == null) {
                throw new RuntimeException("Unable to locate resource: " + "testdata/" + filename);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        if (exchange.isInIoThread()) {
            exchange.dispatch(this);
            return;
        }

        String requestPath = exchange.getRequestPath();

        if (requestPath.endsWith("/mock")) {
            int cursor = Integer.parseInt(exchange.getQueryParameters().computeIfAbsent("seq", key -> new LinkedList<>(Collections.singletonList("1"))).getFirst());
            int size = Integer.parseInt(exchange.getQueryParameters().computeIfAbsent("size", key -> new LinkedList<>(Collections.singletonList("500"))).getFirst());

            if (cursor > 25) {
                exchange.setStatusCode(200);
                if ("application/xml".equalsIgnoreCase(exchange.getRequestHeaders().getFirst("Accept"))) {
                    exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/xml");
                    exchange.getResponseSender().send("<feed/>");
                    return;
                } else {
                    exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
                    exchange.getResponseSender().send("[]");
                    return;
                }
            }

            exchange.setStatusCode(200);

            ArrayNode arrayNode = JsonParser.createJsonParser().createArrayNode();

            for (int n = cursor; n < cursor + size; n++) {
                ObjectNode entry = JsonParser.createJsonParser().createObjectNode().put("id", String.valueOf(n));
                ObjectNode eventNode = JsonParser.createJsonParser().createObjectNode().put("event-id", UUID.randomUUID().toString());
                entry.set("event", eventNode);
                arrayNode.add(entry);
            }

            String payload;

            if (exchange.getRequestHeaders().contains("Accept")) {
                if ("application/xml".equalsIgnoreCase(exchange.getRequestHeaders().getFirst("Accept"))) {
                    exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/xml");
                    XmlMapper mapper = new XmlMapper();
                    mapper.setDefaultUseWrapper(false);
                    mapper.configure(ToXmlGenerator.Feature.WRITE_XML_DECLARATION, true);
                    JsonNode feedNode = mapper.createObjectNode().set("entry", arrayNode);
                    payload = mapper.writeValueAsString(feedNode).replace("ObjectNode", "feed");
                    payload = payload.replace("<feed>", String.format("<feed><link rel=\"next\" href=\"%s\"/>", exchange.getRequestURL() + "?seq=" + (cursor + size) + "&amp;limit=" + size));
                } else {
                    exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
                    payload = JsonParser.createJsonParser().toJSON(arrayNode);
                }
            } else {
                exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
                payload = JsonParser.createJsonParser().toJSON(arrayNode);
            }


            exchange.startBlocking();
            exchange.getOutputStream().write(payload.getBytes());
            exchange.getOutputStream().flush();

            exchange.getResponseSender().close();
            return;
        } /* end mock list */


        NavigableSet<String> pathElements = new TreeSet<>();
        pathElements.addAll(Arrays.asList(requestPath.split("/")));

        pathElements.pollFirst();
        String id = pathElements.pollFirst();
        String mock = pathElements.pollFirst();

        if (pathElements.size() == 0 && mock.equals("mock")) {
            exchange.setStatusCode(200);
            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");

            ObjectNode objectNode = JsonParser.createJsonParser().createObjectNode();
            objectNode.put("id", id);
            objectNode.put("type", exchange.getQueryString());

            String payload;

            if (exchange.getRequestHeaders().contains("Accept")) {
                if ("application/xml".equalsIgnoreCase(exchange.getRequestHeaders().getFirst("Accept"))) {
                    exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/xml");
                    XmlMapper mapper = new XmlMapper();
                    mapper.setDefaultUseWrapper(false);
                    mapper.configure(ToXmlGenerator.Feature.WRITE_XML_DECLARATION, true);
                    payload = mapper.writeValueAsString(objectNode).replace("ObjectNode", "entry");
                } else {
                    exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
                    payload = JsonParser.createJsonParser().toJSON(objectNode);
                }
            } else {
                exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
                payload = JsonParser.createJsonParser().toJSON(objectNode);
            }

            randomNap(150);

            exchange.getResponseSender().send(payload);
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
            throw new RuntimeException(e);
        }
    }

}