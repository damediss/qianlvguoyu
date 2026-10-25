package com.guoyu.platform.textbook.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.textbook.dto.ProjectUpdateReq;
import com.guoyu.platform.textbook.entity.TextbookProjectEntity;
import com.guoyu.platform.textbook.mapper.TextbookChapterAssignmentMapper;
import com.guoyu.platform.textbook.mapper.TextbookSectionContentMapper;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 结构冻结 + 旧 PUT 状态白名单（方案 §12.1-TextbookStructureGuardTest）。
 */
class TextbookStructureGuardTest {

  private TextbookChapterAssignmentMapper assignmentMapper;
  private TextbookSectionContentMapper contentMapper;
  private TextbookStructureGuard guard;

  @BeforeEach
  void setUp() {
    assignmentMapper = mock(TextbookChapterAssignmentMapper.class);
    contentMapper = mock(TextbookSectionContentMapper.class);
    guard = new TextbookStructureGuard(assignmentMapper, contentMapper);
  }

  private void contentCount(long count) {
    when(contentMapper.selectCount(any())).thenReturn(count);
  }

  private void activeAssignmentCount(long count) {
    when(assignmentMapper.selectCount(any())).thenReturn(count);
  }

  private TextbookProjectEntity project(String status) {
    TextbookProjectEntity project = new TextbookProjectEntity();
    project.setId(1L);
    project.setTenantId(10159L);
    project.setStatus(status);
    return project;
  }

  @Test
  @DisplayName("有有效分配锁结构；有正文锁结构")
  void lockedByAssignmentOrContent() {
    contentCount(0);
    activeAssignmentCount(1);
    assertTrue(guard.isLocked(1L), "有有效分配即锁定");
    assertTrue(guard.existsActiveAssignment(1L));

    activeAssignmentCount(0);
    contentCount(2);
    assertTrue(guard.isLocked(1L), "有正文即锁定（即使全部分配已取消）");

    contentCount(0);
    activeAssignmentCount(0);
    assertTrue(!guard.isLocked(1L));
  }

  @Test
  @DisplayName("身份变化闸门：writing/done 409；reviewed 需先撤回；锁定 409；parsed/outlined 未锁定放行")
  void identityChangeGate() {
    assertConflicts(guard, project("writing"), "新增大纲节点");
    assertConflicts(guard, project("done"), "删除大纲节点");
    assertConflicts(guard, project("reviewed"), "重建骨架");

    TextbookProjectEntity outlined = project("outlined");
    contentCount(0);
    activeAssignmentCount(1);
    assertConflicts(guard, outlined, "重建骨架");

    contentCount(0);
    activeAssignmentCount(0);
    guard.assertIdentityChangeAllowed(outlined, "重建骨架"); // 不抛
  }

  @Test
  @DisplayName("安全编辑：标题/字数/排序 writing 允许、done 409")
  void safeEditGate() {
    guard.assertSafeEditAllowed(project("writing"), "编辑标题");
    guard.assertSafeEditAllowed(project("reviewed"), "调整排序");
    BusinessException ex = assertThrows(BusinessException.class,
        () -> guard.assertSafeEditAllowed(project("done"), "编辑标题"));
    assertEquals(ErrorCode.CONFLICT, ex.getCode());
  }

  private static void assertConflicts(TextbookStructureGuard guard,
      TextbookProjectEntity project, String operation) {
    BusinessException ex = assertThrows(BusinessException.class,
        () -> guard.assertIdentityChangeAllowed(project, operation));
    assertEquals(ErrorCode.CONFLICT, ex.getCode());
  }

  // ============================================================
  // 旧 PUT 状态白名单（TextbookProjectApplicationService.update，方案 §3.5）
  // ============================================================

  private TextbookProjectApplicationService projectService;
  private TextbookProjectLockService lockService;
  private TextbookStructureGuard realGuard;
  private java.util.Map<Long, TextbookProjectEntity> projectsDb;

