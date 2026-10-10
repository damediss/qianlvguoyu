package com.guoyu.platform.certification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.guoyu.platform.certification.entity.CertificationCandidateSourceStagingEntity;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.core.io.ClassPathResource;

class CertificationApplicationFormGenerationServiceTest {

  @Test
  void generateApplicationFormReplacesAllTemplateIdentityImages() throws Exception {
    byte[] front = resourceBytes("test-data/certification/test-id-card-front.png");
    byte[] back = resourceBytes("test-data/certification/test-id-card-back.png");
    CertificationImageSourceService imageSourceService = Mockito.mock(CertificationImageSourceService.class);
    when(imageSourceService.readReference("classpath:/front.png")).thenReturn(front);
    when(imageSourceService.readReference("classpath:/back.png")).thenReturn(back);
    CertificationApplicationFormGenerationService service =
        new CertificationApplicationFormGenerationService(imageSourceService);

    CertificationCandidateSourceStagingEntity staging = new CertificationCandidateSourceStagingEntity();
    staging.setCandidateName("测试学员");
    staging.setIdCardNo("450102199001011001");
    staging.setBirthDate(LocalDate.of(1990, 1, 1));
    staging.setIdCardFrontUrl("classpath:/front.png");
    staging.setIdCardBackUrl("classpath:/back.png");

    byte[] document = service.generateApplicationForm(staging, front);
    Path qaDirectory = Path.of("target", "qa");
    Files.createDirectories(qaDirectory);
    Files.write(qaDirectory.resolve("certification-form-image-test.docx"), document);
    Map<String, byte[]> media = unzipMedia(document);

    assertThat(media).containsKeys(
        "word/media/image1.png",
        "word/media/image2.png",
        "word/media/image3.jpeg",
        "word/media/image4.png");
    assertImageSize(media.get("word/media/image1.png"), 600, 400);
    assertImageSize(media.get("word/media/image2.png"), 1200, 800);
    assertImageSize(media.get("word/media/image3.jpeg"), 1200, 800);
    assertImageSize(media.get("word/media/image4.png"), 1200, 800);
  }

  private byte[] resourceBytes(String path) throws Exception {
    try (InputStream input = new ClassPathResource(path).getInputStream()) {
      return input.readAllBytes();
    }
  }

  private Map<String, byte[]> unzipMedia(byte[] document) throws Exception {
    Map<String, byte[]> media = new HashMap<>();
    try (ZipInputStream input = new ZipInputStream(new ByteArrayInputStream(document))) {
      ZipEntry entry;
      while ((entry = input.getNextEntry()) != null) {
        if (entry.getName().startsWith("word/media/")) {
          ByteArrayOutputStream output = new ByteArrayOutputStream();
          input.transferTo(output);
          media.put(entry.getName(), output.toByteArray());
        }
        input.closeEntry();
      }
    }
    return media;
  }

  private void assertImageSize(byte[] content, int width, int height) throws Exception {
    BufferedImage image = ImageIO.read(new ByteArrayInputStream(content));
    assertThat(image).isNotNull();
    assertThat(image.getWidth()).isEqualTo(width);
    assertThat(image.getHeight()).isEqualTo(height);
  }
}
