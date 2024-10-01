package io.descoped.dc.test.server;

import io.descoped.dc.test.config.ConfigurationOverride;
import io.descoped.dc.test.config.ConfigurationProfile;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.platform.commons.support.HierarchyTraversalMode;
import org.junit.platform.commons.support.ReflectionSupport;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.reporting.ReportEntry;
import org.junit.platform.engine.support.descriptor.ClassSource;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

import static java.util.Optional.ofNullable;

/**
 * The TestServerExecutionListener is invoked by JUnit5 before the test suite is executed. It scans all test classes
 * and prepares the ConfigurationFactory with profile and override config bound to each test method. Finally, it
 * initializes the TestServerFactory (@see @link TestServerFactory.#initialize).
 * <p>
 * This is a pre-requisite step for the TestServerExtension!
 */
public class TestServerExecutionListener implements TestExecutionListener {

    private final ConfigurationFactory configurationFactory;
    private final TestServerFactory testServerFactory;

    @SuppressWarnings("unused")
    public TestServerExecutionListener() {
        configurationFactory = ConfigurationFactory.instance();
        testServerFactory = TestServerFactory.instance();
    }

    TestServerExecutionListener(ConfigurationFactory configurationFactory, TestServerFactory testServerFactory) {
        this.configurationFactory = configurationFactory;
        this.testServerFactory = testServerFactory;
    }

    private boolean isTestContainerIdentifier(Set<TestIdentifier> ancestors) {
        return ancestors.isEmpty();
    }

    private boolean isTestClassIdentifier(Set<TestIdentifier> ancestors) {
        return ancestors.size() == 1;
    }

    private boolean isTestMethodIdentifier(Set<TestIdentifier> ancestors) {
        return ancestors.size() == 2;
    }

    private boolean isExtendWithPresent(Class<?> javaClass) {
        return ofNullable(javaClass).orElseThrow().isAnnotationPresent(ExtendWith.class);
    }

    /**
     * Scan test classpath and build a ContainerFactory for TestServerExtension.
     *
     * @param testPlan describes the tree of tests about to be executed
     */
    @Override
    public void testPlanExecutionStarted(TestPlan testPlan) {
        for (TestIdentifier rootIdentifier : testPlan.getRoots()) {
            walk(testPlan, rootIdentifier, (ancestors, testIdentifier) -> {

                if (isTestContainerIdentifier(ancestors)) {
                    // nop

                    // class profile
                } else if (isTestClassIdentifier(ancestors)) {
                    ifTestServerExtensionIsPresent(testIdentifier, (extendWith, classSource) -> {
                        // TODO deal with class level test class profiles
                    });

                    // methods profile
                } else if (isTestMethodIdentifier(ancestors)) {
                    ifMethodAnnotationIsPresent(testIdentifier, ConfigurationProfile.class, ((configurationProfile, methodSource) -> {
                        TestConfigurationBinding testConfigurationBinding = TestConfigurationBinding.method(methodSource);
                        configurationFactory.putIfAbsent(testConfigurationBinding, configurationProfile.value());
                    }));

                    ifMethodAnnotationIsPresent(testIdentifier, ConfigurationOverride.class, ((configurationOverride, methodSource) -> {
                        Map<String, String> configurationOverrideMap = new LinkedHashMap<>();
                        String[] keyAndValuePairs = configurationOverride.value();
                        resolveOverrideKeyValueMap(keyAndValuePairs, configurationOverrideMap, methodSource);
                        TestConfigurationBinding testConfigurationBinding = TestConfigurationBinding.method(methodSource);
                        configurationFactory.putIfAbsent(testConfigurationBinding, configurationOverrideMap);
                    }));
                }
            });
        }

        testServerFactory.initialize();
    }

    void resolveOverrideKeyValueMap(String[] sourceArray, Map<String, String> targetMap, MethodSource methodSource) {
        if (sourceArray.length % 2 != 0) {
            throw new IllegalArgumentException(String.format("Wrong number of arguments (%s) for @ConfigurationOverride -> %s.%s",
                    sourceArray.length, methodSource.getClassName(), methodSource.getMethodName()));
        }
        for (int i = 0; i < sourceArray.length; i += 2) {
            targetMap.put(sourceArray[i], sourceArray[i + 1]);
        }
    }

