package io.descoped.dc.test.server;

import org.junit.jupiter.api.Test;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.TestPlan;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.platform.engine.discovery.ClassNameFilter.excludeClassNamePatterns;
import static org.junit.platform.engine.discovery.ClassNameFilter.includeClassNamePatterns;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectPackage;

class TestServerExecutionListenerTest {

    private static final Logger LOG = LoggerFactory.getLogger(TestServerExecutionListenerTest.class);

    @Test
    void thatConfigurationFactoryDiscoversAllTestConfigurations() {
        LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                .selectors(
                        selectPackage(CaseOneTest.class.getPackageName()),
                        selectClass(CaseOneTest.class),
                        selectClass(CaseTwoTest.class)
                )
                .filters(
                        includeClassNamePatterns(".*Case(\\w+)Test$"),
                        excludeClassNamePatterns("." + TestServerExecutionListenerTest.class.getSimpleName())
                )
                .build();

        Launcher launcher = LauncherFactory.create();
        TestPlan testPlan = launcher.discover(request);

        /*
         * Invocation of TestServerExecutionListener.testPlanExecutionStarted triggers the ContainerFactory and
         * TestServerFactory to be initialized.
         */

        ConfigurationFactory configurationFactory = new ConfigurationFactory();
        TestServerFactory testServerFactory = new TestServerFactory(configurationFactory);
        TestServerExecutionListener testServerExecutionListener = new TestServerExecutionListener(configurationFactory, testServerFactory);

        testServerExecutionListener.testPlanExecutionStarted(testPlan);

        assertNotNull(configurationFactory);

        assertEquals(3, configurationFactory.keys().size());

        TestConfigurationBinding methodOverrideProfileKey = TestConfigurationBinding.method(CaseTwoTest.class, "thatMethodOverrideConfigurationIsCreated");
        Map<String, String> methodOverrideProfileMap = configurationFactory.get(methodOverrideProfileKey).asMap();

        assertEquals("bar", methodOverrideProfileMap.get("foo"));

//        printConfiguration(configurationFactory);
    }

    @Test
    void testConfigurationOverrideKeyValuePairs() {
        assertThrows(IllegalArgumentException.class, () -> {
            String[] configurationOverrideValues = {"k1", "v1", "k2"};
            ConfigurationFactory configurationFactory = new ConfigurationFactory();
            TestServerFactory testServerFactory = new TestServerFactory(configurationFactory);
            new TestServerExecutionListener(configurationFactory, testServerFactory)
                    .resolveOverrideKeyValueMap(configurationOverrideValues, new LinkedHashMap<>(), MethodSource.from("Foo", "method"));
        });
    }

    @Test
    void thatReflectionFindMethodsByName() {
        TestServerExecutionListener listener = new TestServerExecutionListener(null, null);
        List<Method> methods = listener.findMethodsByName(CaseOneTest.class, MethodSource.from(CaseOneTest.class.getName(), "thatExtensionIsInvoked"));
        assertEquals(1, methods.size());

        assertThrows(IllegalStateException.class, () -> {
            listener.findMethodsByName(CaseOneTest.class, MethodSource.from(CaseOneTest.class.getName(), "methodDoesNotExist"));
        });
    }

    @Test
    void testLoadTestClassException() {
        assertThrows(LoadTestClassException.class, () -> {
            throw new LoadTestClassException(new ClassNotFoundException());
        });

        assertThrows(RuntimeException.class, () -> {
            throw new TestServerExecutionListener(null, null)
                    .handleException(new RuntimeException());
        });

        assertThrows(LoadTestClassException.class, () -> {
            throw new TestServerExecutionListener(null, null)
                    .handleException(new ClassNotFoundException());
        });
    }

    private void printConfiguration(ConfigurationFactory factory) {
        Set<TestConfigurationBinding> testConfigurationBindings = factory.keys();
        for (TestConfigurationBinding testConfigurationBinding : testConfigurationBindings) {
            LOG.trace("configurationKey: {} -> \n\t{}",
                    testConfigurationBinding,
                    factory.get(testConfigurationBinding).asMap()
                            .entrySet()
                            .stream()
                            .map(entry -> entry.getKey() + "=" + entry.getValue())
                            .collect(Collectors.joining("\n\t,"))
            );
        }
    }
}
