package com.guoyu.platform.trainingdepartment.support;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.trainingdepartment.entity.TrainSignUpPeopleSelfEntity;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/** 高命中率认定采集模板精简版导出 */
@Component
@Slf4j
public class ResearchCollectionTemplateExportService {

  private static final String TEMPLATE_CLASSPATH = "template/高命中率认定采集模板精简版.xlsx";
  private static final String SHEET_NAME = "采集表";
  private static final String[] LEVEL_LABELS = {"初级工", "中级工", "高级工", "技师", "高级技师"};

  private final String cdnPath;
  private final ObjectMapper objectMapper;

  public ResearchCollectionTemplateExportService(
      @Value("${platform.cdn.path:./data/cdn}") String cdnPath,
      ObjectMapper objectMapper) {
    this.cdnPath = cdnPath;
    this.objectMapper = objectMapper;
  }

  public byte[] export(List<TrainSignUpPeopleSelfEntity> people, String companyName) {
    try (InputStream in = openTemplate();
        Workbook workbook = WorkbookFactory.create(in);
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      Sheet sheet = workbook.getSheet(SHEET_NAME);
      if (sheet == null && workbook.getNumberOfSheets() > 0) {
        sheet = workbook.getSheetAt(0);
      }
      if (sheet == null) {
        throw new BusinessException(ErrorCode.INTERNAL_ERROR, "模板缺少采集表工作表");
      }
      int rowIdx = 1;
      for (TrainSignUpPeopleSelfEntity person : people) {
        Row row = sheet.getRow(rowIdx);
        if (row == null) {
          row = sheet.createRow(rowIdx);
        }
        writePersonRow(row, person, companyName);
        rowIdx++;
      }
      workbook.write(out);
      return out.toByteArray();
    } catch (BusinessException e) {
      throw e;
    } catch (Exception e) {
      log.error("导出精简版采集模板失败", e);
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "导出精简版采集模板失败");
    }
  }

  private void writePersonRow(Row row, TrainSignUpPeopleSelfEntity person, String defaultCompany) {
    CertColumns certs = resolveCertColumns(person);
    setCell(row, 0, person.getName());
    setCell(row, 1, person.getIdCardNumber());
    setCell(row, 2, firstNonBlank(person.getCompanyName(), defaultCompany));
    setCell(row, 3, person.getMajor());
    setCell(row, 4, person.getEducation());
    setCell(row, 5, person.getWorkTime());
    setCell(row, 6, person.getWorkTypeName());
    setCell(row, 7, person.getDepartment());
    setCell(row, 8, firstNonBlank(person.getJob(), person.getJog()));
    setCell(row, 9, certs.levels);
    setCell(row, 10, certs.certNos);
    setCell(row, 11, certs.issueDates);
    setCell(row, 12, person.getWorkDateStart());
    setCell(row, 13, person.getTitleType());
    setCell(row, 14, person.getTitleMajor());
    setCell(row, 15, person.getTitleCertDate());
  }

  private CertColumns resolveCertColumns(TrainSignUpPeopleSelfEntity person) {
    List<Map<String, String>> items = parseCertificatesJson(person.getCertificatesJson());
    if (!items.isEmpty()) {
      return new CertColumns(
          joinLines(items, "level"),
          joinLines(items, "certNo"),
          joinLines(items, "issueDate"));
    }
    String levelText = levelToText(person.getLevel());
    if (StringUtils.hasText(levelText) || StringUtils.hasText(person.getPaperNo())
        || StringUtils.hasText(person.getCertIssueDates())) {
      return new CertColumns(
          defaultString(levelText),
          defaultString(person.getPaperNo()),
          defaultString(person.getCertIssueDates()));
    }
    return new CertColumns("", "", "");
  }

  private List<Map<String, String>> parseCertificatesJson(String json) {
    if (!StringUtils.hasText(json)) {
      return List.of();
    }
    try {
      List<Map<String, Object>> raw = objectMapper.readValue(json, new TypeReference<>() {});
      List<Map<String, String>> result = new ArrayList<>();
      for (Map<String, Object> item : raw) {
        Map<String, String> mapped = new LinkedHashMap<>();
        mapped.put("level", asString(item.get("level")));
        mapped.put("certNo", asString(firstNonNull(item.get("certNo"), item.get("cert_no"))));
        mapped.put("issueDate", asString(firstNonNull(item.get("issueDate"), item.get("issue_date"))));
        result.add(mapped);
      }
      return result;
    } catch (Exception e) {
      log.warn("解析 certificates_json 失败: {}", e.getMessage());
      return List.of();
    }
  }

  private static String joinLines(List<Map<String, String>> items, String key) {
    return items.stream()
        .map(item -> defaultString(item.get(key)))
        .collect(Collectors.joining("\n"));
  }

  private static String levelToText(Integer level) {
    if (level == null || level < 1 || level > LEVEL_LABELS.length) {
      return "";
    }
    return LEVEL_LABELS[level - 1];
  }

  private InputStream openTemplate() throws Exception {
    ClassPathResource resource = new ClassPathResource(TEMPLATE_CLASSPATH);
    if (resource.exists()) {
      return resource.getInputStream();
    }
    Path cdnTemplate = Paths.get(cdnPath, TEMPLATE_CLASSPATH);
    if (Files.exists(cdnTemplate)) {
      return Files.newInputStream(cdnTemplate);
    }
    throw new BusinessException(ErrorCode.INTERNAL_ERROR, "未找到精简版采集模板文件");
  }

  private static void setCell(Row row, int col, String value) {
    row.createCell(col).setCellValue(defaultString(value));
  }

  private static String defaultString(String value) {
    return value != null ? value : "";
  }

  private static String firstNonBlank(String... values) {
    for (String value : values) {
      if (StringUtils.hasText(value)) {
        return value;
      }
    }
    return "";
  }

  private static Object firstNonNull(Object a, Object b) {
    return a != null ? a : b;
  }

  private static String asString(Object value) {
    return value != null ? String.valueOf(value) : "";
  }

  private record CertColumns(String levels, String certNos, String issueDates) {}
}
