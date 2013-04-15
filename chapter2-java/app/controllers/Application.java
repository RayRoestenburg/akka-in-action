package controllers;

import play.mvc.Controller;
import play.mvc.Result;
import views.html.index;

public class Application extends Controller {
  
    public static Result index() {
        return ok(index.render("Your new application is ready."));
    }
    
    public static Result newEvent() {
    	return ok(index.render("created new event"));
    }
  
    public static Result currentEvents() {
    	return ok("current events: ");
    }
  
}
