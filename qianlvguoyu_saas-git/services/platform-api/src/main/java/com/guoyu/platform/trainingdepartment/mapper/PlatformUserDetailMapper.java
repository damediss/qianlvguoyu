package com.guoyu.platform.trainingdepartment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guoyu.platform.trainingdepartment.entity.PlatformUserDetailEntity;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PlatformUserDetailMapper extends BaseMapper<PlatformUserDetailEntity> {

  @Select("SELECT * FROM platform_user_detail WHERE idcard = #{idcard} AND deleted = 0 LIMIT 1")
  Map<String, Object> selectMapByIdcard(@Param("idcard") String idcard);
}
