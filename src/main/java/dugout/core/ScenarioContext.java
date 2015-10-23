package dugout.core;

import java.net.HttpCookie;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

import org.assertj.core.api.AbstractCharSequenceAssert;
import org.assertj.core.api.AbstractComparableAssert;
import org.assertj.core.api.AbstractObjectAssert;
import org.assertj.core.api.Assertions;
import org.jsoup.nodes.Document;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.jayway.jsonpath.DocumentContext;

@Slf4j
public class ScenarioContext {
	private final Attr attr = new Attr();
	private final RestTemplate restTemplate;

	public ScenarioContext(RestTemplate restTemplate) {
		this.restTemplate = restTemplate;
	}

	public Attr attr() {
		return attr;
	}

	public RequestPreparingPhase request() {
		return new RequestPreparingPhase("");
	}

	public RequestPreparingPhase request(String requestName) {
		return new RequestPreparingPhase(requestName);
	}

	public class ResponseVerificationPhase<T> {
		protected final ResponseEntity<T> responseEntity;

		public ResponseVerificationPhase(ResponseEntity<T> responseEntity) {
			this.responseEntity = responseEntity;
		}

		public ScenarioContext then() {
			return ScenarioContext.this;
		}

		public ScenarioContext finish() {
			String path = attr().str("path");
			attr().clear().put("path", path);
			return ScenarioContext.this;
		}

		public ResponseVerificationPhase<T> transfer(
				Consumer<ResponseEntity<T>> consumer) {
			consumer.accept(this.responseEntity);
			return this;
		}

		public ResponseVerificationPhase<T> transferFromBody(Consumer<T> consumer) {
			consumer.accept(this.responseEntity.getBody());
			return this;
		}

		public ResponseVerificationPhase<T> transferCookie() {
			List<String> headers = responseEntity.getHeaders().get("Set-Cookie");

			if (!headers.isEmpty()) {
				List<HttpCookie> cookies = headers.stream()
						.flatMap(header -> HttpCookie.parse(header).stream())
						.collect(Collectors.toList());
				ScenarioContext.this.attr.put("cookie",
						cookies.stream().map(c -> c.getName() + "=" + c.getValue())
								.collect(Collectors.joining("; ")));
			}
			return this;
		}

		public ResponseVerificationPhase<T> logBody() {
			if (log.isInfoEnabled()) {
				Object body = responseEntity.getBody();
				if (body != null) {
					body = body.toString().replace("\n", "");
				}
				log.info("Res Body   : {}", body);
			}
			return this;
		}

		public <S> ResponseVerificationPhase<T> assertThat(
				Function<ResponseEntity<T>, S> res,
				Consumer<AbstractObjectAssert<?, S>> assertConsumer) {
			S target = res.apply(this.responseEntity);
			AbstractObjectAssert<?, S> objectAssert = Assertions.<S>assertThat(target);
			assertConsumer.accept(objectAssert);
			return this;
		}

		public ResponseVerificationPhase<T> assertThatString(
				Function<ResponseEntity<T>, String> res,
				Consumer<AbstractCharSequenceAssert<?, String>> assertConsumer) {
			String target = res.apply(this.responseEntity);
			AbstractCharSequenceAssert<?, String> objectAssert = Assertions
					.assertThat(target);
			assertConsumer.accept(objectAssert);
			return this;
		}

		public ResponseVerificationPhase<T> assertThatStatusCode(
				Consumer<AbstractComparableAssert<?, HttpStatus>> assertConsumer) {
			AbstractComparableAssert<?, HttpStatus> objectAssert = Assertions
					.<HttpStatus>assertThat(this.responseEntity.getStatusCode());
			assertConsumer.accept(objectAssert);
			return this;
		}

		public <S> ResponseVerificationPhase<T> assertThatBody(Function<T, S> body,
				Consumer<AbstractObjectAssert<?, S>> assertConsumer) {
			S target = body.apply(this.responseEntity.getBody());
			AbstractObjectAssert<?, S> objectAssert = Assertions.<S>assertThat(target);
			assertConsumer.accept(objectAssert);
			return this;
		}

		public ResponseVerificationPhase<T> assertThatStringOfBody(
				Function<T, String> body,
				Consumer<AbstractCharSequenceAssert<?, String>> assertConsumer) {
			String target = body.apply(this.responseEntity.getBody());
			AbstractCharSequenceAssert<?, String> objectAssert = Assertions
					.assertThat(target);
			assertConsumer.accept(objectAssert);
			return this;
		}
	}

	public class StringResponseVerificationPhase
			extends ResponseVerificationPhase<String> {
		public StringResponseVerificationPhase(ResponseEntity<String> responseEntity) {
			super(responseEntity);
		}

		public StringResponseVerificationPhase assertThatStringBody(
				BiConsumer<AbstractCharSequenceAssert<?, String>, Attr> assertConsumer) {
			String target = this.responseEntity.getBody();
			AbstractCharSequenceAssert<?, String> objectAssert = Assertions
					.assertThat(target);
			assertConsumer.accept(objectAssert, ScenarioContext.this.attr);
			return this;
		}
	}

	public class RequestPreparingPhase {
		private final String requestName;
		private RequestEntity.HeadersBuilder headersBuilder;
		private Object requestBody;

		public RequestPreparingPhase(String requestName) {
			this.requestName = requestName;
		}

		public RequestPreparingPhase get(String path) {
			this.headersBuilder = RequestEntity.get(URI.create(path));
			return this;
		}

