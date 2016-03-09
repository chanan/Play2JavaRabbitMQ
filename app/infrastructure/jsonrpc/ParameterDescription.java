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

import com.rabbitmq.tools.json.JSONUtil;

import java.util.Collection;
import java.util.Map;

/**
 * Description of a single JSON-RPC procedure parameter.
 */
public class ParameterDescription {
    /** The parameter name. */
    public String name;
    /**
     * The parameter type - one of "bit", "num", "str", "arr",
     * "obj", "any" or "nil".
     */
    public String type;

    public ParameterDescription() {
        // Nothing to do here.
    }

    public ParameterDescription(Map<String, Object> pm) {
        JSONUtil.tryFill(this, pm);
    }

    public ParameterDescription(int index, String type) {
        name = "param" + index;
        this.type = type;
    }

    @Override
    public String toString() {
        return "ParameterDescription {" +
                "name: \"" + name + '"' +
                ", type: \"" + type + '"' +
                '}';
    }
}