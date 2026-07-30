package com.guoyu.platform.industrial.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.industrial.dto.IndustrialCollegeCreateReq;
import com.guoyu.platform.industrial.dto.IndustrialCollegeQueryReq;
import com.guoyu.platform.industrial.dto.IndustrialCollegeUpdateReq;
import com.guoyu.platform.industrial.entity.IndustrialCollegeEntity;
import com.guoyu.platform.industrial.mapper.IndustrialCollegeMapper;
import com.guoyu.platform.industrial.vo.IndustrialCollegeStatsVO;
import com.guoyu.platform.industrial.vo.IndustrialCollegeStatsVO.TagCount;
import com.guoyu.platform.industrial.vo.IndustrialCollegeVO;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class IndustrialCollegeApplicationService {

  private static final String DEFAULT_STATUS = "enabled";

  private final IndustrialCollegeMapper mapper;

  public IndustrialCollegeApplicationService(IndustrialCollegeMapper mapper) {
    this.mapper = mapper;
  }

  @Transactional(readOnly = true)
  public PageResponse<IndustrialCollegeVO> page(IndustrialCollegeQueryReq req) {
    LambdaQueryWrapper<IndustrialCollegeEntity> wrapper = new LambdaQueryWrapper<IndustrialCollegeEntity>()
        .orderByAsc(IndustrialCollegeEntity::getSortOrder)
        .orderByDesc(IndustrialCollegeEntity::getCreatedAt);

    if (StringUtils.hasText(req.getKeyword())) {
      wrapper.and(w -> w
          .like(IndustrialCollegeEntity::getName, req.getKeyword())
          .or()
          .like(IndustrialCollegeEntity::getBusinessScope, req.getKeyword())
          .or()
          .like(IndustrialCollegeEntity::getAddress, req.getKeyword())
          .or()
          .like(IndustrialCollegeEntity::getPhone, req.getKeyword()));
    }
    if (StringUtils.hasText(req.getTag())) {
      wrapper.eq(IndustrialCollegeEntity::getTag, req.getTag());
    }
    if (StringUtils.hasText(req.getStatus())) {
      wrapper.eq(IndustrialCollegeEntity::getStatus, req.getStatus());
    }

    Page<IndustrialCollegeEntity> page = mapper.selectPage(new Page<>(req.getPage(), req.getPageSize()), wrapper);
    return new PageResponse<>(
        page.getRecords().stream().map(IndustrialCollegeVO::from).toList(),
        page.getCurrent(),
        page.getSize(),
        page.getTotal()
    );
  }

  @Transactional(readOnly = true)
  public IndustrialCollegeVO getById(Long id) {
    return IndustrialCollegeVO.from(findExisting(id));
  }

  @Transactional(readOnly = true)
  public IndustrialCollegeStatsVO stats() {
    long total = nullSafeCount(mapper.selectCount(new LambdaQueryWrapper<>()));
    long recentCreated = nullSafeCount(mapper.selectCount(new LambdaQueryWrapper<IndustrialCollegeEntity>()
        .ge(IndustrialCollegeEntity::getCreatedAt, LocalDateTime.now().minusDays(30))));
    List<Map<String, Object>> rows = mapper.selectMaps(new QueryWrapper<IndustrialCollegeEntity>()
        .select("COALESCE(NULLIF(tag, ''), '未分类') AS tag", "COUNT(1) AS college_count")
        .groupBy("COALESCE(NULLIF(tag, ''), '未分类')")
        .orderByDesc("college_count"));
    List<TagCount> byTag = rows.stream()
        .map(row -> new TagCount(String.valueOf(row.get("tag")), ((Number) row.get("college_count")).longValue()))
        .toList();
    return new IndustrialCollegeStatsVO(total, recentCreated, byTag);
  }

  @Transactional
  public IndustrialCollegeVO create(IndustrialCollegeCreateReq req) {
    IndustrialCollegeEntity entity = new IndustrialCollegeEntity();
    apply(entity, req.name(), req.tag(), req.businessScope(), req.description(), req.trainingQualification(),
        req.certificationQualification(), req.address(), req.phone(), req.status(), req.sortOrder());
    entity.setDeleted(0);
    mapper.insert(entity);
    return IndustrialCollegeVO.from(entity);
  }

  @Transactional
  public IndustrialCollegeVO update(Long id, IndustrialCollegeUpdateReq req) {
    IndustrialCollegeEntity entity = findExisting(id);
    apply(entity, req.name(), req.tag(), req.businessScope(), req.description(), req.trainingQualification(),
        req.certificationQualification(), req.address(), req.phone(), req.status(), req.sortOrder());
    mapper.updateById(entity);
    return IndustrialCollegeVO.from(entity);
  }

  @Transactional
  public void delete(Long id) {
    findExisting(id);
    mapper.deleteById(id);
  }

  private void apply(
      IndustrialCollegeEntity entity,
      String name,
      String tag,
      String businessScope,
      String description,
      String trainingQualification,
      String certificationQualification,
      String address,
      String phone,
      String status,
      Integer sortOrder
  ) {
    entity.setName(name);
    entity.setTag(tag);
    entity.setBusinessScope(businessScope);
    entity.setDescription(description);
    entity.setTrainingQualification(trainingQualification);
    entity.setCertificationQualification(certificationQualification);
    entity.setAddress(address);
    entity.setPhone(phone);
    entity.setStatus(StringUtils.hasText(status) ? status : DEFAULT_STATUS);
    entity.setSortOrder(sortOrder != null ? sortOrder : 100);
  }

  private IndustrialCollegeEntity findExisting(Long id) {
    IndustrialCollegeEntity entity = mapper.selectById(id);
    if (entity == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "园区学院不存在");
    }
    return entity;
  }

  private long nullSafeCount(Long count) {
    return count == null ? 0L : count;
  }
}
