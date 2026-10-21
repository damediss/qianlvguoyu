package com.guoyu.platform.textbook.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 更新教材工程：改名称/学时/系数/功能映射。
 * functionNos 或系数变化 → 后端自动重建骨架（保留 words_manual=1 节点的字数）。
 * status 传 outlined/reviewed 可做评审状态流转；空则不改动。
 */
public record ProjectUpdateReq(
    @Size(max = 128, message = "workTypeTitle 过长") String workTypeTitle,
    @Min(value = 1, message = "trainingHours 至少 1") Integer trainingHours,
    @Min(value = 100, message = "wordsPerHour 至少 100") Integer wordsPerHour,
    @Size(max = 50, message = "functionNos 过长") List<@Min(1) @Max(99) Integer> functionNos,
    String status
) {
}
