package no.ssb.dc.test.controller;

public class EventListItem {

    private final Integer id;
    private final String eventId;

    public EventListItem(Integer id, String eventId) {
        this.id = id;
        this.eventId = eventId;
    }

    public Integer getId() {
        return id;
    }

    public String getEventId() {
        return eventId;
    }
}
