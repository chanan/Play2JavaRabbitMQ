//  The contents of this file are subject to the Mozilla Public License
//  Version 1.1 (the "License"); you may not use this file except in
//  compliance with the License. You may obtain a copy of the License
//  at http://www.mozilla.org/MPL/
//
//  Software distributed under the License is distributed on an "AS IS"
//  basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
//  the License for the specific language governing rights and
//  limitations under the License.
//
//  The Original Code is RabbitMQ.
//
//  The Initial Developer of the Original Code is VMware, Inc.
//  Copyright (c) 2007-2013 VMware, Inc.  All rights reserved.
//

package infrastructure.jsonrpc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.RpcClient;
import com.rabbitmq.client.ShutdownSignalException;
import com.rabbitmq.tools.jsonrpc.JsonRpcException;
import infrastructure.json.JSONReader;
import infrastructure.json.JSONWriter;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

/**
	  <a href="http://json-rpc.org">JSON-RPC</a> is a lightweight
	  RPC mechanism using <a href="http://www.json.org/">JSON</a>
	  as a data language for request and reply messages. It is
	  rapidly becoming a standard in web development, where it is
	  used to make RPC requests over HTTP. RabbitMQ provides an
	  AMQP transport binding for JSON-RPC in the form of the
	  <code>JsonRpcClient</code> class.

	  JSON-RPC services are self-describing - each service is able
	  to list its supported procedures, and each procedure
	  describes its parameters and types. An instance of
	  JsonRpcClient retrieves its service description using the
	  standard <code>system.describe</code> procedure when it is
	  constructed, and uses the information to coerce parameter
	  types appropriately. A JSON service description is parsed
	  into instances of <code>ServiceDescription</code>. Client
	  code can access the service description by reading the
	  <code>serviceDescription</code> field of
	  <code>JsonRpcClient</code> instances.

	  @see #call(String, Object[])
	  @see #call(String[])
 */
public class JsonRpcClient extends RpcClient implements InvocationHandler {
    /** Holds the JSON-RPC service description for this client. */
    private ServiceDescription serviceDescription;
    private final static ObjectMapper mapper = new ObjectMapper();

    /**
     * Construct a new JsonRpcClient, passing the parameters through
     * to RpcClient's constructor. The service description record is
     * retrieved from the server during construction.
     * @throws TimeoutException if a response is not received within the timeout specified, if any
     * @throws ClassNotFoundException 
     */
    public JsonRpcClient(Channel channel, String exchange, String routingKey, int timeout) throws IOException, JsonRpcException, TimeoutException, ClassNotFoundException {
    	super(channel, exchange, routingKey, timeout);
    	retrieveServiceDescription();
    }

    public JsonRpcClient(Channel channel, String exchange, String routingKey) throws IOException, JsonRpcException, TimeoutException, ClassNotFoundException {
        this(channel, exchange, routingKey, RpcClient.NO_TIMEOUT);
    }

