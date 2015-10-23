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
public class Demo {
	public static void main(String[] args) {
		SpringApplication.run(Demo.class, args);
	}

	@Component
	public static class ExampleScenario implements Scenario {
		@Override
		public ScenarioContext exec(ScenarioContext context) {
			return context.request().get(context.attr().str("path")).responseAsDocument()
					.logBody().assertThatStatusCode(that -> that.isEqualTo(HttpStatus.OK))
					.assertThatStringOfBody(doc -> doc.title(),
							that -> that.isEqualTo("Example Domain"))
					.assertThatStringOfBody(doc -> doc.select("h1").text(),
							that -> that.isEqualTo("Example Domain"))
					.finish();
		}
	}
}
