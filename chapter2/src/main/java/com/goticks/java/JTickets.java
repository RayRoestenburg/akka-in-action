package com.goticks.java;


import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.List;

public class JTickets {
    private List<JTicket> tickets = new ArrayList<JTicket>();

    public JTickets(List<JTicket> tickets) {
        this.tickets = tickets;
    }

    // this needs to return an immutable copy
    // if this class would modify the list
    public List<JTicket> getTickets() {
        return ImmutableList.copyOf(tickets);
    }
}
