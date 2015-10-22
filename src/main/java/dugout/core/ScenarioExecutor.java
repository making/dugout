package dugout.core;

import java.util.concurrent.CompletableFuture;

import org.springframework.scheduling.annotation.Async;

public class ScenarioExecutor {

	@Async
	public CompletableFuture<ScenarioContext> exec(Scenario scenario,
			ScenarioContext context) {
		return CompletableFuture.completedFuture(scenario.exec(context));
	}
}
