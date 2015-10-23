package demo;

import java.util.Random;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
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

	@Order(0)
	@Component
	public static class TopPageScenario implements Scenario {
		@Override
		public ScenarioContext exec(ScenarioContext context) {
			return context.request("============================== Check Top Page")
					.get(context.attr().str("path")).responseAsDocument()
					.assertThatStatusCode(that -> that.isEqualTo(HttpStatus.FOUND))
					.transfer(res -> context.attr().put("location",
							res.getHeaders().getLocation().toString()))
					.then();
		}
	}

	@Order(1)
	@Component
	public static class ListPageScenario implements Scenario {
		@Override
		public ScenarioContext exec(ScenarioContext context) {
			Attr attr = context.attr();
			return context.request("=============================== Check Todo List")
					.get(attr.str("location")).responseAsDocument().logBody()
					.assertThatStatusCode(that -> that.isEqualTo(HttpStatus.OK))
					.assertThatStringOfBody(doc -> doc.title(),
							that -> that.isEqualTo("Todo List"))
					.assertThatStringOfBody(doc -> doc.select("h1").text(),
							that -> that.isEqualTo("Todo List"))
					.assertThatBody(doc -> doc.select("#todoForm > form").size(),
							that -> that.isEqualTo(1))
					.transferFromBody(
							doc -> attr.put("action", doc.select("form").attr("action")))
					.assertThatBody(doc -> doc.select("#todoList").size(),
							that -> that.isEqualTo(1))
					.then();
		}
	}

	@Order(2)
	@Component
	public static class CreateScenario implements Scenario {
		Random random = new Random(System.currentTimeMillis());

		@Override
		public ScenarioContext exec(ScenarioContext context) {
			Attr attr = context.attr();
			int r = random.nextInt();
			return context.request("=============================== Create Todo")
					.post(attr.str("path") + attr.str("action"))
					.form(f -> f.param("todoTitle", "Hello World " + r))
					.responseAsDocument().logBody()
					.assertThatStatusCode(that -> that.isEqualTo(HttpStatus.FOUND))
					.assertThatString(res -> res.getHeaders().getLocation().toString(),
							that -> that.startsWith(attr.str("location")))
					.transfer(res -> attr.put("r", r)).transferCookie().then()
					.request("====================================== Redirected")
					.get(attr.str("location")).responseAsDocument().logBody()
					.assertThatStatusCode(that -> that.isEqualTo(HttpStatus.OK))
					.assertThatStringOfBody(
							doc -> doc.select(".alert-success > span").text(),
							that -> that.isEqualTo(
									"Created successfully!"))
					.assertThatBody(doc -> doc.select("#todoList > ul > li").stream()
							.filter(e -> e.select("span").text()
									.equals("Hello World "
											+ attr.get("r", Integer.class)))
							.count(), that -> that.isEqualTo(1L))
					.then();
		}
	}

	@Order(Ordered.HIGHEST_PRECEDENCE)
	@Component
	public static class CleanupScenario implements Scenario {
		@Override
		public ScenarioContext exec(ScenarioContext context) {
			context.request("============================== CleanUp")
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
			return context;
		}
	}
}
