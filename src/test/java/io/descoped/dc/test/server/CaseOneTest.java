package io.descoped.dc.test.server;

import io.descoped.dc.test.client.TestClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

@ExtendWith(TestServerExtension.class)
public class CaseOneTest {

    private static final Logger LOG = LoggerFactory.getLogger(CaseOneTest.class);

    @Inject
    TestClient client;

    @Test
    void thatExtensionIsInvoked() {
        System.out.println("Hello 1");
    }
}
