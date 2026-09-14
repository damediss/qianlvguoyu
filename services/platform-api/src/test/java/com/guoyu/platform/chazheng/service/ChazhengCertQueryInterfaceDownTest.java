package com.guoyu.platform.chazheng.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.chazheng.exception.CertQueryInterfaceDownException;
import com.guoyu.platform.common.exception.BusinessException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

/**
 * 查证查询服务的失败分类：全部尝试都是网络/对方 5xx → 接口级故障专用异常（触发批次熔断）；
 * 混合或纯 4xx/业务错误 → 普通业务异常（走用户级重试）。
 */
class ChazhengCertQueryInterfaceDownTest {

  @TempDir
  java.nio.file.Path tempDir;

  private CertCheckConfigService configService;
  private RestTemplate restTemplate;
  private ChazhengCertQueryService service;

  @BeforeEach
  void setUp() {
    configService = mock(CertCheckConfigService.class);
    restTemplate = mock(RestTemplate.class);
    service = new ChazhengCertQueryService(configService, restTemplate, new ObjectMapper(), 1000.0);
    ReflectionTestUtils.setField(service, "chazhengLogDir", tempDir.toString());
    when(configService.findApiUrl(1L)).thenReturn("https://example.com/api");
    when(configService.findRequestTemplates(1L))
        .thenReturn(List.of(Map.of("k", "v1"), Map.of("k", "v2")));
  }

  @Test
  void allNetworkErrorsThrowInterfaceDown() {
    when(restTemplate.postForObject(anyString(), any(), eq(Map.class)))
        .thenThrow(new ResourceAccessException("I/O error: Connection timed out"));

    assertThrows(CertQueryInterfaceDownException.class,
        () -> service.queryByNameIdCard("张三", "452301199001011234", 1L),
        "全部模板网络异常应抛接口级故障");
  }

  @Test
  void allServerErrorsThrowInterfaceDown() {
    HttpServerErrorException e500 = HttpServerErrorException.create(
        HttpStatus.INTERNAL_SERVER_ERROR, "err", new HttpHeaders(), new byte[0], StandardCharsets.UTF_8);
    when(restTemplate.postForObject(anyString(), any(), eq(Map.class))).thenThrow(e500);

    assertThrows(CertQueryInterfaceDownException.class,
        () -> service.queryByNameIdCard("张三", "452301199001011234", 1L),
        "全部模板 5xx 应抛接口级故障");
  }

  @Test
  void mixedNetworkAndClientErrorThrowsBusiness() {
    HttpClientErrorException e400 = HttpClientErrorException.create(
        HttpStatus.BAD_REQUEST, "bad", new HttpHeaders(), new byte[0], StandardCharsets.UTF_8);
    when(restTemplate.postForObject(anyString(), any(), eq(Map.class)))
        .thenThrow(new ResourceAccessException("I/O error"))
        .thenThrow(e400);

    assertThrows(BusinessException.class,
        () -> service.queryByNameIdCard("张三", "452301199001011234", 1L),
        "网络+4xx 混合失败仍按普通业务异常（不熔断）");
  }

  @Test
  void bizErrorResponseThrowsBusiness() {
    when(restTemplate.postForObject(anyString(), any(), eq(Map.class)))
        .thenReturn(Map.of("errorCode", "E1001", "message", "参数错误"));

    assertThrows(BusinessException.class,
        () -> service.queryByNameIdCard("张三", "452301199001011234", 1L),
        "接口正常应答但业务报错 → 普通业务异常");
  }
}
