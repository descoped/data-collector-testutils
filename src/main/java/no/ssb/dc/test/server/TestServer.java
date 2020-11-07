package no.ssb.dc.test.server;

import no.ssb.config.DynamicConfiguration;
import no.ssb.dapla.secrets.api.SecretManagerClient;
import no.ssb.dc.api.security.ProvidedBusinessSSLResource;
import no.ssb.dc.application.server.UndertowApplication;
import no.ssb.dc.application.ssl.BusinessSSLResourceSupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.SecureRandom;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

import static no.ssb.dc.api.security.ProvidedBusinessSSLResource.safeConvertBytesToCharArrayAsUTF8;

public class TestServer implements TestUriResolver {

    static final Logger LOG = LoggerFactory.getLogger(TestServer.class);

    private final DynamicConfiguration configuration;

    private final UndertowApplication application;

    private final int testServerServicePort;

    public TestServer(DynamicConfiguration configuration, int testServerServicePort) {
        this.configuration = configuration;
        this.testServerServicePort = testServerServicePort;

        /*
         * Delegated certificate loader from Google Secret Manager is placed here, because we don't won't a
         * dependency in the data collector to any host aware libraries such as the Google Secret Manager.
         * The supplier is wrapped by the BusinessSSLResourceSupplier and passes an implementing instance of
         * BusinessSSLBundle to Worker.useBusinessSSLResourceSupplier() in the Core module.
         *
         * Please note: only Google Secret Manager is supported!
         */

        String businessSslResourceProvider = configuration.evaluateToString("data.collector.sslBundle.provider");
        boolean hasBusinessSslResourceProvider = "google-secret-manager".equals(businessSslResourceProvider);

        Supplier<ProvidedBusinessSSLResource> sslResourceSupplier = () -> {
            if (!hasBusinessSslResourceProvider) {
                return null;
            }
            LOG.info("Create BusinessSSL resource provider: {}", businessSslResourceProvider);
            Map<String, String> providerConfiguration = new LinkedHashMap<>();
            providerConfiguration.put("secrets.provider", businessSslResourceProvider);
            providerConfiguration.put("secrets.projectId", configuration.evaluateToString("data.collector.sslBundle.gcs.projectId"));
            String gcsServiceAccountKeyPath = configuration.evaluateToString("data.collector.sslBundle.gcs.serviceAccountKeyPath");
            if (gcsServiceAccountKeyPath != null) {
                providerConfiguration.put("secrets.serviceAccountKeyPath", gcsServiceAccountKeyPath);
            }

            try (SecretManagerClient secretManagerClient = SecretManagerClient.create(providerConfiguration)) {
                return new ProvidedBusinessSSLResource() {

                    @Override
                    public String getType() {
                        return configuration.evaluateToString("data.collector.sslBundle.type");
                    }

                    @Override
                    public String bundleName() {
                        return secretManagerClient.readString("data.collector.sslBundle.name");
                    }

                    @Override
                    public char[] publicCertificate() {
                        return isPEM() ? safeConvertBytesToCharArrayAsUTF8(secretManagerClient.readBytes("data.collector.sslBundle.publicCertificate")) : new char[0];
                    }

                    @Override
                    public char[] privateCertificate() {
                        return isPEM() ? safeConvertBytesToCharArrayAsUTF8(secretManagerClient.readBytes("data.collector.sslBundle.privateCertificate")) : new char[0];
                    }

                    @Override
                    public byte[] archiveCertificate() {
                        return !isPEM() ? secretManagerClient.readBytes("data.collector.sslBundle.archiveCertificate") : new byte[0];
                    }

                    @Override
                    public char[] passphrase() {
                        return safeConvertBytesToCharArrayAsUTF8(secretManagerClient.readBytes("data.collector.sslBundle.passphrase"));
                    }
                };
            }
        };

        application = UndertowApplication.initializeUndertowApplication(configuration, testServerServicePort, hasBusinessSslResourceProvider ? new BusinessSSLResourceSupplier(sslResourceSupplier) : null);
    }

    public static TestServer create(DynamicConfiguration configuration) {
        return new TestServer(configuration, TestServerFactory.findFreePort(new SecureRandom(), 9000, 9499));
    }

    public void start() {
        application.start();
    }

    public void stop() {
        application.stop();
    }

    public String getTestServerHost() {
        return application.getHost();
    }

    public int getTestServerServicePort() {
        return testServerServicePort;
    }

    @Override
    public String testURL(String uri) {
        try {
            URL url = new URL("http", application.getHost(), application.getPort(), uri);
            return url.toExternalForm();
        } catch (MalformedURLException e) {
            throw new TestServerException(e);
        }
    }

    public DynamicConfiguration getConfiguration() {
        return configuration;
    }

    public UndertowApplication getApplication() {
        return application;
    }

}
