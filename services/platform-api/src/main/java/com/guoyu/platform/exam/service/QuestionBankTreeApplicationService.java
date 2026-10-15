package com.guoyu.platform.exam.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guoyu.platform.exam.entity.ExamCourseEntity;
import com.guoyu.platform.exam.entity.ExamKnowledgeStructureEntity;
import com.guoyu.platform.exam.entity.ExamWorkTypeEntity;
import com.guoyu.platform.exam.helper.ExamCourseLevels;
import com.guoyu.platform.exam.mapper.ExamCourseMapper;
import com.guoyu.platform.exam.mapper.ExamExamDetailMapper;
import com.guoyu.platform.exam.mapper.ExamKnowledgeStructureMapper;
import com.guoyu.platform.exam.mapper.ExamWorkTypeMapper;
import com.guoyu.platform.exam.vo.QuestionBankTreeNodeVO;
import com.guoyu.platform.security.SecurityContext;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 题库分类导航聚合树服务。
 *
 * <p>实现路径: 走 Path A (一次性全量返回 + Java 后序遍历累加)。
 *  <ol>
 *    <li>一次性查本租户所有 work_type (不过滤 status,与现有 page 行为一致)</li>
 *    <li>一次性查本租户所有 course (同样不过滤 status)</li>
 *    <li>一次性查本租户所有 status=1 启用的 knowledge_structure 节点</li>
 *    <li>一次性查本租户所有 exam_exam_detail 的 knowledge_id,聚合成 L3 节点 -> 题数 Map</li>
 *    <li>在 Java 侧建 6 级树 (workType/course/knowledge L0-L3),后序遍历把
 *        questionCount 累加到祖先</li>
 *  </ol>
 *
 * <p>为什么选 A 不选 C (MySQL 递归 CTE): 现有项目全用 MyBatis-Plus 的
 * LambdaQueryWrapper 模式,递归 CTE 需引入 @Select 注解或新建 XML mapper,破坏
 * 现有风格。租户规模 < 10k 节点,Java 侧汇总开销可忽略。
 */
@Service
@Slf4j
public class QuestionBankTreeApplicationService {

  private final ExamWorkTypeMapper workTypeMapper;
  private final ExamCourseMapper courseMapper;
  private final ExamKnowledgeStructureMapper knowledgeMapper;
  private final ExamExamDetailMapper examDetailMapper;

  public QuestionBankTreeApplicationService(
      ExamWorkTypeMapper workTypeMapper,
      ExamCourseMapper courseMapper,
      ExamKnowledgeStructureMapper knowledgeMapper,
      ExamExamDetailMapper examDetailMapper) {
    this.workTypeMapper = workTypeMapper;
    this.courseMapper = courseMapper;
    this.knowledgeMapper = knowledgeMapper;
    this.examDetailMapper = examDetailMapper;
  }

  /**
   * 暴露 tenantId 给 @Cacheable SpEL key 使用 (#root.target.tenantId()),
   * 与 ExamWorkTypeApplicationService.tenantId() 模式一致。
   */
  public Long tenantId() {
    return SecurityContext.currentUser().getTenantId();
  }

