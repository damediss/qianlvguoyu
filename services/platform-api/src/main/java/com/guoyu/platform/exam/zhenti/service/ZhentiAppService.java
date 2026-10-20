package com.guoyu.platform.exam.zhenti.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.exam.zhenti.entity.ExamZhentiEntity;
import com.guoyu.platform.exam.zhenti.mapper.ExamZhentiMapper;
import com.guoyu.platform.exam.zhenti.vo.ZhentiAppListVO;
import com.guoyu.platform.exam.zhenti.vo.ZhentiItemVO;
import com.guoyu.platform.exam.zhenti.vo.ZhentiPreviewVO;
import com.guoyu.platform.security.PlatformUserPrincipal;
import com.guoyu.platform.security.SecurityContext;
import java.time.LocalDateTime;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * App 端 A1/A2：学员仅看已上架真题，预览必须经过签名（防盗链）。
 *
 * <p>埋点：A2 调用写 INFO 日志（学员 id、真题 id、时间），支撑“哪份真题被看得最多”统计。
 */
@Service
@Slf4j
public class ZhentiAppService {

  private final ExamZhentiMapper mapper;
  private final ZhentiViewerUrlBuilder viewerUrlBuilder;
  private final ZhentiScopeResolver scopeResolver;

  public ZhentiAppService(ExamZhentiMapper mapper, ZhentiViewerUrlBuilder viewerUrlBuilder,
      ZhentiScopeResolver scopeResolver) {
    this.mapper = mapper;
    this.viewerUrlBuilder = viewerUrlBuilder;
    this.scopeResolver = scopeResolver;
  }

  @Transactional(readOnly = true)
  public ZhentiAppListVO list(Long examId) {
    Long tenantId = currentUser().getTenantId();
    List<ZhentiScope> scopes = scopeResolver.resolve(examId);
    LambdaQueryWrapper<ExamZhentiEntity> wrapper = new LambdaQueryWrapper<ExamZhentiEntity>()
        .eq(ExamZhentiEntity::getTenantId, tenantId)
        .eq(ExamZhentiEntity::getStatus, 1)
        .orderByDesc(ExamZhentiEntity::getYear)
        .orderByAsc(ExamZhentiEntity::getSort)
        .orderByDesc(ExamZhentiEntity::getId);
    ZhentiScopeFilter.apply(wrapper, examId, scopes);
    List<ExamZhentiEntity> entities = mapper.selectList(wrapper);
    List<ZhentiItemVO> records = entities.stream()
        .map(e -> new ZhentiItemVO(
            String.valueOf(e.getId()), e.getTitle(), e.getYear(), e.getFileSize()))
        .toList();
    return new ZhentiAppListVO(records, records.size(), 1, records.size());
  }

  @Transactional(readOnly = true)
  public ZhentiPreviewVO preview(Long id) {
    PlatformUserPrincipal user = currentUser();
    ExamZhentiEntity entity = mapper.selectOne(new LambdaQueryWrapper<ExamZhentiEntity>()
        .eq(ExamZhentiEntity::getTenantId, user.getTenantId())
        .eq(ExamZhentiEntity::getId, id)
        .eq(ExamZhentiEntity::getStatus, 1)
        .last("LIMIT 1"));
    if (entity == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "真题不存在或已下架");
    }
    log.info("[Zhenti] 学员预览 userId={} zhentiId={} at={}",
        user.getUserId(), entity.getId(), LocalDateTime.now());
    return viewerUrlBuilder.build(entity.getId(), entity.getTitle());
  }

  private PlatformUserPrincipal currentUser() {
    try {
      return SecurityContext.currentUser();
    } catch (IllegalStateException ex) {
      throw new BusinessException(ErrorCode.UNAUTHORIZED, "请先登录");
    }
  }
}
