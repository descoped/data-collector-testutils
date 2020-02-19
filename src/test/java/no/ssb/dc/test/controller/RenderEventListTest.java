package no.ssb.dc.test.controller;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import no.ssb.dc.api.handler.QueryException;
import no.ssb.dc.api.util.JsonParser;
import no.ssb.dc.application.engine.FreemarkerTemplateEngine;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RenderEventListTest {

    static byte[] serialize(Object document) {
        try (StringWriter writer = new StringWriter()) {
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            transformer.transform(new DOMSource((Node) document), new StreamResult(writer));
            return writer.toString().getBytes();
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Exception e) {
            throw new QueryException(e);
        }
    }

    static Object deserialize(byte[] source) {
        try {
            SAXParserFactory sax = SAXParserFactory.newInstance();
            sax.setNamespaceAware(false);
            XMLReader reader = sax.newSAXParser().getXMLReader();
            try (ByteArrayInputStream bais = new ByteArrayInputStream(source)) {
                SAXSource saxSource = new SAXSource(reader, new InputSource(bais));
                DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
                documentBuilderFactory.setNamespaceAware(false);
                DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
                Document doc = documentBuilder.parse(saxSource.getInputSource());
                doc.normalizeDocument();
                return doc;
            }
        } catch (SAXException | IOException | ParserConfigurationException e) {
            throw new QueryException(new String(source), e);
        }
    }

    String prettyXml(String xml) {
        return new String(serialize(deserialize(xml.getBytes())));
    }

    String compactXml(String xml) {
        return xml.replace("\n", "");
    }

    String compactJson(String json) {
        JsonParser jsonParser = JsonParser.createJsonParser();
        Class<?> aClass = json.startsWith("[") ? ArrayNode.class : ObjectNode.class;
        json = jsonParser.toJSON(jsonParser.fromJson(json, aClass));
        return json;
    }

    StringWriter renderTemplate(String templateFile, Map<String, Object> dataModel) {
        FreemarkerTemplateEngine engine = new FreemarkerTemplateEngine("/freemarker-templates");
        StringWriter output = new StringWriter();
        PrintWriter writer = new PrintWriter(output);
        engine.process(templateFile, dataModel, writer);
        return output;
    }

    Map<String, Object> getListDataModel() {
        Map<String, Object> dataModel = new HashMap<>();
        List<EventListItem> list = new ArrayList<>();
        for (int n = 1; n < 11; n++) {
            list.add(new EventListItem(n, String.valueOf(n + 1000)));
        }
        dataModel.put("list", list);
        return dataModel;
    }

    Map<String, Object> getItemDataModel(int position) {
        Map<String, Object> dataModel = new HashMap<>();
        dataModel.put("item", new EventListItem(position, String.valueOf(position + 1000)));
        return dataModel;
    }

    @Test
    void renderXmlList() {
        StringWriter output = renderTemplate("event-list-xml.ftl", getListDataModel());
        String xml = compactXml(output.toString());
        //xml = prettyXml(xml);
        System.out.printf("%s%n", xml);
    }

    @Test
    void renderXmlItem() {
        StringWriter output = renderTemplate("event-list-item-xml.ftl", getItemDataModel(1));
        String xml = compactXml(output.toString());
        //xml = prettyXml(xml);
        System.out.printf("%s%n", xml);
    }

    @Test
    void renderJsonList() {
        StringWriter output = renderTemplate("event-list-json.ftl", getListDataModel());
        String json = output.toString();
        json = compactJson(json);
        System.out.printf("%s%n", json);
    }

    @Test
    void renderJsonItem() {
        StringWriter output = renderTemplate("event-list-item-json.ftl", getItemDataModel(1));
        String json = output.toString();
        json = compactJson(json);
        System.out.printf("%s%n", json);
    }
}
