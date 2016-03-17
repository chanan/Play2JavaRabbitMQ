package infrastructure.jsonrpc;

import akka.actor.AbstractActor;
import akka.actor.Props;
import akka.japi.pf.ReceiveBuilder;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import infrastructure.RabbitConnection;
import infrastructure.models.Procedure;
import infrastructure.models.Protocol;
import infrastructure.models.ServiceDescriptor;
import play.Logger;
import play.libs.Json;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class JsonRpcActorServer extends AbstractActor {
    private final RabbitConnection rabbitConnection;
    private final String queueName;
    private final Class<?> interfaceClass;
    private final Object interfaceInstance;
    private final ServiceDescriptor serviceDescriptor;
    private final JsonRpcService jsonRpcService;

    private Optional<Connection> connection;
    private Optional<Channel> channel;
    private Optional<ActorConsumer> actorConsumer;



    public static Props props(RabbitConnection rabbitConnection, String queueName, Class<?> interfaceClass, Object interfaceInstance, JsonRpcService jsonRpcService) {
        return Props.create(JsonRpcActorServer.class, rabbitConnection, queueName, interfaceClass, interfaceInstance, jsonRpcService);
    }

    public JsonRpcActorServer(RabbitConnection rabbitConnection, String queueName, Class<?> interfaceClass, Object interfaceInstance, JsonRpcService jsonRpcService) {
        this.rabbitConnection = rabbitConnection;
        this.queueName = queueName;
        this.interfaceClass = interfaceClass;
        this.interfaceInstance = interfaceInstance;
        this.jsonRpcService = jsonRpcService;

        try {
            connection = Optional.of(rabbitConnection.getConnection());
            channel = Optional.of(connection.get().createChannel());
            actorConsumer = Optional.of(new ActorConsumer(channel.get(), self()));
            channel.get().basicConsume(queueName, true, actorConsumer.get());
        } catch (Exception e) {
            Logger.error("Error getting channel", e);
        }


        serviceDescriptor = jsonRpcService.createServiceDescriptor(interfaceClass);

        receive(
                ReceiveBuilder.match(Protocol.InvokeRabbitReply.class, invokeRabbitReply -> {
                    Logger.debug("Server request: " + invokeRabbitReply);
                    handleRequest(invokeRabbitReply);
                }).build()
        );
    }

    private void handleRequest(Protocol.InvokeRabbitReply invokeRabbitReply) throws IOException {
        final JsonNode root = Json.parse(invokeRabbitReply.body);
        final Protocol.RabbitMessage message = Json.fromJson(root, Protocol.RabbitMessage.class);
        final Protocol.InvokeReply result = doCall(message);
        replyToClient(invokeRabbitReply, result);
    }

    private void replyToClient(Protocol.InvokeRabbitReply invokeRabbitReply, Protocol.InvokeReply result) throws IOException {
        final String correlationId = invokeRabbitReply.properties.getCorrelationId();
        final String replyTo = invokeRabbitReply.properties.getReplyTo();
        final AMQP.BasicProperties replyProperties = new AMQP.BasicProperties.Builder().correlationId(correlationId).build();
        final byte[] replyBody = Json.toJson(result).toString().getBytes();
        Logger.debug("Server side about to reply: " + new String(replyBody));
        channel.get().basicPublish("", replyTo, replyProperties, replyBody);
    }

    private Protocol.InvokeReply doCall(Protocol.RabbitMessage message) {
        if (message.getMethod().equals("system.describe")) {
            return new Protocol.InvokeReply(null, Protocol.InvokeReplyType.SERVICE_DESCRIPTOR, null, serviceDescriptor, null);
        } else if (message.getMethod().startsWith("system.")) {
            return new Protocol.InvokeReply(null, Protocol.InvokeReplyType.ERROR, new IllegalAccessException("System methods forbidden"), null, null);
        } else {
            Object result;
            try {
                final Optional<Method> m = matchingMethod(message.getMethodId());
                if(!m.isPresent()) return new Protocol.InvokeReply(null, Protocol.InvokeReplyType.ERROR, new IllegalArgumentException("Method not found"), null, null);
                final Object[] params = getObjectParams(m.get(), message.getArgs());
                CompletableFuture<Object> futureResult = (CompletableFuture<Object>)m.get().invoke(interfaceInstance, params);
                result = futureResult.get();
                Logger.debug("Server method result: " + result);
            } catch (Throwable t) {
                return new Protocol.InvokeReply(null, Protocol.InvokeReplyType.ERROR, new Exception("Internal Server Error", t), null, null);
            }
            return new Protocol.InvokeReply(null, Protocol.InvokeReplyType.RESULT, null, null, result);
        }
    }

    private Optional<Method> matchingMethod(int id) {
        final Optional<Procedure> optionalProcedure = jsonRpcService.findProcedure(serviceDescriptor, id);
        if(!optionalProcedure.isPresent()) return Optional.empty();
    return Optional.of(optionalProcedure.get().getInternalMethod());
    }

    private Object[] getObjectParams(Method method, Object[] params) throws IOException, ClassNotFoundException {
        List<Object> list = new ArrayList<>();
        int i = 0;
        final Type[] types = method.getGenericParameterTypes();
        for(Object param : params) {
            String className = types[i].getTypeName();
            if (className.contains("<")) {
                final String genericClassName = className.substring(0, className.indexOf("<"));
                final String typeName = className.substring(className.indexOf("<") + 1, className.length() - 1);
                final Class<?> genericClazz = getClassForName(genericClassName);
                final Class<?> clazz = getClassForName(typeName);
                final JavaType javaType = Json.mapper().getTypeFactory().constructParametricType(genericClazz, clazz);
                final Object obj = Json.mapper().convertValue(param, javaType);
                list.add(obj);
            } else {
                final Class<?> clazz = getClassForName(className);
                final Object obj = Json.mapper().convertValue(param, clazz);
                list.add(obj);
            }
            i++;
        }
        return list.toArray();
    }

    private Class<?> getClassForName(String className) throws ClassNotFoundException {
        String temp;
        if (className.contains("<")) temp = className.substring(0, className.indexOf("<"));
        else temp = className;
        switch (temp.toLowerCase()) {
            case "byte":
                return byte.class;
            case "short":
                return short.class;
            case "int":
                return int.class;
            case "long":
                return long.class;
            case "float":
                return float.class;
            case "double":
                return double.class;
            case "char":
                return char.class;
            case "boolean":
                return boolean.class;
            default:
                return Class.forName(temp);
        }
    }

    @Override
    public void postStop() throws Exception {
        channel.ifPresent(c -> {
            try {
                c.close();
                connection.get().close();
            } catch (Exception e) {
                Logger.error(e.toString());
            }
        });
    }
}