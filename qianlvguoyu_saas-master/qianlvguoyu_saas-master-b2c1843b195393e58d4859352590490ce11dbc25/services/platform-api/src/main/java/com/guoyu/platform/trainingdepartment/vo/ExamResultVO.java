package com.guoyu.platform.trainingdepartment.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 考试结果视图对象 */
public record ExamResultVO(
    Long id,
    /** 题型 */
    String type,
    /** train_exam_list表id */
    Long examPlanId,
    /** 试卷id */
    Long paperId,
    /** 题目id */
    Long detailId,
    /** 获得分数 */
    BigDecimal getScore,
    /** 0正考 1模拟考 2能力测评 3顺序练习 4随机练习 */
    Integer examType,
    /** 配置试卷的主键 */
    Long examId,
    /** 用户ID */
    Long userId,
    Long platformUserId,
    /** 用户答案 */
    String userAnswer,
    /** 排序号 */
    Integer sortNumber,
    Long tenantId,
    LocalDateTime createTime
) {}
