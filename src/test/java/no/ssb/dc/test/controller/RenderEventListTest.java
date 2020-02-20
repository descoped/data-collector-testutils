package no.ssb.dc.test.controller;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.StringWriter;

class RenderEventListTest {

    private static EventListResource listResource;
    private static EventItemResource itemResource;

    @BeforeAll
    static void beforeAll() {
        listResource = new EventListResource();
        itemResource = new EventItemResource();
    }

    @Test
    void renderXmlList() {
        StringWriter output = listResource.renderTemplate("event-list-xml.ftl", listResource.getListDataModel(1, 10, -1));
        String xml = listResource.compactXml(output.toString());
        //xml = resource.prettyXml(xml);
        System.out.printf("%s%n", xml);
    }

    @Test
    void renderXmlItem() {
        StringWriter output = listResource.renderTemplate("event-list-item-xml.ftl", itemResource.getItemDataModel(1));
        String xml = listResource.compactXml(output.toString());
        //xml = resource.prettyXml(xml);
        System.out.printf("%s%n", xml);
    }

    @Test
    void renderJsonList() {
        StringWriter output = listResource.renderTemplate("event-list-json.ftl", listResource.getListDataModel(1, 10, -1));
        String json = output.toString();
        json = listResource.compactJson(json);
        System.out.printf("%s%n", json);
    }

    @Test
    void renderJsonItem() {
        StringWriter output = listResource.renderTemplate("event-list-item-json.ftl", itemResource.getItemDataModel(1));
        String json = output.toString();
        json = listResource.compactJson(json);
        System.out.printf("%s%n", json);
    }
}
