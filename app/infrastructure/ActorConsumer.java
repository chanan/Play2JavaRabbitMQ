package infrastructure;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import play.Logger;

import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import akka.actor.ActorRef;
import akka.pattern.Patterns;
import akka.util.Timeout;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.AMQP.BasicProperties;

public class ActorConsumer extends DefaultConsumer {
	private ActorRef actor;
	private Channel channel;

	public ActorConsumer(Channel channel) {
		super(channel);
	}
	
	public ActorConsumer(Channel channel, ActorRef actor) {
		super(channel);
		this.channel = channel;
		this.actor = actor;
	}

	@Override
	public void handleDelivery(String consumerTag, Envelope envelope, BasicProperties properties, byte[] body) throws IOException {
		long tag = envelope.getDeliveryTag();
		String bodyText = new String(body);
		//Use ask here as well so we can send back an ack to MQ
		Timeout timeout = new Timeout(Duration.create(5, TimeUnit.SECONDS));
		Future<Object> oReply = Patterns.ask(actor, bodyText, timeout);
		try {
			Await.result(oReply, timeout.duration()); //The actual result doesn't matter in this case we only care that the operation completed.
			channel.basicAck(tag, false);
		} catch (Exception e) {
			Logger.error("Handle Delivery", e);
		}
	}
}
