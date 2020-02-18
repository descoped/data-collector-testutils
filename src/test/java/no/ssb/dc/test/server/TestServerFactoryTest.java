package no.ssb.dc.test.server;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.platform.commons.util.ReflectionUtils;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.TestPlan;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectPackage;

class TestServerFactoryTest {

    private static final Logger LOG = LoggerFactory.getLogger(TestServerFactoryTest.class);

    @Test
    void thatLocalPortIsFree() throws IOException {
        TestServerFactory testServerFactory = new TestServerFactory(new ConfigurationFactory());

        assertThrows(IllegalStateException.class, () -> {
            try (ServerSocket ignored = new ServerSocket(10000)) {
                assertEquals(10000, testServerFactory.findFreePort(new Random(), 10000, 10000));
            }
        });
        assertEquals(10000, testServerFactory.findFreePort(new Random(), 10000, 10000));

        int port = testServerFactory.pick(new Random(), 10000, 10500);
        try (ServerSocket ignored = new ServerSocket(port)) {
            assertFalse(testServerFactory.isLocalPortFree(port));
        }
        assertTrue(testServerFactory.isLocalPortFree(port));
    }

    @Test
    void explorationTestFallbackConfigurations() {
        LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                .selectors(
                        selectPackage(CaseOneTest.class.getPackageName())
                )
                .build();

        Launcher launcher = LauncherFactory.create();
        TestPlan testPlan = launcher.discover(request);

        ConfigurationFactory configurationFactory = new ConfigurationFactory();
        TestServerFactory testServerFactory = new TestServerFactory(configurationFactory);
        TestServerExecutionListener testServerExecutionListener = new TestServerExecutionListener(configurationFactory, testServerFactory);

        testServerExecutionListener.testPlanExecutionStarted(testPlan);

        List<Class<?>> testClasses = ReflectionUtils.findAllClassesInPackage(CaseOneTest.class.getPackageName(), classFilter -> true, classNameFilter -> true);
        for (Class<?> testClass : testClasses) {
            if (!(testClass.isAnnotationPresent(ExtendWith.class) && Arrays.stream(testClass.getAnnotation(ExtendWith.class).value()).anyMatch(TestServerExtension.class::isAssignableFrom))) {
                continue;
            }

            List<Method> testMethods = ReflectionUtils.findMethods(testClass, methodFilter -> true);
            for (Method testMethod : testMethods) {
                if (!testMethod.isAnnotationPresent(Test.class)) {
                    continue;
                }

                TestConfigurationBinding testMethodKey = TestConfigurationBinding.method(testClass, testMethod.getName());
                TestConfigurationBinding fallbackKey = testServerFactory.findFallbackConfiguration(testMethodKey);

                if (fallbackKey.isContainer() || fallbackKey.isClass()) {
                    assertNotEquals(testMethodKey, fallbackKey);
                } else {
                    assertEquals(testMethodKey, fallbackKey);
                    assertTrue(fallbackKey.isMethod());
                }

                LOG.trace("TestMethod: [{}]\t{}.{}", TestServerFactory.State.of(fallbackKey), testClass.getSimpleName(), testMethod.getName());
            }
        }
    }
}
