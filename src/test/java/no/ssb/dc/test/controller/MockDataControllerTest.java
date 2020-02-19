package no.ssb.dc.test.controller;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import no.ssb.dc.api.util.JsonParser;
import no.ssb.dc.test.client.ResponseHelper;
import no.ssb.dc.test.client.TestClient;
import no.ssb.dc.test.server.TestServerExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.inject.Inject;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(TestServerExtension.class)
public class MockDataControllerTest {

    private static final Logger LOG = LoggerFactory.getLogger(MockDataControllerTest.class);

    @Inject
    TestClient client;

    @Test
    public void testMockCursorForJson() {
        String cursor = "1";
        int size = 10;
        ResponseHelper<String> eventsResponse = client.get(String.format("/events?position=%s&pageSize=%s", cursor, size)).expect200Ok();
        LOG.trace("{}", eventsResponse.body());
        ArrayNode arrayNode = JsonParser.createJsonParser().fromJson(eventsResponse.body(), ArrayNode.class);
        assertEquals(arrayNode.iterator().next().get("id").asText(), cursor);
        assertEquals(arrayNode.size(), size);
    }

    @Test
    public void testMockCursorAndStopAtForJson() {
        String cursor = "31";
        int size = 10;
        ResponseHelper<String> eventsResponse = client.get(String.format("/events?position=%s&pageSize=%s&stopAt=25", cursor, size)).expect200Ok();
        LOG.trace("{}", eventsResponse.body());
        ArrayNode arrayNode = JsonParser.createJsonParser().fromJson(eventsResponse.body(), ArrayNode.class);
        assertEquals(arrayNode.size(), 0);
    }

    @Test
    public void testMockCursorForXml() {
        String cursor = "1";
        int size = 10;
        ResponseHelper<String> eventsResponse = client.get(String.format("/events?position=%s&pageSize=%s", cursor, size), "Accept", "application/xml").expect200Ok();
        LOG.trace("{}", eventsResponse.body());
        Document doc = AbstractFeedResource.deserializeXml(eventsResponse.body().getBytes());
        Element firstEntry = (Element) doc.getDocumentElement().getElementsByTagName("entry").item(0);
        assertEquals(firstEntry.getElementsByTagName("id").item(0).getTextContent(), cursor);
        assertEquals(doc.getDocumentElement().getElementsByTagName("entry").getLength(), size);
    }

    @Test
    public void testMockCursorAndStopAtForXml() {
        String cursor = "31";
        int size = 10;
        ResponseHelper<String> eventsResponse = client.get(String.format("/events?position=%s&pageSize=%s&stopAt=25", cursor, size), "Accept", "application/xml").expect200Ok();
        LOG.trace("{}", eventsResponse.body());
        Document doc = AbstractFeedResource.deserializeXml(eventsResponse.body().getBytes());
        NodeList entryList = doc.getDocumentElement().getElementsByTagName("entry");
        assertEquals(entryList.getLength(), 0);
    }


    @Test
    public void testMockItemsAsXml() {
        ResponseHelper<String> eventsResponse = client.get("/events/5", "Accept", "application/xml").expect200Ok();
        Document doc = AbstractFeedResource.deserializeXml(eventsResponse.body().getBytes());
        Element firstEntry = (Element) doc.getDocumentElement().getElementsByTagName("id").item(0);
        assertEquals("5", firstEntry.getTextContent());
    }

    @Test
    public void testMockItemsAsJson() {
        ResponseHelper<String> eventsResponse = client.get("/events/5").expect200Ok();
        ObjectNode objectNode = JsonParser.createJsonParser().fromJson(eventsResponse.body(), ObjectNode.class);
        assertEquals("5", objectNode.get("id").asText());
    }

    @Test
    public void testMockItemsAsXmlWith404ErrorResponse() {
        ResponseHelper<String> eventsResponse = client.get("/events/5?404withResponseError", "Accept", "application/xml").expect404NotFound();
        LOG.trace("{}", eventsResponse.body());
    }

    @Test
    public void testMockItemsAsJsonWith404ErrorResponse() {
        ResponseHelper<String> eventsResponse = client.get("/events/5?404withResponseError").expect404NotFound();
        LOG.trace("{}", eventsResponse.body());
    }
}
