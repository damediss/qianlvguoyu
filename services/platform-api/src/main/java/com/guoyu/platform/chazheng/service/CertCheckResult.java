package com.guoyu.platform.chazheng.service;

import java.util.ArrayList;
import java.util.List;

/**
 * 单次查证方法的累积结果（跨多轮模板）。
 *
 * <p>由 {@link ChazhengApplicationService#certCheck} 内的多轮模板循环填充，
 * 最终传给 {@link ChazhengPersistService#persistResult} 执行 DB 写入。
 */
public class CertCheckResult {
  /** 任一轮业务正常（有证或无证都算正常） → isChecked=1 */
  public boolean anyNormal = false;
  /** 任一轮查到证 → isCert=1 */
  public boolean anyCertFound = false;
  /** 累积待写入的证书 */
  public final List<CollectedCert> certs = new ArrayList<>();
}
