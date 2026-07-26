package com.nexus.common.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * Creates a secondary {@link DataSource} for auth queries that need
 * to bypass RLS.
 *
 * <p><b>Why a separate DataSource?</b></p>
 * <p>The primary DataSource connects as {@code nexus_app} — a role
 * with RLS enforced, so every query is filtered by tenant. During
 * login, we don't know the tenant yet (the user hasn't authenticated).
 * If we use the primary DataSource, RLS filters out all users,
 * and nobody can log in.</p>
 *
 * <p>This secondary DataSource connects as the DB owner ({@code nexus}),
 * which bypasses RLS. It's used <b>only</b> by
 * {@link NexusUserDetailsService} for the login query — nothing else
 * should use it.</p>
 */
@Configuration
public class AuthDataSourceConfig {

    @Bean("authDataSource")
    @ConfigurationProperties(prefix = "nexus.security.auth-datasource")
    public DataSource authDataSource() {
        return DataSourceBuilder.create().build();
    }
}
