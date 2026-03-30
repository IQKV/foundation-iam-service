package com.iqscaffold.iam.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Validated
@ConfigurationProperties(prefix = "iqscaffold.messaging.rabbitmq")
public record MessagingConfigurationProperties(
    @NotBlank String host,
    @Valid @NotNull Credentials credentials,
    @Valid @NotNull Exchanges exchanges,
    @Valid @NotNull Queues queues,
    @Valid @NotNull RoutingKeys routingKeys,
    boolean enabled
) {

    public record Credentials(@NotBlank String username, @NotBlank String password) {}

    public record Exchanges(@NotBlank String events, @NotBlank String dlx) {}

    public record Queues(
        @NotBlank String userEvents,
        @NotBlank String notifications,
        @NotBlank String tenantProvisioning,
        @NotBlank String dlq
    ) {}

    public record RoutingKeys(
        @NotBlank String tenantCreated,
        @NotBlank String tenantUpdated,
        @NotBlank String tenantDeleted,
        @NotBlank String userCreated,
        @NotBlank String userUpdated,
        @NotBlank String userDeleted,
        @NotBlank String notificationEmail
    ) {}
}
