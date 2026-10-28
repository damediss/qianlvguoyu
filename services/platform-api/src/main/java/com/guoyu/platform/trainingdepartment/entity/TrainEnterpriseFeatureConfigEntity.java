package com.guoyu.platform.trainingdepartment.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 企业功能配置（菜单白名单 + 可授角色包） */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("train_enterprise_feature_config")
public class TrainEnterpriseFeatureConfigEntity extends BaseEntity {

  private Long enterpriseId;
  /** 配置方组织；超管为 0 */
  private Long configurerOrgId;
  /** JSON 数组：菜单 code */
  private String menuCodesJson;
  /** JSON 数组：角色 id */
  private String roleIdsJson;
  /** 1=启用约束；0=关闭 */
  private Integer constrainEnabled;
}
