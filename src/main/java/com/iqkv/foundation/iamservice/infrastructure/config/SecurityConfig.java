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

package com.iqkv.foundation.iamservice.infrastructure.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.List;

import com.iqkv.foundation.iamservice.security.JwtAuthenticationFilter;
import com.iqkv.foundation.iamservice.security.JwtClaimNames;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

  private final AuthConfigurationProperties authProps;
  private final JwtAuthenticationFilter jwtAuthenticationFilter;
  private final ResourceLoader resourceLoader;

  public SecurityConfig(final AuthConfigurationProperties authProps,
                        @Lazy final JwtAuthenticationFilter jwtAuthenticationFilter,
                        final ResourceLoader resourceLoader) {
    this.authProps = authProps;
    this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    this.resourceLoader = resourceLoader;
  }

  @Bean
  public SecurityFilterChain securityFilterChain(final HttpSecurity http) throws Exception {
    http
        .csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/actuator/**").permitAll()
            .requestMatchers("/api-docs/**").permitAll()
            .requestMatchers("/swagger-ui/**").permitAll()
            .requestMatchers("/.well-known/**").permitAll()
            .requestMatchers(HttpMethod.POST, "/api/v1/iam/auth/signup").permitAll()
            .requestMatchers(HttpMethod.POST, "/api/v1/iam/users/tenants").permitAll()
            .requestMatchers(HttpMethod.POST, "/api/v1/iam/auth/signin").permitAll()
            .requestMatchers(HttpMethod.POST, "/api/v1/iam/auth/admin/signin").permitAll()
            .requestMatchers(HttpMethod.POST, "/api/v1/iam/auth/refresh").permitAll()
            .requestMatchers(HttpMethod.POST, "/api/v1/iam/auth/validate").permitAll()
            .requestMatchers(HttpMethod.POST, "/api/v1/iam/users/email/verify").permitAll()
            .requestMatchers(HttpMethod.POST, "/api/v1/iam/users/email/resend-verification").permitAll()
            .requestMatchers(HttpMethod.POST, "/api/v1/iam/users/password/forgot").permitAll()
            .requestMatchers(HttpMethod.POST, "/api/v1/iam/users/password/reset").permitAll()
            .requestMatchers("/api/v1/iam/admin/**").hasAuthority("PLATFORM_ADMIN")
            // Invitation accept flow — public (no JWT required)
            .requestMatchers(HttpMethod.GET, "/api/v1/iam/invitations/*").permitAll()
            .requestMatchers(HttpMethod.POST, "/api/v1/iam/invitations/*/accept").permitAll()
            .requestMatchers(HttpMethod.GET, "/api/v1/iam/tenants/**").hasAuthority("TENANT_OWNER")
            .requestMatchers(HttpMethod.PATCH, "/api/v1/iam/tenants/**").hasAuthority("TENANT_OWNER")
            .anyRequest().authenticated()
        )
        .oauth2ResourceServer(oauth2 -> oauth2
            .jwt(jwt -> jwt
                .decoder(jwtDecoder())
                .jwtAuthenticationConverter(jwtAuthenticationConverter())
            )
        )
        .addFilterBefore(jwtAuthenticationFilter, BearerTokenAuthenticationFilter.class);

    return http.build();
  }

  @Bean
  public JwtDecoder jwtDecoder() {
    try {
      final String publicKeyPath = authProps.jwt().publicKeyPath();
      final String pem;
      try (InputStream is = resourceLoader.getResource(publicKeyPath).getInputStream()) {
        pem = new String(is.readAllBytes(), StandardCharsets.UTF_8);
      }
      final String stripped = pem
          .replace("-----BEGIN PUBLIC KEY-----", "")
          .replace("-----END PUBLIC KEY-----", "")
          .replaceAll("\\s", "");
      final byte[] keyBytes = Base64.getDecoder().decode(stripped);
      final KeyFactory keyFactory = KeyFactory.getInstance("RSA");
      final RSAPublicKey publicKey = (RSAPublicKey) keyFactory.generatePublic(new X509EncodedKeySpec(keyBytes));
      return NimbusJwtDecoder.withPublicKey(publicKey).build();
    } catch (final IOException | java.security.GeneralSecurityException e) {
      throw new IllegalStateException("Failed to load RSA public key for JWT decoding", e);
    }
  }

  @Bean
  public JwtAuthenticationConverter jwtAuthenticationConverter() {
    final var converter = new JwtAuthenticationConverter();
    converter.setJwtGrantedAuthoritiesConverter(jwt -> {
      final List<String> authorities = jwt.getClaimAsStringList(JwtClaimNames.AUTHORITIES);
      if (authorities == null) {
        return List.of();
      }
      return authorities.stream()
          .map(SimpleGrantedAuthority::new)
          .map(a -> (GrantedAuthority) a)
          .toList();
    });
    return converter;
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(12);
  }
}
