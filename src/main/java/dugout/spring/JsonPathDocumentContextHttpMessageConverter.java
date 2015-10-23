package dugout.spring;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.util.StreamUtils;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;

public class JsonPathDocumentContextHttpMessageConverter
		implements HttpMessageConverter<DocumentContext> {

	@Override
	public boolean canRead(Class<?> aClass, MediaType mediaType) {
		return DocumentContext.class.isAssignableFrom(aClass);
	}

	@Override
	public boolean canWrite(Class<?> aClass, MediaType mediaType) {
		return DocumentContext.class.isAssignableFrom(aClass);
	}

	@Override
	public List<MediaType> getSupportedMediaTypes() {
		return Arrays.asList(MediaType.APPLICATION_JSON);
	}

	@Override
	public DocumentContext read(Class<? extends DocumentContext> aClass,
			HttpInputMessage inputMessage)
					throws IOException, HttpMessageNotReadableException {
		Charset charset = inputMessage.getHeaders().getContentType().getCharSet();
		if (charset == null) {
			charset = StandardCharsets.UTF_8;
		}
		String body = StreamUtils.copyToString(inputMessage.getBody(), charset);
		return JsonPath.parse(body);
	}

	@Override
	public void write(DocumentContext documentContext, MediaType mediaType,
			HttpOutputMessage httpOutputMessage)
					throws IOException, HttpMessageNotWritableException {
		throw new UnsupportedOperationException("writing document is not supported!");
	}
}
