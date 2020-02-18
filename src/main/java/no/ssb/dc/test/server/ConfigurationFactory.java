package no.ssb.dc.test.server;

import no.ssb.config.DynamicConfiguration;
import no.ssb.config.StoreBasedDynamicConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

class ConfigurationFactory {

    private final Map<TestConfigurationBinding, List<Configuration>> serverConfiguration = new ConcurrentHashMap<>();

    ConfigurationFactory() {
    }

    static ConfigurationFactory instance() {
        return ConfigurationFactorySingleton.INSTANCE;
    }

    private StoreBasedDynamicConfiguration.Builder createDynamicBuilder() {
        return new StoreBasedDynamicConfiguration.Builder()
                .propertiesResource("application-defaults.properties")
                .propertiesResource("application-test.properties");
    }

    private DynamicConfiguration buildDynamicBuilder(StoreBasedDynamicConfiguration.Builder builder) {
        return builder
                .systemProperties()
                .environment("DC_")
                .build();
    }

    Set<TestConfigurationBinding> keys() {
        return serverConfiguration.keySet();
    }

    /**
     * Resolve {@link DynamicConfiguration} for a {@link TestConfigurationBinding}. If binding is not found,
     * a default configuration will be returned. If test method is not found, it will try to fallback to test class.
     *
     * @param testConfigurationBinding key of className or className/methodName
     * @return Default configuration unless key matches class- or -method level configuration binding
     */
    DynamicConfiguration get(TestConfigurationBinding testConfigurationBinding) {
        StoreBasedDynamicConfiguration.Builder builder = createDynamicBuilder();

        List<Configuration> configurationList = serverConfiguration.getOrDefault(testConfigurationBinding, new ArrayList<>());
        for (Configuration configuration : configurationList) {
            configuration.copyTo(builder);
        }

        return buildDynamicBuilder(builder);
    }

    void putIfAbsent(TestConfigurationBinding testConfigurationBinding, String profileName) {
        List<Configuration> configurationList = serverConfiguration.computeIfAbsent(testConfigurationBinding, list -> new ArrayList<>());
        ConfigurationProfile configurationProfile = new ConfigurationProfile(profileName);
        if (!configurationList.contains(configurationProfile)) {
            configurationList.add(configurationProfile);
        }
    }

    void putIfAbsent(TestConfigurationBinding testConfigurationBinding, Map<String, String> configurationMap) {
        List<Configuration> configurationList = serverConfiguration.computeIfAbsent(testConfigurationBinding, list -> new ArrayList<>());
        ConfigurationOverride configurationOverride = new ConfigurationOverride(configurationMap);
        if (!configurationList.contains(configurationOverride)) {
            configurationList.add(configurationOverride);
        }
    }

    void clear() {
        serverConfiguration.clear();
    }

    private static class ConfigurationFactorySingleton {
        private static final ConfigurationFactory INSTANCE = new ConfigurationFactory();
    }

    abstract static class Configuration {
        abstract void copyTo(StoreBasedDynamicConfiguration.Builder builder);
    }

    static class ConfigurationProfile extends Configuration {
        final String profileName;

        ConfigurationProfile(String profileName) {
            this.profileName = profileName;
        }

        @Override
        void copyTo(StoreBasedDynamicConfiguration.Builder builder) {
            builder.propertiesResource(String.format("application-%s.properties", profileName));
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ConfigurationProfile that = (ConfigurationProfile) o;
            return profileName.equals(that.profileName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(profileName);
        }

        @Override
        public String toString() {
            return "ConfigurationProfile{" +
                    "profileName='" + profileName + '\'' +
                    '}';
        }
    }

    static class ConfigurationOverride extends Configuration {
        final Map<String, String> overrideMap;

        ConfigurationOverride(Map<String, String> overrideMap) {
            this.overrideMap = overrideMap;
        }

        @Override
        void copyTo(StoreBasedDynamicConfiguration.Builder builder) {
            overrideMap.forEach((key, value) -> builder.values(key, value));
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ConfigurationOverride that = (ConfigurationOverride) o;
            return overrideMap.equals(that.overrideMap);
        }

        @Override
        public int hashCode() {
            return overrideMap.hashCode();
        }

        @Override
        public String toString() {
            return "ConfigurationOverride{" +
                    "overrideMap=" + overrideMap +
                    '}';
        }
    }

}
