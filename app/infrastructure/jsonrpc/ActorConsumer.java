package infrastructure.jsonrpc;

import akka.actor.ActorRef;
import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import play.Logger;

import java.io.IOException;

public class ActorConsumer extends DefaultConsumer {
    private final ActorRef actor;
    private final Channel channel;

    public ActorConsumer(Channel channel, ActorRef actor) {
        super(channel);
        this.channel = channel;
        this.actor = actor;
    }

    @Override
    public void handleDelivery(String consumerTag, Envelope envelope, BasicProperties properties, byte[] body) throws IOException {
        final Protocol.InvokeRabbitReply message = new Protocol.InvokeRabbitReply(consumerTag, envelope, properties, new String(body));
        actor.tell(message, ActorRef.noSender());
    }

    @Override
    public String toString() {
        return "ActorConsumer {" +
                "actor: " + actor +
                ", channel: " + channel +
                '}';
    }
}