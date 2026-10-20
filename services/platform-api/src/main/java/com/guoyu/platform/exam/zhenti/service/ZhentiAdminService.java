package com.guoyu.platform.exam.zhenti.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.exam.entity.ExamWorkTypeEntity;
import com.guoyu.platform.exam.mapper.ExamWorkTypeMapper;
import com.guoyu.platform.exam.zhenti.dto.ZhentiImportLogReq;
import com.guoyu.platform.exam.zhenti.dto.ZhentiQueryReq;
import com.guoyu.platform.exam.zhenti.dto.ZhentiUpdateReq;
import com.guoyu.platform.exam.zhenti.entity.ExamZhentiEntity;
import com.guoyu.platform.exam.zhenti.entity.ExamZhentiImportLogEntity;
import com.guoyu.platform.exam.zhenti.mapper.ExamZhentiImportLogMapper;
import com.guoyu.platform.exam.zhenti.mapper.ExamZhentiMapper;
import com.guoyu.platform.exam.zhenti.vo.ZhentiAdminPageVO;
import com.guoyu.platform.exam.zhenti.vo.ZhentiImportFileResult;
import com.guoyu.platform.exam.zhenti.vo.ZhentiImportLogPageVO;
import com.guoyu.platform.exam.zhenti.vo.ZhentiImportLogVO;
import com.guoyu.platform.exam.zhenti.vo.ZhentiPreviewVO;
import com.guoyu.platform.exam.zhenti.vo.ZhentiUploadVO;
import com.guoyu.platform.exam.zhenti.vo.ZhentiVO;
import com.guoyu.platform.security.PlatformUserPrincipal;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.system.entity.UserEntity;
import com.guoyu.platform.system.mapper.UserMapper;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

/** 管理端 D1-D7（方案 §2.1 / 详规 §5）。 */
@Service
@Slf4j
public class ZhentiAdminService {

  private final ExamZhentiMapper mapper;
  private final ExamZhentiImportLogMapper importLogMapper;
  private final UserMapper userMapper;
  private final ZhentiIngestService ingestService;
  private final ZhentiViewerUrlBuilder viewerUrlBuilder;
  private final ObjectMapper objectMapper;
  private final ZhentiScopeResolver scopeResolver;
  private final ExamWorkTypeMapper examWorkTypeMapper;

  public ZhentiAdminService(
      ExamZhentiMapper mapper,
      ExamZhentiImportLogMapper importLogMapper,
      UserMapper userMapper,
      ZhentiIngestService ingestService,
      ZhentiViewerUrlBuilder viewerUrlBuilder,
      ObjectMapper objectMapper,
      ZhentiScopeResolver scopeResolver,
      ExamWorkTypeMapper examWorkTypeMapper) {
    this.mapper = mapper;
    this.importLogMapper = importLogMapper;
    this.userMapper = userMapper;
    this.ingestService = ingestService;
    this.viewerUrlBuilder = viewerUrlBuilder;
    this.objectMapper = objectMapper;
    this.scopeResolver = scopeResolver;
    this.examWorkTypeMapper = examWorkTypeMapper;
  }

  @Transactional(readOnly = true)
  public ZhentiAdminPageVO page(ZhentiQueryReq req) {
    Long tenantId = currentUser().getTenantId();
    List<ZhentiScope> scopes = scopeResolver.resolve(req.examId());
    LambdaQueryWrapper<ExamZhentiEntity> wrapper = new LambdaQueryWrapper<ExamZhentiEntity>()
        .eq(ExamZhentiEntity::getTenantId, tenantId)
        .eq(StringUtils.hasText(req.year()), ExamZhentiEntity::getYear, req.year())
        .eq(req.status() != null, ExamZhentiEntity::getStatus, req.status())
        .eq(req.metaPending() != null, ExamZhentiEntity::getMetaPending, req.metaPending())
        .like(StringUtils.hasText(req.keyword()), ExamZhentiEntity::getTitle, req.keyword())
        .orderByDesc(ExamZhentiEntity::getYear)
        .orderByAsc(ExamZhentiEntity::getSort)
        .orderByDesc(ExamZhentiEntity::getId);
    ZhentiScopeFilter.apply(wrapper, req.examId(), scopes);

    Page<ExamZhentiEntity> page = mapper.selectPage(
        new Page<>(req.page(), req.pageSize()), wrapper);
    List<ZhentiVO> records = toVOs(page.getRecords());
    return new ZhentiAdminPageVO(
        records, page.getTotal(), page.getCurrent(), page.getSize(), years(req.examId()));
  }

