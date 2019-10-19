package no.ssb.dc.test.server;

import no.ssb.config.DynamicConfiguration;
import no.ssb.config.StoreBasedDynamicConfiguration;
import org.testng.annotations.Test;

import java.util.Random;

public class TestServerTest {

    @Test
    public void testName() throws InterruptedException {
        DynamicConfiguration configuration = new StoreBasedDynamicConfiguration.Builder()
                .build();

        int testServerServicePort = TestServerListener.findFreePort(new Random(), 9000, 9499);

        TestServer testServer = new TestServer(configuration, testServerServicePort);
        testServer.start();
        Thread.sleep(1000);
        testServer.stop();
    }
}
