package com.guoyu.platform.exam.service;

import lombok.extern.slf4j.Slf4j;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.exam.dto.ExamWorkTypeCreateReq;
import com.guoyu.platform.exam.dto.ExamWorkTypeQueryReq;
import com.guoyu.platform.exam.dto.ExamWorkTypeUpdateReq;
import com.guoyu.platform.exam.entity.ExamWorkTypeEntity;
import com.guoyu.platform.exam.mapper.ExamWorkTypeMapper;
import com.guoyu.platform.exam.vo.ExamWorkTypeVO;
import com.guoyu.platform.security.SecurityContext;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Slf4j
public class ExamWorkTypeApplicationService {

  private final ExamWorkTypeMapper mapper;

  public ExamWorkTypeApplicationService(ExamWorkTypeMapper mapper) {
    this.mapper = mapper;
  }

  /** Expose tenantId for @Cacheable SPEL key: #root.target.tenantId() */
  public Long tenantId() {
    return SecurityContext.currentUser().getTenantId();
  }

  @Transactional(readOnly = true)
  public PageResponse<ExamWorkTypeVO> page(ExamWorkTypeQueryReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    LambdaQueryWrapper<ExamWorkTypeEntity> wrapper = new LambdaQueryWrapper<ExamWorkTypeEntity>()
        .eq(ExamWorkTypeEntity::getTenantId, tenantId.intValue())
        .orderByDesc(ExamWorkTypeEntity::getSortNumber)
        .orderByAsc(ExamWorkTypeEntity::getId);

    if (StringUtils.hasText(req.getTitle())) {
      wrapper.and(w -> w
          .like(ExamWorkTypeEntity::getTitle, req.getTitle())
          .or()
          .like(ExamWorkTypeEntity::getSeriesNo, req.getTitle())
          .or()
          .like(ExamWorkTypeEntity::getLevelsNo, req.getTitle()));
    }

    Page<ExamWorkTypeEntity> page = mapper.selectPage(
        new Page<>(req.getPage(), req.getPageSize()), wrapper);
    return new PageResponse<>(
        page.getRecords().stream().map(ExamWorkTypeVO::from).toList(),
        page.getCurrent(),
        page.getSize(),
        page.getTotal()
    );
  }

