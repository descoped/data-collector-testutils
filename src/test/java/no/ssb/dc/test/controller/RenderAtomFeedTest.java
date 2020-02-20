package no.ssb.dc.test.controller;

import io.undertow.server.HttpServerExchange;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RenderAtomFeedTest {

    private static MockResource mockResource;

    @BeforeAll
    static void beforeAll() {
        mockResource = new MockResource();
    }

    Map<String, Object> getListDataModel(int fromPosition, int pageSize, int stopAt) {
        Map<String, Object> dataModel = new HashMap<>();
        List<EventListItem> entries = new ArrayList<>();

        if (stopAt == -1 || fromPosition < stopAt) {
            for (int position = fromPosition; position < fromPosition + pageSize; position++) {
                EventListItem entry = new EventListItem(position, String.valueOf(position));
                entries.add(entry);
            }
        }
        dataModel.put("entries", entries);

        return dataModel;
    }

    @Test
    void renderXmlFeed() {
        Map<String, Object> listDataModel = getListDataModel(1000, 10, -1);

        String linkPreviousURL = "";
        String linkSelfURL = String.format("http://%s:%s/api%s?position=%s&pageSize=%s%s", "0.0.0.0", "9999", "/feed", 1, 10, 25 == -1 ? "" : "&stopAt=" + 25);
        String linkNextURL = String.format("http://%s:%s/api%s?position=%s&pageSize=%s%s", "0.0.0.0", "9999", "/feed", 1 + 10, 10, 25 == -1 ? "" : "&stopAt=" + 25);
        listDataModel.put("linkPreviousURL", linkPreviousURL);
        listDataModel.put("linkSelfURL", linkSelfURL);
        listDataModel.put("linkNextURL", linkNextURL);

        listDataModel.put("fromPosition", String.valueOf(1000));

        StringWriter output = mockResource.renderTemplate("atom-feed-xml.ftl", listDataModel);
        String xml = mockResource.compactXml(output.toString());
        xml = mockResource.prettyXml(xml);
        System.out.printf("%s%n", xml);
    }

    static class MockResource extends AbstractResource {
        @Override
        void handle(HttpServerExchange exchange) {
            // NOP
        }
    }
}
