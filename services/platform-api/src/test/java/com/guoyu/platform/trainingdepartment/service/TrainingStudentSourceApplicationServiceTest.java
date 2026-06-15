package com.guoyu.platform.trainingdepartment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.guoyu.platform.security.PlatformUserPrincipal;
import com.guoyu.platform.trainingdepartment.entity.TrainPeopleListEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainSignUpPeopleSelfEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainSignUpSelfEntity;
import com.guoyu.platform.trainingdepartment.mapper.TrainPeopleListMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainSignUpPeopleSelfMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainSignUpSelfMapper;
import com.guoyu.platform.trainingdepartment.vo.TrainingStudentCertificationSourceVO;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class TrainingStudentSourceApplicationServiceTest {

  private final TrainPeopleListMapper peopleListMapper = Mockito.mock(TrainPeopleListMapper.class);
  private final TrainSignUpSelfMapper signUpSelfMapper = Mockito.mock(TrainSignUpSelfMapper.class);
  private final TrainSignUpPeopleSelfMapper signUpPeopleMapper =
      Mockito.mock(TrainSignUpPeopleSelfMapper.class);

  private TrainingStudentSourceApplicationService service;

  @BeforeEach
  void setUp() {
    PlatformUserPrincipal principal = new PlatformUserPrincipal(
        10L, 1L, "admin", "Admin", "admin", List.of("PLATFORM_ADMIN"), List.of());
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(principal, "token", principal.getAuthorities()));
    service = new TrainingStudentSourceApplicationService(
        peopleListMapper, signUpSelfMapper, signUpPeopleMapper);
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void returnsClassStudentWithInformationCollectionSource() {
    TrainPeopleListEntity student = new TrainPeopleListEntity();
    student.setId(60321L);
    student.setTrainId(20008L);
    student.setTrainClassId(30009L);
    student.setType("student");
    student.setPlatformUserId(990001L);
    student.setRealname("测试学员甲");
    student.setPhone("13900009101");
    student.setIdCard("450102199001011012");

    TrainSignUpSelfEntity signUp = new TrainSignUpSelfEntity();
    signUp.setId(90008L);
    signUp.setTrainInfoId(20008L);

    TrainSignUpPeopleSelfEntity source = new TrainSignUpPeopleSelfEntity();
    source.setId(91011L);
    source.setSignUpId(90008L);
    source.setPlatformUserId(990001L);
    source.setName("测试学员甲");
    source.setPhone("13900009101");
    source.setIdCardNumber("450102199001011012");
    source.setReqWorkTypeName("焊工");
    source.setReqLevel(0);
    source.setCompanyName("演示公司");
    source.setIdCardFront("classpath:/test-data/certification/test-id-card-front.png");
    source.setIdCardBack("classpath:/test-data/certification/test-id-card-back.png");

    when(peopleListMapper.selectOne(any())).thenReturn(student);
    when(signUpSelfMapper.selectOne(any())).thenReturn(signUp);
    when(signUpPeopleMapper.selectOne(any())).thenReturn(source);

    TrainingStudentCertificationSourceVO result = service.requireCertificationSource(60321L);

    assertThat(result.getTrainingPersonId()).isEqualTo(60321L);
    assertThat(result.getSignUpId()).isEqualTo(90008L);
    assertThat(result.getSignUpPersonId()).isEqualTo(91011L);
    assertThat(result.getName()).isEqualTo("测试学员甲");
    assertThat(result.getReqWorkTypeName()).isEqualTo("焊工");
    assertThat(result.getReqLevel()).isZero();
    assertThat(result.getIdCardFront()).contains("test-id-card-front.png");
  }
}
