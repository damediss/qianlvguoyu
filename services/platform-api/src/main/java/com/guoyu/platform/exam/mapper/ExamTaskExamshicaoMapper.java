package com.guoyu.platform.exam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guoyu.platform.exam.entity.ExamTaskExamshicaoEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ExamTaskExamshicaoMapper extends BaseMapper<ExamTaskExamshicaoEntity> {

  /** 追加模式任务题数原子累加（操作技能试题生成 S13：禁止读-改-写，防并发发布丢更新） */
  @Update("UPDATE task_examshicao SET nums = nums + #{delta}, updated_at = NOW() WHERE id = #{taskId}")
  int incrementNums(@Param("taskId") Integer taskId, @Param("delta") int delta);
}
