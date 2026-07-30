package com.guoyu.platform.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.guoyu.platform.ai.config.WeKnoraProperties;
import com.guoyu.platform.ai.vo.WeKnoraEmbedTokenVO;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.security.PlatformUserPrincipal;
import java.net.http.HttpClient;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Slf4j
@Service
public class WeKnoraEmbedApplicationService {

  private final WeKnoraProperties properties;
  private final RestClient restClient;

  public WeKnoraEmbedApplicationService(WeKnoraProperties properties) {
    this.properties = properties;
    HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .build();
    JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
    requestFactory.setReadTimeout(Duration.ofSeconds(10));
    this.restClient = RestClient.builder()
        .requestFactory(requestFactory)
        .build();
  }

  public WeKnoraEmbedTokenVO exchangeToken(PlatformUserPrincipal principal) {
    validateConfigured();
    String origin = origin();

    try {
      return exchangeWithOrigin(origin, principal);
    } catch (RestClientResponseException ex) {
      int status = ex.getStatusCode().value();
      String alternateOrigin = alternateOrigin(origin);
      if (status == 403 && !alternateOrigin.equals(origin)) {
        log.warn("WeKnora exchange forbidden, retrying with alternate origin form: userId={}, tenantId={}, channelId={}, origin={}, alternateOrigin={}",
            principal.getUserId(), principal.getTenantId(), properties.getChannelId(), origin, alternateOrigin);
        try {
          return exchangeWithOrigin(alternateOrigin, principal);
        } catch (RestClientResponseException retryEx) {
          throw exchangeFailed(retryEx, principal, origin + " or " + alternateOrigin);
        }
      }
      throw exchangeFailed(ex, principal, origin);
    } catch (RestClientException ex) {
      log.warn("WeKnora exchange request failed: userId={}, tenantId={}, channelId={}, errorType={}",
          principal.getUserId(), principal.getTenantId(), properties.getChannelId(), ex.getClass().getSimpleName());
      throw badGateway("WeKnora exchange request failed");
    }
  }

  private WeKnoraEmbedTokenVO exchangeWithOrigin(String origin, PlatformUserPrincipal principal) {
    JsonNode response = restClient.post()
        .uri(exchangeUrl())
        .header(HttpHeaders.AUTHORIZATION, "Embed " + publishToken())
        .header(HttpHeaders.ORIGIN, origin)
        .retrieve()
        .body(JsonNode.class);

    JsonNode data = response == null ? null : response.path("data");
    String sessionToken = data == null ? null : data.path("session_token").asText(null);
    int expiresIn = data == null ? 0 : data.path("expires_in").asInt(0);

    if (!StringUtils.hasText(sessionToken) || expiresIn <= 0) {
      log.warn("WeKnora exchange response missing session token or expiry: userId={}, tenantId={}, channelId={}, tokenPresent={}, expiresIn={}",
          principal.getUserId(), principal.getTenantId(), properties.getChannelId(),
          StringUtils.hasText(sessionToken), expiresIn);
      throw badGateway("WeKnora exchange response is invalid");
    }

    return new WeKnoraEmbedTokenVO(sessionToken, expiresIn);
  }

  private BusinessException exchangeFailed(
      RestClientResponseException ex,
      PlatformUserPrincipal principal,
      String allowedOriginHint
  ) {
    int status = ex.getStatusCode().value();
    log.warn("WeKnora exchange returned non-success status: userId={}, tenantId={}, channelId={}, originHint={}, status={}",
        principal.getUserId(), principal.getTenantId(), properties.getChannelId(), allowedOriginHint, status);
    if (status == 401) {
      return badGateway("WeKnora exchange unauthorized. Check publish token and channel");
    }
    if (status == 403) {
      return badGateway("WeKnora exchange forbidden. Check channel origin allowlist for " + allowedOriginHint);
    }
    return badGateway("WeKnora exchange failed with status " + status);
  }

  private String exchangeUrl() {
    return trimTrailingSlash(properties.getBaseUrl())
        + "/api/v1/embed/"
        + properties.getChannelId()
        + "/exchange";
  }

  private void validateConfigured() {
    if (!StringUtils.hasText(properties.getBaseUrl())) {
      log.error("WeKnora embed is not configured: baseUrl missing");
      throw badGateway("WeKnora base URL is not configured");
    }
    if (!StringUtils.hasText(properties.getChannelId())) {
      log.error("WeKnora embed is not configured: channelId missing");
      throw badGateway("WeKnora channel ID is not configured");
    }
    if (!StringUtils.hasText(properties.getPublishToken())) {
      log.error("WeKnora embed is not configured: publishToken missing");
      throw badGateway("WeKnora publish token is not configured");
    }
    String publishToken = publishToken();
    if ("em_xxx".equalsIgnoreCase(publishToken) || publishToken.length() <= "em_xxx".length()) {
      log.error("WeKnora embed is not configured: publishToken placeholder");
      throw badGateway("WeKnora publish token is still a placeholder");
    }
    if (!publishToken.startsWith("em_")) {
      log.error("WeKnora embed is not configured: publishToken format invalid");
      throw badGateway("WeKnora publish token format is invalid");
    }
    if (!StringUtils.hasText(properties.getOrigin())) {
      log.error("WeKnora embed is not configured: origin missing");
      throw badGateway("WeKnora origin is not configured");
    }
  }

  private String publishToken() {
    return properties.getPublishToken().trim();
  }

  private String origin() {
    return properties.getOrigin().trim();
  }

  private String alternateOrigin(String origin) {
    if (origin.endsWith("/")) {
      return trimTrailingSlash(origin);
    }
    return origin + "/";
  }

  private String trimTrailingSlash(String value) {
    String result = value.trim();
    while (result.endsWith("/")) {
      result = result.substring(0, result.length() - 1);
    }
    return result;
  }

  private BusinessException badGateway(String message) {
    return new BusinessException(ErrorCode.BAD_GATEWAY, message);
  }
}
