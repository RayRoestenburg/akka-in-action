package actors;

import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public class TicketingAgent extends UntypedActor {
	LoggingAdapter log = Logging.getLogger(getContext().system(), this);

	@Override
	public void onReceive(Object message) throws Exception {
		if (message instanceof String){
			log.info("hello from inside TicketingAgent...");
		} else {
			log.info("message was not string...");
			unhandled(message);
		}
	}

}
