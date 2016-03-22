package jsonrpc.models;

import akka.actor.ActorRef;
import jsonrpc.ActorConsumer;

import java.time.Instant;

public class ActorConsumerHolder {
    public final Protocol.Invoke invoke;
    public final ActorRef actor;
    public final ActorConsumer consumer;
    public final String replyId;
    public final Instant startTime;

    public ActorConsumerHolder(Protocol.Invoke invoke, ActorRef actor, ActorConsumer consumer, String replyId) {
        this.invoke = invoke;
        this.actor = actor;
        this.consumer = consumer;
        this.replyId = replyId;
        this.startTime = Instant.now();
    }

    @Override
    public String toString() {
        return "ActorConsumerHolder {" +
                "invoke: {" + invoke + '}' +
                ", actor: {" + actor + '}' +
                ", consumer: {" + consumer + '}' +
                ", replyId: \"" + replyId + '"' +
                ", startTime: \"" + startTime + '"' +
                '}';
    }
}