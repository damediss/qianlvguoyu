package com.guoyu.platform.certification.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("certification_task_checklist_item")
public class CertificationTaskChecklistItemEntity extends BaseEntity {
  private Long taskId;
  private Integer itemNo;
  private String itemName;
  private String quantityText;
  private Integer checked;
  private Long confirmerTaskStaffId;
  private String confirmerName;
  private LocalDateTime confirmedAt;
  private String remark;
  private Integer sortOrder;
}
