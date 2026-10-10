package com.guoyu.platform.trainingdepartment.vo;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

/** train_sign_up_people_self 视图对象（含列表 enrich 展示字段） */
@Getter
@Setter
public class TrainSignUpPeopleSelfVO {

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

  @Getter(AccessLevel.NONE)
  @Setter(AccessLevel.NONE)
  private String idCardFront;
  @Getter(AccessLevel.NONE)
  @Setter(AccessLevel.NONE)
  private String idCardBack;
  @Getter(AccessLevel.NONE)
  @Setter(AccessLevel.NONE)
  private String idCardHeader;
  private String idCardHeader2;
  private String photo;
  /** enrich 解析后的展示 URL（与 idCardFront 等原始值区分） */
  @Getter(AccessLevel.NONE)
  @Setter(AccessLevel.NONE)
  private String idcardFront;
  @Getter(AccessLevel.NONE)
  @Setter(AccessLevel.NONE)
  private String idcardBack;
  @Getter(AccessLevel.NONE)
  @Setter(AccessLevel.NONE)
  private String idcardHeader;

  private String school;
  private String major;
  private Integer schoolPaperHas;
  private String schoolNo;
  private String inSchoolDate;
  private String graduateDate;
  private String schoolPaper;

  private String workTime;
  private String titleType;
  private String titlePaper;
  private Long companyId;
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
  private String declarationCondition;
  private String workExp;
  private List<Map<String, Object>> workExpList;
  private String jog;

  private String contactFirstPage;
  private String contactExpirePage;
  private String contactSignPage;
  private String contractFirst;
  private String contractExpire;
  private String contractSign;
  private String contract;
  private String certificate;

  private String workTypePaperName;
  private Integer workTypePaperLevel;
  private String workTypePaperNo;

  private Integer status;
  private String statusCn;
  private Integer companyStatus;
  private String companyStatusCn;
  private Integer hasPay;
  private String payCn;

  private Long tenantId;
  private LocalDateTime createTime;
  private LocalDateTime updateTime;

  public String getIdCardFront() {
    return idCardFront;
  }

  public void setIdCardFront(String idCardFront) {
    this.idCardFront = idCardFront;
  }

  public String getIdCardBack() {
    return idCardBack;
  }

  public void setIdCardBack(String idCardBack) {
    this.idCardBack = idCardBack;
  }

  public String getIdCardHeader() {
    return idCardHeader;
  }

  public void setIdCardHeader(String idCardHeader) {
    this.idCardHeader = idCardHeader;
  }

  public String getIdcardFront() {
    return idcardFront;
  }

  public void setIdcardFront(String idcardFront) {
    this.idcardFront = idcardFront;
  }

  public String getIdcardBack() {
    return idcardBack;
  }

  public void setIdcardBack(String idcardBack) {
    this.idcardBack = idcardBack;
  }

  public String getIdcardHeader() {
    return idcardHeader;
  }

  public void setIdcardHeader(String idcardHeader) {
    this.idcardHeader = idcardHeader;
  }
}
