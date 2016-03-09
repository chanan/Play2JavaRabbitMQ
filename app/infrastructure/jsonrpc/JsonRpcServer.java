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

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import infrastructure.json.JSONReader;
import infrastructure.json.JSONWriter;

import java.lang.reflect.Type;
import java.util.concurrent.CompletableFuture;
import play.Logger;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.*;

/**
 * JSON-RPC Server class.
 *
 * Given a Java {@link Class}, representing an interface, and an
 * implementation of that interface, JsonRpcServer will reflect on the
 * class to construct the {@link ServiceDescription}, and will route
 * incoming requests for methods on the interface to the
 * implementation object while the mainloop() is running.
 *
 * @see com.rabbitmq.client.RpcServer
 * @see JsonRpcClient
 */
public class JsonRpcServer extends StringRpcServer {
    /** Holds the JSON-RPC service description for this client. */
    public ServiceDescription serviceDescription;
    /** The interface this server implements. */
    public Class<?> interfaceClass;
    /** The instance backing this server. */
    public Object interfaceInstance;
    
    private final static ObjectMapper mapper = new ObjectMapper();

    /**
     * Construct a server that talks to the outside world using the
     * given channel, and constructs a fresh temporary
     * queue. Use getQueueName() to discover the created queue name.
     * @param channel AMQP channel to use
     * @param interfaceClass Java interface that this server is exposing to the world
     * @param interfaceInstance Java instance (of interfaceClass) that is being exposed
     * @throws IOException if something goes wrong during an AMQP operation
     */
    public JsonRpcServer(Channel channel, Class<?> interfaceClass, Object interfaceInstance) throws IOException {
        super(channel);
        init(interfaceClass, interfaceInstance);
    }

    private void init(Class<?> interfaceClass, Object interfaceInstance) {
        this.interfaceClass = interfaceClass;
        this.interfaceInstance = interfaceInstance;
        this.serviceDescription = new ServiceDescription(interfaceClass);
    }

    /**
     * Construct a server that talks to the outside world using the
     * given channel and queue name. Our superclass,
     * RpcServer, expects the queue to exist at the time of
     * construction.
     * @param channel AMQP channel to use
     * @param queueName AMQP queue name to listen for requests on
     * @param interfaceClass Java interface that this server is exposing to the world
     * @param interfaceInstance Java instance (of interfaceClass) that is being exposed
     * @throws IOException if something goes wrong during an AMQP operation
     */
    public JsonRpcServer(Channel channel, String queueName, Class<?> interfaceClass, Object interfaceInstance) throws IOException {
        super(channel, queueName);
        init(interfaceClass, interfaceInstance);
    }

    /**
     * Override our superclass' method, dispatching to doCall.
     */
    @Override
    public String handleStringCall(String requestBody, AMQP.BasicProperties replyProperties) {
        String replyBody = doCall(requestBody);
        return replyBody;
    }

    /**
     * Runs a single JSON-RPC request.
     * @param requestBody the JSON-RPC request string (a JSON encoded value)
     * @return a JSON-RPC response string (a JSON encoded value)
     */
    public String doCall(String requestBody) {
        Object id;
        String method;
        Object[] params;
        int methodId = -1;
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> request = (Map<String, Object>) new JSONReader().read(requestBody);
            if (request == null) {
                return errorResponse(null, 400, "Bad Request", null);
            }
            if (!ServiceDescription.JSON_RPC_VERSION.equals(request.get("version"))) {
                return errorResponse(null, 505, "JSONRPC version not supported", null);
            }

            id = request.get("id");
            method = (String) request.get("method");
            List<?> paramList = (List<?>) request.get("params");
            if (!method.startsWith("system.")) methodId = Integer.parseInt(request.get("method_id").toString());
            params = paramList.toArray();
            
        } catch (ClassCastException cce) {
            // Bogus request!
            return errorResponse(null, 400, "Bad Request", null);
        }

