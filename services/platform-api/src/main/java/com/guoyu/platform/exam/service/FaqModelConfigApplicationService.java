package com.guoyu.platform.exam.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.exam.dto.FaqModelConfigCreateReq;
import com.guoyu.platform.exam.dto.FaqModelConfigQueryReq;
import com.guoyu.platform.exam.dto.FaqModelConfigUpdateReq;
import com.guoyu.platform.exam.entity.ExamFaqModelConfigEntity;
import com.guoyu.platform.exam.mapper.ExamFaqModelConfigMapper;
import com.guoyu.platform.exam.vo.FaqModelConfigVO;
import com.guoyu.platform.security.SecurityContext;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** FAQ 转换 LLM 模型配置 CRUD */
@Service
@Slf4j
public class FaqModelConfigApplicationService {

  private final ExamFaqModelConfigMapper mapper;

  public FaqModelConfigApplicationService(ExamFaqModelConfigMapper mapper) {
    this.mapper = mapper;
  }

  /** Expose tenantId for @Cacheable SPEL key */
  public Long tenantId() {
    return SecurityContext.currentUser().getTenantId();
  }

  @Transactional(readOnly = true)
  public PageResponse<FaqModelConfigVO> page(FaqModelConfigQueryReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    LambdaQueryWrapper<ExamFaqModelConfigEntity> wrapper = new LambdaQueryWrapper<ExamFaqModelConfigEntity>()
        .eq(ExamFaqModelConfigEntity::getTenantId, tenantId)
        .and(StringUtils.hasText(req.keyword()), w -> w
            .like(ExamFaqModelConfigEntity::getModelCode, req.keyword())
            .or()
            .like(ExamFaqModelConfigEntity::getModelName, req.keyword()))
        .eq(StringUtils.hasText(req.status()), ExamFaqModelConfigEntity::getStatus, req.status())
        .orderByDesc(ExamFaqModelConfigEntity::getId);

    long page = req.page() != null ? req.page() : 1;
    long pageSize = req.pageSize() != null ? req.pageSize() : 10;
    Page<ExamFaqModelConfigEntity> resultPage = mapper.selectPage(new Page<>(page, pageSize), wrapper);
    return new PageResponse<>(
        resultPage.getRecords().stream().map(FaqModelConfigVO::from).toList(),
        resultPage.getCurrent(),
        resultPage.getSize(),
        resultPage.getTotal()
    );
  }

