package com.guoyu.platform.textbook.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.textbook.entity.TextbookChapterAssignmentEntity;
import com.guoyu.platform.textbook.entity.TextbookProjectEntity;
import com.guoyu.platform.textbook.entity.TextbookSectionContentEntity;
import com.guoyu.platform.textbook.mapper.TextbookChapterAssignmentMapper;
import com.guoyu.platform.textbook.mapper.TextbookSectionContentMapper;
import org.springframework.stereotype.Component;

/**
 * 编写期结构冻结（方案 §1.2/§5.7）：首个有效章分配或任一正文记录存在即锁定结构身份。
 *
 * <p>锁定后禁止：新增/删除大纲节点、手动 rebuild、改 functionNos/学时/系数触发 regenerate；
 * 仍允许：章/节标题、目标字数、同级排序（writing 下；done 一律只读）。
 * 无分配/无正文检查必须在工程行锁后执行（方案 §7.1），防「检查通过后首个分配恰好创建，
 * rebuild 又删除节点」。
 */
@Component
public class TextbookStructureGuard {

  private final TextbookChapterAssignmentMapper assignmentMapper;
  private final TextbookSectionContentMapper contentMapper;

  public TextbookStructureGuard(TextbookChapterAssignmentMapper assignmentMapper,
      TextbookSectionContentMapper contentMapper) {
    this.assignmentMapper = assignmentMapper;
    this.contentMapper = contentMapper;
  }

  /** locked = 存在任一正文记录 或 存在任一有效分配（方案 §5.7）。 */
  public boolean isLocked(Long projectId) {
    return existsAnyContent(projectId) || existsActiveAssignment(projectId);
  }

  public boolean existsAnyContent(Long projectId) {
    Long count = contentMapper.selectCount(new LambdaQueryWrapper<TextbookSectionContentEntity>()
        .eq(TextbookSectionContentEntity::getProjectId, projectId));
    return count != null && count > 0;
  }

  public boolean existsActiveAssignment(Long projectId) {
    Long count = assignmentMapper.selectCount(
        new LambdaQueryWrapper<TextbookChapterAssignmentEntity>()
            .eq(TextbookChapterAssignmentEntity::getProjectId, projectId)
            .eq(TextbookChapterAssignmentEntity::getActive, 1));
    return count != null && count > 0;
  }

  /**
   * 结构身份变化（新增/删除节点、rebuild、regenerate 触发）统一闸门：
   * 仅 parsed/outlined 且未锁定允许；reviewed 必须先显式撤回为 outlined（方案 §5.7）。
   * 必须在工程行锁内调用。
   */
  public void assertIdentityChangeAllowed(TextbookProjectEntity project, String operation) {
    if (TextbookProjectApplicationService.STATUS_WRITING.equals(project.getStatus())
        || TextbookProjectApplicationService.STATUS_DONE.equals(project.getStatus())) {
      throw new BusinessException(ErrorCode.CONFLICT,
          "工程已进入编写阶段（" + project.getStatus() + "），" + operation + " 已被结构锁定禁止；"
              + "如需重建请先完成/重新打开并参考结构冻结规则");
    }
    if (TextbookProjectApplicationService.STATUS_REVIEWED.equals(project.getStatus())) {
      throw new BusinessException(ErrorCode.CONFLICT,
          "大纲已评审（reviewed），" + operation + " 前必须先撤回大纲评审为 outlined");
    }
    if (isLocked(project.getId())) {
      throw new BusinessException(ErrorCode.CONFLICT,
          "工程已存在章节分配或正文，结构已锁定，禁止" + operation
              + "；如确需重建，请先取消全部分配且确认无任何正文");
    }
  }

  /** 安全编辑（标题/目标字数/同级排序）：outlined/reviewed/writing 允许，done 一律只读。 */
  public void assertSafeEditAllowed(TextbookProjectEntity project, String operation) {
    if (TextbookProjectApplicationService.STATUS_DONE.equals(project.getStatus())) {
      throw new BusinessException(ErrorCode.CONFLICT,
          "工程已完成（done），全页只读，禁止" + operation);
    }
  }
}
