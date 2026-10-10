package com.guoyu.platform.trainingdepartment.vo;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Getter;

/** 培训模块向认定模块提供的班级学员来源数据。 */
@Builder
@Getter
public class TrainingStudentCertificationSourceVO {

  private Long trainingPersonId;
  private Long trainId;
  private Long trainClassId;
  private Long signUpId;
  private Long signUpPersonId;
  private Long platformUserId;
  private Long userId;
  private String name;
  private String phone;
  private String idCardNumber;
  private String gender;
  private String education;
  private String graduateDate;
  private String school;
  private String major;
  private String nation;
  private String address;
  private String companyName;
  private String companyAddress;
  private String zipCode;
  private String companyCode;
  private String industrialPark;
  private String workDateStart;
  private String workTime;
  private String workTypePaperName;
  private Integer workTypePaperLevel;
  private String workTypePaperNo;
  private String paperNo;
  private Long reqWorkTypeId;
  private String reqWorkTypeName;
  private Integer reqLevel;
  private Long workTypeId;
  private String workTypeName;
  private Integer level;
  private String job;
  private String jog;
  private String declarationCondition;
  private String workExp;
  private BigDecimal theoryScore;
  private BigDecimal practicalScore;
  private String idCardHeader;
  private String idCardHeader2;
  private String idCardFront;
  private String idCardBack;
  private String workTypePaper;
  private String schoolPaper;
  private String comments;
}
