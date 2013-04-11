package infrastructure;

import java.io.IOException;

import akka.actor.ActorRef;

import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

public class CommandConsumer extends DefaultConsumer {
	private ActorRef actor;
	private Channel channel;

	public CommandConsumer(Channel channel) {
		super(channel);
	}
	
	public CommandConsumer(Channel channel, ActorRef actor) {
		super(channel);
		this.channel = channel;
		this.actor = actor;
	}

	@Override
	public void handleDelivery(String consumerTag, Envelope envelope, BasicProperties properties, byte[] body) throws IOException {
		long tag = envelope.getDeliveryTag();
		String bodyText = new String(body);
		actor.tell(bodyText, null);
		channel.basicAck(tag, false);
	}

}
