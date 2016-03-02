package infrastructure;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import play.Configuration;

@Singleton
public class RabbitConfig {
	private String rabbitHost;
	private String rabbitQueue;
	private String rabbitRpcQueue;
	private String rabbitExchange;
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
	
	public String getRabbitExchange() {
		if(rabbitExchange == null)
		{
			rabbitExchange = config.getString("rabbitmq.exchange");
		}
		return rabbitExchange;
	}
	
	public String getRabbitRpcQueue() {
		if(rabbitRpcQueue == null)
		{
			rabbitRpcQueue = config.getString("rabbitmq.rpcqueue");
		}
		return rabbitRpcQueue;
	}
}