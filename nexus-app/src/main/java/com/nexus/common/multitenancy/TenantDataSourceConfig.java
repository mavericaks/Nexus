package com.nexus.common.multitenancy;

import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * Wraps the application's {@link DataSource} with {@link TenantAwareDataSource}
 * so that every transaction automatically sets the RLS tenant context.
 *
 * <p>Uses a {@link BeanPostProcessor} to wrap the DataSource after Spring
 * Boot auto-configures HikariCP — this preserves all pool settings while
 * adding the tenant-aware behavior on top.
 *
 * <p>The Flyway DataSource is configured separately (different user/password),
 * but wrapping it is harmless: when Flyway runs, {@link TenantContext} is
 * empty, so no {@code SET LOCAL} is executed.
 */
@Configuration
public class TenantDataSourceConfig {

    @Bean
    static BeanPostProcessor tenantAwareDataSourcePostProcessor() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) {
                if (bean instanceof DataSource ds && !(bean instanceof TenantAwareDataSource)) {
                    return new TenantAwareDataSource(ds);
                }
                return bean;
            }
        };
    }
}
