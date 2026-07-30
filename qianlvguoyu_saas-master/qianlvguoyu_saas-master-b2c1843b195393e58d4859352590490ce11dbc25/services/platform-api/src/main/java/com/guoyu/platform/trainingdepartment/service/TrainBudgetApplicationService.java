package com.guoyu.platform.trainingdepartment.service;

import lombok.extern.slf4j.Slf4j;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.trainingdepartment.dto.TrainBudgetCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainBudgetQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainBudgetUpdateReq;
import com.guoyu.platform.trainingdepartment.entity.TrainBudgetEntity;
import com.guoyu.platform.trainingdepartment.mapper.TrainBudgetMapper;
import com.guoyu.platform.trainingdepartment.vo.TrainBudgetVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 培训费用管理服务实现
 */
@Service
@Slf4j
public class TrainBudgetApplicationService {

  private final TrainBudgetMapper mapper;

  public TrainBudgetApplicationService(TrainBudgetMapper mapper) {
    this.mapper = mapper;
  }

  @Transactional(readOnly = true)
  public PageResponse<TrainBudgetVO> page(TrainBudgetQueryReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    LambdaQueryWrapper<TrainBudgetEntity> wrapper = new LambdaQueryWrapper<TrainBudgetEntity>()
        .eq(TrainBudgetEntity::getTenantId, tenantId)
        .eq(TrainBudgetEntity::getDeleted, 0)
        .orderByDesc(TrainBudgetEntity::getCreateTime);
    if (StringUtils.hasText(req.getKeyword())) {
      wrapper.and(w -> w
          .like(TrainBudgetEntity::getProvince, req.getKeyword())
          .or()
          .like(TrainBudgetEntity::getCity, req.getKeyword())
          .or()
          .like(TrainBudgetEntity::getOrgName, req.getKeyword())
          .or()
          .like(TrainBudgetEntity::getTrainWorkTypeName, req.getKeyword()));
    }
    if (req.getTrainId() != null) {
      wrapper.eq(TrainBudgetEntity::getTrainId, req.getTrainId());
    }
    if (req.getOrgId() != null) {
      wrapper.eq(TrainBudgetEntity::getOrgId, req.getOrgId());
    }
    if (req.getStatus() != null) {
      wrapper.eq(TrainBudgetEntity::getStatus, req.getStatus());
    }
    Page<TrainBudgetEntity> page = mapper.selectPage(new Page<>(req.getPage(), req.getPageSize()), wrapper);
    return new PageResponse<>(page.getRecords().stream().map(this::toVO).toList(),
        page.getCurrent(), page.getSize(), page.getTotal());
  }

  @Transactional(readOnly = true)
  public TrainBudgetVO getById(Long id) {
    return toVO(getExisting(id));
  }

  @Transactional
  public TrainBudgetVO create(TrainBudgetCreateReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    Long userId = SecurityContext.currentUser().getUserId();
    TrainBudgetEntity entity = new TrainBudgetEntity();
    entity.setTenantId(tenantId);
    entity.setUserId(userId);
    entity.setTrainId(req.getTrainId());
    entity.setProvince(req.getProvince());
    entity.setCity(req.getCity());
    entity.setRegion(req.getRegion());
    entity.setOrgId(req.getOrgId());
    entity.setOrgName(req.getOrgName());
    entity.setTrainDate(req.getTrainDate());
    entity.setTrainWorkTypeId(req.getTrainWorkTypeId());
    entity.setTrainWorkTypeName(req.getTrainWorkTypeName());
    entity.setTrainLevel(req.getTrainLevel());
    entity.setBonusStandard(req.getBonusStandard());
    entity.setExpectIncome(req.getExpectIncome());
    entity.setActualIncome(req.getActualIncome());
    entity.setSchoolManageFee(req.getSchoolManageFee());
    entity.setManageFee(req.getManageFee());
    entity.setResearchFee(req.getResearchFee());
    entity.setCompanyRequireCommission(req.getCompanyRequireCommission());
    entity.setAgentRequireCommission(req.getAgentRequireCommission());
    entity.setTeacherCourseFee(req.getTeacherCourseFee());
    entity.setTeacherFee(req.getTeacherFee());
    entity.setStaffFee(req.getStaffFee());
    entity.setStaffTravelFee(req.getStaffTravelFee());
    entity.setTrainFee(req.getTrainFee());
    entity.setPointRewards(req.getPointRewards());
    entity.setBusinessFee(req.getBusinessFee());
    entity.setPeopleNum(req.getPeopleNum());
    entity.setChargeFee(req.getChargeFee());
    entity.setServiceFee(req.getServiceFee());
    entity.setReviewFee(req.getReviewFee());
    entity.setMaintainFee(req.getMaintainFee());
    entity.setMakeupPeopleNum(req.getMakeupPeopleNum());
    entity.setTheoryMakeupPeopleNum(req.getTheoryMakeupPeopleNum());
    entity.setOperationMakeupPeopleNum(req.getOperationMakeupPeopleNum());
    entity.setStaffMakeupTrainFee(req.getStaffMakeupTrainFee());
    entity.setReviewMakeupTrainFee(req.getReviewMakeupTrainFee());
    entity.setProfit(req.getProfit());
    entity.setComments(req.getComments());
    entity.setStatus(req.getStatus());
    entity.setSortNumber(req.getSortNumber());
    entity.setDeleted(0);
    mapper.insert(entity);
    return toVO(entity);
  }

