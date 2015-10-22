package dugout.config;

import lombok.Data;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "dugout")
@Data
public class DugoutConfigProperties {
	private String path;
	private int count = 1;
}
