package demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import dugout.DugoutApplication;
import dugout.core.Scenario;
import dugout.core.ScenarioContext;

@SpringBootApplication
@DugoutApplication
public class App {
	public static void main(String[] args) {
		SpringApplication.run(App.class, args);
	}

	@Component
	public static class BeerScenario implements Scenario {
		@Override
		public ScenarioContext exec(ScenarioContext context) {
			return context.request().get(context.attr().str("path")).responseAsDocument()
					.assertThatStringOfBody(doc -> doc.select("#gallery").text(),
							that -> that.isEqualTo("Loading..."))
					.assertThatStatusCode(that -> that.isEqualTo(HttpStatus.OK))
					.assertThatString(res -> res.getHeaders().getFirst("X-Cf-Requestid"),
							that -> that.hasSize(36))
					.then();
		}
	}
}
