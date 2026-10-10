package com.guoyu.platform.competition.player.sms;

import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.dysmsapi20170525.models.SendSmsRequest;
import com.aliyun.dysmsapi20170525.models.SendSmsResponse;
import com.aliyun.teaopenapi.models.Config;
import com.guoyu.platform.competition.player.config.CompetitionPlayerSmsProperties;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Alibaba Cloud Dysmsapi 2017-05-25 implementation for mainland-China verification SMS. */
@Component
@Slf4j
@ConditionalOnProperty(prefix = "platform.competition.player.sms.aliyun", name = "enabled", havingValue = "true")
public class AliyunCompetitionSmsSender implements CompetitionSmsSender {

  private final Client client;
  private final CompetitionPlayerSmsProperties.Aliyun properties;

  public AliyunCompetitionSmsSender(CompetitionPlayerSmsProperties properties) throws Exception {
    this.properties = properties.getAliyun();
    validateConfiguration();
    Config config = new Config()
        .setAccessKeyId(requiredEnvironment("ALIBABA_CLOUD_ACCESS_KEY_ID"))
        .setAccessKeySecret(requiredEnvironment("ALIBABA_CLOUD_ACCESS_KEY_SECRET"))
        .setEndpoint(this.properties.getEndpoint())
        .setRegionId(this.properties.getRegionId());
    this.client = new Client(config);
  }

  @Override
  public CompetitionSmsSendResult send(CompetitionSmsMessage message) {
    try {
      SendSmsRequest request = new SendSmsRequest()
          .setPhoneNumbers(message.mobile())
          .setSignName(properties.getSignName())
          .setTemplateCode(properties.getTemplateCode())
          .setTemplateParam("{\"code\":\"" + message.code() + "\"}");
      SendSmsResponse response = client.sendSms(request);
      if (response == null || response.getBody() == null
          || !Objects.equals("OK", response.getBody().getCode())) {
        log.warn("Aliyun SMS request was rejected, purpose={}", message.purpose().getCode());
        return new CompetitionSmsSendResult(false, null);
      }
      String providerMessageId = response.getBody().getBizId();
      return new CompetitionSmsSendResult(true, providerMessageId);
    } catch (Exception ex) {
      log.warn("Aliyun SMS invocation failed, purpose={}, errorType={}",
          message.purpose().getCode(), ex.getClass().getSimpleName());
      return new CompetitionSmsSendResult(false, null);
    }
  }

  private void validateConfiguration() {
    if (isBlank(properties.getEndpoint()) || isBlank(properties.getRegionId())
        || isBlank(properties.getSignName()) || isBlank(properties.getTemplateCode())) {
      throw new IllegalStateException("Aliyun SMS configuration is incomplete");
    }
  }

  private String requiredEnvironment(String name) {
    String value = System.getenv(name);
    if (isBlank(value)) {
      throw new IllegalStateException("Missing required Alibaba Cloud credential environment variable: " + name);
    }
    return value;
  }

  private boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
