package com.iqkv.foundation.iamservice.oauth2;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import com.iqkv.foundation.iamservice.authentication.JwtTokenGenerator;
import com.iqkv.foundation.iamservice.authentication.dto.AuthenticationDtos;
import com.iqkv.foundation.iamservice.infrastructure.config.OAuth2ConfigurationProperties;
import com.iqkv.foundation.iamservice.oauth2.mapper.UserIdentityMapper;
import com.iqkv.foundation.iamservice.shared.util.UserServiceConstants;
import com.iqkv.foundation.iamservice.signup.SignupResult;
import com.iqkv.foundation.iamservice.signup.SignupStrategy;
import com.iqkv.foundation.iamservice.user.User;
import com.iqkv.foundation.iamservice.user.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OidcUserProvisioningServiceImpl implements OidcUserProvisioningService {

  private static final Logger log = LoggerFactory.getLogger(OidcUserProvisioningServiceImpl.class);

  private final UserIdentityMapper userIdentityMapper;
  private final UserMapper userMapper;
  private final SignupStrategy signupStrategy;
  private final OAuth2ConfigurationProperties oauth2Props;
  private final JwtTokenGenerator jwtTokenGenerator;

  public OidcUserProvisioningServiceImpl(
      final UserIdentityMapper userIdentityMapper,
      final UserMapper userMapper,
      final SignupStrategy signupStrategy,
      final OAuth2ConfigurationProperties oauth2Props,
      final JwtTokenGenerator jwtTokenGenerator
  ) {
    this.userIdentityMapper = userIdentityMapper;
    this.userMapper = userMapper;
    this.signupStrategy = signupStrategy;
    this.oauth2Props = oauth2Props;
    this.jwtTokenGenerator = jwtTokenGenerator;
  }

  @Override
  @Transactional
  public AuthenticationDtos.TokenResponse provisionAndIssueTokens(OidcIdentity identity, String tenantKey) {
    final Optional<UserIdentity> existingIdentity =
        userIdentityMapper.findByProviderAndProviderSub(identity.provider(), identity.providerSub());

    if (existingIdentity.isPresent()) {
      final UserIdentity ui = existingIdentity.get();
      userIdentityMapper.updateLastUsedAt(ui.getId());

      final User user = userMapper.findById(ui.getUserId())
          .orElseThrow(() -> new OidcProvisioningException("User not found"));

      // Resolve tenant: if tenantKey provided, use that; otherwise default
      String resolvedTenantKey = tenantKey != null && !tenantKey.isBlank() ? tenantKey : "platform";

      // Get authorities and issue tokens (reusing password flow logic)
      // Note: For now, assume platform tenant or default tenant based on setup
      // In real implementation, you'd check memberships for the resolved tenant
      // For simplicity here, let's use "platform" as default
      final var authorities = java.util.List.of(UserServiceConstants.AUTHORITY_MEMBER);

      final String accessToken = jwtTokenGenerator.generateAccessToken(user, resolvedTenantKey, authorities);
      final String refreshToken = jwtTokenGenerator.generateRefreshToken(user, resolvedTenantKey);

      return new AuthenticationDtos.TokenResponse(accessToken, refreshToken, resolvedTenantKey);
    }

    // Identity not found: check if user exists by email and auto-link
    Optional<User> existingUserByEmail = Optional.empty();
    if (identity.email() != null && identity.emailVerified() && oauth2Props.autoLink().enabled()) {
      existingUserByEmail = userMapper.findByEmail(identity.email());
    }

    if (existingUserByEmail.isPresent()) {
      // Auto-link scenario
      log.info("Auto-linking new identity provider {} for existing user: {}",
          identity.provider(), identity.email());

      final User user = existingUserByEmail.get();
      final UserIdentity newIdentity = new UserIdentity();
      newIdentity.setId(UUID.randomUUID());
      newIdentity.setUserId(user.getId());
      newIdentity.setProvider(identity.provider());
      newIdentity.setProviderSub(identity.providerSub());
      newIdentity.setEmail(identity.email());
      newIdentity.setDisplayName(identity.firstName() + " " + identity.lastName());
      newIdentity.setAvatarUrl(identity.avatarUrl());
      newIdentity.setLinkedAt(LocalDateTime.now());
      newIdentity.setLastUsedAt(LocalDateTime.now());
      userIdentityMapper.insert(newIdentity);

      // Issue tokens
      String resolvedTenantKey = tenantKey != null && !tenantKey.isBlank() ? tenantKey : "platform";
      final var authorities = java.util.List.of(UserServiceConstants.AUTHORITY_MEMBER);
      final String accessToken = jwtTokenGenerator.generateAccessToken(user, resolvedTenantKey, authorities);
      final String refreshToken = jwtTokenGenerator.generateRefreshToken(user, resolvedTenantKey);

      return new AuthenticationDtos.TokenResponse(accessToken, refreshToken, resolvedTenantKey);
    }

    // Provision new user
    if (!oauth2Props.autoProvisionUsers()) {
      throw new OidcProvisioningException("Auto-provisioning is disabled");
    }

    // Create user via existing signup strategy
    // Note: SignupStrategy expects password; for OIDC we'll set a dummy password hash
    // Since the user won't use password login initially
    final var dummyRequest = new com.iqkv.foundation.iamservice.user.dto.UserDtos.RegisterUserRequest(
        identity.firstName(),
        identity.lastName(),
        identity.email(),
        "oauth2-dummy-password-not-used",
        null // tenantName ignored based on strategy
    );

    final SignupResult signupResult = signupStrategy.execute(dummyRequest);

    // Update user: mark email verified, set avatar, etc.
    final User newUser = signupResult.user();
    newUser.setEmailVerified(true);
    newUser.setAvatarUrl(identity.avatarUrl());
    newUser.setProfileCompleted(true);
    userMapper.update(newUser);

    // Link identity
    final UserIdentity newIdentity = new UserIdentity();
    newIdentity.setId(UUID.randomUUID());
    newIdentity.setUserId(newUser.getId());
    newIdentity.setProvider(identity.provider());
    newIdentity.setProviderSub(identity.providerSub());
    newIdentity.setEmail(identity.email());
    newIdentity.setDisplayName(identity.firstName() + " " + identity.lastName());
    newIdentity.setAvatarUrl(identity.avatarUrl());
    newIdentity.setLinkedAt(LocalDateTime.now());
    newIdentity.setLastUsedAt(LocalDateTime.now());
    userIdentityMapper.insert(newIdentity);

    // Issue tokens
    final String accessToken = jwtTokenGenerator.generateAccessToken(
        newUser,
        signupResult.tenant().getTenantKey(),
        signupResult.authorities()
    );
    final String refreshToken = jwtTokenGenerator.generateRefreshToken(
        newUser,
        signupResult.tenant().getTenantKey()
    );

    log.info("New user provisioned via OAuth2/OIDC provider: {}", identity.provider());

    return new AuthenticationDtos.TokenResponse(
        accessToken,
        refreshToken,
        signupResult.tenant().getTenantKey()
    );
  }
}
