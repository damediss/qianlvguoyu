package com.guoyu.platform.textbook.vo;

/** C1 候选人（方案 §6.1）：不返回手机号等敏感字段；userId 字符串下发。 */
public record TextbookCollaboratorVO(
    String userId,
    String displayName,
    String username,
    String orgName) {
}
