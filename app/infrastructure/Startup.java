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

import java.util.concurrent.CompletableFuture;

@Singleton
public class Startup {
    @Inject
    public Startup(ApplicationLifecycle lifecycle, ActorSystem system, RabbitConnection rabbitConnection, RabbitConfig rabbitConfig) {
        try {
            Connection connection = rabbitConnection.getConnection();
            Channel channel = connection.createChannel();
            RemoteCalculator calculatorkActor = TypedActor.get(system).typedActorOf(new TypedProps<RemoteCalculatorImpl>(RemoteCalculator.class, RemoteCalculatorImpl.class));
            JsonRpcServer calculator = new JsonRpcServer(channel, rabbitConfig.getRabbitRpcQueue(), RemoteCalculator.class, calculatorkActor);
            calculator.start();

            lifecycle.addStopHook(() -> {
                calculator.close();
                channel.close();
                connection.close();
                return CompletableFuture.completedFuture(null);
            });

        } catch (Exception e) {
            Logger.error("Startup", e);
        }
    }
}