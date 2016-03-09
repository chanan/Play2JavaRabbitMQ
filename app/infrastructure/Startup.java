package infrastructure;

import akka.actor.ActorSystem;
import akka.actor.TypedActor;
import akka.actor.TypedProps;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import infrastructure.jsonrpc.JsonRpcServer;
import play.Logger;
import play.inject.ApplicationLifecycle;
import remote.RemoteCalculator;
import remote.RemoteCalculatorImpl;
import remote.RemotePersonRepository;
import remote.RemotePersonRepositoryImpl;

import java.util.concurrent.CompletableFuture;

@Singleton
public class Startup {
    @Inject
    public Startup(ApplicationLifecycle lifecycle, ActorSystem system, RabbitConnection rabbitConnection, RabbitConfig rabbitConfig) {
        try {
            Connection connection = rabbitConnection.getConnection();

            //Calculator
            final Channel channel = connection.createChannel();
            final RemoteCalculator calculatorActor = TypedActor.get(system).typedActorOf(new TypedProps<RemoteCalculatorImpl>(RemoteCalculator.class, RemoteCalculatorImpl.class));
            final JsonRpcServer calculator = new JsonRpcServer(channel, rabbitConfig.getRabbitRpcQueue(), RemoteCalculator.class, calculatorActor);
            calculator.start();

            //Person Repo

            final Channel channelRepo = connection.createChannel();
            final RemotePersonRepository personRepoActor = TypedActor.get(system).typedActorOf(new TypedProps<>(RemotePersonRepository.class, RemotePersonRepositoryImpl.class));
            final JsonRpcServer personRepo = new JsonRpcServer(channelRepo, rabbitConfig.getPersonRepoQueue(), RemotePersonRepository.class, personRepoActor);
            personRepo.start();

            lifecycle.addStopHook(() -> {
                calculator.close();
                channel.close();

                personRepo.close();
                channelRepo.close();

                connection.close();
                return CompletableFuture.completedFuture(null);
            });

        } catch (Exception e) {
            Logger.error("Startup", e);
        }
    }
}