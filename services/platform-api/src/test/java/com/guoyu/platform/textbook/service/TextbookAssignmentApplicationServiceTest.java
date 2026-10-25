package com.guoyu.platform.textbook.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.security.PlatformUserPrincipal;
import com.guoyu.platform.textbook.dto.TextbookAssignmentBatchReq;
import com.guoyu.platform.textbook.dto.TextbookAssignmentBatchReq.Item;
import com.guoyu.platform.textbook.entity.TextbookChapterAssignmentEntity;
import com.guoyu.platform.textbook.entity.TextbookOutlineNodeEntity;
import com.guoyu.platform.textbook.entity.TextbookProjectEntity;
import com.guoyu.platform.textbook.mapper.TextbookChapterAssignmentMapper;
import com.guoyu.platform.textbook.mapper.TextbookCollaboratorQueryMapper;
import com.guoyu.platform.textbook.mapper.TextbookOutlineNodeMapper;
import com.guoyu.platform.textbook.mapper.TextbookProjectMapper;
import com.guoyu.platform.textbook.service.TextbookProjectLockService.LockedChapter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 分配/改派/取消（方案 §12.1-TextbookAssignmentApplicationServiceTest）：
 * 预检拒绝矩阵、版本冲突 data 结构、取消后复用原行重分配。
 */
class TextbookAssignmentApplicationServiceTest {

  private static final long TENANT = 10159L;
  private static final long OPERATOR = 880_001L;
  private static final long WRITER = 880_101L;
  private static final long REVIEWER = 880_202L;

  private TextbookProjectMapper projectMapper;
  private TextbookOutlineNodeMapper nodeMapper;
  private TextbookChapterAssignmentMapper assignmentMapper;
  private TextbookCollaboratorQueryMapper collaboratorMapper;
  private TextbookProjectLockService lockService;
  private TextbookAccessService accessService;
  private TextbookProgressService progressService;
  private TextbookAssignmentApplicationService service;

  private final Map<Long, TextbookChapterAssignmentEntity> assignmentsDb = new HashMap<>();
  private final AtomicInteger inserts = new AtomicInteger();
  private TextbookProjectEntity project;

  @org.junit.jupiter.api.BeforeAll
  static void initMybatisPlusCache() {
    org.apache.ibatis.builder.MapperBuilderAssistant assistant =
        new org.apache.ibatis.builder.MapperBuilderAssistant(
            new com.baomidou.mybatisplus.core.MybatisConfiguration(), "");
    com.baomidou.mybatisplus.core.metadata.TableInfoHelper.initTableInfo(assistant,
        TextbookProjectEntity.class);
    com.baomidou.mybatisplus.core.metadata.TableInfoHelper.initTableInfo(assistant,
        TextbookChapterAssignmentEntity.class);
  }

  @BeforeEach
  void setUp() {
    projectMapper = mock(TextbookProjectMapper.class);
    nodeMapper = mock(TextbookOutlineNodeMapper.class);
    assignmentMapper = mock(TextbookChapterAssignmentMapper.class);
    collaboratorMapper = mock(TextbookCollaboratorQueryMapper.class);
    lockService = mock(TextbookProjectLockService.class);
    accessService = mock(TextbookAccessService.class);
    progressService = mock(TextbookProgressService.class);
    service = new TextbookAssignmentApplicationService(projectMapper, nodeMapper,
        assignmentMapper, null, null, collaboratorMapper, lockService, accessService,
        progressService, mock(TextbookCompletionChecker.class),
        mock(TextbookOutlineApplicationService.class),
        mock(TextbookSectionContentApplicationService.class));

    project = new TextbookProjectEntity();
    project.setId(1L);
    project.setTenantId(TENANT);
    project.setStatus(TextbookProjectApplicationService.STATUS_REVIEWED);

    when(lockService.lockProject(anyLong())).thenAnswer(inv -> project);
    when(lockService.lockAssignmentByChapter(anyLong()))
        .thenAnswer(inv -> assignmentsDb.get(inv.getArgument(0, Long.class)));
    when(assignmentMapper.insert(any(TextbookChapterAssignmentEntity.class)))
        .thenAnswer(inv -> {
          TextbookChapterAssignmentEntity entity = inv.getArgument(0);
          entity.setId(9000L + inserts.incrementAndGet());
          assignmentsDb.put(entity.getChapterId(), entity);
          return 1;
        });
    when(assignmentMapper.updateById(any(TextbookChapterAssignmentEntity.class)))
        .thenAnswer(inv -> {
          TextbookChapterAssignmentEntity entity = inv.getArgument(0);
          assignmentsDb.put(entity.getChapterId(), entity);
          return 1;
        });
    when(assignmentMapper.selectList(any())).thenReturn(List.of());
    when(collaboratorMapper.countUserPermissions(eq(WRITER), anyList()))
        .thenReturn(TextbookAccessService2.writerPerms().size());
    when(collaboratorMapper.countUserPermissions(eq(REVIEWER), anyList()))
        .thenReturn(2);
    // 章与节存在
    when(nodeMapper.selectOne(any())).thenAnswer(inv -> chapter(9001L));
    when(nodeMapper.selectCount(any())).thenReturn(2L);

    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(principal(), "token",
            principal().getAuthorities()));
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private static PlatformUserPrincipal principal() {
    return new PlatformUserPrincipal(OPERATOR, TENANT, "admin", "教研", "admin", List.of(),
        List.of("textbook:project:manage"), null);
  }

