package no.ssb.dc.test.server;

import no.ssb.config.DynamicConfiguration;
import no.ssb.dc.application.server.UndertowApplication;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.SecureRandom;

public class TestServer implements TestUriResolver {

    private final DynamicConfiguration configuration;

    private final UndertowApplication application;

    private final int testServerServicePort;

    public TestServer(DynamicConfiguration configuration, int testServerServicePort) {
        this.configuration = configuration;
        this.testServerServicePort = testServerServicePort;
        application = UndertowApplication.initializeUndertowApplication(configuration, testServerServicePort, null);
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
