package com.guoyu.platform.certification.collection;

import static org.assertj.core.api.Assertions.assertThat;

import com.guoyu.platform.certification.collection.application.PracticalCollectionApplicationService;
import com.guoyu.platform.certification.entity.CertificationCandidateEntity;
import com.guoyu.platform.certification.entity.CertificationTaskEntity;
import com.guoyu.platform.certification.entity.CertificationTaskExamSiteEntity;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.imageio.ImageIO;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
import org.apache.poi.xssf.usermodel.XSSFPicture;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.junit.jupiter.api.Test;

class PracticalCollectionTemplateExportSmokeTest {

  @Test
  void normalizedTemplateEmbedsPortraitAndCaptureImages() throws Exception {
    PracticalCollectionApplicationService service = new PracticalCollectionApplicationService(
        null, null, null, null, null, null, null, null, null, null, null, null, null, null, "");
    CertificationCandidateEntity candidate = new CertificationCandidateEntity();
    candidate.setCandidateName("张三");
    candidate.setIdCardNo("110101199001011234");
    candidate.setAdmissionTicketNo("20260001");
    CertificationTaskEntity task = new CertificationTaskEntity();
    task.setTaskName("模板烟测任务");
    task.setExamDate(java.time.LocalDate.of(2026, 7, 13));
    CertificationTaskExamSiteEntity site = new CertificationTaskExamSiteEntity();
    site.setAddress("测试考场");
    site.setStartTime("09:00");
    site.setEndTime("11:00");

    Class<?> formCandidateType = Class.forName(
        PracticalCollectionApplicationService.class.getName() + "$CollectionFormCandidate");
    Constructor<?> formCandidateConstructor = formCandidateType.getDeclaredConstructor(
        CertificationCandidateEntity.class, byte[].class, byte[].class, byte[].class);
    formCandidateConstructor.setAccessible(true);
    Object formCandidate = formCandidateConstructor.newInstance(
        candidate, png(Color.BLUE), png(Color.BLACK), png(Color.RED));
    Method build = PracticalCollectionApplicationService.class.getDeclaredMethod(
        "buildCollectionFormWorkbook", CertificationTaskEntity.class, CertificationTaskExamSiteEntity.class,
        Class.forName("com.guoyu.platform.certification.entity.CertificationTaskScopeEntity"), List.class);
    build.setAccessible(true);
    byte[] output = (byte[]) build.invoke(service, task, site, null, List.of(formCandidate));

    Path outputPath = Path.of("outputs/practical-collection-qa/generated-collection-form-test.xlsx");
    Files.createDirectories(outputPath.getParent());
    Files.write(outputPath, output);
    try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(output))) {
      assertThat(workbook.getNumberOfSheets()).isEqualTo(1);
      assertThat(workbook.getAllPictures()).hasSize(4);
      assertThat(workbook.getSheetAt(0).getRow(6).getCell(0).getStringCellValue()).isEqualTo("张三");
      assertThat(workbook.getSheetAt(0).getRow(6).getCell(1).getStringCellValue()).isEqualTo("实操");
      assertThat(workbook.getSheetAt(0).getRow(7).getCell(1).getStringCellValue())
          .isEqualTo("110101199001011234");
      assertThat(workbook.getSheetAt(0).getRow(8).getCell(1).getStringCellValue()).isEqualTo("20260001");
      XSSFDrawing drawing = ((XSSFSheet) workbook.getSheetAt(0)).getDrawingPatriarch();
      assertThat(drawing.getShapes()).hasSize(4);
      assertThat(((XSSFPicture) drawing.getShapes().get(0)).getPreferredSize().getFrom().getCol()).isEqualTo(0);
      assertThat(((XSSFPicture) drawing.getShapes().get(0)).getPreferredSize().getFrom().getRow()).isEqualTo(2);
      assertThat(((XSSFPicture) drawing.getShapes().get(1)).getPreferredSize().getFrom().getCol()).isEqualTo(1);
      assertThat(((XSSFPicture) drawing.getShapes().get(1)).getPreferredSize().getFrom().getRow()).isEqualTo(2);
      assertThat(((XSSFPicture) drawing.getShapes().get(2)).getPreferredSize().getFrom().getCol()).isEqualTo(2);
      assertThat(((XSSFPicture) drawing.getShapes().get(2)).getPreferredSize().getFrom().getRow()).isEqualTo(3);
      assertThat(((XSSFPicture) drawing.getShapes().get(3)).getPreferredSize().getFrom().getCol()).isEqualTo(2);
      assertThat(((XSSFPicture) drawing.getShapes().get(3)).getPreferredSize().getFrom().getRow()).isEqualTo(5);
      // 签名格为 1x1，嵌入图应按导出倍率保留明显高于屏幕单元格的像素，避免打印断笔。
      BufferedImage signaturePicture = ImageIO.read(new ByteArrayInputStream(
          ((XSSFPicture) drawing.getShapes().get(2)).getPictureData().getData()));
      assertThat(signaturePicture.getWidth()).isGreaterThanOrEqualTo(120);
      assertThat(signaturePicture.getHeight()).isGreaterThanOrEqualTo(40);
    }
  }

  @Test
  void multipleCandidatesSwitchToNextTemplateBlockBeforeCreatingNextSheet() throws Exception {
    PracticalCollectionApplicationService service = new PracticalCollectionApplicationService(
        null, null, null, null, null, null, null, null, null, null, null, null, null, null, "");
    CertificationTaskEntity task = new CertificationTaskEntity();
    task.setTaskName("多人换列测试任务");
    task.setExamDate(java.time.LocalDate.of(2026, 7, 13));
    CertificationTaskExamSiteEntity site = new CertificationTaskExamSiteEntity();
    site.setAddress("测试考场");
    site.setStartTime("09:00");
    site.setEndTime("11:00");

    Class<?> formCandidateType = Class.forName(
        PracticalCollectionApplicationService.class.getName() + "$CollectionFormCandidate");
    Constructor<?> formCandidateConstructor = formCandidateType.getDeclaredConstructor(
        CertificationCandidateEntity.class, byte[].class, byte[].class, byte[].class);
    formCandidateConstructor.setAccessible(true);
    List<Object> formCandidates = new java.util.ArrayList<>();
    for (int index = 1; index <= 10; index++) {
      CertificationCandidateEntity candidate = new CertificationCandidateEntity();
      candidate.setCandidateName("考生" + String.format("%02d", index));
      candidate.setIdCardNo("11010119900101" + String.format("%04d", index));
      candidate.setAdmissionTicketNo("202600" + String.format("%02d", index));
      formCandidates.add(formCandidateConstructor.newInstance(
          candidate, png(Color.BLUE), png(Color.BLACK), png(Color.RED)));
    }

    Method build = PracticalCollectionApplicationService.class.getDeclaredMethod(
        "buildCollectionFormWorkbook", CertificationTaskEntity.class, CertificationTaskExamSiteEntity.class,
        Class.forName("com.guoyu.platform.certification.entity.CertificationTaskScopeEntity"), List.class);
    build.setAccessible(true);
    byte[] output = (byte[]) build.invoke(service, task, site, null, formCandidates);
    Path outputPath = Path.of("outputs/practical-collection-qa/multi-candidate-collection-form-test.xlsx");
    Files.createDirectories(outputPath.getParent());
    Files.write(outputPath, output);

    try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(output))) {
      assertThat(workbook.getNumberOfSheets()).isEqualTo(2);
      assertThat(workbook.getAllPictures()).hasSize(40);
      assertThat(workbook.getPrintArea(0)).contains("$A$1:$I$23");
      assertThat(workbook.getPrintArea(1)).contains("$A$1:$I$23");
      assertThat(workbook.getSheetAt(0).getRow(6).getCell(0).getStringCellValue()).isEqualTo("考生01");
      assertThat(workbook.getSheetAt(0).getRow(13).getCell(0).getStringCellValue()).isEqualTo("考生04");
      assertThat(workbook.getSheetAt(0).getRow(20).getCell(0).getStringCellValue()).isEqualTo("考生07");
      assertThat(workbook.getSheetAt(1).getRow(6).getCell(0).getStringCellValue()).isEqualTo("考生10");
      assertThat(((XSSFSheet) workbook.getSheetAt(0)).getDrawingPatriarch().getShapes()).hasSize(36);
      assertThat(((XSSFSheet) workbook.getSheetAt(1)).getDrawingPatriarch().getShapes()).hasSize(4);
    }
  }

  private byte[] png(Color color) throws Exception {
    BufferedImage image = new BufferedImage(40, 40, BufferedImage.TYPE_INT_RGB);
    java.awt.Graphics2D graphics = image.createGraphics();
    graphics.setColor(Color.WHITE);
    graphics.fillRect(0, 0, 40, 40);
    graphics.setColor(color);
    graphics.fillRect(5, 5, 30, 30);
    graphics.dispose();
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    ImageIO.write(image, "png", output);
    return output.toByteArray();
  }
}
