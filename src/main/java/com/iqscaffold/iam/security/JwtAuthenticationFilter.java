/*
 * Copyright 2026 IQKV Foundation Team.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.iqscaffold.iam.security;

import java.io.IOException;
import java.time.Instant;
import java.util.UUID;

import com.iqscaffold.iam.denylist.TokenDenylistService;
import com.iqscaffold.iam.user.UserMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Checks two token revocation conditions before Spring Security processes the JWT:
 * <ol>
 *   <li>JTI present in {@code token_denylist} — covers regular signout</li>
 *   <li>Token {@code iat} &le; {@code users.last_global_signout_at} — covers signout-all</li>
 * </ol>
 * Returns 401 with a problem+json body if either condition is met.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
  private static final String TOKEN_REVOKED_BODY = "{\"title\":\"Token revoked\",\"status\":401}";

  private final JwtDecoder jwtDecoder;
  private final TokenDenylistService tokenDenylistService;
  private final UserMapper userMapper;

  public JwtAuthenticationFilter(final JwtDecoder jwtDecoder,
                                  final TokenDenylistService tokenDenylistService,
                                  final UserMapper userMapper) {
    this.jwtDecoder = jwtDecoder;
    this.tokenDenylistService = tokenDenylistService;
    this.userMapper = userMapper;
  }

  @Override
  protected void doFilterInternal(final HttpServletRequest request,
                                  final HttpServletResponse response,
                                  final FilterChain filterChain)
      throws ServletException, IOException {
    final String authorization = request.getHeader("Authorization");
    if (authorization == null || !authorization.startsWith("Bearer ")) {
      filterChain.doFilter(request, response);
      return;
    }

    final Jwt jwt;
    try {
      jwt = jwtDecoder.decode(authorization.substring(7));
    } catch (final JwtException e) {
      log.debug("JWT decode failed in revocation filter: {}", e.getMessage());
      filterChain.doFilter(request, response);
      return;
    }

    final String jti = jwt.getId();
    final String userIdClaim = jwt.getClaimAsString(JwtClaimNames.USER_ID);

    // Check 1: JTI denylist (regular signout)
    if (jti != null && tokenDenylistService.isRevoked(jti)) {
      writeRevoked(response);
      return;
    }

    // Check 2: Global signout — iat <= last_global_signout_at
    if (userIdClaim != null) {
      try {
        final UUID userId = UUID.fromString(userIdClaim);
        final Instant issuedAt = jwt.getIssuedAt();
        final var lastGlobalSignout = userMapper.findLastGlobalSignoutAt(userId);
        if (issuedAt != null && lastGlobalSignout.isPresent()
            && !issuedAt.isAfter(lastGlobalSignout.get())) {
          writeRevoked(response);
          return;
        }
      } catch (final IllegalArgumentException e) {
        log.debug("Invalid userId claim in JWT: {}", userIdClaim);
      }
    }

    filterChain.doFilter(request, response);
  }

  @Override
  protected boolean shouldNotFilter(final HttpServletRequest request) {
    final String path = request.getRequestURI();
    final String method = request.getMethod();

    if (path.startsWith("/actuator/") || path.startsWith("/api-docs/") || path.startsWith("/swagger-ui/")) {
      return true;
    }

    return ("POST".equalsIgnoreCase(method) && path.equals("/api/v1/iam/auth/signup"))
        || ("POST".equalsIgnoreCase(method) && path.equals("/api/v1/iam/users/tenants"))
        || ("POST".equalsIgnoreCase(method) && path.equals("/api/v1/iam/auth/signin"))
        || ("POST".equalsIgnoreCase(method) && path.equals("/api/v1/iam/auth/refresh"))
        || ("POST".equalsIgnoreCase(method) && path.equals("/api/v1/iam/auth/validate"))
        || ("POST".equalsIgnoreCase(method) && path.equals("/api/v1/iam/users/email/verify"))
        || ("POST".equalsIgnoreCase(method) && path.equals("/api/v1/iam/users/email/resend-verification"))
        || ("POST".equalsIgnoreCase(method) && path.equals("/api/v1/iam/users/password/forgot"))
        || ("POST".equalsIgnoreCase(method) && path.equals("/api/v1/iam/users/password/reset"));
  }

  private void writeRevoked(final HttpServletResponse response) throws IOException {
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    response.setContentType("application/problem+json");
    response.getWriter().write(TOKEN_REVOKED_BODY);
  }
}
