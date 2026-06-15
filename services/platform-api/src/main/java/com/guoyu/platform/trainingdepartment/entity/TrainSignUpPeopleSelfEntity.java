package com.guoyu.platform.trainingdepartment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/** train_sign_up_people_self — 对齐 PHP 遗留字段 */
@Getter
@Setter
@TableName("train_sign_up_people_self")
public class TrainSignUpPeopleSelfEntity {

  @TableId(type = IdType.AUTO)
  private Long id;

  private Long signUpId;
  private Long platformUserId;
  private Long userId;
  private String name;
  private String phone;
  private String idCardNumber;
  private Integer age;
  private String gender;
  private String nation;
  private String political;
  private String houseType;
  private String address;
  private String education;

  private String idCardFront;
  private String idCardBack;
  private String idCardHeader;
  private String idCardHeader2;
  private String photo;

  private String school;
  private String major;
  private Integer schoolPaperHas;
  private String schoolNo;
  private String inSchoolDate;
  private String graduateDate;
  private String schoolPaper;

  private String workTime;
  private String titleType;
  private String titleLevel;
  private String titlePaper;
  private Long companyId;
  private String companyName;
  private String companyCode;
  private String zipCode;
  private String companyAddress;
  private String industrialPark;
  private String comments;
  private String workDateStart;
  private String workDateEnd;
  private Integer workTypePaperHas;
  private String workTypePaper;
  private Long workTypeId;
  private String workTypeName;
  private Integer level;
  private String paperNo;
  private Long reqWorkTypeId;
  private String reqWorkTypeName;
  private Integer reqLevel;

  @TableField("`condition`")
  private String declarationCondition;

  private String workExp;
  private String job;
  private String jog;

  private String contactFirstPage;
  private String contactExpirePage;
  private String contactSignPage;
  private String socialSecurity;
  private String contract;
  private String certificate;

  private String workTypePaperName;
  private Integer workTypePaperLevel;
  private String workTypePaperNo;

  private Integer status;
  private Integer companyStatus;
  private Integer hasPay;

  private Long tenantId;
  private LocalDateTime createTime;
  private LocalDateTime updateTime;

  @TableLogic
  private Integer deleted;
}
