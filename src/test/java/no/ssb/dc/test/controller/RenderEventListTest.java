package no.ssb.dc.test.controller;

import io.undertow.server.HttpServerExchange;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.StringWriter;

public class RenderEventListTest {

    private static DummyResource resource;

    @BeforeAll
    static void beforeAll() {
        resource = new DummyResource();
    }

    @Test
    void renderXmlList() {
        StringWriter output = resource.renderTemplate("event-list-xml.ftl", resource.getListDataModel(1, 10, -1));
        String xml = resource.compactXml(output.toString());
        //xml = resource.prettyXml(xml);
        System.out.printf("%s%n", xml);
    }

    @Test
    void renderXmlItem() {
        StringWriter output = resource.renderTemplate("event-list-item-xml.ftl", resource.getItemDataModel(1));
        String xml = resource.compactXml(output.toString());
        //xml = resource.prettyXml(xml);
        System.out.printf("%s%n", xml);
    }

    @Test
    void renderJsonList() {
        StringWriter output = resource.renderTemplate("event-list-json.ftl", resource.getListDataModel(1, 10, -1));
        String json = output.toString();
        json = resource.compactJson(json);
        System.out.printf("%s%n", json);
    }

    @Test
    void renderJsonItem() {
        StringWriter output = resource.renderTemplate("event-list-item-json.ftl", resource.getItemDataModel(1));
        String json = output.toString();
        json = resource.compactJson(json);
        System.out.printf("%s%n", json);
    }

    static class DummyResource extends AbstractResource {
        @Override
        void handle(HttpServerExchange exchange) {
            // NOP
        }
    }
}
