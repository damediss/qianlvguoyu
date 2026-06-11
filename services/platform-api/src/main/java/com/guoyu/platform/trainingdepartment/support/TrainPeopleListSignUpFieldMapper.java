package com.guoyu.platform.trainingdepartment.support;

import com.guoyu.platform.trainingdepartment.entity.TrainPeopleListEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainSignUpPeopleSelfEntity;

/** 从报名人员复制遗留字段到开班人员列表 */
public final class TrainPeopleListSignUpFieldMapper {

  private TrainPeopleListSignUpFieldMapper() {}

  public static void applySignUpFields(TrainPeopleListEntity entity, TrainSignUpPeopleSelfEntity signUp) {
    if (entity == null || signUp == null) {
      return;
    }
    entity.setSignUpPeopleId(signUp.getId());
    entity.setUserId(signUp.getUserId());
    entity.setAge(signUp.getAge());
    entity.setReqWorkTypeId(signUp.getReqWorkTypeId());
    entity.setReqWorkTypeName(signUp.getReqWorkTypeName());
    entity.setReqLevel(signUp.getReqLevel());
    entity.setJob(signUp.getJog());
    entity.setWorkTypeId(signUp.getWorkTypeId());
    entity.setWorkTypeName(signUp.getWorkTypeName());
    if (signUp.getLevel() != null) {
      entity.setLevel(String.valueOf(signUp.getLevel()));
    }
    entity.setCompanyId(signUp.getCompanyId());
    entity.setCompanyName(signUp.getCompanyName());
    entity.setWorkDateStart(signUp.getWorkDateStart());
    entity.setWorkDateEnd(signUp.getWorkDateEnd());
    entity.setWorkTime(signUp.getWorkTime());
    entity.setWorkTypePaperHas(signUp.getWorkTypePaperHas());
    entity.setWorkTypePaper(signUp.getWorkTypePaper());
    entity.setWorkTypePaperNo(signUp.getWorkTypePaperNo());
    entity.setWorkTypePaperLevel(signUp.getWorkTypePaperLevel());
    entity.setWorkTypePaperName(signUp.getWorkTypePaperName());
    entity.setTitleType(signUp.getTitleType());
    entity.setTitlePaper(signUp.getTitlePaper());
    entity.setHasPay(signUp.getHasPay());
    entity.setSchool(signUp.getSchool());
    entity.setMajor(signUp.getMajor());
    entity.setInSchoolDate(signUp.getInSchoolDate());
    entity.setGraduateDate(signUp.getGraduateDate());
    entity.setSchoolPaperHas(signUp.getSchoolPaperHas());
    entity.setSchoolPaper(signUp.getSchoolPaper());
    entity.setNation(signUp.getNation());
    entity.setAddress(signUp.getAddress());
    entity.setEducation(signUp.getEducation());
    entity.setDeclarationCondition(signUp.getDeclarationCondition());
    entity.setSchoolNo(signUp.getSchoolNo());
    entity.setPaperNo(signUp.getPaperNo());
    entity.setWorkExp(signUp.getWorkExp());
    entity.setContactFirstPage(signUp.getContactFirstPage());
    entity.setContactExpirePage(signUp.getContactExpirePage());
    entity.setContactSignPage(signUp.getContactSignPage());
    entity.setHouseType(signUp.getHouseType());
    entity.setPolitical(signUp.getPolitical());
    entity.setCompanyStatus(signUp.getCompanyStatus());
    entity.setIdCardFront(signUp.getIdCardFront());
    entity.setIdCardBack(signUp.getIdCardBack());
    entity.setIdCardHeader(signUp.getIdCardHeader());
    entity.setIdCardHeader2(signUp.getIdCardHeader2());
    if (signUp.getStatus() != null) {
      entity.setStatus(signUp.getStatus());
    }
  }
}
