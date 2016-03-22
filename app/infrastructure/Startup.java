package infrastructure;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import jsonrpc.JsonRpcFactory;
import jsonrpc.RabbitConfig;
import play.Configuration;
import remote.RemoteCalculator;
import remote.RemoteCalculatorImpl;
import remote.RemotePersonRepository;
import remote.RemotePersonRepositoryImpl;

@Singleton
public class Startup {
    @Inject
    public Startup(JsonRpcFactory jsonRpcFactory, Configuration config) {
        jsonRpcFactory.createServer(config.getString("rabbitmq.rpcqueue"), RemoteCalculator.class, RemoteCalculatorImpl.class);
        jsonRpcFactory.createServer(config.getString("rabbitmq.personRepoQueue"), RemotePersonRepository.class, RemotePersonRepositoryImpl.class);
    }
}