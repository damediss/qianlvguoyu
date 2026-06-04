package com.guoyu.platform.certification.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("certification_wbs_item")
public class CertificationWbsItemEntity extends BaseEntity {

  private Long taskId;
  private Long documentId;
  private Long parentId;
  private String wbsCode;
  private String categoryCode;
  private String categoryName;
  private String itemName;
  private String activityDescription;
  private BigDecimal estimateHours;
  private BigDecimal manpowerCount;
  private String manpowerRoleCode;
  private Long responsibleMemberId;
  private String resourceInput;
  private BigDecimal costAmount;
  private Integer durationDays;
  private Long predecessorId;
  private Boolean leaf;
  private Integer progressPercent;
  private String status;
  private Integer sortOrder;
  private String remark;
}
