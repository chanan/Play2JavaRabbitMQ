package framework;

import java.io.IOException;

import com.rabbitmq.client.*;


public class RabbitConnection {
	private static Connection connection;
	
	public static Connection getConnection() throws IOException {
		if(connection == null) {
			ConnectionFactory factory = new ConnectionFactory();
			factory.setHost(RabbitConfig.getRabbitHost());
			connection = factory.newConnection();
		}
		return connection;
	}
}
