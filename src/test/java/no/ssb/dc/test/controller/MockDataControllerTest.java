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
class MockDataControllerTest {

    private static final Logger LOG = LoggerFactory.getLogger(MockDataControllerTest.class);

    @Inject
    TestClient client;

    @Test
    void testMockCursorForJson() {
        String cursor = "1";
        int size = 10;
        ResponseHelper<String> eventsResponse = client.get(String.format("/api/events?position=%s&pageSize=%s", cursor, size)).expect200Ok();
        LOG.trace("{}", eventsResponse.body());
        ArrayNode arrayNode = JsonParser.createJsonParser().fromJson(eventsResponse.body(), ArrayNode.class);
        assertEquals(arrayNode.iterator().next().get("id").asText(), cursor);
        assertEquals(arrayNode.size(), size);
    }

    @Test
    void testMockCursorAndStopAtForJson() {
        String cursor = "31";
        int size = 10;
        ResponseHelper<String> eventsResponse = client.get(String.format("/api/events?position=%s&pageSize=%s&stopAt=25", cursor, size)).expect200Ok();
        LOG.trace("{}", eventsResponse.body());
        ArrayNode arrayNode = JsonParser.createJsonParser().fromJson(eventsResponse.body(), ArrayNode.class);
        assertEquals(arrayNode.size(), 0);
    }

    @Test
    void testMockCursorForXml() {
        String cursor = "1";
        int size = 10;
        ResponseHelper<String> eventsResponse = client.get(String.format("/api/events?position=%s&pageSize=%s", cursor, size), "Accept", "application/xml").expect200Ok();
        LOG.trace("{}", eventsResponse.body());
        Document doc = AbstractResource.deserializeXml(eventsResponse.body().getBytes());
        Element firstEntry = (Element) doc.getDocumentElement().getElementsByTagName("entry").item(0);
        assertEquals(firstEntry.getElementsByTagName("id").item(0).getTextContent(), cursor);
        assertEquals(doc.getDocumentElement().getElementsByTagName("entry").getLength(), size);
    }

    @Test
    void testMockCursorAtHighPositionForXml() {
        String cursor = "2500";
        int size = 10;
        ResponseHelper<String> eventsResponse = client.get(String.format("/api/events?position=%s&pageSize=%s&stopAt=-1", cursor, size), "Accept", "application/xml").expect200Ok();
        LOG.trace("{}", eventsResponse.body());
        Document doc = AbstractResource.deserializeXml(eventsResponse.body().getBytes());
        Element firstEntry = (Element) doc.getDocumentElement().getElementsByTagName("entry").item(0);
        assertEquals(firstEntry.getElementsByTagName("id").item(0).getTextContent(), cursor);
        assertEquals(doc.getDocumentElement().getElementsByTagName("entry").getLength(), size);
    }

    @Test
    void testMockCursorAndStopAtForXml() {
        String cursor = "31";
        int size = 10;
        ResponseHelper<String> eventsResponse = client.get(String.format("/api/events?position=%s&pageSize=%s&stopAt=25", cursor, size), "Accept", "application/xml").expect200Ok();
        LOG.trace("{}", eventsResponse.body());
        Document doc = AbstractResource.deserializeXml(eventsResponse.body().getBytes());
        NodeList entryList = doc.getDocumentElement().getElementsByTagName("entry");
        assertEquals(entryList.getLength(), 0);
    }

    @Test
    void testMockItems() {
        ResponseHelper<String> eventsResponse = client.get("/api/events/5").expect200Ok();
        ObjectNode objectNode = JsonParser.createJsonParser().fromJson(eventsResponse.body(), ObjectNode.class);
        assertEquals("5", objectNode.get("id").asText());
    }

    @Test
    void testMockItemsFailForStatusCodeAndFailAt() {
        ResponseHelper<String> eventsResponse = client.get("/api/events/5?failWithStatusCode=404&failAt=5").expect404NotFound();
        LOG.trace("{}", eventsResponse.body());
    }

    @Test
    void testMockItemsWith404ErrorResponseAsJson() {
        ResponseHelper<String> eventsResponse = client.get("/api/events/5?404withResponseError").expect404NotFound();
        LOG.trace("{}", eventsResponse.body());
    }

    @Test
    void testMockItemsWith404ErrorResponseAsXml() {
        ResponseHelper<String> eventsResponse = client.get("/api/events/5?404withResponseError", "Accept", "application/xml").expect404NotFound();
        LOG.trace("{}", eventsResponse.body());
    }
}
