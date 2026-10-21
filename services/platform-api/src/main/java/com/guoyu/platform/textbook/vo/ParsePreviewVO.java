package com.guoyu.platform.textbook.vo;

import java.util.List;
import java.util.Map;

/**
 * 同步解析预览（不落库）：供新建向导回填。
 *
 * <p>levelCode 均为 API 口径（5=五级 … 1=一级）。
 *
 * @param guessedFunctionNos        方向映射猜测值（首等级首方向的序号数组，供职业功能勾选预选；
 *                                  解析失败为空数组，由表单人工勾选/登记）
 * @param guessedFunctionNosByLevel 各等级解析出的 方向→序号（key=levelCode，等级间映射可能不同；
 *                                  契约外附加字段，前端暂不使用）
 */
public record ParsePreviewVO(
    String workTypeTitle,
    String seriesNo,
    String standardVersion,
    List<String> directions,
    Map<String, List<Integer>> directionLevels,
    Map<Integer, Integer> trainingHoursByLevel,
    List<Integer> guessedFunctionNos,
    Map<Integer, Map<String, List<Integer>>> guessedFunctionNosByLevel,
    List<String> warnings
) {
}
