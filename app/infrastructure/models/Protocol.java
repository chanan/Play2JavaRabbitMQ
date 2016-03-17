package infrastructure.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
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
            return  "Invoke {" +
                    "method: '" + method + '\'' +
                    ", args: " + Arrays.toString(args)
                    + '}';
        }
    }

    public static class RabbitMessage {
        private String method;
        private Object[] args;
        private Integer methodId;

        public RabbitMessage() {
        }

        public RabbitMessage(String method, Object[] args, Integer methodId) {
            this.method = method;
            this.args = args;
            this.methodId = methodId;
        }

        public String getMethod() {
            return method;
        }

        public void setMethod(String method) {
            this.method = method;
        }

        public Object[] getArgs() {
            return args;
        }

        public void setArgs(Object[] args) {
            this.args = args;
        }

        public Integer getMethodId() {
            return methodId;
        }

        public void setMethodId(Integer methodId) {
            this.methodId = methodId;
        }

        @Override
        public String toString() {
            return "RabbitMessage {" +
                    "method: \"" + method + '"' +
                    ", args: [" + Arrays.toString(args) + ']' +
                    ", methodId: \"" + methodId + '"' +
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

    public enum InvokeReplyType {
        ERROR,
        SERVICE_DESCRIPTOR,
        RESULT
    }

    public static class InvokeReply {
        private Invoke invoke;
        private InvokeReplyType replyType;
        private Exception error;
        private ServiceDescriptor serviceDescriptor;
        private Object result;

        private static final String jsonRpcVersion = "3.0";

        @JsonCreator
        public InvokeReply(@JsonProperty("invoke") Invoke invoke, @JsonProperty("replyType") InvokeReplyType replyType, @JsonProperty("error") Exception error,
                           @JsonProperty("serviceDescriptor") ServiceDescriptor serviceDescriptor, @JsonProperty("result") Object result) {
            this.invoke = invoke;
            this.replyType = replyType;
            this.error = error;
            this.serviceDescriptor = serviceDescriptor;
            this.result = result;
        }

        public Invoke getInvoke() {
            return invoke;
        }

        public InvokeReplyType getReplyType() {
            return replyType;
        }

        public Object getResult() {
            return result;
        }

        public Exception getError() {
            return error;
        }

        public ServiceDescriptor getServiceDescriptor() {
            return serviceDescriptor;
        }

        public String getJsonRpcVersion() {
            return jsonRpcVersion;
        }

        @Override
        public String toString() {
            return "InvokeReply {" +
                    "invoke: {" + invoke + '}' +
                    ", replyType: \"" + replyType.toString() + '"' +
                    ", error: {" + error + '}' +
                    ", serviceDescriptor: {" + serviceDescriptor + '}' +
                    ", result: {" + result + '}' +
                    '}';
        }
    }

    public static class NullObject {

    }
}