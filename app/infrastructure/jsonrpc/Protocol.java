package infrastructure.jsonrpc;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Envelope;

import java.util.Arrays;

public class Protocol {
    public static class Invoke {
        public final String method;
        public final Object[] args;

        public Invoke(String method, Object[] args) {
            this.method = method;
            this.args = args;
        }

        @Override
        public String toString() {
            return "Invoke {" +
                    "method: '" + method + '\'' +
                    ", args: " + Arrays.toString(args) +
                    '}';
        }
    }

    public static class InvokeRabbitReply {
        public final String consumerTag;
        public final Envelope envelope;
        public final AMQP.BasicProperties properties;
        public final String body;

        public InvokeRabbitReply(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, String body) {
            this.consumerTag = consumerTag;
            this.envelope = envelope;
            this.properties = properties;
            this.body = body;
        }

        @Override
        public String toString() {
            return "InvokeRabbitReply {" +
                    "consumerTag: '" + consumerTag + '\'' +
                    ", envelope: " + envelope +
                    ", properties: " + properties +
                    ", body: '" + body + '\'' +
                    '}';
        }
    }

    public static abstract class InvokeReply {
        public final Invoke invoke;

        public InvokeReply(Invoke invoke) {
            this.invoke = invoke;
        }

        public abstract boolean isError();
    }

    public static class InvokeError extends InvokeReply {
        public final Exception error;

        public InvokeError(Invoke invoke, Exception error) {
            super(invoke);
            this.error = error;
        }

        public boolean isError() {
            return true;
        }

        @Override
        public String toString() {
            return "InvokeError {" +
                    "invoke: " + invoke +
                    ", error: " + error +
                    '}';
        }
    }

    public static class InvokeObjectReply extends InvokeReply {
        public final Object object;

        public InvokeObjectReply(Invoke invoke, Object object) {
            super(invoke);
            this.object = object;
        }

        public boolean isError() {
            return false;
        }

        @Override
        public String toString() {
            return "InvokeObjectReply {" +
                    "invoke: " + invoke +
                    ", object: " + object +
                    '}';
        }
    }

    public static class InvokeServiceDescriptionReply extends InvokeReply {
        public final ServiceDescription serviceDescription;

        public InvokeServiceDescriptionReply(Invoke invoke, ServiceDescription serviceDescription) {
            super(invoke);
            this.serviceDescription = serviceDescription;
        }

        @Override
        public boolean isError() {
            return false;
        }

        @Override
        public String toString() {
            return "InvokeServiceDescriptionReply {" +
                    "invoke: " + invoke +
                    ", serviceDescription: " + serviceDescription +
                    '}';
        }
    }

    public static class NullObject {

    }
}