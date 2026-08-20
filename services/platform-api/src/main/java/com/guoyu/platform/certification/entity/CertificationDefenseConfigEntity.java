package com.guoyu.platform.certification.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("certification_defense_config")
public class CertificationDefenseConfigEntity extends BaseEntity {
  private Long taskId;
  private Long groupId;
  private String status;
}
