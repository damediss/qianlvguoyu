package com.guoyu.platform.certification.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("certification_task_exam_site")
public class CertificationTaskExamSiteEntity extends BaseEntity {
  private Long taskId;
  private Long siteId;
  private String siteName;
  private String siteType;
  private String address;
  private Integer seatCount;
  private String filingNo;
  private LocalDate examDate;
  private String startTime;
  private String endTime;
  private String examType;
  private Integer arrangedCount;
  private String contactName;
  private String contactPhone;
  private String status;
  private Integer sortOrder;
  private String remark;
}
