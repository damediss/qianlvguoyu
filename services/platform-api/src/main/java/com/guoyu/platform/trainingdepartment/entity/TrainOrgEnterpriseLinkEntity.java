package com.guoyu.platform.trainingdepartment.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 机构–企业业务关联（需求选中后永久有效，不改组织树） */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("train_org_enterprise_link")
public class TrainOrgEnterpriseLinkEntity extends BaseEntity {

  /** 培训机构组织 ID */
  private Long orgId;
  /** 企业组织 ID */
  private Long enterpriseId;
  /** 来源：demand 等 */
  private String source;
  /** 来源单据 ID */
  private Long sourceId;
  /** active / inactive */
  private String status;
}
