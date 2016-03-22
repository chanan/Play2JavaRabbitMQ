package jsonrpc;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import play.Configuration;

@Singleton
public class RabbitConfig {
	private String rabbitHost;
	private String rabbitQueue;
    private final Configuration config;

	@Inject
	public RabbitConfig(Configuration config) {
        this.config = config;
	}

	public String getRabbitHost() {
		if(rabbitHost == null)
		{
			rabbitHost = config.getString("rabbitmq.host");
		}
		return rabbitHost;
	}
	
	public String getRabbitQueue() {
		if(rabbitQueue == null)
		{
			rabbitQueue = config.getString("rabbitmq.queue");
		}
		return rabbitQueue;
	}
}