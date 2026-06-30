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

package com.iqkv.foundation.iamservice.tenancy;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

import com.iqkv.foundation.iamservice.security.JwtClaimNames;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Servlet filter that resolves the tenant key from the {@code X-Tenant-ID} header (priority 1)
 * or the JWT {@code tenant_id} claim (priority 2) and sets {@link TenantContext}.
 *
 * <p>Returns 400 with a problem+json body when the tenant cannot be resolved.
 * Always clears the tenant context in a {@code finally} block.
 *
 * <p>Tenant resolution is intentionally skipped for platform-scoped paths that operate
 * cross-tenant and never require a {@link TenantContext}:
 * <ul>
 *   <li>{@code /api/v1/iam/admin/**} — platform operator CRUD (users, tenants, etc.)</li>
 *   <li>{@code /api/v1/iam/auth/admin/me/**} — platform admin self-service account</li>
 *   <li>{@code /api/v1/iam/auth/signout} and {@code /api/v1/iam/auth/signout-all} — token revocation</li>
 *   <li>{@code /api/v1/iam/users/notifications/**} — stored in {@code public} schema with
 *       fully-qualified SQL; accessible to both tenant users and platform admins whose
 *       tokens carry a null {@code tenant_id}.</li>
 *   <li>{@code GET /api/v1/iam/locales} — platform-wide locale list, stored in {@code public} schema.</li>
 *   <li>{@code GET /api/v1/iam/announcements} — platform-wide announcements, stored in {@code public} schema.</li>
 *   <li>{@code /api/v1/iam/auth/oauth2/**} — OAuth2/OIDC endpoints manage tenant resolution separately.</li>
 *   <li>{@code /.well-known/**} — public JWKS endpoint</li>
 *   <li>Actuator, API docs, Swagger UI</li>
 * </ul>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class TenantExtractionFilter extends OncePerRequestFilter {

  private static final String TENANT_HEADER = "X-Tenant-ID";

  private final JwtDecoder jwtDecoder;

  public TenantExtractionFilter(final JwtDecoder jwtDecoder) {
    this.jwtDecoder = jwtDecoder;
  }

  @Override
  protected void doFilterInternal(final HttpServletRequest request,
                                  final HttpServletResponse response,
                                  final FilterChain filterChain)
      throws ServletException, IOException {
    try {
      final String tenantId = resolveTenantId(request);
      if (tenantId == null || tenantId.isBlank()) {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        response.setContentType("application/problem+json");
        response.getWriter().write("{\"title\":\"Bad Request\",\"status\":400,\"detail\":\"Missing or invalid tenant context\"}");
        return;
      }
      TenantContext.setCurrentTenant(tenantId);
      filterChain.doFilter(request, response);
    } finally {
      TenantContext.clear();
    }
  }

  @Override
  protected boolean shouldNotFilter(final HttpServletRequest request) {
    final String path = request.getRequestURI();
    final String method = request.getMethod();

    // WebSocket / SockJS — handshake and transport paths.
    // The SockJS /info probe and upgrade request carry no JWT or tenant header
    // (browsers cannot set custom headers on WebSocket upgrades).
    // Tenant context is resolved post-handshake via the STOMP CONNECT frame.
    if (path.startsWith("/api/v1/iam/ws/") || path.equals("/api/v1/iam/ws")) {
      return true;
    }

    // Infrastructure / docs — always skip
    if (path.equals("/api-docs")
        || path.startsWith("/api-docs/")
        || path.equals("/actuator")
        || path.startsWith("/actuator/")
        || path.startsWith("/swagger-ui/")
        || path.startsWith("/.well-known/")) {
      return true;
    }

    // Platform-admin paths — cross-tenant by design, no tenant context ever required.
    // Admin JWTs carry a null tenant_id claim intentionally (see adminSignIn).
    if (path.startsWith("/api/v1/iam/admin/")
        || path.startsWith("/api/v1/iam/auth/admin/me")) {
      return true;
    }

    // Token revocation — operates on the token itself, not a tenant schema
    if (path.equals("/api/v1/iam/auth/signout") || path.equals("/api/v1/iam/auth/signout-all")) {
      return true;
    }

    // User notifications — stored in public.user_notifications with fully schema-qualified SQL.
    // No tenant schema routing needed; accessible to both tenant users and platform admins
    // (whose tokens carry a null tenant_id).
    if (path.startsWith("/api/v1/iam/users/notifications")) {
      return true;
    }

    // OAuth2/OIDC — authorize/callback/link flows do not use header-based tenant extraction.
    // Tenant context is carried in signed state or handled explicitly by the controller.
    if (path.startsWith("/api/v1/iam/auth/oauth2/")) {
      return true;
    }

    // Public / tenant-managed endpoints (either tenant context is irrelevant or
    // the controller sets it manually via X-Tenant-ID header)
    return ("POST".equalsIgnoreCase(method) && path.equals("/api/v1/iam/auth/signup"))
           || ("POST".equalsIgnoreCase(method) && path.equals("/api/v1/iam/users/tenants"))
           || ("POST".equalsIgnoreCase(method) && path.equals("/api/v1/iam/auth/exchange"))
           || ("POST".equalsIgnoreCase(method) && path.equals("/api/v1/iam/auth/validate"))
           || ("POST".equalsIgnoreCase(method) && path.equals("/api/v1/iam/users/email/verify"))
           || ("POST".equalsIgnoreCase(method) && path.equals("/api/v1/iam/users/email/resend-verification"))
           || ("POST".equalsIgnoreCase(method) && path.equals("/api/v1/iam/users/password/forgot"))
           || ("POST".equalsIgnoreCase(method) && path.equals("/api/v1/iam/users/password/reset"))
           || ("POST".equalsIgnoreCase(method) && path.equals("/api/v1/iam/auth/signin"))
           || ("POST".equalsIgnoreCase(method) && path.equals("/api/v1/iam/auth/admin/signin"))
           || ("POST".equalsIgnoreCase(method) && path.equals("/api/v1/iam/auth/refresh"))
           || ("POST".equalsIgnoreCase(method) && path.equals("/api/v1/iam/auth/admin/refresh"))
           || ("POST".equalsIgnoreCase(method) && path.startsWith("/api/v1/iam/auth/magic-link/"))
           || ("GET".equalsIgnoreCase(method) && path.equals("/api/v1/iam/locales"))
           || ("GET".equalsIgnoreCase(method) && path.equals("/api/v1/iam/announcements"));
  }

  private String resolveTenantId(final HttpServletRequest request) {
    // Priority 1: X-Tenant-ID header
    final String header = request.getHeader(TENANT_HEADER);
    if (header != null && !header.isBlank()) {
      return header;
    }

    // Priority 2: JWT tenant_id claim from Bearer token
    final String authorization = request.getHeader("Authorization");
    if (authorization != null && authorization.startsWith("Bearer ")) {
      try {
        final Jwt jwt = jwtDecoder.decode(authorization.substring(7));
        return jwt.getClaimAsString(JwtClaimNames.TENANT_ID);
      } catch (final JwtException e) {
        return null;
      }
    }

    return null;
  }
}
