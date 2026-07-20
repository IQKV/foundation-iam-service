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

import java.security.interfaces.RSAPublicKey;
import java.util.List;

import com.iqkv.foundation.iamservice.security.JwtAuthenticationFilter;
import com.iqkv.foundation.iamservice.security.JwtClaimNames;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
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

  private final RSAPublicKey jwtPublicKey;
  private final JwtAuthenticationFilter jwtAuthenticationFilter;

  public SecurityConfig(final RSAPublicKey jwtPublicKey,
                        @Lazy final JwtAuthenticationFilter jwtAuthenticationFilter) {
    this.jwtPublicKey = jwtPublicKey;
    this.jwtAuthenticationFilter = jwtAuthenticationFilter;
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
            .requestMatchers(HttpMethod.GET, "/api/v1/iam/auth/signup/status/*").permitAll()
            .requestMatchers(HttpMethod.POST, "/api/v1/iam/users/tenants").permitAll()
            .requestMatchers(HttpMethod.POST, "/api/v1/iam/auth/signin").permitAll()
            .requestMatchers(HttpMethod.POST, "/api/v1/iam/auth/admin/signin").permitAll()
            .requestMatchers(HttpMethod.POST, "/api/v1/iam/auth/admin/refresh").permitAll()
            .requestMatchers(HttpMethod.POST, "/api/v1/iam/auth/refresh").permitAll()
            .requestMatchers(HttpMethod.POST, "/api/v1/iam/auth/validate").permitAll()
            .requestMatchers(HttpMethod.POST, "/api/v1/iam/users/email/verify").permitAll()
            .requestMatchers(HttpMethod.POST, "/api/v1/iam/users/email/resend-verification").permitAll()
            .requestMatchers(HttpMethod.POST, "/api/v1/iam/users/password/forgot").permitAll()
            .requestMatchers(HttpMethod.POST, "/api/v1/iam/users/password/reset").permitAll()
            .requestMatchers(HttpMethod.POST, "/api/v1/iam/auth/magic-link/initiate").permitAll()
            .requestMatchers(HttpMethod.POST, "/api/v1/iam/auth/magic-link/resend").permitAll()
            .requestMatchers(HttpMethod.POST, "/api/v1/iam/auth/magic-link/exchange").permitAll()
            .requestMatchers(HttpMethod.GET, "/api/v1/iam/locales").permitAll()
            .requestMatchers(HttpMethod.GET, "/api/v1/iam/announcements").permitAll()
            .requestMatchers("/api/v1/iam/ws/**").permitAll()
            .requestMatchers(HttpMethod.GET, "/api/v1/iam/auth/oauth2/authorize").permitAll()
            .requestMatchers(HttpMethod.GET, "/api/v1/iam/auth/oauth2/callback").permitAll()
            .requestMatchers(HttpMethod.GET, "/api/v1/iam/auth/oauth2/link/callback").permitAll()
            .requestMatchers(HttpMethod.GET, "/api/v1/iam/auth/oauth2/providers").permitAll()
            .requestMatchers(HttpMethod.POST, "/api/v1/iam/auth/oauth2/exchange").permitAll()
            .requestMatchers("/api/v1/iam/admin/**").hasAuthority("PLATFORM_ADMIN")
            // Invitation accept flow — public (no JWT required)
            .requestMatchers(HttpMethod.GET, "/api/v1/iam/invitations/*").permitAll()
            .requestMatchers(HttpMethod.POST, "/api/v1/iam/invitations/*/accept").permitAll()
            .requestMatchers(HttpMethod.GET, "/api/v1/iam/tenants/**").hasAnyAuthority("TENANT_OWNER", "ADMIN")
            .requestMatchers(HttpMethod.PATCH, "/api/v1/iam/tenants/**").hasAnyAuthority("TENANT_OWNER", "ADMIN")
            .anyRequest().authenticated()
        )
        .oauth2ResourceServer(oauth2 -> oauth2
            .jwt(jwt -> jwt
                .decoder(jwtDecoder())
                .jwtAuthenticationConverter(jwtAuthenticationConverter())
            )
        )
        .oauth2Client(Customizer.withDefaults())
        .addFilterBefore(jwtAuthenticationFilter, BearerTokenAuthenticationFilter.class);

    return http.build();
  }

  @Bean
  public JwtDecoder jwtDecoder() {
    return NimbusJwtDecoder.withPublicKey(jwtPublicKey).build();
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
