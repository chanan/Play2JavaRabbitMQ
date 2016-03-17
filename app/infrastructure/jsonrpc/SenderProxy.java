package infrastructure.jsonrpc;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import infrastructure.models.Protocol;
import play.Logger;
import scala.Function1;
import scala.compat.java8.FutureConverters;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import static akka.pattern.Patterns.ask;

public class SenderProxy implements InvocationHandler {
    public final ActorSystem system;
    private final ActorRef actor;

    public SenderProxy(ActorSystem system, ActorRef actor) {
        this.system = system;
        this.actor = actor;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        final Protocol.Invoke message = new Protocol.Invoke(method.getName(), args);
        return FutureConverters.toJava(ask(actor, message, 10000)).thenApplyAsync(obj -> {
            if (obj instanceof Protocol.NullObject) return null;
            else if(obj instanceof Exception) throw new RuntimeException((Exception)obj);
            else return obj;
        });
    }
}