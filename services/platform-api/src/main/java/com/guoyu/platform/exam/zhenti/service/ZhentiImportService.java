package com.guoyu.platform.exam.zhenti.service;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.exam.zhenti.entity.ExamZhentiImportLogEntity;
import com.guoyu.platform.exam.zhenti.mapper.ExamZhentiImportLogMapper;
import com.guoyu.platform.exam.zhenti.vo.ZhentiImportFileResult;
import com.guoyu.platform.exam.zhenti.vo.ZhentiImportResultVO;
import com.guoyu.platform.security.PlatformUserPrincipal;
import com.guoyu.platform.security.SecurityContext;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * D3 批量导入（方案 §2.9）。
 *
 * <p>≤50 个文件同步返回结果；&gt;50 落导入记录并转异步，D6 轮询。
 * zip 与单 PDF 的限制在 {@link ZhentiUploadValidator} 与 {@link ZhentiZipProcessor} 内强制。
 */
@Service
@Slf4j
public class ZhentiImportService {

  private final ZhentiZipProcessor zipProcessor;
  private final ZhentiStorageService storageService;
  private final ZhentiImportWorker importWorker;
  private final ExamZhentiImportLogMapper importLogMapper;
  private final ZhentiUploadValidator validator;
  private final ZhentiScopeResolver scopeResolver;
  private final int asyncThreshold;

  public ZhentiImportService(
      ZhentiZipProcessor zipProcessor,
      ZhentiStorageService storageService,
      ZhentiImportWorker importWorker,
      ExamZhentiImportLogMapper importLogMapper,
      ZhentiUploadValidator validator,
      ZhentiScopeResolver scopeResolver,
      @Value("${platform.zhenti.async-threshold:50}") int asyncThreshold) {
    this.zipProcessor = zipProcessor;
    this.storageService = storageService;
    this.importWorker = importWorker;
    this.importLogMapper = importLogMapper;
    this.validator = validator;
    this.scopeResolver = scopeResolver;
    this.asyncThreshold = asyncThreshold > 0 ? asyncThreshold : 50;
  }

  public ZhentiImportResultVO importZip(
      MultipartFile file, Long examId, Long scopeWorkTypeId, Integer scopeLevel) {
    PlatformUserPrincipal user = currentUser();
    // 共享组在 HTTP 线程解析（resolver 依赖租户上下文），Worker 只收显式参数（异步红线）
    ZhentiScope scope = scopeResolver.resolveForUpload(examId, scopeWorkTypeId, scopeLevel);
    String fileName = file.getOriginalFilename();
    if (!validator.isZipName(fileName)) {
      throw new BusinessException(ErrorCode.UNSUPPORTED_MEDIA_TYPE, "仅支持 zip 压缩包");
    }
    if (file.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "压缩包为空，请重新选择");
    }
    if (file.getSize() > ZhentiUploadValidator.MAX_ZIP_BYTES) {
      throw new BusinessException(ErrorCode.PAYLOAD_TOO_LARGE, "zip 压缩包不能超过 200MB");
    }

    Path temp = null;
    try {
      temp = Files.createTempFile("zhenti-import-", ".zip");
      file.transferTo(temp);
      ZhentiZipProcessor.ZipInspection inspection = zipProcessor.inspect(temp);
      if (inspection.total() > asyncThreshold) {
        return submitAsync(user, examId, scope, temp, inspection);
      }
      List<ZhentiImportFileResult> results = zipProcessor.process(
          temp, user.getTenantId(), examId, user.getUserId(), scope, null);
      long ok = results.stream().filter(ZhentiImportFileResult::ok).count();
      log.info("[ZhentiImport] 同步导入完成 examId={} total={} ok={} operator={}",
          examId, results.size(), ok, user.getUserId());
      return ZhentiImportResultVO.sync(results);
    } catch (IOException ex) {
      log.warn("[ZhentiImport] 临时文件处理失败: {}", ex.toString());
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "导入失败，请稍后重试");
    } finally {
      if (temp != null) {
        try {
          Files.deleteIfExists(temp);
        } catch (IOException ignored) {
          // 临时文件清理失败不影响业务
        }
      }
    }
  }

  private ZhentiImportResultVO submitAsync(
      PlatformUserPrincipal user, Long examId, ZhentiScope scope, Path temp,
      ZhentiZipProcessor.ZipInspection inspection) {
    Long importId = IdWorker.getId();
    String zipKey = storageService.putImportZip(user.getTenantId(), importId, temp);

    ExamZhentiImportLogEntity logEntity = new ExamZhentiImportLogEntity();
    logEntity.setId(importId);
    logEntity.setTenantId(user.getTenantId());
    logEntity.setExamId(examId);
    logEntity.setOperatorId(user.getUserId());
    logEntity.setTotal(inspection.total());
    logEntity.setFinished(0);
    logEntity.setStatus("running");
    logEntity.setCreatedBy(user.getUserId());
    logEntity.setUpdatedBy(user.getUserId());
    importLogMapper.insert(logEntity);

    try {
      importWorker.run(user.getTenantId(), importId, examId, user.getUserId(), zipKey,
          inspection.total(), scope);
    } catch (RejectedExecutionException ex) {
      log.warn("[ZhentiImport] 导入线程池已满 importId={}", importId);
      importLogMapper.update(null,
          new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<ExamZhentiImportLogEntity>()
              .eq(ExamZhentiImportLogEntity::getId, importId)
              .set(ExamZhentiImportLogEntity::getStatus, "failed"));
      storageService.delete(zipKey);
      throw ex;
    }
    log.info("[ZhentiImport] 已转异步 importId={} total={}", importId, inspection.total());
    return ZhentiImportResultVO.async(importId);
  }

  private PlatformUserPrincipal currentUser() {
    try {
      return SecurityContext.currentUser();
    } catch (IllegalStateException ex) {
      throw new BusinessException(ErrorCode.UNAUTHORIZED, "请先登录");
    }
  }
}
