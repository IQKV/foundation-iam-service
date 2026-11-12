/*
 * Copyright 2026 iQKV Foundation Team.
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

package com.iqkv.foundation.iamservice.infrastructure.messaging;

import java.util.List;

import com.iqkv.foundation.iamservice.security.JwtClaimNames;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;

/**
 * Authenticates the STOMP {@code CONNECT} frame so personal user destinations resolve.
 *
 * <p>SockJS does not reliably forward the HTTP {@code Authorization} header, so the UI sends the
 * JWT in the STOMP {@code CONNECT} frame headers instead. Without this interceptor the WebSocket
 * session carries no {@link java.security.Principal}, so
 * {@code SimpMessagingTemplate.convertAndSendToUser(userId, ...)} finds no session registered under
 * that user and silently drops every push.
 *
 * <p>The principal name is set to the {@code user_id} claim — not {@code sub}, which holds the
 * email — because publishers address users by their UUID.
 */
@Component
public class WebSocketAuthInterceptor implements ChannelInterceptor {

  private static final Logger log = LoggerFactory.getLogger(WebSocketAuthInterceptor.class);

  private static final String AUTHORIZATION_HEADER = "Authorization";
  private static final String BEARER_PREFIX = "Bearer ";

  private final JwtDecoder jwtDecoder;

  public WebSocketAuthInterceptor(final JwtDecoder jwtDecoder) {
    this.jwtDecoder = jwtDecoder;
  }

  @Override
  public Message<?> preSend(final Message<?> message, final MessageChannel channel) {
    final StompHeaderAccessor accessor =
        MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
    if (accessor == null || !StompCommand.CONNECT.equals(accessor.getCommand())) {
      return message;
    }

    final String authorization = accessor.getFirstNativeHeader(AUTHORIZATION_HEADER);
    if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
      throw new MessagingException("Missing bearer token on STOMP CONNECT frame");
    }

    final Jwt jwt;
    try {
      jwt = jwtDecoder.decode(authorization.substring(BEARER_PREFIX.length()));
    } catch (final JwtException e) {
      throw new MessagingException("Invalid bearer token on STOMP CONNECT frame", e);
    }

    final String userId = jwt.getClaimAsString(JwtClaimNames.USER_ID);
    if (userId == null || userId.isBlank()) {
      throw new MessagingException("JWT is missing the user_id claim");
    }

    accessor.setUser(new UsernamePasswordAuthenticationToken(userId, null, authorities(jwt)));
    log.debug("Authenticated STOMP CONNECT for user {}", userId);
    return message;
  }

  private static List<SimpleGrantedAuthority> authorities(final Jwt jwt) {
    final List<String> authorities = jwt.getClaimAsStringList(JwtClaimNames.AUTHORITIES);
    if (authorities == null) {
      return List.of();
    }
    return authorities.stream().map(SimpleGrantedAuthority::new).toList();
  }
}
