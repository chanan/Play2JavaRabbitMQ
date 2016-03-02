package controllers;

import com.google.inject.Inject;
import play.*;
import play.mvc.*;

import remote.RemoteCalculator;
import services.CalculatorFactory;
import views.html.*;

public class Application extends Controller {
    private final CalculatorFactory calculatorFactory;

    @Inject
    public Application(CalculatorFactory calculatorFactory) {
        this.calculatorFactory = calculatorFactory;
    }

    public Result index() {
        final RemoteCalculator calculator = calculatorFactory.getCalculator();
        final int result = calculator.add(1, 2);
        Logger.info("Add 1 + 2 on a remote machine: " + result);
        return ok("Add 1 + 2 on a remote machine: " + result);
    }
}