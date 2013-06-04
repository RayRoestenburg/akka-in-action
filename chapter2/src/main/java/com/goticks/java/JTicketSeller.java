package com.goticks.java;

import akka.actor.UntypedActor;
import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.List;

public class JTicketSeller extends UntypedActor {
    // this is not immutable. you cannot return tickets in a message
    // but need to make an immutable copy.
    private List<JTicket> tickets = new ArrayList<JTicket>();

    public void onReceive(Object message) throws Exception {
        // big if else, usage of instanceof, limited 'pattern matching'
        if (message instanceof JGetEvents) {
            getSender().tell(tickets.size(), getSelf());

        } else if (message instanceof JTickets) {
            JTickets ticketsMessage = (JTickets) message;
            List<JTicket> newTickets = ticketsMessage.getTickets();
            tickets.addAll(newTickets);

        } else if(message instanceof JBuyTicket) {
          if(tickets.isEmpty()){
              getSender().tell(new JSoldOut());
              getSelf().tell(akka.actor.PoisonPill.getInstance(), null);
          }
          JTicket ticket = tickets.get(0);
          if(ticket != null){
              tickets.remove(ticket);
              getSelf().tell(ticket, getSelf());
          }
        } else if(message instanceof JGetTickets) {
          // if you want to return tickets you have to make an immutable copy
          getSender().tell(ImmutableList.copyOf(tickets));
        } else
          unhandled(message);
    }
}