package demo;

import java.util.Random;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import dugout.DugoutApplication;
import dugout.core.Attr;
import dugout.core.Scenario;
import dugout.core.ScenarioContext;

@SpringBootApplication
@DugoutApplication
public class Demo {
	public static void main(String[] args) {
		SpringApplication.run(Demo.class, args);
	}

	@Component
	public static class TopPageScenario implements Scenario {
		@Override
		public ScenarioContext exec(ScenarioContext context) {
			return context.request("============================== Check Top Page")
					.get(context.attr().str("path")).responseAsDocument()
					.assertThatStatusCode(that -> that.isEqualTo(HttpStatus.FOUND))
					.transfer(res -> context.attr().put("location",
							res.getHeaders().getLocation().toString()))
					.finish();
		}
	}

	@Component
	public static class ListPageScenario implements Scenario {
		@Override
		public ScenarioContext exec(ScenarioContext context) {
			Attr attr = context.attr();
			return context.request("=============================== Check Todo List")
					.get(attr.str("path") + "/todo/list").responseAsDocument().logBody()
					.assertThatStatusCode(that -> that.isEqualTo(HttpStatus.OK))
					.assertThatStringOfBody(doc -> doc.title(),
							that -> that.isEqualTo("Todo List"))
					.assertThatStringOfBody(doc -> doc.select("h1").text(),
							that -> that.isEqualTo("Todo List"))
					.assertThatBody(doc -> doc.select("#todoForm > form").size(),
							that -> that.isEqualTo(1))
					.assertThatBody(doc -> doc.select("#todoList").size(),
							that -> that.isEqualTo(1))
					.finish();
		}
	}

	@Component
	public static class CreateScenario implements Scenario {
		Random random = new Random(System.currentTimeMillis());

		@Override
		public ScenarioContext exec(ScenarioContext context) {
			Attr attr = context.attr();
			int r = random.nextInt();
			return context.request("=============================== Create Todo")
					.post(attr.str("path") + "/todo/create")
					.form(f -> f.param("todoTitle", "Hello World " + r))
					.responseAsDocument().logBody()
					.assertThatStatusCode(that -> that.isEqualTo(HttpStatus.FOUND))
					.transfer(res -> attr.put("r", r))
					.transfer(res -> attr.put("location",
							res.getHeaders().getLocation().toString()))
					.transferCookie().then()
					.request("====================================== Redirected")
					.get(attr.str("location")).responseAsDocument().logBody()
					.assertThatStatusCode(that -> that.isEqualTo(HttpStatus.OK))
					.assertThatStringOfBody(doc -> doc.title(),
							that -> that.isEqualTo("Todo List"))
					.assertThatStringOfBody(doc -> doc.select("h1").text(),
							that -> that.isEqualTo("Todo List"))
					.assertThatStringOfBody(
							doc -> doc.select(".alert-success > span").text(),
							that -> that.isEqualTo(
									"Created successfully!"))
					.assertThatBody(doc -> doc.select("#todoList > ul > li").stream()
							.filter(e -> e.select("span").text()
									.equals("Hello World "
											+ attr.get("r", Integer.class)))
							.count(), that -> that.isEqualTo(1L))
					.finish();
		}
	}

	@Component
	public static class CleanupScenario implements Scenario {
		@Override
		public ScenarioContext exec(ScenarioContext context) {
			return context.request("============================== CleanUp")
					.get(context.attr().str("path") + "/todo/list").responseAsDocument()
					.transferFromBody(doc -> {
						doc.select("form[action=/todo/delete] > input[name=todoId]")
								.parallelStream().map(e -> e.val()).forEach(todoId -> {
							context.request()
									.post(context.attr().str("path") + "/todo/delete")
									.form(f -> f.param("todoId", todoId))
									.responseAsDocument().logBody();
						});
					}).finish();
		}
	}
}
