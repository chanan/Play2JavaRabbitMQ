package infrastructure.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

public class ServiceDescriptor {
    private String className;
    private List<Procedure> procedures = new ArrayList<>();

    public ServiceDescriptor() {
    }

    @JsonCreator
    public ServiceDescriptor(@JsonProperty("className") String className, @JsonProperty("procedures") List<Procedure> procedures) {
        this.className = className;
        this.procedures = procedures;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public List<Procedure> getProcedures() {
        return procedures;
    }

    public void setProcedures(List<Procedure> procedures) {
        this.procedures = procedures;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ServiceDescriptor that = (ServiceDescriptor) o;

        if (className != null ? !className.equals(that.className) : that.className != null) return false;
        return procedures != null ? procedures.equals(that.procedures) : that.procedures == null;

    }

    @Override
    public int hashCode() {
        int result = className != null ? className.hashCode() : 0;
        result = 31 * result + (procedures != null ? procedures.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "ServiceDescriptor {" +
                "className: \"" + className + '"' +
                ", procedures: [" + procedures + ']' +
                '}';
    }
}