  @Transactional(readOnly = true)
  public FaqModelConfigVO getById(Long id) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    ExamFaqModelConfigEntity entity = findExisting(tenantId, id);
    return FaqModelConfigVO.from(entity);
  }

  /** 查询所有 enabled 的模型（供下拉选择使用） */
  @Cacheable(value = "faqModelConfig", key = "#root.target.tenantId() + ':enabled'", unless = "#result == null")
  @Transactional(readOnly = true)
  public List<FaqModelConfigVO> listEnabled() {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    List<ExamFaqModelConfigEntity> list = mapper.selectList(new LambdaQueryWrapper<ExamFaqModelConfigEntity>()
        .eq(ExamFaqModelConfigEntity::getTenantId, tenantId)
        .eq(ExamFaqModelConfigEntity::getStatus, "enabled"));
    // 必须返回 mutable list（ArrayList），否则 Redis 缓存反序列化会报 SerializationException
    return list.stream().map(FaqModelConfigVO::from).collect(java.util.stream.Collectors.toCollection(java.util.ArrayList::new));
  }

  @CacheEvict(value = "faqModelConfig", allEntries = true)
  @Transactional
  public FaqModelConfigVO create(FaqModelConfigCreateReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    Long userId = SecurityContext.currentUser().getUserId();

    // 唯一性检查
    boolean exists = mapper.exists(new LambdaQueryWrapper<ExamFaqModelConfigEntity>()
        .eq(ExamFaqModelConfigEntity::getTenantId, tenantId)
        .eq(ExamFaqModelConfigEntity::getModelCode, req.modelCode()));
    if (exists) {
      throw new BusinessException(ErrorCode.CONFLICT, "模型编码已存在：" + req.modelCode());
    }

    LocalDateTime now = LocalDateTime.now();
    ExamFaqModelConfigEntity entity = new ExamFaqModelConfigEntity();
    entity.setTenantId(tenantId);
    entity.setModelCode(req.modelCode());
    entity.setModelName(req.modelName());
    entity.setBaseUrl(req.baseUrl());
    entity.setApiKey(req.apiKey());
    entity.setApiFormat(req.apiFormat());
    entity.setModelId(req.modelId());
    entity.setTemperature(req.temperature() != null ? req.temperature() : new BigDecimal("0.70"));
    entity.setMaxTokens(req.maxTokens() != null ? req.maxTokens() : 2000);
    entity.setExtraConfig(req.extraConfig());
    entity.setStatus(req.status() != null && req.status() == 1 ? "enabled" : "disabled");
    entity.setCreatedBy(userId);
    entity.setUpdatedBy(userId);
    entity.setCreatedAt(now);
    entity.setUpdatedAt(now);
    entity.setDeleted(0);

    mapper.insert(entity);
    log.info("FAQ 模型配置已创建：tenantId={}, modelCode={}", tenantId, req.modelCode());
    return FaqModelConfigVO.from(entity);
  }

  @CacheEvict(value = "faqModelConfig", allEntries = true)
  @Transactional
  public FaqModelConfigVO update(Long id, FaqModelConfigUpdateReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    Long userId = SecurityContext.currentUser().getUserId();
    ExamFaqModelConfigEntity entity = findExisting(tenantId, id);

    if (req.modelName() != null) entity.setModelName(req.modelName());
    if (req.baseUrl() != null) entity.setBaseUrl(req.baseUrl());
    if (req.apiKey() != null) entity.setApiKey(req.apiKey());
    if (req.apiFormat() != null) entity.setApiFormat(req.apiFormat());
    if (req.modelId() != null) entity.setModelId(req.modelId());
    if (req.temperature() != null) entity.setTemperature(req.temperature());
    if (req.maxTokens() != null) entity.setMaxTokens(req.maxTokens());
    if (req.extraConfig() != null) entity.setExtraConfig(req.extraConfig());
    if (req.status() != null) entity.setStatus(req.status() == 1 ? "enabled" : "disabled");
    entity.setUpdatedBy(userId);
    entity.setUpdatedAt(LocalDateTime.now());

    mapper.updateById(entity);
    log.info("FAQ 模型配置已更新：tenantId={}, id={}", tenantId, id);
    return FaqModelConfigVO.from(entity);
  }

  @CacheEvict(value = "faqModelConfig", allEntries = true)
  @Transactional
  public void delete(Long id) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    findExisting(tenantId, id);
    mapper.deleteById(id);
    log.info("FAQ 模型配置已删除：tenantId={}, id={}", tenantId, id);
  }

  /** 根据 model_code 查找模型配置（供异步调用，通过 tenantId 参数） */
  @Transactional(readOnly = true)
  public ExamFaqModelConfigEntity findByModelCode(Long tenantId, String modelCode) {
    return mapper.selectOne(new LambdaQueryWrapper<ExamFaqModelConfigEntity>()
        .eq(ExamFaqModelConfigEntity::getTenantId, tenantId)
        .eq(ExamFaqModelConfigEntity::getModelCode, modelCode)
        .eq(ExamFaqModelConfigEntity::getStatus, "enabled"));
  }

  private ExamFaqModelConfigEntity findExisting(Long tenantId, Long id) {
    ExamFaqModelConfigEntity entity = mapper.selectOne(new LambdaQueryWrapper<ExamFaqModelConfigEntity>()
        .eq(ExamFaqModelConfigEntity::getTenantId, tenantId)
        .eq(ExamFaqModelConfigEntity::getId, id));
    if (entity == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "模型配置不存在");
    }
    return entity;
  }
}
