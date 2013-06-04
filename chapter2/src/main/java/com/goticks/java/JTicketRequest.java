package com.goticks.java;

public class JTicketRequest {
    private String event;

    public JTicketRequest(String event) {
        this.event = event;
    }

    public String getEvent() {
        return event;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        JTicketRequest that = (JTicketRequest) o;

        if (event != null ? !event.equals(that.event) : that.event != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return event != null ? event.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "JTicketRequest{" +
                "event='" + event + '\'' +
                '}';
    }
}
