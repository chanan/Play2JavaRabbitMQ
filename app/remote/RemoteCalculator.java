package remote;

import java.util.concurrent.CompletionStage;

public interface RemoteCalculator {
	CompletionStage<Integer> add(int a, int b);
	CompletionStage<Void> longOperation(int timeout);
}