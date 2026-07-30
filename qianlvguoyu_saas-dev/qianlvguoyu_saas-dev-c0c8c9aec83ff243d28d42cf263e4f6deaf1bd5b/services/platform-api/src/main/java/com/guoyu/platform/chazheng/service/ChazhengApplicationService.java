package com.guoyu.platform.chazheng.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.chazheng.dto.ChazhengUserQueryReq;
import com.guoyu.platform.chazheng.entity.ChazhengCertEntity;
import com.guoyu.platform.chazheng.entity.ChazhengUserEntity;
import com.guoyu.platform.chazheng.mapper.ChazhengCertMapper;
import com.guoyu.platform.chazheng.mapper.ChazhengUserMapper;
import com.guoyu.platform.chazheng.vo.ChazhengCertVO;
import com.guoyu.platform.chazheng.vo.ChazhengUserVO;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

@Service
@Slf4j
public class ChazhengApplicationService {

  private final ChazhengUserMapper userMapper;
  private final ChazhengCertMapper certMapper;

  public ChazhengApplicationService(ChazhengUserMapper userMapper, ChazhengCertMapper certMapper) {
    this.userMapper = userMapper;
    this.certMapper = certMapper;
  }

  @Value("${chazheng.cert-api-url:#{null}}")
  private String certApiUrl;

  @Transactional(readOnly = true)
  public PageResponse<ChazhengUserVO> page(ChazhengUserQueryReq req) {
    LambdaQueryWrapper<ChazhengUserEntity> wrapper = new LambdaQueryWrapper<ChazhengUserEntity>()
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

    Page<ChazhengUserEntity> page = userMapper.selectPage(
        new Page<>(req.getPage(), req.getPageSize()), wrapper);

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

  @Transactional(readOnly = true)
  public List<String> getYmdList() {
    LambdaQueryWrapper<ChazhengUserEntity> wrapper = new LambdaQueryWrapper<ChazhengUserEntity>()
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

    String ymd = String.valueOf(System.currentTimeMillis());
    int imported = 0;

    try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
      Sheet sheet = workbook.getSheetAt(0);
      for (int i = 1; i <= sheet.getLastRowNum(); i++) {
        Row row = sheet.getRow(i);
        if (row == null) {
          continue;
        }

        String userName = getCellString(row, 0);
        String idCard = getCellString(row, 1);

        if (!StringUtils.hasText(userName) || !StringUtils.hasText(idCard)) {
          continue;
        }
        if (idCard.length() != 18) {
          log.warn("跳过错行[{}]: {}", i, idCard);
          continue;
        }

        ChazhengUserEntity existing = userMapper.selectOne(
            new LambdaQueryWrapper<ChazhengUserEntity>()
                .eq(ChazhengUserEntity::getIdCard, idCard)
                .last("LIMIT 1"));

        if (existing != null) {
          existing.setYmd(ymd);
          existing.setUserName(userName);
          userMapper.updateById(existing);
        } else {
          ChazhengUserEntity entity = new ChazhengUserEntity();
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

  @Transactional
  public ChazhengUserVO certCheck(Integer id) {
    ChazhengUserEntity user = findExistingUser(id);

    String apiUrl = this.certApiUrl;
    if (!StringUtils.hasText(apiUrl)) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "查证接口未配置");
    }

    try {
      RestTemplate rest = new RestTemplate();
      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_JSON);
      Map<String, String> body = new LinkedHashMap<>();
      body.put("idcode", user.getIdCard());
      body.put("username", user.getUserName());
      HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);

      @SuppressWarnings("unchecked")
      Map<String, Object> response = rest.postForObject(apiUrl, request, Map.class);

      if (response != null && response.get("code") != null
          && "0".equals(String.valueOf(response.get("code")))) {
        certMapper.delete(new LambdaQueryWrapper<ChazhengCertEntity>()
            .eq(ChazhengCertEntity::getUserId, user.getId()));

        Object dataObj = response.get("data");
        if (dataObj instanceof Map) {
          @SuppressWarnings("unchecked")
          Map<String, Object> dataMap = (Map<String, Object>) dataObj;
          if (dataMap.containsKey("certNo")) {
            ChazhengCertEntity cert = new ChazhengCertEntity();
            cert.setUserId(user.getId());
            cert.setCertNo(String.valueOf(dataMap.getOrDefault("certNo", "")));
            cert.setCertName(String.valueOf(dataMap.getOrDefault("certName", "")));
            cert.setCertDate(String.valueOf(dataMap.getOrDefault("certDate", "")));
            certMapper.insert(cert);
            user.setIsCert(1);
          }
        } else if (dataObj instanceof List) {
          @SuppressWarnings("unchecked")
          List<Map<String, Object>> certList = (List<Map<String, Object>>) dataObj;
          if (!certList.isEmpty()) {
            for (Map<String, Object> certItem : certList) {
              ChazhengCertEntity cert = new ChazhengCertEntity();
              cert.setUserId(user.getId());
              cert.setCertNo(String.valueOf(certItem.getOrDefault("certNo", "")));
              cert.setCertName(String.valueOf(certItem.getOrDefault("certName", "")));
              cert.setCertDate(String.valueOf(certItem.getOrDefault("certDate", "")));
              certMapper.insert(cert);
            }
            user.setIsCert(1);
          }
        }
      }

      user.setIsChecked(1);
      userMapper.updateById(user);
    } catch (Exception e) {
      log.warn("查证调用失败 user={}", user.getId(), e);
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "查证调用失败: " + e.getMessage());
    }