  /**
   * 构建题库分类导航全树。空租户返回 []。
   *
   * <p>缓存策略: 结果按 tenantId 缓存 30 分钟(见 CacheConfig#questionBankTree)。
   * 写操作(工种/课程/知识点/题目 的 create/update/delete/import)通过 @CacheEvict 主动失效。
   * 缓存命中时直接返回,不走 4 次 DB 查询 + Java 建树,接口延迟从 ~4s 降到 < 50ms。
   */
  @Cacheable(value = "questionBankTree", key = "#root.target.tenantId()")
  @Transactional(readOnly = true)
  public List<QuestionBankTreeNodeVO> buildTree() {
    Integer tenantId = SecurityContext.currentUser().getTenantId().intValue();

    // 1. 工种 (不过滤 status,与 ExamWorkTypeApplicationService.tree 行为一致)
    List<ExamWorkTypeEntity> workTypes = workTypeMapper.selectList(
        new LambdaQueryWrapper<ExamWorkTypeEntity>()
            .eq(ExamWorkTypeEntity::getTenantId, tenantId)
            .orderByAsc(ExamWorkTypeEntity::getSortNumber)
            .orderByAsc(ExamWorkTypeEntity::getId));
    if (workTypes == null || workTypes.isEmpty()) {
      log.debug("Question bank tree: tenant {} has no work types", tenantId);
      return new ArrayList<>();
    }

    // 2. 课程 (不过滤 status;work_type_id 限定到当前租户的工种集合,避免全表)
    List<Integer> workTypeIds = workTypes.stream().map(ExamWorkTypeEntity::getId).toList();
    List<ExamCourseEntity> courses = courseMapper.selectList(
        new LambdaQueryWrapper<ExamCourseEntity>()
            .in(ExamCourseEntity::getWorkTypeId, workTypeIds)
            .orderByAsc(ExamCourseEntity::getSortNumber)
            .orderByAsc(ExamCourseEntity::getId));

    // 3. 启用的知识点 (L0~L3 全部),course_id 限定到当前租户课程集合
    List<Integer> courseIds = courses == null
        ? Collections.emptyList()
        : courses.stream().map(ExamCourseEntity::getId).toList();
    List<ExamKnowledgeStructureEntity> knowledgeNodes = courseIds.isEmpty()
        ? Collections.emptyList()
        : knowledgeMapper.selectList(
            new LambdaQueryWrapper<ExamKnowledgeStructureEntity>()
                .in(ExamKnowledgeStructureEntity::getCourseId, courseIds)
                .eq(ExamKnowledgeStructureEntity::getStatus, 1)
                .orderByAsc(ExamKnowledgeStructureEntity::getSortNumber)
                .orderByAsc(ExamKnowledgeStructureEntity::getId));

    // 4. 题目按 knowledge_id 聚合成 (L3 knowledgeId -> count)
    //    只统计 L3 节点(题目挂载在 L3);知识节点的子树累加由 Java 侧处理
    //    走 MySQL 端 GROUP BY,不再拉所有 (id, knowledge_id) 到 Java 内存聚合。
    //    配套索引 idx_ed_tenant_knowledge_deleted (V20260610_005) 让查询走覆盖索引扫描。
    List<Map<String, Object>> rawCounts = examDetailMapper.countByKnowledgeIdGrouped(tenantId);
    Map<Integer, Long> l3Count = new HashMap<>();
    if (rawCounts != null) {
      for (Map<String, Object> row : rawCounts) {
        Object kidObj = row.get("knowledgeId");
        Object cntObj = row.get("cnt");
        if (kidObj == null || cntObj == null) continue;
        Integer kid = (kidObj instanceof Number) ? ((Number) kidObj).intValue() : null;
        Long cnt = (cntObj instanceof Number) ? ((Number) cntObj).longValue() : 0L;
        if (kid != null) {
          l3Count.put(kid, cnt);
        }
      }
    }

    // 5. 构建 6 级树
    //    a) knowledge 节点按 courseId 分组
    Map<Integer, List<ExamKnowledgeStructureEntity>> knowledgeByCourse = new LinkedHashMap<>();
    if (knowledgeNodes != null) {
      for (ExamKnowledgeStructureEntity k : knowledgeNodes) {
        knowledgeByCourse
            .computeIfAbsent(k.getCourseId(), x -> new ArrayList<>())
            .add(k);
      }
    }

    //    b) course 节点按 workTypeId 分组
    Map<Integer, List<ExamCourseEntity>> courseByWorkType = new LinkedHashMap<>();
    if (courses != null) {
      for (ExamCourseEntity c : courses) {
        courseByWorkType
            .computeIfAbsent(c.getWorkTypeId(), x -> new ArrayList<>())
            .add(c);
      }
    }

    //    c) 构造 VO 树
    List<QuestionBankTreeNodeVO> result = new ArrayList<>(workTypes.size());
    for (ExamWorkTypeEntity wt : workTypes) {
      QuestionBankTreeNodeVO wtNode = toWorkTypeNode(wt);
      List<ExamCourseEntity> wtCourses = courseByWorkType.getOrDefault(
          wt.getId(), Collections.emptyList());
      List<QuestionBankTreeNodeVO> courseNodes = new ArrayList<>(wtCourses.size());
      for (ExamCourseEntity c : wtCourses) {
        QuestionBankTreeNodeVO cNode = toCourseNode(c);
        List<ExamKnowledgeStructureEntity> ksList = knowledgeByCourse.getOrDefault(
            c.getId(), Collections.emptyList());
        cNode.setChildren(buildKnowledgeTree(ksList, c.getId()));
        courseNodes.add(cNode);
      }
      wtNode.setChildren(courseNodes);
      result.add(wtNode);
    }

    // 6. 后序遍历,把 questionCount 累加到祖先
    //    顺序: 知识叶子 (L3) -> L2 -> L1 -> L0 -> course -> workType
    for (QuestionBankTreeNodeVO wt : result) {
      for (QuestionBankTreeNodeVO c : wt.getChildren()) {
        for (QuestionBankTreeNodeVO kn : c.getChildren()) {
          accumulateCounts(kn, l3Count);
        }
        c.setQuestionCount(sumChildren(c));
      }
      wt.setQuestionCount(sumChildren(wt));
    }

    return result;
  }

