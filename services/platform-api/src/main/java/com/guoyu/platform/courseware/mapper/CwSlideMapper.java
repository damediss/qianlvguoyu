package com.guoyu.platform.courseware.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guoyu.platform.courseware.entity.CwSlideEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/** cw_slide Mapper */
@Mapper
public interface CwSlideMapper extends BaseMapper<CwSlideEntity> {

  /** 置为 generating 的乐观占位:仅 pending/failed/ready 可抢占,返回 1 表示抢成功 */
  @Update("UPDATE cw_slide SET status = 'generating', error = NULL "
      + "WHERE id = #{id} AND status IN ('pending','failed','ready')")
  int markGenerating(@Param("id") Long id);
}
