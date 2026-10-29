package com.guoyu.platform.trainingdepartment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guoyu.platform.integration.php.PhpPlatformUserWriter;
import com.guoyu.platform.integration.php.PhpPlatformUserWriter.PhpUserRow;
import com.guoyu.platform.integration.php.PhpPlatformUserWriter.PhpUserWrite;
import com.guoyu.platform.integration.php.UserProvisioningProperties;
import com.guoyu.platform.trainingdepartment.entity.PlatformUserEntity;
import com.guoyu.platform.trainingdepartment.mapper.PlatformUserDetailMapper;
import com.guoyu.platform.trainingdepartment.mapper.PlatformUserMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
class PlatformUserEnsureServiceTest {

  @Mock PlatformUserMapper mapper;
  @Mock PlatformUserDetailMapper detailMapper;
  @Mock PhpPlatformUserWriter phpWriter;
  @Mock JdbcTemplate jdbcTemplate;

  UserProvisioningProperties props;
  PlatformUserEnsureService service;

  @BeforeEach
  void setUp() {
    props = new UserProvisioningProperties();
    props.setPhpWriteEnabled(true);
    props.setFailSoft(true);
    service = new PlatformUserEnsureService(mapper, detailMapper, phpWriter, props, jdbcTemplate);
  }

  @Test
  void hashPassword_matchesPhpMd5Concat() throws Exception {
    MessageDigest digest = MessageDigest.getInstance("MD5");
    String expected = HexFormat.of().formatHex(
        digest.digest("123123abcd".getBytes(StandardCharsets.UTF_8)));
    assertThat(PlatformUserEnsureService.hashPassword("123123", "abcd")).isEqualTo(expected);
  }

  @Test
  void skipWhenMissingIdCard() {
    var result = service.ensureFromDivisionPerson("张三", null, "13800138000", 1L, 1L);
    assertThat(result.outcome()).isEqualTo(PlatformUserEnsureService.Outcome.SKIPPED);
    verify(phpWriter, never()).insert(any());
  }

  @Test
  void skipWhenMissingPhone() {
    var result = service.ensureFromDivisionPerson("张三", "110101199001011234", "", 1L, 1L);
    assertThat(result.outcome()).isEqualTo(PlatformUserEnsureService.Outcome.SKIPPED);
  }

  @Test
  void createWritesPhpThenMirrorsSaas() {
    when(phpWriter.findByIdCard("110101199001011234")).thenReturn(Optional.empty());
    when(phpWriter.findByPhone("13800138000", null)).thenReturn(Optional.empty());
    when(phpWriter.insert(any(PhpUserWrite.class))).thenReturn(12345L);
    when(mapper.selectList(any())).thenReturn(List.of());
    when(mapper.selectById(12345L)).thenReturn(null);
    when(detailMapper.selectOne(any())).thenReturn(null);

    var result = service.ensureFromDivisionPerson(
        "张三", "110101199001011234", "13800138000", 9L, 8L);

    assertThat(result.outcome()).isEqualTo(PlatformUserEnsureService.Outcome.CREATED);
    assertThat(result.platformUserId()).isEqualTo(12345L);

    ArgumentCaptor<PhpUserWrite> cap = ArgumentCaptor.forClass(PhpUserWrite.class);
    verify(phpWriter).insert(cap.capture());
    assertThat(cap.getValue().idcard()).isEqualTo("110101199001011234");
    assertThat(cap.getValue().password()).hasSize(32);
    verify(jdbcTemplate).update(
        any(String.class),
        eq(12345L),
        any(),
        any(),
        any(),
        any(),
        any(),
        anyLong(),
        any(),
        any());
  }

  @Test
  void reuseExistingPhpUserWithoutChangingPassword() {
    PhpUserRow existing = new PhpUserRow(
        99L, "李四", "110101199001011234", "13800138000", "oldhash", "zz", 1L, 2L);
    when(phpWriter.findByIdCard("110101199001011234")).thenReturn(Optional.of(existing));
    when(mapper.selectList(any())).thenReturn(List.of());
    PlatformUserEntity saas = new PlatformUserEntity();
    saas.setId(99L);
    saas.setIdcard("110101199001011234");
    when(mapper.selectById(99L)).thenReturn(saas);
    when(detailMapper.selectOne(any())).thenReturn(null);

    var result = service.ensureFromDivisionPerson(
        "李四", "110101199001011234", "13800138000", 2L, 1L);

    assertThat(result.outcome()).isEqualTo(PlatformUserEnsureService.Outcome.REUSED);
    assertThat(result.platformUserId()).isEqualTo(99L);
    verify(phpWriter, never()).insert(any());
    verify(phpWriter, never()).updateProfile(anyLong(), any());
  }

  @Test
  void legacySaasOnlyWhenPhpWriteDisabled() {
    props.setPhpWriteEnabled(false);
    when(mapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

    var result = service.ensureFromDivisionPerson(
        "王五", "110101199001011235", "13900139000", null, null);

    verify(phpWriter, never()).insert(any());
    verify(mapper).insert(any(PlatformUserEntity.class));
    assertThat(result.outcome()).isEqualTo(PlatformUserEnsureService.Outcome.CREATED);
  }

  @Test
  void skipPhoneConflictOnPhp() {
    when(phpWriter.findByIdCard("110101199001011234")).thenReturn(Optional.empty());
    when(phpWriter.findByPhone(eq("13800138000"), isNull()))
        .thenReturn(Optional.of(
            new PhpUserRow(1L, "他人", "110101199001019999", "13800138000", "h", "s", 0L, null)));

    var result = service.ensureFromDivisionPerson(
        "张三", "110101199001011234", "13800138000", null, null);

    assertThat(result.outcome()).isEqualTo(PlatformUserEnsureService.Outcome.SKIPPED);
    assertThat(result.warning()).contains("手机号");
    verify(phpWriter, never()).insert(any());
  }
}
