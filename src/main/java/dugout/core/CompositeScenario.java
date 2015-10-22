package dugout.core;

import java.util.List;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CompositeScenario implements Scenario {
	private final List<Scenario> scenarios;

	public CompositeScenario(List<Scenario> scenarios) {
		this.scenarios = scenarios;
	}

	@Override
	public ScenarioContext exec(ScenarioContext context) {
		for (Scenario scenario : scenarios) {
			log.info("start {}", scenario.getClass());
			context = scenario.exec(context);
		}
		return context;
	}
}
