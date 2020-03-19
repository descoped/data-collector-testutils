package no.ssb.dc.test.server;

import no.ssb.dc.test.client.TestClient;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.platform.commons.support.HierarchyTraversalMode;
import org.junit.platform.commons.support.ReflectionSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The purpose of this test class is to test TestServerExtension and to assert correct behaviour of its
 * compareAndIfInjectionPointExistsSetFieldValue() method.
 */
class TestServerExtensionRegisterTest {

    private static final Logger LOG = LoggerFactory.getLogger(TestServerExtensionRegisterTest.class);

    @RegisterExtension
    static TestServerExtension extension = new TestServerExtension();

    /*
     * Resolve injection points in TestServerExtensionTest
     * Try to inject a VALID TestClient to field testClient
     */

    @Test
    void thatInjectFieldIsValid() throws Exception {
        List<Field> injectFields = ReflectionSupport.findFields(TestServerExtensionTest.class, field -> field.isAnnotationPresent(Inject.class), HierarchyTraversalMode.TOP_DOWN);
        assertFalse(injectFields.isEmpty());
        TestServerExtensionTest test = new TestServerExtensionTest();
        assertNull(injectFields.get(0).get(test));
        assertTrue(extension.compareAndIfInjectionPointExistsSetFieldValue(null, injectFields.get(0), test, TestClient.newClient(null)));
        assertNotNull(injectFields.get(0).get(test));
    }


    /*
     * Resolve injection points in TestServerExtensionTest.
     * Try to inject a TestClient to field testServer, illegal because it is a invalid type
     */


    @Disabled
    @Test
    void thatInjectFieldIsIllegalWhenExpectedFieldStateIfAlreadySet() {
        assertThrows(IllegalArgumentException.class, () -> {
            List<Field> injectFields = ReflectionSupport.findFields(TestServerExtensionTest.class, field -> field.isAnnotationPresent(Inject.class), HierarchyTraversalMode.TOP_DOWN);
            assertFalse(injectFields.isEmpty());
            TestServerExtensionTest test = new TestServerExtensionTest();
            extension.compareAndIfInjectionPointExistsSetFieldValue(test, injectFields.get(0), test, TestClient.newClient(null));
        });
    }

    /*
     * Negative test where wrong type is used for injection target.
     * The injection point is found in TestServerExtensionTest and is tried to be injected on this extension
     */

    @Test
    @DisplayName("Try to inject TestClient on TestServerExtension that doesn't declare a valid injection point")
    void thatInjectFieldIsInvalid() {
        assertThrows(IllegalArgumentException.class, () -> {
            List<Field> injectFields = ReflectionSupport.findFields(TestServerExtensionTest.class, field -> field.isAnnotationPresent(Inject.class), HierarchyTraversalMode.TOP_DOWN);
            assertFalse(injectFields.isEmpty());
            extension.compareAndIfInjectionPointExistsSetFieldValue(null, injectFields.get(0), extension, TestClient.newClient(null));
        });
    }

    @Test
    void thatInjectFieldIsNotAllowedOnFinalField() {
        assertThrows(TestServerException.class, () -> {
            List<Field> injectFields = ReflectionSupport.findFields(Immutable.class, field -> field.isAnnotationPresent(Inject.class), HierarchyTraversalMode.TOP_DOWN);
            assertFalse(injectFields.isEmpty());
            Immutable immutable = new Immutable();
            extension.compareAndIfInjectionPointExistsSetFieldValue(null, injectFields.get(0), immutable, "test");
        });
    }

    static class Immutable {
        @Inject final String text;

        public Immutable() {
            text = null;
        }
    }
}
