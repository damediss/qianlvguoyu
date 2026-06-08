package com.guoyu.platform.certification.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("certification_archive_basic_snapshot")
public class CertificationArchiveBasicSnapshotEntity extends BaseEntity {
  private Long archiveId;
  private LocalDateTime completedAt;
  private Integer candidateCount;
  private String taskJson;
  private String scopeJson;
  private String examStaffJson;
  private String deviceJson;
  private String roomJson;
  private String evaluatorJson;
}
