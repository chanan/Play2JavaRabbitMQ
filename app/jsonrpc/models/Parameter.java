package jsonrpc.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Parameter {
    private int id;
    private String type;

    public Parameter() {
    }

    @JsonCreator
    public Parameter(@JsonProperty("id") int id, @JsonProperty("type") String type) {
        this.id = id;
        this.type = type;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Parameter parameter = (Parameter) o;

        if (id != parameter.id) return false;
        return type != null ? type.equals(parameter.type) : parameter.type == null;

    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + (type != null ? type.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Parameter {" +
                "id: " + id +
                ", type: \"" + type + '"' +
                '}';
    }
}