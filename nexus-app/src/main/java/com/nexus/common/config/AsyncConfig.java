package com.nexus.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.task.DelegatingSecurityContextAsyncTaskExecutor;

/**
 * Global configuration for @Async method execution.
 *
 * <p>Crucially, this configuration ensures the Spring Security context
 * (which contains the current Tenant ID via JWT) is propagated from the
 * HTTP request thread to the background async thread. Without this,
 * async DB queries would fail Row Level Security (RLS) checks because
 * the tenant_id would be missing.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean
    public TaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("NexusAsync-");
        executor.initialize();

        // Wrap the executor so the SecurityContext is copied to the new thread
        return new DelegatingSecurityContextAsyncTaskExecutor(executor);
    }
}
