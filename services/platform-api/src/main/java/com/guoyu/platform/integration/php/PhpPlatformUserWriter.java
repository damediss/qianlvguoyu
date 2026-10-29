package com.guoyu.platform.integration.php;

import java.util.Optional;

/** PHP 库 platform_user 读写（开户双写）。 */
public interface PhpPlatformUserWriter {

  Optional<PhpUserRow> findByIdCard(String idCard);

  Optional<PhpUserRow> findByPhone(String phone, Long excludeId);

  /** INSERT，返回自增 id；password/stat 由调用方算好传入。 */
  long insert(PhpUserWrite cmd);

  /** 更新姓名/手机/org/company；禁止改 password/stat。 */
  void updateProfile(long id, PhpUserProfilePatch patch);

  record PhpUserRow(
      long id,
      String realname,
      String idcard,
      String phone,
      String password,
      String stat,
      Long orgId,
      Long companyId) {}

  record PhpUserWrite(
      String realname,
      String idcard,
      String phone,
      String password,
      String stat,
      long addtime,
      Long orgId,
      Long companyId) {}

  record PhpUserProfilePatch(
      String realname,
      String phone,
      Long orgId,
      Long companyId) {}
}
