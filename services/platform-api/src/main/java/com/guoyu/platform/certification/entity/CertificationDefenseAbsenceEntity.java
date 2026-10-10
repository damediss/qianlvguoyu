package com.guoyu.platform.certification.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("certification_defense_absence")
public class CertificationDefenseAbsenceEntity extends BaseEntity {
  private Long taskId;
  private Long candidateId;
}
