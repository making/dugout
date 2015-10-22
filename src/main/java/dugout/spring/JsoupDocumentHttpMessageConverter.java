package dugout.spring;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.util.StreamUtils;

public class JsoupDocumentHttpMessageConverter implements HttpMessageConverter<Document> {
	@Override
	public boolean canRead(Class<?> clazz, MediaType mediaType) {
		return Document.class.isAssignableFrom(clazz);
	}

	@Override
	public boolean canWrite(Class<?> clazz, MediaType mediaType) {
		return Document.class.isAssignableFrom(clazz);
	}

	@Override
	public List<MediaType> getSupportedMediaTypes() {
		return Arrays.asList(MediaType.TEXT_HTML);
	}

	@Override
	public Document read(Class<? extends Document> clazz, HttpInputMessage inputMessage)
			throws IOException, HttpMessageNotReadableException {
		Charset charset = inputMessage.getHeaders().getContentType().getCharSet();
		if (charset == null) {
			charset = StandardCharsets.UTF_8;
		}
		String body = StreamUtils.copyToString(inputMessage.getBody(), charset);
		return Jsoup.parse(body);
	}

	@Override
	public void write(Document document, MediaType contentType,
			HttpOutputMessage outputMessage)
					throws IOException, HttpMessageNotWritableException {
		throw new UnsupportedOperationException("writing document is not supported!");
	}
}
