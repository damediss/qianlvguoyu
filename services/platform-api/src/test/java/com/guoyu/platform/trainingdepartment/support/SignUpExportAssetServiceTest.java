package com.guoyu.platform.trainingdepartment.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.guoyu.platform.file.config.FileStorageProperties;
import com.guoyu.platform.file.service.FileApplicationService;
import com.guoyu.platform.file.support.CdnFileServeHelper;
import com.guoyu.platform.trainingdepartment.support.TrainingRemoteImageUtil.ImageBytes;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

/**
 * 导出素材解析：本机磁盘（cdn.path / file.local-root）优先，不回环请求本服务自己的
 * /api/exam/files（生产曾把该地址配成公网 IP:8080，每个候选白等 10s 连接超时）。
 */
class SignUpExportAssetServiceTest {

  private static final String CDN_URL = "http://43.138.163.7:8080/api/exam/files";
  private static final String PUBLIC_URL = "http://43.138.163.7/api/exam/files";
  private static final String REMOTE_FALLBACK = "https://cdn.app.gjjd168.com";

  @TempDir Path tempDir;

  private Path cdnRoot;
  private Path localRoot;
  private Path outDir;
  private TrainingRemoteImageUtil remoteImageUtil;
  private FileApplicationService fileApplicationService;
  private SignUpExportAssetService service;

  @BeforeEach
  void setUp() throws Exception {
    cdnRoot = Files.createDirectories(tempDir.resolve("cdn"));
    localRoot = Files.createDirectories(tempDir.resolve("files"));
    outDir = Files.createDirectories(tempDir.resolve("out"));
    remoteImageUtil = mock(TrainingRemoteImageUtil.class);
    fileApplicationService = mock(FileApplicationService.class);
    CdnFileServeHelper helper = new CdnFileServeHelper(
        new FileStorageProperties("local", localRoot.toString(), "", null),
        cdnRoot.toString(),
        "http://localhost:8080/api/exam/files",
        REMOTE_FALLBACK,
        PUBLIC_URL);
    service = new SignUpExportAssetService(
        helper, remoteImageUtil, fileApplicationService, CDN_URL, PUBLIC_URL);
  }

  @Test
  void copiesFromFileLocalRootWithoutHttp() throws Exception {
    writeFile(localRoot.resolve("idCard/headerOrigin/452126198602100000_header.jpg"), "local-root");

    Path target = outDir.resolve("header.jpg");
    assertThat(service.copyTo("idCard/headerOrigin/452126198602100000_header.jpg", target)).isTrue();
    assertThat(Files.readString(target)).isEqualTo("local-root");
    verifyNoInteractions(remoteImageUtil);
  }

  @Test
  void copiesFromCdnRootAndIgnoresQueryString() throws Exception {
    writeFile(cdnRoot.resolve("idCard/old/headerOrigin/452126198602100000_full.png"), "cdn-root");

    Path target = outDir.resolve("full.png");
    assertThat(service.copyTo("idCard/old/headerOrigin/452126198602100000_full.png?v=7", target))
        .isTrue();
    assertThat(Files.readString(target)).isEqualTo("cdn-root");
    verifyNoInteractions(remoteImageUtil);
  }

  /** 回归：本地没有的候选只允许去远端 CDN，绝不能再请求本服务自己的 :8080 地址。 */
  @Test
  void missingRelativeCandidateOnlyFallsBackToRemoteCdn() throws Exception {
    when(remoteImageUtil.fetchImage(anyString())).thenReturn(Optional.empty());

    Path target = outDir.resolve("header.jpg");
    assertThat(service.copyTo("idCard/headerOrigin/452126198602100000_header.jpg", target)).isFalse();

    List<String> fetched = capturedUrls(1);
    assertThat(fetched).containsExactly(REMOTE_FALLBACK + "/idCard/headerOrigin/452126198602100000_header.jpg");
    assertThat(fetched).noneMatch(url -> url.contains(":8080"));
  }

