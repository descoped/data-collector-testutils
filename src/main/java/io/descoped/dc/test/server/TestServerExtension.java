package io.descoped.dc.test.server;

import io.descoped.dc.test.client.TestClient;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.platform.commons.support.HierarchyTraversalMode;
import org.junit.platform.commons.support.ReflectionSupport;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

// https://junit.org/junit5/docs/current/user-guide/#extensions-intercepting-invocations

/**
 * Start a common server for all tests
 * Start a server per test class
 * Start a server per test method
 * <p>
 * (or)
 * <p>
 * Start a server per @ConfigurationProfile (default, overridden)
 */

public class TestServerExtension implements BeforeAllCallback, BeforeEachCallback, BeforeTestExecutionCallback, ParameterResolver, AfterTestExecutionCallback {

    private static final Logger LOG = LoggerFactory.getLogger(TestServerExtension.class);

    private final TestServerFactory testServerFactory;

    public TestServerExtension() {
        testServerFactory = TestServerFactory.instance();
    }

    private TestConfigurationBinding createContainerConfigurationBinding() {
        return TestConfigurationBinding.container();
    }

    private TestConfigurationBinding createClassConfigurationBinding(ExtensionContext context) {
        return TestConfigurationBinding.clazz(context.getRequiredTestClass());
    }

    private TestConfigurationBinding createMethodConfigurationBinding(ExtensionContext context) {
        MethodSource methodSource = MethodSource.from(context.getRequiredTestClass(), context.getRequiredTestMethod());
        return TestConfigurationBinding.method(methodSource);
    }

    boolean compareAndIfInjectionPointExistsSetFieldValue(Object expect, Field field, Object instance, Object value) {
        if (field.isAnnotationPresent(Inject.class) && value.getClass().isAssignableFrom(field.getType())) {
            try {
                if (Modifier.isFinal(field.getModifiers())) {
                    throw new IllegalAccessException("Illegal access to final field: " + instance.getClass().getName() + "." + field.getName());
                }
                field.setAccessible(true);
                // TODO validate TestServerExceptionRegisterTest.thatInjectFieldIsIllegalWhenExpectedFieldStateIfAlreadySet()
//                if (field.get(instance) == expect) {
                field.set(instance, value);
                return true;
//                } else {
//                    throw new IllegalArgumentException(String.format("Value not equal to expected value! Actual: %s, Expected: %s, Field.name: %s, Value: %s", field.get(instance), expect, field.getName(), value));
//                }
            } catch (IllegalArgumentException e) {
                throw e;
            } catch (IllegalAccessException e) {
                throw new TestServerException(e);
            }
        }
        return false;
    }

    private ExtensionContext.Store getStore(ExtensionContext context, TestConfigurationBinding fallbackTestConfigurationBinding) {
        ExtensionContext.Store store;
        if (fallbackTestConfigurationBinding.isContainer()) {
            store = context.getRoot().getStore(ExtensionContext.Namespace.create(fallbackTestConfigurationBinding));

        } else if (fallbackTestConfigurationBinding.isClass()) {
            store = context.getParent().get().getStore(ExtensionContext.Namespace.create(fallbackTestConfigurationBinding));

        } else if (fallbackTestConfigurationBinding.isMethod()) {
            store = context.getStore(ExtensionContext.Namespace.create(fallbackTestConfigurationBinding));

        } else {
            throw new IllegalStateException();
        }
        return store;
    }

    private TestServerResource getTestServerResource(ExtensionContext context, TestConfigurationBinding fallbackTestConfigurationBinding) {
        ExtensionContext.Store store = getStore(context, fallbackTestConfigurationBinding);
        return store.get(fallbackTestConfigurationBinding, TestServerResource.class);
    }

    private TestServerResource createOrGetTestServerResource(TestConfigurationBinding fallbackTestConfigurationBinding, ExtensionContext.Store store) {
        TestServerResource testServerResource = store.getOrComputeIfAbsent(
                fallbackTestConfigurationBinding,
                produce -> {
                    // TODO print console message for server binding
                    return new TestServerResource(testServerFactory.getSupplier(fallbackTestConfigurationBinding).get());
                },
                TestServerResource.class
        );
        testServerFactory.setCurrentTestServerResource(testServerResource);
        return testServerResource;
    }

    private boolean isContainerContext(ExtensionContext context) {
        return !context.getTestInstance().isPresent();
    }