  @Transactional
  public TrainBudgetVO update(Long id, TrainBudgetUpdateReq req) {
    TrainBudgetEntity entity = getExisting(id);
    if (req.getTrainId() != null) {
      entity.setTrainId(req.getTrainId());
    }
    if (StringUtils.hasText(req.getProvince())) {
      entity.setProvince(req.getProvince());
    }
    if (StringUtils.hasText(req.getCity())) {
      entity.setCity(req.getCity());
    }
    if (StringUtils.hasText(req.getRegion())) {
      entity.setRegion(req.getRegion());
    }
    if (req.getOrgId() != null) {
      entity.setOrgId(req.getOrgId());
    }
    if (StringUtils.hasText(req.getOrgName())) {
      entity.setOrgName(req.getOrgName());
    }
    if (StringUtils.hasText(req.getTrainDate())) {
      entity.setTrainDate(req.getTrainDate());
    }
    if (req.getTrainWorkTypeId() != null) {
      entity.setTrainWorkTypeId(req.getTrainWorkTypeId());
    }
    if (StringUtils.hasText(req.getTrainWorkTypeName())) {
      entity.setTrainWorkTypeName(req.getTrainWorkTypeName());
    }
    if (StringUtils.hasText(req.getTrainLevel())) {
      entity.setTrainLevel(req.getTrainLevel());
    }
    if (StringUtils.hasText(req.getBonusStandard())) {
      entity.setBonusStandard(req.getBonusStandard());
    }
    if (req.getExpectIncome() != null) {
      entity.setExpectIncome(req.getExpectIncome());
    }
    if (req.getActualIncome() != null) {
      entity.setActualIncome(req.getActualIncome());
    }
    if (req.getSchoolManageFee() != null) {
      entity.setSchoolManageFee(req.getSchoolManageFee());
    }
    if (req.getManageFee() != null) {
      entity.setManageFee(req.getManageFee());
    }
    if (req.getResearchFee() != null) {
      entity.setResearchFee(req.getResearchFee());
    }
    if (req.getCompanyRequireCommission() != null) {
      entity.setCompanyRequireCommission(req.getCompanyRequireCommission());
    }
    if (req.getAgentRequireCommission() != null) {
      entity.setAgentRequireCommission(req.getAgentRequireCommission());
    }
    if (req.getTeacherCourseFee() != null) {
      entity.setTeacherCourseFee(req.getTeacherCourseFee());
    }
    if (req.getTeacherFee() != null) {
      entity.setTeacherFee(req.getTeacherFee());
    }
    if (req.getStaffFee() != null) {
      entity.setStaffFee(req.getStaffFee());
    }
    if (req.getStaffTravelFee() != null) {
      entity.setStaffTravelFee(req.getStaffTravelFee());
    }
    if (req.getTrainFee() != null) {
      entity.setTrainFee(req.getTrainFee());
    }
    if (req.getPointRewards() != null) {
      entity.setPointRewards(req.getPointRewards());
    }
    if (req.getBusinessFee() != null) {
      entity.setBusinessFee(req.getBusinessFee());
    }
    if (req.getPeopleNum() != null) {
      entity.setPeopleNum(req.getPeopleNum());
    }
    if (StringUtils.hasText(req.getChargeFee())) {
      entity.setChargeFee(req.getChargeFee());
    }
    if (req.getServiceFee() != null) {
      entity.setServiceFee(req.getServiceFee());
    }
    if (req.getReviewFee() != null) {
      entity.setReviewFee(req.getReviewFee());
    }
    if (req.getMaintainFee() != null) {
      entity.setMaintainFee(req.getMaintainFee());
    }
    if (req.getMakeupPeopleNum() != null) {
      entity.setMakeupPeopleNum(req.getMakeupPeopleNum());
    }
    if (req.getTheoryMakeupPeopleNum() != null) {
      entity.setTheoryMakeupPeopleNum(req.getTheoryMakeupPeopleNum());
    }
    if (req.getOperationMakeupPeopleNum() != null) {
      entity.setOperationMakeupPeopleNum(req.getOperationMakeupPeopleNum());
    }
    if (req.getStaffMakeupTrainFee() != null) {
      entity.setStaffMakeupTrainFee(req.getStaffMakeupTrainFee());
    }
    if (req.getReviewMakeupTrainFee() != null) {
      entity.setReviewMakeupTrainFee(req.getReviewMakeupTrainFee());
    }
    if (req.getProfit() != null) {
      entity.setProfit(req.getProfit());
    }
    if (StringUtils.hasText(req.getComments())) {
      entity.setComments(req.getComments());
    }
    if (req.getStatus() != null) {
      entity.setStatus(req.getStatus());
    }
    if (req.getSortNumber() != null) {
      entity.setSortNumber(req.getSortNumber());
    }
    mapper.updateById(entity);
    return toVO(entity);
  }

