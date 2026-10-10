package com.guoyu.platform.exam.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.exam.dto.ExamKnowledgeRateCreateReq;
import com.guoyu.platform.exam.dto.ExamKnowledgeRateQueryReq;
import com.guoyu.platform.exam.dto.ExamKnowledgeRateUpdateReq;
import com.guoyu.platform.exam.entity.ExamKnowledgeRateDetailEntity;
import com.guoyu.platform.exam.entity.ExamKnowledgeRateEntity;
import com.guoyu.platform.exam.mapper.ExamKnowledgeRateDetailMapper;
import com.guoyu.platform.exam.mapper.ExamKnowledgeRateMapper;
import com.guoyu.platform.exam.vo.ExamKnowledgeRateVO;
import com.guoyu.platform.security.SecurityContext;
import java.time.LocalDateTime;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Slf4j
public class ExamKnowledgeRateApplicationService {

  private final ExamKnowledgeRateMapper mapper;
  private final ExamKnowledgeRateDetailMapper detailMapper;

  public ExamKnowledgeRateApplicationService(
      ExamKnowledgeRateMapper mapper,
      ExamKnowledgeRateDetailMapper detailMapper) {
    this.mapper = mapper;
    this.detailMapper = detailMapper;
  }

  @Transactional(readOnly = true)
  public PageResponse<ExamKnowledgeRateVO> page(ExamKnowledgeRateQueryReq req) {
    Integer tenantId = SecurityContext.currentUser().getTenantId().intValue();

    LambdaQueryWrapper<ExamKnowledgeRateEntity> wrapper = new LambdaQueryWrapper<ExamKnowledgeRateEntity>()
        .eq(ExamKnowledgeRateEntity::getTenantId, tenantId)
        .eq(req.getCourseId() != null, ExamKnowledgeRateEntity::getCourseId, req.getCourseId())
        .like(StringUtils.hasText(req.getKeyword()), ExamKnowledgeRateEntity::getTitle, req.getKeyword())
        .orderByAsc(ExamKnowledgeRateEntity::getSortNumber)
        .orderByDesc(ExamKnowledgeRateEntity::getId);

    Page<ExamKnowledgeRateEntity> page = mapper.selectPage(
        new Page<>(req.getPage(), req.getPageSize()), wrapper);

    List<ExamKnowledgeRateVO> vos = page.getRecords().stream()
        .map(ExamKnowledgeRateVO::from).toList();
    return new PageResponse<>(vos, page.getCurrent(), page.getSize(), page.getTotal());
  }

  @Transactional(readOnly = true)
  public List<ExamKnowledgeRateVO> list(Integer courseId) {
    Integer tenantId = SecurityContext.currentUser().getTenantId().intValue();

    LambdaQueryWrapper<ExamKnowledgeRateEntity> wrapper = new LambdaQueryWrapper<ExamKnowledgeRateEntity>()
        .eq(ExamKnowledgeRateEntity::getTenantId, tenantId)
        .eq(courseId != null, ExamKnowledgeRateEntity::getCourseId, courseId)
        .orderByAsc(ExamKnowledgeRateEntity::getSortNumber)
        .orderByDesc(ExamKnowledgeRateEntity::getId);

    return mapper.selectList(wrapper).stream()
        .map(ExamKnowledgeRateVO::from).toList();
  }

  @Transactional(readOnly = true)
  public ExamKnowledgeRateVO getById(Integer id) {
    ExamKnowledgeRateEntity entity = findExisting(id);
    ExamKnowledgeRateVO vo = ExamKnowledgeRateVO.from(entity);

    // 加载详情列表
    LambdaQueryWrapper<ExamKnowledgeRateDetailEntity> detailWrapper = new LambdaQueryWrapper<ExamKnowledgeRateDetailEntity>()
        .eq(ExamKnowledgeRateDetailEntity::getKnowledgeRateId, id)
        .orderByAsc(ExamKnowledgeRateDetailEntity::getSortNumber)
        .orderByAsc(ExamKnowledgeRateDetailEntity::getId);
    List<ExamKnowledgeRateDetailEntity> details = detailMapper.selectList(detailWrapper);
    vo.setDetailList(details.stream()
        .map(com.guoyu.platform.exam.vo.ExamKnowledgeRateDetailVO::from).toList());
    return vo;
  }

