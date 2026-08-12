package com.guoyu.platform.app.exam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guoyu.platform.app.exam.entity.AppExamMockPaperEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AppExamMockPaperMapper extends BaseMapper<AppExamMockPaperEntity> {

  @Select("SELECT IFNULL(MAX(id), 0) FROM exam_mock_paper")
  Integer selectMaxId();
}
