package io.descoped.dc.test.server;

public class TestServerException extends RuntimeException {

    public TestServerException(String message) {
        super(message);
    }

    public TestServerException(Throwable cause) {
        super(cause);
    }
}
