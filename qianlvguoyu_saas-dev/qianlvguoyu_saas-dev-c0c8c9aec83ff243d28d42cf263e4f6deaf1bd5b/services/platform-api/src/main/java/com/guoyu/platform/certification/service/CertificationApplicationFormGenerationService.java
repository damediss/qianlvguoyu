package com.guoyu.platform.certification.service;

import com.guoyu.platform.certification.entity.CertificationCandidateSourceStagingEntity;
import com.guoyu.platform.certification.support.IdCardSupport;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class CertificationApplicationFormGenerationService {

  private static final String TEMPLATE_PATH = "templates/职业技能等级认定申请表.docx";
  private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy年M月d日");

  public byte[] generateApplicationForm(CertificationCandidateSourceStagingEntity staging) {
    try (InputStream input = new ClassPathResource(TEMPLATE_PATH).getInputStream();
        XWPFDocument document = new XWPFDocument(input);
        ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      Map<String, String> variables = buildVariables(staging);
      replaceInDocument(document, variables);
      document.write(output);
      return output.toByteArray();
    } catch (IOException ex) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "认定申请表模板生成失败");
    }
  }

  private Map<String, String> buildVariables(CertificationCandidateSourceStagingEntity staging) {
    Map<String, String> variables = new LinkedHashMap<>();
    variables.put("candidate_name", text(staging.getCandidateName()));
    variables.put("gender", text(firstNonBlank(staging.getGender(), IdCardSupport.parseGender(staging.getIdCardNo()))));
    variables.put("birth_date", text(firstNonBlank(
        IdCardSupport.formatBirthDate(staging.getBirthDate()),
        IdCardSupport.formatBirthDate(IdCardSupport.parseBirthDate(staging.getIdCardNo())))));
    variables.put("education", text(staging.getEducation()));
    variables.put("graduate_date", text(formatDate(staging.getGraduateDate())));
    variables.put("id_card_no", text(staging.getIdCardNo()));
    variables.put("company_name", text(staging.getCompanyName()));
    variables.put("phone", text(staging.getPhone()));
    variables.put("company_address", text(staging.getCompanyAddress()));
    variables.put("zip_code", text(staging.getZipCode()));
    variables.put("work_start_date", text(formatDate(staging.getWorkStartDate())));
    variables.put("original_work_type", text(firstNonBlank(staging.getOriginalWorkTypeName(), staging.getWorkDirection())));
    variables.put("original_level", text(staging.getOriginalLevelName()));
    variables.put("original_cert_no", text(staging.getOriginalCertNo()));
    variables.put("apply_work_type", text(firstNonBlank(staging.getApplyWorkTypeName(), staging.getCertWorkTypeName())));
    variables.put("apply_work_years", text(staging.getApplyWorkYears()));
    variables.put("apply_level", text(firstNonBlank(staging.getApplyLevelName(), staging.getCertLevelName())));
    variables.put("work_resume", text(staging.getWorkResume()));
    variables.put("company_code", text(staging.getCompanyCreditCode()));
    variables.put("cert_work_type", text(firstNonBlank(staging.getCertWorkTypeName(), staging.getApplyWorkTypeName())));
    variables.put("cert_level", text(firstNonBlank(staging.getCertLevelName(), staging.getApplyLevelName())));
    variables.put("work_type_code", text(staging.getWorkTypeCode()));
    variables.put("work_direction", text(staging.getWorkDirection()));
    variables.put("training_summary", text(staging.getTrainingSummary()));
    variables.put("remark", text(staging.getRemark()));
    variables.put("school", text(staging.getSchool()));
    variables.put("major", text(staging.getMajor()));
    variables.put("nation", text(staging.getNation()));
    variables.put("home_address", text(staging.getHomeAddress()));
    variables.put("apply_condition", text(staging.getApplyCondition()));
    variables.put("hr_opinion", "经审查，该同志填报简历情况属实，同意申报。");
    variables.put("integrity_declaration", "本人承诺以上内容真实有效，如有虚假，由此引发的一切后果由本人承担。");
  String applyWorkType = variables.get("apply_work_type");
    String applyLevel = variables.get("apply_level");
    String certWorkType = variables.get("cert_work_type");
    String certLevel = variables.get("cert_level");
    variables.put(
        "cert_conclusion",
        "经审查，该同志符合" + applyWorkType + applyLevel + "申报条件，且考试合格，同意认定为" + certWorkType + certLevel + "。");
    return variables;
  }

  private void replaceInDocument(XWPFDocument document, Map<String, String> variables) {
    for (XWPFParagraph paragraph : document.getParagraphs()) {
      replaceInParagraph(paragraph, variables);
    }
    for (XWPFTable table : document.getTables()) {
      for (XWPFTableRow row : table.getRows()) {
        for (XWPFTableCell cell : row.getTableCells()) {
          for (XWPFParagraph paragraph : cell.getParagraphs()) {
            replaceInParagraph(paragraph, variables);
          }
        }
      }
    }
  }

  private void replaceInParagraph(XWPFParagraph paragraph, Map<String, String> variables) {
    String original = paragraph.getText();
    if (!StringUtils.hasText(original)) {
      return;
    }
    String replaced = applyVariables(original, variables);
    if (original.equals(replaced)) {
      return;
    }
  List<XWPFRun> runs = paragraph.getRuns();
    if (runs == null || runs.isEmpty()) {
      paragraph.createRun().setText(replaced, 0);
      return;
    }
    for (int i = runs.size() - 1; i > 0; i--) {
      paragraph.removeRun(i);
    }
    runs.get(0).setText(replaced, 0);
  }

  private String applyVariables(String text, Map<String, String> variables) {
    String result = text;
    for (Map.Entry<String, String> entry : variables.entrySet()) {
      result = result.replace("{{" + entry.getKey() + "}}", entry.getValue());
    }
    return result;
  }

  private String text(String value) {
    return StringUtils.hasText(value) ? value : "";
  }

  private String firstNonBlank(String... values) {
    for (String value : values) {
      if (StringUtils.hasText(value)) {
        return value;
      }
    }
    return "";
  }

  private String formatDate(java.time.LocalDate date) {
    return date == null ? "" : date.format(DATE_FORMAT);
  }
}
