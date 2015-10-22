package dugout.core;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import lombok.Data;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.client.RestTemplate;

public class ScenarioInvoker {
	@Autowired
	List<Scenario> scenarios;
	@Autowired
	RestTemplate restTemplate;
	@Autowired
	ScenarioExecutor scenarioExecutor;

	public CompletableFuture<Response> invoke(String path, int count) {
		List<CompletableFuture<ScenarioContext>> futures = new ArrayList<>();

		long start = System.currentTimeMillis();
		for (int i = 0; i < count; i++) {
			ScenarioContext context = new ScenarioContext(restTemplate);
			context.attr().put("path", path);
			Scenario scenario = new CompositeScenario(scenarios);
			CompletableFuture<ScenarioContext> ret = scenarioExecutor.exec(scenario,
					context);
			futures.add(ret);
		}
		return CompletableFuture.allOf(futures.stream().toArray(CompletableFuture[]::new))
				.thenApply(v -> {
					long elapsed = System.currentTimeMillis() - start;
					double ave = ((double) elapsed) / count;
					double tps = ((double) count * 1000) / elapsed;
					return new Response(Result.OK, elapsed, ave, tps, null);
				}).exceptionally(e -> {
					e.printStackTrace();
					long elapsed = System.currentTimeMillis() - start;
					return new Response(Result.NG, elapsed, 0, 0, e.getMessage());
				});
	}

	public enum Result {
		OK, NG
	}

	@Data
	public static class Response {
		private final Result result;
		private final long elapsedMillis;
		private final double ave;
		private final double tps;
		private final String reason;
	}

}
