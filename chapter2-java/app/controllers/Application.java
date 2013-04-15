package controllers;

import play.mvc.Controller;
import play.mvc.Result;
import views.html.index;
import models.Event;
import java.util.List;
import java.util.ArrayList;
import com.google.gson.Gson;

public class Application extends Controller {
  
    public static Result index() {
        return ok(index.render("Your new application is ready."));
    }
    
    public static Result newEvent() {
    	return ok(index.render("created new event"));
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
    	Gson gson = new Gson();
    	String eventsJson=gson.toJson(events);
    	return ok(eventsJson).as("application/json");
    }
  
}
