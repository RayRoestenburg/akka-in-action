package controllers;

import static akka.pattern.Patterns.ask;

import java.util.ArrayList;
import java.util.List;

import models.Event;

import org.codehaus.jackson.JsonNode;

import play.libs.Akka;
import play.libs.F.Function;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import views.html.index;
import actors.TicketingAgent;
import akka.actor.ActorRef;
import akka.actor.Props;

public class Application extends Controller {
	static ActorRef myActor = Akka.system().actorOf(new Props(TicketingAgent.class));

	public static Result index() {
		return ok(index.render("Your new application is ready."));
	}

	public static Result newEvent(String name) {
		return ok("created new event: " + name + " with " + 100 + "seats.");
	}

	public static Result currentEvents() {
		List events = new ArrayList<Event>();
		Event event = new Event();
		event.setName("test event");
		event.setNumberOfTickets(5);
		events.add(event);
		event = new Event();
		event.setName("test event 2");
		event.setNumberOfTickets(100);
		events.add(event);
		JsonNode eventsJson = Json.toJson(events);
		return ok(eventsJson.toString()).as("application/json");
	}

	public static Result ping() {

		return async(Akka.asPromise(ask(myActor, "hello", 1000)).map(
				new Function<Object, Result>() {
					public Result apply(Object response) {
						return ok(response.toString());
					}
				}));
	}

}
