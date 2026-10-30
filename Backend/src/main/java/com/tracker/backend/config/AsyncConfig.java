package com.tracker.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * * Registration and "forgot password" must answer the browser immediately, even when
 * * Brevo is slow or temporarily unreachable. Emails are therefore sent on a small,
 * * bounded background pool instead of the HTTP request thread.
 *
 * * The queue is bounded on purpose: if Brevo is down we drop email work rather than
 * * letting an unbounded backlog eat the container's memory.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    public static final String EMAIL_EXECUTOR = "emailTaskExecutor";

    @Bean(name = EMAIL_EXECUTOR)
    public Executor emailTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("email-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(10);
        executor.initialize();
        return executor;
    }
}
