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
 * @param functionNamesByLevel      各等级 职业功能序号→名称（工作要求表 L1，保持标准顺序；
 *                                  契约外附加字段，供前端勾选项显示真实功能名）
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
    Map<Integer, Map<Integer, String>> functionNamesByLevel,
    List<String> warnings
) {
}
