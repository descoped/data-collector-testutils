package no.ssb.dc.test.server;

import org.junit.platform.engine.support.descriptor.MethodSource;

import java.util.Objects;
import java.util.Optional;

import static java.util.Optional.ofNullable;

/**
 * TestServerBinding is composite key class that defines a binding for Container (suite), Test Classes or Test Methods.
 * The binding is used by ContainerFactory and TestServerFactory.
 *
 * Any test that is NOT annotated with @ConfigurationProfile or ConfigurationOverride will default to Container binding.
 * Annotated test methods will be provided by a shared test server per config profile or override.
 */
class TestConfigurationBinding {

    private static final TestConfigurationBinding DEFAULT = TestConfigurationBinding.clazz("DEFAULT");

    private final String className;
    private final String methodName;

    private TestConfigurationBinding(String className, String methodName) {
        this.className = className;
        this.methodName = methodName;
    }

    static TestConfigurationBinding container() {
        return DEFAULT;
    }

    static TestConfigurationBinding clazz(Class<?> clazz) {
        Objects.requireNonNull(clazz);
        return new TestConfigurationBinding(clazz.getName(), null);
    }

    static TestConfigurationBinding clazz(String className) {
        return new TestConfigurationBinding(className, null);
    }

    static TestConfigurationBinding method(MethodSource methodSource) {
        Objects.requireNonNull(methodSource);
        return new TestConfigurationBinding(methodSource.getClassName(), methodSource.getMethodName());
    }

    static TestConfigurationBinding method(Class<?> clazz, String methodName) {
        Objects.requireNonNull(clazz);
        return new TestConfigurationBinding(clazz.getName(), methodName);
    }

    static TestConfigurationBinding method(String className, String methodName) {
        return new TestConfigurationBinding(className, methodName);
    }

    public String getClassName() {
        return className;
    }

    public Optional<String> getMethodName() {
        return ofNullable(methodName);
    }

    public boolean isContainer() {
        return this.equals(DEFAULT);
    }

    public boolean isClass() {
        return ofNullable(className).isPresent() && ofNullable(methodName).isEmpty();
    }

    public boolean isMethod() {
        return ofNullable(className).isPresent() && ofNullable(methodName).isPresent();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TestConfigurationBinding that = (TestConfigurationBinding) o;
        return className.equals(that.className) &&
                Objects.equals(methodName, that.methodName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(className, methodName);
    }

    @Override
    public String toString() {
        return "ConfigurationKey{" +
                "className='" + className + '\'' +
                ", methodName='" + methodName + '\'' +
                '}';
    }

}
