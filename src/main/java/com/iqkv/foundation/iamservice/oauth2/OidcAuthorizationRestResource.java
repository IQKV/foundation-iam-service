package com.iqkv.foundation.iamservice.oauth2;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iqkv.foundation.iamservice.authentication.dto.AuthenticationDtos;
import com.iqkv.foundation.iamservice.infrastructure.config.OAuth2ConfigurationProperties;
import com.iqkv.foundation.iamservice.oauth2.dto.OidcDtos;
import com.iqkv.foundation.iamservice.oauth2.mapper.UserIdentityMapper;
import com.iqkv.foundation.iamservice.security.JwtClaimNames;
import com.iqkv.foundation.iamservice.user.User;
import com.iqkv.foundation.iamservice.user.UserMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/api/v1/iam/auth/oauth2")
@Tag(name = "OAuth2/OIDC Authentication", description = "Social login, account linking, OIDC endpoints")
public class OidcAuthorizationRestResource {

  private static final Logger log = LoggerFactory.getLogger(OidcAuthorizationRestResource.class);
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().findAndRegisterModules();
  private final OAuth2ConfigurationProperties oauth2Props;
  private final ClientRegistrationRepository clientRegistrationRepository;
  private final OidcStateJwtService oidcStateJwtService;
  private final OidcStateStore oidcStateStore;
  private final OidcUserProvisioningService provisioningService;
  private final GitHubEmailFetcher gitHubEmailFetcher;
  private final UserIdentityMapper userIdentityMapper;
  private final UserMapper userMapper;
  private final RestTemplate restTemplate = new RestTemplate();

  private static final SecureRandom SECURE_RANDOM = new SecureRandom();

  public OidcAuthorizationRestResource(
      final OAuth2ConfigurationProperties oauth2Props,
      final ClientRegistrationRepository clientRegistrationRepository,
      final OidcStateJwtService oidcStateJwtService,
      final OidcStateStore oidcStateStore,
      final OidcUserProvisioningService provisioningService,
      final GitHubEmailFetcher gitHubEmailFetcher,
      final UserIdentityMapper userIdentityMapper,
      final UserMapper userMapper
  ) {
    this.oauth2Props = oauth2Props;
    this.clientRegistrationRepository = clientRegistrationRepository;
    this.oidcStateJwtService = oidcStateJwtService;
    this.oidcStateStore = oidcStateStore;
    this.provisioningService = provisioningService;
    this.gitHubEmailFetcher = gitHubEmailFetcher;
    this.userIdentityMapper = userIdentityMapper;
    this.userMapper = userMapper;
  }

  @GetMapping("/authorize")
  @Operation(summary = "Initiate browser-based OAuth2/OIDC authorization flow")
  @ApiResponses({
      @ApiResponse(responseCode = "302", description = "Redirect to provider authorization endpoint"),
      @ApiResponse(responseCode = "400", description = "Invalid provider")
  })
  public void authorize(
      @RequestParam(name = "provider") final String provider,
      @RequestParam(name = "tenantKey", required = false) final String tenantKey,
      final HttpServletResponse response
  ) throws IOException {
    redirectToAuthorization(
        provider,
        tenantKey,
        oauth2Props.postLoginRedirectUri(),
        "login",
        null,
        buildServerCallbackRedirectUri(),
        response
    );
  }