  @Transactional(readOnly = true)
  public ExamWorkTypeVO getById(Integer id) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    ExamWorkTypeEntity entity = findExisting(tenantId, id);
    return ExamWorkTypeVO.from(entity);
  }

  @Cacheable(value = "workTypeTree", key = "#root.target.tenantId() + ':#ROOT'", unless = "#result == null")
  @Transactional(readOnly = true)
  public List<ExamWorkTypeVO> tree() {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    List<ExamWorkTypeEntity> all = mapper.selectList(new LambdaQueryWrapper<ExamWorkTypeEntity>()
        .eq(ExamWorkTypeEntity::getTenantId, tenantId.intValue())
        .orderByDesc(ExamWorkTypeEntity::getSortNumber)
        .orderByAsc(ExamWorkTypeEntity::getId));

    List<ExamWorkTypeVO> vos = all.stream().map(ExamWorkTypeVO::from).toList();
    Map<Integer, List<ExamWorkTypeVO>> childrenMap = vos.stream()
        .filter(v -> v.getParentId() != null && v.getParentId() > 0)
        .collect(Collectors.groupingBy(ExamWorkTypeVO::getParentId));

    List<ExamWorkTypeVO> roots = new ArrayList<>();
    for (ExamWorkTypeVO vo : vos) {
      Integer pid = vo.getParentId();
      if (pid == null || pid == 0) {
        roots.add(vo);
      }
      List<ExamWorkTypeVO> children = childrenMap.getOrDefault(vo.getId(), Collections.emptyList());
      vo.setChildren(children.isEmpty() ? null : children);
    }
    return roots;
  }

  @Caching(evict = {
      @CacheEvict(value = "workTypeTree", allEntries = true),
      @CacheEvict(value = "questionBankTree", allEntries = true)
  })
  @Transactional
  public ExamWorkTypeVO create(ExamWorkTypeCreateReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    ensureTitleAvailable(tenantId, req.parentId(), req.title(), null);

    ExamWorkTypeEntity entity = new ExamWorkTypeEntity();
    entity.setTenantId(tenantId.intValue());
    entity.setTitle(req.title());
    entity.setSeriesNo(req.seriesNo());
    entity.setLevelsNo(req.levelsNo());
    entity.setParentId(req.parentId());
    entity.setStatus(req.status() != null ? req.status() : 1);
    entity.setSortNumber(req.sortNumber() != null ? req.sortNumber() : 1);
    entity.setDeleted(0);

    mapper.insert(entity);
    return ExamWorkTypeVO.from(entity);
  }

  @Caching(evict = {
      @CacheEvict(value = "workTypeTree", allEntries = true),
      @CacheEvict(value = "questionBankTree", allEntries = true)
  })
  @Transactional
  public ExamWorkTypeVO update(Integer id, ExamWorkTypeUpdateReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    ExamWorkTypeEntity entity = findExisting(tenantId, id);
    ensureTitleAvailable(tenantId, req.parentId(), req.title(), id);

    entity.setTitle(req.title());
    entity.setSeriesNo(req.seriesNo());
    entity.setLevelsNo(req.levelsNo());
    entity.setParentId(req.parentId());
    entity.setStatus(req.status() != null ? req.status() : entity.getStatus());
    entity.setSortNumber(req.sortNumber() != null ? req.sortNumber() : entity.getSortNumber());

    mapper.updateById(entity);
    return ExamWorkTypeVO.from(entity);
  }

  @Caching(evict = {
      @CacheEvict(value = "workTypeTree", allEntries = true),
      @CacheEvict(value = "questionBankTree", allEntries = true)
  })
  @Transactional
  public void delete(Integer id) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    findExisting(tenantId, id);
    List<Integer> idsToDelete = new ArrayList<>();
    idsToDelete.add(id);
    collectChildIds(tenantId, id, idsToDelete);
    mapper.deleteBatchIds(idsToDelete);
  }

  private void collectChildIds(Long tenantId, Integer parentId, List<Integer> collector) {
    List<ExamWorkTypeEntity> children = mapper.selectList(new LambdaQueryWrapper<ExamWorkTypeEntity>()
        .eq(ExamWorkTypeEntity::getTenantId, tenantId.intValue())
        .eq(ExamWorkTypeEntity::getParentId, parentId));
    for (ExamWorkTypeEntity child : children) {
      collector.add(child.getId());
      collectChildIds(tenantId, child.getId(), collector);
    }
  }

  private void ensureTitleAvailable(Long tenantId, Integer parentId, String title, Integer excludeId) {
    LambdaQueryWrapper<ExamWorkTypeEntity> wrapper = new LambdaQueryWrapper<ExamWorkTypeEntity>()
        .eq(ExamWorkTypeEntity::getTenantId, tenantId.intValue())
        .eq(ExamWorkTypeEntity::getTitle, title)
        .ne(excludeId != null, ExamWorkTypeEntity::getId, excludeId);
    if (parentId != null && parentId > 0) {
      wrapper.eq(ExamWorkTypeEntity::getParentId, parentId);
    } else {
      wrapper.and(w -> w.isNull(ExamWorkTypeEntity::getParentId).or().eq(ExamWorkTypeEntity::getParentId, 0));
    }
    boolean exists = mapper.exists(wrapper);
    if (exists) {
      throw new BusinessException(ErrorCode.CONFLICT, "同一父级下已存在同名工种");
    }
  }

  private ExamWorkTypeEntity findExisting(Long tenantId, Integer id) {
    ExamWorkTypeEntity entity = mapper.selectOne(new LambdaQueryWrapper<ExamWorkTypeEntity>()
        .eq(ExamWorkTypeEntity::getTenantId, tenantId.intValue())
        .eq(ExamWorkTypeEntity::getId, id));
    if (entity == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "工种不存在");
    }
    return entity;
  }
}
