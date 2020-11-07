package no.ssb.dc.test.server;

import no.ssb.config.DynamicConfiguration;
import no.ssb.dc.application.server.UndertowApplication;
import no.ssb.dc.application.ssl.BusinessSSLResourceSupplier;
import no.ssb.dc.application.ssl.SecretManagerSSLResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.SecureRandom;
import java.util.function.Supplier;

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
        Supplier<SecretManagerSSLResource> sslResourceSupplier = () -> new SecretManagerSSLResource(configuration);

        application = UndertowApplication.initializeUndertowApplication(configuration, testServerServicePort,
                businessSslResourceProvider != null ? new BusinessSSLResourceSupplier(sslResourceSupplier) : null);
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