    ChazhengUserVO vo = ChazhengUserVO.from(user);
    List<ChazhengCertEntity> certs = certMapper.selectList(
        new LambdaQueryWrapper<ChazhengCertEntity>()
            .eq(ChazhengCertEntity::getUserId, user.getId()));
    vo.setCerts(certs.stream().map(ChazhengCertVO::from).toList());
    return vo;
  }

  @Transactional(readOnly = true)
  public Map<String, Long> getCertProgress() {
    Long total = userMapper.selectCount(new LambdaQueryWrapper<>());
    Long checked = userMapper.selectCount(
        new LambdaQueryWrapper<ChazhengUserEntity>()
            .eq(ChazhengUserEntity::getIsChecked, 1));
    return Map.of("total", total != null ? total : 0,
        "checked", checked != null ? checked : 0);
  }

  @Transactional(readOnly = true)
  public byte[] exportExcel(String ymd) {
    LambdaQueryWrapper<ChazhengUserEntity> wrapper = new LambdaQueryWrapper<ChazhengUserEntity>()
        .orderByDesc(ChazhengUserEntity::getSortNumber)
        .orderByDesc(ChazhengUserEntity::getId);
    if (StringUtils.hasText(ymd)) {
      wrapper.eq(ChazhengUserEntity::getYmd, ymd);
    }
    List<ChazhengUserEntity> users = userMapper.selectList(wrapper);

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
          row.createCell(0).setCellValue(nullToEmpty(user.getUserName()));
          row.createCell(1).setCellValue(nullToEmpty(user.getIdCard()));
          row.createCell(2).setCellValue(user.getGender() != null && user.getGender() == 1 ? "女" : "男");
          row.createCell(3).setCellValue("");
          row.createCell(4).setCellValue("");
          row.createCell(5).setCellValue("");
          row.createCell(6).setCellValue(user.getIsChecked() != null && user.getIsChecked() == 1 ? "已查证" : "未查证");
          row.createCell(7).setCellValue(nullToEmpty(user.getYmd()));
        } else {
          for (ChazhengCertEntity cert : certs) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(nullToEmpty(user.getUserName()));
            row.createCell(1).setCellValue(nullToEmpty(user.getIdCard()));
            row.createCell(2).setCellValue(user.getGender() != null && user.getGender() == 1 ? "女" : "男");
            row.createCell(3).setCellValue(nullToEmpty(cert.getCertNo()));
            row.createCell(4).setCellValue(nullToEmpty(cert.getCertName()));
            row.createCell(5).setCellValue(nullToEmpty(cert.getCertDate()));
            row.createCell(6).setCellValue("已查证");
            row.createCell(7).setCellValue(nullToEmpty(user.getYmd()));
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

  @Transactional(readOnly = true)
  public List<Integer> getEnterprises() {
    LambdaQueryWrapper<ChazhengUserEntity> wrapper = new LambdaQueryWrapper<ChazhengUserEntity>()
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
      Sheet sheet = workbook.createSheet("\u5bfc\u5165\u6a21\u677f");
      Row header = sheet.createRow(0);
      header.createCell(0).setCellValue("\u59d3\u540d");
      header.createCell(1).setCellValue("\u8eab\u4efd\u8bc1\u53f7");
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      workbook.write(bos);
      return bos.toByteArray();
    } catch (IOException e) {
      log.error("\u751f\u6210\u6a21\u677f\u5931\u8d25", e);
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "\u751f\u6210\u6a21\u677f\u5931\u8d25");
    }
  }

  @Transactional(readOnly = true)
  public byte[] exportExcel(String ymd, List<Integer> ids) {
    LambdaQueryWrapper<ChazhengUserEntity> wrapper = new LambdaQueryWrapper<ChazhengUserEntity>()
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
      Sheet sheet = workbook.createSheet("\u67e5\u8bc1\u7528\u6237");
      Row header = sheet.createRow(0);
      header.createCell(0).setCellValue("\u59d3\u540d");
      header.createCell(1).setCellValue("\u8eab\u4efd\u8bc1\u53f7");
      header.createCell(2).setCellValue("\u6027\u522b");
      header.createCell(3).setCellValue("\u8bc1\u4e66\u7f16\u53f7");
      header.createCell(4).setCellValue("\u8bc1\u4e66\u540d\u79f0");
      header.createCell(5).setCellValue("\u53d1\u8bc1\u65e5\u671f");
      header.createCell(6).setCellValue("\u67e5\u8bc1\u72b6\u6001");
      header.createCell(7).setCellValue("\u6279\u6b21");

      int rowIdx = 1;
      for (ChazhengUserEntity user : users) {
        List<ChazhengCertEntity> certs = certMapper.selectList(
            new LambdaQueryWrapper<ChazhengCertEntity>()
                .eq(ChazhengCertEntity::getUserId, user.getId()));

        if (certs.isEmpty()) {
          Row row = sheet.createRow(rowIdx++);
          row.createCell(0).setCellValue(nullToEmpty(user.getUserName()));
          row.createCell(1).setCellValue(nullToEmpty(user.getIdCard()));
          row.createCell(2).setCellValue(user.getGender() != null && user.getGender() == 1 ? "\u5973" : "\u7537");
          row.createCell(3).setCellValue("");
          row.createCell(4).setCellValue("");
          row.createCell(5).setCellValue("");
          row.createCell(6).setCellValue(user.getIsChecked() != null && user.getIsChecked() == 1 ? "\u5df2\u67e5\u8bc1" : "\u672a\u67e5\u8bc1");
          row.createCell(7).setCellValue(nullToEmpty(user.getYmd()));
        } else {
          for (ChazhengCertEntity cert : certs) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(nullToEmpty(user.getUserName()));
            row.createCell(1).setCellValue(nullToEmpty(user.getIdCard()));
            row.createCell(2).setCellValue(user.getGender() != null && user.getGender() == 1 ? "\u5973" : "\u7537");
            row.createCell(3).setCellValue(nullToEmpty(cert.getCertNo()));
            row.createCell(4).setCellValue(nullToEmpty(cert.getCertName()));
            row.createCell(5).setCellValue(nullToEmpty(cert.getCertDate()));
            row.createCell(6).setCellValue("\u5df2\u67e5\u8bc1");
            row.createCell(7).setCellValue(nullToEmpty(user.getYmd()));
          }
        }
      }

      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      workbook.write(bos);
      return bos.toByteArray();
    } catch (IOException e) {
      log.error("\u5bfc\u51fa Excel \u5931\u8d25", e);
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "\u5bfc\u51fa\u5931\u8d25: " + e.getMessage());
    }
  }

  private ChazhengUserEntity findExistingUser(Integer id) {
    ChazhengUserEntity entity = userMapper.selectById(id);
    if (entity == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
    }
    return entity;
  }

  private static String getCellString(Row row, int index) {
    if (row.getCell(index) == null) {
      return "";
    }
    return row.getCell(index).getStringCellValue().trim();
  }

  private static String nullToEmpty(String value) {
    return value == null ? "" : value;
  }
}