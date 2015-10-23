package dugout.config;

import static j2html.TagCreator.*;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import dugout.core.ScenarioExecutor;
import dugout.core.ScenarioInvoker;
import dugout.spring.JsonPathDocumentContextHttpMessageConverter;
import dugout.spring.JsoupDocumentHttpMessageConverter;

@Configuration
@EnableAsync
public class DugoutConfiguration {

	@Bean
	DugoutConfigProperties dugoutConfigProperties() {
		return new DugoutConfigProperties();
	}

	@Bean
	RestTemplate restTemplate() {
		Logger log = LoggerFactory.getLogger("httptrace");
		RestTemplate restTemplate = new RestTemplate();
		restTemplate.getMessageConverters().add(new JsoupDocumentHttpMessageConverter());
		restTemplate.getMessageConverters()
				.add(new JsonPathDocumentContextHttpMessageConverter());
		restTemplate.getInterceptors().add((req, body, execution) -> {
			if (log.isInfoEnabled()) {
				log.info("Req URI    : " + req.getURI());
				log.info("Req Method : " + req.getMethod());
				log.info("Req Header : " + req.getHeaders());
				log.info("Req Body   : " + new String(body).replace("\n", ""));
			}
			ClientHttpResponse res = execution.execute(req, body);
			try {
				if (log.isInfoEnabled()) {
					log.info("Res Header : " + res.getHeaders());
					// log.info("Res Body : " + StreamUtils
					// .copyToString(res.getBody(), StandardCharsets.UTF_8)
					// .replace("\n", ""));
					log.info("Res Status : " + res.getStatusCode());
				}
			}
			catch (IOException e) {
				// ignore
			}
			return res;
		});

		restTemplate.setRequestFactory(new SimpleClientHttpRequestFactory() {
			@Override
			protected void prepareConnection(HttpURLConnection connection,
					String httpMethod) throws IOException {
				super.prepareConnection(connection, httpMethod);
				connection.setInstanceFollowRedirects(false);
			}
		});

		restTemplate.setErrorHandler(new DefaultResponseErrorHandler() {
			@Override
			public void handleError(ClientHttpResponse res) throws IOException {
				log.warn("Res Body : "
						+ StreamUtils.copyToString(res.getBody(), StandardCharsets.UTF_8)
								.replace("\n", ""));
			}
		});
		return restTemplate;
	}

	@Bean
	ScenarioExecutor scenarioExecutor() {
		return new ScenarioExecutor();
	}

	@Bean
	ScenarioInvoker scenarioInvoker() {
		return new ScenarioInvoker();
	}

	@Bean
	@ConditionalOnProperty(name = "spring.main.web-environment", havingValue = "false")
	CommandLineRunner runner(ScenarioInvoker invoker, DugoutConfigProperties config) {
		return (args) -> {
			invoker.invoke(config.getPath(), config.getCount()).thenAccept(x -> {
				System.out.println(x);
			});
		};
	}

	@RestController
	public static class HomeController {
		@Autowired
		ScenarioInvoker scenarioInvoker;
		@Autowired
		DugoutConfigProperties config;

		@RequestMapping("/")
		String home() {
			return body().with(h1("Heading!").withClass("example"), p("Hello!"),
					form().withAction("start").withMethod("post")
							.with(input().withType("text").withName("path")
									.withValue(config.getPath()),
							input().withType("number").withName("count")
									.withValue(String.valueOf(config.getCount())),
							input().withType("submit").withValue("Start")))
					.render();
		}

		@RequestMapping(value = "/start", method = RequestMethod.POST)
		CompletableFuture<ScenarioInvoker.Response> start(
				@RequestParam(value = "path", defaultValue = "http://localhost:8080") String path,
				@RequestParam(value = "count", defaultValue = "1") int count) {
			return scenarioInvoker.invoke(path, count);
		}

	}
}