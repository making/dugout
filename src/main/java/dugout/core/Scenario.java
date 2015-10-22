package dugout.core;

import java.io.Serializable;

public interface Scenario extends Serializable {
	ScenarioContext exec(ScenarioContext context);
}
