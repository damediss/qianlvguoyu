package com.guoyu.platform.exam.zhenti.service;

/**
 * 真题共享组（方案 v2.1 §3.1）：(work_type_id, level)，level 用库内 0~4（0=五级，4=一级）。
 *
 * @param workTypeId 工种 id（exam_work_type 叶子节点）
 * @param level      等级 0~4
 */
public record ZhentiScope(Long workTypeId, int level) {
}
