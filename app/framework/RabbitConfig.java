package framework;

import com.typesafe.config.ConfigFactory;

public class RabbitConfig {
	private static String rabbitHost;
	private static String rabbitQueue;
	private static String rabbitExchange;
	
	
	public static String getRabbitHost() {
		if(rabbitHost == null)
		{
			rabbitHost = ConfigFactory.load().getString("rabbitmq.host");
		}
		return rabbitHost;
	}
	
	public static String getRabbitQueue() {
		if(rabbitQueue == null)
		{
			rabbitQueue = ConfigFactory.load().getString("rabbitmq.queue");
		}
		return rabbitQueue;
	}
	
	public static String getRabbitExchange() {
		if(rabbitExchange == null)
		{
			rabbitExchange = ConfigFactory.load().getString("rabbitmq.exchange");
		}
		return rabbitExchange;
	}

}
