package no.ssb.dc.test.controller;

public class EventListItem {

    private final Integer id;
    private final Integer eventId;

    public EventListItem(Integer id, Integer eventId) {
        this.id = id;
        this.eventId = eventId;
    }

    public Integer getId() {
        return id;
    }

    public Integer getEventId() {
        return eventId;
    }
}
