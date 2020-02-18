package no.ssb.dc.test.server;

import no.ssb.dc.test.ConfigurationOverride;
import no.ssb.dc.test.ConfigurationProfile;
import no.ssb.dc.test.client.TestClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(TestServerExtension.class)
class CaseTwoTest {

    private static final Logger LOG = LoggerFactory.getLogger(CaseTwoTest.class);

    @Inject TestClient client;

    @Test
    void thatExtensionIsInvoked() {
        System.out.println("Hello 2");
    }

    @ConfigurationOverride({"foo", "bar"})
    @Test
    void thatMethodOverrideConfigurationIsCreated() {

    }

    @ConfigurationProfile("dummy")
    @ConfigurationOverride({"foo", "bar"})
    @Test
    void thatMethodOverrideProfileConfigurationIsCreated() {

    }

    @ConfigurationProfile("dummy")
    @ConfigurationOverride({"foo", "bar"})
    @Test
    void thatMethodOverrideProfileConfiguration2IsCreated() {

    }

    @Test
    void handleException() {
        assertThrows(RuntimeException.class, () -> {
            throw new RuntimeException("Blow!");
        });
    }
}
