package services;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import play.Logger;
import remote.RemoteCalculator;
import infrastructure.RabbitConfig;
import infrastructure.RabbitConnection;
import infrastructure.jsonrpc.JsonRpcClient;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;


//TODO: Change this to a guice provider
@Singleton
public class CalculatorFactory {
	private RemoteCalculator calculator = null;
    private final RabbitConnection connection;
    private final RabbitConfig config;

    @Inject
    public CalculatorFactory(RabbitConnection connection, RabbitConfig config) {
        this.connection = connection;
        this.config = config;
    }

    public RemoteCalculator getCalculator() {
		if(calculator == null) {
			try {
				Connection connection = this.connection.getConnection();
				Channel channel = connection.createChannel();
				JsonRpcClient client = new JsonRpcClient(channel, "", config.getRabbitRpcQueue());
				calculator = (RemoteCalculator) client.createProxy(RemoteCalculator.class);
			} catch (Exception e) {
				Logger.error("getCalculator", e);
			}
		}
		return calculator;
	}
}