  private static TextbookOutlineNodeEntity chapter(long id) {
    TextbookOutlineNodeEntity chapter = new TextbookOutlineNodeEntity();
    chapter.setId(id);
    chapter.setTenantId(TENANT);
    chapter.setProjectId(1L);
    chapter.setParentId(0L);
    chapter.setNodeLevel(2);
    chapter.setTitle("铝电解槽操作");
    return chapter;
  }

  private static Item item(Long chapterId, Long writerId, Long reviewerId,
      Integer assignmentVersion) {
    return new Item(String.valueOf(chapterId), String.valueOf(writerId),
        String.valueOf(reviewerId), null, "结合现场", assignmentVersion);
  }

  @Test
  @DisplayName("正常分配：插入 version=1 active=1，reviewed → writing（条件更新）")
  void normalAssignmentInsertsAndMovesToWriting() {
    List<com.guoyu.platform.textbook.vo.TextbookAssignmentVO> result =
        service.assignBatch(1L, new TextbookAssignmentBatchReq(
            List.of(item(9001L, WRITER, REVIEWER, null))));
    assertEquals(1, result.size());
    assertEquals(1, result.get(0).assignmentVersion());
    assertTrue(result.get(0).active());
    TextbookChapterAssignmentEntity stored = assignmentsDb.get(9001L);
    assertEquals(1, stored.getAssignmentVersion());
    assertEquals(Integer.valueOf(1), stored.getActive());
    verify(projectMapper).update(any(), any());
    verify(progressService).refreshChapterSummary(9001L);
  }

  @Test
  @DisplayName("writer/reviewer 同人拒绝")
  void sameWriterAndReviewerRejected() {
    BusinessException ex = assertThrows(BusinessException.class,
        () -> service.assignBatch(1L, new TextbookAssignmentBatchReq(
            List.of(item(9001L, WRITER, WRITER, null)))));
    assertEquals(ErrorCode.BAD_REQUEST, ex.getCode());
    assertTrue(ex.getMessage().contains("同一人"));
  }

  @Test
  @DisplayName("无权限/禁用用户拒绝（服务端按权限码过滤）")
  void userWithoutPermissionRejected() {
    when(collaboratorMapper.countUserPermissions(anyLong(), anyList())).thenReturn(0);
    BusinessException ex = assertThrows(BusinessException.class,
        () -> service.assignBatch(1L, new TextbookAssignmentBatchReq(
            List.of(item(9001L, WRITER, REVIEWER, null)))));
    assertEquals(ErrorCode.BAD_REQUEST, ex.getCode());
    assertTrue(ex.getMessage().contains("不是启用中的教材编写专家"));
  }

  @Test
  @DisplayName("章不属于工程 / 章下无节拒绝")
  void chapterNotInProjectOrEmptyChapterRejected() {
    when(nodeMapper.selectOne(any())).thenReturn(null);
    BusinessException notInProject = assertThrows(BusinessException.class,
        () -> service.assignBatch(1L, new TextbookAssignmentBatchReq(
            List.of(item(9001L, WRITER, REVIEWER, null)))));
    assertEquals(ErrorCode.BAD_REQUEST, notInProject.getCode());

    when(nodeMapper.selectOne(any())).thenAnswer(inv -> chapter(9001L));
    when(nodeMapper.selectCount(any())).thenReturn(0L);
    BusinessException emptyChapter = assertThrows(BusinessException.class,
        () -> service.assignBatch(1L, new TextbookAssignmentBatchReq(
            List.of(item(9001L, WRITER, REVIEWER, null)))));
    assertTrue(emptyChapter.getMessage().contains("没有任何节"));
  }

