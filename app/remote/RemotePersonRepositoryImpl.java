package remote;

import com.google.inject.Singleton;
import models.Person;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@Singleton
public class RemotePersonRepositoryImpl implements RemotePersonRepository {
    private final List<Person> list = new ArrayList<>();

    public RemotePersonRepositoryImpl() {
        Person person1 = new Person();
        person1.setAge(43);
        person1.setName("John Doe");
        list.add(person1);

        Person person2 = new Person();
        person2.setAge(40);
        person2.setName("John Smith");
        list.add(person2);
    }

    @Override
    public CompletionStage<Void> increasePeopleAgeByOne() {
        list.forEach(person -> person.setAge(person.getAge() + 1));
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletionStage<Person> getPerson(int index) {
        return CompletableFuture.completedFuture(list.get(index));
    }

    @Override
    public CompletionStage<List<Person>> getPeople() {
        return CompletableFuture.completedFuture(list);
    }

    @Override
    public CompletionStage<List<Person>> addPerson(Person person) {
        list.add(person);
        return CompletableFuture.completedFuture(list);
    }

    @Override
    public CompletionStage<List<Person>> addPeople(List<Person> people) {
        list.addAll(people);
        return CompletableFuture.completedFuture(list);
    }

    @Override
    public CompletionStage<List<Person>> getPeople(List<Integer> personIds) {
        final List<Person> found = new ArrayList<>();
        personIds.stream().forEach(id -> found.add(list.get(id)));
        return CompletableFuture.completedFuture(found);
    }
}