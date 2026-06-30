package com.iqkv.foundation.iamservice.oauth2;

import java.util.Arrays;
import java.util.Optional;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class GitHubEmailFetcher {

  private final RestTemplate restTemplate = new RestTemplate();

  public Optional<String> fetchVerifiedEmail(String accessToken) {
    final String url = "https://api.github.com/user/emails";
    final HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(accessToken);
    headers.set("Accept", "application/vnd.github+json");

    final HttpEntity<Void> entity = new HttpEntity<>(headers);

    final GitHubEmail[] emails = restTemplate.exchange(
        url,
        HttpMethod.GET,
        entity,
        GitHubEmail[].class
    ).getBody();

    if (emails == null || emails.length == 0) {
      return Optional.empty();
    }

    // Find primary verified email first
    return Arrays.stream(emails)
        .filter(email -> email.primary() && email.verified())
        .map(GitHubEmail::email)
        .findFirst()
        .or(() -> Arrays.stream(emails)
            .filter(email -> email.verified())
            .map(GitHubEmail::email)
            .findFirst());
  }

  public record GitHubEmail(String email, boolean primary, boolean verified, String visibility) {
  }
}
