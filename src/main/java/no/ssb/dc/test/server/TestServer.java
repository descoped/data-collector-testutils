package no.ssb.dc.test.server;

import no.ssb.config.DynamicConfiguration;
import no.ssb.dc.api.application.Application;

import java.net.MalformedURLException;
import java.net.URL;

public class TestServer implements TestUriResolver {

    private final DynamicConfiguration configuration;

    private final Application application;

    private final int testServerServicePort;

    public TestServer(DynamicConfiguration configuration, int testServerServicePort) {
        this.configuration = configuration;
        this.testServerServicePort = testServerServicePort;
        application = Application.create(configuration, testServerServicePort);
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
            throw new RuntimeException(e);
        }
    }

    public DynamicConfiguration getConfiguration() {
        return configuration;
    }

    public Application getApplication() {
        return application;
    }

}