  @GetMapping("/callback")
  @Operation(summary = "Handle provider callback and issue IAM tokens")
  @ApiResponses({
      @ApiResponse(responseCode = "302", description = "Redirect to configured post-login callback with tokens"),
      @ApiResponse(responseCode = "400", description = "Invalid or expired OAuth2 state")
  })
  public void callback(
      @RequestParam(name = "code") final String code,
      @RequestParam(name = "state") final String stateToken,
      final HttpServletResponse response
  ) throws IOException {
    String redirectUri = oauth2Props.postLoginRedirectUri();
    try {
      final OidcState state = oidcStateJwtService.verifyState(stateToken);
      redirectUri = resolveRedirectUri(state);
      final String codeVerifier = oidcStateStore.retrieveAndRemove(state.jti());
      if (codeVerifier == null || codeVerifier.isBlank()) {
        throw new OidcProvisioningException("Invalid or expired OAuth2 state");
      }

      final AuthenticationDtos.TokenResponse tokenResponse = authenticate(
          state.provider(),
          code,
          codeVerifier,
          buildServerCallbackRedirectUri(),
          state.tenantKey(),
          state.nonce()
      );

      response.sendRedirect(buildSuccessRedirectUri(redirectUri, tokenResponse));
    } catch (final Exception ex) {
      log.warn("OIDC callback failed", ex);
      response.sendRedirect(buildErrorRedirectUri(redirectUri, ex.getMessage()));
    }
  }

  @GetMapping("/link/callback")
  @Operation(summary = "Handle provider callback for account linking")
  @ApiResponses({
      @ApiResponse(responseCode = "302", description = "Redirect to configured callback with link result"),
      @ApiResponse(responseCode = "400", description = "Invalid or expired OAuth2 state")
  })
  public void linkCallback(
      @RequestParam(name = "code") final String code,
      @RequestParam(name = "state") final String stateToken,
      final HttpServletResponse response
  ) throws IOException {
    String redirectUri = oauth2Props.postLoginRedirectUri();
    try {
      final OidcState state = oidcStateJwtService.verifyState(stateToken);
      validateLinkState(state);
      redirectUri = resolveRedirectUri(state);
      final String codeVerifier = oidcStateStore.retrieveAndRemove(state.jti());
      if (codeVerifier == null || codeVerifier.isBlank()) {
        throw new OidcProvisioningException("Invalid or expired OAuth2 state");
      }

      final OidcIdentity identity = resolveIdentity(
          state.provider(),
          code,
          codeVerifier,
          buildLinkCallbackRedirectUri(),
          state.nonce()
      );
      linkIdentity(state.userId(), identity);

      response.sendRedirect(buildLinkSuccessRedirectUri(redirectUri, identity.provider()));
    } catch (final Exception ex) {
      log.warn("OIDC link callback failed", ex);
      response.sendRedirect(buildLinkErrorRedirectUri(redirectUri, ex.getMessage()));
    }
  }

