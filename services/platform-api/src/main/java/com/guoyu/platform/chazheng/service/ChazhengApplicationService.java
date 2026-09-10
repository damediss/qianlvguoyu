package com.guoyu.platform.chazheng.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.chazheng.config.CertCheckBatchConfig;
import com.guoyu.platform.chazheng.dto.CertCheckBatchProgress;
import com.guoyu.platform.chazheng.dto.CertCheckBatchReport;
import com.guoyu.platform.chazheng.dto.CertCheckBatchSubmitResp;
import com.guoyu.platform.chazheng.dto.ChazhengUserQueryReq;
import com.guoyu.platform.chazheng.entity.CertCheckBatchEntity;
import com.guoyu.platform.chazheng.entity.CertCheckBatchItemEntity;
import com.guoyu.platform.chazheng.entity.ChazhengCertEntity;
import com.guoyu.platform.chazheng.entity.ChazhengUserEntity;
import com.guoyu.platform.chazheng.mapper.CertCheckBatchItemMapper;
import com.guoyu.platform.chazheng.mapper.CertCheckBatchMapper;
import com.guoyu.platform.chazheng.mapper.ChazhengCertMapper;
import com.guoyu.platform.chazheng.mapper.ChazhengUserMapper;
import com.guoyu.platform.chazheng.support.FixedRateLimiter;
import com.guoyu.platform.chazheng.vo.ChazhengCertVO;
import com.guoyu.platform.chazheng.vo.ChazhengUserVO;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.security.SecurityContext;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

/**
 * 查证应用服务。
 *
 * <p>重构说明(对齐 PHP MainTask::queryCertByTemplate):
 * <ul>
 *   <li>从 sys_dict 读取接口地址 + 请求模板(运行时覆盖 idcode/username)</li>
 *   <li>多模板轮询(datas[0] + datas[1])</li>
 *   <li>正确的响应路径(result.data[])和字段映射(serialid/bzz002/metadata 拼合)</li>
 *   <li>异常分流: curl/JSON 异常 → 不标记 anyNormal(留待重试); 业务异常 → 不标记 anyNormal</li>
 *   <li>HTTP 调用在事务外(不占 DB 连接), DB 写入通过独立 Bean(ChazhengPersistService)短事务</li>
 *   <li>批量异步查证: 导入后自动触发, 多线程并行执行, 支持进度查询</li>
 * </ul>
 */
@Service
@Slf4j
public class ChazhengApplicationService {

  private final ChazhengUserMapper userMapper;
  private final ChazhengCertMapper certMapper;
  private final ChazhengPersistService chazhengPersistService;
  private final CertCheckBatchMapper batchMapper;
  private final CertCheckBatchItemMapper batchItemMapper;
  private final ChazhengCertQueryService chazhengCertQueryService;

  @Value("${chazheng.batch.max-retry:2}")
  private int maxRetry;

  /** 线程池引用(由 Controller 创建后注入) */
  private ThreadPoolTaskExecutor executor;

  public ChazhengApplicationService(ChazhengUserMapper userMapper,
      ChazhengCertMapper certMapper,
      ChazhengPersistService chazhengPersistService,
      CertCheckBatchMapper batchMapper,
      CertCheckBatchItemMapper batchItemMapper,
      ChazhengCertQueryService chazhengCertQueryService) {
    this.userMapper = userMapper;
    this.certMapper = certMapper;
    this.chazhengPersistService = chazhengPersistService;
    this.batchMapper = batchMapper;
    this.batchItemMapper = batchItemMapper;
    this.chazhengCertQueryService = chazhengCertQueryService;
  }

  /** 由 Controller 在 @PostConstruct 中设置线程池时调用 */
  public void setExecutor(ThreadPoolTaskExecutor executor) {
    this.executor = executor;
  }

  // ════════════════════════════════════════════════════════════════════════
  //  分页查询 / 导入 / 导出(原有逻辑)
  // ════════════════════════════════════════════════════════════════════════

