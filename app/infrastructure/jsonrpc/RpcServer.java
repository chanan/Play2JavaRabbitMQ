package infrastructure.jsonrpc;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

import java.io.IOException;
import java.util.UUID;


/**
 * Class which manages a request queue for a simple RPC-style service.
 * The class is agnostic about the format of RPC arguments / return values.
*/
public class RpcServer extends DefaultConsumer {
    /** Queue to receive requests from */
    private final String _queueName;
    
    /**
     * Creates an RpcServer listening on a temporary exclusive
     * autodelete queue.
     * @throws IOException 
     */
    public RpcServer(Channel channel) throws IOException        
    {
    	this(channel, null);
    }

    /**
     * If the passed-in queue name is null, creates a server-named
     * temporary exclusive autodelete queue to use; otherwise expects
     * the queue to have already been declared.
     * @throws IOException 
     */
    public RpcServer(Channel channel, String queueName) throws IOException
    {
    	super(channel);
        if (queueName == null || queueName.equals("")) {
            _queueName = getChannel().queueDeclare().getQueue();
        } else {
            _queueName = queueName;
        }
    }
    
    /**
     * Public API - Starts consuming messages
     * @throws IOException 
     */
    public void start() throws IOException {
    	getChannel().basicConsume(_queueName, false, UUID.randomUUID().toString(), this);
    }

    /**
     * Public API - cancels the consumer, thus deleting the queue, if
     * it was a temporary queue, and marks the RpcServer as closed.
     * @throws IOException if an error is encountered
     */
    public void close() throws IOException {
    	getChannel().basicCancel(getConsumerTag());
    }
    
    @Override
	public void handleDelivery(String consumerTag, Envelope envelope, BasicProperties properties, byte[] body) throws IOException {
    	processRequest(properties, body);
    	getChannel().basicAck(envelope.getDeliveryTag(), false);
	}
    
    /**
     * Private API - Process a single request. Called from handleDelivery().
     */
    public void processRequest(BasicProperties properties, byte[] body) throws IOException {
        String correlationId = properties.getCorrelationId();
        String replyTo = properties.getReplyTo();
        if (correlationId != null && replyTo != null)
        {
            AMQP.BasicProperties replyProperties = new AMQP.BasicProperties.Builder().correlationId(correlationId).build();
            byte[] replyBody = handleCall(properties, body, replyProperties);
            getChannel().basicPublish("", replyTo, replyProperties, replyBody);
        } else {
            handleCast(properties, body);
        }
    }

    /**
     * Mid-level response method. Calls
     * handleCall(byte[],AMQP.BasicProperties).
     */
    public byte[] handleCall(AMQP.BasicProperties requestProperties, byte[] requestBody, AMQP.BasicProperties replyProperties)
    {
        return handleCall(requestBody, replyProperties);
    }

    /**
     * High-level response method. Returns an empty response by
     * default - override this (or other handleCall and handleCast
     * methods) in subclasses.
     */
    public byte[] handleCall(byte[] requestBody, AMQP.BasicProperties replyProperties)
    {
        return new byte[0];
    }

    /**
     * Mid-level handler method. Calls
     * handleCast(byte[]).
     */
    public void handleCast(AMQP.BasicProperties requestProperties, byte[] requestBody)
    {
        handleCast(requestBody);
    }

    /**
     * High-level handler method. Does nothing by default - override
     * this (or other handleCast and handleCast methods) in
     * subclasses.
     */
    public void handleCast(byte[] requestBody)
    {
        // Does nothing.
    }

    /**
     * Retrieve the channel.
     * @return the channel to which this server is connected
     */
    public Channel getChannel() {
        return super.getChannel();
    }

    /**
     * Retrieve the queue name.
     * @return the queue which this server is consuming from
     */
    public String getQueueName() {
        return _queueName;
    }
}

