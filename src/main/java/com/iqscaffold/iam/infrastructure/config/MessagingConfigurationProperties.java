package com.iqscaffold.iam.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "iqscaffold.messaging.rabbitmq")
public record MessagingConfigurationProperties(
    String host,
    Credentials credentials,
    Exchanges exchanges,
    Queues queues,
    RoutingKeys routingKeys,
    boolean enabled
) {

    public record Credentials(String username, String password) {}

    public record Exchanges(String events, String dlx) {}

    public record Queues(
        String userEvents,
        String notifications,
        String tenantProvisioning,
        String dlq
    ) {}

    public record RoutingKeys(
        String tenantCreated,
        String tenantUpdated,
        String tenantDeleted,
        String userCreated,
        String userUpdated,
        String userDeleted,
        String notificationEmail
    ) {}
}
