package com.guoyu.platform.trainingdepartment.vo;

import java.util.List;

/** 班级人员成绩分页 + 整班考试统计 */
public record ExamScoreImportClassPeoplePageVO(
    List<ExamScoreImportClassPeopleVO> items,
    long page,
    long pageSize,
    long total,
    ExamScoreImportClassExamStatsVO stats) {}
