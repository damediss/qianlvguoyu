package com.guoyu.platform.certification.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("certification_assessor")
public class CertificationAssessorEntity extends BaseEntity {
  private Long userId;
  private String assessorName;
  private String certificateNo;
  private String workTypeScope;
  private String workTypeJson;
  private String levelScope;
  private LocalDate validUntil;
  private String phone;
  private String status;
  private String remark;
}
