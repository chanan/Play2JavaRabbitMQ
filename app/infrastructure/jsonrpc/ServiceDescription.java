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

import com.rabbitmq.tools.Tracer;
import com.rabbitmq.tools.json.JSONUtil;
import play.Logger;
import play.libs.Json;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Description of a JSON-RPC service.
 */
public class ServiceDescription {
    public static final String JSON_RPC_VERSION = "2.0";

    /** The service name */
    public String name;
    /** ID for the service */
    public String id;
    /** Version of the service */
    public String version;
    /** Human-readable summary for the service */
    public String summary;
    /** Human-readable instructions for how to get information on the service's operation */
    public String help;

    /** Map from procedure name to {@link ProcedureDescription} */
    private Map<Integer, ProcedureDescription> procedures;

    private int count = 0;

    public ServiceDescription(Map<String, Object> rawServiceDescription) {
        JSONUtil.tryFill(this, rawServiceDescription);
    }

    public ServiceDescription(Class<?> klass) {
        this.procedures = new HashMap<>();
        for (Method m: klass.getMethods()) {
            ProcedureDescription proc = new ProcedureDescription(m, count);
            addProcedure(proc, count);
            count++;
        }
    }

    public ServiceDescription() {
        // No work to do here
    }

    /** Gets a collection of all {@link ProcedureDescription} for this service */
    public Collection<ProcedureDescription> getProcs() {
        return procedures.values();
    }

    /** Private API - used via reflection during parsing/loading */
    public void setProcs(Collection<Map<String, Object>> p) {
        procedures = new HashMap<>();
        for (Map<String, Object> pm: p) {
            ProcedureDescription proc = new ProcedureDescription(pm);
            addProcedure(proc, 0);  // FIXME: 3/7/16
        }
    }

    /** Private API - used during initialization */
    private void addProcedure(ProcedureDescription proc, int num) {
        procedures.put(proc.id, proc);
    }

    /**
     * Called from client side
     * @param name
     * @param params
     * @return
     */
    public ProcedureDescription getProcedure(String name, Object[] params) {
        if(params != null) Arrays.asList(params).stream().forEach(p -> Logger.debug("Param: " + p.toString()));

        final int paramsLength = params != null ? params.length : 0;
        ProcedureDescription found = null;

        for(ProcedureDescription proc : this.getProcs()) {
            boolean match = false;
            if(name.equals(proc.name) && paramsLength == proc.arity()) {
                boolean parseError = false;
                for (int i = 0; i < proc.arity(); i++) {
                    Logger.debug("----------");
                    try {
                        final Class<?> clazz = Class.forName(fixPrimitiveClassName(proc.getParams()[i].type));
                        clazz.cast(params[i]);
                    } catch (Exception e) {
                        Logger.error("Parse error", e);
                        parseError = true;
                        break;
                    }
                }
                if (!parseError) match = true;
            }
            if(match) {
                found = proc;
                break;
            }
        }

        if (found == null) throw new IllegalArgumentException("Procedure not found: " + name);
        Logger.debug("Found: " + found);
        return found;
    }

    /**
     * Called from server side
     * @param methodId
     * @return
     */
    public ProcedureDescription getProcedure(int methodId) {
        return procedures.get(methodId);
    }

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
            default:
                return className;
        }
    }


    @Override
    public String toString() {
        return "ServiceDescription {" +
                "procedures: " + procedures +
                '}';
    }
}