package no.ssb.dc.test.controller;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class RenderAtomFeedTest {

    private static AtomFeedResource atomFeedResource;

    @BeforeAll
    static void beforeAll() {
        atomFeedResource = new AtomFeedResource();
    }

    @Test
    void renderXmlFeed() {
        String output = atomFeedResource.renderAtomFeedAsXml("1000", 25, "-1");
        String xml = atomFeedResource.compactXml(output.toString());
        xml = atomFeedResource.prettyXml(xml);
        System.out.printf("%s%n", xml);
    }

}
