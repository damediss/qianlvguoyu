package com.guoyu.platform.trainingdepartment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guoyu.platform.trainingdepartment.entity.PlatformUserEntity;
import com.guoyu.platform.trainingdepartment.mapper.PlatformUserDetailMapper;
import com.guoyu.platform.trainingdepartment.mapper.PlatformUserMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * 单库化开户：platform_user 为合并库唯一权威表，仅主库 MP 直写（无 PHP 双写/镜像）。
 * dedup 口径：idcard/phone 升序首行。
 */
@ExtendWith(MockitoExtension.class)
class PlatformUserEnsureServiceTest {

  @Mock PlatformUserMapper mapper;
  @Mock PlatformUserDetailMapper detailMapper;

  PlatformUserEnsureService service;

  @BeforeEach
  void setUp() {
    service = new PlatformUserEnsureService(mapper, detailMapper);
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
    verify(mapper, never()).insert(any(PlatformUserEntity.class));
  }

  @Test
  void skipWhenMissingPhone() {
    var result = service.ensureFromDivisionPerson("张三", "110101199001011234", "", 1L, 1L);
    assertThat(result.outcome()).isEqualTo(PlatformUserEnsureService.Outcome.SKIPPED);
  }

  @Test
  void createInsertsIntoMainDbWithDefaultPassword() {
    when(mapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
    when(detailMapper.selectOne(any())).thenReturn(null);

    var result = service.ensureFromDivisionPerson(
        "张三", "110101199001011234", "13800138000", 9L, 8L);

    assertThat(result.outcome()).isEqualTo(PlatformUserEnsureService.Outcome.CREATED);
    ArgumentCaptor<PlatformUserEntity> cap = ArgumentCaptor.forClass(PlatformUserEntity.class);
    verify(mapper).insert(cap.capture());
    assertThat(cap.getValue().getIdcard()).isEqualTo("110101199001011234");
    assertThat(cap.getValue().getPassword()).hasSize(32);
    assertThat(cap.getValue().getCompanyId()).isEqualTo(9L);
    assertThat(cap.getValue().getOrgId()).isEqualTo(8L);
  }

  @Test
  void reuseExistingUserWithoutChangingPassword() {
    PlatformUserEntity existing = new PlatformUserEntity();
    existing.setId(99L);
    existing.setRealname("李四");
    existing.setIdcard("110101199001011234");
    existing.setPhone("13800138000");
    existing.setPassword("oldhash");
    when(mapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existing);

    var result = service.ensureFromDivisionPerson(
        "李四", "110101199001011234", "13800138000", null, null);

    assertThat(result.outcome()).isEqualTo(PlatformUserEnsureService.Outcome.REUSED);
    assertThat(result.platformUserId()).isEqualTo(99L);
    verify(mapper, never()).insert(any(PlatformUserEntity.class));
    verify(mapper, never()).updateById(any(PlatformUserEntity.class));
  }

  @Test
  void skipWhenPhoneOccupiedByOtherUser() {
    // idcard 查无 → 走新建分支；但手机号已被他人占用 → SKIPPED
    PlatformUserEntity phoneOwner = new PlatformUserEntity();
    phoneOwner.setId(7L);
    phoneOwner.setPhone("13800138000");
    when(mapper.selectOne(any(LambdaQueryWrapper.class)))
        .thenReturn(null)       // idcard dedup
        .thenReturn(phoneOwner); // phone dedup

    var result = service.ensureFromDivisionPerson(
        "张三", "110101199001011234", "13800138000", null, null);

    assertThat(result.outcome()).isEqualTo(PlatformUserEnsureService.Outcome.SKIPPED);
    assertThat(result.warning()).contains("手机号");
    verify(mapper, never()).insert(any(PlatformUserEntity.class));
  }

  @Test
  void updateFillsMissingProfile() {
    PlatformUserEntity existing = new PlatformUserEntity();
    existing.setId(42L);
    existing.setIdcard("110101199001011234");
    existing.setPhone("13800138000");
    when(mapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existing);

    var result = service.ensureFromDivisionPerson(
        "张三", "110101199001011234", "13800138000", 5L, 6L);

    assertThat(result.outcome()).isEqualTo(PlatformUserEnsureService.Outcome.UPDATED);
    assertThat(result.platformUserId()).isEqualTo(42L);
    verify(mapper).updateById(any(PlatformUserEntity.class));
    assertThat(existing.getRealname()).isEqualTo("张三");
    assertThat(existing.getCompanyId()).isEqualTo(5L);
    assertThat(existing.getOrgId()).isEqualTo(6L);
  }
}
