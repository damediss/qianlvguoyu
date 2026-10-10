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
}