  @Test
  @DisplayName("批量任一失败全回滚（预检在写库前完成，异常抛出由 @Transactional 回滚）")
  void batchFailsTogetherWhenAnyItemInvalid() {
    when(nodeMapper.selectCount(any())).thenReturn(2L);
    TextbookAssignmentBatchReq req = new TextbookAssignmentBatchReq(List.of(
        item(9001L, WRITER, REVIEWER, null),
        item(9002L, WRITER, WRITER, null))); // 第二项同人 → 预检失败
    assertThrows(BusinessException.class, () -> service.assignBatch(1L, req));
    assertEquals(0, assignmentsDb.size(), "预检全量前置：任一失败不产生任何写入");
  }

  @Test
  @DisplayName("改派版本冲突 → 409 ASSIGNMENT_VERSION_CONFLICT + currentAssignment")
  void reassignVersionConflict() {
    TextbookChapterAssignmentEntity existing = new TextbookChapterAssignmentEntity();
    existing.setId(7001L);
    existing.setChapterId(9001L);
    existing.setWriterId(WRITER);
    existing.setReviewerId(REVIEWER);
    existing.setActive(1);
    existing.setAssignmentVersion(3);
    assignmentsDb.put(9001L, existing);

    BusinessException ex = assertThrows(BusinessException.class,
        () -> service.assignBatch(1L, new TextbookAssignmentBatchReq(
            List.of(item(9001L, WRITER, REVIEWER, 2)))));
    assertEquals(ErrorCode.CONFLICT, ex.getCode());
    @SuppressWarnings("unchecked")
    Map<String, Object> data = (Map<String, Object>) ex.getData();
    assertEquals("ASSIGNMENT_VERSION_CONFLICT", data.get("reason"));
    assertTrue(data.get("currentAssignment") != null);
  }

  @Test
  @DisplayName("已取消（active=0）记录重新分配必须带当前版本")
  void inactiveReassignRequiresVersion() {
    TextbookChapterAssignmentEntity cancelled = new TextbookChapterAssignmentEntity();
    cancelled.setId(7001L);
    cancelled.setChapterId(9001L);
    cancelled.setWriterId(WRITER);
    cancelled.setReviewerId(REVIEWER);
    cancelled.setActive(0);
    cancelled.setAssignmentVersion(5);
    assignmentsDb.put(9001L, cancelled);

    BusinessException noVersion = assertThrows(BusinessException.class,
        () -> service.assignBatch(1L, new TextbookAssignmentBatchReq(
            List.of(item(9001L, WRITER, REVIEWER, null)))));
    assertEquals(ErrorCode.CONFLICT, noVersion.getCode());
    assertTrue(noVersion.getMessage().contains("版本冲突"));

    // 带正确版本：复用原行 active=1、version+1（不插新行）
    service.assignBatch(1L, new TextbookAssignmentBatchReq(
        List.of(item(9001L, WRITER, REVIEWER, 5))));
    assertEquals(1, assignmentsDb.size(), "重新分配复用原记录，不插新行");
    assertEquals(Integer.valueOf(1), cancelled.getActive());
    assertEquals(6, cancelled.getAssignmentVersion());
    verify(assignmentMapper, times(0)).insert(any(TextbookChapterAssignmentEntity.class));
  }

  @Test
  @DisplayName("C4 取消分配：active=0、version+1、返回取消后 VO")
  void cancelAssignmentSetsInactiveAndBumpsVersion() {
    TextbookChapterAssignmentEntity existing = new TextbookChapterAssignmentEntity();
    existing.setId(7001L);
    existing.setChapterId(9001L);
    existing.setWriterId(WRITER);
    existing.setReviewerId(REVIEWER);
    existing.setActive(1);
    existing.setAssignmentVersion(2);
    assignmentsDb.put(9001L, existing);
    when(lockService.lockByChapter(anyLong())).thenReturn(
        new LockedChapter(project, chapter(9001L), existing));

    var vo = service.cancelAssignment(9001L, 2);
    assertEquals(3, vo.assignmentVersion().intValue());
    assertTrue(!vo.active());
    assertEquals(Integer.valueOf(0), existing.getActive());

    // 版本不符 → 409
    BusinessException ex = assertThrows(BusinessException.class,
        () -> service.cancelAssignment(9001L, 2));
    assertEquals(ErrorCode.CONFLICT, ex.getCode());
  }

  /** 权限码集合常量复用（避免测试依赖服务私有字段）。 */
  static final class TextbookAccessService2 {
    static List<String> writerPerms() {
      return List.of(TextbookAccessService.PERM_TASK_VIEW,
          TextbookAccessService.PERM_CONTENT_WRITE);
    }
  }
}
