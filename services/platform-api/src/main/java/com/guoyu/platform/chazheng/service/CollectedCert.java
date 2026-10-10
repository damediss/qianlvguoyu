package com.guoyu.platform.chazheng.service;

/**
 * 从接口单条证响应中提取的待写入数据。
 */
public record CollectedCert(String certNo, String certName, String certDate) {}