    private void computeAndStartTestServerIfInactive(ExtensionContext context, TestConfigurationBinding fallbackTestConfigurationBinding, ExtensionContext.Store store) {
        TestServerResource testServerResource = createOrGetTestServerResource(fallbackTestConfigurationBinding, store);
        testServerResource.startIfInactive();

        if (isContainerContext(context)) {
            return;
        }

        Object testInstance = context.getRequiredTestInstance();
        List<Field> injectFields = ReflectionSupport.findFields(context.getRequiredTestClass(),
                field -> field.isAnnotationPresent(Inject.class), HierarchyTraversalMode.TOP_DOWN);

        for (Field field : injectFields) {
            // test server
            compareAndIfInjectionPointExistsSetFieldValue(null, field, testInstance, testServerResource.getServer());

            // test client
            compareAndIfInjectionPointExistsSetFieldValue(null, field, testInstance, testServerResource.getClient());
        }

    }

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        TestConfigurationBinding classConfigurationBinding = createContainerConfigurationBinding();
        TestConfigurationBinding fallbackTestConfigurationBinding = testServerFactory.findFallbackConfiguration(classConfigurationBinding);

        LOG.trace("BEGIN {} @ TestServer Binding: {}", context.getRequiredTestClass().getSimpleName(), testServerFactory.state(fallbackTestConfigurationBinding));

        ExtensionContext.Store store = getStore(context, fallbackTestConfigurationBinding);
        computeAndStartTestServerIfInactive(context, fallbackTestConfigurationBinding, store);

    }

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        TestConfigurationBinding classConfigurationBinding = createClassConfigurationBinding(context);
        TestConfigurationBinding fallbackTestConfigurationBinding = testServerFactory.findFallbackConfiguration(classConfigurationBinding);

        LOG.trace("BEGIN {} @ TestServer Binding: {}", context.getRequiredTestClass().getSimpleName(), testServerFactory.state(fallbackTestConfigurationBinding));

        ExtensionContext.Store store = getStore(context, fallbackTestConfigurationBinding);
        computeAndStartTestServerIfInactive(context, fallbackTestConfigurationBinding, store);
    }

    @Override
    public void beforeTestExecution(ExtensionContext context) throws Exception {
        TestConfigurationBinding methodConfigurationBinding = createMethodConfigurationBinding(context);
        TestConfigurationBinding fallbackTestConfigurationBinding = testServerFactory.findFallbackConfiguration(methodConfigurationBinding);

        LOG.trace("BEGIN {} # {} @ TestServer Binding: {}", context.getRequiredTestClass().getSimpleName(), context.getRequiredTestMethod().getName(), testServerFactory.state(fallbackTestConfigurationBinding));

        ExtensionContext.Store store = getStore(context, fallbackTestConfigurationBinding);
        computeAndStartTestServerIfInactive(context, fallbackTestConfigurationBinding, store);
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        return List.of(TestServer.class, TestClient.class).stream().anyMatch(paramClass -> parameterContext.getParameter().getType().equals(paramClass));
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        TestConfigurationBinding methodConfigurationBinding = createMethodConfigurationBinding(extensionContext);
        TestConfigurationBinding fallbackTestConfigurationBinding = testServerFactory.findFallbackConfiguration(methodConfigurationBinding);
        TestServerResource testServerResource = getTestServerResource(extensionContext, fallbackTestConfigurationBinding);

        Class<?> parameterType = parameterContext.getParameter().getType();
        if (parameterType.equals(TestServer.class)) {
            return testServerResource.getServer();

        } else if (parameterType.equals(TestClient.class)) {
            return testServerResource.getClient();

        } else {
            throw new UnsupportedOperationException("Parameter type '" + parameterType.getName() + "' is NOT implemented!");
        }
    }

    @Override
    public void afterTestExecution(ExtensionContext context) throws Exception {
        LOG.trace("END {} # {}", context.getRequiredTestClass().getSimpleName(), context.getRequiredTestMethod().getName());
    }

    class TestServerResource implements ExtensionContext.Store.CloseableResource {

        private final TestServer testServer;
        private final TestClient testClient;
        private final Object lock = new Object();
        private final AtomicBoolean started = new AtomicBoolean(false);

        public TestServerResource(TestServer testServer) {
            this.testServer = testServer;
            this.testClient = TestClient.create(testServer);
        }

        public TestServer getServer() {
            return testServer;
        }

        public TestClient getClient() {
            return testClient;
        }

        // TODO move this to UndertowApplication
        public void startIfInactive() {
            synchronized (lock) {
                if (started.get()) {
                    return;
                }
                testServer.start();
                started.set(true);
            }
        }

        // TODO move this to UndertowApplication
        public void stop() {
            synchronized (lock) {
                if (!started.get()) {
                    return;
                }
                testServer.stop();
                started.set(false);
            }
        }

        @Override
        public void close() throws Throwable {
            stop();
        }
    }
}
