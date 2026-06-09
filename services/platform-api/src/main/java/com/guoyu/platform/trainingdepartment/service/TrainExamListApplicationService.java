package com.guoyu.platform.trainingdepartment.service;

import lombok.extern.slf4j.Slf4j;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.trainingdepartment.dto.TrainExamListCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainExamListQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainExamListUpdateReq;
import com.guoyu.platform.trainingdepartment.entity.TrainExamListEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainExamPeopleListEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainExamWorkTypeListEntity;
import com.guoyu.platform.trainingdepartment.mapper.TrainExamListMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainExamPeopleListMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainExamWorkTypeListMapper;
import com.guoyu.platform.trainingdepartment.vo.TrainExamListVO;
import com.guoyu.platform.trainingdepartment.vo.TrainExamPeopleListVO;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Slf4j
public class TrainExamListApplicationService {

  private final TrainExamListMapper mapper;
  private final TrainExamWorkTypeListMapper workTypeListMapper;
  private final TrainExamPeopleListMapper peopleListMapper;

  public TrainExamListApplicationService(TrainExamListMapper mapper,
      TrainExamWorkTypeListMapper workTypeListMapper,
      TrainExamPeopleListMapper peopleListMapper) {
    this.mapper = mapper;
    this.workTypeListMapper = workTypeListMapper;
    this.peopleListMapper = peopleListMapper;
  }

  @Transactional(readOnly = true)
  public PageResponse<TrainExamListVO> page(TrainExamListQueryReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    LambdaQueryWrapper<TrainExamListEntity> wrapper = new LambdaQueryWrapper<TrainExamListEntity>()
        .eq(TrainExamListEntity::getTenantId, tenantId)
        .eq(TrainExamListEntity::getDeleted, 0)
        .orderByDesc(TrainExamListEntity::getCreateTime);
    if (req.getTrainId() != null) {
      wrapper.eq(TrainExamListEntity::getTrainId, req.getTrainId());
    }
    if (req.getOrgId() != null) {
      wrapper.eq(TrainExamListEntity::getOrgId, req.getOrgId());
    }
    if (req.getType() != null) {
      wrapper.eq(TrainExamListEntity::getType, req.getType());
    }
    if (req.getSource() != null) {
      wrapper.eq(TrainExamListEntity::getSource, req.getSource());
    }
    if (StringUtils.hasText(req.getKeyword())) {
      wrapper.and(w -> w
          .like(TrainExamListEntity::getTitle, req.getKeyword())
          .or()
          .like(TrainExamListEntity::getOrgName, req.getKeyword()));
    }
    Page<TrainExamListEntity> page = mapper.selectPage(new Page<>(req.getPage(), req.getPageSize()), wrapper);
    return new PageResponse<>(page.getRecords().stream().map(this::toVO).toList(),
        page.getCurrent(), page.getSize(), page.getTotal());
  }

  @Transactional(readOnly = true)
  public TrainExamListVO getById(Long id) {
    return toVO(getExisting(id));
  }

