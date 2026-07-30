package com.guoyu.platform.chazheng.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.apache.hc.core5.ssl.TrustStrategy;
import org.apache.hc.core5.util.Timeout;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import java.security.cert.X509Certificate;
import javax.net.ssl.SSLContext;

/**
 * 查证模块 RestTemplate 配置。
 *
 * <p>跳过 SSL 证书验证，对齐 PHP 代码中 {@code CURLOPT_SSL_VERIFYPEER=false} 的行为。
 *
 * <p>⚠️ <b>生产环境风险</b>：跳过 SSL 验证使连接可能受到中间人攻击。
 * 本配置与 PHP 行为对齐，仅建议在开发/测试环境使用。
 */
@Configuration
@Slf4j
public class ChazhengRestTemplateConfig {

  @Value("${chazheng.rest.connect-timeout:5000}")
  private int connectTimeoutMs;

  @Value("${chazheng.rest.read-timeout:10000}")
  private int readTimeoutMs;

  @Bean
  public RestTemplate chazhengRestTemplate() throws Exception {
    log.info("[chazheng] 初始化 RestTemplate(SSL跳过, connectTimeout={}ms, readTimeout={}ms)",
        connectTimeoutMs, readTimeoutMs);

    // 信任所有证书（与 PHP CURLOPT_SSL_VERIFYPEER=false 对齐）
    TrustStrategy trustAll = (X509Certificate[] chain, String authType) -> true;
    SSLContext sslContext = SSLContextBuilder.create()
        .loadTrustMaterial(null, trustAll)
        .build();

    var sslSocketFactory = SSLConnectionSocketFactoryBuilder.create()
        .setSslContext(sslContext)
        .build();

    RequestConfig requestConfig = RequestConfig.custom()
        .setConnectTimeout(Timeout.ofMilliseconds(connectTimeoutMs))
        .setResponseTimeout(Timeout.ofMilliseconds(readTimeoutMs))
        .build();

    return new RestTemplateBuilder()
        .requestFactory(() -> {
          var httpClient = HttpClients.custom()
            .setDefaultRequestConfig(requestConfig)
            .setConnectionManager(
              org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder.create()
                .setSSLSocketFactory(sslSocketFactory)
                .build())
            .build();
          return new HttpComponentsClientHttpRequestFactory(httpClient);
        })
        .build();
  }
}
