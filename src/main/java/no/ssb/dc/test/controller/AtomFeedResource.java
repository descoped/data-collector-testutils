package no.ssb.dc.test.controller;

import io.undertow.server.HttpServerExchange;


/**
 * The mock feed simulates an atom feed and only support forward direction of data.
 *
 *
 */
class AtomFeedResource extends AbstractResource {

    AtomFeedResource() {
    }

    @Override
    void handle(HttpServerExchange exchange) {
        String marker = getQueryParam(exchange.getQueryParameters(), "marker", "last");


    }
}
