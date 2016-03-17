package infrastructure;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import infrastructure.jsonrpc.JsonRpcFactory;
import remote.RemoteCalculator;
import remote.RemoteCalculatorImpl;
import remote.RemotePersonRepository;
import remote.RemotePersonRepositoryImpl;

@Singleton
public class Startup {
    @Inject
    public Startup(RabbitConfig rabbitConfig, JsonRpcFactory jsonRpcFactory) {
        jsonRpcFactory.createServer(rabbitConfig.getRabbitRpcQueue(), RemoteCalculator.class, RemoteCalculatorImpl.class);
        jsonRpcFactory.createServer(rabbitConfig.getPersonRepoQueue(), RemotePersonRepository.class, RemotePersonRepositoryImpl.class);
    }
}