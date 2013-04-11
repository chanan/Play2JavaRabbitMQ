package services;

import play.Logger;
import remote.RemoteCalculator;
import infrastructure.RabbitConfig;
import infrastructure.RabbitConnection;
import infrastructure.jsonrpc.JsonRpcClient;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;

public class CalculatorFactory {
	private static RemoteCalculator calculator = null;
	
	public static RemoteCalculator getCalculator() {
		if(calculator == null) {
			try {
				Connection connection = RabbitConnection.getConnection();
				Channel channel = connection.createChannel();
				JsonRpcClient client = new JsonRpcClient(channel, "", RabbitConfig.getRabbitRpcQueue());
				calculator = (RemoteCalculator) client.createProxy(RemoteCalculator.class);
			} catch (Exception e) {
				Logger.error("getCalculator", e);
			}
		}
		return calculator;
	}

}
