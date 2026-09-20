package com.guoyu.platform.courseware.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guoyu.platform.courseware.entity.CwProjectEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/** cw_project Mapper(计数走原子更新,多页并发完成时不丢) */
@Mapper
public interface CwProjectMapper extends BaseMapper<CwProjectEntity> {

  @Update("UPDATE cw_project SET ready_pages = ready_pages + 1 WHERE id = #{id}")
  int incrementReady(@Param("id") Long id);

  @Update("UPDATE cw_project SET failed_pages = failed_pages + 1 WHERE id = #{id}")
  int incrementFailed(@Param("id") Long id);

  @Update("UPDATE cw_project SET ready_pages = ready_pages - 1, failed_pages = failed_pages + 1 "
      + "WHERE id = #{id} AND ready_pages > 0")
  int moveReadyToFailed(@Param("id") Long id);
}