  @Transactional(readOnly = true)
  public List<String> years(Long examId) {
    Long tenantId = currentUser().getTenantId();
    List<ZhentiScope> scopes = scopeResolver.resolve(examId);
    QueryWrapper<ExamZhentiEntity> wrapper = new QueryWrapper<ExamZhentiEntity>()
        .select("DISTINCT year")
        .eq("tenant_id", tenantId)
        .isNotNull("year")
        .ne("year", "")
        .orderByDesc("year");
    ZhentiScopeFilter.apply(wrapper, examId, scopes);
    return mapper.selectObjs(wrapper).stream()
        .filter(java.util.Objects::nonNull)
        .map(String::valueOf)
        .filter(StringUtils::hasText)
        .toList();
  }

  @Transactional
  public ZhentiUploadVO upload(MultipartFile file, Long examId, String title, String year,
      Long scopeWorkTypeId, Integer scopeLevel) {
    PlatformUserPrincipal user = currentUser();
    if (file == null || file.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "文件为空，请重新选择");
    }
    // 双保险：先看入参大小（不占内存），再由 ingest 按实际字节数复核
    if (file.getSize() > ZhentiUploadValidator.MAX_PDF_BYTES) {
      throw new BusinessException(ErrorCode.PAYLOAD_TOO_LARGE, "单个 PDF 不能超过 50MB");
    }
    byte[] content;
    try {
      content = file.getBytes();
    } catch (IOException ex) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "读取上传文件失败，请重试");
    }
    ZhentiScope scope = scopeResolver.resolveForUpload(examId, scopeWorkTypeId, scopeLevel);
    ExamZhentiEntity entity = ingestService.ingest(
        user.getTenantId(), examId, user.getUserId(),
        file.getOriginalFilename(), content, title, year, scope);
    return new ZhentiUploadVO(String.valueOf(entity.getId()), entity.getTitle(),
        entity.getYear(), entity.getMetaPending());
  }

  @Transactional
  public ZhentiUploadVO update(ZhentiUpdateReq req) {
    ExamZhentiEntity entity = findExisting(req.id());
    if (req.status() != null && req.status() != 0 && req.status() != 1) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "状态只能为 0（下架）或 1（上架）");
    }
    if (req.title() != null) {
      if (!StringUtils.hasText(req.title())) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "标题不能为空");
      }
      entity.setTitle(req.title().trim());
    }
    if (req.year() != null) {
      String year = req.year().trim();
      entity.setYear(year.isEmpty() ? null : year);
    }
    if (req.sort() != null) {
      entity.setSort(req.sort());
    }
    if (req.status() != null) {
      entity.setStatus(req.status());
    }
    entity.setMetaPending(
        StringUtils.hasText(entity.getTitle()) && StringUtils.hasText(entity.getYear()) ? 0 : 1);
    mapper.updateById(entity);
    return new ZhentiUploadVO(String.valueOf(entity.getId()), entity.getTitle(),
        entity.getYear(), entity.getMetaPending());
  }

  /** 软删（deleteById 走 @TableLogic）；删除后 F1 立即 404、A1/D1 不再返回。 */
  @Transactional
  public void delete(Long id) {
    ExamZhentiEntity entity = findExisting(id);
    try {
      mapper.deleteById(entity.getId());
    } catch (DuplicateKeyException dup) {
      // uk_zhenti_scope_md5 含 deleted 列（保留“删除后可重传”旧语义）：同一文件重复
      // “删除→重传→再删”时，软删 UPDATE 置 deleted=1 会与历史已删行撞键；
      // 归档 scope_key（追加行 id 后缀）后重试，仍走 deleteById 真软删。
      mapper.archiveScopeKeyForDelete(entity.getId());
      mapper.deleteById(entity.getId());
    }
    log.info("[Zhenti] 软删 id={} examId={}", entity.getId(), entity.getExamId());
  }

  /** 管理端预览：与 App 同签名规则，但走管理端鉴权与权限码。 */
  @Transactional(readOnly = true)
  public ZhentiPreviewVO preview(Long id) {
    ExamZhentiEntity entity = findExisting(id);
    return viewerUrlBuilder.build(entity.getId(), entity.getTitle());
  }

  @Transactional(readOnly = true)
  public Object importLog(ZhentiImportLogReq req) {
    Long tenantId = currentUser().getTenantId();
    if (req.importId() != null) {
      ExamZhentiImportLogEntity entity = importLogMapper.selectOne(
          new LambdaQueryWrapper<ExamZhentiImportLogEntity>()
              .eq(ExamZhentiImportLogEntity::getTenantId, tenantId)
              .eq(ExamZhentiImportLogEntity::getId, req.importId())
              .last("LIMIT 1"));
      if (entity == null) {
        throw new BusinessException(ErrorCode.NOT_FOUND, "导入记录不存在");
      }
      return toLogVO(entity);
    }
    if (req.examId() == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "importId 与 examId 至少传一个");
    }
    long page = req.page() == null || req.page() < 1 ? 1 : req.page();
    long size = req.pageSize() == null || req.pageSize() < 1 ? 20 : Math.min(req.pageSize(), 100);
    Page<ExamZhentiImportLogEntity> result = importLogMapper.selectPage(
        new Page<>(page, size),
        new LambdaQueryWrapper<ExamZhentiImportLogEntity>()
            .eq(ExamZhentiImportLogEntity::getTenantId, tenantId)
            .eq(ExamZhentiImportLogEntity::getExamId, req.examId())
            .orderByDesc(ExamZhentiImportLogEntity::getId));
    return new ZhentiImportLogPageVO(
        result.getRecords().stream().map(this::toLogVO).toList(),
        result.getTotal(), result.getCurrent(), result.getSize());
  }

  public ExamZhentiEntity findExisting(Long id) {
    Long tenantId = currentUser().getTenantId();
    ExamZhentiEntity entity = mapper.selectOne(new LambdaQueryWrapper<ExamZhentiEntity>()
        .eq(ExamZhentiEntity::getTenantId, tenantId)
        .eq(ExamZhentiEntity::getId, id)
        .last("LIMIT 1"));
    if (entity == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "真题不存在或已删除");
    }
    return entity;
  }

  private List<ZhentiVO> toVOs(List<ExamZhentiEntity> entities) {
    if (entities.isEmpty()) {
      return List.of();
    }
    Set<Long> userIds = entities.stream()
        .map(ExamZhentiEntity::getCreatedBy)
        .filter(java.util.Objects::nonNull)
        .collect(Collectors.toSet());
    Map<Long, String> names = userIds.isEmpty()
        ? Collections.emptyMap()
        : userMapper.selectBatchIds(userIds).stream()
            .collect(Collectors.toMap(UserEntity::getId, this::displayName, (a, b) -> a));
    Set<Long> workTypeIds = entities.stream()
        .map(ExamZhentiEntity::getWorkTypeId)
        .filter(java.util.Objects::nonNull)
        .collect(Collectors.toSet());
    Map<Long, String> workTypeTitles = workTypeIds.isEmpty()
        ? Collections.emptyMap()
        : examWorkTypeMapper.selectBatchIds(workTypeIds).stream()
            .collect(Collectors.toMap(w -> w.getId().longValue(), ExamWorkTypeEntity::getTitle,
                (a, b) -> a));
    return entities.stream()
        .map(e -> new ZhentiVO(
            String.valueOf(e.getId()),
            String.valueOf(e.getExamId()),
            e.getTitle(),
            e.getYear(),
            e.getFileSize(),
            e.getSort(),
            e.getStatus(),
            e.getMetaPending(),
            e.getCreatedBy() == null ? null : names.getOrDefault(e.getCreatedBy(), "—"),
            e.getCreatedBy() == null ? null : String.valueOf(e.getCreatedBy()),
            e.getCreatedAt(),
            scopeLabel(e, workTypeTitles)))
        .toList();
  }

  private String scopeLabel(ExamZhentiEntity entity, Map<Long, String> workTypeTitles) {
    if (entity.getWorkTypeId() == null || entity.getLevelCode() == null) {
      return "本计划私有";
    }
    String title = workTypeTitles.getOrDefault(entity.getWorkTypeId(), "未知工种");
    return ZhentiScopeKey.label(
        new ZhentiScope(entity.getWorkTypeId(), entity.getLevelCode()), title);
  }

  private String displayName(UserEntity user) {
    return StringUtils.hasText(user.getDisplayName()) ? user.getDisplayName() : user.getUsername();
  }

  private ZhentiImportLogVO toLogVO(ExamZhentiImportLogEntity entity) {
    return new ZhentiImportLogVO(String.valueOf(entity.getId()), entity.getStatus(),
        entity.getTotal(), entity.getFinished(), parseResults(entity.getResultsJson()));
  }

  private List<ZhentiImportFileResult> parseResults(String json) {
    if (!StringUtils.hasText(json)) {
      return List.of();
    }
    try {
      return objectMapper.readValue(json, new TypeReference<List<ZhentiImportFileResult>>() {
      });
    } catch (IOException ex) {
      log.warn("[Zhenti] 导入结果 JSON 解析失败: {}", ex.getMessage());
      return List.of();
    }
  }

  private PlatformUserPrincipal currentUser() {
    try {
      return SecurityContext.currentUser();
    } catch (IllegalStateException ex) {
      throw new BusinessException(ErrorCode.UNAUTHORIZED, "请先登录");
    }
  }
}
