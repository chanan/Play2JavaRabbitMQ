package controllers;

import com.google.inject.Inject;
import jsonrpc.JsonRpcFactory;
import jsonrpc.RabbitConfig;
import models.Person;
import play.Configuration;
import play.Logger;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import remote.RemoteCalculator;
import remote.RemotePersonRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionStage;

public class Application extends Controller {
    private final RemoteCalculator remoteCalculator;
    private final RemotePersonRepository remotePersonRepo;

    @Inject
    public Application(JsonRpcFactory jsonRpcFactory, Configuration config) {
        this.remoteCalculator = jsonRpcFactory.createClient(RemoteCalculator.class, "", config.getString("rabbitmq.rpcqueue"), 10000);
        this.remotePersonRepo = jsonRpcFactory.createClient(RemotePersonRepository.class, "", config.getString("rabbitmq.personRepoQueue"));
    }

    public Result index() {
        return ok(views.html.index.render());
    }

    public CompletionStage<Result> add(int num1, int num2) {
        return remoteCalculator.add(num1, num2).thenApply(result -> {
            Logger.info("Add " + num1 + " + " + num2 + " on a remote machine: " + result);
            return ok("Add " + num1 + " + " + num2 + " on a remote machine: " + result);
        });
    }

    public CompletionStage<Result> longOperation() {
        return remoteCalculator.longOperation(1000).thenApply(result -> {
            Logger.info("Long Operation done");
            return ok("Long Operation done");
        });
    }

    public CompletionStage<Result> getPerson(int index) {
        return remotePersonRepo.getPerson(index).thenApply(result -> {
            Logger.info("Person: " + result);
            return ok(Json.toJson(result));
        });
    }

    public CompletionStage<Result> getPersonList() {
        return remotePersonRepo.getPeople().thenApply(result -> {
            Logger.info("People: " + result);
            return ok(Json.toJson(result));
        });
    }

    public CompletionStage<Result> increaseAge() {
        return remotePersonRepo.increasePeopleAgeByOne().thenApply(result -> {
            Logger.info("Age increased");
            return ok("Age increased");
        });
    }

    public CompletionStage<Result> addPerson() {
        final Person person = new Person();
        person.setAge(25);
        person.setName("Slim Shady");
        return remotePersonRepo.addPerson(person).thenApply(result -> {
            Logger.info("People: "  + result);
            return ok(Json.toJson(result));
        });
    }

    public CompletionStage<Result> addPeople() {
        final Person person1 = new Person();
        person1.setAge(27);
        person1.setName("George Washington");
        final Person person2 = new Person();
        person2.setAge(35);
        person2.setName("Albert Einstein");
        final List<Person> people = new ArrayList<>();
        people.add(person1);
        people.add(person2);
        return remotePersonRepo.addPeople(people).thenApply(result -> {
            Logger.info("People: "  + result);
            return ok(Json.toJson(result));
        });
    }

    public CompletionStage<Result> getPeopleByIds() {
        final List<Integer> ids = new ArrayList<>();
        ids.add(1);
        ids.add(3);
        return remotePersonRepo.getPeople(ids).thenApply(result -> {
            Logger.info("People: " + result);
            return ok(Json.toJson(result));
        });
    }
}