package infrastructure.models;

import akka.actor.ActorRef;
import infrastructure.jsonrpc.ActorConsumer;

public class ActorConsumerHolder {
    public final Protocol.Invoke invoke;
    public final ActorRef actor;
    public final ActorConsumer consumer;
    public final String replyId;

    public ActorConsumerHolder(Protocol.Invoke invoke, ActorRef actor, ActorConsumer consumer, String replyId) {
        this.invoke = invoke;
        this.actor = actor;
        this.consumer = consumer;
        this.replyId = replyId;
    }

    @Override
    public String toString() {
        return "ActorConsumerHolder {" +
                "invoke: " + invoke +
                ", actor: " + actor +
                ", consumer: " + consumer +
                ", replyId: '" + replyId + '\'' +
                '}';
    }
}