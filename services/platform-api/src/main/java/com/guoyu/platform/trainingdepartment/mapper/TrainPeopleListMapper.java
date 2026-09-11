package com.guoyu.platform.trainingdepartment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.trainingdepartment.entity.TrainPeopleListEntity;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 开班人员列表表数据访问层
 */
public interface TrainPeopleListMapper extends BaseMapper<TrainPeopleListEntity> {

  /**
   * 查询可导入认定任务的培训学员。工种和等级取报名信息收集时的申报值，
   * 不依赖 train_people_list 的遗留冗余字段。
   */
  @Select("""
      <script>
      SELECT DISTINCT p.*
      FROM train_people_list p
      INNER JOIN train_sign_up_self su
        ON su.tenant_id = p.tenant_id
        AND su.train_info_id = p.train_id
        AND su.deleted = 0
      INNER JOIN train_sign_up_people_self sp
        ON sp.tenant_id = p.tenant_id
        AND sp.sign_up_id = su.id
        AND sp.deleted = 0
        AND ((p.platform_user_id IS NOT NULL AND sp.platform_user_id = p.platform_user_id)
          OR sp.id_card_number = p.id_card)
      WHERE p.tenant_id = #{tenantId}
        AND p.deleted = 0
        AND p.type = 'student'
        AND sp.req_work_type_name = #{applyWorkTypeName}
        AND sp.req_level = #{applyLevel}
      <if test="keyword != null and keyword != ''">
        AND (p.realname LIKE CONCAT('%', #{keyword}, '%')
          OR p.phone LIKE CONCAT('%', #{keyword}, '%')
          OR p.id_card LIKE CONCAT('%', #{keyword}, '%'))
      </if>
      <if test="companyId != null">
        AND (
          p.train_class_id IN (
            SELECT id FROM train_info_class WHERE company_id = #{companyId} AND deleted = 0)
          OR (
            p.train_class_id IS NULL
            AND p.train_id IN (
              SELECT id FROM train_info_list WHERE company_id = #{companyId} AND deleted = 0)
          )
        )
      </if>
      ORDER BY p.create_time DESC
      </script>
      """)
  IPage<TrainPeopleListEntity> selectCertificationImportPage(
      Page<TrainPeopleListEntity> page,
      @Param("tenantId") Long tenantId,
      @Param("applyWorkTypeName") String applyWorkTypeName,
      @Param("applyLevel") Integer applyLevel,
      @Param("keyword") String keyword,
      @Param("companyId") Long companyId);
}
