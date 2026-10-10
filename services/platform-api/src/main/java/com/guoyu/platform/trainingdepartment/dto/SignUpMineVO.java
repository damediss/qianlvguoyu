package com.guoyu.platform.trainingdepartment.dto;

import com.guoyu.platform.trainingdepartment.entity.TrainSignUpPeopleSelfEntity;
import com.guoyu.platform.trainingdepartment.support.SignUpLevelSupport;

/**
 * 扫码端 - "我已报名"返回的个人详情（不返回敏感字段如租户ID、状态码）。
 */
public record SignUpMineVO(
    Long id,
    Long signUpId,
    String signUpTitle,
    String name,
    String phone,
    String idCardNumber,
    Integer age,
    String gender,
    String nation,
    String political,
    String education,
    String houseType,
    String address,
    String school,
    String major,
    String inSchoolDate,
    String graduateDate,
    String schoolNo,
    Integer schoolPaperHas,
    String schoolPaper,
    String workTime,
    String titleType,
    String titlePaper,
    Long companyId,
    String workDateStart,
    String workDateEnd,
    Integer workTypePaperHas,
    String workTypePaper,
    Long workTypeId,
    String workTypeName,
    Integer level,
    String paperNo,
    Long reqWorkTypeId,
    String reqWorkTypeName,
    Integer reqLevel,
    String declarationCondition,
    String jog,
    String companyName,
    String companyCode,
    String zipCode,
    String companyAddress,
    String industrialPark,
    String comments,
    String titleLevel,
    String workExp,
    String job,
    String contactFirstPage,
    String contactExpirePage,
    String contactSignPage,
    String socialSecurity,
    String idCardFront,
    String idCardBack,
    String idCardHeader,
    String idCardHeader2,
    String photo,
    String contract,
    String certificate,
    Integer status,
    String statusCn,
    String createTime,
    String updateTime,
    Boolean editable
) {
  public static SignUpMineVO from(TrainSignUpPeopleSelfEntity e, String signUpTitle, boolean editable) {
    if (e == null) return null;
    return new SignUpMineVO(
        e.getId(), e.getSignUpId(), signUpTitle,
        e.getName(), e.getPhone(), e.getIdCardNumber(),
        e.getAge(), e.getGender(), e.getNation(), e.getPolitical(),
        e.getEducation(), e.getHouseType(), e.getAddress(),
        e.getSchool(), e.getMajor(), e.getInSchoolDate(), e.getGraduateDate(),
        e.getSchoolNo(), e.getSchoolPaperHas(), e.getSchoolPaper(),
        e.getWorkTime(), e.getTitleType(), e.getTitlePaper(),
        e.getCompanyId(), e.getWorkDateStart(), e.getWorkDateEnd(),
        e.getWorkTypePaperHas(), e.getWorkTypePaper(),
        e.getWorkTypeId(), e.getWorkTypeName(),
        SignUpLevelSupport.toIndex(e.getLevel()), e.getPaperNo(),
        e.getReqWorkTypeId(), e.getReqWorkTypeName(), e.getReqLevel(),
        e.getDeclarationCondition(), e.getJog(),
        e.getCompanyName(), e.getCompanyCode(),
        e.getZipCode(), e.getCompanyAddress(),
        e.getIndustrialPark(), e.getComments(),
        e.getTitleLevel(), e.getWorkExp(), e.getJob(),
        e.getContactFirstPage(), e.getContactExpirePage(),
        e.getContactSignPage(), e.getSocialSecurity(),
        e.getIdCardFront(), e.getIdCardBack(),
        e.getIdCardHeader(), e.getIdCardHeader2(),
        e.getPhoto(), e.getContract(), e.getCertificate(),
        e.getStatus(),
        statusLabel(e.getStatus()),
        e.getCreateTime() == null ? null : e.getCreateTime().toString(),
        e.getUpdateTime() == null ? null : e.getUpdateTime().toString(),
        editable
    );
  }

  private static String statusLabel(Integer s) {
    if (s == null || s == 0) return "待审核";
    return switch (s) {
      case 1 -> "已通过";
      case 2 -> "拒绝";
      default -> "待审核";
    };
  }
}
