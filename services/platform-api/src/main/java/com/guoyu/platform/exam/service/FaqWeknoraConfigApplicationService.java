package com.guoyu.platform.exam.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guoyu.platform.exam.dto.FaqWeKnoraConfigSaveReq;
import com.guoyu.platform.exam.entity.ExamFaqWeknoraConfigEntity;
import com.guoyu.platform.exam.mapper.ExamFaqWeknoraConfigMapper;
import com.guoyu.platform.exam.vo.FaqWeknoraConfigVO;
import com.guoyu.platform.security.SecurityContext;
import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** WeKnora 集成配置 — 单租户一条记录，走 upsert 语义 */
@Service
@Slf4j
public class FaqWeknoraConfigApplicationService {

  private final ExamFaqWeknoraConfigMapper mapper;

  public FaqWeknoraConfigApplicationService(ExamFaqWeknoraConfigMapper mapper) {
    this.mapper = mapper;
  }

  public Long tenantId() {
    return SecurityContext.currentUser().getTenantId();
  }

  @Cacheable(value = "faqWeknoraConfig", key = "#root.target.tenantId()}", unless = "#result == null")
  @Transactional(readOnly = true)
  public FaqWeknoraConfigVO getConfig() {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    ExamFaqWeknoraConfigEntity entity = mapper.selectOne(new LambdaQueryWrapper<ExamFaqWeknoraConfigEntity>()
        .eq(ExamFaqWeknoraConfigEntity::getTenantId, tenantId));
    if (entity == null) {
      // 返回空配置（未保存过）
      return null;
    }
    return FaqWeknoraConfigVO.from(entity);
  }

  @CacheEvict(value = "faqWeknoraConfig", allEntries = true)
  @Transactional
  public FaqWeknoraConfigVO save(FaqWeKnoraConfigSaveReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    Long userId = SecurityContext.currentUser().getUserId();

    ExamFaqWeknoraConfigEntity entity = mapper.selectOne(new LambdaQueryWrapper<ExamFaqWeknoraConfigEntity>()
        .eq(ExamFaqWeknoraConfigEntity::getTenantId, tenantId));

    LocalDateTime now = LocalDateTime.now();

    if (entity == null) {
      entity = new ExamFaqWeknoraConfigEntity();
      entity.setTenantId(tenantId);
      entity.setWebhookEnabled(0);
      entity.setAutoExport(0);
      entity.setDeleted(0);
      entity.setCreatedBy(userId);
      entity.setCreatedAt(now);
    }

    if (req.webhookSecret() != null) entity.setWebhookSecret(req.webhookSecret());
    if (req.enabledEvents() != null) entity.setEnabledEvents(req.enabledEvents());
    if (req.knowledgeBaseId() != null) entity.setKnowledgeBaseId(req.knowledgeBaseId());
    if (req.faqSetId() != null) entity.setFaqSetId(req.faqSetId());
    if (req.webhookEnabled() != null) entity.setWebhookEnabled(req.webhookEnabled());
    if (req.autoExport() != null) entity.setAutoExport(req.autoExport());
    entity.setUpdatedBy(userId);
    entity.setUpdatedAt(now);

    if (entity.getId() == null) {
      mapper.insert(entity);
      log.info("WeKnora 配置已创建：tenantId={}", tenantId);
    } else {
      mapper.updateById(entity);
      log.info("WeKnora 配置已更新：tenantId={}", tenantId);
    }

    return FaqWeknoraConfigVO.from(entity);
  }

  /** 获取 WeKnora 配置（供异步/下游调用，通过 tenantId 参数） */
  @Transactional(readOnly = true)
  public ExamFaqWeknoraConfigEntity getConfigByTenantId(Long tenantId) {
    return mapper.selectOne(new LambdaQueryWrapper<ExamFaqWeknoraConfigEntity>()
        .eq(ExamFaqWeknoraConfigEntity::getTenantId, tenantId));
  }
}
