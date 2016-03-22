package jsonrpc;

import play.api.Configuration;
import play.api.Environment;
import play.api.inject.Binding;
import play.api.inject.Module;
import scala.collection.Seq;

public class JsonRpcModule extends Module {
    @Override
    public Seq<Binding<?>> bindings(Environment environment, Configuration configuration) {
        return seq(
            bind(JsonRpcFactory.class).to(JsonRpcFactoryImpl.class)
        );
    }
}