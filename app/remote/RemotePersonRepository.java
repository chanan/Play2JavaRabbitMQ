package remote;

import models.Person;

import java.util.List;
import java.util.concurrent.CompletionStage;

public interface RemotePersonRepository {
    //Test return types
    CompletionStage<Void> increasePeopleAgeByOne();
    CompletionStage<Person> getPerson(int index);
    CompletionStage<List<Person>> getPeople();

    //Test arg types
    CompletionStage<List<Person>> addPerson(Person person);
    CompletionStage<List<Person>> addPeople(List<Person> people);
    CompletionStage<List<Person>> getPeople(List<Integer> personIds);
}