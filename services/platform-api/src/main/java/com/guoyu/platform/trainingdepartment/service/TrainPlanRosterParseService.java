package com.guoyu.platform.trainingdepartment.service;

import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.trainingdepartment.entity.TrainPlanRosterEntity;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 企业筛选名单 Excel → 待分班池行。
 */
@Service
public class TrainPlanRosterParseService {

  private final DataFormatter formatter = new DataFormatter();

  public List<TrainPlanRosterEntity> parseEnterpriseRoster(byte[] bytes) {
    if (bytes == null || bytes.length == 0) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "筛选名单文件为空");
    }
    try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
      Sheet sheet = workbook.getNumberOfSheets() > 0 ? workbook.getSheetAt(0) : null;
      if (sheet == null) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "筛选名单无有效工作表");
      }
      int headerRowIdx = findHeaderRow(sheet);
      if (headerRowIdx < 0) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "未识别到含「姓名」的表头行");
      }
      Row header = sheet.getRow(headerRowIdx);
      Map<String, Integer> colMap = mapColumns(header);
      Integer nameCol = colMap.get("name");
      if (nameCol == null) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "表头缺少姓名列");
      }

      List<TrainPlanRosterEntity> rows = new ArrayList<>();
      for (int r = headerRowIdx + 1; r <= sheet.getLastRowNum(); r++) {
        Row row = sheet.getRow(r);
        if (row == null) continue;
        String name = cell(row, nameCol);
        if (!StringUtils.hasText(name)) continue;
        TrainPlanRosterEntity entity = new TrainPlanRosterEntity();
        entity.setSource("enterprise_roster");
        entity.setPersonName(name.trim());
        entity.setIdCard(trimToNull(cell(row, colMap.get("idCard"))));
        entity.setPhone(trimToNull(cell(row, colMap.get("phone"))));
        entity.setJobName(trimToNull(cell(row, colMap.get("job"))));
        entity.setLevelName(trimToNull(cell(row, colMap.get("level"))));
        entity.setWorkshop(trimToNull(cell(row, colMap.get("workshop"))));
        entity.setShiftTeam(trimToNull(cell(row, colMap.get("shiftTeam"))));
        entity.setAssignStatus(0);
        entity.setDeleted(0);
        rows.add(entity);
      }
      if (rows.isEmpty()) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "筛选名单未解析到有效人员行");
      }
      return rows;
    } catch (BusinessException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "解析筛选名单失败：" + ex.getMessage());
    }
  }

  private int findHeaderRow(Sheet sheet) {
    int maxScan = Math.min(sheet.getLastRowNum(), 30);
    for (int i = 0; i <= maxScan; i++) {
      Row row = sheet.getRow(i);
      if (row == null) continue;
      for (Cell cell : row) {
        String v = formatter.formatCellValue(cell);
        if (v != null && v.contains("姓名")) {
          return i;
        }
      }
    }
    return -1;
  }

  private Map<String, Integer> mapColumns(Row header) {
    Map<String, Integer> map = new HashMap<>();
    short last = header.getLastCellNum();
    for (int c = 0; c < last; c++) {
      String raw = formatter.formatCellValue(header.getCell(c));
      if (!StringUtils.hasText(raw)) continue;
      String h = raw.trim().toLowerCase(Locale.ROOT);
      if (h.contains("姓名") && !map.containsKey("name")) map.put("name", c);
      else if ((h.contains("身份证") || h.contains("证件号") || h.equals("证件号码")) && !map.containsKey("idCard")) {
        map.put("idCard", c);
      } else if ((h.contains("手机") || h.contains("电话") || h.contains("联系方式")) && !map.containsKey("phone")) {
        map.put("phone", c);
      } else if ((h.contains("工种") || h.contains("职业")) && !map.containsKey("job")) {
        map.put("job", c);
      } else if ((h.contains("等级") || h.contains("级别")) && !map.containsKey("level")) {
        map.put("level", c);
      } else if ((h.contains("车间") || h.contains("工段") || h.contains("部门")) && !map.containsKey("workshop")) {
        map.put("workshop", c);
      } else if ((h.contains("班组") || h.equals("班别") || h.equals("备注") || h.contains("倒班班"))
          && !map.containsKey("shiftTeam")) {
        map.put("shiftTeam", c);
      } else if (h.contains("班级") && !map.containsKey("shiftTeam")) {
        // 有的表「班级」实为培训班名；若无班组列可忽略，不覆盖 workshop
      }
    }
    // 备注列常放生产班组（来铝样例）
    if (!map.containsKey("shiftTeam")) {
      for (int c = 0; c < last; c++) {
        String raw = formatter.formatCellValue(header.getCell(c));
        if (raw != null && raw.trim().contains("备注")) {
          map.put("shiftTeam", c);
          break;
        }
      }
    }
    return map;
  }

  private String cell(Row row, Integer col) {
    if (col == null) return null;
    return formatter.formatCellValue(row.getCell(col));
  }

  private String trimToNull(String v) {
    if (!StringUtils.hasText(v)) return null;
    String t = v.trim();
    return t.isEmpty() ? null : t;
  }
}
