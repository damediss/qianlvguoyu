package com.guoyu.platform.chazheng.service;

import java.util.List;

/**
 * 单次查证查询结果（不落 chazheng 表）。
 */
public record CertQueryOutcome(
    boolean anyNormal,
    boolean anyCertFound,
    List<CollectedCert> certs
) {}
