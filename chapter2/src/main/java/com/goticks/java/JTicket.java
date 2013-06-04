package com.goticks.java;

import java.io.Serializable;
// and this is just a pojo with two fields, what if it contains lists, maps, etc
public class JTicket implements Serializable, Comparable<JTicket> {
    private String event;
    private int nr;

    public JTicket(String event, int nr) {
        this.event = event;
        this.nr = nr;
    }

    public String getEvent() {
        return event;
    }

    public int getNr() {
        return nr;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        JTicket jTicket = (JTicket) o;

        if (nr != jTicket.nr) return false;
        if (event != null ? !event.equals(jTicket.event) : jTicket.event != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = event != null ? event.hashCode() : 0;
        result = 31 * result + nr;
        return result;
    }

    @Override
    public String toString() {
        return "JTicket{" +
                "event='" + event + '\'' +
                ", nr=" + nr +
                '}';
    }

    @Override
    public int compareTo(JTicket o) {
        if(o == null) return 1;
        String comparable = event + nr;
        return comparable.compareTo(o.event + o.nr);
    }

    public JTicket copy(String replaceEvent, Integer replaceNr) {
      return new JTicket(replaceEvent == null ? event  : replaceEvent,
                         replaceNr == null ? nr : replaceNr);
    }
}
