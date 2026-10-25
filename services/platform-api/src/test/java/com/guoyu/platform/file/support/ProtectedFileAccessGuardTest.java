package com.guoyu.platform.file.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.file.FileStorage;
import com.guoyu.platform.file.LocalFileStorage;
import com.guoyu.platform.file.TencentCosFileStorage;
import com.guoyu.platform.file.config.FileStorageProperties;
import com.guoyu.platform.file.entity.FileEntity;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.CannedAccessControlList;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

/**
 * 受保护文件区闸门（方案 §12.1-ProtectedFileAccessGuardTest）：
 * protected/** 通用读取/删除/预签名默认拒绝、路径变形不可绕过、普通文件行为不变、
 * COS 受保护上传显式私有 ACL。
 */
class ProtectedFileAccessGuardTest {

  @Test
  @DisplayName("protected/** objectKey 识别 + 外部入口 404")
  void protectedObjectKeyDetectionAndRejection() {
    assertThat(ProtectedFileAccessGuard.isProtectedObjectKey("protected/textbook/10159/2026/10/x.png"))
        .isTrue();
    assertThat(ProtectedFileAccessGuard.isProtectedObjectKey("10159/2026/10/x.png")).isFalse();
    assertThat(ProtectedFileAccessGuard.isProtectedObjectKey("protected-x/1.png")).isFalse();
    assertThat(ProtectedFileAccessGuard.isProtectedObjectKey(null)).isFalse();

    FileEntity protectedFile = new FileEntity();
    protectedFile.setObjectKey("protected/textbook/10159/2026/10/x.png");
    assertThatThrownBy(() -> ProtectedFileAccessGuard.rejectExternalAccess(protectedFile))
        .isInstanceOf(BusinessException.class)
        .satisfies(ex -> assertThat(((BusinessException) ex).getCode())
            .isEqualTo(ErrorCode.NOT_FOUND));

    FileEntity normalFile = new FileEntity();
    normalFile.setObjectKey("10159/2026/10/x.png");
    assertThatCode(() -> ProtectedFileAccessGuard.rejectExternalAccess(normalFile))
        .doesNotThrowAnyException();
  }

  @Test
  @DisplayName("受保护文件元数据 url 必须为 null；普通文件 url 不变")
  void externalUrlNulledForProtectedOnly() {
    FileEntity protectedFile = new FileEntity();
    protectedFile.setObjectKey("protected/textbook/1/x.png");
    FileEntity normalFile = new FileEntity();
    normalFile.setObjectKey("1/2026/x.png");
    assertThat(ProtectedFileAccessGuard.externalUrlOrNull(protectedFile, "https://cos/x"))
        .isNull();
    assertThat(ProtectedFileAccessGuard.externalUrlOrNull(normalFile, "https://cos/x"))
        .isEqualTo("https://cos/x");
  }

  @Test
  @DisplayName("请求路径变形（%2F/反斜杠/../重复编码/非法转义）全部不可绕过")
  void requestPathVariantsRejected() {
    List<String> mustBeProtected = List.of(
        "protected/textbook/1/x.png",
        "protected%2Ftextbook%2F1%2Fx.png",
        "PROTECTED/textbook/1/x.png",
        "/protected/textbook/1/x.png",
        "..%2Fprotected%2Ftextbook",
        "..\\protected\\textbook\\x.png",
        "protected%252Ftextbook%2Fx.png",   // 双重编码
        "a/../../protected/textbook/x.png",
        "protected%2fTextbook%2Fx.png",
        "protected/%2e%2e/x.png");
    for (String path : mustBeProtected) {
      assertThat(ProtectedFileAccessGuard.isProtectedRequestPath(path))
          .as("变形路径必须按受保护处理：%s", path)
          .isTrue();
    }
    List<String> normalPaths = List.of(
        "10159/2026/10/uuid.png",
        "sign-up/2026/x.jpg",
        "down/export.zip",
        "idcard/upload/1.jpg");
    for (String path : normalPaths) {
      assertThat(ProtectedFileAccessGuard.isProtectedRequestPath(path))
          .as("普通路径不应被误伤：%s", path)
          .isFalse();
    }
  }

  @Test
  @DisplayName("LocalFileStorage.putProtected：写入受控目录并返回 null（不产生公开 URL）")
  void localPutProtectedWritesControlledDirectory(@TempDir Path tempDir) throws Exception {
    FileStorageProperties properties =
        new FileStorageProperties("local", tempDir.toString(), null, null);
    FileStorage storage = new LocalFileStorage(properties, "http://cdn.local/api/exam/files");

    String url = storage.putProtected("protected/textbook/10159/2026/10/x.png",
        new ByteArrayInputStream("png-bytes".getBytes()), 9, "image/png");

    assertThat(url).as("受保护对象不返回公开 URL").isNull();
    assertThat(Files.exists(tempDir.resolve("protected/textbook/10159/2026/10/x.png")))
        .isTrue();
  }

  @Test
  @DisplayName("COS putProtected：显式携带私有 ACL 且不返回公开 URL（真实桶公开策略属部署验收）")
  void cosPutProtectedSetsPrivateAcl() throws Exception {
    FileStorageProperties.Cos cos = new FileStorageProperties.Cos(
        "test-id", "test-key", "ap-guangzhou", "test-bucket", null);
    FileStorageProperties properties = new FileStorageProperties("cos",
        java.nio.file.Files.createTempDirectory("cos-mirror").toString(), null, cos);

    COSClient client = Mockito.mock(COSClient.class);
    PutObjectResult ok = new PutObjectResult();
    ok.setETag("etag");
    Mockito.when(client.putObject(Mockito.any(PutObjectRequest.class))).thenReturn(ok);
    TencentCosFileStorage storage = new TencentCosFileStorage(properties, client);

    String url = storage.putProtected("protected/textbook/1/2026/10/x.png",
        new ByteArrayInputStream("png".getBytes()), 3, "image/png");

    assertThat(url).as("受保护对象不返回公开 URL").isNull();
    ArgumentCaptor<PutObjectRequest> captor = ArgumentCaptor.forClass(PutObjectRequest.class);
    Mockito.verify(client).putObject(captor.capture());
    assertThat(captor.getValue().getCannedAcl())
        .as("受保护上传必须显式私有 ACL，不能仅靠 key 命名（§5.6-1）")
        .isEqualTo(CannedAccessControlList.Private);
  }
}