        if (method.equals("system.describe")) {
            return resultResponse(id, serviceDescription);
        } else if (method.startsWith("system.")) {
            return errorResponse(id, 403, "System methods forbidden", null);
        } else {
            Object result;
            try {
            	Method m = matchingMethod(methodId);
            	params = getObjectParams(m, requestBody);
                CompletableFuture<Object> futureResult = (CompletableFuture<Object>)m.invoke(interfaceInstance, params);
                result = futureResult.get();
                Logger.debug("Server method result: " + result);
            } catch (Throwable t) {
                return errorResponse(id, 500, "Internal Server Error", t);
            }
            return resultResponse(id, result);
        }
    }

    /**
     * Private API Added by Chanan. 
     * 
     * Get "real" objects for method parameters. The original code only worked with primitive types.
     * 
     * @param method The method to execute
     * @param requestBody
     * @return
     * @throws JsonParseException
     * @throws JsonMappingException
     * @throws IOException
     * @throws ClassNotFoundException
     */
    private Object[] getObjectParams(Method method, String requestBody) throws JsonParseException, JsonMappingException, IOException, ClassNotFoundException {
		List<Object> list = new ArrayList<>();
		JsonNode root = mapper.readTree(requestBody);
		JsonNode params = root.path("params");
		int i = 0;
		Iterator<JsonNode> iterator = params.iterator();
		final Type[] types = method.getGenericParameterTypes();
		while (iterator.hasNext()) { 
			JsonNode param = iterator.next();
			String className = types[i].getTypeName();
            if (className.contains("<")) {
                final String genericClassName = className.substring(0, className.indexOf("<"));
                final String typeName = className.substring(className.indexOf("<") + 1, className.length() - 1);
                final Class<?> genericClazz = getClassForName(genericClassName);
                final Class<?> clazz = getClassForName(typeName);
                final JavaType javaType = mapper.getTypeFactory().constructParametricType(genericClazz, clazz);
                final Object obj = mapper.convertValue(param, javaType);
                list.add(obj);
            } else {
                final Class<?> clazz = getClassForName(className);
                final Object obj = mapper.treeToValue(param, clazz);
                list.add(obj);
            }
			i++;
	    }
		return list.toArray();
	}

    private Class<?> getClassForName(String className) throws ClassNotFoundException {
        String temp;
        if (className.contains("<")) temp = className.substring(0, className.indexOf("<"));
        else temp = className;
        switch (temp.toLowerCase()) {
            case "byte":
                return byte.class;
            case "short":
                return short.class;
            case "int":
                return int.class;
            case "long":
                return long.class;
            case "float":
                return float.class;
            case "double":
                return double.class;
            case "char":
                return char.class;
            case "boolean":
                return boolean.class;
            default:
                return Class.forName(temp);
        }
    }

    /**
     * Retrieves the best matching method for the given method name and parameters.
     *
     */
    public Method matchingMethod(int methodId) {
        ProcedureDescription proc = serviceDescription.getProcedure(methodId);
        return proc.internal_getMethod();
    }

    /**
     * Construct and encode a JSON-RPC error response for the request
     * ID given, using the code, message, and possible
     * (JSON-encodable) argument passed in.
     */
    public static String errorResponse(Object id, int code, String message, Object errorArg) {
        Map<String, Object> err = new HashMap<String, Object>();
        err.put("name", "JSONRPCError");
        err.put("code", code);
        err.put("message", message);
        err.put("error", errorArg);
        return response(id, "error", err);
    }

    /**
     * Construct and encode a JSON-RPC success response for the
     * request ID given, using the result value passed in.
     */
    public static String resultResponse(Object id, Object result) {
        return response(id, "result", result);
    }

    /**
     * Private API - used by errorResponse and resultResponse.
     */
    public static String response(Object id, String label, Object value) {
        Map<String, Object> resp = new HashMap<>();
        resp.put("version", ServiceDescription.JSON_RPC_VERSION);
        if (id != null) {
            resp.put("id", id);
        }
        resp.put(label, value);
        String respStr = new JSONWriter().write(resp);
        return respStr;
    }

    /**
     * Public API - gets the service description record that this
     * service built from interfaceClass at construction time.
     */
    public ServiceDescription getServiceDescription() {
        return serviceDescription;
    }
}
