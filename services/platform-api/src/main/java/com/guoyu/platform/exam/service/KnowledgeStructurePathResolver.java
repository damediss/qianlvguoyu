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
  /**
   * 解析知识点的末三级路径（使用预加载的 Map，避免每条记录全表扫描）。
   *
   * @param knowledgeId   知识点 ID
   * @param knowledgeMap  知识点预加载映射（key = id）
   * @return 末三级路径，无关联返回 null
   */
  public String resolveLastThreeLevels(Integer knowledgeId,
      Map<Integer, ExamKnowledgeStructureEntity> knowledgeMap) {
    if (knowledgeId == null || knowledgeId <= 0 || knowledgeMap == null || knowledgeMap.isEmpty()) {
      return null;
    }
    return buildPath(knowledgeId, knowledgeMap);
  }

  /**
   * 解析知识点的末三级路径（兼容旧调用，单条查询会加载全表）。
   */
  public String resolveLastThreeLevels(Integer knowledgeId, Long tenantId) {
    if (knowledgeId == null || knowledgeId <= 0) {
      return null;
    }
    List<ExamKnowledgeStructureEntity> all = knowledgeStructureMapper.selectList(
        new LambdaQueryWrapper<ExamKnowledgeStructureEntity>()
            .eq(ExamKnowledgeStructureEntity::getTenantId, tenantId.intValue()));
    Map<Integer, ExamKnowledgeStructureEntity> idMap = all.stream()
        .collect(Collectors.toMap(ExamKnowledgeStructureEntity::getId, Function.identity()));
    return buildPath(knowledgeId, idMap);
  }

  private String buildPath(Integer knowledgeId, Map<Integer, ExamKnowledgeStructureEntity> knowledgeMap) {
    List<String> pathSegments = new ArrayList<>();
    ExamKnowledgeStructureEntity current = knowledgeMap.get(knowledgeId);
    int depth = 0;
    while (current != null && depth < 10) {
      pathSegments.add(current.getTitle());
      Integer parentId = current.getParentId();
      if (parentId == null || parentId <= 0) break;
      current = knowledgeMap.get(parentId);
      depth++;
    }

    if (pathSegments.isEmpty()) {
      return null;
    }

    Collections.reverse(pathSegments);
    int fromIndex = Math.max(0, pathSegments.size() - 3);
    List<String> lastThree = pathSegments.subList(fromIndex, pathSegments.size());
    return String.join("-", lastThree);
  }
}
