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
      if (tenantId == null) {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        response.setContentType("application/problem+json");
        response.getWriter().write("{\"title\":\"Tenant ID required\",\"status\":400}");
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

    if (path.startsWith("/actuator/") || path.startsWith("/api-docs/") || path.startsWith("/swagger-ui/")) {
      return true;
    }

    return ("POST".equalsIgnoreCase(method) && path.equals("/api/v1/iam/auth/signup"))
           || ("POST".equalsIgnoreCase(method) && path.equals("/api/v1/iam/users/tenants"))
           || ("POST".equalsIgnoreCase(method) && path.equals("/api/v1/iam/auth/validate"))
           || ("POST".equalsIgnoreCase(method) && path.equals("/api/v1/iam/users/email/verify"))
           || ("POST".equalsIgnoreCase(method) && path.equals("/api/v1/iam/users/email/resend-verification"))
           || ("POST".equalsIgnoreCase(method) && path.equals("/api/v1/iam/users/password/forgot"))
           || ("POST".equalsIgnoreCase(method) && path.equals("/api/v1/iam/users/password/reset"));
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
