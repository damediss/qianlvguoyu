package com.guoyu.platform.industrial.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.industrial.dto.IndustrialPlatformItemReq;
import com.guoyu.platform.industrial.dto.IndustrialPlatformQueryReq;
import com.guoyu.platform.industrial.entity.IndustrialPlatformItemEntity;
import com.guoyu.platform.industrial.mapper.IndustrialPlatformItemMapper;
import com.guoyu.platform.industrial.vo.IndustrialPlatformItemVO;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class IndustrialPlatformApplicationService {

  private static final Set<String> CATEGORIES = Set.of("master", "study", "match", "apprentice", "comment");
  private static final Map<String, String> NUMS_REQUIRED_MESSAGE = Map.of(
      "master", "团队人数不能为空",
      "study", "人数不能为空",
      "apprentice", "人数不能为空"
  );

  private final IndustrialPlatformItemMapper mapper;

  public IndustrialPlatformApplicationService(IndustrialPlatformItemMapper mapper) {
    this.mapper = mapper;
  }

  @Transactional(readOnly = true)
  public PageResponse<IndustrialPlatformItemVO> page(String category, IndustrialPlatformQueryReq req) {
    String normalizedCategory = normalizeCategory(category);
    LambdaQueryWrapper<IndustrialPlatformItemEntity> wrapper = new LambdaQueryWrapper<IndustrialPlatformItemEntity>()
        .eq(IndustrialPlatformItemEntity::getCategory, normalizedCategory)
        .orderByAsc(IndustrialPlatformItemEntity::getSortOrder)
        .orderByDesc(IndustrialPlatformItemEntity::getItemDate)
        .orderByDesc(IndustrialPlatformItemEntity::getCreatedAt);

    if (StringUtils.hasText(req.getKeyword())) {
      wrapper.and(w -> w
          .like(IndustrialPlatformItemEntity::getCompany, req.getKeyword())
          .or()
          .like(IndustrialPlatformItemEntity::getName, req.getKeyword())
          .or()
          .like(IndustrialPlatformItemEntity::getState, req.getKeyword()));
    }
    if (StringUtils.hasText(req.getCompany())) {
      wrapper.like(IndustrialPlatformItemEntity::getCompany, req.getCompany());
    }
    if (StringUtils.hasText(req.getState())) {
      wrapper.eq(IndustrialPlatformItemEntity::getState, req.getState());
    }

    Page<IndustrialPlatformItemEntity> page = mapper.selectPage(new Page<>(req.getPage(), req.getPageSize()), wrapper);
    return new PageResponse<>(
        page.getRecords().stream().map(IndustrialPlatformItemVO::from).toList(),
        page.getCurrent(),
        page.getSize(),
        page.getTotal()
    );
  }

  @Transactional(readOnly = true)
  public IndustrialPlatformItemVO getById(String category, Long id) {
    return IndustrialPlatformItemVO.from(findExisting(category, id));
  }

  @Transactional
  public IndustrialPlatformItemVO create(String category, IndustrialPlatformItemReq req) {
    String normalizedCategory = normalizeCategory(category);
    validate(normalizedCategory, req);
    IndustrialPlatformItemEntity entity = new IndustrialPlatformItemEntity();
    entity.setCategory(normalizedCategory);
    apply(entity, req);
    entity.setDeleted(0);
    mapper.insert(entity);
    return IndustrialPlatformItemVO.from(entity);
  }

  @Transactional
  public IndustrialPlatformItemVO update(String category, Long id, IndustrialPlatformItemReq req) {
    String normalizedCategory = normalizeCategory(category);
    validate(normalizedCategory, req);
    IndustrialPlatformItemEntity entity = findExisting(normalizedCategory, id);
    apply(entity, req);
    mapper.updateById(entity);
    return IndustrialPlatformItemVO.from(entity);
  }

  @Transactional
  public void delete(String category, Long id) {
    findExisting(category, id);
    mapper.deleteById(id);
  }

  private void apply(IndustrialPlatformItemEntity entity, IndustrialPlatformItemReq req) {
    entity.setCompany(req.company());
    entity.setName(req.name());
    entity.setNums(req.nums());
    entity.setItemDate(req.date());
    entity.setState(req.state());
    entity.setSortOrder(100);
  }

  private void validate(String category, IndustrialPlatformItemReq req) {
    String numsMessage = NUMS_REQUIRED_MESSAGE.get(category);
    if (numsMessage != null && req.nums() == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, numsMessage);
    }
    if (Set.of("match", "apprentice", "comment").contains(category) && req.date() == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "日期不能为空");
    }
    if ("comment".equals(category) && !StringUtils.hasText(req.state())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "状态不能为空");
    }
  }

  private IndustrialPlatformItemEntity findExisting(String category, Long id) {
    String normalizedCategory = normalizeCategory(category);
    IndustrialPlatformItemEntity entity = mapper.selectById(id);
    if (entity == null || !normalizedCategory.equals(entity.getCategory())) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "平台建设数据不存在");
    }
    return entity;
  }

  private String normalizeCategory(String category) {
    if (!CATEGORIES.contains(category)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "平台建设分类不正确");
    }
    return category;
  }
}