  @BeforeEach
  void setUpProjectService() {
    lockService = mock(TextbookProjectLockService.class);
    realGuard = new TextbookStructureGuard(assignmentMapper, contentMapper);
    projectService = new TextbookProjectApplicationService(
        mock(com.guoyu.platform.textbook.mapper.TextbookProjectMapper.class),
        mock(com.guoyu.platform.textbook.mapper.TextbookOutlineNodeMapper.class),
        mock(TextbookOutlineApplicationService.class),
        mock(com.guoyu.platform.file.service.FileApplicationService.class),
        mock(com.guoyu.platform.file.mapper.FileMapper.class),
        new com.fasterxml.jackson.databind.ObjectMapper(),
        lockService, realGuard, assignmentMapper,
        mock(com.guoyu.platform.textbook.mapper.TextbookSectionContentMapper.class),
        mock(com.guoyu.platform.textbook.mapper.TextbookSectionContentVersionMapper.class),
        mock(com.guoyu.platform.textbook.mapper.TextbookSectionReviewRecordMapper.class),
        mock(com.guoyu.platform.textbook.mapper.TextbookSectionAssetMapper.class));
    // contentMapper 复用本类桩；projectDb 简化：lockProject 直接回项目
    projectsDb = new java.util.HashMap<>();
    when(lockService.lockProject(any())).thenAnswer(inv -> projectsDb.get(0L));
    contentCount(0);
    activeAssignmentCount(0);
  }

  private void stage(String status) {
    projectsDb.put(0L, project(status));
  }

  @Test
  @DisplayName("旧 PUT：writing 锁定时不能降级；done 一律不能降级（受控撤回的 unlocked writing→outlined 见集成测试）")
  void oldPutCannotDowngradeWritingOrDone() {
    for (String current : List.of("writing", "done")) {
      // writing 的唯一受控撤回前提是“全部分配取消且无正文”；这里模拟仍有有效分配
      contentCount(0);
      activeAssignmentCount(1);
      for (String target : List.of("outlined", "parsed", "reviewed")) {
        stage(current);
        BusinessException ex = assertThrows(BusinessException.class,
            () -> projectService.update(1L, new ProjectUpdateReq(null, null, null,
                null, target)),
            current + " -> " + target + " 必须被拒绝");
        assertEquals(ErrorCode.CONFLICT, ex.getCode());
      }
    }
  }

  @Test
  @DisplayName("旧 PUT 不能执行 writing→done / done→writing（只允许 C5/C6）")
  void oldPutCannotCompleteOrReopen() {
    stage("writing");
    BusinessException toDone = assertThrows(BusinessException.class,
        () -> projectService.update(1L, new ProjectUpdateReq(null, null, null, null, "done")));
    assertEquals(ErrorCode.CONFLICT, toDone.getCode());
    assertTrue(toDone.getMessage().contains("complete/reopen"));

    stage("done");
    BusinessException toWriting = assertThrows(BusinessException.class,
        () -> projectService.update(1L, new ProjectUpdateReq(null, null, null, null, "writing")));
    assertEquals(ErrorCode.CONFLICT, toWriting.getCode());
  }

  @Test
  @DisplayName("结构字段与 status 同一请求 → 400")
  void structuralAndStatusInSameRequestRejected() {
    stage("outlined");
    BusinessException ex = assertThrows(BusinessException.class,
        () -> projectService.update(1L, new ProjectUpdateReq(null, 100, 2500,
            null, "reviewed")));
    assertEquals(ErrorCode.BAD_REQUEST, ex.getCode());
    assertTrue(ex.getMessage().contains("同时携带"));
  }

  @Test
  @DisplayName("reviewed 撤回为 outlined：结构锁定后 409（须先取消分配且无正文）")
  void reviewedWithdrawBlockedWhenLocked() {
    contentCount(0);
    activeAssignmentCount(1);
    stage("reviewed");
    BusinessException ex = assertThrows(BusinessException.class,
        () -> projectService.update(1L, new ProjectUpdateReq(null, null, null, null, "outlined")));
    assertEquals(ErrorCode.CONFLICT, ex.getCode());
    assertTrue(ex.getMessage().contains("结构已锁定"));
  }
}
