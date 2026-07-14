package com.guoyu.platform.exam.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.exam.dto.ExamCourseCreateReq;
import com.guoyu.platform.exam.dto.ExamCourseQueryReq;
import com.guoyu.platform.exam.dto.ExamCourseUpdateReq;
import com.guoyu.platform.exam.entity.ExamCourseEntity;
import com.guoyu.platform.exam.mapper.ExamCourseMapper;
import com.guoyu.platform.exam.vo.ExamCourseVO;
import com.guoyu.platform.security.SecurityContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Slf4j
public class ExamCourseApplicationService {

  private final ExamCourseMapper mapper;

  public ExamCourseApplicationService(ExamCourseMapper mapper) {
    this.mapper = mapper;
  }

  @Transactional(readOnly = true)
  public PageResponse<ExamCourseVO> page(ExamCourseQueryReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    LambdaQueryWrapper<ExamCourseEntity> wrapper = new LambdaQueryWrapper<ExamCourseEntity>()
        .eq(ExamCourseEntity::getTenantId, tenantId)
        .orderByDesc(ExamCourseEntity::getSortNumber)
        .orderByAsc(ExamCourseEntity::getId);

    if (req.getWorkTypeId() != null && req.getWorkTypeId() > 0) {
      wrapper.eq(ExamCourseEntity::getWorkTypeId, req.getWorkTypeId());
    }
    if (StringUtils.hasText(req.getTitle())) {
      wrapper.and(w -> w
          .like(ExamCourseEntity::getTitle, req.getTitle())
          .or()
          .like(ExamCourseEntity::getSeriesNo, req.getTitle())
          .or()
          .like(ExamCourseEntity::getUserInChargeName, req.getTitle()));
    }

    Page<ExamCourseEntity> page = mapper.selectPage(
        new Page<>(req.getPage(), req.getPageSize()), wrapper);
    return new PageResponse<>(
        page.getRecords().stream().map(ExamCourseVO::from).toList(),
        page.getCurrent(),
        page.getSize(),
        page.getTotal()
    );
  }

  @Transactional(readOnly = true)
  public ExamCourseVO getById(Integer id) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    ExamCourseEntity entity = findExisting(tenantId, id);
    return ExamCourseVO.from(entity);
  }

  @CacheEvict(value = "questionBankTree", allEntries = true)
  @Transactional
  public ExamCourseVO create(ExamCourseCreateReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    ExamCourseEntity entity = new ExamCourseEntity();
    entity.setTenantId(tenantId);
    entity.setTitle(req.title());
    entity.setWorkTypeId(req.workTypeId());
    entity.setLevel(req.level());
    entity.setSeriesNoGroup(req.seriesNoGroup());
    entity.setSeriesNo(req.seriesNo());
    entity.setUserInChargeName(req.userInChargeName());
    entity.setLastLevelGetYear(req.lastLevelGetYear());
    entity.setWorkYearLimit(req.workYearLimit());
    entity.setMinLevel(req.minLevel());
    entity.setMinLevelYear(req.minLevelYear());
    entity.setStartAge(req.startAge());
    entity.setIsGeneral(req.isGeneral() != null ? req.isGeneral() : 0);
    entity.setExamCondition(req.examCondition());
    entity.setComments(req.comments());
    entity.setStatus(req.status() != null ? req.status() : 1);
    entity.setSortNumber(req.sortNumber() != null ? req.sortNumber() : 1);
    entity.setDeleted(0);

    mapper.insert(entity);
    return ExamCourseVO.from(entity);
  }

  @CacheEvict(value = "questionBankTree", allEntries = true)
  @Transactional
  public ExamCourseVO update(Integer id, ExamCourseUpdateReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    ExamCourseEntity entity = findExisting(tenantId, id);

    entity.setTitle(req.title());
    entity.setWorkTypeId(req.workTypeId());
    entity.setLevel(req.level());
    entity.setSeriesNoGroup(req.seriesNoGroup());
    entity.setSeriesNo(req.seriesNo());
    entity.setUserInChargeName(req.userInChargeName());
    entity.setLastLevelGetYear(req.lastLevelGetYear());
    entity.setWorkYearLimit(req.workYearLimit());
    entity.setMinLevel(req.minLevel());
    entity.setMinLevelYear(req.minLevelYear());
    entity.setStartAge(req.startAge());
    entity.setIsGeneral(req.isGeneral() != null ? req.isGeneral() : entity.getIsGeneral());
    entity.setExamCondition(req.examCondition());
    entity.setComments(req.comments());
    entity.setStatus(req.status() != null ? req.status() : entity.getStatus());
    entity.setSortNumber(req.sortNumber() != null ? req.sortNumber() : entity.getSortNumber());

    mapper.updateById(entity);
    return ExamCourseVO.from(entity);
  }

  @CacheEvict(value = "questionBankTree", allEntries = true)
  @Transactional
  public void delete(Integer id) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    findExisting(tenantId, id);
    mapper.deleteById(id);
  }

  private ExamCourseEntity findExisting(Long tenantId, Integer id) {
    ExamCourseEntity entity = mapper.selectOne(new LambdaQueryWrapper<ExamCourseEntity>()
        .eq(ExamCourseEntity::getTenantId, tenantId)
        .eq(ExamCourseEntity::getId, id));
    if (entity == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "科目不存在");
    }
    return entity;
  }
}
