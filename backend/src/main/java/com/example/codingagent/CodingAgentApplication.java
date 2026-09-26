package com.example.codingagent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableAsync
public class CodingAgentApplication {
    public static void main(String[] args) {
        boolean isEval = java.util.Arrays.asList(args).contains("--eval")
                || "true".equalsIgnoreCase(System.getenv("EVAL_MODE"))
                || "true".equalsIgnoreCase(System.getProperty("eval.mode"));

        SpringApplication app = new SpringApplication(CodingAgentApplication.class);
        if (isEval) {
            app.setWebApplicationType(org.springframework.boot.WebApplicationType.NONE);
        }
        app.run(args);
    }
}
