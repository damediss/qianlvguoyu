package com.guoyu.platform.certification.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("certification_defense_report")
public class CertificationDefenseReportEntity extends BaseEntity {
  private Long taskId;
  private Long candidateId;
  private Long groupId;
  private Long fileId;
  private String fileName;
  private String matchKey;
  private String status;
  private LocalDateTime uploadedAt;
}
