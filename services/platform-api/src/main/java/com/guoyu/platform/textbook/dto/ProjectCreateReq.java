package com.guoyu.platform.textbook.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 创建教材工程（同步完成 解析→方向过滤→骨架→配比）。
 */
public record ProjectCreateReq(
    @NotNull(message = "fileId 不能为空") Long fileId,
    @NotEmpty(message = "direction 不能为空") String direction,
    @NotNull(message = "levelCode 不能为空")
    @Min(value = 1, message = "levelCode 为 5=五级 … 1=一级")
    @Max(value = 5, message = "levelCode 为 5=五级 … 1=一级")
    Integer levelCode,
    /** 人工登记的方向→职业功能序号；空则用解析映射（单方向工种全功能） */
    @Size(max = 50, message = "functionNos 过长") List<@Min(1) @Max(99) Integer> functionNos,
    /** 空则用解析 1.8 值 */
    @Min(value = 1, message = "trainingHours 至少 1") Integer trainingHours,
    /** 空则默认 2500 */
    @Min(value = 100, message = "wordsPerHour 至少 100") Integer wordsPerHour
) {
}
