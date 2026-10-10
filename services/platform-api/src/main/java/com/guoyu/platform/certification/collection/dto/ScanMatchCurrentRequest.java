package com.guoyu.platform.certification.collection.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 平板端扫码匹配当前考生请求。token 可以是纯 token，也可以是二维码内容或采集链接，
 * 后端会做归一化后再定位考生。
 */
public record ScanMatchCurrentRequest(
    @NotBlank @Size(max = 512) String token
) {
}