  @PostMapping("/exchange")
  @Operation(summary = "Exchange provider authorization code for IAM tokens")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Tokens issued"),
      @ApiResponse(responseCode = "400", description = "Invalid code, code verifier, or provider response")
  })
  public ResponseEntity<AuthenticationDtos.TokenResponse> exchange(
      @Valid @RequestBody final OidcDtos.OidcExchangeRequest request
  ) {
    final AuthenticationDtos.TokenResponse tokenResponse = authenticate(
        request.provider(),
        request.code(),
        request.codeVerifier(),
        request.redirectUri(),
        request.tenantKey(),
        null
    );
    return ResponseEntity.ok(tokenResponse);
  }

  @GetMapping("/providers")
  @Operation(summary = "Get list of enabled OAuth2/OIDC providers")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "List of providers")
  })
  public ResponseEntity<OidcDtos.EnabledProvidersResponse> getEnabledProviders() {
    final List<String> enabledProviders = oauth2Props.enabledProviders().stream()
        .filter(this::hasClientRegistration)
        .toList();
    return ResponseEntity.ok(new OidcDtos.EnabledProvidersResponse(enabledProviders));
  }

  private void redirectToAuthorization(final String provider,
                                       final String tenantKey,
                                       final String redirectUri,
                                       final String flowType,
                                       final UUID userId,
                                       final String callbackRedirectUri,
                                       final HttpServletResponse response) throws IOException {
    response.sendRedirect(buildAuthorizationRequestUri(
        provider,
        tenantKey,
        redirectUri,
        flowType,
        userId,
        callbackRedirectUri
    ));
  }

  private String buildAuthorizationRequestUri(final String provider,
                                              final String tenantKey,
                                              final String redirectUri,
                                              final String flowType,
                                              final UUID userId,
                                              final String callbackRedirectUri) {
    final ClientRegistration clientRegistration = requireClientRegistration(provider);

    final UUID stateJti = UUID.randomUUID();
    final String nonce = generateRandomString(32);
    final String codeVerifier = generateCodeVerifier();
    final String codeChallenge = pkceChallenge(codeVerifier);

    oidcStateStore.store(stateJti, codeVerifier, oauth2Props.stateTtl().getSeconds());

    final OidcState state = new OidcState(
        stateJti,
        provider,
        nonce,
        tenantKey,
        redirectUri,
        flowType,
        userId,
        Instant.now().plus(oauth2Props.stateTtl())
    );
    final String stateToken = oidcStateJwtService.generateState(state);

    final OAuth2AuthorizationRequest authorizationRequest = OAuth2AuthorizationRequest.authorizationCode()
        .clientId(clientRegistration.getClientId())
        .authorizationUri(clientRegistration.getProviderDetails().getAuthorizationUri())
        .redirectUri(callbackRedirectUri)
        .scopes(clientRegistration.getScopes())
        .state(stateToken)
        .additionalParameters(params -> params.putAll(Map.of(
            "nonce", nonce,
            "code_challenge", codeChallenge,
            "code_challenge_method", "S256"
        )))
        .attributes(attrs -> attrs.put("nonce", nonce))
        .build();

    return authorizationRequest.getAuthorizationRequestUri();
  }

  private String generateRandomString(int length) {
    byte[] bytes = new byte[length];
    SECURE_RANDOM.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  private String generateCodeVerifier() {
    return generateRandomString(96);
  }

  private String pkceChallenge(String codeVerifier) {
    try {
      byte[] bytes = codeVerifier.getBytes(StandardCharsets.UTF_8);
      java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
      byte[] digest = md.digest(bytes);
      return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
    } catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }

  private AuthenticationDtos.TokenResponse authenticate(final String provider,
                                                        final String code,
                                                        final String codeVerifier,
                                                        final String redirectUri,
                                                        final String tenantKey,
                                                        final String expectedNonce) {
    final OidcIdentity identity = resolveIdentity(provider, code, codeVerifier, redirectUri, expectedNonce);
    return provisioningService.provisionAndIssueTokens(identity, tenantKey);
  }

  private OidcIdentity resolveIdentity(final String provider,
                                       final String code,
                                       final String codeVerifier,
                                       final String redirectUri,
                                       final String expectedNonce) {
    final ClientRegistration clientRegistration = requireClientRegistration(provider);
    final TokenExchangeResult tokenExchangeResult =
        exchangeAuthorizationCode(clientRegistration, code, codeVerifier, redirectUri);
    return loadIdentity(provider, clientRegistration, tokenExchangeResult, expectedNonce);
  }

  private TokenExchangeResult exchangeAuthorizationCode(final ClientRegistration clientRegistration,
                                                        final String code,
                                                        final String codeVerifier,
                                                        final String redirectUri) {
    final HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
    headers.setAccept(List.of(MediaType.APPLICATION_JSON));

    final MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
    form.add("grant_type", "authorization_code");
    form.add("code", code);
    form.add("redirect_uri", redirectUri);
    form.add("client_id", clientRegistration.getClientId());
    if (codeVerifier != null && !codeVerifier.isBlank()) {
      form.add("code_verifier", codeVerifier);
    }
    if (clientRegistration.getClientSecret() != null && !clientRegistration.getClientSecret().isBlank()) {
      form.add("client_secret", clientRegistration.getClientSecret());
    }

    try {
      final ResponseEntity<Map> tokenResponse = restTemplate.exchange(
          clientRegistration.getProviderDetails().getTokenUri(),
          HttpMethod.POST,
          new HttpEntity<>(form, headers),
          Map.class
      );
      final Map<String, Object> body = tokenResponse.getBody();
      if (body == null || stringValue(body, "access_token") == null) {
        throw new OidcProvisioningException("Provider token response is missing access_token");
      }
      return new TokenExchangeResult(
          stringValue(body, "access_token"),
          stringValue(body, "id_token")
      );
    } catch (final RestClientException ex) {
      throw new OidcProvisioningException("Failed to exchange provider authorization code");
    }
  }

  private OidcIdentity loadIdentity(final String provider,
                                    final ClientRegistration clientRegistration,
                                    final TokenExchangeResult tokenExchangeResult,
                                    final String expectedNonce) {
    final Map<String, Object> idTokenClaims = parseIdTokenClaims(tokenExchangeResult.idToken());
    if (expectedNonce != null && !idTokenClaims.isEmpty()) {
      final String actualNonce = stringValue(idTokenClaims, "nonce");
      if (actualNonce == null || !expectedNonce.equals(actualNonce)) {
        throw new OidcProvisioningException("OIDC nonce validation failed");
      }
    }

    final Map<String, Object> userInfoClaims = fetchUserInfoClaims(clientRegistration, tokenExchangeResult.accessToken());
    if ("github".equals(provider)) {
      final String verifiedEmail = gitHubEmailFetcher.fetchVerifiedEmail(tokenExchangeResult.accessToken())
          .orElseThrow(() -> new OidcProvisioningException("GitHub account does not expose a verified email"));
      return OidcIdentity.fromGitHubClaims(userInfoClaims, verifiedEmail, tokenExchangeResult.idToken());
    }

    final Map<String, Object> mergedClaims = new LinkedHashMap<>(idTokenClaims);
    mergedClaims.putAll(userInfoClaims);
    if (mergedClaims.isEmpty()) {
      throw new OidcProvisioningException("Provider did not return any user identity claims");
    }
    return OidcIdentity.fromOidcClaims(provider, mergedClaims, tokenExchangeResult.idToken());
  }

  private Map<String, Object> fetchUserInfoClaims(final ClientRegistration clientRegistration,
                                                  final String accessToken) {
    final String userInfoUri = clientRegistration.getProviderDetails().getUserInfoEndpoint().getUri();
    if (userInfoUri == null || userInfoUri.isBlank()) {
      return Map.of();
    }

    final HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(accessToken);
    headers.setAccept(List.of(MediaType.APPLICATION_JSON));

    try {
      final ResponseEntity<Map> response = restTemplate.exchange(
          userInfoUri,
          HttpMethod.GET,
          new HttpEntity<>(headers),
          Map.class
      );
      final Map<String, Object> body = response.getBody();
      return body != null ? body : Map.of();
    } catch (final RestClientException ex) {
      throw new OidcProvisioningException("Failed to fetch provider user profile");
    }
  }

  private Map<String, Object> parseIdTokenClaims(final String rawIdToken) {
    if (rawIdToken == null || rawIdToken.isBlank()) {
      return Map.of();
    }
    final String[] parts = rawIdToken.split("\\.");
    if (parts.length < 2) {
      throw new OidcProvisioningException("Provider returned an invalid id_token");
    }
    try {
      final byte[] payload = Base64.getUrlDecoder().decode(parts[1]);
      return OBJECT_MAPPER.readValue(payload, new TypeReference<Map<String, Object>>() {
      });
    } catch (final IOException | IllegalArgumentException ex) {
      throw new OidcProvisioningException("Failed to decode provider id_token");
    }
  }

  private String buildServerCallbackRedirectUri() {
    return oauth2Props.baseUrl() + "/api/v1/iam/auth/oauth2/callback";
  }

  private String buildLinkCallbackRedirectUri() {
    return oauth2Props.baseUrl() + "/api/v1/iam/auth/oauth2/link/callback";
  }

  private String resolveRedirectUri(final OidcState state) {
    if (state.redirectUri() != null && !state.redirectUri().isBlank()) {
      return state.redirectUri();
    }
    return oauth2Props.postLoginRedirectUri();
  }

  private ClientRegistration requireClientRegistration(final String provider) {
    final ClientRegistration clientRegistration = clientRegistrationRepository.findByRegistrationId(provider);
    if (clientRegistration == null) {
      throw new OidcProvisioningException("Invalid provider");
    }
    return clientRegistration;
  }

  private boolean hasClientRegistration(final String provider) {
    return clientRegistrationRepository.findByRegistrationId(provider) != null;
  }

  private void validateLinkState(final OidcState state) {
    if (!"link".equals(state.flowType()) || state.userId() == null) {
      throw new OidcProvisioningException("Invalid account-linking state");
    }
  }

  private String buildSuccessRedirectUri(final String redirectUri,
                                         final AuthenticationDtos.TokenResponse tokenResponse) {
    return redirectUri + "#access_token=" + urlEncode(tokenResponse.accessToken())
        + "&refresh_token=" + urlEncode(tokenResponse.refreshToken())
        + "&tenant_key=" + urlEncode(tokenResponse.tenantKey());
  }

  private String buildErrorRedirectUri(final String redirectUri, final String message) {
    final String detail = (message == null || message.isBlank()) ? "Authentication failed" : message;
    return redirectUri + "#error=oauth2_callback_failed&error_description=" + urlEncode(detail);
  }

  private String buildLinkSuccessRedirectUri(final String redirectUri, final String provider) {
    return redirectUri + "#link_status=success&provider=" + urlEncode(provider);
  }

  private String buildLinkErrorRedirectUri(final String redirectUri, final String message) {
    final String detail = (message == null || message.isBlank()) ? "Account linking failed" : message;
    return redirectUri + "#error=oauth2_link_failed&error_description=" + urlEncode(detail);
  }

  private String urlEncode(final String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8);
  }

  private String stringValue(final Map<String, Object> attributes, final String key) {
    final Object value = attributes.get(key);
    return value != null ? String.valueOf(value) : null;
  }

  private UUID extractUserId(final Jwt jwt) {
    final String userId = jwt != null ? jwt.getClaimAsString(JwtClaimNames.USER_ID) : null;
    if (isBlank(userId)) {
      throw new OidcProvisioningException("Authenticated user context is missing");
    }
    return UUID.fromString(userId);
  }

  private User loadRequiredUser(final UUID userId) {
    return userMapper.findById(userId)
        .orElseThrow(() -> new OidcProvisioningException("User not found"));
  }

  private void linkIdentity(final UUID userId, final OidcIdentity identity) {
    final List<UserIdentity> currentIdentities = userIdentityMapper.findByUserId(userId);
    final UserIdentity existingProviderLink = currentIdentities.stream()
        .filter(candidate -> identity.provider().equals(candidate.getProvider()))
        .findFirst()
        .orElse(null);
    if (existingProviderLink != null) {
      if (identity.providerSub().equals(existingProviderLink.getProviderSub())) {
        userIdentityMapper.updateLastUsedAt(existingProviderLink.getId());
        return;
      }
      throw new OidcProvisioningException("This provider is already linked to a different external account");
    }

    final UserIdentity linkedIdentity = userIdentityMapper.findByProviderAndProviderSub(
            identity.provider(),
            identity.providerSub()
        )
        .orElse(null);
    if (linkedIdentity != null) {
      if (!userId.equals(linkedIdentity.getUserId())) {
        throw new OidcProvisioningException("This external identity is already linked to another account");
      }
      userIdentityMapper.updateLastUsedAt(linkedIdentity.getId());
      return;
    }

    final UserIdentity newIdentity = new UserIdentity();
    newIdentity.setId(UUID.randomUUID());
    newIdentity.setUserId(userId);
    newIdentity.setProvider(identity.provider());
    newIdentity.setProviderSub(identity.providerSub());
    newIdentity.setEmail(identity.email());
    newIdentity.setDisplayName(buildDisplayName(identity));
    newIdentity.setAvatarUrl(identity.avatarUrl());
    newIdentity.setLinkedAt(java.time.LocalDateTime.now());
    newIdentity.setLastUsedAt(java.time.LocalDateTime.now());
    userIdentityMapper.insert(newIdentity);
  }

  private String buildDisplayName(final OidcIdentity identity) {
    if (!isBlank(identity.firstName()) && !isBlank(identity.lastName())) {
      return identity.firstName() + " " + identity.lastName();
    }
    if (!isBlank(identity.firstName())) {
      return identity.firstName();
    }
    if (!isBlank(identity.lastName())) {
      return identity.lastName();
    }
    return identity.email();
  }

  private boolean isBlank(final String value) {
    return value == null || value.isBlank();
  }

  @GetMapping("/link/{provider}")
  @PreAuthorize("isAuthenticated()")
  @SecurityRequirement(name = "bearerAuth")
  @Operation(summary = "Link an additional identity provider to the current account")
  public void initiateLink(
      @PathVariable final String provider,
      @AuthenticationPrincipal final Jwt jwt,
      final HttpServletResponse response
  ) throws IOException {
    redirectToAuthorization(
        provider,
        null,
        oauth2Props.postLoginRedirectUri(),
        "link",
        extractUserId(jwt),
        buildLinkCallbackRedirectUri(),
        response
    );
  }

  @GetMapping("/link/{provider}/authorize-url")
  @PreAuthorize("isAuthenticated()")
  @SecurityRequirement(name = "bearerAuth")
  @Operation(summary = "Get provider authorization URL for account linking")
  public ResponseEntity<OidcDtos.AuthorizationUrlResponse> getLinkAuthorizationUrl(
      @PathVariable final String provider,
      @AuthenticationPrincipal final Jwt jwt
  ) {
    final String url = buildAuthorizationRequestUri(
        provider,
        null,
        oauth2Props.postLoginRedirectUri(),
        "link",
        extractUserId(jwt),
        buildLinkCallbackRedirectUri()
    );
    return ResponseEntity.ok(new OidcDtos.AuthorizationUrlResponse(url));
  }

  @DeleteMapping("/link/{provider}")
  @PreAuthorize("isAuthenticated()")
  @SecurityRequirement(name = "bearerAuth")
  @Operation(summary = "Unlink an identity provider from the current account")
  public ResponseEntity<Void> unlink(
      @PathVariable final String provider,
      @AuthenticationPrincipal final Jwt jwt
  ) {
    final UUID userId = extractUserId(jwt);
    final List<UserIdentity> identities = userIdentityMapper.findByUserId(userId);
    final boolean providerLinked = identities.stream().anyMatch(identity -> provider.equals(identity.getProvider()));
    if (!providerLinked) {
      return ResponseEntity.noContent().build();
    }

    final User user = loadRequiredUser(userId);
    if (identities.size() <= 1 && isBlank(user.getPasswordHash())) {
      throw new OidcProvisioningException("Cannot unlink the last sign-in method from this account");
    }

    userIdentityMapper.deleteByUserIdAndProvider(userId, provider);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/identities")
  @PreAuthorize("isAuthenticated()")
  @SecurityRequirement(name = "bearerAuth")
  @Operation(summary = "Get linked identities for the current user")
  public ResponseEntity<List<OidcDtos.LinkedIdentityResponse>> getIdentities(
      @AuthenticationPrincipal final Jwt jwt
  ) {
    final UUID userId = extractUserId(jwt);
    final List<OidcDtos.LinkedIdentityResponse> identities = userIdentityMapper.findByUserId(userId).stream()
        .map(identity -> new OidcDtos.LinkedIdentityResponse(
            identity.getProvider(),
            identity.getDisplayName(),
            identity.getEmail(),
            identity.getAvatarUrl(),
            identity.getLinkedAt() != null ? identity.getLinkedAt().toString() : null
        ))
        .toList();
    return ResponseEntity.ok(identities);
  }

  private record TokenExchangeResult(
      String accessToken,
      String idToken
  ) {
  }
}
