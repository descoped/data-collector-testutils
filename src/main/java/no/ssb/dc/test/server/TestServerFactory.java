package no.ssb.dc.test.server;

import no.ssb.config.DynamicConfiguration;
import no.ssb.dc.test.client.TestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public class TestServerFactory {

    private static final Logger LOG = LoggerFactory.getLogger(TestServerFactory.class);

    private final ConfigurationFactory configurationFactory;
    private final Map<TestConfigurationBinding, Supplier<TestServer>> testServerSuppliers = new ConcurrentHashMap<>();
    private final Random random = new Random();

    /*
     * The TestServerExtension maintains current execution test server target in beforeTestExecution
     */
    private final AtomicReference<TestServerExtension.TestServerResource> currentTestServerResourceReference = new AtomicReference<>();

    TestServerFactory(ConfigurationFactory configurationFactory) {
        this.configurationFactory = configurationFactory;
    }

    public static TestServerFactory instance() {
        return TestServerFactory.TestServerFactorySingleton.INSTANCE;
    }

    @SuppressWarnings("SameParameterValue")
    int findFreePort(Random random, int from, int to) {
        int port = pick(random, from, to);
        for (int i = 0; i < 2 * ((to + 1) - from); i++) {
            if (isLocalPortFree(port)) {
                return port;
            }
            port = pick(random, from, to);
        }
        throw new IllegalStateException("Unable to find any available ports in range: [" + from + ", " + (to + 1) + ")");
    }

    int pick(Random random, int from, int to) {
        return from + random.nextInt((to + 1) - from);
    }

    boolean isLocalPortFree(int port) {
        try {
            try (ServerSocket ignore = new ServerSocket(port)) {
                return true;
            }
        } catch (IOException e) {
            return false;
        }
    }

    State state(TestConfigurationBinding testConfigurationBinding) {
        if (testConfigurationBinding.isContainer()) {
            return State.CONTAINER;

        } else if (testConfigurationBinding.isClass()) {
            return State.CLASS;

        } else if (testConfigurationBinding.isMethod()) {
            return State.METHOD;

        } else {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Create TestServer suppliers for lazy initialization/creation of test server instances.
     * A TestServer is bound to a TestConfigurationBinding that is resolved by the TestServerExtension during
     * test execution.
     */
    void initialize() {
        createTestServerForConfigurationBinding(TestConfigurationBinding.container());

        for (TestConfigurationBinding testConfigurationBinding : configurationFactory.keys()) {
            createTestServerForConfigurationBinding(testConfigurationBinding);
        }
    }

    /**
     * Resolve the nearest test configuration binding in the order: method, class or container.
     *
     * @param testConfigurationBinding test method target
     * @return if the test-method is annotated it will try to find class config, or fallback to the container config
     */
    TestConfigurationBinding findFallbackConfiguration(TestConfigurationBinding testConfigurationBinding) {
        // return bound configuration
        if (testServerSuppliers.containsKey(testConfigurationBinding)) {
            return testConfigurationBinding;
        }

        // fallback method to class level configuration if exists
        if (testConfigurationBinding.isMethod()) {
            TestConfigurationBinding classTestConfigurationBinding = TestConfigurationBinding.clazz(testConfigurationBinding.getClassName());
            if (testServerSuppliers.containsKey(classTestConfigurationBinding)) {
                return classTestConfigurationBinding;
            }
        }

        // fallback to default configuration if none matches
        return TestConfigurationBinding.container();
    }

    private void createTestServerForConfigurationBinding(TestConfigurationBinding testConfigurationBinding) {
        Supplier<TestServer> testServerSupplier = new Supplier<>() {
            final DynamicConfiguration configuration = configurationFactory.get(testConfigurationBinding);

            @Override
            public TestServer get() {
                int availablePort = findFreePort(random, 9000, 9499);
                return new TestServer(configuration, availablePort);
            }
        };

        testServerSuppliers.put(testConfigurationBinding, testServerSupplier);
    }

    Supplier<TestServer> getSupplier(TestConfigurationBinding testConfigurationBinding) {
        return testServerSuppliers.get(testConfigurationBinding);
    }

    // see field comment
    void setCurrentTestServerResource(TestServerExtension.TestServerResource testServerResource) {
        currentTestServerResourceReference.set(testServerResource);
    }

    public TestServer currentServer() {
        TestServerExtension.TestServerResource testServerResource = currentTestServerResourceReference.get();
        if (testServerResource == null) {
            throw new IllegalStateException("The test server has yet NOT been created!");
        }
        return testServerResource.getServer();
    }

    public TestClient currentClient() {
        TestServerExtension.TestServerResource testServerResource = currentTestServerResourceReference.get();
        if (testServerResource == null) {
            throw new IllegalStateException("The test server has yet NOT been created!");
        }
        return testServerResource.getClient();
    }

    public enum State {
        CONTAINER,
        CLASS,
        METHOD;

        static State of(TestConfigurationBinding key) {
            if (key.isContainer()) {
                return CONTAINER;
            } else if (key.isClass()) {
                return CLASS;
            } else if (key.isMethod()) {
                return METHOD;
            } else {
                throw new UnsupportedOperationException();
            }
        }
    }

    private static class TestServerFactorySingleton {
        private static final TestServerFactory INSTANCE = new TestServerFactory(ConfigurationFactory.instance());
    }
}