  @Transactional(readOnly = true)
  public PageResponse<ChazhengUserVO> page(ChazhengUserQueryReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    LambdaQueryWrapper<ChazhengUserEntity> wrapper = new LambdaQueryWrapper<ChazhengUserEntity>()
        .eq(ChazhengUserEntity::getTenantId, tenantId)
        .orderByDesc(ChazhengUserEntity::getSortNumber)
        .orderByDesc(ChazhengUserEntity::getId);

    if (StringUtils.hasText(req.getKeyword())) {
      wrapper.and(w -> w
          .like(ChazhengUserEntity::getUserName, req.getKeyword())
          .or()
          .like(ChazhengUserEntity::getIdCard, req.getKeyword()));
    }
    if (req.getEnterpriseId() != null) {
      wrapper.eq(ChazhengUserEntity::getEnterpriseId, req.getEnterpriseId());
    }
    if (StringUtils.hasText(req.getYmd())) {
      wrapper.eq(ChazhengUserEntity::getYmd, req.getYmd());
    }

    var page = userMapper.selectPage(new Page<>(req.getPage(), req.getPageSize()), wrapper);
    List<ChazhengUserVO> vos = page.getRecords().stream().map(entity -> {
      ChazhengUserVO vo = ChazhengUserVO.from(entity);
      List<ChazhengCertEntity> certs = certMapper.selectList(
          new LambdaQueryWrapper<ChazhengCertEntity>()
              .eq(ChazhengCertEntity::getUserId, entity.getId()));
      vo.setCerts(certs.stream().map(ChazhengCertVO::from).toList());
      return vo;
    }).toList();

    return new PageResponse<>(vos, page.getCurrent(), page.getSize(), page.getTotal());
  }

  /**
   * 获取最新导入的 ymd 批次号(导入后自动查证用)。
   */
  public String getLatestYmd() {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    ChazhengUserEntity latest = userMapper.selectOne(new LambdaQueryWrapper<ChazhengUserEntity>()
        .eq(ChazhengUserEntity::getTenantId, tenantId)
        .orderByDesc(ChazhengUserEntity::getCreateTime)
        .last("LIMIT 1"));
    return latest != null ? latest.getYmd() : null;
  }

  @Transactional(readOnly = true)
  public List<String> getYmdList() {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    LambdaQueryWrapper<ChazhengUserEntity> wrapper = new LambdaQueryWrapper<ChazhengUserEntity>()
        .eq(ChazhengUserEntity::getTenantId, tenantId)
        .select(ChazhengUserEntity::getYmd)
        .groupBy(ChazhengUserEntity::getYmd)
        .orderByDesc(ChazhengUserEntity::getYmd);
    List<ChazhengUserEntity> list = userMapper.selectList(wrapper);
    return list.stream()
        .map(ChazhengUserEntity::getYmd)
        .filter(StringUtils::hasText)
        .toList();
  }

  @Transactional
  public int importExcel(MultipartFile file) {
    if (file.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "上传文件为空");
    }

    Long tenantId = SecurityContext.currentUser().getTenantId();
    String ymd = String.valueOf(System.currentTimeMillis());
    int imported = 0;