    /**
     * Private API - parses a JSON-RPC reply object, checking it for exceptions.
     * @return the result contained within the reply, if no exception is found
     * @throws JsonRpcException if the reply object contained an exception
     * @throws IOException 
     * @throws ClassNotFoundException 
     * @throws JsonProcessingException 
     */
    public Object checkReply(String method, Object[] params, String replyStr, Map<String, Object> reply) throws JsonRpcException, JsonProcessingException, ClassNotFoundException, IOException {
    	if (reply.containsKey("error")) {
    		@SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) reply.get("error");
            // actually a Map<String, Object>
            throw new JsonRpcException(map);
        }
    	Object result;
    	if("system.describe".equals(method)){
    		result = reply.get("result");
    	} else {
    		result = getObjectResult(method, params, replyStr);
    	}
        return result;
    }
    
    /**
     * Private API Added by Chanan.
     * 
     * Gets the return json as a "real" object
     * 
     * @param method
     * @param params
     * @param replyStr
     * @return
     * @throws JsonProcessingException
     * @throws IOException
     * @throws ClassNotFoundException
     */
    private Object getObjectResult(String method, Object[] params, String replyStr) throws JsonProcessingException, IOException, ClassNotFoundException {
        JsonNode root = mapper.readTree(replyStr);
        JsonNode result = root.path("result");

        String className = getMethodReturnClassName(method, params);
        if("void".equalsIgnoreCase(className)) return null;
        className = fixPrimitiveClassName(className);
        Object ret = mapper.treeToValue(result, Class.forName(className));
        return ret;
    }


    /**
     * Private API Added by Chanan.
     * 
     * Gets the class name of the return object
     * 
     * @param method
     * @param params
     * @return
     */
	private String getMethodReturnClassName(String method, Object[] params) {
		ProcedureDescription proc = serviceDescription.getProcedure(method, params.length);
		return proc.getReturn();
	}

	/**
     * Private API Added by Chanan.
     * 
     * if the class name is a primitive type return the correct class name.
     * 
     * @param className
     * @return
     */
	private String fixPrimitiveClassName(String className) {
		switch (className.toLowerCase()) {
			case "byte":
				return "java.lang.Byte";
			case "short":
				return "java.lang.Short";
			case "int":
				return "java.lang.Integer";
			case "long":
				return "java.lang.Long";
			case "float":
				return "java.lang.Float";
			case "double":
				return "java.lang.Double";
			case "char":
				return "java.lang.Character";
			case "boolean":
				return "java.lang.Boolean";
			case "object":
				return "java.lang.Object";
			default:
				return className;
		}
	}

    /**
     * Public API - builds, encodes and sends a JSON-RPC request, and
     * waits for the response.
     * @return the result contained within the reply, if no exception is found
     * @throws JsonRpcException if the reply object contained an exception
     * @throws TimeoutException if a response is not received within the timeout specified, if any
     * @throws ClassNotFoundException 
     */
    public Object call(String method, Object[] params) throws IOException, JsonRpcException, TimeoutException, ClassNotFoundException
    {
        HashMap<String, Object> request = new HashMap<String, Object>();
        request.put("id", null);
        request.put("method", method);
        request.put("version", ServiceDescription.JSON_RPC_VERSION);
        request.put("params", (params == null) ? new Object[0] : params);
        String requestStr = new JSONWriter().write(request);
        try {
            String replyStr = this.stringCall(requestStr);
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) (new JSONReader().read(replyStr));
            return checkReply(method, params, replyStr, map);
        } catch(ShutdownSignalException ex) {
            throw new IOException(ex.getMessage()); // wrap, re-throw
        }

    }

    /**
     * Public API - implements InvocationHandler.invoke. This is
     * useful for constructing dynamic proxies for JSON-RPC
     * interfaces.
     */
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        return call(method.getName(), args);
    }

    /**
     * Public API - gets a dynamic proxy for a particular interface class.
     */
    public Object createProxy(Class<?> klass) throws IllegalArgumentException {
        return Proxy.newProxyInstance(klass.getClassLoader(), new Class[] { klass }, this);
    }

    /**
     * Private API - used by {@link #call(String[])} to ad-hoc convert
     * strings into the required data types for a call.
     */
    public static Object coerce(String val, String type) throws NumberFormatException {
		if ("bit".equals(type)) {
		    return Boolean.getBoolean(val) ? Boolean.TRUE : Boolean.FALSE;
		} else if ("num".equals(type)) {
		    try {
		    	return new Integer(val);
		    } catch (NumberFormatException nfe) {
		    	return new Double(val);
		    }
		} else if ("str".equals(type)) {
		    return val;
		} else if ("arr".equals(type) || "obj".equals(type) || "any".equals(type)) {
		    return new JSONReader().read(val);
		} else if ("nil".equals(type)) {
		    return null;
		} else {
		    throw new IllegalArgumentException("Bad type: " + type);
		}
    }

    /**
     * Public API - as {@link #call(String,Object[])}, but takes the
     * method name from the first entry in <code>args</code>, and the
     * parameters from subsequent entries. All parameter values are
     * passed through coerce() to attempt to make them the types the
     * server is expecting.
     * @return the result contained within the reply, if no exception is found
     * @throws JsonRpcException if the reply object contained an exception
     * @throws NumberFormatException if a coercion failed
     * @throws TimeoutException if a response is not received within the timeout specified, if any
     * @throws ClassNotFoundException 
     * @see #coerce
     */
    public Object call(String[] args) throws NumberFormatException, IOException, JsonRpcException, TimeoutException, ClassNotFoundException {
    	if (args.length == 0) {
    		throw new IllegalArgumentException("First string argument must be method name");
    	}

    	String method = args[0];
        int arity = args.length - 1;
        ProcedureDescription proc = serviceDescription.getProcedure(method, arity);
        ParameterDescription[] params = proc.getParams();

        Object[] actuals = new Object[arity];
        for (int count = 0; count < params.length; count++) {
        	actuals[count] = coerce(args[count + 1], params[count].type);
        }

        return call(method, actuals);
    }

    /**
     * Public API - gets the service description record that this
     * service loaded from the server itself at construction time.
     */
    public ServiceDescription getServiceDescription() {
    	return serviceDescription;
    }

    /**
     * Private API - invokes the "system.describe" method on the
     * server, and parses and stores the resulting service description
     * in this object.
     * TODO: Avoid calling this from the constructor.
     * @throws TimeoutException if a response is not received within the timeout specified, if any
     * @throws ClassNotFoundException 
     */
    private void retrieveServiceDescription() throws IOException, JsonRpcException, TimeoutException, ClassNotFoundException {
        @SuppressWarnings("unchecked")
        Map<String, Object> rawServiceDescription = (Map<String, Object>) call("system.describe", null);
        serviceDescription = new ServiceDescription(rawServiceDescription);
    }
}