  /**
   * 在 knowledge 子树内做后序累加:
   * <ol>
   *   <li>L3 节点直接取 l3Count 的值(可能为 0)</li>
   *   <li>非叶子节点 = sum(children.questionCount)</li>
   * </ol>
   */
  private void accumulateCounts(QuestionBankTreeNodeVO node, Map<Integer, Long> l3Count) {
    List<QuestionBankTreeNodeVO> children = node.getChildren();
    if (children == null || children.isEmpty()) {
      // L3 叶子;从题目统计里取,缺失则为 0
      long own = l3Count.getOrDefault(node.getId(), 0L);
      node.setQuestionCount(own);
      return;
    }
    for (QuestionBankTreeNodeVO child : children) {
      accumulateCounts(child, l3Count);
    }
    node.setQuestionCount(sumChildren(node));
  }

  private long sumChildren(QuestionBankTreeNodeVO node) {
    List<QuestionBankTreeNodeVO> children = node.getChildren();
    if (children == null) return 0L;
    long total = 0L;
    for (QuestionBankTreeNodeVO c : children) {
      total += c.getQuestionCount();
    }
    return total;
  }

  /** 把 knowledge 节点拼成 VO 子树,按 parentId 嵌套。 */
  private List<QuestionBankTreeNodeVO> buildKnowledgeTree(
      List<ExamKnowledgeStructureEntity> nodes, Integer courseId) {
    if (nodes == null || nodes.isEmpty()) {
      return new ArrayList<>();
    }
    Map<Integer, QuestionBankTreeNodeVO> map = new LinkedHashMap<>();
    Map<Integer, Integer> parentOf = new HashMap<>();
    for (ExamKnowledgeStructureEntity k : nodes) {
      QuestionBankTreeNodeVO vo = toKnowledgeNode(k, courseId);
      vo.setChildren(new ArrayList<>());
      map.put(vo.getId(), vo);
      parentOf.put(vo.getId(), k.getParentId());
    }
    List<QuestionBankTreeNodeVO> roots = new ArrayList<>();
    for (QuestionBankTreeNodeVO vo : map.values()) {
      Integer pid = parentOf.get(vo.getId());
      if (pid == null || pid == 0 || !map.containsKey(pid)) {
        roots.add(vo);
      } else {
        map.get(pid).getChildren().add(vo);
      }
    }
    return roots;
  }

  private QuestionBankTreeNodeVO toWorkTypeNode(ExamWorkTypeEntity e) {
    QuestionBankTreeNodeVO vo = new QuestionBankTreeNodeVO();
    vo.setKey("workType-" + e.getId());
    vo.setId(e.getId());
    vo.setNodeType("workType");
    vo.setTitle(e.getTitle());
    vo.setSortNumber(e.getSortNumber());
    vo.setStatus(e.getStatus());
    vo.setChildren(new ArrayList<>());
    return vo;
  }

  private QuestionBankTreeNodeVO toCourseNode(ExamCourseEntity e) {
    QuestionBankTreeNodeVO vo = new QuestionBankTreeNodeVO();
    vo.setKey("course-" + e.getId());
    vo.setId(e.getId());
    vo.setNodeType("course");
    Integer nationalGrade = ExamCourseLevels.toNationalGrade(e.getLevel());
    vo.setTitle(e.getTitle() != null
        ? e.getTitle() + (nationalGrade != null ? " (" + nationalGrade + "级)" : "")
        : null);
    vo.setSortNumber(e.getSortNumber());
    vo.setStatus(e.getStatus());
    vo.setWorkTypeId(e.getWorkTypeId());
    vo.setChildren(new ArrayList<>());
    return vo;
  }

  private QuestionBankTreeNodeVO toKnowledgeNode(ExamKnowledgeStructureEntity e, Integer courseId) {
    QuestionBankTreeNodeVO vo = new QuestionBankTreeNodeVO();
    vo.setKey("ks-" + e.getId());
    vo.setId(e.getId());
    vo.setNodeType("knowledge");
    vo.setTitle(e.getTitle());
    vo.setSortNumber(e.getSortNumber());
    vo.setStatus(e.getStatus());
    vo.setLevel(e.getLevel());
    vo.setCourseId(courseId);
    // children 始终初始化为空数组,前端永远拿到 [] 而非 null/undefined
    vo.setChildren(new ArrayList<>());
    return vo;
  }
}
