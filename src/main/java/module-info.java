module no.ssb.dc.test {
    requires no.ssb.config;
    requires no.ssb.dc.api;
    requires no.ssb.dc.application;

    requires org.slf4j;
    requires java.net.http;
    requires undertow.core;
    requires javax.inject;

    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.annotation;
    requires com.fasterxml.jackson.dataformat.xml;
    requires com.fasterxml.jackson.dataformat.yaml;

    requires testng;

    exports no.ssb.dc.test;
    exports no.ssb.dc.test.server;
    exports no.ssb.dc.test.client;
    exports no.ssb.dc.test.controller;
}
