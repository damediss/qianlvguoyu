package com.guoyu.platform.trainingdepartment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.exam.mapper.ExamWorkTypeMapper;
import com.guoyu.platform.integration.php.UserProvisioningProperties;
import com.guoyu.platform.security.PlatformUserPrincipal;
import com.guoyu.platform.system.mapper.OrgMapper;
import com.guoyu.platform.system.mapper.UserMapper;
import com.guoyu.platform.trainingdepartment.entity.TrainInfoClassEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainSignUpPeopleSelfEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainSignUpSelfEntity;
import com.guoyu.platform.trainingdepartment.mapper.PlatformUserDetailMapper;
import com.guoyu.platform.trainingdepartment.mapper.PlatformUserMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainInfoClassMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainSignUpPeopleSelfMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainSignUpSelfMapper;
import com.guoyu.platform.trainingdepartment.service.ClassPrepRosterParseService.ParsedRoster;
import com.guoyu.platform.trainingdepartment.service.ClassPrepRosterParseService.ParsedRow;
import com.guoyu.platform.trainingdepartment.service.PlatformUserEnsureService.EnsureResult;
import com.guoyu.platform.trainingdepartment.service.PlatformUserEnsureService.Outcome;
import com.guoyu.platform.trainingdepartment.vo.SignUpPeopleImportResultVO;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ClassPrepRosterImportServiceTest {

  @Mock ClassPrepRosterParseService parseService;
  @Mock TrainSignUpSelfMapper signUpMapper;
  @Mock TrainSignUpPeopleSelfMapper peopleMapper;
  @Mock TrainInfoClassMapper classMapper;
  @Mock ExamWorkTypeMapper examWorkTypeMapper;
  @Mock OrgMapper orgMapper;
  @Mock UserMapper userMapper;
  @Mock PlatformUserMapper platformUserMapper;
  @Mock PlatformUserDetailMapper platformUserDetailMapper;
  @Mock PlatformUserEnsureService platformUserEnsureService;
  @Mock ClassCollectionApplicationService classCollectionApplicationService;
  @Mock TrainingOperatorScopeService operatorScopeService;
  @Mock UserProvisioningProperties userProvisioningProperties;

  ClassPrepRosterImportService service;

  @BeforeEach
  void setUp() {
    service = new ClassPrepRosterImportService(
        parseService,
        signUpMapper,
        peopleMapper,
        classMapper,
        examWorkTypeMapper,
        orgMapper,
        userMapper,
        platformUserMapper,
        platformUserDetailMapper,
        platformUserEnsureService,
        classCollectionApplicationService,
        operatorScopeService,
        userProvisioningProperties);
    PlatformUserPrincipal principal = new PlatformUserPrincipal(
        9L, 1L, "op", "op", "staff", List.of("ORG_ADMIN"), List.of("train:sign-up-people:create"), null);
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(principal, "n", principal.getAuthorities()));
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void importInsertsNewAndSkipsExistingAndFailsBadPhone() {
    stubClassPrepPlan();
    when(userProvisioningProperties.isPhpWriteEnabled()).thenReturn(true);
    when(parseService.parse(any())).thenReturn(new ParsedRoster(
        "新型学徒制培训学员花名册", "测企", "测校", "第3班", null,
        List.of(
            row(7, "张三", "452601198506065110", "15977634123"),
            row(8, "李四", "452625197509211474", "15777650405"),
            row(9, "王五", "45262519851008389X", "123"))));
    when(platformUserEnsureService.ensureFromDivisionPerson(anyString(), anyString(), anyString(), any(), any()))
        .thenAnswer(inv -> new EnsureResult(Outcome.CREATED, 1001L, null));

    TrainSignUpPeopleSelfEntity existing = new TrainSignUpPeopleSelfEntity();
    existing.setId(88L);
    existing.setSignUpId(10L);
    existing.setIdCardNumber("452625197509211474");
    existing.setName("旧名");
    existing.setPhone("15777650405");
    when(peopleMapper.selectOne(any()))
        .thenReturn(null)
        .thenReturn(existing)
        .thenReturn(null);

    SignUpPeopleImportResultVO result = service.importExcel(10L, file());

    assertThat(result.inserted()).isEqualTo(1);
    assertThat(result.skipped()).isEqualTo(1);
    assertThat(result.invalid()).isEqualTo(1);
    assertThat(result.errors().stream().anyMatch(e -> e.contains("手机号不是11位"))).isTrue();
    ArgumentCaptor<TrainSignUpPeopleSelfEntity> cap = ArgumentCaptor.forClass(TrainSignUpPeopleSelfEntity.class);
    verify(peopleMapper).insert(cap.capture());
    assertThat(cap.getValue().getName()).isEqualTo("张三");
    assertThat(cap.getValue().getDataSource()).isEqualTo(ClassPrepRosterImportService.DATA_SOURCE);
    assertThat(cap.getValue().getReqLevel()).isEqualTo(2);
    verify(classCollectionApplicationService, times(2)).syncPersonToRoster(any());
  }

  @Test
  void importRejectsNonClassPrepPlan() {
    TrainSignUpSelfEntity signUp = new TrainSignUpSelfEntity();
    signUp.setId(10L);
    signUp.setTenantId(1L);
    signUp.setDeleted(0);
    when(signUpMapper.selectOne(any())).thenReturn(signUp);
    when(classCollectionApplicationService.isClassPrep(signUp)).thenReturn(false);

    assertThatThrownBy(() -> service.importExcel(10L, file()))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("仅支持开班准备资料收集");
  }

  @Test
  void classLevelToPersonReqLevel() {
    assertThat(ClassPrepRosterImportService.classLevelToPersonReqLevel(5)).isEqualTo(0);
    assertThat(ClassPrepRosterImportService.classLevelToPersonReqLevel(3)).isEqualTo(2);
    assertThat(ClassPrepRosterImportService.classLevelToPersonReqLevel(1)).isEqualTo(4);
    assertThat(ClassPrepRosterImportService.classLevelToPersonReqLevel(null)).isNull();
  }

  private void stubClassPrepPlan() {
    TrainSignUpSelfEntity signUp = new TrainSignUpSelfEntity();
    signUp.setId(10L);
    signUp.setTenantId(1L);
    signUp.setOrgId(20L);
    signUp.setTrainClassId(30L);
    signUp.setCollectionSource(ClassCollectionApplicationService.SOURCE_CLASS_PREP);
    signUp.setDeleted(0);
    when(signUpMapper.selectOne(any())).thenReturn(signUp);
    when(classCollectionApplicationService.isClassPrep(signUp)).thenReturn(true);

    TrainInfoClassEntity clazz = new TrainInfoClassEntity();
    clazz.setId(30L);
    clazz.setCompanyId(20L);
    clazz.setWorkTypeId(7L);
    clazz.setWorkTypeLevel(3);
    clazz.setDeleted(0);
    when(classMapper.selectOne(any())).thenReturn(clazz);
    when(operatorScopeService.resolveCompanyFilter(any())).thenReturn(null);
  }

  private static ParsedRow row(int rowNo, String name, String idCard, String phone) {
    return new ParsedRow(
        rowNo, name, "男", "壮族", "高中", "群众",
        idCard, idCard, "某路", phone, phone, "4531",
        "2019-3-1至无固定期限", "2019-3-1", null, null,
        "企业职工-在岗农民工", null);
  }

  private static MockMultipartFile file() {
    return new MockMultipartFile("file", "roster.xlsx",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new byte[] {1, 2, 3});
  }
}
