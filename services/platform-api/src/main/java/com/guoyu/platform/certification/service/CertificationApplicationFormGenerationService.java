package com.guoyu.platform.certification.service;

import com.guoyu.platform.certification.entity.CertificationCandidateSourceStagingEntity;
import com.guoyu.platform.certification.support.IdCardSupport;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import javax.imageio.ImageIO;
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

  private final CertificationImageSourceService imageSourceService;

  public CertificationApplicationFormGenerationService(CertificationImageSourceService imageSourceService) {
    this.imageSourceService = imageSourceService;
  }

  public byte[] generateApplicationForm(CertificationCandidateSourceStagingEntity staging, byte[] portrait) {
    try (InputStream templateInput = new ClassPathResource(TEMPLATE_PATH).getInputStream()) {
      byte[] template = templateInput.readAllBytes();
      byte[] populatedTemplate = replaceTemplateImages(
          template,
          portrait,
          imageSourceService.readReference(staging.getIdCardFrontUrl()),
          imageSourceService.readReference(staging.getIdCardBackUrl()));
      try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(populatedTemplate));
        ByteArrayOutputStream output = new ByteArrayOutputStream()) {
        Map<String, String> variables = buildVariables(staging);
        replaceInDocument(document, variables);
        document.write(output);
        return output.toByteArray();
      }
    } catch (IOException ex) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "认定申请表模板生成失败");
    }
  }

  private byte[] replaceTemplateImages(
      byte[] template,
      byte[] portrait,
      byte[] idCardFront,
      byte[] idCardBack
  ) throws IOException {
    byte[] portraitPng = normalizeImage(portrait, "两寸证件照", "png", true, 600, 800);
    byte[] frontPng = normalizeImage(idCardFront, "身份证正面未提供", "png", false, 1200, 800);
    byte[] frontJpeg = normalizeImage(idCardFront, "身份证正面未提供", "jpeg", false, 1200, 800);
    byte[] backPng = normalizeImage(idCardBack, "身份证背面未提供", "png", false, 1200, 800);

    try (ZipInputStream zipInput = new ZipInputStream(new ByteArrayInputStream(template));
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ZipOutputStream zipOutput = new ZipOutputStream(output)) {
      ZipEntry entry;
      while ((entry = zipInput.getNextEntry()) != null) {
        ZipEntry copiedEntry = new ZipEntry(entry.getName());
        copiedEntry.setTime(entry.getTime());
        zipOutput.putNextEntry(copiedEntry);
        byte[] replacement = switch (entry.getName()) {
          case "word/media/image1.png" -> portraitPng;
          case "word/media/image2.png" -> frontPng;
          case "word/media/image3.jpeg" -> frontJpeg;
          case "word/media/image4.png" -> backPng;
          default -> null;
        };
        if (replacement == null) {
          zipInput.transferTo(zipOutput);
        } else {
          zipOutput.write(replacement);
        }
        zipOutput.closeEntry();
        zipInput.closeEntry();
      }
      zipOutput.finish();
      return output.toByteArray();
    }
  }

  private byte[] normalizeImage(
      byte[] content,
      String missingLabel,
      String format,
      boolean required,
      int maxWidth,
      int maxHeight
  )
      throws IOException {
    BufferedImage image = null;
    if (content != null && content.length > 0) {
      image = ImageIO.read(new ByteArrayInputStream(content));
    }
    if (image == null && required) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, missingLabel + "不是可识别的图片");
    }
    if (image == null) {
      image = placeholderImage(missingLabel);
    }
    image = resizeToFit(image, maxWidth, maxHeight);
    // Word / docx-preview 对 RGBA PNG 常显示为空白，统一压成白底 RGB
    BufferedImage outputImage = flattenToRgb(image);
    try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      if (!ImageIO.write(outputImage, format, output)) {
        throw new IOException("Unsupported image format: " + format);
      }
      return output.toByteArray();
    }
  }

  private BufferedImage resizeToFit(BufferedImage source, int maxWidth, int maxHeight) {
    double scale = Math.min(
        1D,
        Math.min((double) maxWidth / source.getWidth(), (double) maxHeight / source.getHeight()));
    if (scale >= 1D) {
      return source;
    }
    int width = Math.max(1, (int) Math.round(source.getWidth() * scale));
    int height = Math.max(1, (int) Math.round(source.getHeight() * scale));
    BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    Graphics2D graphics = resized.createGraphics();
    graphics.setColor(Color.WHITE);
    graphics.fillRect(0, 0, width, height);
    graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
    graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    graphics.drawImage(source, 0, 0, width, height, null);
    graphics.dispose();
    return resized;
  }

  private BufferedImage flattenToRgb(BufferedImage source) {
    if (source.getType() == BufferedImage.TYPE_INT_RGB) {
      return source;
    }
    BufferedImage rgb = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
    Graphics2D graphics = rgb.createGraphics();
    graphics.setColor(Color.WHITE);
    graphics.fillRect(0, 0, source.getWidth(), source.getHeight());
    graphics.drawImage(source, 0, 0, null);
    graphics.dispose();
    return rgb;
  }

  private BufferedImage placeholderImage(String label) {
    BufferedImage image = new BufferedImage(1200, 800, BufferedImage.TYPE_INT_RGB);
    Graphics2D graphics = image.createGraphics();
    graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    graphics.setColor(new Color(244, 247, 250));
    graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
    graphics.setColor(new Color(166, 176, 186));
    graphics.drawRoundRect(30, 30, image.getWidth() - 60, image.getHeight() - 60, 28, 28);
    graphics.setFont(new Font("Microsoft YaHei", Font.BOLD, 48));
    graphics.setColor(new Color(86, 98, 110));
    int textWidth = graphics.getFontMetrics().stringWidth(label);
    graphics.drawString(label, Math.max(40, (image.getWidth() - textWidth) / 2), image.getHeight() / 2);
    graphics.dispose();
    return image;
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
    // 人事意见与诚信声明为表单固定表述，不从学员导入数据生成。
    variables.put("hr_opinion", "经审查，该同志填报简历情况属实，同意申报。");
    variables.put("integrity_declaration", "本人承诺以上内容真实有效，如有虚假，由此引发的一切后果由本人承担。");
    String applyWorkType = variables.get("apply_work_type");
    String applyLevel = variables.get("apply_level");
    String certWorkType = variables.get("cert_work_type");
    String certLevel = variables.get("cert_level");
    // 认定结论依赖申报/认定工种与等级；导入数据不足时留空，避免模板样例残留。
    if (StringUtils.hasText(applyWorkType)
        && StringUtils.hasText(applyLevel)
        && StringUtils.hasText(certWorkType)
        && StringUtils.hasText(certLevel)) {
      variables.put(
          "cert_conclusion",
          "经审查，该同志符合"
              + applyWorkType
              + applyLevel
              + "申报条件，且考试合格，同意认定为"
              + certWorkType
              + certLevel
              + "。");
    } else {
      variables.put("cert_conclusion", "");
    }
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
