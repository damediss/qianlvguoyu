package com.guoyu.platform.trainingdepartment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.trainingdepartment.entity.TrainPeopleListEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainSignUpPeopleSelfEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainSignUpSelfEntity;
import com.guoyu.platform.trainingdepartment.mapper.TrainPeopleListMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainSignUpPeopleSelfMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainSignUpSelfMapper;
import com.guoyu.platform.trainingdepartment.support.SignUpLevelSupport;
import com.guoyu.platform.trainingdepartment.vo.TrainingStudentCertificationSourceVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** 对外提供培训班级学员及其信息收集来源，避免其他模块直接访问培训 Mapper。 */
@Service
public class TrainingStudentSourceApplicationService {

  private final TrainPeopleListMapper trainPeopleListMapper;
  private final TrainSignUpSelfMapper signUpSelfMapper;
  private final TrainSignUpPeopleSelfMapper signUpPeopleSelfMapper;

  public TrainingStudentSourceApplicationService(
      TrainPeopleListMapper trainPeopleListMapper,
      TrainSignUpSelfMapper signUpSelfMapper,
      TrainSignUpPeopleSelfMapper signUpPeopleSelfMapper
  ) {
    this.trainPeopleListMapper = trainPeopleListMapper;
    this.signUpSelfMapper = signUpSelfMapper;
    this.signUpPeopleSelfMapper = signUpPeopleSelfMapper;
  }

  @Transactional(readOnly = true)
  public TrainingStudentCertificationSourceVO requireCertificationSource(Long trainingPersonId) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    TrainPeopleListEntity trainingPerson = trainPeopleListMapper.selectOne(
        new LambdaQueryWrapper<TrainPeopleListEntity>()
            .eq(TrainPeopleListEntity::getTenantId, tenantId)
            .eq(TrainPeopleListEntity::getId, trainingPersonId)
            .eq(TrainPeopleListEntity::getType, "student")
            .eq(TrainPeopleListEntity::getDeleted, 0));
    if (trainingPerson == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "培训班级学员不存在");
    }
    if (!StringUtils.hasText(trainingPerson.getRealname())
        || !StringUtils.hasText(trainingPerson.getIdCard())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "培训班级学员缺少姓名或身份证号");
    }

    TrainSignUpSelfEntity signUp = signUpSelfMapper.selectOne(
        new LambdaQueryWrapper<TrainSignUpSelfEntity>()
            .eq(TrainSignUpSelfEntity::getTenantId, tenantId)
            .eq(TrainSignUpSelfEntity::getTrainInfoId, trainingPerson.getTrainId())
            .eq(TrainSignUpSelfEntity::getDeleted, 0)
            .orderByDesc(TrainSignUpSelfEntity::getId)
            .last("LIMIT 1"));
    if (signUp == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "该培训项目未找到信息收集任务");
    }

    LambdaQueryWrapper<TrainSignUpPeopleSelfEntity> wrapper =
        new LambdaQueryWrapper<TrainSignUpPeopleSelfEntity>()
            .eq(TrainSignUpPeopleSelfEntity::getTenantId, tenantId)
            .eq(TrainSignUpPeopleSelfEntity::getSignUpId, signUp.getId())
            .eq(TrainSignUpPeopleSelfEntity::getDeleted, 0);
    if (trainingPerson.getPlatformUserId() != null && trainingPerson.getPlatformUserId() > 0) {
      wrapper.and(w -> w
          .eq(TrainSignUpPeopleSelfEntity::getPlatformUserId, trainingPerson.getPlatformUserId())
          .or()
          .eq(TrainSignUpPeopleSelfEntity::getIdCardNumber, trainingPerson.getIdCard()));
    } else {
      wrapper.eq(TrainSignUpPeopleSelfEntity::getIdCardNumber, trainingPerson.getIdCard());
    }
    TrainSignUpPeopleSelfEntity source =
        signUpPeopleSelfMapper.selectOne(wrapper.orderByDesc(TrainSignUpPeopleSelfEntity::getId).last("LIMIT 1"));
    if (source == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "未找到班级学员对应的信息收集数据");
    }

    return TrainingStudentCertificationSourceVO.builder()
        .trainingPersonId(trainingPerson.getId())
        .trainId(trainingPerson.getTrainId())
        .trainClassId(trainingPerson.getTrainClassId())
        .signUpId(source.getSignUpId())
        .signUpPersonId(source.getId())
        .platformUserId(source.getPlatformUserId())
        .userId(source.getUserId())
        .name(source.getName())
        .phone(source.getPhone())
        .idCardNumber(source.getIdCardNumber())
        .gender(source.getGender())
        .education(source.getEducation())
        .graduateDate(source.getGraduateDate())
        .school(source.getSchool())
        .major(source.getMajor())
        .nation(source.getNation())
        .address(source.getAddress())
        .companyName(source.getCompanyName())
        .companyAddress(source.getCompanyAddress())
        .zipCode(source.getZipCode())
        .companyCode(source.getCompanyCode())
        .industrialPark(source.getIndustrialPark())
        .workDateStart(source.getWorkDateStart())
        .workTime(source.getWorkTime())
        .workTypePaperName(source.getWorkTypePaperName())
        .workTypePaperLevel(source.getWorkTypePaperLevel())
        .workTypePaperNo(source.getWorkTypePaperNo())
        .paperNo(source.getPaperNo())
        .reqWorkTypeId(source.getReqWorkTypeId())
        .reqWorkTypeName(source.getReqWorkTypeName())
        .reqLevel(source.getReqLevel())
        .workTypeId(source.getWorkTypeId())
        .workTypeName(source.getWorkTypeName())
        .level(SignUpLevelSupport.toIndex(source.getLevel()))
        .job(source.getJob())
        .jog(source.getJog())
        .declarationCondition(source.getDeclarationCondition())
        .workExp(source.getWorkExp())
        .theoryScore(trainingPerson.getExamScore())
        .practicalScore(trainingPerson.getRealScore())
        .idCardHeader(source.getIdCardHeader())
        .idCardHeader2(source.getIdCardHeader2())
        .idCardFront(source.getIdCardFront())
        .idCardBack(source.getIdCardBack())
        .workTypePaper(source.getWorkTypePaper())
        .schoolPaper(source.getSchoolPaper())
        .comments(source.getComments())
        .build();
  }
}
