package infrastructure.jsonrpc;

import akka.actor.AbstractActorWithStash;
import akka.actor.Props;
import akka.japi.pf.ReceiveBuilder;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.tools.jsonrpc.JsonRpcException;
import infrastructure.RabbitConnection;
import infrastructure.json.JSONReader;
import infrastructure.json.JSONWriter;
import play.Logger;
import play.libs.Json;
import scala.PartialFunction;
import scala.runtime.BoxedUnit;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class JsonRpcActorClient extends AbstractActorWithStash {
    private final String REPLY_TO = "amq.rabbitmq.reply-to";
    private final Map<String, ActorConsumerHolder> calls = new HashMap<>();
    private final String exchange;
    private final String routingKey;
    private final int timeout;

    private Optional<Connection> connection;
    private Optional<Channel> channel;
    private int correlationId;
    private ServiceDescription serviceDescription;

    public static Props props(RabbitConnection rabbitConnection, String exchange, String routingKey, int timeout) {
        return Props.create(JsonRpcActorClient.class, rabbitConnection, exchange, routingKey, timeout);
    }

    public JsonRpcActorClient(RabbitConnection rabbitConnection, String exchange, String routingKey, int timeout) {
        this.exchange = exchange;
        this.routingKey = routingKey;
        this.timeout = timeout;

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
                    final Protocol.InvokeServiceDescriptionReply invokeServiceDescriptionReply =
                            (Protocol.InvokeServiceDescriptionReply)handleReply(reply, holder);
                    this.serviceDescription = invokeServiceDescriptionReply.serviceDescription;
                    context().become(started);
                    unstashAll();
                }).matchAny(any -> {
                    Logger.info("Stash: " + any);
                    stash();
                }).build()
        );
    }

    private PartialFunction<Object, BoxedUnit> started =
            ReceiveBuilder.match(Protocol.Invoke.class, invoke -> {
                publish(invoke);
            }).match(Protocol.InvokeRabbitReply.class, reply -> {
                Logger.debug("Reply: " + reply);
                final ActorConsumerHolder holder = calls.remove(reply.properties.getCorrelationId());
                final Protocol.InvokeReply invokeReply = handleReply(reply, holder);
                if(invokeReply.isError()) {
                    final Protocol.InvokeError err = (Protocol.InvokeError) invokeReply;
                    holder.actor.tell(err.error, self());
                } else {
                    final Protocol.InvokeObjectReply objectReply = (Protocol.InvokeObjectReply) invokeReply;
                    holder.actor.tell(objectReply.object, self());
                }
            }).matchAny(any -> unhandled(any)).build();

    private Protocol.InvokeReply handleReply(Protocol.InvokeRabbitReply reply, ActorConsumerHolder holder) {
        final Map<String, Object> map = (Map<String, Object>) (new JSONReader().read(reply.body));
        final Protocol.Invoke invoke = holder.invoke;
        return checkReply(invoke, reply, map);
    }

    private Protocol.InvokeReply checkReply(Protocol.Invoke invoke, Protocol.InvokeRabbitReply reply, Map<String, Object> replyMap) {
        if (replyMap.containsKey("error")) {
            @SuppressWarnings("unchecked")
            final Map<String, Object> map = (Map<String, Object>) replyMap.get("error");
            final JsonRpcException e = new JsonRpcException(map);
            Logger.error("checkReply error", e);
            return new Protocol.InvokeError(invoke, e);
        }
        if("system.describe".equals(invoke.method)){
            final Map<String, Object> map = (Map<String, Object>) replyMap.get("result");
            Logger.debug("Map: " + map);
            final ServiceDescription sd = new ServiceDescription(map);
            Logger.debug("SD: " + sd);
            return new Protocol.InvokeServiceDescriptionReply(invoke, sd);
        } else {
            Object result = null;
            try {
                result = getObjectResult(invoke, reply.body);
            } catch (Exception e) {
                Logger.debug("Error casting reply to object", e);
                return new Protocol.InvokeError(invoke, e);
            }
            return new Protocol.InvokeObjectReply(invoke, result); //TODO Add FQN to Reply
        }
    }

    private Object getObjectResult(Protocol.Invoke invoke, String body) throws IOException, ClassNotFoundException {
        final JsonNode root = Json.mapper().readTree(body);
        final JsonNode result = root.path("result");
        final String className = getMethodReturnClassName(invoke);
        if("java.lang.Void".equalsIgnoreCase(className)) return new Protocol.NullObject();
        if(className.contains("<")) return getGenericClass(result, className);
        return Json.mapper().treeToValue(result, Class.forName(className));
    }

    private Object getGenericClass(JsonNode result, String fullClassName) throws ClassNotFoundException {
        final String genericClassName = fullClassName.substring(0, fullClassName.indexOf("<"));
        final String className = fullClassName.substring(fullClassName.indexOf("<") + 1, fullClassName.length() - 1);
        final Class<?> genericClazz = Class.forName(genericClassName);
        final Class<?> clazz = Class.forName(className);
        final JavaType javaType = Json.mapper().getTypeFactory().constructParametricType(genericClazz, clazz);
        return Json.mapper().convertValue(result, javaType);
    }

    private String getMethodReturnClassName(Protocol.Invoke invoke) {
        final int paramLength = invoke.args != null ? invoke.args.length : 0;
        int methodId = this.serviceDescription.getProcedure(invoke.method, invoke.args).id;
        final ProcedureDescription proc = serviceDescription.getProcedure(methodId);
        return proc.getReturn();
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

    private String createCall(Protocol.Invoke invoke)
    {
        final HashMap<String, Object> request = new HashMap<>();
        request.put("id", null);  // FIXME: 3/7/16
        request.put("method", invoke.method);
        if(!invoke.method.startsWith("system"))
            request.put("method_id", this.serviceDescription.getProcedure(invoke.method, invoke.args).id);
        request.put("version", ServiceDescription.JSON_RPC_VERSION);
        request.put("params", (invoke.args == null) ? new Object[0] : invoke.args);
        return new JSONWriter().write(request);
    }

    private void publish(Protocol.Invoke invoke) throws IOException {
        Logger.debug("publish: " + invoke);
        final String message = createCall(invoke);
        correlationId++;
        final String replyId = "" + correlationId;
        final ActorConsumer consumer = new ActorConsumer(channel.get(), self());
        final ActorConsumerHolder holder = new ActorConsumerHolder(invoke, sender(), consumer, replyId);
        calls.put(replyId, holder);
        final String callbackQueueName = channel.get().queueDeclare().getQueue();
        final AMQP.BasicProperties props = new AMQP.BasicProperties.Builder().correlationId(replyId).replyTo(callbackQueueName).build();
        channel.get().basicConsume(callbackQueueName, true, consumer);
        channel.get().basicPublish(exchange, routingKey, props, message.getBytes());
    }
}