    try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
      Sheet sheet = workbook.getSheetAt(0);
      // 按表头定位列：认定名单是宽表（身份证号常在很靠后的列，如 AO），固定读 A/B 会全员跳过
      int[] cols = detectNameIdColumns(sheet.getRow(0));
      int nameCol = cols[0];
      int idCardCol = cols[1];
      log.info("importExcel 列定位：姓名列={}, 身份证列={}", nameCol, idCardCol);
      for (int i = 1; i <= sheet.getLastRowNum(); i++) {
        Row row = sheet.getRow(i);
        if (row == null) continue;

        String userName = getCellString(row, nameCol);
        String idCard = getCellString(row, idCardCol);

        if (!StringUtils.hasText(userName) || !StringUtils.hasText(idCard)) continue;
        if (idCard.length() != 18) {
          log.warn("跳过非法定身份证长度行[{}]: {}", i, idCard);
          continue;
        }

        ChazhengUserEntity existing = userMapper.selectOne(
            new LambdaQueryWrapper<ChazhengUserEntity>()
                .eq(ChazhengUserEntity::getTenantId, tenantId)
                .eq(ChazhengUserEntity::getIdCard, idCard)
                .last("LIMIT 1"));

        if (existing != null) {
          existing.setYmd(ymd);
          existing.setUserName(userName);
          userMapper.updateById(existing);
        } else {
          ChazhengUserEntity entity = new ChazhengUserEntity();
          entity.setTenantId(tenantId);
          entity.setUserName(userName);
          entity.setIdCard(idCard);
          entity.setYmd(ymd);
          entity.setIsCert(0);
          entity.setIsChecked(0);
          entity.setStatus(1);
          entity.setSortNumber(1);
          entity.setDeleted(0);
          userMapper.insert(entity);
        }
        imported++;
      }
    } catch (IOException e) {
      log.error("Excel 解析失败", e);
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Excel 解析失败: " + e.getMessage());
    }
    return imported;
  }

  @Transactional(readOnly = true)
  public Map<String, Long> getCertProgress() {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    Long total = userMapper.selectCount(new LambdaQueryWrapper<ChazhengUserEntity>()
        .eq(ChazhengUserEntity::getTenantId, tenantId));
    Long checked = userMapper.selectCount(
        new LambdaQueryWrapper<ChazhengUserEntity>()
            .eq(ChazhengUserEntity::getTenantId, tenantId)
            .eq(ChazhengUserEntity::getIsChecked, 1));
    return Map.of("total", total != null ? total : 0,
        "checked", checked != null ? checked : 0);
  }

  @Transactional(readOnly = true)
  public List<Integer> getEnterprises() {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    LambdaQueryWrapper<ChazhengUserEntity> wrapper = new LambdaQueryWrapper<ChazhengUserEntity>()
        .eq(ChazhengUserEntity::getTenantId, tenantId)
        .select(ChazhengUserEntity::getEnterpriseId)
        .isNotNull(ChazhengUserEntity::getEnterpriseId)
        .groupBy(ChazhengUserEntity::getEnterpriseId)
        .orderByAsc(ChazhengUserEntity::getEnterpriseId);
    List<ChazhengUserEntity> list = userMapper.selectList(wrapper);
    return list.stream()
        .map(ChazhengUserEntity::getEnterpriseId)
        .toList();
  }

  public byte[] generateTemplate() {
    try (Workbook workbook = new XSSFWorkbook()) {
      Sheet sheet = workbook.createSheet("导入模板");
      Row header = sheet.createRow(0);
      header.createCell(0).setCellValue("姓名");
      header.createCell(1).setCellValue("身份证号");
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      workbook.write(bos);
      return bos.toByteArray();
    } catch (IOException e) {
      log.error("生成模板失败", e);
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "生成模板失败");
    }
  }

  @Transactional(readOnly = true)
  public byte[] exportExcel(String ymd) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    LambdaQueryWrapper<ChazhengUserEntity> wrapper = new LambdaQueryWrapper<ChazhengUserEntity>()
        .eq(ChazhengUserEntity::getTenantId, tenantId)
        .orderByDesc(ChazhengUserEntity::getSortNumber)
        .orderByDesc(ChazhengUserEntity::getId);
    if (StringUtils.hasText(ymd)) {
      wrapper.eq(ChazhengUserEntity::getYmd, ymd);
    }
    List<ChazhengUserEntity> users = userMapper.selectList(wrapper);
    return buildExportWorkbook(users);
  }

  @Transactional(readOnly = true)
  public byte[] exportExcel(String ymd, List<Integer> ids) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    LambdaQueryWrapper<ChazhengUserEntity> wrapper = new LambdaQueryWrapper<ChazhengUserEntity>()
        .eq(ChazhengUserEntity::getTenantId, tenantId)
        .orderByDesc(ChazhengUserEntity::getSortNumber)
        .orderByDesc(ChazhengUserEntity::getId);
    if (ids != null && !ids.isEmpty()) {
      wrapper.in(ChazhengUserEntity::getId, ids);
    } else if (StringUtils.hasText(ymd)) {
      wrapper.eq(ChazhengUserEntity::getYmd, ymd);
    }
    List<ChazhengUserEntity> users = userMapper.selectList(wrapper);
    return buildExportWorkbook(users);
  }

  private byte[] buildExportWorkbook(List<ChazhengUserEntity> users) {
    try (Workbook workbook = new XSSFWorkbook()) {
      Sheet sheet = workbook.createSheet("查证用户");
      Row header = sheet.createRow(0);
      header.createCell(0).setCellValue("姓名");
      header.createCell(1).setCellValue("身份证号");
      header.createCell(2).setCellValue("性别");
      header.createCell(3).setCellValue("证书编号");
      header.createCell(4).setCellValue("证书名称");
      header.createCell(5).setCellValue("发证日期");
      header.createCell(6).setCellValue("查证状态");
      header.createCell(7).setCellValue("批次");

      int rowIdx = 1;
      for (ChazhengUserEntity user : users) {
        List<ChazhengCertEntity> certs = certMapper.selectList(
            new LambdaQueryWrapper<ChazhengCertEntity>()
                .eq(ChazhengCertEntity::getUserId, user.getId()));

        if (certs.isEmpty()) {
          Row row = sheet.createRow(rowIdx++);
          row.createCell(0).setCellValue(user.getUserName() != null ? user.getUserName() : "");
          row.createCell(1).setCellValue(user.getIdCard() != null ? user.getIdCard() : "");
          row.createCell(2).setCellValue(user.getGender() != null && user.getGender() == 1 ? "女" : "男");
          row.createCell(3).setCellValue("");
          row.createCell(4).setCellValue("");
          row.createCell(5).setCellValue("");
          row.createCell(6).setCellValue(user.getIsChecked() != null && user.getIsChecked() == 1 ? "已查证" : "未查证");
          row.createCell(7).setCellValue(user.getYmd() != null ? user.getYmd() : "");
        } else {
          for (ChazhengCertEntity cert : certs) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(user.getUserName() != null ? user.getUserName() : "");
            row.createCell(1).setCellValue(user.getIdCard() != null ? user.getIdCard() : "");
            row.createCell(2).setCellValue(user.getGender() != null && user.getGender() == 1 ? "女" : "男");
            row.createCell(3).setCellValue(cert.getCertNo() != null ? cert.getCertNo() : "");
            row.createCell(4).setCellValue(cert.getCertName() != null ? cert.getCertName() : "");
            row.createCell(5).setCellValue(cert.getCertDate() != null ? cert.getCertDate() : "");
            row.createCell(6).setCellValue("已查证");
            row.createCell(7).setCellValue(user.getYmd() != null ? user.getYmd() : "");
          }
        }
      }

      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      workbook.write(bos);
      return bos.toByteArray();
    } catch (IOException e) {
      log.error("导出 Excel 失败", e);
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "导出失败: " + e.getMessage());
    }
  }

  /**
   * 按表头定位「姓名/身份证号」列：首行模糊匹配（身份证号/证件号码、姓名）。
   * 找不到对应表头时回退固定模板 A=姓名 / B=身份证号（旧名单格式）。
   */
  static int[] detectNameIdColumns(Row headerRow) {
    int nameCol = -1;
    int idCardCol = -1;
    if (headerRow != null) {
      short last = headerRow.getLastCellNum();
      for (int c = 0; c < last; c++) {
        String h = getCellString(headerRow, c);
        if (h.isEmpty()) {
          continue;
        }
        if (idCardCol < 0 && (h.contains("身份证") || h.contains("证件号码"))) {
          idCardCol = c;
        } else if (nameCol < 0 && h.contains("姓名")) {
          nameCol = c;
        }
      }
    }
    if (idCardCol < 0) {
      idCardCol = 1;
    }
    if (nameCol < 0 || nameCol == idCardCol) {
      nameCol = idCardCol == 0 ? 1 : 0;
    }
    return new int[]{nameCol, idCardCol};
  }

  /**
   * 单元格按类型分派读取：身份证号等常被录成数值格式，硬读字符串会抛
   * IllegalStateException 导致整个导入 500。数值按整数还原（不带 .0、不用科学计数法；
   * 但 18 位数字已超 Excel 双精度范围，末几位在源文件里就已丢失，无法还原）。
   */
  static String getCellString(Row row, int index) {
    Cell cell = row.getCell(index);
    if (cell == null) return "";
    return switch (cell.getCellType()) {
      case STRING -> cell.getStringCellValue().trim();
      case NUMERIC -> {
        double d = cell.getNumericCellValue();
        if (d == Math.floor(d) && !Double.isInfinite(d)) {
          // BigDecimal.valueOf 走 Double.toString 再转 plain，避免科学计数法
          yield BigDecimal.valueOf(d).toPlainString();
        }
        yield String.valueOf(d);
      }
      case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
      case FORMULA -> {
        // 公式单元格按缓存的计算结果类型再分派一次
        try {
          yield switch (cell.getCachedFormulaResultType()) {
            case NUMERIC -> {
              double d = cell.getNumericCellValue();
              yield d == Math.floor(d) && !Double.isInfinite(d)
                  ? BigDecimal.valueOf(d).toPlainString() : String.valueOf(d);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> cell.getStringCellValue() == null ? "" : cell.getStringCellValue().trim();
          };
        } catch (RuntimeException e) {
          yield "";
        }
      }
      default -> "";
    };
  }

  // ════════════════════════════════════════════════════════════════════════
  //  单个用户查证（重构版 —— 对齐 PHP queryCertByTemplate）
  // ════════════════════════════════════════════════════════════════════════

  /**
   * 对单个用户执行证书查证（无事务编排层）。
   */
  public ChazhengUserVO certCheck(Integer id, Long tenantId) {
    ChazhengUserEntity user = userMapper.selectOne(new LambdaQueryWrapper<ChazhengUserEntity>()
        .eq(ChazhengUserEntity::getTenantId, tenantId)
        .eq(ChazhengUserEntity::getId, id)
        .eq(ChazhengUserEntity::getDeleted, 0));
    if (user == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "查证用户不存在");
    }

    log.info("[certCheck] 开始查证 user={}, idCard={}", id, maskIdCard(user.getIdCard()));

    CertQueryOutcome outcome = chazhengCertQueryService.queryByNameIdCard(
        user.getUserName(), user.getIdCard(), tenantId);

    log.info("[certCheck] 查证完成 user={}, anyNormal={}, anyCertFound={}, certCount={}",
        id, outcome.anyNormal(), outcome.anyCertFound(), outcome.certs().size());

    try {
      chazhengPersistService.persistResult(
          user, outcome.certs(), outcome.anyNormal(), outcome.anyCertFound());
    } catch (Exception e) {
      log.error("[certCheck] DB 写入失败 user={}, err={}", id, e.getMessage(), e);
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "查证结果写入失败: " + e.getMessage());
    }

    ChazhengUserVO vo = ChazhengUserVO.from(user);
    List<ChazhengCertEntity> certs = certMapper.selectList(
        new LambdaQueryWrapper<ChazhengCertEntity>()
            .eq(ChazhengCertEntity::getUserId, user.getId()));
    vo.setCerts(certs.stream().map(ChazhengCertVO::from).toList());
    return vo;
  }

  /** 截断字符串(日志用) */
  private static String abbreviate(String s, int maxLen) {
    if (s == null) return "null";
    return s.length() <= maxLen ? s : s.substring(0, maxLen) + "...(truncated)";
  }

  /** 身份证号脱敏(日志用) */
  private static String maskIdCard(String idCard) {
    if (idCard == null || idCard.length() < 8) return "***";
    return idCard.substring(0, 4) + "********" + idCard.substring(idCard.length() - 4);
  }

  // ════════════════════════════════════════════════════════════════════════
  //  批量异步查证（新增）
  // ════════════════════════════════════════════════════════════════════════

  /**
   * 提交批次查证任务。
   */
  @Transactional
  public CertCheckBatchSubmitResp submitCertCheckBatch(List<Long> userIds, String sourceFilename) {
    if (userIds == null || userIds.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "无用户可查证");
    }
    Long tenantId = SecurityContext.currentUser().getTenantId();
    Long triggerBy = SecurityContext.currentUser().getUserId();

    // 1. 创建 batch 记录
    CertCheckBatchEntity batch = new CertCheckBatchEntity();
    batch.setTenantId(tenantId);
    batch.setTotalCount(userIds.size());
    batch.setCompletedCount(0);
    batch.setStatus("processing");
    batch.setSourceFilename(sourceFilename);
    batch.setTriggerBy(triggerBy);
    batch.setTriggeredAt(LocalDateTime.now());
    batch.setDeleted(0);
    batchMapper.insert(batch);

    // 2. 为每个用户创建 item 记录
    for (Long userId : userIds) {
      ChazhengUserEntity user = userMapper.selectById(userId);
      CertCheckBatchItemEntity item = new CertCheckBatchItemEntity();
      item.setBatchId(batch.getId());
      item.setUserId(userId);
      item.setUserName(user != null ? user.getUserName() : "");
      item.setTenantId(tenantId);
      item.setStatus("pending");
      item.setRetryCount(0);
      item.setDeleted(0);
      batchItemMapper.insert(item);
    }

    // 3. 提交到线程池(tenantId 从 HTTP 请求线程传递到工作线程)
    for (Long userId : userIds) {
      executor.submit(() -> processBatchUser(batch.getId(), userId, tenantId));
    }

    log.info("[chazheng-batch] 提交批次 batch={}, users={}", batch.getId(), userIds.size());
    return new CertCheckBatchSubmitResp(batch.getId(), userIds.size(),
        "导入完成，正在后台查证，批次 ID: " + batch.getId());
  }

  private void processBatchUser(long batchId, Long userId, Long tenantId) {
    // tenantId 从 HTTP 请求线程传递下来,工作线程不可调 SecurityContext
    batchItemMapper.updateStatus(batchId, userId, "processing", null);
    try {
      ChazhengUserVO vo = certCheck(userId.intValue(), tenantId);

      boolean hasCert = vo.getIsCert() != null && vo.getIsCert() == 1;
      batchItemMapper.updateSuccess(batchId, userId, hasCert ? "success" : "success_no_cert");
      batchMapper.incrementCompleted(batchId);
    } catch (Exception e) {
      log.warn("[chazheng-batch] 查证异常 batch={}, user={}, err={}", batchId, userId, e.toString());
      log.warn("[chazheng-batch] 查证异常堆栈 batch={}, user={}", batchId, userId, e);
      handleBatchFailure(batchId, userId, e, tenantId);
    }
  }

  private void handleBatchFailure(long batchId, Long userId, Exception e, Long tenantId) {
    // 截取错误信息前 200 字符(避免超长短文写入 DB)
    String errorMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
    if (errorMsg.length() > 200) errorMsg = errorMsg.substring(0, 200);

    int retryCount = batchItemMapper.incrementRetry(batchId, userId);
    if (retryCount <= maxRetry) {
      // 重试时也记录异常详情,不再只打印 retry 编号
      log.warn("[chazheng-batch] 查证失败,准备重试 batch={}, user={}, retry={}, cause={}",
          batchId, userId, retryCount, errorMsg);
      executor.submit(() -> processBatchUser(batchId, userId, tenantId));
    } else {
      log.error("[chazheng-batch] 查证最终失败(重试耗尽) batch={}, user={}, err={}", batchId, userId, errorMsg);
      batchItemMapper.updateFailed(batchId, userId, errorMsg);
      batchMapper.incrementCompleted(batchId);
    }
  }

  public CertCheckBatchProgress getBatchProgress(Long batchId) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    CertCheckBatchEntity batch = batchMapper.selectById(batchId);
    if (batch == null || !batch.getTenantId().equals(tenantId)) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "批次不存在或无权访问");
    }

    List<Map<String, Object>> statusRows = batchItemMapper.countByStatus(batchId);
    Map<String, Long> statusMap = new HashMap<>();
    for (Map<String, Object> row : statusRows) {
      statusMap.put(String.valueOf(row.get("status")), Long.valueOf(String.valueOf(row.get("cnt"))));
    }

    long success = statusMap.getOrDefault("success", 0L);
    long successNoCert = statusMap.getOrDefault("success_no_cert", 0L);
    long failed = statusMap.getOrDefault("failed", 0L);
    long processing = statusMap.getOrDefault("processing", 0L);
    long pending = statusMap.getOrDefault("pending", 0L);
    long completed = success + successNoCert + failed;
    int total = batch.getTotalCount();
    int percent = total > 0 ? (int) (completed * 100 / total) : 0;

    LocalDateTime triggered = batch.getTriggeredAt();
    Duration elapsed = Duration.between(triggered, LocalDateTime.now());
    String elapsedStr = formatDuration(elapsed);
    String estimateRemainingStr = "--:--:--";
    if (completed > 0 && completed < total) {
      long avgMsPerUser = elapsed.toMillis() / completed;
      long remainingMs = avgMsPerUser * (total - completed);
      estimateRemainingStr = formatDuration(Duration.ofMillis(remainingMs));
    }

    return CertCheckBatchProgress.builder()
        .batchId(batchId)
        .total(total)
        .completed((int) completed)
        .success(success)
        .successNoCert(successNoCert)
        .failed(failed)
        .pending(pending + processing)
        .percent(percent)
        .status(batch.getStatus())
        .elapsed(elapsedStr)
        .estimateRemaining(estimateRemainingStr)
        .build();
  }

  public CertCheckBatchReport getBatchReport(Long batchId) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    CertCheckBatchEntity batch = batchMapper.selectById(batchId);
    if (batch == null || !batch.getTenantId().equals(tenantId)) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "批次不存在或无权访问");
    }

    List<CertCheckBatchItemEntity> failedItems = batchItemMapper.selectFailedItems(batchId);

    CertCheckBatchReport report = new CertCheckBatchReport();
    report.setBatchId(batchId);
    report.setStatus(batch.getStatus());
    report.setTriggerBy(batch.getTriggerBy());
    report.setSourceFilename(batch.getSourceFilename());
    report.setTriggeredAt(batch.getTriggeredAt());
    report.setCompletedAt(batch.getCompletedAt());

    List<Map<String, Object>> statusRows = batchItemMapper.countByStatus(batchId);
    Map<String, Long> statusMap = new HashMap<>();
    for (Map<String, Object> row : statusRows) {
      statusMap.put(String.valueOf(row.get("status")), Long.valueOf(String.valueOf(row.get("cnt"))));
    }

    CertCheckBatchReport.CertCheckSummary summary = new CertCheckBatchReport.CertCheckSummary();
    summary.setTotal(batch.getTotalCount());
    summary.setSuccess(statusMap.getOrDefault("success", 0L));
    summary.setSuccessNoCert(statusMap.getOrDefault("success_no_cert", 0L));
    summary.setFailed(statusMap.getOrDefault("failed", 0L));
    report.setSummary(summary);

    List<CertCheckBatchReport.CertCheckFailedDetail> failedDetails = new ArrayList<>();
    for (CertCheckBatchItemEntity item : failedItems) {
      CertCheckBatchReport.CertCheckFailedDetail detail = new CertCheckBatchReport.CertCheckFailedDetail();
      detail.setUserId(item.getUserId());
      detail.setUserName(item.getUserName());
      detail.setError(item.getErrorMessage());
      detail.setRetryCount(item.getRetryCount());
      failedDetails.add(detail);
    }
    report.setFailedDetails(failedDetails);
    return report;
  }

  @Transactional
  public CertCheckBatchSubmitResp retryFailedBatch(Long batchId) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    CertCheckBatchEntity batch = batchMapper.selectById(batchId);
    if (batch == null || !batch.getTenantId().equals(tenantId)) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "批次不存在或无权访问");
    }

    int resetCount = batchItemMapper.resetFailedToPending(batchId);

    List<CertCheckBatchItemEntity> pendingItems = batchItemMapper.selectList(
        new LambdaQueryWrapper<CertCheckBatchItemEntity>()
            .eq(CertCheckBatchItemEntity::getBatchId, batchId)
            .eq(CertCheckBatchItemEntity::getStatus, "pending"));

    for (CertCheckBatchItemEntity item : pendingItems) {
      executor.submit(() -> processBatchUser(batchId, item.getUserId(), tenantId));
    }

    log.info("[chazheng-batch] 重试失败用户 batch={}, count={}", batchId, resetCount);
    return new CertCheckBatchSubmitResp(batchId, resetCount, "已重试 " + resetCount + " 个失败用户");
  }

  private static String formatDuration(Duration d) {
    return String.format("%02d:%02d:%02d", d.toHours(), d.toMinutesPart(), d.toSecondsPart());
  }
}
