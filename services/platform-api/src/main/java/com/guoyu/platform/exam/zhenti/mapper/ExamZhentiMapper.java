package com.guoyu.platform.exam.zhenti.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guoyu.platform.exam.zhenti.entity.ExamZhentiEntity;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

public interface ExamZhentiMapper extends BaseMapper<ExamZhentiEntity> {

  /**
   * 删除归档：scope_key 追加行 id 后缀。
   *
   * <p>uk_zhenti_scope_md5 含 deleted 列（保留“删除后可重传”语义），同一文件
   * “删除→重传→再删”时软删 UPDATE 会与历史已删行撞键；归档 scope_key 后重试即通过。
   * 仅限 ZhentiAdminService.delete 的 DuplicateKeyException 兜底分支使用。
   */
  @Update("UPDATE exam_zhenti SET scope_key = CONCAT(scope_key, ':del:', id) WHERE id = #{id}")
  int archiveScopeKeyForDelete(@Param("id") Long id);
}
