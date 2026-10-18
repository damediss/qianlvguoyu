package com.guoyu.platform.exam.zhenti.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.exam.zhenti.entity.ExamZhentiEntity;
import com.guoyu.platform.exam.zhenti.mapper.ExamZhentiMapper;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 单文件入库（D2 上传与 D3 批量导入共用）。
 *
 * <p>校验 → 去重 → 解析元数据 → 落存储 → 插表；插表失败时回删存储对象，避免孤儿文件。
 * 批量导入侧捕获 BusinessException 转成逐文件失败原因，不中断整包。
 */
@Service
@Slf4j
public class ZhentiIngestService {

  private final ExamZhentiMapper mapper;
  private final ZhentiStorageService storageService;
  private final ZhentiUploadValidator validator;
  private final ZhentiFileNameParser fileNameParser;

  public ZhentiIngestService(
      ExamZhentiMapper mapper,
      ZhentiStorageService storageService,
      ZhentiUploadValidator validator,
      ZhentiFileNameParser fileNameParser) {
    this.mapper = mapper;
    this.storageService = storageService;
    this.validator = validator;
    this.fileNameParser = fileNameParser;
  }

  /**
   * 写入一份真题。
   *
   * @param tenantId      租户（异步链路由调用方显式传入）
   * @param examId        考试项目 id
   * @param operatorId    操作人（异步链路显式传入，HTTP 链路可为 null 由 MetaObjectHandler 填充）
   * @param fileName      原始文件名（用于解析与错误提示）
   * @param content       文件字节
   * @param titleOverride 管理员手填标题，空则按文件名解析
   * @param yearOverride  管理员手填年份，空则按文件名解析
   */
  @Transactional
  public ExamZhentiEntity ingest(
      Long tenantId,
      Long examId,
      Long operatorId,
      String fileName,
      byte[] content,
      String titleOverride,
      String yearOverride) {
    validator.validatePdf(fileName, content);
    String md5 = validator.md5(content);
    Optional<ExamZhentiEntity> existing = findActiveByMd5(examId, md5);
    if (existing.isPresent()) {
      throw new BusinessException(ErrorCode.CONFLICT,
          "已存在相同文件：" + existing.get().getTitle());
    }

    ZhentiFileNameParser.ParsedFileName parsed = fileNameParser.parse(fileName);
    String title = resolveTitle(titleOverride, parsed.title());
    String year = resolveYear(yearOverride, parsed.year());
    int metaPending = resolveMetaPending(titleOverride, yearOverride, year, parsed.levelParsed());

    String fileKey = storageService.putPdf(examId, md5, content);
    ExamZhentiEntity entity = new ExamZhentiEntity();
    entity.setTenantId(tenantId);
    entity.setExamId(examId);
    entity.setTitle(title);
    entity.setYear(year);
    entity.setFileKey(fileKey);
    entity.setFileSize((long) content.length);
    entity.setFileMd5(md5);
    entity.setSort(0);
    entity.setStatus(1);
    entity.setMetaPending(metaPending);
    if (operatorId != null) {
      entity.setCreatedBy(operatorId);
      entity.setUpdatedBy(operatorId);
    }
    try {
      mapper.insert(entity);
    } catch (RuntimeException ex) {
      storageService.delete(fileKey);
      throw ex;
    }
    log.info("[Zhenti] 入库成功 id={} examId={} size={} pending={}",
        entity.getId(), examId, content.length, metaPending);
    return entity;
  }

  private Optional<ExamZhentiEntity> findActiveByMd5(Long examId, String md5) {
    return Optional.ofNullable(mapper.selectOne(new LambdaQueryWrapper<ExamZhentiEntity>()
        .eq(ExamZhentiEntity::getExamId, examId)
        .eq(ExamZhentiEntity::getFileMd5, md5)
        .last("LIMIT 1")));
  }

  private static String resolveTitle(String override, String parsedTitle) {
    String title = StringUtils.hasText(override) ? override.trim() : parsedTitle;
    if (!StringUtils.hasText(title)) {
      title = "未命名真题";
    }
    if (title.length() > 200) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "标题最多 200 字");
    }
    return title;
  }

  private static String resolveYear(String override, String parsedYear) {
    String year = StringUtils.hasText(override) ? override.trim() : parsedYear;
    if (year == null) {
      return null;
    }
    if (year.length() > 10) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "年份最多 10 字");
    }
    return year.isEmpty() ? null : year;
  }

  /**
   * 待完善判定：管理员同时填了标题与年份 → 完整；否则缺年份或缺工种等级即入队列。
   */
  private static int resolveMetaPending(
      String titleOverride, String yearOverride, String year, boolean levelParsed) {
    if (StringUtils.hasText(titleOverride) && StringUtils.hasText(yearOverride)) {
      return 0;
    }
    if (!StringUtils.hasText(year)) {
      return 1;
    }
    return levelParsed || StringUtils.hasText(titleOverride) ? 0 : 1;
  }
}
