package jsonrpc;

import akka.actor.ActorRef;

public interface JsonRpcFactory {
    <T> T createClient(Class<T> clazz, String exchange, String routingKey);

    <T> T createClient(Class<T> clazz, String exchange, String routingKey, int timeout);

    ActorRef createServer(String queueName, Class<?> interfaceClass, Class<?> instanceClass);
}
