package com.guoyu.platform.certification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.certification.dto.PreparationCandidateSaveReq;
import com.guoyu.platform.certification.entity.CertificationCandidateSourceStagingEntity;
import com.guoyu.platform.certification.mapper.CertificationCandidateSourceStagingMapper;
import com.guoyu.platform.certification.vo.PreparationCandidateVO;
import com.guoyu.platform.security.PlatformUserPrincipal;
import com.guoyu.platform.trainingdepartment.service.TrainingStudentSourceApplicationService;
import com.guoyu.platform.trainingdepartment.vo.TrainingStudentCertificationSourceVO;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class CertificationCandidateStagingApplicationServiceTest {

  private final CertificationCandidateSourceStagingMapper stagingMapper =
      Mockito.mock(CertificationCandidateSourceStagingMapper.class);
  private final CertificationPreparationApplicationService preparationService =
      Mockito.mock(CertificationPreparationApplicationService.class);
  private final TrainingStudentSourceApplicationService trainingStudentSourceService =
      Mockito.mock(TrainingStudentSourceApplicationService.class);

  private CertificationCandidateStagingApplicationService service;

  @BeforeEach
  void setUp() {
    PlatformUserPrincipal principal = new PlatformUserPrincipal(
        10L,
        1L,
        "admin",
        "Admin",
        "admin",
        List.of("PLATFORM_ADMIN"),
        List.of("certification.preparation.candidate.import"));
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(principal, "token", principal.getAuthorities()));
    service = new CertificationCandidateStagingApplicationService(
        stagingMapper,
        preparationService,
        trainingStudentSourceService,
        new ObjectMapper());
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void promoteTrainingPersonCreatesCandidateAndReusableSourceSnapshot() {
    TrainingStudentCertificationSourceVO source = TrainingStudentCertificationSourceVO.builder()
        .trainingPersonId(88L)
        .trainId(100L)
        .trainClassId(200L)
        .signUpId(300L)
        .signUpPersonId(301L)
        .name("李敏")
        .idCardNumber("450102199001011234")
        .phone("13800001234")
        .reqWorkTypeId(400L)
        .reqWorkTypeName("电工")
        .reqLevel(0)
        .workTypeId(401L)
        .workTypeName("电工")
        .companyName("示例公司")
        .companyCode("91450100TEST")
        .companyAddress("南宁市测试路 1 号")
        .zipCode("530000")
        .education("大专")
        .school("示例职业学院")
        .major("电气自动化")
        .graduateDate("2012-06-30")
        .nation("汉族")
        .address("南宁市青秀区")
        .workDateStart("2013/7/1")
        .workTypePaperName("维修电工")
        .workTypePaperLevel(0)
        .workTypePaperNo("OLD-001")
        .declarationCondition("累计从事本职业工作满 10 年")
        .workExp("2013 年至今从事电气设备维护")
        .comments("培训学员导入")
        .build();
    when(trainingStudentSourceService.requireCertificationSource(88L)).thenReturn(source);
    when(stagingMapper.selectOne(any())).thenReturn(null);
    when(preparationService.createCandidate(any(), any())).thenReturn(new PreparationCandidateVO(
        900L,
        9L,
        "李敏",
        "450102199001011234",
        "450102********1234",
        "13800001234",
        "138****1234",
        "junior",
        "初级",
        "training",
        "示例公司",
        "active",
        0,
        2,
        false,
        0,
        2,
        2,
        null));

    PreparationCandidateVO result = service.promoteTrainingPerson(9L, 88L);

    assertThat(result.id()).isEqualTo(900L);
    ArgumentCaptor<PreparationCandidateSaveReq> candidateCaptor =
        ArgumentCaptor.forClass(PreparationCandidateSaveReq.class);
    verify(preparationService).createCandidate(org.mockito.ArgumentMatchers.eq(9L), candidateCaptor.capture());
    assertThat(candidateCaptor.getValue().levelCode()).isEqualTo("junior");
    assertThat(candidateCaptor.getValue().levelName()).isEqualTo("初级");
    assertThat(candidateCaptor.getValue().orgName()).isEqualTo("示例公司");

    ArgumentCaptor<CertificationCandidateSourceStagingEntity> snapshotCaptor =
        ArgumentCaptor.forClass(CertificationCandidateSourceStagingEntity.class);
    verify(stagingMapper).insert(snapshotCaptor.capture());
    CertificationCandidateSourceStagingEntity snapshot = snapshotCaptor.getValue();
    assertThat(snapshot.getCandidateId()).isEqualTo(900L);
    assertThat(snapshot.getSourceType()).isEqualTo("training_people");
    assertThat(snapshot.getExternalRefTable()).isEqualTo("train_sign_up_people_self");
    assertThat(snapshot.getExternalRefId()).isEqualTo(301L);
    assertThat(snapshot.getSignUpId()).isEqualTo(300L);
    assertThat(snapshot.getCandidateName()).isEqualTo("李敏");
    assertThat(snapshot.getGender()).isEqualTo("男");
    assertThat(snapshot.getBirthDate()).isEqualTo(LocalDate.of(1990, 1, 1));
    assertThat(snapshot.getGraduateDate()).isEqualTo(LocalDate.of(2012, 6, 30));
    assertThat(snapshot.getWorkStartDate()).isEqualTo(LocalDate.of(2013, 7, 1));
    assertThat(snapshot.getCompanyName()).isEqualTo("示例公司");
    assertThat(snapshot.getApplyWorkTypeName()).isEqualTo("电工");
    assertThat(snapshot.getApplyLevelName()).isEqualTo("初级");
    assertThat(snapshot.getOriginalLevelName()).isEqualTo("初级");
    assertThat(snapshot.getWorkResume()).contains("电气设备维护");
    assertThat(snapshot.getTrainingSummary()).isEqualTo("培训ID：100；班级ID：200；信息收集ID：300");
    assertThat(snapshot.getRawPayload()).contains("\"trainingPersonId\":88");
  }

  @Test
  void releasePromoteClearsCandidateLinkSoTrainingPersonCanBeImportedAgain() {
    service.releasePromoteByCandidate(9L, 900L);

    verify(stagingMapper).releasePromotedCandidate(1L, 9L, 900L);
  }
}
