package infrastructure.jsonrpc;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.TypedActor;
import akka.actor.TypedProps;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.rabbitmq.client.Channel;
import infrastructure.RabbitConnection;
import remote.RemotePersonRepository;
import remote.RemotePersonRepositoryImpl;

import java.lang.reflect.Proxy;

@Singleton
public class JsonRpcFactory {
    private final RabbitConnection rabbitConnection;
    private final ActorSystem system;
    private final JsonRpcService jsonRpcService;

    @Inject
    public JsonRpcFactory(RabbitConnection rabbitConnection, ActorSystem system, JsonRpcService jsonRpcService) {
        this.rabbitConnection = rabbitConnection;
        this.system = system;
        this.jsonRpcService = jsonRpcService;
    }

    public <T> T createClient(Class<T> clazz, String exchange, String routingKey) {
        final ActorRef actor = system.actorOf(JsonRpcActorClient.props(rabbitConnection, exchange, routingKey, -1, jsonRpcService));
        final SenderProxy proxy = new SenderProxy(system, actor);
        return (T) Proxy.newProxyInstance(clazz.getClassLoader(), new Class[]{clazz}, proxy);
    }

    public <T> T createClient(Class<T> clazz, String exchange, String routingKey, int timeout) {
        final ActorRef actor = system.actorOf(JsonRpcActorClient.props(rabbitConnection, exchange, routingKey, timeout, jsonRpcService));
        final SenderProxy proxy = new SenderProxy(system, actor);
        return (T) Proxy.newProxyInstance(clazz.getClassLoader(), new Class[]{clazz}, proxy);
    }

    public ActorRef createServer(String queueName, Class<?> interfaceClass, Class<?> instanceClass) {
        final Object actor = TypedActor.get(system).typedActorOf(new TypedProps(interfaceClass, instanceClass));
        final ActorRef server = system.actorOf(JsonRpcActorServer.props(rabbitConnection, queueName, interfaceClass, actor, jsonRpcService));
        return server;
    }
}