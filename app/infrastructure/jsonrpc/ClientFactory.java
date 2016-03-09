package infrastructure.jsonrpc;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import infrastructure.RabbitConnection;

import java.lang.reflect.Proxy;

@Singleton
public class ClientFactory {
    private final RabbitConnection rabbitConnection;
    private final ActorSystem system;

    @Inject
    public ClientFactory(RabbitConnection rabbitConnection, ActorSystem system) {
        this.rabbitConnection = rabbitConnection;
        this.system = system;
    }

    public <T> T createClient(Class<T> clazz, String exchange, String routingKey) {
        final ActorRef actor = system.actorOf(JsonRpcActorClient.props(rabbitConnection, exchange, routingKey, -1));
        final SenderProxy proxy = new SenderProxy(system, actor);
        return (T) Proxy.newProxyInstance(clazz.getClassLoader(), new Class[] { clazz }, proxy);
    }
}