  @Transactional
  public TrainExamListVO create(TrainExamListCreateReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    Long userId = SecurityContext.currentUser().getUserId();
    TrainExamListEntity entity = new TrainExamListEntity();
    entity.setTenantId(tenantId);
    entity.setUserId(userId);
    entity.setTitle(req.getTitle());
    entity.setLevel(req.getLevel());
    entity.setTrainId(req.getTrainId());
    entity.setOrgId(req.getOrgId());
    entity.setOrgName(req.getOrgName());
    entity.setType(req.getType());
    entity.setSource(req.getSource());
    entity.setInvigilateUserId(req.getInvigilateUserId());
    entity.setInvigilateUsername(req.getInvigilateUsername());
    entity.setEvaluateUserId(req.getEvaluateUserId());
    entity.setEvaluateUsername(req.getEvaluateUsername());
    entity.setWatcherUserId(req.getWatcherUserId());
    entity.setWatcherUsername(req.getWatcherUsername());
    entity.setStaffUserId(req.getStaffUserId());
    entity.setStaffUsername(req.getStaffUsername());
    entity.setPropositionUserId(req.getPropositionUserId());
    entity.setPropositionUsername(req.getPropositionUsername());
    entity.setMarkingUserId(req.getMarkingUserId());
    entity.setMarkingUsername(req.getMarkingUsername());
    entity.setExamId(req.getExamId());
    entity.setExamDate(req.getExamDate());
    entity.setExamDateMode(req.getExamDateMode());
    entity.setExamDateList(req.getExamDateList());
    entity.setExamTime(req.getExamTime());
    entity.setNeedFinishAdventure(req.getNeedFinishAdventure());
    entity.setNeedFinishNormal(req.getNeedFinishNormal());
    entity.setJigouId(req.getJigouId());
    entity.setSortNumber(req.getSortNumber() != null ? req.getSortNumber() : 1);
    entity.setDeleted(0);
    mapper.insert(entity);

    // Save work type list entries if course list provided
    if (req.getCourseIdList() != null && !req.getCourseIdList().isEmpty()) {
      saveWorkTypeList(entity.getId(), req.getCourseIdList(), userId, tenantId);
    }

    return toVO(entity);
  }

  @Transactional
  public TrainExamListVO update(Long id, TrainExamListUpdateReq req) {
    TrainExamListEntity entity = getExisting(id);
    Long userId = SecurityContext.currentUser().getUserId();
    if (StringUtils.hasText(req.getTitle())) { entity.setTitle(req.getTitle()); }
    if (StringUtils.hasText(req.getLevel())) { entity.setLevel(req.getLevel()); }
    if (req.getOrgId() != null) { entity.setOrgId(req.getOrgId()); }
    if (StringUtils.hasText(req.getOrgName())) { entity.setOrgName(req.getOrgName()); }
    if (req.getType() != null) { entity.setType(req.getType()); }
    if (req.getSource() != null) { entity.setSource(req.getSource()); }
    if (req.getInvigilateUserId() != null) { entity.setInvigilateUserId(req.getInvigilateUserId()); }
    if (StringUtils.hasText(req.getInvigilateUsername())) { entity.setInvigilateUsername(req.getInvigilateUsername()); }
    if (req.getEvaluateUserId() != null) { entity.setEvaluateUserId(req.getEvaluateUserId()); }
    if (StringUtils.hasText(req.getEvaluateUsername())) { entity.setEvaluateUsername(req.getEvaluateUsername()); }
    if (req.getWatcherUserId() != null) { entity.setWatcherUserId(req.getWatcherUserId()); }
    if (StringUtils.hasText(req.getWatcherUsername())) { entity.setWatcherUsername(req.getWatcherUsername()); }
    if (req.getStaffUserId() != null) { entity.setStaffUserId(req.getStaffUserId()); }
    if (StringUtils.hasText(req.getStaffUsername())) { entity.setStaffUsername(req.getStaffUsername()); }
    if (req.getPropositionUserId() != null) { entity.setPropositionUserId(req.getPropositionUserId()); }
    if (StringUtils.hasText(req.getPropositionUsername())) { entity.setPropositionUsername(req.getPropositionUsername()); }
    if (req.getMarkingUserId() != null) { entity.setMarkingUserId(req.getMarkingUserId()); }
    if (StringUtils.hasText(req.getMarkingUsername())) { entity.setMarkingUsername(req.getMarkingUsername()); }
    if (req.getExamId() != null) { entity.setExamId(req.getExamId()); }
    if (StringUtils.hasText(req.getExamDate())) { entity.setExamDate(req.getExamDate()); }
    if (req.getExamDateMode() != null) { entity.setExamDateMode(req.getExamDateMode()); }
    if (StringUtils.hasText(req.getExamDateList())) { entity.setExamDateList(req.getExamDateList()); }
    if (req.getExamTime() != null) { entity.setExamTime(req.getExamTime()); }
    if (req.getNeedFinishAdventure() != null) { entity.setNeedFinishAdventure(req.getNeedFinishAdventure()); }
    if (req.getNeedFinishNormal() != null) { entity.setNeedFinishNormal(req.getNeedFinishNormal()); }
    if (req.getSortNumber() != null) { entity.setSortNumber(req.getSortNumber()); }
    mapper.updateById(entity);

    // Replace work type list entries if course list provided
    if (req.getCourseIdList() != null && !req.getCourseIdList().isEmpty()) {
      Long tenantId = SecurityContext.currentUser().getTenantId();
      saveWorkTypeList(id, req.getCourseIdList(), userId, tenantId);
    }

    return toVO(entity);
  }

