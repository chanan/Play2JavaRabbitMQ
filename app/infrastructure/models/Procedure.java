package infrastructure.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class Procedure {
    private int Id;
    private String name;
    private List<Parameter> parameters = new ArrayList<>();
    private String returnType;
    private Method method;

    public Procedure() {
    }

    @JsonCreator
    public Procedure(@JsonProperty("id") int id, @JsonProperty("name") String name, @JsonProperty("parameters") List<Parameter> parameters, @JsonProperty("returnType") String returnType) {
        Id = id;
        this.name = name;
        this.parameters = parameters;
        this.returnType = returnType;
    }

    public Procedure(Method method, int id, String name, List<Parameter> parameters, String returnType) {
        this.method = method;
        Id = id;
        this.name = name;
        this.parameters = parameters;
        this.returnType = returnType;
    }

    public int getId() {
        return Id;
    }

    public void setId(int id) {
        Id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Parameter> getParameters() {
        return parameters;
    }

    public void setParameters(List<Parameter> parameters) {
        this.parameters = parameters;
    }

    public String getReturnType() {
        return returnType;
    }

    public void setReturnType(String returnType) {
        this.returnType = returnType;
    }

    @JsonIgnore
    public Method getInternalMethod() {
        return method;
    }

    @JsonIgnore
    public int getArity() {
        return parameters.size();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Procedure procedure = (Procedure) o;

        if (Id != procedure.Id) return false;
        if (name != null ? !name.equals(procedure.name) : procedure.name != null) return false;
        if (parameters != null ? !parameters.equals(procedure.parameters) : procedure.parameters != null)
            return false;
        return returnType != null ? returnType.equals(procedure.returnType) : procedure.returnType == null;

    }

    @Override
    public int hashCode() {
        int result = Id;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (parameters != null ? parameters.hashCode() : 0);
        result = 31 * result + (returnType != null ? returnType.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Procedure {" +
                "Id: \"" + Id + '"' +
                ", name: \"" + name + '"' +
                ", parameters: [" + parameters + ']' +
                ", returnType: \"" + returnType + '"' +
                '}';
    }
}