package com.guoyu.platform.chazheng.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guoyu.platform.chazheng.entity.ChazhengCertEntity;
import com.guoyu.platform.chazheng.entity.ChazhengUserEntity;
import com.guoyu.platform.chazheng.mapper.ChazhengCertMapper;
import com.guoyu.platform.chazheng.mapper.ChazhengUserMapper;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 查证结果持久化服务 —— 独立 Bean 解决自调用事务不生效问题。
 *
 * <p>{@link ChazhengApplicationService#certCheck} 需要调用 {@link #persistResult}，
 * 如果 {@code persistResult} 与 {@code certCheck} 在同一个 Bean 中，Spring AOP
 * 代理不会拦截内部调用，导致 {@code @Transactional} 注解失效。
 * 因此拆为独立 Bean，通过构造器注入调用。
 */
@Service
@Slf4j
public class ChazhengPersistService {

  private final ChazhengUserMapper userMapper;
  private final ChazhengCertMapper certMapper;

  public ChazhengPersistService(ChazhengUserMapper userMapper, ChazhengCertMapper certMapper) {
    this.userMapper = userMapper;
    this.certMapper = certMapper;
  }

  /**
   * 写入查证结果（增量去重）+ 更新用户状态。
   *
   * <p>证书写入采用增量模式：先 {@code exists} 检查 user_id + cert_no，
   * 不存在才 insert。并发场景下两个请求可能同时通过 exists 检查，
   * 此时依赖 DB 唯一索引 {@code uk_user_cert_no(user_id, cert_no)} 兜底，
   * 第二个 insert 会抛 {@link DuplicateKeyException}，catch 后跳过。
   *
   * <p>状态更新规则（对齐 PHP）：
   * <ul>
   *   <li>{@code isCert = 1}：只要任一轮查到证</li>
   *   <li>{@code isChecked = 1}：只要任一轮业务正常（有证或无证都算）；
   *       全部轮次异常时不更新，留待重试</li>
   * </ul>
   */
  @Transactional
  public void persistResult(ChazhengUserEntity user, List<CollectedCert> certs,
                            boolean anyNormal, boolean anyCertFound) {
    for (CollectedCert cert : certs) {
      ChazhengCertEntity existing = certMapper.selectOne(new LambdaQueryWrapper<ChazhengCertEntity>()
          .eq(ChazhengCertEntity::getUserId, user.getId())
          .eq(ChazhengCertEntity::getCertNo, cert.certNo()));
      if (existing == null) {
        ChazhengCertEntity certEntity = new ChazhengCertEntity();
        certEntity.setUserId(user.getId());
        certEntity.setTenantId(user.getTenantId());
        certEntity.setCertNo(cert.certNo());
        certEntity.setCertName(cert.certName());
        certEntity.setCertDate(cert.certDate());
        certEntity.setLicenseFileId(cert.licenseFileId());
        try {
          certMapper.insert(certEntity);
        } catch (DuplicateKeyException dup) {
          log.warn("并发插入证书已存在 user={} certNo={}", user.getId(), cert.certNo());
        }
      } else if (cert.licenseFileId() != null
          && !cert.licenseFileId().equals(existing.getLicenseFileId())) {
        // 重查补齐：旧行无图（或图变更）时用新查到的图片文件 ID 覆盖
        existing.setLicenseFileId(cert.licenseFileId());
        certMapper.updateById(existing);
      }
    }
    if (anyCertFound) user.setIsCert(1);
    if (anyNormal) user.setIsChecked(1);
    userMapper.updateById(user);
  }
}
