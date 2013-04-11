package infrastructure;

import infrastructure.jsonrpc.JsonRpcServer;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import play.libs.Akka;

import remote.RemoteCalculator;
import remote.RemoteCalculatorImpl;
import scala.concurrent.duration.Duration;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.TypedActor;
import akka.actor.TypedProps;
import akka.actor.UntypedActor;
import akka.actor.UntypedActorFactory;
import akka.routing.SmallestMailboxRouter;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;

public class Sender {
	
	public static Channel channel;
	private static JsonRpcServer calculator;

	public static void StartListeners() throws IOException {
		Connection connection = RabbitConnection.getConnection();
		channel = connection.createChannel();
		ActorRef myActor = Akka.system().actorOf(new Props(new UntypedActorFactory() {
			  public UntypedActor create() {
			    return new SendingActor(channel, RabbitConfig.getRabbitQueue());
			  }
			}), "SendingActor");
		Akka.system().scheduler().schedule(
				Duration.create(2, TimeUnit.SECONDS),
				Duration.create(50, TimeUnit.MILLISECONDS),
				myActor,
				"MSG to Queue",
				Akka.system().dispatcher());
		ActorRef actor = Akka.system().actorOf(new Props(new UntypedActorFactory() {
			  public UntypedActor create() {
			    return new CommandActor(new LoggingCommand());
			  }
			}).withRouter(new SmallestMailboxRouter(10)), "RecievingActor");
		channel.basicConsume(RabbitConfig.getRabbitQueue(), false, "event_filter", new CommandConsumer(channel, actor));
		
		//Rpc
		RemoteCalculator calculatorkActor =TypedActor.get(Akka.system()).typedActorOf(new TypedProps<RemoteCalculatorImpl>(RemoteCalculator.class, RemoteCalculatorImpl.class));
		calculator = new JsonRpcServer(channel, RabbitConfig.getRabbitRpcQueue(), RemoteCalculator.class, calculatorkActor);
		calculator.start();
	}
	
	public static void StopListeners() throws IOException {
		channel.basicCancel("event_filter");
		calculator.close();
	}
}
