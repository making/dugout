package dugout;

import java.lang.annotation.*;

import org.springframework.context.annotation.Import;

import dugout.config.DugoutConfiguration;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Import(DugoutConfiguration.class)
public @interface DugoutApplication {
}
