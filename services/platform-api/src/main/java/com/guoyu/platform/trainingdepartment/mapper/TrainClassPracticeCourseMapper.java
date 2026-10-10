package com.guoyu.platform.trainingdepartment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guoyu.platform.trainingdepartment.entity.TrainClassPracticeCourseEntity;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TrainClassPracticeCourseMapper extends BaseMapper<TrainClassPracticeCourseEntity> {

  /**
   * 全量替换白名单时必须物理删除：全局 logic-delete 会软删，
   * 与 uk_class_practice_course(tenant_id, class_id, exam_course_id) 冲突。
   */
  @Delete("""
      DELETE FROM train_class_practice_course
      WHERE tenant_id = #{tenantId}
        AND class_id = #{classId}
      """)
  int physicalDeleteByClass(@Param("tenantId") Long tenantId, @Param("classId") Long classId);
}
