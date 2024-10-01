package io.descoped.dc.test.server;

import io.descoped.config.DynamicConfiguration;
import io.descoped.config.StoreBasedDynamicConfiguration;
import org.junit.jupiter.api.Test;

import java.util.Random;
import java.util.concurrent.Phaser;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestServerTest {

    @Test
    public void testServer() throws InterruptedException {
        DynamicConfiguration configuration = new StoreBasedDynamicConfiguration.Builder()
                .values("http.host", "0.0.0.0")
                .values("http.cors.allow.origin", ".*")
                .values("http.cors.allow.header", "Content-Type,Authorization")
                .values("http.cors.allow.methods", "POST,GET,PUT,DELETE,HEAD")
                .values("http.cors.allow.credentials", "false")
                .values("http.cors.allow.max-age", "900")
                .build();

        TestServerFactory testServerFactory = new TestServerFactory(null);
        int testServerServicePort = testServerFactory.findFreePort(new Random(), 9000, 9499);
        Phaser phaser = new Phaser(1);
        TestServer testServer = new TestServer(configuration, testServerServicePort);
        testServer.start();

        assertEquals("0.0.0.0", testServer.getTestServerHost());
        assertTrue(testServer.getTestServerServicePort() >= 9000 && testServer.getTestServerServicePort() <= 9499);

        assertDoesNotThrow(() -> {
            assertEquals(String.format("http://0.0.0.0:%s", testServer.getTestServerServicePort()), testServer.testURL(""));
        });

        assertEquals(configuration, testServer.getConfiguration());
        assertNotNull(testServer.getApplication());

        phaser.awaitAdvance(1);
        testServer.stop();
        phaser.arrive();
    }

    @Test
    void testTestServerException() {
        assertThrows(TestServerException.class, () -> {
            throw new TestServerException("Error message");
        });
        assertThrows(TestServerException.class, () -> {
            throw new TestServerException(new RuntimeException());
        });
    }
}
