package com.guoyu.platform.chazheng.service;

/**
 * 从接口单条证响应中提取的待写入数据。
 *
 * @param licenseFileId 证书图片文件ID（licensefile base64 解码存文件存储后的 ID），可能为 null
 */
public record CollectedCert(String certNo, String certName, String certDate, String licenseFileId) {}
