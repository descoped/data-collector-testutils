package no.ssb.dc.test.controller;

import io.undertow.server.HttpServerExchange;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.StringWriter;

public class RenderEventListTest {

    private static MockResource mockResource;

    @BeforeAll
    static void beforeAll() {
        mockResource = new MockResource();
    }

    @Test
    void renderXmlList() {
        StringWriter output = mockResource.renderTemplate("event-list-xml.ftl", mockResource.getListDataModel(1, 10, -1));
        String xml = mockResource.compactXml(output.toString());
        //xml = resource.prettyXml(xml);
        System.out.printf("%s%n", xml);
    }

    @Test
    void renderXmlItem() {
        StringWriter output = mockResource.renderTemplate("event-list-item-xml.ftl", mockResource.getItemDataModel(1));
        String xml = mockResource.compactXml(output.toString());
        //xml = resource.prettyXml(xml);
        System.out.printf("%s%n", xml);
    }

    @Test
    void renderJsonList() {
        StringWriter output = mockResource.renderTemplate("event-list-json.ftl", mockResource.getListDataModel(1, 10, -1));
        String json = output.toString();
        json = mockResource.compactJson(json);
        System.out.printf("%s%n", json);
    }

    @Test
    void renderJsonItem() {
        StringWriter output = mockResource.renderTemplate("event-list-item-json.ftl", mockResource.getItemDataModel(1));
        String json = output.toString();
        json = mockResource.compactJson(json);
        System.out.printf("%s%n", json);
    }

    static class MockResource extends AbstractResource {
        @Override
        void handle(HttpServerExchange exchange) {
            // NOP
        }
    }
}
