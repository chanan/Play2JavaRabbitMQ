package jsonrpc;

import jsonrpc.models.Parameter;
import jsonrpc.models.Procedure;
import jsonrpc.models.ServiceDescriptor;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class JsonRpcService {
    public ServiceDescriptor createServiceDescriptor(Class<?> clazz) {

        final Method[] methods = clazz.getMethods();
        final List<Procedure> procedures = IntStream.range(0, methods.length).boxed().map(idx -> {
            final Method method = methods[idx];
            final Class<?>[] parameterTypes = method.getParameterTypes();
            final List<Parameter> parameters = IntStream.range(0, parameterTypes.length).boxed().map(pIdx -> {
                final Class<?> parameterType = parameterTypes[pIdx];
                return new Parameter(pIdx, parameterType.getTypeName());
            }).collect(Collectors.toList());
            final String returnType = getReturnType(method);
           return new Procedure(method, idx, method.getName(), parameters, returnType);
        }).collect(Collectors.toList());
        return new ServiceDescriptor(clazz.getName(), procedures);
    }

    public Optional<Procedure> findProcedure(ServiceDescriptor serviceDescriptor, int id) {
        return serviceDescriptor.getProcedures().stream().filter(p -> p.getId() == id).findFirst();
    }

    public Optional<Procedure> findProcedure(ServiceDescriptor serviceDescriptor, String name, Object[] params) {
        final int paramsLength = params != null ? params.length : 0;

        return serviceDescriptor.getProcedures().stream().filter(proc -> {
            if(!name.equals(proc.getName())) return false;
            if(paramsLength != proc.getArity()) return false;
            boolean parseError = false;
            for (int i = 0; i < proc.getArity(); i++) {
                try {
                    final Class<?> clazz = Class.forName(fixPrimitiveClassName(proc.getParameters().get(i).getType()));
                    clazz.cast(params[i]);
                } catch (Exception e) {
                    parseError = true;
                    break;
                }
            }
            return !parseError;
        }).findFirst();
    }

    private String getReturnType(Method method) {
        final Type returnType = method.getGenericReturnType();
        if (returnType instanceof Class<?>)
            return returnType.getTypeName();
        else  {
            final ParameterizedType type = (ParameterizedType) returnType;
            final Type[] typeArguments = type.getActualTypeArguments();
            String found = null;
            for(Type typeArgument : typeArguments) {
                found = typeArgument.getTypeName();
            }
            return found;
        }
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
}