    private void ifTestServerExtensionIsPresent(TestIdentifier testIdentifier, BiConsumer<ExtendWith, ClassSource> action) {
        ClassSource classSource = (ClassSource) testIdentifier.getSource().orElseThrow();
        Class<?> javaClass = classSource.getJavaClass();

        boolean isExtendedWithPresent = isExtendWithPresent(javaClass);
        if (!isExtendedWithPresent) {
            return;
        }

        ExtendWith extendWith = javaClass.getAnnotation(ExtendWith.class);
        boolean isTestServerExtension = Arrays.stream(extendWith.value()).anyMatch(clazz -> clazz.isAssignableFrom(TestServerExtension.class));

        if (isTestServerExtension) {
            action.accept(extendWith, classSource);
        }
    }

    private <R extends Annotation> void ifMethodAnnotationIsPresent(TestIdentifier testIdentifier, Class<R> annotationClass, BiConsumer<R, MethodSource> action) {
        MethodSource methodSource = (MethodSource) testIdentifier.getSource().orElseThrow();
        Class<?> javaClass = ReflectionSupport.tryToLoadClass(methodSource.getClassName()).getOrThrow(this::handleException);

        boolean isExtendedWithPresent = isExtendWithPresent(javaClass);
        if (!isExtendedWithPresent) {
            return;
        }

        List<Method> matchingMethods = findMethodsByName(javaClass, methodSource);

        for (Method method : matchingMethods) {
            boolean isAnnotationIsPresent = method.isAnnotationPresent(annotationClass);
            if (isAnnotationIsPresent) {
                action.accept(method.getAnnotation(annotationClass), methodSource);
            }
        }
    }

    List<Method> findMethodsByName(Class<?> javaClass, MethodSource methodSource) {
        List<Method> matchingMethods = ReflectionSupport.findMethods(javaClass, method -> methodSource.getMethodName().equals(method.getName()), HierarchyTraversalMode.TOP_DOWN);
        if (matchingMethods.isEmpty()) {
            throw new IllegalStateException("Unable to find method: " + methodSource.getMethodName() + " in class: " + javaClass.getName());
        }
        return matchingMethods;
    }

    RuntimeException handleException(Exception e) {
        if (e instanceof RuntimeException) {
            return (RuntimeException) e;
        }
        return new LoadTestClassException(e);
    }

    private void walk(TestPlan testPlan, TestIdentifier currentTestIdentifier, BiConsumer<Set<TestIdentifier>, TestIdentifier> visitor) {
        traverse(0, new LinkedHashSet<>(), testPlan, new LinkedHashSet<>(), currentTestIdentifier, visitor);
    }

    private void traverse(int depth,
                          Set<TestIdentifier> visitedTestIdentifier,
                          TestPlan testPlan, Set<TestIdentifier> ancestors,
                          TestIdentifier currentTestIdentifier,
                          BiConsumer<Set<TestIdentifier>, TestIdentifier> visitor) {

        if (!visitedTestIdentifier.add(currentTestIdentifier)) {
            return;
        }

        visitor.accept(ancestors, currentTestIdentifier);

        ancestors.add(currentTestIdentifier);

        Set<TestIdentifier> children = testPlan.getChildren(currentTestIdentifier);
        try {
            for (TestIdentifier child : children) {
                traverse(depth + 1, visitedTestIdentifier, testPlan, ancestors, child, visitor);
            }
        } finally {
            ancestors.remove(currentTestIdentifier);
        }
    }

    @Override
    public void dynamicTestRegistered(TestIdentifier testIdentifier) {
        // nop
    }

    @Override
    public void executionSkipped(TestIdentifier testIdentifier, String reason) {
        // nop
    }

    @Override
    public void executionStarted(TestIdentifier testIdentifier) {
        // nop
    }

    @Override
    public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult) {
        // nop
    }

    @Override
    public void reportingEntryPublished(TestIdentifier testIdentifier, ReportEntry entry) {
        // nop
    }

    @Override
    public void testPlanExecutionFinished(TestPlan testPlan) {
        // nop
    }
}
