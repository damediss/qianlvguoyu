package com.guoyu.platform.trainingdepartment.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** train_sign_up_people_self 更新请求 — 对齐 PHP peopleselfsave */
@Getter
@Setter
public class TrainSignUpPeopleSelfUpdateReq {

  private Long signUpId;
  private Long platformUserId;
  private Long userId;

  @Size(max = 100)
  private String name;
  @Size(max = 20)
  private String phone;
  @Size(max = 20)
  private String idCardNumber;
  private Integer age;
  @Size(max = 10)
  private String gender;
  @Size(max = 50)
  private String nation;
  @Size(max = 50)
  private String political;
  @Size(max = 50)
  private String houseType;
  @Size(max = 500)
  private String address;
  @Size(max = 50)
  private String education;

  @Size(max = 500)
  private String idCardFront;
  @Size(max = 500)
  private String idCardBack;
  @Size(max = 500)
  private String idCardHeader;
  @Size(max = 500)
  private String idCardHeader2;
  @Size(max = 500)
  private String photo;

  @Size(max = 255)
  private String school;
  @Size(max = 255)
  private String major;
  private Integer schoolPaperHas;
  @Size(max = 100)
  private String schoolNo;
  @Size(max = 20)
  private String inSchoolDate;
  @Size(max = 20)
  private String graduateDate;
  @Size(max = 500)
  private String schoolPaper;

  @Size(max = 20)
  private String workTime;
  @Size(max = 50)
  private String titleType;
  @Size(max = 500)
  private String titlePaper;
  private Long companyId;
  @Size(max = 20)
  private String workDateStart;
  @Size(max = 20)
  private String workDateEnd;
  private Integer workTypePaperHas;
  @Size(max = 500)
  private String workTypePaper;
  private Long workTypeId;
  @Size(max = 255)
  private String workTypeName;
  private Integer level;
  @Size(max = 100)
  private String paperNo;
  private Long reqWorkTypeId;
  @Size(max = 255)
  private String reqWorkTypeName;
  private Integer reqLevel;
  @Size(max = 255)
  private String declarationCondition;
  private String workExp;
  @Size(max = 255)
  private String jog;

  @Size(max = 500)
  private String contactFirstPage;
  @Size(max = 500)
  private String contactExpirePage;
  @Size(max = 500)
  private String contactSignPage;
  @Size(max = 500)
  private String contract;
  @Size(max = 500)
  private String certificate;

  @Size(max = 255)
  private String workTypePaperName;
  private Integer workTypePaperLevel;
  @Size(max = 100)
  private String workTypePaperNo;

  private Integer status;
  private Integer companyStatus;
  private Integer hasPay;
}
