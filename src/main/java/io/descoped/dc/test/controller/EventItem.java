package io.descoped.dc.test.controller;

public class EventItem {

    public static final int EVENT_ID_OFFSET = 1000;

    private final String eventId;

    public EventItem(String eventId) {
        this.eventId = eventId;
    }

    public String getEventId() {
        return eventId;
    }
}
