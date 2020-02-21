package no.ssb.dc.test.controller;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.undertow.server.HttpServerExchange;
import no.ssb.dc.api.handler.QueryException;
import no.ssb.dc.api.util.JsonParser;
import no.ssb.dc.application.engine.FreemarkerTemplateEngine;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

abstract class AbstractResource {

    abstract void handle(HttpServerExchange exchange);

    static byte[] serializeXml(Object document) {
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

    static Document deserializeXml(byte[] source) {
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

    static String prettyXml(String xml) {
        return new String(serializeXml(deserializeXml(xml.getBytes())));
    }

    Optional<String> getContentTypeHeader(HttpServerExchange exchange) {
        return exchange.getRequestHeaders().getHeaderNames().stream()
                .filter(queryParam -> "Accept".equalsIgnoreCase(queryParam.toString()) || "Content-Type".equalsIgnoreCase(queryParam.toString()))
                .map(headerName -> exchange.getRequestHeaders().getFirst(headerName.toString()))
                .findFirst();
    }

    int getQueryParam(Map<String, Deque<String>> queryParameters, String paramName, int defaultValue) {
        String paramValue = queryParameters.computeIfAbsent(paramName, key -> new LinkedList<>(Collections.singletonList(String.valueOf(defaultValue)))).getFirst();
        return Integer.parseInt(paramValue);
    }

    String getQueryParam(Map<String, Deque<String>> queryParameters, String paramName, String defaultValue) {
        return queryParameters.computeIfAbsent(paramName, key -> new LinkedList<>(Collections.singletonList(defaultValue))).getFirst();
    }

    String getQueryParam(Map<String, Deque<String>> queryParameters, String paramName, String defaultValue, String mapToDefaultValue) {
        String value = queryParameters.computeIfAbsent(paramName, key -> new LinkedList<>(Collections.singletonList(defaultValue))).getFirst();
        if (value.equals(mapToDefaultValue)) {
            value = defaultValue;
        }
        return value;
    }

    int getPathParam(String requestPath, int pathIndex, int defaultValue) {
        List<String> pathElements = Arrays.asList(requestPath.split("/")).stream().skip(MockDataController.ROOT_CONTEXT_PATH_ELEMENT_COUNT).collect(Collectors.toList());

        if (pathElements.size() <= pathIndex) {
            return defaultValue;
        }

        String pathElement = pathElements.get(pathIndex);

        return Integer.parseInt(pathElement);
    }

    StringWriter renderTemplate(String templateFile, Map<String, Object> dataModel) {
        FreemarkerTemplateEngine engine = new FreemarkerTemplateEngine("/freemarker-templates");
        StringWriter output = new StringWriter();
        PrintWriter writer = new PrintWriter(output);
        engine.process(templateFile, dataModel, writer);
        return output;
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

}
