package com.guoyu.platform.trainingdepartment.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** 槽位候选过滤：库里没有任何照片记录的人，不再盲试约定相对路径（只能靠远端 CDN 碰运气）。 */
class SignUpPeopleAssetCandidatesTest {

  @Test
  void recordPresentKeepsEveryCandidateInOrder() {
    List<String> candidates = SignUpPeopleDefaultTemplatePackService.withRecordCandidates(
        true,
        path -> false,
        "idCard/headerOrigin/x_header.jpg",
        "idCard/old/headerOrigin/x_full.png",
        "https://cdn.app.gjjd168.com/upload/common/x.jpg");

    assertThat(candidates).containsExactly(
        "idCard/headerOrigin/x_header.jpg",
        "idCard/old/headerOrigin/x_full.png",
        "https://cdn.app.gjjd168.com/upload/common/x.jpg");
  }

  @Test
  void withoutRecordOnlyLocallyPresentCandidatesSurvive() {
    Set<String> onDisk = Set.of("idCard/old/headerOrigin/x_full.png");

    List<String> candidates = SignUpPeopleDefaultTemplatePackService.withRecordCandidates(
        false,
        onDisk::contains,
        "idCard/headerOrigin/x_header.jpg",
        "idCard/old/headerOrigin/x_full.png",
        null);

    assertThat(candidates).containsExactly("idCard/old/headerOrigin/x_full.png");
  }

  @Test
  void withoutRecordAndNothingOnDiskDropsAllConventionPaths() {
    List<String> candidates = SignUpPeopleDefaultTemplatePackService.withRecordCandidates(
        false, path -> false, "idCard/headerOrigin/x_front.jpg", "idCard/old/headerOrigin/x_front.png");

    assertThat(candidates).isEmpty();
  }

  @Test
  void noCandidatesMeansNoWork() {
    List<String> candidates = SignUpPeopleDefaultTemplatePackService.withRecordCandidates(
        false, path -> false);

    assertThat(candidates).isEmpty();
  }

  @Test
  void exportWithRecordDoesNotFallBackToLegacyConventionPath() {
    List<String> candidates = SignUpPeopleDefaultTemplatePackService.exportImageCandidates(
        true,
        path -> true,
        new String[] {"/api/files/99/download?inline=true", null},
        "idCard/headerOrigin/452628198508162117_header.jpg",
        "idCard/old/headerOrigin/452628198508162117_full.png");

    // 有库记录时禁止回退旧约定路径，避免换图失败仍导出旧照
    assertThat(candidates).containsExactly("/api/files/99/download?inline=true");
  }

  @Test
  void exportWithoutStoredFallsBackToLocalLegacyOnly() {
    Set<String> onDisk = Set.of("idCard/headerOrigin/x_header.jpg");
    List<String> candidates = SignUpPeopleDefaultTemplatePackService.exportImageCandidates(
        false,
        onDisk::contains,
        new String[] {null, ""},
        "idCard/headerOrigin/x_header.jpg",
        "idCard/old/headerOrigin/x_full.png");

    assertThat(candidates).containsExactly("idCard/headerOrigin/x_header.jpg");
  }

  @Test
  void headerCandidatesPreferHeaderThenPhotoThenHeader2() {
    List<String> candidates = SignUpPeopleDefaultTemplatePackService.exportImageCandidates(
        true,
        path -> false,
        new String[] {
            "/api/files/1/download?inline=true",
            null,
            "/api/files/2/download?inline=true"
        },
        "idCard/headerOrigin/x_header.jpg");

    assertThat(candidates).containsExactly(
        "/api/files/1/download?inline=true",
        "/api/files/2/download?inline=true");
  }

  @Test
  void contractCandidatesMapLegacyApiAndHistoricalPlanId() {
    String stored =
        "https://api.gjjd168.com/api/file/contract/452624199804140704_80_first_self.png";
    List<String> candidates = SignUpPeopleDefaultTemplatePackService.exportContractCandidates(
        true,
        path -> false,
        stored,
        "452624199804140704",
        90055L,
        "first_self",
        "contact_first_page");

    assertThat(candidates).containsExactly(
        stored,
        "contract/452624199804140704_80_first_self.png",
        "contract/452624199804140704_80_first_self.jpg");
    assertThat(candidates).noneMatch(c -> c.contains("_90055_"));
  }

  @Test
  void contractWithoutRecordUsesCurrentSignUpConventionWhenOnDisk() {
    Set<String> onDisk = Set.of("contract/452624199804140704_90055_first_self.png");
    List<String> candidates = SignUpPeopleDefaultTemplatePackService.exportContractCandidates(
        false,
        onDisk::contains,
        null,
        "452624199804140704",
        90055L,
        "first_self",
        "contact_first_page");

    assertThat(candidates).containsExactly("contract/452624199804140704_90055_first_self.png");
  }

  @Test
  void paperCandidatesKeepStoredPlatformUrl() {
    List<String> candidates = SignUpPeopleDefaultTemplatePackService.exportImageCandidates(
        true,
        path -> false,
        new String[] {"/api/files/88/download?inline=true"});

    assertThat(candidates).containsExactly("/api/files/88/download?inline=true");
  }
}
