package com.iqscaffold.iam.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "iqscaffold.database")
public record DatabaseConfigurationProperties(
    String url,
    String username,
    String password,
    int poolSize
) {}
