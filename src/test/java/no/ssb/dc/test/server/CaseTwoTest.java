package no.ssb.dc.test.server;

import no.ssb.dc.test.client.TestClient;
import no.ssb.dc.test.config.ConfigurationOverride;
import no.ssb.dc.test.config.ConfigurationProfile;
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
