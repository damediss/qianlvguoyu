package com.guoyu.platform.competition.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.time.LocalDateTime;

@TableName("competition_simulation_provider_binding")
public class CompetitionSimulationProviderBindingEntity extends BaseEntity {
  private Long eventId;
  private Long stageId;
  private Long eventWorkTypeId;
  private Long eventWorkTypeLevelId;
  private String providerCode;
  private String externalHostTenantId;
  private String externalSessionId;
  private String moduleFilter;
  private String status;
  private LocalDateTime lastSyncAt;

  public Long getEventId() { return eventId; }
  public void setEventId(Long value) { eventId = value; }
  public Long getStageId() { return stageId; }
  public void setStageId(Long value) { stageId = value; }
  public Long getEventWorkTypeId() { return eventWorkTypeId; }
  public void setEventWorkTypeId(Long value) { eventWorkTypeId = value; }
  public Long getEventWorkTypeLevelId() { return eventWorkTypeLevelId; }
  public void setEventWorkTypeLevelId(Long value) { eventWorkTypeLevelId = value; }
  public String getProviderCode() { return providerCode; }
  public void setProviderCode(String value) { providerCode = value; }
  public String getExternalHostTenantId() { return externalHostTenantId; }
  public void setExternalHostTenantId(String value) { externalHostTenantId = value; }
  public String getExternalSessionId() { return externalSessionId; }
  public void setExternalSessionId(String value) { externalSessionId = value; }
  public String getModuleFilter() { return moduleFilter; }
  public void setModuleFilter(String value) { moduleFilter = value; }
  public String getStatus() { return status; }
  public void setStatus(String value) { status = value; }
  public LocalDateTime getLastSyncAt() { return lastSyncAt; }
  public void setLastSyncAt(LocalDateTime value) { lastSyncAt = value; }
}
