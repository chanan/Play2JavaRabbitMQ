package infrastructure;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.rabbitmq.client.*;

@Singleton
public class RabbitConnection {
	private Connection connection;
	private final RabbitConfig rabbitConfig;

	@Inject
	public RabbitConnection(RabbitConfig rabbitConfig) {
		this.rabbitConfig = rabbitConfig;
	}

	public Connection getConnection() throws IOException, TimeoutException {
		if(connection == null) {
			ConnectionFactory factory = new ConnectionFactory();
			factory.setHost(rabbitConfig.getRabbitHost());
			connection = factory.newConnection();
		}
		return connection;
	}
}