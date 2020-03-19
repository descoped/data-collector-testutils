package no.ssb.dc.test.server;

import no.ssb.dc.test.client.TestClient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.inject.Inject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(TestServerExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
public class TestServerExtensionBeforeAllAndEachTest {

    @Inject TestClient testClient;

    @Inject TestServer testServer;

    @BeforeAll
    static void beforeAll() {
        assertNotNull(TestServerFactory.instance().currentServer());
        assertNotNull(TestServerFactory.instance().currentClient());
    }

    @BeforeEach
    void beforeEach() {
        assertEquals(testServer, TestServerFactory.instance().currentServer());
        assertEquals(testClient, TestServerFactory.instance().currentClient());
    }

    @Test
    void testContainerScoped() {
        assertEquals(testServer, TestServerFactory.instance().currentServer());
        assertEquals(testClient, TestServerFactory.instance().currentClient());
    }
}
