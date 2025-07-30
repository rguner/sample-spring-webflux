package pl.piomin.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.concurrent.Executor;

import jakarta.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

@SpringBootApplication
// https://github.com/micrometer-metrics/micrometer-samples/blob/main/webflux/src/main/java/com/example/micrometer/WebFluxApplication.java
public class SampleSpringWebFluxApp {

    private static final Logger LOGGER = LoggerFactory.getLogger(SampleSpringWebFluxApp.class);

    public static void main(String[] args) {
        SpringApplication.run(SampleSpringWebFluxApp.class, args);
    }

    @Bean(name = "subscriberTaskExecutor")
    public ThreadPoolTaskExecutor taskExecutor1() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("subscriber-");
        executor.setTaskDecorator(mdcTaskDecorator());
        executor.initialize();
        return executor;
    }

    @Bean(name = "publisherTaskExecutor")
    public ThreadPoolTaskExecutor taskExecutor2() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("publisher-");
        executor.setTaskDecorator(mdcTaskDecorator());
        executor.initialize();
        return executor;
    }

    @Bean(name = "singleThreadTaskExecutor")
    public ThreadPoolTaskExecutor taskExecutor3() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("publisher-single-thread-");
        executor.setTaskDecorator(mdcTaskDecorator());
        executor.initialize();
        return executor;
    }

    @Bean
    public TaskDecorator mdcTaskDecorator() {
        return runnable -> {
            var contextMap = MDC.getCopyOfContextMap();
            return () -> {
                if (contextMap != null) {
                    MDC.setContextMap(contextMap);
                }
                try {
                    runnable.run();
                } finally {
                    MDC.clear();
                }
            };
        };
    }

    @Value("${target.uri}")
    private String targetUri;

    @Bean
    public WebClient webClient() {
        return WebClient.builder().baseUrl(targetUri).build();
    }

    @PostConstruct
    public void init() {
        LOGGER.info("CPU: {}", Runtime.getRuntime().availableProcessors());
    }

}
