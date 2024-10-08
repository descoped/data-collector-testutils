package io.descoped.dc.test.controller;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.descoped.dc.api.handler.QueryException;
import io.descoped.dc.api.util.JsonParser;
import io.descoped.dc.application.engine.FreemarkerTemplateEngine;
import io.undertow.server.HttpServerExchange;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import javax.xml.XMLConstants;
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
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

abstract class AbstractResource {

    static byte[] serializeXml(Object document) {
        try (StringWriter writer = new StringWriter()) {
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            transformerFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);

            // Disable external DTDs and stylesheets
            transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");

            Transformer transformer = transformerFactory.newTransformer();
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

                // Disable external entities
                documentBuilderFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
                documentBuilderFactory.setFeature("http://xml.org/sax/features/external-general-entities", false);
                documentBuilderFactory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
                documentBuilderFactory.setExpandEntityReferences(false);

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

    abstract void handle(HttpServerExchange exchange);

    Optional<String> getContentTypeHeader(HttpServerExchange exchange) {
        return exchange.getRequestHeaders().getHeaderNames().stream()
                .filter(queryParam -> "Accept".equalsIgnoreCase(queryParam.toString()) || "Content-Type".equalsIgnoreCase(queryParam.toString()))
                .map(headerName -> exchange.getRequestHeaders().getFirst(headerName.toString()))
                .findFirst();
    }

    int getQueryParam(Map<String, Deque<String>> queryParameters, String paramName, int defaultValue) {
        String paramValue = queryParameters.containsKey(paramName) ? queryParameters.get(paramName).getFirst() : Integer.toString(defaultValue);
        Objects.requireNonNull(paramValue);
        try {
            return Integer.parseInt(paramValue);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    boolean getQueryParam(Map<String, Deque<String>> queryParameters, String paramName, boolean defaultValue) {
        boolean paramValue = queryParameters.containsKey(paramName) ? Boolean.parseBoolean(queryParameters.get(paramName).getFirst()) : defaultValue;
        Objects.requireNonNull(paramValue);
        return paramValue;
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
