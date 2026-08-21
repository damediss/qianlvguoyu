package com.guoyu.platform.trainingdepartment.vo;

/** 班级人员考试情况汇总（理论/技能均 ≥60 合格；两者皆无 = 缺考） */
public record ExamScoreImportClassExamStatsVO(
    int total,
    int passed,
    int failed,
    int absent,
    int theoryPass,
    int theoryFail,
    int theoryAbsent,
    int skillPass,
    int skillFail,
    int skillAbsent) {}