  @Test
  void selfFileUrlFromDatabaseResolvesOnDiskWithoutHttp() throws Exception {
    writeFile(cdnRoot.resolve("idCard/headerOrigin/452126198602100000_header.jpg"), "self-url");

    Path target = outDir.resolve("header.jpg");
    assertThat(service.copyTo(CDN_URL + "/idCard/headerOrigin/452126198602100000_header.jpg", target))
        .isTrue();
    assertThat(Files.readString(target)).isEqualTo("self-url");
    // 公网入口（Nginx 80）形式同样按相对路径查盘
    assertThat(service.copyTo(PUBLIC_URL + "/idCard/headerOrigin/452126198602100000_header.jpg",
        outDir.resolve("header2.jpg"))).isTrue();
    verifyNoInteractions(remoteImageUtil);
  }

  /** sign-up/** 的远端回退指向本机 public-url，服务端再拉一次也没有新内容，直接判失败。 */
  @Test
  void selfFallbackUrlIsNotFetched() throws Exception {
    Path target = outDir.resolve("qrcode.png");
    assertThat(service.copyTo("sign-up/qrcode/2026-09-15/a.png", target)).isFalse();
    verifyNoInteractions(remoteImageUtil);
  }

  @Test
  void externalUrlIsFetchedThroughRemoteUtil() throws Exception {
    String url = REMOTE_FALLBACK + "/upload/common/20260807/905272130006a753bd7adbcb.jpg";
    when(remoteImageUtil.fetchImage(url))
        .thenReturn(Optional.of(new ImageBytes("remote".getBytes(), ".jpg")));

    Path target = outDir.resolve("photo.jpg");
    assertThat(service.copyTo(url, target)).isTrue();
    assertThat(Files.readString(target)).isEqualTo("remote");
  }

  /** 同一次导出内同一候选只探测一次（名单里同一人可能出现多行）。 */
  @Test
  void missIsCachedPerRun() throws Exception {
    when(remoteImageUtil.fetchImage(anyString())).thenReturn(Optional.empty());
    SignUpExportAssetService.AssetResolver resolver = service.newResolver();

    assertThat(resolver.copyTo("idCard/headerOrigin/452126198602100000_front.jpg",
        outDir.resolve("a.jpg"))).isFalse();
    assertThat(resolver.copyTo("idCard/headerOrigin/452126198602100000_front.jpg",
        outDir.resolve("b.jpg"))).isFalse();

    verify(remoteImageUtil, times(1)).fetchImage(anyString());

    // 换一个解析器（下一次导出）才会重新探测
    service.copyTo("idCard/headerOrigin/452126198602100000_front.jpg", outDir.resolve("c.jpg"));
    verify(remoteImageUtil, times(2)).fetchImage(anyString());
  }

  @Test
  void fileApiIdCopiesThroughFileService() throws Exception {
    when(fileApplicationService.readFileBytesById(20260915L)).thenReturn("by-id".getBytes());

    Path target = outDir.resolve("paper.jpg");
    assertThat(service.copyTo("/api/files/20260915/download", target)).isTrue();
    assertThat(Files.readString(target)).isEqualTo("by-id");
    verify(remoteImageUtil, never()).fetchImage(anyString());
  }

  @Test
  void blankSourceOrTargetIsIgnored() {
    assertThat(service.copyTo("", outDir.resolve("x.jpg"))).isFalse();
    assertThat(service.copyTo("idCard/headerOrigin/x.jpg", null)).isFalse();
  }

  @Test
  void directoryTraversalRelativePathIsRejected() {
    assertThat(service.copyTo("../../etc/passwd", outDir.resolve("x.jpg"))).isFalse();
    verifyNoInteractions(remoteImageUtil);
  }

  private List<String> capturedUrls(int expectedCalls) {
    ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
    verify(remoteImageUtil, times(expectedCalls)).fetchImage(captor.capture());
    return captor.getAllValues();
  }

  private static void writeFile(Path path, String content) throws Exception {
    Files.createDirectories(path.getParent());
    Files.writeString(path, content);
  }
}
