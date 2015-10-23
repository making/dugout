# Dugout

Dugout is Yet Another Benchmark and Scenario Test tool.

## Dependency

    <dependencies>
        <dependency>
            <groupId>am.ik.dugout</groupId>
            <artifactId>dugout</artifactId>
            <version>0.0.1-SNAPSHOT</version>
        </dependency>
        <!-- ... -->
    </dependencies>

    <repositories>
        <repository>
            <id>sonatype-snapshots</id>
            <name>Sonatype Snapshots</name>
            <url>https://oss.sonatype.org/content/repositories/snapshots</url>
            <snapshots>
                <enabled>true</enabled>
            </snapshots>
        </repository>
    </repositories>


## Write a scenario

    package demo;

    import org.springframework.http.HttpStatus;
    import org.springframework.stereotype.Component;
    
    import dugout.core.Scenario;
    import dugout.core.ScenarioContext;
    
    @Component
    public class ExampleScenario implements Scenario {
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

## Make the entry point

    package demo;
    
    import org.springframework.boot.SpringApplication;
    import org.springframework.boot.autoconfigure.SpringBootApplication;
    
    import dugout.DugoutApplication;
    
    @SpringBootApplication
    @DugoutApplication // Important!!
    public class Demo {
        public static void main(String[] args) {
            SpringApplication.run(Demo.class, args);
        }
    }

Define the path to test in `application.properties`

    dugout.path=http://example.com
    spring.main.web-environment=false

You can also run this application as a web application. In this case, no property is required.

## License

Licensed under the Apache License, Version 2.0.