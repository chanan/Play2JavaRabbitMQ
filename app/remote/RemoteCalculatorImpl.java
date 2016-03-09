package remote;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class RemoteCalculatorImpl implements RemoteCalculator {

	@Override
	public CompletionStage<Integer> add(int a, int b) {
		return CompletableFuture.completedFuture(a + b);
	}
}