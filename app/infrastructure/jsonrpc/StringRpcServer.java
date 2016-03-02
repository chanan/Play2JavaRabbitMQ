package infrastructure.jsonrpc;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;

import java.io.IOException;

public class StringRpcServer extends RpcServer {
	public StringRpcServer(Channel channel) throws IOException { 
		super(channel); 
	}

    public StringRpcServer(Channel channel, String queueName) throws IOException { 
    	super(channel, queueName); 
	}

    public static String STRING_ENCODING = "UTF-8";
    
    /**
     * Overridden to do UTF-8 processing, and delegate to
     * handleStringCall. If UTF-8 is not understood by this JVM, falls
     * back to the platform default.
     */
    @Override
	public byte[] handleCall(BasicProperties requestProperties, byte[] requestBody, BasicProperties replyProperties) {
    	String request;
        try {
            request = new String(requestBody, STRING_ENCODING);
        } catch (IOException e) {
            request = new String(requestBody);
        }
        String reply = handleStringCall(request, replyProperties);
        try {
            return reply.getBytes(STRING_ENCODING);
        } catch (IOException e) {
            return reply.getBytes();
        }
	}

    /**
     * Delegates to handleStringCall(String).
     */
    public String handleStringCall(String request, AMQP.BasicProperties replyProperties)
    {
        return handleStringCall(request);
    }

    /**
     * Default implementation - override in subclasses. Returns the empty string.
     */
    public String handleStringCall(String request)
    {
        return "";
    }

    /**
     * Overridden to do UTF-8 processing, and delegate to
     * handleStringCast. If requestBody cannot be interpreted as UTF-8
     * tries the platform default.
     */
    @Override
    public void handleCast(byte[] requestBody)
    {
        try {
            handleStringCast(new String(requestBody, STRING_ENCODING));
        } catch (IOException e) {
            handleStringCast(new String(requestBody));
        }
    }

    /**
     * Default implementation - override in subclasses. Does nothing.
     */
    public void handleStringCast(String requestBody) {
        // Do nothing.
    }

	
}
