package com.guoyu.platform.exam.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guoyu.platform.exam.entity.ExamKnowledgeStructureEntity;
import com.guoyu.platform.exam.mapper.ExamKnowledgeStructureMapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * 知识点路径解析器 — 通过 knowledge_id 递归查 exam_knowledge_structure 路径，
 * 返回末三级用 "-" 连接的路径字符串。
 */
@Component
public class KnowledgeStructurePathResolver {

  private final ExamKnowledgeStructureMapper knowledgeStructureMapper;

  public KnowledgeStructurePathResolver(ExamKnowledgeStructureMapper knowledgeStructureMapper) {
    this.knowledgeStructureMapper = knowledgeStructureMapper;
  }

  /**
   * 解析知识点的末三级路径。
   *
   * @param knowledgeId 知识点 ID（exam_exam_detail.knowledge_id）
   * @param tenantId    租户 ID
   * @return 末三级路径，如 "碳素工艺-煅烧炉-顺流罐式"；无关联返回 null
   */
  public String resolveLastThreeLevels(Integer knowledgeId, Long tenantId) {
    if (knowledgeId == null || knowledgeId <= 0) {
      return null;
    }

    // 一次性加载该租户下所有知识点，避免 N+1 查询
    List<ExamKnowledgeStructureEntity> all = knowledgeStructureMapper.selectList(
        new LambdaQueryWrapper<ExamKnowledgeStructureEntity>()
            .eq(ExamKnowledgeStructureEntity::getTenantId, tenantId.intValue()));

    Map<Integer, ExamKnowledgeStructureEntity> idMap = all.stream()
        .collect(Collectors.toMap(ExamKnowledgeStructureEntity::getId, Function.identity()));

    // 递归向上收集路径节点
    List<String> pathSegments = new ArrayList<>();
    ExamKnowledgeStructureEntity current = idMap.get(knowledgeId);
    int depth = 0;
    while (current != null && depth < 10) {
      pathSegments.add(current.getTitle());
      Integer parentId = current.getParentId();
      if (parentId == null || parentId <= 0) break;
      current = idMap.get(parentId);
      depth++;
    }

    if (pathSegments.isEmpty()) {
      return null;
    }

    // 反转得到从根到叶的顺序
    Collections.reverse(pathSegments);

    // 截取末三级
    int fromIndex = Math.max(0, pathSegments.size() - 3);
    List<String> lastThree = pathSegments.subList(fromIndex, pathSegments.size());
    return String.join("-", lastThree);
  }
}