  @Transactional
  public void delete(Long id) {
    TrainExamListEntity entity = getExisting(id);
    entity.setDeleted(1);
    mapper.deleteById(entity.getId());
  }

  // ======== People management (考生管理) ========

  @Transactional(readOnly = true)
  public PageResponse<TrainExamPeopleListVO> getPeopleGradeList(Long examId, String keyword, Integer page, Integer pageSize) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    LambdaQueryWrapper<TrainExamPeopleListEntity> wrapper = new LambdaQueryWrapper<TrainExamPeopleListEntity>()
        .eq(TrainExamPeopleListEntity::getTenantId, tenantId)
        .eq(TrainExamPeopleListEntity::getExamId, examId)
        .eq(TrainExamPeopleListEntity::getDeleted, 0)
        .orderByDesc(TrainExamPeopleListEntity::getId);
    if (StringUtils.hasText(keyword)) {
      wrapper.and(w -> w
          .like(TrainExamPeopleListEntity::getRealname, keyword)
          .or()
          .like(TrainExamPeopleListEntity::getPhone, keyword));
    }
    Page<TrainExamPeopleListEntity> result = peopleListMapper.selectPage(
        new Page<>(page != null ? page : 1, pageSize != null ? pageSize : 10), wrapper);
    return new PageResponse<>(result.getRecords().stream().map(this::toPeopleVO).toList(),
        result.getCurrent(), result.getSize(), result.getTotal());
  }

  @Transactional
  public void updatePeopleGrade(Long id, Integer inWhiteList, BigDecimal normalScore,
      BigDecimal score, BigDecimal totalScore) {
    TrainExamPeopleListEntity entity = getPeopleExisting(id);
    if (inWhiteList != null) {
      entity.setInWhiteList(inWhiteList);
    }
    if (normalScore != null && totalScore != null) {
      entity.setNormalScore(normalScore);
      entity.setTotalScore(totalScore);
    }
    if (score != null && totalScore != null) {
      entity.setScore(score);
      entity.setTotalScore(totalScore);
    }
    peopleListMapper.updateById(entity);
  }

  @Transactional
  public int importPeopleByIdCard(Long examId, String idCardNumbers) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    if (examId == null || examId == 0) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "考试ID不能为空");
    }
    if (!StringUtils.hasText(idCardNumbers)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "没有要导入的身份证号");
    }

    String[] idcards = idCardNumbers.split("[\\r\\n]+");
    int count = 0;
    for (String idcard : idcards) {
      idcard = idcard.trim();
      if (idcard.isEmpty() || idcard.length() != 18) {
        continue;
      }
      Long existingCount = peopleListMapper.selectCount(new LambdaQueryWrapper<TrainExamPeopleListEntity>()
          .eq(TrainExamPeopleListEntity::getTenantId, tenantId)
          .eq(TrainExamPeopleListEntity::getExamId, examId)
          .eq(TrainExamPeopleListEntity::getIdCardNum, idcard)
          .eq(TrainExamPeopleListEntity::getDeleted, 0));
      if (existingCount > 0) {
        continue;
      }
      TrainExamPeopleListEntity entity = new TrainExamPeopleListEntity();
      entity.setExamId(examId);
      entity.setIdCardNum(idcard);
      entity.setTenantId(tenantId);
      entity.setDeleted(0);
      entity.setSortNumber(1);
      entity.setLearnTime(0);
      entity.setLearnUseTime(0);
      peopleListMapper.insert(entity);
      count++;
    }
    return count;
  }

  @Transactional
  public void batchDeletePeople(List<Long> ids) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    for (Long id : ids) {
      TrainExamPeopleListEntity entity = peopleListMapper.selectOne(
          new LambdaQueryWrapper<TrainExamPeopleListEntity>()
              .eq(TrainExamPeopleListEntity::getTenantId, tenantId)
              .eq(TrainExamPeopleListEntity::getId, id)
              .eq(TrainExamPeopleListEntity::getDeleted, 0));
      if (entity != null) {
        entity.setDeleted(1);
        peopleListMapper.deleteById(entity.getId());
      }
    }
  }

  // ======== Private helpers ========

  private void saveWorkTypeList(Long examId, List<Long> courseIdList, Long userId, Long tenantId) {
    // Soft-delete existing entries
    List<TrainExamWorkTypeListEntity> existing = workTypeListMapper.selectList(
        new LambdaQueryWrapper<TrainExamWorkTypeListEntity>()
            .eq(TrainExamWorkTypeListEntity::getExamId, examId)
            .eq(TrainExamWorkTypeListEntity::getDeleted, 0));
    for (TrainExamWorkTypeListEntity e : existing) {
      e.setDeleted(1);
      workTypeListMapper.deleteById(e.getId());
    }
    for (Long courseId : courseIdList) {
      TrainExamWorkTypeListEntity wte = new TrainExamWorkTypeListEntity();
      wte.setExamId(examId);
      wte.setCourseId(courseId);
      wte.setTenantId(tenantId);
      wte.setDeleted(0);
      workTypeListMapper.insert(wte);
    }
  }

  private TrainExamPeopleListEntity getPeopleExisting(Long id) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    TrainExamPeopleListEntity entity = peopleListMapper.selectOne(
        new LambdaQueryWrapper<TrainExamPeopleListEntity>()
            .eq(TrainExamPeopleListEntity::getTenantId, tenantId)
            .eq(TrainExamPeopleListEntity::getId, id)
            .eq(TrainExamPeopleListEntity::getDeleted, 0));
    if (entity == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "Record not found");
    }
    return entity;
  }

  private TrainExamListEntity getExisting(Long id) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    TrainExamListEntity entity = mapper.selectOne(new LambdaQueryWrapper<TrainExamListEntity>()
        .eq(TrainExamListEntity::getTenantId, tenantId)
        .eq(TrainExamListEntity::getId, id)
        .eq(TrainExamListEntity::getDeleted, 0));
    if (entity == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "Record not found");
    }
    return entity;
  }

  private TrainExamListVO toVO(TrainExamListEntity entity) {
    return new TrainExamListVO(
        entity.getId(), entity.getTitle(), entity.getLevel(), entity.getTrainId(),
        entity.getOrgId(), entity.getOrgName(), entity.getType(), entity.getSource(),
        entity.getInvigilateUserId(), entity.getInvigilateUsername(),
        entity.getEvaluateUserId(), entity.getEvaluateUsername(),
        entity.getWatcherUserId(), entity.getWatcherUsername(),
        entity.getStaffUserId(), entity.getStaffUsername(),
        entity.getPropositionUserId(), entity.getPropositionUsername(),
        entity.getMarkingUserId(), entity.getMarkingUsername(),
        entity.getExamId(), entity.getExamDate(), entity.getExamDateMode(),
        entity.getExamDateList(), entity.getExamTime(),
        entity.getNeedFinishAdventure(), entity.getNeedFinishNormal(),
        entity.getUserId(), entity.getSortNumber(), entity.getTenantId(),
        entity.getJigouId(), entity.getCreateTime()
    );
  }

  private TrainExamPeopleListVO toPeopleVO(TrainExamPeopleListEntity e) {
    return new TrainExamPeopleListVO(
        e.getId(), e.getExamId(), e.getPlatformUserId(), e.getWorkTypeId(),
        e.getWorkTypeLevel(), e.getRealname(), e.getIdCardNum(), e.getPhone(),
        e.getEducation(), e.getRoomId(), e.getInWhiteList(), e.getNormalScore(),
        e.getScore(), e.getTotalScore(), e.getLearnTime(), e.getLearnUseTime(),
        e.getUserId(), e.getSortNumber(), e.getTenantId(), e.getCreateTime(), e.getUpdateTime()
    );
  }
}