  @Transactional
  public void delete(Long id) {
    TrainBudgetEntity entity = getExisting(id);
    entity.setDeleted(1);
    mapper.updateById(entity);
  }

  private TrainBudgetEntity getExisting(Long id) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    TrainBudgetEntity entity = mapper.selectOne(new LambdaQueryWrapper<TrainBudgetEntity>()
        .eq(TrainBudgetEntity::getTenantId, tenantId)
        .eq(TrainBudgetEntity::getId, id)
        .eq(TrainBudgetEntity::getDeleted, 0));
    if (entity == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "Record not found");
    }
    return entity;
  }

  private TrainBudgetVO toVO(TrainBudgetEntity entity) {
    return new TrainBudgetVO(
        entity.getId(),
        entity.getTrainId(),
        entity.getProvince(),
        entity.getCity(),
        entity.getRegion(),
        entity.getOrgId(),
        entity.getOrgName(),
        entity.getTrainDate(),
        entity.getTrainWorkTypeId(),
        entity.getTrainWorkTypeName(),
        entity.getTrainLevel(),
        entity.getBonusStandard(),
        entity.getExpectIncome(),
        entity.getActualIncome(),
        entity.getSchoolManageFee(),
        entity.getManageFee(),
        entity.getResearchFee(),
        entity.getCompanyRequireCommission(),
        entity.getAgentRequireCommission(),
        entity.getTeacherCourseFee(),
        entity.getTeacherFee(),
        entity.getStaffFee(),
        entity.getStaffTravelFee(),
        entity.getTrainFee(),
        entity.getPointRewards(),
        entity.getBusinessFee(),
        entity.getPeopleNum(),
        entity.getChargeFee(),
        entity.getServiceFee(),
        entity.getReviewFee(),
        entity.getMaintainFee(),
        entity.getMakeupPeopleNum(),
        entity.getTheoryMakeupPeopleNum(),
        entity.getOperationMakeupPeopleNum(),
        entity.getStaffMakeupTrainFee(),
        entity.getReviewMakeupTrainFee(),
        entity.getProfit(),
        entity.getComments(),
        entity.getStatus(),
        entity.getSortNumber(),
        entity.getTenantId(),
        entity.getUserId(),
        entity.getCreateTime(),
        entity.getUpdateTime()
    );
  }
}
