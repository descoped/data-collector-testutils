package io.descoped.dc.test.server;

import io.descoped.config.StoreBasedDynamicConfiguration;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ConfigurationFactoryTest {

    @Test
    void testConfigurationKey() {
        TestConfigurationBinding classKey = TestConfigurationBinding.clazz("ClassA");
        assertEquals("ClassA", classKey.getClassName());

        TestConfigurationBinding methodKey = TestConfigurationBinding.method("ClassA", "method");
        assertEquals("ClassA", methodKey.getClassName());
        assertEquals("method", methodKey.getMethodName().orElseThrow());
    }

    @Test
    void thatConfigurationKeyComparesEqualTo() {
        TestConfigurationBinding defaultExpected = TestConfigurationBinding.container();
        assertEquals(defaultExpected, TestConfigurationBinding.container());
    }

    @Test
    void thatKeysAreNotDuplicated() {
        ConfigurationFactory factory = new ConfigurationFactory();

        factory.putIfAbsent(TestConfigurationBinding.clazz("ClassA"), "profile1");
        assertEquals(1, factory.keys().size());

        factory.putIfAbsent(TestConfigurationBinding.clazz("ClassA"), "profile1");
        assertEquals(1, factory.keys().size());

        factory.putIfAbsent(TestConfigurationBinding.method("ClassA", "method1"), "profile2");
        assertEquals(2, factory.keys().size());

        factory.putIfAbsent(TestConfigurationBinding.method("ClassA", "method1"), "profile2");
        assertEquals(2, factory.keys().size());

        factory.clear();
        assertEquals(0, factory.keys().size());
    }

    @Test
    void testConfigurationProfileIsLoaded() {
        StoreBasedDynamicConfiguration.Builder builder = new StoreBasedDynamicConfiguration.Builder();

        ConfigurationFactory.ConfigurationProfile configurationProfile = new ConfigurationFactory.ConfigurationProfile("dummy");
        configurationProfile.copyTo(builder);

        StoreBasedDynamicConfiguration configuration = builder.build();

        assertEquals("v1", configuration.evaluateToString("k1"));

        assertEquals(configurationProfile, new ConfigurationFactory.ConfigurationProfile("dummy"));
        assertEquals(configurationProfile.hashCode(), Objects.hash("dummy"));
        assertNotNull(configurationProfile.toString());
    }

    @Test
    void testConfigurationOverrideIsLoaded() {
        StoreBasedDynamicConfiguration.Builder builder = new StoreBasedDynamicConfiguration.Builder();

        Map<String, String> map = Map.of("k1", "v1");
        ConfigurationFactory.ConfigurationOverride configurationOverride = new ConfigurationFactory.ConfigurationOverride(map);
        configurationOverride.copyTo(builder);

        StoreBasedDynamicConfiguration configuration = builder.build();

        assertEquals("v1", configuration.evaluateToString("k1"));

        assertEquals(configurationOverride, new ConfigurationFactory.ConfigurationOverride(map));
        assertEquals(configurationOverride.hashCode(), map.hashCode());
        assertNotNull(configurationOverride.toString());
    }

    @Test
    void testConfigurationProfileAndOverrideIsLoaded() {
        StoreBasedDynamicConfiguration.Builder builder = new StoreBasedDynamicConfiguration.Builder();

        ConfigurationFactory.ConfigurationProfile configurationProfile = new ConfigurationFactory.ConfigurationProfile("dummy");
        configurationProfile.copyTo(builder);

        Map<String, String> map = Map.of("k2", "v2");
        ConfigurationFactory.ConfigurationOverride configurationOverride = new ConfigurationFactory.ConfigurationOverride(map);
        configurationOverride.copyTo(builder);

        StoreBasedDynamicConfiguration configuration = builder.build();

        assertEquals("v1", configuration.evaluateToString("k1"));
        assertEquals("v2", configuration.evaluateToString("k2"));

        assertEquals(configurationOverride, new ConfigurationFactory.ConfigurationOverride(map));
        assertEquals(configurationOverride.hashCode(), map.hashCode());
        assertNotNull(configurationOverride.toString());
    }

}
