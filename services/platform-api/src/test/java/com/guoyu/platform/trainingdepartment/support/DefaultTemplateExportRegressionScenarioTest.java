package com.guoyu.platform.trainingdepartment.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.guoyu.platform.file.config.FileStorageProperties;
import com.guoyu.platform.file.service.FileApplicationService;
import com.guoyu.platform.file.support.CdnFileServeHelper;
import com.guoyu.platform.trainingdepartment.service.BaiduOcrService;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * 默认模板导出回归场景：A 换图不回退旧图 / B+D 合同映射历史 planId / C OCR 裁剪。
 */
class DefaultTemplateExportRegressionScenarioTest {

  private static final String ID = "452624199804140704";
  private static final String LEGACY_CONTRACT_URL =
      "https://api.gjjd168.com/api/file/contract/" + ID + "_80_first_self.png";

  @TempDir Path tempDir;

  private Path cdnRoot;
  private Path outDir;
  private SignUpExportAssetService assetService;
  private FileApplicationService fileApplicationService;
  private TrainingRemoteImageUtil remoteImageUtil;

  @BeforeEach
  void setUp() throws Exception {
    cdnRoot = Files.createDirectories(tempDir.resolve("cdn"));
    outDir = Files.createDirectories(tempDir.resolve("out"));
    remoteImageUtil = mock(TrainingRemoteImageUtil.class);
    fileApplicationService = mock(FileApplicationService.class);
    CdnFileServeHelper helper = new CdnFileServeHelper(
        new FileStorageProperties("local", tempDir.resolve("files").toString(), "", null),
        cdnRoot.toString(),
        "http://localhost:8080/api/exam/files",
        "https://cdn.app.gjjd168.com",
        "http://43.138.163.7/api/exam/files");
    assetService = new SignUpExportAssetService(
        helper,
        remoteImageUtil,
        fileApplicationService,
        "http://43.138.163.7:8080/api/exam/files",
        "http://43.138.163.7/api/exam/files",
        "https://api.gjjd168.com");
  }

  /** A：库字段有新 fileId，旧 headerOrigin 仍在盘上 → 必须取新图，且候选不含旧路径。 */
  @Test
  void scenarioA_replaceImageDoesNotFallBackToLegacyDisk() throws Exception {
    writeBytes(cdnRoot.resolve("idCard/headerOrigin/" + ID + "_header.jpg"), "OLD-HEADER");
    when(fileApplicationService.readFileBytesById(99L)).thenReturn("NEW-HEADER".getBytes());

    List<String> candidates = SignUpPeopleDefaultTemplatePackService.exportImageCandidates(
        true,
        assetService.newResolver()::existsLocally,
        new String[] {"/api/files/99/download?inline=true"},
        "idCard/headerOrigin/" + ID + "_header.jpg",
        "idCard/old/headerOrigin/" + ID + "_full.png");

    assertThat(candidates).containsExactly("/api/files/99/download?inline=true");
    assertThat(candidates).noneMatch(c -> c.contains("headerOrigin"));

    Path target = outDir.resolve("header.jpg");
    assertThat(assetService.newResolver().copyFirstExisting(target, candidates)).isTrue();
    assertThat(Files.readString(target)).isEqualTo("NEW-HEADER");
    verify(remoteImageUtil, never()).fetchImage(anyString());
  }

  /** A 残留：fileId 读取失败时不得静默命中旧盘同名约定路径（候选里已无 legacy）。 */
  @Test
  void scenarioA_fileIdMissLeavesSlotEmpty() throws Exception {
    writeBytes(cdnRoot.resolve("idCard/headerOrigin/" + ID + "_header.jpg"), "OLD-HEADER");
    when(fileApplicationService.readFileBytesById(anyLong())).thenReturn(null);
    when(fileApplicationService.resolveExportFetchUrl(anyLong())).thenReturn(Optional.empty());
    when(remoteImageUtil.fetchImage(anyString())).thenReturn(Optional.empty());

    List<String> candidates = SignUpPeopleDefaultTemplatePackService.exportImageCandidates(
        true,
        path -> true,
        new String[] {"/api/files/77/download"},
        "idCard/headerOrigin/" + ID + "_header.jpg");

    Path target = outDir.resolve("header-miss.jpg");
    assertThat(assetService.newResolver().copyFirstExisting(target, candidates)).isFalse();
    assertThat(Files.exists(target)).isFalse();
  }

