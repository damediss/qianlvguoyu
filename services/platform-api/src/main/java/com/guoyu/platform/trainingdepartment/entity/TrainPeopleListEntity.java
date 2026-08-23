package com.guoyu.platform.trainingdepartment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * 开班人员列表（对齐 gjjddb train_people_list 遗留结构）
 */
@Getter
@Setter
@TableName("train_people_list")
public class TrainPeopleListEntity {

  @TableId(type = IdType.AUTO)
  private Long id;

  private Long trainId;
  private Long trainClassId;
  private String type;
  private Long userId;
  private Long platformUserId;
  private String realname;
  private String phone;
  private Integer isLeader;
  private Integer isCommittee;
  private BigDecimal examScore;
  /** 理论成绩（人社成绩单） */
  private BigDecimal theoryScore;
  private BigDecimal realScore;
  /** 工作业绩（人社成绩单） */
  private BigDecimal performanceScore;
  private String paper;
  private BigDecimal normalScore;
  private String nationalPaper;
  private Integer currentLevel;
  private Integer sortNumber;
  private Long tenantId;
  private LocalDateTime createTime;
  private String idCard;
  /** 准考证号（人社成绩单导入） */
  private String ticketNo;

  /** 0未结业 / 1已结业 / 2不合格 */
  private Integer graduateStatus;
  private LocalDateTime graduateAt;
  private Long graduateBy;
  private String graduateRemark;

  @TableLogic
  private Integer deleted;
}