  @Transactional
  public ExamKnowledgeRateVO create(ExamKnowledgeRateCreateReq req) {
    Integer tenantId = SecurityContext.currentUser().getTenantId().intValue();
    Long userId = SecurityContext.currentUser().getUserId();

    ExamKnowledgeRateEntity entity = new ExamKnowledgeRateEntity();
    entity.setTenantId(tenantId);
    entity.setTitle(req.title());
    entity.setCourseId(req.courseId());
    entity.setType(req.type());
    entity.setIsStandard(req.isStandard());
    entity.setComments(req.comments());
    entity.setStatus(1);
    entity.setUserId(userId.intValue());
    entity.setDeleted(0);
    entity.setCreateTime(LocalDateTime.now());

    mapper.insert(entity);

    // 批量插入详情
    if (req.detailList() != null && !req.detailList().isEmpty()) {
      int sortNum = 1;
      for (var detailReq : req.detailList()) {
        ExamKnowledgeRateDetailEntity detail = new ExamKnowledgeRateDetailEntity();
        detail.setKnowledgeRateId(entity.getId());
        detail.setKnowledgeId(detailReq.knowledgeId());
        detail.setRate(detailReq.rate());
        detail.setUserId(userId.intValue());
        detail.setSortNumber(sortNum++);
        detail.setStatus(1);
        detail.setTenantId(tenantId);
        detail.setDeleted(0);
        detail.setCreateTime(LocalDateTime.now());
        detailMapper.insert(detail);
      }
    }

    return getById(entity.getId());
  }

  @Transactional
  public ExamKnowledgeRateVO update(Integer id, ExamKnowledgeRateUpdateReq req) {
    ExamKnowledgeRateEntity entity = findExisting(id);

    entity.setTitle(req.title());
    entity.setCourseId(req.courseId());
    entity.setType(req.type());
    entity.setIsStandard(req.isStandard());
    entity.setComments(req.comments());

    mapper.updateById(entity);

    // 删旧 detail + 批量插入新 detail
    LambdaQueryWrapper<ExamKnowledgeRateDetailEntity> deleteWrapper = new LambdaQueryWrapper<ExamKnowledgeRateDetailEntity>()
        .eq(ExamKnowledgeRateDetailEntity::getKnowledgeRateId, id);
    detailMapper.delete(deleteWrapper);

    if (req.detailList() != null && !req.detailList().isEmpty()) {
      Integer tenantId = SecurityContext.currentUser().getTenantId().intValue();
      Long userId = SecurityContext.currentUser().getUserId();
      int sortNum = 1;
      for (var detailReq : req.detailList()) {
        ExamKnowledgeRateDetailEntity detail = new ExamKnowledgeRateDetailEntity();
        detail.setKnowledgeRateId(id);
        detail.setKnowledgeId(detailReq.knowledgeId());
        detail.setRate(detailReq.rate());
        detail.setUserId(userId.intValue());
        detail.setSortNumber(sortNum++);
        detail.setStatus(1);
        detail.setTenantId(tenantId);
        detail.setDeleted(0);
        detail.setCreateTime(LocalDateTime.now());
        detailMapper.insert(detail);
      }
    }

    return getById(id);
  }

  @Transactional
  public void delete(Integer id) {
    findExisting(id);
    mapper.deleteById(id);

    // 删除关联详情
    LambdaQueryWrapper<ExamKnowledgeRateDetailEntity> deleteWrapper = new LambdaQueryWrapper<ExamKnowledgeRateDetailEntity>()
        .eq(ExamKnowledgeRateDetailEntity::getKnowledgeRateId, id);
    detailMapper.delete(deleteWrapper);
  }

  private ExamKnowledgeRateEntity findExisting(Integer id) {
    ExamKnowledgeRateEntity entity = mapper.selectById(id);
    if (entity == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "知识比重不存在");
    }
    return entity;
  }
}
