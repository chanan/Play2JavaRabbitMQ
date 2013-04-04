package framework;

import com.rabbitmq.client.Channel;

import play.Logger;
import akka.actor.*;

public class SendingActor extends UntypedActor  {
	private Channel channel;
	private String queue;
	
	public SendingActor(Channel channel, String queue) {
		this.channel = channel;
		this.queue = queue;
	}
	
	@Override
	public void onReceive(Object message) throws Exception {
		 if (message instanceof String)
		 {
			 String msg = String.format("%s : %d", message, System.currentTimeMillis());
			 channel.basicPublish("", queue, null, msg.getBytes());
		 } else {
			 unhandled(message);
		 }
	}
}