  /** B+D：李丹式旧合同 URL → 映射 contract/_80_ 本机文件；不用当前班 90055。 */
  @Test
  void scenarioD_liDanContractMapsHistoricalPlanIdFromLocalDisk() throws Exception {
    writeBytes(cdnRoot.resolve("contract/" + ID + "_80_first_self.png"), "CONTRACT-80");
    writeBytes(cdnRoot.resolve("contract/" + ID + "_90055_first_self.png"), "CONTRACT-WRONG-CLASS");

    List<String> candidates = SignUpPeopleDefaultTemplatePackService.exportContractCandidates(
        true,
        assetService.newResolver()::existsLocally,
        LEGACY_CONTRACT_URL,
        ID,
        90055L,
        "first_self",
        "contact_first_page");

    assertThat(candidates).containsExactly(
        LEGACY_CONTRACT_URL,
        "contract/" + ID + "_80_first_self.png",
        "contract/" + ID + "_80_first_self.jpg");
    assertThat(candidates).noneMatch(c -> c.contains("_90055_"));

    Path target = outDir.resolve("contract.jpg");
    assertThat(assetService.newResolver().copyFirstExisting(target, candidates)).isTrue();
    assertThat(Files.readString(target)).isEqualTo("CONTRACT-80");
    verify(remoteImageUtil, never()).fetchImage(anyString());
  }

  /** B：HTTP 合同 URL 本机无文件时，仍可按映射相对路径尝试；解析链包含 mapped 路径。 */
  @Test
  void scenarioB_contractUrlMapsEvenWhenHttpWould404() {
    assertThat(SignUpExportAssetService.mapStoredToLocalRelative(LEGACY_CONTRACT_URL))
        .isEqualTo("contract/" + ID + "_80_first_self.png");
    assertThat(SignUpExportAssetService.parseContractSignUpId(LEGACY_CONTRACT_URL))
        .isEqualTo(80L);
  }

  /** C：OCR 启用时按 card_location 裁剪，导出图尺寸小于原图。 */
  @Test
  void scenarioC_idCardOcrCropShrinksImage() throws Exception {
    Path image = outDir.resolve("front.jpg");
    writeSolidJpeg(image, 400, 300, Color.LIGHT_GRAY);

    BaiduOcrService baidu = mock(BaiduOcrService.class);
    when(baidu.isEnabled()).thenReturn(true);
    Map<String, Object> loc = new LinkedHashMap<>();
    loc.put("left", 40);
    loc.put("top", 30);
    loc.put("width", 320);
    loc.put("height", 200);
    Map<String, Object> ocr = new LinkedHashMap<>();
    ocr.put("card_location", loc);
    when(baidu.recognizeIdCard(anyString(), isNull(), eq("front"))).thenReturn(ocr);

    IdCardExportOcrCropSupport support =
        new IdCardExportOcrCropSupport(baidu, new IdCardImageInsetCropper(0.04));
    support.cropIdCardInPlace(image, "front");

    BufferedImage out = ImageIO.read(image.toFile());
    assertThat(out.getWidth()).isLessThan(400);
    assertThat(out.getHeight()).isLessThan(300);
    verify(baidu).recognizeIdCard(anyString(), isNull(), eq("front"));
  }

  private static void writeBytes(Path path, String content) throws Exception {
    Files.createDirectories(path.getParent());
    Files.writeString(path, content);
  }

  private static void writeSolidJpeg(Path path, int w, int h, Color color) throws Exception {
    BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
    Graphics2D g = img.createGraphics();
    g.setColor(color);
    g.fillRect(0, 0, w, h);
    g.dispose();
    Files.createDirectories(path.getParent());
    ImageIO.write(img, "jpg", path.toFile());
  }
}