		public RequestPreparingPhase get(String path,
				Consumer<QueryParamsBuilder> queryParamsCreator) {
			QueryParamsBuilder builder = new QueryParamsBuilder(
					UriComponentsBuilder.fromUriString(path));
			queryParamsCreator.accept(builder);
			this.headersBuilder = RequestEntity
					.get(builder.uriComponentsBuilder.build().toUri());
			return this;
		}

		public RequestPreparingPhase post(String path) {
			this.headersBuilder = RequestEntity.post(URI.create(path));
			return this;

		}

		public RequestPreparingPhase body(Object body) {
			if (this.headersBuilder instanceof RequestEntity.BodyBuilder) {
				this.requestBody = body;
				RequestEntity.BodyBuilder.class.cast(this.headersBuilder).body(body);
			}
			else {
				throw new IllegalStateException("cannot set body");
			}
			return this;
		}

		public RequestPreparingPhase form(Consumer<FormParamsBuilder> consumer) {
			FormParamsBuilder builder = new FormParamsBuilder();
			consumer.accept(builder);
			this.contentType(MediaType.APPLICATION_FORM_URLENCODED);
			return body(builder.getBody());
		}

		RequestEntity<?> buildRequestEntity() {
			if (requestName != null) {
				log.info("{}", requestName);
			}
			if (ScenarioContext.this.attr.exists("cookie", String.class)) {
				this.headersBuilder.header("Cookie",
						ScenarioContext.this.attr.str("cookie"));
			}
			RequestEntity<?> requestEntity;
			if (this.requestBody != null) {
				requestEntity = RequestEntity.BodyBuilder.class.cast(this.headersBuilder)
						.body(this.requestBody);
			}
			else {
				requestEntity = this.headersBuilder.build();
			}
			return requestEntity;
		}

		public <T> ResponseVerificationPhase<T> response(Class<T> responseType) {
			ResponseEntity<T> res = ScenarioContext.this.restTemplate
					.exchange(buildRequestEntity(), responseType);
			return new ResponseVerificationPhase<>(res);
		}

		public <T> ResponseVerificationPhase<T> response(
				ParameterizedTypeReference<T> responseType) {
			ResponseEntity<T> res = ScenarioContext.this.restTemplate
					.exchange(buildRequestEntity(), responseType);
			return new ResponseVerificationPhase<>(res);
		}

		public StringResponseVerificationPhase responseAsString() {
			ResponseEntity<String> res = ScenarioContext.this.restTemplate
					.exchange(buildRequestEntity(), String.class);
			return new StringResponseVerificationPhase(res);
		}

		public ResponseVerificationPhase<Document> responseAsDocument() {
			ResponseEntity<Document> res = ScenarioContext.this.restTemplate
					.exchange(buildRequestEntity(), Document.class);
			return new ResponseVerificationPhase<>(res);
		}

		public ResponseVerificationPhase<DocumentContext> responseAsJson() {
			ResponseEntity<DocumentContext> res = ScenarioContext.this.restTemplate
					.exchange(buildRequestEntity(), DocumentContext.class);
			return new ResponseVerificationPhase<>(res);
		}

		public RequestPreparingPhase header(String headerName, String... headerValues) {
			this.headersBuilder.header(headerName, headerValues);
			return this;
		}

		public RequestPreparingPhase accept(MediaType... acceptableMediaTypes) {
			this.headersBuilder.accept(acceptableMediaTypes);
			return this;
		}

		public RequestPreparingPhase acceptCharset(Charset... acceptableCharsets) {
			this.headersBuilder.acceptCharset(acceptableCharsets);
			return this;
		}

		public RequestPreparingPhase ifModifiedSince(long ifModifiedSince) {
			this.headersBuilder.ifModifiedSince(ifModifiedSince);
			return this;
		}

		public RequestPreparingPhase ifNoneMatch(String... ifNoneMatches) {
			this.headersBuilder.ifNoneMatch(ifNoneMatches);
			return this;
		}

		public RequestPreparingPhase contentLength(long contentLength) {
			if (this.headersBuilder instanceof RequestEntity.BodyBuilder) {
				RequestEntity.BodyBuilder.class.cast(this.headersBuilder)
						.contentLength(contentLength);
			}
			else {
				throw new IllegalStateException("cannot set contentLength");
			}
			return this;
		}

		public RequestPreparingPhase contentType(MediaType contentType) {
			if (this.headersBuilder instanceof RequestEntity.BodyBuilder) {
				RequestEntity.BodyBuilder.class.cast(this.headersBuilder)
						.contentType(contentType);
			}
			else {
				throw new IllegalStateException("cannot set contentType");
			}
			return this;
		}

		public class QueryParamsBuilder {
			private final UriComponentsBuilder uriComponentsBuilder;

			QueryParamsBuilder(UriComponentsBuilder uriComponentsBuilder) {
				this.uriComponentsBuilder = uriComponentsBuilder;
			}

			public QueryParamsBuilder query(String name, Object... values) {
				this.uriComponentsBuilder.queryParam(name, values);
				return this;
			}
		}

		public class FormParamsBuilder {
			private final MultiValueMap<String, String> form = new LinkedMultiValueMap<>();

			public FormParamsBuilder param(String name, String... values) {
				this.form.put(name, Arrays.asList(values));
				return this;
			}

			MultiValueMap<String, String> getBody() {
				return this.form;
			}

		}

		public class RequestBodyPreparingPhase {
			private final Object body;

			public RequestBodyPreparingPhase(Object body) {
				this.body = body;
			}

			Object getBody() {
				return this.body;
			}
		}
	}

}
