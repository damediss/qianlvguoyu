package com.guoyu.platform.exam.knowledgefix.dto;

/** 执行结果（备份表名 + 行数，供前端提示与回滚入口） */
public record ApplyResultVO(String backupTable, int approvedRows, int affectedRows) {
}
