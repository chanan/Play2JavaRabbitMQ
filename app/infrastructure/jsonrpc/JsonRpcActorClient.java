package infrastructure.jsonrpc;

import akka.actor.AbstractActorWithStash;
import akka.actor.Props;
import akka.japi.pf.ReceiveBuilder;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import infrastructure.RabbitConnection;
import infrastructure.models.ActorConsumerHolder;
import infrastructure.models.Procedure;
import infrastructure.models.Protocol;
import infrastructure.models.ServiceDescriptor;
import play.Logger;
import play.libs.Json;
import scala.PartialFunction;
import scala.concurrent.duration.Duration;
import scala.runtime.BoxedUnit;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.TemporalUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class JsonRpcActorClient extends AbstractActorWithStash {
    private final Map<String, ActorConsumerHolder> calls = new HashMap<>();
    private final String exchange;
    private final String routingKey;
    private final int timeout;
    private java.time.Duration timeoutDuration = null;
    private final JsonRpcService jsonRpcService;

    private Optional<Connection> connection;
    private Optional<Channel> channel;
    private int correlationId;
    private ServiceDescriptor serviceDescriptor;

    public static Props props(RabbitConnection rabbitConnection, String exchange, String routingKey, int timeout, JsonRpcService jsonRpcService) {
        return Props.create(JsonRpcActorClient.class, rabbitConnection, exchange, routingKey, timeout, jsonRpcService);
    }

    public JsonRpcActorClient(RabbitConnection rabbitConnection, String exchange, String routingKey, int timeout, JsonRpcService jsonRpcService) {
        this.exchange = exchange;
        this.routingKey = routingKey;
        this.timeout = timeout;
        this.timeoutDuration = java.time.Duration.ofMillis(timeout);
        this.jsonRpcService = jsonRpcService;

        try {
            connection = Optional.of(rabbitConnection.getConnection());
            channel = Optional.of(connection.get().createChannel());
        } catch (Exception e) {
            Logger.error("Error getting channel", e);
        }

        try {
            final Protocol.Invoke describe = new Protocol.Invoke("system.describe", null);
            publish(describe);
        } catch (IOException e) {
            Logger.error("Error in sending system.describe", e);
        }

        receive(
                ReceiveBuilder.match(Protocol.InvokeRabbitReply.class, reply -> {
                    final ActorConsumerHolder holder = calls.remove(reply.properties.getCorrelationId());
                    final Protocol.InvokeReply invokeReply = handleReply(reply, holder);
                    if(invokeReply.getReplyType() == Protocol.InvokeReplyType.SERVICE_DESCRIPTOR) {
                        this.serviceDescriptor = invokeReply.getServiceDescriptor();
                        context().become(started);
                        unstashAll();
                        if(timeout != -1) {
                            context().system().scheduler().schedule(Duration.create(50, TimeUnit.MILLISECONDS),
                                    Duration.create(timeout, TimeUnit.MILLISECONDS),
                                    self(), "tick", context().dispatcher(), self());
                        }
                    }
                }).matchAny(any -> {
                    stash();
                }).build()
        );
    }

    private PartialFunction<Object, BoxedUnit> started =
            ReceiveBuilder.match(Protocol.Invoke.class, invoke -> {
                publish(invoke);
            }).match(Protocol.InvokeRabbitReply.class, reply -> {
                Logger.debug("Reply: " + reply);
                if(calls.containsKey(reply.properties.getCorrelationId())){
                    final ActorConsumerHolder holder = calls.remove(reply.properties.getCorrelationId());
                    final Protocol.InvokeReply invokeReply = handleReply(reply, holder);
                    if(invokeReply.getReplyType() == Protocol.InvokeReplyType.ERROR) {
                        holder.actor.tell(invokeReply.getError(), self());
                    } else {
                        holder.actor.tell(invokeReply.getResult(), self());
                    }
                }
            }).matchEquals("tick", t -> {
                final List<String> evict = new ArrayList<>();
                calls.entrySet().stream().forEach(entry -> {
                    Logger.debug("Duration: " + java.time.Duration.between(Instant.now(), entry.getValue().startTime).compareTo(timeoutDuration));
                    if(java.time.Duration.between(Instant.now(), entry.getValue().startTime).compareTo(timeoutDuration) < 0) {
                        evict.add(entry.getKey());
                    }
                });
                evict.stream().forEach(key -> {
                    final ActorConsumerHolder holder = calls.remove(key);
                    holder.actor.tell(new TimeoutException(), self());
                });
            }).matchAny(any -> unhandled(any)).build();

    private Protocol.InvokeReply handleReply(Protocol.InvokeRabbitReply reply, ActorConsumerHolder holder) {
        final JsonNode node = Json.parse(reply.body);
        final Protocol.InvokeReply invokeReply = Json.fromJson(node, Protocol.InvokeReply.class);
        final Protocol.Invoke invoke = holder.invoke;
        return checkReply(invoke, invokeReply, node);
    }

    private Protocol.InvokeReply checkReply(Protocol.Invoke invoke, Protocol.InvokeReply invokeReply, JsonNode node) {
        if (invokeReply.getReplyType() == Protocol.InvokeReplyType.ERROR) {
            Logger.error("checkReply error", invokeReply.getError());
            return invokeReply;
        }
        if(invokeReply.getReplyType() == Protocol.InvokeReplyType.SERVICE_DESCRIPTOR) {
            return invokeReply;
        } else {
            Object result;
            try {
                result = getObjectResult(invoke, invokeReply.getResult());
            } catch (Exception e) {
                Logger.error("Error casting reply to object", e);
                return new Protocol.InvokeReply(invoke, Protocol.InvokeReplyType.ERROR, e, null, null);
            }
            return new Protocol.InvokeReply(invoke, Protocol.InvokeReplyType.RESULT, null, null, result);
        }
    }

    private Object getObjectResult(Protocol.Invoke invoke, Object object) throws IOException, ClassNotFoundException {
        final String className = getMethodReturnClassName(invoke);
        if("java.lang.Void".equalsIgnoreCase(className)) return new Protocol.NullObject();
        final JavaType javaType = getJavaType(className);
        return Json.mapper().convertValue(object, javaType);
    }

    private JavaType getJavaType(String fullClassName) throws ClassNotFoundException {
        if(fullClassName.contains("<")) {
            final String genericClassName = fullClassName.substring(0, fullClassName.indexOf("<"));
            final String className = fullClassName.substring(fullClassName.indexOf("<") + 1, fullClassName.length() - 1);
            final Class<?> genericClazz = Class.forName(genericClassName);
            final Class<?> clazz = Class.forName(className);
            return Json.mapper().getTypeFactory().constructParametricType(genericClazz, clazz);
        } else {
            return Json.mapper().getTypeFactory().constructType(Class.forName(fullClassName));
        }
    }

    private String getMethodReturnClassName(Protocol.Invoke invoke) {
        final Procedure proc = jsonRpcService.findProcedure(serviceDescriptor, invoke.method, invoke.args).get();
        return proc.getReturnType();
    }

    @Override
    public void postStop() {
        channel.ifPresent(c -> {
            try {
                c.close();
                connection.get().close();
            } catch (Exception e) {
                Logger.error(e.toString());
            }
        });
    }

    private Protocol.RabbitMessage createCall(Protocol.Invoke invoke)
    {
        if(invoke.method.startsWith("system.")) {
            return new Protocol.RabbitMessage(invoke.method, new Object[0], null);
        } else {
            final Procedure proc = jsonRpcService.findProcedure(serviceDescriptor, invoke.method, invoke.args).get();
            final int methodId = proc.getId();
            return new Protocol.RabbitMessage(invoke.method, (invoke.args == null) ? new Object[0] : invoke.args, methodId);
        }
    }

    private void publish(Protocol.Invoke invoke) throws IOException {
        Logger.debug("publish: " + invoke);
        final Protocol.RabbitMessage message = createCall(invoke);
        correlationId++;
        final String replyId = "" + correlationId;
        final ActorConsumer consumer = new ActorConsumer(channel.get(), self());
        final ActorConsumerHolder holder = new ActorConsumerHolder(invoke, sender(), consumer, replyId);
        calls.put(replyId, holder);
        final String callbackQueueName = channel.get().queueDeclare().getQueue();
        final AMQP.BasicProperties props = new AMQP.BasicProperties.Builder().correlationId(replyId).replyTo(callbackQueueName).build();
        channel.get().basicConsume(callbackQueueName, true, consumer);
        channel.get().basicPublish(exchange, routingKey, props, Json.toJson(message).toString().getBytes());
    }
}