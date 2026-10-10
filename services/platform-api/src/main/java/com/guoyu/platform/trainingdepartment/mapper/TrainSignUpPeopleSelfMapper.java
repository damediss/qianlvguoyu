package com.guoyu.platform.trainingdepartment.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guoyu.platform.trainingdepartment.entity.TrainSignUpPeopleSelfEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface TrainSignUpPeopleSelfMapper extends BaseMapper<TrainSignUpPeopleSelfEntity> {

  /**
   * 同企业历史报名人员中出现最多的 6 位邮编（企业主数据缺 zip 时回退）。
   */
  @InterceptorIgnore(tenantLine = "true")
  @Select("""
      SELECT zip_code
      FROM train_sign_up_people_self
      WHERE deleted = 0
        AND zip_code REGEXP '^[0-9]{6}$'
        AND (
          company_name = #{companyName}
          OR work_exp LIKE CONCAT('%', #{companyName}, '%')
        )
      GROUP BY zip_code
      ORDER BY COUNT(*) DESC, zip_code ASC
      LIMIT 1
      """)
  String findMostCommonZipByCompanyName(@Param("companyName") String companyName);
}
