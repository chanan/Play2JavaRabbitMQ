package controllers;

import play.*;
import play.mvc.*;
import remote.RemoteCalculator;
import services.CalculatorFactory;

import views.html.*;

public class Application extends Controller {
  
    public static Result index() {
    	RemoteCalculator calculator = CalculatorFactory.getCalculator();
		Logger.info("Add 1 + 2 on a remote machine: " + calculator.add(1, 2));
        return ok(index.render("Your new application is ready."));
    }
  
}
